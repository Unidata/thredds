/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib;

import dap4.core.ce.CEConstraint;
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
 * <p>
 * WARNING: the printDMR code in some cases duplicates code in dap4.dap4.Dap4Print;
 * so changes here should be propagated.
 */

public class DMRPrinter
{

    //////////////////////////////////////////////////
    // Constants

    // Could use enumset, but it is so ugly,
    // so use good old OR'able flags
    static protected final int NILFLAGS = 0;
    static protected final int PERLINE = 1; // print xml attributes 1 per line
    static protected final int NONAME = 2; // do not print name xml attribute
    static protected final int NONNIL = 4; // print empty xml attributes
    static protected final int XMLESCAPED = 8; // String is already xml escaped

    static protected final String[] GROUPSPECIAL = {
            "_NCProperties",
            "_DAP4_Little_Endian"
    };

    static protected final String[] VARSPECIAL = {
    };

    static protected final String[] RESERVEDTAGS = {
            "_edu.ucar"
    };

    static final public boolean ALLOWFIELDMAPS = false;

    //////////////////////////////////////////////////
    // Instance Variables

    protected PrintWriter writer = null;
    protected IndentWriter printer = null;
    protected DapDataset dmr = null;
    protected CEConstraint ce = null;
    protected ResponseFormat format = null;
    protected boolean testing = false;

    //////////////////////////////////////////////////
    // Constructor(s)

    protected DMRPrinter()
    {
    }

    public DMRPrinter(DapDataset dmr, PrintWriter writer)
    {
        this(dmr, null, writer, null);
    }

    public DMRPrinter(DapDataset dmr, CEConstraint ce, PrintWriter writer, ResponseFormat format)
    {
        this();
        this.dmr = dmr;
        this.ce = ce;
        this.writer = writer;
        this.printer = new IndentWriter(writer);
        this.format = (format == null ? ResponseFormat.XML : format);
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
     * @throws IOException
     */

    public void
    print()
            throws IOException
    {
        if(this.ce == null)
            this.ce = CEConstraint.getUniversal(dmr);
        assert (this.ce != null);
        this.printer.setIndent(0);
        printNode(dmr); // start printing at the root
        printer.eol();
    }


    /**
     * Same as print() except certain items of
     * information are suppressed.
     *
     * @throws IOException
     */

    public void
    testprint()
            throws IOException
    {
        this.testing = true;
        print();
    }

    //////////////////////////////////////////////////
    // Helper functions

    /**
     * Print an arbitrary DapNode and its subnodes
     * as if it is being sent to a client with
     * optional constraint; inclusions are determined
     * by the view.
     * <p>
     * Handling newlines is a bit tricky because they may be
     * embedded for e.g. groups, enums,
     * etc.  So the rule is that the
     * last newline is elided and left
     * for the caller to print.
     * Exceptions: printMetadata
     * printDimrefs, printMaps
     *
     * @param node - the node to print
     * @throws IOException Note that the PrintWriter is global.
     */

    public void
    printNode(DapNode node)
            throws IOException
    {
        if(node == null)
            return;

        DapSort sort = node.getSort();
        String dmrname = sort.getName();

        switch (sort) {
        case DATASET:// treat like group
        case GROUP:
            if(!this.ce.references(node)) break;
            DapGroup group = (DapGroup) node;
            printer.marginPrint("<" + dmrname);
            int flags = (sort == DapSort.DATASET ? PERLINE : NILFLAGS);
            printXMLAttributes(node, ce, flags);
            printer.println(">");
            printer.indent();
            // Make the output order conform to the spec
            if(group.getDimensions().size() > 0) {
                for(DapNode subnode : group.getDimensions()) {
                    if(!this.ce.references(subnode)) continue;
                    printNode(subnode);
                    printer.eol();
                }
            }
            if(group.getEnums().size() > 0) {
                for(DapNode subnode : group.getEnums()) {
                    if(!this.ce.references(subnode)) continue;
                    printNode(subnode);
                    printer.eol();
                }
            }
            if(group.getVariables().size() > 0)
                for(DapNode subnode : group.getVariables()) {
                    if(!this.ce.references(subnode)) continue;
                    printNode(subnode);
                    printer.eol();
                }
            printMetadata(node);
            if(group.getGroups().size() > 0)
                for(DapNode subnode : group.getGroups()) {
                    if(!this.ce.references(subnode)) continue;
                    printNode(subnode);
                    printer.eol();
                }
            printer.outdent();
            printer.marginPrint("</" + dmrname + ">");
            break;

        case DIMENSION:
            if(!this.ce.references(node)) break;
            DapDimension dim = (DapDimension) node;
            if(!dim.isShared()) break; // ignore, here, anonymous dimensions
            printer.marginPrint("<" + dmrname);
            printXMLAttributes(node, ce, NILFLAGS);
            if(dim.isUnlimited())
                printXMLAttribute(AbstractDSP.UCARTAGUNLIMITED, "1", NILFLAGS);
            if(hasMetadata(node)) {
                printer.println(">");
                printMetadata(node);
                printer.marginPrint("</" + dmrname + ">");
            } else {
                printer.print("/>");
            }
            break;

        case ENUMERATION:
            if(!this.ce.references(node)) break;
            DapEnumeration en = (DapEnumeration) node;
            printer.marginPrint("<" + dmrname);
            printXMLAttributes(en, ce, NILFLAGS);
            printer.println(">");
            printer.indent();
            List<String> econstnames = en.getNames();
            for(String econst : econstnames) {
                DapEnumConst value = en.lookup(econst);
                assert (value != null);
                printer.marginPrintln(
                        String.format("<EnumConst name=\"%s\" value=\"%d\"/>",
                                Escape.entityEscape(econst, null), value.getValue()));
            }
            printMetadata(node);
            printer.outdent();
            printer.marginPrint("</" + dmrname + ">");
            break;

        case VARIABLE:
            if(!this.ce.references(node)) break;
            DapVariable var = (DapVariable) node;
            DapType type = var.getBaseType();
            printer.marginPrint("<" + type.getTypeSort().name());
            printXMLAttributes(node, ce, NILFLAGS);
            if(type.isAtomic()) {
                if((hasMetadata(node) || hasDimensions(var) || hasMaps(var))) {
                    printer.println(">");
                    printer.indent();
                    if(hasDimensions(var))
                        printDimrefs(var);
                    if(hasMetadata(var))
                        printMetadata(var);
                    if(hasMaps(var))
                        printMaps(var);
                    printer.outdent();
                    printer.marginPrint("</" + type.getTypeSort().name() + ">");
                } else
                    printer.print("/>");
            } else if(type.getTypeSort().isCompound()) {
                DapStructure struct = (DapStructure) type;
                printer.println(">");
                printer.indent();
                for(DapVariable field : struct.getFields()) {
                    if(!this.ce.references(field)) continue;
                    printNode(field);
                    printer.eol();
                }
                printDimrefs(var);
                printMetadata(var);
                printMaps(var);
                printer.outdent();
                printer.marginPrint("</" + type.getTypeSort().name() + ">");
            } else
                assert false : "Illegal variable base type";
            break;

        default:
            assert (false) : "Unexpected sort: " + sort.name();
            break;
        }

    }

    /**
     * Print info from the node that needs to be in the form of xml attributes
     */
    void
    printXMLAttributes(DapNode node, CEConstraint ce, int flags)
            throws IOException
    {
        if((flags & PERLINE) != 0)
            printer.indent(2);

        // Print name first, if non-null and !NONAME
        // Note that the short name needs to use
        // entity escaping (which is done by printXMLattribute),
        // but backslash escaping is not required.
        String name = node.getShortName();
        if(name != null && (flags & NONAME) == 0) {
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
            if(orig.isShared()) {//not Anonymous
                // name will have already been printed
                // Now, we need to get the size as defined by the constraint
                DapDimension actual = this.ce.getRedefDim(orig);
                if(actual == null)
                    actual = orig;
                long size = actual.getSize();
                printXMLAttribute("size", Long.toString(size), flags);
            }
            break;

        case ENUMERATION:
            printXMLAttribute("basetype", ((DapEnumeration) node).getBaseType().getTypeName(), flags);
            break;

        case VARIABLE:
            DapVariable var = (DapVariable) node;
            DapType basetype = var.getBaseType();
            if(basetype.isEnumType()) {
                printXMLAttribute("enum", basetype.getTypeName(), flags);
            }
            break;

        case ATTRIBUTE:
            DapAttribute attr = (DapAttribute) node;
            basetype = attr.getBaseType();
            printXMLAttribute("type", basetype.getTypeName(), flags);
            if(attr.getBaseType().isEnumType()) {
                printXMLAttribute("enum", basetype.getTypeName(), flags);
            }
            break;

        default:
            break; // node either has no attributes or name only
        } //switch
        if(!this.testing)
            printReserved(node);
        if((flags & PERLINE) != 0) {
            printer.outdent(2);
        }
    }

    /**
     * PrintXMLAttributes helper function
     *
     * @param name
     * @param value
     * @param flags
     * @throws DapException
     */
    protected void
    printXMLAttribute(String name, String value, int flags)
            throws DapException
    {
        if(name == null) return;
        if((flags & NONNIL) == 0
                && (value == null || value.length() == 0))
            return;
        if((flags & PERLINE) != 0) {
            printer.eol();
            printer.margin();
        }
        printer.print(" " + name + "=");
        printer.print("\"");

        if(value != null) {
            // add xml entity escaping
            if((flags & XMLESCAPED) == 0)
                value = Escape.entityEscape(value, "\"");
            printer.print(value);
        }
        printer.print("\"");
    }

    protected void
    printReserved(DapNode node)
            throws DapException
    {
        Map<String, String> xattrs = node.getXMLAttributes();
        if(xattrs == null) return; // ignore
        for(Map.Entry<String, String> entry : xattrs.entrySet()) {
            if(isReserved(entry.getKey()))
                printXMLAttribute(entry.getKey(), entry.getValue(), NILFLAGS);
        }
    }

    protected void
    printMetadata(DapNode node)
            throws IOException
    {

        Map<String, DapAttribute> attributes = node.getAttributes();
        if(attributes.size() == 0) {
            return;
        }
        for(Map.Entry<String, DapAttribute> entry : attributes.entrySet()) {
            DapAttribute attr = entry.getValue();
            assert (attr != null);
            switch (attr.getSort()) {
            case ATTRIBUTE:
                printAttribute(attr);
                break;
            case ATTRIBUTESET:
                printContainerAttribute(attr);
                break;
            case OTHERXML:
                printOtherXML(attr);
                break;
            }
        }
    }

    protected void
    printContainerAttribute(DapAttribute attr)
    {
    }

    protected void
    printOtherXML(DapAttribute attr)
    {
    }

    static boolean isSuppressed(String name)
    {
        return isReserved(name);
    }

    static boolean isReserved(String key)
    {
        for(String s : RESERVEDTAGS) {
            if(key.startsWith(s)) return true;
        }
        return false;
    }

    /**
     * Special here is not the same as reserved
     *
     * @param attr
     * @return
     */
    static boolean isSpecial(DapAttribute attr)
    {
        if(attr.getParent().getSort() == DapSort.DATASET) {
            for(String s : GROUPSPECIAL) {
                if(s.equals(attr.getShortName()))
                    return true;
            }
        } else if(attr.getParent().getSort() == DapSort.VARIABLE) {
            for(String s : VARSPECIAL) {
                if(s.equals(attr.getShortName()))
                    return true;
            }
        }
        return false;
    }

    void
    printAttribute(DapAttribute attr)
            throws IOException
    {
        if(this.testing && isSpecial(attr))
            return;
        printer.marginPrint("<Attribute");
        printXMLAttribute("name", attr.getShortName(), NILFLAGS);
        DapType type = attr.getBaseType();
        printXMLAttribute("type", type.getTypeName(), NILFLAGS);
        printer.println(">");
        Object values = attr.getValues();
        if(values == null)
            throw new DapException("Attribute with no values:" + attr.getFQN());
        printer.indent();
        // The values for a DapAttribute are always strings,
        // but for enums might be either the econst name or the associated integer.
        String[] svec = (String[]) values;
        // Special case for char
        if(type == DapType.CHAR) {
            // Print the value as a string of all the characters
            StringBuilder buf = new StringBuilder();
            for(int i = 0; i < svec.length; i++) {
                buf.append(svec[i]);
            }
            String cs = String.format("<Value value=\"%s\"/>", buf.toString());
            printer.marginPrintln(cs);
        } else if(type.isEnumType()) {
            String[] names = (String[])((DapEnumeration)type).convert(svec);
            for(int i = 0; i < svec.length; i++) {
                String s = Escape.entityEscape(names[i], null);
                String cs = String.format("<Value value=\"%s\"/>", s);
                printer.marginPrintln(cs);
            }
        } else {
            for(int i = 0; i < svec.length; i++) {
                String s = Escape.entityEscape(svec[i], null);
                String cs = String.format("<Value value=\"%s\"/>", s);
                printer.marginPrintln(cs);
            }
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
     * @throws DapException
     */
    void
    printDimrefs(DapVariable var)
            throws DapException
    {
        if(var.getRank() == 0) return;
        List<DapDimension> dimset = this.ce.getConstrainedDimensions(var);
        if(dimset == null)
            throw new DapException("Unknown variable: " + var);
        assert var.getRank() == dimset.size();
        for(int i = 0; i < var.getRank(); i++) {
            DapDimension dim = dimset.get(i);
            printer.marginPrint("<Dim");
            if(dim.isShared()) {
                String fqn = dim.getFQN();
                assert (fqn != null) : "Illegal Dimension reference";
                fqn = fqnXMLEscape(fqn);
                printXMLAttribute("name", fqn, XMLESCAPED);
            } else {
                long size = dim.getSize();// the size for printing purposes
                printXMLAttribute("size", Long.toString(size), NILFLAGS);
            }
            printer.println("/>");
        }
    }

    void
    printMaps(DapVariable parent)
            throws IOException
    {
        if(this.testing)
            return;
        List<DapMap> maps = parent.getMaps();
        if(maps.size() == 0) return;
        for(DapMap map : maps) {
            // Optionally suppress field maps
            if(!ALLOWFIELDMAPS) {
                DapVariable mapvar = map.getVariable();
                if(mapvar.getParent() != null
                    && !mapvar.getParent().getSort().isGroup())
                    continue; // Supress maps with non-top-level vars
            }
            // Separate out name attribute so we can use FQN.
            String name = map.getFQN();
            assert (name != null) : "Illegal <Map> reference";
            printer.marginPrint("<Map");
            name = fqnXMLEscape(name);
            printXMLAttribute("name", name, XMLESCAPED);
            printXMLAttributes(map, ce, NONAME);
            if(hasMetadata(map)) {
                printer.println(">");
                printer.indent();
                printMetadata(map);
                printer.outdent();
                printer.marginPrintln("</Map>");
            } else
                printer.println("/>");
        }
    }

    //////////////////////////////////////////////////
    // Misc. Static Utilities

    /**
     * XML escape a dap fqn
     * and converting '"' to &quot;
     * Assumes backslash escapes are in effect for '/' and '.'
     *
     * @param fqn the backslash escaped fqn
     * @return escaped string
     */
    static public String
    fqnXMLEscape(String fqn)
    {
        // Split the fqn into pieces
        StringBuilder xml = new StringBuilder();
        String segment = null;
        List<String> segments = Escape.backslashsplit(fqn, '/');
        for(int i = 1; i < segments.size() - 1; i++) {  // skip leading /
            segment = segments.get(i);
            segment = Escape.backslashUnescape(segment); // get to raw name
            segment = Escape.entityEscape(segment, "\"");// '"' -> &quot;
            segment = Escape.backslashEscape(segment, "/."); // re-escape
            xml.append("/");
            xml.append(segment);
        }
        // Last segment might be structure path, so similar processing,
        // but worry about '.'
        segment = segments.get(segments.size() - 1);
        segments = Escape.backslashsplit(segment, '.');
        xml.append("/");
        for(int i = 0; i < segments.size(); i++) {
            segment = segments.get(i);
            segment = Escape.backslashUnescape(segment); // get to raw name
            segment = Escape.entityEscape(segment, "\"");// '"' -> &quot;
            segment = Escape.backslashEscape(segment, "/."); // re-escape
            if(i > 0) xml.append(".");
            xml.append(segment);
        }
        return xml.toString();
    }


    static protected String
    getPrintValue(Object value)
    {
        if(value instanceof String) {
            String sclean = Escape.cleanString((String) value);
            return Escape.entityEscape((String) value, null);
        } else if(value instanceof Character) {
            return Escape.entityEscape(((Character) value).toString(), null);
        } else
            return value.toString();
    }

    protected boolean hasMetadata(DapNode node)
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
    
