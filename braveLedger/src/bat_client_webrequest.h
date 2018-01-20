/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef BAT_CLIENT_WEBREQUEST_H_
#define BAT_CLIENT_WEBREQUEST_H_

#include <string>
#include <list>
#include <vector>
#include <mutex>

#include "url_fetcher_delegate.h"
#include "base/callback.h"


// We have to implement another fetcher class for iOS

namespace net {
  class URLFetcher;
  class UploadDataStream;
}

namespace bat_client {

class BatClientWebRequest: public net::URLFetcherDelegate {
public:
  typedef base::Callback<void(bool, const std::string&)> FetchCallback;

  struct URL_FETCH_REQUEST {
    URL_FETCH_REQUEST();
    ~URL_FETCH_REQUEST();

    std::unique_ptr<net::URLFetcher> url_fetcher_;
    FetchCallback callback_;
  };

  BatClientWebRequest();
  ~BatClientWebRequest() final;

  void run(const std::string& url, BatClientWebRequest::FetchCallback callback,
    const std::vector<std::string>& headers, const std::string& content,
    const std::string& contentType);

  void OnURLFetchComplete(const net::URLFetcher* source) final;
  void OnURLFetchDownloadProgress(const net::URLFetcher* source,
                                          int64_t current,
                                          int64_t total,
                                          int64_t current_network_bytes) final {
  }

  void OnURLFetchUploadProgress(const net::URLFetcher* source,
                                        int64_t current,
                                        int64_t total) final {
  }

private:
  void runOnThread(const std::string& url, BatClientWebRequest::FetchCallback callback,
    const std::vector<std::string>& headers, const std::string& content,
    const std::string& contentType);

  std::unique_ptr<net::UploadDataStream> CreateUploadStream(const std::string& stream);

  std::list<std::unique_ptr<URL_FETCH_REQUEST>> url_fetchers_;
  std::mutex fetcher_mutex_;
  //std::unique_ptr<net::URLFetcher> url_fetcher_;
};
}

#endif // BAT_CLIENT_WEBREQUEST_H_
