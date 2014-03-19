/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

import dap4.core.dmr.DapType;

import java.io.IOException;

/**
DataAtomic represents a non-container object.
*/

public interface DataAtomic extends DataVariable
{
    public DapType getType();

    public long getCount(); // dimension cross-product

    public long getElementSize(); // size of a single element in bytes; 0 => undefined

    /**
     * Read of multiple values at once.
     *  The returned value (parametere "data") is some form of java array (e.g. int[]).
     *  The type depends on the value of getType().
     *  Note that implementations of this interface are free to provide
     *  alternate read methods that return values in e.g. a java.nio.Buffer.
     *  Note that unsigned types (e.g. UInt64) are returned as a signed version
     *  (e.g. Int64), and will have the proper bit pattern for the unsigned value.
     *  If the size of the "data" array is not the correct size, then an error
     *  will be returned.
     *  For opaque data, the result is ByteBuffer[].
     *
     *  @param start the first value to read
     *  @param count the number of values to read; |data| must >= count
     *  @param data the array into which the values are returned
     */

    public void read(long start, long count, Object data) throws DataException;

    /**
     *  Provide a read of a single value at a given offset in a (possibly dimensioned)
     *  atomic valued variable. As mentioned above, unsigned types are returned as a signed version.
     *  The type of the returned value is the obvious one (e.g. int->Integer, opaque->ByteBuffer, etc.).
     */

    public Object read(long index) throws DataException;

}
