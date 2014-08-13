/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.DapDimension;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A classic implementation of an odometer
 * taken from the netcdf-c code.
 * Extended to provide iterator interface
 * Extended to provide contiguous slice info.
 */

public class Odometer implements Iterator<Long>
{

    //////////////////////////////////////////////////
    // Constants

    // Mnemonic

    static protected enum STATE
    {
        INITIAL, STARTED, DONE;
    }

    //////////////////////////////////////////////////
    // factory

    static public Odometer
    factory(List<Slice> slices, List<DapDimension> dimset, boolean usecontiguous)
            throws DapException
    {
        boolean multi = false;
        if(slices != null)
            for(int i = 0; i < slices.size(); i++) {
                if(slices.get(i).getSort() == Slice.Sort.Multi) {
                    multi = true;
                    break;
                }
            }
        return multi ? new MultiOdometer(slices, dimset, usecontiguous) : new Odometer(slices, dimset, usecontiguous);
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected STATE state = STATE.INITIAL;

    protected int rank = 0;
    protected Slice[] slices = null;
    protected DapDimension[] dimset = null;

    // If usecontiguous is true, then iterate
    // lock the last slice to the value 0.
    protected boolean usecontiguous = false;
    protected int contiguousdelta = (usecontiguous?1:0); // offset on place to stop

    // The current odometer indices
    protected long[] indices;

    // precompute this.slices[i].getLast() - this.slices[i].getStride()
    protected long[] endpoint;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Odometer()
    {
    }

    public Odometer(List<DapDimension> dimset, boolean usecontiguous)
            throws DapException
    {
        this(DapUtil.dimsetSlices(dimset), dimset, usecontiguous);
    }

    public Odometer(List<Slice> set, List<DapDimension> dimset, boolean usecontiguous)
            throws DapException
    {
        if(set == null)
            throw new DapException("Null slice list");
        if(set.size() != dimset.size())
            throw new DapException("Rank mismatch");
        this.rank = dimset.size();
        if(this.rank == 0)
            throw new DapException("Rank == 0; use Scalar Odometer");
        this.slices = set.toArray(new Slice[this.rank]);
        this.dimset = dimset.toArray(new DapDimension[dimset.size()]);
        this.indices = new long[this.rank];
        this.endpoint = new long[this.rank];
        for(int i = 0; i < this.rank; i++) {
            DapDimension dim = dimset.get(i);
            this.dimset[i] = dim;
        }
        this.usecontiguous = usecontiguous;
        this.contiguousdelta = (usecontiguous ? 1 : 0);
        reset();
    }

    protected void
    reset()
    {
        for(int i = 0; i < this.rank; i++) {
            try {
                slices[i].setMaxSize(dimset[i].getSize());
                slices[i].finish();
                this.indices[i] = this.slices[i].getFirst();
                this.endpoint[i] = this.slices[i].getLast() - this.slices[i].getStride();
            } catch (DapException de) {throw new IllegalArgumentException(de);}
            if(usecontiguous)
                this.indices[this.rank - 1] = 0;
        }
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < rank; i++) {
            if(i > 0)
                buf.append(",");
            buf.append(dimset[i] != null ? dimset[i].getShortName() : "null");
            buf.append(slices[i].toString());
            buf.append(String.format("(%d)", this.slices[i].getCount()));
            if(this.indices != null)
                buf.append(String.format("@%d", this.indices[i]));
        }
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // Odometer API

    /**
     * Compute the linear index
     * from the current odometer indices.
     */
    public long
    index()
    {
        long offset = 0;
        for(int i = 0; i < this.rank; i++) {
            offset *= slices[i].getMaxSize();
            offset += this.indices[i];
        }
        return offset;
    }

    /**
     * Compute the total number of elements.
     */
    public long
    totalSize()
    {
        long size = 1;
        for(int i = 0; i < this.rank; i++) {
            size *= slices[i].getCount();
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
        return this.indices;
    }

    //////////////////////////////////////////////////
    // Iterator API

    @Override
    public boolean
    hasNext()
    {
        int stop = this.rank - contiguousdelta;
        switch (this.state) {
        case INITIAL:
            return true;
        case STARTED:
            int i;
            for(i = stop - 1; i >= 0; i--) { // walk backwards
                if(this.indices[i] <= this.endpoint[i])
                    return true;
            }
            this.state = STATE.DONE;
            break;
        case DONE:
        }
        return false;
    }

    @Override
    public Long
    next()
    {
        int i;
        int stop = this.rank - contiguousdelta;
        switch (this.state) {
        case INITIAL:
            this.state = STATE.STARTED;
            break;
        case STARTED:
            // on entry: indices are the last index set
            // on exit, the indices are the next value
            for(i = stop - 1; i >= 0; i--) { // walk backwards
                if(this.indices[i] > this.endpoint[i])
                    this.indices[i] = this.slices[i].getFirst(); // reset this position
                else {
                    this.indices[i] += this.slices[i].getStride();  // move to next indices
                    break;
                }
            }
            if(i < 0)
                this.state = STATE.DONE;
            break;
        case DONE:
            break;
        }
        if(this.state == STATE.DONE)
            throw new NoSuchElementException();
        return index();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    //////////////////////////////////////////////////
    // Get contiguous slice info. This only applies to
    // the last slice.

    public boolean
    isContiguous()
    {
        return slices[slices.length - 1].isContiguous();
    }

    public List<Slice>
    getContiguous()
    {
        return slices[slices.length - 1].getContiguous();
    }

}
