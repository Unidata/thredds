/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.hdf4;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Read all hdf4 files in cdmUnitTestDir + "formats/hdf4/"
 *
 * @author caron
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestH4readAll {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  static private String testDir = TestDir.cdmUnitTestDir + "formats/hdf4/";

  @AfterClass
  static public void after() {
    H4header.setDebugFlags(new DebugFlagsImpl(""));  // make sure debug flags are off
  }

  @Parameterized.Parameters(name="{0}")
 	public static Collection<Object[]> getTestParameters() throws IOException {
    Collection<Object[]> filenames = new ArrayList<>();

    try {
      TestDir.actOnAllParameterized(testDir , new H4FileFilter(), filenames);
      // TestDir.actOnAllParameterized("D:/hdf4/" , new H4FileFilter(), filenames);
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
  public TestH4readAll(String filename) {
    this.filename = filename;
  }

  @Test
  public void readAll() throws IOException {
    TestDir.readAll(filename);
  }

  static class H4FileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return pathname.getName().endsWith(".hdf") || pathname.getName().endsWith(".eos") || pathname.getName().endsWith(".h4");
    }
  }
}
