/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr;

import dap4.core.util.DapSort;

public interface DapFactory
{
    public Object newNode(DapSort sort);

    public Object newNode(String name, DapSort sort);
}
