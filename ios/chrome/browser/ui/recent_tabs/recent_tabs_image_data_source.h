// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef IOS_CHROME_BROWSER_UI_RECENT_TABS_RECENT_TABS_IMAGE_DATA_SOURCE_H_
#define IOS_CHROME_BROWSER_UI_RECENT_TABS_RECENT_TABS_IMAGE_DATA_SOURCE_H_

#import <UIKit/UIKit.h>

class GURL;
@class FaviconAttributes;

// Protocol that the recent tabs UI uses to synchronously and asynchronously
// get images.
@protocol RecentTabsImageDataSource
// Requests the receiver to provide a favicon image for |URL|. A
// FaviconAttributes instance with non-nil properties is synchronously returned
// for immediate use. |completion| is called asynchronously with a
// FaviconAttribues instance if appropriate. For example, a default image may be
// returned synchronously and the actual favicon returned asynchronously. In
// another example, the image returned synchronously may be the actual favicon,
// so there is no need to call the completion block.
- (FaviconAttributes*)faviconForURL:(const GURL&)URL
                         completion:(void (^)(FaviconAttributes*))completion;
@end

#endif  // IOS_CHROME_BROWSER_UI_RECENT_TABS_RECENT_TABS_IMAGE_DATA_SOURCE_H_
