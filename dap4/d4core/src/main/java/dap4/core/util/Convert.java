/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This code manages the conversion
 * between the DAP atomic type system
 * and the CDM atomic type system.
 * The Java type system is also
 * involved because neither DAP nor
 * CDM have perfect mappings to Java.
 * <p/>
 * In any case, this means that we have to
 * use switches that operate on DAP X CDM
 * atomic types and this is really ugly.
 * <p/>
 * * Special note about DAP4 char: this is treated
 * * as equivalent to Byte, so it does not appear here.
 * * Note also, this may turn out to be a really bad idea.
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
    //static final long UINT64_MAX = not representable;

    // Create big integer constants for the integer type max/min values

    /*
        static final BigInteger BIG_INT8_MAX = BigInteger.valueOf(Byte.MAX_VALUE);
        static final BigInteger BIG_INT8_MIN = BigInteger.valueOf(Byte.MIN_VALUE);
        static final BigInteger BIG_UINT8_MAX = BigInteger.valueOf(255);
        static final BigInteger BIG_UINT8_MIN = BigInteger.ZERO;
        static final BigInteger BIG_INT16_MIN = BigInteger.valueOf(Short.MIN_VALUE);
        static final BigInteger BIG_UINT16_MAX = new BigInteger("FFFF",16);
        static final BigInteger BIG_UINT16_MIN = BigInteger.ZERO;
        static final BigInteger BIG_INT32_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
        static final BigInteger BIG_UINT32_MAX = new BigInteger("FFFFFFFF",16);

        static final BigInteger BIG_UINT32_MAX = BigInteger.valueOf(((1L) << 32) - 1);
        static final BigInteger BIG_UINT32_MIN = BigInteger.ZERO;
    */
    static final BigInteger BIG_INT64_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    static final BigInteger BIG_INT64_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    static final BigInteger BIG_UINT64_MAX = new BigInteger("FFFFFFFFFFFFFFFF", 16);
    static final BigInteger BIG_UINT64_MIN = BigInteger.ZERO;

    //////////////////////////////////////////////////

    /**
     * Given a DapAttribute basetype
     * convert it to avoid losing information.
     * This primarily alters unsigned types
     *
     * @param basetype the basetype of the DapAttribute
     * @return the upcast DapType
     */
    static public DapType
    upcastType(DapType basetype)
    {
        switch (basetype.getAtomicType()) {
        case UInt8:
            return DapType.INT16;
        case UInt16:
            return DapType.INT32;
        case UInt32:
            return DapType.INT64;
        default:
            break;
        }
        return basetype;
    }

    /**
     * Given an value from a DapAttribute,
     * convert it to avoid losing information.
     * This primarily alters unsigned types
     *
     * @param o       the object to upcast
     * @param srctype the basetype of the DapAttribute
     * @return the upcast value
     */
    static public Object
    upcast(Object o, DapType srctype)
    {
        Object result = null;
        AtomicType otype = AtomicType.classToType(o);
        AtomicType srcatomtype = srctype.getAtomicType();
        if(otype == null)
            throw new ConversionException("Unexpected value type: " + o.getClass());
        switch (srcatomtype) {
        case UInt8:
            long lvalue = ((Byte) o).longValue();
            lvalue &= 0xFFL;
            result = new Short((short) lvalue);
            break;
        case UInt16:
            lvalue = ((Short) o).longValue();
            lvalue &= 0xFFFFL;
            result = new Integer((int) lvalue);
            break;
        case UInt32:
            lvalue = ((Integer) o).longValue();
            lvalue &= 0xFFFFFFFFL;
            result = new Long(lvalue);
            break;
        case Structure:
            throw new ConversionException("Cannot convert struct");// illegal
        default:
            result = o;
            break;
        }
        return result;
    }


    static public int getJavaSize(DapType daptype)
    {
        AtomicType atomictype = daptype.getPrimitiveType();
        return getJavaSize(atomictype);
    }

    /* Get the size of an equivalent java object; zero if not defined */
    static public int getJavaSize(AtomicType atomtype)
    {
        switch (atomtype) {
        case Char:
        case Int8:
        case UInt8:
            return 1;
        case Int16:
        case UInt16:
            return 2;
        case Int32:
        case UInt32:
            return 4;
        case Int64:
        case UInt64:
            return 8;
        case Float32:
            return 4;
        case Float64:
            return 8;
        default:
            break;
        }
        return 0;
    }

    /**
     * Peg a value to either the min or max
     * depending on sign.
     *
     * @param value the value to peg
     * @param min   peg to this if value is < min
     * @param max   peg to this if value is > max
     * @return pegg'ed value
     */
    static long
    minmax(long value, long min, long max)
    {
        if(value < min) return min;
        if(value > max) return max;
        return value;
    }

    /**
     * Convert Object to a value consistent with the given type.
     * This is the primary conversion method.
     *
     * @param dsttype Type to which the object is to be converted.
     * @param srctype Assumed type of the value; must be consistent
     *                in sense that if srctype == float32, then
     *                e.g value instanceof double must be true.
     * @param value   The object to convert
     * @return converted value as an object
     * @throws ConversionException if cannot convert
     */

    static public Object
    convert(DapType dsttype, DapType srctype, Object value)
    {
        Object result = null;

        if(dsttype == srctype)
            return value;

        AtomicType srcatomtype = srctype.getAtomicType();
        AtomicType dstatomtype = dsttype.getAtomicType();

        if(srcatomtype.isEnumType())
            return convert(dsttype, ((DapEnum) srctype).getBaseType(), value);
        assert (!srcatomtype.isEnumType());

        // presage
        long lvalue = 0;
        double dvalue = 0.0;
        if(srcatomtype.isNumericType())
            lvalue = Convert.longValue(srctype, value);
        else if(srcatomtype.isFloatType())
            dvalue = Convert.doubleValue(srctype, value);

        boolean fail = false;
        switch (dstatomtype) {
        case Char:
            if(!srcatomtype.isNumericType()) {
                fail = true;
                break;
            }
            lvalue &= 0xFFL;
            result = Character.valueOf((char) lvalue);
            break;
        case UInt8:
            if(!srcatomtype.isNumericType()) {
                fail = true;
                break;
            }
            lvalue &= 0xFFL;
            result = Byte.valueOf((byte) lvalue);
            break;
        case Int8:
            if(!srcatomtype.isNumericType()) {
                fail = true;
                break;
            }
            result = Byte.valueOf((byte) lvalue);
            break;
        case UInt16:
            if(!srcatomtype.isNumericType()) {
                fail = true;
                break;
            }
            lvalue &= 0xFFFFL;
            result = Short.valueOf((short) lvalue);
            break;
        case Int16:
            if(!srcatomtype.isNumericType()) {
                fail = true;
                break;
            }
            result = Short.valueOf((short) lvalue);
            break;
        case UInt32:
            if(!srcatomtype.isNumericType()) {
                fail = true;
                break;
            }
            lvalue &= 0xFFFFL;
            result = Integer.valueOf((int) lvalue);
            break;
        case Int32:
            if(!srcatomtype.isNumericType()) {
                fail = true;
                break;
            }
            result = Integer.valueOf((int) lvalue);
            break;
        case Int64:
        case UInt64:
            if(!srcatomtype.isNumericType()) {
                fail = true;
                break;
            }
            result = Long.valueOf(lvalue);
            break;

        case Float32:
            if(!srcatomtype.isNumericType()) {
                fail = true;
                break;
            }
            result = Float.valueOf((float) dvalue);
            break;
        case Float64:
            if(!srcatomtype.isNumericType()) {
                fail = true;
                break;
            }
            result = Double.valueOf(dvalue);
            break;

        case String:
        case URL:
            if(srcatomtype == AtomicType.Opaque) {
            } else
                result = value.toString();
            break;

        case Opaque:
            if(srcatomtype != AtomicType.Opaque) {
                fail = true;
                break;
            }
            result = value;
            break;

        case Enum:
            if(!srcatomtype.isIntegerType()) {
                fail = true;
                break;
            }
            // Check that the src value matches one of the dst enum consts
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            DapEnum en = (DapEnum) dsttype;
            if(en.lookup(lvalue) == null)
                throw new ConversionException("Enum constant failure");
            result = Long.valueOf(lvalue);
            break;
        default:
            fail = true;
            break;
        }
        if(fail) {
            throw new ConversionException(
                String.format("Cannot convert: %s -> %s", srcatomtype, dstatomtype));
        }
        return result;
    }

    /**
     * Special case of convertValue restricted to integer conversions
     * <p/>
     * Convert numeric value to a value consistent with the given type.
     *
     * @param srctype Assumed type of the value; must be a numeric type
     * @param value   The object to convert; it is an instance of
     *                Long or Double
     * @return converted value as a long.
     * but is guaranteed to be in the proper range for dsttype.
     * @throws ConversionException if cannot convert
     *                             (including out of range)
     */

    static public long
    longValue(DapType srctype, Object value)
    {
        AtomicType srcatomtype = srctype.getAtomicType();

        if(srcatomtype.isEnumType())
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return longValue(((DapEnum) srctype).getBaseType(), value);
        assert (!srctype.isEnumType());
        if(srcatomtype.isCharType())
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return ((long) ((Character) value).charValue()) & 0xFFL;
        else if(srcatomtype.isIntegerType())
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return ((Number) value).longValue();
        else if(srcatomtype == AtomicType.Float32)
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return ((Float) value).longValue();
        else if(srcatomtype == AtomicType.Float64)
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return ((Double) value).longValue();
        else
            throw new ConversionException(
                String.format("Cannot convert: %s -> long", srcatomtype));
    }

    /**
     * Special case of convertValue restricted to numeric conversions
     * <p/>
     * Convert numeric value to a double value
     *
     * @param srctype Assumed type of the value; must be a numeric type
     * @param value   The object to convert
     * @return converted value as a double
     * but is guaranteed to be in the proper range for dsttype.
     * @throws ConversionException if cannot convert
     *                             (including out of range)
     */

    static public double
    doubleValue(DapType srctype, Object value)
    {
        AtomicType srcatomtype = srctype.getAtomicType();

        if(srcatomtype.isEnumType())
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return doubleValue(((DapEnum) srctype).getBaseType(), value);
        assert (!srcatomtype.isEnumType());

        double dvalue = 0;
        if(srcatomtype == AtomicType.UInt64) {
            BigInteger bi = toBigInteger(((Long) value));
            BigDecimal bd = new BigDecimal(bi);
            dvalue = bd.doubleValue();
        } else if(srcatomtype.isIntegerType() || srcatomtype.isCharType()) {
            long lvalue = longValue(srctype, value);
            dvalue = (double) lvalue;
        } else if(srcatomtype == AtomicType.Float32) {
            float f = (Float) value;
            dvalue = (double) f;
        } else if(srcatomtype == AtomicType.Float64)
            dvalue = (Double) value;
        else
            throw new ConversionException(
                String.format("Cannot convert: %s -> double", srcatomtype));
        return dvalue;
    }

    /**
     * UInt64 long values are kept as longs,
     * but to properly process them,
     * it is necessary to convert them
     * to a correct form of BigInteger
     *
     * @param l The long value
     * @return A proper non-negative BigInteger
     */
    static public BigInteger toBigInteger(long l)
    {
        BigInteger bi = BigInteger.valueOf(l);
        bi = bi.and(DapUtil.BIG_UMASK64);
        return bi;
    }


    /**
     * Force a numeric value to be in a specified range
     * Only defined for simple integers (ValueClass LONG)
     * WARNING: unsigned values are forced into the
     * signed size, but the proper bit pattern is maintained.
     * The term "force" means that if the value is outside the typed
     * min/max values, it is pegged to the min or max value depending
     * on the sign. Note that truncation is not used.
     *
     * @param basetype the type to force value to in range
     * @param value    the value to force
     * @return forced value
     * @throws ConversionException if forcing is not possible
     */
    static public long forceRange(AtomicType basetype, long value)
    {
        assert basetype.isIntegerType() : "Internal error";
        switch (basetype) {
        case Char:
            value = minmax(value, 0, 255);
            break;
        case Int8:
            value = minmax(value, (long) Byte.MIN_VALUE, (long) Byte.MAX_VALUE);
            break;
        case UInt8:
            value = value & 0xFFL;
            break;
        case Int16:
            value = minmax(value, (long) Short.MIN_VALUE, (long) Short.MAX_VALUE);
            break;
        case UInt16:
            value = value & 0xFFFFL;
            break;
        case Int32:
            value = minmax(value, (long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE);
            break;
        case UInt32:
            value = value & 0xFFFFFFFFL;
            break;
        case Int64:
        case UInt64:
            break; // value = value case Int64:
        default:
        }
        return value;
    }

    /**
     * Force a double value into either float or double range
     *
     * @param basetype the type to force value to in range
     * @param value    the value to force
     * @return forced value
     * @throws DapException if forcing is not possible
     */
    static public double
    forceRange(AtomicType basetype, double value)
        throws DapException
    {
        assert basetype.isFloatType() : "Internal error";
        if(basetype == AtomicType.Float32) {
            float fvalue = (float) value;
            value = (double) fvalue;
        }
        return value;
    }

    /**
     * Convert string to a value consistent with the base type.
     * Primarily used for attribute value conversion.
     *
     * @param value   the string to convert
     * @param dsttype target type for the conversion
     * @return converted value as an object
     * @throws ConversionException if cannot convert
     */

    static public Object
    fromString(String value, DapType dsttype)
    {
        if(value == null) return value;
        assert (dsttype != null);
        AtomicType atomtype = dsttype.getAtomicType();
        long lvalue = 0;
        if(atomtype.isIntegerType() || atomtype.isCharType()) {
            BigInteger bi = BigInteger.ZERO;
            try {
                bi = new BigInteger(value);
                lvalue = bi.longValue(); // should get the bits right
            } catch (NumberFormatException nfe) {
                throw new ConversionException("Expected integer value: " + value);
            }
            // Force into range
            lvalue = forceRange(atomtype, lvalue);
            switch (atomtype) {
            case UInt8:
            case Int8:
                return Byte.valueOf((byte) lvalue);
            case UInt16:
            case Int16:
                return Short.valueOf((short) lvalue);
            case UInt64:
            case Int64:
                return Long.valueOf(lvalue);
            case Char:
                return Character.valueOf((char) lvalue);
            default:
                assert false;
            }
        } else if(atomtype.isFloatType()) {
            double d = 0;
            try {
                d = Double.parseDouble(value);
            } catch (NumberFormatException nfe) {
                throw new ConversionException("Expected float value: " + value);
            }
            switch (atomtype) {
            case Float32:
                return new Float(d);
            case Float64:
                return new Double(d);
            default:
                assert false;
            }
        } else if(atomtype.isStringType()) {
            if(atomtype == AtomicType.URL) {
                // See if this parses as a URL/URI
                value = value.trim();
                try {
                    new URL((String) value);   // purely want side effect of syntax checking
                } catch (MalformedURLException mue) {
                    throw new ConversionException("Illegal URL value: " + value);
                }
            }
            return value.toString();

        } else if(atomtype.isOpaqueType()) {
            String opaque = ((String) value).trim();
            if(!opaque.startsWith("0x") && !opaque.startsWith("0X"))
                throw new ConversionException("Illegal opaque value: " + value);
            opaque = opaque.substring(2, opaque.length());
            int len = opaque.length();
            assert (len >= 0);
            if(len % 2 == 1) {
                len++;
                opaque = opaque + '0';
            }
            byte[] b = new byte[len];
            int index = 0;
            for(int i = 0;i < len;i += 2) {
                int digit1 = Escape.fromHex(opaque.charAt(i));
                int digit2 = Escape.fromHex(opaque.charAt(i + 1));
                if(digit1 < 0 || digit2 < 0)
                    throw new ConversionException("Illegal attribute value: " + value);
                b[index++] = (byte) (digit1 << 4 | digit2);
            }
            return b;
        } else if(atomtype.isEnumType()) {
            // Note that for this, we use the string as
            // an enum constant name to look for.
            String name = value.trim();
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            Long Lvalue = ((DapEnum) dsttype).lookup(name);
            if(Lvalue == null)
                throw new ConversionException("Illegal enum constant value: " + name);
            return Lvalue;
        }
        throw new ConversionException("Internal error");
    }


    /**
     * Convert a value to a string constant
     *
     * @param value   The object to convert
     * @param srctype The assumed type of the value
     * @return value in string form
     */

    static public String
    toString(Object value, DapType srctype)
    {
        StringBuilder buf = new StringBuilder();
        AtomicType srcatomtype;
        boolean charornum;
        boolean fail = false;
        long lvalue = 0;

        assert (srctype != null) : "Internal error";

        srcatomtype = srctype.getAtomicType();

        // Do some preliminary conversions
        charornum = true;
        if(srcatomtype.isCharType())
            lvalue = ((Character) value).charValue();
        else if(srcatomtype.isNumericType())
            lvalue = ((Number) value).longValue();
        else if(srcatomtype == AtomicType.UInt64)
            lvalue = ((BigInteger) value).longValue();
        else
            charornum = false;

        switch (srcatomtype) {
        case Char:
        case UInt8:
            if(!charornum) {
                fail = true;
                break;
            }
            lvalue &= 0xFFL;
            buf.append("'" + ((char) lvalue) + "'");
            break;
        case Int8:
            if(!charornum) {
                fail = true;
                break;
            }
            buf.append(Long.toString(lvalue));
            break;
        case Int16:
            if(!charornum) {
                fail = true;
                break;
            }
            buf.append(Short.toString((short) lvalue));
            break;
        case UInt16:
            if(!charornum) {
                fail = true;
                break;
            }
            buf.append(Long.toString((lvalue & 0xFFFFL)));
            break;
        case Int32:
            if(!charornum) {
                fail = true;
                break;
            }
            buf.append(Integer.toString((int) lvalue));
            break;
        case UInt32:
            if(!charornum) {
                fail = true;
                break;
            }
            buf.append(Long.toString((lvalue & 0xFFFFFFFFL)));
            break;
        case Int64:
            if(!charornum) {
                fail = true;
                break;
            }
            buf.append(Long.toString(lvalue));
            break;
        case UInt64:
            if(!charornum) {
                fail = true;
                break;
            }
            BigInteger bi = toBigInteger(lvalue);
            bi = bi.and(DapUtil.BIG_UMASK64);
            buf.append(bi.toString());
            break;
        case Enum:
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            DapEnum en = (DapEnum) srctype;
            String name = en.lookup(lvalue);
            if(name == null) name = "?";
            buf.append(en.getFQN() + "." + name);
            break;
        default:
            break;
        }
        if(fail)
            throw new ConversionException();
        return buf.toString();
    }
}
    
