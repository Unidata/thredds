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

/**
 * Class Description.
 *
 * @author caron
 * @since Oct 30, 2008
 */
public class JniIosp extends AbstractIOServiceProvider {
  private static boolean debug = true;

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  private int ncid;
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    load();
    if (debug) System.out.println("open "+ncfile.getLocation());
    IntByReference ncidp = new IntByReference();
    int ret = nc4.nc_open(ncfile.getLocation(), 0, ncidp);
    if (ret != 0) throw new IOException(nc4.nc_strerror( ret));

    Group root = ncfile.getRootGroup();
    ncid = ncidp.getValue();
    readDimensions(ncfile, root);

    IntByReference ngattsp = new IntByReference();
    ret = nc4.nc_inq_natts(ncid, ngattsp);
    if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
    List<Attribute> gatts = readAttributes(ngattsp.getValue(), nc4.NC_GLOBAL);
    for (Attribute att : gatts) {
      ncfile.addAttribute(root, att);
      if (debug) System.out.printf(" add Global Attribute %s %n",att);
    }

    readVariables(ncfile, root);
  }

  private void readDimensions(NetcdfFile ncfile, Group g) throws IOException {
    IntByReference ndimsp = new IntByReference();
    int ret = nc4.nc_inq_ndims(ncid, ndimsp);
    if (ret != 0) throw new IOException( nc4.nc_strerror( ret));

    IntByReference nunlimdimsp = new IntByReference();
    int[] unlimdimids = new int[nc4.NC_MAX_DIMS];
    ret = nc4.nc_inq_unlimdims(ncid, nunlimdimsp, unlimdimids);

    int ndims = ndimsp.getValue();
    for (int i=0; i<ndims; i++) {
      byte[] name = new byte[nc4.NC_MAX_NAME + 1];
      IntByReference lenp = new IntByReference();
      ret = nc4.nc_inq_dim(ncid, i, name, lenp);
      if (ret != 0) throw new IOException( nc4.nc_strerror( ret));

      boolean isUnlimited = containsInt(nunlimdimsp.getValue(), unlimdimids, i);
      Dimension dim = new Dimension(Native.toString(name), lenp.getValue(), true, isUnlimited, false);
      ncfile.addDimension(g, dim);
      if (debug) System.out.printf(" add Dimension %s %n",dim);
    }
  }

  private boolean containsInt(int n, int[] have, int want) {
    for (int i=0; i<n; i++) {
      if (have[i] == want) return true;
    }
    return false;
  }

  private List<Attribute> readAttributes(int natts, int varid) throws IOException {
    List<Attribute> result = new ArrayList<Attribute>(natts);

    for (int attnum=0; attnum<natts; attnum++) {
      byte[] name = new byte[nc4.NC_MAX_NAME + 1];
      int ret =  nc4.nc_inq_attname(ncid, varid, attnum, name);
      if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
      String attname = Native.toString(name);

      IntByReference xtypep = new IntByReference();
      ret = nc4.nc_inq_atttype(ncid, varid, attname, xtypep);
      if (ret != 0) throw new IOException( nc4.nc_strerror( ret));

      IntByReference lenp = new IntByReference();
      ret =  nc4.nc_inq_attlen(ncid, varid, attname, lenp);
      if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
      int len = lenp.getValue();

      Array values = null;
      switch (xtypep.getValue()) {
        case NCLibrary.NC_BYTE:
          byte[] valb = new byte[len];
          ret = nc4.nc_get_att_schar(ncid, varid, attname, valb);
          if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
          values = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[] {len}, valb);
          break;

        case NCLibrary.NC_CHAR:
          byte[] text = new byte[len];
          ret = nc4.nc_get_att_text(ncid, varid, attname, text);
          if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
          Attribute att = new Attribute(attname, Native.toString(text));
          result.add(att);
          break;

        case NCLibrary.NC_DOUBLE:
          double[] vald = new double[len];
          ret = nc4.nc_get_att_double(ncid, varid, attname, vald);
          if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
          values = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] {len}, vald);
          break;

        case NCLibrary.NC_FLOAT:
          float[] valf = new float[len];
          ret = nc4.nc_get_att_float(ncid, varid, attname, valf);
          if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
          values = Array.factory(DataType.FLOAT.getPrimitiveClassType(), new int[] {len}, valf);
          break;

        case NCLibrary.NC_INT:
          int[] vali = new int[len];
          ret = nc4.nc_get_att_int(ncid, varid, attname, vali);
          if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
          values = Array.factory(DataType.INT.getPrimitiveClassType(), new int[] {len}, vali);
          break;

        case NCLibrary.NC_INT64:
          long[] vall = new long[len];
          ret = nc4.nc_get_att_longlong(ncid, varid, attname, vall);
          if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
          values = Array.factory(DataType.LONG.getPrimitiveClassType(), new int[] {len}, vall);
          break;

        case NCLibrary.NC_SHORT:
          short[] vals = new short[len];
          ret = nc4.nc_get_att_short(ncid, varid, attname, vals);
          if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
          values = Array.factory(DataType.SHORT.getPrimitiveClassType(), new int[] {len}, vals);
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
    if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
    int nvars = nvarsp.getValue();

    for (int varno=0; varno<nvars; varno++) {
      byte[] name = new byte[nc4.NC_MAX_NAME + 1];
      IntByReference xtypep = new IntByReference();
      IntByReference ndimsp = new IntByReference();
      int[] dimids = new int[nc4.NC_MAX_DIMS];
      IntByReference nattsp = new IntByReference();

      ret = nc4.nc_inq_var(ncid, varno, name, xtypep, ndimsp, dimids, nattsp);
      if (ret != 0) throw new IOException( nc4.nc_strerror( ret));

      String vname = Native.toString(name);
      Variable v = new Variable(ncfile, g, null, vname, getDataType(xtypep.getValue()),
              makeDimList(g, ndimsp.getValue(), dimids));
      ncfile.addVariable(g, v);

      List<Attribute> gatts = readAttributes(nattsp.getValue(), varno);
      for (Attribute att : gatts) {
        v.addAttribute( att);
        //if (debug) System.out.printf(" add Variable Attribute %s %n",att);
      }

      if (debug) System.out.printf(" add Variable %s %n", v);
    }
  }

  private String makeDimList(Group g, int ndimsp, int[] dims) {
    List<Dimension> dimList = g.getDimensions();
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<ndimsp; i++) {
      Dimension d = dimList.get(dims[i]);
      sb.append(d.getName());
      sb.append(" ");
    }
    return sb.toString();
  }

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void close() throws IOException {
    int ret =  nc4.nc_close(ncid);
    if (ret != 0) throw new IOException( nc4.nc_strerror( ret));
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
      //System.setProperty("jna.library.path", "C:/cdev/install/bin2");
      System.setProperty("jna.library.path", "C:\\dev\\tds\\thredds\\lib\\binary\\win32");

      System.load("C:/cdev/install/bin2/zlib1.dll");
      System.load("C:/cdev/install/bin2/szlibdll.dll");
      System.load("C:/cdev/install/bin2/hdf5dll.dll");

      nc4 = (NCLibrary) Native.loadLibrary("netcdf", NCLibrary.class);
    }

    return nc4;
  }

  private static class MyNetcdfFile extends NetcdfFile {

  }

  public static void main(String args[]) throws IOException {
    JniIosp iosp = new JniIosp();
    MyNetcdfFile ncfile = new MyNetcdfFile();
    ncfile.setLocation("D:/data/test/foo.nc");
    iosp.open(null, ncfile, null);

  }


}
