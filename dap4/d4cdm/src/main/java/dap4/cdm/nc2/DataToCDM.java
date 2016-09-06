/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.cdm.nc2;

import dap4.cdm.NodeMap;
import dap4.core.data.DSP;
import dap4.core.data.DataCursor;
import dap4.core.dmr.*;
import dap4.core.util.*;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create a set of CDM ucar.ma2.array objects that wrap a DSP.
 */

public class DataToCDM
{
    static public boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final int COUNTSIZE = 8; // databuffer as specified by the DAP4 spec

    static protected final String LBRACE = "{";
    static protected final String RBRACE = "}";

    //////////////////////////////////////////////////
    // Instance variables

    protected DapNetcdfFile ncfile = null;
    protected DSP dsp = null;
    protected DapDataset dmr = null;
    protected Group cdmroot = null;
    protected Map<Variable, Array> arraymap = null;
    protected NodeMap nodemap = null;

    //////////////////////////////////////////////////
    //Constructor(s)

    /**
     * Constructor
     *
     * @param ncfile the target NetcdfDataset
     * @param dsp    the compiled D4 databuffer
     */

    public DataToCDM(DapNetcdfFile ncfile, DSP dsp, NodeMap nodemap)
            throws DapException
    {
        this.ncfile = ncfile;
        this.dsp = dsp;
        this.dmr = dsp.getDMR();
        this.nodemap = nodemap;
        arraymap = new HashMap<Variable, Array>();
    }

    //////////////////////////////////////////////////
    // Compile DataCursor objects to ucar.ma2.Array objects

    /* package access */
    Map<Variable, Array>
    create()
            throws DapException
    {
        // iterate over the variables represented in the DSP
        List<DapVariable> topvars = this.dmr.getTopVariables();
        Map<Variable, Array> map = null;
        for(DapVariable var : topvars) {
            DataCursor cursor = this.dsp.getVariableData(var);
            Array array = createVar(cursor);
            Variable cdmvar = (Variable) nodemap.get(var);
            arraymap.put(cdmvar, array);
        }
        return this.arraymap;
    }

    protected Array
    createVar(DataCursor data)
            throws DapException
    {
        DapVariable d4var = (DapVariable) data.getTemplate();
        Array array = null;
        switch (d4var.getSort()) {
        case ATOMICVARIABLE:
            array = createAtomicVar(data);
            break;
        case SEQUENCE:
            array = createSequence(data);
            break;
        case STRUCTURE:
            array = createStructure(data);
            break;
        default:
            assert false : "Unexpected databuffer sort: " + d4var.getSort();
        }
        if(d4var.isTopLevel()) {
            // transfer the checksum attribute
            byte[] csum = d4var.getChecksum();
            String scsum = Escape.bytes2hex(csum);
            Variable cdmvar = (Variable) nodemap.get(d4var);
            Attribute acsum = new Attribute(DapUtil.CHECKSUMATTRNAME, scsum);
            cdmvar.addAttribute(acsum);
        }
        return array;
    }

    /**
     * Create an Atomic Valued variable.
     *
     * @return An Array object wrapping d4var.
     * @throws DapException
     */
    protected CDMArrayAtomic
    createAtomicVar(DataCursor data)
            throws DapException
    {
        CDMArrayAtomic array = new CDMArrayAtomic(data);
        return array;
    }

    /**
     * Create an array of structures. WARNING: the underlying CDM code
     * (esp. NetcdfDataset) apparently does not support nested
     * structure arrays; so this code may throw an exception.
     *
     * @return A CDMArrayStructure for the databuffer for this struct.
     * @throws DapException
     */
    protected CDMArrayStructure
    createStructure(DataCursor data)
            throws DapException
    {
        CDMArrayStructure arraystruct = new CDMArrayStructure(this.cdmroot, data);
        DapStructure struct = (DapStructure) data.getTemplate();
        int nmembers = struct.getFields().size();
        List<DapDimension> dimset = struct.getDimensions();
        if(((DapVariable)data.getTemplate()).getRank()  == 0) { // scalar
            for(int f = 0; f < nmembers; f++) {
                DataCursor dc = data.getField(f);
                Array afield = createVar(dc);
                arraystruct.add(0, f, afield);
            }
        } else {
            Odometer odom = Odometer.factory(DapUtil.dimsetSlices(dimset));
            while(odom.hasNext()) {
                Index index = odom.next();
                long offset = index.index();
                DataCursor ithelement = (DataCursor) data.read(index);
                for(int f = 0; f < nmembers; f++) {
                    DataCursor dc = ithelement.getField(f);
                    Array afield = createVar(dc);
                    arraystruct.add(offset, f, afield);
                }
            }
        }
        return arraystruct;
    }

    /**
     * Create a sequence. WARNING: the underlying CDM code
     * (esp. NetcdfDataset) apparently does not support nested
     * sequence arrays.
     *
     * @param data the data underlying this sequence instance
     * @return A CDMArraySequence for this instance
     * @throws DapException
     */

    protected CDMArraySequence
    createSequence(DataCursor data)
            throws DapException
    {
        CDMArraySequence arrayseq = new CDMArraySequence(this.cdmroot, data);
        DapSequence template = (DapSequence) data.getTemplate();
        List<DapDimension> dimset = template.getDimensions();
        long dimsize = DapUtil.dimProduct(dimset);
        int nfields = template.getFields().size();
        for(int r = 0; r < data.getRecordCount(); r++) {
            DataCursor rec = (DataCursor) data.getRecord(r);
            for(int f = 0; f < nfields; f++) {
                DataCursor dc = rec.getField(f);
                Array afield = createVar(dc);
                arrayseq.add(r, f, afield);
            }
        }
        return arrayseq;
    }
}
