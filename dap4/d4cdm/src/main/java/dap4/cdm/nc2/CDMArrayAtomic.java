/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm.nc2;

import dap4.cdm.CDMTypeFcns;
import dap4.cdm.CDMUtil;
import dap4.core.data.DSP;
import dap4.core.data.DataCursor;
import dap4.core.dmr.DapType;
import dap4.core.dmr.DapVariable;
import dap4.core.util.Convert;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.core.util.Slice;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.nc2.Group;

import java.io.IOException;
import java.util.List;

import static dap4.core.data.DataCursor.Scheme;

/**
 * CDMArrayAtomic wraps a DataCursor object to present
 * the ucar.ma2.Array interface.
 * CDMArrayAtomic manages a single CDM atomic variable:
 * either top-level or for a member.
 */

/*package*/ class CDMArrayAtomic extends Array implements CDMArray
{
    /////////////////////////////////////////////////////
    // Constants

    /////////////////////////////////////////////////////
    // Instance variables

    protected DSP dsp = null;
    protected DapVariable template = null;
    protected DapType basetype = null;

    // CDMArray variables
    protected DataCursor data = null;
    protected Group cdmroot = null;
    protected int elementsize = 0;    // of one element
    protected long dimsize = 0;        // # of elements in array; scalar uses value 1
    protected long totalsize = 0;      // elementsize*dimsize except when isbytestring

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * Constructor
     *
     * @param data DataCursor object providing the actual data
     */
    CDMArrayAtomic(DataCursor data)
            throws DapException
    {
        super(CDMTypeFcns.daptype2cdmtype(((DapVariable) data.getTemplate()).getBaseType()),
                CDMUtil.computeEffectiveShape(((DapVariable) data.getTemplate()).getDimensions()));
        this.dsp = data.getDSP();
        this.data = data;
        this.template = (DapVariable) this.data.getTemplate();
        this.basetype = this.template.getBaseType();

        this.dimsize = DapUtil.dimProduct(this.template.getDimensions());
        this.elementsize = this.basetype.getSize();
    }

    /////////////////////////////////////////////////
    // CDMArray Interface

    @Override
    public DapType
    getBaseType()
    {
        return this.basetype;
    }

    @Override
    public DSP getDSP()
    {
        return this.dsp;
    }

    @Override
    public DapVariable getTemplate()
    {
        return this.template;
    }

    //////////////////////////////////////////////////
    // Accessors

    //////////////////////////////////////////////////
    // Array Interface

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        DapType basetype = getBaseType();
        String sbt = (basetype == null ? "?" : basetype.toString());
        String st = (template == null ? "?" : template.getShortName());
        buf.append(String.format("%s %s[%d]", sbt, st, dimsize));
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // Array API
    // TODO: add index range checks

    public Class
    getElementType()
    {
        DataType dt = CDMTypeFcns.daptype2cdmtype(this.basetype);
        if(dt == null)
            throw new IllegalArgumentException("Unknown datatype: " + this.basetype);
        return CDMTypeFcns.cdmElementClass(dt);
    }

    public double getDouble(ucar.ma2.Index cdmidx)
    {
        return getDouble(CDMUtil.cdmIndexToIndex(cdmidx));
    }

    public float getFloat(ucar.ma2.Index cdmidx)
    {
        return getFloat(CDMUtil.cdmIndexToIndex(cdmidx));
    }

    public long getLong(ucar.ma2.Index cdmidx)
    {
        return getLong(CDMUtil.cdmIndexToIndex(cdmidx));
    }

    public int getInt(ucar.ma2.Index cdmidx)
    {
        return getInt(CDMUtil.cdmIndexToIndex(cdmidx));
    }

    public short getShort(ucar.ma2.Index cdmidx)
    {
        return getShort(CDMUtil.cdmIndexToIndex(cdmidx));
    }

    public byte getByte(ucar.ma2.Index cdmidx)
    {
        return getByte(CDMUtil.cdmIndexToIndex(cdmidx));
    }

    public char getChar(ucar.ma2.Index cdmidx)
    {
        return getChar(CDMUtil.cdmIndexToIndex(cdmidx));
    }

    public boolean getBoolean(ucar.ma2.Index cdmidx)
    {
        return getBoolean(CDMUtil.cdmIndexToIndex(cdmidx));
    }

    public Object getObject(ucar.ma2.Index cdmidx)
    {
        return getObject(CDMUtil.cdmIndexToIndex(cdmidx));
    }

    // Convert int base to Index based

    public double getDouble(int offset)
    {
        DapVariable d4var = (DapVariable) getTemplate();
        long[] dimsizes = DapUtil.getDimSizes(d4var.getDimensions());
        return getDouble(DapUtil.offsetToIndex(offset, dimsizes));
    }

    public float getFloat(int offset)
    {
        DapVariable d4var = (DapVariable) getTemplate();
        long[] dimsizes = DapUtil.getDimSizes(d4var.getDimensions());
        return getFloat(DapUtil.offsetToIndex(offset, dimsizes));
    }

    public long getLong(int offset)
    {
        DapVariable d4var = (DapVariable) getTemplate();
        long[] dimsizes = DapUtil.getDimSizes(d4var.getDimensions());
        return getLong(DapUtil.offsetToIndex(offset, dimsizes));
    }

    public int getInt(int offset)
    {
        DapVariable d4var = (DapVariable) getTemplate();
        long[] dimsizes = DapUtil.getDimSizes(d4var.getDimensions());
        return getInt(DapUtil.offsetToIndex(offset, dimsizes));
    }

    public short getShort(int offset)
    {
        DapVariable d4var = (DapVariable) getTemplate();
        long[] dimsizes = DapUtil.getDimSizes(d4var.getDimensions());
        return getShort(DapUtil.offsetToIndex(offset, dimsizes));
    }

    public byte getByte(int offset)
    {
        DapVariable d4var = (DapVariable) getTemplate();
        long[] dimsizes = DapUtil.getDimSizes(d4var.getDimensions());
        return getByte(DapUtil.offsetToIndex(offset, dimsizes));
    }

    public char getChar(int offset)
    {
        DapVariable d4var = (DapVariable) getTemplate();
        long[] dimsizes = DapUtil.getDimSizes(d4var.getDimensions());
        return getChar(DapUtil.offsetToIndex(offset, dimsizes));
    }

    public boolean getBoolean(int offset)
    {
        DapVariable d4var = (DapVariable) getTemplate();
        long[] dimsizes = DapUtil.getDimSizes(d4var.getDimensions());
        return getBoolean(DapUtil.offsetToIndex(offset, dimsizes));
    }

    public Object getObject(int offset)
    {
        DapVariable d4var = (DapVariable) getTemplate();
        long[] dimsizes = DapUtil.getDimSizes(d4var.getDimensions());
        return getObject(DapUtil.offsetToIndex(offset, dimsizes));
    }

    public Object getStorage()
    {
        try {
            List<Slice> slices = DapUtil.dimsetToSlices(this.template.getDimensions());
            Object result = this.data.read(slices);
            return result;
        } catch (DapException e) {
            throw new IllegalArgumentException();
        }
    }


    // Unsupported Methods

    public void setDouble(Index ima, double value)
    {
        throw new UnsupportedOperationException();
    }

    public void setFloat(Index ima, float value)
    {
        throw new UnsupportedOperationException();
    }

    public void setLong(Index ima, long value)
    {
        throw new UnsupportedOperationException();
    }

    public void setInt(Index ima, int value)
    {
        throw new UnsupportedOperationException();
    }

    public void setShort(Index ima, short value)
    {
        throw new UnsupportedOperationException();
    }

    public void setByte(Index ima, byte value)
    {
        throw new UnsupportedOperationException();
    }

    public void setChar(Index ima, char value)
    {
        throw new UnsupportedOperationException();
    }

    public void setBoolean(Index ima, boolean value)
    {
        throw new UnsupportedOperationException();
    }

    public void setObject(Index ima, Object value)
    {
        throw new UnsupportedOperationException();
    }

    public void setDouble(int elem, double value)
    {
        throw new UnsupportedOperationException();
    }

    public void setFloat(int elem, float value)
    {
        throw new UnsupportedOperationException();
    }

    public void setLong(int elem, long value)
    {
        throw new UnsupportedOperationException();
    }

    public void setInt(int elem, int value)
    {
        throw new UnsupportedOperationException();
    }

    public void setShort(int elem, short value)
    {
        throw new UnsupportedOperationException();
    }

    public void setByte(int elem, byte value)
    {
        throw new UnsupportedOperationException();
    }

    public void setChar(int elem, char value)
    {
        throw new UnsupportedOperationException();
    }

    public void setBoolean(int elem, boolean value)
    {
        throw new UnsupportedOperationException();
    }

    public void setObject(int elem, Object value)
    {
        throw new UnsupportedOperationException();
    }

    protected void copyTo1DJavaArray(IndexIterator indexIterator, Object o)
    {
        throw new UnsupportedOperationException();
    }

    protected void copyFrom1DJavaArray(IndexIterator iter, Object javaArray)
    {
        throw new UnsupportedOperationException();
    }

    protected Array createView(Index index)
    {
        return this;
    }

    //////////////////////////////////////////////////
    // Internal common extractors

    /**
     * Get the array element at a specific dap4 index as a double
     *
     * @param idx of element to get
     * @return value at <code>index</code> cast to double if necessary.
     */
    protected double getDouble(dap4.core.util.Index idx)
    {
        assert data.getScheme() == Scheme.ATOMIC;
        try {
            Object value = data.read(idx);
            value = Convert.convert(DapType.FLOAT64, this.basetype, value);
            return (Double) java.lang.reflect.Array.get(value, 0);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific dap4 index as a float
     * converting as needed.
     *
     * @param idx of element to get
     * @return value at <code>index</code> cast to float if necessary.
     */
    protected float getFloat(dap4.core.util.Index idx)
    {
        assert data.getScheme() == Scheme.ATOMIC;
        try {
            Object value = data.read(idx);
            value = Convert.convert(DapType.FLOAT32, this.basetype, value);
            return (Float) java.lang.reflect.Array.get(value, 0);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific dap4 index as a long
     *
     * @param idx of element to get
     * @return value at <code>index</code> cast to long if necessary.
     */
    protected long getLong(dap4.core.util.Index idx)
    {
        assert data.getScheme() == Scheme.ATOMIC;
        try {
            Object value = data.read(idx);
            value = Convert.convert(DapType.INT64, this.basetype, value);
            return (Long) java.lang.reflect.Array.get(value, 0);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific dap4 index as a integer
     *
     * @param idx of element to get
     * @return value at <code>index</code> cast to integer if necessary.
     */
    protected int getInt(dap4.core.util.Index idx)
    {
        assert data.getScheme() == Scheme.ATOMIC;
        try {
            Object value = data.read(idx);
            value = Convert.convert(DapType.INT32, this.basetype, value);
            return (Integer) java.lang.reflect.Array.get(value, 0);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific dap4 index as a short
     *
     * @param idx of element to get
     * @return value at <code>index</code> cast to short if necessary.
     */
    protected short getShort(dap4.core.util.Index idx)
    {
        assert data.getScheme() == Scheme.ATOMIC;
        try {
            Object value = data.read(idx);
            value = Convert.convert(DapType.INT16, this.basetype, value);
            return (Short) java.lang.reflect.Array.get(value, 0);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific dap4 index as a byte
     *
     * @param idx of element to get
     * @return value at <code>index</code> cast to byte if necessary.
     */
    protected byte getByte(dap4.core.util.Index idx)
    {
        assert data.getScheme() == Scheme.ATOMIC;
        try {
            Object value = data.read(idx);
            value = Convert.convert(DapType.INT8, this.basetype, value);
            return (Byte) java.lang.reflect.Array.get(value, 0);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific dap4 index as a char
     *
     * @param idx of element to get
     * @return value at <code>index</code> cast to char if necessary.
     */
    protected char getChar(dap4.core.util.Index idx)
    {
        assert data.getScheme() == Scheme.ATOMIC;
        try {
            Object value = data.read(idx);
            value = Convert.convert(DapType.CHAR, this.basetype, value);
            return (Character) java.lang.reflect.Array.get(value, 0);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific dap4 index as a boolean
     *
     * @param idx of element to get
     * @return value at <code>index</code> cast to char if necessary.
     */
    protected boolean getBoolean(dap4.core.util.Index idx)
    {
        assert data.getScheme() == Scheme.ATOMIC;
        try {
            Object value = data.read(idx);
            value = Convert.convert(DapType.INT64, this.basetype, value);
            return ((Long) java.lang.reflect.Array.get(value, 0)) != 0;
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific dap4 index as an Object
     *
     * @param idx of element to get
     * @return value at <code>index</code> cast to Object if necessary.
     */
    protected Object getObject(dap4.core.util.Index idx)
    {
        assert data.getScheme() == Scheme.ATOMIC;
        try {
            Object value = data.read(idx);
            value = java.lang.reflect.Array.get(value, 0);
            return value;
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }


    //////////////////////////////////////////////////
    // DataAtomic Interface

    public DapVariable getVariable()
    {
        return this.template;
    }

    public DapType getType()
    {
        return this.basetype;
    }

    /*
    protected Object
    read(long index, DapType datatype, DataAtomic content)
            throws DapException
    {
        Object result;
        int i = (int) index;
        long tmp = 0;
        switch (datatype.getTypeSort()) {
        case Int8:
            result = (Byte) content.getByte(i);
            break;
        case Char:
            result = (Character) content.getChar(i);
            break;
        case SHORT:
            result = (Short) content.getShort(i);
            break;
        case INT:
            result = (Integer) content.getInt(i);
            break;
        case LONG:
            result = (Long) content.getLong(i);
            break;
        case FLOAT:
            result = (Float) content.getFloat(i);
            break;
        case DOUBLE:
            result = (Double) content.getDouble(i);
            break;
        case STRING:
            result = content.getObject(i).toString();
            break;
        case OBJECT:
            result = content.getObject(i);
            break;
        case UBYTE:
            tmp = content.getByte(i) & 0xFF;
            result = (Byte) (byte) tmp;
            break;
        case USHORT:
            tmp = content.getShort(i) & 0xFFFF;
            result = (Short) (short) tmp;
            break;
        case UINT:
            tmp = content.getInt(i) & 0xFFFFFFFF;
            result = (Integer) (int) tmp;
            break;
        case ULONG:
            result = (Long) content.getLong(i);
            break;
        case ENUM1:
            result = read(index, DataType.BYTE, content);
            break;
        case ENUM2:
            result = read(index, DataType.SHORT, content);
            break;
        case ENUM4:
            result = read(index, DataType.INT, content);
            break;
        case OPAQUE:
            result = content.getObject(i);
            break;
        case STRUCTURE:
        case SEQUENCE:
        default:
            throw new DapException("Attempt to read non-atomic value of type: " + datatype);
        }
        return result;
    }
    */

    //////////////////////////////////////////////////
    // Utilities
}

