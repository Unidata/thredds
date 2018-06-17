/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib.netcdf;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import ucar.nc2.jni.netcdf.Nc4Lock;

import java.nio.ByteBuffer;

/**
 * JNA access to Netcdf-4 C Library, using JNI to shared C library.
 * Just the functions actually used.
 *
 * @author heimbigner
 * @since 4/17/2018
 */
public class DapNc4Wrapper implements DapNetcdf
{
    static int counter;

    static void ce()
    {
        if (counter != 0)
            System.err.println("XXX: ce: counter != 0\n");
        counter = 1;
    }

    static String
    cxs(String x)
    {
        if (counter != 1)
            System.err.println("XXX: cx: counter != 1\n");
        counter = 0;
        return x;
    }

    static int
    cx(int x)
    {
        if (counter != 1)
            System.err.println("XXX: cx: counter != 1\n");
        counter = 0;
        return x;
    }

    DapNetcdf nc4 = null;

    public DapNc4Wrapper(DapNetcdf nc4) {this.nc4 = nc4;}


    // library
    public String nc_inq_libvers()
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cxs(nc4.nc_inq_libvers());
        }
    }

    public String nc_strerror(int ncerr)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cxs(nc4.nc_strerror(ncerr));
        }
    }

    // dataset
    // int nc_open(const char *path, int mode, int *ncidp)
    public int nc_open(String path, int mode, IntByReference ncidp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_open(path, mode, ncidp));
        }
    }

    public int nc_close(int ncid)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_close(ncid));
        }
    }

    public int nc_inq_format(int ncid, IntByReference formatp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_format(ncid, formatp));
        }
    }

    public int nc_inq_format_extended(int ncid, IntByReference formatp, IntByReference modep)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_format_extended(ncid, formatp, modep));
        }
    }

    // groups
    public int nc_inq_grps(int ncid, IntByReference numgrps, Pointer np)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_grps(ncid, numgrps, np));
        }
    }

    public int nc_inq_grpname(int ncid, byte[] name)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_grpname(ncid, name));
        }
    }

    // Given ncid, find full name and len of full name. (Root group is
    //         * named "/", with length 1.)
    public int nc_inq_grpname_full(int ncid, SizeTByReference lenp, byte[] full_name)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_grpname_full(ncid, lenp, full_name));
        }
    }

    //Given ncid, find len of full name. 
    public int nc_inq_grpname_len(int ncid, SizeTByReference lenp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_grpname_len(ncid, lenp));
        }
    }

    // dimension info
    public int nc_inq_ndims(int ncid, IntByReference ndimsp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_ndims(ncid, ndimsp));
        }
    }

    public int nc_inq_unlimdims(int ncid, IntByReference nunlimdimsp, Pointer np)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_unlimdims(ncid, nunlimdimsp, np));
        }
    }

    public int nc_inq_dimids(int ncid, IntByReference ndims, Pointer dimids, int include_parents)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_dimids(ncid, ndims, dimids, include_parents));
        }
    }

    public int nc_inq_dim(int ncid, int dimid, byte[] name, SizeTByReference lenp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_dim(ncid, dimid, name, lenp));
        }
    }

    public int nc_inq_dimname(int ncid, int dimid, byte[] name)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_dimname(ncid, dimid, name));
        }
    }

    // attribute info
    public int nc_inq_natts(int ncid, IntByReference nattsp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_natts(ncid, nattsp));
        }
    }

    public int nc_inq_attname(int ncid, int varid, int attnum, byte[] name)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_attname(ncid, varid, attnum, name));
        }
    }

    public int nc_inq_atttype(int ncid, int varid, String name, IntByReference xtypep)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_atttype(ncid, varid, name, xtypep));
        }
    }

    public int nc_inq_attlen(int ncid, int varid, String name, SizeTByReference lenp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_attlen(ncid, varid, name, lenp));
        }
    }

    // attribute values
    public int nc_get_att_double(int ncid, int varid, String name, double[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_double(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_float(int ncid, int varid, String name, float[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_float(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_int(int ncid, int varid, String name, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_int(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_uint(int ncid, int varid, String name, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_uint(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_longlong(int ncid, int varid, String name, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_longlong(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_ulonglong(int ncid, int varid, String name, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_ulonglong(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_schar(int ncid, int varid, String name, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_schar(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_uchar(int ncid, int varid, String name, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_uchar(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_ubyte(int ncid, int varid, String name, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_ubyte(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_short(int ncid, int varid, String name, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_short(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_ushort(int ncid, int varid, String name, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_ushort(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_text(int ncid, int varid, String name, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_text(ncid, varid, name, ip));
        }
    }

    public int nc_get_att_string(int ncid, int varid, String name, String[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att_string(ncid, varid, name, ip));
        }
    }

    public int nc_get_att(int ncid, int varid, String name, Vlen_t[] vlen)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att(ncid, varid, name, vlen));
        }
    }

    public int nc_get_att(int ncid, int varid, String name, Pointer bbuff)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_att(ncid, varid, name, bbuff));
        }
    }

    // variable info
    public int nc_inq_nvars(int ncid, IntByReference nvarsp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_nvars(ncid, nvarsp));
        }
    }

    public int nc_inq_varids(int ncid, IntByReference nvars, Pointer varids)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_varids(ncid, nvars, varids));
        }
    }

    public int nc_inq_var(int ncid, int varid, byte[] name, IntByReference xtypep, IntByReference ndimsp, Pointer dimidsp, IntByReference nattsp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_var(ncid, varid, name, xtypep, ndimsp, dimidsp, nattsp));
        }
    }

    public int nc_inq_varid(int ncid, byte[] name, IntByReference varidp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_varid(ncid, name, varidp));
        }
    }

    // user types
    public int nc_inq_typeids(int ncid, IntByReference ntypes, Pointer typeids)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_typeids(ncid, ntypes, typeids));
        }
    }

    public int nc_inq_type(int ncid, int xtype, byte[] name, SizeTByReference sizep)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_type(ncid, xtype, name, sizep));
        }
    }

    public int nc_inq_user_type(int ncid, int xtype, byte[] name, SizeTByReference sizep, IntByReference baseType, SizeTByReference nfieldsp, IntByReference classp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_user_type(ncid, xtype, name, sizep, baseType, nfieldsp, classp));
        }
    }

    public int nc_inq_enum(int ncid, int xtype, byte[] name, IntByReference baseType, SizeTByReference base_sizep, SizeTByReference num_membersp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_enum(ncid, xtype, name, baseType, base_sizep, num_membersp));
        }
    }

    public int nc_inq_enum_member(int ncid, int xtype, int idx, byte[] name, IntByReference value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_enum_member(ncid, xtype, idx, name, value));
        }
    }

    public int nc_inq_opaque(int ncid, int xtype, byte[] name, SizeTByReference sizep)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_opaque(ncid, xtype, name, sizep));
        }
    }

    // compound user type
    public int nc_inq_compound(int ncid, int xtype, byte[] name, SizeTByReference sizep, SizeTByReference nfieldsp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_compound(ncid, xtype, name, sizep, nfieldsp));
        }
    }

    public int nc_inq_compound_field(int ncid, int xtype, int fieldid, byte[] name, SizeTByReference offsetp, IntByReference field_typeidp, IntByReference ndimsp, Pointer dims)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_compound_field(ncid, xtype, fieldid, name, offsetp, field_typeidp, ndimsp, dims));
        }
    }

    // vlen user type
    public int nc_inq_vlen(int ncid, int xtype, byte[] name, SizeTByReference datum_sizep, IntByReference base_nc_typep)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_vlen(ncid, xtype, name, datum_sizep, base_nc_typep));
        }
    }

    // read entire array
    public int nc_get_var(int ncid, int varid, Pointer p)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var(ncid, varid, p));
        }
    }

    public int nc_get_var_text(int ncid, int varid, byte[] op)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_text(ncid, varid, op));
        }
    }

    public int nc_get_var_schar(int ncid, int varid, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_schar(ncid, varid, ip));
        }
    }

    public int nc_get_var_ubyte(int ncid, int varid, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_ubyte(ncid, varid, ip));
        }
    }

    public int nc_get_var_short(int ncid, int varid, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_short(ncid, varid, ip));
        }
    }

    public int nc_get_var_ushort(int ncid, int varid, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_ushort(ncid, varid, ip));
        }
    }

    public int nc_get_var_int(int ncid, int varid, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_int(ncid, varid, ip));
        }
    }

    public int nc_get_var_uint(int ncid, int varid, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_uint(ncid, varid, ip));
        }
    }

    public int nc_get_var_longlong(int ncid, int varid, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_longlong(ncid, varid, ip));
        }
    }

    public int nc_get_var_ulonglong(int ncid, int varid, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_ulonglong(ncid, varid, ip));
        }
    }

    public int nc_get_var_float(int ncid, int varid, float[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_float(ncid, varid, ip));
        }
    }

    public int nc_get_var_double(int ncid, int varid, double[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_double(ncid, varid, ip));
        }
    }

    public int nc_get_var_string(int ncid, int varid, String[] sarray)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_string(ncid, varid, sarray));
        }
    }

    // read single element
    public int nc_get_var1(int ncid, int varid, SizeT[] indexp, Pointer p)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1(ncid, varid, indexp, p));
        }
    }

    public int nc_get_var1_text(int ncid, int varid, SizeT[] indexp, byte[] op)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_text(ncid, varid, indexp, op));
        }
    }

    public int nc_get_var1_schar(int ncid, int varid, SizeT[] indexp, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_schar(ncid, varid, indexp, ip));
        }
    }

    public int nc_get_var1_ubyte(int ncid, int varid, SizeT[] indexp, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_ubyte(ncid, varid, indexp, ip));
        }
    }

    public int nc_get_var1_short(int ncid, int varid, SizeT[] indexp, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_short(ncid, varid, indexp, ip));
        }
    }

    public int nc_get_var1_ushort(int ncid, int varid, SizeT[] indexp, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_ushort(ncid, varid, indexp, ip));
        }
    }

    public int nc_get_var1_int(int ncid, int varid, SizeT[] indexp, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_int(ncid, varid, indexp, ip));
        }
    }

    public int nc_get_var1_uint(int ncid, int varid, SizeT[] indexp, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_uint(ncid, varid, indexp, ip));
        }
    }

    public int nc_get_var1_longlong(int ncid, int varid, SizeT[] indexp, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_longlong(ncid, varid, indexp, ip));
        }
    }

    public int nc_get_var1_ulonglong(int ncid, int varid, SizeT[] indexp, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_ulonglong(ncid, varid, indexp, ip));
        }
    }

    public int nc_get_var1_float(int ncid, int varid, SizeT[] indexp, float[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_float(ncid, varid, indexp, ip));
        }
    }

    public int nc_get_var1_double(int ncid, int varid, SizeT[] indexp, double[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_double(ncid, varid, indexp, ip));
        }
    }

    public int nc_get_var1_string(int ncid, int varid, SizeT[] indexp, String[] sarray)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1_string(ncid, varid, indexp, sarray));
        }
    }

    // read array section

    public int nc_get_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, Pointer mem)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara(ncid, varid, startp, countp, mem));
        }
    }

    public int nc_get_vara_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_uchar(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_schar(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_text(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_short(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_ushort(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_int(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_uint(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_longlong(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_ulonglong(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, float[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_float(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, double[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_double(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vara_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, String[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vara_string(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_get_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, Pointer p)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars(ncid, varid, startp, countp, stridep, p));
        }
    }

    public int nc_get_vars_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_uchar(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_schar(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_text(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_short(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_ushort(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_int(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_uint(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_longlong(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_ulonglong(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, float[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_float(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, double[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_double(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_get_vars_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, String[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_vars_string(ncid, varid, startp, countp, stridep, ip));
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //// writing

    // Set the default nc_create format to NC_FORMAT_CLASSIC, NC_FORMAT_64BIT, NC_FORMAT_NETCDF4, NC_FORMAT_NETCDF4_CLASSIC.
    public int nc_set_default_format(int format, IntByReference old_formatp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_set_default_format(format, old_formatp));
        }
    }

    public int nc_create(String path, int cmode, IntByReference ncidp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_create(path, cmode, ncidp));
        }
    }

    public int nc_enddef(int ncid)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_enddef(ncid));
        }
    }

    public int nc_sync(int ncid)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_sync(ncid));
        }
    }

    public int nc_def_grp(int parent_ncid, String name, IntByReference new_ncid)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_def_grp(parent_ncid, name, new_ncid));
        }
    }

    public int nc_def_dim(int ncid, String name, SizeT len, IntByReference dimid)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_def_dim(ncid, name, len, dimid));
        }
    }

    public int nc_inq_dimlen(int ncid, int dimid, SizeTByReference lenp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_dimlen(ncid, dimid, lenp));
        }
    }

    public int nc_def_var(int ncid, String name, SizeT xtype, int ndims, int[] dimids, IntByReference varidp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_def_var(ncid, name, xtype, ndims, dimids, varidp));
        }
    }

    public int nc_def_compound(int ncid, SizeT size, String name, IntByReference typeidp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_def_compound(ncid, size, name, typeidp));
        }
    }

    public int nc_insert_compound(int ncid, int typeid, String name, SizeT offset, int field_typeid)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_insert_compound(ncid, typeid, name, offset, field_typeid));
        }
    }

    public int nc_insert_array_compound(int ncid, int typeid, String name, SizeT offset, int field_typeid, int ndims, int[] dim_sizes)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_insert_array_compound(ncid, typeid, name, offset, field_typeid, ndims, dim_sizes));
        }
    }

    // Create an enum type. Provide a base type and a name. At the moment
    // only ints are accepted as base types.
    public int nc_def_enum(int ncid, int base_typeid, String name, IntByReference typeidp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_def_enum(ncid, base_typeid, name, typeidp));
        }
    }

    // Insert a named value into an enum type. The value must fit within
    // the size of the enum type, the name size must be <= NC_MAX_NAME.
    public int nc_insert_enum(int ncid, int enumid, String name, IntByReference value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_insert_enum(ncid, enumid, name, value));
        }
    }

    // Rename a group
    public int nc_rename_grp(int grpid, String name)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_rename_grp(grpid, name));
        }
    }

    // Write entire var of any type.
    public int nc_put_var(int ncid, int varid, byte[] bbuff)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var(ncid, varid, bbuff));
        }
    }

    // write array section
    public int nc_put_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] bbuff)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara(ncid, varid, startp, countp, bbuff));
        }
    }

    public int nc_put_vara_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_uchar(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_put_vara_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_schar(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_put_vara_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_text(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_put_vara_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_short(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_put_vara_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_ushort(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_put_vara_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_int(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_put_vara_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_uint(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_put_vara_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_longlong(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_put_vara_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_ulonglong(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_put_vara_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, float[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_float(ncid, varid, startp, countp, ip));
        }
    }

    public int nc_put_vara_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, double[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_double(ncid, varid, startp, countp, ip));
        }
    }

    /*int nc_get_vara_string(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, char **ip);  */
    public int nc_put_vara_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, String[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vara_string(ncid, varid, startp, countp, stridep, ip));
        }
    }


    //int nc_get_vara_string(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, char **ip);

    //     nc_put_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] bbuff);
    public int nc_put_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] bbuff)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars(ncid, varid, startp, countp, stridep, bbuff));
        }
    }

    // nc_put_vars_uchar(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, const unsigned char *op)
    public int nc_put_vars_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] op)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_uchar(ncid, varid, startp, countp, stridep, op));
        }
    }

    public int nc_put_vars_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_schar(ncid, varid, startp, countp, stridep, ip));
        }
    }

    // nc_put_vars_text(ncid, varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, const char *op)
    public int nc_put_vars_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_text(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_put_vars_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_short(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_put_vars_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_ushort(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_put_vars_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_int(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_put_vars_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_uint(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_put_vars_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_longlong(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_put_vars_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_ulonglong(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_put_vars_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, float[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_float(ncid, varid, startp, countp, stridep, ip));
        }
    }

    // nc_put_vars_double(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, const double *op)
    public int nc_put_vars_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, double[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_double(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_put_vars_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, String[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_vars_string(ncid, varid, startp, countp, stridep, ip));
        }
    }

    public int nc_put_var_uchar(int ncid, int varid, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_uchar(ncid, varid, ip));
        }
    }

    public int nc_put_var_schar(int ncid, int varid, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_schar(ncid, varid, ip));
        }
    }

    public int nc_put_var_text(int ncid, int varid, byte[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_text(ncid, varid, ip));
        }
    }

    public int nc_put_var_short(int ncid, int varid, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_short(ncid, varid, ip));
        }
    }

    public int nc_put_var_ushort(int ncid, int varid, short[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_ushort(ncid, varid, ip));
        }
    }

    public int nc_put_var_int(int ncid, int varid, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_int(ncid, varid, ip));
        }
    }

    public int nc_put_var_uint(int ncid, int varid, int[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_uint(ncid, varid, ip));
        }
    }

    public int nc_put_var_longlong(int ncid, int varid, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_longlong(ncid, varid, ip));
        }
    }

    public int nc_put_var_ulonglong(int ncid, int varid, long[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_ulonglong(ncid, varid, ip));
        }
    }

    public int nc_put_var_float(int ncid, int varid, float[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_float(ncid, varid, ip));
        }
    }

    public int nc_put_var_double(int ncid, int varid, double[] ip)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_double(ncid, varid, ip));
        }
    }

    public int nc_put_var_string(int ncid, int varid, String[] op)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_var_string(ncid, varid, op));
        }
    }

    // write attributes
    public int nc_put_att(int ncid, int varid, String name, int xtype, SizeT len, byte[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att(ncid, varid, name, xtype, len, value));
        }
    }

    public int nc_put_att_string(int ncid, int varid, String attName, SizeT len, String[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_string(ncid, varid, attName, len, value));
        }
    }

    public int nc_put_att_text(int ncid, int varid, String attName, SizeT len, byte[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_text(ncid, varid, attName, len, value));
        }
    }

    public int nc_put_att_uchar(int ncid, int varid, String attName, int xtype, SizeT len, byte[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_uchar(ncid, varid, attName, xtype, len, value));
        }
    }

    public int nc_put_att_schar(int ncid, int varid, String attName, int xtype, SizeT len, byte[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_schar(ncid, varid, attName, xtype, len, value));
        }
    }

    public int nc_put_att_short(int ncid, int varid, String attName, int xtype, SizeT len, short[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_short(ncid, varid, attName, xtype, len, value));
        }
    }

    public int nc_put_att_ushort(int ncid, int varid, String attName, int xtype, SizeT len, short[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_ushort(ncid, varid, attName, xtype, len, value));
        }
    }

    public int nc_put_att_int(int ncid, int varid, String attName, int xtype, SizeT len, int[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_int(ncid, varid, attName, xtype, len, value));
        }
    }

    public int nc_put_att_uint(int ncid, int varid, String attName, int xtype, SizeT len, int[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_uint(ncid, varid, attName, xtype, len, value));
        }
    }

    public int nc_put_att_longlong(int ncid, int varid, String attName, int xtype, SizeT len, long[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_longlong(ncid, varid, attName, xtype, len, value));
        }
    }

    public int nc_put_att_ulonglong(int ncid, int varid, String attName, int xtype, SizeT len, long[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_ulonglong(ncid, varid, attName, xtype, len, value));
        }
    }

    public int nc_put_att_float(int ncid, int varid, String attName, int xtype, SizeT len, float[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_float(ncid, varid, attName, xtype, len, value));
        }
    }

    public int nc_put_att_double(int ncid, int varid, String attName, int xtype, SizeT len, double[] value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_put_att_double(ncid, varid, attName, xtype, len, value));
        }
    }

    // Vlen-specialized Read/write
    public int nc_get_var(int ncid, int varid, Vlen_t[] vlen)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var(ncid, varid, vlen));
        }
    }

    public int nc_get_var1(int ncid, int varid, SizeT[] indexp, Vlen_t[] vlen)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var1(ncid, varid, indexp, vlen));
        }
    }

    // Extra netcdf-4 stuff.

    // Set compression settings for a variable. Lower is faster, higher is better.
    // Must be called after nc_def_var andbefore nc_enddef

    public int nc_def_var_deflate(int ncid, int varid, int shuffle, int deflate, int deflate_level)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_def_var_deflate(ncid, varid, shuffle, deflate, deflate_level));
        }
    }

    // Find out compression settings of a var.
    public int nc_inq_var_deflate(int ncid, int varid, IntByReference shufflep, IntByReference deflatep, IntByReference deflate_levelp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_var_deflate(ncid, varid, shufflep, deflatep, deflate_levelp));
        }
    }

    // Find out szip settings of a var.
    public int nc_inq_var_szip(int ncid, int varid, IntByReference options_maskp, IntByReference pixels_per_blockp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_var_szip(ncid, varid, options_maskp, pixels_per_blockp));
        }
    }

    // Set fletcher32 checksum for a var. This must be done after nc_def_var and before nc_enddef.
    public int nc_def_var_fletcher32(int ncid, int varid, int fletcher32)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_def_var_fletcher32(ncid, varid, fletcher32));
        }
    }

    // Inquire about fletcher32 checksum for a var.
    public int nc_inq_var_fletcher32(int ncid, int varid, IntByReference fletcher32p)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_var_fletcher32(ncid, varid, fletcher32p));
        }
    }

    // Define chunking for a variable. This must be done after nc_def_var and before nc_enddef.
    // nc_def_var_chunking(int ncid, int varid, int storage, const size_t *chunksizesp)
    // nc_def_var_chunking(int ncid, int varid, int storage, const size_t *chunksizesp));}}
    public int nc_def_var_chunking(int ncid, int varid, int storage, SizeT[] chunksizesp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_def_var_chunking(ncid, varid, storage, chunksizesp));
        }
    }

    // Inq chunking stuff for a var.
    public int nc_inq_var_chunking(int ncid, int varid, IntByReference storagep, SizeT[] chunksizesp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_var_chunking(ncid, varid, storagep, chunksizesp));
        }
    }

    // Define fill value behavior for a variable. This must be done after nc_def_var and before nc_enddef.
    public int nc_def_var_fill(int ncid, int varid, int no_fill, byte[] fill_value)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_def_var_fill(ncid, varid, no_fill, fill_value));
        }
    }

    // Inq fill value setting for a var.
    public int nc_inq_var_fill(int ncid, int varid, IntByReference no_fill, byte[] fill_valuep)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_var_fill(ncid, varid, no_fill, fill_valuep));
        }
    }

    // Define the endianness of a variable.
    public int nc_def_var_endian(int ncid, int varid, int endian)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_def_var_endian(ncid, varid, endian));
        }
    }

    // Learn about the endianness of a variable.
    public int nc_inq_var_endian(int ncid, int varid, IntByReference endianp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_inq_var_endian(ncid, varid, endianp));
        }
    }

    // Set the fill mode (classic or 64-bit offset files only).
    public int nc_set_fill(int ncid, int fillmode, IntByReference old_modep)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_set_fill(ncid, fillmode, old_modep));
        }
    }

    // Set the cache size, nelems, and preemption policy.
    public int nc_set_chunk_cache(SizeT size, SizeT nelems, float preemption)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_set_chunk_cache(size, nelems, preemption));
        }
    }

    // Get the cache size, nelems, and preemption policy.
    public int nc_get_chunk_cache(SizeTByReference sizep, SizeTByReference nelemsp, FloatByReference preemptionp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_chunk_cache(sizep, nelemsp, preemptionp));
        }
    }

    // Set the per-variable cache size, nelems, and preemption policy.
    public int nc_set_var_chunk_cache(int ncid, int varid, SizeT size, SizeT nelems, float preemption)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_set_var_chunk_cache(ncid, varid, size, nelems, preemption));
        }
    }

    // Set the per-variable cache size, nelems, and preemption policy.
    public int nc_get_var_chunk_cache(int ncid, int varid, SizeTByReference sizep, SizeTByReference nelemsp, FloatByReference preemptionp)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_get_var_chunk_cache(ncid, varid, sizep, nelemsp, preemptionp));
        }
    }

    // Set the log level
    public int nc_set_log_level(int newlevel)
    {
        synchronized (Nc4Lock.class) {
            ce();
            return cx(nc4.nc_set_log_level(newlevel));
        }
    }
}
