/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.DapDimension;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An odometer capable of dealing with a Multi-slices.
 */

public class MultiOdometer extends Odometer
{

    static protected boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Instance variables

    protected int current; // pointers into multslice list
    protected long[] sizes; // # of subslices in each slice
    protected List<List<Slice>> slicesets; // Set of all combinations of slices
    protected List<Odometer> odomset;  // Odometers created from slicesets

    //////////////////////////////////////////////////
    // Constructor(s)

    public MultiOdometer()
    {
    }

    public MultiOdometer(List<Slice> set)
            throws DapException
    {
        this(set, null);
    }

    public MultiOdometer(List<Slice> set, List<DapDimension> dimset)
            throws DapException
    {
        super(set, dimset);
        super.ismulti = true;
        this.sizes = new long[this.rank];
        this.odomset = new ArrayList<>();
        for(int i = 0; i < this.rank; i++) {
            Slice sl = set.get(i);
            List<Slice> subslices = sl.getSubSlices();
            this.sizes[i] = subslices.size();
        }
        int truerank = this.rank;
        if(truerank == 0) {
            this.slicesets = null;
            this.odomset = null;
        } else {
            PowerSet ps = new PowerSet(this.sizes);
            long pssize = ps.getTotalSize();
            long[][] setindices = ps.getPowerSet();
            assert setindices.length == pssize;
            this.slicesets = new ArrayList<>();
            if(DEBUG) {
                System.err.printf("Multi: |slicesets| = %d%n", setindices.length);
                System.err.println(ps.toString());
            }
            // Create set of slicsets comprising this MultiOdometer
            for(int i = 0; i < pssize; i++) {
                long[] indexset = setindices[i];
                assert indexset.length == truerank;
                // Pick out the desired set of slices
                List<Slice> subset = new ArrayList<>();
                for(int j=0;j<this.rank;j++) {
                    Slice s0 = set.get(j);
                    Slice ss = s0.getSubSlice((int) indexset[j]);
                    subset.add(ss);
                }
                this.slicesets.add(subset);
            }
            assert this.slicesets.size() == pssize;
            // Create set of odometers comprising this MultiOdometer
            for(int i = 0; i < pssize; i++) {
                Odometer sslodom = Odometer.factory(this.slicesets.get(i),dimset);
                this.odomset.add(sslodom);
            }
        }
        this.current = 0;
    }

    //////////////////////////////////////////////////

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < rank; i++) {
            Slice s = slice(i);
            if(i == current)
                buf.append("*");
            buf.append(s.toString());
            buf.append(String.format("(%d)", s.getCount()));
        }
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // Iterator API Overrides

    @Override
    public boolean
    hasNext()
    {
        if(this.current >= odomset.size())
            return false;
        Odometer ocurrent = odomset.get(this.current);
        if(ocurrent.hasNext())
            return true;
        // Try to move to next odometer
        this.current++;
        return hasNext();
    }

    @Override
    public Index
    next()
    {
        if(this.current >= odomset.size())
            throw new NoSuchElementException();
        Odometer ocurrent = odomset.get(this.current);
        assert ocurrent.hasNext();
        return ocurrent.next();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Compute the total number of elements.
     */
    @Override
    public long
    totalSize()
    {
        long size = 1;
        for(int i = 0; i < this.rank; i++) {
            size *= slice(i).getCount();
        }
        return size;
    }

    @Override
    public List<Odometer>
    getSubOdometers()
    {
        return this.odomset;
    }

}
