/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.dap;

import opendap.dap.Server.InvalidParameterException;
import opendap.util.EscapeStrings;

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
 *
 * @see DArray
 */
public final class DArrayDimension implements Cloneable, java.io.Serializable {

    static final long serialVersionUID = 1;

    private String name;
    private int size;
    private int start;
    private int stride;
    private int stop;


    /**
     * Construct a new DArrayDimension.
     *
     * @param size The size of the dimension.
     * @param name The dimension's name, or null if no name.
     */
    public DArrayDimension(int size, String name) {

        this(size, name, true);
    }

    /**
     * Construct a new DArrayDimension.
     *
     * @param size The size of the dimension.
     * @param name The dimension's name, or null if no name.
     */
    public DArrayDimension(int size, String name, boolean decodeName) {

        if (decodeName)
            setName(name);
        else
            setClearName(name);


        this.size = size;
        this.start = 0;
        this.stride = 1;
        this.stop = size - 1;
    }

    /**
     * Clone this object
     */
    public Object clone() {
        try {
            DArrayDimension d = (DArrayDimension) super.clone();
            return d;
        }
        catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    /**
     * Get the dimension name.
     */
    public String getName() {
        return EscapeStrings.id2www(name);
    }

    /**
     * Set the dimension name.
     */
    public void setName(String name) {
        this.name = EscapeStrings.www2id(name);
    }

    /**
     * Get the dimension name.
     */
    public String getClearName() {
        return name;
    }

    /**
     * Set the dimension name.
     */
    public void setClearName(String name) {
        this.name = name;
    }

    /**
     * Get the dimension size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Set the dimension size.
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Set the projection information for this dimension.
     * The parameters <code>start</code> <code>stride</code> and <code>stop</code>
     * are checked to verify that they make sense relative to each other and to
     * the size of this dimension. If not an Invalid ParameterException is thrown.
     * The general rule is: 0&lt;=start&lt;size, 0&lt;stride, 0&lt;=stop<size, start&lt;=stop.
     *
     * @param start  The starting point for the projection of this <code>DArrayDimension</code>.
     * @param stride The size of the stride for the projection of this <code>DArrayDimension</code>.
     * @param stop   The stopping point for the projection of this <code>DArrayDimension</code>.
     */
    public void setProjection(int start, int stride, int stop) throws InvalidParameterException {
        String msg = "DArrayDimension.setProjection: Bad Projection Request: ";

        if (start >= size)
            throw new InvalidParameterException(msg + "start (" + start + ") >= size (" + size + ") for " + name);

        if (start < 0)
            throw new InvalidParameterException(msg + "start < 0");

        if (stride <= 0)
            throw new InvalidParameterException(msg + "stride <= 0");

        if (stop >= size)
            throw new InvalidParameterException(msg + "stop >= size");

        if (stop < 0)
            throw new InvalidParameterException(msg + "stop < 0");

        if (stop < start)
            throw new InvalidParameterException(msg + "stop < start");

        this.start = start;
        this.stride = stride;
        this.stop = stop;
        this.size = 1 + (stop - start) / stride;
    }

    /**
     * Get the projection start point for this dimension.
     */
    public int getStart() {
        return start;
    }

    /**
     * Get the projection stride size for this dimension.
     */
    public int getStride() {
        return stride;
    }

    /**
     * Get the projection stop point for this dimension.
     */
    public int getStop() {
        return stop;
    }


}


