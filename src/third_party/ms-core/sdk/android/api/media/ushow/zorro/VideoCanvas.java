package media.ushow.zorro;

import android.view.SurfaceView;

public class VideoCanvas {
  public SurfaceView view;
  public String uid;

  public VideoCanvas(SurfaceView view) {
    this.view = view;
    this.uid = "";
  }

  public VideoCanvas(SurfaceView view, String uid) {
    this.view = view;
    this.uid = uid;
  }
}

