/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.dap4lib.serial;

import dap4.core.data.ChecksumMode;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4lib.LibTypeFcns;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static dap4.core.data.DataCursor.Scheme;

public class D4DataCompiler
{
    static public boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static final public int COUNTSIZE = 8; // databuffer as specified by the DAP4 spec

    static String LBRACE = "{";
    static String RBRACE = "}";


    //////////////////////////////////////////////////
    // Instance variables

    protected DapDataset dataset = null;

    // Make compile arguments global
    protected ByteBuffer databuffer;

    protected ChecksumMode checksummode = null;
    protected ByteOrder order = null;

    protected D4DSP dsp;

    //////////////////////////////////////////////////
    //Constructor(s)

    /**
     * Constructor
     *
     * @param dsp          the D4DSP
     * @param checksummode
     * @param databuffer   the source of serialized databuffer
     */

    public D4DataCompiler(D4DSP dsp, ChecksumMode checksummode, ByteOrder order,
                          ByteBuffer databuffer)
            throws DapException
    {
        this.dsp = dsp;
        this.dataset = this.dsp.getDMR();
        this.databuffer = databuffer;
        this.checksummode = checksummode;
        this.order = order;
    }

    //////////////////////////////////////////////////
    // DataCompiler API

    /**
     * The goal here is to process the serialized
     * databuffer and locate top-level variable positions
     * in the serialized databuffer. Access to non-top-level
     * variables is accomplished on the fly.
     *
     * @return
     * @throws DapException
     */
    public void
    compile()
            throws DapException
    {
        assert (this.dataset != null && this.databuffer != null);
        // iterate over the variables represented in the databuffer
        for(DapVariable vv : this.dataset.getTopVariables()) {
            D4Cursor data = compileVar(vv, null);
            this.dsp.addVariableData(vv, data);
        }
    }

    protected D4Cursor
    compileVar(DapVariable dapvar, D4Cursor container)
            throws DapException
    {
        boolean isscalar = dapvar.getRank() == 0;
        D4Cursor array = null;
        DapType type = dapvar.getBaseType();
        if(type.isAtomic())
            array = compileAtomicVar(dapvar, container);
        else if(type.isStructType()) {
            array = compileStructureArray(dapvar, container);
        } else if(type.isSeqType()) {
            array = compileSequenceArray(dapvar, container);
        }
        if(dapvar.isTopLevel() && this.checksummode.enabled(ChecksumMode.DAP)) {
            // extract the checksum from databuffer src,
            // attach to the array, and make into an attribute
            int checksum = extractChecksum(databuffer);
            dapvar.setChecksum(checksum);
        }
        return array;
    }

    /**
     * @param var
     * @param container
     * @return
     * @throws DapException
     */

    protected D4Cursor
    compileAtomicVar(DapVariable var, D4Cursor container)
            throws DapException
    {
        DapType daptype = var.getBaseType();
        D4Cursor data = new D4Cursor(Scheme.ATOMIC, (D4DSP) this.dsp, var, container);
        data.setOffset(getPos(this.databuffer));
        long total = 0;
        long dimproduct = var.getCount();
        if(!daptype.isEnumType() && !daptype.isFixedSize()) {
            // this is a string, url, or opaque
            long[] positions = new long[(int) dimproduct];
            int savepos = databuffer.position();
            // Walk the bytestring and return the instance count (in databuffer)
            total = walkByteStrings(positions, databuffer);
            databuffer.position(savepos);// leave position unchanged
            data.setByteStringOffsets(total, positions);
        } else {
            total = dimproduct * daptype.getSize();
        }
        skip(databuffer, (int) total);
        return data;
    }

    /**
     * Compile a structure array.
     *
     * @param var    the template
     * @param container if inside a compound object
     * @return A DataCompoundArray for the databuffer for this struct.
     * @throws DapException
     */
    protected D4Cursor
    compileStructureArray(DapVariable var, D4Cursor container)
            throws DapException
    {
        DapStructure dapstruct = (DapStructure) var.getBaseType();
        D4Cursor structarray = new D4Cursor(Scheme.STRUCTARRAY, this.dsp, var, container)
                .setOffset(getPos(this.databuffer));
        List<DapDimension> dimset = var.getDimensions();
        long dimproduct = DapUtil.dimProduct(dimset);
        D4Cursor[] instances = new D4Cursor[(int) dimproduct];
        Odometer odom = Odometer.factory(DapUtil.dimsetToSlices(dimset), dimset);
        while(odom.hasNext()) {
            Index index = odom.next();
            D4Cursor instance = compileStructure(var, dapstruct, structarray);
            instance.setIndex(index);
            instances[(int) index.index()] = instance;
        }
        structarray.setElements(instances);
        return structarray;
    }

    /**
     * Compile a structure instance.
     *
     * @param dapstruct The template
     * @param container
     * @return A DataStructure for the databuffer for this struct.
     * @throws DapException
     */
    protected D4Cursor
    compileStructure(DapVariable var, DapStructure dapstruct, D4Cursor container)
            throws DapException
    {
        int pos = getPos(this.databuffer);
        D4Cursor d4ds = new D4Cursor(Scheme.STRUCTURE, (D4DSP) this.dsp, var, container)
                .setOffset(pos);
        List<DapVariable> dfields = dapstruct.getFields();
        for(int m = 0; m < dfields.size(); m++) {
            DapVariable dfield = dfields.get(m);
            D4Cursor dvfield = compileVar(dfield, d4ds);
            d4ds.addField(m, dvfield);
            assert dfield.getParent() != null;
        }
        return d4ds;
    }

    /**
     * Compile a sequence array.
     *
     * @param var the template
     * @return A DataCompoundArray for the databuffer for this sequence.
     * @throws DapException
     */
    protected D4Cursor
    compileSequenceArray(DapVariable var, D4Cursor container)
            throws DapException
    {
        DapSequence dapseq = (DapSequence) var.getBaseType();
        D4Cursor seqarray = new D4Cursor(Scheme.SEQARRAY, this.dsp, var, container)
                .setOffset(getPos(this.databuffer));
        List<DapDimension> dimset = var.getDimensions();
        long dimproduct = DapUtil.dimProduct(dimset);
        D4Cursor[] instances = new D4Cursor[(int) dimproduct];
        Odometer odom = Odometer.factory(DapUtil.dimsetToSlices(dimset), dimset);
        while(odom.hasNext()) {
            Index index = odom.next();
            D4Cursor instance = compileSequence(var, dapseq, seqarray);
            instance.setIndex(index);
            instances[(int) index.index()] = instance;
        }
        seqarray.setElements(instances);
        return seqarray;
    }

    /**
     * Compile a sequence as a set of records.
     *
     * @param dapseq
     * @param container
     * @return
     * @throws DapException
     */
    public D4Cursor
    compileSequence(DapVariable var, DapSequence dapseq, D4Cursor container)
            throws DapException
    {
        int pos = getPos(this.databuffer);
        D4Cursor seq = new D4Cursor(Scheme.SEQUENCE, this.dsp, var, container)
                .setOffset(pos);
        List<DapVariable> dfields = dapseq.getFields();
        // Get the count of the number of records
        long nrecs = getCount(this.databuffer);
        for(int r = 0; r < nrecs; r++) {
            pos = getPos(this.databuffer);
            D4Cursor rec = (D4Cursor) new D4Cursor(D4Cursor.Scheme.RECORD, this.dsp, var, container)
                    .setOffset(pos).setRecordIndex(r);
            for(int m = 0; m < dfields.size(); m++) {
                DapVariable dfield = dfields.get(m);
                D4Cursor dvfield = compileVar(dfield, rec);
                rec.addField(m, dvfield);
                assert dfield.getParent() != null;
            }
            seq.addRecord(rec);
        }
        return seq;
    }

    //////////////////////////////////////////////////
    // Utilities

    protected int
    extractChecksum(ByteBuffer data)
            throws DapException
    {
        assert ChecksumMode.DAP.enabled(this.checksummode);
        if(data.remaining() < DapUtil.CHECKSUMSIZE)
            throw new DapException("Short serialization: missing checksum");
        return data.getInt();
    }

    static protected void
    skip(ByteBuffer data, int count)
    {
        data.position(data.position() + count);
    }

    static protected int
    getCount(ByteBuffer data)
    {
        long count = data.getLong();
        count = (count & 0xFFFFFFFF);
        return (int) count;
    }

    static protected int
    getPos(ByteBuffer data)
    {
        return data.position();
    }

    /**
     * Compute the size in databuffer of the serialized form
     *
     * @param daptype
     * @return type's serialized form size
     */
    static protected int
    computeTypeSize(DapType daptype)
    {
        return LibTypeFcns.size(daptype);
    }

    static protected long
    walkByteStrings(long[] positions, ByteBuffer databuffer)
    {
        int count = positions.length;
        long total = 0;
        int savepos = databuffer.position();
        // Walk each bytestring
        for(int i = 0; i < count; i++) {
            int pos = databuffer.position();
            positions[i] = pos;
            int size = getCount(databuffer);
            total += COUNTSIZE;
            total += size;
            skip(databuffer, size);
        }
        databuffer.position(savepos);// leave position unchanged
        return total;
    }

}
