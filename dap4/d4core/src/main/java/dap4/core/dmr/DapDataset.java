/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.*;

import java.util.*;

/**
 * This class defines a non-Gridd Grid:
 * i.e. one with an atomic type.
 */

public class DapDataset extends DapGroup
{

    //////////////////////////////////////////////////
    // Instance variables

    protected List<DapNode> nodelist = new ArrayList<DapNode>(); // ordered by prefix order
    protected Map<String, DapNode> fqnmap = new HashMap<String, DapNode>();
    protected List<DapDimension> anonymousdims = new ArrayList<DapDimension>(); // Track separately

    // Dataset xml attribute values
    protected String dapversion = null;
    protected String dmrversion = null;
    protected String base = null;
    protected String ns = null;

    // Collect some subsets of the nodeset
    protected List<DapVariable> topvariables = null;
    protected List<DapVariable> allvariables = null;
    protected List<DapGroup> allgroups = null;
    protected List<DapEnum> allenums = null;
    protected List<DapDimension> alldimensions = null;

    protected boolean finished = false;

    //////////////////////////////////////////////////
    // Constructors

    public DapDataset()
    {
        super();
        addNode(this);
    }

    public DapDataset(String name)
    {
        super(name);
        addNode(this);
    }

    //////////////////////////////////////////////////
    // Compute inferred information

    public void finish()
    {
        if(this.finished)
            return;
        this.topvariables = new ArrayList<DapVariable>();
        this.allvariables = new ArrayList<DapVariable>();
        this.allgroups = new ArrayList<DapGroup>();
        this.allenums = new ArrayList<DapEnum>();
        this.alldimensions = new ArrayList<DapDimension>();
        finishR(this);
    }

    /**
     * Recursive helper
     *
     * @param node to walk
     */
    protected void
    finishR(DapNode node)
    {
        switch (node.getSort()) {
        case DIMENSION:
            this.alldimensions.add((DapDimension) node);
            break;
        case ENUMERATION:
            this.allenums.add((DapEnum) node);
            break;
        case ATOMICVARIABLE:
        case GRID:
        case SEQUENCE:
        case STRUCTURE:
            if(node.isTopLevel())
                this.topvariables.add((DapVariable) node);
            this.allvariables.add((DapVariable) node);
            break;
        case GROUP:
        case DATASET:
            DapGroup g = (DapGroup) node;
            this.allgroups.add(g);
            for(DapNode subnode : g.getDecls())
                finishR(subnode);
            break;
        default: /*ignore*/
            break;
        }
    }


    //////////////////////////////////////////////////
    // Accessors

    public String getDapVersion()
    {
        return dapversion;
    }

    public void setDapVersion(String value)
    {
        dapversion = value;
    }

    public String getDMRVersion()
    {
        return dmrversion;
    }

    public void setDMRVersion(String value)
    {
        dmrversion = value;
    }

    public String getNS()
    {
        return ns;
    }

    public void setNS(String value)
    {
        ns = value;
    }

    public String getBase()
    {
        return base;
    }

    public void setBase(String value)
    {
        base = value;
    }

    public Map<String, DapNode> getFQNMap()
    {
        return fqnmap;
    }

    public void setFQNMap(Map<String, DapNode> fqnmap)
    {
        this.fqnmap = fqnmap;
    }

    public List<DapNode> getNodeList()
    {
        return nodelist;
    }

    public void addNode(DapNode newnode)
    {
        if(nodelist == null)
            nodelist = new ArrayList<DapNode>();
        newnode.setIndex(nodelist.size());
        nodelist.add(newnode);
    }

    public DapDimension createAnonymous(long size)
        throws DapException
    {
        for(DapDimension dim : anonymousdims) {
            if(dim.getSize() == size) return dim;
        }
        DapDimension anon = new DapDimension();
        anon.setSize(size);
        anonymousdims.add(anon);
        this.addDecl(anon);
        return anon;
    }

    public DapIterator getIterator(EnumSet<DapSort> sortset)
    {
        return new DapIterator(nodelist, sortset);
    }


    // Node subset accessors

    public List<DapVariable>
    getTopVariables()
    {
        return this.topvariables;
    }

    public List<DapVariable>
    getAllVariables()
    {
        return this.allvariables;
    }

    public List<DapGroup>
    getAllGroups()
    {
        return this.groups;
    }

    public List<DapEnum>
    getAllEnums()
    {
        return this.enums;
    }

    public List<DapDimension>
    getAllDimensions()
    {
        return this.dimensions;
    }

    //////////////////////////////////////////////////
    // Lookup Functions

    /**
     * Parse an FQN and use it to trace to a specific
     * object in a dataset. Note that this is quite
     * tricky in the face of backslash escapes and the traversal
     * into structs. Because of backslash escapes, we cannot
     * use the String.split function because it does appear to be
     * possible to write a regexp that handles preceding backslashes
     * correctly. Instead, we must parse character by character left to right.
     * In addition, we need to handle the last segment of the group path
     * to deal with struct walking using '.'. We need to obtain the last segment
     * with escapes intact so we can spot '.' separators.
     * Note: this assumes the fqn is absolute (i.e. starts with '/').
     *
     * @param fqn     the fully qualified name
     * @param sortset the kind(s) of object we are looking for
     * @return the matching Dap Node or null if not found
     */
    public List<DapNode>
    lookup(String fqn, EnumSet<DapSort> sortset)
        throws DapException
    {
        List<DapNode> matches = new ArrayList<DapNode>();
        fqn = fqn.trim();
        if(fqn == null)
            return null;
        if("".equals(fqn) || "/".equals(fqn)) {
            matches.add(this);
            return matches;
        }
        if(fqn.charAt(0) != '/')
            return null;
        fqn = fqn.substring(1); // remove leading /
        // Do not use split to be able to look for escaped '/'
        // Warning: elements of path are unescaped
        List<String> path = DapUtil.backslashSplit(fqn, '/');
        DapGroup current = dataset;
        // Walk all but the last element to walk group path
        for(int i = 0;i < path.size() - 1;i++) {
            String groupname = Escape.backslashUnescape(path.get(i));
            DapNode g = current.findInGroup(groupname, DapSort.GROUP);
            if(g == null)
                return null;
            assert (g.getSort() == DapSort.GROUP);
            current = (DapGroup) g;
        }
        // Locate the last element in the last group
        // Start by looking for any containing structure
        String varpart = path.get(path.size() - 1); // Note that this still has escapes
        // So that '.' parsing will be correct.
        List<String> structpath = DapUtil.backslashSplit(varpart, '.');
        String outer = Escape.backslashUnescape(structpath.get(0));
        if(structpath.size() == 1) {
            matches.addAll(current.findInGroup(outer, sortset));
        } else {// It is apparently a structure field
            // locate the outermost structure to start with
            DapStructure currentstruct = (DapStructure) current.findInGroup(outer, DapSort.STRUCTURE);
            if(currentstruct == null)
                return null; // does not exist
            // search for the innermost structure
            String fieldname;
            for(int i = 1;i < structpath.size() - 1;i++) {
                fieldname = Escape.backslashUnescape(structpath.get(i));
                DapNode field = currentstruct.findByName(fieldname);
                if(field == null)
                    throw new DapException("No such field: " + fieldname);
                if(field.getSort() != DapSort.STRUCTURE)
                    break;
                currentstruct = (DapStructure) field;
            }
            fieldname = Escape.backslashUnescape(structpath.get(structpath.size() - 1));
            DapNode field = currentstruct.findByName(fieldname);
            if(field == null)
                throw new DapException("No such field: " + fieldname);
            if(sortset.contains(field.getSort()))
                matches.add(field);
        }
        return matches;
    }

    //////////////////////////////////////////////////
    // Dap4Action functions

    /**
     * Sort the nodelist into prefix left to right order
     */
    public void
    sort()
    {
        List<DapNode> sorted = new ArrayList<DapNode>();
        sortR(this, sorted);
        // Assign indices
        for(int i = 0;i < sorted.size();i++)
            sorted.get(i).setIndex(i);
        this.nodelist = sorted;
    }

    /**
     * Sort helper
     *
     * @param node the current node we are traversing
     */
    public void
    sortR(DapNode node, List<DapNode> sortlist)
    {
        DapVariable var = null;
        Map<String, DapAttribute> attrs = null;
        sortlist.add(node);
        switch (node.getSort()) {
        case DATASET:
        case GROUP:
            // Walk the decls in this group in order
            // attributes, dimensions, enums, variables, groups
            DapGroup group = (DapGroup) node;
            attrs = group.getAttributes();
            for(Map.Entry<String,DapAttribute> entry : attrs.entrySet()) {
                sortR(entry.getValue(), sortlist);
            }
            List<DapDimension> dims = group.getDimensions();
            if(dims != null)
                for(int i = 0;i < dims.size();i++) {
                    sortR(dims.get(i), sortlist);
                }
            List<DapEnum> enums = group.getEnums();
            if(enums != null)
                for(int i = 0;i < enums.size();i++) {
                    sortR(enums.get(i), sortlist);
                }
            List<DapVariable> vars = group.getVariables();
            if(vars != null)
                for(int i = 0;i < vars.size();i++) {
                    sortR(vars.get(i), sortlist);
                }
            List<DapGroup> groups = group.getGroups();
            if(groups != null)
                for(int i = 0;i < groups.size();i++) {
                    sortR(groups.get(i), sortlist);
                }
            break;
        case GRID:
        case SEQUENCE:
        case STRUCTURE:
            DapStructure struct = (DapStructure) node;
            List<DapVariable> fields = struct.getFields();
            if(fields != null)
                for(int i = 0;i < fields.size();i++) {
                    sortR(fields.get(i), sortlist);
                }
            // fall thru
        case ATOMICVARIABLE:
            var = (DapVariable) node;
            attrs = var.getAttributes();
            if(attrs != null)
                for(Map.Entry<String,DapAttribute> entry : attrs.entrySet()) {
                    sortR(entry.getValue(), sortlist);
                }
            List<DapMap> maps = var.getMaps();
            if(maps != null)
                for(int i = 0;i < maps.size();i++) {
                    sortR(maps.get(i), sortlist);
                }
            dims = var.getDimensions();
            if(dims != null)
                for(int i = 0;i < dims.size();i++) {
                    sortR(dims.get(i), sortlist);
                }
            break;

        case ATTRIBUTE:
            attrs = node.getAttributes();
            if(attrs != null)
                for(String name : attrs.keySet()) {
                    sortR(attrs.get(name), sortlist);
                }
            break;

        default:
            break;
        }
    }

} // class DapDataset

