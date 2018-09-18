package ucar.nc2;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.iosp.NCheader;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.UnitTestCommon;
import ucar.unidata.util.test.category.NeedsContentRoot;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Test that NCheader.checkFileType can recognize various file types
 */
@Category(NeedsContentRoot.class)
@RunWith(Parameterized.class)
public class TestCheckFileType extends UnitTestCommon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String PREFIX = "thredds/public/testdata/";

    @Parameterized.Parameters(name = "{1}")
    static public List<Object[]>
    getTestParameters()
    {
        List<Object[]> result = new ArrayList<>();
        result.add(new Object[]{NCheader.NC_FORMAT_NETCDF3, "testData.nc"});
        result.add(new Object[]{NCheader.NC_FORMAT_64BIT_OFFSET, "nc_test_cdf2.nc"});
        result.add(new Object[]{NCheader.NC_FORMAT_64BIT_DATA, "nc_test_cdf5.nc"});
        result.add(new Object[]{NCheader.NC_FORMAT_HDF5, "group.test2.nc"});  // aka netcdf4
        result.add(new Object[]{NCheader.NC_FORMAT_HDF4, "nc_test_hdf4.hdf4"});
        return result;
    }


    protected String tdsContentRootPath = null;

    @Before
    public void setup()
    {
        super.bindstd();
        // Ignore this class's tests if NetCDF-4 isn't present.
        // We're using @Before because it shows these tests as being ignored.
        // @BeforeClass shows them as *non-existent*, which is not what we want.
        Assert.assertTrue("NetCDF-4 C library not present.", Nc4Iosp.isClibraryPresent());
        tdsContentRootPath = System.getProperty("tds.content.root.path");
        Assert.assertTrue("tds.content.root.path not defined", tdsContentRootPath != null);
    }

    @After
    public void cleanup()
    {
        super.unbindstd();
    }

    @Parameterized.Parameter(0)
    public int kind;

    @Parameterized.Parameter(1)
    public String filename;

    @Test
    public void testCheckFileType()
            throws Exception
    {
        String location = canonjoin(tdsContentRootPath, canonjoin(PREFIX, filename));
        try (RandomAccessFile raf = RandomAccessFile.acquire(location)) {
            // Verify type
            int found = NCheader.checkFileType(raf);
            String foundname = NCheader.formatName(found);
            String kindname = NCheader.formatName(kind);
            System.err.println("Testing format: " + kindname);
            Assert.assertTrue(String.format("***Fail: expected=%s found=%s%n", kindname, foundname),
                    kind == found);
        }
    }

}
