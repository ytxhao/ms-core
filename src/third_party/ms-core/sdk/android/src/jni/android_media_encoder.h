/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#ifndef SDK_ANDROID_SRC_JNI_ANDROIDMEDIAENCODER_H_
#define SDK_ANDROID_SRC_JNI_ANDROIDMEDIAENCODER_H_

#include "api/video_codecs/sdp_video_format.h"
#include "common_video/h264/h264_bitstream_parser.h"
#include "media/base/h264_profile_level_id.h"
#include "modules/video_coding/include/video_codec_interface.h"
#include "rtc_base/thread.h"
#include "rtc_base/weak_ptr.h"
#include "sdk/android/native_api/jni/java_types.h"
#include "sdk/android/src/jni/android_media_codec_common.h"
#include "third_party/libyuv/include/libyuv/video_common.h"

namespace webrtc {
namespace jni {

// MediaCodecVideoEncoder is a VideoEncoder implementation that uses
// Android's MediaCodec SDK API behind the scenes to implement (hopefully)
// HW-backed video encode.  This C++ class is implemented as a very thin shim,
// delegating all of the interesting work to org.webrtc.MediaCodecVideoEncoder.
// MediaCodecVideoEncoder must be operated on a single task queue, currently
// this is the encoder queue from ViE encoder.
class MediaCodecVideoEncoder : public VideoEncoder {
 public:
  ~MediaCodecVideoEncoder() override;
  MediaCodecVideoEncoder(JNIEnv* jni,
                         const SdpVideoFormat& format,
                         bool has_egl_context);

  // VideoEncoder implementation.
  int32_t InitEncode(const VideoCodec* codec_settings,
                     const Settings& settings) override;
  int32_t Encode(const VideoFrame& input_image,
                 const std::vector<VideoFrameType>* frame_types) override;
  int32_t RegisterEncodeCompleteCallback(
      EncodedImageCallback* callback) override;
  int32_t Release() override;
  void SetRates(const RateControlParameters& parameters) override;
  EncoderInfo GetEncoderInfo() const override;

  // Fills the input buffer with data from the buffers passed as parameters.
  bool FillInputBuffer(JNIEnv* jni,
                       int input_buffer_index,
                       uint8_t const* buffer_y,
                       int stride_y,
                       uint8_t const* buffer_u,
                       int stride_u,
                       uint8_t const* buffer_v,
                       int stride_v);

 private:
  class EncodeTask : public QueuedTask {
   public:
    explicit EncodeTask(rtc::WeakPtr<MediaCodecVideoEncoder> encoder);
    bool Run() override;

   private:
    rtc::WeakPtr<MediaCodecVideoEncoder> encoder_;
  };

  // ResetCodec() calls Release() and InitEncodeInternal() in an attempt to
  // restore the codec to an operable state. Necessary after all manner of
  // OMX-layer errors. Returns true if the codec was reset successfully.
  bool ResetCodec();

  // Fallback to a software encoder if one is supported else try to reset the
  // encoder. Called with |reset_if_fallback_unavailable| equal to false from
  // init/release encoder so that we don't go into infinite recursion.
  // Returns true if the codec was reset successfully.
  bool ProcessHWError(bool reset_if_fallback_unavailable);

  // Calls ProcessHWError(true). Returns WEBRTC_VIDEO_CODEC_FALLBACK_SOFTWARE if
  // sw_fallback_required_ was set or WEBRTC_VIDEO_CODEC_ERROR otherwise.
  int32_t ProcessHWErrorOnEncode();

  // If width==0 then this is assumed to be a re-initialization and the
  // previously-current values are reused instead of the passed parameters
  // (makes it easier to reason about thread-safety).
  int32_t InitEncodeInternal(int width,
                             int height,
                             int kbps,
                             int fps,
                             bool use_surface);
  // Reconfigure to match |frame| in width, height. Also reconfigures the
  // encoder if |frame| is a texture/byte buffer and the encoder is initialized
  // for byte buffer/texture. Returns false if reconfiguring fails.
  bool MaybeReconfigureEncoder(JNIEnv* jni, const VideoFrame& frame);

  // Returns true if the frame is a texture frame and we should use surface
  // based encoding.
  bool IsTextureFrame(JNIEnv* jni, const VideoFrame& frame);

  bool EncodeByteBuffer(JNIEnv* jni,
                        bool key_frame,
                        const VideoFrame& frame,
                        int input_buffer_index);
  // Encodes a new style org.webrtc.VideoFrame. Might be a I420 or a texture
  // frame.
  bool EncodeJavaFrame(JNIEnv* jni,
                       bool key_frame,
                       const JavaRef<jobject>& frame,
                       int input_buffer_index);

  // Deliver any outputs pending in the MediaCodec to our |callback_| and return
  // true on success.
  bool DeliverPendingOutputs(JNIEnv* jni);

  VideoEncoder::ScalingSettings GetScalingSettingsInternal() const;

  // Displays encoder statistics.
  void LogStatistics(bool force_log);

  VideoCodecType GetCodecType() const;

#if RTC_DCHECK_IS_ON
  // Mutex for protecting inited_. It is only used for correctness checking on
  // debug build. It is used for checking that encoder has been released in the
  // destructor. Because this might happen on a different thread, we need a
  // mutex.
  webrtc::Mutex inited_crit_;
#endif

  // Type of video codec.
  const SdpVideoFormat format_;

  EncodedImageCallback* callback_;

  // State that is constant for the lifetime of this object once the ctor
  // returns.
  SequenceChecker encoder_queue_checker_;
  ScopedJavaGlobalRef<jobject> j_media_codec_video_encoder_;

  // State that is valid only between InitEncode() and the next Release().
  int width_;   // Frame width in pixels.
  int height_;  // Frame height in pixels.
  bool inited_;
  bool use_surface_;
  enum libyuv::FourCC encoder_fourcc_;  // Encoder color space format.
  uint32_t last_set_bitrate_kbps_;      // Last-requested bitrate in kbps.
  uint32_t last_set_fps_;               // Last-requested frame rate.
  int64_t current_timestamp_us_;        // Current frame timestamps in us.
  int frames_received_;                 // Number of frames received by encoder.
  int frames_encoded_;                  // Number of frames encoded by encoder.
  int frames_dropped_media_encoder_;    // Number of frames dropped by encoder.
  // Number of dropped frames caused by full queue.
  int consecutive_full_queue_frame_drops_;
  int64_t stat_start_time_ms_;  // Start time for statistics.
  int current_frames_;  // Number of frames in the current statistics interval.
  int current_bytes_;   // Encoded bytes in the current statistics interval.
  int current_acc_qp_;  // Accumulated QP in the current statistics interval.
  int current_encoding_time_ms_;  // Overall encoding time in the current second
  int64_t last_input_timestamp_ms_;   // Timestamp of last received yuv frame.
  int64_t last_output_timestamp_ms_;  // Timestamp of last encoded frame.
  // Holds the task while the polling loop is paused.
  std::unique_ptr<QueuedTask> encode_task_;

  struct InputFrameInfo {
    InputFrameInfo(int64_t encode_start_time,
                   int32_t frame_timestamp,
                   int64_t frame_render_time_ms,
                   VideoRotation rotation)
        : encode_start_time(encode_start_time),
          frame_timestamp(frame_timestamp),
          frame_render_time_ms(frame_render_time_ms),
          rotation(rotation) {}
    // Time when video frame is sent to encoder input.
    const int64_t encode_start_time;

    // Input frame information.
    const int32_t frame_timestamp;
    const int64_t frame_render_time_ms;
    const VideoRotation rotation;
  };
  std::list<InputFrameInfo> input_frame_infos_;
  int32_t output_timestamp_;       // Last output frame timestamp from
                                   // |input_frame_infos_|.
  int64_t output_render_time_ms_;  // Last output frame render time from
                                   // |input_frame_infos_|.
  VideoRotation output_rotation_;  // Last output frame rotation from
                                   // |input_frame_infos_|.

  // Frame size in bytes fed to MediaCodec.
  int yuv_size_;
  // True only when between a callback_->OnEncodedImage() call return a positive
  // value and the next Encode() call being ignored.
  bool drop_next_input_frame_;
  bool scale_;
  H264::Profile profile_;
  // Global references; must be deleted in Release().
  std::vector<ScopedJavaGlobalRef<jobject>> input_buffers_;
  H264BitstreamParser h264_bitstream_parser_;

  // VP9 variables to populate codec specific structure.
  GofInfoVP9 gof_;  // Contains each frame's temporal information for
                    // non-flexible VP9 mode.
  size_t gof_idx_;

  const bool has_egl_context_;
  EncoderInfo encoder_info_;

  // Temporary fix for VP8.
  // Sends a key frame if frames are largely spaced apart (possibly
  // corresponding to a large image change).
  int64_t last_frame_received_ms_;
  int frames_received_since_last_key_;
  VideoCodecMode codec_mode_;

  bool sw_fallback_required_;

  // All other member variables should be before WeakPtrFactory. Valid only from
  // InitEncode to Release.
  std::unique_ptr<rtc::WeakPtrFactory<MediaCodecVideoEncoder>> weak_factory_;
};




}  // namespace jni
}  // namespace webrtc

#endif  // SDK_ANDROID_SRC_JNI_ANDROIDMEDIAENCODER_H_
