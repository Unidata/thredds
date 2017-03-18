/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib.serial;

import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4lib.AbstractCursor;
import dap4.dap4lib.LibTypeFcns;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class D4Cursor extends AbstractCursor
{
    //////////////////////////////////////////////////
    // Mnemonics
    static final long NULLOFFSET = -1;

    static final int D4LENSIZE = 8;

    //////////////////////////////////////////////////
    // Instance Variables

    protected long offset = NULLOFFSET;

    protected long[] bytestrings = null;

    // For debugging purposes, we keep these separate,
    // but some merging could be done .

    // Track the array elements for a structure array
    protected D4Cursor[] elements = null; // scheme == STRUCTARRAY|SEQARRAY

    // Track the fields of a structure instance
    protected D4Cursor[] fieldcursors = null; // scheme == STRUCTURE|SEQUENCE

    // Track the records of a sequence instance
    protected List<D4Cursor> records = null; // scheme == SEQUENCE

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4Cursor(Scheme scheme, D4DSP dsp, DapNode template, D4Cursor container)
    {
        super(scheme, dsp, template, container);
    }

    /**
     * Effectively a clone of c
     *
     * @param c cursor to clone
     */
    public D4Cursor(D4Cursor c)
    {
        super(c);
        assert false;
        this.offset = c.offset;
        this.bytestrings = c.bytestrings;
        this.fieldcursors = new D4Cursor[c.fieldcursors.length];
        for(int i = 0; i < c.fieldcursors.length; i++) {
            D4Cursor dc = c.fieldcursors[i];
            this.fieldcursors[i] = new D4Cursor(dc);
            this.fieldcursors[i].setContainer(this); // Change the parent in the clone
        }
        this.elements = new D4Cursor[c.elements.length];
        for(int i = 0; i < c.elements.length; i++) {
            D4Cursor dc = c.elements[i];
            this.elements[i] = new D4Cursor(dc);
            this.elements[i].setContainer(this); // Change the parent in the clone
        }
        this.records = new ArrayList<>();
        for(int i = 0; i < c.records.size(); i++) {
            D4Cursor dc = c.records.get(i);
            this.records.add(new D4Cursor(dc));
            this.records.get(i).setContainer(this); // Change the parent in the clone
        }
    }

    //////////////////////////////////////////////////
    // DataCursor API (Except as Implemented in AbstractCursor)


    @Override
    public Object
    read(Index index)
            throws DapException
    {
        return read(DapUtil.indexToSlices(index));
    }

    @Override
    public Object
    read(List<Slice> slices)
            throws DapException
    {
        switch (this.scheme) {
        case ATOMIC:
            return readAtomic(slices);
        case STRUCTURE:
        case SEQUENCE:
            if(((DapVariable) this.getTemplate()).getRank() == 0
                    || DapUtil.isScalarSlices(slices))
                throw new DapException("Cannot slice a scalar variable");
            return new D4Cursor(this);
        case STRUCTARRAY:
            // Read the structures specified by slices
            Odometer odom = Odometer.factory(slices);
            D4Cursor[] instances = new D4Cursor[(int) odom.totalSize()];
            for(int i = 0; odom.hasNext(); i++) {
                instances[i] = readStructure(odom.next());
            }
            return instances;
        case SEQARRAY:
            odom = Odometer.factory(slices);
            instances = new D4Cursor[(int) odom.totalSize()];
            for(int i = 0; odom.hasNext(); i++) {
                instances[i] = readSequence(odom.next());
            }
            return instances;
        default:
            throw new DapException("Attempt to slice a scalar object");
        }
    }

    @Override
    public D4Cursor
    readField(int findex)
            throws DapException
    {
        assert (this.scheme == scheme.RECORD || this.scheme == scheme.STRUCTURE);
        DapStructure basetype = (DapStructure) ((DapVariable) getTemplate()).getBaseType();
        if(findex < 0 || findex >= basetype.getFields().size())
            throw new DapException("Field index out of range: " + findex);
        D4Cursor field = this.fieldcursors[findex];
        return field;
    }

    @Override
    public D4Cursor
    readRecord(long i)
    {
        assert (this.scheme == Scheme.SEQUENCE);
        if(this.records == null || i < 0 || i > this.records.size())
            throw new IndexOutOfBoundsException("No such record: " + i);
        return this.records.get((int) i);
    }

    @Override
    public long
    getRecordCount()
    {
        assert (this.scheme == Scheme.SEQUENCE);
        return this.records == null ? 0 : this.records.size();
    }

    //////////////////////////////////////////////////
    // Support methods

    protected Object
    readAtomic(List<Slice> slices)
            throws DapException
    {
        if(slices == null)
            throw new DapException("DataCursor.read: null set of slices");
        assert this.scheme == Scheme.ATOMIC;
        DapVariable atomvar = (DapVariable) getTemplate();
        int rank = atomvar.getRank();
        assert slices != null && ((rank == 0 && slices.size() == 1) || (slices.size() == rank));
        DapType basetype = atomvar.getBaseType();
        return readAs(atomvar, basetype, slices);
    }

    /**
     * Allow specification of basetype to use; used for enumerations
     *
     * @param atomvar
     * @param basetype
     * @param slices
     * @return
     * @throws DapException
     */
    protected Object
    readAs(DapVariable atomvar, DapType basetype, List<Slice> slices)
            throws DapException
    {
        if(basetype.getTypeSort() == TypeSort.Enum) {// short circuit this case
            basetype = ((DapEnumeration) basetype).getBaseType();
            return readAs(atomvar, basetype, slices);
        }
        long count = DapUtil.sliceProduct(slices);
        Object result = LibTypeFcns.newVector(basetype, count);
        Odometer odom = Odometer.factory(slices);
        if(DapUtil.isContiguous(slices) && basetype.isFixedSize())
            readContig(slices, basetype, count, odom, result);
        else
            readOdom(slices, basetype, odom, result);
        return result;
    }

    protected void
    readContig(List<Slice> slices, DapType basetype, long count, Odometer odom, Object result)
            throws DapException
    {
        ByteBuffer alldata = ((D4DSP) this.dsp).getBuffer();
        long off = this.offset;
        long ix = odom.indices().index();
        int elemsize = basetype.getSize();
        alldata.position((int) (off + (ix * elemsize)));
        int icount = (int) count;
        long totalsize = count * basetype.getSize();
        switch (basetype.getTypeSort()) {
        case Int8:
        case UInt8:
            alldata.get((byte[]) result);
            break;
        case Char: // remember, we are reading 7-bit ascii, not utf-8 or utf-16
            byte[] ascii = new byte[icount];
            alldata.get(ascii);
            for(int i = 0; i < icount; i++) {
                ((char[]) result)[i] = (char) (ascii[i] & 0x7f);
            }
            break;
        case Int16:
        case UInt16:
            alldata.asShortBuffer().get((short[]) result);
            skip(totalsize, alldata);
            break;
        case Int32:
        case UInt32:
            alldata.asIntBuffer().get((int[]) result);
            skip(totalsize, alldata);
            break;
        case Int64:
        case UInt64:
            alldata.asLongBuffer().get((long[]) result);
            skip(totalsize, alldata);
            break;
        case Float32:
            alldata.asFloatBuffer().get((float[]) result);
            skip(totalsize, alldata);
            break;
        case Float64:
            alldata.asDoubleBuffer().get((double[]) result);
            skip(totalsize, alldata);
            break;
        default:
            throw new DapException("Contiguous read not supported for type: " + basetype.getTypeSort());
        }
    }

    protected Object
    readOdom(List<Slice> slices, DapType basetype, Odometer odom, Object result)
            throws DapException
    {
        ByteBuffer alldata = ((D4DSP) this.dsp).getBuffer();
        alldata.position((int) this.offset);
        ByteBuffer slice = alldata.slice();
        slice.order(alldata.order());
        for(int i = 0; odom.hasNext(); i++) {
            Index index = odom.next();
            int ipos = (int) index.index();
            switch (basetype.getTypeSort()) {
            case Int8:
            case UInt8:
                ((byte[]) result)[i] = slice.get(ipos);
                break;
            case Char: // remember, we are reading 7-bit ascii, not utf-8 or utf-16
                byte ascii = slice.get(ipos);
                ((char[]) result)[i] = (char) ascii;
                break;
            case Int16:
            case UInt16:
                ((short[]) result)[i] = slice.getShort(ipos);
                break;
            case Int32:
            case UInt32:
                ((int[]) result)[i] = slice.getInt(ipos);
                break;
            case Int64:
            case UInt64:
                ((long[]) result)[i] = slice.getLong(ipos);
                break;
            case Float32:
                ((float[]) result)[i] = slice.getFloat(ipos);
                break;
            case Float64:
                ((double[]) result)[i] = slice.getDouble(ipos);
                break;
            case String:
            case URL:
                int savepos = alldata.position();
                long pos = bytestrings[i];
                alldata.position((int) pos); // bytestring offsets are absolute
                long n = getLength(alldata);
                byte[] data = new byte[(int) n];
                alldata.get(data);
                ((String[]) result)[i] = new String(data, DapUtil.UTF8);
                alldata.position(savepos);
                break;
            case Opaque:
                savepos = alldata.position();
                pos = bytestrings[i];
                alldata.position((int) pos); // bytestring offsets are absolute
                n = getLength(alldata);
                data = new byte[(int) n];
                alldata.get(data);
                ByteBuffer buf = ByteBuffer.wrap(data);
                ((ByteBuffer[]) result)[i] = buf;
                alldata.position(savepos);
                break;
            default:
                throw new DapException("Attempt to read non-atomic value of type: " + basetype.getTypeSort());
            }
        }
        return result;
    }

    protected D4Cursor
    readStructure(Index index)
            throws DapException
    {
        assert (this.scheme == Scheme.STRUCTARRAY);
        long pos = index.index();
        long avail = (this.elements == null ? 0 : this.elements.length);
        if(pos < 0 || pos > avail)
            throw new IndexOutOfBoundsException("read: " + index);
        return this.elements[(int) pos];
    }

    public D4Cursor
    readSequence(Index index)
            throws DapException
    {
        assert (this.scheme == Scheme.SEQARRAY);
        long pos = index.index();
        long avail = (this.elements == null ? 0 : this.elements.length);
        if(pos < 0 || pos > avail)
            throw new IndexOutOfBoundsException("read: " + index);
        return this.elements[(int) pos];
    }

    //////////////////////////////////////////////////
    // D4Cursor Extensions

    public D4Cursor
    setElements(D4Cursor[] instances)
    {
        if(!(getScheme() == Scheme.SEQARRAY
                || getScheme() == Scheme.STRUCTARRAY))
            throw new IllegalStateException("Adding element to !(structure|sequence array) object");
        DapVariable var = (DapVariable) getTemplate();
        this.elements = instances;
        return this;
    }

    public D4Cursor
    setOffset(long pos)
    {
        this.offset = pos;
        return this;
    }

    public D4Cursor
    setByteStringOffsets(long total, long[] positions)
    {
        this.bytestrings = positions;
        return this;
    }

    public D4Cursor
    addField(int m, D4Cursor field)
    {
        if(getScheme() != Scheme.RECORD && getScheme() != Scheme.STRUCTURE)
            throw new IllegalStateException("Adding field to non-(structure|record) object");
        if(this.fieldcursors == null) {
            DapStructure ds = (DapStructure) ((DapVariable) getTemplate()).getBaseType();
            List<DapVariable> fields = ds.getFields();
            this.fieldcursors = new D4Cursor[fields.size()];
        }
        if(this.fieldcursors[m] != null)
            throw new IndexOutOfBoundsException("Adding duplicate fields at position:" + m);
        this.fieldcursors[m] = field;
        return this;
    }

    public D4Cursor
    addRecord(D4Cursor rec)
    {
        if(getScheme() != Scheme.SEQUENCE)
            throw new IllegalStateException("Adding record to non-sequence object");
        if(this.records == null)
            this.records = new ArrayList<>();
        this.records.add(rec);
        return this;
    }

    public long
    getElementSize(DapVariable v)
    {
        return v.getBaseType().isFixedSize() ? v.getBaseType().getSize() : 0;
    }

    static ByteBuffer
    skip(long n, ByteBuffer b)
    {
        if(b.position() + ((int) n) > b.limit())
            throw new IllegalArgumentException();
        b.position(b.position() + ((int) n));
        return b;
    }

    static public long
    getLength(ByteBuffer b)
    {
        if(b.position() + D4LENSIZE > b.limit())
            throw new IllegalArgumentException();
        long n = b.getLong();
        return n;
    }

}
