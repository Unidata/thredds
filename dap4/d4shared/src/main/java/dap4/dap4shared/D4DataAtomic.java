/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.*;
import dap4.core.util.*;

import java.nio.ByteBuffer;

public class D4DataAtomic extends D4DataVariable implements DataAtomic
{
    //////////////////////////////////////////////////
    // Instance variables

    protected D4DSP dsp = null;
    protected long product = 0; // dimension cross product; 0 => undefined
    protected DapType basetype = null;
    protected AtomicType atomictype = null;
    protected boolean isscalar = false;
    protected int offset = -1; // absolute offset of the start in bytebyffer.
    protected ByteBuffer databuffer = null;
    protected int elementsize = 0;
    protected boolean isbytestring = false;
    // Following two fields only defined if isbytestring is true
    protected long totalbytestringsize = 0;  // total space used by the bytestrings
    protected int[] bytestrings = null; // List of the absolute start offsets of
    // an array of e.g. opaque,  or string atomictypes.
    // The value is the offset of object's count.

    //////////////////////////////////////////////////
    // Constructors

    public D4DataAtomic(D4DSP dsp, DapAtomicVariable dap, int offset)
        throws DataException
    {
        super(dsp, dap);
        this.dsp = dsp;
        this.databuffer = dsp.getDatabuffer();
        this.offset = offset;
        this.basetype = dap.getBaseType();
        this.atomictype = this.basetype.getPrimitiveType();
        this.product = DapUtil.dimProduct(dap.getDimensions());
        this.elementsize = Dap4Util.daptypeSize(this.basetype.getPrimitiveType());
        this.isbytestring = (this.atomictype.isStringType() || this.atomictype.isOpaqueType());
    }

    //////////////////////////////////////////////////
    // D4DataAtomic Specific API (should all be package accessible only)

    /* Computed by the DataCompiler */
    void
    setByteStringOffsets(long totalsize, int[] offsets)
    {
        this.totalbytestringsize = totalsize;
        this.bytestrings = offsets;
    }

    //////////////////////////////////////////////////
    // DataAtomic Interface

    @Override
    public DapType getType()
    {
        return this.basetype;
    }

    @Override
    public long getCount() // dimension cross-product
    {
        return this.product;
    }

    @Override
    public long getElementSize()
    {
        return 0;
    }

    @Override
    public void
    read(long start, long count, Object data)
        throws DataException
    {
        extractObjectVector(basetype, databuffer, start, count, data);
    }

    @Override
    public Object
    read(long index)
        throws DataException
    {
        setup(index);
        Object result = extractObject(basetype, databuffer);
        return result;
    }

    //////////////////////////////////////////////////
    // Utilities

    protected void
    setup(long index)
    {
        if(index < 0 || index > this.product)
            throw new IndexOutOfBoundsException("D4DataAtomic: " + index);
        if(isbytestring) {
            databuffer.position(bytestrings[(int) index]);
        } else
            databuffer.position((int) (offset + (elementsize * index)));
    }

    /**
     * Extract, as an object, value from a (presumably)
     * atomic typed array of values; dataset position
     * is presumed correct.
     *
     * @param basetype type of object to extract
     * @param dataset  ByteBuffer containing databuffer; position assumed correct
     * @return resulting value as an Object; value does not necessarily conform
     *         to Convert.ValueClass.
     */

    static protected Object
    extractObject(DapType basetype, ByteBuffer dataset)
    {
        Object result = null;
        long lvalue = 0;
        AtomicType atomtype = basetype.getPrimitiveType();
        switch (atomtype) {
        case Char:
            lvalue = dataset.get();
            lvalue &= 0xFFL; // make unsigned
            result = new Character((char) lvalue);
            break;
        case UInt8:
        case Int8:
            result = new Byte(dataset.get());
            break;
        case Int16:
        case UInt16:
            result = new Short(dataset.getShort());
            break;
        case Int32:
        case UInt32:
            result = new Integer(dataset.getInt());
            break;
        case Int64:
        case UInt64:
            result = new Long(dataset.getLong());
            break;
        case Float32:
            result = new Float(dataset.getFloat());
            break;
        case Float64:
            result = new Double(dataset.getDouble());
            break;
        case String:
        case URL:
            long count = dataset.getLong();
            byte[] bytes = new byte[(int) count];
            dataset.get(bytes);
            result = new String(bytes, DapUtil.UTF8);
            break;
        case Opaque:
            count = dataset.getLong();
            bytes = new byte[(int) count];
            dataset.get(bytes);
            result = ByteBuffer.wrap(bytes);
            break;
        case Enum:
            // recast as enum's basetype
            result = extractObject(((DapEnum) basetype).getBaseType(), dataset);
            break;
        }
        return result;
    }

    /**
     * Vector version of extractObject().
     * Extract a vector of objects from a (presumably)
     * atomic typed array of values; dataset position
     * is presumed correct.
     *
     * @param basetype type of object to extract ; must not be Enum
     * @param dataset  ByteBuffer containing databuffer; position assumed correct
     * @param vector
     * @param count
     */

    static protected void
    extractObjectVector(DapType basetype, ByteBuffer dataset,
                        long start, long count, Object vector)
    {
        long position = dataset.position();
        AtomicType atomtype = basetype.getPrimitiveType();
        long elemsize = AtomicType.getSize(atomtype);
        long offset = elemsize * start;
        long extent = elemsize * count;
        // If this is a fixed size type, then we can immediately
        // position the buffer
        if(atomtype.isFixedSize() && !atomtype.isEnumType())
            dataset.position((int) offset);
        switch (atomtype) {
        case Char:
            char[] cresult = (char[]) vector;
            dataset.asCharBuffer().get(cresult, 0, (int) count);
            break;
        case UInt8:
        case Int8:
            byte[] byresult = (byte[]) vector;
            dataset.get(byresult, 0, (int) count);
            // in order to maintain the rule at the end of the switch
            // reset the position.
            dataset.position((int) offset);
            break;
        case Int16:
        case UInt16:
            short[] shresult = (short[]) vector;
            dataset.asShortBuffer().get(shresult, 0, (int) count);
            break;
        case Int32:
        case UInt32:
            int[] iresult = (int[]) vector;
            dataset.asIntBuffer().get(iresult, 0, (int) count);
            break;
        case Int64:
        case UInt64:
            long[] lresult = (long[]) vector;
            dataset.asLongBuffer().get(lresult, 0, (int) count);
            break;
        case Float32:
            float[] fresult = (float[]) vector;
            dataset.asFloatBuffer().get(fresult, 0, (int) count);
            break;
        case Float64:
            double[] dresult = (double[]) vector;
            dataset.asDoubleBuffer().get(dresult, 0, (int) count);
            break;
        case String:
        case URL:
            String[] sresult = (String[]) vector;
            for(int i = 0;i < count;i++) {
                long scount = dataset.getLong();
                byte[] bytes = new byte[(int) count];
                dataset.get(bytes);
                sresult[i] = new String(bytes, DapUtil.UTF8);
            }
            break;
        case Opaque:
            ByteBuffer[] oresult = (ByteBuffer[]) vector;
            for(int i = 0;i < count;i++) {
                long ocount = dataset.getLong();
                byte[] bytes = new byte[(int) count];
                dataset.get(bytes);
                oresult[i] = ByteBuffer.wrap(bytes);
            }
            break;
        case Enum:
            // recast as enum's basetype
            extractObjectVector(((DapEnum) basetype).getBaseType(), dataset, start, count, vector);
            break;
        }
        // If this is a fixed size type (with exceptions),
        // then we can immediately
        // position the buffer past the read data
        if(atomtype.isFixedSize() && !atomtype.isEnumType())
            dataset.position((int) (offset + extent));
    }

}

