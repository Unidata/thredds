package ucar.nc2.ft.point.collection;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.point.StationHelper;
import ucar.unidata.geoloc.Station;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Test CompositeStationCollections when caches are enabled
 *
 * These tests were written to check for locked files in the RandomAccessFile and NetcdfFile caches when in use.
 * This issue resulted in hundreds of file handles being locked in a cache when reading data from a
 * CompositeStationCollection in the TDS, which happens when a request touches multiple files within the collection.
 * Based on that behavior and the code used by the TDS, I was able to reproduce the behavior in netCDF-Java directly,
 * although it wasn't clear exactly how and it's done in a roundabout way.
 *
 */

@Category(NeedsCdmUnitTest.class)
public class TestCompositeStationCollectionsWithCaches {

  private static boolean checkNetcdfFileCache;
  private static boolean checkRafCache;

  @BeforeClass
  public static void setupCaches() {
    RandomAccessFile.enableDefaultGlobalFileCache();
    NetcdfDataset.initNetcdfFileCache(1, 10, 15, 200);
  }

  @Test
  public void testWithRafCache() throws IOException {
    checkRafCache = true;
    checkNetcdfFileCache = false;
    testOpenFileHandles();
  }

  @Test
  public void testWithNetcdfFileCache() throws IOException {
    checkRafCache = false;
    checkNetcdfFileCache = true;
    testOpenFileHandles();
  }

  @Test
  public void testWithRafAndNetcdfFileCache() throws IOException {
    checkRafCache = true;
    checkNetcdfFileCache = true;
    testOpenFileHandles();
  }

  @Test
  public void testWithNoCaches() throws IOException {
    checkRafCache = false;
    checkNetcdfFileCache = false;
    testOpenFileHandles();
  }

  private static void testRafCache() {
    List<String> cacheEntries = RandomAccessFile.getGlobalFileCache().showCache();
    testFileCache(cacheEntries);
  }

  private static void testNetcdfFileCache() {
    List<String> cacheEntries = NetcdfDataset.getNetcdfFileCache().showCache();
    testFileCache(cacheEntries);
  }

  private static void testFileCache(List<String> cacheEntries) {
    boolean isAnyFileLocked = false;
    Set<String> uniqueFileNames = new HashSet<>();
    for (String ce : cacheEntries) {
      // each cache entry looks like:
      // locked times_accessed last_modified <filename>
      // parse out file name and make sure that there are not
      // duplicate entries
      uniqueFileNames.add(ce.split(" ", 4)[3]);
      if(ce.startsWith("true")) {
        isAnyFileLocked = true;
      }
    }
    Assert.assertFalse(isAnyFileLocked);

    // each cache entry looks like:
    // locked times_accessed last_modified <filename>
    // parse out file name and make sure that there are not
    // duplicate entries
    Assert.assertEquals(uniqueFileNames.size(), cacheEntries.size());
  }

  public void testOpenFileHandles() throws IOException {

    if (checkRafCache) {
      if (RandomAccessFile.getGlobalFileCache() != null) {
        RandomAccessFile.getGlobalFileCache().enable();
      } else {
        RandomAccessFile.enableDefaultGlobalFileCache();
      }
    } else {
      RandomAccessFile.getGlobalFileCache().clearCache(true);
      RandomAccessFile.shutdown();
      // RandomAccessFile.getGlobalFileCache().disable();
    }


    if (checkNetcdfFileCache) {
      // FeatureCollections still use NetcdfDataset, so work with that cache.
      NetcdfDataset.getNetcdfFileCache().enable();
    } else {
      NetcdfDataset.getNetcdfFileCache().clearCache(true);
      NetcdfDataset.getNetcdfFileCache().disable();
    }

    // open the collection
    String collection = ucar.nc2.ft.point.collection.CompositeDatasetFactory.SCHEME + TestDir.cdmUnitTestDir
        + "/ft/station/gempak/collection_with_missing_station_features/#yyMMdd#.sf$";
    FeatureDataset fds = FeatureDatasetFactoryManager.open(FeatureType.STATION, collection, null, null);
    Assert.assertTrue(fds instanceof FeatureDatasetPoint);
    FeatureDatasetPoint fdp = (FeatureDatasetPoint) fds;

    // the collection dataset should have one feature collection, and it should
    // be a CompositeStationCollection
    Assert.assertEquals(fdp.getPointFeatureCollectionList().size(), 1);
    FeatureCollection dfc = fdp.getPointFeatureCollectionList().get(0);
    Assert.assertTrue(dfc instanceof CompositeStationCollection);
    CompositeStationCollection csc = (CompositeStationCollection) dfc;

    // now it gets tricky. We have to tickle the PointFeatureIterator for each station trigger
    // the bug, but the iterator is hidden within the station collection because the collection
    // is made up of multiple files. Here, we use the StationHelper to get at the PointFeatureIterator
    // at the individual file level of the collection.
    StationHelper helper = csc.createStationHelper();
    List<Station> stations = helper.getStations();

    // Ensure the RandomAccessFile cache does not contain any locked files
    if (checkRafCache) {
      testRafCache();
    }

    if (checkNetcdfFileCache) {
      testNetcdfFileCache();
    }

    // Now, iterate over the stations. Each station cycles through one or more files of the collection,
    // and the bug is that the file isn't closed or released if it does not contain the given station.
    for (int station = 0; station < 2; station++) {
      // Get the PointFeatureIterator for the given station
      PointFeatureIterator pointFeatureIterator =
          ((StationTimeSeriesFeature) stations.get(station)).getPointFeatureIterator(-1);
      // all we have to do is call .hasNext(), and if not, the underlying code will cycle through the
      // datasets of the collection but not close them
      pointFeatureIterator.hasNext();
      pointFeatureIterator.finish();
    }

    if (checkRafCache) {
      testRafCache();
    }

    if (checkNetcdfFileCache) {
      testNetcdfFileCache();
    }
  }

  @After
  public void cleanupAfterEach() {
    NetcdfDataset.getNetcdfFileCache().clearCache(true);
    RandomAccessFile.getGlobalFileCache().clearCache(true);
  }

  @AfterClass
  public static void cleanup() {
    NetcdfDataset.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
  }
}