/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import java.util.ArrayList;
import java.util.List;

public class DapOtherXML extends DapAttribute
{

    //////////////////////////////////////////////////
    // Instance Variables

    protected DapXML root = null;

    //////////////////////////////////////////////////
    // Constructors

    public DapOtherXML()
    {
        super();
    }

    public DapOtherXML(String name)
    {
        super(name);
    }

    //////////////////////////////////////////////////
    // Get/Set

    public DapXML getRoot()
    {
        return root;
    }

    public void setRoot(DapXML xml)
    {
        this.root = xml;
    }
} // class DapOtherXML

