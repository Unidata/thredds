/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.util.*;

/**
 * This is a general purpose utility for determining a place to write files and cache them, eg for
 * uncompressing files. This class does not scour itself.
 * <p>
 * Note that when a file in the cache is accessed, its lastModified date is set, which is used for the LRU scouring.
 * <p> The cdm library sometimes needs to write files, eg
 * to uncompress them, or for grib index files, etc. The first choice is to write these files
 * in the same directory that the original file lives in. However, that directory may not be
 * writeable, so we need to find a place to write them to.
 * We also want to use the file if it already exists.
 * <p>
 * <p> A writeable cache "root directory" is set: <ol>
 * <li>  explicitly by setRootDirectory()
 * <li>  through the system property "nj22.cache" if it exists
 * <li>  by appending "/nj22/cache" to the system property "user.home" if it exists
 * <li>  by appending "/nj22/cache" to the system property "user.dir" if it exists
 * <li>  use "./nj22/cache" if none of the above
 * </ol>
 * <p>
 * <p> Scenario 1: want to uncompress a file; check to see if already have done so,
 * otherwise get a File that can be written to.
 * <pre>
 * // see if already uncompressed
 * File uncompressedFile = FileCache.getFile( uncompressedFilename, false);
 * if (!uncompressedFile.exists()) {
 *   // nope, uncompress it
 *   UncompressInputStream.uncompress( uriString, uncompressedFile);
 * }
 * doSomething( uncompressedFile);
 * </pre>
 * <p>
 * <p> Scenario 2: want to write a derived file always in the cache.
 * <pre>
 * File derivedFile = FileCache.getCacheFile( derivedFilename);
 * if (!derivedFile.exists()) {
 *   createDerivedFile( derivedFile);
 * }
 * doSomething( derivedFile);
 * </pre>
 * <p>
 * <p> Scenario 3: same as scenario 1, but use the default Cache policy:
 * <pre>
 * File wf = FileCache.getFileStandardPolicy( uncompressedFilename);
 * if (!wf.exists()) {
 *   writeToFile( wf);
 *   wf.close();
 * }
 * doSomething( wf);
 * </pre>
 *
 * @author jcaron
 */
public class DiskCache {
  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("cacheLogger");
  static private String root = null;
  static private boolean standardPolicy = false;
  static private boolean checkExist = false;

  /**
   * debug only
   */
  static public boolean simulateUnwritableDir = false;

  static {
    root = System.getProperty("nj22.cache");

    if (root == null) {
      String home = System.getProperty("user.home");

      if (home == null)
        home = System.getProperty("user.dir");

      if (home == null)
        home = ".";

      root = home + "/.unidata/cache/";
    }

    String policy = System.getProperty("nj22.cachePolicy");
    if (policy != null)
      standardPolicy = policy.equalsIgnoreCase("true");
  }

  /**
   * Set the cache root directory. Create it if it doesnt exist.
   *
   * @param cacheDir the cache directory
   */
  static public void setRootDirectory(String cacheDir) {
    if (!cacheDir.endsWith("/"))
      cacheDir = cacheDir + "/";
    root = StringUtil2.replace(cacheDir, '\\', "/"); // no nasty backslash

    makeRootDirectory();
  }

  /**
   * Make sure that the current root directory exists.
   */
  static public void makeRootDirectory() {
    File dir = new File(root);
    if (!dir.exists())
      if (!dir.mkdirs())
        throw new IllegalStateException("DiskCache.setRootDirectory(): could not create root directory <" + root + ">.");
    checkExist = true;
  }

  /**
   * Get the name of the root directory
   *
   * @return name of the root directory
   */
  static public String getRootDirectory() {
    return root;
  }

  /**
   * Set the standard policy used in getWriteableFileStandardPolicy().
   * Default is to try to create the file in the same directory as the original.
   * Setting alwaysInCache to true means to always create it in the cache directory.
   *
   * @param alwaysInCache make this the default policy
   */
  static public void setCachePolicy(boolean alwaysInCache) {
    standardPolicy = alwaysInCache;
  }

  /**
   * Get a File if it exists. If not, get a File that can be written to.
   * Use the standard policy to decide where to place it.
   * <p>
   * Things are a bit complicated, because in order to guarantee a file in
   * an arbitrary location can be written to, we have to try to open it as a
   * FileOutputStream. If we do, we don't want to open it twice,
   * so we return a WriteableFile that contains an opened FileOutputStream.
   * If it already exists, or we get it from cache, we don't need to open it.
   * <p>
   * In any case, you must call WriteableFile.close() to make sure its closed.
   *
   * @param fileLocation normal file location
   * @return WriteableFile holding the writeable File and a possibly opened FileOutputStream (append false)
   */
  static public File getFileStandardPolicy(String fileLocation) {
    return getFile(fileLocation, standardPolicy);
  }

  /**
   * Get a File if it exists. If not, get a File that can be written to.
   * If alwaysInCache, look only in the cache, otherwise, look in the "normal" location first,
   * then in the cache.
   * <p>
   *
   * @param fileLocation  normal file location
   * @param alwaysInCache true if you want to look only in the cache
   * @return a File that either exists or is writeable.
   */
  static public File getFile(String fileLocation, boolean alwaysInCache) {
    if (alwaysInCache) {
      return getCacheFile(fileLocation);

    } else {
      File f = new File(fileLocation);
      if (f.exists())
        return f;

      // now comes the tricky part to make sure we can open and write to it
      try {
        if (!simulateUnwritableDir && f.createNewFile()) {
          boolean ret = f.delete();
          assert ret;
          return f;
        }
      } catch (IOException e) {
        // cant write to it - drop through
      }

      return getCacheFile(fileLocation);
    }
  }

  /**
   * Get a file in the cache.
   * File may or may not exist. We assume its always writeable.
   * If it does exist, set its LastModifiedDate to current time.
   *
   * @param fileLocation normal file location
   * @return equivalent File in the cache.
   */
  static public File getCacheFile(String fileLocation) {
    File f = new File(makeCachePath(fileLocation));
    if (f.exists()) {
      if (!f.setLastModified(System.currentTimeMillis()))
        logger.warn("Failed to setLastModified on " + f.getPath());
    }

    if (!checkExist) {
      File dir = f.getParentFile();
      if (!dir.exists() && !dir.mkdirs())
        logger.warn("Failed to mkdirs on " + dir.getPath());
      checkExist = true;
    }
    return f;
  }

  /**
   * Make the cache filename
   *
   * @param fileLocation normal file location
   * @return cache filename
   */
  static private String makeCachePath(String fileLocation) {
    Escaper urlPathEscaper = UrlEscapers.urlPathSegmentEscaper();

    fileLocation = fileLocation.replace('\\', '/');  // LOOK - use better normalization code  eg Spring StringUtils
    String cachePath = urlPathEscaper.escape(fileLocation);

    return root + cachePath;
  }

  static public void showCache(PrintStream pw) {
    pw.println("Cache files");
    pw.println("Size   LastModified       Filename");
    File dir = new File(root);
    File[] children = dir.listFiles();
    if (children == null) return;
    for (File file : children) {
      String org = EscapeStrings.urlDecode(file.getName());
      pw.println(" " + file.length() + " " + new Date(file.lastModified()) + " " + org);
    }
  }

  /**
   * Remove all files with date < cutoff.
   *
   * @param cutoff earliest date to allow
   * @param sbuff  write results here, null is ok.
   */
  static public void cleanCache(Date cutoff, StringBuilder sbuff) {
    if (sbuff != null) sbuff.append("CleanCache files before ").append(cutoff).append("\n");
    File dir = new File(root);
    File[] children = dir.listFiles();
    if (children == null) return;
    for (File file : children) {
      Date lastMod = new Date(file.lastModified());
      if (lastMod.before(cutoff)) {
        boolean ret = file.delete();
        if (sbuff != null) {
          sbuff.append(" delete ").append(file).append(" (").append(lastMod).append(")\n");
          if (!ret) sbuff.append("Error deleting ").append(file).append("\n");
        }
      }
    }
  }

  /**
   * Remove files if needed to make cache have less than maxBytes bytes file sizes.
   * This will remove oldest files first.
   *
   * @param maxBytes max number of bytes in cache.
   * @param sbuff    write results here, null is ok.
   */
  static public void cleanCache(long maxBytes, StringBuilder sbuff) {
    cleanCache(maxBytes, new FileAgeComparator(), sbuff);
  }

  /**
   * Remove files if needed to make cache have less than maxBytes bytes file sizes.
   * This will remove files in sort order defined by fileComparator.
   * The first files in the sort order are kept, until the max bytes is exceeded, then they are deleted.
   *
   * @param maxBytes       max number of bytes in cache.
   * @param fileComparator sort files first with this
   * @param sbuff          write results here, null is ok.
   */
  static public void cleanCache(long maxBytes, Comparator<File> fileComparator, StringBuilder sbuff) {
    if (sbuff != null) sbuff.append("DiskCache clean maxBytes= ")
            .append(maxBytes).append("on dir ").append(root).append("\n");

    File dir = new File(root);
    long total = 0, total_delete = 0;

    File[] files = dir.listFiles();
    if (files != null) {
      List<File> fileList = Arrays.asList(files);
      Collections.sort(fileList, fileComparator);

      for (File file : fileList) {
        if (file.length() + total > maxBytes) {
          total_delete += file.length();
          if (sbuff != null)
            sbuff.append(" delete ").append(file).append(" (")
                    .append(file.length()).append(")\n");
          if (!file.delete() && sbuff != null)
            sbuff.append("Error deleting ").append(file).append("\n");
        } else {
          total += file.length();
        }
      }
    }
    if (sbuff != null) {
      sbuff.append("Total bytes deleted= ").append(total_delete).append("\n");
      sbuff.append("Total bytes left in cache= ").append(total).append("\n");
    }
  }

  // reverse sort - latest come first
  static private class FileAgeComparator implements Comparator<File> {
    public int compare(File f1, File f2) {
      long f1Age = f1.lastModified();
      long f2Age = f2.lastModified();
      return (f1Age < f2Age) ? 1 : (f1Age == f2Age ? 0 : -1);  // Steve Ansari 6/3/2010
    }
  }

  /**
   * debug
   *
   * @param filename look for this file
   * @throws java.io.IOException if read error
   */
  static void make(String filename) throws IOException {
    File want = DiskCache.getCacheFile(filename);
    System.out.println("make=" + want.getPath() + "; exists = " + want.exists());
    if (!want.exists()) {
      boolean ret = want.createNewFile();
      assert ret;
    }
    System.out.println(" canRead= " + want.canRead() + " canWrite = " + want.canWrite() + " lastMod = " + new Date(want.lastModified()));
    System.out.println(" original=" + filename);
  }


  static public void main(String[] args) throws IOException {
    DiskCache.setRootDirectory("C:/temp/chill/");
    make("C:/junk.txt");
    make("C:/some/enchanted/evening/joots+3478.txt");
    make("http://www.unidata.ucar.edu/some/enc hanted/eve'ning/nowrite.gibberish");

    showCache(System.out);
    StringBuilder sbuff = new StringBuilder();
    cleanCache(1000 * 1000 * 10, sbuff);
    System.out.println(sbuff);
  }

}
