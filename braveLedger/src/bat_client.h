/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef BAT_CLIENT_H_
#define BAT_CLIENT_H_

#include <string>
#include <vector>
#include "bat_client_webrequest.h"
#include "bat_helper.h"

namespace bat_client {

class BatClient {
public:
  BatClient(const bool& useProxy = true);
  ~BatClient();

  void loadStateOrRegisterPersona();
  void requestCredentialsCallback(bool result, const std::string& response);
  void registerPersonaCallback(bool result, const std::string& response);
  void publisherTimestampCallback(bool result, const std::string& response);

private:
  void loadStateOrRegisterPersonaCallback(bool result, const STATE_ST& state);
  void registerPersona();
  void publisherTimestamp();

  std::string buildURL(const std::string& path, const std::string& prefix);

  bool useProxy_;
  BatClientWebRequest batClientWebRequest_;
  std::string personaId_;
  std::string userId_;
  std::string registrarVK_;
  std::string preFlight_;
  std::string masterUserToken_;
  WALLET_INFO_ST walletInfo_;
  std::string fee_currency_;
  double fee_amount_;
  unsigned int days_;
  unsigned long long bootStamp_;
  unsigned long long reconcileStamp_;
};
}

#endif  // BAT_CLIENT_H_
