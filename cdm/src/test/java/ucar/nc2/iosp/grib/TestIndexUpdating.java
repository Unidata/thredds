package ucar.nc2.iosp.grib;

import junit.framework.*;

import java.io.File;
import java.io.IOException;

import ucar.nc2.util.IO;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Test that external updating of GRIB index files is handled correctly
 * when file caching is used.
 *
 * @author edavis
 * @since 4.0
 */
public class TestIndexUpdating extends TestCase
{

  private String dataFileName = "GFS_CONUS_191km_20090331_1800.grib1";
  private String indexSuffix0 = ".part0.gbx";
  private String indexSuffix1 = ".part1.gbx";
  private String indexSuffix2 = ".part2.gbx";
  private String indexSuffix3 = ".part3.gbx";
  private String indexSuffixFull = ".full.gbx";
  private String indexSuffix = ".gbx";

  private File dataDir;
  private File dataFile;
  private File indexFile;

  private File indexFilePartial;
  private File indexFileFull;

  public TestIndexUpdating( String name )
  {
    super( name );
  }

  /**
   * Test that sync() updates to new index file.
   * <p>Steps taken in this test:</p>
   * <ul>
   * <li> Put partial GRIB index file beside data file.</li>
   * <li> Open the test GRIB data file.</li>
   * <li> Extract some metadata (size of time dimension e.g.) about the dataset.</li>
   * <li> Move full GRIB index file into place.</li>
   * <li> Call sync() on file/dataset/grid[?]</li>
   * <li> Extract same metadata and compare</li>
   * </ul>
   *
   * <p>NOTE: NetcdfFile.sync(), which is an impl of FileCacheable.sync(),
   * calls IOSP.sync(), which in this case finds the
   * GribGridServiceProvider.sync() impl.
   *
   */
  public void testNetcdfFileSync()
  {
    ucar.nc2.iosp.grid.GridServiceProvider.setAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendMode( false );

    // Setup dataset to use partial GRIB index file.
    if ( ! setupGribAndPartialIndex0() )
      return;

    // Initial opening of the data file.
    NetcdfFile ncf = null;
    try
    { ncf = NetcdfFile.open( dataFile.getPath() ); }
    catch ( IOException e )
    {
      fail( "Failed to open data file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return;
    }

    // Read some information
    Dimension timePartial = ncf.findDimension( "time" );
    assertTrue( "Length of time dimension [" + timePartial.getLength() + "] not as expected [4].",
                timePartial.getLength() == 4 );

    // Switch to use the complete GRIB index file.
    if ( ! switchToCompleteGribIndex() )
      return;

    // sync() the dataset with new index.
    try
    { ncf.sync(); }
    catch ( IOException e )
    {
      fail( "Failed to sync() data file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return;
    }

    // Read metadata from dataset.
    Dimension timeComplete = ncf.findDimension( "time" );

    // Compare new metadata with earlier metadata.
    assertTrue( "Complete time dimension [" + timeComplete.getLength() + " (expected 21)] same as partial time dimension [4 (expected 4)].",
                timePartial.getLength() != timeComplete.getLength() );
    assertTrue( "Length of time dimension [" + timeComplete.getLength() + "] not as expected [21].",
                timeComplete.getLength() == 21 );
  }

  public void testNetcdfDatasetSync()
  {
    ucar.nc2.iosp.grid.GridServiceProvider.setAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendMode( false );

    // Setup dataset to use partial GRIB index file.
    if ( ! setupGribAndPartialIndex0() )
      return;

    // Initial opening of the data file.
    NetcdfDataset ncd = null;
    try
    { ncd = NetcdfDataset.openDataset( dataFile.getPath() ); }
    catch ( IOException e )
    {
      fail( "Failed to open data file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return;
    }

    // Read some information
    Dimension timePartial = ncd.findDimension( "time");
    assertTrue( "Length of time dimension [" + timePartial.getLength() + "] not as expected [4].",
                timePartial.getLength() == 4 );

    // Switch to use the complete GRIB index file.
    if ( ! switchToCompleteGribIndex() )
      return;

    // sync() the dataset with new index.
    try { ncd.sync(); }
    catch ( IOException e )
    {
      fail( "Failed to sync() data file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return;
    }

    // Read metadata from dataset.
    Dimension timeComplete = ncd.findDimension( "time" );

    // Compare new metadata with earlier metadata.
    assertTrue( "Complete time dimension [" + timeComplete.getLength() + " (expected 21)] same as partial time dimension [4 (expected 4)].",
                timePartial.getLength() != timeComplete.getLength() );
    assertTrue( "Length of time dimension [" + timeComplete.getLength() + "] not as expected [21].",
                timeComplete.getLength() == 21 );
  }

  public void testBadIndexFileWithExtendModeFalse()
  {
    ucar.nc2.iosp.grid.GridServiceProvider.setAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendMode( false );

    // Setup dataset to use partial GRIB index file.
    if ( ! setupGribAndPartialIndex0() )
      return;
    if ( ! switchToBinaryBadGribIndex() )
      return;

    long badIndexFileLastModified = indexFile.lastModified();
    long badIndexFileLength = indexFile.length();
    
    // Initial opening of the data file.
    NetcdfFile ncf = null;
    try { ncf = NetcdfFile.open( dataFile.getPath() ); }
    catch ( IOException e )
    {
      assertTrue( "Index file has changed: either last mod time ["
                  + indexFile.lastModified() + "] not as expected [" + badIndexFileLastModified
                  + "]; and/or length [" + indexFile.length() + "] not as expected [" + badIndexFileLength + "].",
                  badIndexFileLastModified == indexFile.lastModified()
                  || badIndexFileLength == indexFile.length() );
      return;
    }

    fail( "Expected IOException not thrown.");
  }

  private boolean setupGribAndPartialIndex0()
  {
    // Check that the data directory exists and is writable.
    dataDir = new File( ucar.nc2.TestAll.cdmTestDataDir, "ucar/nc2/iosp/grib/indexUpdating");
    if ( ! dataDir.exists())
    {
      fail( "Non-existent data directory [" + dataDir.getPath() + "].");
      return false;
    }
    if ( ! dataDir.canWrite())
    {
      fail( "Cannot write to data directory [" + dataDir.getPath() + "].");
      return false;
    }

    // Check that the GRIB data file exists and is readable.
    dataFile = new File( dataDir, dataFileName);
    if ( ! dataFile.exists() )
    {
      fail( "Non-existent data file [" + dataFile.getPath() + "].");
      return false;
    }
    if ( ! dataFile.canRead() )
    {
      fail( "Cannot read data file [" + dataFile.getPath() + "]." );
      return false;
    }

    // Check that index file doesn't exist and is writable.
    indexFile = new File( dataDir, dataFileName + ".gbx" );
    if ( indexFile.exists() && ! indexFile.canWrite())
    {
      fail( "Cannot write index file [" + indexFile.getPath() + "].");
      return false;
    }
    indexFile.deleteOnExit();

    // Check that partial index file exists and is readable.
    indexFilePartial = new File( dataDir, dataFileName + ".part0.gbx" );
    if ( ! indexFilePartial.exists())
    {
      fail( "Non-existent partial index file [" + indexFilePartial + "].");
      return false;
    }
    if ( ! indexFilePartial.canRead() )
    {
      fail( "Cannot read partial index file [" + indexFilePartial.getPath() + "]." );
      return false;
    }

    // Copy partial index file into place (".gbx").
    try
    { IO.copyFile( indexFilePartial, indexFile ); }
    catch ( IOException e )
    {
      fail( "Failed to copy partial index file [" + indexFilePartial.getPath() + "] to index file [" + indexFile.getPath() + "]: " + e.getMessage());
      return false;
    }
    return true;
  }


  private boolean switchToCompleteGribIndex()
  {
    return switchGribIndex( ".full.gbx");
  }

  private boolean switchToPartialGribIndex0()
  {
    return switchGribIndex( ".part0.gbx");
  }

  private boolean switchToPartialGribIndex1()
  {
    return switchGribIndex( ".part1.gbx");
  }

  private boolean switchToPartialGribIndex2()
  {
    return switchGribIndex( ".part3.gbx");
  }

  private boolean switchToPartialGribIndex3()
  {
    return switchGribIndex( ".part3.gbx");
  }

  private boolean switchToBinaryCompleteGribIndex()
  {
    return switchGribIndex( ".binFull.gbx");
  }

  private boolean switchToBinaryBadGribIndex()
  {
    return switchGribIndex( ".binModFromPart1.gbx");
  }

  private boolean switchGribIndex( String indexSuffix )
  {
    if ( dataDir == null || dataFileName == null || indexFile == null )
      throw new IllegalStateException( "Must first call setupGribAndPartialIndex0() method.");

    // Check that the complete index file exists and is readable.
    indexFileFull = new File( dataDir, dataFileName + indexSuffix );
    if ( !indexFileFull.exists() )
    {
      fail( "Non-existent full index file [" + indexFileFull + "]." );
      return false;
    }
    if ( !indexFileFull.canRead() )
    {
      fail( "Cannot read full index file [" + indexFileFull.getPath() + "]." );
      return false;
    }

    // Copy the complete index into place.
    try
    { IO.copyFile( indexFileFull, indexFile ); }
    catch ( IOException e )
    {
      fail( "Failed to copy complete index file [" + indexFileFull.getPath() + "] to index file [" + indexFile.getPath() + "]: " + e.getMessage() );
      return false;
    }
    return true;
  }

}
