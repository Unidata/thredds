package ucar.unidata.util;

import ucar.nc2.util.DiskCache;
import ucar.nc2.TestAll;

import java.util.Date;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestDiskCacheUtils
{
  /**
   * Setup DiskCache for use in testing.
   */
  public static void setupDiskCache( boolean alwaysInCache)
  {
    String cacheDir = TestAll.temporaryDataDir + "cache/";
    if ( ! DiskCache.getRootDirectory().equals( cacheDir))
    {
      DiskCache.setRootDirectory( cacheDir );
      DiskCache.makeRootDirectory();
    }

    DiskCache.setCachePolicy( alwaysInCache );
  }

  /**
   * Empty the DiskCache directory.
   */
  public static void emptyDiskCache( StringBuilder report)
  {
    DiskCache.cleanCache( new Date( System.currentTimeMillis() ), report);
  }

  
}
