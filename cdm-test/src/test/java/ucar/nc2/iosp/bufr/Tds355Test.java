package ucar.nc2.iosp.bufr;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;

@Category(NeedsCdmUnitTest.class)
public class Tds355Test {
  File supportDir = new File(TestDir.testdataDir, "support");
  File tds355Dir = new File(supportDir, "TDS-355");

  @Ignore("cant deal with BUFR at the moment")
  @Test
  public void testTds355() throws IOException {
    File example = new File(tds355Dir, "iasi_20110513_045057_metopa_23676_eps_o.l1_bufr");

    try (NetcdfDataset dataset = NetcdfDataset.openDataset(example.getAbsolutePath())) {
      Variable obs = dataset.findVariable("obs");

      obs.read();  // Throws an NPE after about 50 seconds on my machine.
    }
  }
}
