/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;

/**
 * font utilities.
 * Example of use:
 * <pre>
 *    FontUtil.StandardFont fontu = FontUtil.getStandardFont( 20);
 *    g2.setFont( fontu.getFont());
 * </pre>
 * @author John Caron
 */
public class FontUtil  {
//  private static GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//  private static String fontList[] = ge.getAvailableFontFamilyNames();
//  private static Font font[] = ge.getAvailableFonts();

  private static final int MAX_FONTS = 15;
  private static int fontType = Font.PLAIN;
    // standard
  private static Font [] stdFont= new Font[MAX_FONTS];    // list of fonts to use to make text bigger/smaller
  private static FontMetrics [] stdMetrics= new FontMetrics[MAX_FONTS]; // fontMetric for each font
    // mono
  private static Font [] monoFont= new Font[MAX_FONTS];    // list of fonts to use to make text bigger/smaller
  private static FontMetrics [] monoMetrics= new FontMetrics[MAX_FONTS]; // fontMetric for each font

  private static boolean debug = false;
  private static boolean isInit = false;

  private static void init() {
    if (isInit)
      return;
    initFontFamily( "SansSerif", stdFont, stdMetrics);
    initFontFamily( "Monospaced", monoFont, monoMetrics);
    isInit = true;
  }

  private static void initFontFamily( String name, Font[] fonts, FontMetrics[] fontMetrics) {
     for (int i=0; i < MAX_FONTS; i++) {
       int fontSize = i < 6 ? 5+i : (i < 11 ? 10 + 2*(i-5) : 20 + 4*(i-10));
       fonts[i] = new Font(name, fontType, fontSize);
       fontMetrics[i] = Toolkit.getDefaultToolkit().getFontMetrics( fonts[i]);

       if (debug)
         System.out.println("TextSymbol font "+ fonts[i]+" "+fontSize+ " "+ fontMetrics[i].getAscent());
     }
  }

    // gets largest font smaller than pixel_height
    public static FontUtil.StandardFont getStandardFont( int pixel_height) {
    init();
    return new StandardFont( stdFont, stdMetrics, pixel_height);
  }

    // gets largest font smaller than pixel_height
    public static FontUtil.StandardFont getMonoFont( int pixel_height) {
    init();
    return new StandardFont( monoFont, monoMetrics, pixel_height);
  }

  public static class StandardFont {
    private int currFontNo;
    private int height;
    private Font[] fonts;
    private FontMetrics[] fontMetrics;

    StandardFont( Font[] fonts, FontMetrics[] fontMetrics, int pixel_height) {
      this.fonts = fonts;
      this.fontMetrics = fontMetrics;
      currFontNo = findClosest( pixel_height);
      height = fontMetrics[ currFontNo].getAscent();
    }
    public Font getFont() { return fonts[currFontNo]; }
    public int getFontHeight() { return height; }

        /** increment the font size one "increment"  */
    public Font incrFontSize() {
      if (currFontNo < MAX_FONTS-1) {
        currFontNo++;
        this.height = fontMetrics[ currFontNo].getAscent();
      }
      return getFont();
    }

      /** decrement the font size one "increment"  */
    public Font decrFontSize() {
      if (currFontNo > 0) {
        currFontNo--;
        this.height = fontMetrics[ currFontNo].getAscent();
      }
      return getFont();
    }

    public Dimension getBoundingBox(String s) {
      return new Dimension( fontMetrics[currFontNo].stringWidth(s), height);
    }

    // gets largest font smaller than pixel_height
    private int findClosest( int pixel_height) {
      for (int i=0; i< MAX_FONTS-1; i++) {
        if (fontMetrics[i+1].getAscent() > pixel_height)
          return i;
      }
      return MAX_FONTS-1;
    }

  } // inner class StandardFont
}