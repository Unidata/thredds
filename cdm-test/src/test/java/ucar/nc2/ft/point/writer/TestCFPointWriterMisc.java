/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.writer;

import org.junit.Assume;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.ft.*;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * misc tests involving CFPointWriter
 *
 * @author caron
 * @since 7/2/2014
 */
public class TestCFPointWriterMisc {

  @Test
   public void testProfileInnerTime() throws Exception {
     String file = TestDir.cdmLocalTestDataDir + "point/profileMultidimTimePrecise.ncml";
     Formatter buf = new Formatter();
     try (FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.PROFILE, file, null, buf)) {
       List<FeatureCollection> collectionList = pods.getPointFeatureCollectionList();
       assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
       NestedPointFeatureCollection fc1 = (NestedPointFeatureCollection) collectionList.get(0);
       assert fc1 instanceof ProfileFeatureCollection;

       ProfileFeatureCollection profileCollection = (ProfileFeatureCollection) fc1;
       PointFeatureCollectionIterator iter = profileCollection.getPointFeatureCollectionIterator(-1);
        while (iter.hasNext()) {
          PointFeatureCollection pfc = iter.next();
          assert pfc instanceof ProfileFeature : pfc.getClass().getName();
          ProfileFeature profile = (ProfileFeature) pfc;

          PointFeatureIterator inner = profile.getPointFeatureIterator(-1);
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
         FeatureCollection fc2 = collectionList.get(0);
         assert fc2 instanceof NestedPointFeatureCollection;
         NestedPointFeatureCollection npc = (NestedPointFeatureCollection) fc2;
         assert npc instanceof ProfileFeatureCollection;

         ProfileFeatureCollection profileCollection2 = (ProfileFeatureCollection) npc;
         PointFeatureCollectionIterator iter2 = profileCollection2.getPointFeatureCollectionIterator(-1);
         while (iter2.hasNext()) {
           PointFeatureCollection pfc = iter2.next();
           assert pfc instanceof ProfileFeature : pfc.getClass().getName();
           ProfileFeature profile = (ProfileFeature) pfc;

           PointFeatureIterator inner = profile.getPointFeatureIterator(-1);
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
       List<FeatureCollection> collectionList = pods.getPointFeatureCollectionList();
       assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
       NestedPointFeatureCollection fc1 = (NestedPointFeatureCollection) collectionList.get(0);
       assert fc1.getAltUnits() != null : "no Alt Units";
       assert fc1.getAltUnits().equalsIgnoreCase("m") : "Alt Units should be 'm'";

       FeatureDatasetPoint rewrite =  rewriteDataset(pods, "nc4", new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf4));
       collectionList = rewrite.getPointFeatureCollectionList();
       FeatureCollection fc2 = collectionList.get(0);
       assert fc2 instanceof NestedPointFeatureCollection;
       NestedPointFeatureCollection npc = (NestedPointFeatureCollection) fc2;

       assert npc.getAltUnits() != null : "no Alt Units";
       assert npc.getAltUnits().equalsIgnoreCase("m") : "Alt Units should be 'm'";

       rewrite.close();

     }
   }

  @Test
  // the z coordinate doesnt fit into the structures, need a way to get it into the rewritten dataset
  public void testPointZCoord() throws Exception {
    String file = TestDir.cdmLocalTestDataDir + "point/pointUnlimited.nc";
    Formatter buf = new Formatter();
    try (FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.POINT, file, null, buf)) {
      List<FeatureCollection> collectionList = fdpoint.getPointFeatureCollectionList();
      assert (collectionList.size() == 1) : "Can't handle point data with multiple collections";
      FeatureCollection fc = collectionList.get(0);
      assert fc instanceof PointFeatureCollection;
      PointFeatureCollection pc = (PointFeatureCollection) fc;

      NetcdfFile ncfile = fdpoint.getNetcdfFile();
      assert ncfile != null;
      assert null != ncfile.findVariable("z") : "cant find variable 'z' in netcdf file";
      assert null != findExtraVariable(pc, "z") : "cant find variable 'z' in feature collection";

      FeatureDatasetPoint rewrite =  rewriteDataset(fdpoint, "nc3", new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf3));
      collectionList = rewrite.getPointFeatureCollectionList();
      fc = collectionList.get(0);
      assert fc instanceof PointFeatureCollection;
      pc = (PointFeatureCollection) fc;

      ncfile = rewrite.getNetcdfFile();
      assert ncfile != null;
      assert null != ncfile.findVariable("z") : "cant find variable 'z' in rewritten netcdf file";
      assert null != findExtraVariable(pc, "z") : "cant find variable 'z' in rewritten feature collection";

      rewrite.close();
    }
  }

  Variable findExtraVariable(PointFeatureCollection pc, String name) {
    for (Variable v : pc.getExtraVariables())
      if (v.getFullName().equals(name)) return v;
    return null;
  }

  FeatureDatasetPoint rewriteDataset(FeatureDatasetPoint fdpoint, String prefix, CFPointWriterConfig config) throws IOException {
    String location = fdpoint.getLocation();
    File fileIn = new File(fdpoint.getLocation());
    long start = System.currentTimeMillis();

    int pos = location.lastIndexOf("/");
    String name = location.substring(pos + 1);
    //String prefix = (config.version == NetcdfFileWriter.Version.netcdf3) ? ".nc" : (config.version == NetcdfFileWriter.Version.netcdf4) ? ".nc4" : ".nc4c";
    if (!name.endsWith(prefix)) name = name + prefix;
    File fileOut = new File(TestDir.temporaryLocalDataDir, name);

    String absIn = fileIn.getAbsolutePath();
    absIn = StringUtil2.replace(absIn, "\\", "/");
    String absOut = fileOut.getAbsolutePath();
    absOut = StringUtil2.replace(absOut, "\\", "/");
    System.out.printf("================ TestCFPointWriter%n read %s size=%d%n write to=%s%n", absIn, fileIn.length(), absOut);

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
