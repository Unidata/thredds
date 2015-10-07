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
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.FloatByReference;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

/**
 * JNA access to Netcdf-4 C Library, using JNI to shared C library.
 * Just the functions actually used.
 *
 * @author caron
 * @since Oct 30, 2008
 */
public interface Nc4prototypes extends Library {
  static public final int NC_MAX_DIMS = 1024;   /* max dimensions per file */
  static public final int NC_MAX_ATTRS = 8192;   /* max global or per variable attributes */
  static public final int NC_MAX_VARS = 8192;   /* max variables per file */
  static public final int NC_MAX_NAME = 256;   /* max length of a name */
  static public final int NC_MAX_VAR_DIMS = NC_MAX_DIMS; /* max per variable dimensions */

  static public final int NC_GLOBAL = -1;
  static public final int NC_UNLIMITED = 0;

  static public final int NC_NOWRITE = 0;
  static public final int NC_WRITE = 1;

  static public final int NC_NAT = 0;	/* Not-A-Type */
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
  static public final int NC_MAX_ATOMIC_TYPE = NC_STRING;

  /* The following are use internally in support of user-defines
   * types. They are also the class returned by nc_inq_user_type. */
  static public final int NC_VLEN =	13;	/* used internally for vlen types */
  static public final int NC_OPAQUE =	14;	/* used internally for opaque types */
  static public final int NC_ENUM =	15;	/* used internally for enum types */
  static public final int NC_COMPOUND =	16;	/* used internally for compound types */

  static public final int NC_CLOBBER	     = 0;       /**< Destroy existing file. Mode flag for nc_create(). */
  static public final int NC_NOCLOBBER	   = 0x0004;	/**< Don't destroy existing file. Mode flag for nc_create(). */
  static public final int NC_DISKLESS      = 0x0008;  /**< Create a diskless file. Mode flag for nc_create(). */
  static public final int NC_MMAP          = 0x0010;  /**< Use diskless file with mmap. Mode flag for nc_open() or nc_create(). */
  static public final int NC_CLASSIC_MODEL = 0x0100; /**< Enforce classic model. Mode flag for nc_create(). */
  static public final int NC_64BIT_OFFSET  = 0x0200;  /**< Use large (64-bit) file offsets. Mode flag for nc_create(). */
  static public final int NC_NETCDF4       = 0x1000;  /**< Use netCDF-4/HDF5 format. Mode flag for nc_create(). */
  /** Turn on MPI I/O.
      Use this in mode flags for both nc_create() and nc_open(). */
  static public final int NC_MPIIO =         0x2000;
  /** Turn on MPI POSIX I/O.
      Use this in mode flags for both nc_create() and nc_open(). */
  static public final int NC_MPIPOSIX =      0x4000;
  static public final int NC_PNETCDF =       0x8000;	/**< Use parallel-netcdf library. Mode flag for nc_open(). */

/** Format specifier for nc_set_default_format() and returned
 *  by nc_inq_format. 
 */
  static public final int NC_FORMAT_CLASSIC = (1);
  static public final int NC_FORMAT_64BIT = (2);
  static public final int NC_FORMAT_NETCDF4 = (3);
  static public final int NC_FORMAT_NETCDF4_CLASSIC = (4);

/** Extended format specifier returned by  nc_inq_format_extended() 
 *  Added in version 4.3.1. This returns the true format of the
 *  underlying data.
 */

  static public final int NC_FORMAT_NC3 = (1);
  static public final int NC_FORMAT_NC_HDF5 = (2) /*cdf 4 subset of HDF5 */;
  static public final int NC_FORMAT_NC_HDF4 = (3) /* netcdf 4 subset of HDF4 */;
  static public final int NC_FORMAT_PNETCDF = (4);
  static public final int NC_FORMAT_DAP2 = (5);
  static public final int NC_FORMAT_DAP4 = (6);
  static public final int NC_FORMAT_UNDEFINED = (0);

  //  nc_def_var_chunking()
  static public final int NC_CHUNKED    = 0;
  static public final int NC_CONTIGUOUS = 1;

  static public class Vlen_t extends Structure {
    public static class ByValue extends Vlen_t implements Structure.ByValue { }
    public int len; /* Length of VL data (in base type units) */
    //public int p; /* Length of VL data (in base type units) */
    //public NativeLong len; /* Length of VL data (in base type units) */
    public Pointer p;    /* Pointer to VL data */
    protected List getFieldOrder()
    {
      List fields = new ArrayList();
      fields.add("len");
      fields.add("p");
      return fields;
    }
  }

  // library
  String nc_inq_libvers();
  String nc_strerror(int ncerr);

  // dataset
  // int nc_open(const char *path, int mode, int *ncidp);
  int nc_open(String path, int mode, IntByReference ncidp);
  int nc_close(int ncid);
  int nc_inq_format(int ncid, IntByReference formatp);
  int nc_inq_format_extended(int ncid, IntByReference formatp, IntByReference modep);

  // groups
  int nc_inq_grps(int ncid, IntByReference numgrps, Pointer np); // allow to pass NULL
  int nc_inq_grps(int ncid, IntByReference numgrps, int[] ncids);
  int nc_inq_grpname(int ncid, byte[] name);

  // dimension info
  int nc_inq_ndims(int ncid, IntByReference ndimsp);
  int nc_inq_unlimdims(int ncid, IntByReference nunlimdimsp, int[] unlimdimidsp);
  int nc_inq_dimids(int ncid, IntByReference ndims, int[] dimids, int include_parents);
  int nc_inq_dim(int ncid, int dimid, byte[] name, SizeTByReference lenp);
  int nc_inq_dimname(int ncid, int dimid, byte[] name);

  // attribute info
  int nc_inq_natts(int ncid, IntByReference nattsp);
  int nc_inq_attname(int ncid, int varid, int attnum, byte[] name);
  int nc_inq_atttype(int ncid, int varid, String name, IntByReference xtypep);
  int nc_inq_attlen(int ncid, int varid, String name, SizeTByReference lenp);

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
  int nc_get_att(int ncid, int varid, String name, byte[] bbuff); // other user defined types

  // variable info
  int nc_inq_nvars(int ncid, IntByReference nvarsp);
  int nc_inq_varids(int ncid, IntByReference nvars, int[] varids);  
  int nc_inq_var(int ncid, int varid, byte[] name, IntByReference xtypep, IntByReference ndimsp, int[] dimidsp, IntByReference nattsp);

  // user types
  int nc_inq_typeids(int ncid, IntByReference ntypes, Pointer np); // allow to pass NULL
  int nc_inq_typeids(int ncid, IntByReference ntypes, int[] typeids);
  int nc_inq_type(int ncid, int xtype, byte[] name, SizeTByReference sizep);
  int nc_inq_user_type(int ncid, int xtype, byte[] name, SizeTByReference sizep, IntByReference baseType, SizeTByReference nfieldsp, IntByReference classp);
  int nc_inq_enum(int ncid, int xtype, byte[] name, IntByReference baseType, SizeTByReference base_sizep, SizeTByReference num_membersp);
  int nc_inq_enum_member(int ncid, int xtype, int idx, byte[] name, IntByReference value); // void *
  int nc_inq_opaque(int ncid, int xtype, byte[] name, SizeTByReference sizep);

  // compound user type
  int nc_inq_compound(int ncid, int xtype, byte[] name, SizeTByReference sizep, SizeTByReference nfieldsp);
  int nc_inq_compound_field(int ncid, int xtype, int fieldid, byte[] name, SizeTByReference offsetp, IntByReference field_typeidp, IntByReference ndimsp, int[] dims);

  // read entire array
  int nc_get_var(int ncid, int varid, byte[] bbuff);
  int nc_get_var(int ncid, int varid, Vlen_t[] vlen);      // vlen

  int nc_get_var_text(int ncid, int varid, byte[] op);
  int nc_get_var_schar(int ncid, int varid, byte[] ip);
  int nc_get_var_ubyte(int ncid, int varid,  byte[] ip);  
  int nc_get_var_short(int ncid, int varid, short[] ip);
  int nc_get_var_ushort(int ncid, int varid, short[] ip);
  int nc_get_var_int(int ncid, int varid, int[] ip);
  int nc_get_var_uint(int ncid, int varid, int[] ip);
  int nc_get_var_longlong(int ncid, int varid, long[] ip);
  int nc_get_var_ulonglong(int ncid, int varid, long[] ip);
  int nc_get_var_float(int ncid, int varid, float[] ip);
  int nc_get_var_double(int ncid, int varid, double[] ip);
  int nc_get_var_string(int ncid, int varid, String[] sarray);

  // read array section

  int nc_get_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] bbuff);
  int nc_get_vara_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip);
  int nc_get_vara_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip);
  int nc_get_vara_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip);
  int nc_get_vara_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip);
  int nc_get_vara_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip);
  int nc_get_vara_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip);
  int nc_get_vara_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip);
  int nc_get_vara_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip);
  int nc_get_vara_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip);
  int nc_get_vara_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, float[] ip);
  int nc_get_vara_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, double[] ip);
  int nc_get_vara_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, String[] ip);

  int nc_get_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] bbuff);
  int nc_get_vars_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip);
  int nc_get_vars_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip);
  int nc_get_vars_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip);
  int nc_get_vars_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip);
  int nc_get_vars_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip);
  int nc_get_vars_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip);
  int nc_get_vars_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip);
  int nc_get_vars_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip);
  int nc_get_vars_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip);
  int nc_get_vars_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, float[] ip);
  int nc_get_vars_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, double[] ip);
  int nc_get_vars_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, String[] ip);

  //////////////////////////////////////////////////////////////////////////////////
  //// writing

  static public final int  NC_FILL		= 0;	    /**< Argument to nc_set_fill() to clear NC_NOFILL */
  static public final int  NC_NOFILL	= 0x100;	/**< Argument to nc_set_fill() to turn off filling of data. */


  /* Set the default nc_create format to NC_FORMAT_CLASSIC, NC_FORMAT_64BIT, NC_FORMAT_NETCDF4, NC_FORMAT_NETCDF4_CLASSIC. */
  int nc_set_default_format(int format, IntByReference old_formatp);

  int nc_create(String path, int cmode, IntByReference ncidp);
  int nc_enddef	(int ncid);
  int nc_sync	(int ncid);

  int nc_def_grp (int parent_ncid, String name, IntByReference new_ncid);
  int nc_def_dim(int ncid,  String name, SizeT len, IntByReference dimid);
  int nc_inq_dimlen(int ncid, int dimid, SizeTByReference lenp);
  int nc_def_var (int ncid, String name, SizeT xtype, int ndims, int[] dimids, IntByReference varidp);

  int nc_def_compound(int ncid, SizeT size, String name, IntByReference typeidp);
  int nc_insert_compound(int ncid, int typeid, String name, SizeT offset, int field_typeid);
  int nc_insert_array_compound(int ncid, int typeid, String name, SizeT offset, int field_typeid, int ndims, int[] dim_sizes);

 /* Create an enum type. Provide a base type and a name. At the moment
  * only ints are accepted as base types. */
 int nc_def_enum(int ncid, int base_typeid, String name, IntByReference typeidp);
 /* Insert a named value into an enum type. The value must fit within
  * the size of the enum type, the name size must be <= NC_MAX_NAME. */
 int nc_insert_enum(int ncid, int enumid, String name, IntByReference value);

  /* Rename a group */
  int nc_rename_grp(int grpid, String name);

  /* Write entire var of any type. */
  int nc_put_var(int ncid, int varid, byte[] bbuff);

  // write array section
  int nc_put_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] bbuff);
  int nc_put_vara_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip);
  int nc_put_vara_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip);
  int nc_put_vara_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip);
  int nc_put_vara_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip);
  int nc_put_vara_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip);
  int nc_put_vara_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip);
  int nc_put_vara_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip);
  int nc_put_vara_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip);
  int nc_put_vara_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip);
  int nc_put_vara_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, float[] ip);
  int nc_put_vara_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, double[] ip);

/*int nc_get_vara_string(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, char **ip);  */
  int nc_put_vara_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, String[] ip);

  // int nc_put_vars_text(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, const char *op);
  // int nc_put_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, ByteBuffer bbuff);
  int nc_put_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[]  bbuff);

  // nc_put_vars_uchar(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, const unsigned char *op);
  int nc_put_vars_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip);

  int nc_put_vars_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip);
  // nc_put_vars_text(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, const char *op);
  int nc_put_vars_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip);
  int nc_put_vars_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip);
  int nc_put_vars_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip);
  int nc_put_vars_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip);
  int nc_put_vars_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip);
  int nc_put_vars_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip);
  int nc_put_vars_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip);
  int nc_put_vars_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, float[] ip);
  // nc_put_vars_double(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, const double *op);
  int nc_put_vars_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, double[] ip);
  int nc_put_vars_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, String[] ip);

  int nc_put_var_uchar(int ncid, int varid, byte[] ip);
  int nc_put_var_schar(int ncid, int varid,  byte[] ip);
  int nc_put_var_text(int ncid, int varid, byte[] ip);
  int nc_put_var_short(int ncid, int varid, short[] ip);
  int nc_put_var_ushort(int ncid, int varid, short[] ip);
  int nc_put_var_int(int ncid, int varid, int[] ip);
  int nc_put_var_uint(int ncid, int varid, int[] ip);
  int nc_put_var_longlong(int ncid, int varid, long[] ip);
  int nc_put_var_ulonglong(int ncid, int varid, long[] ip);
  int nc_put_var_float(int ncid, int varid, float[] ip);
  int nc_put_var_double(int ncid, int varid, double[] ip);
  int nc_put_var_string(int ncid, int varid, String[] op);

  // write attributes
  int nc_put_att (int ncid, int varid, String name, int xtype, SizeT len, byte[] value);
  int nc_put_att_string(int ncid, int varid, String attName, SizeT len, String[] value);
  int nc_put_att_text(int ncid, int varid, String attName, SizeT len, byte[] value);
  int nc_put_att_uchar(int ncid, int varid, String attName, int xtype, SizeT len, byte[] value);
  int nc_put_att_schar(int ncid, int varid, String attName, int xtype, SizeT len, byte[] value);
  int nc_put_att_short(int ncid, int varid, String attName, int xtype, SizeT len, short[] value);
  int nc_put_att_ushort(int ncid, int varid, String attName, int xtype, SizeT len, short[] value);
  int nc_put_att_int(int ncid, int varid, String attName, int xtype, SizeT len, int[] value);
  int nc_put_att_uint(int ncid, int varid, String attName, int xtype, SizeT len, int[] value);
  int nc_put_att_longlong(int ncid, int varid, String attName, int xtype, SizeT len, long[] value);
  int nc_put_att_ulonglong(int ncid, int varid, String attName, int xtype, SizeT len, long[] value);
  int nc_put_att_float(int ncid, int varid, String attName, int xtype, SizeT len, float[] value);
  int nc_put_att_double(int ncid, int varid, String attName, int xtype, SizeT len, double[] value);
  
  /* Extra netcdf-4 stuff. */
  
  /* Set compression settings for a variable. Lower is faster, higher is better.
   * Must be called after nc_def_var and before nc_enddef. */
  int nc_def_var_deflate(int ncid, int varid, int shuffle, int deflate, int deflate_level);
  
  /* Find out compression settings of a var. */
  int nc_inq_var_deflate(int ncid, int varid, IntByReference shufflep, IntByReference deflatep, IntByReference deflate_levelp);
  
  /* Find out szip settings of a var. */
  int nc_inq_var_szip(int ncid, int varid, IntByReference options_maskp, IntByReference pixels_per_blockp);

  /* Set fletcher32 checksum for a var. This must be done after nc_def_var and before nc_enddef. */
  int nc_def_var_fletcher32(int ncid, int varid, int fletcher32);

  /* Inquire about fletcher32 checksum for a var. */
  int nc_inq_var_fletcher32(int ncid, int varid, IntByReference fletcher32p);
  
  /* Define chunking for a variable. This must be done after nc_def_var and before nc_enddef. */
  // nc_def_var_chunking(int ncid, int varid, int storage, const size_t *chunksizesp);
  int nc_def_var_chunking(int ncid, int varid, int storage, SizeT[] chunksizesp);

  /* Inq chunking stuff for a var. */
  int nc_inq_var_chunking(int ncid, int varid, IntByReference storagep, SizeT[] chunksizesp);
  
  /* Define fill value behavior for a variable. This must be done after nc_def_var and before nc_enddef. */
  int nc_def_var_fill(int ncid, int varid, int no_fill, byte[] fill_value); // const void *  ??
  
  /* Inq fill value setting for a var. */
  int nc_inq_var_fill(int ncid, int varid, IntByReference no_fill, byte[] fill_valuep); // void * ??
  
  /* Define the endianness of a variable. */
  int nc_def_var_endian(int ncid, int varid, int endian);
  
  /* Learn about the endianness of a variable. */
  int nc_inq_var_endian(int ncid, int varid, IntByReference endianp);

  /* Set the fill mode (classic or 64-bit offset files only). */
  int nc_set_fill(int ncid, int fillmode, IntByReference old_modep);

  /* Set the cache size, nelems, and preemption policy. */
  int nc_set_chunk_cache(SizeT size, SizeT nelems, float preemption);
  
  /* Get the cache size, nelems, and preemption policy. */
  int nc_get_chunk_cache(SizeTByReference sizep, SizeTByReference nelemsp, FloatByReference preemptionp);
  
  /* Set the per-variable cache size, nelems, and preemption policy. */
  int nc_set_var_chunk_cache(int ncid, int varid, SizeT size, SizeT nelems, float preemption);
  
  /* Set the per-variable cache size, nelems, and preemption policy. */
  int nc_get_var_chunk_cache(int ncid, int varid, SizeTByReference sizep, SizeTByReference nelemsp, FloatByReference preemptionp);  // size_t
}
