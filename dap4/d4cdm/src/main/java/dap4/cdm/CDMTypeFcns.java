/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm;

import dap4.core.dmr.DapEnumeration;
import dap4.core.dmr.DapType;
import dap4.core.dmr.TypeSort;
import dap4.core.util.ConversionException;
import dap4.core.util.CoreTypeFcns;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.ForbiddenConversionException;
import ucar.nc2.EnumTypedef;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

    // Big integer representation of 0xFFFFFFFFFFFFFFFF
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
    createVector(DataType type, long count)
    {
        int icount = (int) count;
        Object vector = null;
        switch (type) {
        case BOOLEAN:
            vector = new boolean[icount];
            break;
        case CHAR:
            vector = new char[icount];
            break;
        case ENUM1:
        case UBYTE:
        case BYTE:
            vector = new byte[icount];
            break;
        case ENUM2:
            ;
        case SHORT:
        case USHORT:
            vector = new short[icount];
            break;
        case ENUM4:
        case INT:
        case UINT:
            vector = new int[icount];
            break;
        case LONG:
        case ULONG:
            vector = new long[icount];
            break;
        case FLOAT:
            vector = new float[icount];
            break;
        case DOUBLE:
            vector = new double[icount];
            break;
        case STRING:
            vector = new String[icount];
            break;
        case OPAQUE:
            vector = new ByteBuffer[icount];
            break;
        default:
            throw new ForbiddenConversionException();
        }
        return vector;
    }

    static public Object
    createVector(DapType type, long count)
    {
        if(type.getAtomicType() == TypeSort.Enum)
            return createVector(((DapEnumeration) type).getBaseType(), count);
        return CoreTypeFcns.createVector(type.getTypeSort(), count);
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
        assert (type != null);
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
        long lval;
        boolean ok = true;
        int len = 0;
        char[] csrc;
        byte[] bsrc;
        short[] shsrc;
        int[] isrc;
        long[] lsrc;
        float[] fsrc;
        double[] dsrc;
        String[] ssrc;
        char[] cresult;
        byte[] bresult;
        short[] shresult;
        int[] iresult;
        long[] lresult;
        float[] fresult;
        double[] dresult;
        String[] sresult;
        BigInteger bi;
        boolean srcunsigned = srcsort.isUnsigned();
        boolean dstunsigned = dstsort.isUnsigned();

        try {
            // Do a double switch src X dst (ugh!)
            done:
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
                    break done;
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
                    break done;
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
                    break done;
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
                    break done;
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
                    break done;
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
                    break done;
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
                    break done;
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
                    break done;
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
                    break done;
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
                            break done;
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
                            break done;
                        }
                        ;
                        shresult[i] = (short) fsrc[i];
                    }
                    break;
                case Int32: //Float32->Int32
                    result = (iresult = new int[len]);
                    for(int i = 0; i < len; i++) {
                        if(fsrc[i] < 0) {
                            ok = false;
                            break done;
                        }
                        iresult[i] = (int) fsrc[i];
                    }
                    break;
                case UInt32://Float32->UInt32
                    result = (iresult = new int[len]);
                    for(int i = 0; i < len; i++) {
                        if(fsrc[i] < 0) {
                            ok = false;
                            break done;
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
                            break done;
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
                    break done;
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
                            break done;
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
                    break done;
                }
                break;

            case String: //String->
                ssrc = (String[]) src;
                len = ssrc.length;
                switch (dstsort) {
                case Char: //String->char is not defined
                    ok = false;
                    break done;
                case Int8: //String->Int8
                    result = (bresult = new byte[len]);
                    for(int i = 0; i < len; i++) {
                        lval = Long.decode(ssrc[i]);
                        if(lval < -128L || lval > 127L) {
                            ok = false;
                            break done;
                        }
                        bresult[i] = (byte) lval;
                    }
                    break;
                case UInt8://String->UInt8
                    result = (bresult = new byte[len]);
                    for(int i = 0; i < len; i++) {
                        lval = Long.decode(ssrc[i]);
                        if(lval < 0 || lval > 255L) {
                            ok = false;
                            break done;
                        }
                        bresult[i] = (byte) lval;
                    }
                    break;
                case Int16: //String->Int16
                    result = (shresult = new short[len]);
                    for(int i = 0; i < len; i++) {
                        lval = Long.decode(ssrc[i]);
                        if(lval < -32768L || lval > 32767L) {
                            ok = false;
                            break done;
                        }
                        shresult[i] = (short) lval;
                    }
                    break;
                case UInt16://String->UInt16
                    result = (shresult = new short[len]);
                    for(int i = 0; i < len; i++) {
                        lval = Long.decode(ssrc[i]);
                        if(lval < 0 || lval > 65535L) {
                            ok = false;
                            break done;
                        }
                        shresult[i] = (short) lval;
                    }
                    break;
                case Int32: //String->Int32
                    result = (iresult = new int[len]);
                    for(int i = 0; i < len; i++) {
                        lval = Long.decode(ssrc[i]);
                        if(lval < -2147483648L || lval > 2147483647L) {
                            ok = false;
                            break done;
                        }
                        iresult[i] = (int) lval;
                    }
                    break;
                case UInt32://String->UInt32
                    result = (iresult = new int[len]);
                    for(int i = 0; i < len; i++) {
                        lval = Long.decode(ssrc[i]);
                        if(lval < 0 || lval > 4294967295L) {
                            ok = false;
                            break done;
                        }
                        iresult[i] = (int) lval;
                    }
                    break;
                case Int64: //String->Int64
                    result = (lresult = new long[len]);
                    for(int i = 0; i < len; i++) {
                        bi = new BigInteger(ssrc[i]);
                        try {
                            lval = bi.longValueExact();
                        } catch (ArithmeticException ae) {
                            ok = false;
                            break done;
                        }
                        lresult[i] = lval;
                    }
                    break;
                case UInt64://String->UInt64
                    result = (lresult = new long[len]);
                    for(int i = 0; i < len; i++) {
                        bi = new BigInteger(ssrc[i]);
                        if(bi.compareTo(BigInteger.ZERO) < 0) {
                            ok = false;
                            break done;
                        }
                        if(bi.compareTo(LONGMASK) > 0) {
                            ok = false;
                            break done;
                        }
                        lval = bi.longValue();
                        lresult[i] = lval;
                    }
                    break;
                case Float32://String->Float32
                    result = (fresult = new float[len]);
                    for(int i = 0; i < len; i++) {
                        fresult[i] = Float.parseFloat(ssrc[i]);
                    }
                    break;
                case Float64://String->Float64
                    result = (dresult = new double[len]);
                    for(int i = 0; i < len; i++) {
                        dresult[i] = Double.parseDouble(ssrc[i]);
                    }
                    break;
                default:
                    ok = false;
                    break done;
                }
                break;

            default:
                ok = false;
                break done;
            }
        } catch (NumberFormatException nfe) {
            ok = false;
        }
        if(!ok)
            throw new ForbiddenConversionException(srcsort.name() + "->" + dstsort.name());
        return result;
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

    /**
     * convert a string to a specified cdmtype
     * Note that if en is defined, then we attempt
     * to convert the string as enum const
     *
     * @param cdmtype
     * @param en
     * @param o
     * @return
     */
    static public Object
    attributeParse(DataType cdmtype, EnumTypedef en, Object o)
    {
        String so = o.toString();
        if(en != null) {
            switch (cdmtype) {
            case ENUM1:
            case ENUM2:
            case ENUM4:
                if(!(o instanceof Integer))
                    throw new ConversionException(o.toString());
                int eval = (Integer) o;
                String econst = en.lookupEnumString(eval);
                if(econst == null)
                    throw new ConversionException(o.toString());
                return econst;
            default:
                throw new ConversionException(o.toString());
            }
        }
        long lval = 0;
        double dval = 0.0;
        boolean islong = true;
        boolean isdouble = true;
        // Do a quick conversion checks
        try {
            lval = Long.parseLong(so);
        } catch (NumberFormatException nfe) {
            islong = false;
        }
        try {
            dval = Double.parseDouble(so);
        } catch (NumberFormatException nfe) {
            isdouble = false;
        }
        o = null; // default is not convertible
        switch (cdmtype) {
        case BOOLEAN:
            if(so.equalsIgnoreCase("false")
                    || (islong && lval == 0))
                o = Boolean.FALSE;
            else
                o = Boolean.TRUE;
            break;
        case BYTE:
            if(islong) o = Byte.valueOf((byte) lval);
            break;
        case SHORT:
            if(islong) o = Short.valueOf((short) lval);
            break;
        case INT:
            if(islong) o = Integer.valueOf((int) lval);
            break;
        case LONG:
            if(islong) o = Long.valueOf(lval);
            break;
        case UBYTE:  // Keep the proper bit pattern
            if(islong) o = Byte.valueOf((byte) (lval & 0xFFL));
            break;
        case USHORT:
            if(islong) o = Short.valueOf((short) (lval & 0xFFFFL));
            break;
        case UINT:
            if(islong) o = Integer.valueOf((int) (lval & 0xFFFFFFFFL));
            break;
        case ULONG:  //Need to resort to BigInteger
            BigInteger bi = new BigInteger(so);
            bi = bi.and(LONGMASK);
            o = (Long) bi.longValue();
            break;
        case FLOAT:
            if(islong && !isdouble) {
                dval = (double) lval;
                isdouble = true;
            }
            if(isdouble) o = (Float) ((float) dval);
            break;
        case DOUBLE:
            if(islong && !isdouble) {
                dval = (double) lval;
                isdouble = true;
            }
            if(isdouble) o = (Double) (dval);
            break;
        case STRING:
            return so;
        case OPAQUE:  // Big Integer then ByteBuffer
            if(so.startsWith("0x") || so.startsWith("0X"))
                so = so.substring(2);
            bi = new BigInteger(so, 16);
            // Now extract bytes
            byte[] bb = bi.toByteArray();
            o = ByteBuffer.wrap(bb);
            break;
        default:
            throw new ConversionException(o.toString());
        }
        if(o == null)
            throw new ConversionException(o.toString());
        return o;
    }

    static public boolean
    isPrimitiveVector(DataType type, Object o)
    {
        Class c = o.getClass();
        if(!c.isArray())
            return false;
        // cannot use isAssignableFrom, I think because primitive
        switch (type) {
        case BOOLEAN:
            return o instanceof boolean[];
        case CHAR:
            return o instanceof char[];
        case ENUM1:
        case BYTE:
        case UBYTE:
            return o instanceof byte[];
        case ENUM2:
        case SHORT:
        case USHORT:
            return o instanceof short[];
        case ENUM4:
        case INT:
        case UINT:
            return o instanceof int[];
        case LONG:
        case ULONG:
            return o instanceof long[];
        case FLOAT:
            return o instanceof float[];
        case DOUBLE:
            return o instanceof double[];
        case STRING:
            return o instanceof String[];
        case OPAQUE:
            return o instanceof ByteBuffer[];
        default:
            break;
        }
        return false;
    }

    static public Array
    arrayify(DataType datatype, Object o)
    {
        // 1. o is a constant
        if(!o.getClass().isArray()) {
            Object ovec = createVector(datatype, 1);
            java.lang.reflect.Array.set(ovec, 0, o);
            o = ovec;
        }
        int[] shape = new int[]{java.lang.reflect.Array.getLength(o)};
        return Array.factory(datatype, shape, o);
    }

    static public Array
    arrayify(DapType type, Object o)
    {
        if(type.getAtomicType() == TypeSort.Enum)
            return arrayify(((DapEnumeration) type).getBaseType(), o);
        return arrayify(CDMTypeFcns.daptype2cdmtype(type), o);
    }

    static public List
    listify(Object vector)
    {
        List list = new ArrayList();
        int icount = java.lang.reflect.Array.getLength(vector);
        for(int i = 0; i < icount; i++) {
            list.add(java.lang.reflect.Array.get(vector, i));
        }
        return list;
    }


    /*
    static public Array
    arraysection(Array a, List<Slice> slices)
    {
	int rank = slices.size();
	int[] origin = new int[rank];
	int[] subshape = new int[rank];
	for(int i = 0; i < rank; i++) {
	    origin[i] = 0;
	    subshape[i] = (int) index.get(i);
	}
	subshape[rank - 1] = 1; // remove  vlen dimension
	    Array records;
	    try {
		records = seqarray.section(origin, subshape, null);
	    } catch (InvalidRangeException e) {
		throw new DapException("Illegal index", e);
	    }
	    */
}
