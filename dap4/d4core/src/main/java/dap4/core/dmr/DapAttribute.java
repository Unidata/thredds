/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import java.util.ArrayList;
import java.util.List;

public class DapAttribute extends DapNode
{

    //////////////////////////////////////////////////
    // Instance Variables


    protected List<String> namespaceList = new ArrayList<String>();

    protected DapType basetype = null;
    protected String[] valuelist = null;

    //////////////////////////////////////////////////
    // Constructors

    public DapAttribute()
    {
    }

    public DapAttribute(String name, DapType basetype)
    {
        super(name);
        setBaseType(basetype);
    }

    //////////////////////////////////////////////////
    // Get/Set

    public List<String> getNamespaceList()
    {
        return namespaceList;
    }

    public void setNamespaceList(List<String> list)
    {
        if(list == null) return;
        namespaceList.clear();
        for(String ns : list) {
            addNamespace(ns);
        }
    }

    public void
    addNamespace(String ns)
    {
        if(!namespaceList.contains(ns))
            namespaceList.add(ns);
    }

    public DapType getBaseType()
    {
        return basetype;
    }

    public void setBaseType(DapType basetype)
    {
        this.basetype = basetype;
    }

    public String[] getValues()
    {
        return valuelist;
    }

    public void clearValues()
    {
        valuelist = null;
    }

    public DapAttribute setValues(String[] o)
    {
        this.valuelist = o;
        return this;
    }

} // class DapAttribute

