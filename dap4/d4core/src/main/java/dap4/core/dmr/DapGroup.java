/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;
import dap4.core.util.DapSort;

import java.util.*;

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
    protected List<DapEnum> enums = new ArrayList<DapEnum>();
    protected List<DapDimension> dimensions = new ArrayList<DapDimension>();
    protected List<DapVariable> variables = new ArrayList<DapVariable>();

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
            for(DapDimension dim : dimensions) {
                if(!dim.isShared() && dim.getSize() == anon.getSize())
                    throw new DapException("DapGroup: attempt to add duplicate anonymous dimension: " + dim.getSize());
            }
        }
        decls.add(newdecl);
        // Cross link
        newdecl.setParent(this);
        switch (newdecl.getSort()) {
        case ATTRIBUTE:
        case ATTRIBUTESET:
        case OTHERXML:
            super.addAttribute((DapAttribute) newdecl);
            break;
        case DIMENSION:
            dimensions.add((DapDimension) newdecl);
            break;
        case ENUMERATION:
            enums.add((DapEnum) newdecl);
            break;
        case ATOMICVARIABLE:
        case STRUCTURE:
        case SEQUENCE:
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

    public List<DapEnum> getEnums()
    {
        return enums;
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
    findByName(String name, DapSort sort)
    {
        return findInGroup(name, sort);
    }

    public List<DapNode>
    findByName(String name, EnumSet<DapSort> sortset)
    {
        return findInGroup(name, sortset);
    }

    public DapNode
    findInGroup(String name, DapSort sort)
    {
        List<DapNode> set = findInGroup(name, EnumSet.of(sort));
        assert set.size() <= 1;
        if(set.size() == 0)
            return null;
        return set.get(0);
    }

    public List<DapNode>
    findInGroup(String name, EnumSet<DapSort> sortset)
    {
        List<DapNode> matches = new ArrayList<DapNode>();
        for(DapSort sort : sortset) {
            switch (sort) {
            case ATTRIBUTE:
            case ATTRIBUTESET:
            case OTHERXML:
                DapAttribute attr = super.getAttributes().get(name);
                matches.add(attr);
                break;
            case DIMENSION:
                for(DapDimension x : dimensions) {
                    if(x.getShortName().equals(name))
                        matches.add(x);
                }
                break;

            case ENUMERATION:
                for(DapEnum x : enums) {
                    if(x.getShortName().equals(name))
                        matches.add(x);
                }
                break;
            case ATOMICVARIABLE:
                for(DapVariable x : variables) {
                    if(x.getSort() != sort)
                        continue;
                    if(x.getShortName().equals(name))
                        matches.add(x);
                }
                break;
            case STRUCTURE:
                for(DapVariable x : variables) {
                    if(x.getSort() != sort)
                        continue;
                    if(x.getShortName().equals(name))
                        matches.add(x);
                }
                break;
            case SEQUENCE:
                for(DapVariable x : variables) {
                    if(x.getSort() != sort)
                        continue;
                    if(x.getShortName().equals(name))
                        matches.add(x);
                }
                break;
            case GROUP:
                for(DapGroup x : groups) {
                    if(x.getShortName().equals(name))
                        matches.add(x);
                }
                break;

            default:
                break;
            }
        }
        return matches;
    }

    /**
     * Parse an FQN and use it to trace to a specific
     * object in a dataset. Absolute FQN paths are passed
     * to DapDataset.findByFQN(). Relative FQNs are assumed
     * to be WRT to the FQN of this node
     *
     * @param fqn  the fully qualified name
     * @param sort the kind of object we are looking for
     * @return the matching Dap Node or null if not found
     */
    public DapNode
    findByFQN(String fqn, DapSort sort)
        throws DapException
    {
        List<DapNode> nodes = findByFQN(fqn, EnumSet.of(sort));
        if(nodes == null || nodes.size() == 0)
            throw new DapException("No such sort:" + sort);
        return nodes.get(0);
    }

    public List<DapNode>
    findByFQN(String fqn, EnumSet<DapSort> sortset)
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
        DapNode var = findInGroup(name, DapSort.ATOMICVARIABLE);
        if(var == null)
            var = findInGroup(name, DapSort.STRUCTURE);
        return (DapVariable) var;
    }


}// Class DapGroup
