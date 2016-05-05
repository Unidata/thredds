package ucar.nc2.ncml;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 4/26/12
 */
public class TestCachedNcmlData {

  @Test
  public void testCachedData() throws IOException {

    NetcdfFile ncd = null;
    try {
      ncd = NetcdfDataset.openFile(TestDir.cdmLocalTestDataDir + "point/profileMultidim.ncml", null);
      Variable v = ncd.findVariable("data");
      assert v != null;
      Array data = v.read();
      assert data.getSize() == 50 : data.getSize();
    } finally {
      if (ncd != null) ncd.close();
    }
  }
  
  // doesnt work
  public void testCachedDataWithStructure() throws IOException {

    NetcdfFile ncd = null;
    try {
      ncd = NetcdfDataset.openFile(TestDir.cdmLocalTestDataDir + "point/profileMultidim.ncml", null);
      boolean ok = (Boolean) ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      assert ok;
      
      Variable s = ncd.findVariable("record");
      assert s != null;
      assert s instanceof Structure;
      assert s.getSize() == 5 : s.getSize();

      Array data = s.read();
      assert data.getSize() == 5 : data.getSize();

    } finally {
      if (ncd != null) ncd.close();
    }
  }
  

}
