/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.jni.netcdf;

import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;

/**
 * IOSP for reading netcdf files through jni interface to netcdf4 library
 *
 * @author caron
 * @since Oct 30, 2008
 */
public class JniIosp extends AbstractIOServiceProvider {
  private static boolean debug = false;

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    return false;
  }

  private int ncid;

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    load();
    if (debug) System.out.println("open " + ncfile.getLocation());
    IntByReference ncidp = new IntByReference();
    int ret = nc4.nc_open(ncfile.getLocation(), 0, ncidp);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));

    Group root = ncfile.getRootGroup();
    ncid = ncidp.getValue();
    readDimensions(ncfile, root);

    IntByReference ngattsp = new IntByReference();
    ret = nc4.nc_inq_natts(ncid, ngattsp);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
    List<Attribute> gatts = readAttributes(ngattsp.getValue(), NCLibrary.NC_GLOBAL);
    for (Attribute att : gatts) {
      ncfile.addAttribute(root, att);
      if (debug) System.out.printf(" add Global Attribute %s %n", att);
    }

    readVariables(ncfile, root);

    ncfile.finish();
  }

  private void readDimensions(NetcdfFile ncfile, Group g) throws IOException {
    IntByReference ndimsp = new IntByReference();
    int ret = nc4.nc_inq_ndims(ncid, ndimsp);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));

    IntByReference nunlimdimsp = new IntByReference();
    int[] unlimdimids = new int[NCLibrary.NC_MAX_DIMS];
    ret = nc4.nc_inq_unlimdims(ncid, nunlimdimsp, unlimdimids);

    int ndims = ndimsp.getValue();
    for (int i = 0; i < ndims; i++) {
      byte[] name = new byte[NCLibrary.NC_MAX_NAME + 1];
      NativeLongByReference lenp = new NativeLongByReference();
      ret = nc4.nc_inq_dim(ncid, i, name, lenp);
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
      String dname = makeString(name);

      boolean isUnlimited = containsInt(nunlimdimsp.getValue(), unlimdimids, i);
      Dimension dim = new Dimension(dname, lenp.getValue().intValue(), true, isUnlimited, false);
      ncfile.addDimension(g, dim);
      if (debug) System.out.printf(" add Dimension %s %n", dim);
    }
  }

  private boolean containsInt(int n, int[] have, int want) {
    for (int i = 0; i < n; i++) {
      if (have[i] == want) return true;
    }
    return false;
  }

  private String makeString(byte[] b) throws IOException {
    // null terminates
    int count = 0;
    while (count < b.length - 1) {
      if (b[count] == 0) break;
      count++; // dont include the terminating 0
    }

    // copy if its small
    if (count < b.length / 2) {
      byte[] bb = new byte[count];
      System.arraycopy(b, 0, bb, 0, count);
      b = bb;
    }

    return new String(b, 0, count, "UTF-8"); // all strings are considered to be UTF-8 unicode.
  }


  private List<Attribute> readAttributes(int natts, int varid) throws IOException {
    List<Attribute> result = new ArrayList<Attribute>(natts);

    for (int attnum = 0; attnum < natts; attnum++) {
      byte[] name = new byte[NCLibrary.NC_MAX_NAME + 1];
      int ret = nc4.nc_inq_attname(ncid, varid, attnum, name);
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
      String attname = makeString(name);

      IntByReference xtypep = new IntByReference();
      ret = nc4.nc_inq_atttype(ncid, varid, attname, xtypep);
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));

      NativeLongByReference lenp = new NativeLongByReference();
      ret = nc4.nc_inq_attlen(ncid, varid, attname, lenp);
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
      int len = lenp.getValue().intValue();

      Array values = null;
      switch (xtypep.getValue()) {
        case NCLibrary.NC_BYTE:
          byte[] valb = new byte[len];
          ret = nc4.nc_get_att_schar(ncid, varid, attname, valb);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[]{len}, valb);
          break;

        case NCLibrary.NC_CHAR:
          byte[] text = new byte[len];
          ret = nc4.nc_get_att_text(ncid, varid, attname, text);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          Attribute att = new Attribute(attname, makeString(text));
          result.add(att);
          break;

        case NCLibrary.NC_DOUBLE:
          double[] vald = new double[len];
          ret = nc4.nc_get_att_double(ncid, varid, attname, vald);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[]{len}, vald);
          break;

        case NCLibrary.NC_FLOAT:
          float[] valf = new float[len];
          ret = nc4.nc_get_att_float(ncid, varid, attname, valf);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.FLOAT.getPrimitiveClassType(), new int[]{len}, valf);
          break;

        case NCLibrary.NC_INT:
          int[] vali = new int[len];
          ret = nc4.nc_get_att_int(ncid, varid, attname, vali);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.INT.getPrimitiveClassType(), new int[]{len}, vali);
          break;

        case NCLibrary.NC_INT64:
          long[] vall = new long[len];
          ret = nc4.nc_get_att_longlong(ncid, varid, attname, vall);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.LONG.getPrimitiveClassType(), new int[]{len}, vall);
          break;

        case NCLibrary.NC_SHORT:
          short[] vals = new short[len];
          ret = nc4.nc_get_att_short(ncid, varid, attname, vals);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.SHORT.getPrimitiveClassType(), new int[]{len}, vals);
          break;
      }

      if (values != null) {
        Attribute att = new Attribute(attname, values);
        result.add(att);
      }
    }

    return result;
  }

  private void readVariables(NetcdfFile ncfile, Group g) throws IOException {
    IntByReference nvarsp = new IntByReference();
    int ret = nc4.nc_inq_nvars(ncid, nvarsp);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
    int nvars = nvarsp.getValue();

    for (int varno = 0; varno < nvars; varno++) {
      byte[] name = new byte[NCLibrary.NC_MAX_NAME + 1];
      IntByReference xtypep = new IntByReference();
      IntByReference ndimsp = new IntByReference();
      int[] dimids = new int[NCLibrary.NC_MAX_DIMS];
      IntByReference nattsp = new IntByReference();

      ret = nc4.nc_inq_var(ncid, varno, name, xtypep, ndimsp, dimids, nattsp);
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));

      String vname = makeString(name);
      Variable v = new Variable(ncfile, g, null, vname, getDataType(xtypep.getValue()),
              makeDimList(ndimsp.getValue(), dimids));
      ncfile.addVariable(g, v);
      v.setSPobject(new Vinfo(varno));

      List<Attribute> gatts = readAttributes(nattsp.getValue(), varno);
      for (Attribute att : gatts) {
        v.addAttribute(att);
        //if (debug) System.out.printf(" add Variable Attribute %s %n",att);
      }

      if (debug) System.out.printf(" add Variable %s %n", v);
    }
  }

  private String makeDimList(int ndimsp, int[] dims) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ndimsp; i++) {
      byte[] name = new byte[NCLibrary.NC_MAX_NAME + 1];
      int ret = nc4.nc_inq_dimname(ncid, dims[i], name);
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
      String dname = makeString(name);
      sb.append(dname);
      sb.append(" ");
    }
    return sb.toString();
  }

  private class Vinfo {
    int varid;
    Vinfo(int varid) {
      this.varid = varid;
    }
  }

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Vinfo vinfo = (Vinfo) v2.getSPobject();
    Array values = null;
    int vlen = (int) v2.getSize();
    int len = (int) section.computeSize();
    if (vlen == len) return read(v2); // entire array

    switch (v2.getDataType()) {
      case BYTE:
        byte[] valb = new byte[len];
        // int nc_get_vars_schar(int ncid, int varid, int[] startp, int[] countp, int[] stridep, byte[] ip); // size_t, ptrdiff_t
        int ret = nc4.nc_get_vars_schar(ncid, vinfo.varid,
                convert(section.getOrigin()), convert(section.getShape()), section.getStride(), valb);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.BYTE.getPrimitiveClassType(), section.getShape(), valb);
        break;

      case CHAR:
        byte[] valc = new byte[len];
        ret = nc4.nc_get_var_text(ncid, vinfo.varid, valc);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.BYTE.getPrimitiveClassType(), section.getShape(), valc);
        break;

      case DOUBLE:
        double[] vald = new double[len];
        ret = nc4.nc_get_var_double(ncid, vinfo.varid, vald);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), section.getShape(), vald);
        break;

      case FLOAT:
        float[] valf = new float[len];
        ret = nc4.nc_get_var_float(ncid, vinfo.varid, valf);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.FLOAT.getPrimitiveClassType(), section.getShape(), valf);
        break;

      case INT:
        int[] vali = new int[len];
        ret = nc4.nc_get_var_int(ncid, vinfo.varid, vali);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.INT.getPrimitiveClassType(), section.getShape(), vali);
        break;

      case LONG:
        long[] vall = new long[len];
        ret = nc4.nc_get_var_longlong(ncid, vinfo.varid, vall);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.LONG.getPrimitiveClassType(), section.getShape(), vall);
        break;

      case SHORT:
        short[] vals = new short[len];
        ret = nc4.nc_get_var_short(ncid, vinfo.varid, vals);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.SHORT.getPrimitiveClassType(), section.getShape(), vals);
        break;

      default:
        throw new IOException("Unsupported data type = "+v2.getDataType());
    }

    return values;
  }

  private long[] convert(int [] from) {
    long[] to = new long[from.length];
    for (int i=0; i<from.length; i++)
      to[i] = from[i];
    return to;
  }

  private Array read(Variable v2) throws IOException, InvalidRangeException {
    Vinfo vinfo = (Vinfo) v2.getSPobject();
    Array values = null;
    int len = (int) v2.getSize();

    switch (v2.getDataType()) {
      case BYTE:
        byte[] valb = new byte[len];
        int ret = nc4.nc_get_var_schar(ncid, vinfo.varid, valb);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.BYTE.getPrimitiveClassType(), v2.getShape(), valb);
        break;

      case CHAR:
        byte[] valc = new byte[len];
        ret = nc4.nc_get_var_text(ncid, vinfo.varid, valc);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.BYTE.getPrimitiveClassType(), v2.getShape(), valc);
        break;

      case DOUBLE:
        double[] vald = new double[len];
        ret = nc4.nc_get_var_double(ncid, vinfo.varid, vald);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), v2.getShape(), vald);
        break;

      case FLOAT:
        float[] valf = new float[len];
        ret = nc4.nc_get_var_float(ncid, vinfo.varid, valf);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.FLOAT.getPrimitiveClassType(), v2.getShape(), valf);
        break;

      case INT:
        int[] vali = new int[len];
        ret = nc4.nc_get_var_int(ncid, vinfo.varid, vali);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.INT.getPrimitiveClassType(), v2.getShape(), vali);
        break;

      case LONG:
        long[] vall = new long[len];
        ret = nc4.nc_get_var_longlong(ncid, vinfo.varid, vall);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.LONG.getPrimitiveClassType(), v2.getShape(), vall);
        break;

      case SHORT:
        short[] vals = new short[len];
        ret = nc4.nc_get_var_short(ncid, vinfo.varid, vals);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.SHORT.getPrimitiveClassType(), v2.getShape(), vals);
        break;

      default:
        throw new IOException("Unsupported data type = "+v2.getDataType());
    }

    return values;
  }

  public void close() throws IOException {
    int ret = nc4.nc_close(ncid);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
  }

  private DataType getDataType(int type) {
    switch (type) {
      case NCLibrary.NC_BYTE:
      case NCLibrary.NC_UBYTE:
        return DataType.BYTE;

      case NCLibrary.NC_CHAR:
        return DataType.CHAR;

      case NCLibrary.NC_SHORT:
      case NCLibrary.NC_USHORT:
        return DataType.SHORT;

      case NCLibrary.NC_INT:
      case NCLibrary.NC_UINT:
        return DataType.INT;

      case NCLibrary.NC_INT64:
      case NCLibrary.NC_UINT64:
        return DataType.LONG;

      case NCLibrary.NC_FLOAT:
        return DataType.FLOAT;

      case NCLibrary.NC_DOUBLE:
        return DataType.DOUBLE;
    }
    throw new IllegalArgumentException("unknown type == " + type);
  }

  private static NCLibrary nc4;

  private NCLibrary load() {
    if (nc4 == null) {
      String dir = "C:/dev/tds/thredds/lib/binary/win32/";
      //System.setProperty("jna.library.path", "C:/cdev/install/bin2");
      System.setProperty("jna.library.path", dir);

      System.load(dir + "zlib1.dll");
      System.load(dir + "szlibdll.dll");
      System.load(dir + "hdf5dll.dll");

      nc4 = (NCLibrary) Native.loadLibrary("netcdf", NCLibrary.class);
    }

    return nc4;
  }

  private static class MyNetcdfFile extends NetcdfFile {
    MyNetcdfFile(IOServiceProvider spi) {
      this.spi = spi;
    }
  }

  public NetcdfFile open(String location) throws IOException {
    MyNetcdfFile ncfile = new MyNetcdfFile(this);
    ncfile.setLocation(location);
    open(null, ncfile, null);
    return ncfile;
  }

  public static void main(String args[]) throws IOException {
    JniIosp iosp = new JniIosp();
    iosp.open("D:/data/test/foo.nc");
  }

}
