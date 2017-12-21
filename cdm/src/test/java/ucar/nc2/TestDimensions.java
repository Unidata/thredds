package ucar.nc2;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class TestDimensions {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // From a bug report by Bob Simons.
  @Test
  public void testAnonDimFullName() throws Exception {
    Dimension dim = new Dimension(null, 5, false, false, false);  // Anonymous dimension.
    Assert.assertNull(dim.getShortName());
    Assert.assertNull(dim.getFullName());  // This used to cause a NullPointerException.
  }
}
