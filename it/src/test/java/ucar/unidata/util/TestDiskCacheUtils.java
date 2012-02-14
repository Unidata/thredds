package ucar.unidata.util;

import ucar.nc2.util.DiskCache;
import ucar.nc2.TestAll;

import java.util.Date;
import java.io.File;

/**
 * Utility methods to allow tests to configure and empty disk cache directories.
 * Separate methods are provided for dealing with ucar.nc2.util.DiskCache and
 * ucar.nc2.util.DiskCache2
 *
 * @author edavis
 * @since 4.0
 */
public class TestDiskCacheUtils
{
  private TestDiskCacheUtils() {}
  
  /**
   * Configure DiskCache to use a temporary root directory. The policy of
   * whether to check first at the requested location and then the cache
   * directory or to always check only in the cache directory can also
   * be configured.
   *
   * @param alwaysInCache if false check for the file in the given location before checking in the cache directory, otherwise only check in the cache directory.
   */
  public static void setupDiskCacheInTmpDir( boolean alwaysInCache)
  {
    String cacheDir = TestAll.temporaryLocalDataDir + "cache/DiskCache/";
    if ( ! DiskCache.getRootDirectory().equals( cacheDir))
    {
      DiskCache.setRootDirectory( cacheDir );
      DiskCache.makeRootDirectory();
    }

    DiskCache.setCachePolicy( alwaysInCache );
  }

  /**
   * Empty the DiskCache directory.
   *
   * @param report a StringBuilder in which to put a summary report of the emptied contents, null is allowed.
   */
  public static void emptyDiskCache( StringBuilder report)
  {
    DiskCache.cleanCache( new Date( System.currentTimeMillis() ), report);
  }

  /**
   * Configure DiskCache2 to use a temporary root directory. 
   */
  public static void setupDiskCache2WithTmpRootDir()
  {
    String cacheDir = TestAll.temporaryLocalDataDir + "cache/DiskCache2/";
    File f = new File( cacheDir);
    if ( ! f.exists())
      if ( ! f.mkdirs())
        throw new IllegalStateException( "Could not create DiskCache2 temporary root directory ["+ cacheDir + "].");

    if ( ! f.isDirectory() )
      throw new IllegalStateException( "DiskCache2 temporary root directory [" + cacheDir + "] is not a directory.");

    String prevValue = System.setProperty( "nj22.cachePersistRoot", cacheDir);
  }
  
}
