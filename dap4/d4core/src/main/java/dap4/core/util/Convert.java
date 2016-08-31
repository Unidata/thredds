/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.DapEnumConst;
import dap4.core.dmr.DapEnumeration;
import dap4.core.dmr.DapType;
import dap4.core.dmr.TypeSort;

import java.lang.reflect.Array;
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
 * <p>
 * In any case, this means that we have to
 * use switches that operate on DAP X CDM
 * atomic types and this is really ugly.
 * <p>
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

        TypeSort srcatomtype = srctype.getTypeSort();
        TypeSort dstatomtype = dsttype.getTypeSort();

        if(srcatomtype.isEnumType())
            return convert(dsttype, ((DapEnumeration) srctype).getBaseType(), value);
        assert (!srcatomtype.isEnumType());

        // presage

        result = CoreTypeFcns.cvt(dsttype, value);
        if(result == null) {
            throw new ConversionException(
                    String.format("Cannot convert: %s -> %s", srcatomtype, dstatomtype));
        }
        return result;
    }

    /**
     * Special case of convertValue restricted to integer conversions
     * <p>
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
        TypeSort srcatomtype = srctype.getTypeSort();

        if(srcatomtype.isEnumType())
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return longValue(((DapEnumeration) srctype).getBaseType(), value);
        assert (!srctype.isEnumType());
        if(srcatomtype.isCharType())
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return ((long) ((Character) value).charValue()) & 0xFFL;
        else if(srcatomtype.isIntegerType())
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return ((Number) value).longValue();
        else if(srcatomtype == TypeSort.Float32)
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return ((Float) value).longValue();
        else if(srcatomtype == TypeSort.Float64)
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return ((Double) value).longValue();
        else
            throw new ConversionException(
                    String.format("Cannot convert: %s -> long", srcatomtype));
    }

    /**
     * Special case of convertValue restricted to numeric conversions
     * <p>
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
        if(value.getClass().isArray()) {
            assert Array.getLength(value) == 1;
            return doubleValue(srctype, Array.get(value, 0));
        }

        TypeSort srcatomtype = srctype.getTypeSort();

        if(srcatomtype.isEnumType())
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return doubleValue(((DapEnumeration) srctype).getBaseType(), value);
        assert (!srcatomtype.isEnumType());

        double dvalue = 0;
        if(srcatomtype == TypeSort.UInt64) {
            BigInteger bi = toBigInteger(((Long) value));
            BigDecimal bd = new BigDecimal(bi);
            dvalue = bd.doubleValue();
        } else if(srcatomtype.isIntegerType() || srcatomtype.isCharType()) {
            long lvalue = longValue(srctype, value);
            dvalue = (double) lvalue;
        } else if(srcatomtype == TypeSort.Float32) {
            float f = (Float) value;
            dvalue = (double) f;
        } else if(srcatomtype == TypeSort.Float64)
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
     * Force a double value into either float or double range
     *
     * @param basetype the type to force value to in range
     * @param value    the value to force
     * @return forced value
     * @throws DapException if forcing is not possible
     */
    static public double
    forceRange(TypeSort basetype, double value)
            throws DapException
    {
        assert basetype.isFloatType() : "Internal error";
        if(basetype == TypeSort.Float32) {
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
        TypeSort atomtype = dsttype.getTypeSort();
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
            lvalue = CoreTypeFcns.forceRange(atomtype, lvalue);
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
            if(atomtype == TypeSort.URL) {
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
            for(int i = 0; i < len; i += 2) {
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
            DapEnumConst econst = ((DapEnumeration) dsttype).lookup(name);
            if(econst == null)
                throw new ConversionException("Illegal enum constant value: " + name);
            return econst.getValue();
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
        TypeSort srcatomtype;
        boolean charornum;
        boolean fail = false;
        long lvalue = 0;

        assert (srctype != null) : "Internal error";

        srcatomtype = srctype.getTypeSort();

        // Do some preliminary conversions
        charornum = true;
        if(srcatomtype.isCharType())
            lvalue = ((Character) value).charValue();
        else if(srcatomtype.isNumericType())
            lvalue = ((Number) value).longValue();
        else if(srcatomtype == TypeSort.UInt64)
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
            DapEnumeration en = (DapEnumeration) srctype;
            DapEnumConst econst = en.lookup(lvalue);
            String name = econst.getShortName();
            if(econst == null) name = "?";
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
    
