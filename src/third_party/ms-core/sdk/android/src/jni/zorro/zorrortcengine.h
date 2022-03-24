#ifndef ZORRO_RTC_ENGINE_H_
#define ZORRO_RTC_ENGINE_H_

#include "zorro/zorro_common.h"

namespace webrtc {
namespace jni {

void GetDeviceInfo(std::string* device_id,
                   std::string* manufacturer,
                   std::string* device_model,
                   std::string* cpu_name,
                   std::string* cpu_abi,
                   int* cpu_cores,
                   int* cpu_max_freq,
                   int* version_code);

int MaybeReuseVideoRenderer(const std::string& uid);
void RemoveVideoRenderer(const std::string& uid);
void ClearVideoRenderer();

} // namespace jni
} // namespace zorro

#endif // ZORRO_RTC_ENGINE_H_
