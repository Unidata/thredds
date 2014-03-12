/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static dap4.core.dmr.AtomicTypeConstants.*;

/* This private constants class
   is necessary because enum classes
   will not allow anything to be defined
   before the enum constants.
*/
class AtomicTypeConstants
{
    // define classification flag mnemonics
    static public final int ISINTEGER = 1;
    static public final int ISFLOAT = 2;
    static public final int ISSTRING = 4;
    static public final int ISCHAR = 8;
    static public final int ISENUM = 16;
    static public final int ISOPAQUE = 32;
    static public final int ISUNSIGNED = 64;
    static public final int ISFIXEDSIZE = 128;
    static public final int ISSTRUCT = 256;
    static public final int ISSEQ = 512;
    static public final int ISCOMPOUND = 1024;
    static public final int ISLEGALATTRTYPE = 2048;
}


public enum AtomicType
{
    Char("Char", ISFIXEDSIZE | ISCHAR | ISLEGALATTRTYPE),
    Int8("Int8", ISINTEGER | ISFIXEDSIZE | ISLEGALATTRTYPE),
    UInt8("UInt8", ISINTEGER | ISUNSIGNED | ISFIXEDSIZE | ISLEGALATTRTYPE),
    Int16("Int16", ISINTEGER | ISFIXEDSIZE | ISLEGALATTRTYPE),
    UInt16("UInt16", ISINTEGER | ISUNSIGNED | ISFIXEDSIZE | ISLEGALATTRTYPE),
    Int32("Int32", ISINTEGER | ISFIXEDSIZE | ISLEGALATTRTYPE),
    UInt32("UInt32", ISINTEGER | ISUNSIGNED | ISFIXEDSIZE | ISLEGALATTRTYPE),
    Int64("Int64", ISINTEGER | ISFIXEDSIZE | ISLEGALATTRTYPE),
    UInt64("UInt64", ISINTEGER | ISUNSIGNED | ISFIXEDSIZE | ISLEGALATTRTYPE),
    Float32("Float32", ISFLOAT | ISFIXEDSIZE | ISLEGALATTRTYPE),
    Float64("Float64", ISFLOAT | ISFIXEDSIZE | ISLEGALATTRTYPE),
    String("String", ISSTRING | ISLEGALATTRTYPE),
    URL("URL", ISSTRING | ISLEGALATTRTYPE),
    Opaque("Opaque", ISOPAQUE | ISLEGALATTRTYPE),
    Enum("Enum", ISENUM | ISFIXEDSIZE),
    Structure("Structure", ISSTRUCT|ISCOMPOUND), // Add this to avoid having to check the DapSort
    Sequence("Sequence", ISSEQ|ISCOMPOUND); // Add this to avoid having to check the DapSort

    private final String typename;
    private final int classification;

    AtomicType(String typename, int classification)
    {
        this.typename = typename;
        this.classification = classification;
    }

    public final String getTypeName()
    {
        return typename;
    }

    public final int getClassification()
    {
        return classification;
    }

    static public final AtomicType getSignedVersion(AtomicType uat)
    {
        switch (uat) {
        case Int8:
        case UInt8:
            return Int8;
        case Int16:
        case UInt16:
            return Int16;
        case Int32:
        case UInt32:
            return Int32;
        case Int64:
        case UInt64:
            return Int64;
        default:
            break;
        }
        return null;
    }

    static public final int getSize(AtomicType uat)
    {
        switch (uat) {
        case Char:
        case Int8:
        case UInt8:
            return 1;
        case Int16:
        case UInt16:
            return 2;
        case Int32:
        case UInt32:
        case Float32:
            return 4;
        case Int64:
        case UInt64:
        case Float64:
            return 8;
        default:
            break;
        }
        return 0;
    }

    // Map String -> AtomicType
    static public AtomicType getAtomicType(String typename)
    {
        for(AtomicType dt : AtomicType.values()) {
            if(typename.equalsIgnoreCase(dt.getTypeName()))
                return dt;
        }
        return null;
    }

    // Specific classifier tests
    public boolean isIntegerType()
    {
        return (classification & ISINTEGER) != 0;
    }

    public boolean isFloatType()
    {
        return (classification & ISFLOAT) != 0;
    }

    public boolean isStringType()
    {
        return (classification & ISSTRING) != 0;
    }

    public boolean isCharType()
    {
        return (classification & ISCHAR) != 0;
    }

    public boolean isEnumType()
    {
        return (classification & ISENUM) != 0;
    }

    public boolean isOpaqueType()
    {
        return (classification & ISOPAQUE) != 0;
    }

    public boolean isStructType()
    {
        return (classification & ISSTRUCT) != 0 || (classification & ISSEQ) != 0;
    }

    public boolean isNumericType()
    {
        return (classification & (ISINTEGER | ISFLOAT)) != 0;
    }

    public boolean isUnsigned()
    {
        return (classification & ISUNSIGNED) != 0;
    }

    public boolean isFixedSize()
    {
        return (classification & ISFIXEDSIZE) != 0;
    }

    public boolean isLegalAttrType()
    {
        return (classification & ISLEGALATTRTYPE) != 0;
    }

    public boolean isCompound()
    {
        return (classification & ISCOMPOUND) != 0;
    }


    // Map an object instance class to a corresponding AtomicType
    // Try to cover as many Java classes as possible.
    // Note that signedness is ignored

    static Map<Class, AtomicType> classmap;

    static {
        classmap = new HashMap<Class, AtomicType>();
        classmap.put(Character.class, AtomicType.Char);
        classmap.put(Byte.class, AtomicType.Int8);
        classmap.put(Short.class, AtomicType.Int16);
        classmap.put(Integer.class, AtomicType.Int32);
        classmap.put(Long.class, AtomicType.Int64);
        classmap.put(Float.class, AtomicType.Float32);
        classmap.put(Double.class, AtomicType.Float64);
        classmap.put(String.class, AtomicType.String);
        classmap.put(URI.class, AtomicType.URL);
        classmap.put(URL.class, AtomicType.URL);
        classmap.put(ByteBuffer.class, AtomicType.Opaque);
        classmap.put(byte[].class, AtomicType.Opaque);
        classmap.put(BigInteger.class, AtomicType.UInt64);
        classmap.put(DapEnum.class, AtomicType.Enum);
    }

    ;

    // Convert the class of an object to a matching AtomicType
    static public AtomicType classToType(Object o)
    {
        if(o == null) return null;
        return classmap.get(o.getClass());
    }

}


