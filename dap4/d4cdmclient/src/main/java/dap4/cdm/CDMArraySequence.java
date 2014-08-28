/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm;

import dap4.cdmshared.CDMUtil;
import dap4.core.data.DataException;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4shared.*;
import ucar.ma2.*;
import ucar.nc2.Dimension;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * CDM now has an ArraySequence type intended to
 * support VLEN.
 * So, sequence is simulated as a n+1 D structure where
 * the last dimension is "*" (i.e. variable length).
 * That is, given the following DAP4:
 * Sequence S {f1,f2,...fm} [d1][d2]...[dn]
 * Represent it in CDM as this:
 * Structure S {f1,f2,...fm} [d1][d2]...[dn][*]
 * <p/>
 * With respect to the data, the above is stored
 * as an n-D array of ArrayObject instances where the
 * leaf objects are instances of (CDM)ArraySequence.
 * <p/>
 * Internally, the sequence stored as a 2-D ragged array
 * CDMArray[][] records.
 * The first dimension has varying lengths representing
 * the (variable length) set of records in an instance
 * of a Sequence.
 * The second dimension has size |members| i.e. the number
 * of fields in the sequence.
 * <p/>
 * We cannot subclass CDMArrayStructure because we need to subclass
 * ArraySequence, so we are forced to duplicate a lot of the CDMArrayStructure
 * code.
 */

public class CDMArraySequence extends ArraySequence implements CDMArray
{

    //////////////////////////////////////////////////
    // Type decls

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

        public void setBufferSize(int bytes)
        {
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

        public void finish()
        {
        }

    }
    //////////////////////////////////////////////////
    // Instance variables

    protected CDMDataset root = null;
    protected D4DSP dsp = null;
    protected DapVariable template = null;
    protected long bytesize = 0;

    protected D4DataSequence d4data = null;
    protected long nmembers = 0;

    /**
     * As mentioned above, we store an array of
     * arrays of CDMArrays, where each CDMArray innstance
     * represents a single record in some D4DataSequence object.
     * Total number of objects is |records| * |members|,
     * where |members| is the number of field members, and |records| is
     * the total number of records in the sequence instance.
     */

    protected Array[][] records = null;
    protected long nrecords = 0;

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * Constructor
     *
     * @param dsp      the parent DSP
     * @param root     the parent CDMDataset
     * @param template the structure template
     */
    CDMArraySequence(D4DSP dsp, CDMDataset root, DapStructure template, D4DataSequence d4data)
    {
        super(CDMArrayStructure.computemembers((DapStructure) d4data.getTemplate()),
                new SDI(), (int) d4data.getRecordCount());
        this.dsp = dsp;
        this.root = root;
        this.template = (DapVariable) d4data.getTemplate();
        this.d4data = d4data;
        this.nmembers = ((DapStructure) template).getFields().size();
        this.nrecords = d4data.getRecordCount();

        // Fill in the instances and structdata vectors
        // The leaf instances arrays will be filled in by the CDM compiler
        super.sdata = new StructureDataA[(int) this.nrecords];
        for(int i = 0; i < this.nrecords; i++) {
            super.sdata[i] = new StructureDataA(this, i);
        }
        this.records = new Array[(int) (this.nrecords)][(int) this.nmembers];
        ((SDI) super.iter).setList(super.sdata);
    }

    /*package*/ void
    finish()
    {
        for(int i = 0; i < this.nrecords; i++) {
            assert records[i] != null;
        }
        this.bytesize = computeTotalSize();
    }

    //////////////////////////////////////////////////
    // API

    void
    addField(long recno, int fieldno, Array instance)
    {
        assert this.records != null : "Internal Error";
        if(recno < 0 || recno >= this.nrecords)
            throw new ArrayIndexOutOfBoundsException("CDMArrayStructure: record index out of range: " + recno);
        if(fieldno < 0 || fieldno >= this.nmembers)
            throw new ArrayIndexOutOfBoundsException("CDMArrayStructure: field index out of range: " + fieldno);
        Array[] fields = this.records[(int) recno];
        if(fields == null)
            throw new ArrayIndexOutOfBoundsException("CDMArrayStructure: record: " + recno);
        fields[fieldno] = instance;
    }

    //////////////////////////////////////////////////
    // CDMArray Interface
    @Override
    public DSP getDSP()
    {
        return this.dsp;
    }

    @Override
    public AtomicType getPrimitiveType()
    {
        return AtomicType.Sequence;
    }

    @Override
    public DapType
    getBaseType()
    {
        return DapType.SEQ;
    }

    @Override
    public CDMDataset getRoot()
    {
        return root;
    }

    @Override
    public DapVariable getTemplate()
    {
        return template;
    }

    @Override
    public long getByteSize()
    {
        return bytesize;
    }

    //////////////////////////////////////////////////

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        DapStructure struct = (DapStructure) this.template;
        for(int i = 0; i < this.nrecords; i++) {
            List<DapVariable> fields = struct.getFields();
            if(i < (this.nrecords - 1))
                buf.append("\n");
            buf.append("Sequence {\n");
            if(fields != null) {
                for(int j = 0; j < this.nmembers; j++) {
                    DapVariable field = fields.get(j);
                }
            }
            buf.append(String.format("} [%d/%d]", i, this.nrecords));
        }
        return buf.toString();
    }

    public long
    computeTotalSize()
    {
        long totalsize = 0;
        for(int recno = 0; recno < this.nrecords; recno++) {
            Array[] fields = this.records[recno];
            assert fields != null : "internal error";
            for(int m = 0; m < this.nmembers; m++) {
                totalsize += fields[m].getSizeBytes();
            }
        }
        return totalsize;
    }

    //////////////////////////////////////////////////
    // ArraySequence/ArrayStructure overrides

    @Override
    public int getStructureDataCount()
    {
        return (int) this.nrecords;
    }

    @Override
    public long getSizeBytes()
    {
        return this.bytesize;
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
        assert(super.sdata != null);
        if(index < 0 || index >= this.nrecords)
            throw new IllegalArgumentException(index + " >= " + super.sdata.length);
        assert (super.sdata[index] != null);
        return super.sdata[index];
    }

    public ArraySequence getArraySequence(StructureMembers.Member m)
    {
        return this;
    }

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
        Array cdmdata = records[recno][memberindex];
        return cdmdata;
    }
}

