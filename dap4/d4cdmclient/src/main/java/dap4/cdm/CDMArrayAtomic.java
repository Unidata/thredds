/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package   dap4.cdm;

import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.cdmshared.CDMUtil;
import dap4.dap4shared.*;
import ucar.ma2.*;

import java.io.IOException;
import java.util.List;

/**
 * CDMArrayAtomic wraps a D4DataAtomic object to present
 * the ucar.ma2.Array interface.
 * CDMArrayAtomic manages a single CDM atomic variable:
 * either top-level or for a member.
 */

public class CDMArrayAtomic extends Array implements CDMArray
{
    /////////////////////////////////////////////////////
    // Constants

    /////////////////////////////////////////////////////
    // Instance variables

    // CDMArry variables
    protected CDMDataset root = null;
    protected D4DSP dsp = null;
    protected DapVariable template = null;
    protected long bytesize = 0;
    protected DapType basetype = null;
    protected AtomicType primitivetype = null;

    protected D4DataAtomic d4data = null;
    protected int elementsize = 0;    // of one element
    protected long dimsize = 0;        // # of elements in array; scalar uses value 1
    protected long totalsize = 0;      // elementsize*dimsize except when isbytestring

    // these two flags control use of fields following
    protected boolean isbytestring = false; // string and/or opaque

    // Following two fields only defined if isbytestring is true
    protected int[] bytestrings = null; // List of the absolute start positions of
    // an array of e.g. opaque,  or string atomictypes.
    // The value is the offset of object's count.

    //Coverity[FB.URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD]
    protected int totalbytestringsize = 0;  // total space used by the bytestrings

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * Constructor
     *
     * @param dsp    the parent DSP
     * @param d4data the dap4 databuffer object that provided the actual databuffer
     */
    CDMArrayAtomic(D4DSP dsp, CDMDataset root, D4DataAtomic d4data)
    {
        super(CDMUtil.computeEffectiveShape(((DapVariable) d4data.getTemplate()).getDimensions()));
        this.dsp = dsp;
        this.root = root;
        this.d4data = d4data;
        this.template = (DapVariable) d4data.getTemplate();
        this.basetype = this.template.getBaseType();
        this.primitivetype = this.basetype.getPrimitiveType();

        this.isbytestring = (primitivetype.isStringType() || primitivetype.isOpaqueType());
        super.setUnsigned(basetype.isUnsigned());
        this.dimsize = DapUtil.dimProduct(this.template.getDimensions());
        this.elementsize = Dap4Util.daptypeSize(this.primitivetype);

        this.bytesize = computeTotalSize();
    }

    //////////////////////////////////////////////////
    // CDMArray Interface

    @Override
    public DSP getDSP()
    {
        return dsp;
    }

    @Override
    public CDMDataset getRoot()
    {
        return root;
    }

    @Override
    public DapVariable getTemplate()
    {
        return template;
    }

    @Override
    public long getByteSize()
    {
        return bytesize;
    }

    @Override
    public DapType getBaseType()
    {
        return this.basetype;
    }

    @Override
    public AtomicType getPrimitiveType()
    {
        return this.primitivetype;
    }

    //////////////////////////////////////////////////
    // Accessors

    @Override
    public long getSize()
    {
        return dimsize;
    }

    public D4DataAtomic getData()
    {
        return d4data;
    }

    //////////////////////////////////////////////////
    // Utilities

    void
    setup(int index)
    {
        if(index < 0 || index > dimsize)
            throw new IndexOutOfBoundsException("Dap4Array: " + index);
    }

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

    protected long
    computeTotalSize()
    {
        if(isbytestring)
            totalsize = totalbytestringsize;
        else
            totalsize = elementsize * dimsize;
        return totalsize;
    }

    //////////////////////////////////////////////////
    // Array API
    // TODO: add index range checks

    public Class
    getElementType()
    {
        DataType dt = CDMUtil.daptype2cdmtype(this.basetype);
        if(dt == null)
            throw new IllegalArgumentException("Unknown datatype: "+this.basetype);
        return CDMUtil.cdmElementClass(dt);
    }

    /**
     * Get the array element at a specific index as a double
     *
     * @param index of element to get
     * @return value at <code>index</code> cast to double if necessary.
     */
    public double getDouble(int index)
    {
        setup(index);
        try {
            Object value = d4data.read(index);
            return Convert.doubleValue(this.basetype, value);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific index as a float
     * converting as needed.
     *
     * @param index of element to get
     * @return value at <code>index</code> cast to float if necessary.
     */
    public float getFloat(int index)
    {
        setup(index);
        try {
            Object value = d4data.read(index);
            return (float) Convert.doubleValue(this.basetype, value);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific index as a long
     *
     * @param index of element to get
     * @return value at <code>index</code> cast to long if necessary.
     */
    public long getLong(int index)
    {
        setup(index);
        try {
            Object value = d4data.read(index);
            return Convert.longValue(this.basetype, value);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific index as a integer
     *
     * @param index of element to get
     * @return value at <code>index</code> cast to integer if necessary.
     */
    public int getInt(int index)
    {
        setup(index);
        try {
            Object value = d4data.read(index);
            return (int) Convert.longValue(this.basetype, value);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific index as a short
     *
     * @param index of element to get
     * @return value at <code>index</code> cast to short if necessary.
     */
    public short getShort(int index)
    {
        setup(index);
        try {
            Object value = d4data.read(index);
            return (short) Convert.longValue(this.basetype, value);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific index as a byte
     *
     * @param index of element to get
     * @return value at <code>index</code> cast to byte if necessary.
     */
    public byte getByte(int index)
    {
        setup(index);
        try {
            Object value = d4data.read(index);
            return (byte) (Convert.longValue(basetype, value) & 0xFFL);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific index as a char
     *
     * @param index of element to get
     * @return value at <code>index</code> cast to char if necessary.
     */
    public char getChar(int index)
    {
        setup(index);
        try {
            Object value = d4data.read(index);
            return (char) (Convert.longValue(basetype, value) & 0xFFL);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific index as a boolean
     *
     * @param index of element to get
     * @return value at <code>index</code> cast to char if necessary.
     */
    public boolean getBoolean(int index)
    {
        setup(index);
        try {
            Object value = d4data.read(index);
            return (Convert.longValue(basetype, value) != 0);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    /**
     * Get the array element at a specific index as an Object
     *
     * @param index of element to get
     * @return value at <code>index</code> cast to Object if necessary.
     */
    public Object getObject(int index)
    {
        setup(index);
        try {
            return d4data.read(index);
        } catch (IOException ioe) {
            throw new IndexOutOfBoundsException(ioe.getMessage());
        }
    }

    // Convert index base to int based
    public double getDouble(Index idx)
    {
        return getDouble((int) (idx.currentElement()));
    }

    public float getFloat(Index idx)
    {
        return getFloat((int) (idx.currentElement()));
    }

    public long getLong(Index idx)
    {
        return getLong((int) (idx.currentElement()));
    }

    public int getInt(Index idx)
    {
        return getInt((int) (idx.currentElement()));
    }

    public short getShort(Index idx)
    {
        return getShort((int) (idx.currentElement()));
    }

    public byte getByte(Index idx)
    {
        return getByte((int) (idx.currentElement()));
    }

    public char getChar(Index idx)
    {
        return getChar((int) (idx.currentElement()));
    }

    public boolean getBoolean(Index idx)
    {
        return getBoolean((int) (idx.currentElement()));
    }

    public Object getObject(Index idx)
    {
        return getObject((int) (idx.currentElement()));
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

    public Object getStorage()
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
    // Extended interface to support array extraction

    /**
     * Extract a java array of
     * this.basetype and
     * convert it to the specified
     * dsttype, and return the resulting
     * array as an Object.
     *
     * @param dsttype Return a java array of these.
     * @param dimsize Return this many consecutive elements
     * @return a java array corresponding to dsttype
     */
/*
    public Object
    getArray(DapType dsttype, int dimsize)
        throws DataException
    {
        AtomicType srctype = this.basetype.getPrimitiveType();
        AtomicType dstatomtype = dsttype.getPrimitiveType();
        Object array =
            Dap4Util.extractVector(this.d4data, 0, dimsize);
        if(dstatomtype != srctype) {
            // dst type and src type differ => Convert
            array = CDMUtil.convertVector(dsttype, basetype, array);
        }
        return array;
    }
    */

}

