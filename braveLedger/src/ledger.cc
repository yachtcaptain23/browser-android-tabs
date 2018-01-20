/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "ledger.h"
#include "bat_client.h"
#include "bat_publisher.h"

#include "logging.h"


using namespace bat_client;
using namespace bat_publisher;

namespace ledger {

  Ledger::Ledger():
    bat_client_(nullptr),
    bat_publisher_(nullptr) {
  }

  Ledger::~Ledger() {
    if (bat_client_) {
      delete bat_client_;
    }
    if (bat_publisher_) {
      delete bat_publisher_;
    }
  }

  void Ledger::createWallet() {
    initSynopsis();
    if (!bat_client_) {
      bat_client_ = new BatClient();
    }
    bat_client_->loadStateOrRegisterPersona();
  }

  void Ledger::initSynopsis() {
    if (!bat_publisher_) {
      bat_publisher_ = new BatPublisher();
    }
    bat_publisher_->initSynopsis();
  }

  bool Ledger::isBatClientExist() {
    if (!bat_client_) {
      LOG(ERROR) << "ledger bat_client is not exist";

      return false;
    }

    return true;
  }

  bool Ledger::isBatPublisherExist() {
    if (!bat_publisher_) {
      LOG(ERROR) << "ledger bat_publisher is not exist";

      return false;
    }

    return true;
  }

  void Ledger::saveVisit(const std::string& publisher, const uint64_t& duration) {
    if (!isBatPublisherExist()) {
      assert(false);

      return;
    }
    bat_publisher_->saveVisit(publisher, duration);
  }

}
