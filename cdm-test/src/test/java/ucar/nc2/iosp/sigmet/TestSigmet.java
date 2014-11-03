package ucar.nc2.iosp.sigmet;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.NetcdfFile;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rmay on 3/28/14.
 */
@RunWith(Parameterized.class)
public class TestSigmet {

    String filename;

    @Parameterized.Parameters
    public static List<Object[]> getTestParameters() throws IOException{
        final List<Object[]> files = new ArrayList<>(10);
        TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/sigmet/",
                new WildcardFileFilter("*IRIS"),
                new TestDir.Act() {
            public int doAct(String filename) throws IOException {
                files.add(new Object[]{filename});
                return 1;
            }
        }, true);
        return files;
    }

    public TestSigmet(String filename)
    {
        this.filename = filename;
    }

    @Test
    public void testOpen() throws IOException
    {
        try (NetcdfFile nc = NetcdfFile.open(filename)) {

        }
    }

}
