/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Assert2;
import ucar.units.*;

import java.lang.invoke.MethodHandles;
import java.util.Date;

public class TestDateUnits {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private DateFormatter formatter = new DateFormatter();

  @Test
  public void testDate() throws Exception {
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
  
  private void doit(DateUnit du) {
    Date d = du.makeDate(0.0);
    
    Date d2 = du.getDateOrigin();
    assert d2.equals(d);
    
    Date d3 = DateUnit.getStandardDate(du.toString());
    logger.debug("{} == {}, unitsString = {}", du.toString(), formatter.toDateTimeStringISO(d3), du.getUnitsString());
    
    Date d4 = du.getDate();
    logger.debug("{} == {}", du.toString(), formatter.toDateTimeStringISO(d4));
    assert d4.equals(d3) : d4 + "!=" + d3;
  }
  
  @Test
  public void testMakeDate() throws Exception {
    DateUnit du = new DateUnit( "secs since 1972-01-01T00:00:00Z");
    Date d = du.makeDate( 36000);  // 10 hours
    logger.debug(" {} == {}", du.toString(), formatter.toDateTimeStringISO(d));

    du = new DateUnit( "hours since 1972-01-01T00:00:00Z");
    Date d2 = du.makeDate( 10);
    logger.debug(" {} == {}", du.toString(), formatter.toDateTimeStringISO(d));

    assert d2.equals(d);

    // value doesn't matter
    du = new DateUnit( "36 hours since 1972-01-01T00:00:00Z");
    d2 = du.makeDate( 10);
    logger.debug(" {} == {}", du.toString(), formatter.toDateTimeStringISO(d));

    assert d2.equals(d);
  }
  
  @Test
  public void testMakeValue() throws Exception {
    tryMakeValue("secs since 1970-01-02T00:00:00Z", 3600);
    tryMakeValue("hours since 1970-01-02T00:00:00Z", 3600);

    tryMakeValue("secs since 1900-01-01T00:00:00Z", 36000);
    tryMakeValue("hours since 1900-01-01T00:00:00Z", 12);
  }
  
  private void tryMakeValue(String unit, double value) throws Exception {
    DateUnit du = new DateUnit(unit);
    Date d = du.makeDate(value);
    
    double value2 = du.makeValue(d);
    logger.debug(" {} == {}", value, formatter.toDateTimeStringISO(d));
    assert value == value2 : value + " " + value2;
  }
  
  @Test
  public void testDateValue() throws Exception {
    DateUnit du = new DateUnit("hours since 1970-01-01T00:00:00Z");
    Date d = new Date(1000L * 3600 * 24);  // One day after 1970-01-01T00:00:00Z == 1970-01-02T00:00:00Z.
    
    double value = du.makeValue(d); // 1970-01-02T00:00:00Z is 24 hours after 1970-01-01T00:00:00Z.
    logger.debug("testDateValue: {} == {}", value, formatter.toDateTimeStringISO(d));
    assert value == 24 : value;  // Num hours in one day.

    du = new DateUnit("hours since 1971-01-01T00:00:00Z");
    d = new Date(1000L * 3600 * 24 * 375);  // 375 days after 1970-01-01T00:00:00Z == 1971-01-11T00:00:00Z

    value = du.makeValue(d);  // 1971-01-11T00:00:00Z is 240 hours (10 days) after 1971-01-01T00:00:00Z.
    logger.debug("testDateValue: {} == {}", value, formatter.toDateTimeStringISO(d));
    assert value == 240 : value;

    du = new DateUnit("days since 1965-01-01T00:00:00Z");
    d = DateUnit.getStandardDate("days since 1966-01-01T00:00:00Z");

    value = du.makeValue(d);  // 1966-01-01T00:00:00Z is 365 days after 1965-01-01T00:00:00Z.
    logger.debug("testDateValue: {} == {}", value, formatter.toDateTimeStringISO(d));
    Assert2.assertNearlyEquals(value, 365);
  }
  
  @Test
  public void testStandardDate() {
    Date r = ucar.nc2.units.DateUnit.getStandardDate("1 days since 2009-06-15T04:00:00Z");
    DateFormatter format = new DateFormatter();
    assert (format.toDateTimeStringISO(r).equals("2009-06-16T04:00:00Z"));
  }
  
  @Test
  public void testUdunitBug() throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
    
    String unitStr = "days since 2009-06-14 04:00:00 +00:00";
    TimeScaleUnit tsu = (TimeScaleUnit) format.parse(unitStr);
    ScaledUnit su = (ScaledUnit) tsu.getUnit();
    BaseUnit bu = (BaseUnit) su.getUnit();
    
    logger.debug("{} == {}", unitStr, tsu);
    // We gave no explicit offset value for our unit, so "1" is assumed.
    // Offset value is always converted to seconds. 1 day == 86400 seconds.
    Assert.assertEquals("second", bu.getUnitName().getName());
    Assert.assertEquals(86400.0, su.getScale(), 0);

    
    unitStr = "1 days since 2009-06-14 04:00:00 +00:00";
    tsu = (TimeScaleUnit) format.parse(unitStr);
    su = (ScaledUnit) tsu.getUnit();
    bu = (BaseUnit) su.getUnit();
  
    logger.debug("{} == {}", unitStr, tsu);
    // Explicit offset value of "1 days".
    // Offset value is always converted to seconds. 1 day == 86400 seconds.
    Assert.assertEquals("second", bu.getUnitName().getName());
    Assert.assertEquals(86400.0, su.getScale(), 0);

    
    unitStr = "3 days since 2009-06-14 04:00:00 +00:00";
    tsu = (TimeScaleUnit) format.parse(unitStr);
    su = (ScaledUnit) tsu.getUnit();
    bu = (BaseUnit) su.getUnit();
  
    logger.debug("{} == {}", unitStr, tsu);
    // Explicit offset value of "3 days".
    // Offset value is always converted to seconds. 3 days == 259200 seconds.
    Assert.assertEquals("second", bu.getUnitName().getName());
    Assert.assertEquals(259200.0, su.getScale(), 0);
  }

  // Assert that DateUnit.makeDate() == DateUnit.makeCalendarDate().toDate().
  @Test
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
