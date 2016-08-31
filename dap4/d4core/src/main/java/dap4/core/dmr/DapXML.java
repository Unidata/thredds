/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is not intended to be a full org.w3c.DOM
 * implementation. Rather it stores more-or-less
 * equivalent information in a more AST like form.
 */

public class DapXML
{
    //////////////////////////////////////////////////
    // Types

    static public class XMLList extends ArrayList<DapXML>
    {
    }

    // This corresponds to a subset of the org.w3c.dom.Node
    // nodetype codes.

    static public enum NodeType
    {
        ELEMENT(Node.ELEMENT_NODE),
        ATTRIBUTE(Node.ATTRIBUTE_NODE),
        TEXT(Node.TEXT_NODE),
        CDATA(Node.CDATA_SECTION_NODE),
        COMMENT(Node.COMMENT_NODE),
        DOCUMENT(Node.DOCUMENT_NODE),
        DOCTYPE(Node.DOCUMENT_TYPE_NODE);

        private short w3c_nodetype;

        private NodeType(short nodetype)
        {
            this.w3c_nodetype = nodetype;
        }

        public short getW3CNodeType()
        {
            return w3c_nodetype;
        }
    }

    //////////////////////////////////////////////////
    // Instance Variables

    protected String name = null;
    protected DapXML parent = null;

    // Applies to all (or almost all) node types
    protected NodeType nodetype = null;
    protected String prefix = null; // namespace prefix or null

    // case NodeType.ELEMENT
    protected List<DapXML> elements = new ArrayList<DapXML>();
    protected Map<String, DapXML> xmlattributes = new HashMap<String, DapXML>();

    // case NodeType.ATTRIBUTE
    protected String value = null;  // for attribute nodes

    // case NodeType.TEXT
    // case NodeType.CDATA
    // case NodeType.COMMENT
    protected String text = null;   // for text or cdata nodes

    // case NodeType.DOCUMENT
    // case NodeType.DOCTYPE
    // unused

    //////////////////////////////////////////////////
    // Constructor(s)

    public DapXML()
    {
        super();
    }

    public DapXML(NodeType nodetype, String fullname)
    {
        setNodeType(nodetype);
        // Decompose name into prefix plus short name
        int i = fullname.indexOf(':');
        if(i >= 0) {
            this.prefix = fullname.substring(i);
            fullname = fullname.substring(i + 1, fullname.length());
            if(this.prefix.length() == 0) this.prefix = null;
        }
        setName(fullname);
    }

    //////////////////////////////////////////////////
    // Get/Set

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public NodeType getNodeType()
    {
        return nodetype;
    }

    public void setNodeType(NodeType nodetype)
    {
        this.nodetype = nodetype;
        this.prefix = null;
        String fullname = null;
        switch (nodetype) {
        case COMMENT:
            fullname = "#comment";
            break;
        case TEXT:
            fullname = "#text";
            break;
        case DOCUMENT:
            fullname = "#document";
            break;
        case CDATA:
            fullname = "#cdata-section";
            break;
        default:
            break;
        }
        setName(fullname);
    }

    public String getLocalName()
    {
        return getName();
    }

    public void setLocalName(String localname)
    {
        setName(localname);
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public DapXML getParent()
    {
        return this.parent;
    }

    public void setParent(DapXML parent)
    {
        this.parent = parent;
    }

    public List<DapXML> getElements()
    {
        return this.elements;
    }

    public void addElement(DapXML child)
    {
        if(elements == null) this.elements = new ArrayList<DapXML>();
        elements.add(child);
        child.setParent(this);
    }

    public Map<String, DapXML> getXMLAttributes()
    {
        return xmlattributes;
    }

    public void addXMLAttribute(DapXML attr)
            throws DapException
    {
        if(xmlattributes == null) this.xmlattributes = new HashMap<String, DapXML>();
        if(xmlattributes.containsKey(attr.getName()))
            throw new DapException("DapXML: attempt to add duplicate xml attribute: " + attr.getName());
        xmlattributes.put(attr.getName(), attr);
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        stringify(0, buf);
        return buf.toString();
    }

    protected void
    stringify(int depth, StringBuilder buf)
    {
        switch (this.nodetype) {
        case ELEMENT:
            indent(depth, buf);
            buf.append(this.name);
            for(Map.Entry<String, DapXML> entry : xmlattributes.entrySet()) {
                buf.append(' ');
                entry.getValue().stringify(depth + 1, buf);
            }
            buf.append(">\n");
            for(DapXML sub : elements) {
                sub.stringify(depth + 1, buf);
            }
            buf.append("</");
            buf.append(this.name);
            buf.append(">\n");
            break;
        case ATTRIBUTE:
            buf.append(this.name);
            buf.append('=');
            buf.append('"');
            buf.append(this.value);
            buf.append('"');
            break;
        case TEXT:
            indent(depth,buf);
            buf.append(this.text);
            buf.append("\n");
            break;
        case CDATA:
            indent(depth,buf);
            buf.append("<![CDATA[");
            buf.append(this.text);
            buf.append("]]>\n");
            break;
        case COMMENT:
            indent(depth,buf);
            buf.append("<!--");
            buf.append(this.text);
            buf.append("-->\n");
            break;
        case DOCUMENT:
        case DOCTYPE:
            break;
        }
    }

    protected void
    indent(int n, StringBuilder buf)
    {
        for(int i = 0; i < n; i++) {
            buf.append(' ');
        }
    }

}
