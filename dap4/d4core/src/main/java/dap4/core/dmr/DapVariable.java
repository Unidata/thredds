/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a utility to unify
 * the structured and atomic typed variables.
 */

abstract public class DapVariable extends DapNode implements DapDecl
{

    //////////////////////////////////////////////////
    // Instance Variables

    protected DapType basetype = null;
    protected List<DapDimension> dimensions = new ArrayList<DapDimension>();
    protected List<DapMap> maps = new ArrayList<DapMap>(); // maps are ordered
    protected byte[] checksum = null;

    //////////////////////////////////////////////////
    // Constructors

    public DapVariable()
    {
        super();
    }

    public DapVariable(String name)
    {
        super(name);
    }

    //////////////////////////////////////////////////
    // Accessors

    public DapType getBaseType()
    {
        return basetype;
    }

    public void setBaseType(DapType basetype)
    {
        this.basetype = basetype;
    }

    public DapType getTrueBaseType()
    {
        DapType bt = getBaseType();
        if(bt.getAtomicType() == AtomicType.Enum)
            return ((DapEnum) bt).getBaseType();
        else
            return bt;
    }

    public int getRank()
    {
        return dimensions.size();
    }

    public long getCount()   // dimension crossproduct
    {
        return DapUtil.dimProduct(getDimensions());
    }

    public List<DapDimension> getDimensions()
    {
        return dimensions;
    }

    public DapDimension getDimension(int i)
    {
        if(this.dimensions == null
            || i < 0 || i >= this.dimensions.size())
            throw new IllegalArgumentException("Illegal index: "+i);
        return this.dimensions.get(i);
    }

    public void addDimension(DapDimension node)
        throws DapException
    {
        // Enforce rulel that a Variable length dimension
        // must be last
        for(DapDimension d : dimensions) {
            if(d.isVariableLength())
                throw new DapException("Variable length dimension must always be last");
        }
        dimensions.add(node);
    }

    public List<DapMap> getMaps()
    {
        return maps;
    }

    public void addMap(DapMap map)
        throws DapException
    {
        if(maps.contains(map))
            throw new DapException("Duplicate map variables: " + map.getFQN());
        maps.add(map);
    }

    public byte[]
    getChecksum()
    {
        return this.checksum;
    }

    public void
    setChecksum(byte[] csum)
    {
        this.checksum = csum;
    }

    @Override
    public String
    toString()
    {
        StringBuilder s = new StringBuilder();
        s.append(super.toString());
        for(int i = 0;i < getRank();i++) {
            DapDimension dim = dimensions.get(i);
            if(dim == null) // should never happen
                s.append("(null)");
            else
                s.append(String.format("(%d)", dim.getSize()));
        }
        return s.toString();
    }

    abstract public boolean isLeaf();

} // class DapVariable

