/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib.netcdf;

import com.sun.jna.Native;
import dap4.dap4lib.DapLog;

import java.io.IOException;

/**
 * Load the netcdf library via JNA; Singleton class
 * This code should closely match that in ucar.nc2.jni.netcdf.Nc4Iosp.java.
 */
abstract public class NetcdfLoader
{
    //////////////////////////////////////////////////
    // Constants

    static public final boolean DEBUG = false;

    static public final String DFLAG_JNAPATH = "jna.library.path";
    static public final String ENV_JNAPATH = "JNA_PATH"; // environment var
    // Note that e.g. LD_LIBRARY_PATH may also be relevant.
    static String DFALT_NETCDF4LIBNAME = "netcdf";

    //////////////////////////////////////////////////
    // Static variables

    static protected DapNetcdf nc4 = null;
    static protected String jnaPath = null;
    static protected String libName = DFALT_NETCDF4LIBNAME;

    /**
     * set the path and name of the netcdf c library.
     * must be called before load() is called.
     *
     * @param jna_path path to shared libraries
     * @param lib_name library name
     * @throws IllegalArgumentException
     */
    static synchronized public void
    setLibraryAndPath(String jna_path, String lib_name)
    {
        lib_name = nullify(lib_name);
        if(lib_name == null)
            lib_name = DFALT_NETCDF4LIBNAME;
        jna_path = nullify(jna_path);
        if(jna_path == null)
            jna_path = nullify(System.getProperty(DFLAG_JNAPATH)); //get system property (-D flag).
        if(jna_path == null) {
            jna_path = nullify(System.getenv(ENV_JNAPATH));   // Next, try environment variable.
            if(jna_path != null)
                System.setProperty(DFLAG_JNAPATH, jna_path);
        }

        // If jna_path is null, the library might still be found
        // automatically from LD_LIBRARY_PATH or somewhere else
        // So complain but do not fail
        if(jna_path == null)
            DapLog.warn(String.format("Neither -D%s nor getenv(%s) is defined",
                    DFLAG_JNAPATH, ENV_JNAPATH));

        libName = lib_name;
        jnaPath = jna_path;
    }

    static synchronized public DapNetcdf
    load()
            throws IOException
    {
        if(nc4 == null) {
            if(jnaPath == null)
                setLibraryAndPath(null, null);
            try {
                // jna_path may still be null (the user didn't specify a "jna.library.path"), but try to load anyway;
                // the necessary libs may be on the system PATH.
                nc4 = (DapNetcdf) Native.loadLibrary(libName, DapNetcdf.class);
                nc4 = (DapNetcdf) Native.synchronizedLibrary(nc4);
                String message = String.format("NetCDF-4 C library loaded (jna_path='%s', libname='%s').", jnaPath, libName);
                String vermsg = String.format("Netcdf nc_inq_libvers='%s' isProtected=%s%n", nc4.nc_inq_libvers(), Native.isProtected());
                if(DEBUG) {
                    System.out.println(message);
                    System.out.printf(vermsg);
                } else {
                    DapLog.info(message);
                    DapLog.info(vermsg);
                }
            } catch (Throwable t) {
                String message = String.format("NetCDF-4 C library not present (jna_path='%s', libname='%s'); %s.",
                        jnaPath, libName, t.getMessage());
                if(DEBUG) {
                    System.err.println(message);
                    System.err.println(t.getMessage());
                } else {
                    DapLog.info(message);
                    DapLog.info(t.getMessage());
                }
                nc4 = null;
                throw new IOException(message);
            }
        }
        return nc4;
    }

    /**
     * Test if the netcdf C library is present and loaded
     *
     * @return true if present
     */
    static synchronized public boolean
    isClibraryPresent()
    {
        return (nc4 != null);
    }

    /**
     * Convert a zero-length string to null
     *
     * @param s the string to check for length
     * @return null if s.length() == 0, s otherwise
     */
    static protected String nullify(String s)
    {
        if(s != null && s.length() == 0) s = null;
        return s;
    }

}
