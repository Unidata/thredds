/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr;

import dap4.core.util.*;

public class DapDataFactory implements DapFactory
{
    DapFactoryDMR defaultfactory = new DapFactoryDMR();

    //////////////////////////////////////////////////
    // Constructor
    public DapDataFactory()
    {
    }

    //////////////////////////////////////////////////
    // DapFactory API

    public Object newNode(DapSort sort)
    {
        return newNode(null, sort);
    }

    public Object newNode(String ignored, DapSort sort)
    {
        switch (sort) {
        case ATOMICVARIABLE:
            return new DapAtomicVariable();
        case STRUCTURE:
            return new DapStructure();
        case SEQUENCE:
            return new DapSequence();
        case DATASET:
            return new DapDataset();
        default:
            return defaultfactory.newNode(sort);
        }
    }

}

