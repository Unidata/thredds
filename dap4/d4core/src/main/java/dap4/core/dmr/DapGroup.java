/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;
import dap4.core.util.DapSort;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines a non-groupd group:
 * i.e. one with an atomic type.
 */

public class DapGroup extends DapNode implements DapDecl
{

    //////////////////////////////////////////////////
    // Instance variables

    protected List<DapNode> decls = new ArrayList<DapNode>();
    protected List<DapGroup> groups = new ArrayList<DapGroup>();
    protected List<DapEnumeration> enums = new ArrayList<DapEnumeration>();
    protected List<DapDimension> dimensions = new ArrayList<DapDimension>();
    protected List<DapVariable> variables = new ArrayList<DapVariable>();
    protected List<DapStructure> compounds = new ArrayList<DapStructure>();

    //////////////////////////////////////////////////
    // Constructors

    public DapGroup()
    {
        super();
    }

    public DapGroup(String name)
    {
        super(name);
    }

    //////////////////////////////////////////////////
    // Accessors

    public List<DapNode> getDecls()
    {
        return decls;
    }

    public void
    setDecls(List<? extends DapNode> decls)
            throws DapException
    {
        decls.clear();
        groups.clear();
        enums.clear();
        dimensions.clear();
        variables.clear();
        compounds.clear();
        for(DapNode node : decls) {
            addDecl(node);
        }
    }

    /**
     * Add single declaration
     */

    public void
    addDecl(DapNode newdecl)
            throws DapException
    {
        DapSort newsort = newdecl.getSort();
        String newname = newdecl.getShortName();
        boolean suppress = false;
        // Look for name conflicts (ignore anonymous dimensions)
        if(newsort != DapSort.DIMENSION || newname != null) {
            for(DapNode decl : decls) {
                if(newsort == decl.getSort()
                        && newname.equals(decl.getShortName()))
                    throw new DapException("DapGroup: attempt to add duplicate decl: " + newname);
            }
        } else { // Anonymous
            DapDimension anon = (DapDimension) newdecl;
            assert (newsort == DapSort.DIMENSION && newname == null);
            // Search for matching anonymous dimension
            boolean found = false;
            for(DapDimension dim : dimensions) {
                if(!dim.isShared() && dim.getSize() == anon.getSize()) {
                    found = true;
                    break;
                }
            }
            // Define the anondecl in root group
            if(!found && !isTopLevel()) getDataset().addDecl(anon);
            suppress = found || !isTopLevel();
        }
        if(!suppress) {
            decls.add(newdecl);
            newdecl.setParent(this); // Cross link
        }
        switch (newdecl.getSort()) {
        case ATTRIBUTE:
        case ATTRIBUTESET:
        case OTHERXML:
            super.addAttribute((DapAttribute) newdecl);
            break;
        case DIMENSION:
            if(!suppress)
                dimensions.add((DapDimension) newdecl);
            break;
        case ENUMERATION:
            enums.add((DapEnumeration) newdecl);
            break;
        case ATOMICTYPE:
            break; // do nothing
        case STRUCTURE:
        case SEQUENCE:
            compounds.add((DapStructure)newdecl);
            break;
        case VARIABLE:
            variables.add((DapVariable) newdecl);
            break;
        case GROUP:
        case DATASET:
            if(this != (DapGroup) newdecl)
                groups.add((DapGroup) newdecl);
            break;
        default:
            throw new ClassCastException(newdecl.getShortName());
        }
    }

    /**
     * We will need to re-order the groups
     */

    void
    updateGroups(List<DapGroup> groups)
    {
        // Verify that the incoming groups are all and only in the list of groups.
        assert (groups.size() == this.groups.size()) : "Update groups: not same size";
        for(DapGroup g : groups) {
            if(!this.groups.contains(g))
                assert (false) : "Update groups: attempt to add new group";
        }
    }


    public List<DapGroup> getGroups()
    {
        return groups;
    }

    public List<DapEnumeration> getEnums()
    {
        return enums;
    }

    public List<DapStructure> getCompounds()
        {
            return compounds;
        }

    public List<DapDimension> getDimensions()
    {
        return dimensions;
    }

    public List<DapVariable> getVariables()
    {
        return variables;
    }

    //////////////////////////////////////////////////
    // Lookup Functions

    public DapNode
    findByName(String name, DapSort... sortset)
    {
        return findInGroup(name, sortset);
    }

    public DapNode
    findInGroup(String name, DapSort... sortset)
    {
        for(DapSort sort : sortset) {
            switch (sort) {
            case ATTRIBUTE:
            case ATTRIBUTESET:
            case OTHERXML:
                DapAttribute attr = super.getAttributes().get(name);
                return attr;
            case DIMENSION:
                for(DapDimension x : dimensions) {
                    if(x.getShortName() != null && x.getShortName().equals(name))
                        return x;
                }
                break;
            case ENUMERATION:
                for(DapEnumeration x : enums) {
                    if(x.getShortName().equals(name))
                        return x;
                }
                break;
            case STRUCTURE:
                for(DapStructure d : compounds) {
                    if(d.getSort() == DapSort.STRUCTURE) {
                        if(d.getShortName().equals(name))
                            return d;
                    }
                }
                break;
            case SEQUENCE:
                for(DapStructure d : compounds) {
                    if(d.getSort() == DapSort.SEQUENCE) {
                        if(d.getShortName().equals(name))
                            return d;
                    }
                }
                break;
            case VARIABLE:
                for(DapVariable x : variables) {
                    if(x.getShortName().equals(name))
                        return x;
                }
                break;
            case GROUP:
                for(DapGroup x : groups) {
                    if(x.getShortName().equals(name))
                        return (x);
                }
                break;
            default:
                break;
            }
        }
        return null;
    }

    /**
     * Parse an FQN and use it to trace to a specific
     * object in a dataset. Absolute FQN paths are passed
     * to DapDataset.findByFQN(). Relative FQNs are assumed
     * to be WRT to the FQN of this node
     *
     * @param fqn     the fully qualified name
     * @param sortset the kinds of object we are looking for
     * @return the matching Dap Node or null if not found
     */

    public DapNode
    findByFQN(String fqn, DapSort... sortset)
            throws DapException
    {
        fqn = fqn.trim();
        if(fqn == null)
            return null;
        if(fqn.charAt(0) != '/') {
            String prefix = this.getFQN();
            fqn = prefix + '/' + fqn;
        }
        return getDataset().lookup(fqn, sortset);
    }

    /**
     * Locate a variable in this group
     *
     * @param name the variable's name
     * @return the matching Dap Node or null if not found
     */
    public DapVariable
    findVariable(String name)
    {
        DapNode var = findInGroup(name, DapSort.VARIABLE);
        return (DapVariable) var;
    }


}// Class DapGroup
