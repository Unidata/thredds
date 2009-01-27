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
package ucar.nc2.iosp.adde;

import ucar.ma2.*;
import ucar.nc2.dt.image.ImageArrayAdapter;
import java.util.*;

public class AddeImage {
  private static int initialCapacity = 100;
  private static LinkedHashMap hash = new LinkedHashMap(initialCapacity, 0.75f, true); // LRU cache
  private static double memUsed = 0.0; // bytes
  private static int maxCacheSize = 30; // MB
  private static boolean debugCache = false;

  public static AddeImage factory( String urlName) throws java.io.IOException, java.net.MalformedURLException {
    //debugCache = Debug.isSet("ADDE/AddeImage/ShowCache");
    AddeImage image = (AddeImage) hash.get( urlName);
    if (image == null) {
      if (debugCache) System.out.println("ADDE/AddeImage/ShowCache: cache miss "+urlName);
      image = new AddeImage( urlName);
      hash.put( urlName, image);
      memUsed += image.getSize();
      adjustCache();
    } else {
      if (debugCache) System.out.println("ADDE/AddeImage/ShowCache: cache hit "+urlName);
    }
    if (debugCache) System.out.println("  memUsed = "+(memUsed*1.e-6)+" Mb");
    return image;
  }

  public static int getMaxCacheSize() { return maxCacheSize; }
  public static void setMaxCacheSize( int max) { maxCacheSize = max; }
  public static int getCacheSize() { return (int) (memUsed*1.e-6); }

  private static void adjustCache() {
    double max = maxCacheSize * 1.e6;
    if (memUsed <= max) return;

    Iterator iter = hash.values().iterator();
    while (iter.hasNext() && (memUsed > max)) {
      AddeImage image = (AddeImage) iter.next();
      memUsed -= image.getSize();
      if (debugCache) System.out.println("  remove = "+image.getName()+" size= "+
        (image.getSize()*1.e-6)+"  memUsed = "+(memUsed*1.e-6));
      iter.remove();
    }

  }

  //////////////////////////////////////////////////////////////////////////////////

  private String urlName;
  private int nelems = 0, nlines = 0;
  private java.awt.image.BufferedImage image = null;
  private Array ma;
  private boolean debug = false;

  public AddeImage( String urlName) throws java.io.IOException, java.net.MalformedURLException {
    this.urlName = urlName;

    long timeStart = System.currentTimeMillis();
    debug = false; // Debug.isSet("ADDE/AddeImage/MA");

    AreaFile3 areaFile2 = new AreaFile3( urlName);
    ma = areaFile2.getData();

    if (ma.getRank() == 3)
      ma = ma.slice( 0, 0); // we need 2D

    nlines = ma.getShape()[0];
    nelems = ma.getShape()[1];
    long timeEnd = System.currentTimeMillis();
    // if (Debug.isSet("ADDE/AddeImage/createTiming")) System.out.println("ADDE/AddeImage/createTiming AddeImage = "+ .001*(timeEnd - timeStart)+" sec");
  }

  private long getSize() { return getSize( ma); }

  private long getSize( Array ma) {
    long size = ma.getSize();
    java.lang.Class maType = ma.getElementType();
    if (maType == byte.class) return size;
    if (maType == short.class) return 2*size;
    if (maType == int.class) return 4*size;
    if (maType == float.class) return 4*size;
    if (maType == double.class) return 8*size;
    throw new IllegalArgumentException("DataBufferMultiArray ma has illegal data type = "+maType.getName());
  }

  public java.awt.Dimension getPreferredSize() {
    return new java.awt.Dimension(nelems, nlines);
  }

  public java.awt.image.BufferedImage getImage() {
    if (image == null) {
      image = ImageArrayAdapter.makeGrayscaleImage( ma);
    }
    return image;
  }

  public String getName() {
    return urlName;
  }

      /** System-triggered redraw.
  public void paintComponent(Graphics g) {
    if (currentImage == null) return;
    if (debug) System.out.println(" paintComponent = "+currentImage.getWidth()+" "+currentImage.getHeight());

    g.drawImage( currentImage, 0, 0, new java.awt.image.ImageObserver() {
      public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        return true;
      }
    });
  }


  public void clear() {
    Graphics2D g = (Graphics2D) getGraphics();
    if (g == null) return;
    Rectangle bounds = getBounds();
    g.setBackground(Color.white);
    g.clearRect(0, 0, bounds.width, bounds.height);
    g.dispose();
  } */



  private class LRUCache extends java.util.LinkedHashMap {
    public LRUCache(int maxsize) {
	    super(maxsize*4/3 + 1, 0.75f, true);
	    this.maxsize = maxsize;
    }
    protected int maxsize;
    protected boolean removeEldestEntry() { return size() > maxsize; }
  }


}

