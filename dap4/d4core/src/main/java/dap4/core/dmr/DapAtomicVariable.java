/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

/**
 * This class defines a non-structured variable:
 * i.e. one with an atomic type.
 */

public class DapAtomicVariable extends DapVariable
{

    //////////////////////////////////////////////////
    // Constructors

    public DapAtomicVariable()
    {
        super();
    }

    public DapAtomicVariable(String name, DapType basetype)
    {
        super(name);
        this.basetype = basetype;
    }

    public boolean isLeaf()
    {
        return true;
    }

    public DapType getTrueBaseType()
    {
        DapType bt = getBaseType();
        if(bt.getTypeSort() == TypeSort.Enum)
            return ((DapEnumeration) bt).getBaseType();
        else
            return bt;
    }


}


