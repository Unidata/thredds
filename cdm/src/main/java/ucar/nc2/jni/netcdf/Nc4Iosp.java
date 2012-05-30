package ucar.nc2.jni.netcdf;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 5/29/12
 */
public class Nc4Iosp {

  private static NC4 nc4;

  private NC4 load() {
    if (nc4 == null) {
      String dir = "C:/cdev/lib/";
      System.setProperty("jna.library.path", dir);

      System.load(dir + "zlib.dll");
      System.load(dir + "szip.dll");
      System.load(dir + "hdf5dll.dll");
      System.load(dir + "hdf5_hldll.dll");
      System.load(dir + "netcdf-7.dll");

      Native.setProtected(true);
      nc4 = (NC4) Native.loadLibrary("netcdf-7", NC4.class);
      System.out.printf(" Netcdf nc_inq_libvers=%s isProtected=%s %n ", nc4.nc_inq_libvers(), Native.isProtected());
    }

    return nc4;
  }

  public int open(String location) throws Exception {
    load(); // load jni
    IntByReference ncidp = new IntByReference();
    int ret = nc4.nc_open(location, 0, ncidp);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
    return ncidp.getValue();
  }

  public static void main(String args[]) throws Exception {
    Nc4Iosp iosp = new Nc4Iosp();

    String loc = "C:/temp/tst_atts.nc";
    int ret = iosp.open(loc);
    System.out.printf("Open %s ret = %d", loc, ret);
  }

}
