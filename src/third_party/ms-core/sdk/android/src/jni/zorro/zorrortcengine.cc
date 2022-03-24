#include <jni.h>
#include <vector>
#include <unordered_map>

#include "rtc_base/checks.h"
#include "rtc_base/logging.h"
#include "sdk/android/generated_zorro_rtc_engine_jni/DeviceInfo_jni.h"
#include "sdk/android/generated_zorro_rtc_engine_jni/RtcEngineImpl_jni.h"
#include "sdk/android/generated_zorro_rtc_engine_jni/IRtcEngineObserver_jni.h"
#include "sdk/android/generated_zorro_rtc_engine_jni/VideoEncoderConfiguration_jni.h"
#include "sdk/android/generated_zorro_rtc_engine_jni/LiveTranscoding_jni.h"
#include "sdk/android/generated_zorro_rtc_engine_jni/ZorroVideoFrame_jni.h"
#include "sdk/android/src/jni/jni_helpers.h"
#include "sdk/android/native_api/jni/java_types.h"
#include "modules/utility/include/jvm_android.h"
#include "sdk/android/native_api/video/wrapper.h"
#include "json/json.h"

#include "zorro/zorro_common.h"
#include "zorro/call_manager_interface.h"
// #include "zorro_audio_processing.h" // TODOO

#if 0
#include "webrtc/api/java/jni/classreferenceholder.h"
#include "webrtc/voice_engine/include/voe_base.h"
#include "webrtc/api/java/jni/androidvideocapturer_jni.h"
#include "webrtc/modules/video_capture/video_capture_internal.h"
#include "webrtc/modules/video_render/video_render_internal.h"
#endif

extern zorro::CallManagerInterface* GetCallManager();

namespace webrtc {
namespace jni {

namespace {

zorro::LiveTranscodingConfig JavaToNativeLiveTranscodingConfig(
    JNIEnv* jni,
    const JavaRef<jobject>& object) {
  zorro::LiveTranscodingConfig config;
  config.audioChannels = Java_LiveTranscoding_getAudioChannel(jni, object);
  config.audioBitrateKbps = Java_LiveTranscoding_getAudioBitrateKbps(jni, object);
  config.audioSamplerate = Java_LiveTranscoding_getAudioSampleRate(jni, object);
  config.videoWidth = Java_LiveTranscoding_getWidth(jni, object);
  config.videoHeight = Java_LiveTranscoding_getHeight(jni, object);
  config.videoFps = Java_LiveTranscoding_getVideoFramerate(jni, object);
  config.videoBitrateKbps = Java_LiveTranscoding_getVideoBitrateKbps(jni, object);
  config.videoGop = Java_LiveTranscoding_getVideoGop(jni, object);
  ScopedJavaLocalRef<jstring> bgColor = Java_LiveTranscoding_getBackgroundColor(jni, object);
  if (!bgColor.is_null()) {
    config.videoBackgroundColor = JavaToNativeString(jni, bgColor);
  }
  ScopedJavaLocalRef<jstring> bgUrl = Java_LiveTranscoding_getVideoBackgroundUrl(jni, object);
  if (!bgUrl.is_null()) {
    config.videoBackgroundUrl = JavaToNativeString(jni, bgUrl);
  }
  ScopedJavaLocalRef<jobject> users = Java_LiveTranscoding_getUsers(jni, object);
  if (!users.is_null()) {
    for (const JavaRef<jobject>& user : Iterable(jni, users)) {
      ScopedJavaLocalRef<jstring> uid = Java_TranscodingUser_getUid(jni, user);
      jint is_video = Java_TranscodingUser_getIsVideo(jni, user);
      jint left = Java_TranscodingUser_getLeft(jni, user);
      jint top = Java_TranscodingUser_getTop(jni, user);
      jint width = Java_TranscodingUser_getWidth(jni, user);
      jint height = Java_TranscodingUser_getHeight(jni, user);
      jint zorder = Java_TranscodingUser_getZorder(jni, user);
      config.users.emplace_back(JavaToNativeString(jni, uid),
                                is_video, left, top, width, height, zorder);
    }
  }
  return config;
}

ScopedJavaLocalRef<jobject> CreateJavaAudioVolumeInfo(
    JNIEnv* env,
    const std::string& uid,
    int level) {
  return Java_AudioVolumeInfo_Constructor(
      env, NativeToJavaString(env, uid), level);
}

ScopedJavaLocalRef<jobject> NativeToJavaAudioVolumeInfo(
    JNIEnv* env,
    const zorro::AudioVolumeInfo& info) {
  return CreateJavaAudioVolumeInfo(env, info.uid, info.level);
}

ScopedJavaLocalRef<jobjectArray> NativeToJavaAudioVolumeInfoArray(
    JNIEnv* jni,
    const std::vector<zorro::AudioVolumeInfo>& info) {
  jclass clazz = media_ushow_zorro_IRtcEngineObserver_00024AudioVolumeInfo_clazz(jni);
  return NativeToJavaObjectArray(jni, info, clazz,
                                 &NativeToJavaAudioVolumeInfo);
}

ScopedJavaLocalRef<jobject> CreateJavaRemoteAudioTransportStats(
    JNIEnv* env,
    const std::string& uid,
    int delay_ms,
    int lost_percent,
    int received_bitrate_kbps) {
  return Java_RemoteAudioTransportStats_Constructor(
      env, NativeToJavaString(env, uid), delay_ms, lost_percent, received_bitrate_kbps);
}

ScopedJavaLocalRef<jobject> NativeToJavaRemoteAudioTransportStats(
    JNIEnv* env,
    const zorro::RemoteAudioTransportStats& s) {
  return CreateJavaRemoteAudioTransportStats(env, s.uid, s.delay_ms, s.lost_percent, s.received_bitrate_kbps);
}

ScopedJavaLocalRef<jobjectArray> NativeToJavaRemoteAudioTransportStatsArray(
    JNIEnv* jni,
    const std::vector<zorro::RemoteAudioTransportStats>& stats) {
  jclass clazz = media_ushow_zorro_IRtcEngineObserver_00024RemoteAudioTransportStats_clazz(jni);
  return NativeToJavaObjectArray(jni, stats, clazz,
                                 &NativeToJavaRemoteAudioTransportStats);
}

ScopedJavaLocalRef<jobject> CreateJavaRemoteVideoTransportStats(
    JNIEnv* env,
    const std::string& uid,
    int delay_ms,
    int lost_percent,
    int received_bitrate_kbps) {
  return Java_RemoteVideoTransportStats_Constructor(
      env, NativeToJavaString(env, uid), delay_ms, lost_percent, received_bitrate_kbps);
}

ScopedJavaLocalRef<jobject> NativeToJavaRemoteVideoTransportStats(
    JNIEnv* env,
    const zorro::RemoteVideoTransportStats& s) {
  return CreateJavaRemoteVideoTransportStats(env, s.uid, s.delay_ms, s.lost_percent, s.received_bitrate_kbps);
}

ScopedJavaLocalRef<jobjectArray> NativeToJavaRemoteVideoTransportStatsArray(
    JNIEnv* jni,
    const std::vector<zorro::RemoteVideoTransportStats>& stats) {
  jclass clazz = media_ushow_zorro_IRtcEngineObserver_00024RemoteVideoTransportStats_clazz(jni);
  return NativeToJavaObjectArray(jni, stats, clazz,
                                 &NativeToJavaRemoteVideoTransportStats);
}

ScopedJavaLocalRef<jobject> CreateJavaRemoteAudioStats(
    JNIEnv* env,
    const std::string& uid,
    int network_transport_delay_ms,
    int jitter_buffer_delay_ms,
    int loss_rate,
    int e2e_delay_ms,
    int interruption_rate,
    int interruption_count,
    int64_t total_interruption_duration_ms) {
  return Java_RemoteAudioStats_Constructor(
      env, NativeToJavaString(env, uid), network_transport_delay_ms,
      jitter_buffer_delay_ms, loss_rate, e2e_delay_ms, interruption_rate,
      interruption_count, total_interruption_duration_ms);
}

ScopedJavaLocalRef<jobject> NativeToJavaRemoteAudioStats(
    JNIEnv* env,
    const zorro::RemoteAudioStats& s) {
  int interruption_count = s.interruption_count;
  int total_duration_ms = s.total_time_ms;
  int total_interruption_duration_ms = s.total_interruption_duration_ms;
  int interruption_rate = total_duration_ms > 0
      ? (100 * total_interruption_duration_ms / total_duration_ms) : 0;

  return CreateJavaRemoteAudioStats(
      env, s.uid, s.network_transport_delay_ms, s.jitter_buffer_delay_ms,
      s.loss_rate, s.e2e_delay_ms, interruption_rate, interruption_count,
      total_interruption_duration_ms);
}

ScopedJavaLocalRef<jobjectArray> NativeToJavaRemoteAudioStatsArray(
    JNIEnv* jni,
    const std::vector<zorro::RemoteAudioStats>& stats) {
  jclass clazz = media_ushow_zorro_IRtcEngineObserver_00024RemoteAudioStats_clazz(jni);
  return NativeToJavaObjectArray(jni, stats, clazz,
                                 &NativeToJavaRemoteAudioStats);
}

ScopedJavaLocalRef<jobject> CreateJavaRemoteVideoStats(
    JNIEnv* env,
    const std::string& uid,
    int width,
    int height,
    int received_bitrate_kbps,
    int received_frame_rate,
    int frozen_counts,
    int frozen_rate,
    int64_t total_frozen_time_ms,
    int e2e_delay_ms) {
  return Java_RemoteVideoStats_Constructor(
      env, NativeToJavaString(env, uid), width, height, received_bitrate_kbps, received_frame_rate,
      frozen_counts, frozen_rate, total_frozen_time_ms, e2e_delay_ms);
}

ScopedJavaLocalRef<jobject> NativeToJavaRemoteVideoStats(
    JNIEnv* env,
    const zorro::RemoteVideoStats& s) {
  return CreateJavaRemoteVideoStats(env, s.uid, s.width, s.height, s.received_bitrate_kbps, s.received_frame_rate,
      s.frozen_counts, s.frozen_rate, s.total_frozen_time_ms, s.e2e_delay_ms);
}

ScopedJavaLocalRef<jobjectArray> NativeToJavaRemoteVideoStatsArray(
    JNIEnv* jni,
    const std::vector<zorro::RemoteVideoStats>& stats) {
  jclass clazz = media_ushow_zorro_IRtcEngineObserver_00024RemoteVideoStats_clazz(jni);
  return NativeToJavaObjectArray(jni, stats, clazz,
                                 &NativeToJavaRemoteVideoStats);
}

ScopedJavaLocalRef<jobject> CreateJavaLocalAudioStats(
    JNIEnv* env,
    int delay_ms,
    int sent_bitrate_kbps,
    int lost_percent) {
  return Java_LocalAudioStats_Constructor(
      env, delay_ms, sent_bitrate_kbps, lost_percent);
}

ScopedJavaLocalRef<jobject> NativeToJavaLocalAudioStats(
    JNIEnv* env,
    const zorro::LocalAudioStats& s) {
  return CreateJavaLocalAudioStats(env, s.delay_ms, s.sent_bitrate_kbps, s.lost_percent);
}

ScopedJavaLocalRef<jobject> CreateJavaLocalVideoStats(
    JNIEnv* env,
    int width,
    int height,
    int sent_bitrate_kbps,
    int sent_frame_rate,
    int lost_percent) {
  return Java_LocalVideoStats_Constructor(
      env, width, height, sent_bitrate_kbps, sent_frame_rate, lost_percent);
}

ScopedJavaLocalRef<jobject> NativeToJavaLocalVideoStats(
    JNIEnv* env,
    const zorro::LocalVideoStats& s) {
  return CreateJavaLocalVideoStats(env, s.width, s.height, s.sent_bitrate_kbps, s.sent_frame_rate, s.lost_percent);
}

ScopedJavaLocalRef<jobject> CreateJavaRtcStats(JNIEnv* env, const zorro::RtcStats& s) {
  ScopedJavaLocalRef<jobjectArray> remote_audio_transport_stats = NULL;
  ScopedJavaLocalRef<jobjectArray> remote_video_transport_stats = NULL;
  ScopedJavaLocalRef<jobjectArray> remote_audio_stats = NULL;
  ScopedJavaLocalRef<jobjectArray> remote_video_stats = NULL;
  ScopedJavaLocalRef<jobject> local_audio_stats = NULL;
  ScopedJavaLocalRef<jobject> local_video_stats = NULL;
  if (s.remote_audio_transport_stats.size() > 0) {
    remote_audio_transport_stats = NativeToJavaRemoteAudioTransportStatsArray(env, s.remote_audio_transport_stats);
  }
  if (s.remote_video_transport_stats.size() > 0) {
    remote_video_transport_stats = NativeToJavaRemoteVideoTransportStatsArray(env, s.remote_video_transport_stats);
  }
  if (s.remote_audio_stats.size() > 0) {
    remote_audio_stats = NativeToJavaRemoteAudioStatsArray(env, s.remote_audio_stats);
  }
  if (s.remote_video_stats.size() > 0) {
    remote_video_stats = NativeToJavaRemoteVideoStatsArray(env, s.remote_video_stats);
  }
  if (s.local_audio_stats.sent_bitrate_kbps >= 0) {
    local_audio_stats = NativeToJavaLocalAudioStats(env, s.local_audio_stats);
  }
  if (s.local_video_stats.sent_bitrate_kbps >= 0) {
    local_video_stats = NativeToJavaLocalVideoStats(env, s.local_video_stats);
  }
  base::android::ScopedJavaLocalRef<jobject> ret = Java_RtcStats_Constructor(env, 
                                                                             remote_audio_transport_stats, 
                                                                             remote_video_transport_stats,
                                                                             remote_audio_stats, 
                                                                             remote_video_stats, 
                                                                             local_audio_stats, 
                                                                             local_video_stats);
  return ret;
}

ScopedJavaLocalRef<jobject> NativeToJavaRtcStats(
    JNIEnv* env,
    const zorro::RtcStats& s) {
  return CreateJavaRtcStats(env, s);
}

bool g_zorro_rtc_engine_initialized = false;

std::string g_local_uid;
webrtc::Mutex g_uid_video_renderer_map_lock;
std::unordered_map<std::string, jobject> g_uid_video_renderer_map;

} // namespace


class RtcEngineObserverJni : public zorro::RtcEngineObserverInterface {
 public:
  RtcEngineObserverJni(
    JNIEnv* jni,
    const JavaRef<jobject>& j_observer)
    : j_observer_global_(jni, j_observer) {}

  ~RtcEngineObserverJni() override = default;

  void OnJoinChannelSuccess(const std::string& channel, const std::string& uid,
      int elapsed) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onJoinChannelSuccess(
        env, j_observer_global_,
        NativeToJavaString(env, channel),
        NativeToJavaString(env, uid),
        elapsed);
  }

  void OnLeaveChannel() override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onLeaveChannel(
        env, j_observer_global_);
  }

  void OnStartLivePkSuccess(const std::string& channel, const std::string& uid,
      int elapsed) override {
      JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onStartLivePkSuccess(
        env, j_observer_global_,
        NativeToJavaString(env, channel),
        NativeToJavaString(env, uid),
        elapsed);
  }

  void OnUserJoined(const std::string& uid, int elapsed,
      const std::string& extra_info) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onUserJoined(
        env, j_observer_global_,
        NativeToJavaString(env, uid),
        elapsed,
        NativeToJavaString(env, extra_info));
  }

  void OnUserOffline(const std::string& uid) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onUserOffline(
        env, j_observer_global_,
        NativeToJavaString(env, uid));
  }

  void OnFirstRemoteAudioFrame(const std::string& uid) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onFirstRemoteAudioFrame(
        env, j_observer_global_,
        NativeToJavaString(env, uid));
  }

  void OnFirstRemoteVideoFrame(const std::string& uid) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onFirstRemoteVideoFrame(
        env, j_observer_global_,
        NativeToJavaString(env, uid));
  }

  void OnFirstRemoteVideoFrameRendered(const std::string& uid) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onFirstRemoteVideoFrameRendered(
        env, j_observer_global_,
        NativeToJavaString(env, uid));
  }

  void OnAudioVolumeIndication(const std::vector<zorro::AudioVolumeInfo>& info) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onAudioVolumeIndication(
        env, j_observer_global_,
        NativeToJavaAudioVolumeInfoArray(env, info));
  }

  void OnError(zorro::ErrorCode code, const std::string& text) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    int err;
    switch (code) {
      case zorro::kKickedByRemote:
        err = 12; // see Constants.java
        break;
      case zorro::kJoinChannelRejected:
        err = 10;
        break;
      case zorro::kStartLivePkFailure:
        err = 14;
        break;
      case zorro::kLivePkError:
        err = 15;
        break;
      default:
        err = -1;
        break;
    }
    Java_IRtcEngineObserver_onError(
        env, j_observer_global_,
        err,
        NativeToJavaString(env, text));
  }

  void OnRtcStats(const zorro::RtcStats& stats) override {
      JNIEnv* env = AttachCurrentThreadIfNeeded();
      Java_IRtcEngineObserver_onRtcStats(
          env, j_observer_global_,
          NativeToJavaRtcStats(env, stats));
  }
    
  void OnVideoKeyFrameRequest() override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onVideoKeyFrameRequest(
        env, j_observer_global_);
  }
    
  void OnVideoEncodingRateSet(int bitrate_kbps, int framerate) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onVideoEncodingRateSet(
        env, j_observer_global_,
        bitrate_kbps, framerate);
  }
  
  void OnVideoSEIData(int type, const std::string &payload) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    ScopedJavaLocalRef<jbyteArray> bytes =
        ScopedJavaLocalRef<jbyteArray>(env, env->NewByteArray(payload.size()));
    env->SetByteArrayRegion(bytes.obj(), 0, payload.size(), (const jbyte *)payload.data());
    Java_IRtcEngineObserver_onVideoSEIData(env, j_observer_global_, type, bytes);
  }

  void OnMediaSideInfo(const std::string &uid, uint8_t *info, int size) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    ScopedJavaLocalRef<jbyteArray> bytes =
        ScopedJavaLocalRef<jbyteArray>(env, env->NewByteArray(size));
    env->SetByteArrayRegion(bytes.obj(), 0, size, (const jbyte *)info);
    Java_IRtcEngineObserver_onMediaSideInfo(env, j_observer_global_, NativeToJavaString(env, uid), bytes);
  }

  void OnConnectionStateChanged(int state, int reason) override {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    Java_IRtcEngineObserver_onConnectionStateChanged(env, j_observer_global_, state, reason);
  }

 private:
   const ScopedJavaGlobalRef<jobject> j_observer_global_;
};

static base::android::ScopedJavaLocalRef<jstring> JNI_RtcEngineImpl_GetSdkVersion(JNIEnv* env) {
  std::string version = GetCallManager()->GetSDKVersion();
  return webrtc::NativeToJavaString(env, version);
}

static jint JNI_RtcEngineImpl_Init(JNIEnv* env, const base::android::JavaParamRef<jobject>& jcaller,
    const base::android::JavaParamRef<jobject>& context,
    const base::android::JavaParamRef<jobject>& observer,
    const base::android::JavaParamRef<jstring>& appId,
    const base::android::JavaParamRef<jstring>& countryCode,
    const base::android::JavaParamRef<jstring>& logPath) {
  std::string app_id_str = appId.obj() ? webrtc::JavaToNativeString(env, appId) : "";
  if (!g_zorro_rtc_engine_initialized) {
    std::string log_path_str = logPath.obj() ? webrtc::JavaToNativeString(env, logPath) : "";
    JVM::Initialize(GetJVM(), context.obj());
    GetCallManager()->Init(log_path_str, app_id_str);
    // zorro::ZorroAudioProcessing::Startup(env, context.obj()); // TODOO
    g_zorro_rtc_engine_initialized = true;
  }

  std::string country_code_str = countryCode.obj() ? webrtc::JavaToNativeString(env, countryCode) : "";
  GetCallManager()->SetRtcEngineConfig(new RtcEngineObserverJni(env, observer), app_id_str, country_code_str);

  return 0;
}

static jint JNI_RtcEngineImpl_SetObserver(JNIEnv* env, const base::android::JavaParamRef<jobject>&
    jcaller,
    const base::android::JavaParamRef<jobject>& observer) {
  if (observer.obj() != nullptr) {
    RtcEngineObserverJni* o = new RtcEngineObserverJni(env, observer);
    GetCallManager()->SetRtcEngineObserver(o);
  } else {
    GetCallManager()->SetRtcEngineObserver(nullptr);
  }
  return 0;
}

static jint JNI_RtcEngineImpl_SetParameters(JNIEnv* env, const base::android::JavaParamRef<jobject>&
    jcaller,
    const base::android::JavaParamRef<jstring>& parameters) {
  std::string parameters_str = parameters.obj() ? webrtc::JavaToNativeString(env, parameters) : "";
  if (parameters_str.empty()) return 0;

  return GetCallManager()->SetParameters(parameters_str);
}

static jint JNI_RtcEngineImpl_SetAudioSource(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    jboolean external,
    jint sampleRate,
    jint channels) {
  return GetCallManager()->SetAudioSource(external, sampleRate, channels);
}

static jint JNI_RtcEngineImpl_SetVideoSource(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    jboolean external,
    jint width,
    jint height) {
  return GetCallManager()->SetVideoSource(external, width, height);
}

static jint JNI_RtcEngineImpl_SetChannelProfile(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    jint profile) {
  if (profile > zorro::kProfileInvalid && profile < zorro::kProfileMax) {
    return GetCallManager()->SetChannelProfile((zorro::Profile)profile);
  } else {
    return -1; 
  }
}

static jint JNI_RtcEngineImpl_EnableVideo(JNIEnv* env, const base::android::JavaParamRef<jobject>&
    jcaller) {
  return GetCallManager()->EnableVideo(true);
}

static jint JNI_RtcEngineImpl_DisableVideo(JNIEnv* env, const base::android::JavaParamRef<jobject>&
    jcaller) {
  return GetCallManager()->EnableVideo(false);
}

static jint JNI_RtcEngineImpl_SetClientRole(JNIEnv* env, const base::android::JavaParamRef<jobject>&
    jcaller,
    jint role) {
  return GetCallManager()->SetClientRole((zorro::Role)role);
}

static jint JNI_RtcEngineImpl_SetVideoEncoderConfiguration(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    const base::android::JavaParamRef<jobject>& config) {
  jint bitrateKbps = Java_VideoEncoderConfiguration_getBitrateKbps(env, config);
  jint minBitrateKbps = Java_VideoEncoderConfiguration_getMinBitrateKbps(env, config);
  jint frameRate = Java_VideoEncoderConfiguration_getFrameRate(env, config);
  jint minFrameRate = Java_VideoEncoderConfiguration_getMinFrameRate(env, config);

  return GetCallManager()->SetVideoEncoderConfiguration(bitrateKbps,
                                                        minBitrateKbps,
                                                        frameRate,
                                                        minFrameRate);
}

static jint JNI_RtcEngineImpl_AddPublishStreamUrl(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    const base::android::JavaParamRef<jstring>& url) {
  std::string url_str = url.obj() ? webrtc::JavaToNativeString(env, url) : "";
  return GetCallManager()->AddPublishStreamUrl(url_str);
}

static jint JNI_RtcEngineImpl_RemovePublishStreamUrl(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    const base::android::JavaParamRef<jstring>& url) {
  std::string url_str = url.obj() ? webrtc::JavaToNativeString(env, url) : "";
  return GetCallManager()->RemovePublishStreamUrl(url_str);
}

static jint JNI_RtcEngineImpl_SetLiveTranscoding(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    const base::android::JavaParamRef<jobject>& transcoding) {
  if (!transcoding.is_null()) {
    return GetCallManager()->SetLiveTranscodingConfig(JavaToNativeLiveTranscodingConfig(env, transcoding));
  } else {
    return -1; 
  }
}

static jint JNI_RtcEngineImpl_EnableAudioVolumeIndication(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    jint intervalMs) {
  return GetCallManager()->EnableAudioVolumeIndication(intervalMs);
}

static jint JNI_RtcEngineImpl_JoinChannel(JNIEnv* env, const base::android::JavaParamRef<jobject>&
    jcaller,
    const base::android::JavaParamRef<jstring>& token,
    const base::android::JavaParamRef<jstring>& channelName,
    const base::android::JavaParamRef<jstring>& extraInfo,
    const base::android::JavaParamRef<jstring>& uid) {
  std::string token_str = token.obj() ? webrtc::JavaToNativeString(env, token) : "";
  std::string channel_name_str = channelName.obj() ? webrtc::JavaToNativeString(env, channelName) : "";
  std::string extra_info_str = extraInfo.obj() ? webrtc::JavaToNativeString(env, extraInfo) : "";
  std::string uid_str = uid.obj() ? webrtc::JavaToNativeString(env, uid) : "";

  if (channel_name_str.empty() || uid_str.empty()) {
    return -1;
  }

  g_local_uid = uid_str;

  return GetCallManager()->JoinChannel(token_str, channel_name_str, extra_info_str, uid_str);
}

static jint JNI_RtcEngineImpl_LeaveChannel(JNIEnv* env, const base::android::JavaParamRef<jobject>&
    jcaller) {
  return GetCallManager()->LeaveChannel();
}

static jint JNI_RtcEngineImpl_StartLivePk(JNIEnv* env, const base::android::JavaParamRef<jobject>&
    jcaller,
    const base::android::JavaParamRef<jstring>& channelName,
    const base::android::JavaParamRef<jstring>& remote_uid) {
  std::string channel_name_str = channelName.obj() ? webrtc::JavaToNativeString(env, channelName) : "";
  std::string uid_str = remote_uid.obj() ? webrtc::JavaToNativeString(env, remote_uid) : "";

  if (channel_name_str.empty() || uid_str.empty()) {
    return -1;
  }

  return GetCallManager()->StartLivePk(channel_name_str, uid_str);
}

static jint JNI_RtcEngineImpl_StopLivePk(JNIEnv* env, const base::android::JavaParamRef<jobject>&
    jcaller) {
  return GetCallManager()->StopLivePk();
}

static jint JNI_RtcEngineImpl_MuteLocalAudioStream(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    jboolean mute) {
  return GetCallManager()->MuteLocalAudioStream(mute);
}

static jint JNI_RtcEngineImpl_MuteRemoteAudioStream(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    const base::android::JavaParamRef<jstring>& uid,
    jboolean mute) {
  std::string user_id = webrtc::JavaToNativeString(env, uid);
  RTC_DCHECK(!user_id.empty());
  return GetCallManager()->MuteRemoteAudioStream(user_id, mute);
}

static jint JNI_RtcEngineImpl_SetAudioPlayVolume(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    jint volume) {
  return GetCallManager()->SetAudioPlayVolume(volume);
}

static jint JNI_RtcEngineImpl_SendMediaSideInfo(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    const base::android::JavaParamRef<jbyteArray>& info) {
    int result = -1;
    const size_t len = env->GetArrayLength(info.obj());
    if (len > 0) {
        jbyte *data = env->GetByteArrayElements(info.obj(), nullptr);
        result = GetCallManager()->SendMediaSideInfo((uint8_t *)data, len);
        env->ReleaseByteArrayElements(info.obj(), data, JNI_ABORT);
    }
    return result;
}

static jint JNI_RtcEngineImpl_KickOut(JNIEnv* env, const base::android::JavaParamRef<jobject>&
    jcaller,
    const base::android::JavaParamRef<jstring>& uid) {
  std::string user_id = webrtc::JavaToNativeString(env, uid);
  RTC_DCHECK(!user_id.empty());
  return GetCallManager()->KickOut(user_id);
}

static base::android::ScopedJavaLocalRef<jobjectArray> JNI_RtcEngineImpl_GetBroadcasters(JNIEnv*
    env, const base::android::JavaParamRef<jobject>& jcaller) {
  std::vector<std::string> uids;
  GetCallManager()->GetBroadcasters(&uids);
  return NativeToJavaStringArray(env, uids);
}

static jint JNI_RtcEngineImpl_PushExternalVideoFrame(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    const base::android::JavaParamRef<jobject>& frame) {
  base::android::ScopedJavaLocalRef<jobject> byte_buffer =
      Java_ZorroVideoFrame_getByteBuffer(env, frame);
  jclass cls = env->GetObjectClass(byte_buffer.obj());
  jmethodID mid_limit = env->GetMethodID(cls, "limit", "()I");
  jmethodID mid_position = env->GetMethodID(cls, "position", "()I");
  jint limit = env->CallIntMethod(byte_buffer.obj(), mid_limit);
  jint position = env->CallIntMethod(byte_buffer.obj(), mid_position);
  uint8_t* direct_buffer_address =
      static_cast<uint8_t*>(env->GetDirectBufferAddress(byte_buffer.obj()));
  if (direct_buffer_address != nullptr) {
    direct_buffer_address += position;
    size_t direct_buffer_capacity_in_bytes = limit - position;
      // static_cast<size_t>(env->GetDirectBufferCapacity(byte_buffer.obj()));
    zorro::VideoFrameType zorro_frame_type = zorro::kVideoFrameI;
    int frame_type = Java_ZorroVideoFrame_getFrameType(env, frame);
    if (frame_type == 0) {
      zorro_frame_type = zorro::kVideoFrameI;
    } else if (frame_type == 1) {
      zorro_frame_type = zorro::kVideoFrameP;
    }
    int64_t timestamp = Java_ZorroVideoFrame_getTimestamp(env, frame);

    zorro::VideoEnc zorro_video_enc = zorro::kVideoEncUnknown;
    int encoder_type = Java_ZorroVideoFrame_getEncoderType(env, frame);
    if (encoder_type == 0) {
      zorro_video_enc = zorro::kVideoEncHardware;
    } else if (encoder_type == 1) {
      zorro_video_enc = zorro::kVideoEncOpenH264;
    }
    return GetCallManager()->SendVideoFrame(direct_buffer_address,
                                            direct_buffer_capacity_in_bytes,
                                            zorro_frame_type,
                                            timestamp,
                                            zorro_video_enc);
  } else {
    RTC_LOG(LS_INFO) << "Error: Not a direct buffer";
    RTC_DCHECK(0); 
    return false;
  }
}

static void AddVideoRenderer(JNIEnv* env, const std::string& uid,
    const base::android::JavaParamRef<jobject>& renderer) {
  webrtc::MutexLock lock(&g_uid_video_renderer_map_lock);
  auto iter = g_uid_video_renderer_map.find(uid);
  if (iter != g_uid_video_renderer_map.end()) {
    jclass cls = env->GetObjectClass(iter->second);
    jmethodID mid_release = env->GetMethodID(cls, "release", "()V");
    env->CallVoidMethod(iter->second, mid_release);
    env->DeleteGlobalRef(iter->second);
    g_uid_video_renderer_map.erase(iter);
  }
  
  g_uid_video_renderer_map.emplace(
      std::make_pair(uid, env->NewGlobalRef(renderer.obj())));
}

static jint JNI_RtcEngineImpl_SetLocalVideoRenderer(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    const base::android::JavaParamRef<jobject>& renderer) {
  std::unique_ptr<rtc::VideoSinkInterface<webrtc::VideoFrame>> sink =
      webrtc::JavaToNativeVideoSink(env, renderer.obj());
  int ret = GetCallManager()->SetLocalVideoRenderer(sink.get(), true);
  sink.release();

  AddVideoRenderer(env, g_local_uid, renderer);

  return ret;
}

static jint JNI_RtcEngineImpl_SetRemoteVideoRenderer(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller,
    const base::android::JavaParamRef<jobject>& renderer,
    const base::android::JavaParamRef<jstring>& uid) {
  std::string user_id = webrtc::JavaToNativeString(env, uid);
  RTC_DCHECK(!user_id.empty());
  std::unique_ptr<rtc::VideoSinkInterface<webrtc::VideoFrame>> sink =
      webrtc::JavaToNativeVideoSink(env, renderer.obj());
  int ret = GetCallManager()->SetRemoteVideoRenderer(user_id, sink.get(), true);
  sink.release();

  AddVideoRenderer(env, user_id, renderer);
  
  return ret;
}

int MaybeReuseVideoRenderer(const std::string& uid) {
  int ret = -1;
  webrtc::MutexLock lock(&g_uid_video_renderer_map_lock);
  auto iter = g_uid_video_renderer_map.find(uid);
  if (iter != g_uid_video_renderer_map.end()) {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    std::unique_ptr<rtc::VideoSinkInterface<webrtc::VideoFrame>> sink =
        webrtc::JavaToNativeVideoSink(env, iter->second);
    if (uid != g_local_uid) {
      ret = GetCallManager()->SetRemoteVideoRenderer(uid, sink.get(), false);
    } else {
      ret = GetCallManager()->SetLocalVideoRenderer(sink.get(), false);
    }
    sink.release();

  }

  return ret;
}

void RemoveVideoRenderer(const std::string& uid) {
  webrtc::MutexLock lock(&g_uid_video_renderer_map_lock);
  auto iter = g_uid_video_renderer_map.find(uid);
  if (iter != g_uid_video_renderer_map.end()) {
    JNIEnv* env = AttachCurrentThreadIfNeeded();
    jclass cls = env->GetObjectClass(iter->second);
    jmethodID mid_limit = env->GetMethodID(cls, "release", "()V");
    env->CallVoidMethod(iter->second, mid_limit);
    env->DeleteLocalRef(cls);
    env->DeleteGlobalRef(iter->second);
    g_uid_video_renderer_map.erase(iter);
  }
}

void ClearVideoRenderer() {
  webrtc::MutexLock lock(&g_uid_video_renderer_map_lock);
  JNIEnv* env = AttachCurrentThreadIfNeeded();
  for (auto &it : g_uid_video_renderer_map) {
    jclass cls = env->GetObjectClass(it.second);
    jmethodID mid_release = env->GetMethodID(cls, "release", "()V");
    env->CallVoidMethod(it.second, mid_release);
    env->DeleteLocalRef(cls);
    env->DeleteGlobalRef(it.second);
  }
  g_uid_video_renderer_map.clear();
}

void GetDeviceInfo(std::string* device_id,
                   std::string* manufacturer,
                   std::string* device_model,
                   std::string* cpu_name,
                   std::string* cpu_abi,
                   int* cpu_cores,
                   int* cpu_max_freq,
                   int* version_code) {
  JNIEnv* env = AttachCurrentThreadIfNeeded();

  // base::android::ScopedJavaLocalRef<jstring> j_device_id = Java_DeviceInfo_getDeviceId(env);
  // *device_id = j_device_id.obj() ? webrtc::JavaToNativeString(env, j_device_id) : "";
  base::android::ScopedJavaLocalRef<jstring> j_manufacturer = Java_DeviceInfo_getManufacturer(env);
  *manufacturer = j_manufacturer.obj() ? webrtc::JavaToNativeString(env, j_manufacturer) : "unknown";

  base::android::ScopedJavaLocalRef<jstring> j_device_model = Java_DeviceInfo_getDeviceModel(env);
  *device_model = j_device_model.obj() ? webrtc::JavaToNativeString(env, j_device_model) : "unknown";

  base::android::ScopedJavaLocalRef<jstring> j_cpu_name = Java_DeviceInfo_getCpuName(env);
  *cpu_name = j_cpu_name.obj() ? webrtc::JavaToNativeString(env, j_cpu_name) : "";

  base::android::ScopedJavaLocalRef<jstring> j_cpu_abi = Java_DeviceInfo_getCpuABI(env);
  *cpu_abi = j_cpu_abi.obj() ? webrtc::JavaToNativeString(env, j_cpu_abi) : "";

  *cpu_cores = Java_DeviceInfo_getNumberOfCPUCores(env);
  *cpu_max_freq = Java_DeviceInfo_getCPUMaxFreqHz(env);
  *version_code = Java_DeviceInfo_getSdkVersionCode(env);
}


} // namespace jni
} // namespace zorro

