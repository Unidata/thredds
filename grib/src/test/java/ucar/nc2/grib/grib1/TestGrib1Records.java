package ucar.nc2.grib.grib1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.io.RandomAccessFile;

@RunWith(Parameterized.class)
public class TestGrib1Records {
  interface Callback {
    boolean call(RandomAccessFile raf, Grib1Record gr) throws IOException;
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[]{"thinGrid.grib1", 0, 5329});
    result.add(new Object[]{"HPPI89_KWBC.grb", 0, 5329});
    result.add(new Object[]{"airtmp_zht_000002_000000_1a0061x0061_2010011200_00240000_fcstfld.grib", 1, 3721});
    result.add(new Object[]{"radar_national.grib", 3, 400000});
    result.add(new Object[]{"complex_packing.grib1", 4, 131072});
    result.add(new Object[]{"D2.2006091400.F012.002M.CLWMR.GRIB", 5, 102960});
    result.add(new Object[]{"rotatedlatlon.grb", 10, 176904});
    result.add(new Object[]{"ncepPredefinedGds.grib1", 21000, 1332});

    return result;
  }

  String filename;
  int gsdTemplate;
  long datalen;

  public TestGrib1Records(String ds, int gsdTemplate, long datalen) {
    this.filename = "../grib/src/test/data/" + ds;
    this.gsdTemplate = gsdTemplate;
    this.datalen = datalen;
  }

  @Test
  public void testRead() throws IOException {
    readFile(filename, (raf, gr) -> {
      Assert.assertEquals(gsdTemplate, gr.getGDS().template);
      float[] data = gr.readData(raf);
      Assert.assertEquals(datalen, data.length);
      System.out.printf("%s: template,len=  %d, %d%n", filename, gr.getGDS().template, data.length);
      return true;
    });
  }

  private void readFile(String path, Callback callback) throws IOException {
    try (RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(path, "r")) {
      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();
        if (gr == null) break;
        callback.call(raf, gr);
      }

    }
  }


}
