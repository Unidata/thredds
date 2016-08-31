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
 */

public class Odometer implements Iterator<Index>
{

    //////////////////////////////////////////////////
    // Constants

    // Mnemonic

    static protected enum STATE
    {
        INITIAL, STARTED, DONE;
    }

    //////////////////////////////////////////////////
    // factories

    static public Odometer
    factoryScalar()
    {
        return new ScalarOdometer();
    }

    static public Odometer
    factory(List<Slice> slices)
            throws DapException
    {
        return factory(slices, null);
    }

    static public Odometer
    factory(List<Slice> slices, List<DapDimension> dimset)
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
        if(slices == null || slices.size() == 0)
            return factoryScalar();
        else if(multi)
            return new MultiOdometer(slices, dimset);
        else
            return new Odometer(slices, dimset);
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected STATE state = STATE.INITIAL;

    protected int rank = 0;
    protected Slice[] slices = null;
    protected DapDimension[] dimset = null;

    // The current odometer indices
    protected Index index;

    // precompute this.slices[i].getLast() - this.slices[i].getStride()
    protected long[] endpoint;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Odometer()
    {
    }

    public Odometer(List<Slice> set)
            throws DapException
    {
        this(set, null);
    }

    public Odometer(List<Slice> set, List<DapDimension> dimset)
            throws DapException
    {
        if(set == null)
            throw new DapException("Null slice list");
        if(dimset != null && set.size() != dimset.size())
            throw new DapException("Rank mismatch");
        this.rank = set.size();
        if(this.rank == 0)
            throw new DapException("Rank == 0; use Scalar Odometer");
        this.slices = set.toArray(new Slice[this.rank]);
        if(dimset != null)
            this.dimset = dimset.toArray(new DapDimension[dimset.size()]);
        this.endpoint = new long[this.rank];
        this.index = new Index(rank);
        if(dimset != null)
            for(int i = 0; i < this.rank; i++) {
                DapDimension dim = dimset.get(i);
                this.dimset[i] = dim;
            }
        for(int i = 0; i < this.rank; i++) {
            this.index.dimsizes[i] = slices[i].getMaxSize();
        }
        reset();
    }

    protected void
    reset()
    {
        for(int i = 0; i < this.rank; i++) {
            try {
                slices[i].finish();
                this.index.indices[i] = this.slices[i].getFirst();
                this.endpoint[i] = this.slices[i].getLast() - this.slices[i].getStride();
            } catch (DapException de) {
                throw new IllegalArgumentException(de);
            }
        }
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < rank; i++) {
            if(i > 0)
                buf.append(",");
            if(dimset != null)
                buf.append(dimset[i] != null ? dimset[i].getShortName() : "null");
            buf.append(slices[i].toString());
            buf.append(String.format("(%d)", this.slices[i].getCount()));
            if(this.index != null)
                buf.append(String.format("@%d", this.index.indices[i]));
        }
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // Odometer API

    /**
     * Return odometer rank
     */
    public int
    rank()
    {
        return this.rank;
    }

    /**
     * Return ith slice
     */
    public Slice
    slice(int i)
    {
        if(i < 0 || i >= this.rank)
            throw new IllegalArgumentException();
        return this.slices[i];
    }

    /**
     * Compute the linear index
     * from the current odometer indices.
     */
    public long
    index()
    {
        return index.index();
    }

    /**
     * Return current set of indices
     */
    public Index
    indices()
    {
        return this.index;
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

    //////////////////////////////////////////////////
    // Iterator API

    @Override
    public boolean
    hasNext()
    {
        int stop = this.rank;
        switch (this.state) {
        case INITIAL:
            return true;
        case STARTED:
            int i;
            for(i = stop - 1; i >= 0; i--) { // walk backwards
                if(this.index.indices[i] <= this.endpoint[i])
                    return true;
            }
            this.state = STATE.DONE;
            break;
        case DONE:
        }
        return false;
    }

    @Override
    public Index
    next()
    {
        int i;
        int lastpos = this.rank;
        int firstpos = 0;
        switch (this.state) {
        case INITIAL:
            this.state = STATE.STARTED;
            break;
        case STARTED:
            i = step(firstpos, lastpos);
            if(i < 0)
                this.state = STATE.DONE;
            break;
        case DONE:
            break;
        }
        if(this.state == STATE.DONE)
            throw new NoSuchElementException();
        return indices();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    //////////////////////////////////////////////////

    // on entry: indices are the last index set
    // on exit, the indices are the next value
    // return index of place where we have room to step;
    // return -1 if we have completed.
    public int
    step(int firstpos, int lastpos)
    {
        for(int i = lastpos - 1; i >= firstpos; i--) { // walk backwards
            if(this.index.indices[i] > this.endpoint[i])
                this.index.indices[i] = this.slices[i].getFirst(); // reset this position
            else {
                this.index.indices[i] += this.slices[i].getStride();  // move to next indices
                return i;
            }
        }
        return -1;
    }


}
