// $Id: FontUtil.java 50 2006-07-12 16:30:06Z caron $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
 * @version $Id: FontUtil.java 50 2006-07-12 16:30:06Z caron $
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
