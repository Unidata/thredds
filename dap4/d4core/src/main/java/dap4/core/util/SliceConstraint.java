/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import java.util.*;

/**
 * SliceConstraint hold a List<List<Slice>>.
 * It is designed to represent a list of  "slicesets"
 * For example {[x:y],[a:b]},{[z],[w]}
 * It also provides for iteration over the sets
 * to generate all possible cases:
 * e.g. [x:y][z] [x:y][w] [a:b][z] [a:b][w]
 */

public class SliceConstraint
{

    //////////////////////////////////////////////////
    // Instance variables

    protected int rank = 0;
    protected List<List<Slice>> slicesets = null;
    protected int pos[] = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public SliceConstraint()
    {
    }

    public SliceConstraint(int rank)
            throws DapException
    {
        this.slicesets = new ArrayList<List<Slice>>();
        this.rank = rank;
        this.pos = new int[this.rank];
        Arrays.fill(this.pos, 0);
    }

    public SliceConstraint(List<List<Slice>> ss)
            throws DapException
    {
        this(ss.size());
        for(int i = 0; i < this.rank; i++) {
            add(ss.get(i));
        }
    }

    //////////////////////////////////////////////////
    // Limited set of List operations

    public String
    toString()
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < this.rank; i++) {
            List<Slice> slices = this.slicesets.get(i);
            buf.append("[");
            for(int j = 0; j < slices.size(); j++) {
                if(j > 0)
                    buf.append(",");
                buf.append(slices.get(j).toString());
            }
            buf.append("]");
        }
        return buf.toString();
    }

    protected void add(List<Slice> slices)
            throws DapException
    {
        if(slices == null || slices.size() == 0)
            throw new DapException("Null slice set");
        if(this.slicesets.size() == this.rank)
            throw new DapException("Sliceset overflow");
        this.slicesets.add(slices);
    }

    public int
    getRank()
    {
        return this.rank;
    }

    public List<Slice>
    get(int i)
    {
        return slicesets.get(i);
    }

    public Iterator<List<Slice>>
    iterator()
    {
        return new SliceCEIterator(this);
    }

    //////////////////////////////////////////////////
    // Iterator subclass

    static public class SliceCEIterator implements Iterator<List<Slice>>
    {

        protected SliceConstraint sce;
        protected int rank;
        protected int[] pos;

        public SliceCEIterator(SliceConstraint sce)
        {
            this.sce = sce;
            this.rank = sce.getRank();
            pos = new int[this.rank];
            Arrays.fill(pos, 0);
        }

        public boolean hasNext()
        {
            for(int i = 0; i < this.rank; i++) {
                List<Slice> innerset = sce.get(i);
                if(this.pos[i] < innerset.size())
                    return true;
            }
            return false;
        }

        public List<Slice> next()
        {
            List<Slice> result = new ArrayList<Slice>();
            int index = -1;
            for(int i = this.rank - 1; i >= 0; i--) {
                List<Slice> innerset = sce.get(i);
                if(this.pos[i] >= innerset.size()) {
                    this.pos[i] = 0;
                } else {
                    index = i;
                    this.pos[i]++;
                    break;
                }
            }
            if(index < 0)
                throw new NoSuchElementException();
            for(int i = 0; i < this.rank; i++) {
                List<Slice> innerset = sce.get(i);
                result.add(innerset.get(this.pos[i]));
            }
            return result;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }


}
