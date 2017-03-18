/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.dap4lib.netcdf;

import dap4.core.dmr.*;
import dap4.core.util.DapSort;
import dap4.core.util.DapUtil;

/**
 * Note that ideally, this info should be part of the
 * Nc4DMR classes, but that would require multiple inheritance.
 * Hence, we isolate that info here and add it to the instances
 * via annotation
 */

abstract public class Nc4Notes
{
    //////////////////////////////////////////////////
    // Constants

    // Mnemonics
    static public final int NOGROUP = -1;
    static public final int NOID = -1;
    static public final int NOFIELDID = -1;

    //////////////////////////////////////////////////
    // Use a factory so we can debug constructor calls

    static Notes
    factory(NoteSort ns, int g, int id, Nc4DSP dsp)
    {
        Notes note = null;
        switch (ns) {
        case TYPE:
            note = new TypeNotes(g, id, dsp);
            break;
        case VAR:
            note = new VarNotes(g, id, dsp);
            break;
        case DIM:
            note = new DimNotes(g, id, dsp);
            break;
        case GROUP:
            note = new GroupNotes(g, id, dsp);
            break;
        }
        return note;
    }

    //////////////////////////////////////////////////
    //Manage the compound id for variables

    static public long
    getVarId(VarNotes note)
    {
        return getVarId(note.gid, note.id, note.getFieldIndex());
    }

    static public long
    getVarId(int gid, int varid, int ifid)
    {
        long gv = ((long) gid) << 32;
        assert varid < 0x100000;
        gv = gv | ((long) varid) << 20;
        long fid = (long) ifid;
        if(fid >= 0)
            gv |= fid;
        return gv;
    }

    //////////////////////////////////////////////////
    // Type Decls

    static public enum NoteSort
    {
        TYPE, VAR, GROUP, DIM;
    }

    static public class Notes implements Cloneable
    {
        Nc4DSP dsp; // Need a place to store global state
        NoteSort sort;
        int gid;
        int id;
        DapNode node = null;
        protected String name = null;
        protected Notes parent = null;
        protected TypeNotes basetype = null;
        protected long offset = 0;
        // For most types, there is only one size,
        // but for vlen/sequence, there are two sizes:
        //  (1) the record size and (2) the instance size (= |vlen_t|);
        protected long size = 0;
        protected long recordsize = 0;

        protected Notes(NoteSort sort, int gid, int id, Nc4DSP dsp)
        {
            this.sort = sort;
            this.dsp = dsp;
            this.gid = gid;
            this.id = id;
        }

        public NoteSort getSort()
        {
            return this.sort;
        }

        public Notes setName(String name)
        {
            this.name = name;
            return this;
        }

        public Notes set(DapNode node)
        {
            this.node = node;
            if(this.name == null) setName(node.getShortName());
            return this;
        }

        public DapNode get()
        {
            return this.node;
        }

        public Notes setContainer(Notes parent)
        {
            this.parent = parent;
            return this;
        }

        public Notes getContainer()
        {
            return this.parent;
        }

        public long getOffset()
        {
            return this.offset;
        }

        public Notes setOffset(long offset)
        {
            this.offset = offset;
            return this;
        }

        public long getSize()
        {
            return this.size;
        }

        public Notes setSize(long size)
        {
            this.size = size;
            return this;
        }

        public long getRecordSize()
        {
            return this.recordsize;
        }

        public Notes setRecordSize(long size)
        {
            this.recordsize = size;
            return this;
        }

        public Notes setBaseType(TypeNotes t)
        {
            this.basetype = t;
            return this;
        }

        public TypeNotes getBaseType()
        {
            return this.basetype;
        }

        DapGroup group()
        {
            GroupNotes g = (GroupNotes) dsp.find(gid, NoteSort.GROUP);
            return (g == null ? null : g.get());
        }

        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append(this.getClass().getName());
            buf.append("{");
            if(name != null) {
                buf.append("name=");
                buf.append(name);
            }
            buf.append(" node=");
            buf.append(this.node != null ? this.node.getShortName() : "null");
            if(this.basetype != null)      {
                buf.append(" type=");
                buf.append(this.node.getShortName());
            }
            buf.append("}");
            return buf.toString();
        }
    }

    static public class GroupNotes extends Notes
    {
        protected GroupNotes(int p, int g, Nc4DSP dsp)
        {
            super(NoteSort.GROUP, p, g, dsp);
        }

        public DapGroup get()
        {
            return (DapGroup) super.get();
        }

        public GroupNotes set(DapNode node)
        {
            return (GroupNotes) super.set(node);
        }

    }

    static public class DimNotes extends Notes
    {
        protected DimNotes(int g, int id, Nc4DSP dsp)
        {
            super(NoteSort.DIM, g, id, dsp);
        }

        public DapDimension get()
        {
            return (DapDimension) super.get();
        }

        public DimNotes set(DapNode node)
        {
            return (DimNotes) super.set(node);
        }

    }

    static public class TypeNotes extends Notes
    {
        public int enumbase = -1;
        public boolean isvlen = false;

        protected TypeNotes(int g, int id, Nc4DSP dsp)
        {
            super(NoteSort.TYPE, g, id, dsp);
        }

        public DapType getType()
        {
            DapSort sort = this.node.getSort();
            switch (sort) {
            case ATOMICTYPE:
            case STRUCTURE:
            case SEQUENCE:
                return (DapType) super.get();
            case ENUMERATION:
                return (DapEnumeration) super.get();
            case VARIABLE:
                return ((DapVariable) super.get()).getBaseType();
            default:
                break;
            }
            return null;
        }

        public TypeNotes setOpaque(long len)
        {
            super.setSize(len);
            return this;
        }

        public TypeNotes setEnumBaseType(int bt)
        {
            this.enumbase = bt;
            TypeNotes btt = (TypeNotes)this.dsp.find(bt,NoteSort.TYPE);
            setSize(btt.getSize());
            return this;
        }

        public boolean isOpaque()
        {
            return getType().getTypeSort().isOpaqueType();
        }

        public boolean isEnum()
        {
            return getType().getTypeSort().isEnumType();
        }

        public boolean isSeq()
        {
            return getType().getTypeSort().isSeqType();
        }

        public boolean isCompound()
        {
            return getType().getTypeSort().isCompoundType();
        }

        public boolean isVlen()
        {
            return this.isvlen;
        }

        public TypeNotes markVlen()
        {
            this.isvlen = true;
            return this;
        }

        public DapType get()
        {
            return (DapType) super.get();
        }

        public TypeNotes set(DapNode node)
        {
            return (TypeNotes) super.set(node);
        }

    }

    static public class VarNotes extends Notes
    {
        protected VarNotes(int g, int v, Nc4DSP dsp)
        {
            super(NoteSort.VAR, g, v, dsp);
        }

        public VarNotes setBaseType(TypeNotes ti)
        {
            return (VarNotes) super.setBaseType(ti);
        }

        public DapVariable get()
        {
            return (DapVariable) super.get();
        }

        public VarNotes set(DapNode node)
        {
            return (VarNotes) super.set(node);
        }

        public int getFieldIndex()
        {
            assert this.get() != null;
            return this.get().getFieldIndex();
        }

        @Override
        public long getSize()
        {
            return this.getBaseType().getSize() * DapUtil.dimProduct(get().getDimensions());
        }

    }
}
