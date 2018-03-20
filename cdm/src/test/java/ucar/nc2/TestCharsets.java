/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * test available charsets
 *
 * @author caron
 * @since 6/13/13
 */
public class TestCharsets {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
