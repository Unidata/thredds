/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapSort;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This reifies all of the atomic types
 * and specifically all enumeration declarations
 * as specific objects denoting a type.
 * Structures and Sequence are specifically excluded
 * WARNING: these DapType instances have a fake parent Dataset group
 */

public class DapType extends DapNode implements DapDecl
{
    /**
     * Define instances of DapType for every TypeSort.
     * Watch out: the static lists must be immutable
     * because they might be shared by the server and client
     * during testing
     */

    static public final DapType CHAR;
    static public final DapType INT8;
    static public final DapType UINT8;
    static public final DapType INT16;
    static public final DapType UINT16;
    static public final DapType INT32;
    static public final DapType UINT32;
    static public final DapType INT64;
    static public final DapType UINT64;
    static public final DapType FLOAT32;
    static public final DapType FLOAT64;
    static public final DapType STRING;
    static public final DapType URL;
    static public final DapType OPAQUE;

    // Non-primitives
    static public final DapType STRUCTURE;
    static public final DapType SEQUENCE;

    static final protected DapDataset pseudoroot = new DapDataset("");

    /**
     * Define a map from the Atomic Type Sort to the
     * corresponding DapType primitive.
     */

    static final Map<TypeSort, DapType> typemap;

    static {
        typemap = new HashMap<TypeSort, DapType>();

        CHAR = new DapType(TypeSort.Char);
        INT8 = new DapType(TypeSort.Int8);
        UINT8 = new DapType(TypeSort.UInt8);
        INT16 = new DapType(TypeSort.Int16);
        UINT16 = new DapType(TypeSort.UInt16);
        INT32 = new DapType(TypeSort.Int32);
        UINT32 = new DapType(TypeSort.UInt32);
        INT64 = new DapType(TypeSort.Int64);
        UINT64 = new DapType(TypeSort.UInt64);
        FLOAT32 = new DapType(TypeSort.Float32);
        FLOAT64 = new DapType(TypeSort.Float64);
        STRING = new DapType(TypeSort.String);
        URL = new DapType(TypeSort.URL);
        OPAQUE = new DapType(TypeSort.Opaque);
        STRUCTURE = new DapType(TypeSort.Structure);
        SEQUENCE = new DapType(TypeSort.Sequence);

        typemap.put(TypeSort.Char, DapType.CHAR);
        typemap.put(TypeSort.Int8, DapType.INT8);
        typemap.put(TypeSort.UInt8, DapType.UINT8);
        typemap.put(TypeSort.Int16, DapType.INT16);
        typemap.put(TypeSort.UInt16, DapType.UINT16);
        typemap.put(TypeSort.Int32, DapType.INT32);
        typemap.put(TypeSort.UInt32, DapType.UINT32);
        typemap.put(TypeSort.Int64, DapType.INT64);
        typemap.put(TypeSort.UInt64, DapType.UINT64);
        typemap.put(TypeSort.Float32, DapType.FLOAT32);
        typemap.put(TypeSort.Float64, DapType.FLOAT64);
        typemap.put(TypeSort.String, DapType.STRING);
        typemap.put(TypeSort.URL, DapType.URL);
        typemap.put(TypeSort.Opaque, DapType.OPAQUE);

        typemap.put(TypeSort.Structure, DapType.STRUCTURE);
        typemap.put(TypeSort.Sequence, DapType.SEQUENCE);

	for(Map.Entry<TypeSort,DapType> entry: typemap.entrySet()) {
        entry.getValue().setDataset(pseudoroot);
        entry.getValue().setParent(pseudoroot);
	}

    }

    //////////////////////////////////////////////////
    // Static methods

    static public DapType lookup(TypeSort atomic)
    {
        if(atomic == TypeSort.Enum)
            return null;// we need more info
        return typemap.get(atomic);
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected TypeSort typesort = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    // Only used in static block
    protected DapType(TypeSort typesort)
    {
        this(typesort.name(),typesort);
    }

    public DapType(String name, TypeSort typesort)
    {
        super(name);
        if(sort == DapSort.ENUMERATION) {
            setTypeSort(TypeSort.Enum); // enum is (currently)
            // the only user-extendible atomic type
        } else
            setTypeSort(typesort);
    }

    //////////////////////////////////////////////////
    // Accessors

    /**
     * Return the lowest possible TypeSort.
     * This is the same as getTypeSort()
     * except for enums, where it returns the
     * basetype of the enum.
     *
     * @return lowest level atomic type
     */
    public TypeSort getAtomicType()
    {
        if(this.typesort == TypeSort.Enum) {
            return ((DapEnumeration) this).getBaseType().getTypeSort();
        } else
            return getTypeSort();
    }

    public TypeSort getTypeSort()
    {
        return this.typesort;
    }

    public String getTypeName()
    {
        return (typesort == TypeSort.Enum ? this.getFQN() : this.getShortName());
    }

    protected void setTypeSort(TypeSort typesort)
    {
        this.typesort = typesort;
    }

    public boolean isUnsigned()
    {
        if(typesort == TypeSort.Enum)
            return ((DapEnumeration) this).getBaseType().isUnsigned();
        else
            return typesort.isUnsigned();
    }

    public boolean isAtomic()
    {
        return getTypeSort().isAtomic();
    }

    // Pass thru to atomictype
    public boolean isIntegerType()
    {
        return typesort.isIntegerType();
    }

    public boolean isFloatType()
    {
        return typesort.isFloatType();
    }

    public boolean isNumericType()
    {
        return typesort.isNumericType();
    }

    public boolean isStringType()
    {
        return typesort.isStringType();
    }

    public boolean isEnumType()
    {
        return typesort.isEnumType();
    }

    public boolean isCharType()
    {
        return typesort.isCharType();
    }

    public boolean isOpaqueType()
    {
        return typesort.isOpaqueType();
    }

    public boolean isFixedSize()
    {
        return typesort.isFixedSize();
    }

    public boolean isStructType()
    {
        return typesort.isStructType();
    }

    public boolean isSeqType()
    {
        return typesort.isSeqType();
    }

    public boolean isCompoundType()
    {
        return typesort.isCompoundType();
    }


    public boolean isLegalAttrType()
    {
        return typesort.isLegalAttrType();
    }

    public boolean isCompound()
    {
        return typesort.isCompound();
    }

    public int getSize()
    {
        return TypeSort.getSize(getAtomicType());
    }

    // Reflective operations

    public Object vector(int n)
    {
        if(n < 0)
            throw new IllegalArgumentException();
        return Array.newInstance(this.getClass(), n);
    }

    public Object vectorget(Object vector, int n)
    {
        if(n < 0)
            throw new IllegalArgumentException();
        assert (vector.getClass().isArray()) : "Attempt to apply index to non-vector";
        return Array.get(vector, n);
    }

}
