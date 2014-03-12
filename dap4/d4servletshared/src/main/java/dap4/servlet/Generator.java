/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.ce.CEConstraint;
import dap4.core.dmr.*;
import dap4.core.util.*;

import java.io.*;
import java.nio.ByteOrder;
import java.util.List;

import static dap4.servlet.Value.ValueSource;

/**
 * Given a DMR, return:
 * 1. A byte array (byte[]) containing serialized data
 * and DMR
 * <p/>
 * Requirements:
 * 1. repeatability: given the same DMR, return the same byte array
 * <p/>
 * Notes:
 * 1. Two options are provided for generating values:
 * a. use of a random number generator with a fixed seed.
 * b. use of a pre-defined sequence of values with repetition
 * when the sequence is exhausted (not yet implemented).
 */

public class Generator extends DapSerializer
{
    static final boolean PARSEDEBUG = false;
    static final boolean DEBUG = false;

    static final String LBRACE = "{";
    static final String RBRACE = "}";

    static final int DEFALTCACHELIMIT = 4; // max cache size    

    static final ByteOrder DEFAULTORDER = ByteOrder.LITTLE_ENDIAN;

    static final String DATADIR = "tests/src/test/data"; // relative to opuls root
    static final String TESTSRC = DATADIR + "/resources/testfiles";

    static final String SERIALEXT = ".ser";
    static final String SRCEXT = ".dmr" + SERIALEXT;
    static final String DSTEXT = ".dap" + SERIALEXT;

    ValueSource source = null;

    //////////////////////////////////////////////////
    // Instance variables

    protected Value values = null; // Value generator
    protected ByteArrayOutputStream stream = null;
    protected byte[] serialization = null;
    protected ChunkWriter cw = null;
    protected CEConstraint ce = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Generator(ValueSource src)
        throws DapException
    {
        super();
        if(src == null)
            src = ValueSource.RANDOM; // default
        switch (src) {
        case FIXED:
        case RANDOM:
        default:
            values = new RandomValue();
            break;
        }
        SerialWriter.DEBUG = DEBUG;
    }

    //////////////////////////////////////////////////
    // Generator

    public void
    generate(DapDataset dmr, CEConstraint ce, ChunkWriter cw)
        throws DapException
    {
        this.dmr = dmr;
        this.cw = cw;
        this.ce = ce;
        this.order = cw.getOrder();

        generate();
    }

    protected void
    generate()
        throws DapException
    {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            DMRPrint dp = new DMRPrint(pw);
            dp.print(this.dmr, this.ce);
            pw.close();
            sw.close();
            String tmp = sw.toString();
            this.cw.writeDMR(tmp);
            this.cw.flush();
            writer = new SerialWriter(this.cw, this.order);
            generateData(); // generate the serialized data
        } catch (Exception e) {
            throw new DapException(e);
        }
    }

    protected void
    generateData()
        throws DapException
    {
        writer.startDataset();
        // Iterator over the variables in order
        for(DapVariable var : this.dmr.getTopVariables()) {
            if(!this.ce.references(var))
                continue;
            generateVar(var);
        }
        writer.endDataset();
    }

    protected void
    generateVar(DapVariable dapvar)
        throws DapException
    {
        writer.startVariable();
        if(dapvar.getSort() == DapSort.ATOMICVARIABLE) {
            generateAtomicVar(dapvar);
        } else {//dapvar.getSort() == DapSort.STRUCTURE)
            generateStructureVar(dapvar);
        }
        try {
            writer.endVariable();
        } catch (IOException ioe) {
            throw new DapException(ioe);
        }
    }

    protected void
    generateAtomicVar(DapVariable dapvar)
        throws DapException
    {
        DapType basetype = ((DapAtomicVariable) dapvar).getBaseType();
        Odometer odom = null;
        if(dapvar.getRank() == 0) {//scalar
            odom = Odometer.getScalarOdometer();
        } else {// dimensioned
            // get the slices from the constraint
            List<Slice> slices = ce.getVariableSlices(dapvar);
            // Create an odometer from the slices
            odom = new Odometer(slices,dapvar.getDimensions());
        }
        while(odom.hasNext()) {
            Object value = values.nextValue(basetype);
            try {
                writer.writeObject(basetype, value);
            } catch (IOException ioe) {
                throw new DapException(ioe);
            }
            odom.next();
        }
    }

    void
    generateStructureVar(DapVariable dapvar)
        throws DapException
    {
        DapStructure struct = (DapStructure) dapvar;
        List<DapVariable> fields = struct.getFields();
        Odometer odom = null;
        if(dapvar.getRank() == 0) {//scalar
            odom = Odometer.getScalarOdometer();
        } else {// dimensioned
            List<Slice> slices = ce.getVariableSlices(dapvar);
            odom = new Odometer(slices,struct.getDimensions());
        }
        while(odom.hasNext()) {
            // generate a value for each field recursively
            for(int i = 0;i < fields.size();i++) {
                DapVariable field = fields.get(i);
                generateVar(field);
            }
            odom.next();
        }
    }

}

