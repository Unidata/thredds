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

import com.sun.jna.Library;
import com.sun.jna.ptr.IntByReference;

/**
 * JNA access to Netcd4 C Library, using JNI to shared C library.
 * Just the functions actually used.
 *
 * @author caron
 * @since Oct 30, 2008
 */
public interface NCLibrary extends Library {
  static public final int NC_MAX_DIMS = 1024;   /* max dimensions per file */
  static public final int NC_MAX_ATTRS = 8192;   /* max global or per variable attributes */
  static public final int NC_MAX_VARS = 8192;   /* max variables per file */
  static public final int NC_MAX_NAME = 256;   /* max length of a name */
  static public final int NC_MAX_VAR_DIMS = NC_MAX_DIMS; /* max per variable dimensions */

  static public final int NC_GLOBAL = -1;

  static public final int NC_BYTE = 1;	/* signed 1 byte integer */
  static public final int NC_CHAR =	2;	/* ISO/ASCII character */
  static public final int NC_SHORT =	3;	/* signed 2 byte integer */
  static public final int NC_INT = 4;	/* signed 4 byte integer */
  static public final int NC_FLOAT =	5;	/* single precision floating point number */
  static public final int NC_DOUBLE =	6;	/* double precision floating point number */
  static public final int NC_UBYTE =	7;	/* unsigned 1 byte int */
  static public final int NC_USHORT =	8;	/* unsigned 2-byte int */
  static public final int NC_UINT =	9;	/* unsigned 4-byte int */
  static public final int NC_INT64 =	10;	/* signed 8-byte int */
  static public final int NC_UINT64 =	11;/* unsigned 8-byte int */
  static public final int NC_STRING =	12;	/* string */

  /* The folloing are use internally in support of user-defines
   * types. They are also the class returned by nc_inq_user_type. */
  static public final int NC_VLEN =	13;	/* used internally for vlen types */
  static public final int NC_OPAQUE =	14;	/* used internally for opaque types */
  static public final int NC_ENUM =	15;	/* used internally for enum types */
  static public final int NC_COMPOUND =	16;	/* used internally for compound types */

  int nc_open(String path, int mode, IntByReference ncidp);
  String nc_strerror(int ncerr);
  int nc_close(int ncid);

  int nc_inq_ndims(int ncid, IntByReference ndimsp);
  int nc_inq_unlimdims(int ncid, IntByReference nunlimdimsp, int[] unlimdimidsp);
  int nc_inq_dim(int ncid, int dimid, byte[] name, IntByReference lenp); // size_t

  int nc_inq_natts(int ncid, IntByReference nattsp);
  int nc_inq_attname(int ncid, int varid, int attnum, byte[] name);
  int nc_inq_atttype(int ncid, int varid, String name, IntByReference xtypep);
  int nc_inq_attlen(int ncid, int varid, String name, IntByReference lenp); // size_t

  int nc_get_att_double(int ncid, int varid, String name, double[] ip);
  int nc_get_att_float(int ncid, int varid, String name, float[] ip);
  int nc_get_att_int(int ncid, int varid, String name, int[] ip);
  int nc_get_att_longlong(int ncid, int varid, String name, long[] ip);
  int nc_get_att_schar(int ncid, int varid, String name, byte[] ip);
  int nc_get_att_short(int ncid, int varid, String name, short[] ip);
  int nc_get_att_text(int ncid, int varid, String name, byte[] ip);

  int nc_inq_nvars(int ncid, IntByReference nvarsp);
  int nc_inq_var(int ncid, int varid, byte[] name, IntByReference xtypep,
          IntByReference ndimsp, int[] dimidsp, IntByReference nattsp);
}
