/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;

import dap4.core.dmr.DapDimension;

/**
 * A Slice is used for two purposes
 * <ol>
 * <li> To specify a subset of data to extract.
 * <li> To specify a subset of data as specified in a constraint.
 * </ol>
 * In case two, the "last" value of the slice may be undefined
 * (e.q. specifying [0:*]) and so they must be filled in at some point.
 * Note that first cannot be undefined because it always defaults to zero;
 * similarly stride defaults to one.
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

    //////////////////////////////////////////////////
    // Instance variables

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
     * Indicate if this is known to be a whole slice
     * Boolean.TRUE => yes
     * Boolean.FALSE => no
     * null => unknown
     */
    Boolean whole = null;

    /**
     * Non-null if this slice is associated with a shared DapDimension.
     */
//    DapDimension srcdim = null;

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
        this();
        setIndices(first, last, stride);
    }

    public Slice(Slice s)
        throws DapException
    {
        this();
        setIndices(s.getFirst(), s.getLast(), s.getStride());
        setConstrained(s.isConstrained());
        setWhole(s.isWhole());
    }

/*
    public Slice(DapDimension dim)
        throws DapException
    {
        this();
        setSourceDimension(dim);
        setIndicesFrom(dim); // fill in the rest of the info
        setWhole(dim.isShared());
    }
*/

    //////////////////////////////////////////////////
    // Accessors

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


    public Slice complete(DapDimension dim)
    {
        if(this.last == UNDEFINED)
            this.last = dim.getSize()-1;
        if(this.first == UNDEFINED)
            this.first = 0;
        if(this.stride == UNDEFINED)
            this.stride = 1;
        return this; // fluent interface
    }


    public Slice fill(DapDimension dim)
        throws DapException
    {
        setIndices(0, dim.getSize() - 1, 1);
        setWhole(true);
        setConstrained(false);
        return this; // fluent interface
    }


    public Slice setIndices(long first, long last, long stride)
        throws DapException
    {
        assert first != UNDEFINED;
        assert stride != UNDEFINED;
        this.first = first;
        this.last = last;
        this.stride = stride;
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

    public Slice setWholeWRT(long maxsize)
    {
        if(last == 0 && stride == 1 && (last + 1) == maxsize)
            whole = Boolean.TRUE;
        else
            whole = Boolean.FALSE;
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

    //////////////////////////////////////////////////
    // Computed accessors

    // Determine if this slice is incomplete; this is independent
    public boolean incomplete()
    {
        return (getLast() == Slice.UNDEFINED);
    }

/*
    public boolean isShared()
    {
        return (this.srcdim != null && this.srcdim.isShared());
    }
*/

    //////////////////////////////////////////////////
    // Misc. methods

    /**
     * Compute the number of elements int
     * the slice. Note that this is different from
     * the getLast()+1
     */
    public long getCount()
    {
        long count = (getLast() + 1) - getFirst();
        count = (count + getStride() - 1);
        count /= stride;
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
        return other.getFirst() == getFirst()
            && other.getLast() == getLast()
            && other.getStride() == getStride();
    }

    @Override
    public String toString()
    {
        String slast = getLast() == UNDEFINED ? "?" : Long.toString(getLast());
        String sstride = getStride() == UNDEFINED ? "?" : Long.toString(getStride());
        String sfirst = getFirst() == UNDEFINED ? "?" : Long.toString(getFirst());
        if(getStride() == 1)
            return String.format("[%s:%s]", sfirst, slast);
        else
            return String.format("[%s:%s:%s]", sfirst, sstride, slast);
    }

    /**
     * Convert this slice to a bracketed string
     * suitable for use in a constraint
     *
     * @return constraint usable string
     */
    public String toConstraintString()
    {
        if(incomplete())
            return "[]";
        if(getStride() == 1) {
	    if(getFirst() == getLast())
                return String.format("[%d]", getFirst());
	    else
                return String.format("[%d:%d]", getFirst(), getLast());
        } else
            return String.format("[%d:%d:%d]", getFirst(), getStride(), getLast());
    }

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
        long sr_last = (target.getLast() < lastx ? target.getLast()
            : lastx); //min(last(),lastx)
        return new Slice(sr_first, sr_last, sr_stride).validate();
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

    /**
     * Perform sanity checks on a slice
     *
     * @throws DapException if slice is malformed
     */
    public Slice
    validate()
        throws DapException
    {
        long first = getFirst();
        long last = getLast();
        long stride = getStride();
        assert (first != UNDEFINED);
        assert (stride != UNDEFINED);
        assert (last != UNDEFINED);
        if(first > MAXLENGTH)
            throw new DapException("Slice: first index > MAXLENGTH");
        if(stride > MAXLENGTH)
            throw new DapException("Slice: stride > MAXLENGTH");
        if(last > MAXLENGTH)
            throw new DapException("Slice: last > MAXLENGTH");
        if(first < 0)
            throw new DapException("Slice: first index < 0");
        if(last < 0)
            throw new DapException("Slice: last index < 0");
        if(stride <= 0)
            throw new DapException("Slice: stride index <= 0");
        if(first > last)
            throw new DapException("Slice: first index > last");
/*
        if(srcdim != null) {
            if(last != UNDEFINED && srcdim.getSize() <= last)
                throw new DapException("Slice: last index >= "
                    + srcdim.getShortName()
                    + ".getSize()");
        }
*/
        return this; // fluent interface
    }

}
