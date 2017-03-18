/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.DapEnumConst;
import dap4.core.dmr.DapEnumeration;
import dap4.core.dmr.DapType;
import dap4.core.dmr.TypeSort;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * This code manages value conversions.
 * The src and target types are specified
 * using DapType. The values themselves
 * are represented as Java typed values.
 * The mapping between DAPType
 * and the Java atomic type system
 * is imperfect because Java does not
 * have unsigned types, so we need to convert
 * to signed value that has the same bits as
 * the unsigned value would have.
 * <p>
 * Additionally: since we need to pass arbitrary values of primitive types,
 * we pass an Object which is assumed to be a vector of the primitive type.
 * So we pass int[] instead of int or Integer.
 * This is because the primitive type values are not a subclass of Object.
 * We could use the wrapper types (e.g. Integer) but using a vector
 * is more closely aligned with the way CDM does it.
 */

public abstract class Convert
{
    //////////////////////////////////////////////////
    // Constants

    // Create integer constants for (some) integer types
    static final long INT8_MAX = Byte.MAX_VALUE;
    static final long INT8_MIN = Byte.MIN_VALUE;
    static final long UINT8_MAX = 255;
    static final long UINT8_MIN = 0;
    static final long INT16_MAX = Short.MAX_VALUE;
    static final long INT16_MIN = Short.MIN_VALUE;
    static final long UINT16_MAX = ((1L << 16) - 1);
    static final long UINT16_MIN = 0;
    static final long INT32_MAX = Integer.MAX_VALUE;
    static final long INT32_MIN = Integer.MIN_VALUE;
    static final long UINT32_MAX = ((1L << 32) - 1);
    static final long UINT32_MIN = 0;
    static final long INT64_MAX = Long.MAX_VALUE;
    static final long INT64_MIN = Long.MIN_VALUE;
    static final long UINT64_MIN = 0;
    // static final long UINT64_MAX = Long.MIN_VALUE; // same bit pattern; not used

    // Create big integer constants for the long integer type max/min values
    static final BigInteger BIG_INT64_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    static final BigInteger BIG_INT64_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    static final BigInteger BIG_UINT64_MAX = new BigInteger("FFFFFFFFFFFFFFFF", 16);
    static final BigInteger BIG_UINT64_MIN = BigInteger.ZERO;

    //////////////////////////////////////////////////

    /**
     * Convert Object to a value consistent with the given type.
     * This is the primary conversion method.
     *
     * @param dsttype Type to which the object is to be converted.
     * @param srctype Assumed type of the value; must be consistent
     *                in sense that if srctype == float32, then
     *                e.g value instanceof float[] must be true.
     * @param values  The vector of values to convert
     * @return converted values as a vector
     * @throws ConversionException if cannot convert (runtime exception)
     */

    static public Object
    convert(DapType dsttype, DapType srctype, Object values)
    {
        if(dsttype == srctype)
            return values;

        TypeSort srcsort = srctype.getTypeSort();
        TypeSort dstsort = dsttype.getTypeSort();

        // Special actions for enumeration cases
        if(srcsort.isEnumType() && dstsort.isEnumType())
            throw new ConversionException("Cannot convert enum to enum");

        // We will move the values to a common super type
        // and then down to the dsttype

        Object result = null;
        int count = java.lang.reflect.Array.getLength(values);

        // First do some special cases
        if(srcsort == TypeSort.Char && dstsort.isStringType()) {
            String[] svals = new String[count];
            result = svals;
            for(int i = 0; i < count; i++) {
                svals[i] = Character.toString(((char[]) values)[i]);
            }
            return result;
        } else if(srcsort == TypeSort.Char && !dstsort.isIntegerType()) {
            throw new ConversionException("Cannot convert Char to " + dstsort.toString());
        } else if(srcsort == TypeSort.Opaque && dstsort != TypeSort.Opaque) {
            throw new ConversionException("Cannot only convert Opaque to Opaque");
        }

        // All other conversion cases
        long[] lvalues = null;
        double[] dvalues = null;
        String[] svalues = null;
        ByteBuffer[] opvalues = null;

        // For many cases, we can convert to an intermediate type
        // if we know that the dstsort is in some class e.g. integertype
        switch (srcsort) {
        case Char:
            if(dstsort.isIntegerType()) {
                lvalues = new long[count];
                for(int i = 0; i < count; i++) {
                    lvalues[i] = (long) ((char[]) values)[i];
                }
            } else if(dstsort.isStringType()) {
                svalues = new String[count];
                for(int i = 0; i < count; i++) {
                    svalues[i] = Character.toString(((char[]) values)[i]);
                }
            }
            break;
        case Int8:
            lvalues = new long[count];
            for(int i = 0; i < count; i++) {
                lvalues[i] = (long) ((byte[]) values)[i];
            }
            break;
        case UInt8:
            lvalues = new long[count];
            for(int i = 0; i < count; i++) {
                lvalues[i] = ((long) ((byte[]) values)[i]) & 0xFFL;
            }
            break;
        case Int16:
            lvalues = new long[count];
            for(int i = 0; i < count; i++) {
                lvalues[i] = (long) ((short[]) values)[i];
            }
            break;
        case UInt16:
            lvalues = new long[count];
            for(int i = 0; i < count; i++) {
                lvalues[i] = ((long) ((short[]) values)[i]) & 0xFFFFL;
            }
            break;
        case Int32:
            lvalues = new long[count];
            for(int i = 0; i < count; i++) {
                lvalues[i] = (long) ((int[]) values)[i];
            }
            break;
        case UInt32:
            lvalues = new long[count];
            for(int i = 0; i < count; i++) {
                lvalues[i] = ((long) ((int[]) values)[i]) & 0xFFFFFFFFL;
            }
            break;
        case Int64:
            lvalues = new long[count];
            for(int i = 0; i < count; i++) {
                lvalues[i] = ((long[]) values)[i];
            }
            break;
        case UInt64: // Have to go thru BigInteger
            lvalues = new long[count];
            for(int i = 0; i < count; i++) {
                long[] lv = (long[]) values;
                BigInteger bi = BigInteger.valueOf(lv[i]);
                bi = bi.and(BIG_UINT64_MAX);
                lvalues[i] = bi.longValue();
            }
            break;
        case Float32:
            dvalues = new double[count];
            for(int i = 0; i < count; i++) {
                dvalues[i] = (double) ((float[]) values)[i];
            }
            break;
        case Float64:
            dvalues = new double[count];
            for(int i = 0; i < count; i++) {
                dvalues[i] = ((double[]) values)[i];
            }
            break;
        case String:
        case URL:
            svalues = (String[]) values;
            break;
        case Opaque:
            opvalues = (ByteBuffer[]) values;
            break;
        case Enum:
            // The incoming set of values might be a vector of some kind of integer
            // or a vector of strings. In the latter case, the string might represent
            // an integer or represent a enum const name.
            DapEnumeration srcenum = (DapEnumeration) srctype;
            DapType enumbase = srcenum.getBaseType();
            lvalues = new long[count]; // for all cases, this is what we want
            DapType vectype =  vectorType(values);
            if(vectype.isIntegerType()) {
               lvalues = (long[])convert(DapType.INT64,vectype, values);
            } else if(vectype.isStringType()) {
                for(int i = 0; i < count; i++) {
                    String sval = ((String[]) values)[i];
                    try {// see if this is an integer
                        lvalues[i] = Long.parseLong(sval);
                        // See if this is a legal value for the enum
                        if(srcenum.lookup(lvalues[i]) == null)
                            throw new ConversionException("Illegal Enum constant: " + sval);
                    } catch (NumberFormatException nfe) {// not an integer
                        DapEnumConst dec = srcenum.lookup(sval);
                        if(dec == null)
                            throw new ConversionException("Illegal Enum constant: " + sval);
                        lvalues[i] = dec.getValue();
                    }
                }
            } else
                throw new ConversionException(String.format("Cannot convert values of type %s to enum",vectype));
            break;
        default:
            throw new ConversionException("Illegal srctype: " + srctype);
        }

        // Now convert to the dsttype
        // First, convert from the intermediate type

        switch (dstsort) {
        case Char: // Warning: remember that our chars are 8 bit => ascii
            char[] cresult = new char[count];
            result = cresult;
            if(svalues != null) {
                for(int i = 0; i < count; i++) {
                    if(svalues[i].length() != 1)
                        throw new ConversionException("Cannot convert multi-char string to a char");
                    cresult[i] = svalues[i].charAt(0);
                }
            } else {
                if(lvalues == null && dvalues != null)
                    lvalues = double2long(dvalues);
                for(int i = 0; i < count; i++) {
                    cresult[i] = (char) (lvalues[i] & 0x7FL);
                }
            }
            break;
        case Int8:
        case UInt8:
            if(svalues != null) lvalues = string2long(svalues);
            else if(dvalues != null) lvalues = double2long(dvalues);
            byte[] bresult = new byte[count];
            result = bresult;
            for(int i = 0; i < count; i++) {
                bresult[i] = (byte) lvalues[i];
            }
            break;
        case Int16:
        case UInt16:
            if(svalues != null) lvalues = string2long(svalues);
            else if(dvalues != null) lvalues = double2long(dvalues);
            short[] shresult = new short[count];
            result = shresult;
            for(int i = 0; i < count; i++) {
                shresult[i] = (short) lvalues[i];
            }
            break;
        case Int32:
        case UInt32:
            if(svalues != null) lvalues = string2long(svalues);
            else if(dvalues != null) lvalues = double2long(dvalues);
            int[] iresult = new int[count];
            result = iresult;
            for(int i = 0; i < count; i++) {
                iresult[i] = (int) lvalues[i];
            }
            break;
        case Int64:
        case UInt64:
            if(svalues != null) lvalues = string2long(svalues);
            else if(dvalues != null) lvalues = double2long(dvalues);
            result = lvalues;
            break;
        case Float32:
            if(svalues != null) dvalues = string2double(svalues);
            else if(lvalues != null) dvalues = long2double(lvalues);
            float[] fresult = new float[count];
            for(int i = 0; i < count; i++) {
                fresult[i] = (float) dvalues[i];
            }
            break;
        case Float64:
            if(svalues != null) dvalues = string2double(svalues);
            else if(lvalues != null) dvalues = long2double(lvalues);
            result = dvalues;
            break;
        case String:
        case URL:
            if(svalues != null) {
                result = svalues;
            } else {
                String[] stresult = new String[count];
                result = stresult;
                if(lvalues != null) {// long -> string
                    for(int i = 0; i < count; i++) {
                        stresult[i] = Long.toString(lvalues[i]);
                    }
                } else {// double -> string
                    for(int i = 0; i < count; i++) {
                        stresult[i] = Double.toString(dvalues[i]);
                    }
                }
            }
            break;
        case Enum:
            // If dst is an enumeration, then convert to a vector of strings representing
            // enum const names.
            if(opvalues != null)
                throw new ConversionException("Cannot convert opaque to enum");
            if(svalues == null && lvalues == null && dvalues != null)
                lvalues = double2long(dvalues);DapEnumeration dstenum = (DapEnumeration) dsttype;
            if(svalues != null) {
                svalues = (String[])dstenum.convert(svalues); // treat strings as econst names or ints
            } else if(lvalues != null) {
                // Lookup each lvalue and get corresponding econst name
                svalues = new String[count];
                for(int i=0;i<count;i++) {
                    DapEnumConst ec = dstenum.lookup(lvalues[i]);
                    if(ec == null)
                        throw new ConversionException("Illegal Enum Const: " + lvalues[i]);
                    svalues[i] = ec.getShortName();
                }
            }
            result = svalues;
            break;
        default:
            throw new ConversionException("Illegal dsttype: " + dsttype);
        }
        return result;
    }

    static protected long[]
    double2long(double[] in)
    {
        long[] out = new long[in.length];
        for(int i = 0; i < in.length; i++) {
            out[i] = (long) in[i];
        }
        return out;
    }

    static protected double[]
    long2double(long[] in)
    {
        double[] out = new double[in.length];
        for(int i = 0; i < in.length; i++) {
            out[i] = (double) in[i];
        }
        return out;
    }

    static protected long[]
    string2long(String[] in)
    {
        long[] out = new long[in.length];
        for(int i = 0; i < in.length; i++) {
            try {
                out[i] = Long.parseLong(in[i]);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("string->long: " + in[i]);
            }
        }
        return out;
    }

    static protected double[]
    string2double(String[] in)
    {
        double[] out = new double[in.length];
        for(int i = 0; i < in.length; i++) {
            try {
                out[i] = Double.parseDouble(in[i]);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("string->double: " + in[i]);
            }
        }
        return out;
    }

    static public DapType
    vectorType(Object o)
    {
        Class c = o.getClass();
        if(!c.isArray()) return null;
        if(o instanceof byte[]) return DapType.INT8;
        if(o instanceof short[]) return DapType.INT16;
        if(o instanceof int[]) return DapType.INT32;
        if(o instanceof String[]) return DapType.STRING;
        if(o instanceof char[]) return DapType.CHAR;
        if(o instanceof float[]) return DapType.FLOAT32;
        if(o instanceof double[]) return DapType.FLOAT64;
        if(o instanceof ByteBuffer[]) return DapType.OPAQUE;
        return null;
    }
}
    
