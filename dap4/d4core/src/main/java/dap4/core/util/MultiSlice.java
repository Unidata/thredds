/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A MultiSlice is a list of slices to support e.g. [x:y,a:b]
 * It subclasses Slice so it can appear in same places slices do;
 * code is expected to differentiate.
 */


public class MultiSlice extends Slice
{
    //////////////////////////////////////////////////
    // Instance Variables

    protected List<Slice> slices;

    // cache some values
    protected long count = -1;

    //////////////////////////////////////////////////
    // Constructor(s)

    public MultiSlice(List<Slice> slices)
            throws DapException
    {
        super();
        this.sort = Sort.Multi;
        this.slices = slices;
        finish();
        // provide values for first, last, etc
        this.first = -1;
        this.last = -1;
        this.stride = -1;
        this.size = -1;
        for(int i = 0; i < this.slices.size(); i++) {
            Slice s = this.slices.get(i);
            this.first = (this.first < 0 ? s.getFirst() : Math.min(this.first, s.getFirst()));
            this.last = Math.max(this.last, s.getLast());
            this.stride = Math.max(this.stride, s.getStride());
            this.size = Math.max(this.size, s.getMaxSize());
        }
        this.whole = false;
        this.constrained = true; // by definition
    }

    //////////////////////////////////////////////////

    /**
     * Compare two slices for equality
     *
     * @param o the other slice to compare with
     * @return true if this and other are the same, false otherwise
     */
    @Override
    public boolean equals(Object o)
    {
        if(o == this) return true;
        if(o == null) return false;
        if(o instanceof Slice) try {
            List<Slice> tmp = new ArrayList<Slice>();
            tmp.add((Slice)o);
            o = new MultiSlice(tmp);
        } catch (DapException de) {
            throw new IllegalArgumentException();
        }
        if(!(o instanceof MultiSlice)) return false;
        Slice other = (Slice) o;
        return other.getFirst() == getFirst()
            && other.getLast() == getLast()
            && other.getStride() == getStride();
    }

    @Override
    public int hashCode()
    {
        long accum = 0;
        for(int i=0;i<slices.size();i++) {
            Slice s = slices.get(i);
            accum += s.hashCode() * i;
        }
        return (int) accum;
    }

    public String
    toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[");
        for(int i = 0; i < slices.size(); i++) {
            if(i > 0)
                buf.append(",");
            buf.append(slices.get(i).toString(false));
        }
        buf.append("]");
        return buf.toString();
    }

    @Override
    public Slice
    finish()
            throws DapException
    {
        this.size = -1;
        for(int i = 0; i < slices.size(); i++) {// size is max size
            Slice sl = this.slices.get(i);
            assert (sl.getSort() == Slice.Sort.Single);
            sl.finish();
            if(this.size < sl.getMaxSize())
                this.size = sl.getMaxSize();
        }
        if(this.size < 0)
            throw new DapException("Cannot compute multislice size");
        for(int i = 0; i < slices.size(); i++) {
            this.slices.get(i).setMaxSize(this.size);
        }
        this.count = 0;
        for(int i = 0; i < slices.size(); i++) {
            count += this.slices.get(i).getCount();
        }
        return this; // fluent interface
    }

    //////////////////////////////////////////////////
    // Accessors

    public List<Slice>
    getSlices()
    {
        return this.slices;
    }

    @Override
    public long
    getCount()
    {
        return this.count;
    }

    @Override
    public void
    setMaxSize(long size)
            throws DapException
    {
        for(int i = 0; i < slices.size(); i++) {
            slices.get(i).setMaxSize(size);
        }
    }

    //////////////////////////////////////////////////
    // Contiguous slice extraction

    @Override
    public boolean
    isContiguous()
    {
        for(int i = 0; i < slices.size(); i++) {
            if(slices.get(i).getStride() != 1)
                return false;
        }
        return true;
    }

    @Override
    public List<Slice>
    getContiguous()
    {
        List<Slice> contig = new ArrayList();
        if(isContiguous())
            contig.addAll(this.slices);
        return contig;
    }

}
