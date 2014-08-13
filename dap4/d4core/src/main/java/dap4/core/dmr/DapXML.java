/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;
import org.w3c.dom.Node;

import java.util.*;

/**
 * This class is not intended to be a full org.w3c.DOM
 * implementation. Rather it stores more-or-less
 * equivalent information in a more AST like form.
 */

public class DapXML extends DapNode
{
    //////////////////////////////////////////////////
    // Types

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

    ;

    //////////////////////////////////////////////////
    // Instance Variables

    // Applies to all (or almost all) node types
    NodeType nodetype = null;
    String prefix = null; // namespace prefix or null

    // case NodeType.ELEMENT
    List<DapXML> elements = new ArrayList<DapXML>();
    Map<String, DapXML> xmlattributes = new HashMap<String, DapXML>();

    // case NodeType.ATTRIBUTE
    String value = null;  // for attribute nodes

    // case NodeType.TEXT
    // case NodeType.CDATA
    String text = null;   // for text or cdata nodes

    // case NodeType.COMMENT
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
        super(null);
        setNodeType(nodetype);
        // Decompose name into prefix plus short name
        int i = fullname.indexOf(':');
        if(i >= 0) {
            this.prefix = fullname.substring(i);
            fullname = fullname.substring(i + 1, fullname.length());
            if(this.prefix.length() == 0) this.prefix = null;
        }
        setShortName(fullname);
    }

    //////////////////////////////////////////////////
    // Get/Set

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
        setShortName(fullname);
    }

    public String getLocalName()
    {
        return getShortName();
    }

    public void setLocalName(String localname)
    {
        setShortName(localname);
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
        return (DapXML) super.getParent();
    }

    public List<DapXML> getElements()
    {
        return elements;
    }

    public void addElement(DapXML child)
    {
        if(elements == null) this.elements = new ArrayList<DapXML>();
        elements.add(child);
    }

    public Map<String, DapXML> getXMLAttributes()
    {
        return xmlattributes;
    }

    public void addXMLAttribute(DapXML attr)
        throws DapException
    {
        if(xmlattributes == null) this.xmlattributes = new HashMap<String, DapXML>();
        if(xmlattributes.containsKey(attr.getShortName()))
            throw new DapException("DapXML: attempt to add duplicate xml attribute: " + attr.getShortName());
        xmlattributes.put(attr.getShortName(), attr);
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

}
