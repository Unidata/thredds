/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr;

import dap4.core.util.DapSort;

public class DapFactoryDMR implements DapFactory
{

    //////////////////////////////////////////////////
    // Constructor
    public DapFactoryDMR()
    {
    }

    //////////////////////////////////////////////////
    // DapFactory API

    public Object newNode(DapSort sort)
    {
        return newNode(null, sort);
    }


    public Object newNode(String name, DapSort sort)
    {
        switch (sort) {
        case ATTRIBUTE:
            return new DapAttribute(name);
        case ATTRIBUTESET:
            return new DapAttributeSet(name);
        case OTHERXML:
            return new DapOtherXML(name);
        case DIMENSION:
            return new DapDimension(name);
        case ENUMERATION:
            return new DapEnum(name);
        case MAP:
            return new DapMap();   // NOTE: different signature
        case ATOMICVARIABLE:
            return new DapAtomicVariable(name);
        case GRID:
            return new DapGrid(name);
        case SEQUENCE:
            return new DapSequence(name);
        case STRUCTURE:
            return new DapStructure(name);
        case GROUP:
            return new DapGroup(name);
        case DATASET:
            return new DapDataset(name);
        default:
            break;
        }
        assert false : ("DapFactoryDefault: unknown sort: " + sort.name());
        return null;
    }

} // class DapFactoryDMR
