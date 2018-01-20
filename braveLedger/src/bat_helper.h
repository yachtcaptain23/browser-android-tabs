/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef BAT_HELPER_H_
#define BAT_HELPER_H_

#include <string>
#include <vector>

#include "base/callback.h"

struct REQUEST_CREDENTIALS_ST {
  REQUEST_CREDENTIALS_ST();
  ~REQUEST_CREDENTIALS_ST();

  std::string proof_;
  std::string requestType_;
  std::string request_body_currency_;
  std::string request_body_label_;
  std::string request_body_publicKey_;
  std::string request_body_octets_;
  std::string request_headers_digest_;
  std::string request_headers_signature_;
};

struct WALLET_INFO_ST {
  WALLET_INFO_ST();
  ~WALLET_INFO_ST();

  std::string paymentId_;
  std::string addressBAT_;
  std::string addressBTC_;
  std::string addressCARD_ID_;
  std::string addressETH_;
  std::string addressLTC_;
  std::vector<uint8_t> keyInfoSeed_;
};

struct STATE_ST {
  STATE_ST();
  ~STATE_ST();

  WALLET_INFO_ST walletInfo_;
  unsigned long long bootStamp_;
  unsigned long long reconcileStamp_;
  std::string personaId_;
  std::string userId_;
  std::string registrarVK_;
  std::string masterUserToken_;
  std::string fee_currency_;
  std::string settings_;
  double fee_amount_;
  unsigned int days_;
};

struct PUBLISHER_ST {
  PUBLISHER_ST();
  ~PUBLISHER_ST();

  uint64_t duration_;
  std::string fav_icon_URL_;    // TODO
  double score_;
  unsigned int visits_;
};

class BatHelper {
public:
  typedef base::Callback<void(bool, const STATE_ST&)> ReadStateCallback;

  static std::string getJSONValue(const std::string& fieldName, const std::string& json);
  static void getJSONWalletInfo(const std::string& json, WALLET_INFO_ST& walletInfo,
    std::string& fee_currency, double& fee_amount, unsigned int& days);
  static void getJSONState(const std::string& json, STATE_ST& state);
  static void getJSONPublisher(const std::string& json, PUBLISHER_ST& publisher_st);
  static std::vector<uint8_t> generateSeed();
  static std::vector<uint8_t> getHKDF(const std::vector<uint8_t>& seed);
  static void getPublicKeyFromSeed(const std::vector<uint8_t>& seed,
    std::vector<uint8_t>& publicKey, std::vector<uint8_t>& secretKey);
  static std::string uint8ToHex(const std::vector<uint8_t>& in);
  static std::string stringify(std::string* keys, std::string* values, const unsigned int& size);
  static std::string stringifyRequestCredentialsSt(const REQUEST_CREDENTIALS_ST& request_credentials);
  static std::string stringifyState(const STATE_ST& state);
  static std::string stringifyPublisher(PUBLISHER_ST& publisher_st);
  static std::vector<uint8_t> getSHA256(const std::string& in);
  static std::string getBase64(const std::vector<uint8_t>& in);
  static std::vector<uint8_t> getFromBase64(const std::string& in);
  // Sign using ed25519 algorithm
  static std::string sign(std::string* keys, std::string* values, const unsigned int& size,
    const std::string& keyId, const std::vector<uint8_t>& secretKey);
  static unsigned long long currentTime();
  static void saveState(const STATE_ST& state);
  static void loadState(BatHelper::ReadStateCallback callback);
  // We have to implement different function for iOS, probably laptop
  static void writeStateFile(const std::string& data);
  // We have to implement different function for iOS, probably laptop
  static void readStateFile(BatHelper::ReadStateCallback callback);

private:
  BatHelper();
  ~BatHelper();
};

#endif  // BAT_HELPER_H_
