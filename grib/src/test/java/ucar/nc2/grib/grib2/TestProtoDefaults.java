/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
