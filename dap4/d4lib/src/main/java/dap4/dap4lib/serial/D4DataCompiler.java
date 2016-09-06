/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.dap4lib.serial;

import dap4.core.dmr.*;
import dap4.core.util.DapDump;
import dap4.core.util.DapException;
import dap4.core.util.DapSort;
import dap4.core.util.DapUtil;
import dap4.dap4lib.ChecksumMode;
import dap4.dap4lib.LibTypeFcns;
import dap4.dap4lib.RequestMode;

import java.nio.ByteBuffer;
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

    public D4DataCompiler(D4DSP dsp, ChecksumMode checksummode,
                          ByteBuffer databuffer)
            throws DapException
    {
        this.dsp = dsp;
        this.dataset = this.dsp.getDMR();
        this.databuffer = databuffer;
        this.checksummode = checksummode;
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
        if(DEBUG) {
            DapDump.dumpbytes(this.databuffer, false);
        }
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
        if(dapvar.getSort() == DapSort.ATOMICVARIABLE) {
            array = compileAtomicVar((DapAtomicVariable) dapvar, container);
        } else if(dapvar.getSort() == DapSort.STRUCTURE) {
            if(isscalar)
                array = compileStructure((DapStructure) dapvar, container);
            else
                array = compileStructureArray(dapvar, container);
        } else if(dapvar.getSort() == DapSort.SEQUENCE) {
            if(isscalar)
                array = compileSequence((DapSequence) dapvar, container);
            else
                array = compileSequenceArray(dapvar, container);
        }
        if(dapvar.isTopLevel()) {
            // extract the checksum from databuffer src,
            // attach to the array, and make into an attribute
            byte[] checksum = getChecksum(databuffer);
            dapvar.setChecksum(checksum);
        }
        return array;
    }

    /**
     * @param atomvar
     * @param container
     * @return
     * @throws DapException
     */
    protected D4Cursor
    compileAtomicVar(DapAtomicVariable atomvar, D4Cursor container)
            throws DapException
    {
        DapType daptype = atomvar.getBaseType();
        D4Cursor data = new D4Cursor(Scheme.ATOMIC, this.dsp, atomvar);
        data.setOffset(getPos(this.databuffer));
        long total = 0;
        long dimproduct = atomvar.getCount();
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
     * @param dapvar    the template
     * @param container if inside a compound object
     * @return A DataCompoundArray for the databuffer for this struct.
     * @throws DapException
     */
    protected D4Cursor
    compileStructureArray(DapVariable dapvar, D4Cursor container)
            throws DapException
    {
        DapStructure struct = (DapStructure) dapvar;
        D4Cursor structarray = new D4Cursor(Scheme.STRUCTARRAY, this.dsp, struct)
                .setOffset(getPos(this.databuffer));
        long dimproduct = struct.getCount();
        for(int i = 0; i < dimproduct; i++) {
            D4Cursor instance = compileStructure(struct, structarray);
            structarray.addElement(i, instance);
        }
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
    compileStructure(DapStructure dapstruct, D4Cursor container)
            throws DapException
    {
        int pos = getPos(this.databuffer);
        D4Cursor d4ds = new D4Cursor(Scheme.STRUCTURE, this.dsp, dapstruct)
                .setOffset(pos);
        List<DapVariable> dfields = dapstruct.getFields();
        for(int m = 0; m < dfields.size(); m++) {
            DapVariable dfield = dfields.get(m);
            D4Cursor dvfield = compileVar(dfield, d4ds);
            d4ds.addField(m, dvfield);
        }
        return d4ds;
    }

    /**
     * Compile a sequence array.
     *
     * @param dapvar the template
     * @return A DataCompoundArray for the databuffer for this sequence.
     * @throws DapException
     */
    protected D4Cursor
    compileSequenceArray(DapVariable dapvar, D4Cursor container)
            throws DapException
    {
        DapSequence dapseq = (DapSequence) dapvar;
        D4Cursor seqarray = new D4Cursor(Scheme.SEQARRAY, this.dsp, dapseq)
                .setOffset(getPos(this.databuffer));
        long dimproduct = dapseq.getCount();
        for(int i = 0; i < dimproduct; i++) {
            D4Cursor instance = compileSequence(dapseq, seqarray);
            seqarray.addElement(i, instance);
        }
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
    compileSequence(DapSequence dapseq, D4Cursor container)
            throws DapException
    {
        int pos = getPos(this.databuffer);
        D4Cursor seq = new D4Cursor(Scheme.SEQUENCE, this.dsp, dapseq)
                .setOffset(pos);
        List<DapVariable> dfields = dapseq.getFields();
        // Get the count of the number of records
        long nrecs = getCount(this.databuffer);
        for(int r = 0; r < nrecs; r++) {
            pos = getPos(this.databuffer);
            D4Cursor rec = new D4Cursor(D4Cursor.Scheme.RECORD, this.dsp, dapseq)
                    .setOffset(pos);
            for(int m = 0; m < dfields.size(); m++) {
                DapVariable dfield = dfields.get(m);
                D4Cursor dvfield = compileVar(dfield, rec);
                rec.addField(m, dvfield);
            }
            seq.addRecord(rec);
        }
        return seq;
    }

    //////////////////////////////////////////////////
    // Utilities

    protected byte[]
    getChecksum(ByteBuffer data)
            throws DapException
    {
        if(!ChecksumMode.enabled(RequestMode.DAP, checksummode)) return null;
        if(data.remaining() < DapUtil.CHECKSUMSIZE)
            throw new DapException("Short serialization: missing checksum");
        byte[] checksum = new byte[DapUtil.CHECKSUMSIZE];
        data.get(checksum);
        return checksum;
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
