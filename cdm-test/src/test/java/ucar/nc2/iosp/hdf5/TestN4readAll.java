/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.hdf5;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Describe
 *
 * @author caron
 * @since 10/27/2014
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestN4readAll {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static String testDir = TestDir.cdmUnitTestDir + "formats/netcdf4/";

  @Parameterized.Parameters(name="{0}")
 	public static Collection<Object[]> getTestParameters() throws IOException {
    Collection<Object[]> filenames = new ArrayList<>();

    try {
      TestDir.actOnAllParameterized(testDir + "nc4-classic", null, filenames);
      TestDir.actOnAllParameterized(testDir + "files", null, filenames);
    } catch (IOException e) {
      // JUnit *always* executes a test class's @Parameters method, even if it won't subsequently run the class's tests
      // due to an @Category exclusion. Therefore, we must not let it throw an exception, or else we'll get a build
      // failure. Instead, we return a collection containing a nonsense value (to wit, the exception message).
      //
      // Naturally, if we execute a test using that nonsense value, it'll fail. That's fine; we need to deal with the
      // root cause. However, it is more likely that the exception occurred because "!isCdmUnitTestDirAvailable", and
      // as a result, all NeedsCdmUnitTest tests will be excluded.
      filenames.add(new Object[]{e.getMessage()});
    }

    return filenames;
 	}

  String filename;
  public TestN4readAll(String filename) {
    this.filename = filename;
  }

  @Test
  public void readAll() throws IOException {
    TestDir.readAll(filename);
  }
}
