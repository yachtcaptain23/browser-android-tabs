#ifndef BRAVE_REWARDS_NATIVE_WORKER_H_
#define BRAVE_REWARDS_NATIVE_WORKER_H_

#include <jni.h>
#include <memory>
#include <map>
#include "base/android/jni_weak_ref.h"
#include "base/memory/weak_ptr.h"

namespace chrome {
namespace android {

class BraveAdsNativeHelper {
  public:
    static bool IsBraveAdsEnabled(JNIEnv* env, const base::android::JavaParamRef<jobject>& j_profile_android);
    static bool IsLocaleValid(JNIEnv* env, const base::android::JavaParamRef<jobject>& j_profile_android);

};
}
}
