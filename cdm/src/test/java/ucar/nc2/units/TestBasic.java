/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Assert2;
import ucar.units.*;

import java.lang.invoke.MethodHandles;

public class TestBasic extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  public TestBasic( String name) {
    super(name);
  }

  public void testBasic() throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
  
    Unit meter = format.parse("meter");
    Unit second = format.parse("second");
    Unit meterPerSecondUnit = meter.divideBy(second);
    Unit knot = format.parse("knot");
    assert (meterPerSecondUnit.isCompatible(knot));
  
    logger.debug("5 knots is {} {}", knot.convertTo(5, meterPerSecondUnit), format.format(meterPerSecondUnit));
    Assert2.assertNearlyEquals(2.5722222f, knot.convertTo(5, meterPerSecondUnit));
  }

  public void testTimeConversion() throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
    Unit t1, t2;
  
    t1 = format.parse("secs since 1999-01-01 00:00:00");
    t2 = format.parse("secs since 1999-01-02 00:00:00");
    assert(t1.isCompatible( t2));
  
    logger.debug("t2.convertTo(0.0, t1) = {}", t2.convertTo(0.0, t1));
    Assert2.assertNearlyEquals(86400.0, t2.convertTo(0.0, t1));
  }

  public void testTimeConversion2() throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
    Unit t1, t2;
  
    t1 = format.parse("hours since 1999-01-01 00:00:00");
    t2 = format.parse("hours since 1999-01-02 00:00:00");
    assert(t1.isCompatible( t2));
  
    Assert2.assertNearlyEquals(24.0, t2.convertTo(0.0, t1));
  }

  public void testException() throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
    Unit uu = format.parse("barf");
    logger.debug("Parse ok = {}", uu);
  }
}
