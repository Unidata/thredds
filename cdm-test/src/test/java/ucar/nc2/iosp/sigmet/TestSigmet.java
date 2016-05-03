package ucar.nc2.iosp.sigmet;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.NetcdfFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by rmay on 3/28/14.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestSigmet {
  String filename;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() throws IOException {
    final Collection<Object[]> filenames = new ArrayList<>();

    try {
      TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/sigmet/",
              new WildcardFileFilter("*IRIS"),
              new TestDir.Act() {
                public int doAct(String filename) throws IOException {
                  filenames.add(new Object[]{filename});
                  return 1;
                }
              }, true);
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

  public TestSigmet(String filename) {
    this.filename = filename;
  }

  @Test
  public void testOpen() throws IOException {
    try (NetcdfFile nc = NetcdfFile.open(filename)) {

    }
  }
}
