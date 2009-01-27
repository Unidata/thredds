/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/*****************************************************************
 * This code is free and may be redistributed in any form given that
 * credit is given to all authors, and the original content from the
 * class header is included.
 *****************************************************************
 *
 * This class will parse and validate a given xml/xsd pair
 *
 *  @author Clint M. Frederickson
 *****************************************************************/
package thredds.catalog;

import java.io.*;

import java.util.List;
import java.util.Iterator;
import java.lang.String;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

public class XMLTest
{
    XMLTest()
    {/*empty constructor*/}

    /*******************************************************************
     * This method instantiates the parser, sets it to validate against
     * a schema, and builds the document.
     *
     * @param xml The xml filename
     * @param xsd The xsd *namespace* and filename
     *******************************************************************/

    public void buildDoc(String xml, String xsd)
    {
	try
	{
	    //builds a xerces parser with validation turned on
	    builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", true);
	    // builder = new SAXBuilder(false);

	    //turns on Schema Validation with Xerces
	    builder.setFeature("http://xml.org/sax/features/validation", true);
	    builder.setFeature("http://apache.org/xml/features/validation/schema", true);
	    System.out.println("Feature set...");

	    /* This gives the XML Schema to be used.
	     * NOTE: See driver for explanation of the xsd paramater
	     */
	    //builder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", xsd);

	    System.out.println("Property set...");


	    System.out.println("Building document...");
	    /* This is where the document gets built (If everything is okay in
	     * the given XML/XSD pair)
	     */
	    document = builder.build(xml);
	    System.out.println(xml + " was parsed and verified against " +
			       xsd + " successfully!");

	    root = document.getRootElement();
	}

	catch(Exception e)
	{
	    System.out.println("Excpetion thrown on load.");
	    System.out.println(e);
	}
    }



    /**************************************************************
     * This method prints the document content (which should be the
     * root element and namespace. This method is primarily used to
     * give the user feedback that the parse finished sucessfully.
     **************************************************************/
    public void printDocContent()
    {
	Iterator iter;
	content = document.getContent();
	iter = content.iterator();

	System.out.println("\nList Size: " + content.size());

	while(iter.hasNext())
	    System.out.println(iter.next());
    }

    /**************************************************************
     * This method will traverse the tree searching for a specifc
     * Element.
     *
     * @param elementName The Element name to search for
     * @return boolean Whether the given Element exists
     **************************************************************/

    public boolean doesElementExist(String elementName)
    {
	List children = root.getChildren();
	Iterator iter = children.iterator();


	printListSize(children);
	printList(children);
	System.out.println("Looking for " + elementName);
	System.out.println("Found: " + root.getName());


	return (searchSubTree(root, elementName));



    }
    /**************************************************************
     * This recursively searches each subTree until it finds the
     * requested Element, or it has searched the whole tree
     *
     * @param subRoot the node that is currently being considered
     *                the root of the subTree
     * @param searchElem the Element being searched for
     *
     * @return boolean Whether the given Element was found in the
     *                 current subtree
     **************************************************************/
    private boolean searchSubTree(Element subRoot, String searchElem)
    {
	List children = subRoot.getChildren();
	Iterator iter = children.iterator();

	//while there are more children (branhes) to be checked
	while(iter.hasNext())
	{
	    System.out.println("Found: '" + subRoot.getName() +
			       "'  Looking for: '" + searchElem + "'");

	    if( (subRoot.getName()).equals(searchElem))
		return true; //the node has been found


	    if(searchSubTree((Element)iter.next(), searchElem))
		return true; //propogates the 'true' value up through recursion
	}
	return false; //node is not in tree
    }

    /**************************************************************
     * This method will print the size of the list given to it.
     *
     * @param l List to print size of
     **************************************************************/
    private void printListSize(List l)
    {
	System.out.println("\nList Size: " + l.size());
    }

    /**************************************************************
     * This method will traverse the list, printing each element
     *
     * @param l List to print
     **************************************************************/
    private void printList(List l)
    {
	Iterator iter = l.iterator();

	while(iter.hasNext())
	    System.out.println(iter.next());
    }

    /*****************************************************************
     * This main method drives the test program. Notice that the second
     * command line argument given is actually two strings. YOU MUST
     * ENCLOSE THOSE TWO STRINGS IN SINGLE QUOTES FOR IT TO BE READ AS
     * AS ONE ARGUMENT!
     *
     * @param args[1] the name of the xml file
     * @param args[2] the namespace AND xsd file
     *****************************************************************/
    public static void main(String [] args) throws Exception
    {
	String elmtToFind = "ELEMENT NAME";
	XMLTest xmlReader = new XMLTest();

        /*
	if(args.length != 2)
	{
	    System.out.println("Error: syntax error");
	    System.out.println("\nusage: xml xsd");
	    System.out.print("example: java XMLTest sample.xml ");
	    System.out.println("'http://domain.org/xml/sample sample.xsd'");
	    System.out.println();
	    System.exit(1);
	}

	xmlReader.buildDoc(args[0], args[1]); */
	xmlReader.buildDoc("N:/thredds/src/test/catalog/editor.xml",
         "http://www.xml-schema.com/examples/schema/Editor N:/thredds/src/test/catalog/editor.xsd");
  //       "'http://www.xml-schema.com/examples/schema/Editor N:/thredds/src/test/catalog/editor.xsd'");
	xmlReader.printDocContent();

	if(xmlReader.doesElementExist(elmtToFind))
	    System.out.println("ELEMENT FOUND!! : " + elmtToFind);
	else
	    System.out.println("ELEMENT NOT FOUND!");


    }

    private SAXBuilder builder;
    private Document document;
    private List content;
    private String xsd;
    private String xml;
    private Element root;

}
