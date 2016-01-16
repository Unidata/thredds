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

import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.*;

/**
 * Manages a place on disk to persistently cache files, which are deleted when the last modified date exceeds a certain time.
 * This starts up a thread to periodically scour itself; be sure to call exit() to terminate the thread.
 *
 * <p> Each DiskCache has a "root directory", which may be set as an absolute path, or reletive to the
 * DiskCache "home directory". The root directory must be writeable.
 *
 * The DiskCache home directory is set in the following order: <ol>
 * <li>  through the system property "nj22.cache" if it exists
 * <li>  through the system property "user.home" if it exists
 * <li>  through the system property "user.dir" if it exists
 * <li>  to the current working directory
 * </ol>
 * @author jcaron
 */
public class DiskCache2 {

  public enum CachePathPolicy {
    OneDirectory,
    NestedDirectory,
    NestedTruncate }

  private static org.slf4j.Logger cacheLog = org.slf4j.LoggerFactory.getLogger("cacheLogger");

  private CachePathPolicy cachePathPolicy = CachePathPolicy.NestedDirectory;
  private boolean alwaysUseCache = false;
  private boolean neverUseCache = false;
  private String cachePathPolicyParam = null;

  private String root;
  private int persistMinutes, scourEveryMinutes;
  private Timer timer;
  private boolean fail = false;

  /**
   * Default DiskCache2 strategy: use $user_home/.unidata/cache/, no scouring, alwaysUseCache = false
   * Mimics default DiskCache static class
   */
  static public DiskCache2 getDefault() {
    String root = System.getProperty("nj22.cache");

    if (root == null) {
      String home = System.getProperty("user.home");

      if (home == null)
        home = System.getProperty("user.dir");

      if (home == null)
        home = ".";

      root = home + "/.unidata/cache/";
    }

    DiskCache2 result = new DiskCache2();
    result.setRootDirectory(root);
    result.alwaysUseCache = false;
    return result;
  }

  // NOOP
  static public DiskCache2 getNoop() {
    DiskCache2 noop = new DiskCache2();
    noop.neverUseCache = true;
    return noop;
  }

  private DiskCache2() {}
  /**
   * Create a cache on disk. Use default policy (CachePathPolicy.NestedDirectory)
   * @param root the root directory of the cache. Must be writeable.
   * @param reletiveToHome if the root directory is relative to the cache home directory.
   * @param persistMinutes  a file is deleted if its last modified time is greater than persistMinutes
   * @param scourEveryMinutes how often to run the scour process. If <= 0, don't scour.
   */
  public DiskCache2(String root, boolean reletiveToHome, int persistMinutes, int scourEveryMinutes) {
    this.persistMinutes = persistMinutes;
    this.scourEveryMinutes = scourEveryMinutes;

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

    if (!fail && scourEveryMinutes > 0) {
      timer = new Timer("DiskCache-"+root);
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
    root = StringUtil2.replace(cacheDir, '\\', "/"); // no nasty backslash

    File dir = new File(root);
    if (!dir.mkdirs()) {
      // ok
    }
    if (!dir.exists()) {
      fail = true;
      cacheLog.error("DiskCache2 failed to create directory "+root);
    } else {
      cacheLog.debug("DiskCache2 create directory "+root);
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
   * If fileLocation has "/" in it, and cachePathPolicy == NestedDirectory, the
   * nested directories will be created.
   *
   * @param fileLocation logical file location
   * @return physical File in the cache.
   */
  public File getCacheFile(String fileLocation) {
    if (neverUseCache)
      return null;

    if (!alwaysUseCache) {
      File f = new File(fileLocation);
      if (canWrite(f)) return f;
    }

    File f = new File(makeCachePath(fileLocation));
    //if (f.exists())
    // f.setLastModified( System.currentTimeMillis());

    if (cachePathPolicy == CachePathPolicy.NestedDirectory) {
      File dir = f.getParentFile();
      if (!dir.exists()) {
        boolean ret = dir.mkdirs();
        if (!ret) cacheLog.warn("Error creating dir: " + dir);
      }
    }

    return f;
  }

  /**
   * Get the named File.
   * If exists or isWritable, return it.
   * Otherwise get corresponding file in the cache directory.
   *
   * If fileLocation has "/" in it, and cachePathPolicy == NestedDirectory, the
   * nested directories will be created.
   *
   * @param fileLocation logical file location
   * @return physical File as named, or in the cache.
   */
  public File getFile(String fileLocation) {
    if (!alwaysUseCache) {
      File f = new File(fileLocation);
      if (f.exists()) return f;
      if (canWrite(f)) return f;
    }

    if (neverUseCache) {
      throw new IllegalStateException("neverUseCache=true, but file does not exist and directory is not writeable ="+fileLocation);
    }

    File f = new File(makeCachePath(fileLocation));
    if (cachePathPolicy == CachePathPolicy.NestedDirectory) {
      File dir = f.getParentFile();
      if (!dir.exists() && !dir.mkdirs())
        cacheLog.warn("Cant create directories for file "+dir.getPath());
    }

    return f;
  }

  /**
   * Returns {@code true} if we can write to the file.
   *
   * @param f a file. It may be a regular file or a directory. It may even be non-existent, in which case the
   *          writability of the file's parent dir is tested.
   * @return {@code true} if we can write to the file.
   */
  public static boolean canWrite(File f) {
    Path path = f.toPath().toAbsolutePath();

    try {
      if (Files.isDirectory(path)) {
        // Try to create a file within the directory to determine if it's writable.
        Files.delete(Files.createTempFile(path, "check", null));
      } else if (Files.isRegularFile(path)) {
        // Try to open the file for writing in append mode.
        Files.newOutputStream(path, StandardOpenOption.APPEND).close();
      } else {
        // File does not exist. See if it's parent directory exists and is writeable.
        Files.delete(Files.createTempFile(path.getParent(), "check", null));
      }
    } catch (IOException | SecurityException e) {
      return false;
    }

    return true;
  }

  /**
   * Looking for an existing file, in cache or no
   * @param fileLocation the original name
   * @return existing file if you can find it
   */
  public File getExistingFileOrCache(String fileLocation) {
    File f = new File(fileLocation);
    if (f.exists()) return f;

    if (neverUseCache) return null;

    File fc = new File(makeCachePath(fileLocation));
    if (fc.exists()) return fc;

    return null;
  }

  /**
   * Create a new, uniquely named file in the root directory.
   * Mimics File.createTempFile()
   *
   * @param prefix The prefix string to be used in generating the file's
   *               name; must be at least three characters long
   * @param suffix The suffix string to be used in generating the file's
   *               name; may be <code>null</code>, in which case the
   *               suffix <code>".tmp"</code> will be used
   * @return uniquely named file
   */
  public synchronized File createUniqueFile(String prefix, String suffix) {
    if (suffix == null) suffix = ".tmp";
    Random random = new Random(System.currentTimeMillis());
    File result = new File(getRootDirectory(), prefix + Integer.toString(random.nextInt()) + suffix);
    while (result.exists())
      result = new File(getRootDirectory(), prefix + Integer.toString(random.nextInt()) + suffix);
    return result;
  }


  /**
   * Set the cache path policy
   * @param cachePathPolicy one of:
   *   OneDirectory (default) : replace "/" with "-", so all files are in one directory.
   *   NestedDirectory: cache files are in nested directories under the root.
   *   NestedTruncate: eliminate leading directories
   */
  public void setPolicy(CachePathPolicy cachePathPolicy) {
    this.cachePathPolicy = cachePathPolicy;
  }

  /**
   * Set the cache path policy
   * @param cachePathPolicy one of:
   *   OneDirectory (default) : replace "/" with "-", so all files are in one directory.
   *   NestedDirectory: cache files are in nested directories under the root.
   *   NestedTruncate: eliminate leading directories
   *
   * @param cachePathPolicyParam for NestedTruncate, eliminate this string
   */
  public void setCachePathPolicy(CachePathPolicy cachePathPolicy, String cachePathPolicyParam) {
    this.cachePathPolicy = cachePathPolicy;
    this.cachePathPolicyParam = cachePathPolicyParam;
  }

  /**
   * @deprecated use setCachePathPolicy(CachePathPolicy cachePathPolicy, String cachePathPolicyParam)
   */
  public void setCachePathPolicy(int cachePathPolicy, String cachePathPolicyParam) {
    setPolicy(cachePathPolicy);
    this.cachePathPolicyParam = cachePathPolicyParam;
  }

  /**
   * @deprecated use setCachePathPolicy(CachePathPolicy cachePathPolicy)
   */
  public void setPolicy(int cachePathPolicy) {
    switch (cachePathPolicy) {
      case 0 : setPolicy(CachePathPolicy.OneDirectory);
        break;
      case 1 : setPolicy(CachePathPolicy.NestedDirectory);
        break;
      case 2 : setPolicy(CachePathPolicy.NestedTruncate);
        break;
    }
  }

  public void setPolicy(String policy) {
    if (policy == null) return;
    if (policy.equalsIgnoreCase("oneDirectory")) setCachePathPolicy(CachePathPolicy.OneDirectory, null);
    else if (policy.equalsIgnoreCase("nestedDirectory")) setCachePathPolicy(CachePathPolicy.NestedDirectory, null);
  }

  /**
   * If true, always put the file in the cache. default false.
   * @param alwaysUseCache  If true, always put the file in the cache
   */
  public void setAlwaysUseCache(boolean alwaysUseCache) {
    this.alwaysUseCache = alwaysUseCache;
  }


  /**
   * If true, never put the file in the cache. default false.
   * @param neverUseCache  If true, never put the file in the cache
   */
  public void setNeverUseCache(boolean neverUseCache) {
    this.neverUseCache = neverUseCache;
  }

  /**
   * Make the cache filename
   * @param fileLocation normal file location
   * @return cache filename
   */
  private String makeCachePath(String fileLocation) {

    // remove ':', '?', '=', replace '\' with '/', leading or trailing '/'
    String cachePath = fileLocation;
    cachePath = StringUtil2.remove(cachePath, '?');
    cachePath = StringUtil2.remove(cachePath, '=');
    cachePath = StringUtil2.replace(cachePath, '\\', "/");
    if (cachePath.startsWith("/"))
      cachePath = cachePath.substring(1);
    if (cachePath.endsWith("/"))
      cachePath = cachePath.substring(0, cachePath.length()-1);
    cachePath = StringUtil2.remove(cachePath, ':');

    // remove directories
    if (cachePathPolicy == CachePathPolicy.OneDirectory) {
      cachePath = StringUtil2.replace(cachePath, '/', "-");
    }

    // eliminate leading directories
    else if (cachePathPolicy == CachePathPolicy.NestedTruncate) {
      int pos = cachePath.indexOf(cachePathPolicyParam);
      if (pos >= 0)
        cachePath = cachePath.substring(pos+cachePathPolicyParam.length());
      if (cachePath.startsWith("/"))
        cachePath = cachePath.substring(1);
    }

    // make sure the parent directory exists
    if (cachePathPolicy != CachePathPolicy.OneDirectory) {
      File file = new File(root + cachePath);
      File parent = file.getParentFile();
      if (!parent.exists()) {
        if (root == null) { // LOOK shouldnt happen, remove soon
          System.out.printf("mkdir4 %s%n", parent.getPath());
          new Throwable().printStackTrace();
        }
        boolean ret = parent.mkdirs();
        if (!ret) cacheLog.warn("Error creating parent: " + parent);
      }
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
    if (files != null)
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
        boolean ret = dir.delete();
        if (sbuff != null)
          sbuff.append(" deleted ").append(ret).append(dir.getPath()).append(" last= ").append(new Date(dir.lastModified())).append("\n");
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
          boolean ret = file.delete();
          // assert ret;
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
      sbuff.append("DiskCache2 scour on directory= ").append(root).append("\n");
      cleanCache( new File(root), sbuff, true);
      sbuff.append("----------------------\n");
      if (cacheLog != null) cacheLog.info(sbuff.toString());
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("DiskCache2");
    sb.append("{cachePathPolicy=").append(cachePathPolicy);
    sb.append(", alwaysUseCache=").append(alwaysUseCache);
    sb.append(", cachePathPolicyParam='").append(cachePathPolicyParam).append('\'');
    sb.append(", root='").append(root).append('\'');
    sb.append(", scourEveryMinutes=").append(scourEveryMinutes);
    sb.append(", persistMinutes=").append(persistMinutes);
    sb.append(", fail=").append(fail);
    sb.append('}');
    return sb.toString();
  }

  /* debug
  static void make(DiskCache2 dc, String filename) throws IOException {
    File want = dc.getCacheFile(filename);
    if (want == null) return;
    System.out.println("make=" + want.getPath() + "; exists = " + want.exists());
    if (!want.exists())
      want.createNewFile();
    System.out.println(" canRead= " + want.canRead() + " canWrite = " + canWrite(want) + " lastMod = " + new Date(want.lastModified()));
    System.out.println(" original=" + EscapeStrings.urlDecode(filename));
  }

  static void testCanWrite(String filename) throws IOException {
    File want = new File(filename);
    Path p = want.toPath(); // see if we can write into this directory
    boolean isWriteable = Files.isWritable(p);

    Path p2 = want.getParentFile().toPath();
    boolean isWriteable2 = Files.isWritable(p2);

    System.out.printf("%s %s canWrite %s%n", isWriteable, isWriteable2, filename);
  }

  /* debug
  static public void main(String[] args) throws IOException {
    DiskCache2 dc = new DiskCache2("C:/TEMP/test/", false, 0, 0);
    dc.setRootDirectory("C:/temp/chill/");

    testCanWrite("C:/temp/wms.xml");
    testCanWrite("C:/temp/junk.txt");
    testCanWrite("C:/some/enchanted/evening/joots+3478.txt");
    testCanWrite("C:/Users/mschmidt/test.txt");

    dc.showCache( System.out);
    StringBuilder sbuff = new StringBuilder();
    dc.cleanCache(new File( dc.getRootDirectory()), sbuff, true);
    System.out.println(sbuff);
  }  */

}
