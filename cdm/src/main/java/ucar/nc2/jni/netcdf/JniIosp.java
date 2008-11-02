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
import ucar.nc2.iosp.IospHelper;
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
import java.util.Map;
import java.util.HashMap;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
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

  private NetcdfFile ncfile;
  private int ncid, format;
  private boolean isClosed = false;

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    load();
    this.ncfile = ncfile;

    if (debug) System.out.println("open " + ncfile.getLocation());
    IntByReference ncidp = new IntByReference();
    int ret = nc4.nc_open(ncfile.getLocation(), 0, ncidp);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
    ncid = ncidp.getValue();

    IntByReference formatp = new IntByReference();
    ret = nc4.nc_inq_format(ncid, formatp);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
    format = formatp.getValue();
    System.out.printf("open %s id=%d format=%d %n", ncfile.getLocation(), ncid, format);

    // apparently ncid acts as the root group id
    readGroup(ncid, ncfile.getRootGroup());

    ncfile.finish();
  }

  private void readGroup( int grpid, Group g) throws IOException {
    readDimensions(grpid, g);

    IntByReference ngattsp = new IntByReference();
    int ret = nc4.nc_inq_natts(grpid, ngattsp);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
    List<Attribute> gatts = readAttributes(grpid, NCLibrary.NC_GLOBAL, ngattsp.getValue());
    for (Attribute att : gatts) {
      ncfile.addAttribute(g, att);
      if (debug) System.out.printf(" add Global Attribute %s %n", att);
    }

    readTypes(grpid, g);
    readVariables(grpid, g);

    if (format == NCLibrary.NC_FORMAT_NETCDF4) {       
      // read subordinate groups
      IntByReference numgrps = new IntByReference();
      ret = nc4.nc_inq_grps(grpid, numgrps, Pointer.NULL);
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
      int[] grids = new int[numgrps.getValue()];
      ret = nc4.nc_inq_grps(grpid, numgrps, grids);
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));

      for (int i=0; i<grids.length; i++) {
        byte[] name = new byte[NCLibrary.NC_MAX_NAME + 1];
        ret = nc4.nc_inq_grpname(grids[i], name);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        Group child = new Group(ncfile, g, makeString(name));
        g.addGroup(child);
        readGroup(grids[i], child);
      }
    }

  }

  private void readTypes(int grpid, Group g) throws IOException {
    IntByReference ntypesp = new IntByReference();
    int ret = nc4.nc_inq_typeids(grpid, ntypesp, Pointer.NULL);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
    int ntypes = ntypesp.getValue();
    if (ntypes == 0) return;
    int[] xtypes = new int[ntypes];
    ret = nc4.nc_inq_typeids(grpid, ntypesp, xtypes);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));

    for (int i=0; i<ntypes; i++) {
      byte[] nameb = new byte[NCLibrary.NC_MAX_NAME + 1];
      NativeLongByReference sizep = new NativeLongByReference();
      IntByReference baseType = new IntByReference();
      NativeLongByReference nfieldsp = new NativeLongByReference();
      IntByReference classp = new IntByReference();
      //ret = nc4.nc_inq_type(grpid, xtypes[i], nameb, sizep);
      ret =  nc4.nc_inq_user_type(grpid, xtypes[i], nameb, sizep, baseType, nfieldsp, classp); // size_t
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));

      String name = makeString(nameb);
      int utype = classp.getValue();
      System.out.printf(" type=%d name=%s size=%d baseType=%d nfields=%d class=%d %n ",
          xtypes[i], name, sizep.getValue().longValue(), baseType.getValue(), nfieldsp.getValue().longValue(), utype);
      if (utype == NCLibrary.NC_ENUM) {
        Map<Integer, String> map = readEnums(grpid, xtypes[i]);
        EnumTypedef e = new EnumTypedef(name, map);
        g.addEnumeration(e);
      }
    }
  }

  private Map<Integer, String> readEnums(int grpid, int xtype) throws IOException {
    byte[] nameb = new byte[NCLibrary.NC_MAX_NAME + 1];
    IntByReference baseType = new IntByReference();
    NativeLongByReference baseSize = new NativeLongByReference();
    NativeLongByReference numMembers = new NativeLongByReference();

    int ret = nc4.nc_inq_enum(grpid, xtype, nameb, baseType, baseSize, numMembers);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
    int nmembers = numMembers.getValue().intValue();
    String name = makeString(nameb);

    //System.out.printf(" type=%d name=%s baseType=%d baseType=%d numMembers=%d %n ",
    //    xtype, name, baseType.getValue(), baseSize.getValue().longValue(), nmembers);
    Map<Integer, String> map = new HashMap<Integer, String>( 2*nmembers);

    for (int i=0; i<nmembers; i++) {
      byte[] mnameb = new byte[NCLibrary.NC_MAX_NAME + 1];
      IntByReference value = new IntByReference();
      ret = nc4.nc_inq_enum_member(grpid, xtype, i, mnameb, value); // void *
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));

      String mname = makeString(mnameb);
      //System.out.printf(" member name=%s value=%d %n ",  mname, value.getValue());
      map.put(value.getValue(), name);
    }

    return map;
  }

  private void readDimensions(int grpid, Group g) throws IOException {
    IntByReference ndimsp = new IntByReference();
    int ret = nc4.nc_inq_ndims(grpid, ndimsp);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));

    IntByReference nunlimdimsp = new IntByReference();
    int[] unlimdimids = new int[NCLibrary.NC_MAX_DIMS];
    ret = nc4.nc_inq_unlimdims(grpid, nunlimdimsp, unlimdimids);

    int ndims = ndimsp.getValue();
    for (int i = 0; i < ndims; i++) {
      byte[] name = new byte[NCLibrary.NC_MAX_NAME + 1];
      NativeLongByReference lenp = new NativeLongByReference();
      ret = nc4.nc_inq_dim(grpid, i, name, lenp);
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
    while (count < b.length) {
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


  private List<Attribute> readAttributes(int grpid, int varid, int natts ) throws IOException {
    List<Attribute> result = new ArrayList<Attribute>(natts);

    for (int attnum = 0; attnum < natts; attnum++) {
      byte[] name = new byte[NCLibrary.NC_MAX_NAME + 1];
      int ret = nc4.nc_inq_attname(grpid, varid, attnum, name);
      if (ret != 0)
        throw new IOException(nc4.nc_strerror(ret)+ " varid="+varid+" attnum="+attnum);
      String attname = makeString(name);

      IntByReference xtypep = new IntByReference();
      ret = nc4.nc_inq_atttype(grpid, varid, attname, xtypep);
      if (ret != 0)
        throw new IOException(nc4.nc_strerror(ret)+ " varid="+varid+"attnum="+attnum);

      NativeLongByReference lenp = new NativeLongByReference();
      ret = nc4.nc_inq_attlen(grpid, varid, attname, lenp);
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
      int len = lenp.getValue().intValue();

      Array values = null;
      switch (xtypep.getValue()) {
        case NCLibrary.NC_UBYTE:
          byte[] valbu = new byte[len];
          ret = nc4.nc_get_att_uchar(grpid, varid, attname, valbu);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[]{len}, valbu);
          break;

        case NCLibrary.NC_BYTE:
          byte[] valb = new byte[len];
          ret = nc4.nc_get_att_schar(grpid, varid, attname, valb);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[]{len}, valb);
          break;

        case NCLibrary.NC_CHAR:
          byte[] text = new byte[len];
          ret = nc4.nc_get_att_text(grpid, varid, attname, text);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          Attribute att = new Attribute(attname, makeString(text));
          result.add(att);
          break;

        case NCLibrary.NC_DOUBLE:
          double[] vald = new double[len];
          ret = nc4.nc_get_att_double(grpid, varid, attname, vald);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[]{len}, vald);
          break;

        case NCLibrary.NC_FLOAT:
          float[] valf = new float[len];
          ret = nc4.nc_get_att_float(grpid, varid, attname, valf);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.FLOAT.getPrimitiveClassType(), new int[]{len}, valf);
          break;

        case NCLibrary.NC_UINT:
          int[] valiu = new int[len];
          ret = nc4.nc_get_att_uint(grpid, varid, attname, valiu);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.INT.getPrimitiveClassType(), new int[]{len}, valiu);
          break;

        case NCLibrary.NC_INT:
          int[] vali = new int[len];
          ret = nc4.nc_get_att_int(grpid, varid, attname, vali);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.INT.getPrimitiveClassType(), new int[]{len}, vali);
          break;

        case NCLibrary.NC_UINT64:
          long[] vallu = new long[len];
          ret = nc4.nc_get_att_ulonglong(grpid, varid, attname, vallu);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.LONG.getPrimitiveClassType(), new int[]{len}, vallu);
          break;

        case NCLibrary.NC_INT64:
          long[] vall = new long[len];
          ret = nc4.nc_get_att_longlong(grpid, varid, attname, vall);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.LONG.getPrimitiveClassType(), new int[]{len}, vall);
          break;

        case NCLibrary.NC_USHORT:
          short[] valsu = new short[len];
          ret = nc4.nc_get_att_ushort(grpid, varid, attname, valsu);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.SHORT.getPrimitiveClassType(), new int[]{len}, valsu);
          break;

        case NCLibrary.NC_SHORT:
          short[] vals = new short[len];
          ret = nc4.nc_get_att_short(grpid, varid, attname, vals);
          if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
          values = Array.factory(DataType.SHORT.getPrimitiveClassType(), new int[]{len}, vals);
          break;

      default:
        //throw new IOException("Unsupported attribute data type = "+xtypep.getValue());
        System.out.println("Unsupported attribute data type = "+xtypep.getValue());
      }

      if (values != null) {
        Attribute att = new Attribute(attname, values);
        result.add(att);
      }
    }

    return result;
  }

  private void readVariables(int grpid, Group g) throws IOException {
    IntByReference nvarsp = new IntByReference();
    int ret = nc4.nc_inq_nvars(grpid, nvarsp);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
    int nvars = nvarsp.getValue();
    if (debug) System.out.printf(" nvars= %d %n", nvars);

    int[] varids = new int[nvars];
    ret =  nc4.nc_inq_varids(grpid, nvarsp, varids);

    for (int i = 0; i < varids.length; i++) {
      int varno = varids[i];
      if (varno != i) System.out.printf("HEY varno=%d i=%d%n",varno,i);

      byte[] name = new byte[NCLibrary.NC_MAX_NAME + 1];
      IntByReference xtypep = new IntByReference();
      IntByReference ndimsp = new IntByReference();
      int[] dimids = new int[NCLibrary.NC_MAX_DIMS];
      IntByReference nattsp = new IntByReference();

      ret = nc4.nc_inq_var(grpid, varno, name, xtypep, ndimsp, dimids, nattsp);
      if (ret != 0)
        throw new IOException(nc4.nc_strerror(ret));

      String vname = makeString(name);
      Variable v = new Variable(ncfile, g, null, vname, getDataType(xtypep.getValue()),
              makeDimList(grpid, ndimsp.getValue(), dimids));
      ncfile.addVariable(g, v);
      v.setSPobject(new Vinfo(grpid, varno));

      List<Attribute> gatts = readAttributes(grpid, varno, nattsp.getValue());
      for (Attribute att : gatts) {
        v.addAttribute(att);
        //if (debug) System.out.printf(" add Variable Attribute %s %n",att);
      }

      if (debug) System.out.printf(" add Variable %s %n", v);
    }
  }

  private String makeDimList(int grpid, int ndimsp, int[] dims) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ndimsp; i++) {
      byte[] name = new byte[NCLibrary.NC_MAX_NAME + 1];
      int ret = nc4.nc_inq_dimname(grpid, dims[i], name);
      if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
      String dname = makeString(name);
      sb.append(dname);
      sb.append(" ");
    }
    return sb.toString();
  }

  private class Vinfo {
    int grpid, varid;
    Vinfo(int grpid, int varid) {
      this.grpid = grpid;
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
        int ret = nc4.nc_get_vars_schar(vinfo.grpid, vinfo.varid,
                convert(section.getOrigin()), convert(section.getShape()), section.getStride(), valb);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.BYTE.getPrimitiveClassType(), section.getShape(), valb);
        break;

      case CHAR:
        byte[] valc = new byte[len];
        ret = nc4.nc_get_vars_text(vinfo.grpid, vinfo.varid,
                convert(section.getOrigin()), convert(section.getShape()), section.getStride(), valc);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.CHAR.getPrimitiveClassType(), section.getShape(), IospHelper.convertByteToChar(valc));
        break;

      case DOUBLE:
        double[] vald = new double[len];
        ret = nc4.nc_get_vars_double(vinfo.grpid, vinfo.varid,
                 convert(section.getOrigin()), convert(section.getShape()), section.getStride(), vald);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), section.getShape(), vald);
        break;

      case FLOAT:
        float[] valf = new float[len];
        ret = nc4.nc_get_vars_float(vinfo.grpid, vinfo.varid,
                 convert(section.getOrigin()), convert(section.getShape()), section.getStride(), valf);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.FLOAT.getPrimitiveClassType(), section.getShape(), valf);
        break;

      case INT:
        int[] vali = new int[len];
        ret = nc4.nc_get_vars_int(vinfo.grpid, vinfo.varid,
                 convert(section.getOrigin()), convert(section.getShape()), section.getStride(), vali);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.INT.getPrimitiveClassType(), section.getShape(), vali);
        break;

      case LONG:
        long[] vall = new long[len];
        ret = nc4.nc_get_vars_longlong(vinfo.grpid, vinfo.varid,
                 convert(section.getOrigin()), convert(section.getShape()), section.getStride(), vall);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.LONG.getPrimitiveClassType(), section.getShape(), vall);
        break;

      case SHORT:
        short[] vals = new short[len];
        ret = nc4.nc_get_vars_short(vinfo.grpid, vinfo.varid,
                 convert(section.getOrigin()), convert(section.getShape()), section.getStride(), vals);
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
        int ret = nc4.nc_get_var_schar(vinfo.grpid, vinfo.varid, valb);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.BYTE.getPrimitiveClassType(), v2.getShape(), valb);
        break;

      case CHAR:
        byte[] valc = new byte[len];
        ret = nc4.nc_get_var_text(vinfo.grpid, vinfo.varid, valc);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.CHAR.getPrimitiveClassType(), v2.getShape(), IospHelper.convertByteToChar(valc));
        break;

      case DOUBLE:
        double[] vald = new double[len];
        ret = nc4.nc_get_var_double(vinfo.grpid, vinfo.varid, vald);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), v2.getShape(), vald);
        break;

      case FLOAT:
        float[] valf = new float[len];
        ret = nc4.nc_get_var_float(vinfo.grpid, vinfo.varid, valf);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.FLOAT.getPrimitiveClassType(), v2.getShape(), valf);
        break;

      case INT:
        int[] vali = new int[len];
        ret = nc4.nc_get_var_int(vinfo.grpid, vinfo.varid, vali);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.INT.getPrimitiveClassType(), v2.getShape(), vali);
        break;

      case LONG:
        long[] vall = new long[len];
        ret = nc4.nc_get_var_longlong(vinfo.grpid, vinfo.varid, vall);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.LONG.getPrimitiveClassType(), v2.getShape(), vall);
        break;

      case SHORT:
        short[] vals = new short[len];
        ret = nc4.nc_get_var_short(vinfo.grpid, vinfo.varid, vals);
        if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
        values = Array.factory(DataType.SHORT.getPrimitiveClassType(), v2.getShape(), vals);
        break;

      default:
        throw new IOException("Unsupported data type = "+v2.getDataType());
    }

    return values;
  }

  public void close() throws IOException {
    if (isClosed) return;
    int ret = nc4.nc_close(ncid);
    if (ret != 0) throw new IOException(nc4.nc_strerror(ret));
    isClosed = true;
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

      case NCLibrary.NC_ENUM:
        return DataType.ENUM1;
    }
    throw new IllegalArgumentException("unknown type == " + type);
  }

  private static NCLibrary nc4;

  private NCLibrary load() {
    if (nc4 == null) {
      String dir = "C:/cdev/libpath/";
      System.setProperty("jna.library.path", dir);

      System.load(dir + "zlib1.dll");
      System.load(dir + "szlibdll.dll");
      System.load(dir + "hdf5dll.dll");
      System.load(dir + "hdf5_hldll.dll");
      //System.load(dir + "netcdf.dll");

      Native.setProtected(true);
      nc4 = (NCLibrary) Native.loadLibrary("netcdf", NCLibrary.class);
      System.out.printf(" Netcdf nc_inq_libvers=%s isProtecetd=%s %n ",nc4.nc_inq_libvers(), Native.isProtected());
    }

    return nc4;
  }

  private static class MyNetcdfFile extends NetcdfFile {
    MyNetcdfFile(IOServiceProvider spi) {
      this.spi = spi;
    }
  }

  public NetcdfFile open(String location) throws Exception {
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

  public static void main(String args[]) throws Exception {
    JniIosp iosp = new JniIosp();
    NetcdfFile ncfile = iosp.open("C:/data/test2.nc");
    System.out.println(""+ncfile);
  }

}
