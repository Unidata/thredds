package ucar.nc2.adde;

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public class AddeServiceProvider implements IOServiceProvider {
  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
    return false;
  }

  public void open(ucar.unidata.io.RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public Array readData(Variable v2, List section) throws IOException, InvalidRangeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Array readNestedData(Variable v2, List section) throws IOException, InvalidRangeException {
    throw new UnsupportedOperationException("Adde IOSP does not support nested variables");
  }

  public boolean syncExtend() { return false; }
  public boolean sync() { return false; }

  public void close() throws IOException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void setProperties( List iospProperties) { }

  public String toStringDebug(Object o) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public String getDetailInfo() {
    return "";
  }
}
