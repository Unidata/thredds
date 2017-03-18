/* Copyright 2012, UCAR/Unidata.
    See the LICENSE file for more information. */

package dap4.core.ce.parser;

import dap4.core.ce.CEAST;
import dap4.core.dmr.*;
import dap4.core.dmr.parser.ParseException;
import dap4.core.util.*;

import java.util.*;


public class CEParserImpl extends CEBisonParser
{

    //////////////////////////////////////////////////
    // Constants

    static final long UNDEFINED = dap4.core.util.Slice.UNDEFINED;

    //////////////////////////////////////////////////
    // static variables

    static protected int globaldebuglevel = 0;

    //////////////////////////////////////////////////
    // static methods

    static public void setGlobalDebugLevel(int level) {globaldebuglevel = level;}
    //////////////////////////////////////////////////
    // Instance variables

    protected DapDataset template = null;

    protected Map<String, Slice> dimdefs = new HashMap<String, Slice>();

    protected CEAST constraint = null;

    //////////////////////////////////////////////////
    // Constructors

    public CEParserImpl(DapDataset template)
            throws ParseException
    {
        super(null);
        CELexer lexer = new CELexer(this);
        setLexer(lexer);
        this.template = template;
	if(globaldebuglevel > 0)
	    super.setDebugLevel(globaldebuglevel);
    }

    //////////////////////////////////////////////////
    // Get/Set

    public CEAST getCEAST()
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
        for(CEAST node : forest) {
            parent.addSegment(node);
        }
        return parent;
    }

    @Override
    CEAST
    segment(String name, CEAST.SliceList slices)
            throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.SEGMENT);
        node.name = name;
        node.slices = (slices == null ? new CEAST.SliceList() : slices);
        return node;
    }

    @Override
    Slice
    slice(CEAST.SliceList subslices)
            throws ParseException
    {
        Slice slice = null;
        if(subslices != null) {
            switch (subslices.size()) {
            case 0:
                break;
            case 1:
                slice = subslices.get(0); // no need for a multislice
                break;
            default:
                List<Slice> list = new ArrayList(subslices);
                try {
                    slice = new MultiSlice(list);
                } catch (DapException de) {
                    throw new ParseException(de);
                }
                break;
            }
        }
        return slice;
    }

    @Override
    Slice
    subslice(int state, String sfirst, String send, String sstride)
            throws ParseException
    {
        long first = 0;
        long last = UNDEFINED;
        long stop = UNDEFINED;
        long stride = 1;
        try {
            if(sfirst != null) first = Long.parseLong(sfirst);
            if(send != null) last = Long.parseLong(send);
            if(sstride != null) stride = Long.parseLong(sstride);
        } catch (NumberFormatException nfe) {
            throw new ParseException(String.format("Illegal slice: [%s:%s:%s]",
                    sfirst, send, sstride));
        }
        stop = (last == UNDEFINED ? UNDEFINED : (last+1));

        Slice x;
        try {
            x = new Slice(first, stop, stride);
        } catch (DapException de) {
            throw new ParseException(de);
        }

        try {
            // Fill in where possible
            switch (state) {
            case 0: // []
                x.setConstrained(false);
                break;
            case 1: // [i]
                x.setIndices(x.getFirst(), x.getFirst()+1, 1);
                break;
            case 2: // [f:l]
                x.setIndices(x.getFirst(), x.getStop(), 1);
                break;
            case 3: // [f:s:l]
            case 4: // [f:]
            case 5: // [f:s:]
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
            slice.setMaxSize(dim.getSize());
            slice.finish();
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
        node.projection = projection;
        node.filter = filter;
        return node;
    }


    @Override
    CEAST
    logicalAnd(CEAST lhs, CEAST rhs)
            throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.EXPR);
        node.op = CEAST.Operator.AND;
        node.lhs = lhs;
        node.rhs = rhs;
        return node;
    }

    @Override
    CEAST
    logicalNot(CEAST lhs)
            throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.EXPR);
        node.op = CEAST.Operator.NOT;
        node.lhs = lhs;
        return node;
    }

    @Override
    CEAST
    predicate(CEAST.Operator op, CEAST lhs, CEAST rhs)
            throws ParseException
    {
        CEAST node = new CEAST(CEAST.Sort.EXPR);
        node.op = op;
        node.lhs = (CEAST) lhs;
        node.rhs = (CEAST) rhs;
        return node;
    }

    @Override
    CEAST
    predicaterange(CEAST.Operator op1, CEAST.Operator op2, CEAST lhs, CEAST mid, CEAST rhs)
            throws ParseException
    {
        CEAST node2 = new CEAST(CEAST.Sort.EXPR);
        node2.op = op2;
        node2.lhs = (CEAST) mid;
        node2.rhs = (CEAST) rhs;
        CEAST node1 = new CEAST(CEAST.Sort.EXPR);
        node1.op = op1;
        node1.lhs = (CEAST) lhs;
        node1.rhs = (CEAST) mid;
        CEAST andnode = new CEAST(CEAST.Sort.EXPR);
        andnode.op = CEAST.Operator.AND;
        andnode.lhs = node1;
        andnode.rhs = node2;
        return andnode;
    }

    @Override
    CEAST
    fieldname(String value)
            throws ParseException
    {
        CEAST seg = new CEAST(CEAST.Sort.SEGMENT);
        seg.name = value;
        seg.isleaf = true;
        return seg;
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


}
