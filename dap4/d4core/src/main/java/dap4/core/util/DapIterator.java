/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;

import dap4.core.dmr.DapNode;

import java.util.*;

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

public class DapIterator implements java.util.Iterator<DapNode>
{
    //////////////////////////////////////////////////
    // Instance Variables

    int index;
    boolean valid;
    List<DapNode> source;
    EnumSet<DapSort> sortset;

    public DapIterator(List<DapNode> source, EnumSet<DapSort> sortset)
    {
        this.source = source;
        if(sortset == null)
            sortset = EnumSet.allOf(DapSort.class);
        this.sortset = sortset;
        this.index = -1;
        valid = false;
    }

    public boolean hasNext()
    {
        if(!valid) findNext();
        return valid;
    }

    public DapNode next()
    {
        if(!valid) findNext();
        if(!valid)
             throw new NoSuchElementException();
        valid = false;  // do not reuse this element
        return source.get(index);
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    protected boolean findNext()
    {
        if(valid) return true;
        for(index++;index < source.size();index++) {
            DapNode node = source.get(index);
            if(sortset.contains(node.getSort())) {
                valid = true;
                break;
            }
        }
        return valid;
    }

}
