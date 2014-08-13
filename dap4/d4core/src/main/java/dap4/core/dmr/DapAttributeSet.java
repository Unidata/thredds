/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import java.util.ArrayList;
import java.util.List;

public class DapAttributeSet extends DapAttribute
{

    //////////////////////////////////////////////////
    // Instance Variables

    //////////////////////////////////////////////////
    // Constructors

    public DapAttributeSet()
    {
	super();
    }

    public DapAttributeSet(String name)
    {
        super(name);
    }

    //////////////////////////////////////////////////
    // Get/Set

    // The set of contained attributes are stored in DapNode.attributes
    public DapAttribute getAttribute(String name)
    {
	return attributes.get(name);
    }

} // class DapAttribute

