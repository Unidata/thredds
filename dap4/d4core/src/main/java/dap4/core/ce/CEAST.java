/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.ce;

import dap4.core.dmr.DapVariable;
import dap4.core.util.Slice;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class CEAST
{
    //////////////////////////////////////////////////
    // Type Decls

    static public class NodeList extends ArrayList<CEAST>
    {
    }

    static public class Path extends NodeList
    {
    }

    static public class StringList extends ArrayList<String>
    {
    }

    static public class SliceList extends ArrayList<Slice>
    {
    }

    static public enum Sort
    {
        CONSTRAINT,
        PROJECTION,
        SEGMENT,
        SELECTION,
        EXPR,
        CONSTANT,
        DEFINE,
    }

    static public enum Constant
    {
        STRING, LONG, DOUBLE, BOOLEAN;

        static public EnumSet<Constant> COMPARABLE = EnumSet.of(LONG, DOUBLE, STRING);
        static public EnumSet<Constant> NUMERIC = EnumSet.of(LONG, DOUBLE);

    }

    static public enum Operator
    {
        LT("<", true, Constant.BOOLEAN, Constant.COMPARABLE),
        LE("<=", true, Constant.BOOLEAN, Constant.COMPARABLE),
        GT(">", true, Constant.BOOLEAN, Constant.COMPARABLE),
        GE(">=", true, Constant.BOOLEAN, Constant.COMPARABLE),
        EQ("==", true, Constant.BOOLEAN, Constant.COMPARABLE),
        NEQ("!=", true, Constant.BOOLEAN, Constant.COMPARABLE),
        REQ("~=", true, Constant.BOOLEAN, EnumSet.of(Constant.STRING)),
        AND(",", true, Constant.BOOLEAN, EnumSet.of(Constant.BOOLEAN)),
        NOT("!", true, Constant.BOOLEAN, EnumSet.of(Constant.BOOLEAN));

        private final String printstring;
        private final boolean binary;
        private final Constant type;
        private final EnumSet<Constant> legalvalues;

        Operator(String printstring, boolean binary, Constant type, EnumSet<Constant> legal)
        {
            this.printstring = printstring;
            this.binary = binary;
            this.type = type;
            this.legalvalues = legal;
        }

        public String printstring()
        {
            return this.printstring;
        }

        public boolean isbinary()
        {
            return this.binary;
        }

        public EnumSet<Constant> legalvalues()
        {
            return this.legalvalues;
        }

        public Constant returntype()
        {
            return this.type;
        }
    }

    //////////////////////////////////////////////////
    // Instance Variables ; do not bother with accessors.

    public Sort sort = null;

    // case CONSTRAINT
    public NodeList clauses = null;
    public Map<String, Slice> dimdefs = null;

    // case PROJECTION
    public CEAST tree = null;

    // case SEGMENT; actually a node in a segment tree, so may have subnodes
    public String name = null;
    public boolean isleaf = true;
    public List<Slice> slices = null;
    public NodeList subnodes = null;

    // case SELECTION
    public CEAST projection = null;
    public CEAST filter = null;
    public DapVariable field = null; // used by compilefilter()

    // case EXPR
    public Operator op = null;
    public CEAST lhs = null;
    public CEAST rhs = null;

    // public case CONSTANT
    public CEAST.Constant kind = null;
    public Object value = null;

    // case DEFINE: also uses name
    public Slice slice = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public CEAST(Sort sort)
    {
        this.sort = sort;
    }

    //////////////////////////////////////////////////
    // Sort specific

    public void
    addSegment(CEAST segment)
    {
        assert sort == Sort.SEGMENT;
        if(subnodes == null)
            subnodes = new NodeList();
        subnodes.add(segment);
    }

    //////////////////////////////////////////////////
    // Misc.

    static public void toString(CEAST node, StringBuilder buf)
    {
        if(node == null) return;
        switch (node.sort) {
        case CONSTRAINT:
            boolean first = true;
            for(CEAST elem : node.clauses) {
                if(!first) buf.append(";");
                toString(elem, buf);
                first = false;
            }
            break;
        case PROJECTION:
            toString(node.tree, buf);
            break;
        case SELECTION:
            toString(node.projection, buf);
            buf.append("|");
            toString(node.filter, buf);
            break;
        case SEGMENT:
            buf.append(node.name);
            if(node.slice != null)
                buf.append(node.slice.toString());
            if(node.subnodes != null) {
                buf.append(".{");
                first = true;
                for(CEAST subnode : node.subnodes) {
                    if(!first) {
                        buf.append(",");
                    } else {
                        first = false;
                    }
                    buf.append(subnode.toString());
                }
                buf.append("}");
            }
            break;
        case EXPR:
            if(node.op.isbinary()) {
                buf.append(node.lhs.toString());
                buf.append(node.op.printstring());
                buf.append(node.rhs.toString());
            }
            break;
        case CONSTANT:
            buf.append(node.value.toString());
            break;
        case DEFINE:
            buf.append(node.name);
            if(node.slice != null) {
                buf.append("=");
                buf.append(node.slice.toString());
            }
            break;
        }
    }

    // Primarily for debug; dump in input form: 
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        toString(this, buf);
        return buf.toString();
    }

}
