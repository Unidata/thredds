/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.DapEnumConst;
import dap4.core.dmr.DapEnumeration;
import dap4.core.dmr.DapType;
import dap4.core.dmr.TypeSort;

import java.math.BigInteger;
import java.nio.ByteBuffer;

abstract public class CoreTypeFcns
{

    /**
     * Force an attribute value (typically string)
     * to match a given basetype
     *
     * @param type  expected type
     * @param value actual value
     * @return the value as made to conform to the expected type
     */
    static public Object
    attributeConvert(DapType type, Object value)
    {
        if(value == null) return value;
        if(type.isEnumType()) {
            if(value instanceof String) {
                // See if the constant is an int vs enum const name
                try {
                    int ival = Integer.parseInt(value.toString());
                    return ival;
                } catch (NumberFormatException nfe) {
                    // Assume it is an econst name; try to locate it
                    DapEnumConst dec = ((DapEnumeration) type).lookup(value.toString());
                    if(dec == null)
                        return value;
                    return dec.getValue();
                }
            } else if(value instanceof Long) {
              return (Long) value;
            }
        } else if(value instanceof Long) {
            return (Long) value;
        } else if(value instanceof Float) {
            return (Float) value;
        } else if(value instanceof Double) {
            return (Double) value;
        } else if(value instanceof Character) {
            return ((Character) value);
        }
        return value;
    }

    /* Get the size of an equivalent java object; zero if not defined */
    static public int getJavaSize(TypeSort atomtype)
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
    static public long forceRange(TypeSort basetype, long value)
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
     * Peg a value to either the min or max
     * depending on sign.
     *
     * @param value the value to peg
     * @param min   peg to this if value is < min
     * @param max   peg to this if value is > max
     * @return pegg'ed value
     */
    static protected long
    minmax(long value, long min, long max)
    {
        if(value < min) return min;
        if(value > max) return max;
        return value;
    }

    static public Object
    get(DapType type, Object v, int n)
    {
        switch (type.getAtomicType()) {
        case Char:
            return ((char[]) v)[n];
        case Int8:
        case UInt8:
            return ((byte[]) v)[n];
        case Int16:
        case UInt16:
            return ((short[]) v)[n];
        case Int32:
        case UInt32:
            return ((int[]) v)[n];
        case Int64:
        case UInt64:
            return ((long[]) v)[n];
        case Float32:
            return ((float[]) v)[n];
        case Float64:
            return ((double[]) v)[n];
        case String:
        case URL:
            return ((String[]) v)[n];
        case Opaque:
            return ((ByteBuffer[]) v)[n];
        case Enum:
            return get(((DapEnumeration) type).getBaseType(), v, n);
        case Structure:
        case Sequence:
        default:
            break;
        }
        throw new IllegalArgumentException();
    }

    static public void
    put(TypeSort sort, Object v, int n, Object value)
    {
        switch (sort) {
        case Char:
            ((char[]) v)[n] = ((char[]) value)[0];
            break;
        case Int8:
        case UInt8:
            ((byte[]) v)[n] = ((byte[]) value)[0];
            break;
        case Int16:
        case UInt16:
            ((short[]) v)[n] = ((short[]) value)[0];
            break;
        case Int32:
        case UInt32:
            ((int[]) v)[n] = ((int[]) value)[0];
            break;
        case Int64:
        case UInt64:
            ((long[]) v)[n] = ((long[]) value)[0];
            break;
        case Float32:
            ((float[]) v)[n] = ((float[]) value)[0];
            break;
        case Float64:
            ((double[]) v)[n] = ((double[]) value)[0];
            break;
        case String:
        case URL:
            ((String[]) v)[n] = ((String[]) value)[0];
            break;
        case Opaque:
            ((ByteBuffer[]) v)[n] = ((ByteBuffer[]) value)[0];
            break;
        case Enum:
        case Structure:
        case Sequence:
        default:
            throw new IllegalArgumentException();
        }
    }

    static public int
    putVector(TypeSort sort, Object v, int offset, Object vec)
    {
        int len = 0;
        switch (sort) {
        case Char:
            len = ((char[]) vec).length;
            System.arraycopy(((char[]) v), offset, ((char[]) vec), 0, len);
            break;
        case Int8:
        case UInt8:
            len = ((int[]) vec).length;
            System.arraycopy(((byte[]) v), offset, ((byte[]) vec), 0, len);
            break;
        case Int16:
        case UInt16:
            len = ((short[]) vec).length;
            System.arraycopy(((short[]) v), offset, ((short[]) vec), 0, len);
            break;
        case Int32:
        case UInt32:
            len = ((int[]) vec).length;
            System.arraycopy(((int[]) v), offset, ((int[]) vec), 0, len);
            break;
        case Int64:
        case UInt64:
            len = ((long[]) vec).length;
            System.arraycopy(((long[]) v), offset, ((long[]) vec), 0, len);
            break;
        case Float32:
            len = ((float[]) vec).length;
            System.arraycopy(((float[]) v), offset, ((float[]) vec), 0, len);
            break;
        case Float64:
            len = ((double[]) vec).length;
            System.arraycopy(((double[]) v), offset, ((double[]) vec), 0, len);
            break;
        case String:
        case URL:
            len = ((String[]) vec).length;
            System.arraycopy(((String[]) v), offset, ((String[]) vec), 0, len);
            break;
        case Opaque:
            len = ((ByteBuffer[]) vec).length;
            System.arraycopy(((ByteBuffer[]) v), offset, ((ByteBuffer[]) vec), 0, len);
            break;
        case Enum:
        case Structure:
        case Sequence:
        default:
            throw new IllegalArgumentException();
        }
        return len;
    }

    static public Object get(TypeSort sort, ByteBuffer b, int n)
    {
        switch (sort) {
        case Char:
            return (char) (b.get(n) & 0xFFL);
        case Int8:
        case UInt8:
            return (b.get(n));
        case Int16:
        case UInt16:
            return (b.getShort(n));
        case Int32:
        case UInt32:
            return (b.getInt(n));
        case Int64:
        case UInt64:
            return (b.getLong(n));
        case Float32:
            return (b.getFloat(n));
        case Float64:
            return (b.getDouble(n));
        case String:
        case URL:
            byte[] bytes = new byte[(int) b.getLong()];
            b.get(bytes);
            return new String(bytes, DapUtil.UTF8);
        case Opaque:
            bytes = new byte[(int) b.getLong()];
            b.get(bytes);
            return ByteBuffer.wrap(bytes);
        case Enum:
        case Structure:
        case Sequence:
        default:
            break;
        }
        throw new IllegalArgumentException();
    }


    static public Object
    createVector(TypeSort type, long count)
    {
        int icount = (int) count;
        Object vector = null;
        switch (type) {
        case Char:
            vector = new char[icount];
            break;
        case Int8:
        case UInt8:
            vector = new byte[icount];
            break;
        case Int16:
        case UInt16:
            vector = new short[icount];
            break;
        case Int32:
        case UInt32:
            vector = new int[icount];
            break;
        case Int64:
        case UInt64:
            vector = new long[icount];
            break;
        case Float32:
            vector = new float[icount];
            break;
        case Float64:
            vector = new double[icount];
            break;
        case String:
            vector = new String[icount];
            break;
        case Opaque:
            vector = new ByteBuffer[icount];
            break;
        default:
            throw new ConversionException();
        }
        return vector;
    }

}

