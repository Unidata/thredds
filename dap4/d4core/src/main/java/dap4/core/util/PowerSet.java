/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;


/**
 * Given a set of ranges, {0..1,0..1,0..2}, say,
 * generate all possible lists of values
 * 0,0,0
 * 0,0,1
 * 0,0,2
 * 0,1,0
 * 0,1,1
 * 0,1,2
 * 1,0,0
 * 1,0,1
 * 1,0,2
 * 1,1,0
 * 1,1,1
 * 1,1,2
 * The ranges are all assumed to run from 0  upto but not including some max,
 * so above would be generated from {2,2,3}.
 */

public class PowerSet
{

    static protected boolean DEBUG = false;


    protected long[][] powerset;
    protected long[] sizes;
    protected int rank;
    protected long totalsize;

    public PowerSet(long[] sizes)
    {
       this(sizes,sizes.length);
    }

    public PowerSet(long[] sizes, int count)
    {
        this.sizes = sizes;
        this.rank = count;
        this.totalsize = 1;
        for(int i = 0; i < this.rank; i++) {
            this.totalsize *= sizes[i];
        }
        this.powerset = new long[(int) this.totalsize][this.rank];
        generate();
        if(DEBUG) System.err.println(this.toString());
    }

    public long
    getTotalSize()
    {
        return this.totalsize;
    }

    public long[][]
    getPowerSet()
    {
        return powerset;
    }

    protected void
    generate()
    {
        // start at one using zero case as base
        for(int index = 1; index < this.totalsize; index++) {
            long[] ith = this.powerset[index];
            System.arraycopy(this.powerset[index - 1], 0, ith, 0, this.rank);
            for(int pos = this.rank - 1; pos >= 0; pos--) {// walk backward
                ith[pos]++;
                if(ith[pos] < this.sizes[pos]) {
                    break;
                } else
                    ith[pos]= 0;
            }
        }
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("Powerset:\n");
        for(int i = 0; i < this.totalsize; i++) {
            buf.append(String.format("[%2d]",i));
            for(int j = 0; j < this.rank; j++) {
                buf.append(String.format(" %2d", this.powerset[i][j]));
            }
            buf.append("\n");
        }
        return buf.toString();
    }

    static final long[] l0 = new long[]{2,2};
    static final long[] l1 = new long[]{2, 2, 3};

    static public void main(String[] argv)
    {
        new PowerSet(l1);
    }

}
