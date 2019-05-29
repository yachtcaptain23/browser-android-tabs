#include "brave_ads_native_helper.h"
#include "base/android/jni_android.h"
#include "base/android/jni_string.h"
#include "chrome/browser/profiles/profile_android.h"
#include "brave/components/brave_ads/browser/ads_service.h"
#include "brave/components/brave_ads/browser/ads_service_factory.h"
#include "chrome/browser/profiles/profile.h"
#include "jni/BraveAdsNativeHelper_jni.h"

using base::android::JavaParamRef;
using base::android::ScopedJavaLocalRef;

namespace chrome {
namespace android {
bool BraveAdsNativeHelper::IsBraveAdsEnabled(JNIEnv* env, const base::android::JavaParamRef<jobject>& j_profile_android) {
//    Profile* profile = ProfileAndroid::FromProfileAndroid(j_profile_android);
//    auto* ads_service_ = brave_ads::AdsServiceFactory::GetForProfile(profile);
    LOG(WARNING) << "albert in IsBraveAdsEnabled!";
    return false;
}

// bool JNI_BraveAds_IsLocaleValid(JNIEnv* env, const base::android::JavaParamRef<jobject>& j_profile_android) {
bool BraveAdsNativeHelper::IsLocaleValid(JNIEnv* env, const base::android::JavaParamRef<jobject>& j_profile_android) {
    LOG(WARNING) << "albert in IsLocaleValid!";
    return true;
}
}
}
