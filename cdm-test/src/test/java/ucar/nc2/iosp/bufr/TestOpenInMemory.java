package ucar.nc2.iosp.bufr;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Created by snake on 2/28/2015.
 */
@Category(NeedsCdmUnitTest.class)
public class TestOpenInMemory {

  @Test
  public void testOpen() throws Exception {
    scanBufrFile(TestDir.cdmUnitTestDir+"formats/bufr/userExamples/BUFR_99990223.bin");
  }

  private boolean scanBufrFile(String filename) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
    int count = 0;
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {

      MessageScanner scan = new MessageScanner(raf);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) continue;
          byte[] mbytes = scan.getMessageBytes(m);
          try( NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes,  "ucar.nc2.iosp.bufr.BufrIosp2")) {
            NetcdfDataset ncd = new NetcdfDataset(ncfile);
          }
        }
      }
    return true;
  }

}
