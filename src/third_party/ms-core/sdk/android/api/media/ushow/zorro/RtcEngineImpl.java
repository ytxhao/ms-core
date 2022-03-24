package media.ushow.zorro;

import android.content.Context;
import android.text.TextUtils;
import android.os.Build;
import android.os.StatFs;
import android.os.Looper;
import android.view.SurfaceView;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

import org.webrtc.Logging;
import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.RendererCommon.ScalingType;

import media.ushow.zorro.IRtcEngineObserver;
import media.ushow.zorro.Constants;
import media.ushow.zorro.LiveTranscoding;
import media.ushow.zorro.VideoCanvas;


public class RtcEngineImpl extends RtcEngine {
  private static final String TAG = "ZorroEngine";

  private static final long MIN_FREE_SPACE_BYTES = 1024 * 1024 * 250L;

  private static boolean libLoaded;
  private WeakReference<Context> context;
  private int profile;
  private int clientRole;
  private boolean localVideoEnabled;
  private static @Nullable EglBase eglBase;
  private String logPath;

  public static void loadNativeLibrary() {
    System.loadLibrary("zorro");
  }

  public static boolean initializeNativeLibs() {
    synchronized(RtcEngineImpl.class) {
      if (!libLoaded) {
        loadNativeLibrary();
        libLoaded = true;
      }
    }
    return libLoaded;
  }

  public void doDestroy() {
    nativeSetObserver(null);
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

  private void checkPermissions(Context context, String paramString) throws SecurityException {
    if (context == null || context.checkCallingOrSelfPermission(paramString) != PackageManager.PERMISSION_GRANTED)
      throw new SecurityException(paramString + " is not granted"); 
  }
  
  private void checkPermissions(Context context) throws SecurityException {
    checkPermissions(context, "android.permission.INTERNET");
    checkPermissions(context, "android.permission.RECORD_AUDIO");
    checkPermissions(context, "android.permission.MODIFY_AUDIO_SETTINGS");
    // if (this.mVideoSourceType == 1 && this.localVideoEnabled)
      // checkPermissions(context, "android.permission.CAMERA"); 
  }
  
  private int checkPermissions(Context context, int role) {
    switch (role) {
      case Constants.CLIENT_ROLE_BROADCASTER:
        try {
          checkPermissions(context);
        } catch (SecurityException securityException) {
          Logging.e(TAG, "Do not have enough permission for BROADCASTER! ", securityException);
          return -1;
        }
        return 0;
      case Constants.CLIENT_ROLE_AUDIENCE:
        try {
          checkPermissions(context, "android.permission.INTERNET");
        } catch (SecurityException securityException) {
          Logging.e(TAG, "Do not have Internet permission for AUDIENCE!");
          return -1;
        }
        return 0;
    } 
    return -1;
  }

  private int doCheckPermission(Context context) {
    if (checkPermissions(context, this.clientRole) != 0) {
      Logging.e(TAG, "can't join channel without permission");
      return -1;
    }
    return 0;
  }
  
  public static String getSdkVersion() {
    if (!initializeNativeLibs()) {
      return "";
    }
    return nativeGetSdkVersion();
  }

  public void reinitialize(RtcEngineConfig config) {
    this.context = new WeakReference<>(config.context);

    if (logPath == null) {
      logPath = "";
      File logFileDir = config.context.getExternalFilesDir("zorro");
      if (logFileDir != null) {
        logPath = logFileDir.getAbsolutePath();
      }
    }

    if (!TextUtils.isEmpty(logPath)) {
      long freeSpace = getFreeSpaceInBytes(logPath);
      if (freeSpace < MIN_FREE_SPACE_BYTES) {
        logPath = "";
        Logging.e(TAG, "Not enough storage space for logging: " + freeSpace + " bytes");
      }
    }

    nativeInit(config.context, config.observer, config.appId, config.countryCode, logPath);
  }

  public RtcEngineImpl(RtcEngineConfig config) throws Exception {
    reinitialize(config);
  }

  @Override
  public int setParameters(String parameters) {
    if (parameters.equals("{\"audio.mode\":1}")) {
      Context context = this.context.get();
      if (context == null) return -1;
      AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
      int mode = audioManager.getMode();
      if (mode != AudioManager.MODE_IN_COMMUNICATION) {
        Logging.d(TAG, "Audio mode: " + mode + "->" + AudioManager.MODE_IN_COMMUNICATION);
        try {
          audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } catch (NullPointerException e) {
          Logging.e(TAG, "Exception when set audio mode: " + mode + "->" + AudioManager.MODE_IN_COMMUNICATION
              + ", error: " + e.getMessage());
        }
      }
    } else if (parameters.equals("{\"audio.mode\":2}")) {
      Context context = this.context.get();
      if (context == null) return -1;
      AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
      int mode = audioManager.getMode();
      if (mode != AudioManager.MODE_NORMAL) {
        Logging.d(TAG, "Audio mode: " + mode + "->" + AudioManager.MODE_NORMAL);
        try {
          audioManager.setMode(AudioManager.MODE_NORMAL);
        } catch (NullPointerException e) {
          Logging.e(TAG, "Exception when set audio mode: " + mode + "->" + AudioManager.MODE_NORMAL + ", error: "
              + e.getMessage());
        }
      }
    } else if (parameters.equals("{\"audio.mode\":3}")) {
      Context context = this.context.get();
      if (context == null) return -1;
      AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
      int mode = audioManager.getMode();
      if (mode != AudioManager.MODE_IN_CALL) {
        Logging.d(TAG, "Audio mode: " + mode + "->" + AudioManager.MODE_IN_CALL);
        try {
          audioManager.setMode(AudioManager.MODE_IN_CALL);
        } catch (NullPointerException e) {
          Logging.e(TAG, "Exception when set audio mode: " + mode + "->" + AudioManager.MODE_IN_CALL + ", error: "
              + e.getMessage());
        }
      }
    }
    return nativeSetParameters(parameters);
  }

  @Override
  public int setObserver(IRtcEngineObserver observer) {
    return nativeSetObserver(observer);
  }

  @Override
  public int setAudioSource(boolean external, int sampleRate, int channels) {
    return nativeSetAudioSource(external, sampleRate, channels);
  }

  @Override
  public int setVideoSource(boolean external, int width, int height) {
    return nativeSetVideoSource(external, width, height);
  }

  @Override
  public int setChannelProfile(int profile) {
    if (profile == Constants.CHANNEL_PROFILE_VOICE_CHAT_ROOM ||
        profile == Constants.CHANNEL_PROFILE_LIVE_CALL) {
      this.profile = profile;
      if (profile == Constants.CHANNEL_PROFILE_VOICE_CHAT_ROOM) {
        setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
        if (this.localVideoEnabled) {
          disableVideo();
          Logging.w(TAG, "Video disabled for CHANNEL_PROFILE_VOICE_CHAT_ROOM.");
        }
      } else {
        setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
      }
      return nativeSetChannelProfile(profile);
    } else {
      return -1; 
    }
  }

  @Override
  public int enableVideo() {
    if (this.profile != Constants.CHANNEL_PROFILE_VOICE_CHAT_ROOM) {
      this.localVideoEnabled = true;
      return nativeEnableVideo();
    } else {
      Logging.e(TAG, "Video not supported for CHANNEL_PROFILE_VOICE_CHAT_ROOM.");
      return -1; 
    }
  }

  @Override
  public int disableVideo() {
    this.localVideoEnabled = false;
    return nativeDisableVideo();
  }

  @Override
  public int setClientRole(int role) {
    if (role == Constants.CLIENT_ROLE_BROADCASTER ||
        role == Constants.CLIENT_ROLE_AUDIENCE) {
      this.clientRole = role;
      return nativeSetClientRole(role);
    } else {
      return -1; 
    }
  }

  public static @Nullable SurfaceView createRendererView(Context context) {
    if (!RtcEngineImpl.checkIsOnMainThread("createRendererView") || context == null) {
      return null;
    }

    if (eglBase == null) {
      eglBase = EglBase.create();
    }
    SurfaceViewRenderer svr = new SurfaceViewRenderer(context);
    svr.init(eglBase.getEglBaseContext(), null);
    svr.setScalingType(ScalingType.SCALE_ASPECT_FIT);
    svr.setVisibility(0);

    return svr;
  }

  @Override
  public int setVideoEncoderConfiguration(VideoEncoderConfiguration config) {
    return nativeSetVideoEncoderConfiguration(config);
  }

  @Override
  public int addPublishStreamUrl(String url) {
    return nativeAddPublishStreamUrl(url);
  }

  @Override
  public int removePublishStreamUrl(String url) {
    return nativeRemovePublishStreamUrl(url);
  }

  @Override
  public int setLiveTranscoding(LiveTranscoding transcoding) {
    return nativeSetLiveTranscoding(transcoding);
  }

  @Override
  public int enableAudioVolumeIndication(int intervalMs) {
    return nativeEnableAudioVolumeIndication(intervalMs);
  }

  @Override
  public int joinChannel(String token, String channelName, String extraInfo, String uid) {
    Context context = this.context.get();
    if (context == null) return -1;
    doCheckPermission(context);
    if (eglBase != null) {
      eglBase.release();
      eglBase = null;
    }
    return nativeJoinChannel(token, channelName, extraInfo, uid);
  }

  @Override
  public int leaveChannel() {
    int ret = nativeLeaveChannel();
    if (eglBase != null) {
      eglBase.release();
      eglBase = null;
    }
    return ret;
  }

  @Override
  public int startLivePk(String channelName, String remoteUid) {
    return nativeStartLivePk(channelName, remoteUid);
  }

  @Override
  public int stopLivePk() {
    return nativeStopLivePk();
  }

  @Override
  public int muteLocalAudioStream(boolean mute) {
    return nativeMuteLocalAudioStream(mute);
  }

  @Override
  public int muteRemoteAudioStream(String uid, boolean mute) {
    return nativeMuteRemoteAudioStream(uid, mute);
  }

  @Override
  public int kickOut(String uid) {
    return nativeKickOut(uid);
  }

  @Override
  public String[] getBroadcasters() {
    return nativeGetBroadcasters();
  }

  @Override
  public boolean pushExternalVideoFrame(ZorroVideoFrame frame) {
    if (frame == null || frame.format != ZorroVideoFrame.FORMAT_H264 ||
        (frame.format == ZorroVideoFrame.FORMAT_H264 && !(frame.frameType == ZorroVideoFrame.FRAME_TYPE_I || frame.frameType == ZorroVideoFrame.FRAME_TYPE_P)) ||
        !frame.byteBuffer.isDirect()) {
      Logging.e(TAG, "pushExternalVideoFrame failed: invalid video frame.");
      return false;
    }
    return (nativePushExternalVideoFrame(frame) == 0);
  }

  @Override
  public int setupLocalVideo(VideoCanvas canvas) {
    if (RtcEngineImpl.checkIsOnMainThread("setupLocalVideo")) {
      if (canvas.view instanceof SurfaceViewRenderer) {
        return nativeSetLocalVideoRenderer((SurfaceViewRenderer)canvas.view); 
      } else {
        Logging.e(TAG, "setupLocalVideo failed: pls. use createRendererView to set canvas view.");
        return -1;
      }
    }
    return -1;
  }

  @Override
  public int setupRemoteVideo(VideoCanvas canvas) {
    if (RtcEngineImpl.checkIsOnMainThread("setupRemoteVideo")) {
      if (canvas.view instanceof SurfaceViewRenderer) {
        return nativeSetRemoteVideoRenderer((SurfaceViewRenderer)canvas.view, canvas.uid); 
      } else {
        Logging.e(TAG, "setupRemoteVideo failed: pls. use createRendererView to set canvas view.");
        return -1;
      }
    }
    return -1;
  }

  public static boolean checkIsOnMainThread(String action) {
    if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
      // Logging.i(TAG, action + " on main Thread");
      return true;
    }
    Logging.e(TAG, action + " not on main Thread");
    return false;
  }

  @Override
  public boolean setAudioPlayVolume(int volume) {
    return (nativeSetAudioPlayVolume(volume) == 0);
  }

  @Override
  public int sendMediaSideInfo(byte[] info) { return nativeSendMediaSideInfo(info); }

  private static native String nativeGetSdkVersion();
  private native int nativeInit(Context context, IRtcEngineObserver observer, String appId, String countryCode, String logPath);
  private native int nativeSetObserver(IRtcEngineObserver observer);
  private native int nativeSetParameters(String parameters);
  private native int nativeSetAudioSource(boolean external, int sampleRate, int channels);
  private native int nativeSetVideoSource(boolean external, int width, int height);
  private native int nativeSetChannelProfile(int profile);
  private native int nativeEnableVideo();
  private native int nativeDisableVideo();
  private native int nativeSetClientRole(int role);
  private native int nativeSetVideoEncoderConfiguration(VideoEncoderConfiguration config);
  private native int nativeAddPublishStreamUrl(String url);
  private native int nativeRemovePublishStreamUrl(String url);
  private native int nativeSetLiveTranscoding(LiveTranscoding transcoding);
  private native int nativeEnableAudioVolumeIndication(int intervalMs);
  private native int nativeJoinChannel(String token, String channelName, String extraInfo, String uid);
  private native int nativeLeaveChannel();
  private native int nativeStartLivePk(String channelName, String remoteUid);
  private native int nativeStopLivePk();
  private native int nativeMuteLocalAudioStream(boolean mute);
  private native int nativeMuteRemoteAudioStream(String uid, boolean mute);
  private native int nativeKickOut(String uid);
  private native String[] nativeGetBroadcasters();
  private native int nativePushExternalVideoFrame(ZorroVideoFrame frame);
  private native int nativeSetLocalVideoRenderer(SurfaceViewRenderer renderer);
  private native int nativeSetRemoteVideoRenderer(SurfaceViewRenderer renderer, String uid);
  private native int nativeSetAudioPlayVolume(int volume);
  private native int nativeSendMediaSideInfo(byte[] info);

}

