package ucar.nc2.adde;

import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public class AddeServiceProvider extends AbstractIOServiceProvider {
  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
    return false;
  }

  public void open(ucar.unidata.io.RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void close() throws IOException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

}
