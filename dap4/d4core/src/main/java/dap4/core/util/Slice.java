/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;

import dap4.core.dmr.DapDimension;

import java.util.ArrayList;
import java.util.List;

/**
 * A Slice is used for two purposes
 * <ol>
 * <li> To specify a subset of data to extract.
 * <li> To specify a subset of data as specified in a constraint.
 * </ol>
 * In case two, the "last" value of the slice may be undefined
 * (e.q. specifying [0:]) and so they must be filled in at some point.
 * Note that first cannot be undefined because it always defaults to zero;
 * similarly stride defaults to one.
 * Slice Supports iteration.
 * Modifiied 10/15/2016 to support zero sized slices.
 */

public class Slice
{

    //////////////////////////////////////////////////
    // Constants

    // Define a constant to indicate the slice is undefined (i.e.
    // its "last" value is the UNDEFINED value).
    // This will primarily be used in the constraint expression parser

    //static public final long UNDEFINED = 0x7FFFFFFFFFFFFFFFL;
    static public final long UNDEFINED = -1L;

    // Define maximum legal dimension based on the spec

    static public final long MAXLENGTH = 0x3FFFFFFFFFFFFFFFL;

    static public enum Sort
    {
        Single, Multi;
    }

    // Define a set of slices indicating the canonical scalar set
    static public List<Slice> SCALARSLICES;
    static public Slice SCALARSLICE;

    static {
        try {
            SCALARSLICE = new Slice(0, 1, 1, 1).finish();
            SCALARSLICES = new ArrayList<Slice>();
            SCALARSLICES.add(SCALARSLICE);
        } catch (DapException de) {
            SCALARSLICES = null;
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    // Is this a Slice or a Multislice
    protected Sort sort = Sort.Single;

    /**
     * First index
     */
    long first = UNDEFINED;

    /**
     * Last+1 index; (stop - first) should be size of the slice.
     */
    long stop = UNDEFINED;

    /**
     * Stride
     */
    long stride = UNDEFINED;

    /**
     * Max size  (typically the size of the underlying dimension)
     */
    long maxsize = MAXLENGTH;

    /**
     * Indicate if this is known to be a whole slice
     * Boolean.TRUE => yes
     * Boolean.FALSE => no
     * null => unknown
     */
    Boolean whole = null;

    /**
     * Indicate that this slice's first/last/stride were
     * specifically set; typically from a constraint.
     * Defaults to true.
     */
    protected boolean constrained = true;

    //////////////////////////////////////////////////
    // Constructors

    public Slice()
    {
    }

    public Slice(long first, long stop, long stride)
            throws DapException
    {
        this(first, stop, stride, UNDEFINED);
    }

    public Slice(long first, long stop, long stride, long maxsize)
            throws DapException
    {
        this();
        setIndices(first, stop, stride, maxsize);
    }

    public Slice(Slice s)
            throws DapException
    {
        this();
        setIndices(s.getFirst(), s.getStop(), s.getStride(), s.getMax());
        setConstrained(s.isConstrained());
        setWhole(s.isWhole());
    }

    public Slice(DapDimension dim)
            throws DapException
    {
        this();
        setIndices(0, dim.getSize(), 1, dim.getSize());
        setWhole(true);
        setConstrained(false);
    }

    //////////////////////////////////////////////////
    // Slice specific API

    /**
     * Perform sanity checks on a slice and repair where possible.
     *
     * @return this (fluent interface)
     * @throws dap4.core.util.DapException if slice is malformed
     */
    public Slice
    finish()
            throws DapException
    {
        // Attempt to repair undefined values
        if(this.first == UNDEFINED) this.first = 0;   // default
        if(this.stride == UNDEFINED) this.stride = 1; // default
        if(this.stop == UNDEFINED && this.maxsize != UNDEFINED)
            this.stop = this.maxsize;
        if(this.stop == UNDEFINED && this.maxsize == UNDEFINED)
            this.stop = this.first + 1;
        if(this.maxsize == UNDEFINED && this.stop != UNDEFINED)
            this.maxsize = this.stop;
        // else (this.stop != UNDEFINED && this.maxsize != UNDEFINED)
        assert (this.first != UNDEFINED);
        assert (this.stride != UNDEFINED);
        assert (this.stop != UNDEFINED);
        // sanity checks
        if(this.first > this.maxsize)
            throw new DapException("Slice: first index > max size");
        if(this.stop > (this.maxsize+1))
            throw new DapException("Slice: stop > max size");
        if(this.first < 0)
            throw new DapException("Slice: first index < 0");
        if(this.stop < 0)
            throw new DapException("Slice: stop index < 0");
        if(this.stride <= 0)
            throw new DapException("Slice: stride index <= 0");
        if(this.first > this.stop)
            throw new DapException("Slice: first index > last");
        return this; // fluent interface
    }

    public SliceIterator
    iterator()
    {
        return new SliceIterator(this);
    }

    //////////////////////////////////////////////////
    // Accessors

    public Sort getSort()
    {
        return this.sort;
    }

    public long getFirst()
    {
        return this.first;
    }

    public long getStop()
    {
        return this.stop;
    }

    public long getLast()
    {
        return ((this.stop - this.first) == 0 ? 0 : this.stop - 1);
    }

    public long getStride()
    {
        return this.stride;
    }

    public long getSize() // not same as getcount and not same as maxsize
    {
        return (this.stop - this.first);
    }

    public long getMax() // not same as getcount and not same as maxsize
    {
        return this.maxsize;
    }

    public Slice setMaxSize(long size)
            throws DapException
    {
        return setIndices(this.first, this.stop, this.stride, size);
    }

    public Slice setIndices(long first, long stop, long stride)
            throws DapException
    {
        return setIndices(first, stop, stride, UNDEFINED);
    }

    public Slice setIndices(long first, long stop, long stride, long maxsize)
            throws DapException
    {
        this.first = first;
        this.stop = stop;
        this.stride = stride;
        this.maxsize = maxsize;
        return this.finish(); // fluent interface
    }

    public Boolean isWhole()
    {
        return whole;
    }

    public Slice setWhole(Boolean tf)
    {
        whole = tf;
        return this; // fluent interface
    }

    public Boolean isConstrained()
    {
        return constrained;
    }

    public Slice setConstrained(Boolean tf)
    {
        constrained = tf;
        return this; // fluent interface
    }

    //////////////////////////////////////////////////
    // Misc. methods

    /**
     * Compute the number of elements in
     * the slice. Note that this is different from
     * getStop() because stride is taken into account.
     */
    public long
    getCount()
    {
        assert this.first != UNDEFINED && this.stride != UNDEFINED && this.stop != UNDEFINED;
        long count = (this.stop) - this.first;
        count = (count + this.stride - 1);
        count /= this.stride;
        return count;
    }

    /**
     * Compare two slices for equality
     *
     * @param o the other slice to compare with
     * @return true if this and other are the same, false otherwise
     */
    @Override
    public boolean equals(Object o)
    {
        if(!(o instanceof Slice)) return false;
        Slice other = (Slice) o;
        if(other == this) return true;
        return other.getFirst() == this.getFirst()
                && other.getStop() == this.getStop()
                && other.getStride() == this.getStride();
    }

    @Override
    public int hashCode()
    {
        return (int) (getFirst() << 20 | getStop() << 10 | getStride());
    }

    @Override
    public String toString()
    {
        return toString(true);
    }

    public String toString(boolean withbrackets)
    {
        StringBuilder buf = new StringBuilder();
        if(withbrackets)
            buf.append("[");
        if((this.stop - this.first) == 0)
            buf.append("0");
        else if(this.stride == 1)
            buf.append(String.format("%d:%d", this.first, this.stop - 1));
        else
            buf.append(String.format("%d:%d:%d", this.first, this.stride, this.stop - 1));
        buf.append(String.format("|%d", (this.stop - this.first)));
        if(withbrackets)
            buf.append("]");
        return buf.toString();
    }

    /**
     * Convert this slice to a string
     * suitable for use in a constraint
     *
     * @return constraint usable string
     * @throws DapException
     */
    public String toConstraintString()
            throws DapException
    {
        assert this.first != UNDEFINED && this.stride != UNDEFINED && this.stop != UNDEFINED;
        if((this.stop - this.first) == 0) {
            return String.format("[0]");
        } else if(this.stride == 1) {
            if((this.stop - this.first) == 1)
                return String.format("[%d]", this.first);
            else
                return String.format("[%d:%d]", this.first, this.stop - 1);
        } else
            return String.format("[%d:%d:%d]", this.first, this.stride, this.stop - 1);
    }

    // Static Utilities

    /**
     * Take two slices and compose src wrt target
     * Assume neither argument is null. This code
     * should match ucar.ma2.Section in thredds
     * and dceconstraint.c in the netcdf-c library.
     *
     * @param target
     * @param src
     * @return new, composed Range
     * @throws DapException
     */
    static public Slice
    compose(Slice target, Slice src)
            throws DapException
    {
        long sr_stride = target.getStride() * src.getStride();
        long sr_first = MAP(target, src.getFirst());
        long lastx = MAP(target, src.getLast());
        long sr_last = (target.getLast() < lastx ? target.getLast() : lastx); //min(last(),lastx)
        return new Slice(sr_first, sr_last + 1, sr_stride, sr_last + 1).finish();
    }

    /**
     * Map ith element of one range wrt a target range
     *
     * @param target the target range
     * @param i      index of the element
     * @return the i-th element of a range
     * @throws DapException if index is invalid
     */
    static long
    MAP(Slice target, long i)
            throws DapException
    {
        if(i < 0)
            throw new DapException("Slice.compose: i must be >= 0");
        if(i > target.getStop())
            throw new DapException("i must be < stop");
        return target.getFirst() + i * target.getStride();
    }

    public List<Slice>
    getSubSlices()
    {
        List<Slice> list = new ArrayList<>();
        list.add(this);
        return list;
    }

    public Slice
    getSubSlice(int i)
    {
        if(i != 0)
            throw new IndexOutOfBoundsException();
        return this;
    }


}
