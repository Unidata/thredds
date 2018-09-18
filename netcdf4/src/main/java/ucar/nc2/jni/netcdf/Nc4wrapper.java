/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;

/**
 * JNA access to Netcdf-4 C Library, using JNI to shared C library.
 * Just the functions actually used.
 *
 * I suspect that this whole construct:
 *  public synchronized
 *      String {function}() {
 *          ce();
 *          String ret = nc4.{function}();
 *          cx();
 *          return ret;
 *      }
 * can be significantly simplified in Java 9.
 *
 * @author dmh
 * @since June 11, 2018
 */
public class Nc4wrapper implements Nc4prototypes
{

    static public boolean TRACE = false;

    static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Nc4wrapper.class);

    static int counter;

    static protected void trace(Object ret, String fcn, Object... args)
    {
	if(!TRACE) return;
	StringBuilder sargs = new StringBuilder("(");
	for(int i=0;i<args.length;i++) {
	    Object o = args[i];
	    if(i > 0) sargs.append(",");
	    sargs.append(o==null?"null":o.toString());
	}
	sargs.append(")");
	log.info(String.format("trace: %s ret=%s args=%s",
		 fcn, ret.toString(), sargs.toString()));
    }

    static protected void err(String cc, int c) {log.error("Serial failure: "+cc+": counter != "+c+"\n");}

    static protected void ce() {if(counter != 0) err("ce",0); counter = 1;}
    static protected void cx() {if(counter != 1) err("cx",1); counter=0;};

    private Nc4prototypes nc4 = null;

    public Nc4wrapper(Nc4prototypes nc4) {this.nc4 = nc4;}

    // Begin API Override

    @Override
    public synchronized
    String nc_inq_libvers() {
    String ret;
    try {ce();
    ret = nc4.nc_inq_libvers();
    if(TRACE) trace(ret,"nc_inq_libvers","-"); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    String nc_strerror(int ncerr) {
    String ret;
    try {ce();
    ret = nc4.nc_strerror(ncerr);
    if(TRACE) trace(ret,"nc_strerror",ncerr); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_open(String path, int mode, IntByReference ncidp) {
    int ret;
    try {ce();
    ret = nc4.nc_open(path,mode, ncidp);
    if(TRACE) trace(ret,"nc_open",path,mode, ncidp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_close(int ncid) {
    int ret;
    try {ce();
    ret = nc4.nc_close(ncid);
    if(TRACE) trace(ret,"nc_close",ncid); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_format(int ncid, IntByReference formatp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_format(ncid, formatp);
    if(TRACE) trace(ret,"nc_inq_format",ncid, formatp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_format_extended(int ncid, IntByReference formatp, IntByReference modep) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_format_extended(ncid, formatp, modep);
    if(TRACE) trace(ret,"nc_inq_format_extended",ncid, formatp, modep); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_grps(int ncid, IntByReference numgrps, int[] ncids) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_grps(ncid, numgrps, ncids);
    if(TRACE) trace(ret,"nc_inq_grps",ncid, numgrps, ncids); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_grpname(int ncid, byte[] name) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_grpname(ncid, name);
    if(TRACE) trace(ret,"nc_inq_grpname",ncid, name); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_grpname_full(int ncid, SizeTByReference lenp, byte[] full_name) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_grpname_full(ncid, lenp, full_name);
    if(TRACE) trace(ret,"nc_inq_grpname_full",ncid, lenp, full_name); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_grpname_len(int ncid, SizeTByReference lenp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_grpname_len(ncid, lenp);
    if(TRACE) trace(ret,"nc_inq_grpname_len",ncid, lenp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_ndims(int ncid, IntByReference ndimsp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_ndims(ncid, ndimsp);
    if(TRACE) trace(ret,"nc_inq_ndims",ncid, ndimsp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_unlimdims(int ncid, IntByReference nunlimdimsp, int[] unlimdimidsp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_unlimdims(ncid, nunlimdimsp, unlimdimidsp);
    if(TRACE) trace(ret,"nc_inq_unlimdims",ncid, nunlimdimsp, unlimdimidsp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_dimids(int ncid, IntByReference ndims, int[] dimids, int include_parents) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_dimids(ncid, ndims, dimids,include_parents);
    if(TRACE) trace(ret,"nc_inq_dimids",ncid, ndims, dimids,include_parents); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_dim(int ncid, int dimid, byte[] name, SizeTByReference lenp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_dim(ncid,dimid, name, lenp);
    if(TRACE) trace(ret,"nc_inq_dim",ncid,dimid, name, lenp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_dimname(int ncid, int dimid, byte[] name) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_dimname(ncid,dimid, name);
    if(TRACE) trace(ret,"nc_inq_dimname",ncid,dimid, name); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_natts(int ncid, IntByReference nattsp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_natts(ncid, nattsp);
    if(TRACE) trace(ret,"nc_inq_natts",ncid, nattsp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_attname(int ncid, int varid, int attnum, byte[] name) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_attname(ncid,varid,attnum, name);
    if(TRACE) trace(ret,"nc_inq_attname",ncid,varid,attnum, name); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_atttype(int ncid, int varid, String name, IntByReference xtypep) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_atttype(ncid,varid,name, xtypep);
    if(TRACE) trace(ret,"nc_inq_atttype",ncid,varid,name, xtypep); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_attlen(int ncid, int varid, String name, SizeTByReference lenp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_attlen(ncid,varid,name, lenp);
    if(TRACE) trace(ret,"nc_inq_attlen",ncid,varid,name, lenp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_double(int ncid, int varid, String name, double[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_double(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_double",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_float(int ncid, int varid, String name, float[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_float(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_float",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_int(int ncid, int varid, String name, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_int(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_int",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_uint(int ncid, int varid, String name, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_uint(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_uint",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_longlong(int ncid, int varid, String name, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_longlong(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_longlong",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_ulonglong(int ncid, int varid, String name, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_ulonglong(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_ulonglong",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_schar(int ncid, int varid, String name, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_schar(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_schar",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_uchar(int ncid, int varid, String name, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_uchar(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_uchar",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_ubyte(int ncid, int varid, String name, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_ubyte(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_ubyte",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_short(int ncid, int varid, String name, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_short(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_short",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_ushort(int ncid, int varid, String name, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_ushort(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_ushort",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_text(int ncid, int varid, String name, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_text(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_text",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att_string(int ncid, int varid, String name, String[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att_string(ncid,varid,name, ip);
    if(TRACE) trace(ret,"nc_get_att_string",ncid,varid,name, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att(int ncid, int varid, String name, byte[] bbuff) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att(ncid,varid,name, bbuff);
    if(TRACE) trace(ret,"nc_get_att",ncid,varid,name, bbuff); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_nvars(int ncid, IntByReference nvarsp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_nvars(ncid, nvarsp);
    if(TRACE) trace(ret,"nc_inq_nvars",ncid, nvarsp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_varids(int ncid, IntByReference nvars, int[] varids) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_varids(ncid, nvars, varids);
    if(TRACE) trace(ret,"nc_inq_varids",ncid, nvars, varids); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_var(int ncid, int varid, byte[] name, IntByReference xtypep, IntByReference ndimsp, int[] dimidsp, IntByReference nattsp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_var(ncid,varid, name, xtypep, ndimsp, dimidsp, nattsp);
    if(TRACE) trace(ret,"nc_inq_var",ncid,varid, name, xtypep, ndimsp, dimidsp, nattsp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_varid(int ncid, byte[] name, IntByReference varidp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_varid(ncid, name, varidp);
    if(TRACE) trace(ret,"nc_inq_varid",ncid, name, varidp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_vardimid(int ncid, int varid, int[] dimidsp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_vardimid(ncid,varid, dimidsp);
    if(TRACE) trace(ret,"nc_inq_vardimid",ncid,varid, dimidsp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_varnatts(int ncid, int varid, IntByReference nattsp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_varnatts(ncid,varid, nattsp);
    if(TRACE) trace(ret,"nc_inq_varnatts",ncid,varid, nattsp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_typeids(int ncid, IntByReference ntypes, int[] typeids) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_typeids(ncid, ntypes, typeids);
    if(TRACE) trace(ret,"nc_inq_typeids",ncid, ntypes, typeids); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_type(int ncid, int xtype, byte[] name, SizeTByReference sizep) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_type(ncid,xtype, name, sizep);
    if(TRACE) trace(ret,"nc_inq_type",ncid,xtype, name, sizep); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_user_type(int ncid, int xtype, byte[] name, SizeTByReference sizep, IntByReference baseType, SizeTByReference nfieldsp, IntByReference classp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_user_type(ncid,xtype, name, sizep, baseType, nfieldsp, classp);
    if(TRACE) trace(ret,"nc_inq_user_type",ncid,xtype, name, sizep, baseType, nfieldsp, classp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_enum(int ncid, int xtype, byte[] name, IntByReference baseType, SizeTByReference base_sizep, SizeTByReference num_membersp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_enum(ncid,xtype, name, baseType, base_sizep, num_membersp);
    if(TRACE) trace(ret,"nc_inq_enum",ncid,xtype, name, baseType, base_sizep, num_membersp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_enum_member(int ncid, int xtype, int idx, byte[] name, IntByReference value) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_enum_member(ncid,xtype,idx, name, value);
    if(TRACE) trace(ret,"nc_inq_enum_member",ncid,xtype,idx, name, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_opaque(int ncid, int xtype, byte[] name, SizeTByReference sizep) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_opaque(ncid,xtype, name, sizep);
    if(TRACE) trace(ret,"nc_inq_opaque",ncid,xtype, name, sizep); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var(int ncid, int varid, byte[] buf) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var(ncid,varid, buf);
    if(TRACE) trace(ret,"nc_get_var",ncid,varid, buf); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_text(int ncid, int varid, byte[] op) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_text(ncid,varid, op);
    if(TRACE) trace(ret,"nc_get_var_text",ncid,varid, op); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_schar(int ncid, int varid, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_schar(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_get_var_schar",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_ubyte(int ncid, int varid,  byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_ubyte(ncid,varid,  ip);
    if(TRACE) trace(ret,"nc_get_var_ubyte",ncid,varid,  ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_short(int ncid, int varid, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_short(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_get_var_short",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_ushort(int ncid, int varid, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_ushort(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_get_var_ushort",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_int(int ncid, int varid, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_int(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_get_var_int",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_uint(int ncid, int varid, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_uint(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_get_var_uint",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_longlong(int ncid, int varid, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_longlong(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_get_var_longlong",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_ulonglong(int ncid, int varid, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_ulonglong(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_get_var_ulonglong",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_float(int ncid, int varid, float[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_float(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_get_var_float",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_double(int ncid, int varid, double[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_double(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_get_var_double",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_string(int ncid, int varid, String[] sarray) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_string(ncid,varid, sarray);
    if(TRACE) trace(ret,"nc_get_var_string",ncid,varid, sarray); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1(int ncid, int varid, SizeT[] indexp, byte[] buf) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1(ncid,varid, indexp, buf);
    if(TRACE) trace(ret,"nc_get_var1",ncid,varid, indexp, buf); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_text(int ncid, int varid, SizeT[] indexp, byte[] op) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_text(ncid,varid, indexp, op);
    if(TRACE) trace(ret,"nc_get_var1_text",ncid,varid, indexp, op); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_schar(int ncid, int varid, SizeT[] indexp, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_schar(ncid,varid, indexp, ip);
    if(TRACE) trace(ret,"nc_get_var1_schar",ncid,varid, indexp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_ubyte(int ncid, int varid, SizeT[] indexp, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_ubyte(ncid,varid, indexp, ip);
    if(TRACE) trace(ret,"nc_get_var1_ubyte",ncid,varid, indexp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_short(int ncid, int varid, SizeT[] indexp, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_short(ncid,varid, indexp, ip);
    if(TRACE) trace(ret,"nc_get_var1_short",ncid,varid, indexp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_ushort(int ncid, int varid, SizeT[] indexp, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_ushort(ncid,varid, indexp, ip);
    if(TRACE) trace(ret,"nc_get_var1_ushort",ncid,varid, indexp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_int(int ncid, int varid, SizeT[] indexp, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_int(ncid,varid, indexp, ip);
    if(TRACE) trace(ret,"nc_get_var1_int",ncid,varid, indexp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_uint(int ncid, int varid, SizeT[] indexp, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_uint(ncid,varid, indexp, ip);
    if(TRACE) trace(ret,"nc_get_var1_uint",ncid,varid, indexp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_longlong(int ncid, int varid, SizeT[] indexp, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_longlong(ncid,varid, indexp, ip);
    if(TRACE) trace(ret,"nc_get_var1_longlong",ncid,varid, indexp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_ulonglong(int ncid, int varid, SizeT[] indexp, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_ulonglong(ncid,varid, indexp, ip);
    if(TRACE) trace(ret,"nc_get_var1_ulonglong",ncid,varid, indexp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_float(int ncid, int varid, SizeT[] indexp, float[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_float(ncid,varid, indexp, ip);
    if(TRACE) trace(ret,"nc_get_var1_float",ncid,varid, indexp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_double(int ncid, int varid, SizeT[] indexp, double[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_double(ncid,varid, indexp, ip);
    if(TRACE) trace(ret,"nc_get_var1_double",ncid,varid, indexp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1_string(int ncid, int varid, SizeT[] indexp, String[] sarray) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1_string(ncid,varid, indexp, sarray);
    if(TRACE) trace(ret,"nc_get_var1_string",ncid,varid, indexp, sarray); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] buf) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara(ncid,varid, startp, countp, buf);
    if(TRACE) trace(ret,"nc_get_vara",ncid,varid, startp, countp, buf); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_uchar(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_uchar",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_schar(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_schar",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_text(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_text",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_short(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_short",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_ushort(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_ushort",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_int(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_int",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_uint(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_uint",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_longlong(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_longlong",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_ulonglong(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_ulonglong",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, float[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_float(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_float",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, double[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_double(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_double",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, String[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara_string(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_get_vara_string",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] buf) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars(ncid,varid, startp, countp, stridep, buf);
    if(TRACE) trace(ret,"nc_get_vars",ncid,varid, startp, countp, stridep, buf); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_uchar(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_uchar",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_schar(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_schar",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_text(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_text",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_short(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_short",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_ushort(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_ushort",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_int(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_int",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_uint(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_uint",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_longlong(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_longlong",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_ulonglong(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_ulonglong",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, float[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_float(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_float",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, double[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_double(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_double",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, String[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars_string(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_get_vars_string",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_set_default_format(int format, IntByReference old_formatp) {
    int ret;
    try {ce();
    ret = nc4.nc_set_default_format(format, old_formatp);
    if(TRACE) trace(ret,"nc_set_default_format",format, old_formatp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_create(String path, int cmode, IntByReference ncidp) {
    int ret;
    try {ce();
    ret = nc4.nc_create(path,cmode, ncidp);
    if(TRACE) trace(ret,"nc_create",path,cmode, ncidp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_enddef   (int ncid) {
    int ret;
    try {ce();
    ret = nc4.nc_enddef   (ncid);
    if(TRACE) trace(ret,"nc_enddef   ",ncid); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_sync     (int ncid) {
    int ret;
    try {ce();
    ret = nc4.nc_sync     (ncid);
    if(TRACE) trace(ret,"nc_sync     ",ncid); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_def_grp (int parent_ncid, String name, IntByReference new_ncid) {
    int ret;
    try {ce();
    ret = nc4.nc_def_grp (parent_ncid,name, new_ncid);
    if(TRACE) trace(ret,"nc_def_grp ",parent_ncid,name, new_ncid); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_def_dim(int ncid,  String name, SizeT len, IntByReference dimid) {
    int ret;
    try {ce();
    ret = nc4.nc_def_dim(ncid,name,len, dimid);
    if(TRACE) trace(ret,"nc_def_dim",ncid,name,len, dimid); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_dimlen(int ncid, int dimid, SizeTByReference lenp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_dimlen(ncid,dimid, lenp);
    if(TRACE) trace(ret,"nc_inq_dimlen",ncid,dimid, lenp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_def_var (int ncid, String name, SizeT xtype, int ndims, int[] dimids, IntByReference varidp) {
    int ret;
    try {ce();
    ret = nc4.nc_def_var (ncid,name,xtype,ndims, dimids, varidp);
    if(TRACE) trace(ret,"nc_def_var ",ncid,name,xtype,ndims, dimids, varidp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_def_compound(int ncid, SizeT size, String name, IntByReference typeidp) {
    int ret;
    try {ce();
    ret = nc4.nc_def_compound(ncid,size,name, typeidp);
    if(TRACE) trace(ret,"nc_def_compound",ncid,size,name, typeidp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_insert_compound(int ncid, int typeid, String name, SizeT offset, int field_typeid) {
    int ret;
    try {ce();
    ret = nc4.nc_insert_compound(ncid,typeid,name,offset,field_typeid);
    if(TRACE) trace(ret,"nc_insert_compound",ncid,typeid,name,offset,field_typeid); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_insert_array_compound(int ncid, int typeid, String name, SizeT offset, int field_typeid, int ndims, int[] dim_sizes) {
    int ret;
    try {ce();
    ret = nc4.nc_insert_array_compound(ncid,typeid,name,offset,field_typeid,ndims, dim_sizes);
    if(TRACE) trace(ret,"nc_insert_array_compound",ncid,typeid,name,offset,field_typeid,ndims, dim_sizes); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_def_enum(int ncid, int base_typeid, String name, IntByReference typeidp) {
    int ret;
    try {ce();
    ret = nc4.nc_def_enum(ncid,base_typeid,name, typeidp);
    if(TRACE) trace(ret,"nc_def_enum",ncid,base_typeid,name, typeidp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_insert_enum(int ncid, int enumid, String name, IntByReference value) {
    int ret;
    try {ce();
    ret = nc4.nc_insert_enum(ncid,enumid,name, value);
    if(TRACE) trace(ret,"nc_insert_enum",ncid,enumid,name, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_rename_grp(int grpid, String name) {
    int ret;
    try {ce();
    ret = nc4.nc_rename_grp(grpid,name);
    if(TRACE) trace(ret,"nc_rename_grp",grpid,name); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var(int ncid, int varid, byte[] bbuff) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var(ncid,varid, bbuff);
    if(TRACE) trace(ret,"nc_put_var",ncid,varid, bbuff); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] bbuff) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara(ncid,varid, startp, countp, bbuff);
    if(TRACE) trace(ret,"nc_put_vara",ncid,varid, startp, countp, bbuff); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_uchar(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_uchar",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_schar(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_schar",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_text(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_text",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_short(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_short",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_ushort(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_ushort",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_int(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_int",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_uint(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_uint",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_longlong(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_longlong",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_ulonglong(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_ulonglong",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, float[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_float(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_float",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, double[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_double(ncid,varid, startp, countp, ip);
    if(TRACE) trace(ret,"nc_put_vara_double",ncid,varid, startp, countp, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, String[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara_string(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vara_string",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[]  bbuff) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars(ncid,varid, startp, countp, stridep, bbuff);
    if(TRACE) trace(ret,"nc_put_vars",ncid,varid, startp, countp, stridep, bbuff); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_uchar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_uchar(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_uchar",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_schar(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_schar(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_schar",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_text(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_text(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_text",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_short(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_short(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_short",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_ushort(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_ushort(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_ushort",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_int(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_int(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_int",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_uint(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_uint(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_uint",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_longlong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_longlong(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_longlong",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_ulonglong(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_ulonglong(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_ulonglong",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_float(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, float[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_float(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_float",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_double(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, double[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_double(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_double",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars_string(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, String[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars_string(ncid,varid, startp, countp, stridep, ip);
    if(TRACE) trace(ret,"nc_put_vars_string",ncid,varid, startp, countp, stridep, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_uchar(int ncid, int varid, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_uchar(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_put_var_uchar",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_schar(int ncid, int varid,  byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_schar(ncid,varid,  ip);
    if(TRACE) trace(ret,"nc_put_var_schar",ncid,varid,  ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_text(int ncid, int varid, byte[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_text(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_put_var_text",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_short(int ncid, int varid, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_short(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_put_var_short",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_ushort(int ncid, int varid, short[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_ushort(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_put_var_ushort",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_int(int ncid, int varid, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_int(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_put_var_int",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_uint(int ncid, int varid, int[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_uint(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_put_var_uint",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_longlong(int ncid, int varid, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_longlong(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_put_var_longlong",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_ulonglong(int ncid, int varid, long[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_ulonglong(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_put_var_ulonglong",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_float(int ncid, int varid, float[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_float(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_put_var_float",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_double(int ncid, int varid, double[] ip) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_double(ncid,varid, ip);
    if(TRACE) trace(ret,"nc_put_var_double",ncid,varid, ip); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var_string(int ncid, int varid, String[] op) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var_string(ncid,varid, op);
    if(TRACE) trace(ret,"nc_put_var_string",ncid,varid, op); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att (int ncid, int varid, String name, int xtype, SizeT len, byte[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att (ncid,varid,name,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att ",ncid,varid,name,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_string(int ncid, int varid, String attName, SizeT len, String[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_string(ncid,varid,attName,len, value);
    if(TRACE) trace(ret,"nc_put_att_string",ncid,varid,attName,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_text(int ncid, int varid, String attName, SizeT len, byte[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_text(ncid,varid,attName,len, value);
    if(TRACE) trace(ret,"nc_put_att_text",ncid,varid,attName,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_uchar(int ncid, int varid, String attName, int xtype, SizeT len, byte[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_uchar(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att_uchar",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_schar(int ncid, int varid, String attName, int xtype, SizeT len, byte[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_schar(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att_schar",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_short(int ncid, int varid, String attName, int xtype, SizeT len, short[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_short(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att_short",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_ushort(int ncid, int varid, String attName, int xtype, SizeT len, short[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_ushort(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att_ushort",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_int(int ncid, int varid, String attName, int xtype, SizeT len, int[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_int(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att_int",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_uint(int ncid, int varid, String attName, int xtype, SizeT len, int[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_uint(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att_uint",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_longlong(int ncid, int varid, String attName, int xtype, SizeT len, long[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_longlong(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att_longlong",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_ulonglong(int ncid, int varid, String attName, int xtype, SizeT len, long[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_ulonglong(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att_ulonglong",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_float(int ncid, int varid, String attName, int xtype, SizeT len, float[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_float(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att_float",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att_double(int ncid, int varid, String attName, int xtype, SizeT len, double[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att_double(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att_double",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_def_var_deflate(int ncid, int varid, int shuffle, int deflate, int deflate_level) {
    int ret;
    try {ce();
    ret = nc4.nc_def_var_deflate(ncid,varid,shuffle,deflate,deflate_level);
    if(TRACE) trace(ret,"nc_def_var_deflate",ncid,varid,shuffle,deflate,deflate_level); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_var_deflate(int ncid, int varid, IntByReference shufflep, IntByReference deflatep, IntByReference deflate_levelp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_var_deflate(ncid,varid, shufflep, deflatep, deflate_levelp);
    if(TRACE) trace(ret,"nc_inq_var_deflate",ncid,varid, shufflep, deflatep, deflate_levelp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_var_szip(int ncid, int varid, IntByReference options_maskp, IntByReference pixels_per_blockp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_var_szip(ncid,varid, options_maskp, pixels_per_blockp);
    if(TRACE) trace(ret,"nc_inq_var_szip",ncid,varid, options_maskp, pixels_per_blockp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_def_var_fletcher32(int ncid, int varid, int fletcher32) {
    int ret;
    try {ce();
    ret = nc4.nc_def_var_fletcher32(ncid,varid,fletcher32);
    if(TRACE) trace(ret,"nc_def_var_fletcher32",ncid,varid,fletcher32); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_var_fletcher32(int ncid, int varid, IntByReference fletcher32p) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_var_fletcher32(ncid,varid, fletcher32p);
    if(TRACE) trace(ret,"nc_inq_var_fletcher32",ncid,varid, fletcher32p); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_def_var_chunking(int ncid, int varid, int storage, SizeT[] chunksizesp) {
    int ret;
    try {ce();
    ret = nc4.nc_def_var_chunking(ncid,varid,storage, chunksizesp);
    if(TRACE) trace(ret,"nc_def_var_chunking",ncid,varid,storage, chunksizesp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_var_chunking(int ncid, int varid, IntByReference storagep, SizeT[] chunksizesp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_var_chunking(ncid,varid, storagep, chunksizesp);
    if(TRACE) trace(ret,"nc_inq_var_chunking",ncid,varid, storagep, chunksizesp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_def_var_fill(int ncid, int varid, int no_fill, byte[] fill_value) {
    int ret;
    try {ce();
    ret = nc4.nc_def_var_fill(ncid,varid,no_fill, fill_value);
    if(TRACE) trace(ret,"nc_def_var_fill",ncid,varid,no_fill, fill_value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_var_fill(int ncid, int varid, IntByReference no_fill, byte[] fill_valuep) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_var_fill(ncid,varid, no_fill, fill_valuep);
    if(TRACE) trace(ret,"nc_inq_var_fill",ncid,varid, no_fill, fill_valuep); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_def_var_endian(int ncid, int varid, int endian) {
    int ret;
    try {ce();
    ret = nc4.nc_def_var_endian(ncid,varid,endian);
    if(TRACE) trace(ret,"nc_def_var_endian",ncid,varid,endian); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_var_endian(int ncid, int varid, IntByReference endianp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_var_endian(ncid,varid, endianp);
    if(TRACE) trace(ret,"nc_inq_var_endian",ncid,varid, endianp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_set_fill(int ncid, int fillmode, IntByReference old_modep) {
    int ret;
    try {ce();
    ret = nc4.nc_set_fill(ncid,fillmode, old_modep);
    if(TRACE) trace(ret,"nc_set_fill",ncid,fillmode, old_modep); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_set_chunk_cache(SizeT size, SizeT nelems, float preemption) {
    int ret;
    try {ce();
    ret = nc4.nc_set_chunk_cache(size,nelems,preemption);
    if(TRACE) trace(ret,"nc_set_chunk_cache",size,nelems,preemption); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_chunk_cache(SizeTByReference sizep, SizeTByReference nelemsp, FloatByReference preemptionp) {
    int ret;
    try {ce();
    ret = nc4.nc_get_chunk_cache(sizep, nelemsp, preemptionp);
    if(TRACE) trace(ret,"nc_get_chunk_cache",sizep, nelemsp, preemptionp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_set_var_chunk_cache(int ncid, int varid, SizeT size, SizeT nelems, float preemption) {
    int ret;
    try {ce();
    ret = nc4.nc_set_var_chunk_cache(ncid,varid,size,nelems,preemption);
    if(TRACE) trace(ret,"nc_set_var_chunk_cache",ncid,varid,size,nelems,preemption); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var_chunk_cache(int ncid, int varid, SizeTByReference sizep, SizeTByReference nelemsp, FloatByReference preemptionp) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var_chunk_cache(ncid,varid, sizep, nelemsp, preemptionp);
    if(TRACE) trace(ret,"nc_get_var_chunk_cache",ncid,varid, sizep, nelemsp, preemptionp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_set_log_level(int newlevel) {
    int ret;
    try {ce();
    ret = nc4.nc_set_log_level(newlevel);
    if(TRACE) trace(ret,"nc_set_log_level",newlevel); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_compound(int ncid, int xtype, byte[] name, SizeTByReference sizep, SizeTByReference nfieldsp) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_compound(ncid,xtype, name, sizep, nfieldsp);
    if(TRACE) trace(ret,"nc_inq_compound",ncid,xtype, name, sizep, nfieldsp); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_compound_field(int ncid, int xtype, int fieldid, byte[] name, SizeTByReference offsetp, IntByReference field_typeidp, IntByReference ndimsp, int[] dims) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_compound_field(ncid,xtype,fieldid, name, offsetp, field_typeidp, ndimsp, dims);
    if(TRACE) trace(ret,"nc_inq_compound_field",ncid,xtype,fieldid, name, offsetp, field_typeidp, ndimsp, dims); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_inq_vlen(int ncid, int xtype, byte[] name, SizeTByReference datum_sizep, IntByReference base_nc_typep) {
    int ret;
    try {ce();
    ret = nc4.nc_inq_vlen(ncid,xtype, name, datum_sizep, base_nc_typep);
    if(TRACE) trace(ret,"nc_inq_vlen",ncid,xtype, name, datum_sizep, base_nc_typep); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att(int ncid, int varid, String name, Vlen_t[] vlen) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att(ncid,varid,name, vlen);
    if(TRACE) trace(ret,"nc_get_att",ncid,varid,name, vlen); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var(int ncid, int varid, Vlen_t[] vlen) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var(ncid,varid, vlen);
    if(TRACE) trace(ret,"nc_get_var",ncid,varid, vlen); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1(int ncid, int varid, SizeT[] indexp, Vlen_t[] vlen) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1(ncid,varid, indexp, vlen);
    if(TRACE) trace(ret,"nc_get_var1",ncid,varid, indexp, vlen); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, Vlen_t[] v) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara(ncid,varid, startp, countp, v);
    if(TRACE) trace(ret,"nc_get_vara",ncid,varid, startp, countp, v); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, Vlen_t[] v) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars(ncid,varid, startp, countp, stridep, v);
    if(TRACE) trace(ret,"nc_get_vars",ncid,varid, startp, countp, stridep, v); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att(int ncid, int varid, String attName, int xtype, SizeT len, Vlen_t[] value) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att(ncid,varid,attName,xtype,len, value);
    if(TRACE) trace(ret,"nc_put_att",ncid,varid,attName,xtype,len, value); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var(int ncid, int varid, Vlen_t[] vlen) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var(ncid,varid, vlen);
    if(TRACE) trace(ret,"nc_put_var",ncid,varid, vlen); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var1(int ncid, int varid, SizeT[] indexp, Vlen_t[] vlen) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var1(ncid,varid, indexp, vlen);
    if(TRACE) trace(ret,"nc_put_var1",ncid,varid, indexp, vlen); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, Vlen_t[] v) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara(ncid,varid, startp, countp, v);
    if(TRACE) trace(ret,"nc_put_vara",ncid,varid, startp, countp, v); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, Vlen_t[] v) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars(ncid,varid, startp, countp, stridep, v);
    if(TRACE) trace(ret,"nc_put_vars",ncid,varid, startp, countp, stridep, v); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_att(int ncid, int varid, String name, Pointer p) {
    int ret;
    try {ce();
    ret = nc4.nc_get_att(ncid,varid,name,p);
    if(TRACE) trace(ret,"nc_get_att",ncid,varid,name,p); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var(int ncid, int varid, Pointer p) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var(ncid,varid,p);
    if(TRACE) trace(ret,"nc_get_var",ncid,varid,p); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_var1(int ncid, int varid, SizeT[] indexp, Pointer p) {
    int ret;
    try {ce();
    ret = nc4.nc_get_var1(ncid,varid, indexp,p);
    if(TRACE) trace(ret,"nc_get_var1",ncid,varid, indexp,p); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, Pointer p) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vara(ncid,varid, startp, countp,p);
    if(TRACE) trace(ret,"nc_get_vara",ncid,varid, startp, countp,p); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_get_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, Pointer p) {
    int ret;
    try {ce();
    ret = nc4.nc_get_vars(ncid,varid, startp, countp, stridep,p);
    if(TRACE) trace(ret,"nc_get_vars",ncid,varid, startp, countp, stridep,p); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_att(int ncid, int varid, String attName, int xtype, SizeT len, Pointer p) {
    int ret;
    try {ce();
    ret = nc4.nc_put_att(ncid,varid,attName,xtype,len,p);
    if(TRACE) trace(ret,"nc_put_att",ncid,varid,attName,xtype,len,p); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var(int ncid, int varid, Pointer p) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var(ncid,varid,p);
    if(TRACE) trace(ret,"nc_put_var",ncid,varid,p); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_var1(int ncid, int varid, SizeT[] indexp, Pointer p) {
    int ret;
    try {ce();
    ret = nc4.nc_put_var1(ncid,varid, indexp,p);
    if(TRACE) trace(ret,"nc_put_var1",ncid,varid, indexp,p); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vara(int ncid, int varid, SizeT[] startp, SizeT[] countp, Pointer p) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vara(ncid,varid, startp, countp,p);
    if(TRACE) trace(ret,"nc_put_vara",ncid,varid, startp, countp,p); 
    } finally {cx();}
    return ret;
    }

    @Override
    public synchronized
    int nc_put_vars(int ncid, int varid, SizeT[] startp, SizeT[] countp, SizeT[] stridep, Pointer p) {
    int ret;
    try {ce();
    ret = nc4.nc_put_vars(ncid,varid, startp, countp, stridep,p);
    if(TRACE) trace(ret,"nc_put_vars",ncid,varid, startp, countp, stridep,p); 
    } finally {cx();}
    return ret;
    }

}
