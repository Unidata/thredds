/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Assert2;

import java.lang.invoke.MethodHandles;

public class TestTimeUnits {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  @Test
  public void testTimes() throws Exception {
    TimeUnit tu = new TimeUnit(3.0, "hours");
    logger.debug("TimeUnit.toString: {}", tu.toString());
    logger.debug("TimeUnit.getValue: {}", tu.getValue());
    logger.debug("TimeUnit.getUnitString: {}", tu.getUnitString());
    
    String unitBefore = tu.getUnitString();
    double secsBefore = tu.getValueInSeconds();

    tu.setValue(33.0);
    logger.debug("NewTimeUnit.toString: {}", tu.toString());

    assert tu.getValue() == 33.0;
    assert 3600.0 * tu.getValue() == tu.getValueInSeconds() : tu.getValue() +" "+tu.getValueInSeconds();
    assert tu.getUnitString().equals( unitBefore);
    Assert2.assertNearlyEquals(tu.getValueInSeconds(), 11.0 * secsBefore);

    tu.setValueInSeconds(3600.0);
    logger.debug("NewTimeUnitSecs.toString: {}", tu.toString());

    assert tu.getValue() == 1.0;
    assert tu.getValueInSeconds() == 3600.0 : tu.getValueInSeconds();
    assert tu.getUnitString().equals( unitBefore);
    Assert2.assertNearlyEquals( 3.0 * tu.getValueInSeconds(), secsBefore);

    TimeUnit day = new TimeUnit(1.0, "day");
    double hoursInDay = day.convertTo(1.0, tu);
    assert hoursInDay == 24.0;

    // note the value is ignored, only the base unit is used
    day = new TimeUnit(10.0, "day");
    hoursInDay = day.convertTo(1.0, tu);
    assert hoursInDay == 24.0;

    hoursInDay = day.convertTo(10.0, tu);
    assert hoursInDay == 240.0 : hoursInDay;
  }
}
