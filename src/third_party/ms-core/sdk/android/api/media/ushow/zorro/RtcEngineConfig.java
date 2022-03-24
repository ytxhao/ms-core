package media.ushow.zorro;

import android.content.Context;

public class RtcEngineConfig {
  public Context context;

  public IRtcEngineObserver observer;
  
  public String appId;
  
  public String countryCode;

  public RtcEngineConfig(Context context, IRtcEngineObserver observer, String appId, String countryCode) {
    this.context = context;  
    this.observer = observer;
    this.appId = appId;
    this.countryCode = countryCode;
  }
}
