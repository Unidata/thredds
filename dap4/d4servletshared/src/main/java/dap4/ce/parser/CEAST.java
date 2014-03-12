/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.ce.parser;

import dap4.core.util.Slice;

import java.util.ArrayList;
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

    static public enum Operator
    {
        LT, LE, GT, GE, EQ, NEQ, REQ, AND
    }

    static public enum Constant
    {
        STRING, LONG, DOUBLE, BOOLEAN;
    }

    //////////////////////////////////////////////////
    // Instance Variables

    Sort sort = null;

    // case CONSTRAINT
    NodeList clauses = null;
    Map<String, Slice> dimdefs = null;

    // case PROJECTION
    CEAST tree = null;

    // case SEGMENT; actually a node in a segment tree, so may have subnodes
    String name = null;
    boolean isleaf = true;
    SliceList slices = null;
    NodeList subnodes = null;

    // case SELECTION
    CEAST projection = null;
    CEAST filter = null;

    // case EXPR
    Operator op = null;
    CEAST lhs = null;
    CEAST rhs = null;

    // case CONSTANT
    CEAST.Constant kind = null;
    Object value = null;

    // case DEFINE: also uses name
    Slice slice = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public CEAST(Sort sort)
    {
        this.sort = sort;
    }

    //////////////////////////////////////////////////
    // Sort specific

    void
    addSegment(CEAST segment)
    {
        assert sort == Sort.SEGMENT;
        if(subnodes == null)
            subnodes = new NodeList();
        subnodes.add(segment);
    }

    //////////////////////////////////////////////////
    // Misc.

    static void toString(CEAST node, StringBuilder buf)
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
        case CONSTANT:
        case DEFINE:
            buf.append(node.name);
            if(node.slice != null)
                buf.append(node.slice.toString());
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
