/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.core.ce.CEConstraint;
import dap4.core.data.ChecksumMode;
import dap4.core.dmr.*;
import dap4.core.util.DapException;
import dap4.core.util.Odometer;
import dap4.core.util.Slice;
import dap4.dap4lib.DMRPrinter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteOrder;
import java.util.List;

import static dap4.servlet.Value.ValueSource;

/**
 * Given a DMR, return:
 * 1. A byte array (byte[]) containing serialized data
 * and (optionally) the DMR
 * <p>
 * Requirements:
 * 1. repeatability: given the same DMR, return the same byte array
 * <p>
 * Notes:
 * 1. Two options are provided for generating values:
 * a. use of a random number generator with a fixed seed.
 * b. use of a pre-defined sequence of values with repetition
 * when the sequence is exhausted (not yet implemented).
 * <p>
 * Additionally, provide two options for generating data from a DMR.
 * 1. Automated generation of the data from the whole DMR.
 * 2. Selective generation by starting at some variable
 * in the DMR. This is useful when one wants more detailed
 * control over e.g. the number of tuples in a sequence.
 */

public class Generator extends DapSerializer
{
    static final boolean DEBUG = false;

    static final String LBRACE = "{";
    static final String RBRACE = "}";

    static final int DEFALTCACHELIMIT = 4; // max cache size    

    static final ByteOrder DEFAULTORDER = ByteOrder.LITTLE_ENDIAN;

    static final String DATADIR = "d4tests/src/test/data"; // relative to dap4 root
    static final String TESTSRC = DATADIR + "/resources/testfiles";

    static final String SERIALEXT = ".ser";
    static final String SRCEXT = ".dmr" + SERIALEXT;
    static final String DSTEXT = ".dap" + SERIALEXT;

    static final int MAXROWS = 5;

    static int rowcount = 0;

    //////////////////////////////////////////////////
    // static methods
    protected static boolean asciionly = true;

    static public void setASCII(boolean tf)
    {
        asciionly = tf;
    }

    static public int getRowCount()
    {
        return rowcount;
    }

    static public void setRowCount(int count)
    {
        if(count >= 0)
            rowcount = count;
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected Value values = null; // Value generator
    protected ByteArrayOutputStream stream = null;
    protected ChunkWriter cw = null;
    protected boolean withdmr = true;
    protected DapDataset dmr = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Generator(DapDataset dmr, ValueSource src)
            throws DapException
    {
        super();
        this.dmr = dmr;
        if(src == null)
            src = ValueSource.RANDOM; // default
        switch (src) {
        case FIXED:
        case RANDOM:
        default:
            values = new RandomValue();
            break;
        }
        values.setASCII(asciionly);
        SerialWriter.DEBUG = DEBUG;
    }

    //////////////////////////////////////////////////
    // Generator

    public void
    generate(CEConstraint ce, ChunkWriter cw, boolean withdmr, ChecksumMode mode)
            throws DapException
    {
        begin(ce, cw, withdmr, mode);
        if(this.withdmr)
            generateDMR(this.dmr);
        dataset(this.dmr);
        end();
    }

    public void
    begin(CEConstraint ce, ChunkWriter cw, boolean withdmr, ChecksumMode mode)
            throws DapException
    {
        this.cw = cw;
        if(ce == null)
            ce = CEConstraint.getUniversal(this.dmr);
        this.ce = ce;
        this.order = cw.getWriteOrder();
        this.checksummode = mode;
        this.withdmr = withdmr;
        writer = new SerialWriter(this.cw, this.order,this.checksummode);
    }

    public void
    end()
            throws DapException
    {
    }

    public void
    generateDMR(DapDataset dmr)
            throws DapException
    {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            DMRPrinter dp = new DMRPrinter(dmr, this.ce, pw, null);
            dp.print();
            pw.close();
            sw.close();
            String tmp = sw.toString();
            this.cw.cacheDMR(tmp);
            this.cw.flush();
        } catch (Exception e) {
            throw new DapException(e);
        }
    }

    //////////////////////////////////////////////////
    // Node specific generators

    public void
    dataset(DapDataset dmr)
            throws DapException
    {
        // Iterate over the variables in order
        for(DapVariable var : this.dmr.getTopVariables()) {
            if(!this.ce.references(var))
                continue;
            variable(var);
        }
    }

    public void
    variable(DapVariable dapvar)
            throws DapException
    {
        writer.startVariable();
        if(dapvar.isAtomic())
            atomicVariable(dapvar);
        else if(dapvar.isStructure())
            structureVariable(dapvar);
        else if(dapvar.isSequence())
            sequenceVariable(dapvar);
        else
            throw new DapException("generate var: not a variable:" + dapvar.getFQN());
        try {
            writer.endVariable();
        } catch (IOException ioe) {
            throw new DapException(ioe);
        }
    }

    public void
    atomicVariable(DapVariable dapvar)
            throws DapException
    {
        DapType basetype = dapvar.getBaseType();
        Odometer odom = null;
        if(dapvar.getRank() == 0) {//scalar
            odom = Odometer.factoryScalar();
        } else {// dimensioned
            // get the slices from the constraint
            List<Slice> slices = ce.getConstrainedSlices(dapvar);
            // Create an odometer from the slices
            odom = Odometer.factory(slices, dapvar.getDimensions());
        }
        while(odom.hasNext()) {
            Object value = values.nextValue(basetype);
            if(DEBUG) {
                System.err.printf("generate: %s = %s%n", dapvar.getFQN(), stringify(value));
                System.err.flush();
            }
            try {
                assert (writer != null);
                writer.writeAtomicArray(basetype,value);
            } catch (IOException ioe) {
                throw new DapException(ioe);
            }
            odom.next();
        }
    }

    public void
    structureVariable(DapVariable var)
            throws DapException
    {
        DapStructure struct = (DapStructure) var.getBaseType();
        List<DapVariable> fields = struct.getFields();
        Odometer odom = null;
        if(var.getRank() == 0) {//scalar
            odom = Odometer.factoryScalar();
        } else {// dimensioned
            List<Slice> slices = ce.getConstrainedSlices(var);
            odom = Odometer.factory(slices, var.getDimensions());
        }
        while(odom.hasNext()) {
            // generate a value for each field recursively
            for(int i = 0; i < fields.size(); i++) {
                DapVariable field = fields.get(i);
                variable(field);
            }
            odom.next();
        }
    }

    public void
    sequenceVariable(DapVariable var)
            throws DapException
    {
        DapSequence seq = (DapSequence) var.getBaseType();
        List<DapVariable> fields = seq.getFields();
        Odometer odom = null;
        if(var.getRank() == 0) {//scalar
            odom = Odometer.factoryScalar();
        } else {// dimensioned
            List<Slice> slices = ce.getConstrainedSlices(var);
            odom = Odometer.factory(slices, var.getDimensions());
        }
        try {
            while(odom.hasNext()) {
                // Decide how many rows for this sequence
                int nrows = (rowcount == 0 ? this.values.nextCount(MAXROWS)
                        : rowcount);
                writer.writeAtomicArray(DapType.INT64, new long[]{nrows});
                for(int i = 0; i < nrows; i++) {
                    for(int j = 0; j < fields.size(); j++) {
                        DapVariable field = fields.get(j);
                        variable(field);
                    }
                }
                odom.next();
            }
        } catch (IOException ioe) {
            throw new DapException(ioe);
        }
    }

    protected String
    stringify(Object v)
    {
        if(v.getClass().isArray())
            return stringify(java.lang.reflect.Array.get(v, 0));
        if(v instanceof Float || v instanceof Double)
            return v.toString();
        if(v instanceof Byte)
            return String.format("%01d 0x%01x", v, v);
        if(v instanceof Short)
            return String.format("%02d 0x%02x", v, v);
        if(v instanceof Integer)
            return String.format("%04d 0x%04x", v, v);
        if(v instanceof Long)
            return String.format("%08d 0x%08x", v, v);
        if(v instanceof Character)
            return String.format("'%c' 0x%02x", v, (short) v);
        return v.toString();
    }


}

