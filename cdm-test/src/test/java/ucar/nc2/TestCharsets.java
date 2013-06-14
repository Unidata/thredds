package ucar.nc2;

import org.junit.Test;

import java.awt.*;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Describe
 *
 * @author caron
 * @since 6/13/13
 */
public class TestCharsets {

  @Test
  public void testCharsets() {
    Map<String,Charset> map = Charset.availableCharsets();
    for (String key : map.keySet()) {
      Charset cs = map.get(key);
      System.out.println(" "+cs);
    }
    System.out.println("default= "+Charset.defaultCharset());

    System.out.println("\nFont names:");
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (String s : env.getAvailableFontFamilyNames()) {
      System.out.println(" "+s);
    }

    int c1 = 0x1f73;

    System.out.println("\nFonts:");
    for (Font f: env.getAllFonts()) {
      f.canDisplay(c1);
      System.out.println(f.canDisplay(c1)+" "+f.getFontName());
    }
  }


}
