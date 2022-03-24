package media.ushow.zorro;

import org.webrtc.CalledByNative;

import java.nio.ByteBuffer;

public class ZorroVideoFrame {
  public static final int FORMAT_NONE = -1;
  public static final int FORMAT_H264 = 0;
  public static final int FRAME_TYPE_I = 0; // Use FORMAT_H264
  public static final int FRAME_TYPE_P = 1; // Use FORMAT_H264
  public static final int ENCODER_TYPE_UNKNOWN = -1;
  public static final int ENCODER_TYPE_HARDWARE = 0;
  public static final int ENCODER_TYPE_OPENH264 = 1;
  public int format;
  public int frameType;
  public int height;
  public int stride;
  public long timestamp;
  public ByteBuffer byteBuffer; // Note: MUST BE a direct byte buffer
  public int encoderType; // -1: unknown; 0: hardware; 1: OpenH264;

  public ZorroVideoFrame(ByteBuffer byteBuffer, int format, int frameType, long timestamp, int encoderType) {
    this.byteBuffer = byteBuffer;
    // this.stride = stride;
    // this.height = height;
    this.format = format;
    this.frameType = frameType;
    this.timestamp = timestamp;
    this.encoderType = encoderType;
  }
  // @CalledByNative
  // int getFormat() {
    // return format;
  // }

  @CalledByNative
  int getFrameType() {
    return frameType;
  }
 
  // @CalledByNative
  // int getHeight() {
    // return height;
  // }

  // @CalledByNative
  // int getStride() {
    // return stride;
  // }

  @CalledByNative
  long getTimestamp() {
    return timestamp;
  }

  @CalledByNative
  ByteBuffer getByteBuffer() {
    return byteBuffer;
  }

  @CalledByNative
  int getEncoderType() {
    return encoderType;
  }
}
