/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.Misc;
import ucar.units.*;

import java.lang.invoke.MethodHandles;
import java.util.Date;

public class TestDateUnits {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private DateFormatter formatter = new DateFormatter();

  public void doit(DateUnit du) {
    Date d = du.makeDate(0.0);

    Date d2 = du.getDateOrigin();
    assert d2.equals(d);

    Date d3 = DateUnit.getStandardDate(du.toString());
    System.out.println(du.toString() + " == " + formatter.toDateTimeStringISO(d3) + " unitsString= " + du.getUnitsString());

    Date d4 = du.getDate();
    System.out.println(du.toString() + " == " + formatter.toDateTimeStringISO(d4));
    assert d4.equals(d3) : d4 + "!=" + d3;
  }

  public void testDate() throws Exception {
    System.out.println();
    doit(new DateUnit("2 secs since 1972-01-01T00:00:00Z"));

    doit(new DateUnit("3600 secs since 1972-01-01T00:00:00Z"));
    doit(new DateUnit("24 hours since 1972-01-01T00:00:00Z"));
    doit(new DateUnit("22 years since 2000-01-01T00:00:00Z"));

    //  Not all of these were being parsed properly by ucar.units.
    doit(new DateUnit("22 years since 2000-01-01T00:00:00 -06:00"));
    doit(new DateUnit("22 years since 2000-01-01T00:00:00 +06:00"));
    doit(new DateUnit("22 years since 2000-01-01T00:00:00 +06"));
    doit(new DateUnit("22 years since 2000-01-01T00:00:00 -06"));
    doit(new DateUnit("22 years since 2000-01-01T00:00:00 +0600"));
    doit(new DateUnit("22 years since 2000-01-01T00:00:00 -0600"));
  }

  public void testMakeDate() throws Exception {
    System.out.println("\ntestStandardDate");
    DateUnit du = new DateUnit( "secs since 1972-01-01T00:00:00Z");
    Date d = du.makeDate( 36000);
    System.out.println(" "+du.toString()+" == "+formatter.toDateTimeStringISO(d));
    //assert du.getTimeUnitString().equals("secs");
    //showUnitInfo( du.getUnit());

    du = new DateUnit( "hours since 1972-01-01T00:00:00Z");
    Date d2 = du.makeDate( 10);
    System.out.println(" " + du.toString() + " == " + formatter.toDateTimeStringISO(d));
    //assert du.getTimeUnitString().equals("hours");
    // showUnitInfo( du.getUnit());

    assert d2.equals(d);

    // value
     // doesnt matter
    du = new DateUnit( "36 hours since 1972-01-01T00:00:00Z");
    d2 = du.makeDate( 10);
    System.out.println(" " + du.toString() + " == " + formatter.toDateTimeStringISO(d));
    //assert du.getTimeUnitString().equals("hours");
    //showUnitInfo( du.getUnit());

    assert d2.equals(d);
  }

  private void tryMakeValue(String unit, double value) throws Exception {
    DateUnit du = new DateUnit(unit);
    Date d = du.makeDate(value);

    double value2 = du.makeValue(d);
    System.out.println(" " + value + " == " + formatter.toDateTimeStringISO(d));
    assert value == value2 : value + " " + value2;
  }

  public void testMakeValue() throws Exception {
    System.out.println("\ntestMakeValue");
    tryMakeValue("secs since 1970-01-02T00:00:00Z", 3600);
    tryMakeValue("hours since 1970-01-02T00:00:00Z", 3600);

    tryMakeValue("secs since 1900-01-01T00:00:00Z", 36000);
    tryMakeValue("hours since 1900-01-01T00:00:00Z", 12);
  }

  public void testDateValue() throws Exception {
    DateUnit du = new DateUnit("hours since 1970-01-01T00:00:00Z");
    Date d = new Date(1000L * 3600 * 24);

    double value = du.makeValue(d);
    System.out.println("testDateValue " + value + " == " + formatter.toDateTimeStringISO(d));
    assert value == 24 : value;

    du = new DateUnit("hours since 1971-01-01T00:00:00Z");
    d = new Date(1000L * 3600 * 24 * 375);

    value = du.makeValue(d);
    System.out.println("testDateValue " + value + " == " + formatter.toDateTimeStringISO(d));
    assert value == 240 : value;

    du = new DateUnit("days since 1965-01-01T00:00:00Z");
    d = DateUnit.getStandardDate("days since 1966-01-01T00:00:00Z");

    value = du.makeValue(d);
    System.out.println("testDateValue " + value + " == " + formatter.toDateTimeStringISO(d));
    assert Misc.nearlyEquals(value, 365) : value;
  }

  private void showUnitInfo(Unit uu) {
    System.out.println(" ucar.units.Unit.class=              " + uu.getClass().getName());
    System.out.println(" ucar.units.Unit.toString=           " + uu.toString());
    System.out.println(" ucar.units.Unit.getCanonicalString= " + uu.getCanonicalString());
    System.out.println(" ucar.units.Unit.getName=            " + uu.getName());
    System.out.println(" ucar.units.Unit.getSymbol=          " + uu.getSymbol());
    System.out.println(" ucar.units.Unit.getUnitName=        " + uu.getUnitName());
    System.out.println(" ucar.units.Unit.getDerivedUnit=     " + uu.getDerivedUnit());

    if (uu instanceof TimeScaleUnit) {
      TimeScaleUnit su = (TimeScaleUnit) uu;
      DerivedUnit du = su.getDerivedUnit();
      showUnitInfo(du);
    }
  }

  public void testStandardDate() {
    Date r = ucar.nc2.units.DateUnit.getStandardDate("1 days since 2009-06-15T04:00:00Z");
    DateFormatter format = new DateFormatter();
    assert (format.toDateTimeStringISO(r).equals("2009-06-16T04:00:00Z"));
  }

     /** testing */
   public void utestShowExtremes() throws Exception {
    System.out.println();

    long msec = 0;
    Date d = new Date(msec);
    System.out.println(msec + " = " + formatter.toDateTimeStringISO(d));

    msec = 60L * 60 * 24 * 1000 * 365 * 2; // 2 years later = 1972-01-01T00:00:00Z
    d = new Date(msec);
    System.out.println(msec + " = " + formatter.toDateTimeStringISO(d));

    msec = -60L * 60 * 24 * 1000 * 365 * 1972;
    d = new Date(msec);
    System.out.println(msec + " = " + formatter.toDateTimeStringISO(d));

    msec = Long.MAX_VALUE;
    d = new Date(msec);
    System.out.println(msec + " = " + formatter.toDateTimeStringISO(d));

    msec = Long.MIN_VALUE;
    d = new Date(msec);
    System.out.println(msec + " = " + formatter.toDateTimeStringISO(d));
  }

  public void testUdunitBug() throws UnitDBException, UnitSystemException, SpecificationException, PrefixDBException, UnitParseException {
    UnitFormat format = UnitFormatManager.instance();
    String unit = "days since 2009-06-14 04:00:00 +00:00";
    Unit uu = format.parse(unit);
    System.out.printf("%s == %s %n", unit, uu);

    unit = "1 days since 2009-06-14 04:00:00 +00:00";
    uu = format.parse(unit);
    System.out.printf("%s == %s %n", unit, uu);

    unit = "3 days since 2009-06-14 04:00:00 +00:00";
    uu = format.parse(unit);
    System.out.printf("%s == %s %n", unit, uu);

  }

  public void testUdunitBug2() throws UnitDBException, UnitSystemException, SpecificationException, PrefixDBException, UnitParseException {
    UnitFormat format = UnitFormatManager.instance();
    String unit = "2.0 secs since 1985-02-02 12:00:00";
    Unit uu = format.parse(unit);
    System.out.printf("%s == %s %n", unit, uu);
  }

  // Assert that DateUnit.makeDate() == DateUnit.makeCalendarDate().toDate().
  public void testMakeCalendarDate() {
    String[] unitSpecs = new String[] {
            "3600 secs since 1972-01-01T00:00:00Z",
            "22 years since 2000-01-01T00:00:00 -06",
            "53 hours since 1970-01-02T00:00:00Z",
            "19 days since 1959-11-02T00:00:00Z",
            " 104 secs since 1992-10-8 15:15:42.5 -6:00"
    };

    for (String unitSpec : unitSpecs) {
      DateUnit du = DateUnit.factory(unitSpec);
      Assert.assertEquals(String.format("'%s' failed", unitSpec), du.makeDate(0), du.makeCalendarDate(0).toDate());
    }
  }
}
