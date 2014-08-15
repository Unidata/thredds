/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.ce.CEConstraint;
import dap4.core.dmr.*;
import dap4.core.util.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * DMR Printer.
 * Given a constraint and a Dataset,
 * print the constrained subset of the dataset.
 * <p/>
 * WARNING: the printDMR code in some cases duplicates code in dap4.dap4.Dap4Print;
 * so changes here should be propagated.
 */

public class DMRPrint
{

    //////////////////////////////////////////////////
    // Constants

    // Could use enumset, but it is so ugly,
    // so use good old OR'able flags
    static protected final int NILFLAGS = 0;
    static protected final int PERLINE = 1; // print xml attributes 1 per line
    static protected final int NONAME = 2; // do not print name xml attribute
    static protected final int NONNIL = 4; // print empty xml attributes

    //////////////////////////////////////////////////
    // Instance Variables

    protected PrintWriter writer = null;
    protected IndentWriter printer = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public DMRPrint()
    {
    }

    public DMRPrint(PrintWriter writer)
    {
        this();
        this.writer = writer;
        this.printer = new IndentWriter(writer);
    }

    //////////////////////////////////////////////////
    // API

    public void flush()
    {
        this.printer.flush();
    }

    public void close()
    {
        this.flush();
    }

    //////////////////////////////////////////////////
    // External API

    /**
     * Print a DapDataset:
     * - as DMR
     * - optionally constrained
     *
     * @param dataset
     * @throws IOException
     */

    public void
    printDMR(DapDataset dataset)
            throws IOException
    {
        CEConstraint ce = CEConstraint.getUniversal(dataset);
        assert(ce != null);
        printDMR(ce,dataset);
    }


    public void
    printDMR(CEConstraint ce, DapDataset dmr)
            throws IOException
    {
        this.printer.setIndent(0);
        print(dmr, ce); // start printing at the root
        printer.eol();
    }

    //////////////////////////////////////////////////
    // Helper functions

    /**
     * Print an arbitrary DapNode and its subnodes
     * as if it is being sent to a client with
     * optional constraint; inclusions are determined
     * by the view.
     * <p/>
     * Handling newlines is a bit tricky because they may be
     * embedded for e.g. groups, enums,
     * etc.  So the rule is that the
     * last newline is elided and left
     * for the caller to print.
     * Exceptions: printMetadata
     * printDimrefs, printMaps
     *
     * @param node - the node to rpint
     * @throws IOException
     */

    void
    print(DapNode node, CEConstraint ce)
            throws IOException
    {
        if (node == null)
            return;

        DapSort sort = node.getSort();
        String dmrname = sort.getName();

        switch (sort) {
        case DATASET:// treat like group
        case GROUP:
            if (!ce.references(node)) break;
            DapGroup group = (DapGroup) node;
            printer.marginPrint("<" + dmrname);
            int flags = (sort == DapSort.DATASET ? PERLINE : NILFLAGS);
            printXMLAttributes(node, ce, flags);
            printer.println(">");
            printer.indent();
            // Make the output order conform to the spec
            if (group.getDimensions().size() > 0) {
                for (DapNode subnode : group.getDimensions()) {
                    if (!ce.references(subnode)) continue;
                    print(subnode, ce);
                    printer.eol();
                }
            }
            if (group.getEnums().size() > 0) {
                for (DapNode subnode : group.getEnums()) {
                    if (!ce.references(subnode)) continue;
                    print(subnode, ce);
                    printer.eol();
                }
            }
            if (group.getVariables().size() > 0)
                for (DapNode subnode : group.getVariables()) {
                    if (!ce.references(subnode)) continue;
                    print(subnode, ce);
                    printer.eol();
                }
            printMetadata(node, ce);
            if (group.getGroups().size() > 0)
                for (DapNode subnode : group.getGroups()) {
                    if (!ce.references(subnode)) continue;
                    print(subnode, ce);
                    printer.eol();
                }
            printer.outdent();
            printer.marginPrint("</" + dmrname + ">");
            break;

        case DIMENSION:
            if (!ce.references(node)) break;
            DapDimension dim = (DapDimension) node;
            if (!dim.isShared()) break; // ignore, here, anonymous dimensions
            printer.marginPrint("<" + dmrname);
            printXMLAttributes(node, ce, NILFLAGS);
            if (hasMetadata(node)) {
                printer.println(">");
                printMetadata(node, ce);
                printer.marginPrint("</" + dmrname + ">");
            } else {
                printer.print("/>");
            }
            break;

        case ENUMERATION:
            if (!ce.references(node)) break;
            DapEnum en = (DapEnum) node;
            printer.marginPrint("<" + dmrname);
            printXMLAttributes(en, ce, NILFLAGS);
            printer.println(">");
            printer.indent();
            List<String> econstnames = en.getNames();
            for (String econst : econstnames) {
                Long value = en.lookup(econst);
                assert (value != null);
                printer.marginPrintln(
                        String.format("<EnumConst name=\"%s\" value=\"%s\"/>",
                                Escape.entityEscape(econst), value.toString()));
            }
            printMetadata(node, ce);
            printer.outdent();
            printer.marginPrint("</" + dmrname + ">");
            break;

        case STRUCTURE:
        case SEQUENCE:
            if (!ce.references(node)) break;
            DapStructure struct = (DapStructure) node;
            printer.marginPrint("<" + dmrname);
            printXMLAttributes(node, ce, NILFLAGS);
            printer.println(">");
            printer.indent();
            for (DapVariable field : struct.getFields()) {
                if (!ce.references(field)) continue;
                print(field, ce);
                printer.eol();
            }
            printDimrefs(struct, ce);
            printMetadata(node, ce);
            printMaps(struct, ce);
            printer.outdent();
            printer.marginPrint("</" + dmrname + ">");
            break;

        case ATOMICVARIABLE:
            if (!ce.references(node)) break;
            DapAtomicVariable var = (DapAtomicVariable) node;
            // Get the type sort of the variable
            DapType basetype = var.getBaseType();
            printer.marginPrint("<" + basetype.getAtomicType().name());
            printXMLAttributes(node, ce, NILFLAGS);
            if (hasMetadata(node) || hasDimensions(var) || hasMaps(var)) {
                printer.println(">");
                printer.indent();
                if (hasDimensions(var))
                    printDimrefs(var, ce);
                if (hasMetadata(var))
                    printMetadata(var, ce);
                if (hasMaps(var))
                    printMaps(var, ce);
                printer.outdent();
                printer.marginPrint("</" + basetype.getAtomicType().name() + ">");
            } else
                printer.print("/>");
            break;

        default:
            assert (false) : "Unexpected sort: " + sort.name();
            break;
        }

    }

    void
    printXMLAttributes(DapNode node, CEConstraint ce, int flags)
            throws IOException
    {
        if ((flags & PERLINE) != 0)
            printer.indent(2);

        // Print name first, if non-null and !NONAME
        // Note that the short name needs to use
        // entity escaping (which is done by printXMLattribute),
        // but backslash escaping is not required.
        String name = node.getShortName();
        if (name != null && (flags & NONAME) == 0) {
            name = node.getShortName();
            printXMLAttribute("name", name, flags);
        }

        switch (node.getSort()) {
        case DATASET:
            DapDataset dataset = (DapDataset) node;
            printXMLAttribute("dapVersion", dataset.getDapVersion(), flags);
            printXMLAttribute("dmrVersion", dataset.getDMRVersion(), flags);
            // boilerplate
            printXMLAttribute("xmlns", "http://xml.opendap.org/ns/DAP/4.0#", flags);
            printXMLAttribute("xmlns:dap", "http://xml.opendap.org/ns/DAP/4.0#", flags);
            break;

        case DIMENSION:
            DapDimension orig = (DapDimension) node;
            if (orig.isShared()) {//not Anonymous
                // name will have already been printed
                // Now, we need to get the size as defined by the constraint
                DapDimension actual = ce.getRedefDim(orig);
                if (actual == null)
                    actual = orig;
                long size = actual.getSize();
                if (size == DapDimension.VARIABLELENGTH)
                    printXMLAttribute("size", "*", flags);
                else
                    printXMLAttribute("size", Long.toString(size), flags);
            }
            break;

        case ENUMERATION:
            printXMLAttribute("basetype", ((DapEnum) node).getBaseType().getTypeName(), flags);
            break;

        case ATOMICVARIABLE:
            DapAtomicVariable atom = (DapAtomicVariable) node;
            DapType basetype = atom.getBaseType();
            if (basetype.isEnumType()) {
                printXMLAttribute("enum", basetype.getTypeName(), flags);
            }
            break;

        case ATTRIBUTE:
            DapAttribute attr = (DapAttribute) node;
            basetype = attr.getBaseType();
            printXMLAttribute("type", basetype.getTypeName(), flags);
            if (attr.getBaseType().isEnumType()) {
                printXMLAttribute("enum", basetype.getTypeName(), flags);
            }
            break;

        default:
            break; // node either has no attributes or name only
        } //switch

        if ((flags & PERLINE) != 0) {
            printer.outdent(2);
        }
    }

    /**
     * PrintXMLAttributes helper function
     */
    void
    printXMLAttribute(String name, String value, int flags)
            throws DapException
    {
        if (name == null) return;
        if ((flags & NONNIL) == 0
                && (value == null || value.length() == 0))
            return;
        if ((flags & PERLINE) != 0) {
            printer.eol();
            printer.margin();
        }
        printer.print(" " + name + "=");
        printer.print("\"");

        if (value != null) {
            // add xml entity escaping
            value = Escape.entityEscape(value);
            printer.print(value);
        }
        printer.print("\"");
    }

    void
    printMetadata(DapNode node, CEConstraint ce)
            throws IOException
    {

        Map<String, DapAttribute> attributes = node.getAttributes();
        if (attributes.size() == 0) {
            return;
        }
        for (Map.Entry<String,DapAttribute> entry : attributes.entrySet()) {
            DapAttribute attr = entry.getValue();
            assert (attr != null);
            switch (attr.getSort()) {
            case ATTRIBUTE:
                printAttribute(attr, ce);
                break;
            case ATTRIBUTESET:
                printContainerAttribute(attr, ce);
                break;
            case OTHERXML:
                printOtherXML(attr, ce);
                break;
            }
        }
    }

    void
    printContainerAttribute(DapAttribute attr, CEConstraint ce)
    {
    }

    void
    printOtherXML(DapAttribute attr, CEConstraint ce)
    {
    }

    void
    printAttribute(DapAttribute attr, CEConstraint ce)
            throws IOException
    {
        printer.marginPrint("<Attribute");
        printXMLAttributes(attr, ce, NILFLAGS);
        List<Object> values = attr.getValues();
        printer.println(">");
        if (values == null)
            throw new DapException("Attribute with no values:" + attr.getFQN());
        printer.indent();
        if (values.size() == 1) {
            printer.marginPrintln(String.format("<Value value=\"%s\"/>", getPrintValue(values.get(0))));
        } else {
            printer.marginPrintln("<Value>");
            printer.indent();
            for (Object value : values) {
                printer.marginPrint(getPrintValue(value));
                printer.eol();
            }
            printer.outdent();
            printer.marginPrintln("</Value>");
        }
        printer.outdent();
        printer.marginPrintln("</Attribute>");
    }

    /**
     * Print the dimrefs for a variable's dimensions.
     * If the variable has a non-whole projection, then use size
     * else use the dimension name.
     *
     * @param var whole dimensions are to be printed
     * @param ce  constraint
     * @throws DapException
     */
    void
    printDimrefs(DapVariable var, CEConstraint ce)
            throws DapException
    {
        if (var.getRank() == 0) return;
        List<DapDimension> dimset = ce.getConstrainedDimensions(var);
        if (dimset == null)
            throw new DapException("Unknown variable: " + var);
        assert var.getRank() == dimset.size();
        for (int i = 0; i < var.getRank(); i++) {
            DapDimension dim = dimset.get(i);
            printer.marginPrint("<Dim");
            if (dim.isShared()) {
                String fqn = dim.getFQN();
                assert (fqn != null) : "Illegal Dimension reference";
                printXMLAttribute("name", fqn, NILFLAGS);
            } else {
                long size = dim.getSize();// the size for printing purposes
                printXMLAttribute("size", Long.toString(size), NILFLAGS);
            }
            printer.println("/>");
        }
    }

    void
    printMaps(DapVariable parent, CEConstraint ce)
            throws IOException
    {
        List<DapMap> maps = parent.getMaps();
        if (maps.size() == 0) return;
        for (DapMap map : maps) {
            printer.marginPrint("<Map");
            // Separate out name attribute so we can use FQN.
            String name = map.getFQN();
            assert (name != null) : "Illegal <Map> reference";
            printXMLAttribute("name", name, NILFLAGS);
            printXMLAttributes(map, ce, NONAME);
            if (hasMetadata(map)) {
                printer.println(">");
                printer.indent();
                printMetadata(map, ce);
                printer.outdent();
                printer.marginPrintln("</Map>");
            } else
                printer.println("/>");
        }
    }

    //////////////////////////////////////////////////
    // Misc. Static Utilities

    static protected String
    getPrintValue(Object value)
    {
        if (value instanceof String) {
            return Escape.entityEscape((String) value);
        } else
            return value.toString();
    }

    static protected boolean hasMetadata(DapNode node)
    {
        return node.getAttributes().size() > 0;
    }

    static protected boolean hasMaps(DapVariable var)
    {
        return var.getMaps().size() > 0;
    }

    static protected boolean hasDimensions(DapVariable var)
    {
        return var.getDimensions().size() > 0;
    }


} // class DapPrint
    
