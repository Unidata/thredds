/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

/*
TODO:
1. make sure all nodes areproperly annotated
*/


package dap4.dap4lib.netcdf;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import dap4.core.data.DataCursor;
import dap4.core.dmr.DMRFactory;
import dap4.core.dmr.DapNode;
import dap4.core.dmr.DapType;
import dap4.core.dmr.DapVariable;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.AbstractDSP;
import dap4.dap4lib.DapCodes;
import dap4.dap4lib.XURI;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.jni.netcdf.Nc4prototypes;
import ucar.nc2.jni.netcdf.SizeTByReference;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static dap4.dap4lib.netcdf.Nc4Notes.*;
import static ucar.nc2.jni.netcdf.Nc4prototypes.*;

/**
 * DSP for reading netcdf files through jni interface to netcdf4 library
 */
public class Nc4DSP extends AbstractDSP
{
    //////////////////////////////////////////////////
    // Constants

    static public final boolean DEBUG = false;
    static public final boolean DUMPDMR = false;

    static String PATHSUFFIX = "/src/data";

    static public String[] EXTENSIONS = new String[]{".nc", ".hdf5"};

    static final Pointer NC_NULL = Pointer.NULL;
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
    // com.sun.jna.Memory control

    /**
     * Provide a wrapper for pointers that tracks the size.
     * Also allows for allocation.
     */
    static public class Nc4Pointer
    {
        static public Nc4Pointer
        allocate(long size)
        {
            if(size == 0)
                throw new IllegalArgumentException("Attempt to allocate zero bytes");
            Memory m = new Memory(size);
            return new Nc4Pointer(m, size);
        }

        public Pointer p;  // alow direct access
        public long size; //allow direct access

        public Nc4Pointer(Pointer p, long size)
        {
            this.p = p;
            this.size = size;
        }

        public Nc4Pointer
        share(long offset, long size)
        {
            try {
                Pointer ps = p.share(offset, size);
                Nc4Pointer newp = new Nc4Pointer(ps, size);
                return newp;
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }

        public String
        toString()
        {
            return String.format("0x%016x/%d", Pointer.nativeValue(this.p), this.size);
        }

        static public boolean
        validate(Nc4Pointer mem, long require)
        {
            if(mem == null || mem.p == null || mem.size == 0) return false;
            return (mem.size > require);
        }
    }

    //////////////////////////////////////////////////
    // DSP Match API

    /**
     * A path is file if it has no base protocol or is file:
     *
     * @param path
     * @param context Any parameters that may help to decide.
     * @return true if this path appears to be processible by this DSP
     */
    public boolean dspMatch(String path, DapContext context)
    {
        for(String s : EXTENSIONS) {
            if(path.endsWith(s)) return true;
        }
        return false;
    }

    //////////////////////////////////////////////////
    // Notes Management

    protected Map<NoteSort, Map<Long, Notes>> allnotes = null;

    /*package*/ void
    note(Notes note)
    {
        assert (this.allnotes != null);
        int gid = note.gid;
        int id = note.id;
        NoteSort sort = note.getSort();
        Map<Long, Notes> sortnotes = this.allnotes.get(sort);
        assert sortnotes != null;
        switch (sort) {
            case TYPE:
            case GROUP:
            case DIM:
                assert sortnotes.get(id) == null;
                sortnotes.put((long) id, note);
                break;
            case VAR:
                long gv = Nc4Notes.getVarId((VarNotes) note);
                assert sortnotes.get(gv) == null;
                sortnotes.put(gv, note);
                break;
        }
    }

    /*package*/ VarNotes
    findVar(int gid, int varid)
    {
        long gv = Nc4Notes.getVarId(gid, varid, -1);
        return (VarNotes) find(gv, NoteSort.VAR);
    }

    /*package*/ VarNotes
    findField(int gid, int varid, int fid)
    {
        long gv = Nc4Notes.getVarId(gid, varid, fid);
        return (VarNotes) find(gv, NoteSort.VAR);
    }

    public Notes
    find(long id, NoteSort sort)
    {
        assert (this.allnotes != null);
        Map<Long, Notes> sortnotes = this.allnotes.get(sort);
        assert sortnotes != null;
        return sortnotes.get((long) id);
    }

    /*package*/Notes
    find(DapNode node)
    {
        NoteSort sort = noteSortFor(node);
        assert (this.allnotes != null);
        Map<Long, Notes> sortnotes = this.allnotes.get(sort);
        assert sortnotes != null;
        for(Map.Entry<Long, Notes> entries : sortnotes.entrySet()) {
            Notes note = entries.getValue();
            if(note.get() == node)
                return note;
        }
        return null;
    }

    protected NoteSort
    noteSortFor(DapNode node)
    {
        switch (node.getSort()) {
            case ATOMICTYPE:
            case STRUCTURE:
            case SEQUENCE:
                return NoteSort.TYPE;
            case VARIABLE:
                return NoteSort.VAR;
            case GROUP:
            case DATASET:
                return NoteSort.GROUP;
            case DIMENSION:
                return NoteSort.DIM;
            default:
                break;
        }
        return null;
    }

    protected void allnotesInit()
    {
        this.allnotes = new HashMap<>();
        for(NoteSort s : NoteSort.values()) {
            this.allnotes.put(s, new HashMap<Long, Notes>());
        }
        Notes n;
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_BYTE, this);
        n.set(DapType.INT8);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_CHAR, this);
        n.set(DapType.CHAR);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_SHORT, this);
        n.set(DapType.INT16);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_INT, this);
        n.set(DapType.INT32);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_FLOAT, this);
        n.set(DapType.FLOAT32);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_DOUBLE, this);
        n.set(DapType.FLOAT64);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_UBYTE, this);
        n.set(DapType.UINT8);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_USHORT, this);
        n.set(DapType.UINT16);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_UINT, this);
        n.set(DapType.UINT32);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_INT64, this);
        n.set(DapType.INT64);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_UINT64, this);
        n.set(DapType.UINT64);
        this.note(n);
        n = Nc4Notes.factory(NoteSort.TYPE, 0, NC_STRING, this);
        n.set(DapType.STRING);
        this.note(n);

        for(int i = NC_BYTE; i <= NC_MAX_ATOMIC_TYPE; i++) {
            Nc4Notes.TypeNotes tn = (Nc4Notes.TypeNotes) find(i, NoteSort.TYPE);
            assert tn != null;
            int ret = 0;
            byte[] namep = new byte[NC_MAX_NAME + 1];
            if(i == NC_STRING) {
                // There is a bug in some versions of netcdf that does not
                // handle NC_STRING correctly when the gid is invalid.
                // Handle specially ; this is a temporary hack
                tn.setSize(Pointer.SIZE);
            } else {
                SizeTByReference sizep = new SizeTByReference();
                try {
                    Nc4Cursor.errcheck(getJNI(), ret = nc4.nc_inq_type(0, i, namep, sizep));
                } catch (DapException e) {
                    e.printStackTrace();
                    assert false; // should never happen
                }
                tn.setSize(sizep.intValue());
            }
        }
    }

    //////////////////////////////////////////////////
    // Instance Variables

    protected Nc4prototypes nc4 = null;

    protected boolean trace = false;
    protected boolean closed = false;

    protected int ncid = -1;        // file id ; also set as DSP.source
    protected int format = 0;       // from nc_inq_format
    protected int mode = 0;
    protected String filepath = null; // real path to the dataset

    protected DMRFactory dmrfactory = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Nc4DSP()
            throws DapException
    {
        super();
        if(this.nc4 == null) {
            this.nc4 = Nc4Iosp.getCLibrary();
            if(this.nc4 == null)
                throw new DapException("Could not load libnetcdf");
        }
        dmrfactory = new DMRFactory();
        allnotesInit();
    }

    //////////////////////////////////////////////////
    // DSP API

    @Override
    public Nc4DSP
    open(String filepath)
            throws DapException
    {
        if(filepath.startsWith("file:")) try {
            XURI xuri = new XURI(filepath);
            filepath = xuri.getPath();
        } catch (URISyntaxException use) {
            throw new DapException("Malformed filepath: " + filepath)
                    .setCode(DapCodes.SC_NOT_FOUND);
        }
        int ret, mode;
        IntByReference ncidp = new IntByReference();
        this.filepath = filepath;
        try {
            mode = NC_NOWRITE;
            Nc4Cursor.errcheck(nc4, ret = nc4.nc_open(this.filepath, mode, ncidp));
            this.ncid = ncidp.getValue();
            // Figure out what kind of file
            IntByReference formatp = new IntByReference();
            Nc4Cursor.errcheck(nc4, ret = nc4.nc_inq_format(ncid, formatp));
            this.format = formatp.getValue();
            if(DEBUG)
                System.out.printf("TestNetcdf: open: %s; ncid=%d; format=%d%n",
                        this.filepath, ncid, this.format);
            // Compile the DMR
            Nc4DMRCompiler dmrcompiler = new Nc4DMRCompiler(this, ncid, dmrfactory);
            setDMR(dmrcompiler.compile());
            if(DEBUG || DUMPDMR) {
                System.err.println("+++++++++++++++++++++");
                System.err.println(printDMR(getDMR()));
                System.err.println("+++++++++++++++++++++");
            }
            return this;
        } catch (Exception t) {
            t.printStackTrace();
        }
        return null;
    }

    @Override
    public void close()
            throws DapException
    {
        if(this.closed) return;
        if(this.ncid < 0) return;
        int ret = nc4.nc_close(ncid);
        Nc4Cursor.errcheck(nc4, ret);
        closed = true;
        if(trace)
            System.out.printf("Nc4DSP: closed: %s%n", this.filepath);
    }

    @Override
    public Nc4Cursor
    getVariableData(DapVariable var)
            throws DapException
    {
        assert (var.isTopLevel());
        DapType type = var.getBaseType();
        Nc4Cursor vardata = (Nc4Cursor) super.getVariableData(var);
        if(vardata == null) {
            switch (type.getTypeSort()) {
                case Structure:
                    vardata = new Nc4Cursor(DataCursor.Scheme.STRUCTARRAY, this, var, null);
                    break;
                case Sequence:
                    vardata = new Nc4Cursor(DataCursor.Scheme.SEQARRAY, this, var, null);
                    break;
                default:
                    if(!type.isAtomic())
                        throw new DapException("Unexpected cursor type: " + type);
                    vardata = new Nc4Cursor(DataCursor.Scheme.ATOMIC, this, var, null);
                    break;
            }
            super.addVariableData(var, vardata);
        }
        assert var.isTopLevel();
        return vardata;
    }

    //////////////////////////////////////////////////
    // Accessors


    public Nc4prototypes getJNI()
    {
        return this.nc4;
    }

    @Override
    public String getLocation()
    {
        return this.filepath;
    }

    //////////////////////////////////////////////////
    // Utilities

    static public String makeString(byte[] b)
    {
        // null terminates
        int count;
        for(count = 0; (count < b.length && b[count] != 0); count++) {
            ;
        }
        return new String(b, 0, count, DapUtil.UTF8);
    }


}
