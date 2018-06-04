package thredds.tds;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.tools.DataFactory;
import thredds.server.catalog.TdsLocalCatalog;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;

/**
 * An featureCollection FMRC with inner and outer NcML, from John Mauer
 * http://oos.soest.hawaii.edu/thredds/catalog/hioos/roms_forec/hiig/catalog.html?dataset=roms_hiig_forecast/HI-ROMS_Forecast_Model_Run_Collection_best.ncd
 *
 * @author caron
 * @since Sep 24, 2010
 */
@Category(NeedsCdmUnitTest.class)
@Ignore("FeatureCollection is empty because cdmUnitTest/tds/hioos is empty.")
public class TestWaveModel {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testNcml() throws IOException, InvalidRangeException {
    String catalog = "/catalog/hioos/model/wav/swan/oahu/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    String id = "hioos/model/wav/swan/oahu/SWAN_Oahu_Regional_Wave_Model_(500m)_best.ncd";
    Dataset ds = cat.findDatasetByID(id);
    assert (ds != null) : "cant find dataset id="+id;
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset( ds, null)) {
      assert !dataResult.fatalError;
      assert dataResult.featureDataset != null;

      FeatureDatasetCoverage gds = (FeatureDatasetCoverage) dataResult.featureDataset;
      String gridName = "salt";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      Assert.assertEquals(1, gds.getCoverageCollections().size());
      CoverageCollection cc = gds.getCoverageCollections().get(0);
      Coverage grid = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      CoverageCoordSys gcs = grid.getCoordSys();
      Assert.assertNotNull(gcs);

      int[] expectShape = new int[] {65, 30, 194, 294};
      Assert.assertArrayEquals(expectShape, grid.getShape());

      Attribute att = grid.findAttributeIgnoreCase("_FillValue");
      assert att != null;
      assert att.getDataType() == DataType.FLOAT;
      assert Float.isNaN((Float) att.getNumericValue());
    }
  }

  @Test
  public void testOffset() throws IOException, InvalidRangeException, ParseException {
    String catalog = "/catalog/hioos/model/wav/swan/oahu/offset/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    String id = "hioos/model/wav/swan/oahu/offset/SWAN_Oahu_Regional_Wave_Model_(500m)_Offset_21.0hr";
    Dataset ds = cat.findDatasetByID(id);
    assert (ds != null) : "cant find dataset id="+id;
    assert ds.getFeatureType() == FeatureType.GRID;

    DateRange dr = ds.getTimeCoverage();
    assert dr != null;
    assert dr.getStart().getCalendarDate().toString().equals("2011-07-12T21:00:00Z") : dr.getStart().getCalendarDate();
    assert dr.getEnd().getCalendarDate().toString().equals(("2011-07-13T21:00:00Z")) : dr.getEnd().getCalendarDate();
    assert dr.getDuration().equals(new TimeDuration("24 hours")) : dr.getDuration();

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset( ds, null)) {
      assert !dataResult.fatalError;
      assert dataResult.featureDataset != null;

      FeatureDatasetCoverage gds = (FeatureDatasetCoverage) dataResult.featureDataset;
      String gridName = "salt";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      Assert.assertEquals(1, gds.getCoverageCollections().size());
      CoverageCollection cc = gds.getCoverageCollections().get(0);
      Coverage grid = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      CoverageCoordSys gcs = grid.getCoordSys();
      Assert.assertNotNull(gcs);

      int[] expectShape = new int[]{2, 30, 194, 294};
      Assert.assertArrayEquals(expectShape, grid.getShape());

      CoverageCoordAxis time = gcs.getTimeAxis();
      Assert.assertNotNull("time axis", time);
      Assert.assertEquals(2, time.getNcoords());

      double[] expect = new double[]{21., 45.};
      Array data = time.getCoordsAsArray();
      for (int i = 0; i < expect.length; i++)
        Assert2.assertNearlyEquals(expect[i], data.getDouble(i));

      CoverageCoordAxis runtime = gcs.getAxis(AxisType.RunTime);
      Assert.assertNotNull("runtime axis", runtime);
      Assert.assertEquals(2, runtime.getNcoords());

      expect = new double[]{0, 24};
      data = runtime.getCoordsAsArray();
      for (int i = 0; i < expect.length; i++)
        Assert2.assertNearlyEquals(expect[i], data.getDouble(i));
    }
  }
}
