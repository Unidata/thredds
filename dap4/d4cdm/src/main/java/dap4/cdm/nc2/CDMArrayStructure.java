/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm.nc2;

import dap4.cdm.CDMTypeFcns;
import dap4.cdm.CDMUtil;
import dap4.core.data.DSP;
import dap4.core.data.DataCursor;
import dap4.core.dmr.DapStructure;
import dap4.core.dmr.DapType;
import dap4.core.dmr.DapVariable;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.core.util.Slice;
import dap4.dap4lib.LibTypeFcns;
import ucar.ma2.*;
import ucar.nc2.Group;

import java.nio.ByteBuffer;
import java.util.List;

import static dap4.core.data.DataCursor.Scheme;

/**
 * Implementation of ArrayStructure that wraps
 * DAP4 databuffer
 * (Note: Needs serious optimization applied).
 * <p>
 * Given
 * Structure S {f1,f2,...fm} [d1][d2]...[dn],
 * internally, this is stored as a 2-D array
 * CDMArray[][] instances;
 * The first dimension's length is d1*d2*...dn.
 * The second dimension has size |members| i.e. the number
 * of fields in the sequence.
 */

/*package*/ class
CDMArrayStructure extends ArrayStructure implements CDMArray
{
    //////////////////////////////////////////////////
    // Type decls

    /**
     * We need to keep a map of index X fieldno -> Array
     * representing the Array behind each field for each
     * struct instance in a matrix of struct instances.
     * To promote some clarity a eschew Array[|dimset|][|fields|]
     * in favor of FieldArrays[|dimset|].
     */
    static protected class FieldArrays
    {
         public Array[] fields; // Make externally accessible
        FieldArrays(int nfields)
        {
            fields = new Array[nfields];
        }

    }

    //////////////////////////////////////////////////
    // Instance variables

    // CDMArry variables
    protected Group cdmroot = null;
    protected DSP dsp = null;
    protected DapVariable template = null;
    protected DapType basetype = null;
    protected long dimsize = 0;
    protected int nmembers = 0;

    protected DataCursor data = null;

    /**
     * Since we are using StructureDataA,
     * we store a list Field sets
     * So we have a map: index -> Field object.
     * Total number of objects is dimsize.
     * Accessed by calls from StructureDataA.
     * Note: We use the super.sdata field to store
     * the StructureData instances.
     */

    // Note: term records here means the elements of the array,
    // not record as in Sequence

    protected FieldArrays[] records = null; // list of Structure elements


    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * Constructor
     *
     * @param cdmroot the parent CDMDataset
     * @param data    the structure data
     */
    CDMArrayStructure(Group cdmroot, DataCursor data)
    {
        super(computemembers((DapVariable) data.getTemplate()),
                CDMUtil.computeEffectiveShape(((DapVariable) data.getTemplate()).getDimensions()));
        this.template = (DapVariable) data.getTemplate();
        assert data.getScheme() == Scheme.STRUCTARRAY;
        this.dsp = data.getDSP();
        this.cdmroot = cdmroot;
        this.basetype = this.template.getBaseType();
        this.dimsize = DapUtil.dimProduct(template.getDimensions());
        this.nmembers = ((DapStructure) template.getBaseType()).getFields().size();

        this.data = data;

        // Fill in the structdata (in parent) and instance vectors
        super.sdata = new StructureDataA[(int) this.dimsize];
        records = new FieldArrays[(int) this.dimsize];
        for(int i = 0; i < dimsize; i++) {
            super.sdata[i] = new StructureDataA(this, i);
            records[i] = new FieldArrays(this.nmembers);
        }
    }

    /**
     *
     * @param recno   struct instance
     * @param fieldno  field of that struct
     * @param field   Array backing this field in this struct instance
     */
    /*package*/
    void
    add(long recno, int fieldno, Array field)
    {
        FieldArrays fs = records[(int) recno];
        if(fs == null)
            records[(int) recno] = (fs = new FieldArrays(this.nmembers));
        fs.fields[fieldno] = field;
    }
    //////////////////////////////////////////////////
    // CDMArray Interface

    @Override
    public DSP getDSP()
    {
        return this.dsp;
    }

    @Override
    public DapVariable getTemplate()
    {
        return this.template;
    }

    @Override
    public DapType getBaseType()
    {
        return this.basetype;
    }

    //////////////////////////////////////////////////
    // Accessors

    @Override
    public long getSize()
    {
        return this.dimsize;
    }

    //////////////////////////////////////////////////

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        DapVariable var = (DapVariable) this.template;
        DapStructure struct = (DapStructure) var.getBaseType();
        for(int i = 0; i < this.dimsize; i++) {
            List<DapVariable> fields = struct.getFields();
            if(i < (this.dimsize - 1))
                buf.append("\n");
            buf.append("Structure {\n");
            if(fields != null) {
                for(int j = 0; j < this.nmembers; j++) {
                    Array field = records[i].fields[j];
                    String sfield = (field == null ? "null" : fields.toString());
                    buf.append(sfield + "\n");
                }
            }
            buf.append(String.format("} [%d/%d]", i, dimsize));
        }
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // ArrayStructure interface

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
    public StructureData
    getStructureData(int index)
    {
        assert (super.sdata != null);
        if(index < 0 || index >= this.dimsize)
            throw new IllegalArgumentException(index + " >= " + super.sdata.length);
        assert (super.sdata[index] != null);
        return super.sdata[index];
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

    public double[]
    getJavaArrayDouble(int recnum, StructureMembers.Member m)
    {
        CDMArrayAtomic array = getAtomicArray(recnum, m);
        if(!array.getBaseType().isNumericType())
            throw new IllegalArgumentException("Cannot convert non-numeric type");
        DapType atomtype = array.getBaseType();
        try {
            List<Slice> slices = CDMUtil.shapeToSlices(m.getShape());
            Object vector = data.read(slices);
            return (double[]) LibTypeFcns.convertVector(DapType.FLOAT64, atomtype, vector);
        } catch (DapException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public float[]
    getJavaArrayFloat(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic array = getAtomicArray(index, m);
        if(!array.getBaseType().isNumericType())
            throw new IllegalArgumentException("Cannot convert non-numeric type");
        DapType atomtype = array.getBaseType();
        try {
            List<Slice> slices = CDMUtil.shapeToSlices(m.getShape());
            Object vector = data.read(slices);
            return (float[]) LibTypeFcns.convertVector(DapType.FLOAT32, atomtype, vector);
        } catch (DapException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public byte[]
    getJavaArrayByte(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic array = getAtomicArray(index, m);
        if(!array.getBaseType().isNumericType())
            throw new IllegalArgumentException("Cannot convert non-numeric type");
        DapType atomtype = array.getBaseType();
        try {
            List<Slice> slices = CDMUtil.shapeToSlices(m.getShape());
            Object vector = data.read(slices);
            return (byte[]) LibTypeFcns.convertVector(DapType.INT8, atomtype, vector);
        } catch (DapException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public short[]
    getJavaArrayShort(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic array = getAtomicArray(index, m);
        if(!array.getBaseType().isNumericType())
            throw new IllegalArgumentException("Cannot convert non-numeric type");
        DapType atomtype = array.getBaseType();
        try {
            List<Slice> slices = CDMUtil.shapeToSlices(m.getShape());
            Object vector = data.read(slices);
            return (short[]) LibTypeFcns.convertVector(DapType.INT16, atomtype, vector);
        } catch (DapException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public int[]
    getJavaArrayInt(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic array = getAtomicArray(index, m);
        if(!array.getBaseType().isNumericType())
            throw new IllegalArgumentException("Cannot convert non-numeric type");
        DapType atomtype = array.getBaseType();
        try {
            List<Slice> slices = CDMUtil.shapeToSlices(m.getShape());
            Object vector = data.read(slices);
            return (int[]) LibTypeFcns.convertVector(DapType.INT32, atomtype, vector);
        } catch (DapException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public long[]
    getJavaArrayLong(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic array = getAtomicArray(index, m);
        if(!array.getBaseType().isNumericType())
            throw new IllegalArgumentException("Cannot convert non-numeric type");
        DapType atomtype = array.getBaseType();
        try {
            List<Slice> slices = CDMUtil.shapeToSlices(m.getShape());
            Object vector = data.read(slices);
            return (long[]) LibTypeFcns.convertVector(DapType.INT64, atomtype, vector);
        } catch (DapException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public char[]
    getJavaArrayChar(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic array = getAtomicArray(index, m);
        if(!array.getBaseType().isNumericType())
            throw new IllegalArgumentException("Cannot convert non-numeric type");
        DapType atomtype = array.getBaseType();
        try {
            List<Slice> slices = CDMUtil.shapeToSlices(m.getShape());
            Object vector = data.read(slices);
            return (char[]) LibTypeFcns.convertVector(DapType.CHAR, atomtype, vector);
        } catch (DapException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public String[]
    getJavaArrayString(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic array = getAtomicArray(index, m);
        DapType atomtype = array.getBaseType();
        try {
            List<Slice> slices = CDMUtil.shapeToSlices(m.getShape());
            Object vector = data.read(slices);
            return (String[]) LibTypeFcns.convertVector(DapType.STRING, atomtype, vector);
        } catch (DapException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    public ByteBuffer[]
    getJavaArrayOpaque(int index, StructureMembers.Member m)
    {
        CDMArrayAtomic array = getAtomicArray(index, m);
        DapType atomtype = array.getBaseType();
        try {
            List<Slice> slices = CDMUtil.shapeToSlices(m.getShape());
            Object vector = data.read(slices);
            return (ByteBuffer[]) LibTypeFcns.convertVector(DapType.OPAQUE, atomtype, vector);
        } catch (DapException de) {
            throw new UnsupportedOperationException(de);
        }
    }

    // Non-atomic cases

    public StructureData
    getScalarStructure(int index, StructureMembers.Member m)
    {
        if(m.getDataType() != DataType.STRUCTURE)
            throw new ForbiddenConversionException("Atomic field cannot be converted to Structure");
        Array ca = memberArray(index, memberIndex(m));
        if(ca.getDataType() != DataType.STRUCTURE && ca.getDataType() != DataType.SEQUENCE)
            throw new ForbiddenConversionException("Attempt to access non-structure member");
        CDMArrayStructure as = (CDMArrayStructure) ca;
        return as.getStructureData(0);
    }

    public ArrayStructure
    getArrayStructure(int index, StructureMembers.Member m)
    {
        if(m.getDataType() != DataType.STRUCTURE)
            throw new ForbiddenConversionException("Atomic field cannot be converted to Structure");
        Array dd = memberArray(index, memberIndex(m));
        if(dd.getDataType() != DataType.STRUCTURE && dd.getDataType() != DataType.SEQUENCE)
            throw new ForbiddenConversionException("Attempt to access non-structure member");
        return (CDMArrayStructure) dd;
    }

    public ArraySequence
    getArraySequence(StructureMembers.Member m)
    {
        throw new UnsupportedOperationException("CDMArraySequence");
    }

    @Override
    public Array copy()
    {
        return this; // temporary
    }

    /////////////////////////
    // Define API required by StructureDataA

    /**
     * Key interface method coming in from StructureDataA.
     *
     * @param recno The instance # of the array of Structure instances
     * @param m     The member of interest in the Structure instance
     * @return The ucar.ma2.Array instance corresponding to the instance.
     * <p>
     * Hidden: friend of StructureDataA
     */
    @Override
    public ucar.ma2.Array
    getArray(int recno, StructureMembers.Member m)
    {
        return (ucar.ma2.Array) memberArray(recno, memberIndex(m));
    }

    //////////////////////////////////////////////////
    // Utilities

    @Override
    protected StructureData
    makeStructureData(ArrayStructure as, int index)
    {
        if(super.sdata[index] == null)
            super.sdata[index] = new StructureDataA(as, index);
        return super.sdata[index];
    }

    /**
     * Compute the StructureMembers object
     * from a DapStructure. May need to recurse
     * if a field is itself a Structure
     *
     * @param var The DapVariable to use to construct
     *           a StructureMembers object.
     * @return The StructureMembers object for the given DapStructure
     */
    static StructureMembers
    computemembers(DapVariable var)
    {
        DapStructure ds = (DapStructure)var.getBaseType();
        StructureMembers sm
                = new StructureMembers(ds.getShortName());
        List<DapVariable> fields = ds.getFields();
        for(int i = 0; i < fields.size(); i++) {
            DapVariable field = fields.get(i);
            DapType dt = field.getBaseType();
            DataType cdmtype = CDMTypeFcns.daptype2cdmtype(dt);
            StructureMembers.Member m =
                    sm.addMember(
                            field.getShortName(), "", null,
                            cdmtype,
                            CDMUtil.computeEffectiveShape(field.getDimensions()));
            m.setDataParam(i); // So we can index into various lists
            // recurse if this field is itself a structure
            if(dt.getTypeSort().isStructType()) {
                StructureMembers subsm = computemembers(field);
                m.setStructureMembers(subsm);
            }
        }
        return sm;
    }

    /**
     * @param recno The instance # of the array of Structure instances
     * @param memberindex    The member of interest in the Structure instance
     * @return The ucar.ma2.Array instance corresponding to the instance.
     */
    protected Array
    memberArray(int recno, int memberindex)
    {
        DapVariable var = (DapVariable) this.getTemplate();
        DapStructure struct = (DapStructure)var.getBaseType();
        DapVariable field = struct.getField(memberindex);
        DapType base = field.getBaseType();
        if(base == null)
            throw new IllegalStateException("Unknown field type: " + field);
        Object[] values = new Object[(int) field.getCount()];
        FieldArrays fs = records[recno];
        Array fa = fs.fields[memberindex];
        return fa;
    }

    static protected int
    memberIndex(StructureMembers.Member m)
    {
        return m.getDataParam();
    }

    protected CDMArrayAtomic
    getAtomicArray(int index, StructureMembers.Member m)
    {
        Array dd = memberArray(index, memberIndex(m));
        if(dd.getDataType() != DataType.STRUCTURE && dd.getDataType() != DataType.SEQUENCE)
            return (CDMArrayAtomic) dd;
        throw new ForbiddenConversionException("Cannot convert structure to AtomicArray");
    }

}
