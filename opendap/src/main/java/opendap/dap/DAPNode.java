/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////


package opendap.dap;

import ucar.nc2.util.EscapeStrings;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The DAPNode class is the common parent type for
 * all nodes in the DDS and the DAS. It is used to manage
 * the following elements.
 * 1. Names - both encoded and clear
 * 2. Cloning - it implements the single clone procedure
 *    and converts it to calls to cloneDAG.
 * 3. Parent - this was moved here from BaseType
 *    because it (should) represent the only cyclic pointer
 *    in the tree.
 * 4. projection - this really only for server nodes.
 *    it should be removed when we quit using cloning.
 *
 * @author dmh (Dennis Heimbigner, Unidata)
 * @version $Revision: 22951 $
 */

public class DAPNode implements Cloneable, Serializable
{
    //////////////////////////////////////////////////////////////////////////
    static public org.slf4j.Logger log
                = org.slf4j.LoggerFactory.getLogger(DAPNode.class);
    //////////////////////////////////////////////////////////////////////////

    static final long serialVersionUID = 1;

    // Define a singleton value for which we can test with ==
    static DAPNode NULLNODE = new DAPNode("null");

    /**
     * The name of this variable - not www enccoded
     */
    protected String _nameClear; //with all escapes removed
    protected String _nameEncoded; // with illegal characters escaped

    /**
     * The parent (container class) of this object, if one exists
     */
    private DAPNode _myParent;

    /**
     * Track if this variable is being used as part of a projection.
     */
    private boolean projected = false;

    /**
     * Constructs a new <code>DAPNode</code> with no name.
     */
    public DAPNode() {
        this(null);
    }

    /**
     * Constructs a new <code>DAPNode</code> with name <code>n</code>.
     * Name is assumed to never be DAP encoded
     * @param n the name of the variable.
     */
    public DAPNode(String n)
    {
        _myParent = null;
        setClearName(n);
    }

    public void setProjected(boolean tf)
    {
        projected = tf;
    }


    /**
     * Check the projection state of this variable.
     * Is the given variable marked as projected? If the variable is listed
     * in the projection part of a constraint expression, then the CE parser
     * should mark it as <em>projected</em>. When this method is called on
     * such a variable it should return <code>true</code>, otherwise it
     * should return <code>false</code>.
     *
     * @return <code>true</code> if the variable is part of the current
     *         projections, <code>false</code> otherwise.
     * @see opendap.servers.CEEvaluator
     */
    public boolean isProject() {
        return (projected);
    }

    /**
      * Set the state of this variable's projection. <code>true</code> means
      * that this variable is part of the current projection as defined by
      * the current constraint expression, otherwise the current projection
      * for this variable should be <code>false</code>.
      *
      * @param state <code>true</code> if the variable is part of the current
      *              projection, <code>false</code> otherwise.
      * @param all   If <code>true</code>, set the Project property of all the
      *              members (and their children, and so on).
      * @see opendap.servers.CEEvaluator
      */
     public void setProject(boolean state, boolean all) {
         setProjected(state);
     }

     /**
      * Set the state of this variable's projection. <code>true</code> means
      * that this variable is part of the current projection as defined by
      * the current constraint expression, otherwise the current projection
      * for this variable should be <code>false</code>. <p>
      * This is equivalent to setProjection(<code>state</code>,
      * <code>true</code>).
      *
      * @param state <code>true</code> if the variable is part of the current
      *              projection, <code>false</code> otherwise.
      * @see opendap.servers.CEEvaluator
      */
     public void setProject(boolean state) {
         setProject(state, true);
     }


    public void setParent(DAPNode bt) {
        _myParent = bt;
    }

    public DAPNode getParent() {
        return (_myParent);
    }

    /**
     * Returns the unencoded name of the class instance.
     *
     * @return the name of the class instance.
     */
    public final String getClearName() {
        return _nameClear;
    }

    /**
     * Returns the WWW encoded name of the class instance.
     *
     * @return the name of the class instance.
     */
    public final String getEncodedName() {
        return _nameEncoded;
    }

    /**
     * Sets the name of the class instance.
     *
     * @param n the name of the class instance; with escapes
     */
    public final void setEncodedName(String n)
    {
	     _nameEncoded = n;
         _nameClear = EscapeStrings.unescapeDAPIdentifier(n);
    }

    /**
     * Sets the unencoded name of the class instance.
     *
     * @param n the unencoded name of the class instance.
     */
    public  void setClearName(String n) {
        _nameClear = n;
       _nameEncoded = EscapeStrings.escapeDAPIdentifier(n);
    }

    /**
     *  Clone interface. Note that in order to do this properly,
     *  we need to be prepared to clone a DAG rather than just
     *  a tree. This means we need two additional procedures: cloneDAG(CloneMap, DAPNode)
     *  and cloneDAG(CloneMap).   These functions carry along a map of already cloned
     *  nodes in order to avoid re-cloning.
     */

    // Define a class for holding all the clone mapping information.
   // Members are kept public for direct access.
   static public class CloneMap
   {
       Map<DAPNode,DAPNode> nodes = new HashMap<DAPNode,DAPNode>(); // map base object to clone
   }

        /**
     * Returns a clone of this <code>DAPNode</code>.  A deep copy is performed
     * on all data inside the variable.
     *
     * @return a clone of this <code>DAPNode</code>.
     */

    public Object clone() {
        try {
  	        CloneMap map = new CloneMap();
            map.nodes.put(NULLNODE,NULLNODE);
	        return cloneDAG(map);
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }


    /**
     * Returns a clone of this <code>DAPNode</code>.
     * on all data inside the variable. Uses a set of already
     * cloned nodes to avoid re-cloning. All sub classes of DAPNode
     * should implement and invoke cloneDAG, not clone.
     *
     * @param map The set of already cloned nodes.
     * @return a clone of this <code>DAPNode</code>.
     */

    /**
     *  This version of cloneDAG() is the primary
     *  point of cloning. If the src is already cloned,
     *  then that existing clone is immediately returned.
     *  Otherwise cloneDAG(map) is called to have the object
     *  clone itself. Note this is static because it uses no
     *  existing state.
     * @param map list of previously cloned nodes
     * @return  the clone of the src node
     * @throws CloneNotSupportedException
     */
    static public DAPNode cloneDAG(CloneMap map, DAPNode src)
        throws CloneNotSupportedException
    {
        DAPNode bt = map.nodes.get(src);
        if(bt == null)
            bt = src.cloneDAG(map);
        return bt;
    }

    /**
     * This procedure does the actual recursive clone.
     * @param map  list of previously cloned nodes
     * @return  clone of this node
     * @throws CloneNotSupportedException
     */
    public DAPNode cloneDAG(CloneMap map)
        throws CloneNotSupportedException
    {
        DAPNode node = (DAPNode)super.clone(); // Object.clone
	map.nodes.put(this,node);

        DAPNode tmp = map.nodes.get(_myParent);
        if(tmp != node)
            _myParent = tmp;
        return node;
    }

}
