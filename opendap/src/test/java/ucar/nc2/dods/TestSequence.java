package ucar.nc2.dods;

import junit.framework.TestCase;
import ucar.nc2.util.net.EscapeStrings;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import ucar.ma2.Array;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

public class TestSequence extends TestCase
{
  final String TITLE = "OpenDAP Sequence Test" ;

  public TestSequence(String name, String testdir) {
    super(name);
  }
  public TestSequence(String name) {
    super(name);
  }

  static String baseline = "";

  public void testSequence()
  {
    String url = "http://tsds.net/tsds/test/Scalar";
    try {
        NetcdfDataset ds = NetcdfDataset.openDataset(url);
        System.out.println(ds);
        Structure struct = (Structure) ds.findVariable("TimeSeries");
        Variable var = struct.findVariable("time");
        Array arr = var.read();
        int n = (int) arr.getSize();
        for (int i=0; i<n; i++)
	    System.out.println(arr.nextDouble());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
