/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import java.util.Arrays;

public class Index
{
    static public final Index SCALAR = new Index(0);

    public int rank;
    public long[] indices; // allow direct access
    public long[] dimsizes; // allow direct access

    //////////////////////////////////////////////////
    // Constructor(s)

    public Index(int rank)
    {
        this.rank = rank;
        this.dimsizes = new long[rank];
        indices = new long[rank];
        if(this.rank > 0) {
            Arrays.fill(indices, 0);
            Arrays.fill(dimsizes, 0);
        }
    }

    public Index(Index index)
    {
        this(index.getRank());
        if(this.rank > 0) {
            System.arraycopy(index.indices, 0, this.indices, 0, this.rank);
            System.arraycopy(index.dimsizes, 0, this.dimsizes, 0, this.rank);
        }
    }

    public Index(long[] indices, long[] dimsizes)
    {
        this(dimsizes.length);
        if(this.rank > 0) {
            System.arraycopy(indices, 0, this.indices, 0, this.rank);
            System.arraycopy(dimsizes, 0, this.dimsizes, 0, this.rank);
        }
    }

    public String
    toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        for(int i = 0; i < this.rank; i++) {
            if(i > 0) buf.append(',');
            buf.append(indices[i]);
            buf.append('/');
            buf.append(dimsizes[i]);
        }
        buf.append("](");
        buf.append(this.index());
        buf.append(")");
        return buf.toString();
    }


    /**
     * Compute the linear index
     * from the current odometer indices.
     */
    public long
    index()
    {
        long offset = 0;
        for(int i = 0; i < this.indices.length; i++) {
            offset *= this.dimsizes[i];
            offset += this.indices[i];
        }
        return offset;
    }

    public int getRank()
    {
        return this.rank;
    }

    public long
    get(int i)
    {
        if(i < 0 || i >= this.rank)
            throw new IllegalArgumentException();
        return this.indices[i];
    }

    public long
        getSize(int i)
        {
            if(i < 0 || i >= this.rank)
                throw new IllegalArgumentException();
            return this.dimsizes[i];
        }

    public boolean isScalar()
    {
        return (rank == 0 && indices.length == 1 && index() == 1);
    }


}
