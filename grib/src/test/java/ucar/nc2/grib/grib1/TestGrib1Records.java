package ucar.nc2.grib.grib1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;

@RunWith(Parameterized.class)
public class TestGrib1Records {
  interface Callback {
    boolean call(RandomAccessFile raf, Grib1Record gr) throws IOException;
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[]{"afwa.grib1", 0, 61, 16899, CalendarDate.parseISOformat("ISO8601","2005-05-04T00:00:00Z")});
    result.add(new Object[]{"ECMWF.grib1", 0, 129, 1681, CalendarDate.parseISOformat("ISO8601","2006-12-25T12:00:00Z")});
    result.add(new Object[]{"HPPI89_KWBC.grib1", 0, 2, 5329, CalendarDate.parseISOformat("ISO8601","2010-03-31T18:00:00Z")});
    result.add(new Object[]{"thinGrid.grib1", 0, 11, 5329, CalendarDate.parseISOformat("ISO8601","2005-07-27T12:00:00Z")});
    result.add(new Object[]{"airtmp_zht_000002_000000_1a0061x0061_2010011200_00240000_fcstfld.grib1", 1, 11, 3721, CalendarDate.parseISOformat("ISO8601","2010-01-12T00:00:00Z")});
    result.add(new Object[]{"radar_national.grib1", 3, 201, 400000, CalendarDate.parseISOformat("ISO8601","2005-01-20T02:15:00Z")});
    result.add(new Object[]{"jma.grib1", 3, 2, 83525, CalendarDate.parseISOformat("ISO8601","2006-08-14T00:00:00Z")});
    result.add(new Object[]{"complex_packing.grib1", 4, 122, 131072, CalendarDate.parseISOformat("ISO8601","2015-11-09T00:00:00Z")});
    result.add(new Object[]{"complex_packing2.grib1", 4, 144, 131072, CalendarDate.parseISOformat("ISO8601","2015-11-09T00:00:00Z")});
    result.add(new Object[]{"D2.2006091400.F012.002M.CLWMR.grib1", 5, 153, 102960, CalendarDate.parseISOformat("ISO8601","2006-09-14T00:00:00Z")});
    result.add(new Object[]{"noaaRFC-QPE.grib1", 5, 237, 157500, CalendarDate.parseISOformat("ISO8601","2010-10-05T00:00:00Z")});
    result.add(new Object[]{"rotatedlatlon.grib1", 10, 7, 176904, CalendarDate.parseISOformat("ISO8601","2003-12-15T18:00:00Z")});
    result.add(new Object[]{"ncepPredefinedGds.grib1", 21000, 100, 37*36, CalendarDate.parseISOformat("ISO8601","2004-11-19T00:00:00Z")});

    return result;
  }

  String filename;
  boolean check;
  int gdsTemplate;
  int param;
  long datalen;
  CalendarDate refdate;

  public TestGrib1Records(String ds, int gdsTemplate, int param, long datalen, CalendarDate refdate) {
    this.filename = "../grib/src/test/data/" + ds;
    this.gdsTemplate = gdsTemplate;
    this.param = param;
    this.datalen = datalen;
    this.refdate = refdate;
    this.check = gdsTemplate >= 0;
  }

  @Test
  public void testRead() throws IOException {
    readFile(filename, (raf, gr) -> {
      Grib1Gds gds = gr.getGDS();
      if (check) Assert.assertEquals(gdsTemplate, gds.template);
      if (check) Assert.assertTrue(gds.toString().contains("template="+gdsTemplate));
      gds.testHorizCoordSys(new Formatter());

      Grib1SectionProductDefinition pds = gr.getPDSsection();
      if (check) Assert.assertEquals(param, pds.getParameterNumber());
      Formatter f = new Formatter();
      pds.showPds(Grib1Customizer.factory(gr, null), f);
      if (check) Assert.assertTrue(f.toString().contains(String.format("Parameter Name : (%d)", param)) ||
          f.toString().contains(String.format("Parameter %d not found", param)));
      if (check) Assert.assertEquals(this.refdate, pds.getReferenceDate());

      float[] data = gr.readData(raf);
      if (check) Assert.assertEquals(datalen, data.length);
      System.out.printf("%s: template,param,len=  %d, %d, %d, %s %n", filename, gds.template, pds.getParameterNumber(),
          data.length, pds.getReferenceDate());
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
