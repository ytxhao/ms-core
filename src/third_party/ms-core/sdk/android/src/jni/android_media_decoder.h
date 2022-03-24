/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#ifndef SDK_ANDROID_SRC_JNI_ANDROIDMEDIADECODER_H_
#define SDK_ANDROID_SRC_JNI_ANDROIDMEDIADECODER_H_

#include "common_video/h264/h264_bitstream_parser.h"
#include "common_video/include/video_frame_buffer_pool.h"
#include "modules/video_coding/include/video_codec_interface.h"
#include "rtc_base/thread.h"
#include "sdk/android/native_api/jni/java_types.h"
#include "sdk/android/src/jni/android_media_codec_common.h"

namespace webrtc {
namespace jni {

class MediaCodecVideoDecoder : public VideoDecoder, public rtc::MessageHandler {
 public:
  explicit MediaCodecVideoDecoder(JNIEnv* jni,
                                  VideoCodecType codecType,
                                  bool use_surface);
  ~MediaCodecVideoDecoder() override;

  int32_t InitDecode(const VideoCodec* codecSettings,
                     int32_t numberOfCores) override;

  int32_t Decode(const EncodedImage& inputImage,
                 bool missingFrames,
                 int64_t renderTimeMs = -1) override;

  int32_t RegisterDecodeCompleteCallback(
      DecodedImageCallback* callback) override;

  int32_t Release() override;

  // rtc::MessageHandler implementation.
  void OnMessage(rtc::Message* msg) override;

  const char* ImplementationName() const override;

 private:
  // CHECK-fail if not running on |codec_thread_|.
  void CheckOnCodecThread();

  int32_t InitDecodeOnCodecThread();
  int32_t ResetDecodeOnCodecThread();
  int32_t ReleaseOnCodecThread();
  int32_t DecodeOnCodecThread(const EncodedImage& inputImage);
  // Deliver any outputs pending in the MediaCodec to our |callback_| and return
  // true on success.
  bool DeliverPendingOutputs(JNIEnv* jni, int dequeue_timeout_us);
  int32_t ProcessHWErrorOnCodecThread();
  void EnableFrameLogOnWarning();
  void ResetVariables();

  // Type of video codec.
  VideoCodecType codecType_;

  bool key_frame_required_;
  bool inited_;
  bool sw_fallback_required_;
  const bool use_surface_;
  VideoCodec codec_;
  VideoFrameBufferPool decoded_frame_pool_;
  DecodedImageCallback* callback_;
  int frames_received_;  // Number of frames received by decoder.
  int frames_decoded_;   // Number of frames decoded by decoder.
  // Number of decoded frames for which log information is displayed.
  int frames_decoded_logged_;
  int64_t start_time_ms_;  // Start time for statistics.
  int current_frames_;  // Number of frames in the current statistics interval.
  int current_bytes_;   // Encoded bytes in the current statistics interval.
  int current_decoding_time_ms_;  // Overall decoding time in the current second
  int current_delay_time_ms_;     // Overall delay time in the current second.
  int32_t max_pending_frames_;    // Maximum number of pending input frames.
  H264BitstreamParser h264_bitstream_parser_;
  std::deque<absl::optional<uint8_t>> pending_frame_qps_;

  // State that is constant for the lifetime of this object once the ctor
  // returns.
  std::unique_ptr<rtc::Thread>
      codec_thread_;  // Thread on which to operate MediaCodec.
  ScopedJavaGlobalRef<jobject> j_media_codec_video_decoder_;

  // Global references; must be deleted in Release().
  std::vector<ScopedJavaGlobalRef<jobject>> input_buffers_;

  std::string uid_;
};


}  // namespace jni
}  // namespace webrtc

#endif  // SDK_ANDROID_SRC_JNI_ANDROIDMEDIADECODER_H_
