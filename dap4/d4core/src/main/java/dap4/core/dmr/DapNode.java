/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.*;

import java.util.*;

abstract public class DapNode
{

    //////////////////////////////////////////////////
    // Instance variables

    /**
     * Assign a "sort" to this node to avoid
     * use of instanceof().
     */
    protected DapSort sort = null;

    /**
     * Assign unique id to all nodes.
     * Id is unique relative to the whole tree only.
     * and reflects the order within nodelist
     */
    protected int index;

    /**
     * Unqualified (short) name of this node wrt the tree
     */
    protected String shortname = null;

    /**
     * Escaped version of shortname
     */
    protected String escapedname = null;

    /**
     * Fields to support threading of the tree
     */

    /**
     * Parent DapNode; may be:
     * Group, Structure, Grid, Sequence (for e.g. variables, dimensions),
     * Variable (for e.g. attributes or maps).
     */
    protected DapNode parent = null;

    protected DapDataset dataset = null; // top-level group

    /**
     * Fully qualified name; note that this is always backslash escaped
     */
    protected String fqn = null;

    /**
     * DAP Attributes attached to this node (as opposed to the xml attributes)
     */
    protected Map<String, DapAttribute> attributes = null;

    //////////////////////////////////////////////////
    // Constructors

    public DapNode()
    {
        // Use Instanceof to figure out the sort
        // Because of subclass relationships, order is important
        if(this instanceof DapAtomicVariable)
            this.sort = DapSort.ATOMICVARIABLE;
        else if(this instanceof DapSequence)  //must precede structure because seq subclass struct
            this.sort = DapSort.SEQUENCE;
        else if(this instanceof DapStructure)
            this.sort = DapSort.STRUCTURE;
        else if(this instanceof DapOtherXML)
            this.sort = DapSort.OTHERXML;
        else if(this instanceof DapAttributeSet)
            this.sort = DapSort.ATTRIBUTESET;
        else if(this instanceof DapAttribute)
            this.sort = DapSort.ATTRIBUTE;
        else if(this instanceof DapDataset) // test dataset before group
            this.sort = DapSort.DATASET;
        else if(this instanceof DapGroup)
            this.sort = DapSort.GROUP;
        else if(this instanceof DapDimension)
            this.sort = DapSort.DIMENSION;
        else if(this instanceof DapEnum)
            this.sort = DapSort.ENUMERATION;
        else if(this instanceof DapGrid)
            this.sort = DapSort.GRID;
        else if(this instanceof DapXML)
            this.sort = DapSort.XML;
        else if(this instanceof DapEnum)
            this.sort = DapSort.ENUMERATION;
        else if(this instanceof DapMap)
            this.sort = DapSort.MAP;
        else if(this instanceof DapType) // must follow enum
            this.sort = DapSort.TYPE;
        else
            assert (false) : "Internal error";
    }

    public DapNode(String shortname)
    {
        this();
        setShortName(shortname);
    }

    //////////////////////////////////////////////////
    // Attribute support
    // Note that depending on the semantics,
    // attributes are not allowed on some node types

    public Map<String, DapAttribute> getAttributes()
    {
        if(attributes == null) attributes = new HashMap<String, DapAttribute>();
        return attributes;
    }

    public void setAttributes(Map<String, DapAttribute> alist)
    {
        this.attributes = alist;
    }

    // This may  occur after initial construction
    synchronized public DapAttribute setAttribute(DapAttribute attr)
            throws DapException
    {
        if(attributes == null)
            attributes = new HashMap<String, DapAttribute>();
        DapAttribute old = attributes.get(attr.getShortName());
        attributes.put(attr.getShortName(), attr);
        attr.setParent(this);
        return old;
    }

    public synchronized void addAttribute(DapAttribute attr)
            throws DapException
    {
        String name = attr.getShortName();
        if(this.attributes == null)
            this.attributes = new HashMap<String, DapAttribute>();
        if(this.attributes.containsKey(name))
            throw new DapException("Attempt to add duplicate attribute: " + attr.getShortName());
        setAttribute(attr);
    }

    /**
     * Used by AbstractDSP to suppress certain attributes.
     * @param attr
     * @throws DapException
     */
    public synchronized void removeAttribute(DapAttribute attr)
            throws DapException
    {
        if(this.attributes == null)
            return;
        String name = attr.getShortName();
        if(this.attributes.containsKey(name))
            this.attributes.remove(name);
    }

    public synchronized DapAttribute findAttribute(String name)
    {
        return this.attributes.get(name);
    }


    //////////////////////////////////////////////////
    // Get/set

    public DapSort getSort()
    {
        return sort;
    }

    public void setSort(DapSort sort)
    {
        this.sort = sort;
    }

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public DapDataset getDataset()
    {
        return dataset;
    }

    public void setDataset(DapDataset dataset)
    {
        this.dataset = dataset;
        if(dataset != null && this != dataset)
            dataset.addNode(this);
    }

    /**
     * Closest containing group
     *
     * @returns closest group not equal to this
     * or null if this is a DapDataset
     */

    public DapGroup getGroup()
    {
        if(this.sort == DapSort.DATASET)
            return null;
        // Walk the parent node until we find a group
        DapNode group = parent;
        while(group != null) {
            switch (group.getSort()) {
            case DATASET:
            case GROUP:
                return (DapGroup) group;
            default:
                group = group.getParent();
                break;
            }
        }
        return (DapGroup) group;
    }

    /**
     * Closest containing group, structure
     * sequence or Grid.
     *
     * @returns closest container
     */

    public DapNode getContainer()
    {
        // Walk the parent node until we find a container
        DapNode container = parent;
        while(container != null) {
            switch (container.getSort()) {
            case DATASET:
            case GROUP:
            case STRUCTURE:
            case GRID:
            case SEQUENCE:
                return container;
            default:
                container = container.getParent();
                break;
            }
        }
        return container;
    }

    public DapNode getParent()
    {
        return parent;
    }

    /**
     * Set the parent DapNode; may sometimes be same as container,
     * but not always (think attributes or maps).
     * We can infer the container, so set that also.
     *
     * @param parent the proposed parent node
     */
    public void setParent(DapNode parent)
    {
        this.parent = parent;
    }

    public String getShortName()
    {
        return shortname;
    }

    public void setShortName(String shortname)
    {
        this.shortname = shortname;
        // force recomputation
        this.escapedname = null;
        this.fqn = null;
    }

    // Here, escaped means backslash escaped short name
    // create on demand
    public String getEscapedShortName()
    {
        if(this.escapedname == null)
            this.escapedname = Escape.backslashEscape(getShortName(), null);
        return this.escapedname;
    }

    public String getFQN()
    {
        if(this.fqn == null)
            this.fqn = computefqn();
        assert (fqn.length() > 0 || this.getSort() == DapSort.DATASET);
        return this.fqn;
    }

    /**
     * Compute the path to the root dataset.
     * The root dataset is included as is this node
     *
     * @return ordered list of parent nodes
     */
    public List<DapNode>
    getPath()
    {
        List<DapNode> path = new ArrayList<DapNode>();
        DapNode current = this;
        for(; ; ) {
            path.add(0, current);
            if(current.getParent() == null)
                break;
            current = current.getParent();
        }
        return path;
    }

    /**
     * Get the transitive list of containers
     * Not including this node
     *
     * @return list of container nodes
     */
    public List<DapNode> getContainerPath()
    {
        List<DapNode> path = new ArrayList<DapNode>();
        DapNode current = this.getContainer();
        for(; ; ) {
            path.add(0, current);
            if(current.getContainer() == null)
                break;
            current = current.getContainer();
        }
        return path;
    }

    /**
     * Get the transitive list of containing groups
     * Possibly including this node
     *
     * @return list of group nodes
     */
    public List<DapGroup> getGroupPath()
    {
        List<DapGroup> path = new ArrayList<DapGroup>();
        DapNode current = this;
        for(; ; ) {
            if(current.getSort() == DapSort.GROUP
                    || current.getSort() == DapSort.DATASET)
                path.add(0, (DapGroup) current);
            if(current.getContainer() == null)
                break;
            current = current.getContainer();
        }
        return path;
    }


    //////////////////////////////////////////////////
    // Compute fqn of this node

    /**
     * Compute the FQN of this node
     */
    String
    computefqn()
    {
        List<DapNode> path = getPath();
        StringBuilder fqn = new StringBuilder();
        DapNode parent = path.get(0);
        for(int i = 1; i < path.size(); i++) {// start past the root dataset
            DapNode current = path.get(i);
            // Depending on what parent is, use different delimiters
            switch (parent.getSort()) {
            case DATASET:
            case GROUP:
                fqn.append('/');
                fqn.append(current.getEscapedShortName());
                break;
            // These use '.'
            case STRUCTURE:
            case ENUMERATION:
                fqn.append('.');
                fqn.append(current.getEscapedShortName());
                break;
            // Others should never happen
            default:
                assert (false) : "Illegal FQN parent";
            }
            parent = current;
        }
        return fqn.toString();
    }

    //////////////////////////////////////////////////
    // Misc. Methods

    public boolean isTopLevel()
    {
        return parent == null
                || parent.getSort() == DapSort.DATASET
                || parent.getSort() == DapSort.GROUP;
    }

    //////////////////////////////////////////////////
    // Common Methods

    public String toString()
    {
        String sortname = (sort == null ? "undefined" : sort.name());
        //String name = getFQN();
        String name = null;
        if(name == null) name = getShortName();
        if(name == null) name = "?";
        return sortname + "::" + name;
    }

} // class DapNode
