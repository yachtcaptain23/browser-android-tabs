/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "bat_publisher.h"
#include "leveldb/db.h"
#include "base/sequenced_task_runner.h"
#include "base/bind.h"
#include "base/files/file_path.h"
#include "base/path_service.h"
#include "base/files/file_util.h"
#include "base/task_scheduler/post_task.h"
#include "chrome/browser/browser_process.h"
#include <cmath>

#include "logging.h"

#define PUBLISHERS_DB_NAME  "ledger_publishers"

namespace bat_publisher {

static const unsigned int _min_pubslisher_duration = 8000;  // In milliseconds

static double _d = 1.0 / (30.0 * 1000.0);
static unsigned int _a = 1.0 / (_d * 2.0) - _min_pubslisher_duration;
static unsigned int _a2 = _a * 2;
static unsigned int _a4 = _a2 * 2;
static unsigned int _b = _min_pubslisher_duration - _a;
static unsigned int _b2 = _b * _b;


BatPublisher::BatPublisher():
  level_db_(nullptr) {
}

BatPublisher::~BatPublisher() {
  if (nullptr != level_db_) {
    delete level_db_;
  }
}

void BatPublisher::openPublishersDB() {
  base::FilePath dbFilePath;
  base::PathService::Get(base::DIR_HOME, &dbFilePath);
  dbFilePath = dbFilePath.Append(PUBLISHERS_DB_NAME);

  leveldb::Options options;
  options.create_if_missing = true;
  leveldb::Status status = leveldb::DB::Open(options, dbFilePath.value().c_str(), &level_db_);
  if (!status.ok() || !level_db_) {
      if (level_db_) {
          delete level_db_;
          level_db_ = nullptr;
      }

      LOG(ERROR) << "openPublishersDB level db open error " << dbFilePath.value().c_str();
  }
}

void BatPublisher::loadPublishers() {
  openPublishersDB();
  if (!level_db_) {
    assert(false);
    LOG(ERROR) << "loadPublishers level db is not initialized";

    return;
  }

  std::lock_guard<std::mutex> guard(publishers_map_mutex_);
  leveldb::Iterator* it = level_db_->NewIterator(leveldb::ReadOptions());
  for (it->SeekToFirst(); it->Valid(); it->Next()) {
    std::string publisher = it->key().ToString();
    PUBLISHER_ST publisher_st;
    BatHelper::getJSONPublisher(it->value().ToString(), publisher_st);
    publishers_[publisher] = publisher_st;
  }
  assert(it->status().ok());  // Check for any errors found during the scan
  delete it;
}

void BatPublisher::initSynopsis() {
  scoped_refptr<base::SequencedTaskRunner> task_runner =
     base::CreateSequencedTaskRunnerWithTraits(
         {base::MayBlock(), base::TaskShutdownBehavior::SKIP_ON_SHUTDOWN});
  task_runner->PostTask(FROM_HERE, base::Bind(&BatPublisher::loadPublishers, base::Unretained(this)));
}

void BatPublisher::saveVisitInternal(const std::string& publisher, const uint64_t& duration) {
  double currentScore = concaveScore(duration);

  std::string stringifiedPublisher;
  std::lock_guard<std::mutex> guard(publishers_map_mutex_);
  std::map<std::string, PUBLISHER_ST>::iterator iter = publishers_.find(publisher);
  if (publishers_.end() == iter) {
    PUBLISHER_ST publisher_st;
    publisher_st.duration_ = duration;
    publisher_st.score_ = currentScore;
    publisher_st.visits_ = 1;
    publishers_[publisher] = publisher_st;
    stringifiedPublisher = BatHelper::stringifyPublisher(publisher_st);
  } else {
    iter->second.duration_ += duration;
    iter->second.score_ += currentScore;
    iter->second.visits_ += 1;
    stringifiedPublisher = BatHelper::stringifyPublisher(iter->second);
  }
  if (!level_db_) {
    assert(false);

    return;
  }

  // Save the publisher to the database
  leveldb::Status status = level_db_->Put(leveldb::WriteOptions(), publisher, stringifiedPublisher);
  assert(status.ok());
}

void BatPublisher::saveVisit(const std::string& publisher, const uint64_t& duration) {
  if (duration < _min_pubslisher_duration) {
    return;
  }

  // TODO checks if the publisher verified, disabled and etc

  scoped_refptr<base::SequencedTaskRunner> task_runner =
     base::CreateSequencedTaskRunnerWithTraits(
         {base::MayBlock(), base::TaskShutdownBehavior::SKIP_ON_SHUTDOWN});
  task_runner->PostTask(FROM_HERE, base::Bind(&BatPublisher::saveVisitInternal, base::Unretained(this),
    publisher, duration));
}

double BatPublisher::concaveScore(const uint64_t& duration) {
  return (std::sqrt(_b2 + _a4 * duration) - _b) / (double)_a2;
}

}
