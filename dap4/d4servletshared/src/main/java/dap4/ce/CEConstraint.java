/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.ce;

import dap4.ce.parser.CEAST;
import dap4.core.dmr.*;
import dap4.core.util.*;

import java.util.*;

/**
 * A Constraint is a structure
 * containing a parsed representation
 * of a constraint expression.
 * <p/>
 * The constraint, with respect to some underlying DMR,
 * defines a subset view over that DMR in that it specifies
 * a set of declarations (variables, enums, dimensions, groups)
 * to be included.
 * For each such variable, the constraint specifies
 * any overriding of the dimensions of the variables.
 * The DMR printer and the serializer access the constraint
 * to determine what to put out.
 * <p/>
 * At some point this class will be substantially
 * more complex as it will contain predicates
 * and function invocations.
 */

public class CEConstraint
{
    //////////////////////////////////////////////////
    // Constants

    // Mnemonics
    static final public boolean EXPAND = true;

    static final String LBRACE = "{";
    static final String RBRACE = "}";

    //////////////////////////////////////////////////
    // Type Decls

    static protected class Universal extends CEConstraint
    {
        public Universal(DapDataset dmr)
        {
            super(dmr);
        }

        @Override
        public void addRedef(DapDimension dim, Slice slice)
        {
        }

        @Override
        public void addVariable(DapVariable var, List<Slice> slices)
        {
        }

        @Override
        public void addAttribute(DapNode node, DapAttribute attr)
        {
        }


        @Override
        public DapDimension getRedefDim(DapDimension orig)
        {
            return null;
        }

        @Override
        public List<Slice> getVariableSlices(DapVariable var)
        {
            try {
                return DapUtil.dimsetSlices(var.getDimensions());
            } catch (DapException de) {
                return null;
            }
        }


        @Override
        public boolean
        references(DapNode node)
        {
            switch (node.getSort()) {
            case DIMENSION:
            case ENUMERATION:
            case ATOMICVARIABLE:
            case GRID:
            case SEQUENCE:
            case STRUCTURE:
            case GROUP:
            case DATASET:
                return true;
            default:
                break;
            }
            return false;
        }
    }

    static protected class Segment
    {
        DapVariable var;
        List<Slice> slices;

        Segment(DapVariable var, List<Slice> slices)
        {
            this.var = var;
            this.slices = slices;
        }

        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append(var.getFQN());
            if(slices != null)
                for(int i=0;i<slices.size();i++)
                    buf.append(slices.get(i).toString());
            return buf.toString();
        }
    }

    //////////////////////////////////////////////////
    // class variables and methods

    static public CEConstraint
    getUniversal(DapDataset dmr)
    {
        return new Universal(dmr);
    }

    //////////////////////////////////////////////////
    // Instance variables

    // Information given to us by the compiler
    protected DapDataset dmr = null; // Underlying DMR

    // "Map" of variables (at all levels) to be included -> associated slices
    // modified by computdimensions().
    // Note that we keep the original insertion order

    protected List<Segment> variables = new ArrayList<Segment>();

    // Track redefs
    protected Map<DapDimension, Slice> redefslice = new HashMap<DapDimension, Slice>();


    // Hold any extra attributes
    protected Map<DapNode, List<DapAttribute>> attributes = new HashMap<DapNode, List<DapAttribute>>();
    // Computed information

    // Map original dimension to the redef 
    protected Map<DapDimension, DapDimension> redef = new HashMap<DapDimension, DapDimension>();

    // list of all referenced original dimensions
    protected List<DapDimension> dimrefs = new ArrayList<DapDimension>();

    // List of enumeration decls to be included
    protected List<DapEnum> enums = new ArrayList<DapEnum>();

    // List of group decls to be included
    protected List<DapGroup> groups = new ArrayList<DapGroup>();

    // List of referenced shared dimensions (including redefs)
    protected List<DapDimension> refdims = new ArrayList<DapDimension>();

    protected boolean finished = false;

    //////////////////////////////////////////////////
    // Constructor(s)

    public CEConstraint()
    {
    }

    public CEConstraint(DapDataset dmr)
    {
        this.dmr = dmr;
    }

    //////////////////////////////////////////////////
    // Accessors

    public DapDataset
    getDMR()
    {
        return this.dmr;
    }

    public void addRedef(DapDimension dim, Slice slice)
    {
        this.redefslice.put(dim, slice);
    }

    public void addVariable(DapVariable var, List<Slice> slices)
    {
        if(findVariable(var) < 0)
            this.variables.add(new Segment(var, slices));
    }

    public void addAttribute(DapNode node, DapAttribute attr)
    {
        List<DapAttribute> attrs = this.attributes.get(node);
        if(attrs == null) {
            attrs = new ArrayList<DapAttribute>();
            this.attributes.put(node, attrs);
        }
        attrs.add(attr);
    }

    public DapDimension getRedefDim(DapDimension orig)
    {
        return redef.get(orig);
    }

    public List<Slice> getVariableSlices(DapVariable var)
    {
        int index = findVariable(var);
        if(index < 0)
            return null;
        return this.variables.get(index).slices;
    }

    //////////////////////////////////////////////////
    // Standard

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for(int i = 0;i < variables.size();i++) {
            Segment seg = variables.get(i);
            if(!seg.var.isTopLevel())
                continue;
            if(!first) buf.append(";");
            first = false;
            dumpvar(seg, buf, false);
        }
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // API

    public boolean
    references(DapNode node)
    {
        boolean isref = false;
        switch (node.getSort()) {
        case DIMENSION:
            DapDimension dim = this.redef.get((DapDimension) node);
            if(dim == null) dim = (DapDimension) node;
            isref = this.dimrefs.contains(dim);
            break;
        case ENUMERATION:
            isref = (this.enums.contains((DapEnum) node));
            break;
        case ATOMICVARIABLE:
        case GRID:
        case SEQUENCE:
        case STRUCTURE:
            isref = (findVariable((DapVariable) node) >= 0);
            break;
        case GROUP:
        case DATASET:
            isref = (this.groups.contains((DapGroup) node));
            break;
        default:
            break;
        }
        return isref;
    }

    /**
     * Finish creating this Constraint.
     *
     * @param expand expand structures in the underlying view
     * @throws DapException
     * @returns this - fluent interface
     */
    public CEConstraint
    finish(boolean expand)
        throws DapException
    {
        if(!finished) {
            finished = true;
            expandCompoundTypes();
            // order is important
            computeenums();
            computedimensions();
            computegroups();
        }
        return this;
    }

    /**
     * Convert the view to a constraint string suitable
     * for use in a URL, except not URL encoded.
     *
     * @return constraint string
     */
    public String toConstraintString()
    {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for(int i = 0;i < variables.size();i++) {
            Segment seg = variables.get(i);
            if(!seg.var.isTopLevel())
                continue;
            if(!first) buf.append(";");
            first = false;
            dumpvar(seg, buf, true);
        }
        return buf.toString();
    }

    /**
     * Recursive helper for tostring/toConstraintString
     *
     * @param seg
     * @param buf
     * @param forconstraint
     */
    protected void
    dumpvar(Segment seg, StringBuilder buf, boolean forconstraint)
    {
        if(seg.var.isTopLevel())
            buf.append(seg.var.getFQN());
        else
            buf.append(seg.var.getShortName());
        // Add any slices
        List<Slice> slices = seg.slices;
        List<DapDimension> dimset = seg.var.getDimensions();
        if(slices != null)
            assert dimset.size() == slices.size();
        for(int i = 0;i < dimset.size();i++) {
            Slice slice = slices.get(i);
            DapDimension dim = dimset.get(i);
            if(!slice.isConstrained() && slice.isWhole())
                buf.append("[]");
            else
                buf.append(forconstraint ? slice.toConstraintString() : slice.toString());
        }

        // if the var is atomic, then we are done
        if(seg.var.getSort() == DapSort.ATOMICVARIABLE)
            return;
        // If structure and all fields are in the view, then done
        if(seg.var.getSort() == DapSort.STRUCTURE
            && isWholeStructure((DapStructure) seg.var))
            return;
        // Need to insert {...} and recurse
        buf.append(LBRACE);
        DapStructure struct = (DapStructure) seg.var;
        boolean first = true;
        for(DapVariable field : struct.getFields()) {
            if(!first) buf.append(";");
            first = false;
            Segment fseg = getVariable(field);
            dumpvar(fseg, buf, forconstraint);
        }
        buf.append(RBRACE);
    }

    //////////////////////////////////////////////////
    // Utilities

    /* Search the set of variables */
    protected int findVariable(DapVariable var)
    {
        for(int i = 0;i < variables.size();i++) {
            if(variables.get(i).var == var)
                return i;
        }
        return -1;
    }

    protected Segment getVariable(DapVariable var)
    {
        for(int i = 0;i < variables.size();i++) {
            if(variables.get(i).var == var)
                return variables.get(i);
        }
        return null;
    }

    /**
     * See if all the fields in this structure are part of the
     * view; this must be done recursively.
     */
    protected boolean
    isWholeStructure(DapStructure struct)
    {
        for(DapVariable field : struct.getFields()) {
            if(findVariable(field) < 0)
                return false;
            if(field.getSort() == DapSort.STRUCTURE) {
                // recurse
                if(!isWholeStructure((DapStructure) field))
                    return false;
            }
        }
        return true;
    }

    /**
     * Locate each unexpanded Structure|Sequence and:
     * 1. check that none of its fields is referenced => do not expand
     * 2. add all of its fields as leaves
     * Note that #2 may end up adding additional leaf structs &/or seqs
     *
     * @throws DapException
     */
    protected void
    expandCompoundTypes()
    {
        // Create a queue of unprocessed leaf compounds
        Queue<DapVariable> queue = new ArrayDeque<DapVariable>();
        for(int i = 0;i < variables.size();i++) {
            Segment seg = variables.get(i);
            if(!seg.var.isTopLevel())
                continue;
            // prime the queue
            if(seg.var.getSort() == DapSort.STRUCTURE || seg.var.getSort() == DapSort.SEQUENCE) {
                DapStructure struct = (DapStructure) seg.var; // remember Sequence subclass Structure
                if(expansionCount(struct) == 0)
                    queue.add(seg.var);
            }
        }
        // Process the queue in prefix order
        while(queue.size() > 0) {
            DapVariable vvstruct = queue.remove();
            DapStructure dstruct = (DapStructure) vvstruct;
            for(DapVariable field : dstruct.getFields()) {
                if(findVariable(field) < 0) {
                    // Add field as leaf
                    this.variables.add(new Segment(field, null));
                }
                if(field.getSort() == DapSort.STRUCTURE || field.getSort() == DapSort.SEQUENCE) {
                    if(expansionCount((DapStructure) field) == 0)
                        queue.add(field);
                }
            }
        }
    }

    /**
     * Count the number of fields of a structure that
     * already in this view.
     *
     * @param struct the dapstructure to check
     * @return # of fields in this view
     * @throws DapException
     */

    protected int
    expansionCount(DapStructure struct)
    {
        int count = 0;
        for(DapVariable field : struct.getFields()) {
            if(findVariable(field) >= 0) count++;
        }
        return count;
    }

    //////////////////////////////////////////////////
    // Utilities for computing inferred information

    /**
     * Compute dimension related information
     * using slicing and redef info.
     * <p/>
     * Assume that the constraint compiler has given us the following info:
     * <ol>
     * <li> A list of the variables to include.
     * <li> A pair (DapDimension,Slice) for each redef
     * <li> For each variable in #1, a list of slices
     * taken from the constraint expression
     * </ol>
     * <p/>
     * Two products will be produced.
     * <ol>
     * <li> The variables map will be modified so that the
     * slices properly reflect any original or redef dimensions.
     * <li> A set, dimrefs, of all referenced original dimensions.
     * </ol>
     * <p/>
     * The processing is as follows
     * <ol>
     * <li> For each redef create a new redef dimension
     * <li> For each variable:
     * <ol>
     * <li> if the variable is scalar, do nothing.
     * <li> if the variable has no associated slices, then make its
     * new dimensions be the original dimensions.
     * <li> otherwise, walk the slices and create new dimensions
     * from them; use redefs where indicated
     * <li>
     * </ol>
     * </ol>
     */
    protected void
    computedimensions()
        throws DapException
    {
        // Build the redefmap
        for(DapDimension key : redefslice.keySet()) {
            Slice slice = redefslice.get(key);
            DapDimension newdim = (DapDimension) key.clone();
            newdim.setSize(slice.getCount());
            redef.put(key, newdim);
        }

        // Process each variable
        for(int i = 0;i < variables.size();i++) {
            Segment seg = variables.get(i);
            if(seg.var.getRank() == 0)
                continue;
            List<Slice> slices = seg.slices;
            List<DapDimension> orig = seg.var.getDimensions();
            // If the slice list is short then pad it with
            // default slices
            if(slices == null)
                slices = new ArrayList<Slice>();
            while(slices.size() < orig.size()) // pad
                slices.add(new Slice().setConstrained(false));
            assert (slices != null && slices.size() == orig.size());
            for(int j = 0;j < slices.size();j++) {
                Slice slice = slices.get(j);
                DapDimension dim0 = orig.get(j);
                DapDimension newdim = redef.get(dim0);
                if(newdim == null) newdim = dim0;
                if(slice.incomplete())  // fill in the undefined last value
                    slice.complete(newdim);
                Slice newslice = null;
                if(!slice.isConstrained()) {
                    // replace with a new slice from the dim
                    newslice = new Slice().fill(newdim);
                    if(newslice != null) {
                        // track set of referenced non-anonymous dimensions
                        if(!dimrefs.contains(dim0)) dimrefs.add(dim0);
                        slices.set(j, newslice);
                    }
                }
            }
        }
    }

    /**
     * Walk all the included variables and accumulate
     * the referenced enums
     */
    protected void computeenums()
    {
        for(int i = 0;i < variables.size();i++) {
            Segment seg = variables.get(i);
            if(seg.var.getSort() != DapSort.ATOMICVARIABLE)
                continue;
            DapType daptype = seg.var.getBaseType();
            if(!daptype.isEnumType())
                continue;
            if(!this.enums.contains((DapEnum) daptype))
                this.enums.add((DapEnum) daptype);
        }
    }

    /**
     * Walk all the included declarations
     * and accumulate the set of referenced groups
     */
    protected void computegroups()
    {
        // 1. variables
        for(int i=0;i<variables.size();i++) {
            DapVariable var = variables.get(i).var;
            List<DapGroup> path = var.getGroupPath();
            for(DapGroup group : path) {
                if(!this.groups.contains(group))
                    this.groups.add(group);
            }
        }
        // 2. Dimensions
        for(DapDimension dim : this.dimrefs) {
            if(!dim.isShared())
                continue;
            List<DapGroup> path = dim.getGroupPath();
            for(DapGroup group : path) {
                if(!this.groups.contains(group))
                    this.groups.add(group);
            }
        }
        // 2. enumerations
        for(DapEnum en : this.enums) {
            List<DapGroup> path = en.getGroupPath();
            for(DapGroup group : path) {
                if(!this.groups.contains(group))
                    this.groups.add(group);
            }
        }
    }

}
