package media.ushow.zorro;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.webrtc.CalledByNative;


public class LiveTranscoding {
  public int audioChannels;
  public int audioBitrateKbps;
  public int audioSampleRate;

  public int width;
  public int height;
  public int videoFramerate;
  public int videoBitrateKbps;
  public int videoGop;
  public String backgroundColor;
  public String videoBackgroundUrl;
  private Map<String, TranscodingUser> users;

  public LiveTranscoding() {
    this.audioChannels = 1;
    this.audioSampleRate = 44100;
    this.audioBitrateKbps = 48000;

    this.width = 360;
    this.height = 640;
    this.videoFramerate = 15;
    this.videoBitrateKbps = 800;
    this.videoGop = 15;
    this.backgroundColor = "";
    this.videoBackgroundUrl = "";

    this.users = new HashMap<>();
  }

  public LiveTranscoding(int audioChannels, int audioSampleRate, int audioBitrateKbps,
      int width, int height, int videoFramerate, int videoBitrateKbps, int videoGop) {
    this.audioChannels = audioChannels;
    this.audioSampleRate = audioSampleRate;
    this.audioBitrateKbps = audioBitrateKbps;

    this.width = width;
    this.height = height;
    this.videoFramerate = videoFramerate;
    this.videoBitrateKbps = videoBitrateKbps;
    this.videoGop = videoGop;
    this.backgroundColor = "";
    this.videoBackgroundUrl = "";

    this.users = new HashMap<>();
  }

  public static class TranscodingUser {
    public String uid;
    public boolean isVideo;
    public int left;
    public int top;
    public int width;
    public int height;
    public int zorder;

    public TranscodingUser(String uid, boolean isVideo, int left, int top,
                           int width, int height, int zorder) {
      if (uid == null || uid.isEmpty()) {
        throw new IllegalArgumentException("uid == null || uid.isEmpty()");
      }

      if (left < 0 || top < 0 || width <= 0 || height <= 0) {
        throw new IllegalArgumentException(
            "left < 0 || top < 0 || width <= 0 || height <= 0");
      }

      this.uid = uid;
      this.isVideo = isVideo;
      this.left = left;
      this.top = top;
      this.width = width;
      this.height = height;
      this.zorder = zorder;
    }

    @CalledByNative("TranscodingUser")
    String getUid() {
      return uid;
    }

    @CalledByNative("TranscodingUser")
    boolean getIsVideo() {
      return isVideo;
    }

    @CalledByNative("TranscodingUser")
    int getLeft() {
      return left;
    }

    @CalledByNative("TranscodingUser")
    int getTop() {
      return top;
    }

    @CalledByNative("TranscodingUser")
    int getWidth() {
      return width;
    }

    @CalledByNative("TranscodingUser")
    int getHeight() {
      return height;
    }

    @CalledByNative("TranscodingUser")
    int getZorder() {
      return zorder;
    }
  }

  @CalledByNative
  int getAudioChannel() {
    return audioChannels;
  }

  @CalledByNative
  int getAudioBitrateKbps() {
    return audioBitrateKbps;
  }

  @CalledByNative
  int getAudioSampleRate() {
    return audioSampleRate;
  }

  @CalledByNative
  int getWidth() {
    return width;
  }

  @CalledByNative
  int getHeight() {
    return height;
  }

  @CalledByNative
  int getVideoFramerate() {
    return videoFramerate;
  }

  @CalledByNative
  int getVideoBitrateKbps() {
    return videoBitrateKbps;
  }

  @CalledByNative
  int getVideoGop() {
    return videoGop;
  }

  @CalledByNative
  String getBackgroundColor() {
    return backgroundColor;
  }

  @CalledByNative
  String getVideoBackgroundUrl() {
    return videoBackgroundUrl;
  }
  
  public int addUser(TranscodingUser user) {
    if (user == null || user.uid == null || user.uid.isEmpty()) {
      return -1;
    }
    this.users.put(user.uid, user);
    return 0;
  }

  public int removeUser(String uid) {
    if (!this.users.containsKey(uid)) {
      return -1;
	}
    this.users.remove(uid);
    return 0;
  }

  public int getUserCount() {
    return this.users.size();
  }

  @CalledByNative
  public final ArrayList<TranscodingUser> getUsers() {
    Collection<TranscodingUser> collection = this.users.values();
    return new ArrayList<>(collection);
  }

}

