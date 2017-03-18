/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm;

import dap4.core.dmr.DapNode;
import ucar.nc2.CDMNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Provide a bi-directional 1-1 map between DapNode
 * instances and CDMNode instances.
 * <p>
 * There is a complication.  Currently, the hashCode() in
 * ucar.nc2.Variable (and other classes) is unstable when
 * objects are being incrementally constructed. So until and
 * unless that is changed, it is necessary to provide a way to
 * map CDMNode <-> DapNode that is independent of e.g
 * Variable.hashCode(); We do this by overriding hashCode() to
 * use Object.hashCode() explicitly.
 */

public class NodeMap<CDM_T extends CDMNode, DAP_T extends DapNode>
{
    ////b//////////////////////////////////////////////
    // Instance Variables

    /**
     * Map from DAP_T -> CDM_T
     */
    Map<DAP_T, CDM_T> cdmmap = new HashMap<>();

    /**
     * Map from CDM_T -> DAP_T via a specific hashCode integer
     */
    Map<Integer, DAP_T> dapmap = new HashMap<>();

    //////////////////////////////////////////////////
    // Constructor(s)

    public NodeMap()
    {
    }

    //////////////////////////////////////////////////
    // Accessors

    public DAP_T get(CDM_T cdm)
    {
        cdm = (CDM_T)CDMNode.unwrap(cdm);
        int lh = cdm.localhash();
        return dapmap.get(lh);
    }

    public CDM_T get(DAP_T dap)
    {
        return cdmmap.get(dap);
    }

    public boolean containsKey(CDM_T node)
    {
        return dapmap.containsKey(node.localhash());
    }

    public boolean containsKey(DAP_T node)
    {
        return cdmmap.containsKey(node);
    }

    /**
     * Given a CDM_T <-> DAP_T pair, insert
     * into the maps
     *
     * @param cdm
     * @param dap
     */
    public void put(CDM_T cdm, DAP_T dap)
    {
        assert (dap != null && cdm != null);
        cdm = (CDM_T)CDMNode.unwrap(cdm);
        int lh = cdm.localhash();
        dapmap.put(lh, dap);
        cdmmap.put(dap, cdm);
    }

    /**
     * Given a DAP_T <-> CDM_T pair, remove
     * from the maps
     * @param cdm
     * @param dap
     */
    public void remove(CDM_T cdm, DAP_T dap)
    {
        assert (dap != null && cdm != null);
        cdm = (CDM_T)CDMNode.unwrap(cdm);
        dapmap.remove(cdm.localhash());
        cdmmap.remove(dap);
    }

    // Access one of the underlying maps Needed when trying to do enumeration matching
    public Map<DAP_T,CDM_T> getCDMMap() {return cdmmap;}
//    public Map<CDM_T,DAP_T> getDapMap() {return dapmap;}

}
