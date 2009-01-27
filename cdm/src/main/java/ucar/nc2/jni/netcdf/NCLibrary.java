/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
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

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;

import java.nio.ByteBuffer;

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

  // nc_inq_format
  static public final int NC_FORMAT_CLASSIC = 1;
  static public final int NC_FORMAT_64BIT = 2;
  static public final int NC_FORMAT_NETCDF4 = 3;
  static public final int NC_FORMAT_NETCDF4_CLASSIC = 4;

  static public class Vlen_t extends Structure {
    public static class ByValue extends Vlen_t implements Structure.ByValue { }
    public int len; /* Length of VL data (in base type units) */
    //public int p; /* Length of VL data (in base type units) */
    //public NativeLong len; /* Length of VL data (in base type units) */
    public Pointer p;    /* Pointer to VL data */
  }
  // library
  String nc_inq_libvers();
  String nc_strerror(int ncerr);

  // dataset
  int nc_open(String path, int mode, IntByReference ncidp);
  int nc_close(int ncid);
  int nc_inq_format(int ncid, IntByReference formatp);

  // groups
  int nc_inq_grps(int ncid, IntByReference numgrps, Pointer np); // allow to pass NULL
  int nc_inq_grps(int ncid, IntByReference numgrps, int[] ncids);
  int nc_inq_grpname(int ncid, byte[] name);

  // dimension info
  int nc_inq_ndims(int ncid, IntByReference ndimsp);
  int nc_inq_unlimdims(int ncid, IntByReference nunlimdimsp, int[] unlimdimidsp);
  int nc_inq_dimids(int ncid, IntByReference ndims, int[] dimids, int include_parents);
  int nc_inq_dim(int ncid, int dimid, byte[] name, NativeLongByReference lenp); // size_t
  int nc_inq_dimname(int ncid, int dimid, byte[] name);

  // attribute info
  int nc_inq_natts(int ncid, IntByReference nattsp);
  int nc_inq_attname(int ncid, int varid, int attnum, byte[] name);
  int nc_inq_atttype(int ncid, int varid, String name, IntByReference xtypep);
  int nc_inq_attlen(int ncid, int varid, String name, NativeLongByReference lenp); // size_t

  // attribute values
  int nc_get_att_double(int ncid, int varid, String name, double[] ip);
  int nc_get_att_float(int ncid, int varid, String name, float[] ip);
  int nc_get_att_int(int ncid, int varid, String name, int[] ip);
  int nc_get_att_uint(int ncid, int varid, String name, int[] ip);
  int nc_get_att_longlong(int ncid, int varid, String name, long[] ip);
  int nc_get_att_ulonglong(int ncid, int varid, String name, long[] ip);
  int nc_get_att_schar(int ncid, int varid, String name, byte[] ip);
  int nc_get_att_uchar(int ncid, int varid, String name, byte[] ip);
  int nc_get_att_ubyte(int ncid, int varid, String name, byte[] ip);
  int nc_get_att_short(int ncid, int varid, String name, short[] ip);
  int nc_get_att_ushort(int ncid, int varid, String name, short[] ip);
  int nc_get_att_text(int ncid, int varid, String name, byte[] ip);
  int nc_get_att_string(int ncid, int varid, String name, String[] ip);
  int nc_get_att(int ncid, int varid, String name, Vlen_t[] vlen);    // vlen
  int nc_get_att(int ncid, int varid, String name, ByteBuffer bbuff); // other user defined types

  // variable info
  int nc_inq_nvars(int ncid, IntByReference nvarsp);
  int nc_inq_varids(int ncid, IntByReference nvars, int[] varids);  
  int nc_inq_var(int ncid, int varid, byte[] name, IntByReference xtypep,
          IntByReference ndimsp, int[] dimidsp, IntByReference nattsp);

  // user types
  int nc_inq_typeids(int ncid, IntByReference ntypes, Pointer np); // allow to pass NULL
  int nc_inq_typeids(int ncid, IntByReference ntypes, int[] typeids);
  int nc_inq_type(int ncid, int xtype, byte[] name, NativeLongByReference sizep); // size_t
  int nc_inq_user_type(int ncid, int xtype, byte[] name, NativeLongByReference sizep,
                   IntByReference baseType, NativeLongByReference nfieldsp, IntByReference classp); // size_t
  int nc_inq_enum(int ncid, int xtype, byte[] name, IntByReference baseType,
              NativeLongByReference base_sizep, NativeLongByReference num_membersp); //size_t
  int nc_inq_enum_member(int ncid, int xtype, int idx, byte[] name, IntByReference value); // void *
  int nc_inq_opaque(int ncid, int xtype, byte[] name, NativeLongByReference sizep);

  // compound user type
  int nc_inq_compound(int ncid, int xtype, byte[] name, NativeLongByReference sizep, NativeLongByReference nfieldsp);
  int nc_inq_compound_field(int ncid, int xtype, int fieldid, byte[] name, 
	  NativeLongByReference offsetp, IntByReference field_typeidp, IntByReference ndimsp, int[] dims);

  // read entire array
  int nc_get_var(int ncid, int varid, ByteBuffer bbuff);
  int nc_get_var(int ncid, int varid, Vlen_t[] vlen);      // vlen

  int nc_get_var_text(int ncid, int varid, byte[] op);
  int nc_get_var_schar(int ncid, int varid, byte[] ip);
  int nc_get_var_ubyte(int ncid, int varid,  byte[] ip);  
  int nc_get_var_short(int ncid, int varid, short[] ip);
  int nc_get_var_ushort(int ncid, int varid, short[] ip);
  int nc_get_var_int(int ncid, int varid, int[] ip);
  int nc_get_var_longlong(int ncid, int varid, long[] ip);
  int nc_get_var_ulonglong(int ncid, int varid, long[] ip);
  int nc_get_var_float(int ncid, int varid, float[] ip);
  int nc_get_var_double(int ncid, int varid, double[] ip);
  int nc_get_var_string(int ncid, int varid, String[] sarray);

  // read array section
  // working on blueman
  int nc_get_vars_schar(int ncid, int varid, long[] startp, long[] countp, int[] stridep, byte[] ip); // size_t, ptrdiff_t
  int nc_get_vars_text(int ncid, int varid, long[] startp, long[] countp, int[] stridep, byte[] ip); // size_t, ptrdiff_t
  int nc_get_vars_short(int ncid, int varid, long[] startp, long[] countp, int[] stridep, short[] ip); // size_t, ptrdiff_t
  int nc_get_vars_int(int ncid, int varid, long[] startp, long[] countp, int[] stridep, int[] ip); // size_t, ptrdiff_t
  int nc_get_vars_longlong(int ncid, int varid, long[] startp, long[] countp, int[] stridep, long[] ip); // size_t, ptrdiff_t
  int nc_get_vars_float(int ncid, int varid, long[] startp, long[] countp, int[] stridep, float[] ip); // size_t, ptrdiff_t
  int nc_get_vars_double(int ncid, int varid, long[] startp, long[] countp, int[] stridep, double[] ip); // size_t, ptrdiff_t
}
