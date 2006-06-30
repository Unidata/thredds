/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap;
import dods.dap.Server.InvalidParameterException;

/**
 * This class holds information about each dimension in a <code>DArray</code>.
 * Each array dimension carries with it its own projection information, as
 * well as it's name and size.
 * <p> The projection information takes the form of three integers: the start,
 * stop, and stride values. This is clearest with an example. Consider a
 * one-dimensional array 10 elements long. If the start value of the
 * dimension constraint is 3, then the constrained array appears to be seven
 * elements long. If the stop value is changed to 7, then the array appears
 * to be five elements long. If the stride is changed to two, the array will
 * appear to be 3 elements long. Array constraints are written as
 * <code>[start:stride:stop]</code>.
 * @see DArray
*/
public final class DArrayDimension implements Cloneable {
    private String  name;
    private int     size;
    private int     start;
    private int     stride;
    private int     stop;


    /** Construct a new DArrayDimension.
    @param size The size of the dimension.
    @param name The dimension's name, or null if no name.
    */
    public DArrayDimension(int size, String name) {
        this.size   = size;
        this.name   = name;
            this.start  = 0;
            this.stride = 1;
            this.stop   = size - 1;
    }

    /** Clone this object */
    public Object clone() {
        try {
            DArrayDimension d = (DArrayDimension)super.clone();
            return d;
        }
            catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    /** Get the dimension name. */
    public String getName() {
        return name;
    }

    /** Set the dimension name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Get the dimension size. */
    public int getSize() {
        return size;
    }

    /** Set the dimension size. */
    public void setSize(int size) {
        this.size = size;
    }

    /** Set the projection information for this dimension.
      * The parameters <code>start</code> <code>stride</code> and <code>stop</code>
      * are checked to verify that they make sense relative to each other and to
      * the size of this dimension. If not an Invalid ParameterException is thrown.
      * The general rule is: 0&lt;=start&lt;size, 0&lt;stride, 0&lt;=stop<size, start&lt;=stop.
      *
      * @param start The starting point for the projection of this <code>DArrayDimension</code>.
      * @param stride The size of the stride for the projection of this <code>DArrayDimension</code>.
      * @param stop The stopping point for the projection of this <code>DArrayDimension</code>.
      */
    public void setProjection(int start, int stride, int stop) throws InvalidParameterException  {
        String msg = "DArrayDimension.setProjection: Bad Projection Request: ";

        if( start >= size)
            throw new InvalidParameterException(msg + "start ("+start+") >= size ("+size+") for "+name);

        if( start < 0)
            throw new InvalidParameterException(msg + "start < 0");

        if( stride <= 0)
            throw new InvalidParameterException(msg + "stride <= 0");

        if( stop >= size)
            throw new InvalidParameterException(msg + "stop >= size");

        if( stop < 0)
            throw new InvalidParameterException(msg + "stop < 0");

        if( stop < start)
            throw new InvalidParameterException(msg + "stop < start");

        this.start  = start;
        this.stride = stride;
        this.stop   = stop;
        this.size   = 1 + (stop - start) / stride;
    }

    /** Get the projection start point for this dimension. */
    public int getStart() {
        return start;
    }

    /** Get the projection stride size for this dimension. */
    public int getStride() {
        return stride;
    }

    /** Get the projection stop point for this dimension. */
    public int getStop() {
        return stop;
    }


}
