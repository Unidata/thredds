/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib.netcdf;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import dap4.core.data.DataCursor;
import dap4.core.dmr.*;
import dap4.core.util.*;

import java.nio.ByteBuffer;
import java.util.List;

import static dap4.dap4lib.netcdf.Nc4Notes.*;

public class Nc4Cursor implements DataCursor
{

    //////////////////////////////////////////////////
    // Instance variables

    protected Scheme scheme;

    protected Nc4DSP dsp = null;
    protected DapNode template = null;

    protected Pointer memory = null;

    // Computed instance variables
    DapSort sort;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Nc4Cursor(Scheme scheme, DapNode template, Nc4DSP dsp)
            throws DapException
    {
        this.scheme = scheme;
        this.template = template;
        this.dsp = dsp;
        this.sort = template.getSort();
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append(getScheme().toString());
        if(getScheme() == Scheme.STRUCTARRAY || getScheme() == Scheme.SEQARRAY)
            buf.append("[]");
        buf.append(":");
        buf.append(getTemplate().getFQN());
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // DataCursor Interface

    @Override
    public Scheme getScheme()
    {
        return this.scheme;
    }

    @Override
    public Nc4DSP getDSP()
    {
        return this.dsp;
    }

    @Override
    public DapNode getTemplate()
    {
        return this.template;
    }

    public Object
    read(List<Slice> slices)
            throws DapException
    {
        try {
            DataCursor[] instances = null;
            switch (this.scheme) {
            case ATOMIC:
                return readAtomic(slices);
            case STRUCTARRAY:
                Odometer odom = Odometer.factory(slices);
                instances = new DataCursor[(int) odom.totalSize()];
                for(int i = 0; odom.hasNext(); i++) {
                    instances[i] = readStructure(odom.next());
                }
                return instances;
            case SEQARRAY:
                odom = Odometer.factory(slices);
                instances = new DataCursor[(int) odom.totalSize()];
                for(int i = 0; odom.hasNext(); i++) {
                    instances[i] = readSequence(odom.next());
                }
                return instances;
            default:
                throw new DapException("Unexpected data cursor type: " + this.scheme);
            }
        } catch (DapException e) {
            throw new DapException(e);
        }
    }

    public Object
    read(Index index)
            throws DapException
    {
        switch (this.scheme) {
        case ATOMIC:
            return readAtomic(DapUtil.indexToSlices(index, (DapAtomicVariable) getTemplate()));
        case STRUCTARRAY:
            return readStructure(index);
        case SEQARRAY:
            return readSequence(index);
        default:
            break;
        }
        throw new DapException("Unexpected cursor scheme: " + this.scheme);
    }

    protected Object
    readAtomic(List<Slice> slices)
            throws DapException
    {
        if(slices == null)
            throw new DapException("DataCursor.read: null set of slices");
        assert (this.scheme == scheme.ATOMIC);
        DapAtomicVariable atomvar = (DapAtomicVariable) getTemplate();
        int rank = atomvar.getRank();
        assert slices != null && slices.size() == rank;
        // Get VarNotes and TypeNotes
        Notes n = (Notes) this.template.annotation();
        Object result = null;
        long count = DapUtil.sliceProduct(slices);
        if(template.isTopLevel()) {
            VarNotes vi = (VarNotes) n;
            TypeNotes ti = vi.basetype;
            if(rank == 0) { //scalar
                result = readAtomicScalar(vi, ti);
            } else {
                result = readAtomicVector(vi, ti, count, slices);
            }
        } else {// field of a structure instance or record
            FieldNotes fn = (FieldNotes) n;
            TypeNotes ti = fn.basetype;
            long elemsize = ((DapType) ti.get()).getSize();
            Pointer mem = this.getMemory().share(fn.getOffset(), count * elemsize);  //FIX
            result = getatomicdata(ti.getType(), count, elemsize, mem);
        }
        return result;
    }

    protected Object
    readAtomicScalar(VarNotes vi, TypeNotes ti)
            throws DapException
    {
        DapAtomicVariable atomvar = (DapAtomicVariable) getTemplate();
        // Get into memory
        DapNetcdf nc4 = this.dsp.getJNI();
        int ret;
        DapType basetype = ti.getType();
        Object result = null;
        if(basetype.isFixedSize()) {
            long memsize = ((DapType) ti.get()).getSize();
            Pointer mem = Mem.allocate(memsize);
            readcheck(nc4, ret = nc4.nc_get_var(vi.gid, vi.id, mem));
            result = getatomicdata(ti.getType(), 1, memsize, mem);
        } else if(basetype.isStringType()) {
            String[] s = new String[1];
            readcheck(nc4, ret = nc4.nc_get_var_string(vi.gid, vi.id, s));
            result = s;
        } else if(basetype.isOpaqueType()) {
            Pointer mem = Mem.allocate(ti.opaquelen);
            readcheck(nc4, ret = nc4.nc_get_var(vi.gid, vi.id, mem));
            ByteBuffer[] buf = new ByteBuffer[1];
            buf[0] = mem.getByteBuffer(0, ti.opaquelen);
            result = buf;
        } else
            throw new DapException("Unexpected atomic type: " + basetype);
        return result;
    }

    protected Object
    readAtomicVector(VarNotes vi, TypeNotes ti, long count, List<Slice> slices)
            throws DapException
    {
        DapAtomicVariable atomvar = (DapAtomicVariable) getTemplate();
        // Get into memory
        DapNetcdf nc4 = this.dsp.getJNI();
        DapType basetype = ti.getType();
        // Convert slices to (start,count,stride);
        int rank = atomvar.getRank();
        SizeT[] startp = new SizeT[rank];
        SizeT[] countp = new SizeT[rank];
        SizeT[] stridep = new SizeT[rank];
        slicesToVars(slices, startp, countp, stridep);
        int ret;
        Object result = null;
        if(basetype.isFixedSize()) {
            Odometer odom = Odometer.factory(slices);
            long elemsize = ((DapType) ti.get()).getSize();
            long memsize = count * elemsize;
            Pointer mem = Mem.allocate(memsize);
            readcheck(nc4, ret = nc4.nc_get_vars(vi.gid, vi.id, startp, countp, stridep, mem));
            result = getatomicdata(ti.getType(), count, elemsize, mem);
        } else if(basetype.isStringType()) {
            String[] ss = new String[(int) count];
            readcheck(nc4, ret = nc4.nc_get_vars_string(vi.gid, vi.id, startp, countp, stridep, ss));
            result = ss;
        } else if(basetype.isOpaqueType()) {
            Pointer mem = Mem.allocate(count * ti.opaquelen);
            readcheck(nc4, ret = nc4.nc_get_var(vi.gid, vi.id, mem));
            ByteBuffer[] buf = new ByteBuffer[(int) count];
            for(int i = 0; i < count; i++) {
                buf[i] = mem.getByteBuffer(ti.opaquelen * i, ti.opaquelen);
            }
            result = buf;
        } else
            throw new DapException("Unexpected atomic type: " + basetype);
        return result;
    }

    protected Nc4Cursor
    readStructure(Index index)
            throws DapException
    {
        assert (this.scheme == scheme.STRUCTARRAY);
        assert (index != null);
        DapStructure template = (DapStructure) getTemplate();
        long pos = index.index();
        if(pos < 0 || pos >= template.getCount())
            throw new IndexOutOfBoundsException("read: " + index);
        Pointer mem;
        VarNotes vi = (VarNotes) template.annotation();
        TypeNotes ti = vi.basetype;
        DapStructure stvar = (DapStructure) template;
        if(template.isTopLevel()) {
            int ret;
            mem = Mem.allocate(ti.compoundsize);
            DapNetcdf nc4 = this.dsp.getJNI();
            SizeT[] sizes = indexToSizes(index);
            readcheck(nc4, ret = nc4.nc_get_var1(vi.gid, vi.id, sizes, mem));
        } else {// field of a structure instance or record
            // Ok, we need to operate relative to the parent's memory
            // move to the appropriate offset
            mem = this.getMemory().share(pos * ti.compoundsize, ti.compoundsize);
        }
        return new Nc4Cursor(Scheme.STRUCTURE, stvar, this.dsp)
                .setMemory(mem);
    }

    public Nc4Cursor
    getField(int findex)
            throws DapException
    {
        assert (this.scheme == scheme.RECORD || this.scheme == scheme.STRUCTURE);
        DapStructure template = (DapStructure) getTemplate();
        DapVariable field = template.getField(findex);
        // Get VarNotes and TypeNotes
        FieldNotes fi = (FieldNotes) field.annotation();
        long dimproduct = DapUtil.dimProduct(template.getDimensions());
        TypeNotes ti = fi.basetype;
        long elemsize = getElementSize(ti); // read only one instance
        long totalsize = elemsize * dimproduct;
        Nc4Cursor cursor = null;
        TypeSort typesort = ti.getType().getTypeSort();
        if(typesort.isAtomic()) {
            cursor = new Nc4Cursor(Scheme.ATOMIC, field, this.dsp);
        } else if(typesort == TypeSort.Structure) {
            if(field.getRank() == 0)
                cursor = new Nc4Cursor(Scheme.STRUCTURE, ti.getType(), this.dsp);
            else
                cursor = new Nc4Cursor(Scheme.STRUCTARRAY, ti.getType(), this.dsp);
        } else if(typesort == TypeSort.Sequence)
            throw new UnsupportedOperationException();
        // as a rule, a field's memory is its parent container memory.
        return cursor.setMemory(this.getMemory());
    }

    public Nc4Cursor
    readSequence(Index index)
            throws DapException
    {
        assert (this.scheme == scheme.SEQARRAY);
        throw new UnsupportedOperationException();
    }

    public long getRecordCount()
    {
        assert (this.scheme == scheme.SEQUENCE);
        throw new UnsupportedOperationException("Not a Sequence");
    }

    public DataCursor
    getRecord(long i)
    {
        assert (this.scheme == scheme.SEQUENCE);
        throw new UnsupportedOperationException("Not a Sequence");
    }

    //////////////////////////////////////////////////
    // Nc4Cursor Extensions

    public Pointer
    getMemory()
    {
        return this.memory;
    }

    public Nc4Cursor
    setMemory(Pointer p)
    {
        this.memory = p;
        return this;
    }

    //////////////////////////////////////////////////
    // Type Decls

    // com.sun.jna.Memory control

    static /*package*/ abstract class Mem
    {
        static Memory
        allocate(long size)
        {
            if(size == 0)
                throw new IllegalArgumentException("Attempt to allocate zero bytes");
            Memory m = new Memory(size);
            return m;
        }

    }

    //////////////////////////////////////////////////

    protected long
    getElementSize(TypeNotes ti)
    {
        DapType type = ti.getType();
        switch (type.getTypeSort()) {
        case Structure:
        case Sequence:
            throw new IllegalArgumentException();
        case String:
        case URL:
            return Pointer.SIZE;
        case Enum:
            return getElementSize(TypeNotes.find(ti.enumbase));
        case Opaque:
            return ti.opaquelen;
        default:
            return type.getSize();
        }
    }

    protected Object
    getatomicdata(DapType basetype, long lcount, long elemsize, Pointer mem)
    {
        Object result = null;
        TypeSort sort = basetype.getTypeSort();
        int icount = (int) lcount;
        switch (sort) {
        case Char:
            // need to extract and convert utf8(really ascii) -> utf16
            byte[] bresult = mem.getByteArray(0, icount);
            char[] cresult = new char[bresult.length];
            for(int i = 0; i < icount; i++) {
                int ascii = bresult[i];
                ascii = ascii & 0x7F;
                cresult[i] = (char) ascii;
            }
            result = cresult;
            break;
        case UInt8:
        case Int8:
            result = mem.getByteArray(0, icount);
            break;
        case Int16:
        case UInt16:
            result = mem.getShortArray(0, icount);
            break;
        case Int32:
        case UInt32:
            result = mem.getIntArray(0, icount);
            break;
        case Int64:
        case UInt64:
            result = mem.getLongArray(0, icount);
            break;
        case Float32:
            result = mem.getFloatArray(0, icount);
            break;
        case Float64:
            result = mem.getDoubleArray(0, icount);
            break;
        case String:
        case URL:
            // TODO: properly free underlying strings
            result = mem.getStringArray(0, icount);
            break;
        case Opaque:
            ByteBuffer[] ops = new ByteBuffer[icount];
            result = ops;
            for(int i = 0; i < icount; i++) {
                ops[i] = mem.getByteBuffer(i * elemsize, elemsize);
            }
            break;
        case Enum:
            DapEnumeration de = (DapEnumeration) basetype;
            result = getatomicdata((DapType) de.getBaseType(), lcount, elemsize, mem);
            break;
        }
        return result;
    }

    //////////////////////////////////////////////////
    // Utilities

    static void
    slicesToVars(List<Slice> slices, SizeT[] startp, SizeT[] countp, SizeT[] stridep)
    {
        for(int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            startp[i] = new SizeT(slice.getFirst());
            countp[i] = new SizeT(slice.getCount());
            stridep[i] = new SizeT(slice.getStride());
        }
    }

    static public void
    errcheck(DapNetcdf nc4, int ret)
            throws DapException
    {
        if(ret != 0) {
            String msg = String.format("TestNetcdf: errno=%d; %s", ret, nc4.nc_strerror(ret));
            throw new DapException(msg);
        }
    }

    static public void
    readcheck(DapNetcdf nc4, int ret)
            throws DapException
    {
        try {
            errcheck(nc4, ret);
        } catch (DapException de) {
            throw new DapException(de);
        }
    }

    static SizeT[]
    indexToSizes(Index index)
    {
        SizeT[] sizes = new SizeT[index.getRank()];
        for(int i=0;i<sizes.length;i++) {
            sizes[i] = new SizeT(index.get(i));
        }
        return sizes;
    }

}
