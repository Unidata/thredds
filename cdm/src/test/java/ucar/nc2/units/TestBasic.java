package ucar.nc2.units;

import junit.framework.*;
import ucar.units.*;

public class TestBasic extends TestCase {
  private boolean debug = false;

  public TestBasic( String name) {
    super(name);
  }

  public void testBasic() {
    UnitFormat format = UnitFormatManager.instance();

    try {
      Unit meter = format.parse("meter");
      Unit second = format.parse("second");
      Unit meterPerSecondUnit = meter.divideBy(second);
      Unit knot = format.parse("knot");
      assert (meterPerSecondUnit.isCompatible(knot));

      if (debug) System.out.println("5 knots is " +
        knot.convertTo(5, meterPerSecondUnit) +
        ' ' + format.format(meterPerSecondUnit));
      assert(closeEnough(2.5722222, knot.convertTo(5, meterPerSecondUnit)));

    } catch (Exception e) {
      System.out.println("Exception " + e);
    }
  }

  private boolean closeEnough( double d1, double d2) {
    return Math.abs(d1-d2) < 1.0e-5;
  }

  public void testTimeConversion()   {
    UnitFormat format = UnitFormatManager.instance();
    Unit t1, t2;

    try {
      t1 = format.parse("secs since 1999-01-01 00:00:00");
      t2 = format.parse("secs since 1999-01-02 00:00:00");
      assert(t1.isCompatible( t2));
    } catch (Exception e) {
      System.out.println("testTimeConversion failed " +e);
      return;
    }

    try {
      if (debug) System.out.println("t2.convertTo(0.0, t1) " +t2.convertTo(0.0, t1));
      assert(closeEnough(86400.0, t2.convertTo(0.0, t1)));
    } catch (Exception e) {
      System.out.println("testTimeConversion failed 2 =" +e);
    }
  }

  public void testTimeConversion2()   {
    UnitFormat format = UnitFormatManager.instance();
    Unit t1, t2;

    try {
      t1 = format.parse("secs since 1999-01-01 00:00:00");
      t2 = format.parse("10 hours since 1999-01-01 00:00:00");
      assert(t1.isCompatible( t2));
    } catch (Exception e) {
      System.out.println("testTimeConversion failed " +e);
      return;
    }

    try {
      System.out.println("t2.convertTo(0.0, t1) " +t2.convertTo(0.0, t1));
      // assert(closeEnough(86400.0, t2.convertTo(0.0, t1)));
    } catch (Exception e) {
      System.out.println("testTimeConversion failed 2 =" +e);
    }
  }

  public void testException() {
    UnitFormat format = UnitFormatManager.instance();
    try {
      Unit uu = format.parse("barf");
      System.out.println("Parse ok= " +uu);
    } catch (Exception e) {
      System.out.println("Parse got Exception " +e);
    }
  }


}
