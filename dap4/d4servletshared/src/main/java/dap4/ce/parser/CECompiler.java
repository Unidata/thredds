/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.ce.parser;

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
    protected Stack<DapVariable> scopestack = null;

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
        this.scopestack = new Stack<DapVariable>();
        compileAST(root);
        return this.ce;
    }

    //////////////////////////////////////////////////
    // Accessors

    //////////////////////////////////////////////////

    // Recursive AST walker
    void
    compileAST(CEAST ast)
        throws DapException
    {
        switch (ast.sort) {
        case CONSTRAINT:
            for(CEAST clause : ast.clauses)
                compileAST(clause);
            // invoke semantic checks
            this.ce.finish(CEConstraint.EXPAND);
            break;
        case PROJECTION:
            scopestack.clear();
            compileAST(ast.tree);
            break;
        case SEGMENT:
            compilesegment(ast);
            break;
        case SELECTION:
            break;
        case EXPR:
            break;
        case CONSTANT:
            break;
        case DEFINE:
            dimredef(ast);
            break;
        default:
            assert false : "uknown CEAST node type";
        }
    }

    void
    compilesegment(CEAST ast)
        throws DapException
    {
        DapNode parent = getParent();
        DapNode node = null;
        if(parent == null) {
            // name must be fqn
            List<DapNode> matches = this.dataset.findByFQN(ast.name, EnumSet.of(DapSort.ATOMICVARIABLE, DapSort.SEQUENCE, DapSort.STRUCTURE));
            if(matches.size() > 1)
                throw new DapException("Multiply defined variable name: "+ast.name);
            if(matches.size() == 0)
                node = null; // not of interest
            else
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
            throw new DapException("Constraint projection does not reference a known variable: " + ast.name);
        }
        if(!(node instanceof DapVariable))
            throw new DapException("Attempt to use non-variable in projection: " + node.getFQN());
        DapVariable var = (DapVariable) node;
        ce.addVariable(var,ast.slices);
        scopestack.push(var);
    }

    /*
    // Create the necessary new dimension objects for a variable
    void
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
    void
    dimredef(CEAST node)
        throws DapException
    {
        DapDimension dim = (DapDimension) dataset.findByFQN(node.name, DapSort.DIMENSION);
        if(dim == null)
            throw new DapException("Constraint dim redef: no dimension name: " + node.name);
        Slice slice = node.slice;
        slice.validate();
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

