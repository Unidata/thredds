package thredds.tds;

import junit.framework.TestCase;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDataset;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.util.CompareNetcdf;
import ucar.nc2.util.CompareNetcdf2;

import java.io.IOException;
import java.text.ParseException;

/**
 * An featureCollection FMRC with inner and outer NcML, from John Mauer
 * http://oos.soest.hawaii.edu/thredds/catalog/hioos/roms_forec/hiig/catalog.html?dataset=roms_hiig_forecast/HI-ROMS_Forecast_Model_Run_Collection_best.ncd
 *
 * @author caron
 * @since Sep 24, 2010
 */
public class TestWaveModel extends TestCase {

  public TestWaveModel( String name) {
    super(name);
  }

  public void testNcml() throws IOException, InvalidRangeException {
    String catalog = "/catalog/hioos/model/wav/swan/oahu/catalog.xml";
    InvCatalogImpl cat = TestTdsLocal.open(catalog);

    InvDataset ds = cat.findDatasetByID("swan_oahu/SWAN_Oahu_Regional_Wave_Model_(500m)_best.ncd");
    assert (ds != null) : "cant find dataset 'swan_oahu/SWAN_Oahu_Regional_Wave_Model_(500m)_best.ncd'";
    assert ds.getDataType() == FeatureType.GRID;

    ThreddsDataFactory fac = new ThreddsDataFactory();

    ThreddsDataFactory.Result dataResult = fac.openFeatureDataset( ds, null);

    assert dataResult != null;
    assert !dataResult.fatalError;
    assert dataResult.featureDataset != null;

    GridDataset gds = (GridDataset) dataResult.featureDataset;
    GridDatatype grid = gds.findGridDatatype("salt");
    assert grid != null;
    Section haveShape = new Section(grid.getShape());
    Section wantShape = new Section(new int[] {65,30, 194, 294});
    assert haveShape.equals(wantShape) : wantShape + " != " + haveShape;

    Attribute att = grid.findAttributeIgnoreCase("_FillValue");
    assert att != null;
    assert att.getDataType() == DataType.FLOAT;
    assert Float.isNaN((Float)att.getNumericValue());

    gds.close();
  }

  public void testOffset() throws IOException, InvalidRangeException, ParseException {
    String catalog = "/catalog/hioos/model/wav/swan/oahu/offset/catalog.xml";
    InvCatalogImpl cat = TestTdsLocal.open(catalog);

    InvDataset ds = cat.findDatasetByID("swan_oahu/offset/SWAN_Oahu_Regional_Wave_Model_(500m)_Offset_21.0hr");
    assert (ds != null) : "cant find dataset 'swan_oahu/offset/SWAN_Oahu_Regional_Wave_Model_(500m)_Offset_21.0hr'";
    assert ds.getDataType() == FeatureType.GRID;

    DateFormatter df = new DateFormatter();
    DateRange dr = ds.getTimeCoverage();
    assert dr != null;
    assert dr.getStart().getDate().equals( df.getISODate("2011-07-12T21:00:00Z")) : df.toDateTimeStringISO(dr.getStart().getDate());
    assert dr.getEnd().getDate().equals( df.getISODate("2011-07-13T21:00:00Z")) : df.toDateTimeStringISO(dr.getEnd().getDate());
    assert dr.getDuration().equals(new TimeDuration("24 hours")) : dr.getDuration();

    ThreddsDataFactory fac = new ThreddsDataFactory();

    ThreddsDataFactory.Result dataResult = fac.openFeatureDataset( ds, null);

    assert dataResult != null;
    assert !dataResult.fatalError;
    assert dataResult.featureDataset != null;

    GridDataset gds = (GridDataset) dataResult.featureDataset;
    GridDatatype grid = gds.findGridDatatype("salt");
    assert grid != null;
    Section haveShape = new Section(grid.getShape());
    Section wantShape = new Section(new int[] {2, 30, 194, 294});
    assert haveShape.equals(wantShape) : wantShape + " != " + haveShape;

    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;

    CoordinateAxis1D time = gcs.getTimeAxis1D();
    assert time != null;
    assert time.getSize() == 2;
    double[] want = new double[] {21, 45};
    CompareNetcdf2 cn = new CompareNetcdf2();
    assert cn.compareData("time", time.read(), Array.factory(want), false);

    CoordinateAxis1D runtime = gcs.getRunTimeAxis();
    assert runtime != null;
    assert runtime.getSize() == 2;
    want = new double[] {0, 24};
    cn = new CompareNetcdf2();
    assert cn.compareData("runtime", runtime.read(), Array.factory(want), false);

    gds.close();
  }

}