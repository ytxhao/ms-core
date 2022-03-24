package media.ushow.zorro;

// import android.graphics.Rect;
// import io.agora.rtc.models.UserInfo;

import org.webrtc.CalledByNative;

public abstract class IRtcEngineObserver {
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
    public String uid;
    public int    networkTransportDelayMs;
    public int    jitterBufferDelayMs;
    public int    lossRate;
    public int    e2eDelayMs;
    public int    interruptionRate;
    public int    interruptionCount;
    public long   totalInterruptionDurationMs;
    @CalledByNative("RemoteAudioStats")
    public RemoteAudioStats(String uid, int networkTransportDelayMs, int jitterBufferDelayMs,
        int lossRate, int e2eDelayMs, int interruptionRate, int interruptionCount,
        long totalInterruptionDurationMs) {
      this.uid = uid;
      this.networkTransportDelayMs = networkTransportDelayMs;
      this.jitterBufferDelayMs = jitterBufferDelayMs;
      this.lossRate = lossRate;
      this.e2eDelayMs = e2eDelayMs;
      this.interruptionRate = interruptionRate;
      this.interruptionCount = interruptionCount;
      this.totalInterruptionDurationMs = totalInterruptionDurationMs;
    }
  }

  public static class RemoteVideoStats {
    public String  uid;
    public int     width;
    public int     height;
    public int     receivedBitrateKbps;
    public int     receivedFrameRate;
    public int     frozenCounts;
    public int     frozenRate;
    public long    totalFrozenTimeMs;
    public int     e2eDelayMs;

    @CalledByNative("RemoteVideoStats")
    public RemoteVideoStats(String uid, int width, int height, int receivedBitrateKbps, int receivedFrameRate, int frozenCounts, int frozenRate, long totalFrozenTimeMs, int e2eDelayMs) {
      this.uid = uid;
      this.width = width;
      this.height = height;
      this.receivedBitrateKbps = receivedBitrateKbps;
      this.receivedFrameRate = receivedFrameRate;
      this.frozenCounts = frozenCounts;
      this.frozenRate = frozenRate;
      this.totalFrozenTimeMs = totalFrozenTimeMs;
      this.e2eDelayMs = e2eDelayMs;
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

  @CalledByNative
  public void onJoinChannelSuccess(String channel, String uid, int elapsed) {}

  @CalledByNative
  public void onStartLivePkSuccess(String channel, String uid, int elapsed) {}

  @CalledByNative
  public void onLeaveChannel() {}

  @CalledByNative
  public void onUserJoined(String uid, int elapsed, String extraInfo) {}

  @CalledByNative 
  public void onUserOffline(String uid) {}

  @CalledByNative
  public void onFirstRemoteAudioFrame(String uid) {}

  @CalledByNative
  public void onFirstRemoteVideoFrame(String uid) {}

  @CalledByNative
  public void onFirstRemoteVideoFrameRendered(String uid) {}

  @CalledByNative
  public void onAudioVolumeIndication(AudioVolumeInfo[] speakers) {}

  @CalledByNative
  public void onRtcStats(RtcStats stats) {}

  @CalledByNative
  public void onError(int code, String text) {}

  @CalledByNative
  public void onVideoKeyFrameRequest() {}

  @CalledByNative
  public void onVideoEncodingRateSet(int bitrateKbps, int frameRate) {}

  @CalledByNative
  public void onVideoSEIData(int type, byte[] payload) {}

  @CalledByNative
  public void onMediaSideInfo(String uid, byte[] data) {}

  @CalledByNative
  public void onConnectionStateChanged(int state, int reason) {}
}


