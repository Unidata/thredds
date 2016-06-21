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
package thredds.server.cdmrf;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.TestWithLocalServer;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.tools.DataFactory;
import thredds.server.catalog.TdsLocalCatalog;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsRdaData;

import java.io.IOException;

/**
 * Test RDA grib datasets in it
 *
 * @author caron
 * @since 2/24/2016.
 */
@Category(NeedsRdaData.class)
public class TestRdaProblems {

  // /thredds/cdmrfeature/grid/aggregations/g/ds094.2_t/GaussLatLon_880X1760-0p0000N-180p0000E?req=data&
  // var=Temperature_height_above_ground_Mixed_intervals_AverageNforecasts&timePresent=true

  // http://localhost:8081/thredds/catalog/rdaTest/ds094.2_t/catalog.html?dataset=rdaTest/ds094.2_t/GaussLatLon_880X1760-0p0000N-180p0000E
  @Test
  public void testIndexOutOfBounds() throws IOException, InvalidRangeException {
    //String endpoint = TestWithLocalServer.withPath("cdmrfeature/grid/rdaTest/ds094.2_t/GaussLatLon_880X1760-0p0000N-180p0000E");
    //String ccName = "ds094.2_t#GaussLatLon_880X1760-0p0000N-180p0000E";
    String covName = "Temperature_height_above_ground_Mixed_intervals_AverageNforecasts";
    //System.out.printf("%s%n", endpoint);

    Catalog cat = TdsLocalCatalog.open("catalog/rdaTest/ds094.2_t/catalog.xml");
    Assert.assertNotNull(cat);
    Dataset ds = cat.findDatasetByID("rdaTest/ds094.2_t/GaussLatLon_880X1760-0p0000N-180p0000E");

    DataFactory fac = new DataFactory();
    try ( DataFactory.Result result = fac.openFeatureDataset(ds, null)) {
      Assert.assertFalse(result.errLog.toString(), result.fatalError);
      Assert.assertNotNull(result.featureDataset);
      Assert.assertEquals(FeatureDatasetCoverage.class, result.featureDataset.getClass());

      FeatureDatasetCoverage fdc = (FeatureDatasetCoverage) result.featureDataset;
      CoverageCollection cc = fdc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.GRID.toString(), cc);
      Coverage cov = cc.findCoverage(covName);
      Assert.assertNotNull(covName, cov);

      SubsetParams subset = new SubsetParams().setTimePresent();
      GeoReferencedArray geo = cov.readData(subset);
      Array data = geo.getData();
      System.out.printf(" read data from %s shape = %s%n", cov.getName(), Misc.showInts(data.getShape()));
    }
  }

    /*

2016-02-26T13:02:24 [76516564] [104] INFO threddsServlet:  (184.96.199.251)
/thredds/cdmrfeature/grid/aggregations/g/ds084.3/1/TwoD?
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

    String covName = "Geopotential_height_isobaric";
    Catalog cat = TdsLocalCatalog.open("catalog/rdaTest/ds084.3/catalog.xml");
    Assert.assertNotNull(cat);
    Dataset ds = cat.findDatasetByID("rdaTest/ds084.3/TwoD");

    DataFactory fac = new DataFactory();
    try ( DataFactory.Result result = fac.openFeatureDataset(ds, null)) {
      Assert.assertFalse(result.errLog.toString(), result.fatalError);
      Assert.assertNotNull(result.featureDataset);
      Assert.assertEquals(FeatureDatasetCoverage.class, result.featureDataset.getClass());

      FeatureDatasetCoverage fdc = (FeatureDatasetCoverage) result.featureDataset;
      CoverageCollection cc = fdc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull(FeatureType.FMRC.toString(), cc);
      Coverage cov = cc.findCoverage(covName);
      Assert.assertNotNull(covName, cov);

      SubsetParams subset = new SubsetParams().setTimePresent();
      GeoReferencedArray geo = cov.readData(subset);
      Array data = geo.getData();
      System.out.printf(" read data from %s shape = %s%n", cov.getName(), Misc.showInts(data.getShape()));
    }
  }
}
