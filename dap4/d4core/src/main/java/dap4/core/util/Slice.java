/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;

import dap4.core.dmr.DapDimension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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

/*
    static public Slice UNDEFINEDSLICE;

    static {
        try {
            DEFAULTSLICE = new Slice(0, Slice.UNDEFINED, 1).setConstrained(true);
        } catch (Exception e) {
            DEFAULTSLICE = null;
        }
    }
*/

    static public enum Sort
    {
        Single, Multi;
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
     * Last index
     */
    long last = UNDEFINED;

    /**
     * Stride
     */
    long stride = UNDEFINED;

    /**
     * Max size
     */
    long size = MAXLENGTH;

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

    public Slice(long first, long last, long stride)
            throws DapException
    {
        this(first, last, stride, (last == UNDEFINED ? UNDEFINED : last + 1));
    }

    public Slice(long first, long last, long stride, long maxsize)
            throws DapException
    {
        this();
        setIndices(first, last, stride, maxsize);
    }

    public Slice(Slice s)
            throws DapException
    {
        this();
        setIndices(s.getFirst(), s.getLast(), s.getStride(), s.getMaxSize());
        setConstrained(s.isConstrained());
        setWhole(s.isWhole());
    }

    public Slice(DapDimension dim)
            throws DapException
    {
        this();
        setIndices(0, dim.getSize() - 1, 1, dim.getSize());
        setWhole(true);
        setConstrained(false);
    }

    //////////////////////////////////////////////////
    // Slice specific API

    /**
     * Perform sanity checks on a slice and repair where possible.
     *
     * @return this (fluent interface)
     * @throws DapException if slice is malformed
     */
    public Slice
    finish()
            throws DapException
    {
        // Attempt to repair undefined values
        if(this.first == UNDEFINED) this.first = 0;   // default
        if(this.stride == UNDEFINED) this.stride = 1; // default
        if(this.last == UNDEFINED && this.size != UNDEFINED)
            this.last = this.size - 1;
        if(this.size == UNDEFINED && this.last != UNDEFINED)
            this.size = this.last + 1;
        else if(this.last == UNDEFINED && this.size == UNDEFINED)
            throw new DapException("Slice: both last and size are UNDEFINED");
        // else (this.last != UNDEFINED && this.size != UNDEFINED)
        assert (first != UNDEFINED);
        assert (stride != UNDEFINED);
        assert (last != UNDEFINED);
        // sanity checks
        if(first > this.size)
            throw new DapException("Slice: first index > max size");
        if(stride > size)
            throw new DapException("Slice: stride > max size");
        if(last > size)
            throw new DapException("Slice: last > max size");
        if(first < 0)
            throw new DapException("Slice: first index < 0");
        if(last < 0)
            throw new DapException("Slice: last index < 0");
        if(stride <= 0)
            throw new DapException("Slice: stride index <= 0");
        if(first > last)
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
        return first;
    }

    public long getLast()
    {
        return last;
    }

    public long getStride()
    {
        return stride;
    }

    public long getMaxSize()
    {
        return size;
    }

    public void setMaxSize(long size)
            throws DapException
    {
        setIndices(first, last, stride, size);
    }

/*    public Slice fill(DapDimension dim)
            throws DapException
    {
        setIndices(0, dim.getSize() - 1, 1, dim.getSize());
        setWhole(true);
        setConstrained(false);
        return this; // fluent interface
    }

*/

    public Slice setIndices(long first, long last, long stride)
            throws DapException
    {
        return setIndices(first, last, stride, UNDEFINED);
    }

    public Slice setIndices(long first, long last, long stride, long maxsize)
            throws DapException
    {
        this.first = first;
        this.last = last;
        this.stride = stride;
        this.size = maxsize;
        return this; // fluent interface
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

/*
    public Slice setWholeWRT(long maxsize)
    {
        if(last == 0 && stride == 1 && (last + 1) == maxsize)
            whole = Boolean.TRUE;
        else
            whole = Boolean.FALSE;
        return this; // fluent interface
    }
*/

    public Boolean isConstrained()
    {
        return constrained;
    }

    public Slice setConstrained(Boolean tf)
    {
        constrained = tf;
        return this; // fluent interface
    }

/*
    public DapDimension getSourceDimension()
    {
        return this.srcdim;
    }

    public Slice setSourceDimension(DapDimension dim)
        throws DapException
    {
        this.srcdim = dim;
        return this; // fluent interface
    }
*/

/*
    //////////////////////////////////////////////////
    // Computed accessors

    // Determine if this slice is incomplete
    public boolean incomplete()
    {
        return (getLast() == Slice.UNDEFINED);
    }
*/

    //////////////////////////////////////////////////
    // Misc. methods

    /**
     * Compute the number of elements int
     * the slice. Note that this is different from
     * the getLast()+1
     */
    public long
    getCount()
    {
        assert this.first != UNDEFINED && this.stride != UNDEFINED && this.last != UNDEFINED;
        long count = (this.last + 1) - this.first;
        count = (count + this.stride - 1);
        count /= this.stride;
        return count;
    }

    public long
    getStop()
    {
        assert this.last != UNDEFINED;
        return this.last + 1;
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
        return other.getFirst() == getFirst()
                && other.getLast() == getLast()
                && other.getStride() == getStride();
    }

    @Override
    public int hashCode()
    {
        return (int)(getFirst()<<20 | getLast()<<10 | getStride());
    }

    @Override
    public String toString()
    {
        return toString(true);
    }

    public String toString(boolean withbrackets)
    {
        StringBuilder buf = new StringBuilder();
        String slast = this.last == UNDEFINED ? "?" : Long.toString(this.last);
        String sstride = this.stride == UNDEFINED ? "?" : Long.toString(this.stride);
        String sfirst = this.first == UNDEFINED ? "?" : Long.toString(this.first);
        String ssize = this.size == UNDEFINED ? "?" : Long.toString(this.size);
        if(withbrackets)
            buf.append("[");
        if(this.stride == 1)
            buf.append(String.format("%s:%s", sfirst, slast));
        else
            buf.append(String.format("%s:%s:%s", sfirst, sstride, slast));
        buf.append("|");
        buf.append(ssize);
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
        assert this.first != UNDEFINED && this.stride != UNDEFINED && this.last != UNDEFINED;
        if(this.stride == 1) {
            if(this.first == this.last)
                return String.format("[%d]", this.first);
            else
                return String.format("[%d:%d]", this.first, this.last);
        } else
            return String.format("[%d:%d:%d]", this.first, this.stride, this.last);
    }

    //////////////////////////////////////////////////
    // Contiguous slice extraction

    public boolean
    isContiguous()
    {
        return (this.stride == 1);
    }

    public List<Slice>
    getContiguous()
    {
        List<Slice> contig = new ArrayList();
        if(this.stride == 1)
            contig.add(this);
        return contig;
    }

    //////////////////////////////////////////////////
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
        return new Slice(sr_first, sr_last, sr_stride, sr_last + 1).finish();
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
        if(i > target.getLast())
            throw new DapException("i must be <= last");
        return target.getFirst() + i * target.getStride();
    }

}
