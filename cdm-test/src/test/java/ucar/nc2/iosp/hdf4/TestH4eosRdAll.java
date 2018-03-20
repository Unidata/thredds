/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.hdf4;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestH4eosRdAll {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name="{0}")
 	public static Collection<Object[]> getTestParameters() throws IOException {
    Collection<Object[]> filenames = new ArrayList<>();

    try {
      H4EosFileFilter ff = new H4EosFileFilter();
      TestDir.actOnAllParameterized(TestH4eos.testDir, ff, filenames);
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

  static class H4EosFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return pathname.getName().endsWith(".hdf") || pathname.getName().endsWith(".eos");
    }
  }

  String filename;
  public TestH4eosRdAll(String filename) {
    this.filename = filename;
  }

  @Test
  public void testForStructMetadata() throws IOException {
    System.out.printf("TestH4eosReadAll %s%n", filename);
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
       Group root = ncfile.getRootGroup();
       Group g = root.findGroup("HDFEOS INFORMATION");
       if (g == null) g = ncfile.getRootGroup();

       Variable dset = g.findVariable("StructMetadata.0");
       assert (dset != null);
     }
   }
}
