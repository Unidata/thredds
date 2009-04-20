package ucar.nc2.iosp.grib;

import junit.framework.*;

import java.io.File;
import java.io.IOException;

import ucar.nc2.util.IO;
import ucar.nc2.util.DiskCache;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.io.RandomAccessFile;

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
  private String dataSuffix1_8 = "times1-8";
  private String dataSuffix9_12 = "times9-12";
  private String dataSuffix13_18 = "times13-18";
  private String dataSuffix19_21 = "times19-21";

  private String indexFileName = dataFileName + ".gbx";
  private String indexSuffix1_8 = "times1-8";
  private String indexSuffix1_12 = "times1-12";
  private String indexSuffix1_18 = "times1-18";
  private String indexSuffix1_21 = "times1-21";

  private File dataDir;
  private File dataFile;
  private File indexFile;

  private File indexFilePartial;
  private File indexFileFull;

  public TestIndexUpdating( String name )
  {
    super( name );
  }

  @Override
  protected void tearDown() throws Exception
  {
    // Remove dataFile, created on setup.
    if ( dataFile != null && dataFile.exists() )
      dataFile.delete();

    // Remove index file, also created on setup.
    if ( indexFile != null && indexFile.exists() )
      indexFile.delete();

    // always remove cache index if it exists
    File cacheIndex = null;
    if ( indexFile != null )
      cacheIndex = DiskCache.getFile(indexFile.getPath(), true);
    if ( cacheIndex != null && cacheIndex.exists())
    {
      if ( ! cacheIndex.canWrite())
        fail( "Cannot write/remove cache index file [" + cacheIndex.getPath() + "].");
      else if ( cacheIndex.exists() )
        cacheIndex.delete();
    }
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
    ucar.nc2.iosp.grib.GribServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grib.GribServiceProvider.setExtendIndex( false);
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

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

    // Switch to use the complete GRIB file.
    if ( ! switchToCompleteGrib() )
      return;

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
    ucar.nc2.iosp.grib.GribServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grib.GribServiceProvider.setExtendIndex( false);
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

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

    // Switch to use the complete GRIB file.
    if ( ! switchToCompleteGrib() )
      return;

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
    ucar.nc2.iosp.grib.GribServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grib.GribServiceProvider.setExtendIndex( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

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

  /**
   * tests the TDS configuration : there is no index available on first access to the Grib file, so
   * an index is created in the cache directory because the TDS doesn't have write permission in the
   * same directory as the Grib file. Then future accesses use the index in the  same
   * directory as the Grib file.
   */
  public void testNoIndexTDS()
  {
    ucar.nc2.iosp.grib.GribServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grib.GribServiceProvider.setExtendIndex( false);
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    // Setup dataset to use partial GRIB index file.
    if ( ! setupGrib() )
      return;

    DiskCache.simulateUnwritableDir = true;    

    // Initial opening of the data file.
    NetcdfFile ncf = null;
    try { ncf = NetcdfFile.open( dataFile.getPath() ); }
    catch ( IOException e )
    {
      fail( "exception opening");
    }

    Dimension timePartial = ncf.findDimension( "time" );

    assertTrue( "Time dimension ["+ timePartial.getLength()+"] not as expected [4].", timePartial.getLength() == 4 );

    // Switch to use the complete GRIB file.
    if ( ! switchToCompleteGrib() )
      return;

    if ( ! switchToCompleteGribIndex() )
      return;

    // sync() the dataset with new index.
    try
    {
      ncf.sync();
    }
    catch ( IOException e )
    {
      fail( "Failed to sync() data file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return;
    }

    Dimension timeNew = ncf.findDimension( "time" );

    assertTrue( "Time dimension [" + timeNew.getLength() + "] not as expected [21].", timeNew.getLength() == 21 );
    DiskCache.simulateUnwritableDir = false;
  }

   /**
     * tests the ToolsUI or TDS configuration : there is no index available on first access to the Grib file, so
     * an index is created in the same directory as the Grib file. Then future accesses use the index in the  same
     * directory as the Grib file but the Index can be extended with sync().
     */
    public void testNoIndexNextToGrib()
    {
      ucar.nc2.iosp.grib.GribServiceProvider.setIndexAlwaysInCache( false );
      ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
      ucar.nc2.iosp.grib.GribServiceProvider.setExtendIndex( true );
      ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( true  );

      // Setup dataset to use partial GRIB index file.
      if ( ! setupGrib() )
        return;

      // Initial opening of the data file.
      NetcdfFile ncf = null;
      try { ncf = NetcdfFile.open( dataFile.getPath() ); }
      catch ( IOException e )
      {
        fail( "exception opening");
      }

      Dimension timePartial = ncf.findDimension( "time" );

      assertTrue( "Time dimension ["+ timePartial.getLength()+"] not as expected [4].", timePartial.getLength() == 4 );

      // Switch to use the complete GRIB file.
      if ( ! switchToCompleteGrib() )
        return;

      try
      {
        ncf.sync();
      }
      catch ( IOException e )
      {
        fail( "Failed to sync() data file [" + dataFile.getPath() + "]: " + e.getMessage() );
        return;
      }

      Dimension timeNew = ncf.findDimension( "time" );

      assertTrue( "Time dimension [" + timeNew.getLength() + "] not as expected [21].", timeNew.getLength() == 21 );
    }

  /**
   * Test a TDS configuration where the program has write permission in the directory
   * where the Grib file is located. If the Index doesn't exist then one will created. If the
   * Index exists then it will be checked to see if it's up to date. Sync() doesn't have any
   * affect because the index has already been updated.
   */
  public void testExtendModeTrue()
  {
    ucar.nc2.iosp.grib.GribServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grib.GribServiceProvider.setExtendIndex( true);
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( true );

    // Setup dataset to use partial GRIB index file.
    if ( ! setupGribAndPartialIndex0() )
      return;

    // Switch to use the complete GRIB file.
    if ( ! switchToCompleteGrib() )
      return;

    // Initial opening of the data file.
    NetcdfFile ncf = null;
    try { ncf = NetcdfFile.open( dataFile.getPath() ); }
    catch ( IOException e )
    {
      fail( "exception opening");
    }

    Dimension timeComplete = ncf.findDimension( "time" );

    assertTrue( "Time dimension ["+ timeComplete.getLength()+"] not as expected [21].", timeComplete.getLength() == 21 );

    try
    {
      ncf.sync();
    }
    catch ( IOException e )
    {
      fail( "Failed to sync() data file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return;
    }

    Dimension timeNew = ncf.findDimension( "time" );

    assertTrue( "Time dimension [" + timeNew.getLength() + "] not as expected [21].", timeNew.getLength() == 21 );

  }

  public void testAlwaysInCacheAndExtendModeTrue()
  {
    ucar.nc2.iosp.grib.GribServiceProvider.setIndexAlwaysInCache( true );
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( true );
    ucar.nc2.iosp.grib.GribServiceProvider.setExtendIndex( true);
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( true );

    // Setup dataset to use partial GRIB index file.
    if ( ! setupGribAndPartialIndex0() )
      return;

    // Initial opening of the data file.
    NetcdfFile ncf = null;
    try { ncf = NetcdfFile.open( dataFile.getPath() ); }
    catch ( IOException e )
    {
      fail( "exception opening");
    }

    Dimension timeComplete = ncf.findDimension( "time" );

    assertTrue( "Time dimension ["+ timeComplete.getLength()+"] not as expected [4].",
                timeComplete.getLength() == 4 );

    // Switch to use the complete GRIB file.
    if ( ! switchToCompleteGrib() )
      return;

    // sync() the dataset  .
    try
    {
      ncf.sync();
    }
    catch ( IOException e )
    {
      fail( "Failed to sync() data file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return;
    }

    Dimension timeNew = ncf.findDimension( "time" );

    assertTrue( "Time dimension [" + timeNew.getLength() + "] not as expected [21].", timeNew.getLength() == 21 );
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
    // Copy partial grib file into place
    File gribFilePartial = new File( dataDir, dataFileName + ".part0" );
    dataFile = new File( dataDir, dataFileName);
    try
    { IO.copyFile( gribFilePartial, dataFile ); }
    catch ( IOException e )
    {
      fail( "Failed to copy partial grib file [" + gribFilePartial.getPath() + "] to grib file [" + dataFile.getPath() + "]: " + e.getMessage());
      return false;
    }
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

  private boolean setupGrib()
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
    // Copy partial grib file into place
    File gribFilePartial = new File( dataDir, dataFileName + ".part0" );
    dataFile = new File( dataDir, dataFileName);
    try
    { IO.copyFile( gribFilePartial, dataFile ); }
    catch ( IOException e )
    {
      fail( "Failed to copy partial grib file [" + gribFilePartial.getPath() + "] to grib file [" + dataFile.getPath() + "]: " + e.getMessage());
      return false;
    }

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
 
    return true;
  }

  private boolean switchToCompleteGrib()
  {
    RandomAccessFile input = null, output = null;
    try {
    // read in extra data
    input = new RandomAccessFile(dataFile.getPath() +".extra", "r");
    byte[] extra = new byte[ (int) input.length()];
    input.read( extra );
    input.close();

    output = new RandomAccessFile(dataFile.getPath(), "rw");
    output.seek( output.length());
    output.write( extra );
    output.close();

    } catch (Exception e ) {
      fail( "Failed to add file [" + input.getLocation() + "] to  file [" + dataFile.getPath() + "]: " + e.getMessage());
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


  //===============
  // New setup for times1-8, times9-12, etc
  //===============

  private boolean gribInit_1_8()
  {
    // Check that the data directory exists and is writable.
    dataDir = new File( ucar.nc2.TestAll.cdmTestDataDir, "ucar/nc2/iosp/grib/indexUpdating" );
    if ( ! dataDir.exists() )
    {
      fail( "Non-existent data directory [" + dataDir.getPath() + "]." );
      return false;
    }
    if ( ! dataDir.canWrite() )
    {
      fail( "Cannot write to data directory [" + dataDir.getPath() + "]." );
      return false;
    }

    // Check that the partial GRIB file exists and is readable.
    File gribFilePartial = new File( dataDir, dataFileName + dataSuffix1_8 );
    if ( ! gribFilePartial.exists())
    {
      fail( "Non-existent partial GRIB file [" + gribFilePartial.getPath() + "].");
      return false;
    }
    if ( ! gribFilePartial.canRead())
    {
      fail( "Cannot read the partial GRIB file [" + gribFilePartial.getPath() + "].");
      return false;
    }

    // Locate data file and setup for final deletion.
    dataFile = new File( dataDir, dataFileName );
    dataFile.deleteOnExit();

    // Copy partial grib file into place
    try
    {
      IO.copyFile( gribFilePartial, dataFile );
    }
    catch ( IOException e )
    {
      fail( "Failed to copy partial grib file [" + gribFilePartial.getPath() + "] to grib file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return false;
    }

    if ( ! dataFile.exists() )
    {
      fail( "Non-existent data file [" + dataFile.getPath() + "]." );
      return false;
    }
    if ( ! dataFile.canRead() )
    {
      fail( "Cannot read data file [" + dataFile.getPath() + "]." );
      return false;
    }


    return true;
  }

  private boolean gribAppend9_12() { return gribAppend( dataSuffix9_12); }
  private boolean gribAppend13_18() { return gribAppend( dataSuffix13_18); }
  private boolean gribAppend19_21() { return gribAppend( dataSuffix19_21); }

  private boolean gribAppend( String suffix )
  {
    RandomAccessFile input = null, output = null;
    try
    {
      // read in extra data
      input = new RandomAccessFile( dataFile.getPath() + suffix, "r" );
      byte[] extra = new byte[(int) input.length()];
      input.read( extra );
      input.close();

      output = new RandomAccessFile( dataFile.getPath(), "rw" );
      output.seek( output.length() );
      output.write( extra );
      output.close();

    }
    catch ( Exception e )
    {
      fail( "Failed to add file [" + input.getLocation() + "] to  file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return false;
    }

    return true;
  }

  private boolean indexSetup1_8() { return indexSetup( indexSuffix1_8); }
  private boolean indexSetup1_12() { return indexSetup( indexSuffix1_12); }
  private boolean indexSetup1_18() { return indexSetup( indexSuffix1_18); }
  private boolean indexSetup1_21() { return indexSetup( indexSuffix1_21); }

  private boolean indexSetup( String suffix)
  {
    // Check that the source index file exists and is readable.
    File indexFileSource= new File( dataDir, indexFileName + suffix );
    if ( ! indexFileSource.exists() )
    {
      fail( "Non-existent source index file [" + indexFileSource + "]." );
      return false;
    }
    if ( ! indexFileSource.canRead() )
    {
      fail( "Cannot read source index file [" + indexFileSource.getPath() + "]." );
      return false;
    }

    // Locate index file and setup for final deletion
    indexFile = new File( dataDir, indexFileName );
    indexFile.deleteOnExit();

    // Copy source index file into place (".gbx").
    try
    {
      IO.copyFile( indexFileSource, indexFile );
    }
    catch ( IOException e )
    {
      fail( "Failed to copy source index file [" + indexFileSource.getPath() + "] to index file [" + indexFile.getPath() + "]: " + e.getMessage() );
      return false;
    }

    if ( ! indexFile.exists())
    {
      fail( "Non-existent index file [" + indexFile.getPath() + "].");
      return false;
    }
    if ( ! indexFile.canRead())
    {
      fail( "Cannot read index file [" + indexFile.getPath() + "]." );
      return false;
    }

    return true;
  }
}
