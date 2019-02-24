package ucar.nc2.grib.grib2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;

@RunWith(Parameterized.class)
public class TestGrib2Records {
  interface Callback {
    boolean call(RandomAccessFile raf, Grib2Record gr) throws IOException;
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    // Example of each GDS that we have
    result.add(new Object[]{"HLYA10.grib2",  0, 0, 259920, "2014-01-15T00:00:00Z"});
    result.add(new Object[]{"MRMS_LowLevelCompositeReflectivity_00.50_20141207-072038.grib2", 0, 0, 24500000, "2014-12-07T07:20:38Z"});
    result.add(new Object[]{"cosmo-eu.grib2", 1, 0, 25, "2010-03-29T00:00:00Z"});
    result.add(new Object[]{"ds.sky.grib2", 10, 0, 22833, "2005-09-01T15:00:00Z"});
    result.add(new Object[]{"sref_eta.grib2", 20, 0, 14873, "2009-03-16T21:00:00Z"});
    result.add(new Object[]{"ds.snow.grib2", 30, 8, 739297, "2005-09-01T15:00:00Z"});
    result.add(new Object[]{"Albers_viirs_s.grib2", 31, 0, 3255148, "2014-02-14T12:00:00Z"});
    result.add(new Object[]{"thinGrid.grib2", 40, 0, 2097152, "2012-01-01T00:00:00Z"});
    result.add(new Object[]{"Eumetsat.VerticalPerspective.grib2", 90, 30, 1530169, "2007-05-22T11:45:00Z"});
    result.add(new Object[]{"ofs_atl.grib2", 204, 0, 2020800, "2007-08-07T00:00:00Z"});
    result.add(new Object[]{"rap-native.grib2", 32769, 0, 35, "2016-04-25T22:00:00Z"});

    // Example of each PDS that we have
    result.add(new Object[]{"pdsScale.pds1.grib2",  0, 1, 65160, "2010-11-06T00:00:00Z"});
    result.add(new Object[]{"sref.pds2.grib2",  30, 2, 23865, "2009-01-25T21:00:00Z"});
    result.add(new Object[]{"problem.pds9.grib2",  30, 9, 739297, "2010-09-13T00:00:00Z"});
    result.add(new Object[]{"cosmo.pds11.grib2",  1, 11, 194081, "2009-05-11T00:00:00Z"});
    result.add(new Object[]{"sref.pds12.grib2",  30, 12, 23865, "2009-01-25T21:00:00Z"});
    result.add(new Object[]{"rugley.pds15.grib2",  0, 15, 41760, "2010-10-13T18:00:00Z"});
    result.add(new Object[]{"Lannion.pds31.grib2",  0, 31, 5760000, "2013-11-18T02:00:00Z"});
    return result;
  }

  String filename;
  boolean check;
  int gdsTemplate;
  int pdsTemplate;
  long datalen;
  CalendarDate refdate;

  public TestGrib2Records(String ds, int gdsTemplate, int param, long datalen, String refdateIso) {
    this.filename = "../grib/src/test/data/" + ds;
    this.gdsTemplate = gdsTemplate;
    this.pdsTemplate = param;
    this.datalen = datalen;
    this.refdate = CalendarDate.parseISOformat("ISO8601", refdateIso);
    this.check = gdsTemplate >= 0;
  }

  @Test
  public void testRead() throws IOException {
    readFile(filename, (raf, gr) -> {
      Grib2Gds gds = gr.getGDS();
      if (check) Assert.assertEquals(gdsTemplate, gds.template);
      gds.testHorizCoordSys(new Formatter());

      Grib2SectionProductDefinition pdss = gr.getPDSsection();
      Grib2Pds pds = pdss.getPDS();
      if (check) Assert.assertEquals(pdsTemplate, pdss.getPDSTemplateNumber());
      Formatter f = new Formatter();
      pds.show(f);
      if (check) Assert.assertTrue(f.toString().contains(String.format("template=%d", pdsTemplate)));
      if (check) Assert.assertEquals(this.refdate, gr.getReferenceDate());

      float[] data = gr.readData(raf);
      if (check) Assert.assertEquals(datalen, data.length);
      System.out.printf("%s: template,param,len=  %d, %d, %d, \"%s\" %n", filename, gds.template,
              pdss.getPDSTemplateNumber(), data.length, gr.getReferenceDate());
      return true;
    });
  }

  private void readFile(String path, Callback callback) throws IOException {
    try (RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(path, "r")) {
      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      Grib2RecordScanner reader = new Grib2RecordScanner(raf);
      while (reader.hasNext()) {
        Grib2Record gr = reader.next();
        if (gr == null) break;
        callback.call(raf, gr);
      }
    }
  }


}
