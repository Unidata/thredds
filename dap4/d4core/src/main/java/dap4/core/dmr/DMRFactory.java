/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr;

public class DMRFactory
{
    /////////////////////////////////////////////////

    static public boolean DEBUG = false;

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
        DapAttribute node = new DapAttribute(name, basetype);
        return node;
    }

    public DapAttributeSet
    newAttributeSet(String name)
    {
        DapAttributeSet node = new DapAttributeSet(name);
        if(DEBUG) debug(node);
        return node;
    }

    public DapOtherXML
    newOtherXML(String name)
    {
        DapOtherXML node = new DapOtherXML(name);
        if(DEBUG) debug(node);
        return node;
    }

    public DapDimension
    newDimension(String name, long size)
    {
        DapDimension node = new DapDimension(name, size);
        if(DEBUG) debug(node);
        return node;
    }

    public DapMap
    newMap(DapVariable target)
    {
        DapMap node = new DapMap(target);
        if(DEBUG) debug(node);
        return node;
    }

    public DapVariable
    newVariable(String name, DapType t)
    {
        DapVariable node = new DapVariable(name, t);
        if(DEBUG) debug(node);
        return node;
    }

    public DapGroup
    newGroup(String name)
    {
        DapGroup node = new DapGroup(name);
        if(DEBUG) debug(node);
        return node;
    }

    public DapDataset
    newDataset(String name)
    {
        DapDataset node = new DapDataset(name);
        if(DEBUG) debug(node);
        return node;
    }

    public DapEnumeration
    newEnumeration(String name, DapType basetype)
    {
        DapEnumeration node = new DapEnumeration(name, basetype);
        if(DEBUG) debug(node);
        return node;
    }

    public DapEnumConst
    newEnumConst(String name, long value)
    {
        DapEnumConst node = new DapEnumConst(name, value);
        if(DEBUG) debug(node);
        return node;
    }

    public DapStructure
    newStructure(String name)
    {
        DapStructure node = new DapStructure(name);
        if(DEBUG) debug(node);
        return node;
    }

    public DapSequence
    newSequence(String name)
    {
        DapSequence node = new DapSequence(name);
        if(DEBUG) debug(node);
        return node;
    }

    protected void
    debug(DapNode node)
    {
        System.err.printf("NEW: %s%n", node.toString());
    }

}

