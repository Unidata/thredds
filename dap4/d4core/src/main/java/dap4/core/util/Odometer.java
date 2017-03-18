/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.DapDimension;

import java.util.ArrayList;
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
        // check for scalar case
        if(dimset != null && dimset.size() == 0) {
            if(!DapUtil.isScalarSlices(slices))
                throw new DapException("Cannot build scalar odometer with non-scalar slices");
            return factoryScalar();
        }
        boolean multi = false;
        if(slices != null) {
            for(int i = 0; i < slices.size(); i++) {
                if(slices.get(i).getSort() == Slice.Sort.Multi) {
                    multi = true;
                    break;
                }
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

    protected boolean ismulti = false;

    protected int rank = 0;
    protected List<Slice> slices = null;
    protected List<DapDimension> dimset = null;

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
        this.slices = new ArrayList<>();
        this.slices.addAll(set);
        if(dimset != null) {
            this.dimset = new ArrayList<>();
            this.dimset.addAll(dimset);
        }
        this.endpoint = new long[this.rank];
        this.index = new Index(rank);
        for(int i = 0; i < this.rank; i++) {
            this.index.dimsizes[i] = slices.get(i).getMax();
        }
        reset();
    }

    protected void
    reset()
    {
        for(int i = 0; i < this.rank; i++) {
            try {
                slices.get(i).finish();
                this.index.indices[i] = this.slices.get(i).getFirst();
                this.endpoint[i] = this.slices.get(i).getLast() - this.slices.get(i).getStride();
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
                buf.append(dimset.get(i) != null ? dimset.get(i).getShortName() : "null");
            buf.append(this.slices.get(i).toString());
            buf.append(String.format("(%d)", this.slices.get(i).getCount()));
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
        return this.slices.get(i);
    }


    public List<Slice>
    getSlices()
    {
        return this.slices;
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
            size *= this.slices.get(i).getCount();
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
                this.index.indices[i] = this.slices.get(i).getFirst(); // reset this position
            else {
                this.index.indices[i] += this.slices.get(i).getStride();  // move to next indices
                return i;
            }
        }
        return -1;
    }

    public List<Odometer>
    getSubOdometers()
    {
        List<Odometer> list = new ArrayList<>();
        list.add(this);
        return list;
    }

    public boolean
    isMulti()
    {
        return this.ismulti;
    }


}
