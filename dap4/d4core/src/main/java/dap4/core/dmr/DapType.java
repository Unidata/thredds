/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapSort;

import java.util.*;

/**
 * This class reifies all of the atomic types
 * and specifically all enumeration declarations
 * as specific objects denoting a type.
 * Structure are specifically excluded
 */

public class DapType extends DapNode implements DapDecl
{

    /**
     * Define instances of DapType for every AtomicType.
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
    static public final DapType ENUM;
    static public final DapType STRUCT; // Add this to avoid having to check the DapSort
    static public final DapType SEQ; // Add this to avoid having to check the DapSort

    /**
     * Define a map from the Atomic Type Sort to the
     * corresponding DapType primitive.
     */

    static final Map<AtomicType, DapType> typemap;

    /**
     * Define a list of defined DapEnums
     */
    static List<DapEnum> enumlist;

    static {

        enumlist = new ArrayList<DapEnum>();
        typemap = new HashMap<AtomicType, DapType>();

        CHAR = new DapType(AtomicType.Char);
        INT8 = new DapType(AtomicType.Int8);
        UINT8 = new DapType(AtomicType.UInt8);
        INT16 = new DapType(AtomicType.Int16);
        UINT16 = new DapType(AtomicType.UInt16);
        INT32 = new DapType(AtomicType.Int32);
        UINT32 = new DapType(AtomicType.UInt32);
        INT64 = new DapType(AtomicType.Int64);
        UINT64 = new DapType(AtomicType.UInt64);
        FLOAT32 = new DapType(AtomicType.Float32);
        FLOAT64 = new DapType(AtomicType.Float64);
        STRING = new DapType(AtomicType.String);
        URL = new DapType(AtomicType.URL);
        OPAQUE = new DapType(AtomicType.Opaque);
        ENUM = new DapType(AtomicType.Enum);
        STRUCT = new DapType(AtomicType.Structure);
        SEQ = new DapType(AtomicType.Sequence);

        typemap.put(AtomicType.Char, DapType.CHAR);
        typemap.put(AtomicType.Int8, DapType.INT8);
        typemap.put(AtomicType.UInt8, DapType.UINT8);
        typemap.put(AtomicType.Int16, DapType.INT16);
        typemap.put(AtomicType.UInt16, DapType.UINT16);
        typemap.put(AtomicType.Int32, DapType.INT32);
        typemap.put(AtomicType.UInt32, DapType.UINT32);
        typemap.put(AtomicType.Int64, DapType.INT64);
        typemap.put(AtomicType.UInt64, DapType.UINT64);
        typemap.put(AtomicType.Float32, DapType.FLOAT32);
        typemap.put(AtomicType.Float64, DapType.FLOAT64);
        typemap.put(AtomicType.String, DapType.STRING);
        typemap.put(AtomicType.URL, DapType.URL);
        typemap.put(AtomicType.Opaque, DapType.OPAQUE);
        typemap.put(AtomicType.Enum, DapType.ENUM);
        typemap.put(AtomicType.Structure, DapType.STRUCT);
        typemap.put(AtomicType.Sequence, DapType.SEQ);
    }

    //////////////////////////////////////////////////
    // Static methods

    static public DapType lookup(AtomicType atomic)
    {
        if(atomic == AtomicType.Enum)
            return null;// we need more info
        return typemap.get(atomic);
    }

    static public DapType reify(String typename)
    {
        // See if this is an enum type
        for(DapEnum de : enumlist) {
            if(typename.equals(de.getFQN()))
                return de;
        }
        // Assume it is a non-enum atomic type
        return typemap.get(AtomicType.getAtomicType(typename));
    }

    static public Map<AtomicType, DapType> getTypeMap()
    {
        return typemap;
    }

    static public List<DapEnum> getEnumList()
    {
        return enumlist;
    }

    static void addEnum(DapEnum dapenum)
    {
        if(!enumlist.contains(dapenum))
            enumlist.add(dapenum);
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected AtomicType typesort = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    // Only used in static block
    protected DapType(AtomicType typesort)
    {
        this(typesort.name());
        setAtomicType(typesort);
    }

    public DapType(String name)
    {
        super(name);
        if(sort == DapSort.ENUMERATION) {
            setAtomicType(AtomicType.Enum); // enum is (currently)
            // the only user-extendible
            // atomic type
            addEnum((DapEnum) this);
        }
    }

    //////////////////////////////////////////////////
    // Accessors

    /**
     * Return the lowest possible AtomicType.
     * This is the same as getAtomicType()
     * except for enums, where it returns the
     * basetype of the enum.
     *
     * @return  lowest level atomic type
     */
    public AtomicType getPrimitiveType()
    {
        if(this.typesort == AtomicType.Enum) {
            return ((DapEnum)this).getBaseType().getAtomicType();
        } else
            return getAtomicType();
    }

    public AtomicType getAtomicType()
    {
        return this.typesort;
    }

    public String getTypeName()
    {
        return (typesort == AtomicType.Enum ? this.getFQN() : this.getShortName());
    }

    protected void setAtomicType(AtomicType typesort)
    {
        this.typesort = typesort;
    }

    public boolean isUnsigned()
    {
        if(typesort == AtomicType.Enum)
            return ((DapEnum) this).getBaseType().isUnsigned();
        else
            return typesort.isUnsigned();
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
        return AtomicType.getSize(getPrimitiveType());
    }
}
