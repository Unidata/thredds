/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;

import java.util.ArrayList;
import java.util.List;

/**
 * DapStructure is normally used as a singleton
 * type for a variable, but for consistency,
 * we track it as a type rather than a variable
 * and create a separate variable whose basetype points
 * to it.
 * This complicates thigs because in effect a structure type
 * has two parents: 1) the group i which it is declared as a type
 * and 2) the group or structure in which the structure var
 * is declared.
 */

public class DapStructure extends DapType
{

    //////////////////////////////////////////////////
    // Instance variables

    // Use list because field ordering can be important
    List<DapVariable> fields = new ArrayList<DapVariable>();

    //////////////////////////////////////////////////
    // Constructors

    public DapStructure(String name)
    {
        super(name, TypeSort.Structure);
    }

    //////////////////////////////////////////////////
    // Accessors

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

    public int
    indexByField(DapVariable target)
    {
        for(int i = 0; i < fields.size(); i++) {
            DapVariable field = fields.get(i);
            if(field == target)
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

    /*public void setFields(DapVariable instance, List<? extends DapNode> fields)
            throws DapException
    {
        fields.clear();
        for(int i = 0; i < fields.size(); i++) {
            addField((DapVariable)fields.get(i),instance);
        }
    } */

    public boolean isLeaf()
    {
        return false;
    }

    public void addField(DapVariable newfield)
            throws DapException
    {
        DapStructure ds = this;
        for(DapVariable v : ds.fields) {
            if(v.getShortName().equals(newfield.getShortName()))
                throw new DapException("DapStructure: attempt to add duplicate field: " + newfield.getShortName());
        }
        ds.fields.add((DapVariable) newfield);
    }


} // class DapStructure
