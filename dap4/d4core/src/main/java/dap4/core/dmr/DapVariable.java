/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;
import dap4.core.util.DapSort;
import dap4.core.util.DapUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents any variable or field.
 */

public class DapVariable extends DapNode implements DapDecl
{

    //////////////////////////////////////////////////
    // Instance Variables

    protected DapType basetype = null;
    protected List<DapDimension> dimensions = new ArrayList<DapDimension>();
    protected List<DapMap> maps = new ArrayList<DapMap>(); // maps are ordered
    protected int checksum = 0;
    protected int fieldindex = -1;

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

    public DapVariable(String name, DapType basetype)
    {
        super(name);
        setBaseType(basetype);
    }

    //////////////////////////////////////////////////
    // Accessors

    public DapType
    getBaseType()
    {
        return this.basetype;
    }

    public DapVariable
    setBaseType(DapType t)
    {
        this.basetype = t;
        return this;
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
            throw new IllegalArgumentException("Illegal index: " + i);
        return this.dimensions.get(i);
    }

    public void addDimension(DapDimension node)
            throws DapException
    {
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

    public int
    getChecksum()
    {
        return this.checksum;
    }

    public void
    setChecksum(int csum)
    {
        this.checksum = csum;
    }

    public int
    getFieldIndex()
    {
        return this.fieldindex;
    }

    public void
    setFieldIndex(int index)
    {
        this.fieldindex = index;
    }

    @Override
    public String
    toString()
    {
        StringBuilder s = new StringBuilder();
        if(this.getBaseType() != null) {
            s.append(this.getBaseType().toString());
            s.append("|");
        }
        s.append(super.toString());
        for(int i = 0; i < getRank(); i++) {
            DapDimension dim = dimensions.get(i);
            if(dim == null) // should never happen
                s.append("(null)");
            else
                s.append(String.format("(%d)", dim.getSize()));
        }
        return s.toString();
    }

    public DapType getTrueBaseType()
    {
        DapType bt = getBaseType();
        if(bt.getTypeSort() == TypeSort.Enum)
            return ((DapEnumeration) bt).getBaseType();
        else
            return bt;
    }

    public boolean isLeaf()
    {
        return (this.isAtomic());
    }

    // convenience
    public boolean
    isAtomic()
    {
        return (getBaseType() == null ? false : getBaseType().getTypeSort().isAtomic());
    }

    public boolean
    isEnum()
    {
        return (getBaseType() == null ? false : getBaseType().getTypeSort().isEnumType());
    }

    public boolean
    isSequence()
    {
        return (getBaseType() == null ? false : getBaseType().getTypeSort().isSeqType());
    }

    public boolean
    isStructure()
    {
        return (getBaseType() == null ? false : getBaseType().getTypeSort().isStructType());
    }

    public boolean
    isCompound()
    {
        return (isStructure() || isSequence());
    }

} // class DapVariable

