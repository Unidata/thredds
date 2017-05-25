/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.ce.CEConstraint;
import dap4.core.util.*;

import java.util.*;

/**
 * This class defines a non-Gridd Grid:
 * i.e. one with an atomic type.
 */

public class DapDataset extends DapGroup
{

    //////////////////////////////////////////////////
    // Constants

    // Do not, for now, allow maps to specify fields
    static private final boolean ALLOWFIELDMAPS = false;

    //////////////////////////////////////////////////
    // Instance variables

    protected CEConstraint ce = null;

    protected List<DapNode> nodelist = new ArrayList<DapNode>(); // ordered by prefix order
    protected Map<String, DapNode> fqnmap = new HashMap<String, DapNode>();
    protected List<DapDimension> anonymousdims = new ArrayList<DapDimension>(); // Track separately

    protected List<DapNode> visiblenodes = null; // nodelist filtered by constraint

    // Dataset xml attribute values
    protected String dapversion = null;
    protected String dmrversion = null;
    protected String base = null;
    protected String ns = null;

    // Collect some (optionally constrained) subsets of the nodeset
    protected List<DapVariable> topvariables = null;
    protected List<DapVariable> allvariables = null;
    protected List<DapGroup> allgroups = null;
    protected List<DapEnumeration> allenums = null;
    protected List<DapStructure> allcompounds = null;
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
        if(this.ce == null)
            this.visiblenodes = nodelist;
        else {
            this.visiblenodes = new ArrayList<DapNode>(nodelist.size());
            for(int i = 0; i < nodelist.size(); i++) {
                DapNode node = nodelist.get(i);
                if(ce.references(node))
                    visiblenodes.add(node);
            }
        }
        this.topvariables = new ArrayList<DapVariable>();
        this.allvariables = new ArrayList<DapVariable>();
        this.allgroups = new ArrayList<DapGroup>();
        this.allenums = new ArrayList<DapEnumeration>();
        this.allcompounds = new ArrayList<DapStructure>();
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
        if(ce != null && !ce.references(node)) return;
        switch (node.getSort()) {
        case DIMENSION:
            this.alldimensions.add((DapDimension) node);
            break;
        case ENUMERATION:
            this.allenums.add((DapEnumeration) node);
            break;
        case SEQUENCE:
        case STRUCTURE:
            this.allcompounds.add((DapStructure)node);
            break;
        case VARIABLE:
            if(node.isTopLevel())
                this.topvariables.add((DapVariable) node);
            this.allvariables.add((DapVariable) node);
            break;
        case GROUP:
        case DATASET:
            DapGroup g = (DapGroup) node;
            this.allgroups.add(g);
            for(DapNode subnode : g.getDecls()) {
                finishR(subnode);
            }
            break;
        default: /*ignore*/
            break;
        }
    }

    //////////////////////////////////////////////////
    // Accessors

    public CEConstraint getConstraint()
    {
        return this.ce;
    }

    public DapDataset
    setConstraint(CEConstraint ce)
    {
        this.ce = ce;
        return this;
    }

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
        return this.visiblenodes;
    }

    public List<DapVariable> getTopVariables() { return this.topvariables;}

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
        return new DapIterator(visiblenodes, sortset);
    }

    //////////////////////////////////////////////////
    // Lookup Functions

    /**
     * Parse an FQN and use it to trace to a specific
     * object in a dataset. Note that this is quite
     * tricky in the face of backslash escapes.
     * Because of backslash escapes, we cannot
     * use the String.split function because it does appear to be
     * possible to write a regexp that handles preceding backslashes
     * correctly. Instead, we must parse character by character left to right.
     * Traversal into structs:
     * In theory, a map variable could point to a field of a structure.
     * However, in practice, this will not work because we would need a
     * specific instance of that field, which means including dimension
     * indices must be included starting from some top-level variable.
     * We choose, for now, to make that illegal until such time as there
     * is a well-defined meaning for this.
     * Note: this assumes the fqn is absolute (i.e. starts with '/').
     *
     * @param fqn     the fully qualified name
     * @param sortset the kind(s) of object we are looking for
     * @return the matching Dap Node or null if not found
     */
    public DapNode
    lookup(String fqn, DapSort... sortset)
            throws DapException
    {
        fqn = fqn.trim();
        if(fqn == null)
            return null;
        if("".equals(fqn) || "/".equals(fqn)) {
            return this;
        }
        if(fqn.charAt(0) == '/')
            fqn = fqn.substring(1); // remove leading /
        //Check first for an atomic type
        TypeSort ts = TypeSort.getTypeSort(fqn);
        if(ts != null && ts.isAtomic()) {
            // see if we are looking for an atomic type
            for(DapSort ds: sortset) {
                if(ds == DapSort.ATOMICTYPE)
                    return DapType.lookup(ts);
            }
        }

        // Do not use split to be able to look for escaped '/'
        // Warning: elements of path are unescaped
        List<String> path = DapUtil.backslashSplit(fqn, '/');
        DapGroup current = dataset;
        // Walk all but the last element to walk group path
        for(int i = 0; i < path.size() - 1; i++) {
            String groupname = Escape.backslashUnescape(path.get(i));
            DapGroup g = (DapGroup)current.findInGroup(groupname, DapSort.GROUP);
            if(g == null)
                return null;
            assert (g.getSort() == DapSort.GROUP);
            current = (DapGroup) g;
        }
        if(!ALLOWFIELDMAPS) {
            String targetname = Escape.backslashUnescape(path.get(path.size()-1));
            return current.findInGroup(targetname, sortset);
        } else { // ALLOWFIELDMAPS)
            // We need to handle the last segment of the group path
            // to deal with struct walking using '.'. We need to obtain the last segment
            // with escapes intact so we can spot '.' separators.
            // Locate the last element in the last group
            // Start by looking for any containing structure
            String varpart = path.get(path.size() - 1); // Note that this still has escapes
            // So that '.' parsing will be correct.
            List<String> structpath = DapUtil.backslashSplit(varpart, '.');
            String outer = Escape.backslashUnescape(structpath.get(0));
            if(structpath.size() == 1) {
                return current.findInGroup(outer, sortset);
            } else {// It is apparently a structure field
                // locate the outermost structure to start with
                DapStructure currentstruct = (DapStructure) current.findInGroup(outer, DapSort.STRUCTURE, DapSort.SEQUENCE);
                if(currentstruct == null)
                    return null; // does not exist
                // search for the innermost structure
                String fieldname;
                for(int i = 1; i < structpath.size() - 1; i++) {
                    fieldname = Escape.backslashUnescape(structpath.get(i));
                    DapVariable field = (DapVariable) currentstruct.findByName(fieldname);
                    if(field == null)
                        throw new DapException("No such field: " + fieldname);
                    if(!field.isCompound())
                        break;
                    currentstruct = (DapStructure) field.getBaseType();
                }
                fieldname = Escape.backslashUnescape(structpath.get(structpath.size() - 1));
                DapVariable field = currentstruct.findByName(fieldname);
                if(field == null)
                    throw new DapException("No such field: " + fieldname);
                if(field.getSort().oneof(sortset))
                    return (field);
            }
        }
        return null;
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
        for(int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setIndex(i);
        }
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
            for(Map.Entry<String, DapAttribute> entry : attrs.entrySet()) {
                sortR(entry.getValue(), sortlist);
            }
            List<DapDimension> dims = group.getDimensions();
            if(dims != null)
                for(int i = 0; i < dims.size(); i++) {
                    sortR(dims.get(i), sortlist);
                }
            List<DapEnumeration> enums = group.getEnums();
            if(enums != null)
                for(int i = 0; i < enums.size(); i++) {
                    sortR(enums.get(i), sortlist);
                }
            List<DapVariable> vars = group.getVariables();
            if(vars != null)
                for(int i = 0; i < vars.size(); i++) {
                    sortR(vars.get(i), sortlist);
                }
            List<DapGroup> groups = group.getGroups();
            if(groups != null)
                for(int i = 0; i < groups.size(); i++) {
                    sortR(groups.get(i), sortlist);
                }
            break;
        case VARIABLE:
            var = (DapVariable) node;
            attrs = var.getAttributes();
            if(attrs != null)
                for(Map.Entry<String, DapAttribute> entry : attrs.entrySet()) {
                    sortR(entry.getValue(), sortlist);
                }
            List<DapMap> maps = var.getMaps();
            if(maps != null)
                for(int i = 0; i < maps.size(); i++) {
                    sortR(maps.get(i), sortlist);
                }
            dims = var.getDimensions();
            if(dims != null)
                for(int i = 0; i < dims.size(); i++) {
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

