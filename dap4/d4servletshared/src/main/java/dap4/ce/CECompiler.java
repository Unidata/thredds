/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.ce;

import dap4.ce.CEConstraint;
import dap4.core.dmr.*;
import dap4.core.dmr.parser.ParseException;
import dap4.core.util.*;

import java.util.*;

/**
 * Given an AST, compile it into a CEConstraint instance
 * Eventually this will go away and the constraint parser
 * will directly create the constraint.
 */

public class CECompiler
{
    protected Deque<DapVariable> scopestack = null;

    protected DapDataset dataset = null;

    protected CEConstraint ce = null; // will wrap view

    protected CEAST root = null;

    public CECompiler()
    {
    }

    public CEConstraint compile(DapDataset template, CEAST root)
            throws DapException
    {
        this.dataset = template;
        this.root = root;
        this.ce = new CEConstraint(this.dataset);
        this.scopestack = new ArrayDeque<DapVariable>();
        compileAST(root);
        return this.ce;
    }

    //////////////////////////////////////////////////
    // Accessors

    //////////////////////////////////////////////////

    // Recursive AST walker; compilation of filters is done elsewhere.
    protected void
    compileAST(CEAST ast)
            throws DapException
    {
        switch (ast.sort) {
        case CONSTRAINT:
            for(CEAST clause : ast.clauses) {
                compileAST(clause);
            }
            // invoke semantic checks
            this.ce.expand();
            this.ce.finish();
            break;
        case PROJECTION:
            scopestack.clear();
            compileAST(ast.tree);
            break;
        case SEGMENT:
            compilesegment(ast);
            break;
        case SELECTION:
            scopestack.clear();
            compileselection(ast);
            break;
        case DEFINE:
            dimredef(ast);
            break;
        default:
            assert false : "uknown CEAST node type";
        }
    }

    protected void
    compileselection(CEAST ast)
            throws DapException
    {
        DapVariable var = compilesegment(ast.projection);
        if(var.getSort() != DapSort.SEQUENCE)
            throw new DapException("Attempt to apply a filter to a non-sequence variable: " + var.getFQN());
        // Convert field references in the filter
        // and canonicalize the comparisons
        compilefilter((DapSequence) var, ast.filter);
        // add filter
        ce.setFilter(var, ast.filter);
    }

    protected DapVariable
    compilesegment(CEAST ast)
            throws DapException
    {
        DapNode parent = getParent();
        DapNode node = null;
        if(parent == null) {
            // name must be fqn
            List<DapNode> matches = this.dataset.findByFQN(ast.name, EnumSet.of(DapSort.ATOMICVARIABLE, DapSort.SEQUENCE, DapSort.STRUCTURE));
            if(matches == null || matches.size() == 0)
                throw new DapException("Multiply defined variable name: " + ast.name);
            if(matches.size() > 1)
                throw new DapException("Multiply defined variable name: " + ast.name);
            node = matches.get(0);
        } else if(parent.getSort() == DapSort.STRUCTURE) {
            DapStructure struct = (DapStructure) parent;
            node = struct.findByName(ast.name);
        } else if(parent.getSort() == DapSort.SEQUENCE) {
            DapSequence seq = (DapSequence) parent;
            node = seq.findByName(ast.name);
        } else {
            throw new DapException("Attempt to treat non-structure object as structure: " + parent.getFQN());
        }
        if(node == null) {
            throw new DapException("Constraint expression does not reference a known field: " + ast.name);
        }
        if(!(node instanceof DapVariable))
            throw new DapException("Attempt to use non-variable in projection: " + node.getFQN());
        DapVariable var = (DapVariable) node;
        ce.addVariable(var, ast.slices);
        scopestack.push(var);
        return var;
    }

    /**
     * Convert field references in a filter
     */
    public void
    compilefilter(DapSequence seq, CEAST expr)
            throws DapException
    {
        if(expr == null)
            return;
        if(expr.sort == CEAST.Sort.SEGMENT) {
            // This must be a simple segment and it must appear in seq
            if(expr.subnodes != null)
                throw new DapException("compilefilter: Non-simple segment:" + expr.name);
            // Look for the name in the top-level field of seq
            DapVariable field = seq.findByName(expr.name);
            if(field == null)
                throw new DapException("compilefilter: Unknown filter variable:" + expr.name);
            expr.field = field;
        } else if(expr.sort == CEAST.Sort.EXPR) {
            if(expr.lhs != null)
                compilefilter(seq, expr.lhs);
            if(expr.rhs != null)
                compilefilter(seq, expr.rhs);
            // If both lhs and rhs are non-null,
            // canonicalize any comparison so that it is var op const
            if(expr.lhs != null && expr.rhs != null) {
                boolean leftvar = (expr.lhs.sort == CEAST.Sort.SEGMENT);
                boolean rightvar = (expr.rhs.sort == CEAST.Sort.SEGMENT);

                if(rightvar && !leftvar) { // swap operands
                    CEAST tmp = expr.lhs;
                    expr.lhs = expr.rhs;
                    expr.rhs = tmp;
                    // fix operator
                    switch (expr.op) {
                    case LT:  //x<y -> y>x
                        expr.op = CEAST.Operator.GT;
                        break;
                    case LE: //x<=y -> y>=x
                        expr.op = CEAST.Operator.GE;
                        break;
                    case GT:  //x>y -> y<x
                        expr.op = CEAST.Operator.LT;
                        break;
                    case GE:   //x>=y -> y<=x
                        expr.op = CEAST.Operator.LE;
                        break;
                    default:
                        break; // leave as is
                    }
                }
            }
        } else if(expr.sort == CEAST.Sort.CONSTANT) {
            return;
        } else
            throw new DapException("compilefilter: Unexpected node type:" + expr.sort);
    }

    /*
    // Create the necessary new dimension objects for a variable
    protected void
    createdimensions(DapVariable var, List<Slice> slices)
        throws DapException
    {
        List<DapDimension> dimset = var.getDimensions();
        int rank = dimset.size();
        assert rank == slices.size();
        Map<DapDimension, CEConstraint.Redef> redefs = ce.getNewDimensions();
        for(int i = 0;i < rank;i++) {
            DapDimension dim = dimset.get(i);
            Slice slice = slices.get(i);
            // See if this slice is "default" => use original or any redef
            if(slice.isDefault()) {
                // 1. See is this has been redefined
                CEConstraint.Redef redef = redefs.get(dim);
                if(newdim == null) {
                    // use the original dimension
                    ce.addDimension(dim);
                    dimset.add(dim);
                } else { // use the redef dimension
                    dimset.add(redef.newdim);
                }
            } else {// Specific slice was specified
                // Create an anonymous Dimension

            }
            slice.validate();
        }
    }  */

    // Process a dim redefinition
    protected void
    dimredef(CEAST node)
            throws DapException
    {
        DapDimension dim = (DapDimension) dataset.findByFQN(node.name, DapSort.DIMENSION);
        if(dim == null)
            throw new DapException("Constraint dim redef: no dimension name: " + node.name);
        Slice slice = node.slice;
        slice.finish();
        ce.addRedef(dim, slice);
    }

    //////////////////////////////////////////////////
    // Utilities

    DapVariable
    getParent()
    {
        if(scopestack.size() > 0)
            return scopestack.peek();
        return null;
    }

}

