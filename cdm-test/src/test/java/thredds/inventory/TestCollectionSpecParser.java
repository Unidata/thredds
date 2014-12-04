package thredds.inventory;

import org.junit.Test;
import thredds.filesystem.MFileOS7;
import thredds.inventory.filter.WildcardMatchOnName;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateFromString;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 12/3/2014
 */
public class TestCollectionSpecParser {

  @Test
  public void testDateExtractor() throws IOException {
    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser("/data/ldm/pub/native/grid/NCEP/GFS/CONUS_95km/GFS_CONUS_95km_#yyyyMMdd_HHmm#.grib2", errlog);
    System.out.printf("errlog=%s%n", errlog);
    System.out.printf("specp=%s%n", specp);

    // test parsing
    assert specp.getRootDir().equals("/data/ldm/pub/native/grid/NCEP/GFS/CONUS_95km");
    assert !specp.wantSubdirs();
    assert specp.getFilter().toString().equals("GFS_CONUS_95km_..............grib2");
    assert specp.getDateFormatMark().equals("GFS_CONUS_95km_#yyyyMMdd_HHmm");

    // test filter
    MFileFilter mfilter = new WildcardMatchOnName(specp.getFilter());
    String path = "/data/ldm/pub/native/grid/NCEP/GFS/CONUS_95km/GFS_CONUS_95km_20141203_0000.grib2";
    MFile mfile = new MFileOS7(Paths.get(path), null);
    assert mfilter.accept(mfile);

    // 2014-12-03T16:43:59.433 -0700 ERROR - ucar.nc2.units.DateFromString - Must delineate Date between 2 '#' chars, dateFormatString = GFS_CONUS_95km_#yyyyMMdd_HHmm

    // test date extractor
    DateExtractor extractor = new DateExtractorFromName(specp.getDateFormatMark(), true);

    CalendarDate cd =  extractor.getCalendarDate(mfile);
    assert cd != null : "date extractor failed";
    System.out.printf("%s -> %s%n", path, cd);
  }

  /**
   *    * Example:
      *  dateString =  /data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc
      *  dateFormatString =                    #wrfout_d01_#yyyy-MM-dd_HHmm
      *  would extract the date 2006-07-06T08:00
      *
      *  dateString =  /data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc
      *  dateFormatString =          yyyyMMddHH#/wrfout_d01_#
      *  would extract the date 2006-07-06T11:00
      * </pre>
      *
      * @param dateString the String to be parsed
      * @param dateFormatString the date format String
      * @param demark the demarkation character
      * @return the Date that was parsed.
      *
     public static Date getDateUsingDemarkatedMatch( String dateString, String dateFormatString, char demark )

   */

}
