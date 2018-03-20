/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsRdaData;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test RDA datasets not in cdmUnitTest.
 *
 * @author caron
 * @since 2/24/2016.
 */
@Category(NeedsRdaData.class)
public class TestRdaReading {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // time2D coordinate, not orthogonal, but times are unique
  // GribCollectionImmutable assumes time2D -> orthogonal
  // doesnt actually work since we only have the gbx9
  @Test
  public void testNonOrthMRUTC() throws IOException, InvalidRangeException {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));

    String endpoint = "D:/work/rdavm/ds277.6/monthly/ds277.6.ncx4";
    String ccName = "ds277.6#MRUTC-LatLon_418X360-4p83S-179p50W";
    String covName = "Salinity_depth_below_sea_Average";
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, fdc);
      CoverageCollection cc = fdc.findCoverageDataset(ccName);
      Assert.assertNotNull(ccName, cc);
      Coverage cov = cc.findCoverage(covName);
      Assert.assertNotNull(covName, cov);

      SubsetParams subset = new SubsetParams().setTimePresent();
      GeoReferencedArray geo = cov.readData(subset);
      Array data = geo.getData();
      System.out.printf(" read data from %s shape = %s%n", cov.getName(), Misc.showInts(data.getShape()));
    }

    Grib.setDebugFlags(new DebugFlagsImpl(""));
  }

  // /thredds/cdmrfeature/grid/aggregations/g/ds084.3/1/TwoD?req=data&var=v-component_of_wind_potential_vorticity_surface&timePresent=true
  @Test
  public void testNegDataSize() throws IOException, InvalidRangeException {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));

    String endpoint = "D:/work/rdavm/ds084.3/ds084.3.ncx4";
    String covName = "v-component_of_wind_potential_vorticity_surface";
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, fdc);
      CoverageCollection cc = fdc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.FMRC.toString(), cc);
      Coverage cov = cc.findCoverage(covName);
      Assert.assertNotNull(covName, cov);

      SubsetParams subset = new SubsetParams().setTimePresent();
      GeoReferencedArray geo = cov.readData(subset);
      Array data = geo.getData();
      System.out.printf(" read data from %s shape = %s%n", cov.getName(), Misc.showInts(data.getShape()));
    }

    Grib.setDebugFlags(new DebugFlagsImpl(""));
  }

  /* 2016-02-25T16:09:29 [1341825] [47] INFO threddsServlet:  (184.96.199.251) /thredds/cdmrfeature/grid/aggregations/g/ds083.2/2/Best?
  req=data&var=Soil_temperature_depth_below_surface_layer&timePresent=true
Caused by: java.lang.RuntimeException: masterRuntime does not contain runtime 2023-04-04T12:00:00Z
	at ucar.nc2.grib.collection.PartitionCollectionImmutable$VariableIndexPartitioned.getDataRecord(PartitionCollectionImmutable.java:684) ~[grib-5.0.0-SNAPSHOT.jar:5.0.0-SNAPSHOT]
	at ucar.nc2.grib.collection.GribDataReader.readDataFromPartition2(GribDataReader.java:223) ~[grib-5.0.0-SNAPSHOT.jar:5.0.0-SNAPSHOT]
	at ucar.nc2.grib.collection.GribDataReader.readData2(GribDataReader.java:192) ~[grib-5.0.0-SNAPSHOT.jar:5.0.0-SNAPSHOT]
	at ucar.nc2.grib.coverage.GribCoverageDataset.readData(GribCoverageDataset.java:1278) ~[grib-5.0.0-SNAPSHOT.jar:5.0.0-SNAPSHOT]
	at ucar.nc2.ft2.coverage.Coverage.readData(Coverage.java:196) ~[cdm-5.0.0-SNAPSHOT.jar:5.0.0-SNAPSHOT]
	at thredds.server.cdmrfeature.CdmrGridController.handleDataRequest(CdmrGridController.java:253) ~[classes/:5.0.0-SNAPSHOT]
 */

  //@Test
  public void testWrongRuntime() throws IOException, InvalidRangeException {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));

    String endpoint = "D:/work/rdavm/index/ds083.2_Grib2.ncx4";
    String covName = "Soil_temperature_depth_below_surface_layer";
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, fdc);
      CoverageCollection cc = fdc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.GRID.toString(), cc);
      Coverage cov = cc.findCoverage(covName);
      Assert.assertNotNull(covName, cov);

      SubsetParams subset = new SubsetParams().setTimePresent();
      GeoReferencedArray geo = cov.readData(subset);
      Array data = geo.getData();
      System.out.printf(" read data from %s shape = %s%n", cov.getName(), Misc.showInts(data.getShape()));
    }

    Grib.setDebugFlags(new DebugFlagsImpl(""));
  }

  /*

2016-02-26T13:02:24 [76516564] [104] INFO threddsServlet:  (184.96.199.251) /thredds/cdmrfeature/grid/aggregations/g/ds084.3/1/TwoD?
req=data&var=Geopotential_height_isobaric&timePresent=true

Caused by: java.lang.OutOfMemoryError: Java heap space
	at ucar.nc2.grib.collection.GribDataReader$DataReceiver.<init>(GribDataReader.java:391) ~[grib-5.0.0-SNAPSHOT.jar:5.0.0-SNAPSHOT]
	at ucar.nc2.grib.collection.GribDataReader.readDataFromPartition2(GribDataReader.java:232) ~[grib-5.0.0-SNAPSHOT.jar:5.0.0-SNAPSHOT]
	at ucar.nc2.grib.collection.GribDataReader.readData2(GribDataReader.java:192) ~[grib-5.0.0-SNAPSHOT.jar:5.0.0-SNAPSHOT]
	at ucar.nc2.grib.coverage.GribCoverageDataset.readData(GribCoverageDataset.java:1279) ~[grib-5.0.0-SNAPSHOT.jar:5.0.0-SNAPSHOT]
	at ucar.nc2.ft2.coverage.Coverage.readData(Coverage.java:196) ~[cdm-5.0.0-SNAPSHOT.jar:5.0.0-SNAPSHOT]
	at thredds.server.cdmrfeature.CdmrGridController.handleDataRequest(CdmrGridController.java:253) ~[classes/:5.0.0-SNAPSHOT]

Im thinking a bad integer causing huge mem alloc.
   */
  @Test
  public void testBadSize() throws IOException, InvalidRangeException {
    // Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));

    String endpoint = "D:/work/rdavm/ds084.3/ds084.3.ncx4";
    String covName = "Geopotential_height_isobaric";
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, fdc);
      CoverageCollection cc = fdc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.FMRC.toString(), cc);
      Coverage cov = cc.findCoverage(covName);
      Assert.assertNotNull(covName, cov);

      SubsetParams subset = new SubsetParams().setTimePresent();
      GeoReferencedArray geo = cov.readData(subset);
      Array data = geo.getData();
      System.out.printf(" read data from %s shape = %s%n", cov.getName(), Misc.showInts(data.getShape()));
    }

    Grib.setDebugFlags(new DebugFlagsImpl(""));
  }

}
