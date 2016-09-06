/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm;

import dap4.core.dmr.DapEnumeration;
import dap4.core.dmr.DapType;
import dap4.core.dmr.TypeSort;
import dap4.core.util.ConversionException;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import ucar.ma2.DataType;
import ucar.ma2.ForbiddenConversionException;
import ucar.nc2.EnumTypedef;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * This Class is used to isolate as many as possible
 * of the switch statements using TypeSort enums
 * (Or in somecase DapType.getTypeSort())
 * Singleton
 */

abstract public class CDMTypeFcns
{
    //////////////////////////////////////////////////
    // Constants

    static final BigInteger LONGMASK = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    //////////////////////////////////////////////////
    // Static Methods

    /* Needed to implement Array.getElement() */
    static public Class
    cdmElementClass(DataType dt)
    {
        switch (dt) {
        case BOOLEAN:
            return boolean.class;
        case ENUM1:
        case BYTE:
            return byte.class;
        case CHAR:
            return char.class;
        case ENUM2:
        case SHORT:
            return short.class;
        case ENUM4:
        case INT:
            return int.class;
        case LONG:
            return long.class;
        case FLOAT:
            return float.class;
        case DOUBLE:
            return double.class;
        case STRING:
            return String.class;
        case OPAQUE:
            return ByteBuffer.class;
        case UBYTE:
            return byte.class;
        case USHORT:
            return short.class;
        case UINT:
            return int.class;
        case ULONG:
            return long.class;
        default:
            break;
        }
        return null;
    }

    static public Object
    createVector(DapType type, long count)
    {
        int icount = (int) count;
        Object vector = null;
        switch (type.getAtomicType()) {
        case Char:
            vector = new char[icount];
            break;
        case UInt8:
        case Int8:
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
        case URL:
            vector = new String[icount];
            break;
        case Opaque:
            vector = new ByteBuffer[icount];
            break;
        case Enum:
            return createVector(((DapEnumeration) type).getBaseType(), count);
        default:
            throw new ForbiddenConversionException();
        }
        return vector;
    }

    static public DataType
    enumTypeFor(DapType type)
    {
        switch (type.getTypeSort()) {
        case Char:
        case Int8:
        case UInt8:
            return DataType.ENUM1;
        case Int16:
        case UInt16:
            return DataType.ENUM2;
        case Int32:
        case UInt32:
            return DataType.ENUM4;
        case Int64:
        case UInt64:
            // Unfortunately, CDM does not support (U)Int64, so truncate to 32.
            return DataType.ENUM4;
        case Enum:
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return CDMTypeFcns.enumTypeFor(((DapEnumeration) type).getBaseType());
        default:
            break;
        }
        return null;
    }

    static public DapType
    cdmtype2daptype(DataType datatype)
    {
        switch (datatype) {
        case CHAR:
            return DapType.CHAR;
        case BYTE:
            return DapType.INT8;
        case SHORT:
            return DapType.INT16;
        case INT:
            return DapType.INT32;
        case LONG:
            return DapType.INT64;
        case UBYTE:
            return DapType.UINT8;
        case USHORT:
            return DapType.UINT16;
        case UINT:
            return DapType.UINT32;
        case ULONG:
            return DapType.UINT64;
        case FLOAT:
            return DapType.FLOAT32;
        case DOUBLE:
            return DapType.FLOAT64;
        case STRING:
            return DapType.STRING;
        case OPAQUE:
            return DapType.OPAQUE;

        // For these, return the integer basetype
        case ENUM1:
            return DapType.INT8;
        case ENUM2:
            return DapType.INT16;
        case ENUM4:
            return DapType.INT32;

        // Undefined
        case SEQUENCE:
        case STRUCTURE:
        default:
            break;
        }
        return null;
    }

    static public DataType
    daptype2cdmtype(DapType type)
    {
        assert(type != null);
        switch (type.getTypeSort()) {
        case Char:
            return DataType.CHAR;
        case UInt8:
            return DataType.UBYTE;
        case Int8:
            return DataType.BYTE;
        case Int16:
            return DataType.SHORT;
        case UInt16:
            return DataType.USHORT;
        case Int32:
            return DataType.INT;
        case UInt32:
            return DataType.UINT;
        case Int64:
            return DataType.LONG;
        case UInt64:
            return DataType.ULONG;
        case Float32:
            return DataType.FLOAT;
        case Float64:
            return DataType.DOUBLE;
        case String:
        case URL:
            return DataType.STRING;
        case Opaque:
            return DataType.OPAQUE;
        case Enum:
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            DapEnumeration dapenum = (DapEnumeration) type;
            switch (dapenum.getBaseType().getTypeSort()) {
            case Char:
            case UInt8:
            case Int8:
                return DataType.ENUM1;
            case Int16:
            case UInt16:
                return DataType.ENUM2;
            case Int32:
            case UInt32:
                return DataType.ENUM4;
            case Int64:
            case UInt64:
                // since there is no ENUM8, use ENUM4
                return DataType.ENUM4;
            default:
                break;
            }
            break;
        case Structure:
            return DataType.STRUCTURE;
        case Sequence:
            return DataType.SEQUENCE;
        default:
            break;
        }
        return null;
    }


    /**
     * Conmpute the size, in databuffer,
     * of the daptype wrt to a serialization;
     * 0 if undefined.
     *
     * @param atomtype The type of interest
     * @return the size, in databuffer
     */
    static public int
    daptypeSize(TypeSort atomtype)
    {
        switch (atomtype) {
        case Char: // remember serial size is 1, not 2.
        case UInt8:
        case Int8:
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

    static public long
    extract(TypeSort sort, Object value)
    {
        long lvalue = 0;
        switch (sort) {
        case Int8:
            lvalue = (long) ((Byte) value).byteValue();
            break;
        case Char:
        case UInt8:
            lvalue = (long) ((Byte) value).byteValue();
            lvalue = lvalue & 0xFFL;
            break;
        case Int16:
            lvalue = (long) ((Short) value).shortValue();
            break;
        case UInt16:
            lvalue = (long) ((Short) value).shortValue();
            lvalue = lvalue & 0xFFFFL;
            break;
        case Int32:
            lvalue = (long) ((Integer) value).intValue();
            break;
        case UInt32:
            lvalue = (long) ((Integer) value).intValue();
            lvalue = lvalue & 0xFFFFFFFFL;
            break;
        case Int64:
        case UInt64:
            lvalue = ((Long) value).longValue();
            break;
        case Float32:
            lvalue = (long) ((Float) value).floatValue();
            break;
        case Float64:
            lvalue = (long) ((Double) value).doubleValue();
            break;
        default:
            throw new ForbiddenConversionException("Type not convertible to long");
        }
        return lvalue;
    }

    static public Object
    convert(TypeSort dstsort, TypeSort srcsort, Object src)
    {
        Object result = null;
        boolean ok = true;
        int len = 0;
        char[] csrc;
        byte[] bsrc;
        short[] shsrc;
        int[] isrc;
        long[] lsrc;
        float[] fsrc;
        double[] dsrc;
        char[] cresult;
        byte[] bresult;
        short[] shresult;
        int[] iresult;
        long[] lresult;
        float[] fresult;
        double[] dresult;
        BigInteger bi;
        boolean srcunsigned = srcsort.isUnsigned();
        boolean dstunsigned = dstsort.isUnsigned();

        // Do a double switch src X dst (ugh!)
        switch (srcsort) {

        case Char: //Char->
            csrc = (char[]) src;
            len = csrc.length;
            switch (dstsort) {
            case Char: //char->char
            case Int8: //char->int8
            case UInt8: //char->uint8
                return src;
            case Int16: //char->Int16
            case UInt16://char->UInt16;
                result = (shresult = new short[len]);
                for(int i = 0; i < len; i++) {
                    shresult[i] = (short) (((int) csrc[i]) & 0xFF);
                }
                break;
            case Int32: //char->Int32
            case UInt32://char->UInt32;
                result = (iresult = new int[len]);
                for(int i = 0; i < len; i++) {
                    iresult[i] = (int) (((int) csrc[i]) & 0xFF);
                }
                break;
            case Int64: //char->Int64
            case UInt64://char->UInt64;
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    lresult[i] = (long) (((int) csrc[i]) & 0xFF);
                }
                break;
            case Float32:
                result = (fresult = new float[len]);
                for(int i = 0; i < len; i++) {
                    fresult[i] = (float) (((int) csrc[i]) & 0xFF);
                }
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(int i = 0; i < len; i++) {
                    dresult[i] = (double) (((int) csrc[i]) & 0xFF);
                }
                break;
            default:
                ok = false;
                break;
            }
            break;
        case Int8: //Int8->
            bsrc = (byte[]) src;
            len = bsrc.length;
            switch (dstsort) {
            case Char: //int8->char
                result = (cresult = new char[len]);
                for(int i = 0; i < len; i++) {
                    cresult[i] = (char) (((int) bsrc[i]) & 0xFF);
                }
                break;
            case Int16: //int8->Int16
            case UInt16://int8->UInt16;
                result = (shresult = new short[len]);
                for(int i = 0; i < len; i++) {
                    shresult[i] = (short) bsrc[i];
                }
                if(dstunsigned) {
                    for(int i = 0; i < len; i++) {
                        shresult[i] &= (short) 0xFF;
                    }
                }
                break;
            case Int32: //int8->Int32
            case UInt32://int8->UInt32;
                result = (iresult = new int[len]);
                for(int i = 0; i < len; i++) {
                    iresult[i] = (int) bsrc[i];
                }
                if(dstunsigned) {
                    for(int i = 0; i < len; i++) {
                        iresult[i] &= 0xFF;
                    }
                }
                break;
            case Int64: //int8->Int64
            case UInt64://int8->UInt64;
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    lresult[i] = (long) bsrc[i];
                }
                if(dstunsigned) {
                    for(int i = 0; i < len; i++) {
                        lresult[i] &= 0xFFL;
                    }
                }
                break;
            case Float32: //int8->float
                result = (fresult = new float[len]);
                for(int i = 0; i < len; i++) {
                    fresult[i] = (float) bsrc[i];
                }
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(int i = 0; i < len; i++) {
                    dresult[i] = (double) bsrc[i];
                }
                break;
            default:
                ok = false;
                break;
            }
            break;
        case UInt8: //UInt8->
            bsrc = (byte[]) src;
            len = bsrc.length;
            switch (dstsort) {
            case Char: //Byte->char
                result = (cresult = new char[len]);
                for(int i = 0; i < len; i++) {
                    cresult[i] = (char) (((int) bsrc[i]) & 0xFF);
                }
                break;
            case Int16: //Byte->Int16
            case UInt16://Byte->UInt16;
                result = (shresult = new short[len]);
                for(int i = 0; i < len; i++) {
                    shresult[i] = (short) (((int) bsrc[i]) & 0xFF);
                }
                break;
            case Int32: //Byte->Int32
            case UInt32://Byte->UInt32;
                result = (iresult = new int[len]);
                for(int i = 0; i < len; i++) {
                    iresult[i] = ((int) bsrc[i]) & 0xFF;
                }
                break;
            case Int64: //Byte->Int64
            case UInt64://Byte->UInt64;
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    lresult[i] = ((long) bsrc[i]) & 0xFFL;
                }
                break;
            case Float32: //Byte->float
                result = (fresult = new float[len]);
                for(int i = 0; i < len; i++) {
                    fresult[i] = (float) ((int) bsrc[i] & 0xFF);
                }
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(int i = 0; i < len; i++) {
                    dresult[i] = (double) ((int) bsrc[i] & 0xFF);
                }
                break;
            default:
                ok = false;
                break;
            }
            break;
        case Int16: //Int16->
            shsrc = (short[]) src;
            len = shsrc.length;
            switch (dstsort) {
            case Char: //int16->char
                result = (cresult = new char[len]);
                for(int i = 0; i < len; i++) {
                    cresult[i] = (char) (((int) shsrc[i]) & 0xFF);
                }
                break;
            case Int8: //int16->Int8
            case UInt8://int16->UInt8
                result = (bresult = new byte[len]);
                for(int i = 0; i < len; i++) {
                    bresult[i] = (byte) shsrc[i];
                }
                break;
            case Int32: //int16->Int32
            case UInt32://int16->UInt32;
                result = (iresult = new int[len]);
                for(int i = 0; i < len; i++) {
                    iresult[i] = (int) shsrc[i];
                }
                if(dstunsigned) {
                    for(int i = 0; i < len; i++) {
                        iresult[i] &= 0xFFFF;
                    }
                }
                break;
            case Int64: //int16->Int64
            case UInt64://int16->UInt64;
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    lresult[i] = (long) shsrc[i];
                }
                if(dstunsigned) {
                    for(int i = 0; i < len; i++) {
                        lresult[i] &= 0xFFFFL;
                    }
                }
                break;
            case Float32: //int16->float
                result = (fresult = new float[len]);
                for(int i = 0; i < len; i++) {
                    fresult[i] = (float) shsrc[i];
                }
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(int i = 0; i < len; i++) {
                    dresult[i] = (double) shsrc[i];
                }
                break;
            default:
                ok = false;
                break;
            }
            break;
        case UInt16: //UInt16->
            shsrc = (short[]) src;
            len = shsrc.length;
            switch (dstsort) {
            case Char: //UInt16->char
                result = (cresult = new char[len]);
                for(int i = 0; i < len; i++) {
                    cresult[i] = (char) (((int) shsrc[i]) & 0xFF);
                }
                break;
            case Int8: //UInt16->Int8
            case UInt8://UInt16->UInt8
                result = (bresult = new byte[len]);
                for(int i = 0; i < len; i++) {
                    bresult[i] = (byte) shsrc[i];
                }
                break;
            case Int32: //UInt16->Int32
            case UInt32://UInt16->UInt32;
                result = (iresult = new int[len]);
                for(int i = 0; i < len; i++) {
                    iresult[i] = ((int) shsrc[i]) & 0xFFFF;
                }
                break;
            case Int64: //UInt16->Int64
            case UInt64://UInt16->UInt64;
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    lresult[i] = ((long) shsrc[i]) & 0xFFFFL;
                }
                break;
            case Float32: //UInt16->float
                result = (fresult = new float[len]);
                for(int i = 0; i < len; i++) {
                    fresult[i] = (float) ((int) shsrc[i] & 0xFFFF);
                }
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(int i = 0; i < len; i++) {
                    dresult[i] = (double) ((int) shsrc[i] & 0xFFFF);
                }
                break;
            default:
                ok = false;
                break;
            }
            break;
        case Int32: //Int32->
            isrc = (int[]) src;
            len = isrc.length;
            switch (dstsort) {
            case Char: //int32->char
                result = (cresult = new char[len]);
                for(int i = 0; i < len; i++) {
                    cresult[i] = (char) (isrc[i] & 0xFF);
                }
                break;
            case Int8: //Int32->Int8
            case UInt8://Int32->UInt8
                result = (bresult = new byte[len]);
                for(int i = 0; i < len; i++) {
                    bresult[i] = (byte) isrc[i];
                }
                break;
            case Int16: //Int32->Int16
            case UInt16://Int32->UInt16;
                result = (shresult = new short[len]);
                for(int i = 0; i < len; i++) {
                    shresult[i] = (short) isrc[i];
                }
                break;
            case Int64: //Int32->Int64
            case UInt64://Int32->UInt64
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    lresult[i] = (long) isrc[i];
                }
                if(dstunsigned) {
                    for(int i = 0; i < len; i++) {
                        lresult[i] &= 0xFFFFL;
                    }
                }
                break;
            case Float32: //int32->float
                result = (fresult = new float[len]);
                for(int i = 0; i < len; i++) {
                    fresult[i] = (float) isrc[i];
                }
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(int i = 0; i < len; i++) {
                    dresult[i] = (double) isrc[i];
                }
                break;
            default:
                ok = false;
                break;
            }
            break;
        case UInt32: //UInt32->
            isrc = (int[]) src;
            len = isrc.length;
            switch (dstsort) {
            case Char: //UInt32->char
                result = (cresult = new char[len]);
                for(int i = 0; i < len; i++) {
                    cresult[i] = (char) (((int) isrc[i]) & 0xFF);
                }
                break;
            case Int8: //Int32->Int8
            case UInt8://UInt32->UInt8
                result = (bresult = new byte[len]);
                for(int i = 0; i < len; i++) {
                    bresult[i] = (byte) isrc[i];
                }
                break;
            case Int16: //Int32->Int16
            case UInt16://UInt32->UInt16
                result = (shresult = new short[len]);
                for(int i = 0; i < len; i++) {
                    shresult[i] = (short) isrc[i];
                }
                break;
            case Int64: //Int32->Int64
            case UInt64://UInt32->UInt64;
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    lresult[i] = (long) isrc[i];
                }
                if(dstunsigned) {
                    for(int i = 0; i < len; i++) {
                        lresult[i] &= 0xFFFFFFFFL;
                    }
                }
                break;
            case Float32: //UInt32->float
                result = (fresult = new float[len]);
                for(int i = 0; i < len; i++) {
                    fresult[i] = (float) ((int) isrc[i] & 0xFFFF);
                }
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(int i = 0; i < len; i++) {
                    dresult[i] = (double) ((int) isrc[i] & 0xFFFF);
                }
                break;
            default:
                ok = false;
                break;
            }
            break;
        case Int64: //Int64->
            lsrc = (long[]) src;
            len = lsrc.length;
            switch (dstsort) {
            case Char: //Int64->char
                result = (cresult = new char[len]);
                for(int i = 0; i < len; i++) {
                    cresult[i] = (char) (lsrc[i] & 0xFF);
                }
                break;
            case Int8: //Int64->Int8
            case UInt8://Int64->UInt8
                result = (bresult = new byte[len]);
                for(int i = 0; i < len; i++) {
                    bresult[i] = (byte) lsrc[i];
                }
                break;
            case Int16: //Int64->Int16
            case UInt16://Int64->UInt16;
                result = (shresult = new short[len]);
                for(int i = 0; i < len; i++) {
                    shresult[i] = (short) lsrc[i];
                }
                break;
            case Int32: //Int64->Int32
            case UInt32://Int64->UInt32;
                result = (iresult = new int[len]);
                for(int i = 0; i < len; i++) {
                    iresult[i] = (int) lsrc[i];
                }
                break;
            case Float32: //Int64->float
                result = (fresult = new float[len]);
                for(int i = 0; i < len; i++) {
                    fresult[i] = (float) lsrc[i];
                }
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(int i = 0; i < len; i++) {
                    dresult[i] = (double) lsrc[i];
                }
                break;
            default:
                ok = false;
                break;
            }
            break;
        case UInt64: //UInt64->
            lsrc = (long[]) src;
            len = lsrc.length;
            switch (dstsort) {
            case Char: //UInt64->char
                result = (cresult = new char[len]);
                for(int i = 0; i < len; i++) {
                    cresult[i] = (char) (lsrc[i] & 0xFFL);
                }
                break;
            case Int8: //Int64->Int8
            case UInt8://UInt64->UInt8
                result = (bresult = new byte[len]);
                for(int i = 0; i < len; i++) {
                    bresult[i] = (byte) lsrc[i];
                }
                break;
            case Int16: //Int64->Int16
            case UInt16://UInt64->UInt16
                result = (shresult = new short[len]);
                for(int i = 0; i < len; i++) {
                    shresult[i] = (short) lsrc[i];
                }
                break;
            case Int32: //Int64->Int32
            case UInt32://UInt64->UInt32
                result = (iresult = new int[len]);
                for(int i = 0; i < len; i++) {
                    iresult[i] = (int) lsrc[i];
                }
                break;
            case Float32: //UInt64->float
                result = (fresult = new float[len]);
                for(int i = 0; i < len; i++) {
                    bi = BigInteger.valueOf(lsrc[i]);
                    bi = bi.and(DapUtil.BIG_UMASK64);
                    fresult[i] = bi.floatValue();
                }
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(int i = 0; i < len; i++) {
                    bi = BigInteger.valueOf(lsrc[i]);
                    bi = bi.and(DapUtil.BIG_UMASK64);
                    dresult[i] = bi.doubleValue();
                }
                break;
            default:
                ok = false;
                break;
            }
            break;
        case Float32: //Float32->
            fsrc = (float[]) src;
            len = fsrc.length;
            switch (dstsort) {
            case Char: //Float32->char
                result = (cresult = new char[len]);
                for(int i = 0; i < len; i++) {
                    cresult[i] = (char) (((int) fsrc[i]) & 0xFF);
                }
                break;
            case Int8: //Float32->Int8
                result = (bresult = new byte[len]);
                for(int i = 0; i < len; i++) {
                    bresult[i] = (byte) fsrc[i];
                }
                break;
            case UInt8://Float32->UInt8
                result = (bresult = new byte[len]);
                for(int i = 0; i < len; i++) {
                    if(fsrc[i] < 0) {
                        ok = false;
                        break;
                    }
                    bresult[i] = (byte) fsrc[i];
                }
                break;
            case Int16: //Float32->Int16
                result = (shresult = new short[len]);
                for(int i = 0; i < len; i++) {
                    shresult[i] = (short) fsrc[i];
                }
                break;
            case UInt16://Float32->UInt16
                result = (shresult = new short[len]);
                for(int i = 0; i < len; i++) {
                    if(fsrc[i] < 0) {
                        ok = false;
                        break;
                    }
                    shresult[i] = (short) fsrc[i];
                }
                break;
            case Int32: //Float32->Int32
            case UInt32://Float32->UInt32
                result = (iresult = new int[len]);
                for(int i = 0; i < len; i++) {
                    if(fsrc[i] < 0) {
                        ok = false;
                        break;
                    }
                    iresult[i] = (int) fsrc[i];
                }
                break;
            case Int64: //Float32->Int64
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    BigDecimal bd = new BigDecimal(fsrc[i]);
                    lresult[i] = bd.toBigInteger().longValue();
                }
                break;
            case UInt64://Float32->UInt64
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    if(fsrc[i] < 0) {
                        ok = false;
                        break;
                    } // not convertible
                    BigDecimal bd = new BigDecimal(fsrc[i]);
                    lresult[i] = bd.toBigInteger().longValue();
                }
                break;
            case Float64://Float32->Float64
                result = (dresult = new double[len]);
                for(int i = 0; i < len; i++) {
                    dresult[i] = (double) fsrc[i];
                }
                break;
            default:
                ok = false;
                break;
            }
            break;
        case Float64: //Float64->
            dsrc = (double[]) src;
            len = dsrc.length;
            switch (dstsort) {
            case Char: //Float64->char
                result = (cresult = new char[len]);
                for(int i = 0; i < len; i++) {
                    cresult[i] = (char) (((int) dsrc[i]) & 0xFF);
                }
                break;
            case Int8: //Float64->Int8
            case UInt8://Float64->UInt8
                result = (bresult = new byte[len]);
                for(int i = 0; i < len; i++) {
                    bresult[i] = (byte) dsrc[i];
                }
                break;
            case Int16: //Float64->Int16
            case UInt16://Float64->UInt16
                result = (shresult = new short[len]);
                for(int i = 0; i < len; i++) {
                    shresult[i] = (short) dsrc[i];
                }
                break;
            case Int32: //Float64->Int32
            case UInt32://Float64->UInt32
                result = (iresult = new int[len]);
                for(int i = 0; i < len; i++) {
                    iresult[i] = (int) dsrc[i];
                }
                break;
            case Int64: //Float64->Int64
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    BigDecimal bd = new BigDecimal(dsrc[i]);
                    lresult[i] = bd.toBigInteger().longValue();
                }
                break;
            case UInt64://Float64->UInt64
                result = (lresult = new long[len]);
                for(int i = 0; i < len; i++) {
                    if(dsrc[i] < 0) {
                        ok = false;
                        break;
                    } // not convertible
                    BigDecimal bd = new BigDecimal(dsrc[i]);
                    lresult[i] = bd.toBigInteger().longValue();
                }
                break;
            case Float32://Float32->Float64
                result = (fresult = new float[len]);
                for(int i = 0; i < len; i++) {
                    fresult[i] = (float) dsrc[i];
                }
                break;
            default:
                ok = false;
                break;
            }
            break;

        default:
            throw new ForbiddenConversionException();
        }
        return (ok ? result : null);
    }


    static public void
    vectorcopy(DapType datatype, Object src, Object dst, long srcoffset, long dstoffset)
            throws DapException
    {
        switch (datatype.getTypeSort()) {
        case UInt8:
        case Int8:
            ((byte[]) dst)[(int) dstoffset] = ((byte[]) src)[(int) srcoffset];
            break;
        case Char:
            ((char[]) dst)[(int) dstoffset] = ((char[]) src)[(int) srcoffset];
            break;
        case UInt16:
        case Int16:
            ((short[]) dst)[(int) dstoffset] = ((short[]) src)[(int) srcoffset];
            break;
        case UInt32:
        case Int32:
            ((int[]) dst)[(int) dstoffset] = ((int[]) src)[(int) srcoffset];
            break;
        case UInt64:
        case Int64:
            ((long[]) dst)[(int) dstoffset] = ((long[]) src)[(int) srcoffset];
            break;
        case Float32:
            ((float[]) dst)[(int) dstoffset] = ((float[]) src)[(int) srcoffset];
            break;
        case Float64:
            ((double[]) dst)[(int) dstoffset] = ((double[]) src)[(int) srcoffset];
            break;
        case Opaque:
            // Sigh, bytebuffer hidden by CDM
            Object o = ((Object[]) src)[(int) srcoffset];
            ((ByteBuffer[]) dst)[(int) dstoffset] = (ByteBuffer) o;
            break;
        case String:
            // Sigh, String hidden by CDM
            o = ((Object[]) src)[(int) srcoffset];
            ((String[]) dst)[(int) dstoffset] = (String) o;
            break;
        case Enum:
            vectorcopy(((DapEnumeration) datatype).getBaseType(), src, dst, srcoffset, dstoffset);
            break;
        case Structure:
        case Sequence:
        default:
            throw new DapException("Attempt to read non-atomic value of type: " + datatype);
        }
    }

    static public Object
    attributeConvert(DataType cdmtype, EnumTypedef en, Object o)
    {
        if(en != null) {
            switch (cdmtype) {
            case ENUM1:
            case ENUM2:
            case ENUM4:
                if(!(o instanceof Long))
                    throw new ConversionException(o.toString());
                long eval = (Long) o;
                String econst = en.lookupEnumString((int) eval);
                if(econst == null)
                    throw new ConversionException(o.toString());
                return econst;
            default:
                throw new ConversionException(o.toString());
            }
        } else if(cdmtype == DataType.STRING) {
            return o.toString();
        } else if(o instanceof Long) {
            long lval = (Long) o;
            switch (cdmtype) {
            case BOOLEAN:
                return (lval == 0 ? Boolean.TRUE : Boolean.FALSE);
            case BYTE:
                return (byte) (lval);
            case SHORT:
                return (short) (lval);
            case INT:
                return (int) (lval);
            case LONG:
                return lval;
            case UBYTE:
                return (byte) (lval & 0xFF);
            case USHORT:
                return (short) (lval & 0xFFFF);
            case UINT:
                return (short) (lval & 0xFFFFFFFF);
            case ULONG:
                return lval;
            case STRING:
                return ((Long) o).toString();
            default:
                throw new ConversionException(o.toString());
            }
        } else if(o instanceof Float || o instanceof Double || o instanceof Character)
            return o;
        else if(cdmtype == DataType.OPAQUE) {
            assert o instanceof ByteBuffer;
            return o;
        }
        return o;
    }

}
