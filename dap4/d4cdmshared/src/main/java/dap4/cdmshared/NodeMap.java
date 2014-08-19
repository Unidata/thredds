/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdmshared;

import dap4.core.dmr.DapNode;
import ucar.nc2.CDMNode;

import java.util.HashMap;
import java.util.Map;

/**

Provide a bi-directional 1-1 map between DapNode
instances and CDMNode instances.

There is a complication.  Currently, the hashCode() in
ucar.nc2.Variable (and other classes) is unstable when
objects are being incrementally constructed. So until and
unless that is changed, it is necessary to provide a way to
map CDMNode <-> DapNode that is independent of e.g
Variable.hashCode(); We do this by overriding hashCode() to
use Object.hashCode() explicitly.

*/

public class NodeMap
{
    //////////////////////////////////////////////////
    // Instance Variables

    /**
     * Map from DapNode -> CDMNode
     */
    Map<DapNode, CDMNode> cdmmap = new HashMap<DapNode, CDMNode>();

    /**
     * Map from CDMNode -> DapNode via a specific hashCode integer
     */
    Map<Integer, DapNode> dapmap = new HashMap<Integer, DapNode>();

    //////////////////////////////////////////////////
    // Constructor(s)

    public NodeMap() {};

    //////////////////////////////////////////////////
    // Accessors

    public DapNode get(CDMNode cdmnode)
    {
        cdmnode = CDMNode.unwrap(cdmnode);
        int lh = cdmnode.localhash();
        return dapmap.get(lh);
    }

    public CDMNode get(DapNode node) {return cdmmap.get(node);}

    public boolean containsKey(CDMNode node) {return dapmap.containsKey(node.localhash());}

    public boolean containsKey(DapNode node) {return cdmmap.containsKey(node);}

    /**
     * Given a dapnode <-> cdmnode pair, insert
     * into the maps
     * @param dapnode
     * @param cdmnode
     */
    public void put(DapNode dapnode, CDMNode cdmnode)
    {
        assert(dapnode != null && cdmnode != null);
        cdmnode = CDMNode.unwrap(cdmnode);
        int lh = cdmnode.localhash();
        dapmap.put(lh,dapnode);
        cdmmap.put(dapnode,cdmnode);
    }

    /**
     * Given a dapnode <-> cdmnode pair, remove
     * from the maps
     * @param dapnode
     * @param cdmnode
     */
    public void remove(DapNode dapnode, CDMNode cdmnode)
    {
        assert(dapnode != null && cdmnode != null);
        cdmnode = CDMNode.unwrap(cdmnode);
        dapmap.remove(cdmnode.localhash());
        cdmmap.remove(dapnode);
    }

    // Access one of the underlying maps
    public Map<DapNode,CDMNode> getCDMMap() {return cdmmap;}
//    public Map<CDMNode,DapNode> getDapMap() {return dapmap;}

}
