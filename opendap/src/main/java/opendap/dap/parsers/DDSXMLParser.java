/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////


package opendap.dap.parsers;


import java.io.*;
import java.util.List;
import java.util.Iterator;

import org.jdom2.*;
import org.jdom2.Attribute;
import org.jdom2.input.SAXBuilder;

import opendap.dap.*;
import opendap.util.Debug;

/**
 * <code>DDSXMLParser</code> is used to parse a DDX (the XML formatted persistent
 * representation of a DDS) into a DDS object in memory. This parser uses the
 * Xerces XML parser and JDOM packages to perform it's duties.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @opendap.ddx.experimental WARNING! This class supports a pre-released version of the DDX.
 * The XML schema that this class is designed to work with WILL BE CHANGED prior
 * to software release.
 */
public class DDSXMLParser {

    // Debugging output control
    private boolean _Debug = false;

    // depth of recursion in the parser.
    // This used only for Debugging purposes.
    private int parseLevel;

    // The DDS that the parser is building.
    private DDS dds = null;

    // The BaseTypeFactory the parser uses
    // to create each variable in the DDS.
    private BaseTypeFactory factory = null;

    // The namespace through which the XML document is validated.
    // This gets set up in the constructor for this class.
    private Namespace opendapNameSpace;


    // The parent DConstructor of the current variable.
    // This is state information used by the parser.
    // Variables in the DDS are always added to a
    // a container of this type (DDS is an instance
    // of a DConstructor)
    private DConstructor parentDC;

    // The BaseType thatis currently being parsed.
    // This is state information used by the parser.
    private BaseType currentBT = null;

    // The Attribute Table (container) to which
    // Attributes currently being parsed should be added.
    // This is state information used by the parser.
    private AttributeTable currentAT = null;

    // The last DOM Document that we used to construct a DDS;
    private Document lastDoc;

    /**
     * Constructs a new <code>DDSXMLParser</code>.
     * The OPeNDAP namespace is defined during the construction
     * of an instance of this class.
     */
    public DDSXMLParser(String nameSpace) {
        super();
        opendapNameSpace = Namespace.getNamespace(nameSpace);
        lastDoc = null;
        _Debug = Debug.isSet("DDSXMLParser");
    }


    /**
     * Returns the <code>Document</code> object created by the most
     * recent invocation of the parser. This might be usefull to some XML
     * hacker somewhere. :)
     *
     * @return The <code>Document</code> object created by the most
     *         recent invocation of the parser.
     */
    public Document getLastDomDoc() {
        return (lastDoc);
    }


    /**
     * Parse the DDX waiting in the <code>InputStream</code> and instantiate all of
     * the member <code>BaseType</code> variables and their associated <code>Attributes
     * </code> into a <code>DDS</code> using the passed <code>BaseTypeFactory</code>
     *
     * @param ddx        A JDOM <code>Document</code> containing the DDX to parse.
     * @param targetDDS  The <code>DDS</code> in which to place all of the <code>BaseType
     *                   </code> variables and their associated <code>Attributes</code>.
     * @param fac        The <code>BaseTypeFactory</code> to use when creating new
     *                   <code>BaseType</code> variables.
     * @param validation Is a boolean indicating whether or not the parser should
     *                   validate the XML document using the Schema (typically referenced in the
     *                   document itself). In general server side applications should always vaidate,
     *                   while clients shouldn't bother (since they are ostensibly receiving the
     *                   document from a server that has already done so.)
     * @see DDS
     * @see BaseTypeFactory
     */
    public void parse(Document ddx, DDS targetDDS, BaseTypeFactory fac, boolean validation) throws DAP2Exception {

        dds = targetDDS;
        factory = fac;

        // Build up the OPeNDAP data objects rpresented by the XML document.
        // Additional validation will take place  during this process.

        Element root = ddx.getRootElement();
        lastDoc = ddx;

        // This is just a little tracker to help with debugging.
        parseLevel = 0;

        // Make sure the root element is in fact a Dataset.
        // Trying to enforce this in the schema would create a
        // contrived and difficult to interpret schema design.
        String type = root.getName();
        if (!(type.equals("Dataset"))) {
            throw new NoSuchTypeException("Root Element MUST be <Dataset>. Found: " + type);
        }

        String name = root.getAttribute("name").getValue();
        //System.out.println("DDS should be named: "+name);
        dds.setClearName(name);
        parentDC = dds;
        currentBT = dds;

        // Parse any Attributes (or AttributeTables/containers) at the
        // top level in the Dataset.
        parseAttributes(root, "-- ");

        // Parse any Aliases at the
        // top level in the Dataset.
        parseAliases(root, "++ ");

        // Parse all of the child elements (which would be OPeNDAP
        // BaseType variables) in the Dataset.
        Iterator ci = root.getChildren().iterator();
        while (ci.hasNext()) {
            Element child = (Element) ci.next();
            parseBase(child, "    ");

        }

//	catch(Exception e) {
//	    throw new DAP2Exception("PARSER ERROR! \n"+
//	                            e.getClass().getName()+ ": "+e.getMessage());
//	}

    }

    /**
     * Parse the DDX waiting in the <code>InputStream</code> and instantiate all of
     * the member <code>BaseType</code> variables and their associated <code>Attributes
     * </code> into a <code>DDS</code> using the passed <code>BaseTypeFactory</code>
     *
     * @param is         The <code>InputStream</code> containing the DDX to parse.
     * @param targetDDS  The <code>DDS</code> in which to place all of the <code>BaseType
     *                   </code> variables and their associated <code>Attributes</code>.
     * @param fac        The <code>BaseTypeFactory</code> to use when creating new
     *                   <code>BaseType</code> variables.
     * @param validation Is a boolean indicating whether or not the parser should
     *                   validate the XML document using the Schema (typically referenced in the
     *                   document itself). In general server side applications should always vaidate,
     *                   while clients shouldn't bother (since they are ostensibly receiving the
     *                   document from a server that has already done so.)
     * @see DDS
     * @see BaseTypeFactory
     */
    public void parse(InputStream is, DDS targetDDS, BaseTypeFactory fac, boolean validation) throws DAP2Exception {


        try {

            // get a jdom parser to parse and validate the XML document.
            SAXBuilder parser = new SAXBuilder("org.apache.xerces.parsers.SAXParser", validation);

            // turn on validation
            parser.setFeature("http://apache.org/xml/features/validation/schema", validation);

            // parse the document into a hierarchical document
            Document doc = parser.build(is);

            if (_Debug) System.out.println("Document is " +
                    (validation ? "valid and " : "") +
                    "well-formed.\nContent: " + doc);

            parse(doc, targetDDS, fac, validation);

        } catch (JDOMException jde) {
            throw new DDSException(opendap.dap.DAP2Exception.UNKNOWN_ERROR, jde.getMessage());
        }
        catch (IOException ioe) {
            throw new DDSException(opendap.dap.DAP2Exception.UNKNOWN_ERROR, ioe.getMessage());
        }


    }

    /**
     * This method recursively travels through the DOM tree, locating
     * BaseType derived nodes and placing them in the DDS. The structure
     * of the BaseType derived elements in the XML instance document
     * is captured in the DOM object that is being parsed. This structure
     * again reflected in the resulting DDS.
     */
    private void parseBase(Element e, String indent)
            throws DASException, NoSuchTypeException, BadSemanticsException {


        parseLevel++;

        String type = e.getName();

        if (type.equals("Attribute")) {
            // Do nothing here, the Attributes get parsed when the BaseType's
            // get built. This conditional basically serves as a "trap" to
            // ignore the <Attribute> tag.
        } else if (type.equals("Alias")) {
            // Do nothing here, the Aliases get parsed when the BaseType's
            // get built. This conditional basically serves as a "trap" to
            // ignore the <Alias> tag.

        } else if (type.equals("dataBLOB")) {

            // dataBLOB?
            // The schema says that the href attribute is
            // required for the dataBLOB element.
            org.jdom2.Attribute hrefAttr = e.getAttribute("href");

            // Since it's required we know that the getAttribute()
            // method is not going to return null.
            String contentID = hrefAttr.getValue();

            if (_Debug) System.out.println("Found dataBLOB element. contentID=\"" + contentID + "\"");

            dds.setBlobContentID(contentID);

        } else {

            // What's left must be a OPeNDAP BaseType

            if (_Debug) System.out.println("Parsing new BaseType element. Parse level: " + parseLevel);
            if (_Debug) showXMLElement(e, indent);

            // Go get a new BaseType formed from this element
            BaseType bt = newBaseType(e);

            // Set it's parent.
            // bt.setParent(parentDC);

            // Add it to it's parent (container)
            parentDC.addVariable(bt);

            // Now we need to make sure this particular BaseType
            // derived element isn't some special type that needs
            // additional parsing:

            // Is it a container?
            if (bt instanceof DConstructor) {
                // Up date the parsers state, (cache my parent)
                DConstructor myParentDC = parentDC;
                parentDC = (DConstructor) bt;
                try {
                // Grids are special containers, handle them
                if (bt instanceof DGrid) {
                    parseGrid(e, indent);
                } else {
                    // Otherwise, recurse on the children
                  for (Element child : e.getChildren()) {
                    parseBase(child, indent + "    ");
                  }
                }
                } finally {
                // restore my parent
                parentDC = myParentDC;
                }
            } else if (bt instanceof DArray) {
                // Array's are special, better build it if it is one

                if (_Debug) System.out.println("Parsing Array instance.  Array name: '" + bt.getClearName() + "'");

                parseArray(e, (DArray) bt, indent);

            }


        }

        parseLevel--;

    }


    /**
     * Arrays have special parsing need as their syntax is different from
     * that of a typical BaseType derived type or a container type. The
     * array is based on a template variable that can have ANY structure
     * that can be represented by the OPeNDAP data model. With the exception
     * of (you knew this was comming, right?) of other arrays. IE You can't
     * have Arrays of Arrays. This caveat is enforced by the XML schema.
     */
    private void parseArray(Element ArrayElement, DArray da, String indent)
            throws DASException, NoSuchTypeException, BadSemanticsException {

        int countTemplateVars = 0;
        int numDims = 0;


      for (Element e : ArrayElement.getChildren()) {

        if (_Debug) System.out.println(indent + "Working on Array element: " + e.getName());

        // Is this element an Attribute of the Array?
        if (e.getName().equals("Attribute")) {
          //Then ignore it!
        }
        // Is this element an Attribute of the Alias?
        else if (e.getName().equals("Alias")) {
          //Then ignore it!
        }
        // Is this element an array dimension?
        else if (e.getName().equals("dimension")) {

          // Then count it,
          numDims++;

          // And now let's add it to the array...

          // Array dimension are not required to have names, so
          // the schema does not enforce the use of the name attribute.

          // try to get the dimension's name, and use it id it's there.
          String name = null;
          Attribute nameAttr = e.getAttribute("name");

          if (nameAttr != null)
            name = nameAttr.getValue();

          // The presence of the 'size' attribute is enforeced by the schema.
          // get it, parse it, use it.
          int size = Integer.parseInt(e.getAttribute("size").getValue());

          // add the dimension to the array.
          da.appendDim(size, (name));
        } else { // otherwise, it must be THE template element.

          // Just to make sure the schema validation didn't fail (because
          // I am basically paranoid about software) count the number of
          // template candidates we find and throw an Exception later
          // if there was more than one.
          countTemplateVars++;

          // The template element is just another BaseType
          // derived element. So, let's go build it!
          BaseType template = buildArrayTemplate(e, indent);

          // Oddly, in the OPeNDAP implementation of Array, the Array variable
          // takes it's name from it's (internal) template variable. This
          // is probably an artifact of the original DDSParser.

          // So, set the name of the template variable to the name of the Array.
          template.setClearName(da.getClearName());

          // Add the template variable to the Array
          da.addVariable(template);

        }
      }

        if (_Debug) {
            System.out.println(indent + "Built Array: ");
            da.printDecl(System.out, indent);
            System.out.println(indent + "dimensions: " + numDims + "  templates: " + countTemplateVars);
        }


        if (countTemplateVars != 1) {
            throw new NoSuchTypeException("ONLY ONE (1) TEMPLATE VARIABLE ALLOWED PER ARRAY!");
        }

    }


    /**
     * Builds the template variable for an array. The logic here has a lot
     * in common with the logic in parseBase. I considered trying to
     * refactor the code so that the two methods could utilize the same logic,
     * but I bagged it. Cie' la vie.
     * <p/>
     * Arrays of arrays are not allowed this rule should is enforced
     * through the schema validation process.
     */
    private BaseType buildArrayTemplate(Element template, String indent)
            throws DASException, NoSuchTypeException, BadSemanticsException {

        BaseType bt = null;

        if (_Debug) showXMLElement(template, indent + "...:");

        // Get all of the Attribute elements (tagged <Attribute>)
        Iterator attrElements = template.getChildren("Attribute", opendapNameSpace).iterator();

        if (attrElements.hasNext())
            throw new BadSemanticsException("Array Template Variables MAY NOT have Attributes");

        // Build the appropriate BaseType from the tag.
        bt = newBaseType(template);

        if (_Debug) System.out.println("Got template: " + bt.getTypeName() + "   " + bt.getClearName());

        // Now we need to make sure this particular BaseType
        // derived element isn't some special type that needs
        // additional parsing:

        // Is it a container?
        if (bt instanceof DConstructor) {

            // Up date the parsers state, (cache my parent)
            DConstructor myParentDC = parentDC;
            parentDC = (DConstructor) bt;
            try {
            // Grids are special containers, handle them
            if (bt instanceof DGrid) {
                parseGrid(template, indent);
            } else {
                // Otherwise, recurse on the children
              for (Element child : template.getChildren()) {
                parseBase(child, indent + "    ");
              }
            }
            } finally {
            // restore my parent
            parentDC = myParentDC;
            }

        }

        return (bt);
    }


    /**
     * Grids are unusual examples of DConstructor and require special
     * handling when parsing.
     */
    private void parseGrid(Element gridElement, String indent)
            throws DASException, NoSuchTypeException, BadSemanticsException {

        parseLevel++;

        // Grab the parent object (which better be a Grid!)
        // just to elminate the hassle of casting everytime...
        DGrid myGrid = (DGrid) parentDC;

        if (_Debug) {
            System.out.println("Parsing Grid Element: " + gridElement);
            System.out.println("Grid Elements: ");
            //showXMLElement(gridElement, indent);
          for (Element element : gridElement.getChildren()) System.out.println(element);
        }

        // Get and parse the grid's Array element.
        String eName = "Array";
        if (_Debug) {
            System.out.println("Parsing Array element.");
            System.out.println("Asking for element: '" + eName + "' in namespace: '" + opendapNameSpace + "'");
        }
        Element arrayElement = gridElement.getChild(eName, opendapNameSpace);
        if (_Debug) System.out.println("Got Array element: " + arrayElement);
        DArray gridArray = (DArray) newBaseType(arrayElement);
        parseArray(arrayElement, gridArray, indent + "    ");

        // Add it to the Grid
        myGrid.addVariable(gridArray, DGrid.ARRAY);

        // Get the Map elements
        eName = "Map";
        if (_Debug) {
            System.out.println("Parsing Map elements.");
            System.out.println("Asking for element: '" + eName + "' in namespace: '" + opendapNameSpace + "'");
        }
        List<Element> mapElements = gridElement.getChildren("Map", opendapNameSpace);

        // Make sure the number of Map elements matches the dimension of the Grid Array.
        if (mapElements.size() != gridArray.numDimensions())
            throw new BadSemanticsException("Error in Grid syntax: " +
                    "The number of Map arrays must " +
                    "equal the number of dimensions " +
                    "of the data array.");

        // Parse each Map element and poke it into the Grid.
      for (Element mapElement : mapElements) {
        DArray thisMap = (DArray) newBaseType(mapElement);
        parseArray(mapElement, thisMap, indent + "    ");

        if (thisMap.numDimensions() != 1)
          throw new BadSemanticsException("Error in Grid syntax: " +
                  "Maps may have only one dimension.");

        myGrid.addVariable(thisMap, DGrid.MAPS);

      }

        parseLevel--;

    }

    /**
     * A convienience function used for displaying information in _Debug mode.
     * Prints an XML element's name, content (if any) and Attributes to
     * System.out
     */
    private void showXMLElement(Element e, String indent) {


        System.out.print(parseLevel + indent + "Element: " + e.getName() + "  ");
        String text = e.getTextNormalize();
        if (!text.equals(""))
            System.out.print(" = " + text + "   ");

        //System.out.println("");


      for (Attribute att : e.getAttributes()) {
        //System.out.print(parseLevel + indent + "    ");
        System.out.print(att.getName() + ": " + att.getValue() + "  ");
      }
        System.out.println("");

      for (Element kid : e.getChildren()) {
        showXMLElement(kid, indent + "    ");
      }


    }


    /*
    * Builds a new BaseType derived type from the passed XML element.
    * <p>This happens in 4 steps:
    * <ul>
    * <li> 1) Determine the OPeNDAP type and vairiable name </li>
    * <li> 2) Get an new one of the thing in (1) </li>
    * <li> 3) Parse any Attribute tags associated with this OPeNDAP type.
    *         (They appear as children of the XML element)</li>
    * <li> 4) Parse any Alias tags associated with this OPeNDAP type.
    *         (They appear as children of the XML element)</li>
    * </ul>
    *
    */
    private BaseType newBaseType(Element e) throws DASException, NoSuchTypeException {

        if (_Debug) System.out.println("Getting new BaseType() from: " + e);

        // What's the Element Name? This IS the OPeNDAP typename.
        String type = e.getName();

        // What is the name of this variable? Since BaseType derived types
        // are not required to have names we have to do this carefully.
        String name = null;
        org.jdom2.Attribute nameAttr = e.getAttribute("name");

        if (nameAttr != null)
            name = nameAttr.getValue();

        if (_Debug) System.out.println("    type: " + type + "   name: '" + name + "'");

        // GO get a fresh new OPeNDAP variable (BaseType derived type)
        currentBT = newBaseTypeFactory(type, name);

        //Parse any Attribute tagged child elements for this variable.
        parseAttributes(e, "--- ");

        //Parse any Alias tagged child elements for this variable.
        parseAliases(e, "+++ ");

        return (currentBT);
    }


    /**
     * The name of this method might be a bit misleading. This method is basically
     * a wrapper for the BaseTypeFactory associated with the DDS that we are building.
     * <p/>
     * I think it should really be a method of BaseTypeFactory.
     * <p/>
     * Something like:
     * <p/>
     * BaseTypeFactory.getNewVariable(String typeString, String name)
     * <p/>
     * But, well BaseTypeFactory is an interface, so that's a crappy idea. *sigh*
     */
    private BaseType newBaseTypeFactory(String typeString, String name) throws NoSuchTypeException {

        BaseType bt;

        if (typeString.equals("Array") || typeString.equals("Map")) {

            bt = factory.newDArray();
            bt.setClearName(name);

        } else if (typeString.equals("Grid")) {

            bt = factory.newDGrid();
            bt.setClearName(name);

        } else if (typeString.equals("Structure")) {

            bt = factory.newDStructure();
            bt.setClearName(name);

        } else if (typeString.equals("Sequence")) {

            bt = factory.newDSequence();
            bt.setClearName(name);

        } else if (typeString.equals("Int16")) {

            bt = factory.newDInt16();
            bt.setClearName(name);

        } else if (typeString.equals("UInt16")) {

            bt = factory.newDUInt16();
            bt.setClearName(name);

        } else if (typeString.equals("Int32")) {

            bt = factory.newDInt32();
            bt.setClearName(name);

        } else if (typeString.equals("UInt32")) {

            bt = factory.newDUInt32();
            bt.setClearName(name);

        } else if (typeString.equals("Float32")) {

            bt = factory.newDFloat32();
            bt.setClearName(name);

        } else if (typeString.equals("Float64")) {

            bt = factory.newDFloat64();
            bt.setClearName(name);

        } else if (typeString.equals("Byte")) {

            bt = factory.newDByte();
            bt.setClearName(name);

        } else if (typeString.equals("String")) {

            bt = factory.newDString();
            bt.setClearName(name);

        } else if (typeString.equals("Url")) {

            bt = factory.newDURL();
            bt.setClearName(name);

        } else
            throw new NoSuchTypeException("Unknown Type: " + typeString);

        return bt;

    }


    /**
     * Parse the Attribute tags for a given variable element in the XML document.
     * Build the appropriate Attributes and AttributeTables and add them to the
     * the current variable's (currentBT's) AttributeTable.
     */

    private void parseAttributes(Element e, String indent)
            throws DASException, NoSuchTypeException {

        parseLevel++;

        String subIndent = indent + "    ";
        if (_Debug) System.out.println(indent + "Parsing Attributes: ");

        if (_Debug)
            System.out.println(subIndent + "currentBT: " + currentBT.getTypeName() + " " + currentBT.getClearName());

        // Get all of the Attribute elements (tagged <Attribute>)
      for (Element attrElement : e.getChildren("Attribute", opendapNameSpace)) {

        String name = null;
        Attribute nameAttr = attrElement.getAttribute("name");
        // no need to check that the getAttribute call worked because the Schema enforces
        // the presence of the "name" attribute for the <Attribute> tag in the OPeNDAP namespace
        name = nameAttr.getValue();

        String typeName = null;
        Attribute typeAttr = attrElement.getAttribute("type");
        // no need to check that the getAttribute call worked because the Schema enforces
        // the presence of the "type" attribute for the <Attribute> tag in the OPeNDAP namespace
        typeName = typeAttr.getValue();

        // Is this Attribute a container??
        if (typeName.equals("Container")) {

          // Make sure that the document is valid for Attribute Containers and Values
          Iterator valueChildren = attrElement.getChildren("value", opendapNameSpace).iterator();
          if (valueChildren.hasNext())
            throw new AttributeBadValueException(
                    "Container Attributes may " +
                            "contain only other Attributes.\n" +
                            "Container Attributes may NOT " +
                            "contain values.");

          // Cache the currentAT (AttributeTable), this might be a null
          // in which case the the container should be added to the currentBT.
          AttributeTable cacheAttributeTable = currentAT;

          if (_Debug)
            System.out.println(indent + "currentBT: " + currentBT.getTypeName() + " " + currentBT.getClearName());
          if (_Debug) System.out.println(indent + "Attribute '" + name + "' is type " + typeName);

          // Add the Attribute container to the appropriate object.
          // If the currentAT is null, this indicates that we are working
          // on the top level attributes for the currentBT, if it's not
          // then we are working on the Attributes for some AttributeTable
          // contained within the top level Attributes in the currentBT.
          // Set the currentAT to the newly built (and returned) AttributeTable
          if (currentAT == null)
            currentAT = currentBT.appendAttributeContainer(name);
          else
            currentAT = currentAT.appendContainer(name);

          // Go parse the child Attributes of this Attribute table.
          // Note that this is a recursive call.
          parseAttributes(attrElement, indent + "    ");

          // Now parse all of the Aliases that exist in this Attribute table.
          parseAliases(attrElement, "+++ ");

          // restore the currentAT from the cached one, thus regaining the
          // the state that we entered this method with.
          currentAT = cacheAttributeTable;

        } else {

          // Make sure that the document is valid for Attribute Containers and Values
          Iterator attrChildren = attrElement.getChildren("Attribute", opendapNameSpace).iterator();
          if (attrChildren.hasNext())
            throw new AttributeBadValueException(
                    "Attributes must be of type Container " +
                            "in order to contain other Attributes.\n" +
                            "Attributes of types other than Container " +
                            "must contain values.");

          // Walk through the <value> elements
          for (Element valueChild : attrElement.getChildren("value", opendapNameSpace)) {
            // Get the content of the value.
            // There are several methods for getting this content in the
            // org.jdom2.Element object. The method getText() makes no effort
            // to "normalize" the white space content. IE tabs, spaces,
            // carriage return, newlines are all preserved. This might not
            // be the right thing to do, but only time will tell.
            String value = valueChild.getText();

            if (_Debug) {
              System.out.println(subIndent + "Attribute '" + name + "' of " + currentBT.getClearName() +
                      " is type " + typeName + " and has value: " + value);
            }

            // get the Attribute value type code.
            int typeVal = opendap.dap.Attribute.getTypeVal(typeName);

            // Add the attribute and it's value to the appropriat
            // container. Note that the interface for appending
            // values to opendap.dap.Attributes is built such that
            // the Attribute must be named each time. If the Attribte
            // name already exists, then the value is added to the list
            // of values for the Attribute. If the Attribute name does not
            // already exist, a new Attribute is made to hold the value.
            if (currentAT == null)
              currentBT.appendAttribute(name, typeVal, value, true);
            else
              currentAT.appendAttribute(name, typeVal, value, true);

          }

        }

      }


        parseLevel--;

    }


    /**
     * Parse all of the Alias tags in this element of the XML document.
     * Add each one to the correct Attribute Table.
     */
    private void parseAliases(Element e, String indent) throws DASException {

        parseLevel++;

        String subIndent = indent + "    ";
        if (_Debug) System.out.println(indent + "Parsing Aliases: ");

        if (_Debug)
            System.out.println(subIndent + "currentBT: " + currentBT.getTypeName() + " " + currentBT.getClearName());

        // Get the Alias elements
      for (Element aliasElement : e.getChildren("Alias", opendapNameSpace)) {
        String name = null;
        Attribute nameAttr = aliasElement.getAttribute("name");
        // no need to check that the getAttribute call worked because the Schema enforces
        // the presence of the "name" attribute for the <Alias> tag in the OPeNDAP namespace
        name = nameAttr.getValue();

        String attributeName = null;
        Attribute attributeAttr = aliasElement.getAttribute("Attribute");
        // no need to check that the getAttribute call worked because the Schema enforces
        // the presence of the "Attribute" attribute for the <Alias> tag in the OPeNDAP namespace
        attributeName = attributeAttr.getValue();

        if (_Debug) {
          System.out.println(subIndent + "The name '" + name +
                  "' is aliased to dds attribute: '" + attributeName + "'");
        }

        // Add the Alias to the appropriate container.
        if (currentAT == null)
          currentBT.addAttributeAlias(name, attributeName);
        else
          currentAT.addAlias(name, attributeName);

      }

        parseLevel--;
    }


    /**
     * This method is used to normalize strings prior
     * to their inclusion in XML documents. XML has certain parsing requirements
     * around reserved characters. These reserved characters must be replaced with
     * symbols recognized by the XML parser as place holder for the actual symbol.
     * <p/>
     * The rule for this normalization is as follows:
     * <p/>
     * <ul>
     * <li> The &lt; (less than) character is replaced with &amp;lt;
     * <li> The &gt; (greater than) character is replaced with &amp;gt;
     * <li> The &amp; (ampersand) character is replaced with &amp;amp;
     * <li> The ' (apostrophe) character is replaced with &amp;apos;
     * <li> The &quot; (double quote) character is replaced with &amp;quot;
     * </ul>
     *
     * @param s The String to be normalized.
     * @return The normalized String.
     */
    public static String normalizeToXML(String s) {

        // Some handy definitons.
        String xmlGT = "&gt;";
        String xmlLT = "&lt;";
        String xmlAmp = "&amp;";
        String xmlApos = "&apos;";
        String xmlQuote = "&quot;";

        boolean Debug = false;
        StringBuilder sb = new StringBuilder(s);

        for (int offset = 0; offset < sb.length(); offset++) {

            char c = sb.charAt(offset);

            switch (c) {

                case '>': // GreaterThan
                    sb.replace(offset, offset + 1, xmlGT);
                    break;

                case '<': // Less Than
                    sb.replace(offset, offset + 1, xmlLT);
                    break;

                case '&': // Ampersand
                    sb.replace(offset, offset + 1, xmlAmp);
                    break;

                case '\'': // Single Quote
                    sb.replace(offset, offset + 1, xmlApos);
                    break;

                case '\"': // Double Quote
                    sb.replace(offset, offset + 1, xmlQuote);
                    break;

                default:
                    break;
            }

        }
        //Coverity[DEADCODE]
        if (Debug) System.out.println("String: `" + s + "` normalized to: `" + sb + "`");

        return (sb.toString());

    }


}


