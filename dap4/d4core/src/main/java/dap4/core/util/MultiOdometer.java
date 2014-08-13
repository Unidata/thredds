/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.DapDimension;

import java.util.ArrayList;
import java.util.Iterator;
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

    protected Slice[] cache;
    protected int current; // pointers into multslice list
    protected long[][] setindices; // enumeration of the possible slice sets
    protected long[] sizes;
    //////////////////////////////////////////////////
    // Constructor(s)

    public MultiOdometer()
    {
    }

    public MultiOdometer(List<Slice> set, List<DapDimension> dimset, boolean usecontiguous)
            throws DapException
    {
        super(set, dimset, usecontiguous);
        this.cache = set.toArray(new Slice[set.size()]);
        this.sizes = new long[this.rank];
        for(int i = 0; i < this.rank; i++) {
            Slice sl = set.get(i);
            switch (sl.getSort()) {
            case Single:
                this.sizes[i] = 1;
                break;
            case Multi:
                this.sizes[i] = ((MultiSlice) sl).getSlices().size();
            }
        }
        int truerank = this.rank - contiguousdelta;
        if(truerank == 0)
            this.setindices = null;
        else {
            PowerSet ps = new PowerSet(this.sizes);
            this.setindices = ps.getPowerSet();
            if(DEBUG) {
                System.err.printf("Multi: |slicesets| = %d%n", setindices.length);
                System.err.println(ps.toString());
            }
        }
        this.current = 0;
        moveToNextSet(); // prime the iteration
    }

    //////////////////////////////////////////////////

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < rank; i++) {
            Slice s = this.cache[i];
            if(i == current)
                buf.append("*");
            buf.append(s.toString());
            buf.append(String.format("(%d)", s.getCount()));
        }
        return buf.toString();
    }

    /*
    protected List<List<Slice>>
    generate(int pos)
    {
        List<List<Slice>> listlist = null;
        List<Slice> list = null;
        if(pos == this.slices.length) // terminate recursion
            listlist = new ArrayList<List<Slice>>();
        else {
            Slice slice = this.slices[pos];
            switch (slice.getSort()) {
            case Single:
                listlist = generate(pos + 1);
                for(List<Slice> l : listlist) // prefix with this slice
                {
                    l.add(0, slice);
                }
                break;
            case Multi:
                MultiSlice ms = (MultiSlice) slice;
                for(int i = 0; i < ms.getSlices().size(); i++) {
                    slice = ms.getSlices().get(i);
                    list = new ArrayList();
                    list.add(slice);
                    listlist = generate(pos + 1);
                    listlist.add(0, list);
                }
                break;
            }
        }
        return listlist;
    }
    */
//////////////////////////////////////////////////
// Iterator API Overrides

    @Override
    public boolean
    hasNext()
    {
        if(super.hasNext())
            return true;
        if(this.setindices != null && current + 1 < this.setindices.length)
            return true;
        return false;
    }

    @Override
    public Long
    next()
    {
        if(super.hasNext())
            return super.next();
        // move to the next set of slices
        current++;
        if(this.setindices == null || current >= this.setindices.length)
            throw new NoSuchElementException();
        moveToNextSet();
        this.state = STATE.INITIAL;
        return super.next();
    }

    protected void
    moveToNextSet()
    {
        if(this.setindices == null)
            return;
        long[] indices = this.setindices[current];
        this.slices = new Slice[this.rank];
        for(int i = 0; i < this.rank; i++) {
            Slice sl = this.cache[i];
            if(sl.getSort() == Slice.Sort.Multi) {
                int ii = (int) indices[i];
                MultiSlice msl = (MultiSlice) sl;
                Slice s = msl.getSlices().get(ii);
                this.slices[i] = s;
            }
        }
        if(DEBUG)
            System.err.println("Multislice: " + this.toString());
        super.reset();
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
            size *= cache[i].getCount();
        }
        return size;
    }

    @Override
    public boolean
    isContiguous()
    {
        return cache[cache.length - 1].isContiguous();
    }

    public List<Slice>
    getContiguous()
    {
        return cache[cache.length - 1].getContiguous();
    }


}
