/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef BAT_PUBLISHER_H_
#define BAT_PUBLISHER_H_

#include "bat_helper.h"
#include <string>
#include <map>
#include <mutex>

namespace leveldb {
class DB;
}

namespace bat_publisher {

class BatPublisher {
public:
  BatPublisher();
  ~BatPublisher();

  void initSynopsis();
  void saveVisit(const std::string& publisher, const uint64_t& duration);

private:
  double concaveScore(const uint64_t& duration);
  void openPublishersDB();
  void loadPublishers();
  void saveVisitInternal(const std::string& publisher, const uint64_t& duration);

  std::map<std::string, PUBLISHER_ST> publishers_;
  std::mutex publishers_map_mutex_;
  leveldb::DB* level_db_;
};

}

#endif  // BAT_PUBLISHER_H_
