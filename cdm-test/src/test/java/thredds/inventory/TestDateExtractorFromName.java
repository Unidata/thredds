package thredds.inventory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.test.TestDir;

import java.util.ArrayList;
import java.util.List;

/**
 * Test DateExtractorFromName
 *
 * @author caron
 * @since 12/8/2014
 */
@RunWith(Parameterized.class)
public class TestDateExtractorFromName {
  static String base = "thredds:resolve:http://"+ TestDir.threddsTestServer+"/thredds/";

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // Grib files, one from each model
    result.add(new Object[]{"/san4/work/jcaron/cfsrr/198507", "#cfsrr/#yyyyMM", false, "1985-07-01T00:00:00Z"});
    result.add(new Object[]{"/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/20111226/Run_1200.grib1", "#Alaska_191km/#yyyyMMdd'/Run_'HHmm",false, "2011-12-26T12:00:00Z"});
    result.add(new Object[]{"/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/20111226/Run_1200/stuff/random.grib1", "#Alaska_191km/#yyyyMMdd'/Run_'HHmm",false, "2011-12-26T12:00:00Z"});
    result.add(new Object[]{"pgb.ft06.198407", "pgb.ft06.#yyyyMM", true, "1984-07-01T00:00:00Z"});
    result.add(new Object[]{"/random/shit/pgb.ft06.198407", "pgb.ft06.#yyyyMM", true, "1984-07-01T00:00:00Z"});
    result.add(new Object[]{"e20c.oper.an.pl.3hr.128_248_cc.regn80sc.1949120100_1949123121.grb", "#regn80sc.#yyyyMMddHH", false, "1949-12-01T00:00:00Z"});
    result.add(new Object[]{"e20c.oper.an.pl.3hr.128_248_cc.regn80sc.1949120100_1949123121.grb", "#regn80#'sc.'yyyyMMddHH", false, "1949-12-01T00:00:00Z"});
    result.add(new Object[]{"e20c.oper.an.pl.3hr.128_248_cc.regn80sc.1949120100_1949123121.grb", "#regn80#...yyyyMMddHH", false, "1949-12-01T00:00:00Z"});
    result.add(new Object[]{"e20c.oper.an.pl.3hr.128_248_cc.regn80uv.1949120100_1949123121.grb", "#regn80#...yyyyMMddHH", false, "1949-12-01T00:00:00Z"});

    return result;
  }

  String name;
  String dateFormatMark;
  boolean useName;
  String result;

  public TestDateExtractorFromName(String name, String dateFormatMark, boolean useName, String result) {
    this.name = name;
    this.dateFormatMark = dateFormatMark;
    this.useName = useName;
    this.result = result;
  }

  @Test
  public void doit() {
    DateExtractorFromName de = new DateExtractorFromName(dateFormatMark, useName);
    CalendarDate d = de.getCalendarDateFromPath(name);
    System.out.printf("%s == %s%n", name , d);
    assert d.toString().equals(result);
  }

}
