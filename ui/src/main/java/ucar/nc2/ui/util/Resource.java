/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.util;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.ImageIcon;

/**
  Cover for fetching files using Class.getResource().
  If the application is started from a jar file, then it looks in there.
  If the application is started from a class file, then it looks in the
  disk file heirarchy, using directories in the classpath as root(s).

  1. Filenames.
  use "/" as path seperator when embedded in a string. This works on both Windows and Unix.

  2. Filepath.
  Use a resourcePath starting with a forward slash. getResource() then searches relative to the classpath.
    otherwise it will search reletive to ucar.nc2.ui.util, which is probably not what you want.
*/

public class Resource {

  static Class cl = Resource.class;
  static private boolean debug = false, debugIcon = false;

  /** Get a gif file, make it into an ImageIcon.
    @param fullIconName full path name of gif file (use forward slashes!)
    @param errMsg print err message on failure
    @return ImageIcon or null on failure
  */
  public static ImageIcon getIcon( String fullIconName, boolean errMsg) {
    ImageIcon icon = null;

    java.net.URL iconR = cl.getResource(fullIconName);
    if (debugIcon) {
      System.out.println("classLoader "+cl.getClassLoader());
      System.out.println("  Resource.getIcon on "+fullIconName+" = "+iconR);
    }

    if (iconR != null)
      icon = new ImageIcon(iconR);

    if ((icon == null) && errMsg) System.out.println("  ERROR: Resource.getIcon failed on "+fullIconName);
    else if (debugIcon) System.out.println("  Resource.getIcon ok on "+fullIconName);

    return icon;
  }

  /** Get a gif file, make it into an Image.
    @param fullImageName full path name of gif file (use forward slashes!)
    @return Image or null on failure
  */
  public static Image getImage( String fullImageName) {
    Image image = null;
    java.net.URL url = cl.getResource(fullImageName);
    if (url != null)
      image = Toolkit.getDefaultToolkit().createImage(url);
    if (image == null) System.out.println("  ERROR: Resource.getImageResource failed on "+fullImageName);
    return image;
  }

  /** Get a file as a URL
    @param filename full path name of file (use forward slashes!)
    @return URL or null on failure
  */
  public static java.net.URL getURL( String filename) {
    return cl.getResource(filename);
  }

  /** Get a gif file, make it into a Cursor.
    @param name full path name of gif file (use forward slashes!)
    @return Cursor or null on failure
  */
  public static Cursor makeCursor( String name) {
    Image image = getImage(name);
    if (null == image)
      return null;

    Cursor cursor;
    try {
      Toolkit tk = Toolkit.getDefaultToolkit();
      if (debug) {
        ImageObserver obs = new ImageObserver() {
          public boolean imageUpdate(Image image, int flags, int x, int y, int width, int height) {
            return true;
          }
        };
        System.out.println(" bestCursorSize = "+ tk.getBestCursorSize(image.getWidth(obs), image.getHeight(obs)));
        System.out.println(" getMaximumCursorColors = "+ tk.getMaximumCursorColors());
      }
      cursor = tk.createCustomCursor(image, new Point(17,17), name);
    } catch (IndexOutOfBoundsException e) {
      System.out.println("NavigatedPanel createCustomCursor failed " + e);
      return null;
    }
    return cursor;
  }

  /** Open a resource as a Stream. First try ClassLoader.getResourceAsStream().
   *  If that fails, try a plain old FileInputStream().
   * @param resourcePath name of file path (use forward slashes!)
   * @return InputStream or null on failure
  */

  public static InputStream getFileResource( String resourcePath) {

    InputStream is = cl.getResourceAsStream(resourcePath);
    if (is != null) {
      if (debug) System.out.println("Resource.getResourceAsStream ok on "+resourcePath);
      return is;
    } else if (debug)
      System.out.println("Resource.getResourceAsStream failed on ("+resourcePath+")");

    try {
      is =  new FileInputStream(resourcePath);
      if (debug) System.out.println("Resource.FileInputStream ok on "+resourcePath);
    } catch (FileNotFoundException e) {
      if (debug)  System.out.println("  FileNotFoundException: Resource.getFile failed on "+resourcePath);
    } catch (java.security.AccessControlException e) {
      if (debug)  System.out.println("  AccessControlException: Resource.getFile failed on "+resourcePath);
    }

    return is;
  }

  // test
  public static void main(String[] args) throws IOException {
    System.out.println("java.class.path = "+ System.getProperty("java.class.path"));
    System.out.println("Class = "+ cl);
    System.out.println("Class Loader = "+ cl.getClassLoader());

    try (InputStream is = getFileResource("/ucar.unidata.util/Resource.java")) {}
    try (InputStream is = getFileResource("Resource.java")) {}
    try (InputStream is = getFileResource("test/test/Resource.java")) {}
  }

}