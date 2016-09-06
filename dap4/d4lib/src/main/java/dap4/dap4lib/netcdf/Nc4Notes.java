/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.dap4lib.netcdf;

import dap4.core.dmr.*;

import java.util.HashMap;
import java.util.Map;

import static dap4.dap4lib.netcdf.DapNetcdf.*;

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
    // Type Decls

    static public class Notes implements Cloneable
    {
        int gid;
        int id;
        DapNode node = null;

        public Notes(int gid, int id)
        {
            this.gid = gid;
            this.id = id;
        }

        public Object clone()
        //throws CloneNotSupportedException
        {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }


        public Notes set(DapNode node)
        {
            this.node = node;
            node.annotate(this);
            return this;
        }

        public DapNode get()
        {
            return this.node;
        }

        public DapDecl getDecl()
        {
            return (DapDecl) this.node;
        }

        DapGroup group()
        {
            GroupNotes g = GroupNotes.find(gid);
            return (g == null ? null : g.getGroup());
        }
    }

    static public class GroupNotes extends Notes
    {
        static Map<Integer, GroupNotes> allgroups = new HashMap<>();

        static public GroupNotes find(int gid)
        {
            return allgroups.get(gid);
        }

        public GroupNotes(int p, int g)
        {
            super(p, g);
            allgroups.put(g, this);
        }

        public DapGroup getGroup()
        {
            return (DapGroup) this.node;
        }

        public GroupNotes set(DapNode node)
        {
            return (GroupNotes) super.set(node);
        }

    }

    static public class DimNotes extends Notes
    {
        static Map<Integer, DimNotes> alldims = new HashMap<>();

        static public DimNotes find(int id)
        {
            return alldims.get(id);
        }

        public DimNotes(int g, int id)
        {
            super(g, id);
            alldims.put(id, this);
        }

        public DapDimension getDim()
        {
            return (DapDimension) this.node;
        }

        public DimNotes set(DapNode node)
        {
            return (DimNotes) super.set(node);
        }

    }

    static public class TypeNotes extends Notes
    {
        static Map<Integer, TypeNotes> alltypes = new HashMap<>();

        static public TypeNotes find(int id)
        {
            return alltypes.get(id);
        }

        static public TypeNotes find(DapType dt)
        {
            for(Map.Entry<Integer, TypeNotes> entry : alltypes.entrySet()) {
                if(entry.getValue().getType() == dt) {
                    return entry.getValue();
                }
            }
            return null;
        }

        public int opaquelen = -1;
        public int enumbase = -1;
        public int compoundsize = -1;
        public boolean isvlen = false;

        public TypeNotes(int g, int id)
        {
            super(g, id);
            alltypes.put(id, this);
        }

        public DapType getType()
        {
            return (DapType) this.node;
        }

        public TypeNotes setOpaque(int len)
        {
            opaquelen = len;
            return this;
        }

        public TypeNotes setEnumBaseType(int bt)
        {
            this.enumbase = bt;
            return this;
        }

        public boolean isOpaque()
        {
            return this.opaquelen >= 0;
        }

        public boolean isEnum()
        {
            return this.enumbase >= 0;
        }

        public boolean isCompound()
        {
            return this.compoundsize >= 0;
        }

        public boolean isVlen()
        {
            return this.isvlen;
        }

        public TypeNotes setCompoundSize(int size)
        {
            this.compoundsize = size;
            return this;
        }

        public TypeNotes markVlen()
        {
            this.isvlen = true;
            return this;
        }

        public TypeNotes set(DapNode node)
        {
            return (TypeNotes) super.set(node);
        }

        static {
            new TypeNotes(0, NC_BYTE).set(DapType.INT8);
            new TypeNotes(0, NC_CHAR).set(DapType.CHAR);
            new TypeNotes(0, NC_SHORT).set(DapType.INT16);
            new TypeNotes(0, NC_INT).set(DapType.INT32);
            new TypeNotes(0, NC_FLOAT).set(DapType.FLOAT32);
            new TypeNotes(0, NC_DOUBLE).set(DapType.FLOAT64);
            new TypeNotes(0, NC_UBYTE).set(DapType.UINT8);
            new TypeNotes(0, NC_USHORT).set(DapType.UINT16);
            new TypeNotes(0, NC_UINT).set(DapType.UINT32);
            new TypeNotes(0, NC_INT64).set(DapType.INT64);
            new TypeNotes(0, NC_UINT64).set(DapType.UINT64);
            new TypeNotes(0, NC_STRING).set(DapType.STRING);
        }

    }

    static public class VarNotes extends Notes
    {
        static Map<Long, VarNotes> allvars = new HashMap<>();

        static public VarNotes find(int gid, int vid)
        {
            long gv = (((long) gid) << 32) | vid;
            return allvars.get(gv);
        }

        protected TypeNotes basetype = null;

        public VarNotes(int g, int v)
        {
            super(g, v);
            long gv = (((long) g) << 32) | v;
            allvars.put(gv, this);

        }

        public VarNotes setBaseType(TypeNotes ti)
        {
            this.basetype = ti;
            return this;
        }

        public DapVariable getVar()
        {
            return (DapVariable) this.node;
        }

        public VarNotes set(DapNode node)
        {
            return (VarNotes) super.set(node);
        }

    }

    static public class FieldNotes extends Notes
    {
        protected TypeNotes parent = null;
        protected TypeNotes basetype = null;
        protected int fieldid = NOFIELDID;
        protected long offset = -1;

        public FieldNotes(TypeNotes parent, int fid, long offset)
        {
            super(NOGROUP, NOID);
            this.parent = parent;
            this.fieldid = fid;
            this.offset = offset;
        }

        public FieldNotes setBaseType(TypeNotes ti)
        {
            this.basetype = ti;
            return this;
        }

        public VarNotes set(DapNode node)
        {
            return (VarNotes) super.set(node);
        }

        public int getFieldID()
        {
            return this.fieldid;
        }

        public long getOffset()
        {
            return this.offset;
        }

        public TypeNotes getParent()
        {
            return this.parent;
        }

        public DapVariable getVar()
        {
            return (DapVariable) get();
        }
    }

}
