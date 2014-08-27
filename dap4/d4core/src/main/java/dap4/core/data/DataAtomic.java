/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

import dap4.core.dmr.DapType;
import dap4.core.util.Slice;

import java.io.IOException;
import java.util.List;

/**
DataAtomic represents a non-container object.
*/

public interface DataAtomic extends DataVariable
{
    /**
     * Get the type of this atomic variable
     * @return  the type
     */
    public DapType getType();

    /**
     * Get the total number of elements in the atomic array.
     * A scalar is treated as a one element array.
     *
     * @return 1 if the variable is scalar, else the product
     *         of the dimensions of the variable.
     */
    public long getCount(); // dimension product

    /**
     * Get the s ize of a single element in bytes; 0 => undefined
     * @return  size
     */
    public long getElementSize();

    /**
     *  Read of multiple values at once.
     *  The returned value (parameter "data") is some form of java array (e.g. int[]).
     *  The type depends on the value of getType().
     *  Note that implementations of this interface are free to provide
     *  alternate read methods that return values in e.g. a java.nio.Buffer.
     *  Note that unsigned types (e.g. UInt64) are returned as a signed version
     *  (e.g. Int64), and will have the proper bit pattern for the unsigned value.
     *  If the size of the "data" array is not the correct size, then an error
     *  will be returned.
     *  For opaque data, the result is ByteBuffer[].
     *
     * @param constraint the set of slices defining which values to return
     * @param data the array into which the values are returned
     * @param offset the offset into data into which to read
     */
    public void read(List<Slice> constraint, Object data, long offset) throws DataException;

    /*
       *  @param start the first value to read
     *  @param count the number of values to read; |data| must >= (offset+count)
     *  @param data the array into which the values are returned
     *  @param offset the offset into data into which to read

    public void read(long start, long count, Object data, long offset) throws DataException;
     */


    /**
     *  Provide a read of a single value at a given offset in a (possibly dimensioned)
     *  atomic valued variable. As mentioned above, unsigned types are returned as a signed version.
     *  The type of the returned value is the obvious one (e.g. int->Integer, opaque->ByteBuffer, etc.).
     */

    public Object read(long index) throws DataException;

}
