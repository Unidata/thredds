package ucar.nc2.dataset;

import ucar.nc2.NetcdfFile;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.bufr.BufrConfig;
import ucar.nc2.iosp.bufr.BufrIosp2;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 8/12/13
 */
public class BufrCoordSys extends CoordSysBuilder {


  public static boolean isMine(NetcdfFile ncfile) {
    IOServiceProvider iosp = ncfile.getIosp();
    return iosp != null && iosp instanceof BufrIosp2;
  }

  // needed for ServiceLoader
  public BufrCoordSys() {
    this.conventionName = "BUFR/CDM";
  }

  public void augmentDataset(NetcdfDataset ncd, CancelTask cancelTask) throws IOException {
    BufrIosp2 iosp = (BufrIosp2) ncd.getIosp();
    BufrConfig config = iosp.getConfig();
    Formatter f = new Formatter();
    config.show(f);
    System.out.printf("%s%n", f);
  }



}
