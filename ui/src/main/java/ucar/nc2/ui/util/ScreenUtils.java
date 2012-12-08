package ucar.nc2.ui.util;

import java.awt.*;

/**
 * Description
 *
 * @author John
 * @since 12/8/12
 */
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

}
