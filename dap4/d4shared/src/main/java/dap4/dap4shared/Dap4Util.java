/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.DataException;
import dap4.core.dmr.*;
import dap4.core.util.DapUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Constants and utilities
 * either top-level or for a member.
 */

abstract public class Dap4Util
{
    /////////////////////////////////////////////////////
    // Constants


    //////////////////////////////////////////////////
    // Static utility methods

    /**
     * Conmpute the size, in databuffer,
     * of the daptype wrt to a serialization;
     * 0 if undefined.
     *
     * @param atomtype The type of interest
     * @return the size, in databuffer
     */
    static public int
    daptypeSize(AtomicType atomtype)
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


    static public Object
    createVector(AtomicType atype, long count)
        throws DataException
    {
        int icount = (int) count;
        Object vector = null;
        switch (atype) {
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
        default:
            throw new DataException("Illegal Conversion");
        }
        return vector;
    }

    /**
     * Convert an array of one type of values to another type
     *
     * @param dsttype target type
     * @param srctype source type
     * @param src     array of values to convert
     * @return resulting array of converted values as an object
     */

    static public Object
    convertVector(DapType dsttype, DapType srctype, Object src)
        throws DataException
    {
        int i;

        AtomicType srcatomtype = srctype.getPrimitiveType();
        AtomicType dstatomtype = dsttype.getPrimitiveType();

        if(srcatomtype == dstatomtype)
            return src;
        if(srcatomtype.isIntegerType()
            && AtomicType.getSignedVersion(srcatomtype) == AtomicType.getSignedVersion(dstatomtype))
            return src;

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
        boolean srcunsigned = srcatomtype.isUnsigned();
        boolean dstunsigned = dstatomtype.isUnsigned();

        // Do a double switch src X dst (ugh!)
        switch (srcatomtype) {

        case Char: //Char->
            csrc = (char[]) src;
            len = csrc.length;
            switch (dstatomtype) {
            case Char: //char->char
            case Int8: //char->int8
            case UInt8: //char->uint8
                return src;
            case Int16: //char->Int16
            case UInt16://char->UInt16;
                result = (shresult = new short[len]);
                for(i = 0;i < len;i++)
                    shresult[i] = (short) (((int) csrc[i]) & 0xFF);
                break;
            case Int32: //char->Int32
            case UInt32://char->UInt32;
                result = (iresult = new int[len]);
                for(i = 0;i < len;i++)
                    iresult[i] = (int) (((int) csrc[i]) & 0xFF);
                break;
            case Int64: //char->Int64
            case UInt64://char->UInt64;
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++)
                    lresult[i] = (long) (((int) csrc[i]) & 0xFF);
                break;
            case Float32:
                result = (fresult = new float[len]);
                for(i = 0;i < len;i++)
                    fresult[i] = (float) (((int) csrc[i]) & 0xFF);
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(i = 0;i < len;i++)
                    dresult[i] = (double) (((int) csrc[i]) & 0xFF);
                break;
            default:
                ok = false;
                break;
            }
            break;
        case Int8: //Int8->
            bsrc = (byte[]) src;
            len = bsrc.length;
            switch (dstatomtype) {
            case Char: //int8->char
                result = (cresult = new char[len]);
                for(i = 0;i < len;i++)
                    cresult[i] = (char) (((int) bsrc[i]) & 0xFF);
                break;
            case Int16: //int8->Int16
            case UInt16://int8->UInt16;
                result = (shresult = new short[len]);
                for(i = 0;i < len;i++) shresult[i] = (short) bsrc[i];
                if(dstunsigned) {
                    for(i = 0;i < len;i++) shresult[i] &= (short) 0xFF;
                }
                break;
            case Int32: //int8->Int32
            case UInt32://int8->UInt32;
                result = (iresult = new int[len]);
                for(i = 0;i < len;i++) iresult[i] = (int) bsrc[i];
                if(dstunsigned) {
                    for(i = 0;i < len;i++) iresult[i] &= 0xFF;
                }
                break;
            case Int64: //int8->Int64
            case UInt64://int8->UInt64;
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++) lresult[i] = (long) bsrc[i];
                if(dstunsigned) {
                    for(i = 0;i < len;i++) lresult[i] &= 0xFFL;
                }
                break;
            case Float32: //int8->float
                result = (fresult = new float[len]);
                for(i = 0;i < len;i++) fresult[i] = (float) bsrc[i];
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(i = 0;i < len;i++) dresult[i] = (double) bsrc[i];
                break;
            default:
                ok = false;
                break;
            }
            break;
        case UInt8: //UInt8->
            bsrc = (byte[]) src;
            len = bsrc.length;
            switch (dstatomtype) {
            case Char: //Byte->char
                result = (cresult = new char[len]);
                for(i = 0;i < len;i++)
                    cresult[i] = (char) (((int) bsrc[i]) & 0xFF);
                break;
            case Int16: //Byte->Int16
            case UInt16://Byte->UInt16;
                result = (shresult = new short[len]);
                for(i = 0;i < len;i++) shresult[i] = (short) (((int) bsrc[i]) & 0xFF);
                break;
            case Int32: //Byte->Int32
            case UInt32://Byte->UInt32;
                result = (iresult = new int[len]);
                for(i = 0;i < len;i++) iresult[i] = ((int) bsrc[i]) & 0xFF;
                break;
            case Int64: //Byte->Int64
            case UInt64://Byte->UInt64;
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++) lresult[i] = ((long) bsrc[i]) & 0xFFL;
                break;
            case Float32: //Byte->float
                result = (fresult = new float[len]);
                for(i = 0;i < len;i++) fresult[i] = (float) ((int) bsrc[i] & 0xFF);
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(i = 0;i < len;i++) dresult[i] = (double) ((int) bsrc[i] & 0xFF);
                break;
            default:
                ok = false;
                break;
            }
            break;
        case Int16: //Int16->
            shsrc = (short[]) src;
            len = shsrc.length;
            switch (dstatomtype) {
            case Char: //int16->char
                result = (cresult = new char[len]);
                for(i = 0;i < len;i++)
                    cresult[i] = (char) (((int) shsrc[i]) & 0xFF);
                break;
            case Int8: //int16->Int8
            case UInt8://int16->UInt8
                result = (bresult = new byte[len]);
                for(i = 0;i < len;i++) bresult[i] = (byte) shsrc[i];
                break;
            case Int32: //int16->Int32
            case UInt32://int16->UInt32;
                result = (iresult = new int[len]);
                for(i = 0;i < len;i++) iresult[i] = (int) shsrc[i];
                if(dstunsigned) {
                    for(i = 0;i < len;i++) iresult[i] &= 0xFFFF;
                }
                break;
            case Int64: //int16->Int64
            case UInt64://int16->UInt64;
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++) lresult[i] = (long) shsrc[i];
                if(dstunsigned) {
                    for(i = 0;i < len;i++) lresult[i] &= 0xFFFFL;
                }
                break;
            case Float32: //int16->float
                result = (fresult = new float[len]);
                for(i = 0;i < len;i++) fresult[i] = (float) shsrc[i];
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(i = 0;i < len;i++) dresult[i] = (double) shsrc[i];
                break;
            default:
                ok = false;
                break;
            }
            break;
        case UInt16: //UInt16->
            shsrc = (short[]) src;
            len = shsrc.length;
            switch (dstatomtype) {
            case Char: //UInt16->char
                result = (cresult = new char[len]);
                for(i = 0;i < len;i++)
                    cresult[i] = (char) (((int) shsrc[i]) & 0xFF);
                break;
            case Int8: //UInt16->Int8
            case UInt8://UInt16->UInt8
                result = (bresult = new byte[len]);
                for(i = 0;i < len;i++) bresult[i] = (byte) shsrc[i];
                break;
            case Int32: //UInt16->Int32
            case UInt32://UInt16->UInt32;
                result = (iresult = new int[len]);
                for(i = 0;i < len;i++) iresult[i] = ((int) shsrc[i]) & 0xFFFF;
                break;
            case Int64: //UInt16->Int64
            case UInt64://UInt16->UInt64;
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++) lresult[i] = ((long) shsrc[i]) & 0xFFFFL;
                break;
            case Float32: //UInt16->float
                result = (fresult = new float[len]);
                for(i = 0;i < len;i++) fresult[i] = (float) ((int) shsrc[i] & 0xFFFF);
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(i = 0;i < len;i++) dresult[i] = (double) ((int) shsrc[i] & 0xFFFF);
                break;
            default:
                ok = false;
                break;
            }
            break;
        case Int32: //Int32->
            isrc = (int[]) src;
            len = isrc.length;
            switch (dstatomtype) {
            case Char: //int32->char
                result = (cresult = new char[len]);
                for(i = 0;i < len;i++)
                    cresult[i] = (char) (isrc[i] & 0xFF);
                break;
            case Int8: //Int32->Int8
            case UInt8://Int32->UInt8
                result = (bresult = new byte[len]);
                for(i = 0;i < len;i++) bresult[i] = (byte) isrc[i];
                break;
            case Int16: //Int32->Int16
            case UInt16://Int32->UInt16;
                result = (shresult = new short[len]);
                for(i = 0;i < len;i++) shresult[i] = (short) isrc[i];
                break;
            case Int64: //Int32->Int64
            case UInt64://Int32->UInt64
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++) lresult[i] = (long) isrc[i];
                if(dstunsigned) {
                    for(i = 0;i < len;i++) lresult[i] &= 0xFFFFL;
                }
                break;
            case Float32: //int32->float
                result = (fresult = new float[len]);
                for(i = 0;i < len;i++) fresult[i] = (float) isrc[i];
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(i = 0;i < len;i++) dresult[i] = (double) isrc[i];
                break;
            default:
                ok = false;
                break;
            }
            break;
        case UInt32: //UInt32->
            isrc = (int[]) src;
            len = isrc.length;
            switch (dstatomtype) {
            case Char: //UInt32->char
                result = (cresult = new char[len]);
                for(i = 0;i < len;i++)
                    cresult[i] = (char) (((int) isrc[i]) & 0xFF);
                break;
            case Int8: //Int32->Int8
            case UInt8://UInt32->UInt8
                result = (bresult = new byte[len]);
                for(i = 0;i < len;i++) bresult[i] = (byte) isrc[i];
                break;
            case Int16: //Int32->Int16
            case UInt16://UInt32->UInt16
                result = (shresult = new short[len]);
                for(i = 0;i < len;i++) shresult[i] = (short) isrc[i];
                break;
            case Int64: //Int32->Int64
            case UInt64://UInt32->UInt64;
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++) lresult[i] = (long) isrc[i];
                if(dstunsigned) {
                    for(i = 0;i < len;i++) lresult[i] &= 0xFFFFFFFFL;
                }
                break;
            case Float32: //UInt32->float
                result = (fresult = new float[len]);
                for(i = 0;i < len;i++) fresult[i] = (float) ((int) isrc[i] & 0xFFFF);
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(i = 0;i < len;i++) dresult[i] = (double) ((int) isrc[i] & 0xFFFF);
                break;
            default:
                ok = false;
                break;
            }
            break;
        case Int64: //Int64->
            lsrc = (long[]) src;
            len = lsrc.length;
            switch (dstatomtype) {
            case Char: //Int64->char
                result = (cresult = new char[len]);
                for(i = 0;i < len;i++)
                    cresult[i] = (char) (lsrc[i] & 0xFF);
                break;
            case Int8: //Int64->Int8
            case UInt8://Int64->UInt8
                result = (bresult = new byte[len]);
                for(i = 0;i < len;i++) bresult[i] = (byte) lsrc[i];
                break;
            case Int16: //Int64->Int16
            case UInt16://Int64->UInt16;
                result = (shresult = new short[len]);
                for(i = 0;i < len;i++) shresult[i] = (short) lsrc[i];
                break;
            case Int32: //Int64->Int32
            case UInt32://Int64->UInt32;
                result = (iresult = new int[len]);
                for(i = 0;i < len;i++) iresult[i] = (int) lsrc[i];
                break;
            case Float32: //Int64->float
                result = (fresult = new float[len]);
                for(i = 0;i < len;i++) fresult[i] = (float) lsrc[i];
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(i = 0;i < len;i++) dresult[i] = (double) lsrc[i];
                break;
            default:
                ok = false;
                break;
            }
            break;
        case UInt64: //UInt64->
            lsrc = (long[]) src;
            len = lsrc.length;
            switch (dstatomtype) {
            case Char: //UInt64->char
                result = (cresult = new char[len]);
                for(i = 0;i < len;i++)
                    cresult[i] = (char) (lsrc[i] & 0xFFL);
                break;
            case Int8: //Int64->Int8
            case UInt8://UInt64->UInt8
                result = (bresult = new byte[len]);
                for(i = 0;i < len;i++) bresult[i] = (byte) lsrc[i];
                break;
            case Int16: //Int64->Int16
            case UInt16://UInt64->UInt16
                result = (shresult = new short[len]);
                for(i = 0;i < len;i++) shresult[i] = (short) lsrc[i];
                break;
            case Int32: //Int64->Int32
            case UInt32://UInt64->UInt32
                result = (iresult = new int[len]);
                for(i = 0;i < len;i++) iresult[i] = (int) lsrc[i];
                break;
            case Float32: //UInt64->float
                result = (fresult = new float[len]);
                for(i = 0;i < len;i++) {
                    bi = BigInteger.valueOf(lsrc[i]);
                    bi = bi.and(DapUtil.BIG_UMASK64);
                    fresult[i] = bi.floatValue();
                }
                break;
            case Float64:
                result = (dresult = new double[len]);
                for(i = 0;i < len;i++) {
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
            switch (dstatomtype) {
            case Char: //Float32->char
                result = (cresult = new char[len]);
                for(i = 0;i < len;i++)
                    cresult[i] = (char) (((int) fsrc[i]) & 0xFF);
                break;
            case Int8: //Float32->Int8
                result = (bresult = new byte[len]);
                for(i = 0;i < len;i++) bresult[i] = (byte) fsrc[i];
                break;
            case UInt8://Float32->UInt8
                result = (bresult = new byte[len]);
                for(i = 0;i < len;i++) {
                    if(fsrc[i] < 0) {
                        ok = false;
                        break;
                    }
                    bresult[i] = (byte) fsrc[i];
                }
                break;
            case Int16: //Float32->Int16
                result = (shresult = new short[len]);
                for(i = 0;i < len;i++) shresult[i] = (short) fsrc[i];
                break;
            case UInt16://Float32->UInt16
                result = (shresult = new short[len]);
                for(i = 0;i < len;i++) {
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
                for(i = 0;i < len;i++) {
                    if(fsrc[i] < 0) {
                        ok = false;
                        break;
                    }
                    iresult[i] = (int) fsrc[i];
                }
                break;
            case Int64: //Float32->Int64
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++) {
                    BigDecimal bd = new BigDecimal(fsrc[i]);
                    lresult[i] = bd.toBigInteger().longValue();
                }
                break;
            case UInt64://Float32->UInt64
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++) {
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
                for(i = 0;i < len;i++) {
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
            switch (dstatomtype) {
            case Char: //Float64->char
                result = (cresult = new char[len]);
                for(i = 0;i < len;i++)
                    cresult[i] = (char) (((int) dsrc[i]) & 0xFF);
                break;
            case Int8: //Float64->Int8
            case UInt8://Float64->UInt8
                result = (bresult = new byte[len]);
                for(i = 0;i < len;i++) bresult[i] = (byte) dsrc[i];
                break;
            case Int16: //Float64->Int16
            case UInt16://Float64->UInt16
                result = (shresult = new short[len]);
                for(i = 0;i < len;i++) shresult[i] = (short) dsrc[i];
                break;
            case Int32: //Float64->Int32
            case UInt32://Float64->UInt32
                result = (iresult = new int[len]);
                for(i = 0;i < len;i++) iresult[i] = (int) dsrc[i];
                break;
            case Int64: //Float64->Int64
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++) {
                    BigDecimal bd = new BigDecimal(dsrc[i]);
                    lresult[i] = bd.toBigInteger().longValue();
                }
                break;
            case UInt64://Float64->UInt64
                result = (lresult = new long[len]);
                for(i = 0;i < len;i++) {
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
                for(i = 0;i < len;i++) {
                    fresult[i] = (float) dsrc[i];
                }
                break;
            default:
                ok = false;
                break;
            }
            break;

        default:
            throw new DataException(String.format("Illegal Conversion: %s->%s", srctype, dsttype));
        }
        if(!ok)
            throw new DataException(String.format("Illegal Conversion: %s->%s", srctype, dsttype));
        return result;
    }


}
