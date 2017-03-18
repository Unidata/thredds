/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;


import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterate the indices of a slice. Extends Iterator with some additional
 * methods.
 */

public class SliceIterator implements Iterator<Long>
{

    //////////////////////////////////////////////////
    // Constants

    static protected enum STATE
    {
        INITIAL, STARTED, DONE
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected STATE state;
    protected Slice slice;
    protected long index;

    //////////////////////////////////////////////////
    // Constructor(s)

    public SliceIterator()
    {
    }

    public SliceIterator(Slice slice)
    {
        this.slice = slice;
        reset();
    }


    public void
    reset()
    {
        this.state = STATE.INITIAL;
        this.index = slice.getFirst();
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append(slice.toString());
        buf.append(String.format("(%d/%d)", this.slice.getCount(),this.slice.getMax()));
        buf.append(String.format("@%d", this.index));
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // Iterator interface extended

    @Override
    public boolean hasNext()
    {
        switch (state) {
        case INITIAL:
            return (slice.getFirst() < slice.getStop());
        case STARTED:
            return (this.index < slice.getLast());
        case DONE:
        }
        return false;
    }

    @Override
    public Long next()
    {
        if(!hasNext())
            throw new NoSuchElementException();
        switch (this.state) {
        case INITIAL:
            this.index = slice.getFirst();
            this.state = STATE.STARTED;
            break;
        case STARTED:
            this.index += this.slice.getStride();
            if(this.index >= slice.getStop())
                this.state = STATE.DONE;
            break;
        case DONE:
            throw new NoSuchElementException();
        }
        return this.index;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    // Extended API
    public Long
    getIndex()
    {
        return this.index;
    }

}
