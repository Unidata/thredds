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

    protected List<Slice> subslices;

    // cache some values
    protected long count = -1;

    //////////////////////////////////////////////////
    // Constructor(s)

    public MultiSlice(List<Slice> subslices)
            throws dap4.core.util.DapException
    {
        super();
        this.sort = Sort.Multi;
        this.subslices = subslices;
        finish();
        // provide values for first, last, etc
        this.first = UNDEFINED;
        this.stop = UNDEFINED;
        this.stride = UNDEFINED;
        this.maxsize = UNDEFINED;
        for(int i = 0; i < this.subslices.size(); i++) {
            Slice s = this.subslices.get(i);
            this.first = (this.first < 0 ? s.getFirst() : Math.min(this.first, s.getFirst()));
            this.stop = Math.max(this.stop, s.getStop());
            this.stride = Math.max(this.stride, s.getStride());
            this.maxsize = Math.max(this.maxsize, s.getMax());
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
            tmp.add((Slice) o);
            o = new MultiSlice(tmp);
        } catch (dap4.core.util.DapException de) {
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
        for(int i = 0; i < this.subslices.size(); i++) {
            Slice s = this.subslices.get(i);
            accum += s.hashCode() * i;
        }
        return (int) accum;
    }

    public String
    toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[");
        for(int i = 0; i < this.subslices.size(); i++) {
            if(i > 0)
                buf.append(",");
            buf.append(this.subslices.get(i).toString(false));
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * Convert this multislice to a string
     * suitable for use in a constraint
     *
     * @return constraint usable string
     * @throws DapException
     */
    @Override
    public String toConstraintString()
            throws DapException
    {
        assert this.first != UNDEFINED && this.stride != UNDEFINED && this.stop != UNDEFINED;
        StringBuilder buf = new StringBuilder();
        buf.append("[");
        boolean first = true;
        for(Slice sub: this.subslices) {
            if(!first) buf.append(",");
            first = false;
            if((sub.stop - sub.first) == 0) {
                buf.append("0");
            } else if(sub.stride == 1) {
                if((sub.stop - sub.first) == 1)
                    buf.append(sub.first);
                else
                    buf.append(String.format("%d:%d", sub.first, sub.stop - 1));
            } else
                buf.append(String.format("%d:%d:%d", sub.first, sub.stride, sub.stop - 1));
        }
        buf.append("]");
        return buf.toString();
    }


    @Override
    public Slice
    finish()
            throws dap4.core.util.DapException
    {
        this.maxsize = UNDEFINED;
        for(int i = 0; i < this.subslices.size(); i++) {// size is max size
            Slice sl = this.subslices.get(i);
            assert (sl.getSort() == Slice.Sort.Single);
            sl.finish();
            if(this.maxsize < sl.getMax())
                this.maxsize = sl.getMax();
        }
        if(this.maxsize < 0)
            throw new dap4.core.util.DapException("Cannot compute multislice size");
        for(int i = 0; i < this.subslices.size(); i++) {
            this.subslices.get(i).setMaxSize(this.maxsize);
        }
        this.count = 0;
        for(int i = 0; i < this.subslices.size(); i++) {
            count += this.subslices.get(i).getCount();
        }
        return this; // fluent interface
    }

    //////////////////////////////////////////////////
    // Accessors

    @Override
    public List<Slice>
    getSubSlices()
    {
        return this.subslices;
    }

    public Slice
    getSubSlice(int i)
    {
        return this.subslices.get(i);
    }


    @Override
    public long
    getCount()
    {
        return this.count;
    }

    @Override
    public Slice
    setMaxSize(long size)
            throws DapException
    {
        for(int i = 0; i < this.subslices.size(); i++) {
            this.subslices.get(i).setMaxSize(size);
        }
        return this;
    }

}
