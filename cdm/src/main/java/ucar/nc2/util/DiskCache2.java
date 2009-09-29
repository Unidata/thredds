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
package ucar.nc2.util;

import ucar.unidata.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.PrintStream;
import java.util.*;
import java.net.URLDecoder;

/**
 * Manages a place on disk to persistently cache files, which are deleted when the last modified date exceeds a certain time.
 * This starts up a thread to periodically scour itself; be sure to call exit() to terminate the thread.
 *
 * <p> Each DiskCache has a "root directory", which may be set as an absolute path, or reletive to the
 * DiskCache "home directory". The root directory must be writeable.
 *
 * The DiskCache home directory is set in the following order: <ol>
 * <li>  through the system property "nj22.cachePersistRoot" if it exists
 * <li>  through the system property "user.home" if it exists
 * <li>  through the system property "user.dir" if it exists
 * <li>  to the current working directory
 * </ol>
 * @author jcaron
 */
public class DiskCache2 {
  public static int CACHEPATH_POLICY_ONE_DIRECTORY = 0;
  public static int CACHEPATH_POLICY_NESTED_DIRECTORY = 1;
  public static int CACHEPATH_POLICY_NESTED_TRUNCATE = 2;

  private int cachePathPolicy;
  private String cachePathPolicyParam = null;

  private String root = null;
  private int persistMinutes;
  private Timer timer;
  private org.slf4j.Logger cacheLog = org.slf4j.LoggerFactory.getLogger("cacheLogger");

  /**
   * Create a cache on disk.
   * @param root the root directory of the cache. Must be writeable.
   * @param reletiveToHome if the root directory is reletive to the cache home directory.
   * @param persistMinutes  a file is deleted if its last modified time is greater than persistMinutes
   * @param scourEveryMinutes how often to run the scour process. If <= 0, dont scour.
   */
  public DiskCache2(String root, boolean reletiveToHome, int persistMinutes, int scourEveryMinutes) {
    this.persistMinutes = persistMinutes;

    if (reletiveToHome) {
      String home = System.getProperty("nj22.cachePersistRoot");

      if (home == null)
        home = System.getProperty("user.home");

      if (home == null)
        home = System.getProperty("user.dir");

      if (home == null)
        home = ".";

      if (!home.endsWith("/"))
        home = home + "/";

      root = home + root;
    }
    setRootDirectory(root);

    if (scourEveryMinutes > 0) {
      timer = new Timer();
      Calendar c = Calendar.getInstance(); // contains current startup time
      c.add(Calendar.MINUTE, scourEveryMinutes);
      timer.scheduleAtFixedRate(new CacheScourTask(), c.getTime(), (long) 1000 * 60 * scourEveryMinutes);
    }

  }

  /** Be sure to call this when your application exits, otherwise the scour thread may not die. */
  public void exit() {
    if (timer != null)
      timer.cancel();
  }

  /** Optionally set a logger. Each time a scour is done, a messsage is written to it.
   * @param cacheLog use this logger
   */
  public void setLogger(org.slf4j.Logger cacheLog) {
    this.cacheLog = cacheLog;
  }

  /*
   * Set the cache root directory. Create it if it doesnt exist.
   * Normally this is set in the Constructor.
   * @param cacheDir the cache directory
   */
  public void setRootDirectory(String cacheDir) {
    if (!cacheDir.endsWith("/"))
      cacheDir = cacheDir + "/";
    root = StringUtil.replace(cacheDir, '\\', "/"); // no nasty backslash

    File dir = new File(root);
    dir.mkdirs();
    if (!dir.exists()) {
      cacheLog.error("Failed to create directory "+root);
    }
  }

  /**
   * Get the cache root directory.
   * @return the cache root directory.
   */
   public String getRootDirectory() { return root; }

  /**
   * Get a File in the cache, corresponding to the fileLocation.
   * File may or may not exist.
   * If fileLocation has "/" in it, and cachePathPolicy == CACHEPATH_POLICY_NESTED_DIRECTORY, the
   * nested directories will be created.
   *
   * @param fileLocation logical file location
   * @return physical File in the cache.
   */
  public File getCacheFile(String fileLocation) {
    File f = new File(makeCachePath(fileLocation));
    //if (f.exists())
    // f.setLastModified( System.currentTimeMillis());

    if (cachePathPolicy == CACHEPATH_POLICY_NESTED_DIRECTORY) {
      File dir = f.getParentFile();
      dir.mkdirs();
    }

    return f;
  }

  /**
   * Set the cache path policy
   * @param cachePathPolicy one of:
   *   CACHEPATH_POLICY_ONE_DIRECTORY (default) : replace "/" with "-", so all files are in one directory.
   *   CACHEPATH_POLICY_NESTED_DIRECTORY: cache files are in nested directories under the root.
   *   CACHEPATH_POLICY_NESTED_TRUNCATE: eliminate leading directories
   *
   * @param cachePathPolicyParam for CACHEPATH_POLICY_NESTED_TRUNCATE, eliminate this string
   */
  public void setCachePathPolicy(int cachePathPolicy, String cachePathPolicyParam) {
    this.cachePathPolicy = cachePathPolicy;
    this.cachePathPolicyParam = cachePathPolicyParam;
  }

  /**
   * Set the cache path policy
   * @param cachePathPolicy one of CACHEPATH_POLICY__XXXX
   */
  public void setPolicy(int cachePathPolicy) {
    this.cachePathPolicy = cachePathPolicy;
  }

  /**
   * Make the cache filename
   * @param fileLocation normal file location
   * @return cache filename
   */
  private String makeCachePath(String fileLocation) {

    // remove ':', '?', '=', replace '\' with '/', leading or trailing '/'
    String cachePath = StringUtil.remove(fileLocation, ':');
    cachePath = StringUtil.remove(cachePath, '?');
    cachePath = StringUtil.remove(cachePath, '=');
    cachePath = StringUtil.replace(cachePath, '\\', "/");
    if (cachePath.startsWith("/"))
      cachePath = cachePath.substring(1);
    if (cachePath.endsWith("/"))
      cachePath = cachePath.substring(0, cachePath.length()-1);

    // remove directories
    if (cachePathPolicy == CACHEPATH_POLICY_ONE_DIRECTORY) {
      cachePath = StringUtil.replace(cachePath, '/', "-");
    }

    // eliminate leading directories
    else if (cachePathPolicy == CACHEPATH_POLICY_NESTED_TRUNCATE) {
      int pos = cachePath.indexOf(cachePathPolicyParam);
      if (pos >= 0)
        cachePath = cachePath.substring(pos+cachePathPolicyParam.length());
      if (cachePath.startsWith("/"))
        cachePath = cachePath.substring(1);
    }

    // make sure the parent directory exists
    if (cachePathPolicy != CACHEPATH_POLICY_ONE_DIRECTORY) {
      File file = new File(root + cachePath);
      File parent = file.getParentFile();
      if (!parent.exists())
        parent.mkdirs();
    }

    return root + cachePath;
  }

  /**
   * Show cache contents.
   * @param pw write to this PrintStream.
   */
  public void showCache(PrintStream pw) {
    pw.println("Cache files");
    pw.println("Size   LastModified       Filename");
    File dir = new File(root);
    File[] files = dir.listFiles();
    for (File file : files) {
      String org = null;
      try {
        org = URLDecoder.decode(file.getName(), "UTF8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      pw.println(" " + file.length() + " " + new Date(file.lastModified()) + " " + org);
    }
  }

  /**
   * Remove any files or directories whose last modified time greater than persistMinutes
   * @param dir clean starting here
   * @param sbuff status messages here, may be null
   * @param isRoot delete empty directories, bit not root directory
   */
  public void cleanCache(File dir, StringBuilder sbuff, boolean isRoot) {
    long now = System.currentTimeMillis();
    File[] files = dir.listFiles();
    if (files == null) {
      throw new IllegalStateException( "DiskCache2: not a directory or I/O error on dir="+dir.getAbsolutePath());
    }

    // check for empty directory
    if (!isRoot && (files.length == 0)) {
      long duration = now - dir.lastModified();
      duration /= 1000 * 60; // minutes
      if (duration > persistMinutes) {
        dir.delete();
        if (sbuff != null)
          sbuff.append(" deleted ").append(dir.getPath()).append(" last= ").append(new Date(dir.lastModified())).append("\n");
      }
      return;
    }

    // check for expired files
    for (File file : files) {
      if (file.isDirectory()) {
        cleanCache(file, sbuff, false);
      } else {
        long duration = now - file.lastModified();
        duration /= 1000 * 60; // minutes
        if (duration > persistMinutes) {
          file.delete();
          if (sbuff != null)
            sbuff.append(" deleted ").append(file.getPath()).append(" last= ").append(new Date(file.lastModified())).append("\n");
        }
      }
    }
  }

  private class CacheScourTask extends TimerTask {
    CacheScourTask( ) { }

    public void run() {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("CacheScourTask on").append(root).append("\n");
      cleanCache( new File(root), sbuff, true);
      sbuff.append("----------------------\n");
      if (cacheLog != null) cacheLog.info(sbuff.toString());
    }
  }

  /** debug */
  static void make(DiskCache2 dc, String filename) throws IOException {
    File want = dc.getCacheFile(filename);
    System.out.println("make=" + want.getPath() + "; exists = " + want.exists());
    if (!want.exists())
      want.createNewFile();
    System.out.println(" canRead= " + want.canRead() + " canWrite = " + want.canWrite() + " lastMod = " + new Date(want.lastModified()));

    try {
      String enc = java.net.URLEncoder.encode(filename, "UTF8");
      System.out.println(" original=" + java.net.URLDecoder.decode(enc, "UTF8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace(); 
    }
  }

  /** debug */
  static public void main(String[] args) throws IOException {
    DiskCache2 dc = new DiskCache2("C:/TEMP/test/", false, 0, 0);

    dc.setRootDirectory("C:/temp/chill/");
    make(dc, "C:/junk.txt");
    make(dc, "C:/some/enchanted/evening/joots+3478.txt");
    make(dc, "http://www.unidata.ucar.edu/some/enc hanted/eve'ning/nowrite.gibberish");

    dc.showCache( System.out);
    StringBuilder sbuff = new StringBuilder();
    dc.cleanCache(new File( dc.getRootDirectory()), sbuff, true);
    System.out.println(sbuff);
  }

}
