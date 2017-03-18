/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.DapDimension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A implementation of an odometer for scalar variables.
 */

public class ScalarOdometer extends Odometer
{
    //////////////////////////////////////////////////
    // Constants

    public ScalarOdometer()
    {
        this.state = STATE.INITIAL;
        this.index = new Index(0);
        this.slices = Slice.SCALARSLICES;
    }

    public long index()
    {
        return 0;
    }

    public long totalSize()
    {
        return 1;
    }

    public boolean hasNext()
    {
        return this.state != STATE.DONE;
    }

    public Index next()
    {
        if(this.state == STATE.DONE)
            throw new NoSuchElementException();
        this.state = STATE.DONE;
        return Index.SCALAR;
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

}
