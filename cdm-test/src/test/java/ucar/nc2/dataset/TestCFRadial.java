package ucar.nc2.dataset;

import java.io.IOException;
import java.util.Formatter;

import org.junit.Test;
import org.junit.Assert;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.test.util.TestDir;

/**
 * Created by rmay on 3/7/14.
 */
public class TestCFRadial {

    private RadialDatasetSweep testData() throws IOException
    {
        String filename = TestDir.cdmUnitTestDir + "conventions/cfradial/cfrad.20080604_002217_000_SPOL_v36_SUR.nc";
        Formatter buf = new Formatter();
        return (RadialDatasetSweep) FeatureDatasetFactoryManager.open(
                FeatureType.RADIAL, filename, null, buf);
    }

    private RadialDatasetSweep oneDData() throws IOException
    {
        String filename = TestDir.cdmUnitTestDir + "conventions/cfradial/cfrad.20140608_220305.809_to_20140608_220710.630_KFTG_v348_Surveillance_SUR.nc";
        Formatter buf = new Formatter();
        return (RadialDatasetSweep) FeatureDatasetFactoryManager.open(
                FeatureType.RADIAL, filename, null, buf);
    }

    @Test
    public void testBasic() throws IOException
    {
        RadialDatasetSweep ds = testData();
        Assert.assertEquals(FeatureType.RADIAL, ds.getFeatureType());
        Assert.assertEquals("CF/RadialNetCDF", ds.getDataFormat());
        Assert.assertTrue(ds.isVolume());
        Assert.assertEquals(2, ds.getDataVariables().size());
        Assert.assertNotNull(ds.getDataVariable("DBZ"));
        Assert.assertNotNull(ds.getDataVariable("VR"));
    }

    @Test
    public void testSite() throws IOException
    {
        RadialDatasetSweep ds = testData();
        Assert.assertTrue(ds.isStationary());
        Assert.assertEquals("SPOLRVP8", ds.getRadarName());
        Assert.assertEquals("XXXX", ds.getRadarID());
    }

    @Test
    public void testDates() throws IOException
    {
        RadialDatasetSweep ds = testData();
        CalendarDate trueStart = CalendarDate.of(null, 2008, 6, 4, 0, 15, 3);
        Assert.assertEquals(trueStart, ds.getCalendarDateStart());
        CalendarDate trueEnd = CalendarDate.of(null, 2008, 6, 4, 0, 22, 17);
        Assert.assertEquals(trueEnd, ds.getCalendarDateEnd());
    }

    @Test
    public void testSweeps() throws IOException
    {
        RadialDatasetSweep ds = testData();
        RadialDatasetSweep.RadialVariable var =
                (RadialDatasetSweep.RadialVariable) ds.getDataVariable("DBZ");
        Assert.assertEquals(9, var.getNumSweeps());
        Assert.assertEquals(22.526699,
                            var.getSweep(0).getOrigin(0).getLatitude(), 1e-6);
        Assert.assertEquals(120.43350219726,
                            var.getSweep(0).getOrigin(0).getLongitude(), 1e-6);
        Assert.assertEquals(45, var.getSweep(0).getOrigin(0).getAltitude(),
                            1e-5);
        Assert.assertEquals(0.379, var.getSweep(0).getElevation(0), 1e-6);

        final int firstRads = 483;
        Assert.assertEquals(firstRads, var.getSweep(0).getRadialNumber());
        Assert.assertEquals(0.5109, var.getSweep(0).getElevation(firstRads - 1),
                1e-6);
        Assert.assertEquals(0.9998, var.getSweep(1).getElevation(1), 1e-6);
    }

    @Test
    public void testOneD() throws IOException
    {
        RadialDatasetSweep ds = oneDData();
        Assert.assertEquals(FeatureType.RADIAL, ds.getFeatureType());
        RadialDatasetSweep.RadialVariable var =
                (RadialDatasetSweep.RadialVariable) ds.getDataVariable("REF");
        Assert.assertEquals(14, var.getNumSweeps());

        // Check getting all data
        float[] data = var.readAllData();
        Assert.assertEquals(6697440, data.length);
        Assert.assertEquals(-7.5, data[0], 1e-6);

        // Check getting sweep
        data = var.getSweep(6).readData();
        Assert.assertEquals(335520, data.length);
        Assert.assertEquals(10.5, data[0], 1e-6);

        // Check random portion of data from a sweep and ray
        data = var.getSweep(2).readData(3);
        Assert.assertEquals(1468, data.length);
        Assert.assertEquals(-4.5, data[100], 1e-6);
        Assert.assertEquals(9.0, data[101], 1e-6);
        Assert.assertEquals(1.5, data[102], 1e-6);
    }
}
