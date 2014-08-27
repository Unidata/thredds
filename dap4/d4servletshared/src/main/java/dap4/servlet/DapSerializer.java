/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.cdmshared.CDMUtil;
import dap4.ce.CEConstraint;
import dap4.core.data.*;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4shared.DSP;
import dap4.dap4shared.Dap4Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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
    protected DataDataset data = null;
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
        this.data = dsp.getDataDataset();
        this.ce = constraint;
    }

    public void
    write(DapDataset dmr)
            throws IOException
    {
        writer = new SerialWriter(this.stream, this.order);
        // Iterate over the top-level variables in the constraint
        for(DapVariable var : dmr.getTopVariables()) {
            DataVariable dv = data.getVariableData(var);
            if(!ce.references(var))
                continue;
            if(dv == null)
                throw new DapException("DapSerializer: cannot find  Variable data " + var.getFQN());
            writeVariable(var, dv, writer);
        }
    }

    //////////////////////////////////////////////////
    // Recursive variable writer

    /**
     * @param dapvar
     * @param dv
     * @throws IOException
     */
    protected void
    writeVariable(DapVariable dapvar, DataVariable dv, SerialWriter dst)
            throws IOException
    {
        assert (dapvar == dv.getTemplate());
        dst.startVariable();
        switch (dv.getSort()) {
        case ATOMIC:
            writeAtomicVariable(dapvar, dv, dst);
            break;
        case STRUCTURE:
            writeStructure(dapvar, (DataStructure) dv, dst);
            break;
        case SEQUENCE:
            writeSequence(dapvar, (DataSequence) dv, dst);
            break;
        case COMPOUNDARRAY:
            writeCompoundArray(dapvar, (DataCompoundArray) dv, dst);
            break;
        default:
            assert false : "Unexpected variable type";
        }
        dst.endVariable();
    }

    /**
     * Write out an atomic variable.
     *
     * @param vv the atomic variable
     * @param dv the variable's data
     * @throws IOException
     */
    protected void
    writeAtomicVariable(DapVariable vv, DataVariable dv, SerialWriter dst)
            throws DataException
    {
        try {
            DapAtomicVariable dapvar = (DapAtomicVariable) vv;
            DataAtomic dav = (DataAtomic) dv;
            DapType basetype = dapvar.getBaseType();
            List<Slice> slices;
            ByteBuffer buf;
            if(dapvar.getRank() == 0) { // scalar
                dst.writeObject(basetype, dav.read(0));
            } else {// dimensioned
                // get the slices from constraint
                slices = ce.getConstrainedSlices(dapvar);
                if(slices == null)
                    throw new DataException("Unknown variable: " + dapvar.getFQN());
                long count = DapUtil.sliceProduct(slices);
                Object vector = Dap4Util.createVector(basetype.getPrimitiveType(), count);
                dav.read(slices,vector,0);
                dst.writeArray(basetype, vector);
            }
        } catch (IOException ioe) {
            throw new DataException(ioe);
        }
    }

    /* unneeded?
    static public Object
    buildAtomicArray(int product, DapType typ)
    {
        Object array = null;
        AtomicType atype = typ.getAtomicType();
        switch (atype) {
        case Char:
        case Int8:
        case UInt8:
            array = new byte[product];
            break;
        case Int16:
        case UInt16:
            array = new short[product];
            break;
        case Int32:
        case UInt32:
            array = new int[product];
            break;
        case Int64:
        case UInt64:
            array = new long[product];
            break;
        case Float32:
            array = new float[product];
            break;
        case Float64:
            array = new double[product];
            break;
        default:
            assert false;
        }
        return array;
    }
    */

    /**
     * Write out a single structure instance.
     *
     * @param vv the structure view
     * @param ds the structure data
     * @throws DataException
     */

    protected void
    writeStructure(DapVariable vv, DataStructure ds, SerialWriter dst)
            throws DataException
    {
        try {
            DapStructure dapvar = (DapStructure) vv;
            for(DapVariable field : dapvar.getFields()) {
                if(!ce.references(field)) continue; // not in the view
                DataVariable dv = ds.readfield(field.getShortName());
                writeVariable(field, dv, dst);
            }
        } catch (IOException ioe) {
            throw new DataException(ioe);
        }
    }

    /**
     * Write out a singleton compound variable.
     *
     * @param vv the structure variable
     * @param dc the data
     * @throws DataException
     */

    protected void
    writeCompound(DapVariable vv, DataCompound dc, SerialWriter dst)
            throws DataException
    {
        if(dc.getSort() == DataSort.STRUCTURE)
            writeStructure(vv, (DataStructure) dc, dst);
        else
            writeSequence(vv, (DataSequence) dc, dst);
        return;
    }

    /**
     * Write out a dimensioned ompound variable.
     *
     * @param dapvar the structure variable
     * @param dca    the array's data
     * @throws DataException
     */

    protected void
    writeCompoundArray(DapVariable dapvar, DataCompoundArray dca, SerialWriter dst)
            throws DataException
    {
        try {
            if(dapvar.getRank() == 0) {
                writeCompound(dapvar, dca.read(0), dst);
                return;
            }
            // Get the active set of slices for this variable
            List<Slice> slices = ce.getConstrainedSlices(dapvar);
            if(slices == null)
                throw new DataException("Undefined variable: " + dapvar);
            long count = DapUtil.sliceProduct(slices);
            DataCompound[] dc = new DataCompound[(int) count];
            dca.read(slices, dc);
            for(int i = 0; i < count; i++) {
                writeCompound(dapvar, dc[i], dst);
            }
        } catch (IOException ioe) {
            throw new DataException(ioe);
        }
    }

    /**
     * Write out a single Record instance.
     *
     * @param vv the record view
     * @param dr the record data
     * @throws DataException
     */

    protected void
    writeRecord(DapVariable vv, DataRecord dr, SerialWriter dst)
            throws DataException
    {
        try {
            DapSequence dapvar = (DapSequence) vv;
            for(DapVariable field : dapvar.getFields()) {
                if(!ce.references(field)) continue; // not in the view
                DataVariable dv = dr.readfield(field.getShortName());
                writeVariable(field, dv, dst);
            }
        } catch (IOException ioe) {
            throw new DataException(ioe);
        }
    }

    /**
     * Write out a single Sequence of records
     * (Eventually use any filter in the DapVariable)
     *
     * @param dapvar the constraint view
     * @param ds     the structure data
     * @throws DataException
     */

    protected void
    writeSequence(DapVariable dapvar, DataSequence ds, SerialWriter dst)
            throws DataException
    {
        DapSequence seq = (DapSequence) dapvar;
        long nrecs = ds.getRecordCount();
        // We need to create a temporary serializing buffer
        // so we can properly precede the records with the correct count.
        long actual = 0;
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        SerialWriter tmp = new SerialWriter(bytestream, this.order);
        tmp.computeChecksums(false);
        try {
            for(int i = 0; i < nrecs; i++) {
                DataRecord rec = ds.readRecord(i);
                if(ce.match(seq, rec)) {
                    writeRecord(dapvar, ds.readRecord(i), tmp);
                    actual++;
                }
            }
            bytestream.flush();
            dst.writeCount(actual);
            dst.writeBytes(bytestream.toByteArray());
        } catch (IOException ioe) {
            throw new DataException(ioe);
        }
    }

/**
 * Write out a possibly dimensioned sequence variable.
 *
 * @param vv   the sequence variable
 * @param data the variable's data
 * @throws DataException
 */

/*
    void
    writeSequenceVariable(View view, DapVariable vv, DataSequence data)
        throws DataException
    {
        Dapequence dapvar = (DapSequence) vv.getVariable();
        if(dapvar.getRank() == 0) {
            writeSequence(view, vv, dav.read(0));
            return;
        }
        List<Slice> slices = vv.getSlices();
        long count = DapUtil.sliceProduct(slices);
        DataSequence[] dvs = dav.read(slices);
        assert dvs.length == count;
        for(int i = 0;i < count;i++) {
            writeSequence(view, vv, dvs[i]);
        }


        // The sequence will never be a leaf because of the call
        // to CESemantics.expandStruct().
        DapVariable dapvar = vv.getVariable();
        assert (dapvar.getSort() == DapSort.SEQUENCE);
        DapSequence dapseq = (DapSequence) dapvar;
        List<DapVariable> fields = dapseq.getFields();

        long nrecords = data.getRecordCount();
        for(int recno = 0;recno < nrecords;recno++) {
            DataRecord instance = data.readRecord(recno);
            // Walk and dump the fields of the instance
            for(DapVariable field : fields) {
                DapVariable fieldproj = view.get(field);
                if(fieldproj == null) continue; // do not output
                DataVariable fielddata = instance.readfield(field.getShortName());
                writeVariable(fieldproj, fielddata);
            }
        }
    }
    */
}

