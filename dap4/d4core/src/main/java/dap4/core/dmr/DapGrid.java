/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;
import dap4.core.util.DapSort;

/**
 * This class defines a non-Gridd Grid:
 * i.e. one with an atomic type.
 */

public class DapGrid extends DapStructure
{

//////////////////////////////////////////////////
// Constructors

    public DapGrid()
    {
        super();
    }

    public DapGrid(String name)
    {
        super(name);
    }

//////////////////////////////////////////////////
// Get/Set

    @Override
    public void
    addField(DapNode node)
        throws DapException
    {
        if(node.getSort() != DapSort.ATOMICVARIABLE)
            throw new DapException("DapGrid: Attempt to add non atomic field");
        super.addField(node);
    }

    /**
     * Convenience Functions.
     */

    public DapVariable
    getArray()
    {
        if(fields.size() > 0)
            return fields.get(0);
        else
            return null;
    }

    /**
     * Warning: map indices start at zero, not one.
     */
    public DapVariable
    getMap(int index)
    {
        index++; // make it the true index
        if(fields.size() > index)
            return fields.get(index);
        else
            return null;
    }

    @Override
    public void addDimension(DapDimension node)
        throws DapException
    {
        throw new DapException("DapGrid: Grids may not have dimensions");
    }

} // class DapGrid

