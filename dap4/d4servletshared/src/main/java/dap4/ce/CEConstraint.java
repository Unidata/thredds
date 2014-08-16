/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.ce;

import dap4.core.data.*;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.servlet.DapSerializer;

import java.util.*;

/**
 * A Constraint is a structure
 * containing a parsed representation
 * of a constraint expression.
 * Its purpose is define a subset of interest of a dataset.
 * <p/>
 * The constraint object is defined with respect to some underlying DMR.
 * It defines a subset view over that DMR in that it specifies
 * a set of declarations (variables, enums, dimensions, groups)
 * to be included.
 * For each such variable, the constraint specifies
 * any overriding of the dimensions of the variables.
 * Additionally, each variable (if appropriate) may have a filter expr.
 * <p/>
 * Thus, there are three 'sub' constraints within a full constraint.
 * 1. Referencing - is a variable from the underlying dataset
 * included in the constraint, directly (by
 * or indirectly: e.g. fields of a structure when only
 * the structure is referenced
 * 2. Projection - the actual values of a variable to be included
 * in the constraint; this is specified by a triple [start:stride:stop]
 * for each dimension of a variable.
 * 3. Selection (aka filters) - A predicate over the scalar fields
 * of a row of a Sequence; if it evaluates to true, then that
 * row matches the constraint.
 * <p/>
 * There are multiple ways to effect a constraint.
 * 1. Generate and test mode: the constraint is asked if a given
 * element matches the constraint. E.g.
 * a. For referencing, one might ask the constraint
 * if a given variable or field is in the constraint
 * b. For a projection filter, one might ask the constraint
 * if a given set of dimension indices match the projection.
 * c. For a selection filter, one might ask the constraint
 * if a given sequence row matches the filter predicate
 * 2. Iteration mode: the constraint provides an iterator
 * that returns the elements matching the constraint. E.g.
 * a. For referencing, the iterator would return all the
 * variables and fields referenced in the constraint.
 * b. For a projection filter, the iterator would return
 * the successive sets of indices of the projection,
 * or it could return the actual matching value.
 * c. For a selection filter, the iterator would return
 * either the row indices or the actual rows of a sequence
 * that matched the filter predicate
 * <p/>
 * 3. Read mode: Sometimes, it may be more efficient to let the
 * DataVariable object handle the constraint more directly. E.g.
 * a. For example, if the data variable was backed by a netcdf
 * file, then passing in the complete projection might be more
 * efficient than pulling values 1 by 1.
 * b. Similarly, if the sequence object had an associated btree,
 * then it would be more efficient to allow the sequence object
 * to evaluate the filter using the btree. Note that this
 * requires analysis of the filter expression to see if the
 * btree is usable.
 * <p/>
 * Ideally, we would allow all three modes, but for now, only
 * generate-and-test and iteration are implemented, and only a subset
 * of those. Specifically, iteration is provided for referencing,
 * projection, and selection (filters). Generate-and-test is provided for
 * referencing and selection. It is not provided for projection
 * (for now) because it essentially requires the inverse of iteration
 * and that is fairly tricky.
 */

public class CEConstraint implements Constraint
{
    //////////////////////////////////////////////////
    // Constants

    // Mnemonics

    static final String LBRACE = "{";
    static final String RBRACE = "}";


    //////////////////////////////////////////////////
    // Type Decls

    static protected enum Expand
    {
        NONE, EXPANDED, CONTRACTED
    }

    ;

    static protected class Segment
    {
        DapVariable var;
        List<Slice> slices; // projection slices for this variable
        List<DapDimension> dimset; // dimensions for the variable; including
        // redefs and anonymous derived from slices
        CEAST filter;

        Segment(DapVariable var)
        {
            this.var = var;
            this.slices = null; // added later
            this.filter = null; // added later
            this.dimset = null; // added later
        }

        void setDimset(List<DapDimension> dimset)
        {
            this.dimset = dimset;
        }

        void setSlices(List<Slice> slices)
            throws DapException
        {
            this.slices = slices;
            // Make sure they are finished
            for(Slice sl : slices)
                sl.finish();
        }

        void setFilter(CEAST filter)
        {
            this.filter = filter;
        }

        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append(var.getFQN());
            buf.append(slices.toString());
            if(this.filter != null) {
                buf.append("|");
                buf.append(filter.toString());
            }
            return buf.toString();
        }
    }

    static protected class ReferenceIterator implements Iterator<DapNode>
    {

        //////////////////////////////////////////////////
        // Instance Variables

        List<DapNode> list = new ArrayList<>();
        Iterator<DapNode> listiter = null;

        /**
         * @param ce the constraint over which to iterate
         * @throws DapException
         */
        public ReferenceIterator(CEConstraint ce)
            throws DapException
        {
            list.addAll(ce.dimrefs);
            list.addAll(ce.enums);
            list.addAll(ce.variables);
            listiter = list.iterator();
        }

        //////////////////////////////////////////////////
        // Iterator Interface

        public boolean hasNext()
        {
            return listiter.hasNext();
        }

        public DapNode next()
        {
            return listiter.next();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }


    static protected class FilterIterator implements Iterator<DataRecord>
    {
        protected DapSequence seq;
        protected DataSequence data;
        protected long nrecords;
        protected CEAST filter;

        protected int recno;
        protected DataRecord current;
        CEConstraint ce;

        public FilterIterator(CEConstraint ce, DapSequence seq, DataSequence data, CEAST filter)
        {
            this.ce = ce;
            this.filter = filter;
            this.seq = seq;
            this.data = data;
            this.nrecords = data.getRecordCount();
            this.recno = 0; // actually recno of next record to read
            this.current = null;
        }

        // Iterator interface
        public boolean hasNext()
        {
            if(recno < nrecords)
                return false;
            try {
                // look for next matching record starting at recno
                if(filter == null) {
                    this.current = data.readRecord(this.recno);
                    this.recno++;
                    return true;
                } else for(;recno < nrecords;recno++) {
                    this.current = data.readRecord(this.recno);
                    if(ce.matches(this.seq, this.current, filter))
                        return true;
                }
            } catch (DapException de) {
                return false;
            }
            this.current = null;
            return false;
        }

        public DataRecord next()
        {
            if(this.recno >= nrecords || this.current == null)
                throw new NoSuchElementException();
            return this.current;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    //////////////////////////////////////////////////
    // class variables and methods

    static protected Map<DapDataset, CEConstraint> universals = new HashMap<>();


    static public CEConstraint
    getUniversal(DapDataset dmr)
    {
        CEConstraint u = universals.get(dmr);
        if(u == null) {
            try {
                u = new Universal(dmr);
                universals.put(dmr, u);
            } catch (DapException de) {
                throw new IllegalArgumentException("DapDataSet has no universal");
            }
        }
        return u;
    }

    static public void release(DapDataset dmr)
    {
        universals.remove(dmr);
    }

    static protected Object
    fieldValue(DapSequence seq, DataRecord record, String field)
        throws DapException
    {
        DapVariable dapv = seq.findByName(field);
        if(dapv == null)
            throw new DapException("Unknown variable in filter: " + field);
        if(dapv.getSort() != DapSort.ATOMICVARIABLE)
            throw new DapException("Non-atomic variable in filter: " + field);
        if(dapv.getRank() > 0)
            throw new DapException("Non-scalar variable in filter: " + field);
        DataAtomic da = (DataAtomic) (record.readfield(field));
        if(da == null)
            throw new DapException("No such field: "+field);
        return da.read(0);
    }

    static protected int
    compare(Object lvalue, Object rvalue)
        throws DapException
    {
        if(lvalue instanceof String && rvalue instanceof String)
            return ((String) lvalue).compareTo((String) rvalue);
        if(lvalue instanceof Boolean && rvalue instanceof Boolean)
            return compare((Boolean) lvalue ? 1 : 0, (Boolean) rvalue ? 1 : 0);
        if(lvalue instanceof Double || lvalue instanceof Float
            || rvalue instanceof Double || rvalue instanceof Float) {
            double d1 = ((Number) lvalue).doubleValue();
            double d2 = ((Number) lvalue).doubleValue();
            return Double.compare(d1, d2);
        } else {
            long l1 = ((Number) lvalue).longValue();
            long l2 = ((Number) rvalue).longValue();
            return Long.compare(l1, l2);
        }
    }


    /**
     * Evaluate a filter with respect to a Sequence record.
     * Assumes the filter has been canonicalized so that
     * the lhs is a variable.
     *
     * @param seq    the template
     * @param record the record to evaluate
     * @param expr   the filter
     * @throws DapException
     * @returns the value of the expression (usually a Boolean)
     */
    protected Object
    eval(DapSequence seq, DataRecord record, CEAST expr)
        throws DapException
    {
        switch (expr.sort) {

        case CONSTANT:
            return expr.value;

        case SEGMENT:
            return fieldValue(seq, record, expr.name);

        case EXPR:
            Object lhs = eval(seq, record, expr.lhs);
            Object rhs = (expr.rhs == null ? null : eval(seq, record, expr.rhs));
            if(rhs != null)
                switch (expr.op) {
                case LT:
                    return compare(lhs, rhs) < 0;
                case LE:
                    return compare(lhs, rhs) <= 0;
                case GT:
                    return compare(lhs, rhs) > 0;
                case GE:
                    return compare(lhs, rhs) >= 0;
                case EQ:
                    return lhs.equals(rhs);
                case NEQ:
                    return !lhs.equals(rhs);
                case REQ:
                    return lhs.toString().matches(rhs.toString());
                case AND:
                    return ((Boolean) lhs) && ((Boolean) rhs);
                }
            else switch (expr.op) {
            case NOT:
                return !((Boolean) lhs);
            }
        }
        throw new DapException("Malformed Filter");
    }

    //////////////////////////////////////////////////
    // Instance variables

    // Information given to us by the compiler
    protected DapDataset dmr = null; // Underlying DMR

    /**
     * "Map" of variables (at all levels) to be included
     * Maps variables -> associated slices
     * and is modified by computdimensions().
     * Note that we keep the original insertion order
     */

    protected List<Segment> segments = new ArrayList<>();

    /**
     * Also keep a raw list of variables
     */
    protected List<DapVariable> variables = new ArrayList<>();

    // Track redefs
    protected Map<DapDimension, Slice> redefslice = new HashMap<DapDimension, Slice>();

    // Hold any extra attributes
    protected Map<DapNode, List<DapAttribute>> attributes = new HashMap<DapNode, List<DapAttribute>>();
    // Computed information

    // Map original dimension to the redef
    protected Map<DapDimension, DapDimension> redef = new HashMap<DapDimension, DapDimension>();

    // list of all referenced original dimensions
    protected List<DapDimension> dimrefs = new ArrayList<>();

    // List of enumeration decls to be included
    protected List<DapEnum> enums = new ArrayList<>();

    // List of group decls to be included
    protected List<DapGroup> groups = new ArrayList<>();

    // List of referenced shared dimensions (including redefs)
    protected List<DapDimension> refdims = new ArrayList<>();

    protected boolean finished = false;

    protected Expand expansion = Expand.NONE;

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

    public DapDimension getRedefDim(DapDimension orig)
    {
        return redef.get(orig);
    }

    public void addRedef(DapDimension dim, Slice slice)
    {
        this.redefslice.put(dim, slice);
    }

    public void addVariable(DapVariable var, List<Slice> slices)
        throws DapException
    {
        if(findVariableIndex(var) < 0) {
            Segment segment = new Segment(var);
            segment.setSlices(slices);
            this.segments.add(segment);
            this.variables.add(var);
        }
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

    public void setFilter(DapVariable var, CEAST filter)
    {
        Segment seg = findSegment(var);
        if(seg != null)
            seg.filter = filter;
    }

    public List<Slice>
    getConstrainedSlices(DapVariable var)
    {
        Segment seg = findSegment(var);
        if(seg == null)
            return null;
        return seg.slices;
    }

    public List<DapDimension>
    getConstrainedDimensions(DapVariable var)
    {
        List<DapDimension> dimset = null;
        int index = findVariableIndex(var);
        if(index >= 0)
            dimset = segments.get(index).dimset;
        return dimset;
    }

    //////////////////////////////////////////////////
    // Standard

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for(int i = 0;i < segments.size();i++) {
            Segment seg = segments.get(i);
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

    /**
     * Finish creating this Constraint.
     *
     * @throws DapException
     * @returns this - fluent interface
     */
    public CEConstraint
    finish()
        throws DapException
    {
        if(!finished) {
            finished = true;
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
        for(int i = 0;i < segments.size();i++) {
            Segment seg = segments.get(i);
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
        List<DapDimension> dimset = seg.var.getDimensions();
        // Add any slices
        List<Slice> slices = seg.slices;
        if(slices == null)
            dimset = new ArrayList<DapDimension>();
        else
            assert dimset.size() == slices.size();
        for(int i = 0;i < dimset.size();i++) {
            Slice slice = slices.get(i);
            DapDimension dim = dimset.get(i);
            try {
                buf.append(forconstraint ? slice.toConstraintString() : slice.toString());
            } catch (DapException de) {
            }
        }
        // if the var is atomic, then we are done
        if(seg.var.getSort() == DapSort.ATOMICVARIABLE)
            return;
        // If structure and all fields are in the view, then done
        if(seg.var.getSort() == DapSort.STRUCTURE
            || seg.var.getSort() == DapSort.SEQUENCE) {
            if(!isWholeCompound((DapStructure) seg.var)) {
                // Need to insert {...} and recurse
                buf.append(LBRACE);
                DapStructure struct = (DapStructure) seg.var;
                boolean first = true;
                for(DapVariable field : struct.getFields()) {
                    if(!first) buf.append(";");
                    first = false;
                    Segment fseg = findSegment(field);
                    dumpvar(fseg, buf, forconstraint);
                }
                buf.append(RBRACE);
            }
            if(seg.var.getSort() == DapSort.SEQUENCE
                && seg.filter != null) {
                buf.append("|");
                buf.append(seg.filter.toString());
            }
        }

    }

    //////////////////////////////////////////////////
    // Reference processing

    /**
     * Reference X match
     *
     * @param node to test
     * @return true if node is referenced by this constraint
     */

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
            isref = (findVariableIndex((DapVariable) node) >= 0);
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
     * Reference X Iterator
     * Iterate over the variables and return
     * those that are referenced. The order of
     * return is preorder.
     * Inputs:
     * 1.  the variable whose slices are to be iterated.
     *
     * @return ReferenceIterator
     * @throws DapException if could not create.
     */

    public ReferenceIterator
    referenceIterator()
        throws DapException
    {
        return new ReferenceIterator(this);
    }

    //////////////////////////////////////////////////
    // Projection processing

    /**
     * Projection X match
     * This is actually rather difficult because it requires
     * sort of the inverse of an odometer. For this reason,
     * It's implementation is deferred.
     */

    /**
     * Projection X Iterator
     * This basically returns an odometer that
     * will iterate over the appropriate values.
     *
     * @param var over whose dimensions to iterate
     * @throws DapException
     */

    public Odometer
    projectionIterator(DapVariable var)
        throws DapException
    {
        Segment seg = findSegment(var);
        if(seg == null)
            return null;
        return Odometer.factory(seg.slices, seg.dimset, false);
    }

    //////////////////////////////////////////////////
    // Selection (Filter) processing

    /**
     * Selection X match
     * <p/>
     * Evaluate a filter with respect to a Sequence record.
     * Assumes the filter has been canonicalized.
     *
     * @param seq the template
     * @param rec the record to evaluate
     * @throws DapException
     * @returns true if the filter matches the record
     */
    public boolean match(DapSequence seq, DataRecord rec)
        throws DapException
    {
        Segment sseq = findSegment(seq);
        if(sseq == null)
            return false;
        CEAST filter = sseq.filter;
        if(filter == null)
            return true;
        return matches(seq, rec, filter);

    }

    /**
     * Evaluate a filter with respect to a Sequence record.
     *
     * @param seq    the template
     * @param rec    the record to evaluate
     * @param filter the filter
     * @throws DapException
     * @returns true if a match
     */
    protected boolean
    matches(DapSequence seq, DataRecord rec, CEAST filter)
        throws DapException
    {
        Object value = eval(seq, rec, filter);
        return ((Boolean) value);
    }

    /**
     * Selection X Iterator
     * Filter evaluation using an iterator.
     * The iterator evaluates records from a sequence one-by-one
     * and returns the next one that matches the filter.
     * In order to evaluate a record, we need as input:
     * 1.  the DapSequence from which the free
     * variables in the filter are taken.
     * 2.  the DataRecord to evaluate
     *
     * @param dapseq
     * @param dataseq
     */

    public FilterIterator
    filterIterator(DapSequence dapseq, DataSequence dataseq)
        throws DapException
    {
        // Locate the filter for this sequence
        Segment seg = findSegment(dapseq);
        if(seg == null)
            return null;
        return new FilterIterator(this, dapseq, dataseq, seg.filter);
    }


    //////////////////////////////////////////////////
    // Utilities

    /* Search the set of variables */
    protected int findVariableIndex(DapVariable var)
    {
        for(int i = 0;i < variables.size();i++) {
            if(variables.get(i) == var)
                return i;
        }
        return -1;
    }

    protected Segment findSegment(DapVariable var)
    {
        for(int i = 0;i < segments.size();i++) {
            if(segments.get(i).var == var)
                return segments.get(i);
        }
        return null;
    }

    /**
     * Locate each unexpanded Structure|Sequence and:
     * 1. check that none of its fields is referenced => do not expand
     * 2. add all of its fields as leaves
     * Note that #2 may end up adding additional leaf structs &/or seqs
     */
    public void
    expand()
    {
        // Create a queue of unprocessed leaf compounds
        Queue<DapVariable> queue = new ArrayDeque<DapVariable>();

        for(int i = 0;i < variables.size();i++) {
            DapVariable var = variables.get(i);
            if(!var.isTopLevel())
                continue;
            // prime the queue
            if(var.getSort() == DapSort.STRUCTURE || var.getSort() == DapSort.SEQUENCE) {
                DapStructure struct = (DapStructure) var; // remember Sequence subclass Structure
                if(expansionCount(struct) == 0)
                    queue.add(var);
            }
        }
        // Process the queue in prefix order
        while(queue.size() > 0) {
            DapVariable vvstruct = queue.remove();
            DapStructure dstruct = (DapStructure) vvstruct;
            for(DapVariable field : dstruct.getFields()) {
                if(findVariableIndex(field) < 0) {
                    // Add field as leaf
                    this.segments.add(new Segment(field));
                    this.variables.add(field);
                }
                if(field.getSort() == DapSort.STRUCTURE || field.getSort() == DapSort.SEQUENCE) {
                    if(expansionCount((DapStructure) field) == 0)
                        queue.add(field);
                }
            }
        }
        this.expansion = Expand.EXPANDED;
    }

    /**
     * Locate each Structure|Sequence and:
     * 1. check that all of its fields are referenced recursively and not constrained,
     * otherwise ignore
     * 2. contract by removing all of the fields of the Structure or Sequence.
     * This is intended to be (not quite) the dual of expand();
     */
    public void
    contract()
    {
        // Create a set of contracted compounds
        Set<DapStructure> contracted = new HashSet<>();
        for(int i = 0;i < variables.size();i++) {
            DapVariable var = variables.get(i);
            if(var.isTopLevel()) {
                if(var.getSort() == DapSort.STRUCTURE || var.getSort() == DapSort.SEQUENCE) {
                    contractR((DapStructure) var, contracted);
                }
            }
        }
        this.expansion = Expand.CONTRACTED;
    }

    /**
     * Recursive helper
     *
     * @param dstruct    to contract
     * @param contracted set of already contracted compounds
     * @return true if this structure was contracted, false otherwise
     */

    protected boolean
    contractR(DapStructure dstruct, Set<DapStructure> contracted)
    {
        if(contracted.contains(dstruct))
            return true;
        int processed = 0;
        List<DapVariable> fields = dstruct.getFields();
        for(DapVariable field : fields) {
            if(findVariableIndex(field) < 0)
                break; // this compound cannot be contracted
            if((field.getSort() == DapSort.STRUCTURE || field.getSort() == DapSort.SEQUENCE)
                && !contracted.contains((DapStructure) field)) {
                if(!contractR((DapStructure) field, contracted))
                    break; // this compound cannot be contracted
            }
            processed++;
        }
        if(processed < fields.size())
            return false;
        contracted.add(dstruct); // all compound fields were successfully contracted.
        return true;
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
            if(findVariableIndex(field) >= 0) count++;
        }
        return count;
    }

    /**
     * See if a structure is "whole", which
     * means that none of its fields is missing from the
     * constraint, all of fields use default (non-constrained)
     * dimension), and all of its fields are also whole.
     * This must be done recursively.
     *
     * @param dstruct to test
     * @return true if this structure is whole.
     */

    protected boolean
    isWholeCompound(DapStructure dstruct)
    {
        int processed = 0;
        List<DapVariable> fields = dstruct.getFields();
        for(DapVariable field : fields) {
            // not contractable if this field has non-original dimensions
            Segment seg = findSegment(field);
            if(seg == null)
                break; // this compound is not whole
            List<Slice> slices = seg.slices;
            if(slices != null) {
                for(Slice slice : slices) {
                    if(slice.isConstrained())
                        break;
                }
            }
            if(field.getSort() == DapSort.STRUCTURE || field.getSort() == DapSort.SEQUENCE) {
                if(!isWholeCompound((DapStructure) field))
                    break; // this compound is not whole

            }
            processed++;
        }
        return (processed == fields.size());
    }

    //////////////////////////////////////////////////
    // Utilities for computing inferred information

    /**
     * Compute dimension related information
     * using slicing and redef info.
     * In effect, this is where projection constraints
     * are applied
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
        for(int i = 0;i < segments.size();i++) {
            Segment seg = segments.get(i);
            if(seg.var.getRank() == 0)
                continue;
            List<Slice> slices = seg.slices;
            List<DapDimension> orig = seg.var.getDimensions();
            List<DapDimension> newdims = new ArrayList<>();
            // If the slice list is short then pad it with
            // default slices
            if(slices == null)
                slices = new ArrayList<Slice>();
            while(slices.size() < orig.size()) // pad
            {
                slices.add(new Slice().setConstrained(false));
            }
            assert (slices != null && slices.size() == orig.size());
            for(int j = 0;j < slices.size();j++) {
                Slice slice = slices.get(j);
                DapDimension dim0 = orig.get(j);
                DapDimension newdim = redef.get(dim0);
                if(newdim == null)
                    newdim = dim0;
                // fill in the undefined last value
                slice.setMaxSize(newdim.getSize());
                slice.finish();

                Slice newslice = null;
                if(slice.isConstrained()) {
                    // Construct an anonymous dimension for this slice
                    newdim = new DapDimension(slice.getCount());
                } else { // replace with a new slice from the dim
                    newslice = new Slice(newdim);
                    if(newslice != null) {
                        // track set of referenced non-anonymous dimensions
                        if(!dimrefs.contains(dim0)) dimrefs.add(dim0);
                        slices.set(j, newslice);
                    }
                }
                // record the dimension per variable
                newdims.add(newdim);
            }
            seg.setDimset(newdims);
        }
    }

    /**
     * Walk all the included variables and accumulate
     * the referenced enums
     */
    protected void computeenums()
    {
        for(int i = 0;i < variables.size();i++) {
            DapVariable var = variables.get(i);
            if(var.getSort() != DapSort.ATOMICVARIABLE)
                continue;
            DapType daptype = var.getBaseType();
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
        for(int i = 0;i < variables.size();i++) {
            DapVariable var = variables.get(i);
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
