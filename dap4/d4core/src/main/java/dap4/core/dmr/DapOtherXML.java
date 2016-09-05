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

    protected Object root = null;

    //////////////////////////////////////////////////
    // Constructors

    public DapOtherXML()
    {
        super();
    }

    public DapOtherXML(String name)
    {
        super(name,null);
    }

    //////////////////////////////////////////////////
    // Get/Set

    public Object getRoot()
    {
        return root;
    }

    public void setRoot(Object xml)
    {
        this.root = xml;
    }
}

