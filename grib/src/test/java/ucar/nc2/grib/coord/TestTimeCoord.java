package ucar.nc2.grib.coord;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

@RunWith(JUnit4.class)
public class TestTimeCoord {

  @Test
  public void testTinvDate() {
    CalendarDate start = CalendarDate.of(1269820799000L);
    CalendarDate end = CalendarDate.of(1269824399000L);
    TimeCoordIntvDateValue tinvDate = new TimeCoordIntvDateValue(start, end);
    System.out.printf("tinvDate = %s%n", tinvDate);
    assertEquals("(2010-03-28T23:59:59Z,2010-03-29T00:59:59Z)", tinvDate.toString());

    CalendarDate refDate = CalendarDate.of(1269820800000L);
    CalendarPeriod timeUnit = CalendarPeriod.of("Hour");

    TimeCoordIntvValue tinv = tinvDate.convertReferenceDate(refDate, timeUnit);
    System.out.printf("tinv = %s offset from %s%n", tinv, refDate);
    assertEquals("2010-03-29T00:00:00Z", refDate.toString());
  }

}

