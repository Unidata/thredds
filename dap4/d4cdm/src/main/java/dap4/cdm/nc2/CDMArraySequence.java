/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm.nc2;

import dap4.cdm.CDMTypeFcns;
import dap4.core.data.DSP;
import dap4.core.data.DataCursor;
import dap4.core.dmr.DapSequence;
import dap4.core.dmr.DapStructure;
import dap4.core.dmr.DapType;
import dap4.core.dmr.DapVariable;
import dap4.core.util.*;
import ucar.ma2.*;
import ucar.nc2.Group;

import java.io.IOException;
import java.util.List;

/**
 * CDM now has an ArraySequence type intended to
 * support VLEN (aka CDM (*) dimension).
 * So, sequence is simulated as a rank n+1 structure where
 * the last dimension is "*" (i.e. variable length).
 * That is, given the following DAP4:
 * Sequence S {f1,f2,...fm} [d1][d2]...[dn]
 * Represent it in CDM as this:
 * Structure S {f1,f2,...fm} [d1][d2]...[dn][*]
 * We cannot subclass CDMArrayStructure because we need to subclass
 * ArraySequence, so we are forced to duplicate a lot of the CDMArrayStructure
 * code.
 * The important point to note is that for CDM, we do not need to support
 * Dimensioned sequences; the dimensions are supported by the enclosing
 * ArrayStructure covering the non-vlen dimensions
 */

/*package*/ class CDMArraySequence extends ArraySequence implements CDMArray
{

    //////////////////////////////////////////////////
    // Type decls

    // Define an open wrapper around a field array in order
    // to make the code somewhat more clear

    static protected class FieldSet
    {
        public Array[] fields;

        FieldSet(int nfields)
        {
            fields = new Array[nfields];
        }
    }

    static public class SDI implements StructureDataIterator
    {
        protected StructureData[] list;
        protected int position;

        public SDI()
        {
            this.list = null;
            this.position = 0;
        }

        public SDI
        setList(StructureData[] list)
        {
            this.list = list;
            return this;
        }

        public boolean hasNext() throws IOException
        {
            return position < list.length;
        }

        public StructureData next() throws IOException
        {
            if(position >= list.length)
                throw new IOException("No next element");
            return list[position++];
        }

        public StructureDataIterator reset()
        {
            position = 0;
            return this;
        }

        public int getCurrentRecno()
        {
            return position;
        }

    }

    //////////////////////////////////////////////////
    // Instance variables

    protected Group cdmroot = null;
    protected DSP dsp;
    protected DapVariable template;
    protected DapType basetype;
    protected long bytesize = 0;
    protected long recordcount = 0;
    protected int nmembers = 0;

    protected DataCursor seqdata = null;

    /**
     * Since in CDM a sequence is the last dimension of
     * array, we do not need to keep dimensionality info, only
     * the variable length stuff, which we do using
     * StructureDataA instances: 1 per record.
     */

    // Track the records of this sequence as an array
    // Note: term records here means the elements of the array,
    // not record as in Sequence

    protected FieldSet[] records = null; // list of records

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * Constructor
     *
     * @param data
     */
    CDMArraySequence(Group group, DataCursor data)
            throws DapException
    {
        super(CDMArrayStructure.computemembers((DapVariable) data.getTemplate()),
                new SDI(), 0);
        this.template = (DapVariable) data.getTemplate();
        this.basetype =  this.template.getBaseType();
        // Currently do not allow non-scalar sequences
        if(this.template.getRank() != 0)
            throw new DapException("Non-scalar sequences unsupported through CDM interface");
        assert data.getScheme() == DataCursor.Scheme.SEQARRAY;
        this.cdmroot = group;
        this.dsp = dsp;
        // Since this is a scalar, pull out the single instance
        this.seqdata = ((DataCursor[])data.read(dap4.core.util.Index.SCALAR))[0];
        this.recordcount = this.seqdata.getRecordCount();
        this.nmembers = ((DapStructure)this.basetype).getFields().size();

        // Fill in the structdata (in parent) and record vectors
        super.sdata = new StructureDataA[(int) this.recordcount];
        records = new FieldSet[(int) this.recordcount];
        for(int i = 0; i < this.recordcount; i++) {
            super.sdata[i] = new StructureDataA(this, i);
            records[i] = new FieldSet(this.nmembers);
        }

        ((SDI) super.iter).setList(super.sdata);
    }

    //////////////////////////////////////////////////
    // Compiler API

    /*package*/
    void
    add(long recno, int fieldno, Array field)
    {
        //Make sure all the space is allocated
        if(records.length <= recno) {
            FieldSet[] newrecs = new FieldSet[(int) recno + 1];
            System.arraycopy(records, 0, newrecs, 0, records.length);
            records = newrecs;
        }
        FieldSet fs = records[(int) recno];
        if(fs == null) {
            records[(int) recno] = (fs = new FieldSet(this.nmembers));
        }
        fs.fields[fieldno] = field;
    }

    //////////////////////////////////////////////////
    // CDMArray Interface

    @Override
    public DapType getBaseType()
    {
        return this.basetype;
    }

    @Override
    public DSP getDSP()
    {
        return this.dsp;
    }

    @Override
    public DapVariable getTemplate()
    {
        return template;
    }

    //////////////////////////////////////////////////

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        DapVariable var = this.template;
        DapSequence seq = (DapSequence)this.basetype;
        long dimsize = DapUtil.dimProduct(var.getDimensions());
        for(int i = 0; i < dimsize; i++) {
            List<DapVariable> fields = seq.getFields();
            if(i < (dimsize - 1))
                buf.append("\n");
            buf.append("Sequence {\n");
            buf.append(String.format("} [%d/%d]", i, dimsize));
        }
        return buf.toString();
    }


    //////////////////////////////////////////////////
    // ArraySequence/ArrayStructure overrides

    @Override
    public int getStructureDataCount()
    {
        return this.records.length;
    }

    @Override
    protected StructureData makeStructureData(ArrayStructure as, int index)
    {
        throw new UnsupportedOperationException("Cannot subset a Sequence");
    }

    /**
     * Get the index'th StructureData(StructureDataA) object
     * We need instances of StructureData to give to the user.
     * We use StructureDataA so we can centralize everything
     * in this class. The total number of StructureData objects
     * is dimsize.
     *
     * @param index
     * @return
     */
    @Override
    public StructureData getStructureData(int index)
    {
        assert (super.sdata != null);
        if(index < 0 || index >= this.records.length)
            throw new IllegalArgumentException(index + " >= " + super.sdata.length);
        assert (super.sdata[index] != null);
        return super.sdata[index];
    }

    public ArraySequence getArraySequence(StructureMembers.Member m)
    {
        return this;
    }

    /////////////////////////
    // Define API required by StructureDataA
    @Override
    public Array copy()
    {
        return this; // temporary
    }

    /**
     * Get member data of any type for a specific record as an Array.
     * This may avoid the overhead of creating the StructureData object,
     * but is equivalent to getStructure(recno).getArray( Member m).
     *
     * @param recno get data from the recnum-th StructureData of the ArrayStructure.
     *              Must be less than getSize();
     * @param m     get data from this StructureMembers.Member.
     * @return Array values.
     */
    public Array getArray(int recno, StructureMembers.Member m)
    {
        return (ucar.ma2.Array) memberArray(recno, CDMArrayStructure.memberIndex(m));
    }

    /////////////////////////

    protected CDMArrayAtomic
    getAtomicArray(int index, StructureMembers.Member m)
    {
        Array dd = memberArray(index, CDMArrayStructure.memberIndex(m));
        if(dd.getDataType() != DataType.STRUCTURE && dd.getDataType() != DataType.SEQUENCE)
            return (CDMArrayAtomic) dd;
        throw new ForbiddenConversionException("Cannot convert structure to AtomicArray");
    }

    protected Array
    memberArray(int recno, int memberindex)
    {

        Object[] values = new Object[(int) this.recordcount];
        for(int i = 0; i < this.recordcount; i++) {
            FieldSet fs = records[i];
            values[i] = fs.fields[memberindex];
        }
        DapVariable field = ((DapStructure)this.basetype).getField(memberindex);
        DapType base = field.getBaseType();
        if(base == null)
            throw new IllegalStateException("Unknown field type: "+field);
        DataType dt = CDMTypeFcns.daptype2cdmtype(base);
        Class elemtype = CDMTypeFcns.cdmElementClass(dt);
        int shape[] = new int[]{(int) this.recordcount};
        return new ArrayObject(dt, elemtype, false, shape, values);
    }
}

