/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr;

public class DMRFactory
{
    //////////////////////////////////////////////////
    // Constructor
    public DMRFactory()
    {
    }

    //////////////////////////////////////////////////
    // DMRFactory API

    public DapAttribute
    newAttribute(String name, DapType basetype)
    {
        return new DapAttribute(name, basetype);
    }

    public DapAttributeSet
    newAttributeSet(String name)
    {
        return new DapAttributeSet(name);
    }

    public DapOtherXML
    newOtherXML(String name)
    {
        return new DapOtherXML(name);
    }

    public DapDimension
    newDimension(String name, long size)
    {
        return new DapDimension(name, size);
    }

    public DapMap
    newMap(DapVariable target)
    {
        return new DapMap(target);
    }

    public DapAtomicVariable
    newAtomicVariable(String name, DapType t)
    {
        return new DapAtomicVariable(name, t);
    }

    public DapVariable
    newStructureVariable(String name, DapType t)
    {
        return new DapStructure(name);
    }

    public DapVariable
    newSequenceVariable(String name, DapType t)
        {
            return new DapSequence(name);
        }

    public DapGroup
    newGroup(String name)
    {
        return new DapGroup(name);
    }

    public DapDataset
    newDataset(String name)
    {
        return new DapDataset(name);
    }

    public DapEnumeration
    newEnumeration(String name, DapType basetype)
    {
        return new DapEnumeration(name, basetype);
    }

    public DapEnumConst
    newEnumConst(String name, long value)
    {
        return new DapEnumConst(name, value);
    }

    public DapStructure
    newStructure(String name)
    {
        return new DapStructure(name);
    }

    public DapSequence
    newSequence(String name)
    {
        return new DapSequence(name);
    }

}

