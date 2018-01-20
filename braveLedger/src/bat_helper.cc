/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "bat_helper.h"
#include "static_values.h"
#include "base/json/json_reader.h"
#include "base/json/json_writer.h"
#include "base/values.h"
#include "base/files/file_path.h"
#include "base/path_service.h"
#include "base/files/file_util.h"
#include "base/sequenced_task_runner.h"
#include "base/bind.h"
#include "base/task_scheduler/post_task.h"
#include "chrome/browser/browser_process.h"
#include "browser_thread.h"
#include "tweetnacl.h"
#include <openssl/hkdf.h>
#include <openssl/digest.h>
#include <openssl/sha.h>
#include <openssl/base64.h>

#include <sstream>
#include <random>
#include <utility>
#include <iomanip>
#include <ctime>


REQUEST_CREDENTIALS_ST::REQUEST_CREDENTIALS_ST() {}
REQUEST_CREDENTIALS_ST::~REQUEST_CREDENTIALS_ST() {}
WALLET_INFO_ST::WALLET_INFO_ST() {}
WALLET_INFO_ST::~WALLET_INFO_ST() {}
STATE_ST::STATE_ST(): settings_(AD_FREE_SETTINGS) {}
STATE_ST::~STATE_ST() {}
PUBLISHER_ST::PUBLISHER_ST():
  duration_(0),
  score_(0),
  visits_(0) {
}
PUBLISHER_ST::~PUBLISHER_ST() {}



BatHelper::BatHelper() {
}

BatHelper::~BatHelper() {
}

std::string BatHelper::getJSONValue(const std::string& fieldName, const std::string& json) {
  std::string res;

  std::unique_ptr<base::Value> json_object = base::JSONReader::Read(json);
  if (nullptr == json_object.get()) {
      LOG(ERROR) << "BatHelper::getJSONValue: incorrect json object";

      return "";
  }

  const base::DictionaryValue* childTopDictionary = nullptr;
  json_object->GetAsDictionary(&childTopDictionary);
  if (nullptr == childTopDictionary) {
      return "";
  }
  const base::Value* value = nullptr;
  if (childTopDictionary->Get(fieldName, &value)) {
    if (value->GetAsString(&res)) {
      return res;
    }
  }

  return res;
}

void BatHelper::getJSONState(const std::string& json, STATE_ST& state) {
  std::unique_ptr<base::Value> json_object = base::JSONReader::Read(json);
  if (nullptr == json_object.get()) {
      LOG(ERROR) << "BatHelper::getJSONState: incorrect json object";

      return;
  }

  const base::DictionaryValue* childTopDictionary = nullptr;
  json_object->GetAsDictionary(&childTopDictionary);
  if (nullptr == childTopDictionary) {
      return;
  }
  const base::Value* value = nullptr;
  if (childTopDictionary->Get("bootStamp", &value)) {
    std::string bootStamp;
    value->GetAsString(&bootStamp);
    std::stringstream temp(bootStamp);
    temp >> state.bootStamp_;
    DCHECK(state.bootStamp_ != 0);
  }
  if (childTopDictionary->Get("reconcileStamp", &value)) {
    std::string reconcileStamp;
    value->GetAsString(&reconcileStamp);
    std::stringstream temp(reconcileStamp);
    temp >> state.reconcileStamp_;
    DCHECK(state.reconcileStamp_ != 0);
  }
  if (childTopDictionary->Get("personaId", &value)) {
    value->GetAsString(&state.personaId_);
    DCHECK(!state.personaId_.empty());
  }
  if (childTopDictionary->Get("userId", &value)) {
    value->GetAsString(&state.userId_);
    DCHECK(!state.userId_.empty());
  }
  if (childTopDictionary->Get("registrarVK", &value)) {
    value->GetAsString(&state.registrarVK_);
    DCHECK(!state.registrarVK_.empty());
  }
  if (childTopDictionary->Get("masterUserToken", &value)) {
    value->GetAsString(&state.masterUserToken_);
    DCHECK(!state.masterUserToken_.empty());
  }
  if (childTopDictionary->Get("fee_currency", &value)) {
    value->GetAsString(&state.fee_currency_);
    DCHECK(!state.fee_currency_.empty());
  }
  if (childTopDictionary->Get("settings", &value)) {
    value->GetAsString(&state.settings_);
    DCHECK(!state.settings_.empty());
  }
  if (childTopDictionary->Get("fee_amount", &value)) {
    value->GetAsDouble(&state.fee_amount_);
    DCHECK(0 != state.fee_amount_);
  }
  if (childTopDictionary->Get("days", &value)) {
    value->GetAsInteger((int*)&state.days_);
    DCHECK(0 != state.days_);
  }
  if (childTopDictionary->Get("wallet_info.paymentId", &value)) {
    value->GetAsString(&state.walletInfo_.paymentId_);
    DCHECK(!state.walletInfo_.paymentId_.empty());
  }
  if (childTopDictionary->Get("wallet_info.addressBAT", &value)) {
    value->GetAsString(&state.walletInfo_.addressBAT_);
    DCHECK(!state.walletInfo_.addressBAT_.empty());
  }
  if (childTopDictionary->Get("wallet_info.addressBTC", &value)) {
    value->GetAsString(&state.walletInfo_.addressBTC_);
    DCHECK(!state.walletInfo_.addressBTC_.empty());
  }
  if (childTopDictionary->Get("wallet_info.addressCARD_ID", &value)) {
    value->GetAsString(&state.walletInfo_.addressCARD_ID_);
    DCHECK(!state.walletInfo_.addressCARD_ID_.empty());
  }
  if (childTopDictionary->Get("wallet_info.addressETH", &value)) {
    value->GetAsString(&state.walletInfo_.addressETH_);
    DCHECK(!state.walletInfo_.addressETH_.empty());
  }
  if (childTopDictionary->Get("wallet_info.addressLTC", &value)) {
    value->GetAsString(&state.walletInfo_.addressLTC_);
    DCHECK(!state.walletInfo_.addressLTC_.empty());
  }
  if (childTopDictionary->Get("wallet_info.keyInfoSeed_", &value)) {
    std::string keyInfoSeed;
    value->GetAsString(&keyInfoSeed);
    DCHECK(!keyInfoSeed.empty());
    state.walletInfo_.keyInfoSeed_ = BatHelper::getFromBase64(keyInfoSeed);
  }
}

void BatHelper::getJSONPublisher(const std::string& json, PUBLISHER_ST& publisher_st) {
  std::unique_ptr<base::Value> json_object = base::JSONReader::Read(json);
  if (nullptr == json_object.get()) {
      LOG(ERROR) << "BatHelper::getJSONPublisher: incorrect json object";

      return;
  }

  const base::DictionaryValue* childTopDictionary = nullptr;
  json_object->GetAsDictionary(&childTopDictionary);
  if (nullptr == childTopDictionary) {
      return;
  }
  const base::Value* value = nullptr;
  if (childTopDictionary->Get("duration", &value)) {
    std::string duration;
    value->GetAsString(&duration);
    std::stringstream temp(duration);
    temp >> publisher_st.duration_;
  }
  if (childTopDictionary->Get("fav_icon_URL", &value)) {
    value->GetAsString(&publisher_st.fav_icon_URL_);
  }
  if (childTopDictionary->Get("score", &value)) {
    value->GetAsDouble(&publisher_st.score_);
  }
  if (childTopDictionary->Get("visits", &value)) {
    value->GetAsInteger((int*)&publisher_st.visits_);
  }
}


void BatHelper::getJSONWalletInfo(const std::string& json, WALLET_INFO_ST& walletInfo,
      std::string& fee_currency, double& fee_amount, unsigned int& days) {
  std::unique_ptr<base::Value> json_object = base::JSONReader::Read(json);
  if (nullptr == json_object.get()) {
      LOG(ERROR) << "BatHelper::getJSONWalletInfo: incorrect json object";

      return;
  }

  const base::DictionaryValue* childTopDictionary = nullptr;
  json_object->GetAsDictionary(&childTopDictionary);
  if (nullptr == childTopDictionary) {
      return;
  }
  const base::Value* value = nullptr;
  if (childTopDictionary->Get("wallet.paymentId", &value)) {
    value->GetAsString(&walletInfo.paymentId_);
    DCHECK(!walletInfo.paymentId_.empty());
  }
  if (childTopDictionary->Get("wallet.addresses.BAT", &value)) {
    value->GetAsString(&walletInfo.addressBAT_);
    DCHECK(!walletInfo.addressBAT_.empty());
  }
  if (childTopDictionary->Get("wallet.addresses.BTC", &value)) {
    value->GetAsString(&walletInfo.addressBTC_);
    DCHECK(!walletInfo.addressBTC_.empty());
  }
  if (childTopDictionary->Get("wallet.addresses.CARD_ID", &value)) {
    value->GetAsString(&walletInfo.addressCARD_ID_);
    DCHECK(!walletInfo.addressCARD_ID_.empty());
  }
  if (childTopDictionary->Get("wallet.addresses.ETH", &value)) {
    value->GetAsString(&walletInfo.addressETH_);
    DCHECK(!walletInfo.addressETH_.empty());
  }
  if (childTopDictionary->Get("wallet.addresses.LTC", &value)) {
    value->GetAsString(&walletInfo.addressLTC_);
    DCHECK(!walletInfo.addressLTC_.empty());
  }

  if (childTopDictionary->Get("payload.adFree.days", &value)) {
    value->GetAsInteger((int*)&days);
    DCHECK(days != 0);
  }
  const base::DictionaryValue* feeDictionary = nullptr;
  if (childTopDictionary->Get("payload.adFree.fee", &value)) {
    if (!value->GetAsDictionary(&feeDictionary)) {
      LOG(ERROR) << "BatHelper::getJSONWalletInfo: could not get fee object";

      return;
    }
    base::detail::const_dict_iterator_proxy dictIterator = feeDictionary->DictItems();
    if (dictIterator.begin() != dictIterator.end()) {
      fee_currency = dictIterator.begin()->first;
      dictIterator.begin()->second.GetAsDouble(&fee_amount);
    }
  }
}

std::vector<uint8_t> BatHelper::generateSeed() {
  //std::ostringstream seedStr;

  std::vector<uint8_t> vSeed(SEED_LENGTH);
  std::random_device r;
  std::seed_seq seed{r(), r(), r(), r(), r(), r(), r(), r()};
  auto rand = std::bind(std::uniform_int_distribution<>(0, UCHAR_MAX),
                          std::mt19937(seed));

  //std::generate_n(std::ostream_iterator<int>(seedStr, ","), seedLength, rand);
  std::generate_n(vSeed.begin(), SEED_LENGTH, rand);
  /*for (size_t i = 0; i < vSeed.size(); i++) {
    if (0 != i) {
      seedStr << ",";
    }
    seedStr << vSeed[i];
  }
  std::string res = seedStr.str();
  //if (!res.empty()) {
  //  res.erase(res.end() - 1);
  //}
  LOG(ERROR) << res;

  return res;*/
  return vSeed;
}

std::vector<uint8_t> BatHelper::getHKDF(const std::vector<uint8_t>& seed) {
  DCHECK(!seed.empty());
  std::vector<uint8_t> out(SEED_LENGTH);
  //uint8_t out[SEED_LENGTH];
  //to do debug
  /*std::ostringstream seedStr1;
  for (size_t i = 0; i < SEED_LENGTH; i++) {
    if (0 != i) {
      seedStr1 << ",";
    }
    seedStr1 << (int)seed[i];
  }
  LOG(ERROR) << "!!!seed == " << seedStr1.str();*/
  //
  int hkdfRes = HKDF(&out.front(), SEED_LENGTH, EVP_sha512(), &seed.front(), seed.size(),
    g_hkdfSalt, SALT_LENGTH, nullptr, 0);

  DCHECK(hkdfRes);
  DCHECK(!seed.empty());

  //to do debug
  /*std::ostringstream seedStr;
  for (size_t i = 0; i < SEED_LENGTH; i++) {
    if (0 != i) {
      seedStr << ",";
    }
    seedStr << (int)out[i];
  }
  LOG(ERROR) << "!!!hkdfRes == " << hkdfRes << ", out == " << seedStr.str();*/
  //

  return out;
}

void BatHelper::getPublicKeyFromSeed(const std::vector<uint8_t>& seed,
      std::vector<uint8_t>& publicKey, std::vector<uint8_t>& secretKey) {
  DCHECK(!seed.empty());
  publicKey.resize(crypto_sign_PUBLICKEYBYTES);
  secretKey = seed;
  secretKey.resize(crypto_sign_SECRETKEYBYTES);

  crypto_sign_keypair(&publicKey.front(), &secretKey.front(), 1);

  DCHECK(!publicKey.empty() && !secretKey.empty());
  //to do debug
  /*std::ostringstream publicStr;
  for (size_t i = 0; i < crypto_sign_PUBLICKEYBYTES; i++) {
    if (0 != i) {
      publicStr << ",";
    }
    publicStr << (int)outPublic[i];
  }
  std::ostringstream secretStr;
  for (size_t i = 0; i < crypto_sign_SECRETKEYBYTES; i++) {
    if (0 != i) {
      secretStr << ",";
    }
    secretStr << (int)outSecret[i];
  }
  LOG(ERROR) << "!!!publicStr == " << publicStr.str();
  LOG(ERROR) << "!!!secretStr == " << secretStr.str();*/
}

std::string BatHelper::uint8ToHex(const std::vector<uint8_t>& in) {
  std::ostringstream res;
  for (size_t i = 0; i < in.size(); i++) {
    res << std::setfill('0') << std::setw(sizeof(uint8_t) * 2)
       << std::hex << (int)in[i];
  }

  return res.str();
}

std::string BatHelper::stringify(std::string* keys,
    std::string* values, const unsigned int& size) {
  std::string res;

  base::DictionaryValue root_dict;
  for (unsigned int i = 0; i < size; i++) {
    root_dict.SetString(keys[i], values[i]);
  }

  base::JSONWriter::Write(root_dict, &res);

  return res;
}

std::string BatHelper::stringifyRequestCredentialsSt(const REQUEST_CREDENTIALS_ST& request_credentials) {
  std::string res;

  base::DictionaryValue root_dict;
  root_dict.SetString("requestType", request_credentials.requestType_);
  std::unique_ptr<base::DictionaryValue> request_dict(new base::DictionaryValue());
  std::unique_ptr<base::DictionaryValue> request_headers_dict(new base::DictionaryValue());
  request_headers_dict->SetString("digest", request_credentials.request_headers_digest_);
  request_headers_dict->SetString("signature", request_credentials.request_headers_signature_);
  request_dict->Set("headers", std::move(request_headers_dict));
  std::unique_ptr<base::DictionaryValue> request_body_dict(new base::DictionaryValue());
  request_body_dict->SetString("currency", request_credentials.request_body_currency_);
  request_body_dict->SetString("label", request_credentials.request_body_label_);
  request_body_dict->SetString("publicKey", request_credentials.request_body_publicKey_);
  request_dict->Set("body", std::move(request_body_dict));
  request_dict->SetString("octets", request_credentials.request_body_octets_);
  root_dict.Set("request", std::move(request_dict));
  root_dict.SetString("proof", request_credentials.proof_);

  base::JSONWriter::Write(root_dict, &res);

  return res;
}

std::string BatHelper::stringifyState(const STATE_ST& state) {
  std::string res;

  base::DictionaryValue root_dict;
  root_dict.SetString("bootStamp", std::to_string(state.bootStamp_));
  root_dict.SetString("reconcileStamp", std::to_string(state.reconcileStamp_));
  root_dict.SetString("personaId", state.personaId_);
  root_dict.SetString("userId", state.userId_);
  root_dict.SetString("registrarVK", state.registrarVK_);
  root_dict.SetString("masterUserToken", state.masterUserToken_);
  root_dict.SetString("fee_currency", state.fee_currency_);
  root_dict.SetString("settings", state.settings_);
  root_dict.SetDouble("fee_amount", state.fee_amount_);
  root_dict.SetInteger("days", state.days_);
  std::unique_ptr<base::DictionaryValue> wallet_info_dict(new base::DictionaryValue());
  wallet_info_dict->SetString("paymentId", state.walletInfo_.paymentId_);
  wallet_info_dict->SetString("addressBAT", state.walletInfo_.addressBAT_);
  wallet_info_dict->SetString("addressBTC", state.walletInfo_.addressBTC_);
  wallet_info_dict->SetString("addressCARD_ID", state.walletInfo_.addressCARD_ID_);
  wallet_info_dict->SetString("addressETH", state.walletInfo_.addressETH_);
  wallet_info_dict->SetString("addressLTC", state.walletInfo_.addressLTC_);
  wallet_info_dict->SetString("keyInfoSeed_", BatHelper::getBase64(state.walletInfo_.keyInfoSeed_));
  root_dict.Set("wallet_info", std::move(wallet_info_dict));

  base::JSONWriter::Write(root_dict, &res);

  return res;
}

std::string BatHelper::stringifyPublisher(PUBLISHER_ST& publisher_st) {
  std::string res;

  base::DictionaryValue root_dict;
  root_dict.SetString("duration", std::to_string(publisher_st.duration_));
  root_dict.SetString("fav_icon_URL", publisher_st.fav_icon_URL_);
  root_dict.SetDouble("score", publisher_st.score_);
  root_dict.SetInteger("visits", publisher_st.visits_);

  base::JSONWriter::Write(root_dict, &res);
  LOG(ERROR) << "!!!stringifyPublisher res == " << res;

  return res;
}

std::vector<uint8_t> BatHelper::getSHA256(const std::string& in) {
  std::vector<uint8_t> res(SHA256_DIGEST_LENGTH);

  SHA256((uint8_t*)in.c_str(), in.length(), &res.front());

  return res;
}

std::string BatHelper::getBase64(const std::vector<uint8_t>& in) {
  std::string res;

  size_t size = 0;
  if (!EVP_EncodedLength(&size, in.size())) {
    DCHECK(false);
    LOG(ERROR) << "EVP_EncodedLength failure in BatHelper::getBase64";

    return "";
  }
  std::vector<uint8_t> out(size);
  DCHECK(EVP_EncodeBlock(&out.front(), &in.front(), in.size()) != 0);
  res = (char*)&out.front();

  return res;
}

std::vector<uint8_t> BatHelper::getFromBase64(const std::string& in) {
  std::vector<uint8_t> res;

  size_t size = 0;
  if (!EVP_DecodedLength(&size, in.length())) {
    DCHECK(false);
    LOG(ERROR) << "EVP_DecodedLength failure in BatHelper::getFromBase64";

    return res;
  }
  res.resize(size);
  DCHECK(EVP_DecodeBase64(&res.front(), &size, size, (const uint8_t*)in.c_str(), in.length()));
  LOG(ERROR) << "!!!decoded size == " << size;

  return res;
}

std::string BatHelper::sign(std::string* keys, std::string* values, const unsigned int& size,
    const std::string& keyId, const std::vector<uint8_t>& secretKey) {
  std::string headers;
  std::string message;
  for (unsigned int i = 0; i < size; i++) {
    if (0 != i) {
      headers += " ";
      message += "\n";
    }
    headers += keys[i];
    message += keys[i] + ": " + values[i];
  }
  std::vector<uint8_t> signedMsg(crypto_sign_BYTES + message.length());
  unsigned long long signedMsgSize = 0;
  crypto_sign(&signedMsg.front(), &signedMsgSize, (const unsigned char*)message.c_str(),
    (unsigned long long)message.length(), &secretKey.front());
  std::vector<uint8_t> signature(crypto_sign_BYTES);
  std::copy(signedMsg.begin(), signedMsg.begin() + crypto_sign_BYTES, signature.begin());

  return "keyId=\"" + keyId + "\",algorithm=\"" + SIGNATURE_ALGORITHM +
    "\",headers=\"" + headers + "\",signature=\"" + BatHelper::getBase64(signature) + "\"";
}

unsigned long long BatHelper::currentTime() {
  return time(0);
}

void BatHelper::writeStateFile(const std::string& data) {
  base::FilePath dirToSave;
  base::PathService::Get(base::DIR_HOME, &dirToSave);
  dirToSave = dirToSave.Append(LEDGER_STATE_FILENAME);

  // TODO write the state
}

void BatHelper::readStateFile(BatHelper::ReadStateCallback callback) {
  base::FilePath dirToSave;
  base::PathService::Get(base::DIR_HOME, &dirToSave);
  dirToSave = dirToSave.Append(LEDGER_STATE_FILENAME);
  int64_t file_size = 0;
  if (!GetFileSize(dirToSave, &file_size)) {
    callback.Run(false, STATE_ST());

    return;
  }
  std::vector<char> data(file_size + 1);
  if (-1 != base::ReadFile(dirToSave, &data.front(), file_size)) {
    data[file_size] = '\0';
    STATE_ST state;
    BatHelper::getJSONState(&data.front(), state);
    callback.Run(true, state);

    return;
  }

  callback.Run(false, STATE_ST());
}

void BatHelper::saveState(const STATE_ST& state) {
  std::string data = BatHelper::stringifyState(state);
  scoped_refptr<base::SequencedTaskRunner> task_runner =
     base::CreateSequencedTaskRunnerWithTraits(
         {base::MayBlock(), base::TaskShutdownBehavior::SKIP_ON_SHUTDOWN});
  task_runner->PostTask(FROM_HERE, base::Bind(&BatHelper::writeStateFile,
     data));
}

void BatHelper::loadState(BatHelper::ReadStateCallback callback) {
  scoped_refptr<base::SequencedTaskRunner> task_runner =
     base::CreateSequencedTaskRunnerWithTraits(
         {base::MayBlock(), base::TaskShutdownBehavior::SKIP_ON_SHUTDOWN});
  task_runner->PostTask(FROM_HERE, base::Bind(&BatHelper::readStateFile, callback));
}
