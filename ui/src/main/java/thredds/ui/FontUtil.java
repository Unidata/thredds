// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.ui;

import java.awt.*;

/**
 * font utilities.
 * Example of use:
 * <pre>
 *    FontUtil.StandardFont fontu = FontUtil.getStandardFont( 20);
 *    g2.setFont( fontu.getFont());
 * </pre>
 * @author John Caron
 * @version $Id$
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

  static private void init() {
    if (isInit)
      return;
    initFontFamily( "SansSerif", stdFont, stdMetrics);
    initFontFamily( "Monospaced", monoFont, monoMetrics);
    isInit = true;
  }

  static private void initFontFamily( String name, Font[] fonts, FontMetrics[] fontMetrics) {
     for (int i=0; i < MAX_FONTS; i++) {
       int fontSize = i < 6 ? 5+i : (i < 11 ? 10 + 2*(i-5) : 20 + 4*(i-10));
       fonts[i] = new Font(name, fontType, fontSize);
       fontMetrics[i] = Toolkit.getDefaultToolkit().getFontMetrics( fonts[i]);

       if (debug)
         System.out.println("TextSymbol font "+ fonts[i]+" "+fontSize+ " "+ fontMetrics[i].getAscent());
     }
  }

    // gets largest font smaller than pixel_height
  static public FontUtil.StandardFont getStandardFont( int pixel_height) {
    init();
    return new StandardFont( stdFont, stdMetrics, pixel_height);
  }

    // gets largest font smaller than pixel_height
  static public FontUtil.StandardFont getMonoFont( int pixel_height) {
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

/* Change History:
   $Log: FontUtil.java,v $
   Revision 1.3  2004/09/24 03:26:33  caron
   merge nj22

   Revision 1.2  2003/05/29 23:03:28  john
   minor

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.1.1.1  2002/02/15 00:01:48  caron
   import sources

*/
