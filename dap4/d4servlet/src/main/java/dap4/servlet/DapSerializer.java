/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.core.ce.CEConstraint;
import dap4.core.data.DSP;
import dap4.core.data.DataCursor;
import dap4.core.dmr.*;
import dap4.core.util.DapUtil;
import dap4.core.util.Index;
import dap4.core.util.Odometer;
import dap4.core.util.Slice;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Given a DSP, serialize
 * possibly constrained data.
 */

public class DapSerializer
{
    //////////////////////////////////////////////////
    // Instance variables

    protected OutputStream stream = null;
    protected SerialWriter writer = null;
    protected DSP dsp = null;
    protected CEConstraint ce = null;
    protected ByteOrder order = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public DapSerializer()
    {
    }

    /**
     * Primary constructor
     *
     * @param dsp        The DSP to write
     * @param constraint Any applicable constraint
     * @param stream     Write to this stream
     * @param order      The byte order to use
     */
    public DapSerializer(DSP dsp, CEConstraint constraint,
                         OutputStream stream, ByteOrder order)
            throws IOException
    {
        this.dsp = dsp;
        this.order = order;
        this.stream = stream;
        this.ce = constraint;
    }

    public void
    write(DapDataset dmr)
            throws IOException
    {
        writer = new SerialWriter(this.stream, this.order);
        writer.flush(); // If stream is ChunkWriter, then dump DMR
        // Iterate over the top-level variables in the constraint
        for(DapVariable var : dmr.getTopVariables()) {
            if(!this.ce.references(var))
                continue;
            DataCursor varcursor = this.dsp.getVariableData(var);
            if(varcursor == null)
                throw new dap4.core.util.DapException("DapSerializer: cannot find  Variable data " + var.getFQN());
            writeVariable(varcursor, writer);
        }
    }

    //////////////////////////////////////////////////
    // Recursive variable writer

    /**
     * @param data - cursor referencing what to write
     * @param dst  - where to write
     * @throws IOException
     */
    protected void
    writeVariable(DataCursor data, SerialWriter dst)
            throws IOException
    {
        DapVariable template = (DapVariable) data.getTemplate();
        dst.startVariable();
        switch (data.getScheme()) {
        case ATOMIC:
            writeAtomicVariable(data, dst);
            break;
        case STRUCTARRAY:
            writeStructure(data, dst);
            break;
        case SEQARRAY:
            writeSequence(data, dst);
            break;
        case STRUCTURE:
            writeStructure1(data, dst);
            break;
        case SEQUENCE:
            writeSequence1(data, dst);
            break;
        default:
            assert false : "Unexpected variable type";
        }
        dst.endVariable();
    }

    /**
     * Write out an atomic variable.
     *
     * @param data
     * @param dst
     * @throws dap4.core.util.DapException
     */
    protected void
    writeAtomicVariable(DataCursor data, SerialWriter dst)
            throws IOException
    {
        DapAtomicVariable template = (DapAtomicVariable) data.getTemplate();
        assert (this.ce.references(template));
        DapType basetype = template.getBaseType();
        if(template.getRank() == 0) { // scalar
            dst.writeAtomicArray(basetype, data.read(Index.SCALAR));
        } else {// dimensioned
            // get the slices from constraint
            List<Slice> slices = ce.getConstrainedSlices(template);
            if(slices == null)
                throw new dap4.core.util.DapException("Unknown variable: " + template.getFQN());
            long count = DapUtil.sliceProduct(slices);
            Object vector = data.read(slices);
            dst.writeAtomicArray(basetype, vector);
        }
    }

    /**
     * Write out a single or array structure instance
     *
     * @param data
     * @param dst  - where to write
     * @throws dap4.core.util.DapException
     */

    protected void
    writeStructure(DataCursor data, SerialWriter dst)
            throws IOException
    {
        DapStructure template = (DapStructure) data.getTemplate();
        assert (this.ce.references(template));
        if(template.getRank() == 0) { // scalar
            writeStructure1(data, dst);
        } else {
            List<Slice> slices = ce.getConstrainedSlices(template);
            Odometer odom = Odometer.factory(slices);
            while(odom.hasNext()) {
                Index index = odom.next();
                DataCursor instance = (DataCursor) data.read(index);
                writeStructure1(instance, dst);
            }
        }
    }

    /**
     * Write out a single structure instance
     *
     * @param instance
     * @param dst      - where to write
     * @throws dap4.core.util.DapException
     */

    protected void
    writeStructure1(DataCursor instance, SerialWriter dst)
            throws IOException
    {
        assert instance.getScheme() == DataCursor.Scheme.STRUCTURE;
        DapStructure template = (DapStructure) instance.getTemplate();
        assert (this.ce.references(template));
        List<DapVariable> fields = template.getFields();
        for(int i = 0; i < fields.size(); i++) {
            DapVariable field = fields.get(i);
            if(!this.ce.references(field)) continue; // not in the view
            DataCursor df = instance.getField(i);
            writeVariable(df, dst);
        }
    }

    /**
     * Write out a single or array sequence instance
     *
     * @param data
     * @param dst  - where to write
     * @throws dap4.core.util.DapException
     */

    protected void
    writeSequence(DataCursor data, SerialWriter dst)
            throws IOException
    {
        DapSequence template = (DapSequence) data.getTemplate();
        assert (this.ce.references(template));
        if(template.getRank() == 0) { // scalar
            writeSequence1(data, dst);
        } else {
            List<Slice> slices = ce.getConstrainedSlices(template);
            Odometer odom = Odometer.factory(slices);
            while(odom.hasNext()) {
                Index index = odom.next();
                DataCursor instance = (DataCursor) data.read(index);
                writeSequence1(instance, dst);
            }
        }
    }


    /**
     * Write out a single Sequence of records
     * (Eventually use any filter in the DapVariable)
     *
     * @param instance the sequence instance
     * @param dst      write target
     * @throws dap4.core.util.DapException
     */

    protected void
    writeSequence1(DataCursor instance, SerialWriter dst)
            throws IOException
    {
        DapSequence template = (DapSequence) instance.getTemplate();
        assert (this.ce.references(template));
        long nrecs = instance.getRecordCount();
        dst.writeCount(nrecs);
        for(long i = 0; i < nrecs; i++) {
            DataCursor record = instance.getRecord(i);
            writeRecord(record, dst);
        }
    }

    /**
     * Write out a single Record instance.
     *
     * @param record the record data cursor
     * @param dst    to which to write
     * @throws dap4.core.util.DapException
     */

    protected void
    writeRecord(DataCursor record, SerialWriter dst)
            throws IOException
    {
        DapSequence template = (DapSequence) record.getTemplate();
        List<DapVariable> fields = template.getFields();
        for(int i = 0; i < fields.size(); i++) {
            DapVariable field = fields.get(i);
            if(!this.ce.references(field)) continue; // not in the view
            DataCursor df = record.getField(i);
            writeVariable(df, dst);
        }
    }

}


