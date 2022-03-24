package media.ushow.zorro;

import org.webrtc.CalledByNative;

public class VideoEncoderConfiguration {
  public int bitrateKbps;
  public int minBitrateKbps;
  public int frameRate;
  public int minFrameRate;

  public VideoEncoderConfiguration(int bitrateKbps, int minBitrateKbps, int frameRate, int minFrameRate) {
    this.bitrateKbps = bitrateKbps;
    this.minBitrateKbps = minBitrateKbps;
    this.frameRate = frameRate;
    this.minFrameRate = minFrameRate;
  }

  @CalledByNative
  int getBitrateKbps() {
    return bitrateKbps;
  }

  @CalledByNative
  int getMinBitrateKbps() {
    return minBitrateKbps;
  }
 
  @CalledByNative
  int getFrameRate() {
    return frameRate;
  }

  @CalledByNative
  int getMinFrameRate() {
    return minFrameRate;
  }
}
