/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/**
 * misc tests involving CFPointWriter
 *
 * @author caron
 * @since 7/2/2014
 */
public class TestCFPointWriterMisc {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testPointProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/point/netcdf/Surface_Buoy_20090921_0000.nc";
    TestCFPointWriter.writeDataset(filename, FeatureType.POINT,
            new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf3), false, tempFolder.newFile());
  }

  @Test
  public void testProfileProblem() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "point/profileMultidimZJoin.ncml";
    TestCFPointWriter.writeDataset(filename, FeatureType.PROFILE,
            new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf3), false, tempFolder.newFile());
  }

  @Test
  public void testStationProfileProblem() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "point/stationProfileSingle.ncml";
    TestCFPointWriter.writeDataset(filename, FeatureType.STATION_PROFILE,
            new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf3), false, tempFolder.newFile());
  }

  @Test
   public void testProfileInnerTime() throws Exception {
     String file = TestDir.cdmLocalTestDataDir + "point/profileMultidimTimePrecise.ncml";
     Formatter buf = new Formatter();
     try (FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.PROFILE, file, null, buf)) {
       List<DsgFeatureCollection> collectionList = pods.getPointFeatureCollectionList();
       assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
       DsgFeatureCollection fc1 = collectionList.get(0);
       assert fc1 instanceof ProfileFeatureCollection;

       ProfileFeatureCollection profileCollection = (ProfileFeatureCollection) fc1;
       PointFeatureCollectionIterator iter = profileCollection.getPointFeatureCollectionIterator();
        while (iter.hasNext()) {
          PointFeatureCollection pfc = iter.next();
          assert pfc instanceof ProfileFeature : pfc.getClass().getName();
          ProfileFeature profile = (ProfileFeature) pfc;

          PointFeatureIterator inner = profile.getPointFeatureIterator();
          while (inner.hasNext()) {
            PointFeature pf = inner.next();
            StructureData sdata = pf.getFeatureData();
            StructureMembers.Member m = sdata.findMember("timePrecise");
            assert m != null : "missing timePrecise";
            assert m.getDataType() == DataType.DOUBLE : "time not a double";
            System.out.printf(" %s", sdata.getScalarDouble(m));
          }
          System.out.printf("%n");
        }

       try (FeatureDatasetPoint rewrite = rewriteDataset(pods, "nc", new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf3))) {
         collectionList = rewrite.getPointFeatureCollectionList();
         DsgFeatureCollection fc2 = collectionList.get(0);
         assert fc2 instanceof ProfileFeatureCollection;
         ProfileFeatureCollection profileCollection2 = (ProfileFeatureCollection) fc2;

         PointFeatureCollectionIterator iter2 = profileCollection2.getPointFeatureCollectionIterator();
         while (iter2.hasNext()) {
           PointFeatureCollection pfc = iter2.next();
           assert pfc instanceof ProfileFeature : pfc.getClass().getName();
           ProfileFeature profile = (ProfileFeature) pfc;

           PointFeatureIterator inner = profile.getPointFeatureIterator();
           while (inner.hasNext()) {
             PointFeature pf = inner.next();
             StructureData sdata = pf.getFeatureData();
             StructureMembers.Member m = sdata.findMember("timePrecise");
             assert m != null : "missing timePrecise";
             assert m.getDataType() == DataType.DOUBLE : "time not a double";
             System.out.printf(" %s", sdata.getScalarDouble(m));
           }
           System.out.printf("%n");
         }
       }
     }
   }


  @Test
   public void testAltUnits() throws Exception {
    // Ignore this test if NetCDF-4 isn't present.
    Assume.assumeTrue("NetCDF-4 C library not present.", Nc4Iosp.isClibraryPresent());

     String file = TestDir.cdmLocalTestDataDir + "point/stationRaggedContig.ncml";
     Formatter buf = new Formatter();
     try (FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.STATION, file, null, buf)) {
       List<DsgFeatureCollection> collectionList = pods.getPointFeatureCollectionList();
       assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
       DsgFeatureCollection fc1 = collectionList.get(0);
       assert fc1.getAltUnits() != null : "no Alt Units";
       assert fc1.getAltUnits().equalsIgnoreCase("m") : "Alt Units should be 'm'";

       FeatureDatasetPoint rewrite =  rewriteDataset(pods, "nc4", new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf4));
       collectionList = rewrite.getPointFeatureCollectionList();
       DsgFeatureCollection fc2 = collectionList.get(0);
       assert fc2 instanceof PointFeatureCC;

       assert fc2.getAltUnits() != null : "no Alt Units";
       assert fc2.getAltUnits().equalsIgnoreCase("m") : "Alt Units should be 'm'";

       rewrite.close();

     }
   }

  @Test
  // the z coordinate doesnt fit into the structures, but must be transferred to the rewritten dataset
  public void testPointZCoord() throws Exception {
    String file = TestDir.cdmLocalTestDataDir + "point/pointUnlimited.nc";
    Formatter buf = new Formatter();
    try (FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.POINT, file, null, buf)) {
      Assert.assertNotNull(fdpoint);
      List<DsgFeatureCollection> collectionList = fdpoint.getPointFeatureCollectionList();
      assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
      DsgFeatureCollection fc = collectionList.get(0);
      assert fc instanceof PointFeatureCollection;

      NetcdfFile ncfile = fdpoint.getNetcdfFile();
      Assert.assertNotNull(ncfile);
      Assert.assertNotNull("cant find variable 'z' in netcdf file", ncfile.findVariable("z"));

      FeatureDatasetPoint rewrite =  rewriteDataset(fdpoint, "nc3", new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf3));
      collectionList = rewrite.getPointFeatureCollectionList();
      fc = collectionList.get(0);
      assert fc instanceof PointFeatureCollection;

      ncfile = rewrite.getNetcdfFile();
      Assert.assertNotNull(ncfile);
      Assert.assertNotNull("cant find variable 'z' in rewritten netcdf file", ncfile.findVariable("z"));

      rewrite.close();
    }
  }

  @Test
  // the z coordinate doesnt fit into the structures, but must be transferred to the rewritten dataset
  public void testStationMultidimZCoord() throws Exception {
    String file = TestDir.cdmLocalTestDataDir + "point/stationMultidim.ncml";
    Formatter buf = new Formatter();
    try (FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.STATION, file, null, buf)) {
      Assert.assertNotNull(fdpoint);
      List<DsgFeatureCollection> collectionList = fdpoint.getPointFeatureCollectionList();
      assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
      DsgFeatureCollection fc = collectionList.get(0);
      assert fc instanceof StationTimeSeriesFeatureCollection;

      NetcdfFile ncfile = fdpoint.getNetcdfFile();
      Assert.assertNotNull(ncfile);
      Assert.assertNotNull("cant find variable 'z' in netcdf file", ncfile.findVariable("z"));

      FeatureDatasetPoint rewrite =  rewriteDataset(fdpoint, "nc3", new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf3));
      collectionList = rewrite.getPointFeatureCollectionList();
      fc = collectionList.get(0);
      assert fc instanceof StationTimeSeriesFeatureCollection;

      ncfile = rewrite.getNetcdfFile();
      Assert.assertNotNull(ncfile);
      Assert.assertNotNull("cant find variable 'z' in rewritten netcdf file", ncfile.findVariable("z"));

      Variable data = ncfile.findVariable("data");
      Assert.assertNotNull("cant find variable 'data' in rewritten netcdf file", data);
      Assert.assertEquals(2, data.getRank());
      Assert.assertEquals("obs", data.getDimension(0).getShortName());
      Assert.assertEquals("z", data.getDimension(1).getShortName());

      rewrite.close();
    }
  }

  FeatureDatasetPoint rewriteDataset(FeatureDatasetPoint fdpoint, String prefix, CFPointWriterConfig config) throws IOException {
    String location = fdpoint.getLocation();
    if (location.startsWith("file:")) location = location.substring(5);
    File fileIn = new File(location);
    long start = System.currentTimeMillis();

    int pos = location.lastIndexOf("/");
    String name = location.substring(pos + 1);
    //String prefix = (config.version == NetcdfFileWriter.Version.netcdf3) ? ".nc" : (config.version == NetcdfFileWriter.Version.netcdf4) ? ".nc4" : ".nc4c";
    if (!name.endsWith(prefix)) name = name + prefix;
    File fileOut = tempFolder.newFile();

    //String absIn = fileIn.getAbsolutePath();
    //absIn = StringUtil2.replace(absIn, "\\", "/");
    //String absOut = fileOut.getAbsolutePath();
    //absOut = StringUtil2.replace(absOut, "\\", "/");
    System.out.printf("================ TestCFPointWriter%n read %s size=%d%n write to=%s%n", fileIn.getAbsolutePath(), fileIn.length(), fileOut.getAbsolutePath());

    int count = CFPointWriter.writeFeatureCollection(fdpoint, fileOut.getPath(), config);
    long took = System.currentTimeMillis() - start;
    System.out.printf(" nrecords written = %d took=%d msecs%n%n", count, took);

    ////////////////////////////////
    // open result

    System.out.printf(" open result dataset=%s size = %d (%f ratio out/in) %n", fileOut.getPath(), fileOut.length(), ((double) fileOut.length() / fileIn.length()));
    Formatter errlog = new Formatter();
    FeatureDataset result = FeatureDatasetFactoryManager.open(null, fileOut.getPath(), null, errlog);
    if (result == null) {
      System.out.printf(" **failed --> %n%s <--END FAIL messages%n", errlog);
      assert false;
    }
    assert result instanceof FeatureDatasetPoint;

    return (FeatureDatasetPoint) result;
  }
}
