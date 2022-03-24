package media.ushow.zorro;

import android.content.Context;
import android.view.SurfaceView;
import android.support.annotation.Nullable;

import media.ushow.zorro.RtcEngineConfig;
import media.ushow.zorro.ZorroVideoFrame;
import media.ushow.zorro.VideoCanvas;

import org.webrtc.CalledByNative;


public abstract class RtcEngine {
  private static @Nullable RtcEngineImpl instance;
  
  public static @Nullable RtcEngine create(RtcEngineConfig config) throws Exception {
    if (config.context == null || !RtcEngineImpl.initializeNativeLibs()) {
      return null;
    }
    synchronized (RtcEngineImpl.class) {
      if (instance == null) {
        instance = new RtcEngineImpl(config);
      } else {
        instance.reinitialize(config);
      }
    }
    return (RtcEngine)instance;
  }

  public static void destroy() {
    synchronized (RtcEngineImpl.class)  {
      if (instance != null) {
        instance.doDestroy();
        instance = null;
        System.gc();
      }
    }
  }

  public static String getSdkVersion() {
    return RtcEngineImpl.getSdkVersion();
  }

  public static @Nullable SurfaceView createRendererView(Context context) {
    return RtcEngineImpl.createRendererView(context);
  }

  public abstract int setObserver(IRtcEngineObserver observer);
  public abstract int setAudioSource(boolean external, int sampleRate, int channels);
  public abstract int setVideoSource(boolean external, int width, int height);
  public abstract int setChannelProfile(int profile); // MUST BE called before @setClientRole
  public abstract int enableVideo();
  public abstract int disableVideo();
  public abstract int setVideoEncoderConfiguration(VideoEncoderConfiguration config);
  public abstract int addPublishStreamUrl(String url);
  public abstract int removePublishStreamUrl(String url);
  public abstract int setParameters(String parameters); // codec, audio, mode
  public abstract int enableAudioVolumeIndication(int intervalMs);
  public abstract int joinChannel(String token, String channelName, String extraInfo, String uid);
  public abstract int setLiveTranscoding(LiveTranscoding transcoding);
  public abstract int setClientRole(int role);
  public abstract int setupLocalVideo(VideoCanvas canvas);
  public abstract int setupRemoteVideo(VideoCanvas canvas);
  public abstract boolean pushExternalVideoFrame(ZorroVideoFrame frame);
  public abstract int muteLocalAudioStream(boolean mute);
  public abstract int muteRemoteAudioStream(String uid, boolean mute);
  public abstract int kickOut(String uid);
  public abstract String[] getBroadcasters();
  public abstract int leaveChannel();
  public abstract int startLivePk(String channelName, String remoteUid); // PK with @remoteUid in @channelName
  public abstract int stopLivePk();
  public abstract boolean setAudioPlayVolume(int volume); // @volume: 0-200, default 100
  public abstract int sendMediaSideInfo(byte[] info);

}

