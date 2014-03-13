/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package   dap4.cdm;

import dap4.cdmshared.NodeMap;
import dap4.core.data.DataSort;
import dap4.core.data.DataVariable;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4shared.*;
import ucar.nc2.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * The goal for the CDM compiler is two-fold:
 * 1. Create a set of CDMNodes corresponding to the
 * relevant nodes in the DMR.
 * 2. Create a set of CDM ucar.ma2.array objects that wrap the
 * DataDataset object.
 */

public class CDMCompiler
{
    static final boolean DEBUG = false;
    static final boolean DUMP = false;

    //////////////////////////////////////////////////
    // Constants

    static final int COUNTSIZE = 8; // databuffer as specified by the DAP4 spec

    static String LBRACE = "{";
    static String RBRACE = "}";

    static final String CHECKSUMATTRNAME = "_DAP4_Checksum_CRC32";

    static final int CHECKSUMSIZE = 4; // for CRC32

    //////////////////////////////////////////////////
    // Instance variables

    DapNetcdfFile ncfile = null;
    D4DSP dsp = null;
    DapDataset dmr = null;
    D4DataDataset d4root = null;
    CDMDataset cdmroot = null;

    NodeMap nodemap = new NodeMap();

    //////////////////////////////////////////////////
    //Constructor(s)

    /**
     * Constructor
     *
     * @param ncfile the target NetcdfFile
     * @param dsp    the compiled D4 databuffer
     */

    public CDMCompiler(DapNetcdfFile ncfile, D4DSP dsp)
        throws DapException
    {
        this.ncfile = ncfile;
        this.dsp = dsp;
        this.d4root = (D4DataDataset) dsp.getDataDataset();
        this.dmr = dsp.getDMR();
    }

    //////////////////////////////////////////////////
    // Accessors

    public NodeMap getNodeMap()
    {
        return nodemap;
    }

    //////////////////////////////////////////////////
    // Compile DMR->set of CDM nodes

    /**
     * Convert a DMR to equivalent CDM meta-databuffer
     * and populate a NetcdfFile with it.
     *
     * @throws DapException
     */

    protected void
    compileDMR()
        throws DapException
    {
        // Convert the DMR to CDM metadata
        // and return a mapping from DapNode -> CDMNode
        this.nodemap = new DSPToCDM(this.ncfile, this.dmr).create();
    }

    //////////////////////////////////////////////////
    // Compile Data objects to ucar.ma2.Array objects

    /* package access*/
    void
    compile()
        throws DapException
    {
        assert d4root.getSort() == DataSort.DATASET;
        //cdmroot = new CDMDataset();
        compileDMR();
        // iterate over the variables represented in the databuffer
        List<DataVariable> vars = d4root.getTopVariables();
        for(DataVariable var : vars) {
            CDMArray array = compileVar(var);
            Variable cdmvar = (Variable) nodemap.get(var.getTemplate());
            cdmvar.setCachedData((ucar.ma2.Array) array);
        }
    }

    protected CDMArray
    compileVar(DataVariable d4var)
        throws DapException
    {
        CDMArray array = null;
        DapVariable dapvar = (DapVariable) d4var.getTemplate();
        switch (d4var.getSort()) {
        case ATOMIC:
            array = compileAtomicVar(d4var);
            break;
        case SEQUENCE:
            array = null; //compileSequenceArray((D4DataCompoundArray)d4var);
            break;
        case COMPOUNDARRAY:
            array = compileStructureArray((D4DataCompoundArray) d4var);
            break;
        default:
            assert false : "Unexpected databuffer sort: " + d4var.getSort();
        }
        if(dapvar.isTopLevel()) {
            // transfer the checksum attribute
            byte[] csum = dapvar.getChecksum();
            String scsum = Escape.bytes2hex(csum);
            Variable cdmvar = (Variable) nodemap.get(dapvar);
            Attribute acsum = new Attribute(CHECKSUMATTRNAME, scsum);
            cdmvar.addAttribute(acsum);
        }
        return array;
    }

    /**
     * Compile an Atomic Valued variable.
     *
     * @param d4var The D4 databuffer wrapper
     * @return An Array object wrapping d4var.
     * @throws DapException
     */
    protected CDMArray
    compileAtomicVar(DataVariable d4var)
        throws DapException
    {
        DapAtomicVariable atomvar = (DapAtomicVariable) d4var.getTemplate();
        DapType daptype = atomvar.getBaseType();
        CDMArrayAtomic array = new CDMArrayAtomic(this.dsp, this.cdmroot, (D4DataAtomic) d4var);
        return array;
    }

    /**
     * Compile a single structure instance. WARNING: the underlying CDM code
     * (esp. NetcdfDataset) apparently does not support nested
     * structure arrays; so this code may throw an exception.
     *
     * @param d4var     the data underlying this structure instance
     * @param index     the index in the parent compound array.
     * @param container the parent CDMArrayStructure
     * @return An Array for this instance
     * @throws DapException
     */
    protected CDMArray
    compileStructure(D4DataStructure d4var, int index, CDMArrayStructure container)
        throws DapException
    {
        assert (d4var.getSort() == DataSort.STRUCTURE);
        DapStructure dapstruct = (DapStructure) d4var.getTemplate();
        assert (dapstruct.getRank() > 0 || index == 0);
        int nmembers = dapstruct.getFields().size();
        List<DapVariable> dfields = dapstruct.getFields();
        assert nmembers == dfields.size();
        for(int m = 0;m<nmembers;m++) {
            DataVariable dfield = d4var.readfield(m);
            CDMArray afield = (CDMArray) compileVar(dfield);
            container.addField(index, m, afield);
        }
        container.finish();
        return container;
    }

    /**
     * Compile an array of structures. WARNING: the underlying CDM code
     * (esp. NetcdfDataset) apparently does not support nested
     * structure arrays; so this code may throw an exception.
     *
     * @param d4var The D4 databuffer wrapper
     * @return A CDMArrayStructure for the databuffer for this struct.
     * @throws DapException
     */
    protected CDMArray
    compileStructureArray(D4DataCompoundArray d4var)
        throws DapException
    {
        assert (d4var.getSort() == DataSort.COMPOUNDARRAY);
        DapStructure dapstruct = (DapStructure) d4var.getTemplate();
        long dimproduct = DapUtil.dimProduct(dapstruct.getDimensions());
        CDMArrayStructure arraystruct
            = new CDMArrayStructure(this.dsp, this.cdmroot, d4var);
        try {
            for(int i = 0;i < dimproduct;i++) {
                D4DataStructure dds = (D4DataStructure) d4var.read(i);
                compileStructure(dds, i, arraystruct);
            }
            arraystruct.finish();
            return arraystruct;
        } catch (IOException ioe) {
            throw new DapException(ioe);
        }
    }

    /**
     * Compile a sequence. WARNING: the underlying CDM code
     * (esp. NetcdfDataset) apparently does not support nested
     * sequence arrays.
     *
     * @param d4var     the data underlying this sequence instance
     * @param index     the index in the parent compound array.
     * @param container the parent CDMArraySequence
     * @return A CDMArray for this instance
     * @throws DapException
     */

    /*
    protected Array
    compileSequence(D4DataSequence d4var, int index, CDMArrayVLEN container)
        throws DapException
    {
        assert (d4var.getSort() == DataSort.SEQUENCE);
        DapSequence dapseq = (DapSequence) d4var.getTemplate();
        assert (dapseq.getRank() > 0 || index == 0);
        List<DapVariable> dfields = dapseq.getFields();
        int nmembers = dfields.size();
        for(int m = 0;m < dfields.size();m++) {
            DapVariable dfield = dfields.get(m);
            CDMArray afield = compileVar(fdv);
            container.addField(i, m, afield);
        }
        container.computeTotalSize();
        return container;
    }  */

    /**
     * Compile an array of sequences. WARNING: the underlying CDM code
     * (esp. NetcdfDataset) apparently does not support nested
     * sequence arrays; so this code may throw an exception.
     * <p/>
     * param d4var The D4 databuffer wrapper
     * return A CDMArraySequence for the databuffer for this seq.
     * throws DapException
     */
    /*
    protected Array
    compileSequenceArray(D4DataCompoundArray d4var)
        throws DapException
    {
        assert (d4var.getSort() == DataSort.SEQUENCE);
        DapSequence dapseq = (DapSequence) d4var.getTemplate();
        assert (dapseq.getRank() > 0);
        long dimproduct =  DapUtil.computeDimProduct(dapseq.getDimensions());
        CDMArraySequence arrayseq
            = new CDMArraySequence(this.ds, dapseq);
        for(int i = 0;i < dimproduct;i++) {
            D4DataSequence dds = (D4DataSequence) d4var.read(i);
            ArraySequence instance
                = compileSequence(dds, i, arrayseq);
        }
        arrayseq.computeTotalSize();
        return arrayseq;
    }  */

    /*
    Array
    compileAtomicVLEN(ViewVariable annotation)
        throws DapException
    {
        DapAtomicVariable atomvar = (DapAtomicVariable) annotation.getVariable();
        DapType daptype = atomvar.getBaseType();
        List<DapDimension> dimset = atomvar.getDimensions();

        // For the VLEN case, we need to build a simple Array whose storage
        // is Object. Each element of the storage will contain
        // a Dap4AtomicVLENArray pointing to one of the vlen instances.

        // Compute rank upto the VLEN
        int prefixrank = dimset.size() - 1;

        // Compute product size up to the VLEN
        int dimproduct = 1;
        for(int i = 0;i < prefixrank;i++)
            dimproduct *= dimset.get(i).getSize();

        // Collect the vlen's databuffer arrays
        Object[] databuffer = new Object[dimproduct];
        List<Slice> slices = new ArrayList<Slice>(); // reusable
        for(int i = 0;i < dimproduct;i++) {
            int savepos = databuffer.position();  // mark the start of this instance
            // Get the number of elements in this vlen instance
            int count = getCount(databuffer);
            slices.clear();
            slices.add(new Slice(0, count - 1, 1)); // create synthetic slice to cover the vlen count
            Dap4AtomicVLENArray vlenarray
                = new Dap4AtomicVLENArray(this.dap4dataset, atomvar, slices, databuffer.position());
            databuffer[i] = vlenarray;
            vlenarray.setSize(count, databuffer.position());
            if(!daptype.isEnumType() && !daptype.isFixedSize()) {
                // this is a string, url, or opaque
                int[] positions = new int[count];
                long total = walkByteStrings(positions, databuffer);
                vlenarray.setByteStrings(positions, total);
            }
            vlenarray.computeTotalSize();
            databuffer.position(savepos);
            skip(databuffer, (int) vlenarray.getTotalSize());
        }

        // Construct the return array; code taken from Nc4Iosp
        if(prefixrank == 0) // if scalar, return just the len Array
            return (Array) databuffer[0];
        //if(prefixrank == 1)
        //    return (Array) new ArrayObject(databuffer[0].getClass(), new int[]{dimproduct}, databuffer);

        // Otherwise create and fill in an n-dimensional Array Of Arrays
        int[] shape = new int[prefixrank];
        for(int i = 0;i < prefixrank;i++)
            shape[i] = (int) dimset.get(i).getSize(); //todo: or do we use the annotation
        Array ndimarray = Array.factory(Array.class, shape);
        // Transfer the elements of databuffer into the n-dim arrays
        IndexIterator iter = ndimarray.getIndexIterator();
        for(int i = 0;iter.hasNext();i++) {
            iter.setObjectNext(databuffer[i]);
        }
        return ndimarray;
    }
    */
    static void
    skip(ByteBuffer data, int count)
    {
        data.position(data.position() + count);
    }

    static int
    getCount(ByteBuffer data)
    {
        long count = data.getLong();
        return (int) (count & 0xFFFFFFFF);
    }

    /**
     * Compute the size in databuffer of the serialized form
     *
     * param daptype
     * @return type's serialized form size
     */
/*    static int
    computeTypeSize(DapType daptype)
    {
        AtomicType atype = daptype.getAtomicType();
        if(atype == AtomicType.Enum) {
            DapEnum dapenum = (DapEnum) daptype;
            atype = dapenum.getBaseType().getAtomicType();
        }
        return Dap4Util.daptypeSize(atype);
    }
    */


    static long
    walkByteStrings(int[] positions, ByteBuffer databuffer)
    {
        int count = positions.length;
        long total = 0;
        int savepos = databuffer.position();
        // Walk each bytestring
        for(int i = 0;i < count;i++) {
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

    /*
    Array
    compileAtomicVLEN(ViewVariable annotation)
        throws DapException
    {
        DapAtomicVariable atomvar = (DapAtomicVariable) annotation.getVariable();
        DapType daptype = atomvar.getBaseType();
        List<DapDimension> dimset = atomvar.getDimensions();

        // For the VLEN case, we need to build a simple Array whose storage
        // is Object. Each element of the storage will contain
        // a Dap4AtomicVLENArray pointing to one of the vlen instances.

        // Compute rank upto the VLEN
        int prefixrank = dimset.size() - 1;

        // Compute product size up to the VLEN
        int dimproduct = 1;
        for(int i = 0;i < prefixrank;i++)
            dimproduct *= dimset.get(i).getSize();

        // Collect the vlen's databuffer arrays
        Object[] databuffer = new Object[dimproduct];
        List<Slice> slices = new ArrayList<Slice>(); // reusable
        for(int i = 0;i < dimproduct;i++) {
            int savepos = databuffer.position();  // mark the start of this instance
            // Get the number of elements in this vlen instance
            int count = getCount(databuffer);
            slices.clear();
            slices.add(new Slice(0, count - 1, 1)); // create synthetic slice to cover the vlen count
            Dap4AtomicVLENArray vlenarray
                = new Dap4AtomicVLENArray(this.d4dataset, atomvar, slices, databuffer.position());
            databuffer[i] = vlenarray;
            vlenarray.setSize(count, databuffer.position());
            if(!daptype.isEnumType() && !daptype.isFixedSize()) {
                // this is a string, url, or opaque
                int[] positions = new int[count];
                long total = walkByteStrings(positions, databuffer);
                vlenarray.setByteStrings(positions, total);
            }
            vlenarray.computeTotalSize();
            databuffer.position(savepos);
            skip(databuffer, (int) vlenarray.getTotalSize());
        }

        // Construct the return array; code taken from Nc4Iosp
        if(prefixrank == 0) // if scalar, return just the len Array
            return (Array) databuffer[0];
        //if(prefixrank == 1)
        //    return (Array) new ArrayObject(databuffer[0].getClass(), new int[]{dimproduct}, databuffer);

        // Otherwise create and fill in an n-dimensional Array Of Arrays
        int[] shape = new int[prefixrank];
        for(int i = 0;i < prefixrank;i++)
            shape[i] = (int) dimset.get(i).getSize(); //todo: or do we use the annotation
        Array ndimarray = Array.factory(Array.class, shape);
        // Transfer the elements of databuffer into the n-dim arrays
        IndexIterator iter = ndimarray.getIndexIterator();
        for(int i = 0;iter.hasNext();i++) {
            iter.setObjectNext(databuffer[i]);
        }
        return ndimarray;
    }
    */
}
