/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
 *
 * This is a transliteration of the netcdf-c library file include/netcdf.h.
 * Refer to that for documentation of the constants and functions.
 *
 */

public interface Nc4prototypes extends Library {

    static final int NC_MAX_DIMS = 1024;   /* max dimensions per file */
    static final int NC_MAX_ATTRS = 8192;   /* max global or per variable attributes */
    static final int NC_MAX_VARS = 8192;   /* max variables per file */
    static final int NC_MAX_NAME = 256;   /* max length of a name */
    static final int NC_MAX_VAR_DIMS = NC_MAX_DIMS; /* max per variable dimensions */

    static final int NC_GLOBAL = -1;
    static final int NC_UNLIMITED = 0;

    static final int NC_FILL = 0; /**< Argument to nc_set_fill() to clear NC_NOFILL */
    static final int NC_NOFILL = 0x100; /**< Argument to nc_set_fill() to turn off filling of data. */

    /* Mode Flags */
    static final int NC_NOWRITE = 0x0000; // Set read-only access for nc_open().
    static final int NC_WRITE = 0x0001; // Set read-write access for nc_open().
    static final int NC_CLOBBER = 0x0000; // Destroy existing file. Mode flag for nc_create().
    static final int NC_NOCLOBBER = 0x0004; // Don't destroy existing file. Mode flag for nc_create().

    static final int NC_DISKLESS = 0x0008;  /**< Use diskless file. Mode flag for nc_open() or nc_create(). */
    static final int NC_MMAP = 0x0010;  /**< Use diskless file with mmap. Mode flag for nc_open() or nc_create(). */
    static final int NC_INMEMORY = 0x8000;  /**< Read from memory. Mode flag for nc_open() or nc_create() => NC_DISKLESS */

    /* Format Flags */
    static final int NC_64BIT_OFFSET = 0x0200;  /**< Use large (64-bit) file offsets. Mode flag for nc_create(). */
    static final int NC_64BIT_DATA = 0x0020;  /**< CDF-5 format: classic model but 64 bit dimensions and sizes */
    static final int NC_CDF5 = NC_64BIT_DATA;  /**< Alias NC_CDF5 to NC_64BIT_DATA */

    static final int NC_CLASSIC_MODEL = 0x0100; /**< Enforce classic model on netCDF-4. Mode flag for nc_create(). */
    static final int NC_NETCDF4 = 0x1000;  /**< Use netCDF-4/HDF5 format. Mode flag for nc_create(). */

/** Turn on MPI I/O. Use this in mode flags for both nc_create() and nc_open(). */
    static final int NC_MPIIO = 0x2000;
/** Turn on MPI POSIX I/O. Use this in mode flags for both nc_create() and nc_open(). */
    static final int NC_MPIPOSIX = 0x4000; /**< \deprecated As of libhdf5 1.8.13. */

    static final int NC_PNETCDF = (NC_MPIIO); /**< Use parallel-netcdf library; alias for NC_MPIIO. */

    static final int NC_NAT = 0;	/* Not-A-Type */
    static final int NC_BYTE = 1;	/* signed 1 byte integer */
    static final int NC_CHAR = 2;	/* ISO/ASCII character */
    static final int NC_SHORT = 3;	/* signed 2 byte integer */
    static final int NC_INT = 4;	/* signed 4 byte integer */
    static final int NC_FLOAT = 5;	/* single precision floating point number */
    static final int NC_DOUBLE = 6;	/* double precision floating point number */
    static final int NC_UBYTE = 7;	/* unsigned 1 byte int */
    static final int NC_USHORT = 8;	/* unsigned 2-byte int */
    static final int NC_UINT = 9;	/* unsigned 4-byte int */
    static final int NC_INT64 = 10;	/* signed 8-byte int */
    static final int NC_UINT64 = 11;/* unsigned 8-byte int */
    static final int NC_STRING = 12;	/* string */
    static final int NC_MAX_ATOMIC_TYPE = NC_STRING;

    /* The following are use internally in support of user-defines
     * types. They are also the class returned by nc_inq_user_type. */
    static final int NC_VLEN = 13;	/* used internally for vlen types */
    static final int NC_OPAQUE = 14;	/* used internally for opaque types */
    static final int NC_ENUM = 15;	/* used internally for enum types */
    static final int NC_COMPOUND = 16;	/* used internally for compound types */

    /**
     * Format specifier for nc_set_default_format() and returned
     * by nc_inq_format.
     */
    static final int NC_FORMAT_CLASSIC = (1);
    static final int NC_FORMAT_64BIT = (2);
    static final int NC_FORMAT_NETCDF4 = (3);
    static final int NC_FORMAT_NETCDF4_CLASSIC = (4);

    /**
     * Extended format specifier returned by  nc_inq_format_extended()
     * Added in version 4.3.1. This returns the true format of the
     * underlying data.
     */

    static final int NC_FORMAT_NC3 = (1);
    static final int NC_FORMAT_NC_HDF5 = (2) /*cdf 4 subset of HDF5 */;
    static final int NC_FORMAT_NC_HDF4 = (3) /* netcdf 4 subset of HDF4 */;
    static final int NC_FORMAT_PNETCDF = (4);
    static final int NC_FORMAT_DAP2 = (5);
    static final int NC_FORMAT_DAP4 = (6);
    static final int NC_FORMAT_UNDEFINED = (0);

    //  nc_def_var_chunking()
    static final int NC_CHUNKED = 0;
    static final int NC_CONTIGUOUS = 1;

    // Selected errors
    static final int NC_NOERR = 0;

    static public class Vlen_t extends Structure {

        static public int VLENSIZE = new Vlen_t().size();

        static public byte[] contents(Vlen_t v)
        {
            if(v.p == Pointer.NULL) return null;
            return v.p.getByteArray(0,v.len);
        }

        public static class ByValue extends Vlen_t
				implements Structure.ByValue {}

        // Vlen_t Structure Fields
        public int len; /* Length of VL data (in base type units) */
        public Pointer p;    /* Pointer to VL data */

        public Vlen_t() {super();}

        public Vlen_t(Pointer p) {super(p);}

        protected List getFieldOrder() {
            List fields = new ArrayList();
            fields.add("len");
            fields.add("p");
            return fields;
        }
    }

    // Begin API; Do not Remove this Line

    String nc_inq_libvers();
    String nc_strerror(int ncerr);

    int nc_open(String path, int mode, IntByReference ncidp);
    int nc_close(int ncid);
    int nc_inq_format(int ncid, IntByReference formatp);
    int nc_inq_format_extended(int ncid, IntByReference formatp, IntByReference modep);

    int nc_inq_grps(int ncid, IntByReference numgrps, int[] ncids);
    int nc_inq_grpname(int ncid, byte[] name);
    int nc_inq_grpname_full(int ncid, SizeTByReference lenp, byte[] full_name);
    int nc_inq_grpname_len(int ncid, SizeTByReference lenp);

    int nc_inq_ndims(int ncid, IntByReference ndimsp);
    int nc_inq_unlimdims(int ncid, IntByReference nunlimdimsp, int[] unlimdimidsp);
    int nc_inq_dimids(int ncid, IntByReference ndims, int[] dimids, int include_parents);
    int nc_inq_dim(int ncid, int dimid, byte[] name, SizeTByReference lenp);
    int nc_inq_dimname(int ncid, int dimid, byte[] name);

    int nc_inq_natts(int ncid, IntByReference nattsp);
    int nc_inq_attname(int ncid, int varid, int attnum, byte[] name);
    int nc_inq_atttype(int ncid, int varid, String name, IntByReference xtypep);
    int nc_inq_attlen(int ncid, int varid, String name, SizeTByReference lenp);

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
    int nc_get_att(int ncid, int varid, String name, byte[] bbuff);

    int nc_inq_nvars(int ncid, IntByReference nvarsp);
    int nc_inq_varids(int ncid, IntByReference nvars, int[] varids);
    int nc_inq_var(int ncid, int varid, byte[] name, IntByReference xtypep, IntByReference ndimsp, int[] dimidsp, IntByReference nattsp);
    int nc_inq_varid(int ncid, byte[] name, IntByReference varidp);
    int nc_inq_vardimid(int ncid, int varid, int[] dimidsp);
    int nc_inq_varnatts(int ncid, int varid, IntByReference nattsp);

    int nc_inq_typeids(int ncid, IntByReference ntypes, int[] typeids);
    int nc_inq_type(int ncid, int xtype, byte[] name, SizeTByReference sizep);
    int nc_inq_user_type(int ncid, int xtype, byte[] name, SizeTByReference sizep, IntByReference baseType, SizeTByReference nfieldsp, IntByReference classp);
    int nc_inq_enum(int ncid, int xtype, byte[] name, IntByReference baseType, SizeTByReference base_sizep, SizeTByReference num_membersp);
    int nc_inq_enum_member(int ncid, int xtype, int idx, byte[] name, IntByReference value);
    int nc_inq_opaque(int ncid, int xtype, byte[] name, SizeTByReference sizep);

    int nc_get_var(int ncid, int varid, byte[] buf);
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

    int nc_get_var1(int ncid, int varid, SizeT[] indexp, byte[] buf);
    int nc_get_var1_text(int ncid, int varid, SizeT[] indexp, byte[] op);
    int nc_get_var1_schar(int ncid, int varid, SizeT[] indexp, byte[] ip);
    int nc_get_var1_ubyte(int ncid, int varid, SizeT[] indexp, byte[] ip);
    int nc_get_var1_short(int ncid, int varid, SizeT[] indexp, short[] ip);
    int nc_get_var1_ushort(int ncid, int varid, SizeT[] indexp, short[] ip);
    int nc_get_var1_int(int ncid, int varid, SizeT[] indexp, int[] ip);
    int nc_get_var1_uint(int ncid, int varid, SizeT[] indexp, int[] ip);
    int nc_get_var1_longlong(int ncid, int varid, SizeT[] indexp, long[] ip);
    int nc_get_var1_ulonglong(int ncid, int varid, SizeT[] indexp, long[] ip);
    int nc_get_var1_float(int ncid, int varid, SizeT[] indexp, float[] ip);
    int nc_get_var1_double(int ncid, int varid, SizeT[] indexp, double[] ip);
    int nc_get_var1_string(int ncid, int varid, SizeT[] indexp, String[] sarray);

    int nc_get_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] buf);
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

    int nc_get_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] buf);
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

    int nc_set_default_format(int format, IntByReference old_formatp);
    int nc_create(String path, int cmode, IntByReference ncidp);
    int nc_enddef   (int ncid);
    int nc_sync     (int ncid);
    int nc_def_grp (int parent_ncid, String name, IntByReference new_ncid);
    int nc_def_dim(int ncid,  String name, SizeT len, IntByReference dimid);
    int nc_inq_dimlen(int ncid, int dimid, SizeTByReference lenp);
    int nc_def_var (int ncid, String name, SizeT xtype, int ndims, int[] dimids, IntByReference varidp);
    int nc_def_compound(int ncid, SizeT size, String name, IntByReference typeidp);
    int nc_insert_compound(int ncid, int typeid, String name, SizeT offset, int field_typeid);
    int nc_insert_array_compound(int ncid, int typeid, String name, SizeT offset, int field_typeid, int ndims, int[] dim_sizes);
    int nc_def_enum(int ncid, int base_typeid, String name, IntByReference typeidp);
    int nc_insert_enum(int ncid, int enumid, String name, IntByReference value);

    int nc_rename_grp(int grpid, String name);
    int nc_put_var(int ncid, int varid, byte[] bbuff);

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
    int nc_put_vara_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, String[] ip);

    int nc_put_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[]  bbuff);

    int nc_put_vars_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip);
    int nc_put_vars_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip);

    int nc_put_vars_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip);
    int nc_put_vars_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip);
    int nc_put_vars_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip);
    int nc_put_vars_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip);
    int nc_put_vars_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip);
    int nc_put_vars_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip);
    int nc_put_vars_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip);
    int nc_put_vars_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, float[] ip);

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

    int nc_def_var_deflate(int ncid, int varid, int shuffle, int deflate, int deflate_level);
    int nc_inq_var_deflate(int ncid, int varid, IntByReference shufflep, IntByReference deflatep, IntByReference deflate_levelp);
    int nc_inq_var_szip(int ncid, int varid, IntByReference options_maskp, IntByReference pixels_per_blockp);
    int nc_def_var_fletcher32(int ncid, int varid, int fletcher32);
    int nc_inq_var_fletcher32(int ncid, int varid, IntByReference fletcher32p);

    int nc_def_var_chunking(int ncid, int varid, int storage, SizeT[] chunksizesp);
    int nc_inq_var_chunking(int ncid, int varid, IntByReference storagep, SizeT[] chunksizesp);
    int nc_def_var_fill(int ncid, int varid, int no_fill, byte[] fill_value);
    int nc_inq_var_fill(int ncid, int varid, IntByReference no_fill, byte[] fill_valuep);
    int nc_def_var_endian(int ncid, int varid, int endian);
    int nc_inq_var_endian(int ncid, int varid, IntByReference endianp);
    int nc_set_fill(int ncid, int fillmode, IntByReference old_modep);
    int nc_set_chunk_cache(SizeT size, SizeT nelems, float preemption);
    int nc_get_chunk_cache(SizeTByReference sizep, SizeTByReference nelemsp, FloatByReference preemptionp);
    int nc_set_var_chunk_cache(int ncid, int varid, SizeT size, SizeT nelems, float preemption);
    int nc_get_var_chunk_cache(int ncid, int varid, SizeTByReference sizep, SizeTByReference nelemsp, FloatByReference preemptionp);
    int nc_set_log_level(int newlevel);

    // User type inquiry
    int nc_inq_compound(int ncid, int xtype, byte[] name, SizeTByReference sizep, SizeTByReference nfieldsp);
    int nc_inq_compound_field(int ncid, int xtype, int fieldid, byte[] name, SizeTByReference offsetp, IntByReference field_typeidp, IntByReference ndimsp, int[] dims);
    int nc_inq_vlen(int ncid, int xtype, byte[] name, SizeTByReference datum_sizep, IntByReference base_nc_typep);

    // Vlen specific read/write
    int nc_get_att(int ncid, int varid, String name, Vlen_t[] vlen);
    int nc_get_var(int ncid, int varid, Vlen_t[] vlen);
    int nc_get_var1(int ncid, int varid, SizeT[] indexp, Vlen_t[] vlen);
    int nc_get_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, Vlen_t[] v);
    int nc_get_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, Vlen_t[] v);

    int nc_put_att(int ncid, int varid, String attName, int xtype, SizeT len, Vlen_t[] value);
    int nc_put_var(int ncid, int varid, Vlen_t[] vlen);
    int nc_put_var1(int ncid, int varid, SizeT[] indexp, Vlen_t[] vlen);
    int nc_put_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, Vlen_t[] v);
    int nc_put_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, Vlen_t[] v);

    // Pointer based read/write for use by DAP4

    int nc_get_att(int ncid, int varid, String name, Pointer p);
    int nc_get_var(int ncid, int varid, Pointer p);
    int nc_get_var1(int ncid, int varid, SizeT[] indexp, Pointer p);
    int nc_get_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, Pointer p);
    int nc_get_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, Pointer p);

    int nc_put_att(int ncid, int varid, String attName, int xtype, SizeT len, Pointer p);
    int nc_put_var(int ncid, int varid, Pointer p);
    int nc_put_var1(int ncid, int varid, SizeT[] indexp, Pointer p);
    int nc_put_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, Pointer p);
    int nc_put_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, Pointer p);

}
