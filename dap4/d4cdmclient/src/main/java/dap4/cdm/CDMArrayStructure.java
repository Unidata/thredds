/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package   dap4.cdm;

import dap4.cdmshared.CDMUtil;
import dap4.core.data.DataException;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4shared.*;
import ucar.ma2.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of ArrayStructure that wraps
 * DAP4 databuffer
 * <p/>
 * Needs serious optimization applied.
 */

public class CDMArrayStructure extends ArrayStructure implements CDMArray
{
    //////////////////////////////////////////////////
    // Instance variables

    // CDMArry variables
    protected CDMDataset root = null;
    protected D4DSP dsp = null;
    protected DapVariable template = null;
    protected long bytesize = 0;
    protected DapType basetype = null;
    protected AtomicType primitivetype = null;

    protected D4DataCompoundArray d4data = null;
    protected long dimsize = 0;

    protected int dimactual = 0;

    /**
     * Since we are using StructureData,
     * we do not actually need to keep the
     * D4DataStructure instances as such.
     * We need a "map" from index X member to a
     * CDMArray object.
     * Total number of objects is dimsize * |members|.
     * Accessed by StructureData.
     * We linearize the array so that asking for
     * element (dim,fielindex) => dim*|members\ + fieldindex.
     */

    protected CDMArray[] instances = null;

    /**
     * We need instances of StructureData to give to the user.
     * We use StructureDataA so we can centralize everything
     * in this class. The total number of StructureData objects
     * is dimsize.
     * If StructureDataA was an interface, then we
     *  could merge with the instances vector above.
     */

    protected StructureDataA[] structdata = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * Constructor
     *
     * @param dsp    the parent DSP
     * @param root   the parent CDMDataset
     * @param d4data the structure data
     */
    CDMArrayStructure(D4DSP dsp, CDMDataset root, D4DataCompoundArray d4data)
    {
        super(computemembers((DapStructure) d4data.getTemplate()),
            CDMUtil.computeEffectiveShape(((DapVariable) d4data.getTemplate()).getDimensions()));
        this.dsp = dsp;
        this.root = root;
        this.template = (DapVariable) d4data.getTemplate();
        this.basetype = this.template.getBaseType();
        this.primitivetype = this.basetype.getPrimitiveType();

        this.dimsize = DapUtil.dimProduct(template.getDimensions());
        this.d4data = d4data;
        // Fill in the instances and structdata vectors
        // The leaf instances arrays will be filled in by the CDM compiler
        structdata = new StructureDataA[(int) this.dimsize];
        int nmembers = ((DapStructure) template).getFields().size();
        instances = new CDMArray[(int) (this.dimsize*nmembers)];
        Arrays.fill(instances, null);
        for(int i = 0;i < dimsize;i++) {
            structdata[i] = new StructureDataA(this, i);
        }
    }

    /*package access*/ void
    finish()
    {
        int nmembers = ((DapStructure) template).getFields().size();
        for(int i=0;i<this.dimactual;i++) {
            for(int m=0;m<nmembers;m++) {
                int offset = (int)(i * this.dimsize) + m;
                assert instances[offset] != null;
            }
        }
        this.bytesize = computeTotalSize();
    }

    //////////////////////////////////////////////////
    // CDMArry Interface

    @Override
    public D4DSP getDSP()
    {
        return dsp;
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

    @Override
    public DapType getBaseType()
    {
        return this.basetype;
    }

    @Override
    public AtomicType getPrimitiveType()
    {
        return this.primitivetype;
    }


    //////////////////////////////////////////////////
    // Accessors

    @Override
    public long getSize()
    {
        return this.dimsize;
    }

    void addField(int index, StructureMembers.Member m, Array instance)
    {
        int mindex = memberIndex(m);
        addField(index, m, instance);
    }

    void addField(long index, int mindex, CDMArray instance)
    {
        assert this.instances != null : "Internal Error";
        if(index < 0 || index >= this.instances.length)
            throw new ArrayIndexOutOfBoundsException("CDMArrayStructure.addInstance: index out of range: " + index);
        if(index > this.dimactual) this.dimactual = (int)index+1;
        int offset = (int)(index * this.dimsize) + mindex;
        this.instances[offset] = instance; // WARNING: overwrites
    }

    //////////////////////////////////////////////////

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        DapStructure struct = (DapStructure) this.template;
        for(int i = 0;i < dimsize;i++) {
            List<DapVariable> fields = struct.getFields();
            if(i < (dimsize - 1))
                buf.append("\n");
            buf.append("Structure {\n");
            if(fields != null)
                for(int j = 0;j < fields.size();j++) {
                    DapVariable field = fields.get(j);
                    String sfield;
                    int offset = (int)((i*this.dimsize) + j);
                    if(instances != null && offset < instances.length
                        && instances[offset] != null) {
                        CDMArray array = instances[offset];
                        sfield = (array == null ? "null" : array.toString());
                    } else
                        sfield = "null";
                    buf.append(sfield + "\n");
                }
            buf.append(String.format("} [%d/%d]", i, dimsize));
        }
        return buf.toString();
    }

    public long
    computeTotalSize()
    {
        long totalsize = 0;
        int nmembers = this.getStructureMembers().getMembers().size();
        for(int recno = 0;recno < this.dimactual;recno++) {
            for(int m = 0; m < nmembers;m++) {
                int offset = (int)((recno*this.dimsize)+m);
                totalsize += instances[offset].getByteSize();
            }
        }
        return totalsize;
    }

    //////////////////////////////////////////////////
    // ArrayStructure interface

    /**
     * Key interface method coming in from StructureDataA.
     *
     * @param index The instance # of the array of Structure instances
     * @param m     The member of interest in the Structure instance
     * @return The ucar.ma2.Array instance corresponding to the instance.
     */
    public ucar.ma2.Array
    getArray(int index, StructureMembers.Member m)
    {
        if(index < 0 || index >= this.instances.length)
            throw new ArrayIndexOutOfBoundsException("CDMArrayStructure.getArray: index out of range: " + index);
        int mindex = memberIndex(m);
        int offset = (int)((index*this.dimsize)+mindex);
        return (ucar.ma2.Array) instances[offset];
    }

    public double getScalarDouble(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic data = getAtomicArray(index, m);
        return data.getDouble(0);
    }

    public float
    getScalarFloat(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic data = getAtomicArray(index, m);
        return data.getFloat(0);
    }

    public byte
    getScalarByte(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic data = getAtomicArray(index, m);
        return data.getByte(0);
    }

    public short
    getScalarShort(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic data = getAtomicArray(index, m);
        return data.getShort(0);
    }

    public int
    getScalarInt(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic data = getAtomicArray(index, m);
        return data.getInt(0);
    }

    public long
    getScalarLong(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic data = getAtomicArray(index, m);
        return data.getLong(0);
    }

    public char
    getScalarChar(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic data = getAtomicArray(index, m);
        return data.getChar(0);
    }

    /**
     * Get member databuffer of type String or char.
     *
     * @param recnum get databuffer from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
     * @param m      get databuffer from this StructureMembers.Member. Must be of type String or char.
     * @return scalar String value
     */
    public String getScalarString(int recnum, StructureMembers.Member m)
    {
        Array data = m.getDataArray();
        return (String) data.getObject(recnum).toString();
    }

    public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m)
    {
        D4DataAtomic data = getAtomicArray(recnum,m).getData();
        DapType atype = data.getType();
        long count = atype.getSize();
        try {
            Object vector = Dap4Util.createVector(atype.getPrimitiveType(),count);
	        data.read(0,count,vector);
            return (double[])Dap4Util.convertVector(DapType.FLOAT64,atype,vector);
        } catch (DataException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public float[]
    getJavaArrayFloat(int index, StructureMembers.Member m)
    {
        D4DataAtomic data = getAtomicArray(index,m).getData();
        DapType atype = data.getType();
        long count = atype.getSize();
        try {
            Object vector = Dap4Util.createVector(atype.getPrimitiveType(),count);
	        data.read(0,count,vector);
            return (float[])Dap4Util.convertVector(DapType.FLOAT32,atype,vector);
        } catch (DataException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public byte[]
    getJavaArrayByte(int index, StructureMembers.Member m)
    {
        D4DataAtomic data = getAtomicArray(index,m).getData();
        DapType atype = data.getType();
        long count = atype.getSize();
        try {
            Object vector = Dap4Util.createVector(atype.getPrimitiveType(),count);
	        data.read(0,count,vector);
            return (byte[])Dap4Util.convertVector(DapType.INT8,atype,vector);
        } catch (DataException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public short[]
    getJavaArrayShort(int index, StructureMembers.Member m)
    {
        D4DataAtomic data = getAtomicArray(index,m).getData();
        DapType atype = data.getType();
        long count = atype.getSize();
        try {
            Object vector = Dap4Util.createVector(atype.getPrimitiveType(),count);
	        data.read(0,count,vector);
            return (short[])Dap4Util.convertVector(DapType.INT16,atype,vector);
        } catch (DataException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public int[]
    getJavaArrayInt(int index, StructureMembers.Member m)
    {
        D4DataAtomic data = getAtomicArray(index,m).getData();
        DapType atype = data.getType();
        long count = atype.getSize();
        try {
            Object vector = Dap4Util.createVector(atype.getPrimitiveType(),count);
	        data.read(0,count,vector);
            return (int[])Dap4Util.convertVector(DapType.INT32,atype,vector);
        } catch (DataException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public long[]
    getJavaArrayLong(int index, StructureMembers.Member m)
    {
        D4DataAtomic data = getAtomicArray(index,m).getData();
        DapType atype = data.getType();
        long count = atype.getSize();
        try {
            Object vector = Dap4Util.createVector(atype.getPrimitiveType(),count);
	        data.read(0,count,vector);
            return (long[])Dap4Util.convertVector(DapType.INT64,atype,vector);
        } catch (DataException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public char[]
    getJavaArrayChar(int index, StructureMembers.Member m)
    {
        D4DataAtomic data = getAtomicArray(index,m).getData();
        DapType atype = data.getType();
        long count = atype.getSize();
        try {
            Object vector = Dap4Util.createVector(atype.getPrimitiveType(),count);
	        data.read(0,count,vector);
            return (char[])Dap4Util.convertVector(DapType.CHAR,atype,vector);
        } catch (DataException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public String[]
    getJavaArrayString(int index, StructureMembers.Member m)
    {
        D4DataAtomic data = getAtomicArray(index,m).getData();
        DapType atype = data.getType();
        long count = atype.getSize();
        try {
            Object vector = Dap4Util.createVector(atype.getPrimitiveType(),count);
	        data.read(0,count,vector);
            return (String[])Dap4Util.convertVector(DapType.STRING,atype,vector);
        } catch (DataException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public ByteBuffer[]
    getJavaArrayOpaque(int index, StructureMembers.Member m)
    {
        D4DataAtomic data = getAtomicArray(index,m).getData();
        DapType atype = data.getType();
        long count = atype.getSize();
        try {
            Object vector = Dap4Util.createVector(atype.getPrimitiveType(),count);
	        data.read(0,count,vector);
            return (ByteBuffer[])Dap4Util.convertVector(DapType.OPAQUE,atype,vector);
        } catch (DataException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    // Non-atomic cases

    public StructureData getScalarStructure(int index,
                                            StructureMembers.Member m)
    {
        if(m.getDataType() != DataType.STRUCTURE)
            throw new ForbiddenConversionException("Atomic field cannot be converted to Structure");
        CDMArray ca = memberArray(index, memberIndex(m));
        if(!ca.getBaseType().isCompound())
            throw new ForbiddenConversionException("Attempt to access non-structure member");
        CDMArrayStructure as = (CDMArrayStructure) ca;
        return as.getStructureData(0);
    }

    public ArrayStructure getArrayStructure(int index, StructureMembers.Member m)
    {
        if(m.getDataType() != DataType.STRUCTURE)
            throw new ForbiddenConversionException("Atomic field cannot be converted to Structure");
        CDMArray dd = memberArray(index, memberIndex(m));
        if(!dd.getBaseType().isCompound())
            throw new ForbiddenConversionException("Attempt to access non-structure member");
        return (CDMArrayStructure) dd;
    }

    public ArraySequence getArraySequence(StructureMembers.Member m)
    {
        throw new UnsupportedOperationException("CDMArraySequence");
    }

    //////////////////////////////////////////////////
    // Utilities

    @Override
    protected StructureData
    makeStructureData(ArrayStructure as, int index)
    {
        if(structdata[index] == null)
            structdata[index] = new StructureDataA(as, index);
        return structdata[index];
    }

    /**
     * Compute the StructureMembers object
     * from a DapStructure. May need to recurse
     * if a field is itself a Structure
     *
     * @param ds The DapStructure to use to construct
     *           a StructureMembers object.
     * @return The StructureMembers object for the given DapStructure
     */
    static protected StructureMembers
    computemembers(DapStructure ds)
    {
        StructureMembers sm
            = new StructureMembers(ds.getShortName());
        List<DapVariable> fields = ds.getFields();
        for(int i = 0;i < fields.size();i++) {
            DapVariable field = fields.get(i);
            StructureMembers.Member m =
                sm.addMember(
                    field.getShortName(), "", null,
                    CDMUtil.daptype2cdmtype(field.getBaseType()),
                    CDMUtil.computeEffectiveShape(ds.getDimensions()));
            m.setDataParam(i); // So we can index into various lists
            // recurse if this field is itself a structure
            if(field.getSort() == DapSort.STRUCTURE) {
                StructureMembers subsm = computemembers((DapStructure) field);
                m.setStructureMembers(subsm);
            }
        }
        return sm;
    }

    protected CDMArray
    memberArray(int recno, int memberindex)
    {
        int offset = (int)((recno*this.dimsize)+memberindex);
        CDMArray cdmdata = instances[offset];
        return cdmdata;
    }

    static protected int
    memberIndex(StructureMembers.Member m)
    {
        return m.getDataParam();
    }

    protected CDMArrayAtomic
    getAtomicArray(int index, StructureMembers.Member m)
    {
        CDMArray dd = memberArray(index, memberIndex(m));
        if(!dd.getBaseType().isCompound())
            return (CDMArrayAtomic) dd;
        throw new ForbiddenConversionException("Cannot convert structure to AtomicArray");
    }

}
