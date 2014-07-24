package thredds.util;

import org.junit.Test;
import java.util.List;

/**
 * Test the TdsDataPathRemapper used for helping old programs continue to
 * connect to Unidata TDS servers without needed to update the data access URLS
 *
 * Specifically, map TDS 4.2 data path URLS to TDS 4.3 (big ole' grib update)
 *
 */
public class TestUnidataTdsDataPathRemapper {

    @Test
    public void testFilePath() {
        UnidataTdsDataPathRemapper u = new UnidataTdsDataPathRemapper();

        String oldPath = "fmrc/NCEP/GFS/Global_0p5deg/files/";
        String newPath = "grib/NCEP/GFS/Global_0p5deg/files/";

        // test without specifying type
        List<String> result1 = u.getMappedUrlPaths(oldPath);
        assert result1.size() == 1;
        assert result1.get(0).equals(newPath);

        // test by specifying type
        List<String> result2 = u.getMappedUrlPaths(oldPath, "files");
        assert result1.equals(result2);
    }

    @Test
    public void testBestPath() {
        UnidataTdsDataPathRemapper u = new UnidataTdsDataPathRemapper();

        String oldPath = "fmrc/NCEP/GFS/Global_0p5deg/NCEP-GFS-Global_0p5deg_best.ncd";
        String newPath = "grib/NCEP/GFS/Global_0p5deg/best";

        // test without specifying type
        List<String> result1 = u.getMappedUrlPaths(oldPath);
        assert result1.size() == 1;
        assert result1.get(0).equals(newPath);

        // test by specifying type
        List<String> result2 = u.getMappedUrlPaths(oldPath, "best");
        assert result1.equals(result2);
    }


}
