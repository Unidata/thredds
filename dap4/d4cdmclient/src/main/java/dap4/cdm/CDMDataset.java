/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package   dap4.cdm;

import dap4.cdmshared.NodeMap;
import ucar.ma2.Array;
import ucar.nc2.CDMNode;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrap D4DSP to project the CDM API.
 * CDMDataset is the top level, root
 * object that manages the whole
 * databuffer part of a CDM wrap of a dap4 response.
 * It is never seen by the client
 * and it is not related to ucar.nc2.Array.
 */

public class CDMDataset
{
    /////////////////////////////////////////////////////
    // Constants

    /////////////////////////////////////////////////////

    protected Map<CDMNode, Array> arraymap = new HashMap<CDMNode, Array>();

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * Constructor
     */
    CDMDataset()
    {

    }

    //////////////////////////////////////////////////
    // Accessors

    public Map<CDMNode, Array> getArrayMap()
    {
        return arraymap;
    }

    public void putArray(CDMNode node, Array array)
    {
        arraymap.put(node, array);
    }

    public Array getArray(CDMNode node)
    {
        return arraymap.get(node);
    }

    //////////////////////////////////////////////////
    // toString

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("Dataset {\n");
        /*if(annotations != null) {
            View.Iterator iter = annotations.getIterator();
            while(iter.hasNext()) {
                DapVariable dapvar = iter.next().getVariable();
                if(!dapvar.isTopLevel()) continue;
                CDMNode cdmnode = nodemap.get(dapvar);
                Dap4Array array = (Dap4Array) arraymap.get(cdmnode);
                if(array != null)
                    buf.append(array.toString() + "\n");
            }
        } */
        buf.append("}");
        return buf.toString();
    }

}
