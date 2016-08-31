/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;
import dap4.core.util.DapSort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
DapStructure is normally used as a singleton
type for a variable; hence it subclasses DapVariable.
However, when wrapping e.g. NetCDF-4, it can also act
as a template representing a NetCDF-4 compound type.
In this latter capacity, it can be "cloned" to produce
variables with structure identical to the template.
*/

public class DapStructure extends DapVariable
{

    //////////////////////////////////////////////////
    // Instance variables

    // Use list because field ordering can be important
    List<DapVariable> fields = new ArrayList<DapVariable>();

    // When representing some kinds of underlying data, this
    // may be considered a template rather than a true variable
    protected boolean istemplate = false;

    //////////////////////////////////////////////////
    // Constructors

    public DapStructure()
    {
        super();
        this.basetype = DapType.STRUCTURE;
    }

    public DapStructure(String name)
    {
        super(name);
        this.basetype = DapType.STRUCTURE;
    }

    //////////////////////////////////////////////////
    // Accessors

    public void setTemplate(boolean tf)
    {
        this.istemplate = tf;
    }

    public boolean isTemplate()
    {
        return this.istemplate;
    }

    public DapVariable
    findByName(String shortname)
    {
        for(DapVariable field : fields) {
            if(shortname.equals(field.getShortName()))
                return field;
        }
        return null;
    }

    public int
    indexByName(String shortname)
    {
        for(int i = 0; i < fields.size(); i++) {
            DapVariable field = fields.get(i);
            if(shortname.equals(field.getShortName()))
                return i;
        }
        return -1;
    }

    public DapVariable getField(int i)
    {
        return fields.get(i);
    }

    public List<DapVariable> getFields()
    {
        return fields;
    }

    public void setFields(List<? extends DapNode> fields)
            throws DapException
    {
        fields.clear();
        for(int i = 0; i < fields.size(); i++) {
            addField(fields.get(i));
        }
    }

    public void addField(DapNode newfield)
            throws DapException
    {
        if(!(newfield instanceof DapVariable))
            throw new ClassCastException("DapVariable");
        for(DapVariable v : fields) {
            if(v.getShortName().equals(newfield.getShortName()))
                throw new DapException("DapStructure: attempt to add duplicate field: " + newfield.getShortName());
        }
        fields.add((DapVariable) newfield);
        newfield.setParent(this);
    }

    public boolean isLeaf()
    {
        return false;
    }


} // class DapStructure
