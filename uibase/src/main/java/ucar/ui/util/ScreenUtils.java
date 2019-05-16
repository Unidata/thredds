package ucar.ui.util;

import java.awt.*;
import javax.swing.JFrame;

public class ScreenUtils {
  private static Rectangle virtualBounds = null;

  public static Rectangle getScreenVirtualSize() {
    if (virtualBounds == null) {
      virtualBounds = new Rectangle();
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice[] gs = ge.getScreenDevices();
      for (GraphicsDevice gd : gs) {
        GraphicsConfiguration[] gc = gd.getConfigurations();
        for (GraphicsConfiguration aGc : gc)
          virtualBounds = virtualBounds.union(aGc.getBounds());
      }
    }

    return virtualBounds;
  }

  // thanks to Heinz M. Kabutz
  public static Frame findActiveFrame() {
    Frame[] frames = JFrame.getFrames();
    for (Frame frame : frames) {
      if (frame.isVisible())
        return frame;
    }
    return null;
  }

}
