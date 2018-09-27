/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib.netcdf;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import dap4.core.dmr.*;
import dap4.core.util.Convert;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import ucar.nc2.jni.netcdf.Nc4prototypes;
import ucar.nc2.jni.netcdf.SizeTByReference;

import static ucar.nc2.jni.netcdf.Nc4prototypes.*;
import static dap4.dap4lib.netcdf.Nc4DSP.*;
import static dap4.dap4lib.netcdf.Nc4Notes.*;


/**
 * Compile netcdf file info into DMR
 */
public class Nc4DMRCompiler
{
    //////////////////////////////////////////////////
    // Constants

    static public final boolean DEBUG = false;

    // Define reserved attributes
    static public final String UCARTAGVLEN = Nc4DSP.UCARTAGVLEN;
    static public final String UCARTAGOPAQUE = Nc4DSP.UCARTAGOPAQUE;

    static final int NC_FALSE = 0;
    static final int NC_TRUE = 1;
    // "null" id(s)
    static public final int NC_GRPNULL = 0;
    static public final int NC_IDNULL = -1;
    static public final int NC_NOERR = 0;

    static int NC_INT_BYTES = (java.lang.Integer.SIZE / java.lang.Byte.SIZE);
    static int NC_LONG_BYTES = (Native.LONG_SIZE);
    static int NC_POINTER_BYTES = (Native.POINTER_SIZE);
    static int NC_SIZET_BYTES = (Native.SIZE_T_SIZE);

    //////////////////////////////////////////////////
    // Static methods

    /**
     * A path is file if it has no base protocol or is file:
     *
     * @param path
     * @param context Any parameters that may help to decide.
     * @return true if this path appears to be processible by this DSP
     */
    static public boolean dspMatch(String path, DapContext context)
    {
        for(String s : EXTENSIONS) {
            if(path.endsWith(s)) return true;
        }
        return false;
    }

    //////////////////////////////////////////////////
    // Instance Variables

    protected Nc4prototypes nc4 = null;

    protected boolean trace = false;
    protected boolean closed = false;

    protected int ncid = -1;        // file id
    protected int format = 0;       // from nc_inq_format
    protected int mode = 0;
    protected String path = null;

    protected String pathprefix = null;

    protected DMRFactory factory = null;
    protected Nc4DSP dsp = null;
    protected DapDataset dmr = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Nc4DMRCompiler(Nc4DSP dsp, int ncid, DMRFactory factory)
            throws DapException
    {
        this.dsp = dsp;
        this.nc4 = dsp.getJNI();
        this.path = dsp.getLocation();
        this.ncid = ncid;
        this.factory = factory;
    }


    //////////////////////////////////////////////////
    // Main entry point

    public DapDataset
    compile()
            throws DapException
    {
        // create and fill the root group
        buildrootgroup(this.ncid);
        if(this.dmr != null) dmr.finish();
        return this.dmr;
    }

    //////////////////////////////////////////////////

    protected void
    buildrootgroup(int ncid)
            throws DapException
    {
        int ret;
        byte[] namep = new byte[NC_MAX_NAME + 1];
        errcheck(ret = nc4.nc_inq_grpname(ncid, namep));
        String[] pieces = DapUtil.canonicalpath(this.path).split("[/]");
        DapDataset g = factory.newDataset(pieces[pieces.length - 1]);
        GroupNotes gi = (GroupNotes)Nc4Notes.factory(NoteSort.GROUP,ncid, ncid, this.dsp);
        gi.set(g);
        this.dsp.note(gi);
        this.dmr = g;
        fillgroup(ncid);
    }

    protected void
    fillgroup(int gid)
            throws DapException
    {
        int ret, mode;
        int[] dims = getDimensions(gid);
        int[] udims = getUnlimitedDimensions(gid);
        for(int dimid : dims) {
            builddim(gid, dimid, udims);
        }
        int[] typeids = getUserTypes(gid);
        for(int i=0;i<typeids.length;i++) {
            for(int j = 0; j < i; j++) {
                 if(typeids[i] == typeids[j])
                     assert false;
            }
        }

        for(int typeid : typeids) {
            buildusertype(gid, typeid);
        }
        int[] varids = getVars(gid);
        for(int varid : varids) {
            buildvar(gid, varid);
        }
        // globalattributes
        String[] gattnames = getAttributes(gid, NC_GLOBAL);
        for(String ga : gattnames) {
            buildattr(gid, NC_GLOBAL, ga);
        }
        int[] groupids = getGroups(gid);
        for(int groupid : groupids) {
            buildgroup(gid, groupid);
        }
    }

    protected void
    buildgroup(int parent, int gid)
            throws DapException
    {
        int ret;
        byte[] namep = new byte[NC_MAX_NAME + 1];
        errcheck(ret = nc4.nc_inq_grpname(gid, namep));
        DapGroup g = factory.newGroup(Nc4DSP.makeString(namep));
        GroupNotes gi = (GroupNotes)Nc4Notes.factory(NoteSort.GROUP,parent, gid, this.dsp);
        gi.set(g);
        this.dsp.note(gi);
        GroupNotes gp = (GroupNotes) this.dsp.find(parent, NoteSort.GROUP);
        gp.get().addDecl(g);
        fillgroup(gid);
    }

    protected void
    builddim(int gid, int did, int[] udims)
            throws DapException
    {
        int ret = NC_NOERR;
        byte[] namep = new byte[NC_MAX_NAME + 1];
        SizeTByReference lenp = new SizeTByReference();
        errcheck(ret = nc4.nc_inq_dim(gid, did, namep, lenp));
        String name = Nc4DSP.makeString(namep);
        int len = lenp.intValue();
        boolean isunlimited = contains(udims, did);
        DapDimension dim = factory.newDimension(name, lenp.longValue());
        dim.setUnlimited(isunlimited);
        DimNotes di = (DimNotes)Nc4Notes.factory(NoteSort.DIM,gid, did, this.dsp);
        di.set(dim);
        this.dsp.note(di);
        GroupNotes gp = (GroupNotes) this.dsp.find(gid, NoteSort.GROUP);
        gp.get().addDecl(dim);
        if(trace)
            System.out.printf("Nc4DSP: dimension: %s size=%d%n", name, dim.getSize());
    }

    protected void
    buildusertype(int gid, int tid)
            throws DapException
    {
        int ret = NC_NOERR;
        byte[] namep = new byte[NC_MAX_NAME + 1];
        SizeTByReference lenp = new SizeTByReference();
        IntByReference basetypep = new IntByReference();
        IntByReference classp = new IntByReference();
        SizeTByReference nfieldsp = new SizeTByReference();
        errcheck(ret = nc4.nc_inq_user_type(gid, tid, namep, lenp, basetypep, nfieldsp, classp));
        String name = Nc4DSP.makeString(namep);
        int basetype = basetypep.getValue();
        long len = lenp.longValue();
        long nfields = nfieldsp.longValue();
        TypeNotes ti = (TypeNotes)Nc4Notes.factory(NoteSort.TYPE,gid, tid, this.dsp);
        switch (classp.getValue()) {
        case NC_OPAQUE:
            buildopaquetype(ti, name, len);
            break;
        case NC_ENUM:
            buildenumtype(ti, name, basetype);
            break;
        case NC_COMPOUND:
            buildcompoundtype(ti, name, nfields, len);
            break;
        case NC_VLEN:
            buildvlentype(ti, name, basetype, len);
            break;
        default:
            throw new DapException("Unknown class: " + classp.getValue());
        }
    }

    protected void
    buildopaquetype(TypeNotes ti, String name, long len)
            throws DapException
    {
        int ret;
        ti.setOpaque(len);
        DapType dt = DapType.lookup(TypeSort.Opaque);
        ti.set(dt);
        this.dsp.note(ti);
    }

    protected void
    buildenumtype(TypeNotes ti, String name, int basetype)
            throws DapException
    {
        int ret;
        SizeTByReference nmembersp = new SizeTByReference();
        SizeTByReference sizep = new SizeTByReference();
        byte[] namep = new byte[NC_MAX_NAME + 1];
        IntByReference basetypep = new IntByReference();
        IntByReference valuep = new IntByReference();
        TypeNotes base = (TypeNotes) this.dsp.find(basetype, NoteSort.TYPE);
        if(!isintegertype(base))
            throw new DapException("Enum base type must be integer type");
        errcheck(ret = nc4.nc_inq_enum(ti.gid, ti.id, namep, basetypep, sizep, nmembersp));
        DapEnumeration de = factory.newEnumeration(name, DapType.lookup(base.getType().getTypeSort()));
        ti.set(de);
        this.dsp.note(ti);
        ti.setEnumBaseType(basetype);
        ti.group().addDecl(de);
        // build list of enum consts
        int nconsts = nmembersp.intValue();
        for(int i = 0; i < nconsts; i++) {
            // Get info about the ith const
            errcheck(ret = nc4.nc_inq_enum_member(ti.gid, ti.id, i, namep, valuep));
            String ecname = Nc4DSP.makeString(namep);
            long ecval = (long) valuep.getValue();
            DapEnumConst dec = factory.newEnumConst(ecname, ecval);
            de.addEnumConst(dec);
        }
    }

    protected void
    buildcompoundtype(TypeNotes ti, String name, long nfields, long len)
            throws DapException
    {
        DapStructure ds = factory.newStructure(name);
        ti.set(ds);
        this.dsp.note(ti);
        ti.group().addDecl(ds);
        for(int i = 0; i < nfields; i++) {
            buildfield(ti, i, ds);
        }
        // Finally, extract the size of the structure
        int ret;
        SizeTByReference sizep = new SizeTByReference();
        SizeTByReference nfieldsp = new SizeTByReference();
        byte[] namep = new byte[NC_MAX_NAME + 1];
        errcheck(ret = nc4.nc_inq_compound(ti.gid, ti.id, namep, sizep, nfieldsp));
        ti.setSize(sizep.longValue());
        assert len == sizep.longValue();
        assert name.equals(Nc4DSP.makeString(namep));
        // Add the netcdf4 name as an xml attribute.
        ds.addXMLAttribute(UCARTAGORIGTYPE,name);
    }

    protected void
    buildfield(TypeNotes ti, int fid, DapStructure container)
            throws DapException
    {
        int ret;
        byte[] namep = new byte[NC_MAX_NAME + 1];
        SizeTByReference offsetp = new SizeTByReference();
        IntByReference fieldtypep = new IntByReference();
        IntByReference ndimsp = new IntByReference();

        // Get everything but actual dims
        errcheck(ret = nc4.nc_inq_compound_field(ti.gid, ti.id, fid, namep,
                offsetp, fieldtypep, ndimsp, null));
        int fieldtype = fieldtypep.getValue();
        TypeNotes baset = (TypeNotes) this.dsp.find(fieldtype, NoteSort.TYPE);
        if(baset == null)
            throw new DapException("Undefined field base type: " + fieldtype);
        int[] dimsizes = getFieldDimsizes(ti.gid, ti.id, fid, ndimsp.getValue());
        VarNotes fieldnotes = makeField(ti, fid, Nc4DSP.makeString(namep), baset, offsetp.intValue(), dimsizes);
        assert baset.getSize() > 0;
    }

    protected VarNotes
    makeField(TypeNotes container, int fieldid, String name, TypeNotes baset, int offset, int[] dimsizes)
            throws DapException
    {
        DapVariable field;
        DapStructure ds = (DapStructure) container.getType();
        field = factory.newVariable(name, baset.getType());
        field.setParent(ds);
        field.setFieldIndex(fieldid);
        VarNotes notes = (VarNotes) Nc4Notes.factory(NoteSort.VAR, container.gid, container.id, this.dsp);
        notes.setOffset(offset)
                .setBaseType(baset)
                .setContainer(container);
        notes.set(field);
        this.dsp.note(notes);
        // set dimsizes
        if(dimsizes.length > 0) {
            for(int i = 0; i < dimsizes.length; i++) {
                DapDimension dim = factory.newDimension(null, dimsizes[i]);
                field.addDimension(dim);
            }
        }
        ds.addField(field);
        return notes;
    }

    protected DapVariable
    buildvar(int gid, int vid)
            throws DapException
    {
        int ret;
        byte[] namep = new byte[NC_MAX_NAME + 1];
        IntByReference ndimsp = new IntByReference();
        IntByReference xtypep = new IntByReference();
        IntByReference nattsp = new IntByReference();
        errcheck(ret = nc4.nc_inq_var(gid, vid, namep, xtypep, ndimsp, null, nattsp));
        String name = Nc4DSP.makeString(namep);
        TypeNotes xtype = (TypeNotes) this.dsp.find(xtypep.getValue(), NoteSort.TYPE);
        if(DEBUG) {
            System.err.printf("NC4: inqvar: name=%s gid=%d vid=%d xtype=%d ndims=%d natts=%d%n",
                    name, gid, vid, xtype.id, ndimsp.getValue(), nattsp.getValue());
        }
        if(xtype == null)
            throw new DapException("Unknown type id: " + xtype.id);
        DapVariable var;
        switch (((DapType) xtype.node).getTypeSort()) {
        default: /* atomic */
            var = factory.newVariable(name, xtype.getType());
            break;
        case Enum:
            var = factory.newVariable(name, xtype.getType());
            break;
        case Structure:
            DapStructure st = (DapStructure) xtype.get();
            var = factory.newVariable(name, xtype.getType());
            break;
        case Sequence:
            DapSequence seq = (DapSequence) xtype.get();
            var = factory.newVariable(name, xtype.getType());
            break;
        }
        VarNotes vi = (VarNotes)Nc4Notes.factory(NoteSort.VAR,gid, vid, this.dsp);
        vi.set(var);
        this.dsp.note(vi);
        vi.setBaseType(xtype);
        vi.group().addDecl(var);
        int[] dimids = getVardims(gid, vid, ndimsp.getValue());
        for(int i = 0; i < dimids.length; i++) {
            DimNotes di = (DimNotes) this.dsp.find(dimids[i], NoteSort.DIM);
            if(di == null)
                throw new DapException("Undefined variable dimension id: " + dimids[i]);
            var.addDimension(di.get());
        }
        // Now, if this is of type opaque, tag it with the size
        if(xtype.isOpaque()) {
            var.addXMLAttribute(UCARTAGOPAQUE,Long.toString(xtype.getSize()));
        }
        // fill in any attributes
        String[] attnames = getAttributes(gid, vid);
        for(String a : attnames) {
            buildattr(gid, vid, a);
        }
        return var;
    }

    protected void
    buildvlentype(TypeNotes ti, String vname, int basetypeid, long len)
            throws DapException
    {
        int ref;
        // We map vlen to a sequence with a single field of the
        // basetype of the vlen. Field name is same as the vlen type.
        // So we need to build two things:
        // 1. a Sequence object
        // 2. a Field
        DapSequence ds = factory.newSequence(vname);
        ti.set(ds);
        this.dsp.note(ti);
        ti.group().addDecl(ds);
        ti.markVlen();
        TypeNotes fieldtype = (TypeNotes) this.dsp.find(basetypeid, NoteSort.TYPE);
        if(fieldtype == null)
            throw new DapException("Undefined vlen basetype: " + basetypeid);
        VarNotes fieldnotes = makeField(ti, 0, vname, fieldtype, 0, new int[0]);
        // Annotate to indicate that this came from a vlen
        ds.addXMLAttribute(UCARTAGVLEN,"1");

        // Annotate to indicate that the original type name
        ds.addXMLAttribute(UCARTAGORIGTYPE,ds.getFQN());

        // Finally, extract the size of the structure, which is the same
        // as the size of the singleton field
        ti.setRecordSize(fieldtype.getSize());
        ti.setSize(Nc4prototypes.Vlen_t.VLENSIZE);
    }

    protected void
    buildattr(int gid, int vid, String name)
            throws DapException
    {
        int ret;
        boolean isglobal = (vid == NC_GLOBAL);
        IntByReference basetypep = new IntByReference();
        errcheck(ret = nc4.nc_inq_atttype(gid, vid, name, basetypep));
        int basetype = basetypep.getValue();
        TypeNotes base = (TypeNotes) this.dsp.find(basetype, NoteSort.TYPE);
        if(!islegalattrtype(base))
            throw new DapException("Non-atomic attribute types not supported: " + name);
        SizeTByReference countp = new SizeTByReference();
        errcheck(ret = nc4.nc_inq_attlen(gid, vid, name, countp));
        // Get the values of the attribute
        String[] values = getAttributeValues(gid, vid, name, base, countp.intValue());
        DapAttribute da = factory.newAttribute(name, (DapType) base.getType());
        da.setValues(values);
        if(isglobal) {
            GroupNotes gi = (GroupNotes) this.dsp.find(gid, NoteSort.GROUP);
            gi.get().addAttribute(da);
        } else {
            VarNotes vi = this.dsp.findVar(gid, vid);
            vi.get().addAttribute(da);
        }
    }

    //////////////////////////////////////////////////

    int[]
    getGroups(int gid)
            throws DapException
    {
        int ret, n;
        IntByReference ip = new IntByReference();
        errcheck(ret = nc4.nc_inq_grps(gid, ip, null));
        n = ip.getValue();
        int[] grpids = null;
        if(n > 0) {
            grpids = new int[n];
            errcheck(ret = nc4.nc_inq_grps(gid, ip, grpids));
        } else
            grpids = new int[0];
        return grpids;
    }

    int[]
    getDimensions(int gid)
            throws DapException
    {
        int ret, n;
        IntByReference ip = new IntByReference();
        errcheck(ret = nc4.nc_inq_ndims(gid, ip));
        n = ip.getValue();
        int[] dimids = new int[n];
        if(n > 0)
            errcheck(ret = nc4.nc_inq_dimids(gid, ip, dimids, NC_FALSE));
        return dimids;
    }

    int[]
    getUnlimitedDimensions(int gid)
            throws DapException
    {
        int ret, n;
        IntByReference ip = new IntByReference();
        errcheck(ret = nc4.nc_inq_unlimdims(gid, ip, null));
        n = ip.getValue();
        int[] dimids = new int[n];
        if(n > 0)
            errcheck(ret = nc4.nc_inq_unlimdims(gid, ip, dimids));
        return dimids;
    }

    int[]
    getUserTypes(int gid)
            throws DapException
    {
        int ret, n;
        IntByReference ip = new IntByReference();
        errcheck(ret = nc4.nc_inq_typeids(gid, ip, null));
        n = ip.getValue();
        int[] typeids = new int[n];
        if(n > 0)
            errcheck(ret = nc4.nc_inq_typeids(gid, ip, typeids));
        return typeids;
    }

    int[]
    getVars(int gid)
            throws DapException
    {
        int ret, n;
        IntByReference ip = new IntByReference();
        errcheck(ret = nc4.nc_inq_nvars(gid, ip));
        n = ip.getValue();
        int[] ids = new int[n];
        if(n > 0)
            errcheck(ret = nc4.nc_inq_varids(gid, ip, ids));
        return ids;
    }

    int[]
    getVardims(int gid, int vid, int ndims)
            throws DapException
    {
        int ret;
        int[] dimids = new int[ndims];

        if(ndims > 0) {
            byte[] namep = new byte[NC_MAX_NAME + 1];
            IntByReference ndimsp = new IntByReference();
            errcheck(ret = nc4.nc_inq_var(gid, vid, null, null, ndimsp, dimids, null));
        }
        return dimids;
    }

    int[]
    getFieldDimsizes(int gid, int tid, int fid, int ndims)
            throws DapException
    {
        int ret;
        int[] dimsizes = new int[ndims];
        if(ndims > 0) {
            IntByReference ndimsp = new IntByReference();
            errcheck(ret = nc4.nc_inq_compound_field(gid, tid, fid, null, null, null, ndimsp, dimsizes));
        }
        return dimsizes;
    }

    String[]
    getAttributes(int gid, int vid)
            throws DapException
    {
        int ret, n;
        boolean isglobal = (vid == NC_GLOBAL);
        IntByReference nattsp = new IntByReference();
        byte[] namep = new byte[NC_MAX_NAME + 1];
        IntByReference ndimsp = new IntByReference();
        IntByReference xtypep = new IntByReference();
        if(isglobal)
            errcheck(ret = nc4.nc_inq_natts(gid, nattsp));
        else {
            errcheck(ret = nc4.nc_inq_var(gid, vid, namep, xtypep, ndimsp, null, nattsp));
        }
        n = nattsp.getValue();
        String[] names = new String[n];
        for(int i = 0; i < n; i++) {
            errcheck(ret = nc4.nc_inq_attname(gid, vid, i, namep));
            names[i] = Nc4DSP.makeString(namep);
        }
        return names;
    }

    String[]
    getAttributeValues(int gid, int vid, String name, TypeNotes tn, int count)
            throws DapException
    {
        int ret;
        // Currently certain types only are allowed.
        if(!islegalattrtype(tn))
            throw new DapException("Unsupported attribute type: " + tn.getType().getShortName());
        if(isenumtype(tn))
            tn = enumbasetype(tn);
        Object vector = getRawAttributeValues(tn, count, gid, vid, name);
        DapType basetype = tn.getType();
        // basetype == Char requires special pre-conversion
        // since the nc file data is coming back as utf-8, not utf-16.
        if(basetype.isCharType()) {
            byte[] data = (byte[]) vector;  // raw utf-8
            String sdata = new String(data,DapUtil.UTF8);
            char[] cdata = sdata.toCharArray();
            count = cdata.length;
            vector = cdata;
        }
        String[] values = (String[])Convert.convert(DapType.STRING, basetype, vector);
        return values;
    }

    Object
    getRawAttributeValues(TypeNotes base, int count, int gid, int vid, String name)
            throws DapException
    {
        int nativetypesize = base.getType().getSize();
        if(isstringtype(base))
            nativetypesize = NC_POINTER_BYTES;
        else if(nativetypesize == 0)
            throw new DapException("Illegal Type Sort:" + base.get().getShortName());
        Object values = null;
        if(count > 0) {
            int ret;
            long totalsize = nativetypesize * count;
            Nc4Pointer mem = Nc4Pointer.allocate(totalsize);
            errcheck(ret = nc4.nc_get_att(gid, vid, name, mem.p));
            switch (base.getType().getTypeSort()) {
            case Char:
                values = mem.p.getByteArray(0, count);
                break;
            case Int8:
                values = mem.p.getByteArray(0, count);
                break;
            case UInt8:
                values = mem.p.getByteArray(0, count);
                break;
            case Int16:
                values = mem.p.getShortArray(0, count);
                break;
            case UInt16:
                values = mem.p.getShortArray(0, count);
                break;
            case Int32:
                values = mem.p.getIntArray(0, count);
                break;
            case UInt32:
                values = mem.p.getIntArray(0, count);
                break;
            case Int64:
                values = mem.p.getLongArray(0, count);
                break;
            case UInt64:
                values = mem.p.getLongArray(0, count);
                break;
            case Float32:
                values = mem.p.getFloatArray(0, count);
                break;
            case Float64:
                values = mem.p.getDoubleArray(0, count);
                break;
            case String:
                values = mem.p.getStringArray(0, count);
                break;
            case Opaque:
                values = mem.p.getByteArray(0, (int) totalsize);
                break;
            case Enum:
                break;
            default:
                throw new IllegalArgumentException("Unexpected sort: " + base.getType().getShortName());
            }
        }
        return values;
    }

    /*Object[]
    convert(int count, Object src, TypeNotes basetype)
            throws DapException
    {
        boolean isenum = isenumtype(basetype);
        boolean isopaque = basetype.isOpaque();
        TypeNotes truetype = basetype;
        if(isenum)
            truetype = enumbasetype(basetype);

        Object[] dst;
        if(ischartype(basetype))
            dst = new Character[count];
        else
            dst = new Object[count];
        try {
            for(int i = 0; i < dst.length; i++) {
                switch (basetype.getType().getTypeSort()) {
                case Char:
                    if(src instanceof char[])
                        dst[i] = ((char[]) src)[i];
                    else
                        dst[i] = (char) (((byte[]) src)[i]);
                    break;
                case Int8:
                case UInt8:
                    dst[i] = ((byte[]) src)[i];
                    break;
                case Int16:
                case UInt16:
                    dst[i] = ((short[]) src)[i];
                    break;
                case Int32:
                case UInt32:
                    dst[i] = ((int[]) src)[i];
                    break;
                case Int64:
                case UInt64:
                    dst[i] = ((long[]) src)[i];
                    break;
                case Float32:
                    dst[i] = ((float[]) src)[i];
                    break;
                case Float64:
                    dst[i] = ((double[]) src)[i];
                    break;
                case String:
                    dst[i] = ((String[]) src)[i];
                    break;
                case Opaque:
                    byte[] alldata = (byte[]) src;
                    int oplen = alldata.length / count;
                    for(i = 0; i < count; i++) {
                        dst[i] = new byte[oplen];
                        System.arraycopy(alldata, oplen * i, dst[i], 0, oplen);
                    }
                    break;
                case Enum:
                    dst = convert(count, src, truetype);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected sort: " + basetype.getType().getShortName());
                }
            }
            return dst;
        } catch (IllegalArgumentException |
                ArrayIndexOutOfBoundsException e
                ) {
            throw new DapException(e);
        }
    } */

    protected void
    errcheck(int ret)
            throws DapException
    {
        if(ret != 0) {
            String msg = String.format("TestNetcdf: errno=%d; %s", ret, nc4.nc_strerror(ret));
            if(DEBUG)
                System.err.println(msg);
            throw new DapException(msg);
        }
    }

    boolean
    contains(int[] list, int value)
    {
        for(int i = 0; i < list.length; i++) {
            if(list[i] == value) return true;
        }
        return false;
    }

    boolean
    islegalattrtype(TypeNotes nctype)
    {
        return isatomictype(nctype)
                || isenumtype(nctype)
                || nctype.isOpaque();
    }

    boolean
    isatomictype(TypeNotes t)
    {
        return (t.id <= NC_MAX_ATOMIC_TYPE);
    }

    boolean
    isstringtype(TypeNotes nctype)
    {
        return (nctype.id == NC_STRING);
    }

    boolean
    ischartype(TypeNotes t)
    {
        return (t.id == NC_CHAR);
    }

    boolean
    isintegertype(TypeNotes t)
    {
        return (t.id <= NC_UINT64 && t.id != NC_CHAR);
    }

    boolean
    isenumtype(TypeNotes nctype)
    {
        return (nctype == null ? false : nctype.getType().isEnumType());
    }

    TypeNotes
    enumbasetype(TypeNotes etype)
    {
        if(etype == null || !etype.getType().isEnumType()) return null;
        DapType dt = ((DapEnumeration) etype.getType()).getBaseType();
        return (TypeNotes) this.dsp.find(dt);
    }

    protected String
    Nc4FQN(TypeNotes t)
            throws DapException
    {
        int ret = 0;
        // get enclosing ncid fqn
        SizeTByReference lenp = new SizeTByReference();
        errcheck(ret = nc4.nc_inq_grpname_len(t.gid, lenp));
        byte[] namep = new byte[lenp.intValue() + 1];
        errcheck(ret = nc4.nc_inq_grpname_full(t.gid, lenp, namep));
        return Nc4DSP.makeString(namep);
    }
}
