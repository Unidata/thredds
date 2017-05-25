package ucar.nc2.jni.netcdf;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.NCheader;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UnitTestCommon;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.category.NeedsContentRoot;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Test Reading of CDF-5 files using JNI netcdf-4 iosp
 */
@Category(NeedsContentRoot.class)
public class TestCDF5Reading extends UnitTestCommon
{
    static final ArrayFloat.D1 BASELINE;

    static {
        BASELINE = new ArrayFloat.D1(3);
        BASELINE.set(0, -3.4028235E38F);
        BASELINE.set(1, 3.4028235E38F);
        BASELINE.set(2,Float.NEGATIVE_INFINITY);
    }

    protected String tdsContentRootPath = null;

    @Before
    public void setup()
    {
        // Ignore this class's tests if NetCDF-4 isn't present.
        // We're using @Before because it shows these tests as being ignored.
        // @BeforeClass shows them as *non-existent*, which is not what we want.
        Assert.assertTrue("NetCDF-4 C library not present.", Nc4Iosp.isClibraryPresent());
        tdsContentRootPath = System.getProperty("tds.content.root.path");
        Assert.assertTrue("tds.content.root.path not defined", tdsContentRootPath != null);
    }

    @Test
    public void testReadSubsection()
            throws IOException, InvalidRangeException
    {
        String location = canonjoin(tdsContentRootPath,"thredds/public/testdata/nc_test_cdf5.nc");
        try (RandomAccessFile raf  = RandomAccessFile.acquire(location)) {
            // Verify that this is a netcdf-5 file
            int format = NCheader.checkFileType(raf);
            Assert.assertTrue("Fail: file format is not CDF-5",format == NCheader.NC_FORMAT_64BIT_DATA);
        }
        try (NetcdfFile jni = openJni(location)) {
            jni.setLocation(location + " (jni)");
            Array data = read(jni, "f4", "0:2");
            if(prop_visual) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                NCdumpW.printArray(data, pw);
                pw.close();
                sw.close();
                String testresult = sw.toString().replace('r', ' ').replace('\n', ' ').trim();
                visual("CDF Read", testresult);
            }
            Assert.assertTrue(String.format("***Fail: data mismatch"),
                                              MAMath.fuzzyEquals(data, BASELINE));
            System.err.println("***Pass");
        }
    }

    private Array read(NetcdfFile ncfile, String vname, String section) throws IOException, InvalidRangeException
    {
        Variable v = ncfile.findVariable(vname);
        assert v != null;
        return v.read(section);
    }

    private NetcdfFile openJni(String location) throws IOException
    {
        Nc4Iosp iosp = new Nc4Iosp(NetcdfFileWriter.Version.netcdf4);
        NetcdfFile ncfile = new NetcdfFileSubclass(iosp, location);
        RandomAccessFile raf = new RandomAccessFile(location, "r");
        iosp.open(raf, ncfile, null);
        return ncfile;
    }

}
