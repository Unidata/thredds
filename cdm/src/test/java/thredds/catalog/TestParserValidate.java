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
package thredds.catalog;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import java.io.*;
import java.net.*;
import java.util.*;

/*
 VERSION notes

  Havent been able to detect how to know what version parser you have. may be new feature in JAXP 1.3

  Java 1.4.2 uses JAXP 1.1, which doesnt do W3S Schema validation. (see http://xml.apache.org/~edwingo/jaxp-ri-1.2.0-fcs/docs/)
    when you call factory.setAttribute() get message "No attributes are implemented"
    when you validate=true, get warning "Valid documents must have a <!DOCTYPE declaration.", plus lots of errors,
    but no fatal errors.

  It appears that "Tiger should support the latest version of JAXP...JAXP will be update to conform to the
    new specifications: XML 1.1 Namespaces 1.1 SAX 2.0.1". Note they dont actually say what that is. My
    JavaOne notes claim JAXP 1.3 (JSR 206). Hard to tell what they've added: access to version info and
    perhaps XInclude.

  JAXP 1.2 available in WSDP 1.3.
  Install JAXP 1.2 into J2SE 1.4, see http://java.sun.com/webservices/docs/1.2/jaxp/Updating.html

  "Version 1.4 of the Java 2 platform has a JAXP 1.1 implementation built in. (JAXP 1.1 has a smaller
   application footprint, but JAXP 1.2 implements XML Schema and the transform compiler, XSLTC.)
   Because the built-in libraries are accessed before any classpath entries, you can only access the
   updated libraries by using the Java platform's Endorsed Standards mechanism.
  There are two ways to use the endorsed standards mechanism. The first way is to copy all of the jar
    files except jaxp-api.jar into <JAVA_HOME>/jre/lib/endorsed/
  Note:
   The jaxp-api.jar file should not be copied, because it contains high-level factory APIs that are not
    subject to change.
  Alternatively, you can use the java.endorsed.dirs system property to dynamically add those jar files to
    the JVM when you start your program. Using that system property gives you flexibility of using different
    implementations for different applications.  You specify that property using
     -Djava.endorsed.dirs=yourDirectoryPath.
  Finally, although the endorsed directory mechanism guarantees that the specified jars will be searched
   before the internal classes in the VM (which are searched before the classpath), the order in which the
   jar files are searched is indeterminate. For that reason, there should be no overlaps in the classes
   specified using that property. For more information, see the Endorsed Standards documentation for the
   1.4 version of the Java platform."

  It seems that this will make webstart delivery impossible ?
*/

/*
  Cant set JAXP_SCHEMA_LANGUAGE on catalog 0.6, get:
    ** Non-Fatal XML Error error(s) =
    *** XML parser error=cvc-elt.1: Cannot find the declaration of element 'catalog'.
  so validation is not done. Problem is that in JAXP 1.2, setting JAXP_SCHEMA_LANGUAGE overrides the DTD
   even when theres no schema declared!

  Of cource, you dont know what version until you start parsing !!!!

  "Set Schema Source" in JAXP 1.2: see http://java.sun.com/xml/jaxp/change-requests-11.html
*/

public class TestParserValidate {

  // JAXP and DTD caching
  private String dtdUrl = "http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.dtd";
  private String dtdString;
  private DocumentBuilderFactory factory;

  boolean debugJaxp = true, schemaValidation = true;

  private String name, version;

  private HashMap versionHash = new HashMap(10);
  private StringBuffer errMessages = new StringBuffer();
  private StringBuffer fatalMessages = new StringBuffer();


  // schema validation
  static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
  static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
  static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

  static final String XHTML_SCHEMA = "http://www.w3.org/1999/xhtml";

  // only use one DocumentBuilderFactory, so needs to be synchronized
  DocumentBuilder getDocumentBuilder(boolean validate) {

    /* DTD cache
    try { // try to read from local file resource, eg from catalog.jar
      ByteArrayOutputStream sbuff = new ByteArrayOutputStream(3000);
      InputStream is = thredds.util.Resource.getFileResource( "/xml/InvCatalog.0.6.dtd");
      if (is != null) {
        thredds.util.IO.copy(is, sbuff);
        dtdString = sbuff.toString();
      } else { // otherwise, get from network and cache
        dtdString = thredds.util.IO.readURLcontentsWithException(dtdUrl);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } */

    // Get Document Builder Factory
    factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(validate);

    if (schemaValidation) {
      try {
        factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        // factory.setAttribute(JAXP_SCHEMA_SOURCE, new File(schemaSource));
      }
      catch (IllegalArgumentException e) {
        System.out.println("***This can happen if the parser does not support JAXP 1.2\n"+e);
      }
    }

    if (debugJaxp) showFactoryInfo(factory);


    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver(new MyEntityResolver());
      builder.setErrorHandler(new MyErrorHandler());

      if (debugJaxp) showBuilderInfo(builder);
      return builder;
    }
    catch (ParserConfigurationException e) {
      System.out.println("The underlying parser does not support the requested features.");
    }
    catch (FactoryConfigurationError e) {
      System.out.println("Error occurred obtaining Document Builder Factory.");
    }
    return null;
  }

  static private void showFactoryInfo( DocumentBuilderFactory factory) {
    System.out.println("-----------------------");
    System.out.println(" factory.isValidating()="+factory.isValidating());
    System.out.println(" factory.isNamespaceAware()="+factory.isNamespaceAware());
    System.out.println(" factory.isIgnoringElementContentWhitespace()="+factory.isIgnoringElementContentWhitespace());
    System.out.println(" factory.isExpandEntityReferences()="+factory.isExpandEntityReferences());
    System.out.println(" factory.isIgnoringComments()="+factory.isIgnoringComments());
    System.out.println(" factory.isCoalescing()="+factory.isCoalescing());
    System.out.println("-----------------------");
  }

  static private void showBuilderInfo( DocumentBuilder builder) {
     System.out.println("-----------------------");
   System.out.println(" builder.isValidating()="+builder.isValidating());
    System.out.println(" builder.isNamespaceAware()="+builder.isNamespaceAware());
    System.out.println("-----------------------");
  }

  private class MyEntityResolver implements org.xml.sax.EntityResolver {

    public org.xml.sax.InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
      if (debugJaxp) System.out.println("** publicId= "+publicId+" systemId="+systemId);
      return null;
      /* if (systemId.equals("http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.dtd")) {
        version = "0.6";
        return new MyInputSource();
      } else if (systemId.indexOf("InvCatalog.0.6.dtd") >= 0) {
        version = "0.6";
        return new MyInputSource();
      } else
        return null; */
    }

  }

  private class MyInputSource extends org.xml.sax.InputSource {

    MyInputSource() {
      setCharacterStream(new StringReader(dtdString));
    }

  }

  private class MyErrorHandler implements org.xml.sax.ErrorHandler {

    public void warning(SAXParseException e) throws SAXException {
      errMessages.append("*** XML parser warning="+e.getMessage()+"\n");
    }

    public void error(SAXParseException e) throws SAXException {
      errMessages.append("*** XML parser error="+e.getMessage()+"\n");
    }

    public void fatalError(SAXParseException e) throws SAXException {
      fatalMessages.append("*** XML parser fatalError="+e.getMessage()+"\n");
    }

  }


  public void read(String uriString, boolean validate) {
    URI uri = null;
    try {
      uri = new URI( uriString);
    } catch (URISyntaxException e) {
      System.out.println( "**Fatal:  InvCatalogFactory.readXML URISyntaxException on URL ("+
        uriString+") "+e.getMessage()+"\n");
      return;
    }

    // get ready for XML parsing
    DocumentBuilder builder = getDocumentBuilder(validate);
    errMessages.setLength(0);
    fatalMessages.setLength(0);
    version = null;
    Document doc = null;
    try {
      doc = builder.parse(uriString);
      System.out.println("JAXP Parse OK");
    } catch (Exception e) {
      fatalMessages.append( "**Fatal:  InvCatalogFactory.readXML failed"
        +"\n Exception= "+e.getClass().getName()+" "+e.getMessage()
        +"\n fatalMessages= " +fatalMessages.toString()
        +"\n errMessages= " +errMessages.toString()+"\n");
    }

    if (fatalMessages.length() > 0) {
      System.out.println("**Fatal: XML error(s) =\n"+
        fatalMessages.toString()+"\n");
    }

    if (errMessages.length() > 0) {
      System.out.println("** Non-Fatal XML Error error(s) =\n"+
        errMessages.toString()+"\n");
    }

  }

  public static void main(String[] arg) {
    TestParserValidate test = new TestParserValidate();
    test.read("file:///C:/dev/thredds/xml/v7/ExampleJoin.xml", true );
    //test.read("file:///C:/dev/thredds/xml/v7/testQualify.xml", true );
    //test.read("file:///C:/dev/thredds/server/catalogBad.xml", true);
   // read("http://motherlode.ucar.edu/cgi-bin/thredds/MetarServer.pl?format=qc", null);
    //read("E:/metadata/netcdf/testValidate.xml",
      //   "http://www.ucar.edu/schemas/netcdf E:/metadata/netcdf/netcdf2.xsd");
  }
}