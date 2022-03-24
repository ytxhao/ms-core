package media.ushow.zorro;

import java.lang.System;
import java.lang.String;
import java.util.List;
import java.util.LinkedHashMap;
import javax.annotation.Nullable;
import android.content.Context;
import android.view.SurfaceView;
import org.webrtc.Logging;
import org.webrtc.CalledByNative;
import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.ContextUtils;
import android.media.AudioManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.StatFs;
import android.util.Log;
import android.text.TextUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ZorroRtcEngine {
  private static final String TAG = "ZorroRtcEngine";
  private static final long MIN_FREE_SPACE_BYTES = 1024 * 1024 * 250L;
  static {
    System.loadLibrary("zorro");
    instance = new ZorroRtcEngine();
    Logging.d(TAG, "Native library loaded for Zorro-RTC-Engine");
  }

  public enum Profile {
    PROFILE_VOICE_CHAT_ROOM,
    PROFILE_LIVE,
  };

  public enum MediaType {
    MEDIA_AUDIO,
    MEDIA_VIDEO,
    MEDIA_AUDIOVIDEO;

    @CalledByNative("MediaType")
    static MediaType fromNativeIndex(int nativeIndex) {
      return values()[nativeIndex];
    }
  };

  public enum MediaDirection {
    MEDIA_INACTIVE,
    MEDIA_SENDONLY,
    MEDIA_RECVONLY,
    MEDIA_SENDRECV,
  };

  public enum AudioMode {
    AUDIO_MODE_COMMUNICATION(1),
    AUDIO_MODE_MEDIA(2);

    private final int value;
    AudioMode(int value) {
        this.value = value;
    }
    public int getValue() {
      return value;
    }
  };

  public enum AudioCodec {
    AUDIO_CODEC_LC_AAC,
    AUDIO_CODEC_HE_AAC,
    AUDIO_CODEC_OPUS,
  };

  public enum State {
    SUCCESS,
    FAILED;

    @CalledByNative("State")
    static State fromNativeIndex(int nativeIndex) {
      return values()[nativeIndex];
    }
  };

  public enum ErrorCode {
    ERROR_KICKED_BY_REMOTE,
    ERROR_MEDIA_CONNECTION_FAILURE,
    ERROR_LOGIN_TIMEOUT,
    ERROR_PUSH_TIMEOUT,
    ERROR_NO_SEND_VIDEO_DATA_TIMEOUT,
    ERROR_NO_SEND_AUDIO_DATA_TIMEOUT,
    ERROR_SETUP_CALL_TIMEOUT,
    ERROR_SIGNAL_CONNECTION_FAILURE,
    ERROR_SERVICE_UPDATE,
    ERROR_MEDIA_CONNECTION_TIMEOUT,
    ERROR_CODE_MAX;

    @CalledByNative("ErrorCode")
    static ErrorCode fromNativeIndex(int nativeIndex) {
      return values()[nativeIndex];
    }
  };

  public enum VideoFrameType {
    I_FRAME,
    P_FRAME,
  };

  public static interface Observer {
    @CalledByNative("Observer") void onLogInState(State state);
    @CalledByNative("Observer") void onLogOutState(State state);
    @CalledByNative("Observer") void onCallSetUp(State state);
    @CalledByNative("Observer") void onCallClosedDown();
    @CalledByNative("Observer") void onMediaStreamAdded(String uid, MediaType media, String extraInfo);
    @CalledByNative("Observer") void onMediaStreamRemoved(String uid);
    @CalledByNative("Observer") void onVideoKeyFrameRequest();
    @CalledByNative("Observer") void onVideoEncodingRateSet(int bitrate_kbps, int framerate);
    @CalledByNative("Observer") void onFirstVideoFrameDecoded(String uid);
    @CalledByNative("Observer") void onFirstAudioFrameDecoded(String uid);
    @CalledByNative("Observer") void onFirstMediaFrameSent();
    @CalledByNative("Observer") void onVideoLayoutReceived(String layout);
    @CalledByNative("Observer") void onVideoSEIData(int type, byte[] payload);
    @CalledByNative("Observer") void onError(ErrorCode code, String text);
    @CalledByNative("Observer") void onRoomDestroy(String reason);
    @CalledByNative("Observer") void onAudioVolumeIndication(AudioVolumeInfo[] speakers);
    @CalledByNative("Observer") void OnRtcStats(RtcStats stats);
  }

  public static class MediaConfig {
    public MediaType      type;
    public MediaDirection dir;
    public int            videoWidth;
    public int            videoHeight;
    public int            targetBitrateKbps;
    public int            minBitrateKbps;
    public boolean        useExternalH264Capture;
    public boolean        useExternalPcmCapture;
    public AudioCodec     audioCodec;
    public boolean        enablePush;
    public String         pushUrl;
    public String         mediaServerLocation;

    public MediaConfig(MediaType type, MediaDirection dir, int videoWidth, int videoHeight, 
        int targetBitrateKbps, int minBitrateKbps, boolean useExternalH264Capture, 
        boolean useExternalPcmCapture, AudioCodec audioCodec, boolean enablePush, 
        String pushUrl, String mediaServerLocation) {
      if (type != MediaType.MEDIA_AUDIO && type != MediaType.MEDIA_AUDIOVIDEO) {
        throw new IllegalArgumentException("type != MEDIA_AUDIO && type != MEDIA_AUDIOVIDEO");
      }
      if (dir != MediaDirection.MEDIA_RECVONLY && dir != MediaDirection.MEDIA_SENDRECV) {
        throw new IllegalArgumentException("dir != MEDIA_RECVONLY && dir != MEDIA_SENDRECV");
      }
      if (videoWidth <= 0 || videoHeight <= 0 || targetBitrateKbps <= 0 || minBitrateKbps <= 0) {
        throw new IllegalArgumentException(
            "videoWidth <= 0 || videoHeight <= 0 || targetBitrateKbps <= 0 || minBitrateKbps <= 0");
      }
      if (minBitrateKbps > targetBitrateKbps) {
        throw new IllegalArgumentException("minBitrateKbps > targetBitrateKbps");
      }
      if (audioCodec != AudioCodec.AUDIO_CODEC_OPUS && 
          audioCodec != AudioCodec.AUDIO_CODEC_LC_AAC && 
          audioCodec != AudioCodec.AUDIO_CODEC_HE_AAC) {
        throw new IllegalArgumentException(
            "audioCodec != AUDIO_CODEC_OPUS && audioCodec != AUDIO_CODEC_LC_AAC && audioCodec != AudioCodec.AUDIO_CODEC_HE_AAC");
      }
      if (enablePush) {
        if (pushUrl == null || pushUrl.isEmpty()) {
          throw new IllegalArgumentException("pushUrl == null || pushUrl.isEmpty()");
        }
      }
      if (pushUrl == null) {
        pushUrl = "";
      }
      if (mediaServerLocation == null) {
        mediaServerLocation = "";
      }

      this.type = type;
      this.dir = dir;
      this.videoWidth = videoWidth;
      this.videoHeight = videoHeight;
      this.targetBitrateKbps = targetBitrateKbps;
      this.minBitrateKbps = minBitrateKbps;
      this.useExternalH264Capture = useExternalH264Capture;
      this.useExternalPcmCapture = useExternalPcmCapture;
      this.audioCodec = audioCodec;
      this.enablePush = enablePush;
      this.pushUrl = pushUrl;
      this.mediaServerLocation = mediaServerLocation;
    }

    @CalledByNative("MediaConfig")
    MediaType getMediaType() {
      return type;
    }

    @CalledByNative("MediaConfig")
    MediaDirection getMediaDirection() {
      return dir;
    }

    @CalledByNative("MediaConfig")
    int getVideoWidth() {
      return videoWidth;
    }

    @CalledByNative("MediaConfig")
    int getVideoHeight() {
      return videoHeight;
    }

    @CalledByNative("MediaConfig")
    int getTargetBitrateKbps() {
      return targetBitrateKbps;
    }

    @CalledByNative("MediaConfig")
    int getMinBitrateKbps() {
      return minBitrateKbps;
    }

    @CalledByNative("MediaConfig")
    boolean getUseExternalH264Capture() {
      return useExternalH264Capture;
    }

    @CalledByNative("MediaConfig")
    boolean getUseExternalPcmCapture() {
      return useExternalPcmCapture;
    }

    @CalledByNative("MediaConfig")
    AudioCodec getAudioCodec() {
      return audioCodec;
    }

    @CalledByNative("MediaConfig")
    boolean getEnablePush() {
      return enablePush;
    }

    @CalledByNative("MediaConfig")
    String getPushURL() {
      return pushUrl;
    }

    @CalledByNative("MediaConfig")
    String getMediaServerLocation() {
      return mediaServerLocation;
    }
  };

  public static class MixStreamRegionConfig {
    public String uid;
    public int slotIndex;
    public boolean isVideo;
    public int left;
    public int top;
    public int width;
    public int height;

    public MixStreamRegionConfig(String uid, int slotIndex, boolean isVideo, int left, int top,
                           int width, int height) {
      if (uid == null || uid.isEmpty()) {
        throw new IllegalArgumentException("uid == null || uid.isEmpty()");
      }

      if (left < 0 || top < 0 || width <= 0 || height <= 0) {
        throw new IllegalArgumentException(
            "left < 0 || top < 0 || width <= 0 || height <= 0");
      }

      this.uid = uid;
      this.slotIndex = slotIndex;
      this.isVideo = isVideo;
      this.left = left;
      this.top = top;
      this.width = width;
      this.height = height;
    }

    @CalledByNative("MixStreamRegionConfig")
    String getUserId() {
      return uid;
    }

    @CalledByNative("MixStreamRegionConfig")
    int getSlotIndex() {
      return slotIndex;
    }

    @CalledByNative("MixStreamRegionConfig")
    boolean getIsVideo() {
      return isVideo;
    }

    @CalledByNative("MixStreamRegionConfig")
    int getLeft() {
      return left;
    }

    @CalledByNative("MixStreamRegionConfig")
    int getTop() {
      return top;
    }

    @CalledByNative("MixStreamRegionConfig")
    int getWidth() {
      return width;
    }

    @CalledByNative("MixStreamRegionConfig")
    int getHeight() {
      return height;
    }
  }

  public static class MixStreamConfig {
    public int audioChannel;
    public int audioBitrateKbps;
    public int audioSamplerate;

    public int videoWidth;
    public int videoHeight;
    public int videoFps;
    public int videoBitrateKbps;
    public int videoGop;
    public String videoBackgroundColor;
    public String videoBackgroundUrl;
    public List<MixStreamRegionConfig> regions;

    @CalledByNative("MixStreamConfig")
    int getAudioChannel() {
      return audioChannel;
    }

    @CalledByNative("MixStreamConfig")
    int getAudioBitrateKbps() {
      return audioBitrateKbps;
    }

    @CalledByNative("MixStreamConfig")
    int getAudioSamplerate() {
      return audioSamplerate;
    }

    @CalledByNative("MixStreamConfig")
    int getVideoWidth() {
      return videoWidth;
    }

    @CalledByNative("MixStreamConfig")
    int getVideoHeight() {
      return videoHeight;
    }

    @CalledByNative("MixStreamConfig")
    int getVideoFps() {
      return videoFps;
    }

    @CalledByNative("MixStreamConfig")
    int getVideoBitrateKbps() {
      return videoBitrateKbps;
    }

    @CalledByNative("MixStreamConfig")
    int getVideoGop() {
      return videoGop;
    }

    @CalledByNative("MixStreamConfig")
    String getVideoBackgroundColor() {
      return videoBackgroundColor;
    }

    @CalledByNative("MixStreamConfig")
    String getVideoBackgroundUrl() {
      return videoBackgroundUrl;
    }

    @CalledByNative("MixStreamConfig")
    List<MixStreamRegionConfig> getRegions() {
      return regions;
    }
  }

  public static class AudioVolumeInfo {
    public String  uid;
    public int     level;

    @CalledByNative("AudioVolumeInfo")
    public AudioVolumeInfo(String uid, int level) {
      this.uid = uid;
      this.level = level;
    }
  }

  public static class RemoteAudioTransportStats {
    public String  uid;
    public int     delayMs;
    public int     lostPercent;
    public int     receivedBitrateKbps;

    @CalledByNative("RemoteAudioTransportStats")
    public RemoteAudioTransportStats(String uid, int delayMs, int lostPercent, int receivedBitrateKbps) {
      this.uid = uid;
      this.delayMs = delayMs;
      this.lostPercent = lostPercent;
      this.receivedBitrateKbps = receivedBitrateKbps;
    }
  }

  public static class RemoteVideoTransportStats {
    public String  uid;
    public int     delayMs;
    public int     lostPercent;
    public int     receivedBitrateKbps;

    @CalledByNative("RemoteVideoTransportStats")
    public RemoteVideoTransportStats(String uid, int delayMs, int lostPercent, int receivedBitrateKbps) {
      this.uid = uid;
      this.delayMs = delayMs;
      this.lostPercent = lostPercent;
      this.receivedBitrateKbps = receivedBitrateKbps;
    }
  }

  public static class RemoteAudioStats {
    public String  uid;
    public int     networkTransportDelayMs;
    public int     jitterBufferDelayMs;

    @CalledByNative("RemoteAudioStats")
    public RemoteAudioStats(String uid, int networkTransportDelayMs, int jitterBufferDelayMs) {
      this.uid = uid;
      this.networkTransportDelayMs = networkTransportDelayMs;
      this.jitterBufferDelayMs = jitterBufferDelayMs;
    }
  }
  
  public static class RemoteVideoStats {
    public String  uid;
    public int     width;
    public int     height;
    public int     receivedBitrateKbps;
    public int     receivedFrameRate;
    public int     frozenRate;
    public long    totalFrozenTimeMs;

    @CalledByNative("RemoteVideoStats")
    public RemoteVideoStats(String uid, int width, int height, int receivedBitrateKbps, int receivedFrameRate, int frozenRate, long totalFrozenTimeMs) {
      this.uid = uid;
      this.width = width;
      this.height = height;
      this.receivedBitrateKbps = receivedBitrateKbps;
      this.receivedFrameRate = receivedFrameRate;
      this.frozenRate = frozenRate;
      this.totalFrozenTimeMs = totalFrozenTimeMs;
    }
  }

  public static class LocalAudioStats {
    public int     sentBitrateKbps;
    public int     delayMs;
    public int     lostPercent;

    @CalledByNative("LocalAudioStats")
    public LocalAudioStats(int delayMs, int sentBitrateKbps, int lostPercent) {
      this.delayMs = delayMs;
      this.sentBitrateKbps = sentBitrateKbps;
      this.lostPercent = lostPercent;
    }
  }
 
  public static class LocalVideoStats {
    public int     width;
    public int     height;
    public int     sentBitrateKbps;
    public int     sentFrameRate;
    public int     lostPercent;

    @CalledByNative("LocalVideoStats")
    public LocalVideoStats(int width, int height, int sentBitrateKbps, int sentFrameRate, int lostPercent) {
      this.width = width;
      this.height = height;
      this.sentBitrateKbps = sentBitrateKbps;
      this.sentFrameRate = sentFrameRate;
      this.lostPercent = lostPercent;
    }
  }

    public static class RtcStats {
      public RemoteAudioTransportStats[] remoteAudioTransportStats;
      public RemoteVideoTransportStats[] remoteVideoTransportStats;
      public RemoteAudioStats[]          remoteAudioStats;
      public RemoteVideoStats[]          remoteVideoStats;
      public LocalAudioStats             localAudioStats;
      public LocalVideoStats             localVideoStats;

    @CalledByNative("RtcStats")
    public RtcStats(RemoteAudioTransportStats[] remoteAudioTransportStats, 
                    RemoteVideoTransportStats[] remoteVideoTransportStats, 
                    RemoteAudioStats[] remoteAudioStats, 
                    RemoteVideoStats[] remoteVideoStats, 
                    LocalAudioStats localAudioStats, 
                    LocalVideoStats localVideoStats) {
      this.remoteAudioTransportStats = remoteAudioTransportStats;
      this.remoteVideoTransportStats = remoteVideoTransportStats;
      this.remoteAudioStats = remoteAudioStats;
      this.remoteVideoStats = remoteVideoStats;
      this.localAudioStats = localAudioStats;
      this.localVideoStats = localVideoStats;
    }
  }

  private class HeadsetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
        AudioManager audioManager = (AudioManager) ContextUtils.getApplicationContext()
            .getSystemService(Context.AUDIO_SERVICE);
        int state = intent.getIntExtra("state", -1);
        if (state == 0 && !audioManager.isSpeakerphoneOn()) { // Headset is unplugged
          audioManager.setSpeakerphoneOn(true);
        } else if (state == 1 && audioManager.isSpeakerphoneOn()) { // Headset is plugged
          audioManager.setSpeakerphoneOn(false);
        }
      }
    }
  }

  private void autoSwitchSpeaker(boolean on) {
    if (on) {
      if (headsetReceiver == null) {
        headsetReceiver = new HeadsetReceiver();
        ContextUtils.getApplicationContext().registerReceiver(headsetReceiver,
            new IntentFilter(Intent.ACTION_HEADSET_PLUG));
      }
      AudioManager audioManager = (AudioManager) ContextUtils.getApplicationContext()
          .getSystemService(Context.AUDIO_SERVICE);
      boolean wiredHeadsetOn = audioManager.isWiredHeadsetOn();
      if (audioManager.isSpeakerphoneOn() != !wiredHeadsetOn) {
        audioManager.setSpeakerphoneOn(!wiredHeadsetOn);
      }
    } else {
      if (headsetReceiver != null) {
        try {
          ContextUtils.getApplicationContext().unregisterReceiver(headsetReceiver);
        } catch (Exception e) {
          e.printStackTrace();
        }
        headsetReceiver = null;
      }
    }
  }

  private long getFreeSpaceInBytes(String filePath) {
    long freeSpaceInBytes = 0;
    if (TextUtils.isEmpty(filePath)) {
      return freeSpaceInBytes;
    }
    try {
      File file = new File(filePath);
      freeSpaceInBytes = file.getFreeSpace();
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      if (freeSpaceInBytes <= 0) {
        StatFs statFs = new StatFs(filePath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          freeSpaceInBytes = statFs.getAvailableBytes();
        } else {
          long size = statFs.getBlockSize();
          freeSpaceInBytes = size * statFs.getAvailableBlocks();
          }
        }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return freeSpaceInBytes;
}

  private static ZorroRtcEngine instance;
  private Observer observer;
  private @Nullable EglBase eglBase;
  private @Nullable HeadsetReceiver headsetReceiver;

  public static ZorroRtcEngine getInstance() {
    return instance;
  }

  private ZorroRtcEngine() {}

  public int init(Context context, Observer observer, String appDomain) {
    this.observer = observer;
    String path = "";
    if (context.getExternalFilesDir("zorro") != null) {
      path = context.getExternalFilesDir("zorro").getAbsolutePath();
    } 
    nativeSetDeviceInfo(ZorroDeviceInfo.getDeviceInfo());
    long freeSpace = getFreeSpaceInBytes(path);
    if (freeSpace < MIN_FREE_SPACE_BYTES) {
      path = "";
      Logging.e(TAG, "Storage space not enough to write log!");
    } 
    ConnectivityManager connectivityManager =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    return nativeInit(context, observer, path, connectivityManager, appDomain);
  }

  public int setObserver(Observer observer) {
    this.observer = observer;
    return nativeSetObserver(observer);
  }

  // MUST be called before logIn()
  public void setProfile(Profile profile) {
    nativeSetProfile(profile); 
  }

  public int logIn(String appId, String roomId, String uid, String passwd, String location, 
      boolean isOwner, int timeoutMs) {
    if (appId == null || appId.isEmpty() || roomId == null || roomId.isEmpty() || uid == null ||
        uid.isEmpty() || timeoutMs <= 0) {
      throw new IllegalArgumentException(
          "appId == null || appId.isEmpty() || roomId == null || roomId.isEmpty() || uid == null || uid.isEmpty() || timeoutMs <= 0");
    }
    autoSwitchSpeaker(true);
    nativeLogIn(appId, roomId, uid, passwd, location, isOwner, timeoutMs);

    return 0;
  }

  // @relogin: true, if call logIn() on onLogOutState().
  public void logOut(boolean relogin) {
    autoSwitchSpeaker(false);
    nativeLogOut(relogin);
  }

  public void setAudioSource(boolean external, int sampleRate, int channels) {
    nativeSetAudioSource(external, sampleRate, channels);
  }

  // @extraInfo: other users receive it by onMediaStreamAdded of this user.
  public int setUpCall(MediaConfig mediaConfig, long callId, String extraInfo, int timeoutMs) {
    if (mediaConfig == null || timeoutMs <= 0) {
      throw new IllegalArgumentException(
          "mediaConfig == null || timeoutMs <= 0");
    }

    eglBase = null;
    return nativeSetUpCall(mediaConfig, callId, extraInfo, timeoutMs);
  }

  // @resetup: true, if call setupCall() on onLogOutState().
  public void closeDownCall(boolean resetup) {
    nativeCloseDownCall(resetup);
  }

  public int setMixStreamConfig(MixStreamConfig config) {
    return nativeSetMixStreamConfig(config);
  }

  // Make sure call CreateVideoRenderer() after onCallSetUp() for local renderer,  and after 
  // onMediaStreamAdded() for remote renderers.
  @Nullable
  public SurfaceView CreateVideoRenderer(Context context, String uid) {
    if (context == null || uid == null || uid.isEmpty()) {
      throw new IllegalArgumentException("context == null || uid == null || uid.isEmpty()");
    }

    if (eglBase == null) {
      eglBase = EglBase.create();
    }
    SurfaceViewRenderer svr = new SurfaceViewRenderer(context);
    svr.init(eglBase.getEglBaseContext(), null);
    svr.setScalingType(ScalingType.SCALE_ASPECT_FIT);
    nativeSetVideoSurfaceViewRenderer(uid, svr);

    return svr;
  }

  // @createdSurfaceview MUST be created by CreateVideoRenderer()
  public void SetVideoRenderer(String uid, SurfaceView createdSurfaceview) {
    if (createdSurfaceview == null || uid == null || uid.isEmpty()) {
      throw new IllegalArgumentException("context == null || uid == null || uid.isEmpty()");
    }
    if (createdSurfaceview != null) {
      nativeSetVideoSurfaceViewRenderer(uid, (SurfaceViewRenderer)createdSurfaceview);
    }
  }

  public int enableAudioVolumeIndication(int intervalMs) {
    return nativeEnableAudioVolumeIndication(intervalMs);
  }

  public int enableMediaStreamFilter(
      boolean enable, List<String> uidWhitelist, List<String> uidBlacklist) {
    if (enable && !uidWhitelist.isEmpty() && !uidBlacklist.isEmpty()) {
      throw new IllegalArgumentException("enable && !uidWhitelist.isEmpty() && !uidBlacklist.isEmpty()");
    }
    return nativeEnableMediaStreamFilter(enable, uidWhitelist, uidBlacklist);
  }

  public void pushVideoFrame(byte[] data, int size, VideoFrameType type, long timestamp) {
    nativePushVideoFrame(data, size, type, timestamp);
  }

  public int setAudioMode(AudioMode mode) {
    AudioManager audioManager =
        (AudioManager)ContextUtils.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    if (mode == AudioMode.AUDIO_MODE_COMMUNICATION) {
      audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    } else if (mode == AudioMode.AUDIO_MODE_MEDIA) {
      audioManager.setMode(AudioManager.MODE_NORMAL);
    } else {
      throw new IllegalArgumentException("mode != AUDIO_MODE_COMMUNICATION && mode != AUDIO_MODE_MEDIA");
    }
    return nativeSetAudioMode(mode);
  }

  public void startMediaStatsReport(int intervalMs) {
    nativeStartMediaStatsReport(intervalMs);
  }

  public void stopMediaStatsReport() {
    nativeStopMediaStatsReport();
  }

  public void kickOut(String uid) {
    if (uid == null || uid.isEmpty()) {
      throw new IllegalArgumentException("uid == null || uid.isEmpty()");
    }
    nativeKickOut(uid);
  }

  public void setMediaDirection(MediaDirection dir) {
    nativeSetMediaDirection(dir);
  }

  public String[] getBroadcasters() {
    return nativeGetBroadcasters();
  }

  public void muteRemoteSpeaker(String uid, boolean mute) {
    nativeMuteRemoteSpeaker(uid, mute);
  }

  public void muteLocalAudioStream(boolean mute) {
    nativeMuteLocalAudioStream(mute);
  }

  public String getSDKVersion() {
    return nativeGetSDKVersion();
  }
 
  private native void nativeSetProfile(Profile profile);
  private native void nativeSetDeviceInfo(ZorroDeviceInfo.DeviceInfo info);
  private native String nativeGetSDKVersion();
  private native int nativeInit(Context context, Observer observer, String logAbsolutePath,
      ConnectivityManager connectivityManager, String appDomain);
  private native int nativeSetObserver(Observer observer);
  private native int nativeLogIn(String appId, String roomId, String uid, String passwd, 
      String location, boolean isOwner, int timeoutMs);
  private native void nativeLogOut(boolean relogin);
  private native void nativeSetAudioSource(boolean external, int sampleRate, int channels);
  private native int nativeSetUpCall(MediaConfig media, long callId, String extraInfo, int timeoutMs);
  private native void nativeCloseDownCall(boolean resetup);
  private native void nativeSetVideoSurfaceViewRenderer(String uid, SurfaceViewRenderer svr);
  private native int nativeSetMixStreamConfig(MixStreamConfig config);
  private native int nativeEnableAudioVolumeIndication(int intervalMs);
  // ONLY use @uidWhiteList or @uidBlackList
  private native int nativeEnableMediaStreamFilter(
      boolean enable, List<String> uidWhitelist, List<String> uidBlacklist);
  private native void nativePushVideoFrame(
      byte[] data, int size, VideoFrameType type, long timestamp);
  private native int nativeSetAudioMode(AudioMode mode); 
  private native void nativeStartMediaStatsReport(int intervalMs);
  private native void nativeStopMediaStatsReport();
  private native void nativeKickOut(String uid);
  private native void nativeSetMediaDirection(MediaDirection dir);
  private native String[] nativeGetBroadcasters();
  private native void nativeMuteRemoteSpeaker(String uid, boolean mute);
  private native void nativeMuteLocalAudioStream(boolean mute);
}

