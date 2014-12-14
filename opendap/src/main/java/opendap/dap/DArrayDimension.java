/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////

package opendap.dap;

import java.io.*;
import java.util.Formatter;

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
public final class DArrayDimension extends DAPNode
{
    static final long serialVersionUID = DArrayDimension.class.hashCode();

    static public class Slice {
        public int size;
        public int start;
        public int stride;
        public int stop;
        public Slice(int size, int start, int stride, int stop)
        {
            this.size = size;
            this.start = start;
            this.stride = stride;
            this.stop = stop;
        }
        public Slice(int start, int stride, int stop)
        {
            this(0,start,stride,stop);
            this.size = 1 + (stop - start) / stride;
        }
    };

    // Define two slices for each dimension
    transient private Slice decl = null; // as defined in the original declaration
    transient private Slice projection = null; // as defined in any projection of this dimension
    private DArray container = null;

    /**
     * Construct a new DArrayDimension.
     *
     * @param size The size of the dimension.
     * @param name The dimension's name, or null if no name.
     */
    public DArrayDimension(int size, String name) {
        super(name);
        this.decl = new Slice(size,0,1,size-1);
    }

    /**
     * Get the dimension size.
     */
    public int getSize() {
        return (projection != null?projection.size:decl.size);
    }

    /**
     * Set the dimension size.
     */
    public void setSize(int size) {
        decl.size = size;
    }

    /**
     * Get the projection start point for this dimension.
     */
    public int getStart() {
        return (projection != null?projection.start:decl.start);
    }

    /**
     * Get the projection stride size for this dimension.
     */
    public int getStride() {
        return (projection != null?projection.stride:decl.stride);
    }

    /**
     * Get the projection stop point for this dimension.
     */
    public int getStop() {
        return (projection != null?projection.stop:decl.stop);
    }

    public void setContainer(DArray da)
    {
        container = da;
    }

    public DArray getContainer() {return container;}

    public void printConstraint(PrintWriter os)
    {
	String buf = "[";
	if(getSize() == 1 && getStart() == getStop())
	    buf += ""+getStart();
	else if(getStride() == 1)
	    buf += getStart() + ":" + getStop();
	else
	    buf += getStart() + ":" + getStride() + ":" + getStop();
	buf += "]";
	os.print(buf);
    }


    /**
     * Set the projection information for this dimension.
     * The parameters <code>start</code> <code>stride</code> and <code>stop</code>
     * are checked to verify that they make sense relative to each other and to
     * the size of this dimension. If not an Invalid ParameterException is thrown.
     * The general rule is: 0&lt;=start&lt;size, 0&lt;stride, 0&lt;=stop&lt;size, start&lt;=stop.
     *
     * @param start  The starting point for the projection of this <code>DArrayDimension</code>.
     * @param stride The size of the stride for the projection of this <code>DArrayDimension</code>.
     * @param stop   The stopping point for the projection of this <code>DArrayDimension</code>.
     */
    public void setProjection(int start, int stride, int stop) throws InvalidDimensionException
    {
        if(projection != null) {
            // See if we are changing start/stride/stop
          if(getSize() != start || getStride() != stride || getStop() != stop) {
            Formatter f = new Formatter();
            f.format(" [%d:%d:%d (%d)] != [%d:%d:%d (%d)]", start, stride, stop, projection.size, projection.start, projection.stride, projection.stop, projection.size);
            throw new ConstraintException("Implementation limitation: muliple references to same variable in single constraint: "+container.getLongName()+f.toString());
          }
        }

        String msg = "DArrayDimension.setProjection: Bad Projection Request: ";

        // validate the arguments

        if (start < 0)
            throw new InvalidDimensionException(msg + "start < 0");

        if (stride <= 0)
            throw new InvalidDimensionException(msg + "stride <= 0");

        if (stop < 0)
             throw new InvalidDimensionException(msg + "stop < 0");

        if (start < decl.start)
                   throw new InvalidDimensionException(msg + "start (" + start + ") < size (" + decl.size + ") for " + _nameClear);

        if (stop >= decl.size)
            throw new InvalidDimensionException(msg + "stop >= size: "+stop+":"+decl.size);


        if (stop < start)
            throw new InvalidDimensionException(msg + "stop < start");

        projection = new Slice(start,stride,stop);
    }


    /**
     * Returns a clone of this <code>Array</code>.
     * See DAPNode.cloneDag()
     *
     * @param map track previously cloned nodes
     * @return a clone of this object.
     */
    public DAPNode cloneDAG(CloneMap map)
        throws CloneNotSupportedException
    {
        DArrayDimension d = (DArrayDimension) super.cloneDAG(map);
        if(container != null) d.container = (DArray)cloneDAG(map,container);
        return d;
    }

}



