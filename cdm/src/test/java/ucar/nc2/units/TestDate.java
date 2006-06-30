package ucar.nc2.units;

import junit.framework.*;
import ucar.units.*;

import java.util.Date;

public class TestDate extends TestCase {
  private static final boolean debug = true, debug2 = true;
  private DateFormatter formatter = new DateFormatter();

  public TestDate( String name) {
    super(name);
  }

  void doTime2(double value, String name, boolean ok) {
    ucar.units.UnitFormat format = UnitFormatManager.instance();

    ucar.units.Unit timeUnit;
    try {
      timeUnit = format.parse("secs since 1970-01-01 00:00:00");
    } catch (Exception e) {
      System.out.println("SimpleUnit initialization failed " +e);
      return;
    }

    ucar.units.Unit uu;
    try {
      uu = format.parse(name);
    } catch (Exception e) {
      System.out.println("Parse " +name +" got Exception " +e);
      return;
    }

    System.out.println("isCompatible="+uu.isCompatible( timeUnit));

    try {
      System.out.println("convert "+uu.convertTo( value, timeUnit));
    } catch (Exception e) {
      System.out.println("convert " +name +" got Exception " +e);
      return;
    }
  }

  public void testStandardDate() {
    Date d = DateUnit.getStandardDate("25 days since 1985-02-02 00:00:00");
    System.out.println(" d="+formatter.toDateTimeStringISO(d));

    d = DateUnit.getStandardDate("0.0 secs since 1985-02-02 12:00:00");
    System.out.println(" d="+formatter.toDateTimeStringISO(d));

    d = DateUnit.getStandardDate("1.0 secs since 1985-02-02 12:00:00");
    System.out.println(" d="+formatter.toDateTimeStringISO(d));
  }

  public void testTime() {
    doTime2(1.0, "years since 1985", true);
    doTime2(1.0, "year since 1985", true);
  }

}