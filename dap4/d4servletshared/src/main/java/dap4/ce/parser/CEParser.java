/* Copyright 2012, UCAR/Unidata.
    See the LICENSE file for more information. */

package dap4.ce.parser;

import dap4.core.dmr.*;
import dap4.core.dmr.parser.ParseException;
import dap4.core.util.*;

import java.util.*;


public class CEParser extends CEParserBody
{

    //////////////////////////////////////////////////
    // Constants

    static final long UNDEFINED = dap4.core.util.Slice.UNDEFINED;

    //////////////////////////////////////////////////
    // Instance variables

    protected DapDataset template = null;

    protected Map<String, Slice> dimdefs = new HashMap<String, Slice>();

    protected CEAST constraint = null;

    //////////////////////////////////////////////////
    // Constructors

    public CEParser(DapDataset template)
        throws ParseException
    {
        super(null);
        CELexer lexer = new CELexer(this);
        setLexer(lexer);
        this.template = template;
    }

    //////////////////////////////////////////////////
    // Get/Set

    public CEAST getConstraint()
    {
        return constraint;
    }

    //////////////////////////////////////////////////
    // Parser API

    public boolean
    parse(String document)
        throws ParseException
    {
        ((CELexer) getLexer()).setText(document);
        return super.parse();
    }

    //////////////////////////////////////////////////
    // Parser primary actions

    @Override
    CEAST
    constraint(CEAST.NodeList clauses)
        throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.CONSTRAINT);
        node.clauses = clauses;
        node.dimdefs = dimdefs; // save for constructing the DMR
        this.constraint = node;
        return node;
    }

    @Override
    CEAST
    projection(CEAST segmenttree)
        throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.PROJECTION);
        node.tree = segmenttree;
        return node;
    }

    @Override
    CEAST
    segmenttree(CEAST parent, CEAST segment)
    {
        if(parent == null)
            parent = segment;
        else {
            parent.addSegment(segment);
            parent.isleaf = false;
        }
        return parent;
    }

    @Override
    CEAST
    segmenttree(CEAST parent, CEAST.NodeList forest)
    {
        assert parent != null;
        parent.isleaf = false;
        for(CEAST node : forest)
            parent.addSegment(node);
        return parent;
    }

    @Override
    CEAST
    segment(String name, CEAST.SliceList slices)
        throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.SEGMENT);
        node.name = name;
        node.slices = (slices == null? new CEAST.SliceList() : slices);
        return node;
    }

    @Override
    Slice
    slice(int state, String sfirst, String send, String sstride)
        throws ParseException
    {
        long first = 0;
        long last = UNDEFINED;
        long stride = 1;
        try {
            if(sfirst != null) first = Long.parseLong(sfirst);
            if(send != null) last = Long.parseLong(send);
            if(sstride != null) stride = Long.parseLong(sstride);
        } catch (NumberFormatException nfe) {
            throw new ParseException(String.format("Illegal slice: [%s:%s:%s]",
                sfirst, send, sstride));
        }

        Slice x;
        try {
            x = new Slice(first, last, stride);
        } catch (DapException de) {
            throw new ParseException(de);
        }

        try {
            // Fill in where possible
            switch (state) {
            case 0: // [] [*]
                x.setConstrained(false);
                break;
            case 1: // [i]
                x.setIndices(x.getFirst(),x.getFirst(),1);
                break;
            case 2: // [f:l]
                x.setIndices(x.getFirst(),x.getLast(),1);
                break;
            case 3: // [f:s:l]
            case 4: // [f:] [f:*]
            case 5: // [f:s:] [f:s:*]
                break;
            default:
                assert false : "Illegal slice case";
            }
        } catch (DapException de) {
            throw new ParseException(de);
        }
        return x;
    }

    @Override
    void
    dimredef(String name, Slice slice)
        throws ParseException
    {
        // First, make sure name is defined only once
        if(dimdefs.containsKey(name))
            throw new ParseException("Multiply defined shared dim: " + name);
        // Now locate the corresponding underlying shared dimension
        DapDimension dim;
        try {
            dim = (DapDimension) this.template.getDataset().findByFQN(name, DapSort.DIMENSION);
        } catch (DapException de) {
            throw new ParseException(de);
        }
        if(dim == null)
            throw new ParseException("Attempt to redefine a non-existent shared dimension: " + name);
        // Verify that the slice is consistent with the shared dimension
        try {
            if(slice.incomplete())
                slice.complete(dim);
            slice.validate();
        } catch (DapException de) {
            throw new ParseException(de);
        }
        if(slice.getLast() >= dim.getSize())
            throw new ParseException("Slice is inconsistent with the underlying shared dimension: " + name);
        dimdefs.put(name, slice);
    }

    //////////////////////////////////////////////////
    // Selection actions

    @Override
    CEAST
    selection(CEAST projection, CEAST filter)
        throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.SELECTION);
        return node;
    }


    @Override
    CEAST
    conjunction(CEAST lhs, CEAST rhs)
        throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.EXPR);
        return node;
    }

    @Override
    CEAST
    negation(CEAST lhs)
        throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.EXPR);
        return node;
    }

    @Override
    CEAST
    predicate(CEAST.Operator op, Object lhs, Object rhs)
        throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.EXPR);
        return node;
    }

    @Override
    CEAST
    predicaterange(CEAST.Operator op1, CEAST.Operator op2, Object lhs, Object mid, Object rhs)
        throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.EXPR);
        return node;
    }


    @Override
    CEAST
    constant(CEAST.Constant sort, String value)
        throws ParseException
    {
        CEAST con = new CEAST(CEAST.Sort.CONSTANT);
        con.kind = sort;
        switch (sort) {
        case STRING:
            con.value = value;
            break;
        case LONG:
            try {
                con.value = Long.parseLong(value);
            } catch (NumberFormatException nfe) {
                throw new ParseException(nfe);
            }
            break;
        case DOUBLE:
            try {
                con.value = Double.parseDouble(value);
            } catch (NumberFormatException nfe) {
                throw new ParseException(nfe);
            }
            break;
        case BOOLEAN:
            if(value.equals("0") || value.equalsIgnoreCase("false"))
                con.value = Boolean.FALSE;
            else if(value.equals("1") || value.equalsIgnoreCase("true"))
                con.value = Boolean.TRUE;
            else
                throw new ParseException("Malformed boolean constant: " + value);
            break;
        default:
            throw new ParseException("Unknown constant kind: " + sort);
        }
        return con;
    }

    //////////////////////////////////////////////////
    // Parser list support

    @Override
    CEAST.NodeList
    nodelist(CEAST.NodeList list, CEAST ast)
    {
        if(list == null) list = new CEAST.NodeList();
        if(ast != null) list.add(ast);
        return list;
    }

    @Override
    CEAST.SliceList
    slicelist(CEAST.SliceList list, Slice slice)
    {
        if(list == null) list = new CEAST.SliceList();
        if(slice != null) list.add(slice);
        return list;
    }

    @Override
    CEAST.StringList
    stringlist(CEAST.StringList list, String string)
    {
        if(list == null) list = new CEAST.StringList();
        if(string != null) list.add(string);
        return list;
    }
}
