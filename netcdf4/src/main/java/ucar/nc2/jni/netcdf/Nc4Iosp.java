/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.jni.netcdf;

import com.sun.jna.MyPointer;
import thredds.catalog.DataFormatType;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.IOServiceProviderWriter;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.*;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.nc2.jni.netcdf.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategyImpl;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;

import static ucar.nc2.jni.netcdf.Nc4prototypes.*;

/**
 * IOSP for reading netcdf files through jni interface to netcdf4 library
 *
 * @author caron
 * @see "http://www.unidata.ucar.edu/software/netcdf/docs/netcdf-c.html"
 * @see "http://earthdata.nasa.gov/sites/default/files/field/document/ESDS-RFC-022v1.pdf"
 * @see "http://www.unidata.ucar.edu/software/netcdf/docs/faq.html#fv15" hdf5 features not supported
 * @see "http://www.unidata.ucar.edu/software/netcdf/win_netcdf/"
 * @since Oct 30, 2008
 */
public class Nc4Iosp extends AbstractIOServiceProvider implements IOServiceProviderWriter
{
    static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Nc4Iosp.class);
    static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");
    static private Nc4prototypes nc4;
    static public final String JNA_PATH = "jna.library.path";
    static public final String JNA_PATH_ENV = "JNA_PATH"; // environment var
    static private String jnaPath;
    static private String libName = "netcdf";

    static private boolean warn = true;
    static private final boolean debug = false,
        debugCompoundAtt = false,
        debugUserTypes = false,
        debugWrite = false;

    /**
     * Suppress warning messages
     */
    static public void setWarnOff()
    {
        warn = false;
    }

    /**
     * Test if the netcdf C library is present and loaded     *
     *
     * @return true if present
     */
    static public boolean isClibraryPresent()
    {
        try {
            load();
            if(warn) {
                startupLog.info("netcdf4 c library loaded jna_path= '{}' libname='{}'", jnaPath, libName);
                //log.info("netcdf4 c library loaded jna_path= '{}' libname='{}'", jnaPath, libName);
                warn = false;
            }
        } catch (Throwable t) {
            if(warn) {
                startupLog.warn("netcdf4 c library not present jna_path='" + jnaPath + "' libname='" + libName + "' " + t.getMessage());
                //log.warn("netcdf4 c library not present jna_path='" + jnaPath + "' libname='" + libName + "' " + t.getMessage());
                warn = false;
            }
        }
        return (nc4 != null);
    }

    /**
     * set the path and name of the netcdf c library.
     * must be called before load() is called.
     * Order of priority is (currently):
     * 1. jna_path argument to this function, it if exists
     * 2. -Djna.library.path
     * 3. JNA_PATH env variable
     *
     * @param jna_path path
     * @param libname  library name
     */
    static public void setLibraryAndPath(String jna_path, String libname)
    {
        // See if jna_path exists
        File f = new File(jna_path);
        if(!f.exists())
            jna_path = null; // ignore it
        if(jna_path == null || jna_path.length() == 0) {
            jna_path = System.getProperty(JNA_PATH);
            if(jna_path == null || jna_path.length() == 0)
                jna_path = System.getenv(JNA_PATH_ENV);
        }
        if(jna_path != null && jna_path.length() > 0)
            System.setProperty(JNA_PATH, jna_path);
        if(libname != null)
            libName = libname;
    }

    static private Nc4prototypes load()
    {
        if(nc4 == null) {
            if(jnaPath == null) {
                jnaPath = System.getProperty(JNA_PATH);
                if(jnaPath == null) {
                    jnaPath = "/usr/jna_lib/";
                    System.setProperty(JNA_PATH, jnaPath);
                }
            }
            //Native.setProtected(true);
            nc4 = (Nc4prototypes) Native.loadLibrary(libName, Nc4prototypes.class);
            if(debug)
                System.out.printf(" Netcdf nc_inq_libvers='%s' isProtected=%s %n ", nc4.nc_inq_libvers(), Native.isProtected());
        }
        return nc4;
    }

    static {
        String jnapath = System.getProperty(JNA_PATH);
        if(jnapath == null || jnapath.length() == 0){
            jnapath = System.getenv(JNA_PATH_ENV);
            if(jnapath != null && jnapath.length() > 0)
                System.setProperty(JNA_PATH, jnapath);
        }
    }

    //////////////////////////////////////////////////
    // Instance Variables

    private NetcdfFileWriter.Version version = null;  // can use c library to create these different version files
    private NetcdfFile ncfile = null;
    private int ncid = -1;    // file id
    private int format = 0;       // nc4 or nc3
    private boolean isClosed = false;
    private Map<Integer, UserType> userTypes = new HashMap<Integer, UserType>();  // hash by typeid
    private Map<Group, Integer> groupHash = new HashMap<Group, Integer>();  // group, nc4 grpid
    private Nc4Chunking chunker = new Nc4ChunkingStrategyImpl();

    //////////////////////////////////////////////////
    // Constructor(s)

    public Nc4Iosp()
    {
        this(NetcdfFileWriter.Version.netcdf4); // ensure the version always has a value
    }

    public Nc4Iosp(NetcdfFileWriter.Version version)
    {
        this.version = version;
    }

    public void setChunker(Nc4Chunking chunker)
    {
        if(chunker != null)
            this.chunker = chunker;
    }

    public boolean isValidFile(RandomAccessFile raf)
        throws IOException
    {
        boolean match = false;
        if(raf.getLocation().endsWith(".nc")) {
            long savepos = raf.getFilePointer();
            raf.seek(1);
            byte[] hdr = new byte[3];
            raf.read(hdr);
            String shdr = new String(hdr, "US-ASCII");
            if("HDF".equals(shdr)) match = true;
            raf.seek(savepos);
        }
        return match;
    }

    public String getFileTypeId()
    {
        return version.isNetdf4format() ? DataFormatType.NETCDF4.toString()
            : DataFormatType.NETCDF.toString();
    }

    public String getFileTypeDescription()
    {
        return "Netcdf/JNI: " + version;
    }

    public void close()
        throws IOException
    {
        if(isClosed) return;
        if(ncid < 0) return;
        int ret = nc4.nc_close(ncid);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        isClosed = true;
        // System.out.printf("%s closed%n", ncfile.getLocation());
    }

    public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
      _open(raf, ncfile, true);
    }

    public void openForWriting(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {
      _open(raf, ncfile, false);
    }

    private void _open(RandomAccessFile raf, NetcdfFile ncfile, boolean readOnly) throws IOException {
      load(); // load jni
      this.ncfile = ncfile;
  
      // open
      if (debug) System.out.println("open " + ncfile.getLocation());
      IntByReference ncidp = new IntByReference();
      int ret = nc4.nc_open(ncfile.getLocation(), readOnly ? NC_NOWRITE : NC_WRITE, ncidp);
      if (ret != 0) throw new IOException(ret + ": " + nc4.nc_strerror(ret));
      ncid = ncidp.getValue();
  
      // format
      IntByReference formatp = new IntByReference();
      ret = nc4.nc_inq_format(ncid, formatp);
      if (ret != 0) throw new IOException(ret + ": " + nc4.nc_strerror(ret));
      format = formatp.getValue();
      if (debug) System.out.printf("open %s id=%d format=%d %n", ncfile.getLocation(), ncid, format);
  
      // read root group
      makeGroup(ncid, new Group4(ncfile.getRootGroup(), null));
      ncfile.finish();
    }

    private void makeGroup(int grpid, Group4 g4)
        throws IOException
    {
        groupHash.put(g4.g, grpid);

        makeDimensions(grpid, g4);
        makeUserTypes(grpid, g4.g);

        // group attributes
        IntByReference ngattsp = new IntByReference();
        int ret = nc4.nc_inq_natts(grpid, ngattsp);
        if(ret != 0) throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        List<Attribute> gatts = makeAttributes(grpid, Nc4prototypes.NC_GLOBAL, ngattsp.getValue(), null);
        for(Attribute att : gatts) {
            ncfile.addAttribute(g4.g, att);
            if(debug) System.out.printf(" add Global Attribute %s %n", att);
        }

        makeVariables(grpid, g4.g);

        if(format == Nc4prototypes.NC_FORMAT_NETCDF4) {
            // read subordinate groups
            IntByReference numgrps = new IntByReference();
            ret = nc4.nc_inq_grps(grpid, numgrps, Pointer.NULL);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            int[] group_ids = new int[numgrps.getValue()];
            ret = nc4.nc_inq_grps(grpid, numgrps, group_ids);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));

            for(int group_id : group_ids) {
                byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
                ret = nc4.nc_inq_grpname(group_id, name);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                Group child = new Group(ncfile, g4.g, makeString(name));
                g4.g.addGroup(child);
                makeGroup(group_id, new Group4(child, g4));
            }
        }
    }

    private void makeDimensions(int grpid, Group4 g4)
        throws IOException
    {
        IntByReference ndimsp = new IntByReference();
        int ret = nc4.nc_inq_ndims(grpid, ndimsp);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        int[] dimids = new int[ndimsp.getValue()];
        ret = nc4.nc_inq_dimids(grpid, ndimsp, dimids, 0);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        IntByReference nunlimdimsp = new IntByReference();
        int[] unlimdimids = new int[Nc4prototypes.NC_MAX_DIMS];
        ret = nc4.nc_inq_unlimdims(grpid, nunlimdimsp, unlimdimids);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        int ndims = ndimsp.getValue();
        for(int i = 0;i < ndims;i++) {
            byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
            NativeLongByReference lenp = new NativeLongByReference();
            ret = nc4.nc_inq_dim(grpid, dimids[i], name, lenp);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            String dname = makeString(name);

            boolean isUnlimited = containsInt(nunlimdimsp.getValue(), unlimdimids, i);
            Dimension dim = new Dimension(dname, lenp.getValue().intValue(), true, isUnlimited, false);
            ncfile.addDimension(g4.g, dim);
            if(debug) System.out.printf(" add Dimension %s (%d) %n", dim, dimids[i]);
        }
    }

    private boolean containsInt(int n, int[] have, int want)
    {
        for(int i = 0;i < n;i++) {
            if(have[i] == want) return true;
        }
        return false;
    }

    private void updateDimensions(Group g)
        throws IOException
    {
        int grpid = groupHash.get(g);

        IntByReference nunlimdimsp = new IntByReference();
        int[] unlimdimids = new int[Nc4prototypes.NC_MAX_DIMS];
        int ret = nc4.nc_inq_unlimdims(grpid, nunlimdimsp, unlimdimids);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        int ndims = nunlimdimsp.getValue();
        for(int i = 0;i < ndims;i++) {
            byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
            NativeLongByReference lenp = new NativeLongByReference();
            ret = nc4.nc_inq_dim(grpid, unlimdimids[i], name, lenp);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            String dname = makeString(name);

            Dimension d = g.findDimension(dname);
            if(d == null)
                throw new IllegalStateException("Cant find dimension " + dname);

            if(!d.isUnlimited())
                throw new IllegalStateException("dimension " + dname + " should be unlimited");

            int len = lenp.getValue().intValue();
            if(len != d.getLength()) {
                d.setLength(len);
                // must update all variables that use this dimension
                for(Variable var : g.getVariables()) {
                    if(contains(var.getDimensions(), d)) {
                        var.resetShape();
                        var.invalidateCache();
                    }
                }
            }
        }

        // recurse
        for(Group child : g.getGroups())
            updateDimensions(child);
    }

    // must check by name, not object equality
    private boolean contains(List<Dimension> dims, Dimension want)
    {
        for(Dimension have : dims) {
            if(have.getShortName().equals(want.getShortName())) return true;
        }
        return false;
    }

    private String makeString(byte[] b)
        throws IOException
    {
        // null terminates
        int count = 0;
        while(count < b.length) {
            if(b[count] == 0) break;
            count++; // dont include the terminating 0
        }

        // copy if its small
        if(count < b.length / 2) {
            byte[] bb = new byte[count];
            System.arraycopy(b, 0, bb, 0, count);
            b = bb;
        }
        return new String(b, 0, count, CDM.utf8Charset); // all strings are considered to be UTF-8 unicode.
    }

    // follow what happens in the Java side
    private String makeAttString(byte[] b)
        throws IOException
    {
        // null terminates
        int count = 0;
        while(count < b.length) {
            if(b[count] == 0) break;
            count++; // dont include the terminating 0
        }
        return new String(b, 0, count, CDM.utf8Charset); // all strings are considered to be UTF-8 unicode.
    /*
 char[] carray = new char[count];
 for (int i=0; i<count; i++)
   carray[i] = (char) DataType.unsignedByteToShort(b[i]);

 return new String(carray); */
    }

    private List<Attribute>
    makeAttributes(int grpid, int varid, int natts, Variable v)
        throws IOException
    {
        List<Attribute> result = new ArrayList<Attribute>(natts);

        for(int attnum = 0;attnum < natts;attnum++) {
            byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
            int ret = nc4.nc_inq_attname(grpid, varid, attnum, name);
            if(ret != 0)
                throw new IOException(nc4.nc_strerror(ret) + " varid=" + varid + " attnum=" + attnum);
            String attname = makeString(name);
            IntByReference xtypep = new IntByReference();
            ret = nc4.nc_inq_atttype(grpid, varid, attname, xtypep);
            if(ret != 0)
                throw new IOException(nc4.nc_strerror(ret) + " varid=" + varid + "attnum=" + attnum);

            /* xtypep : Pointer to location for returned attribute type,
                  one of the set of predefined netCDF external data types.
                  The type of this parameter, nc_type, is defined in the netCDF
              header file.  The valid netCDF external data types are
              NC_BYTE, NC_CHAR, NC_SHORT, NC_INT, NC_FLOAT, and NC_DOUBLE.
              If this parameter is given as '0' (a null pointer), no type
              will be returned so no variable to hold the type needs to be declared.
            */
            int type = xtypep.getValue();
            NativeLongByReference lenp = new NativeLongByReference();
            ret = nc4.nc_inq_attlen(grpid, varid, attname, lenp);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            int len = lenp.getValue().intValue();

            // deal with empty attributes
            if(len == 0) {
                Attribute att;
                switch (type) {
                case Nc4prototypes.NC_BYTE:
                case Nc4prototypes.NC_UBYTE:
                    att = new Attribute(attname, DataType.BYTE);
                    break;
                case Nc4prototypes.NC_CHAR: // a zero length char is considered to be an empty string
                    att = new Attribute(attname, "");
                    break;
                case Nc4prototypes.NC_DOUBLE:
                    att = new Attribute(attname, DataType.DOUBLE);
                    break;
                case Nc4prototypes.NC_FLOAT:
                    att = new Attribute(attname, DataType.FLOAT);
                    break;
                case Nc4prototypes.NC_INT:
                case Nc4prototypes.NC_UINT:
                    att = new Attribute(attname, DataType.INT);
                    break;
                case Nc4prototypes.NC_UINT64:
                case Nc4prototypes.NC_INT64:
                    att = new Attribute(attname, DataType.LONG);
                    break;
                case Nc4prototypes.NC_USHORT:
                case Nc4prototypes.NC_SHORT:
                    att = new Attribute(attname, DataType.SHORT);
                    break;
                case Nc4prototypes.NC_STRING:
                    att = new Attribute(attname, DataType.STRING);
                    break;
                default:
                    log.warn("Unsupported attribute data type == " + type);
                    continue;
                }
                result.add(att);
                continue; // avoid reading the values since there are none.
            }

            // read the att values
            Array values = null;

            switch (type) {

            case Nc4prototypes.NC_UBYTE:
                byte[] valbu = new byte[len];
                ret = nc4.nc_get_att_uchar(grpid, varid, attname, valbu);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[]{len}, valbu);
                break;

            case Nc4prototypes.NC_BYTE:
                byte[] valb = new byte[len];
                ret = nc4.nc_get_att_schar(grpid, varid, attname, valb);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[]{len}, valb);
                break;

            case Nc4prototypes.NC_CHAR:
                byte[] text = new byte[len];
                ret = nc4.nc_get_att_text(grpid, varid, attname, text);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                Attribute att = new Attribute(attname, makeAttString(text));
                result.add(att);
                break;

            case Nc4prototypes.NC_DOUBLE:
                double[] vald = new double[len];
                ret = nc4.nc_get_att_double(grpid, varid, attname, vald);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[]{len}, vald);
                break;

            case Nc4prototypes.NC_FLOAT:
                float[] valf = new float[len];
                ret = nc4.nc_get_att_float(grpid, varid, attname, valf);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(DataType.FLOAT.getPrimitiveClassType(), new int[]{len}, valf);
                break;

            case Nc4prototypes.NC_UINT:
                int[] valiu = new int[len];
                ret = nc4.nc_get_att_uint(grpid, varid, attname, valiu);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(DataType.INT.getPrimitiveClassType(), new int[]{len}, valiu);
                break;

            case Nc4prototypes.NC_INT:
                int[] vali = new int[len];
                ret = nc4.nc_get_att_int(grpid, varid, attname, vali);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(DataType.INT.getPrimitiveClassType(), new int[]{len}, vali);
                break;

            case Nc4prototypes.NC_UINT64:
                long[] vallu = new long[len];
                ret = nc4.nc_get_att_ulonglong(grpid, varid, attname, vallu);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(DataType.LONG.getPrimitiveClassType(), new int[]{len}, vallu);
                break;

            case Nc4prototypes.NC_INT64:
                long[] vall = new long[len];
                ret = nc4.nc_get_att_longlong(grpid, varid, attname, vall);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(DataType.LONG.getPrimitiveClassType(), new int[]{len}, vall);
                break;

            case Nc4prototypes.NC_USHORT:
                short[] valsu = new short[len];
                ret = nc4.nc_get_att_ushort(grpid, varid, attname, valsu);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(DataType.SHORT.getPrimitiveClassType(), new int[]{len}, valsu);
                break;

            case Nc4prototypes.NC_SHORT:
                short[] vals = new short[len];
                ret = nc4.nc_get_att_short(grpid, varid, attname, vals);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(DataType.SHORT.getPrimitiveClassType(), new int[]{len}, vals);
                break;

            case Nc4prototypes.NC_STRING:
                if(len > 1)
                    System.out.println("HEY");
                String[] valss = new String[len];
                ret = nc4.nc_get_att_string(grpid, varid, attname, valss);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));
                values = Array.factory(String.class, new int[]{len}, valss);
                break;

            default:
                UserType userType = userTypes.get(type);
                if(userType == null) {
                    log.warn("Unsupported attribute data type == " + type);
                    continue;
                } else if(userType.typeClass == Nc4prototypes.NC_ENUM) {
                    result.add(readEnumAttValues(grpid, varid, attname, len, userType));
                    continue;
                } else if(userType.typeClass == Nc4prototypes.NC_OPAQUE) {
                    result.add(readOpaqueAttValues(grpid, varid, attname, len, userType));
                    continue;
                } else if(userType.typeClass == Nc4prototypes.NC_VLEN) {
                    values = readVlenAttValues(grpid, varid, attname, len, userType);
                } else if(userType.typeClass == Nc4prototypes.NC_COMPOUND) {
                    readCompoundAttValues(grpid, varid, attname, len, userType, result, v);
                    continue;
                } else {
                    log.warn("Unsupported attribute data type == " + userType);
                    continue;
                }
            }
            if(values != null) {
                Attribute att = new Attribute(attname, values);
                result.add(att);
            }
        }
        return result;
    }

    private Array
    readVlenAttValues(int grpid, int varid, String attname, int len, UserType userType)
        throws IOException
    {
        Nc4prototypes.Vlen_t[] vlen = new Nc4prototypes.Vlen_t[len];
        int ret = nc4.nc_get_att(grpid, varid, attname, vlen);    // vlen
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        int count = 0;
        for(int i = 0;i < len;i++)
            count += vlen[i].len;

        switch (userType.baseTypeid) {
        case Nc4prototypes.NC_INT:
            Array intArray = Array.factory(DataType.INT, new int[]{count});
            IndexIterator iter = intArray.getIndexIterator();
            for(int i = 0;i < len;i++) {
                //System.out.print(" len=" + vlen[i].len + "; p= " + vlen[i].p + ";");
                int[] ba = vlen[i].p.getIntArray(0, vlen[i].len);
                for(int j = 0;j < ba.length;j++) {
                    //System.out.print(" " + ba[j]);
                    iter.setIntNext(ba[j]);
                }
                //System.out.println();
            }
            return intArray;

        case Nc4prototypes.NC_FLOAT:
            Array fArray = Array.factory(DataType.FLOAT, new int[]{count});
            iter = fArray.getIndexIterator();
            for(int i = 0;i < len;i++) {
                float[] ba = vlen[i].p.getFloatArray(0, vlen[i].len);
                for(int j = 0;j < ba.length;j++)
                    iter.setFloatNext(ba[j]);
            }
            return fArray;
        }
        return null;
    }

    private Attribute
    readEnumAttValues(int grpid, int varid, String attname, int len, UserType userType)
        throws IOException
    {
        int ret;

        DataType dtype = convertDataType(userType.baseTypeid).dt;
        int elemSize = dtype.getSize();

        ByteBuffer bb = ByteBuffer.allocate(len * elemSize);
        ret = nc4.nc_get_att(grpid, varid, attname, bb);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        Array data = convertByteBuffer(bb, userType.baseTypeid, new int[]{len});
        IndexIterator ii = data.getIndexIterator();

        if(len == 1) {
            String val = userType.e.lookupEnumString(ii.getIntNext());
            return new Attribute(attname, val);
        } else {
            ArrayObject.D1 attArray = (ArrayObject.D1) Array.factory(DataType.STRING, new int[]{len});
            for(int i = 0;i < len;i++) {
                int val = ii.getIntNext();
                String vals = userType.e.lookupEnumString(val);
                if(vals == null)
                    throw new IOException("Illegal enum val " + val + " for attribute " + attname);
                attArray.set(i, vals);
            }
            return new Attribute(attname, attArray);
        }
    }

    private Array
    convertByteBuffer(ByteBuffer bb, int baseType, int shape[])
        throws IOException
    {

        switch (baseType) {
        case Nc4prototypes.NC_BYTE:
        case Nc4prototypes.NC_UBYTE:
            Array sArray = Array.factory(DataType.BYTE, shape, bb.array());
            return (baseType == Nc4prototypes.NC_BYTE) ? sArray : MAMath.convertUnsigned(sArray);

        case Nc4prototypes.NC_SHORT:
        case Nc4prototypes.NC_USHORT:
            ShortBuffer sb = bb.asShortBuffer();
            sArray = Array.factory(DataType.SHORT, shape, sb.array());
            return (baseType == Nc4prototypes.NC_SHORT) ? sArray : MAMath.convertUnsigned(sArray);

        case Nc4prototypes.NC_INT:
        case Nc4prototypes.NC_UINT:
            IntBuffer ib = bb.asIntBuffer();
            sArray = Array.factory(DataType.INT, shape, ib.array());
            return (baseType == Nc4prototypes.NC_INT) ? sArray : MAMath.convertUnsigned(sArray);
        }
        return null;
    }

    private Attribute
    readOpaqueAttValues(int grpid, int varid, String attname, int len, UserType userType)
        throws IOException
    {
        int total = len * userType.size;
        ByteBuffer bb = ByteBuffer.allocate(total);
        int ret = nc4.nc_get_att(grpid, varid, attname, bb);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return new Attribute(attname, Array.factory(DataType.BYTE, new int[]{total}, bb.array()));
    }

    private void
    readCompoundAttValues(int grpid, int varid, String attname, int len, UserType userType,
                          List<Attribute> result, Variable v)
        throws IOException
    {

        int buffSize = len * userType.size;
        ByteBuffer bbuff = ByteBuffer.allocate(buffSize);
        int ret = nc4.nc_get_att(grpid, varid, attname, bbuff);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        decodeCompoundData(len, userType, bbuff);

        // if its a Structure, distribute to matching fields
        if((v != null) && (v instanceof Structure)) {
            Structure s = (Structure) v;
            for(Field fld : userType.flds) {
                Variable mv = s.findVariable(fld.name);
                if(mv != null)
                    mv.addAttribute(new Attribute(attname, fld.data));
                else
                    result.add(new Attribute(attname + "." + fld.name, fld.data));
            }
        } else {
            for(Field fld : userType.flds)
                result.add(new Attribute(attname + "." + fld.name, fld.data));
        }
    }

    // LOOK: placing results in the fld of the userType - ok for production ??
    private void decodeCompoundData(int len, UserType userType, ByteBuffer bbuff)
        throws IOException
    {
        bbuff.order(ByteOrder.LITTLE_ENDIAN);

        for(Field fld : userType.flds) {
            ConvertedType ct = convertDataType(fld.fldtypeid);
            if(fld.fldtypeid == Nc4prototypes.NC_CHAR) {
                fld.data = Array.factory(DataType.STRING, new int[]{len}); // LOOK ??
            } else if(ct.isVlen) {
                fld.data = new ArrayObject(ct.dt.getPrimitiveClassType(), new int[]{len});
                // fld.data = Array.factory( Object.class, new int[] { len});  // object array
            } else {
                fld.data = Array.factory(ct.dt, new int[]{len});
            }
        }

        for(int i = 0;i < len;i++) {
            int record_start = i * userType.size;

            for(Field fld : userType.flds) {
                int pos = record_start + fld.offset;

                switch (fld.fldtypeid) {
                case Nc4prototypes.NC_CHAR:
                    // copy bytes out of buffer, make into a String object
                    int blen = 1;
                    if(fld.dims != null) {
                        Section s = new Section(fld.dims);
                        blen = (int) s.computeSize();
                    }
                    byte[] dst = new byte[blen];
                    bbuff.get(dst, 0, blen);

                    String cval = makeAttString(dst);
                    fld.data.setObject(i, cval);
                    if(debugCompoundAtt) System.out.println("result= " + cval);
                    continue;

                case Nc4prototypes.NC_UBYTE:
                case Nc4prototypes.NC_BYTE:
                    byte bval = bbuff.get(pos);
                    if(debugCompoundAtt) System.out.println("bval= " + bval);
                    fld.data.setByte(i, bval);
                    continue;

                case Nc4prototypes.NC_USHORT:
                case Nc4prototypes.NC_SHORT:
                    short sval = bbuff.getShort(pos);
                    if(debugCompoundAtt) System.out.println("sval= " + sval);
                    fld.data.setShort(i, sval);
                    continue;

                case Nc4prototypes.NC_UINT:
                case Nc4prototypes.NC_INT:
                    int ival = bbuff.getInt(pos);
                    if(debugCompoundAtt) System.out.println("ival= " + ival);
                    fld.data.setInt(i, ival);
                    continue;

                case Nc4prototypes.NC_UINT64:
                case Nc4prototypes.NC_INT64:
                    long lval = bbuff.getLong(pos);
                    if(debugCompoundAtt) System.out.println("lval= " + lval);
                    fld.data.setLong(i, lval);
                    continue;

                case Nc4prototypes.NC_FLOAT:
                    float fval = bbuff.getFloat(pos);
                    if(debugCompoundAtt) System.out.println("fval= " + fval);
                    fld.data.setFloat(i, fval);
                    continue;

                case Nc4prototypes.NC_DOUBLE:
                    double dval = bbuff.getDouble(pos);
                    if(debugCompoundAtt) System.out.println("dval= " + dval);
                    fld.data.setDouble(i, dval);
                    continue;

                case Nc4prototypes.NC_STRING:
                    lval = bbuff.getLong(pos);
                    Pointer p = new MyPointer(lval);
                    String strval = p.getString(0, false);
                    fld.data.setObject(i, strval);
                    if(debugCompoundAtt) System.out.println("result= " + strval);
                    continue;

                default:
                    UserType subUserType = userTypes.get(fld.fldtypeid);
                    if(subUserType == null) {
                        throw new IOException("Unknown compound user type == " + fld);
                    } else if(subUserType.typeClass == Nc4prototypes.NC_ENUM) {
                    } else if(subUserType.typeClass == Nc4prototypes.NC_VLEN) {
                        decodeVlenField(fld, subUserType, pos, i, bbuff);
                        break;
                    } else if(subUserType.typeClass == Nc4prototypes.NC_OPAQUE) {
                        //return readOpaque(grpid, varid, len, userType.size);
                    } else if(subUserType.typeClass == Nc4prototypes.NC_COMPOUND) {
                        //return readCompound(grpid, varid, len, userType);
                    }

                    log.warn("UNSUPPORTED compound fld.fldtypeid= " + fld.fldtypeid);
                    continue;
                } // switch on fld type
            } // loop over fields
        } // loop over len
    }

    /////////////////////////////////////////////////////////////////////////////

    private void makeVariables(int grpid, Group g)
        throws IOException
    {
        IntByReference nvarsp = new IntByReference();
        int ret = nc4.nc_inq_nvars(grpid, nvarsp);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        int nvars = nvarsp.getValue();
        if(debug) System.out.printf(" nvars= %d %n", nvars);

        int[] varids = new int[nvars];
        ret = nc4.nc_inq_varids(grpid, nvarsp, varids);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        for(int i = 0;i < varids.length;i++) {
            int varno = varids[i];
            if(varno != i) log.error("HEY varno=%d i=%d%n", varno, i);

            byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
            IntByReference xtypep = new IntByReference();
            IntByReference ndimsp = new IntByReference();
            int[] dimids = new int[Nc4prototypes.NC_MAX_DIMS];
            IntByReference nattsp = new IntByReference();

            ret = nc4.nc_inq_var(grpid, varno, name, xtypep, ndimsp, dimids, nattsp);
            if(ret != 0)
                throw new IOException(nc4.nc_strerror(ret));

            // figure out the datatype
            int typeid = xtypep.getValue();
            //DataType dtype = convertDataType(typeid).dt;

            String vname = makeString(name);
            Vinfo vinfo = new Vinfo(grpid, varno, typeid);

            // figure out the dimensions
            String dimList = makeDimList(grpid, ndimsp.getValue(), dimids);
            UserType utype = userTypes.get(typeid);
            if(utype != null) {
                vinfo.utype = utype;
                if(utype.typeClass == Nc4prototypes.NC_VLEN)  // LOOK ??
                    dimList = dimList + " *";
            }

            Variable v = makeVariable(g, null, vname, typeid, dimList);
            /* if(dtype != DataType.STRUCTURE) {
               v = new Variable(ncfile, g, null, vname, dtype, dimList);
           } else if(utype != null) {
               Structure s = new Structure(ncfile, g, null, vname);
               s.setDimensions(dimList);
               v = s;
               if(utype.flds == null)
               utype.readFields();
               for(Field f : utype.flds) {
               s.addMemberVariable(f.makeMemberVariable(g, s));
           }
           } else {
               throw new IllegalStateException("Dunno what to with " + dtype);
           } */

            // create the Variable
            ncfile.addVariable(g, v);
            v.setSPobject(vinfo);

            if(isUnsigned(typeid))
                v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));

            // read Variable attributes
            List<Attribute> atts = makeAttributes(grpid, varno, nattsp.getValue(), v);
            for(Attribute att : atts) {
                v.addAttribute(att);
            }

            if(debug) System.out.printf(" added Variable %s %n", v);
        }
    }

    private Variable
    makeVariable(Group g, Structure parent, String vname, int typeid, String dimList)
        throws IOException
    {
        ConvertedType cvttype = convertDataType(typeid);
        DataType dtype = cvttype.dt;
        UserType utype = userTypes.get(typeid);

        Variable v;
        if(dtype != DataType.STRUCTURE) {
            v = new Variable(ncfile, g, parent, vname, dtype, dimList);
        } else if(utype != null) {
            Structure s = new Structure(ncfile, g, parent, vname);
            s.setDimensions(dimList);
            v = s;
            if(utype.flds == null)
                utype.readFields();

            for(Field f : utype.flds) {
                s.addMemberVariable(f.makeMemberVariable(g, s));
            }
        } else {
            throw new IllegalStateException("Dunno what to with " + dtype);
        }

        if(isUnsigned(typeid))
            v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));

        if(dtype.isEnum()) {
            EnumTypedef enumTypedef = g.findEnumeration(utype.name);
            v.setEnumTypedef(enumTypedef);
        }

        return v;
    }

    private String makeDimList(int grpid, int ndimsp, int[] dims)
        throws IOException
    {
        StringBuilder sb = new StringBuilder();
        for(int i = 0;i < ndimsp;i++) {
            byte[] name = new byte[Nc4prototypes.NC_MAX_NAME + 1];
            int ret = nc4.nc_inq_dimname(grpid, dims[i], name);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            String dname = makeString(name);
            sb.append(dname);
            sb.append(" ");
        }
        return sb.toString();
    }

    //////////////////////////////////////////////////////////////////////////

    private class Vinfo
    {
        int grpid, varid, typeid;
        UserType utype; // may be null

        Vinfo(int grpid, int varid, int typeid)
        {
            this.grpid = grpid;
            this.varid = varid;
            this.typeid = typeid;
        }
    }

    private class Group4
    {
        Group g;
        Group4 parent;
        Map<Dimension, Integer> dimHash;

        Group4(Group g, Group4 parent)
        {
            this.g = g;
            this.parent = parent;
        }
    }

    private class UserType
    {
        int grpid;
        int typeid;
        String name;
        int size; // the size of the user defined type
        int baseTypeid; // the base typeid for vlen and enum types
        long nfields; // the number of fields for enum and compound types
        int typeClass; // the class of the user defined type: NC_VLEN, NC_OPAQUE, NC_ENUM, or NC_COMPOUND.

        EnumTypedef e;
        List<Field> flds;

        UserType(int grpid, int typeid, String name, long size, int baseTypeid, long nfields, int typeClass)
            throws IOException
        {
            this.grpid = grpid;
            this.typeid = typeid;
            this.name = name;
            this.size = (int) size;
            this.baseTypeid = baseTypeid;
            this.nfields = nfields;
            this.typeClass = typeClass;
            if(debugUserTypes) System.out.printf("%s%n", this);

            if(typeClass == Nc4prototypes.NC_COMPOUND)
                readFields();
        }

        void setEnum(EnumTypedef e)
        {
            this.e = e;
            // set the enum's basetype
            if(baseTypeid > 0 && baseTypeid <= NC_MAX_ATOMIC_TYPE) {
                DataType cdmtype = null;
                boolean isunsigned = false;
                switch (baseTypeid) {
                case NC_CHAR:
                case NC_UBYTE:
                    isunsigned = true;
                case NC_BYTE:
                    cdmtype = DataType.ENUM1;
                    break;
                case NC_USHORT:
                    isunsigned = true;
                case NC_SHORT:
                    cdmtype = DataType.ENUM1;
                    break;
                case NC_UINT:
                    isunsigned = true;
                case NC_INT:
                default:
                    cdmtype = DataType.ENUM4;
                    break;
                }
                // not supported this.e.setUnsigned(isunsigned);
                this.e.setBaseType(cdmtype);
            }
        }

        void addField(Field fld)
        {
            if(flds == null)
                flds = new ArrayList<Field>(10);
            flds.add(fld);
        }

        public String toString2()
        {
            return "name='" + name + "' id=" + getDataTypeName(typeid) + " userType=" + getDataTypeName(typeClass)
                + " baseType=" + getDataTypeName(baseTypeid);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("UserType");
            sb.append("{grpid=").append(grpid);
            sb.append(", typeid=").append(typeid);
            sb.append(", name='").append(name).append('\'');
            sb.append(", size=").append(size);
            sb.append(", baseTypeid=").append(baseTypeid);
            sb.append(", nfields=").append(nfields);
            sb.append(", typeClass=").append(typeClass);
            sb.append(", e=").append(e);
            sb.append('}');
            return sb.toString();
        }

        void readFields()
            throws IOException
        {
            for(int fldidx = 0;fldidx < nfields;fldidx++) {
                byte[] fldname = new byte[Nc4prototypes.NC_MAX_NAME + 1];
                IntByReference field_typeidp = new IntByReference();
                IntByReference ndimsp = new IntByReference();
                NativeLongByReference offsetp = new NativeLongByReference();

                int[] dims = new int[Nc4prototypes.NC_MAX_DIMS];
                int ret = nc4.nc_inq_compound_field(grpid, typeid, fldidx, fldname, offsetp, field_typeidp, ndimsp, dims);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));

                Field fld = new Field(grpid, typeid, fldidx, makeString(fldname), offsetp.getValue().intValue(),
                    field_typeidp.getValue(), ndimsp.getValue(), dims);

                addField(fld);
                if(debugUserTypes) System.out.printf(" %s add field= %s%n", name, fld);
            }
        }
    }

    // encapsolate the fields in a compound type
    private class Field
    {
        int grpid;
        int typeid; // containing structure
        int fldidx;
        String name;
        int offset;
        int fldtypeid;
        int ndims;
        int[] dims;

        ConvertedType ctype;
        //int total_size;
        Array data;

        // grpid, varid, fldidx, fldname, offsetp, field_typeidp, ndimsp, dim_sizesp
        Field(int grpid, int typeid, int fldidx, String name, int offset, int fldtypeid, int ndims, int[] dimz)
        {
            this.grpid = grpid;
            this.typeid = typeid;
            this.fldidx = fldidx;
            this.name = name;
            this.offset = offset;
            this.fldtypeid = fldtypeid;
            // Reduce the stored dimensions to match the actual rank
            // because some code (i.e. Section) is using this.dims.length
            // to compute the rank.
            this.ndims = ndims;
            this.dims = new int[ndims];
            System.arraycopy(dimz, 0, this.dims, 0, ndims);

            ctype = convertDataType(fldtypeid);
            Section s = new Section(this.dims);
            //total_size = (int) s.computeSize() * ctype.dt.getSize();

            if(isVlen(fldtypeid)) {
                int[] edims = new int[ndims + 1];
                if(ndims > 0)
                    System.arraycopy(dimz, 0, edims, 0, ndims);
                edims[ndims] = -1;
                this.dims = edims;
                this.ndims++;
            }
        }

        public String toString2()
        {
            return "name='" + name + " fldtypeid=" + getDataTypeName(fldtypeid) + " ndims=" + ndims + " offset=" + offset;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("Field");
            sb.append("{grpid=").append(grpid);
            sb.append(", typeid=").append(typeid);
            sb.append(", fldidx=").append(fldidx);
            sb.append(", name='").append(name).append('\'');
            sb.append(", offset=").append(offset);
            sb.append(", fldtypeid=").append(fldtypeid);
            sb.append(", ndims=").append(ndims);
            sb.append(", dims=").append(dims == null ? "null" : "");
            for(int i = 0;dims != null && i < dims.length;++i)
                sb.append(i == 0 ? "" : ", ").append(dims[i]);
            sb.append(", dtype=").append(ctype.dt);
            if(ctype.isVlen) sb.append("(vlen)");
            sb.append('}');
            return sb.toString();
        }

        /* Variable makeMemberVariable(Group g, Structure parent)
      {
          Variable v = new Variable(ncfile, g, parent, name);
          v.setDataType(convertDataType(fldtypeid).dt);
          if(isUnsigned(fldtypeid))
          v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));

          if(ctype.isVlen) {
          v.setDimensions("*");
          } else {
              try {
              v.setDimensionsAnonymous(dims);
              } catch (InvalidRangeException e) {
                  e.printStackTrace();
              }
          }
          return v;
      } */

        Variable makeMemberVariable(Group g, Structure parent)
            throws IOException
        {
            Variable v = makeVariable(g, parent, name, fldtypeid, "");

            //if(ctype.isVlen) {
            //v.setDimensions("*");
            //} else
            {
                try {
                    v.setDimensionsAnonymous(dims); // LOOK no shared dimensions ?
                } catch (InvalidRangeException e) {
                    e.printStackTrace();
                }
            }
            return v;
        }
    }

    private void makeUserTypes(int grpid, Group g)
        throws IOException
    {
        // find user types in this group
        IntByReference ntypesp = new IntByReference();
        int ret = nc4.nc_inq_typeids(grpid, ntypesp, Pointer.NULL);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        int ntypes = ntypesp.getValue();
        if(ntypes == 0) return;
        int[] xtypes = new int[ntypes];
        ret = nc4.nc_inq_typeids(grpid, ntypesp, xtypes);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        // for each defined "user type", get information, store in Map
        for(int typeid : xtypes) {
            byte[] nameb = new byte[Nc4prototypes.NC_MAX_NAME + 1];
            NativeLongByReference sizep = new NativeLongByReference();
            IntByReference baseType = new IntByReference();
            NativeLongByReference nfieldsp = new NativeLongByReference();
            IntByReference classp = new IntByReference();

            /*
            ncid    The ncid for the group containing the user defined type.
            xtype   The typeid for this type, as returned by nc_def_compound, nc_def_opaque, nc_def_enum, nc_def_vlen, or nc_inq_var.
            name    If non-NULL, the name of the user defined type will be copied here. It will be NC_MAX_NAME bytes or less.
            sizep   If non-NULL, the (in-memory) size of the type in bytes will be copied here. VLEN type size is the size of nc_vlen_t.
            String size is returned as the size of a character pointer. The size may be used to malloc space for the data, no matter what the type.
            nfieldsp If non-NULL, the number of fields will be copied here for enum and compound types.
            classp  Return the class of the user defined type, NC_VLEN, NC_OPAQUE, NC_ENUM, or NC_COMPOUND.
            */
            ret = nc4.nc_inq_user_type(grpid, typeid, nameb, sizep, baseType, nfieldsp, classp); // size_t
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));

            String name = makeString(nameb);
            int utype = classp.getValue();
            if(debug) System.out.printf(" user type id=%d name=%s size=%d baseType=%d nfields=%d class=%d%n",
                typeid, name, sizep.getValue().longValue(), baseType.getValue(), nfieldsp.getValue().longValue(), utype);

            UserType ut = new UserType(grpid, typeid, name, sizep.getValue().longValue(), baseType.getValue(),
                nfieldsp.getValue().longValue(), utype);
            userTypes.put(typeid, ut);

            if(utype == Nc4prototypes.NC_ENUM) {
                Map<Integer, String> map = makeEnum(grpid, typeid);
                EnumTypedef e = new EnumTypedef(name, map);
                g.addEnumeration(e);
                ut.setEnum(e);
            } else if(utype == Nc4prototypes.NC_OPAQUE) {
                byte[] nameo = new byte[Nc4prototypes.NC_MAX_NAME + 1];
                NativeLongByReference sizep2 = new NativeLongByReference();
                ret = nc4.nc_inq_opaque(grpid, typeid, nameo, sizep2);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));

                // doesnt seem to be any new info
                // String nameos = makeString(nameo);
                //System.out.printf("   opaque type=%d name=%s size=%d %n ",
                //    typeid, nameos, sizep2.getValue().longValue());
            }
        }
    }

    private Map<Integer, String> makeEnum(int grpid, int xtype)
        throws IOException
    {
        byte[] nameb = new byte[Nc4prototypes.NC_MAX_NAME + 1];
        IntByReference baseType = new IntByReference();
        NativeLongByReference baseSize = new NativeLongByReference();
        NativeLongByReference numMembers = new NativeLongByReference();

        int ret = nc4.nc_inq_enum(grpid, xtype, nameb, baseType, baseSize, numMembers);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        int nmembers = numMembers.getValue().intValue();
        String name = makeString(nameb);

        //System.out.printf(" type=%d name=%s baseType=%d baseType=%d numMembers=%d %n ",
        //    xtype, name, baseType.getValue(), baseSize.getValue().longValue(), nmembers);
        Map<Integer, String> map = new HashMap<Integer, String>(2 * nmembers);

        for(int i = 0;i < nmembers;i++) {
            byte[] mnameb = new byte[Nc4prototypes.NC_MAX_NAME + 1];
            IntByReference value = new IntByReference();
            ret = nc4.nc_inq_enum_member(grpid, xtype, i, mnameb, value); // void *
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));

            String mname = makeString(mnameb);
            //System.out.printf(" member name=%s value=%d %n ",  mname, value.getValue());
            map.put(value.getValue(), mname);
        }
        return map;
    }

    /////////////////////////////////////////////////////////////////////////////////

    @Override
    public Array readData(Variable v2, Section section)
        throws IOException, InvalidRangeException
    {
        Vinfo vinfo = (Vinfo) v2.getSPobject();
        int vlen = (int) v2.getSize();
        int len = (int) section.computeSize();
        if(vlen == len) // entire array
            return readDataAll(vinfo.grpid, vinfo.varid, vinfo.typeid, v2.getShapeAsSection());

        //if(!section.isStrided()) // optimisation for unstrided section
        //  return readUnstrided(vinfo.grpid, vinfo.varid, vinfo.typeid, section);

        return readDataSection(vinfo.grpid, vinfo.varid, vinfo.typeid, section);
    }

    private Array readDataSection(int grpid, int varid, int typeid, Section section)
        throws IOException, InvalidRangeException
    {
        // general sectioning with strides
        long[] origin = convert(section.getOrigin());
        long[] shape = convert(section.getShape());
        long[] stride = convert(section.getStride());
        boolean isUnsigned = isUnsigned(typeid);
        int len = (int) section.computeSize();
        Array values;

        switch (typeid) {
        // int nc_get_vars_schar(int ncid, int varid, long[] startp, long[] countp, int[] stridep, byte[] ip);

        case Nc4prototypes.NC_BYTE:
        case Nc4prototypes.NC_UBYTE:
            byte[] valb = new byte[len];
            int ret;
            ret = isUnsigned ? nc4.nc_get_vars_uchar(grpid, varid, origin, shape, stride, valb)
                : nc4.nc_get_vars_schar(grpid, varid, origin, shape, stride, valb);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            values = Array.factory(DataType.BYTE.getPrimitiveClassType(), section.getShape(), valb);
            break;

        case Nc4prototypes.NC_CHAR:
            byte[] valc = new byte[len];
            ret = nc4.nc_get_vars_text(grpid, varid, origin, shape, stride, valc);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            values = Array.factory(DataType.CHAR.getPrimitiveClassType(), section.getShape(), IospHelper.convertByteToChar(valc));
            break;

        case Nc4prototypes.NC_DOUBLE:
            double[] vald = new double[len];
            ret = nc4.nc_get_vars_double(grpid, varid, origin, shape, stride, vald);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            values = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), section.getShape(), vald);
            break;

        case Nc4prototypes.NC_FLOAT:
            float[] valf = new float[len];
            ret = nc4.nc_get_vars_float(grpid, varid, origin, shape, stride, valf);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            values = Array.factory(DataType.FLOAT.getPrimitiveClassType(), section.getShape(), valf);
            break;

        case Nc4prototypes.NC_INT:
            int[] vali = new int[len];

            ret = isUnsigned ? nc4.nc_get_vars_uint(grpid, varid, origin, shape, stride, vali)
                : nc4.nc_get_vars_int(grpid, varid, origin, shape, stride, vali);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            values = Array.factory(DataType.INT.getPrimitiveClassType(), section.getShape(), vali);
            break;

        case Nc4prototypes.NC_INT64:
            long[] vall = new long[len];
            ret = isUnsigned ? nc4.nc_get_vars_ulonglong(grpid, varid, origin, shape, stride, vall)
                : nc4.nc_get_vars_longlong(grpid, varid, origin, shape, stride, vall);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            values = Array.factory(DataType.LONG.getPrimitiveClassType(), section.getShape(), vall);
            break;

        case Nc4prototypes.NC_SHORT:
            short[] vals = new short[len];
            ret = isUnsigned ? nc4.nc_get_vars_ushort(grpid, varid, origin, shape, stride, vals)
                : nc4.nc_get_vars_short(grpid, varid, origin, shape, stride, vals);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            values = Array.factory(DataType.SHORT.getPrimitiveClassType(), section.getShape(), vals);
            break;

        case Nc4prototypes.NC_STRING:
            String[] valss = new String[len];
            ret = nc4.nc_get_vars_string(grpid, varid, origin, shape, stride, valss);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.STRING.getPrimitiveClassType(), section.getShape(), valss);

        default:
            UserType userType = userTypes.get(typeid);
            if(userType == null) {
                throw new IOException("Unknown userType == " + typeid);
            } else if(userType.typeClass == Nc4prototypes.NC_ENUM) {
                return readDataSection(grpid, varid, userType.baseTypeid, section);
            } else if(userType.typeClass == Nc4prototypes.NC_VLEN) { // cannot subset
                return readVlen(grpid, varid, userType, section);
            } else if(userType.typeClass == Nc4prototypes.NC_OPAQUE) {
                return readOpaque(grpid, varid, section, userType.size);
            } else if(userType.typeClass == Nc4prototypes.NC_COMPOUND) {
                return readCompound(grpid, varid, section, userType);
            }
            throw new IOException("Unsupported userType = " + typeid + " userType= " + userType);
        }
        return values;
    }

    // read entire array
    private Array readDataAll(int grpid, int varid, int typeid, Section section)
        throws IOException, InvalidRangeException
    {
        int ret;
        int len = (int) section.computeSize();
        int[] shape = section.getShape();

        switch (typeid) {

        case Nc4prototypes.NC_UBYTE:
            byte[] valbu = new byte[len];
            ret = nc4.nc_get_var_ubyte(grpid, varid, valbu);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.BYTE.getPrimitiveClassType(), shape, valbu);

        case Nc4prototypes.NC_BYTE:
            byte[] valb = new byte[len];
            ret = nc4.nc_get_var_schar(grpid, varid, valb);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.BYTE.getPrimitiveClassType(), shape, valb);

        case Nc4prototypes.NC_CHAR:
            byte[] valc = new byte[len];
            ret = nc4.nc_get_var_text(grpid, varid, valc);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            char[] cvals = IospHelper.convertByteToChar(valc);
            return Array.factory(DataType.CHAR.getPrimitiveClassType(), shape, cvals);

        case Nc4prototypes.NC_DOUBLE:
            double[] vald = new double[len];
            ret = nc4.nc_get_var_double(grpid, varid, vald);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.DOUBLE.getPrimitiveClassType(), shape, vald);

        case Nc4prototypes.NC_FLOAT:
            float[] valf = new float[len];
            ret = nc4.nc_get_var_float(grpid, varid, valf);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.FLOAT.getPrimitiveClassType(), shape, valf);

        case Nc4prototypes.NC_INT:
            int[] vali = new int[len];
            ret = nc4.nc_get_var_int(grpid, varid, vali);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.INT.getPrimitiveClassType(), shape, vali);

        case Nc4prototypes.NC_INT64:
            long[] vall = new long[len];
            ret = nc4.nc_get_var_longlong(grpid, varid, vall);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.LONG.getPrimitiveClassType(), shape, vall);

        case Nc4prototypes.NC_UINT64:
            long[] vallu = new long[len];
            ret = nc4.nc_get_var_ulonglong(grpid, varid, vallu);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.LONG.getPrimitiveClassType(), shape, vallu);

        case Nc4prototypes.NC_SHORT:
            short[] vals = new short[len];
            ret = nc4.nc_get_var_short(grpid, varid, vals);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.SHORT.getPrimitiveClassType(), shape, vals);

        case Nc4prototypes.NC_USHORT:
            short[] valsu = new short[len];
            ret = nc4.nc_get_var_ushort(grpid, varid, valsu);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.SHORT.getPrimitiveClassType(), shape, valsu);

        case Nc4prototypes.NC_UINT:
            int[] valiu = new int[len];
            ret = nc4.nc_get_var_uint(grpid, varid, valiu);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.INT.getPrimitiveClassType(), shape, valiu);

        case Nc4prototypes.NC_STRING:
            String[] valss = new String[len];
            ret = nc4.nc_get_var_string(grpid, varid, valss);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            return Array.factory(DataType.STRING.getPrimitiveClassType(), shape, valss);

        default:
            UserType userType = userTypes.get(typeid);
            if(userType == null) {
                throw new IOException("Unknown userType == " + typeid);
            } else if(userType.typeClass == Nc4prototypes.NC_ENUM) {
                int buffSize = len * userType.size;
                ByteBuffer bbuff = ByteBuffer.allocate(buffSize);
                bbuff.order(ByteOrder.nativeOrder()); // c library returns in native order i hope
                // read in the data
                ret = nc4.nc_get_var(grpid, varid, bbuff);
                if(ret != 0)
                    throw new IOException(ret + ": " + nc4.nc_strerror(ret));

                switch (userType.baseTypeid) {
                case Nc4prototypes.NC_BYTE:
                case Nc4prototypes.NC_UBYTE:
                    return Array.factory(DataType.BYTE, shape, bbuff);
                case Nc4prototypes.NC_SHORT:
                case Nc4prototypes.NC_USHORT:
                    return Array.factory(DataType.SHORT, shape, bbuff);
                }
                throw new IOException("unknown type " + userType.baseTypeid);
            } else if(userType.typeClass == Nc4prototypes.NC_VLEN) {
                return readVlen(grpid, varid, userType, section);
            } else if(userType.typeClass == Nc4prototypes.NC_OPAQUE) {
                return readOpaque(grpid, varid, section, userType.size);
            } else if(userType.typeClass == Nc4prototypes.NC_COMPOUND) {
                return readCompound(grpid, varid, section, userType);
            }
            throw new IOException("Unsupported userType = " + typeid + " userType= " + userType);
        }
    }

    private Array readCompound(int grpid, int varid, Section section, UserType userType)
        throws IOException
    {
        long[] origin = convert(section.getOrigin());
        long[] shape = convert(section.getShape());
        long[] stride = convert(section.getStride());
        int len = (int) section.computeSize();

        int buffSize = len * userType.size;
        ByteBuffer bbuff = ByteBuffer.allocate(buffSize);
        bbuff.order(ByteOrder.nativeOrder()); // c library returns in native order i hope

        // read in the data
        int ret;
        ret = nc4.nc_get_vars(grpid, varid, origin, shape, stride, bbuff);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        StructureMembers sm = createStructureMembers(userType);
        ArrayStructureBB asbb = new ArrayStructureBB(sm, section.getShape(), bbuff, 0);

        // find and convert String and vlen fields, put on asbb heap
        int destPos = 0;
        for(int i = 0;i < len;i++) { // loop over each structure
            convertHeap(asbb, destPos, sm);
            destPos += userType.size;
        }
        return asbb;
    }

    private StructureMembers createStructureMembers(UserType userType)
    {
        StructureMembers sm = new StructureMembers(userType.name);
        for(Field fld : userType.flds) {
            StructureMembers.Member m = sm.addMember(fld.name, null, null, fld.ctype.dt, fld.dims);
            m.setDataParam(fld.offset);
            /* This should already have been taken care of
            if(fld.ctype.isVlen) {m.setShape(new int[]{-1});  } */

            if(fld.ctype.dt == DataType.STRUCTURE) {
                UserType nested_utype = userTypes.get(fld.fldtypeid);
                StructureMembers nested_sm = createStructureMembers(nested_utype);
                m.setStructureMembers(nested_sm);
            }
        }
        sm.setStructureSize(userType.size);
        return sm;
    }

    // LOOK: handling nested ??
    private void convertHeap(ArrayStructureBB asbb, int pos, StructureMembers sm)
        throws IOException
    {
        ByteBuffer bb = asbb.getByteBuffer();
        for(StructureMembers.Member m : sm.getMembers()) {
            if(m.getDataType() == DataType.STRING) {
                int size = m.getSize();
                int destPos = pos + m.getDataParam();
                String[] result = new String[size];
                for(int i = 0;i < size;i++) {
                    long addr = bb.getLong(pos);
                    Pointer p = new MyPointer(addr);
                    result[i] = p.getString(0, false);
                }
                int index = asbb.addObjectToHeap(result);
                bb.putInt(destPos, index); // overwrite with the index into the StringHeap
            } else if(m.isVariableLength()) {
                // We need to do like readVLEN, but store the resulting array
                // in the asbb heap (a bit of a hack).
                // we assume that pos "points" to the beginning of this structure instance
                // and so  pos + m.getDataParam() "points" to field m in this structure instance.
                int nc_vlen_t_size = (new Nc4prototypes.Vlen_t()).size();
                int startPos = pos + m.getDataParam();
                // Compute rank and size upto the first (and ideally last) VLEN
                int[] fieldshape = m.getShape();
                int prefixrank = 0;
                int size = 1;
                for(;prefixrank < fieldshape.length;prefixrank++) {
                    if(fieldshape[prefixrank] < 0) break;
                    size *= fieldshape[prefixrank];
                }
                assert size == m.getSize() : "Internal error: field size mismatch";
                Array[] fieldarray = new Array[size]; // hold all the nc_vlen_t instance data
                // destPos will point to each nc_vlen_t instance in turn
                // assuming we have 'size' such instances in a row.
                int destPos = startPos;
                for(int i = 0;i < size;i++) {
                    // vlenarray extracts the i'th nc_vlen_t contents (struct not supported).
                    Array vlenArray = decodeVlen(m.getDataType(), destPos, bb);
                    fieldarray[i] = vlenArray;
                    destPos += nc_vlen_t_size;
                }
                Array result = null;
                if(prefixrank == 0) // if scalar, return just the singleton vlen array
                    result = fieldarray[0];
                else if(prefixrank == 1)
                    result = new ArrayObject(fieldarray[0].getClass(), new int[]{size}, fieldarray);
                else {
                    // Otherwise create and fill in an n-dimensional Array Of Arrays
                    int[] newshape = new int[prefixrank];
                    System.arraycopy(fieldshape, 0, newshape, 0, prefixrank);
                    Array ndimarray = Array.factory(Array.class, newshape);
                    // Transfer the elements of data into the n-dim arrays
                    IndexIterator iter = ndimarray.getIndexIterator();
                    for(int i = 0;iter.hasNext();i++) {
                        iter.setObjectNext(fieldarray[i]);
                    }
                    result = ndimarray;
                }
                // Store result in the heap
                int index = asbb.addObjectToHeap(result);
                bb.order(ByteOrder.nativeOrder()); // the string index is always written in "native order"
                bb.putInt(startPos, index); // overwrite with the index into the StringHeap
            }
        }
    }

    private void
    decodeVlenField(Field fld, UserType userType, int pos, int idx, ByteBuffer bbuff)
        throws IOException
    {
        ConvertedType cvt = convertDataType(userType.baseTypeid);
        Array array = decodeVlen(cvt.dt, pos, bbuff);
        fld.data.setObject(idx, array);
    }

    private Array
    decodeVlen(DataType dt, int pos, ByteBuffer bbuff)
        throws IOException
    {
        Array array = null;
        int n = (int) bbuff.getLong(pos); // Note that this does not increment the buffer position
        long addr = bbuff.getLong(pos + 8); // LOOK: this assumes 64 bit pointers
        Pointer p = new MyPointer(addr);
        Object data = null;
        switch (dt) {
        case BOOLEAN: /*byte[]*/
            data = p.getByteArray(0, n);
            break;
        case ENUM1:
        case BYTE: /*byte[]*/
            data = p.getByteArray(0, n);
            break;
        case ENUM2:
        case SHORT: /*short[]*/
            data = p.getShortArray(0, n);
            break;
        case ENUM4:
        case INT: /*int[]*/
            data = p.getIntArray(0, n);
            break;
        case LONG: /*long[]*/
            data = p.getLongArray(0, n);
            break;
        case FLOAT: /*float[]*/
            data = p.getFloatArray(0, n);
            break;
        case DOUBLE: /*double[]*/
            data = p.getDoubleArray(0, n);
            break;
        case CHAR: /*char[]*/
            data = p.getCharArray(0, n);
            break;
        case STRING: /*String[]*/
            // For now we need to use p.getString()
            // because p.getStringArray(int,int) does not exist
            // in jna version 3.0.9, but does exist in
            // verssion 4.0 and possibly some intermediate versions
            String[] stringdata = new String[n];
            for(int i=0;i<n;i++)
                stringdata[i] = p.getString(i*8);
            data = stringdata;
            break;
        case OPAQUE:
        case STRUCTURE:
        default:
            throw new IllegalStateException();
        }
        array = Array.factory(dt, new int[]{n}, data);
        return array;
    }

    /**
     * Note that this only works for atomic base types;
     * structures will fail.
     */
    Array readVlen(int grpid, int varid, UserType userType, Section section)
        throws IOException
    {
        // Read all the vlen pointers
        int len = (int) section.computeSize();
        Nc4prototypes.Vlen_t[] vlen = new Nc4prototypes.Vlen_t[len];
        int ret = nc4.nc_get_var(grpid, varid, vlen);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        // Compute rank upto the first VLEN
        int prefixrank = 0;
        for(;prefixrank < section.getRank();prefixrank++) {
            if(section.getRange(prefixrank) == Range.VLEN) break;
        }

        //DataType dtype = convertDataType(userType.baseTypeid);
        //ArrayObject.D1 vlenArray = new ArrayObject.D1( dtype.getPrimitiveClassType(), len);

        // Collect the vlen's data arrays
        Object[] data = new Object[len];
        switch (userType.baseTypeid) {
        case Nc4prototypes.NC_UINT:
        case Nc4prototypes.NC_INT:
            for(int i = 0;i < len;i++) {
                int slen = vlen[i].len;
                int[] ba = vlen[i].p.getIntArray(0, slen);
                data[i] = Array.factory(DataType.INT, new int[]{slen}, ba);
            }
            break;
        case Nc4prototypes.NC_USHORT:
        case Nc4prototypes.NC_SHORT:
            for(int i = 0;i < len;i++) {
                int slen = vlen[i].len;
                short[] ba = vlen[i].p.getShortArray(0, slen);
                data[i] = Array.factory(DataType.SHORT, new int[]{slen}, ba);
            }
            break;
        case Nc4prototypes.NC_FLOAT:
            for(int i = 0;i < len;i++) {
                int slen = vlen[i].len;
                float[] ba = vlen[i].p.getFloatArray(0, slen);
                data[i] = Array.factory(DataType.FLOAT, new int[]{slen}, ba);
            }
            break;
        default:
            throw new UnsupportedOperationException("Vlen type " + userType.baseTypeid + " = " + convertDataType(userType.baseTypeid));
        }
        if(prefixrank == 0) { // if scalar, return just the len Array
            return (Array) data[0];
        } else if(prefixrank == 1)
            return (Array) new ArrayObject(data[0].getClass(), new int[]{len}, data);

        // Otherwise create and fill in an n-dimensional Array Of Arrays
        int[] shape = new int[prefixrank];
        for(int i = 0;i < prefixrank;i++)
            shape[i] = section.getRange(i).length();

        Array ndimarray = Array.factory(Array.class, shape);
        // Transfer the elements of data into the n-dim arrays
        IndexIterator iter = ndimarray.getIndexIterator();
        for(int i = 0;iter.hasNext();i++) {
            iter.setObjectNext(data[i]);
        }
        return ndimarray;
    }

    // opaques use ArrayObjects of ByteBuffer
    private Array readOpaque(int grpid, int varid, Section section, int size)
        throws IOException, InvalidRangeException
    {
        int ret;

        long[] origin = convert(section.getOrigin());
        long[] shape = convert(section.getShape());
        long[] stride = convert(section.getStride());
        int len = (int) section.computeSize();

        ByteBuffer bb = ByteBuffer.allocate(len * size);
        ret = nc4.nc_get_vars(grpid, varid, origin, shape, stride, bb);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        byte[] entire = bb.array();

        // fix: this is ignoring the rank of section.
        // was: ArrayObject values = new ArrayObject(ByteBuffer.class, new int[]{len});
        int[] intshape = new int[shape.length];
        for(int i = 0;i < intshape.length;i++) {
            intshape[i] = (int) shape[i];
        }
        ArrayObject values = new ArrayObject(ByteBuffer.class, intshape);

        int count = 0;
        IndexIterator ii = values.getIndexIterator();
        while(ii.hasNext()) {
            ii.setObjectNext(ByteBuffer.wrap(entire, count * size, size));
            count++;
        }
        return values;
    }

    /* private Array readEnum(int grpid, int varid, int baseType, int len, int[] shape)
  throws IOException, InvalidRangeException
  {
  int ret;

  ConvertedType ctype = convertDataType(baseType);
  int elemSize = ctype.dt.getSize();

  ByteBuffer bb = ByteBuffer.allocate(len * elemSize);
  ret = nc4.nc_get_var(grpid, varid, bb);
  if(ret != 0)
      throw new IOException(ret+": "+nc4.nc_strerror(ret)) ;

  switch (baseType) {
      case NCLibrary.NC_BYTE:
      case NCLibrary.NC_UBYTE:
  return Array.factory( DataType.BYTE.getPrimitiveClassType(), shape, bb.array());

      case NCLibrary.NC_SHORT:
      case NCLibrary.NC_USHORT:
  ShortBuffer sb = bb.asShortBuffer();
  return Array.factory( DataType.BYTE.getPrimitiveClassType(), shape, sb.array());

      case NCLibrary.NC_INT:
      case NCLibrary.NC_UINT:
  IntBuffer ib = bb.asIntBuffer();
  return Array.factory( DataType.BYTE.getPrimitiveClassType(), shape, ib.array());
  }

  return null;
  }  */

    private boolean isUnsigned(int type)
    {
        return (type == Nc4prototypes.NC_UBYTE) || (type == Nc4prototypes.NC_USHORT) ||
            (type == Nc4prototypes.NC_UINT) || (type == Nc4prototypes.NC_UINT64);
    }

    private boolean isVlen(int type)
    {
        UserType userType = userTypes.get(type);
        return (userType == null) ? false : (userType.typeClass == Nc4prototypes.NC_VLEN);
    }

    private boolean isStride1(int[] strides)
    {
        if(strides == null) return true;
        for(int i = 0;i < strides.length;i++) {
            if(strides[i] != 1) return false;
        }
        return true;
    }

    private long[] convert(int[] from)
    {
        long[] to = new long[from.length];
        for(int i = 0;i < from.length;i++)
            to[i] = from[i];
        return to;
    }

private class ConvertedType
{
    DataType dt;
    boolean isVlen;

    ConvertedType(DataType dt)
    {
        this.dt = dt;
    }
}

    private int convertDataType(DataType dt)
    {
        switch (dt) {
        case BYTE:
            return Nc4prototypes.NC_BYTE;
        case CHAR:
            return Nc4prototypes.NC_CHAR;
        case DOUBLE:
            return Nc4prototypes.NC_DOUBLE;
        case FLOAT:
            return Nc4prototypes.NC_FLOAT;
        case INT:
            return Nc4prototypes.NC_INT;
        case LONG:
            return Nc4prototypes.NC_INT64;
        case SHORT:
            return Nc4prototypes.NC_SHORT;
        case STRING:
            return Nc4prototypes.NC_STRING;
        case ENUM1:
        case ENUM2:
        case ENUM4:
            return Nc4prototypes.NC_ENUM;
        case OPAQUE:
            log.warn("Skipping Opaque Type");
            return -1;
        case STRUCTURE:
            return Nc4prototypes.NC_COMPOUND;
        }
        throw new IllegalArgumentException("unimplemented type == " + dt);
    }


    private ConvertedType convertDataType(int type)
    {
        switch (type) {
        case Nc4prototypes.NC_BYTE:
        case Nc4prototypes.NC_UBYTE:
            return new ConvertedType(DataType.BYTE);

        case Nc4prototypes.NC_CHAR:
            return new ConvertedType(DataType.CHAR);

        case Nc4prototypes.NC_SHORT:
        case Nc4prototypes.NC_USHORT:
            return new ConvertedType(DataType.SHORT);

        case Nc4prototypes.NC_INT:
        case Nc4prototypes.NC_UINT:
            return new ConvertedType(DataType.INT);

        case Nc4prototypes.NC_INT64:
        case Nc4prototypes.NC_UINT64:
            return new ConvertedType(DataType.LONG);

        case Nc4prototypes.NC_FLOAT:
            return new ConvertedType(DataType.FLOAT);

        case Nc4prototypes.NC_DOUBLE:
            return new ConvertedType(DataType.DOUBLE);

        case Nc4prototypes.NC_ENUM:
            return new ConvertedType(DataType.ENUM1); // LOOK width ??

        case Nc4prototypes.NC_STRING:
            return new ConvertedType(DataType.STRING);

        default:
            UserType userType = userTypes.get(type);
            if(userType == null)
                throw new IllegalArgumentException("unknown type == " + type);

            switch (userType.typeClass) {
            case Nc4prototypes.NC_ENUM:
                return new ConvertedType(DataType.ENUM1);

            case Nc4prototypes.NC_COMPOUND:
                return new ConvertedType(DataType.STRUCTURE);

            case Nc4prototypes.NC_OPAQUE:
                return new ConvertedType(DataType.OPAQUE);

            case Nc4prototypes.NC_VLEN:
                ConvertedType result = convertDataType(userType.baseTypeid);
                result.isVlen = true;
                return result;
            }
            throw new IllegalArgumentException("unknown type == " + type);
        }
    }

    private String getDataTypeName(int type)
    {
        switch (type) {
        case Nc4prototypes.NC_BYTE:
            return "byte";
        case Nc4prototypes.NC_UBYTE:
            return "ubyte";
        case Nc4prototypes.NC_CHAR:
            return "char";
        case Nc4prototypes.NC_SHORT:
            return "short";
        case Nc4prototypes.NC_USHORT:
            return "ushort";
        case Nc4prototypes.NC_INT:
            return "int";
        case Nc4prototypes.NC_UINT:
            return "uint";
        case Nc4prototypes.NC_INT64:
            return "long";
        case Nc4prototypes.NC_UINT64:
            return "ulong";
        case Nc4prototypes.NC_FLOAT:
            return "float";
        case Nc4prototypes.NC_DOUBLE:
            return "double";
        case Nc4prototypes.NC_ENUM:
            return "enum";
        case Nc4prototypes.NC_STRING:
            return "string";
        case Nc4prototypes.NC_COMPOUND:
            return "struct";
        case Nc4prototypes.NC_OPAQUE:
            return "opaque";
        case Nc4prototypes.NC_VLEN:
            return "vlen";

        default:
            UserType userType = userTypes.get(type);
            if(userType == null)
                return "unknown type " + type;

            switch (userType.typeClass) {
            case Nc4prototypes.NC_ENUM:
                return "userType-enum";
            case Nc4prototypes.NC_COMPOUND:
                return "userType-struct";
            case Nc4prototypes.NC_OPAQUE:
                return "userType-opaque";
            case Nc4prototypes.NC_VLEN:
                return "userType-vlen";
            }
            return "unknown userType " + userType.typeClass;
        }
    }

    //////////////////////////////////////////////////////////////////////
    // writing data

    @Override
    public void create(String filename, NetcdfFile ncfile, int extra, long preallocateSize, boolean largeFile)
        throws IOException
    {
        load(); // load jni
        this.ncfile = ncfile;

        // finish any structures
        ncfile.finish();

        /*
       cmode    The creation mode flag. The following flags are available:
       NC_NOCLOBBER (do not overwrite existing file),
       NC_SHARE (limit write caching - netcdf classic files onlt),
       NC_64BIT_OFFSET (create 64-bit offset file),
       NC_NETCDF4 (create netCDF-4/HDF5 file),
       NC_CLASSIC_MODEL (enforce netCDF classic mode on netCDF-4/HDF5 files),
       NC_DISKLESS (store data only in memory),
       NC_MMAP (use MMAP for NC_DISKLESS), and
       NC_WRITE. See discussion below.
        */

        // create new file
        if(debug) System.out.println("open " + ncfile.getLocation());
        IntByReference ncidp = new IntByReference();
        int ret = nc4.nc_create(filename, createMode(), ncidp);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        ncid = ncidp.getValue();

        // turn off fill mode for the moment
        //IntByReference old_modep = new IntByReference();
        //ret = nc4.nc_set_fill(ncid, nc4.NC_NOFILL, old_modep);
        //        if(ret != 0)
        //throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        createGroup(ncid, new Group4(ncfile.getRootGroup(), null));

        // done with define mode
        nc4.nc_enddef(ncid);
    }

    /*
   netcdf4,             // jni netcdf4 iosp mode = NC_FORMAT_NETCDF4
   netcdf4_classic,     // jni netcdf4 iosp mode = NC_FORMAT_NETCDF4_CLASSIC
   netcdf3c,            // jni netcdf4 iosp mode = NC_FORMAT_CLASSIC
   netcdf3c64;           // jni netcdf4 iosp mode = NC_FORMAT_64BIT
    */

    private int createMode()
    {
        switch (version) {
        case netcdf4:
            return Nc4prototypes.NC_NETCDF4;
        case netcdf4_classic:
            return Nc4prototypes.NC_FORMAT_NETCDF4_CLASSIC;
        case netcdf3c:
            return Nc4prototypes.NC_FORMAT_CLASSIC;
        case netcdf3c64:
            return Nc4prototypes.NC_FORMAT_64BIT;
        }
        throw new IllegalStateException("version = " + version);
    }

    private void createGroup(int grpid, Group4 g4)
        throws IOException
    {
        groupHash.put(g4.g, grpid);
        g4.dimHash = new HashMap<Dimension, Integer>();

        // attributes
        for(Attribute att : g4.g.getAttributes())
            writeAttribute(grpid, Nc4prototypes.NC_GLOBAL, att, null);

        // dimensions
        for(Dimension dim : g4.g.getDimensions()) {
            int dimid = addDimension(grpid, dim.getShortName(), dim.getLength());
            g4.dimHash.put(dim, dimid);
            if(debugWrite)
                System.out.printf(" create dim '%s' (%d) in group '%s'%n", dim.getShortName(), dimid, g4.g.getFullName());
        }

        // types
        for(Variable v : g4.g.getVariables()) {
            switch (v.getDataType()) {
            case STRUCTURE:
                createCompoundType((Structure) v);
                break;
            }
        }

        // variables
        for(Variable v : g4.g.getVariables()) {
      switch (v.getDataType()) {
        case STRUCTURE:
          createCompoundVariable(grpid, g4, (Structure) v);
          break;
        default:
          createVariable(grpid, g4, v);
      }
        }

        // groups
        for(Group nested : g4.g.getGroups()) {
            IntByReference grpidp = new IntByReference();
            int ret = nc4.nc_def_grp(grpid, nested.getShortName(), grpidp);

            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            int nestedId = grpidp.getValue();
            createGroup(nestedId, new Group4(nested, g4));
        }
    }

    private void createCompoundType(Structure s)
    {

    }

    private void createVariable(int grpid, Group4 g4, Variable v) throws IOException {
      int[] dimids = new int[v.getRank()];
      int count = 0;
      for (Dimension d : v.getDimensions()) {
        int dimid;
        if (!d.isShared()) {
          dimid = addDimension(grpid, v.getShortName() + "_Dim" + count, d.getLength());
        } else {
          dimid = findDimensionId(g4, d);
        }
        if (debugWrite)
          System.out.printf("  use dim '%s' (%d) in variable '%s'%n", d.getShortName(), dimid, v.getShortName());
        dimids[count++] = dimid;
      }

      IntByReference varidp = new IntByReference();
      int typid = convertDataType(v.getDataType());
      if (typid < 0) return; // not implemented yet

      int ret = nc4.nc_def_var(grpid, v.getShortName(), typid, dimids.length, dimids, varidp);
      if (ret != 0)
        throw new IOException(nc4.nc_strerror(ret) + " on\n" + v);
      int varid = varidp.getValue();

      if (version == NetcdfFileWriter.Version.netcdf4) {
        //   int nc_def_var_chunking(int ncid, int varid, int storage, long[] chunksizesp); // const size_t *   ??
        boolean isChunked = chunker.isChunked(v);
        int storage = isChunked ? Nc4prototypes.NC_CHUNKED : Nc4prototypes.NC_CONTIGUOUS;
        long[] chunking = isChunked ? chunker.computeChunking(v) : new long[v.getRank()];
        ret = nc4.nc_def_var_chunking(grpid, varid, storage, chunking);
        if (ret != 0) {
          throw new IOException(nc4.nc_strerror(ret) + " nc_def_var_chunking on variable " + v.getFullName());
        }

        // int nc_def_var_deflate(int ncid, int varid, int shuffle, int deflate, int deflate_level);
        int deflateLevel = isChunked ? chunker.getDeflateLevel(v) : 0;
        int deflate = deflateLevel > 0 ? 1 : 0;
        int shuffle = isChunked && chunker.isShuffle(v) ? 1 : 0;
        if (deflateLevel > 0) {
          ret = nc4.nc_def_var_deflate(grpid, varid, shuffle, deflate, deflateLevel);
          if (ret != 0)
            throw new IOException(nc4.nc_strerror(ret));
        }
      }

      v.setSPobject(new Vinfo(grpid, varid, typid));

      for (Attribute att : v.getAttributes())
        writeAttribute(grpid, varid, att, v);
    }


    private void createCompoundVariable(int grpid, Group4 g4, Structure s) {

    }

    private Integer findDimensionId(Group4 g4, Dimension d)
    {
        if(g4 == null) return null;

        Integer dimid = g4.dimHash.get(d);
        if(dimid == null) {
            dimid = findDimensionId(g4.parent, d); // search in parent
        }

        return dimid;
    }

    private int addDimension(int grpid, String name, int length)
        throws IOException
    {
        IntByReference dimidp = new IntByReference();
        int ret = nc4.nc_def_dim(grpid, name, length, dimidp);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));
        return dimidp.getValue();
    }


    private void writeAttribute(int grpid, int varid, Attribute att, Variable v)
        throws IOException
    {
        if(v != null && att.getShortName().equals(CDM.FILL_VALUE)) {
            if(att.getLength() != 1) {
                log.warn("_FillValue length must be one on var = " + v.getFullName());
                return;
            }
            if(att.getDataType() != v.getDataType()) {
                log.warn("_FillValue type must agree with var = " + v.getFullName() + " type " + att.getDataType() + "!=" + v.getDataType());
                return;
            }
        }

        // dont propagate these - handles internally
        if(att.getShortName().equals(H5header.HDF5_CLASS)) return;
        if(att.getShortName().equals(H5header.HDF5_DIMENSION_LIST)) return;
        if(att.getShortName().equals(H5header.HDF5_DIMENSION_SCALE)) return;
        if(att.getShortName().equals(H5header.HDF5_DIMENSION_LABELS)) return;

        int ret = 0;
        Array values = att.getValues();
        switch (att.getDataType()) {
        case STRING: // problem may be that we are mapping char * atts to string type
            if(att.getLength() == 1 && !att.getShortName().equals(CDM.FILL_VALUE)) {
                byte[] svalb = att.getStringValue().getBytes(CDM.utf8Charset);
                ret = nc4.nc_put_att_text(grpid, varid, att.getShortName(), svalb.length, svalb);
            } else {
                String[] svalues = new String[att.getLength()];
                for(int i = 0;i < att.getLength();i++) svalues[i] = (String) att.getValue(i);
                ret = nc4.nc_put_att_string(grpid, varid, att.getShortName(), att.getLength(), svalues);
            }
            break;
        case BYTE:
            ret = nc4.nc_put_att_schar(grpid, varid, att.getShortName(), Nc4prototypes.NC_BYTE, att.getLength(), (byte[]) values.getStorage());
            break;
        case CHAR:
            ret = nc4.nc_put_att_text(grpid, varid, att.getShortName(), att.getLength(), IospHelper.convertCharToByte((char[]) values.getStorage()));
            break;
        case DOUBLE:
            ret = nc4.nc_put_att_double(grpid, varid, att.getShortName(), Nc4prototypes.NC_DOUBLE, att.getLength(), (double[]) values.getStorage());
            break;
        case FLOAT:
            ret = nc4.nc_put_att_float(grpid, varid, att.getShortName(), Nc4prototypes.NC_FLOAT, att.getLength(), (float[]) values.getStorage());
            break;
        case INT:
            ret = nc4.nc_put_att_int(grpid, varid, att.getShortName(), Nc4prototypes.NC_INT, att.getLength(), (int[]) values.getStorage());
            break;
        case LONG:
            ret = nc4.nc_put_att_longlong(grpid, varid, att.getShortName(), Nc4prototypes.NC_INT64, att.getLength(), (long[]) values.getStorage());
            break;
        case SHORT:
            ret = nc4.nc_put_att_short(grpid, varid, att.getShortName(), Nc4prototypes.NC_SHORT, att.getLength(), (short[]) values.getStorage());
            break;
        }

        if(ret != 0) {
            String where = v != null ? "var " + v.getFullName() : "global or group attribute";
            throw new IOException(ret + " (" + nc4.nc_strerror(ret) + ") on attribute '" + att + "' on " + where);
        }
    }

    @Override
    public void writeData(Variable v2, Section section, Array values)
        throws IOException
        , InvalidRangeException
    {
        Vinfo vinfo = (Vinfo) v2.getSPobject();
        if(vinfo == null) {
            log.error("HEY vinfo null for " + v2);
            return;
        }
        int vlen = (int) v2.getSize();
        int len = (int) section.computeSize();
        if(vlen == len) // entire array
            writeDataAll(v2, vinfo.grpid, vinfo.varid, vinfo.typeid, values);
        else
            writeData(v2, vinfo.grpid, vinfo.varid, vinfo.typeid, section, values);
    }

    private void writeData(Variable v, int grpid, int varid, int typeid, Section section, Array values)
        throws IOException
        , InvalidRangeException
    {

        // general sectioning with strides
        long[] origin = convert(section.getOrigin());
        long[] shape = convert(section.getShape());
        long[] stride = convert(section.getStride());
        boolean isUnsigned = isUnsigned(typeid);
        int sectionLen = (int) section.computeSize();

        Object data = values.getStorage();

        switch (typeid) {

        case Nc4prototypes.NC_BYTE:
        case Nc4prototypes.NC_UBYTE:
            byte[] valb = (byte[]) data;
            assert valb.length == sectionLen;
            int ret = isUnsigned ? nc4.nc_put_vars_uchar(grpid, varid, origin, shape, stride, valb) :
                nc4.nc_put_vars_schar(grpid, varid, origin, shape, stride, valb);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            break;

        case Nc4prototypes.NC_CHAR:
            char[] valc = (char[]) data;   // chars are lame
            assert valc.length == sectionLen;

            valb = IospHelper.convertCharToByte(valc);
            ret = nc4.nc_put_vars_text(grpid, varid, origin, shape, stride, valb);
            if(ret != 0) {
                log.error("{} on var {}", nc4.nc_strerror(ret), v);
                return;
                //throw new IOException(nc4.nc_strerror(ret));
            }
            break;

        case Nc4prototypes.NC_DOUBLE:
            double[] vald = (double[]) data;
            assert vald.length == sectionLen;
            ret = nc4.nc_put_vars_double(grpid, varid, origin, shape, stride, vald);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            break;

        case Nc4prototypes.NC_FLOAT:
            float[] valf = (float[]) data;
            assert valf.length == sectionLen;
            ret = nc4.nc_put_vars_float(grpid, varid, origin, shape, stride, valf);
            if(ret != 0) {
                log.error("{} on var {}", nc4.nc_strerror(ret), v);
                return;
                //throw new IOException(nc4.nc_strerror(ret));
            }
            break;

        case Nc4prototypes.NC_INT:
            int[] vali = (int[]) data;
            assert vali.length == sectionLen;
            ret = isUnsigned ? nc4.nc_put_vars_uint(grpid, varid, origin, shape, stride, vali) :
                nc4.nc_put_vars_int(grpid, varid, origin, shape, stride, vali);
            if(ret != 0) {
                log.error("{} on var {}", nc4.nc_strerror(ret), v);
                return;
                //throw new IOException(nc4.nc_strerror(ret));
            }
            break;

        case Nc4prototypes.NC_INT64:
            long[] vall = (long[]) data;
            assert vall.length == sectionLen;
            ret = isUnsigned ? nc4.nc_put_vars_ulonglong(grpid, varid, origin, shape, stride, vall) :
                nc4.nc_put_vars_longlong(grpid, varid, origin, shape, stride, vall);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            break;

        case Nc4prototypes.NC_SHORT:
            short[] vals = (short[]) data;
            assert vals.length == sectionLen;
            ret = isUnsigned ? nc4.nc_put_vars_ushort(grpid, varid, origin, shape, stride, vals) :
                nc4.nc_put_vars_short(grpid, varid, origin, shape, stride, vals);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            break;

        case Nc4prototypes.NC_STRING:
            String[] valss = convertStringData(data);
            assert valss.length == sectionLen;
            ret = nc4.nc_put_vars_string(grpid, varid, origin, shape, stride, valss);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            break;

        default:
            UserType userType = userTypes.get(typeid);
            if(userType == null) {
                throw new IOException("Unknown userType == " + typeid);

            } else if(userType.typeClass == Nc4prototypes.NC_ENUM) {
                //return readDataSection(grpid, varid, userType.baseTypeid, section);

            } else if(userType.typeClass == Nc4prototypes.NC_VLEN) { // cannot subset
                //return readVlen(grpid, varid, len, userType);

            } else if(userType.typeClass == Nc4prototypes.NC_OPAQUE) {
                //return readOpaque(grpid, varid, section, userType.size);

            } else if(userType.typeClass == Nc4prototypes.NC_COMPOUND) {
                //return readCompound(grpid, varid, section, userType);
            }

            throw new IOException("Unsupported userType = " + typeid + " userType= " + userType);
        }
        // System.out.printf("OK var %s%n", v);

    }

    private void writeDataAll(Variable v, int grpid, int varid, int typeid, Array values)
        throws IOException
        , InvalidRangeException
    {

        Object data = values.getStorage();
        boolean isUnsigned = isUnsigned(typeid);

        switch (typeid) {

        case Nc4prototypes.NC_BYTE:
        case Nc4prototypes.NC_UBYTE:
            byte[] valb = (byte[]) data;
            int ret = isUnsigned ? nc4.nc_put_var_uchar(grpid, varid, valb) :
                nc4.nc_put_var_schar(grpid, varid, valb);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            break;

        case Nc4prototypes.NC_CHAR:
            char[] valc = (char[]) data;   // chars are lame
            valb = IospHelper.convertCharToByte(valc);
            ret = nc4.nc_put_var_text(grpid, varid, valb);
            if(ret != 0) {
                log.error("{} on var {}", nc4.nc_strerror(ret), v);
                return;
                //throw new IOException(nc4.nc_strerror(ret));
            }
            break;

        case Nc4prototypes.NC_DOUBLE:
            double[] vald = (double[]) data;
            ret = nc4.nc_put_var_double(grpid, varid, vald);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            break;

        case Nc4prototypes.NC_FLOAT:
            float[] valf = (float[]) data;
            ret = nc4.nc_put_var_float(grpid, varid, valf);
            if(ret != 0) {
                log.error("{} on var {}", nc4.nc_strerror(ret), v);
                return;
                //throw new IOException(nc4.nc_strerror(ret));
            }
            break;

        case Nc4prototypes.NC_INT:
            int[] vali = (int[]) data;
            ret = isUnsigned ? nc4.nc_put_var_uint(grpid, varid, vali) :
                nc4.nc_put_var_int(grpid, varid, vali);
            if(ret != 0) {
                log.error("{} on var {}", nc4.nc_strerror(ret), v);
                return;
                //throw new IOException(nc4.nc_strerror(ret));
            }
            break;

        case Nc4prototypes.NC_INT64:
            long[] vall = (long[]) data;
            ret = isUnsigned ? nc4.nc_put_var_ulonglong(grpid, varid, vall) :
                nc4.nc_put_var_longlong(grpid, varid, vall);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            break;

        case Nc4prototypes.NC_SHORT:
            short[] vals = (short[]) data;
            ret = isUnsigned ? nc4.nc_put_var_ushort(grpid, varid, vals) :
                nc4.nc_put_var_short(grpid, varid, vals);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            break;

        case Nc4prototypes.NC_STRING:
            String[] valss = convertStringData(data);
            ret = nc4.nc_put_var_string(grpid, varid, valss);
            if(ret != 0)
                throw new IOException(ret + ": " + nc4.nc_strerror(ret));
            break;

        default:
            UserType userType = userTypes.get(typeid);
            if(userType == null) {
                throw new IOException("Unknown userType == " + typeid);

            } else if(userType.typeClass == Nc4prototypes.NC_ENUM) {
                //return readDataSection(grpid, varid, userType.baseTypeid, section);

            } else if(userType.typeClass == Nc4prototypes.NC_VLEN) { // cannot subset
                //return readVlen(grpid, varid, len, userType);

            } else if(userType.typeClass == Nc4prototypes.NC_OPAQUE) {
                //return readOpaque(grpid, varid, section, userType.size);

            } else if(userType.typeClass == Nc4prototypes.NC_COMPOUND) {
                //return readCompound(grpid, varid, section, userType);
            }

            throw new IOException("Unsupported userType = " + typeid + " userType= " + userType);
        }
        // System.out.printf("OK var %s%n", v);

    }

    private String[] convertStringData(Object org)
        throws IOException
    {
        if(org instanceof String[]) return (String[]) org;
        if(org instanceof Object[]) {
            Object[] oo = (Object[]) org;
            String[] result = new String[oo.length];
            int count = 0;
            for(Object s : oo)
                result[count++] = (String) s;
            return result;
        }
        throw new IOException("convertStringData failed on class = " + org.getClass().getName());
    }

    /////////////////////////////////////////////////////////////////////////


    @Override
    public void flush()
        throws IOException
    {
        int ret = nc4.nc_sync(ncid);
        if(ret != 0)
            throw new IOException(ret + ": " + nc4.nc_strerror(ret));

        // reread dimension in     case unlimited has grown
        updateDimensions(ncfile.getRootGroup());
    }

    @Override
    public void setFill(boolean fill)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public boolean rewriteHeader(boolean largeFile)
        throws IOException
    {
        return false;  // LOOK !!
    }

    @Override
    public void updateAttribute(Variable v2, Attribute att)
        throws IOException
    {
        if(v2 == null)
            writeAttribute(ncid, Nc4prototypes.NC_GLOBAL, att, null);
        else {
            Vinfo vinfo = (Vinfo) v2.getSPobject();
            writeAttribute(vinfo.grpid, vinfo.varid, att, v2);
        }
    }

/////////////////////////////////////////////////////////////////////////

private static class MyNetcdfFile extends NetcdfFile
{
    MyNetcdfFile(IOServiceProvider spi)
    {
        this.spi = spi;
    }

}

    public NetcdfFile open(String location)
        throws Exception
    {
        MyNetcdfFile ncfile = new MyNetcdfFile(this);
        ncfile.setLocation(location);
        try {
            open(null, ncfile, null);
        } catch (Exception e) {
            close(); // make sure that the file gets closed
            throw e;
        }
        return ncfile;
    }

    public static void main(String args[])
        throws Exception
    {
        Nc4Iosp iosp = new Nc4Iosp(NetcdfFileWriter.Version.netcdf4);

        String loc4 = "Q:/cdmUnitTest/formats/netcdf4/testNestedStructure.nc";
        NetcdfFile ncfile = iosp.open(loc4);
        System.out.println("" + ncfile);
    }

}
    
