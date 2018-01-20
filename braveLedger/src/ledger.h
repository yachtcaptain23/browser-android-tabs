/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef LEDGER_H_
#define LEDGER_H_

#include <string>

namespace bat_client {
  class BatClient;
}

namespace bat_publisher {
  class BatPublisher;
}

namespace ledger {

class Ledger {
public:
  Ledger();
  ~Ledger();

  void createWallet();
  void initSynopsis();
  void saveVisit(const std::string& publisher, const uint64_t& duration);

private:
  bool isBatClientExist();
  bool isBatPublisherExist();

  bat_client::BatClient* bat_client_;
  bat_publisher::BatPublisher* bat_publisher_;
};
}

#endif  // LEDGER_H_
