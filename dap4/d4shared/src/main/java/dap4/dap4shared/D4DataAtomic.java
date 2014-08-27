/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.*;
import dap4.core.util.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.List;

public class D4DataAtomic extends D4DataVariable implements DataAtomic
{
    //////////////////////////////////////////////////
    // Instance variables

    protected long product = 0; // dimension cross product; 0 => undefined
    protected DapType basetype = null;
    protected AtomicType atomictype = null;
    protected boolean isscalar = false;
    protected long varoffset = -1; // absolute offset of the start in bytebyffer.
    protected long varelementsize = 0;
    protected boolean isbytestring = false;
    // Following two fields only defined if isbytestring is true
    //Coverity[FB.URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD]
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
        this.varoffset = offset;
        this.basetype = dap.getBaseType();
        this.atomictype = this.basetype.getPrimitiveType();
        this.product = DapUtil.dimProduct(dap.getDimensions());
        this.varelementsize = Dap4Util.daptypeSize(this.basetype.getPrimitiveType());
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
    read(List<Slice> slices, Object data, long offset)
        throws DataException
    {
        if(slices == null || slices.size() == 0) { // scalar
            extractObjectVector(this.basetype, this.dsp.getData(), 0, 1, data, 0);
        } else {// dimensioned
            boolean contig = DapUtil.isContiguous(slices);
            Odometer odom;
            try {
                odom = Odometer.factory(slices,
                    ((DapVariable) this.getTemplate()).getDimensions(),
                    contig);
            } catch (DapException de) {
                throw new DataException(de);
            }
            long localoffset = offset;
            if(odom.isContiguous()) {
                List<Slice> pieces = odom.getContiguous();
                assert pieces.size() == 1;  // temporary
                Slice lastslice = pieces.get(0);
                assert lastslice.getStride() == 1;
                long first = lastslice.getFirst();
                long extent = lastslice.getCount();
                while(odom.hasNext()) {
                    long index = odom.next();
                    extractObjectVector(this.basetype, this.dsp.getData(), index+first, extent, data, localoffset);
                    localoffset += extent;
                }
            } else { // read one by one
                while(odom.hasNext()) {
                    long index = odom.next();
                    extractObjectVector(this.basetype, this.dsp.getData(), index, 1, data, localoffset);
                    localoffset++;
                }
            }
        }
    }

    /*protected void
    read(long start, long count, Object data, long offset) throws DataException;

    extractObjectVector(basetype, this.dsp.getData(), start, count, data, offset);
     */

    @Override
    public Object
    read(long index)
            throws DataException
    {
        Object result = extractObject(basetype, this.dsp.getData(), index);
        return result;
    }

    //////////////////////////////////////////////////
    // Utilities

    protected void
    setup(long index)
    {
        ByteBuffer databuffer = this.dsp.getData();
        if(index < 0 || index > this.product)
            throw new IndexOutOfBoundsException("D4DataAtomic: " + index);
        if(isbytestring) {
            databuffer.position(bytestrings[(int) index]);
        } else
            databuffer.position((int) (this.varoffset + (this.varelementsize * index)));
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

    protected Object
    extractObject(DapType basetype, ByteBuffer dataset, long index)
    {
        Object result = null;
        long lvalue = 0;
        AtomicType atomtype = basetype.getPrimitiveType();
        setup(index);
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
            result = ByteBuffer.wrap(bytes); // order is irrelevant
            break;
        case Enum:
            // recast as enum's basetype
            result = extractObject(((DapEnum) basetype).getBaseType(), dataset, index);
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

    protected void
    extractObjectVector(DapType basetype, ByteBuffer dataset,
                        long start, long count, Object vector, long offset)
    {
        int ioffset = (int) offset;
        long position = dataset.position();
        AtomicType atomtype = basetype.getPrimitiveType();
        setup(start);
        //long elemsize = AtomicType.getSize(atomtype);
        long elemsize = this.varelementsize;
        //long localoffset = this.varoffset + (elemsize * start);
        //long extent = elemsize * count;
        // If this is a fixed size type, then we can immediately
        // position the buffer
        /*if(atomtype.isFixedSize() && !atomtype.isEnumType()) {
            dataset.position((int) localoffset);
        }*/
        switch (atomtype) {
        case Char:
            // need to extract and convert utf8(really ascii) -> utf16
            char[] cresult = (char[]) vector;
            for(int i = 0; i < count; i++) {
                int ascii = dataset.get();
                ascii = ascii & 0x7F;
                cresult[ioffset + i] = (char) ascii;
            }
            break;
        case UInt8:
        case Int8:
            byte[] byresult = (byte[]) vector;
            dataset.get(byresult, ioffset, (int) count);
            // in order to maintain the rule at the end of the switch
            // reset the position.
            //dataset.position((int) localoffset);
            break;
        case Int16:
        case UInt16:
            short[] shresult = (short[]) vector;
            dataset.asShortBuffer().get(shresult, ioffset, (int) count);
            break;
        case Int32:
        case UInt32:
            int[] iresult = (int[]) vector;
            dataset.asIntBuffer().get(iresult, ioffset, (int) count);
            break;
        case Int64:
        case UInt64:
            long[] lresult = (long[]) vector;
            dataset.asLongBuffer().get(lresult, ioffset, (int) count);
            break;
        case Float32:
            float[] fresult = (float[]) vector;
            dataset.asFloatBuffer().get(fresult, ioffset, (int) count);
            break;
        case Float64:
            double[] dresult = (double[]) vector;
            dataset.asDoubleBuffer().get(dresult, ioffset, (int) count);
            break;
        case String:
        case URL:
            String[] sresult = (String[]) vector;
            for(int i = 0; i < count; i++) {
                dataset.position(bytestrings[(int) start + i]);
                long scount = dataset.getLong();
                byte[] bytes = new byte[(int) scount];
                dataset.get(bytes);
                sresult[ioffset + i] = new String(bytes, DapUtil.UTF8);
            }
            break;
        case Opaque:
            ByteBuffer[] oresult = (ByteBuffer[]) vector;
            for(int i = 0; i < count; i++) {
                dataset.position(bytestrings[(int) start + i]);
                long scount = dataset.getLong();
                byte[] bytes = new byte[(int) scount];
                dataset.get(bytes);
                oresult[ioffset + i] = ByteBuffer.wrap(bytes);
            }
            /*for(int i = 0;i < count;i++) {
                long ocount = dataset.getLong();
                byte[] bytes = new byte[(int) count];
                dataset.get(bytes);
                oresult[ioffset+i] = ByteBuffer.wrap(bytes);
            } */
            break;
        case Enum:
            // recast as enum's basetype
            extractObjectVector(((DapEnum) basetype).getBaseType(), dataset, start, count, vector, offset);
            break;
        }
        // If this is a fixed size type (with exceptions),
        // then we can immediately
        // position the buffer past the read data
        //if(atomtype.isFixedSize() && !atomtype.isEnumType())
        //    dataset.position((int) (localoffset + extent));
    }

}

