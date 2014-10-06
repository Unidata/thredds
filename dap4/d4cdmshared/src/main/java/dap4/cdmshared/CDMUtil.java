/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdmshared;

import dap4.core.data.DataException;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4shared.D4DataAtomic;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * CDM related Constants and utilities
 * common to client and server code
 */

abstract public class CDMUtil
{

    static final String hexchars = "0123456789abcdef";

    /**
     * Convert a list of ucar.ma2.Range to a list of Slice
     * More or less the inverst of create CDMRanges
     *
     * @param rangelist the set of ucar.ma2.Range
     * @result the equivalent list of Slice
     */
    static public List<Slice>
    createSlices(List<Range> rangelist)
        throws DapException
    {
        List<Slice> slices = new ArrayList<Slice>(rangelist.size());
        for(int i = 0;i < rangelist.size();i++) {
            Range r = rangelist.get(i);
            // r does not store last
            int stride = r.stride();
            int first = r.first();
            int n = r.length();
            int stop = first + (n * stride);
            Slice cer = new Slice(first, stop - 1, stride);
            slices.add(cer);
        }
        return slices;
    }

    /**
     * Test a List<Range> against a List<DapDimension>
     * to see if the range list represents the whole
     * set of dimensions within the specified indices.
     *
     * @param rangelist the set of ucar.ma2.Range
     * @param dimset    the set of DapDimensions
     * @param start     start looking here
     * @param stop      stop looking here
     * @result true if rangelist is whole; false otherwise.
     */

    static public boolean
    isWhole(List<Range> rangelist, List<DapDimension> dimset, int start, int stop)
        throws DapException
    {
        int rsize = (rangelist == null ? 0 : rangelist.size());
        if(rsize != dimset.size())
            throw new DapException("range/dimset rank mismatch");
        if(rsize == 0)
            return true;
        if(start < 0 || stop < start || stop > rsize)
            throw new DapException("Invalid start/stop indices");

        for(int i = start;i < stop;i++) {
            Range r = rangelist.get(i);
            DapDimension d = dimset.get(i);
            if(r.stride() != 1 || r.first() != 0 || r.length() != d.getSize())
                return false;
        }
        return true;
    }

    /**
     * Test a List<Range> against a List<Slice>
     * to see if the range list is whole
     * wrt the slices
     *
     * @param rangelist the set of ucar.ma2.Range
     * @param slices    the set of slices
     * @result true if rangelist is whole wrt slices; false otherwise.
     */
    static public boolean
    isWhole(List<Range> rangelist, List<Slice> slices)
        throws DapException
    {
        if(rangelist.size() != slices.size())
            return false;
        for(int i = 0;i < rangelist.size();i++) {
            Range r = rangelist.get(i);
            Slice slice = slices.get(i);
            if(r.stride() != 1 || r.first() != 0 || r.length() != slice.getCount())
                return false;
        }
        return true;
    }

    /**
     * Test a List<Range> against the CDM variable's dimensions
     * to see if the range list is whole
     * wrt the dimensions
     *
     * @param rangelist the set of ucar.ma2.Range
     * @param var    the cdm var
     * @result true if rangelist is whole wrt slices; false otherwise.
     */
    static public boolean
    isWhole(List<Range> rangelist, Variable var)
        throws DapException
    {
        List<Dimension> dimset = var.getDimensions();
        if(rangelist.size() != dimset.size())
            return false;
        for(int i = 0;i < rangelist.size();i++) {
            Range r = rangelist.get(i);
            Dimension dim = dimset.get(i);
            if(r.stride() != 1 || r.first() != 0 || r.length() != dim.getLength())
                return false;
        }
        return true;
    }

    static public List<ucar.ma2.Range>
    createCDMRanges(List<Slice> slices)
        throws IOException
    {
        List<ucar.ma2.Range> cdmranges = new ArrayList<Range>();
        for(int i = 0;i < slices.size();i++) {
            Slice r = slices.get(i);
            try {
                ucar.ma2.Range cmdr;
                cmdr = new ucar.ma2.Range((int) r.getFirst(),
                    (int) r.getLast(),
                    (int) r.getStride());
                cdmranges.add(cmdr);
            } catch (InvalidRangeException ire) {
                throw new IOException(ire);
            }
        }
        return cdmranges;
    }

    /**
     * NetcdfDataset can end up wrapping a variable
     * in multiple wrapping classes (e.g. VariableDS).
     * Goal of this procedure is to get down to the
     * lowest level Variable instance
     *
     * @param var possibly wrapped variable
     * @return the lowest level Variable instance
     */
    static public Variable unwrap(Variable var)
    {
/*        for(;;) {
            if(var instanceof VariableDS) {
                VariableDS vds = (VariableDS) var;
                var = vds.getOriginalVariable();
                if(var == null) {
                    var = vds;
                    break;
                }
            } else if(var instanceof StructureDS) {
                StructureDS sds = (StructureDS) var;
                var = sds.getOriginalVariable();
                if(var == null) {
                    var = sds;
                    break;
                }
            } else
                break;
        }
        return var;
        */
        return (Variable)CDMNode.unwrap(var);
    }

    /**
     * NetcdfDataset can wrap a NetcdfFile.
     * Goal of this procedure is to get down to the
     * lowest level NetcdfFile instance.
     *
     * @param file NetcdfFile or NetcdfDataset
     * @return the lowest level NetcdfFile instance
     */
    static public NetcdfFile unwrapfile(NetcdfFile file)
    {
        for(;;) {
            if(file instanceof NetcdfDataset) {
                NetcdfDataset ds = (NetcdfDataset) file;
                file = ds.getReferencedFile();
                if(file == null) break;
            } else break;
        }
        return file;
    }

    static public boolean
    hasVLEN(List<Range> ranges)
    {
        if(ranges == null || ranges.size() == 0) return false;
        return ranges.get(ranges.size() - 1) == Range.VLEN;
    }

    /**
     * Test if any dimension is variable length
     */
    static public boolean
    hasVLEN(Variable v)
    {
        for(Dimension dim : v.getDimensions()) {
            if(dim.isVariableLength())
                return true;
        }
        return false;
    }

    static public DataType
    enumtypefor(DapType dt)
    {
        switch (dt.getAtomicType()) {
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
        case Enum:
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            return enumtypefor(((DapEnum)dt).getBaseType());
        default:
            break;
        }
        return null;
    }


    static public DapType
    cdmtype2daptype(DataType datatype, boolean unsigned)
    {
        switch (datatype) {
        case CHAR:
            return DapType.CHAR;
        case BYTE:
            return (unsigned ? DapType.UINT8 : DapType.INT8);
        case SHORT:
            return (unsigned ? DapType.UINT16 : DapType.INT16);
        case INT:
            return (unsigned ? DapType.UINT32 : DapType.INT32);
        case LONG:
            return (unsigned ? DapType.UINT64 : DapType.INT64);
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
    daptype2cdmtype(DapType daptype)
    {
        AtomicType atomtype = daptype.getPrimitiveType();
        switch (atomtype) {
        case Char:
            return DataType.CHAR;
        case UInt8:
        case Int8:
            return DataType.BYTE;
        case Int16:
        case UInt16:
            return DataType.SHORT;
        case Int32:
        case UInt32:
            return DataType.INT;
        case Int64:
        case UInt64:
            return DataType.LONG;
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
            DapEnum dapenum = (DapEnum) daptype;
            switch (dapenum.getBaseType().getAtomicType()) {
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
        default:
            break;
        }
        return null;
    }

    /**
     * Compute the shape inferred from a set of slices.
     * 'Effective' means that any trailing vlen will be
     * ignored.
     *
     * @param dimset from which to generate shape
     * @return
     */
    static public int[]
    computeEffectiveShape(List<DapDimension> dimset)
    {
        if(dimset == null || dimset.size() == 0)
            return new int[0];
        int effectiverank = dimset.size();
        int[] shape = new int[effectiverank];
        for(int i = 0;i < effectiverank;i++) {
            shape[i] = (int) dimset.get(i).getSize();
        }
        return shape;
    }

    /*
    static public int
    computeVariableSize(View view, DapVariable var, boolean scalar)
    {
        ViewVariable annotation = view.getAnnotation(var);
        int dimproduct = (scalar ? 1 : computeDimProduct(annotation.getSlices()));
        int elementsize = 0;
        switch (var.getSort()) {
        case ATOMICVARIABLE:
	        // This does not work for String or Opaque.
            DapType dt = ((DapAtomicVariable) var).getBaseType();
            elementsize =  CDMUtil.daptypeSize(dt.getAtomicType());
            break;
        case STRUCTURE:
        case SEQUENCE:
        case GRID:
            for(DapVariable field : ((DapStructure) var).getFields()) {
                elementsize += computeVariableSize(dataset, field, false);
            }
            break;

        default:
            break;
        }
        return dimproduct * elementsize;
    }
    */

    /**
     * Extract, as an object, value from a (presumably)
     * atomic typed array of values; dataset position
     * is presumed correct.
     *
     * @param atomtype type of object to extract ; must not be Enum
     * @param dataset  D4Data containing the objects
     * @param index    Which element of dataset to read
     * @return resulting value as an Object; value does not necessarily conform
     *         to Convert.ValueClass.
     */

    static Object
    extractObject(AtomicType atomtype, D4DataAtomic dataset, long index)
        throws DataException
    {
        try {
            Object result = dataset.read(index);
            return result;
        } catch (IOException ioe) {
            throw new DataException(ioe);
        }
    }

    /**
     * Extract, as a long, value from a (presumably)
     * atomic typed array of values; dataset position
     * is presumed correct.
     *
     * @param atomtype type of object to extract
     * @param dataset  D4Data containing the objects
     * @param index    Which element of dataset to read
     * @return resulting value as a long
     * @throws ForbiddenConversionException if cannot convert to long
     */

    static public long
    extractLongValue(AtomicType atomtype, D4DataAtomic dataset, long index)
        throws DataException
    {
        Object result;
        try {
            result = dataset.read(index);
        } catch (IOException ioe) {
            throw new DataException(ioe);
        }
        long lvalue;
        switch (atomtype) {
        case Int8:
            lvalue = (long) ((Byte) result).byteValue();
            break;
        case Char:
        case UInt8:
            lvalue = (long) ((Byte) result).byteValue();
            lvalue = lvalue & 0xFFL;
            break;
        case Int16:
            lvalue = (long) ((Short) result).shortValue();
            break;
        case UInt16:
            lvalue = (long) ((Short) result).shortValue();
            lvalue = lvalue & 0xFFFFL;
            break;
        case Int32:
            lvalue = (long) ((Integer) result).intValue();
            break;
        case UInt32:
            lvalue = (long) ((Integer) result).intValue();
            lvalue = lvalue & 0xFFFFFFFFL;
            break;
        case Int64:
        case UInt64:
            lvalue = ((Long) result).longValue();
            break;
        case Float32:
            lvalue = (long) ((Float) result).floatValue();
            break;
        case Float64:
            lvalue = (long) ((Double) result).doubleValue();
            break;
        default:
            throw new ForbiddenConversionException("Type not convertible to long");
        }
        return lvalue;
    }

    /**
     * Extract, as a double, value from a (presumably)
     * atomic typed array of values; dataset position
     * is presumed correct.
     *
     * @param atomtype type of object to extract
     * @param dataset  D4Data containing the objects
     * @param index    Which element of dataset to read
     * @return resulting value as a double
     * @throws ForbiddenConversionException if cannot convert to double
     */

    static public double
    extractDoubleValue(AtomicType atomtype, D4DataAtomic dataset, int index)
        throws DataException
    {
        Object result;
        try {
            result = dataset.read(index);
        } catch (IOException ioe) {
            throw new DataException(ioe);
        }
        double dvalue = 0.0;
        if(atomtype.isIntegerType() || atomtype.isEnumType()) {
            long lvalue = extractLongValue(atomtype, dataset, index);
            dvalue = (double) lvalue;
        } else if(atomtype == AtomicType.Float32) {
            dvalue = (double) ((Float) result).floatValue();
        } else if(atomtype == AtomicType.Float64) {
            dvalue = ((Double) result).doubleValue();
        } else
            throw new ForbiddenConversionException();
        return dvalue;
    }

    /**
     * Extract, as an object, n consecutive values
     * of an atomic typed array of values
     *
     * @param dataset  D4Data containing the objects
     * @param index    Starting element to read
     * @param count    Number of elements to read
     * @return resulting array of values as an object
     */

    /*
    static public Object
    extractVector(D4DataAtomic dataset, long index, long count, long offset)
        throws DataException
    {
        Object vector = createVector(dataset.getType().getPrimitiveType(),count);
        try {
            dataset.read(index, count, vector, offset);
        } catch (IOException ioe) {
            throw new DataException(ioe);
        }
        return vector;
    }
    */

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
    {
        int i;

        AtomicType srcatomtype = srctype.getPrimitiveType();
        AtomicType dstatomtype = dsttype.getPrimitiveType();

        if(srcatomtype == dstatomtype) {
            return src;
        }
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
            throw new ForbiddenConversionException();
        }
        if(!ok)
            throw new ForbiddenConversionException();
        return result;
    }

    /**
     * Given an arbitrary Array (including ArrayStructure), produce
     * a new Array that represents the slice defined by the
     * section.  For now, we create a simple array of the relevant
     * type and fill it by extracting the values specified by the
     * section.
     *
     * param array   the array from which the section is extracted
     * param section determines what to extract
     * throws DapException
     * returns the slice array
     */
    /*static public ucar.ma2.Array
    arraySlice(ucar.ma2.Array array, Section section)
        throws DapException
    {
        // Case it out.
        if(!dapvar.getBaseType().isStructType()) { // =>Atomic type
            if(dapvar.isTopLevel()) {
                // Simplest case: use createview, but watch out for final VLEN
                List<Range> ranges = section.getRanges();
                try {
                    if(CDMUtil.hasVLEN(ranges))
                        return array.section(ranges.subList(0, ranges.size() - 2));
                    else
                        return array.section(ranges);
                } catch (InvalidRangeException ire) {
                    throw new DapException(ire);
                }
            } else
                throw new UnsupportedOperationException(); // same as other cdm
        } else { // struct type
            assert (array instanceof CDMArrayStructure);
            CDMArrayStructure struct = (CDMArrayStructure) array;
            if(dapvar.isTopLevel()) {
                // Build a new ArrayStructure containing
                // the relevant instances.
                int[] shape = section.getShape();
                StructureMembers sm = new StructureMembers(struct.getStructureMembers());
                ArrayStructureMA slice = new ArrayStructureMA(sm, shape);
                CDMOdometer odom = new CDMOdometer(dapvar.getDimensions(), section.getRanges());
                // Compute the number of structuredata instances we need
                long totalsize = section.computeSize();
                List<StructureMembers.Member> mlist = sm.getMembers();
                StructureData[] newdata = new StructureData[(int) totalsize];
                for(int i = 0;odom.hasNext();odom.next(), i++) {
                    long recno = odom.index();
                    StructureDataW clone = new StructureDataW(sm);
                    newdata[i] = clone;
                    StructureData record = struct.getStructureData((int) recno);
                    for(int j = 0;j < mlist.size();j++) {
                        StructureMembers.Member m = mlist.get(j);
                        clone.setMemberData(m, record.getArray(m));
                    }
                }
                return slice;
            } else
                throw new UnsupportedOperationException(); // same as other cdm
        }
    }*/

    static public Object
    createVector(AtomicType atype, long count)
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
            throw new ForbiddenConversionException();
        }
        return vector;
    }

    static public String
    getChecksumString(byte[] checksum)
    {
        StringBuilder buf = new StringBuilder();
        for(int i=0;i<checksum.length;i++) {
            byte b = checksum[i];
            buf.append(hexchars.charAt(b>>4));
            buf.append(hexchars.charAt(b & 0xF));
        }
        return buf.toString();
    }

    /**
     * Convert a Section + variable to a constraint


    static public View
    sectionToView(CDMDSP dsp, Variable v, Section section)
        throws DapException
    {
        if(section == null || section.getRank() == 0)
            return null;
        // Get the corresponding DapNode
        DapVariable dv = (DapVariable) dsp.getNode().get(v);
        if(dv == null)
            throw new DapException("Variable has no corresponding dap node: " + v.getFullName());
        // Get the structure path wrt DapDataset for dv
        // and use path plus the Section to construct a constraint
        List<DapVariable> structpath = DapUtil.getStructurePath(dv);
        List<Range> ranges = section.getRanges();
        View view = new View(dmr);
        int next = 0;
        for(int i = 0;i < structpath.size();i++) {
            dv = structpath.get(i);
            int rank = dv.getRank();
            ViewVariable vv = new ViewVariable(dv);
            List<Slice> slices = new ArrayList<Slice>(rank);
            for(int j = 0;j < rank;j++, next++) {
                if(next >= ranges.size())
                    throw new DapException("Range::Rank mismatch");
                Range range = ranges.get(next);
                Slice slice = new Slice(range.first(), range.last(), range.stride()).validate();
                slices.add(slice);
            }
            vv.setSlices(slices);
            view.put(dv, vv);
        }
        view.validate(View.EXPAND);
        return view;
    }   */


    static public List<Range>
    dimsetToRanges(List<DapDimension> dimset)
        throws DapException
    {
        if(dimset == null)
             return null;
        List<Range> ranges = new ArrayList<>();
        for(int i=0;i<dimset.size();i++) {
            DapDimension dim = dimset.get(i);
            try {
                Range r = new Range(dim.getShortName(), 0, (int) dim.getSize() - 1, 1);
                ranges.add(r);
            } catch(InvalidRangeException ire) {
                throw new DapException(ire);
            }
        }
        return ranges;
    }

    static public List<Slice>
    shapeToSlices(int[] shape)
        throws DapException
    {
        if(shape == null)
            return null;
        List<Slice> slices = new ArrayList<>(shape.length);
        for(int i=0;i<shape.length;i++) {
            Slice sl = new Slice(0,shape[i]-1,1);
            slices.add(sl);
        }
        return slices;
    }
}
