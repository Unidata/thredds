/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.cdm.dsp;

import dap4.core.dmr.*;

public class CDMDMR
{
    //////////////////////////////////////////////////

    static public class CDMAttribute extends DapAttribute
    {
        public CDMAttribute(String name, DapType basetype)
        {
            super(name, basetype);
        }
    }

    static public class CDMAttributeSet extends DapAttributeSet
    {
        public CDMAttributeSet(String name)
        {
            super(name);
        }
    }

    static public class CDMDimension extends DapDimension
    {
        public CDMDimension(String name, long size)
        {
            super(name, size);
        }
    }

    static public class CDMMap extends DapMap
    {
        public CDMMap(DapVariable target)
        {
            super(target);
        }
    }

    abstract static public class CDMVariable extends DapVariable
    {
        public CDMVariable(String name, DapType t)
        {
            super(name, t);
        }
    }

    static public class CDMGroup extends DapGroup
    {
        public CDMGroup(String name)
        {
            super(name);
        }
    }

    static public class CDMDataset extends DapDataset
    {
        public CDMDataset(String name)
        {
            super(name);
        }
    }

    static public class CDMEnumeration extends DapEnumeration
    {
        public CDMEnumeration(String name, DapType basetype)
        {
            super(name, basetype);
        }
    }

    static public class CDMEnumConst extends DapEnumConst
    {
        public CDMEnumConst(String name, long value)
        {
            super(name, value);
        }
    }

    static public class CDMStructure extends DapStructure
    {
        public CDMStructure(String name)
        {
            super(name);
        }
    }

    static public class CDMSequence extends DapSequence
    {
        public CDMSequence(String name)
        {
            super(name);
        }
    }

    static public class CDMOtherXML extends DapOtherXML
    {
        public CDMOtherXML(String name)
        {
            super(name);
        }
    }
}
