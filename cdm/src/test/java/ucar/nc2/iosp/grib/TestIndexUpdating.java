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
import ucar.grib.GribIndexName;

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
  private String dataSuffix1_8 = ".times1-8";
  private String dataSuffix9_12 = ".times9-12";
  private String dataSuffix13_18 = ".times13-18";
  private String dataSuffix19_21 = ".times19-21";

  private String indexFileName = dataFileName + ".gbx";
  //private String indexFileName = dataFileName + GribIndexName.currentSuffix;
  private String indexSuffix1_8 = ".times1-8";
  private String indexSuffix1_12 = ".times1-12";
  private String indexSuffix1_18 = ".times1-18";
  private String indexSuffix1_21 = ".times1-21";

  private String localDataDirPath = "ucar/nc2/iosp/grib/indexUpdating";
  private File dataDir;
  private File dataFile;
  private File indexFile;

  private NetcdfFile netcdfObj;

  private File indexFilePartial;
  private File indexFileFull;

  public TestIndexUpdating( String name )
  {
    super( name );
  }

  @Override
  protected void setUp() throws Exception
  {
    // Check that the data directory exists and is writable.
    dataDir = new File( ucar.nc2.TestAll.cdmLocalTestDataDir, localDataDirPath );
    if ( ! dataDir.exists() )
    {
      fail( "Non-existent data directory [" + dataDir.getPath() + "]." );
      return;
    }
    if ( ! dataDir.canWrite() )
    {
      fail( "Cannot write to data directory [" + dataDir.getPath() + "]." );
      return;
    }

    // Locate data file and setup for final deletion.
    dataFile = new File( dataDir, dataFileName );
    dataFile.deleteOnExit();

    // Locate index file and setup for final deletion
    indexFile = new File( dataDir, indexFileName );
    indexFile.deleteOnExit();

    // Check that index file doesn't exist or, if it does, is writable.
    if ( indexFile.exists() && ! indexFile.canWrite() )
      fail( "Cannot write index file [" + indexFile.getPath() + "]." );
  }

  @Override
  protected void tearDown() throws Exception
  {
    if ( netcdfObj != null )
      netcdfObj.close();

    // Remove dataFile, created on setup.
    if ( dataFile != null && dataFile.exists() )
      dataFile.delete();

    // Remove index file, also created on setup.
    if ( indexFile != null && indexFile.exists() )
      indexFile.delete();

    File indexFileGbx8 = new File( GribIndexName.get(indexFile.getPath() ));
    if ( indexFileGbx8 != null && indexFileGbx8.exists() )
      indexFileGbx8.delete();

    // always remove cache index if it exists
    File cacheIndex = null;
    if ( indexFile != null )
      cacheIndex = DiskCache.getFile( indexFile.getPath(), true);
    if ( cacheIndex != null && cacheIndex.exists())
    {
      if ( ! cacheIndex.canWrite())
        fail( "Cannot write/remove cache index file [" + cacheIndex.getPath() + "].");
      else if ( cacheIndex.exists() )
        cacheIndex.delete();
    }
    cacheIndex = null;
    if ( indexFile != null )
      cacheIndex = DiskCache.getFile( GribIndexName.get(indexFile.getPath()), true);
    if ( cacheIndex != null && cacheIndex.exists())
    {
      if ( ! cacheIndex.canWrite())
        fail( "Cannot write/remove cache index file [" + cacheIndex.getPath() + "].");
      else if ( cacheIndex.exists() )
        cacheIndex.delete();
    }
  }

  /**
   * Test existing index in "Server with External Indexer" user story
   * with NetcdfFile using the GribGrid IOSP (new).
   */
  public void testExistingUpdatingIndex_ServerWithExternalIndexer_NcFile_NewGribIosp()
  {
    // Setup for "Server with external indexer" user story.
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    runTestExistingUpdatingIndex( NcObjectType.FILE, GribIospVersion.NEW );
  }

  /**
   * Test existing index in "Server with External Indexer" user story
   * with NetcdfDataset using the GribGrid IOSP (new).
   */
  public void testExistingUpdatingIndex_ServerWithExternalIndexer_NcDataset_NewGribIosp()
  {
    // Setup for "Server with external indexer" user story.
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    runTestExistingUpdatingIndex( NcObjectType.DATASET, GribIospVersion.NEW );
  }

//  /**
//   * Test existing index in "Server with External Indexer" user story
//   * with NetcdfFile using the Grib IOSP (old).
//   */
//  public void testExistingUpdatingIndex_ServerWithExternalIndexer_NcFile_OldGribIosp()
//  {
//    // Setup for "Server with external indexer" user story.
//    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
//    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );
//
//    runTestExistingUpdatingIndex( NcObjectType.FILE, GribIospVersion.OLD );
//  }
//
//  /**
//   * Test existing index in "Server with External Indexer" user story
//   * with NetcdfDataset using the Grib IOSP (old).
//   */
//  public void testExistingUpdatingIndex_ServerWithExternalIndexer_NcDataset_OldGribIosp()
//  {
//    // Setup for "Server with external indexer" user story.
//    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
//    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );
//
//    runTestExistingUpdatingIndex( NcObjectType.DATASET, GribIospVersion.OLD );
//  }

  /**
   * Test GRIB IOSP open() and sync() on growing GRIB file with an external
   * indexer, the index matches the GRIB file on the initial read.
   *
   * <p>Steps taken in this test:</p>
   * <ul>
   * <li> Setup 1: GRIB file (8 time steps), index file (8 time steps).</li>
   * <li> Open GRIB file and check that "time" dimension has 8 time steps.</li>
   * <li> Setup 2: GRIB file (12 time step), index file (8 time steps).</li>
   * <li> Sync() dataset and check that "time" dimension has 8 time steps.</li>
   * <li> Setup 3: GRIB file (12 time steps), index file (12 time steps)..</li>
   * <li> Sync() dataset and check that "time" dimension has 12 time steps.</li>
   * <li> Setup 4: GRIB file (18 time steps), index file (18 time steps)..</li>
   * <li> Sync() dataset and check that "time" dimension has 18 time steps.</li>
   * <li> Setup 5: GRIB file (21 time steps), index file (21 time steps)..</li>
   * <li> Sync() dataset and check that "time" dimension has 21 time steps.</li>
   * </ul>
   *
   * <p>NOTE: NetcdfFile.sync(), which is an impl of FileCacheable.sync(),
   * calls IOSP.sync(), which in this case finds the
   * GribGridServiceProvider.sync() impl.
   * 
   * @param ncObjType the type of nc object to use (NetcdfFile, NetcdfDataset, etc).
   * @param gribIospVer the GRIB IOSP impl to use.
   * @return true if the test ran successfully, false on failure.
   */
  private boolean runTestExistingUpdatingIndex( NcObjectType ncObjType, GribIospVersion gribIospVer )
  {
    // Setup 1: data file (1-8), index file (1-8).
    if ( ! gribInit_1_8() ) return false;
    if ( ! indexSetup1_8() ) return false;

    netcdfObj = openNc( ncObjType, gribIospVer );

    int timeDimLengthExpected = 8;
    Dimension timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 2: data file (1-12, CHANGE), index file (1-8).
    if ( ! gribAppend9_12() ) return false;
    if ( ! syncNc( netcdfObj ) ) return false;

    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 2: data file (1-12), index file (1-12, CHANGE).
    if ( ! indexSetup1_12() ) return false;
    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 12;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 2: data file (1-18, CHANGE), index file (1-18, CHANGE).
    if ( ! gribAppend13_18() ) return false;
    if ( ! indexSetup1_18() ) return false;
    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 18;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 2: data file (1-21, CHANGE), index file (1-21, CHANGE).
    if ( ! gribAppend19_21() ) return false;
    if ( ! indexSetup1_21() ) return false;

    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 21;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    return true;
  }

  public void testExistingOutOfDateIndex_ServerWithExternalIndexer_NcFile_NewGribIosp()
  {
    // Setup for "Server with external indexer" user story.
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    runTestExistingOutOfDateIndex( NcObjectType.FILE, GribIospVersion.NEW );
  }

  /**
   * Test existing index in "Server with External Indexer" user story
   * with NetcdfDataset using the GribGrid IOSP (new).
   */
  public void testExistingOutOfDateIndex_ServerWithExternalIndexer_NcDataset_NewGribIosp()
  {
    // Setup for "Server with external indexer" user story.
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    runTestExistingOutOfDateIndex( NcObjectType.DATASET, GribIospVersion.NEW );
  }

  private boolean runTestExistingOutOfDateIndex( NcObjectType ncObjType, GribIospVersion gribIospVer )
  {
    // Setup 1: data file (1-12, CHANGE), index file (1-8, CHANGE).
    if ( ! gribInit_1_8() ) return false;
    if ( ! indexSetup1_8() ) return false;
    if ( ! gribAppend9_12() ) return false;

    netcdfObj = openNc( ncObjType, gribIospVer );

    int timeDimLengthExpected = 8;
    Dimension timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 2: data file (1-12), index file (1-12, CHANGE).
    if ( ! indexSetup1_12() ) return false;
    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 12;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 3: data file (1-18, CHANGE), index file (1-18, CHANGE).
    if ( ! gribAppend13_18() ) return false;
    if ( ! indexSetup1_18() ) return false;
    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 18;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    return true;
  }

  /**
   * Test existing index in "Server with External Indexer" user story
   * with NetcdfFile using the GribGrid IOSP (new).
   */
  public void testInitiallyMissingUpdatingIndex_ServerWithExternalIndexer_NcFile_NewGribIosp()
  {
    // Setup for "Server with external indexer" user story.
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    runTestInitiallyMissingUpdatingIndex( NcObjectType.FILE, GribIospVersion.NEW );
  }

  /**
   * Test existing index in "Server with External Indexer" user story
   * with NetcdfDataset using the GribGrid IOSP (new).
   */
  public void testInitiallyMissingUpdatingIndex_ServerWithExternalIndexer_NcDataset_NewGribIosp()
  {
    // Setup for "Server with external indexer" user story.
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    runTestInitiallyMissingUpdatingIndex( NcObjectType.DATASET, GribIospVersion.NEW );
  }

//  /**
//   * Test existing index in "Server with External Indexer" user story
//   * with NetcdfFile using the Grib IOSP (old).
//   */
//  public void testInitiallyMissingUpdatingIndex_ServerWithExternalIndexer_NcFile_OldGribIosp()
//  {
//    // Setup for "Server with external indexer" user story.
//    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
//    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );
//
//    runTestInitiallyMissingUpdatingIndex( NcObjectType.FILE, GribIospVersion.OLD );
//  }
//
//  /**
//   * Test existing index in "Server with External Indexer" user story
//   * with NetcdfFile using the Grib IOSP (old).
//   */
//  public void testInitiallyMissingUpdatingIndex_ServerWithExternalIndexer_NcDataset_OldGribIosp()
//  {
//    // Setup for "Server with external indexer" user story.
//    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
//    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );
//
//    runTestInitiallyMissingUpdatingIndex( NcObjectType.DATASET, GribIospVersion.OLD );
//  }

  /**
   * Test GRIB IOSP open() and sync() on growing GRIB file with an external
   * indexer but no index file on initial read. Expect: initial read to create
   * index file in cache directory, subsequent sync() calls to use index
   * created by external indexer.
   *
   * @param ncObjType the type of nc object to use (NetcdfFile, NetcdfDataset, etc).
   * @param gribIospVer the GRIB IOSP impl to use.
   * @return true if the test ran successfully, false on failure.
   */
  private boolean runTestInitiallyMissingUpdatingIndex( NcObjectType ncObjType, GribIospVersion gribIospVer )
  {
    DiskCache.simulateUnwritableDir = true;

    // Setup 1: data file (1-8), no index file.
    //         [Open should create index in cache.]
    if ( ! gribInit_1_8() ) return false;

    netcdfObj = openNc( ncObjType, gribIospVer );

    int timeDimLengthExpected = 8;
    Dimension timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 2: data file (1-18, CHANGE), index (1-12, CHANGE).
    if ( ! gribAppend9_12() ) return false;
    if ( ! indexSetup1_12() ) return false;
    if ( ! gribAppend13_18()) return false;

    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 12;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 3: data file (1-18), index (1-18, CHANGE)
    if ( ! indexSetup1_18() ) return false;
    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 18;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 3: data file (1-21, CHANGE), index (1-21, CHANGE)
    if ( ! gribAppend19_21() ) return false;
    if ( ! indexSetup1_21() ) return false;
    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 21;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    DiskCache.simulateUnwritableDir = false;

    return true;
  }

  /**
   * Test missing index/updating data in "Server with External Indexer" user story
   * with NetcdfFile using the GribGrid IOSP (new).
   */
  /*
  public void testMissingIndexUpdatingData_ServerWithExternalIndexer_NcFile_NewGribIosp()
  {
    // Setup for "Server with external indexer" user story.
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    runTestMissingIndexUpdatingData( NcObjectType.FILE, GribIospVersion.NEW );
  }
  */
  
  /**
   * Test missing index/updating data in "Server with External Indexer" user story
   * with NetcdfDataset using the GribGrid IOSP (new).
   */
  /*
  public void testMissingIndexUpdatingData_ServerWithExternalIndexer_NcDataset_NewGribIosp()
  {
    // Setup for "Server with external indexer" user story.
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    runTestMissingIndexUpdatingData( NcObjectType.DATASET, GribIospVersion.NEW );
  }

  private boolean runTestMissingIndexUpdatingData( NcObjectType ncObjType, GribIospVersion gribIospVer )
  {
    DiskCache.simulateUnwritableDir = true;

    // Setup 1: data file (1-8), no index file.
    //         [Open should create index in cache.]
    if ( ! gribInit_1_8() ) return false;

    netcdfObj = openNc( ncObjType, gribIospVer );

    int timeDimLengthExpected = 8;
    Dimension timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 2: data file (1-12, CHANGE), no index file.
    //          [Sync should extend index in cache.]
    if ( ! gribAppend9_12() ) return false;

    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 12;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    DiskCache.simulateUnwritableDir = false;

    return true;
  }
  */

  /**
   * Test missing index/updating data in "Server with External Indexer" user story
   * with NetcdfFile using the GribGrid IOSP (new).
   */
  public void testMissingIndexUpdatedThenAlternate_ServerWithExternalIndexer_NcFile_NewGribIosp()
  {
    // Setup for "Server with external indexer" user story.
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    runTestMissingIndexUpdatedThenAlternate( NcObjectType.FILE, GribIospVersion.NEW );
  }

  /**
   * Test missing index/updating data in "Server with External Indexer" user story
   * with NetcdfDataset using the GribGrid IOSP (new).
   */
  public void testMissingIndexUpdatedThenAlternate_ServerWithExternalIndexer_NcDataset_NewGribIosp()
  {
    // Setup for "Server with external indexer" user story.
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    runTestMissingIndexUpdatedThenAlternate( NcObjectType.DATASET, GribIospVersion.NEW );
  }

  private boolean runTestMissingIndexUpdatedThenAlternate( NcObjectType ncObjType, GribIospVersion gribIospVer )
  {
    DiskCache.simulateUnwritableDir = true;

    // Setup 1: data file (1-8), no index file.
    //         [Open should create index in cache.]
    if ( ! gribInit_1_8() ) return false;

    netcdfObj = openNc( ncObjType, gribIospVer );

    int timeDimLengthExpected = 8;
    Dimension timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 2: data file (1-8), index file (1-8, CHANGE).
    //          [Sync should not use index in cache. HOW TO TEST?]
    if ( ! indexSetup1_8() ) return false;

    if ( ! syncNc( netcdfObj ) ) return false;

    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 3: data file (1-12, CHANGE), index file (1-8).
    if ( ! gribAppend9_12()) return false;

    if ( ! syncNc( netcdfObj ) ) return false;

    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 4: data file (1-12), index file (1-12, CHANGE).
    if ( ! indexSetup1_12()) return false;

    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 12;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 5: data file (1-18, CHANGE), index file (1-12).
    if ( ! gribAppend13_18()) return false;

    if ( ! syncNc( netcdfObj ) ) return false;

    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 6: data file (1-18), index file (1-18, CHANGE).
    if ( ! indexSetup1_18()) return false;

    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 18;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    // Setup 7: data file (1-21, CHANGE), index file (1-21, CHANGE).
    if ( ! gribAppend19_21()) return false;
    if ( ! indexSetup1_21()) return false;

    if ( ! syncNc( netcdfObj ) ) return false;

    timeDimLengthExpected = 21;
    timeDim = netcdfObj.findDimension( "time" );
    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
                  timeDim.getLength(),
                  timeDimLengthExpected );

    DiskCache.simulateUnwritableDir = false;

    return true;
  }

//  /**
//   * Test missing index/updating data in "Server with External Indexer" user story
//   * with NetcdfFile using the GribGrid IOSP (new).
//   */
//  public void testMissingIndexAdded_ServerWithExternalIndexer_NcFile_NewGribIosp()
//  {
//    // Setup for "Server with external indexer" user story.
//    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
//    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );
//
//    runTestMissingIndexAdded( NcObjectType.FILE, GribIospVersion.NEW );
//  }
//
//  /**
//   * Test missing index/updating data in "Server with External Indexer" user story
//   * with NetcdfDataset using the GribGrid IOSP (new).
//   */
//  public void testMissingIndexAdded_ServerWithExternalIndexer_NcDataset_NewGribIosp()
//  {
//    // Setup for "Server with external indexer" user story.
//    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
//    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );
//
//    runTestMissingIndexAdded( NcObjectType.DATASET, GribIospVersion.NEW );
//  }
//
//  private boolean runTestMissingIndexAdded( NcObjectType ncObjType, GribIospVersion gribIospVer )
//  {
//    DiskCache.simulateUnwritableDir = true;
//
//    // Setup 1: data file (1-8), no index file.
//    //         [Open should create index in cache.]
//    if ( ! gribInit_1_8() ) return false;
//
//    netcdfObj = openNc( ncObjType, gribIospVer );
//
//    int timeDimLengthExpected = 8;
//    Dimension timeDim = netcdfObj.findDimension( "time" );
//    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
//                  timeDim.getLength(),
//                  timeDimLengthExpected );
//
//    // Setup 2: data file (1-8), index file (1-8, CHANGE).
//    //          [Sync should not use index in cache. HOW TO TEST?]
//    if ( !indexSetup1_8() ) return false;
//
//    if ( ! syncNc( netcdfObj ) ) return false;
//
//    timeDim = netcdfObj.findDimension( "time" );
//    assertEquals( "Length of time dimension [" + timeDim.getLength() + "] not as expected [" + timeDimLengthExpected + "].",
//                  timeDim.getLength(),
//                  timeDimLengthExpected );
//
//    DiskCache.simulateUnwritableDir = false;
//
//    return true;
//  }
 /*
  public void testBadIndexFileWithExtendModeFalse()
  {
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    // Setup dataset to use partial GRIB index file.
    if ( ! setupGribAndPartialIndex0() )
      return;
    if ( ! switchToBinaryBadGribIndex() )
      return;

    long badIndexFileLastModified = indexFile.lastModified();
    long badIndexFileLength = indexFile.length();

    // Initial opening of the data file.
    try { netcdfObj = NetcdfFile.open( dataFile.getPath() ); }
    catch ( IOException e )
    {
      assertTrue( "Index file has changed: either last mod time ["
                  + indexFile.lastModified() + "] not as expected [" + badIndexFileLastModified
                  + "]; and/or length [" + indexFile.length() + "] not as expected [" + badIndexFileLength + "].",
                  badIndexFileLastModified == indexFile.lastModified()
                  || badIndexFileLength == indexFile.length() );

      fail( " IOException was thrown - should have rewritten the index on read failure");
    }

  }
  */
  /**
   * tests the TDS configuration : there is no index available on first access to the Grib file, so
   * an index is created in the cache directory because the TDS doesn't have write permission in the
   * same directory as the Grib file. Then future accesses use the index in the  same
   * directory as the Grib file.
   */
  public void testNoIndexTDS()
  {
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( false );

    // Setup dataset to use partial GRIB index file.
    if ( ! setupGrib() )
      return;

    DiskCache.simulateUnwritableDir = true;

    // Initial opening of the data file.
    try { netcdfObj = NetcdfFile.open( dataFile.getPath() ); }
    catch ( IOException e )
    {
      fail( "exception opening");
    }

    Dimension timePartial = netcdfObj.findDimension( "time" );

    assertTrue( "Time dimension ["+ timePartial.getLength()+"] not as expected [4].", timePartial.getLength() == 4 );

    // Switch to use the complete GRIB file.
    if ( ! switchToCompleteGrib() )
      return;

    if ( ! switchToCompleteGribIndex() )
      return;

    // sync() the dataset with new index.
    try
    {
      netcdfObj.sync();
    }
    catch ( IOException e )
    {
      fail( "Failed to sync() data file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return;
    }

    Dimension timeNew = netcdfObj.findDimension( "time" );

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
      ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
      ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( true  );

      // Setup dataset to use partial GRIB index file.
      if ( ! setupGrib() )
        return;

      // Initial opening of the data file.
      try { netcdfObj = NetcdfFile.open( dataFile.getPath() ); }
      catch ( IOException e )
      {
        fail( "exception opening");
      }

      Dimension timePartial = netcdfObj.findDimension( "time" );

      assertTrue( "Time dimension ["+ timePartial.getLength()+"] not as expected [4].", timePartial.getLength() == 4 );

      // Switch to use the complete GRIB file.
      if ( ! switchToCompleteGrib() )
        return;

      try
      {
        netcdfObj.sync();
      }
      catch ( IOException e )
      {
        fail( "Failed to sync() data file [" + dataFile.getPath() + "]: " + e.getMessage() );
        return;
      }

      Dimension timeNew = netcdfObj.findDimension( "time" );

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
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( false );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( true );

    // Setup dataset to use partial GRIB index file.
    if ( ! setupGribAndPartialIndex0() )
      return;

    // Switch to use the complete GRIB file.
    if ( ! switchToCompleteGrib() )
      return;

    // Initial opening of the data file.
    try { netcdfObj = NetcdfFile.open( dataFile.getPath() ); }
    catch ( IOException e )
    {
      fail( "exception opening");
    }

    Dimension timeComplete = netcdfObj.findDimension( "time" );

    assertTrue( "Time dimension ["+ timeComplete.getLength()+"] not as expected [21].", timeComplete.getLength() == 21 );

    // sync() the dataset with  index.
    try
    {
      netcdfObj.sync();
    }
    catch ( IOException e )
    {
      fail( "Failed to sync() data file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return;
    }

    Dimension timeNew = netcdfObj.findDimension( "time" );

    assertTrue( "Time dimension [" + timeNew.getLength() + "] not as expected [21].", timeNew.getLength() == 21 );

  }
  /*
  public void testAlwaysInCacheAndExtendModeTrue()
  {
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( true );
    ucar.nc2.iosp.grid.GridServiceProvider.setExtendIndex( true );

    // Setup dataset to use partial GRIB index file.
    if ( ! setupGribAndPartialIndex0() )
      return;

    // Initial opening of the data file.
    try { netcdfObj = NetcdfFile.open( dataFile.getPath() ); }
    catch ( IOException e )
    {
      fail( "exception opening");
    }

    Dimension timeComplete = netcdfObj.findDimension( "time" );

    assertTrue( "Time dimension ["+ timeComplete.getLength()+"] not as expected [4].",
                timeComplete.getLength() == 4 );

    // Switch to use the complete GRIB file.
    if ( ! switchToCompleteGrib() )
      return;

    // sync() the dataset  .
    try
    {
      netcdfObj.sync();
    }
    catch ( IOException e )
    {
      fail( "Failed to sync() data file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return;
    }

    Dimension timeNew = netcdfObj.findDimension( "time" );

    assertTrue( "Time dimension [" + timeNew.getLength() + "] not as expected [21].", timeNew.getLength() == 21 );
  }
  */
  
  private boolean setupGribAndPartialIndex0()
  {
    // Locate the source data file and check that it exists and can be read.
    File sourceDataFile = new File( dataDir, dataFileName + ".part0" );
    if ( ! sourceDataFile.exists() )
    {
      fail( "Non-existent source data file [" + sourceDataFile.getPath() + "].");
      return false;
    }
    if ( ! sourceDataFile.canRead() )
    {
      fail( "Cannot read source data file [" + sourceDataFile.getPath() + "]." );
      return false;
    }

    // Copy source grib file into place
    try
    { IO.copyFile( sourceDataFile, dataFile ); }
    catch ( IOException e )
    {
      fail( "Failed to copy partial grib file [" + sourceDataFile.getPath() + "] to grib file [" + dataFile.getPath() + "]: " + e.getMessage());
      return false;
    }

    // Check that the GRIB data file exists and is readable.
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

    // Check that the GRIB index file exists and is readable.
    if ( ! indexFile.exists() )
    {
      fail( "Non-existent index file [" + indexFile.getPath() + "]." );
      return false;
    }
    if ( ! indexFile.canRead() )
    {
      fail( "Cannot read index file [" + indexFile.getPath() + "]." );
      return false;
    }

    return true;
  }

  private boolean setupGrib()
  {
    // Locate source data file and check that it exists and is readable.
    File sourceDataFile = new File( dataDir, dataFileName + ".part0" );
    if ( ! sourceDataFile.exists())
    {
      fail( "Non-existent source data file [" + sourceDataFile.getPath() + "].");
      return false;
    }
    if ( ! sourceDataFile.canRead() )
    {
      fail( "Cannot read data file [" + sourceDataFile.getPath() + "]." );
      return false;
    }

    // Copy source grib file into place
    try
    { IO.copyFile( sourceDataFile, dataFile ); }
    catch ( IOException e )
    {
      fail( "Failed to copy partial grib file [" + sourceDataFile.getPath() + "] to grib file [" + dataFile.getPath() + "]: " + e.getMessage());
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

    return true;
  }

  private boolean switchToCompleteGrib()
  {
    RandomAccessFile input = null, output = null;
    try {
    // read in extra data
    input = new RandomAccessFile( dataFile.getPath() +".extra", "r");
    byte[] extra = new byte[ (int) input.length()];
    input.read( extra );
    input.close();

    output = new RandomAccessFile( dataFile.getPath(), "rw");
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
    // Check that the complete index file exists and is readable.
    indexFileFull = new File( dataDir, dataFileName + indexSuffix );
    if ( ! indexFileFull.exists() )
    {
      fail( "Non-existent full index file [" + indexFileFull + "]." );
      return false;
    }
    if ( ! indexFileFull.canRead() )
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

    // Check that the GRIB data file exists and is readable.
    if ( ! indexFile.exists() )
    {
      fail( "Non-existent index file [" + indexFile.getPath() + "]." );
      return false;
    }
    if ( ! indexFile.canRead() )
    {
      fail( "Cannot read index file [" + indexFile.getPath() + "]." );
      return false;
    }

    return true;
  }

  enum NcObjectType
  {
    FILE( "ucar.nc2.NetcdfFile"),
    DATASET( "ucar.nc2.NetcdfDataset"),
    FMRC( "???thredds.catalog.InvDatasetFmrc???");

    private String className;
    private NcObjectType( String className) { this.className = className; }
    public String getClassName() { return this.className; }
  }

  enum GribIospVersion
  {
    //OLD( "ucar.nc2.iosp.grib.GribServiceProvider" ),
    NEW( "ucar.nc2.iosp.grib.GribGridServiceProvider" );

    private String className;
    private GribIospVersion( String className ) { this.className = className; } 
    public String getClassName() { return className; }
  }

  private NetcdfFile openNc( NcObjectType t, GribIospVersion gribIospVersion )
  {
    try
    {
      if ( t.equals( NcObjectType.FILE ))
        return NetcdfFile.open( dataFile.getPath(), gribIospVersion.getClassName(), -1, null, null );
      else if ( t.equals( NcObjectType.DATASET))
        return NetcdfDataset.open( dataFile.getPath(), gribIospVersion.getClassName(), -1, null, null );
      else
        fail( "Unknown NcObjectType [" + t.name() + "].");
    }
    catch ( IOException e )
    {
      fail( "Failed to open data file [" + dataFile.getPath() + "]: " + e.getMessage() );
    }
    catch ( Exception e) // ClassNotFoundException, InstantiationException, IllegalAccessException
    {
      fail( "Trouble while opening dataset [" + dataFile.getPath() + "]: " + e.getMessage());
    }
    return null;
  }

  private boolean syncNc( NetcdfFile ncf )
  {
    // sync() the dataset with  index.
    try { ncf.sync(); }
    catch ( IOException e )
    {
      fail( "Failed to sync() data file [" + ncf.getLocation() + "]: " + e.getMessage() );
      return false;
    }
    return true;
  }

  //===============
  // New setup for times1-8, times9-12, etc
  //===============

  private boolean gribInit_1_8()
  {

    // Check that the source GRIB file exists and is readable.
    File sourceDataFile = new File( dataDir, dataFileName + dataSuffix1_8 );
    if ( ! sourceDataFile.exists())
    {
      fail( "Non-existent source GRIB file [" + sourceDataFile.getPath() + "].");
      return false;
    }
    if ( ! sourceDataFile.canRead())
    {
      fail( "Cannot read the source GRIB file [" + sourceDataFile.getPath() + "].");
      return false;
    }

    // Copy source grib file into place
    try
    {
      IO.copyFile( sourceDataFile, dataFile );
    }
    catch ( IOException e )
    {
      fail( "Failed to copy partial grib file [" + sourceDataFile.getPath() + "] to grib file [" + dataFile.getPath() + "]: " + e.getMessage() );
      return false;
    }

    // Check that data file exists and can be read.
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

    // Check that index file exists and can be read.
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
