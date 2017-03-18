/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.ce;


import dap4.core.dmr.*;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.core.util.Slice;

import java.util.ArrayList;
import java.util.List;

/**
 * The Universal constraint is a constraint
 * that includes everything in a DMR, even
 * including items such as dimensions or enums
 * that are defined but never used.
 */

public class Universal extends CEConstraint
{

    //////////////////////////////////////////////////
    // Constructor(s)

    public Universal()
    {
    }

    public Universal(DapDataset dmr)
            throws DapException
    {
        super(dmr);
        build();
    }

    //////////////////////////////////////////////////
    // API

    @Override
    public boolean isUniversal() {return true;}

    /**
     * Finish creating this Constraint; for Universal,
     * this does nothing; the important stuff is in build().
     *
     * @throws DapException
     * @returns this - fluent interface
     */
    @Override
    public CEConstraint
    finish()
            throws DapException
    {
        super.finished = true;
        return this;
    }


    protected void
    build()
            throws DapException
    {
        List<DapVariable> top = dmr.getTopVariables();
        for(int i = 0; i < top.size(); i++) {
            DapVariable var = top.get(i);
            List<Slice> slices = DapUtil.dimsetToSlices(var.getDimensions());
            super.addVariable(var, slices);
        }
        super.enums = new ArrayList<DapEnumeration>();
        super.enums.addAll(dmr.getEnums());
        super.dimrefs = new ArrayList<DapDimension>();
        super.dimrefs.addAll(dmr.getDimensions());
        super.groups = new ArrayList<DapGroup>();
        super.groups.addAll(dmr.getGroups());
        super.expand();
        super.finish();
    }

    // Selected consult or iterator overrides for efficiency
    @Override
    public boolean references(DapNode node)
    {
        switch (node.getSort()) {
        case DIMENSION:
        case ENUMERATION:
        case VARIABLE:
        case GROUP:
        case DATASET:
            return true;
        default:
            break;
        }
        return false;
    }

    @Override
    public List<Slice>
    getConstrainedSlices(DapVariable var)
            throws DapException
    {
        List<Slice> slices = super.getConstrainedSlices(var);
        if(slices != null)
            return slices;
        // Create a universal slice
        return universalSlices(var);
    }

    static public List<Slice>
    universalSlices(DapVariable var)
            throws DapException
    {
        return DapUtil.dimsetToSlices(var.getDimensions());
    }

}
