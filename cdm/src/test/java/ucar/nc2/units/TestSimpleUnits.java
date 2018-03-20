/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class TestSimpleUnits extends TestCase  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestSimpleUnits( String name) {
    super(name);
  }


  ///////////////////////////////////////////////////////////
  // testing

  /* private void tryDivide() throws Exception {
    SimpleUnit t1 = SimpleUnit.factory("9 hPa");
    SimpleUnit t2 = SimpleUnit.factory("3 mbar");
    SimpleUnit t3 = t1.divideBy(t2);
    System.out.println(t1+" divideBy "+t2+" = " +t3);
    assert t3.getValue() == 3.0 : t3.getValue();
  } */

  private  void tryConvert() throws Exception {
    SimpleUnit t1 = SimpleUnit.factory("1 days");
    SimpleUnit t2 = SimpleUnit.factory("1 hour");
    double v = t1.convertTo(1.0, t2);
    System.out.println(t1+" convertTo "+t2+" = " +v);

    assert v == 24.0;
  }

  /** testing */
  public void testUnits() throws Exception {
    SimpleUnit su = SimpleUnit.factory( "11 hPa");
    assert !(su instanceof TimeUnit);
    assert su.getValue() == 1100.0 : su;
    assert su.getUnitString().equals("Pa") : su;

    su = SimpleUnit.factory( "11 km");
    assert !(su instanceof TimeUnit);
    assert su.getValue() == 11000.0 : su;
    assert su.getUnitString().equals("m") : su;

    SimpleUnit tu = SimpleUnit.factory("3 days");
    assert tu instanceof TimeUnit : tu.getClass().getName();
    assert tu.getUnitString().equals("days");
    assert tu.getValue() == 3.0 : su;

   /* String text = "3 days since 1930-07-27 12:00:00-05:00";
    SimpleUnit du = SimpleUnit.factory( text);
    System.out.println(text+" == standard format "+du);
    assert !(du instanceof TimeUnit);
    assert du instanceof DateUnit;

    text = "hours since 1930-07-29T01:00:00-08:00";
    du = SimpleUnit.factory( text);
    System.out.println(text+" == standard format "+du);
    assert !(du instanceof TimeUnit);
    assert du instanceof DateUnit;

    text = "0 hours since 1930-07-29T01:00:00-08:00";
    du = SimpleUnit.factory( text);
    System.out.println(text+" == standard format "+du);
    assert !(du instanceof TimeUnit);
    assert du instanceof DateUnit;  */

    //tryDivide();
    tryConvert();
  }

  public void testCompatible() {
    SimpleUnit su = SimpleUnit.factory( "11 hPa");
    assert su.isCompatible("mbar");
    assert !su.isCompatible("m");
    assert !su.isCompatible("sec");
    assert !su.isCompatible("3 days since 1930-07-27 12:00:00-05:00");
  }

  public void testDates() throws Exception {
    DateFormatter df = new DateFormatter();
    String text = "months since 1930-01-01";
    DateUnit du = new DateUnit(text);
    for (int i=0; i<12; i++) {
      System.out.printf("%d %s == %s%n", i, text, df.toDateTimeStringISO(du.makeDate(i)));
    }

    text = "years since 1850-01-01";
    du = new DateUnit(text);
    for (int i=0; i<100; i+=10) {
      System.out.printf("%d %s == %s%n", i, text, df.toDateTimeStringISO(du.makeDate(i)));
    }
  }

}
