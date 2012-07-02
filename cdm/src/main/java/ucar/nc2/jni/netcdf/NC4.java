package ucar.nc2.jni.netcdf;

import com.sun.jna.Library;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Describe
 *
 * @author caron
 * @since 5/29/12
 */
public interface NC4 extends Library  {

    // library
  String nc_inq_libvers();
  String nc_strerror(int ncerr);

  // EXTERNL int nc_open(const char *path, int mode, int *ncidp);
  int nc_open(String path, int mode, IntByReference ncidp);

}
