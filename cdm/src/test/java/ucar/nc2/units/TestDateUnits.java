package ucar.nc2.units;

import junit.framework.*;

import java.util.*;

import ucar.units.Unit;
import ucar.units.ScaledUnit;
import ucar.units.DerivedUnit;
import ucar.units.TimeScaleUnit;

public class TestDateUnits extends TestCase  {

  public TestDateUnits( String name) {
    super(name);
  }

  private DateFormatter formatter = new DateFormatter();

  public void doit(SimpleUnit su ) {
    assert su instanceof DateUnit;

    DateUnit du = (DateUnit) su;
    Date d = du.makeDate( 0.0);

    Date d2 = du.getDateOrigin();
    assert d2.equals(d);

    Date d3 = DateUnit.getStandardDate(du.toString());
    System.out.println(du.toString()+" == "+formatter.toDateTimeStringISO(d3)+" unitsString= "+du.getUnitsString());

    Date d4 = du.getDate();
    System.out.println(du.toString()+" == "+formatter.toDateTimeStringISO(d4));
    assert d4.equals(d3) :  d4+"!="+ d3;
  }

   public void testDate() {
    System.out.println();
    SimpleUnit su = SimpleUnit.factory( "0 secs since 1972-01-01T00:00:00Z");
    doit(su);

    su = SimpleUnit.factory( "3600 secs since 1972-01-01T00:00:00Z");
    doit(su);

    su = SimpleUnit.factory( "24 hours since 1972-01-01T00:00:00Z");
    doit(su);

    su = SimpleUnit.factory( "22 years since 2000-01-01T00:00:00Z");
    doit(su);

     //  Not all of these were being parsed properly by ucar.units.
     su = SimpleUnit.factory( "22 years since 2000-01-01T00:00:00 -06:00" );
     doit( su );

     su = SimpleUnit.factory( "22 years since 2000-01-01T00:00:00 +06:00" );
     doit( su );

     su = SimpleUnit.factory( "22 years since 2000-01-01T00:00:00 +06" );
     doit( su );

     su = SimpleUnit.factory( "22 years since 2000-01-01T00:00:00 -06" );
     doit( su );

     su = SimpleUnit.factory( "22 years since 2000-01-01T00:00:00 +0600" );
     doit( su );

     su = SimpleUnit.factory( "22 years since 2000-01-01T00:00:00 -0600" );
     doit( su );
  }

   public void testMakeDate() {
    System.out.println("\ntestStandardDate");
    DateUnit du = (DateUnit) SimpleUnit.factory( "secs since 1972-01-01T00:00:00Z");
    Date d = du.makeDate( 36000);
    System.out.println(" "+du.toString()+" == "+formatter.toDateTimeStringISO(d));
    assert du.getTimeUnitString().equals("secs");
    //showUnitInfo( du.getUnit());

    du = (DateUnit) SimpleUnit.factory( "hours since 1972-01-01T00:00:00Z");
    Date d2 = du.makeDate( 10);
    System.out.println(" "+du.toString()+" == "+formatter.toDateTimeStringISO(d));
    assert du.getTimeUnitString().equals("hours");
    // showUnitInfo( du.getUnit());

    assert d2.equals(d);

    // value
     // doesnt matter
    du = (DateUnit) SimpleUnit.factory( "36 hours since 1972-01-01T00:00:00Z");
    d2 = du.makeDate( 10);
    System.out.println(" "+du.toString()+" == "+formatter.toDateTimeStringISO(d));
    assert du.getTimeUnitString().equals("hours");
    //showUnitInfo( du.getUnit());

    assert d2.equals(d);
  }

  private void tryMakeValue(String unit, double value) throws Exception {
    DateUnit du = (DateUnit) SimpleUnit.factory( unit);
    Date d = du.makeDate( value);

    double value2 = du.makeValue( d);
    System.out.println(" "+value+" == "+formatter.toDateTimeStringISO(d));
    assert value == value2 : value +" "+value2;
  }

  public void testMakeValue() throws Exception {
    System.out.println("\ntestMakeValue");
    tryMakeValue("secs since 1970-01-02T00:00:00Z", 3600);
    tryMakeValue("hours since 1970-01-02T00:00:00Z", 3600);

    tryMakeValue("secs since 1900-01-01T00:00:00Z", 36000);
    tryMakeValue("hours since 1900-01-01T00:00:00Z", 12);
  }

  public void testDateValue() throws Exception {
    DateUnit du = (DateUnit) SimpleUnit.factory( "hours since 1970-01-01T00:00:00Z");
    Date d = new Date(1000L * 3600 * 24);

    double value = du.makeValue( d);
    System.out.println("testDateValue "+value+" == "+formatter.toDateTimeStringISO(d));
    assert value == 24 : value;

    du = (DateUnit) SimpleUnit.factory( "hours since 1971-01-01T00:00:00Z");
    d = new Date(1000L * 3600 * 24 * 375);

    value = du.makeValue( d);
    System.out.println("testDateValue "+value+" == "+formatter.toDateTimeStringISO(d));
    assert value == 240 : value;

    du = (DateUnit) SimpleUnit.factory( "days since 1965-01-01T00:00:00Z");
    d = DateUnit.getStandardDate("days since 1966-01-01T00:00:00Z");

    value = du.makeValue( d);
    System.out.println("testDateValue "+value+" == "+formatter.toDateTimeStringISO(d));
    assert ucar.nc2.TestLocal.closeEnough(value, 365) : value;
  }

   private void showUnitInfo( Unit uu) {
    System.out.println(" ucar.units.Unit.class=              "+uu.getClass().getName());
    System.out.println(" ucar.units.Unit.toString=           "+uu.toString());
    System.out.println(" ucar.units.Unit.getCanonicalString= "+uu.getCanonicalString());
    System.out.println(" ucar.units.Unit.getName=            "+uu.getName());
    System.out.println(" ucar.units.Unit.getSymbol=          "+uu.getSymbol());
    System.out.println(" ucar.units.Unit.getUnitName=        "+uu.getUnitName());
    System.out.println(" ucar.units.Unit.getDerivedUnit=     "+uu.getDerivedUnit());

    if (uu instanceof TimeScaleUnit) {
      TimeScaleUnit su = (TimeScaleUnit) uu;
      DerivedUnit du = su.getDerivedUnit();
      showUnitInfo( du);
    }
  }

     /** testing */
   public void utestShowExtremes() throws Exception {
    System.out.println();

    long msec = 0;
    Date d = new Date(msec);
    System.out.println(msec +" = "+formatter.toDateTimeStringISO(d));

    msec = 60L * 60 * 24 * 1000 * 365 * 2; // 2 years later = 1972-01-01T00:00:00Z
    d = new Date(msec);
    System.out.println(msec +" = "+formatter.toDateTimeStringISO(d));

    msec = -60L * 60 * 24 * 1000 * 365 * 1972;
    d = new Date(msec);
    System.out.println(msec +" = "+formatter.toDateTimeStringISO(d));

    msec = Long.MAX_VALUE;
    d = new Date(msec);
    System.out.println(msec +" = "+formatter.toDateTimeStringISO(d));

    msec = Long.MIN_VALUE;
    d = new Date(msec);
    System.out.println(msec +" = "+formatter.toDateTimeStringISO(d));
  }


}
