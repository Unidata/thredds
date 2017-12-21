/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.grib.grib2;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.coord.SparseArray;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribHorizCoordSystem;
import ucar.nc2.grib.grib1.Grib1Gds;
import ucar.nc2.grib.grib1.Grib1SectionGridDefinition;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 10/16/2015.
 */
public class TestProtoDefaults {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Ignore("Needs updating to ncx4")
  @Test
  public void testProto2Default() throws IOException {
    final String testfile = "../grib/src/test/data/radar_national.grib.proto2d.ncx3";
    try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(testfile,  new FeatureCollectionConfig(), false, logger)) {
      Assert.assertNotNull(gc);
      Assert.assertEquals(true, gc.isGrib1);

      Assert.assertEquals(2, gc.getVersion());
      GribCollectionImmutable.Dataset ds = gc.getDatasetCanonical();
      Assert.assertNotNull(ds);
      Assert.assertEquals(1, ds.getGroupsSize());

      GribCollectionImmutable.GroupGC group = ds.getGroup(0);
      Assert.assertNotNull(group);

      GribHorizCoordSystem ghcs = group.getGribHorizCoordSys();
      Assert.assertNotNull(ghcs);

      GdsHorizCoordSys hcs = ghcs.getHcs();
      Assert.assertNotNull(hcs);

      Grib1SectionGridDefinition gdss = new Grib1SectionGridDefinition(ghcs.getRawGds());
      Assert.assertNotNull(gdss);

      Grib1Gds gds = gdss.getGDS();
      Assert.assertNotNull(gds);

      Assert.assertEquals(64, gds.getScanMode());

      List<GribCollectionImmutable.VariableIndex> vars = group.getVariables();
      Assert.assertEquals(1, vars.size());
      GribCollectionImmutable.VariableIndex var = vars.get(0);
      var.readRecords();

      SparseArray<GribCollectionImmutable.Record> sa = var.getSparseArray();
      Assert.assertNotNull(sa);
      Assert.assertEquals(1, sa.getTotalSize());

      //GribCollectionImmutable.Record r = sa.getContent(0);
      //Assert.assertEquals(9999, r.scanMode);
    }
  }

  @Ignore("Needs updating to ncx4")
  @Test
  public void testProto3Default() throws IOException {
    final String testfile = "../grib/src/test/data/radar_national.grib.proto3d.ncx3";
    try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(testfile, new FeatureCollectionConfig(), false, logger)) {
      Assert.assertNotNull(gc);
      Assert.assertEquals(true, gc.isGrib1);

      Assert.assertEquals(3, gc.getVersion());
      GribCollectionImmutable.Dataset ds = gc.getDatasetCanonical();
      Assert.assertNotNull(ds);
      Assert.assertEquals(1, ds.getGroupsSize());

      GribCollectionImmutable.GroupGC group = ds.getGroup(0);
      Assert.assertNotNull(group);

      GribHorizCoordSystem ghcs = group.getGribHorizCoordSys();
      Assert.assertNotNull(ghcs);

      GdsHorizCoordSys hcs = ghcs.getHcs();
      Assert.assertNotNull(hcs);

      Grib1SectionGridDefinition gdss = new Grib1SectionGridDefinition(ghcs.getRawGds());
      Assert.assertNotNull(gdss);

      Grib1Gds gds = gdss.getGDS();
      Assert.assertNotNull(gds);

      Assert.assertEquals(64, gds.getScanMode());

      List<GribCollectionImmutable.VariableIndex> vars = group.getVariables();
      Assert.assertEquals(1, vars.size());
      GribCollectionImmutable.VariableIndex var = vars.get(0);
      var.readRecords();

      SparseArray<GribCollectionImmutable.Record> sa = var.getSparseArray();
      Assert.assertNotNull(sa);
      Assert.assertEquals(1, sa.getTotalSize());

      //GribCollectionImmutable.Record r = sa.getContent(0);
      //Assert.assertEquals(0, r.scanMode);
    }
  }

  @Ignore("Needs updating to ncx4")
  @Test
  public void testProto3Set() throws IOException {
    final String testfile = "../grib/src/test/data/radar_national.grib.proto3.ncx3";
    try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(testfile, new FeatureCollectionConfig(), false, logger)) {
      Assert.assertNotNull(gc);
      Assert.assertEquals(true, gc.isGrib1);

      Assert.assertEquals(3, gc.getVersion());
      GribCollectionImmutable.Dataset ds = gc.getDatasetCanonical();
      Assert.assertNotNull(ds);
      Assert.assertEquals(1, ds.getGroupsSize());

      GribCollectionImmutable.GroupGC group = ds.getGroup(0);
      Assert.assertNotNull(group);

      GribHorizCoordSystem ghcs = group.getGribHorizCoordSys();
      Assert.assertNotNull(ghcs);

      GdsHorizCoordSys hcs = ghcs.getHcs();
      Assert.assertNotNull(hcs);

      Grib1SectionGridDefinition gdss = new Grib1SectionGridDefinition(ghcs.getRawGds());
      Assert.assertNotNull(gdss);

      Grib1Gds gds = gdss.getGDS();
      Assert.assertNotNull(gds);

      Assert.assertEquals(64, gds.getScanMode());

      List<GribCollectionImmutable.VariableIndex> vars = group.getVariables();
      Assert.assertEquals(1, vars.size());
      GribCollectionImmutable.VariableIndex var = vars.get(0);
      var.readRecords();

      SparseArray<GribCollectionImmutable.Record> sa = var.getSparseArray();
      Assert.assertNotNull(sa);
      Assert.assertEquals(1, sa.getTotalSize());

      //GribCollectionImmutable.Record r = sa.getContent(0);
      //Assert.assertEquals(64, r.scanMode);
    }
  }
}
