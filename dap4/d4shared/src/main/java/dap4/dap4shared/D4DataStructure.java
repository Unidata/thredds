/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.DapStructure;

public class D4DataStructure extends D4DataVariable implements DataStructure
{
    //////////////////////////////////////////////////
    // Type Decls

/*    
    static class Field
    {
        DapVariable field;
        StructureMembers.Member member;
        int index;

        public Field(DapVariable field, int index, StructureMembers.Member member)
        {
            this.field = field;
            this.member = member;
            this.index = index;
        }
    }
*/

    //////////////////////////////////////////////////
    // Instance variables

    protected D4DataCompoundArray parent = null;
    //Coverity[FB.URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD]
    protected int index = 0;
    protected D4DataVariable[] fielddata = null;

    //////////////////////////////////////////////////
    // Constructors

    public D4DataStructure(D4DSP dsp, DapStructure dap, D4DataCompoundArray parent, int index)
        throws DataException
    {
        super(dsp, dap);
        this.parent = parent;
        this.index = index;
        this.fielddata = new D4DataVariable[dap.getFields().size()];
    }

    //////////////////////////////////////////////////
    // Accessors

    public void
    addField(int mindex, D4DataVariable ddv)
    {
        if(fielddata[mindex] != null)
            throw new IllegalStateException("duplicate fields");
        fielddata[mindex] = ddv;
    }

    //////////////////////////////////////////////////
    // DataStructure Interface

    @Override
    public DataVariable readfield(String name) throws DataException
    {
        int index = ((DapStructure)this.getTemplate()).indexByName(name);
        return readfield(index);
    }

    @Override
    public DataVariable readfield(int index)
        throws DataException
    {
        D4DataVariable ddv = fielddata[index];
        return ddv;
    }

}
