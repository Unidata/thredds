/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.DapDimension;

import java.util.Iterator;
import java.util.List;

/**
 * A classic implementation of an odometer
 * taken from the netcdf-c code.
 */

public class Odometer implements Iterator<Boolean>
{
    static public class ScalarOdometer extends Odometer
    {
        protected boolean first = true;

        public ScalarOdometer()
        {
        }

        @Override
        public long index()
        {
            return 0;
        }

        @Override
        public long totalSize()
        {
            return 1;
        }

        @Override
        public boolean hasNext()
        {
            boolean first = this.first;
            this.first = false;
            return first;
        }
    }

    static public ScalarOdometer getScalarOdometer()
    {
        return new ScalarOdometer();
    }

    //////////////////////////////////////////////////
    // Instance variables
    protected int rank = 0;
    protected long[] first = null;
    protected long[] stop = null;
    protected long[] stride = null;
    protected long[] count = null;
    protected long[] declsize = null;

    protected DapDimension[] dimset = null;

    // The current odometer state
    protected long[] index = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Odometer()
    {
    }

    public Odometer(List<DapDimension> dimset)
        throws DapException
    {
	this(DapUtil.dimsetSlices(dimset),dimset);
    }

    public Odometer(List<Slice> set, List<DapDimension> dimset)
        throws DapException
    {
        if(set == null)
            throw new DapException("Null slice list");
        setup(set.size());
        if(this.rank == 0) {
            // we don't actually know what is being set
            return;
        }
        for(int i = 0;i < this.rank;i++) {
            Slice slice = set.get(i);
            fill(i, slice.getFirst(), slice.getLast() + 1, slice.getStride());
        }
        setDeclsizes(dimset);
    }

    protected void
    setDeclsizes(List<DapDimension> dimset)
        throws DapException
    {
        int rank = dimset.size();
        if(rank != this.rank)
            throw new DapException("Odometer: |dimset| != rank");
        for(int i = 0;i < dimset.size();i++) {
            DapDimension dim = dimset.get(i);
            long size = dim.getSize();
            if(size <= first[i] || size < stop[i])
                throw new DapException("Odometer: Dimset invalidates odometer");
            declsize[i] = size;
            this.dimset[i] = dim;
        }
    }

    protected void fill(int i, long first, long stop, long stride)
    {
        this.stop[i] = stop;
        this.first[i] = first;
        this.stride[i] = stride;
        this.count[i] = ((stop - first) + (stride - 1)) / stride;
        this.index[i] = first;
        this.declsize[i] = this.stop[i]; // unless otherwise specified
    }

    protected void setup(int rank)
    {
        this.rank = rank;
        this.first = new long[this.rank];
        this.stop = new long[this.rank];
        this.stride = new long[this.rank];
        this.count = new long[this.rank];
        this.declsize = new long[this.rank];
        this.index = new long[this.rank];
        this.dimset = new DapDimension[this.rank];
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0;i < rank;i++) {
            if(i > 0)
                buf.append(",");
            buf.append(dimset[i] != null ? dimset[i].getShortName() : "null");
            buf.append("[");
            if(this.stride[i] == 1) {
                buf.append(String.format("%d:%d", this.first[i], this.stop[i] - 1));
            } else
                buf.append(String.format("%d:%d:%d", this.first[i], this.stride[i], this.stop[i] - 1));
            buf.append(String.format("|%d",this.index[i]));
            if(this.declsize[i] > 0)
                buf.append(String.format("/%d",this.declsize[i]));
            buf.append("]");
        }
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // Odometer specific API

    /**
     * Compute the linear index
     * from the current odometer indices.
     */
    public long index()
    {
        long offset = 0;
        for(int i = 0;i < rank;i++) {
            offset *= declsize[i];
            offset += index[i];
        }
        return offset;
    }

    /**
     * Compute the total number of elements.
     */
    public long totalSize()
    {
        long size = 1;
        for(int i = 0;i < rank;i++) {
            size *= count[i];
        }
        return size;
    }

    /**
     * Get the current set of indices
     *
     * @return current set of indices
     */
    public long[]
    getIndices()
    {
        return index;
    }
    //////////////////////////////////////////////////
    // Iterator-like API

    public boolean hasNext()
    {
        return (this.index[0] < this.stop[0]);
    }

    public Boolean next()
    {
        Boolean more = Boolean.FALSE;
        for(int i = rank - 1;i >= 0;i--) {
            index[i] += stride[i];
            if(index[i] < stop[i]) {
                more = Boolean.TRUE;
                break;
            }
            if(i > 0) // do not reset 0'th position so next will return false
                index[i] = first[i]; /* reset this position*/
        }
        return more;
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

}
