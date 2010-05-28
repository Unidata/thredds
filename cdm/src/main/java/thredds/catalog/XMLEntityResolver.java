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
import org.xml.sax.*;
import org.jdom.input.SAXBuilder;
import org.jdom.Namespace;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

import ucar.nc2.util.IO;

/**
 * Handles the interaction with JAXP, resolving dtd, schemas locally if possible.
 * Get a javax.xml.parsers.DocumentBuilder from here, allows you to validate or not.
 * <p>
 * The Crimson parser wont do schema validation.
 *
 * @author John Caron
 */

/*
  PROBLEM: in JAXP 1.2, setting JAXP_SCHEMA_LANGUAGE overrides the DTD
   even when theres no schema declared!

  Cant set JAXP_SCHEMA_LANGUAGE on catalog 0.6, get:
    ** Non-Fatal XML Error error(s) =
    *** XML parser error=cvc-elt.1: Cannot find the declaration of element 'catalog'.
  so validation is not done.

  Of course, you dont know what version until you start parsing !!!!

  "Set Schema Source" in JAXP 1.2: see http://java.sun.com/xml/jaxp/change-requests-11.html

  workaround: make a schema version of old DTD.
  problem: need Xerces parser (crimson wont do schema validation). So if you dont have, we dont even get DTD
    validation.
*/

/*
http://java.sun.com/xml/jaxp/reference/faqs/index.html

Q. How do I use a different JAXP compatible implementation?

The JAXP 1.1 API allows applications to plug in different JAXP compatible implementations of parsers or XSLT processors. For example, when an application wants to create a new JAXP DocumentBuilderFactory instance, it calls the staic method DocumentBuilderFactory.newInstance(). This causes a search for the name of a concrete subclass of DocumentBuilderFactory using the following order:

The value of a system property like javax.xml.parsers.DocumentBuilderFactory if it exists and is accessible.
The contents of the file $JAVA_HOME/jre/lib/jaxp.properties if it exists.
The Jar Service Provider discovery mechanism specified in the Jar File Specification. A jar file can have a resource (i.e. an embedded file) such as META-INF/services/javax.xml.parsers.DocumentBuilderFactory containing the name of the concrete class to instantiate.
The fallback platform default implementation.
Of the above ways to specify an implementation, perhaps the most useful is the jar service provider mechanism. To use this mechanism, place the implementation jar file on your classpath. For example, to use Xerces 1.4.4 instead of the version of Crimson which is bundled with JDK 1.4 (Java Development Kit version 1.4), place xerces.jar in your classpath. This mechanism also works with older versions of the JDK which do not bundle JAXP. If you are using JDK 1.4 and above, see this question for potential problems.

*/

/*
 VERSION notes

  Havent been able to detect how to know what version parser you have. May be new feature in JAXP 1.3

  Java 1.4.2 uses JAXP 1.1, which doesnt do W3S Schema validation.
              (see http://xml.apache.org/~edwingo/jaxp-ri-1.2.0-fcs/docs/)
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

  Webstart see http://forum.java.sun.com/thread.jsp?forum=38&thread=231995

"With my workaround for the context classloader in-place (see bug report) all I've ever needed to switch
 between xerces and crimson is the single property;
 <property name="javax.xml.parsers.SAXParserFactory" value="org.apache.xerces.jaxp.SAXParserFactoryImpl"/>

added into my jnlp; along with the jars, jaxp.jar, xercesImpl.jar & xmlParserAPIs.jar. (jnlp also needs to
   work under 1.2 & 1.3 - and it does!).
Hardcoding the parser just defeats the purpose of JAXP, although I too am wondering just which parser is
  going to be the default in the future - I was expecting xerces in 1.4.1 as it's the only way to handle
  schemas, maybe when JAXP gets updated with its schema properties/features I guess. "

----------

Add these to the jnlp file: (see Viewer.jnlp):

 <property name="javax.xml.parsers.SAXParserFactory" value="org.apache.xerces.jaxp.SAXParserFactoryImpl"/>
 <property name="javax.xml.parsers.DocumentBuilderFactory" value="org.apache.xerces.jaxp.DocumentBuilderFactoryImpl"/>

 these 2 classes are in xercesImpl.jar

  org.apache.xerces.* not in 1.4.2/rt.jar
*/

/* 4/26/04
   not apparently picking up the unique/key/keyref constraints in schema.
   if catalog has 2 datasets with same ID; dont get an error.
   does give an error using catalog 0.6, which still uses schema, but uses xs:ID type.
 */

/* 5/10/04
  tomcat (5.0.19, 5.0.24) ships with "Xerces-J 2.6.1" in common/endorsed
*/

public class XMLEntityResolver implements org.xml.sax.EntityResolver {
  static private boolean debugEntityResolution = false; //, debugFactory = false, debugMessages = false;
  static private Map<String, String> entityHash = new HashMap<String, String>();

  // schema validation
  static private boolean schemaValidationOk = true;
  static private final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
  static public final String W3C_XML_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
  static private final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

  // catalog namespaces
  static public final String CATALOG_NAMESPACE_06 = "http://www.unidata.ucar.edu/thredds";
  static public final String CATALOG_NAMESPACE_10 = "http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0";

  // dqc namespaces
  static public final String DQC_NAMESPACE_02 = "http://www.unidata.ucar.edu/schemas/thredds/queryCapability";
  static public final String DQC_NAMESPACE_03 = "http://www.unidata.ucar.edu/namespaces/thredds/queryCapability/v0.3";
  static public final String DQC_NAMESPACE_04 = "http://www.unidata.ucar.edu/namespaces/thredds/queryCapability/v0.4";

  // catgen namespace
  static public final String CATGEN_NAMESPACE_05 = "http://www.unidata.ucar.edu/namespaces/thredds/CatalogGenConfig/v0.5";

  // nj22 namespaces
  static public final String NJ22_NAMESPACE = "http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2";

  // other namespaces
  static public final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
  static public final Namespace xlinkNS = Namespace.getNamespace("xlink", XLINK_NAMESPACE);

  static public final Namespace xsiNS = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");


  static {
    /* Get Document Builder Factory
    docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setNamespaceAware(true);

    // try to get schema validation
    if (schemaValidationOk) {
      try {
        docBuilderFactory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_NAMESPACE);
        docBuilderFactory.setAttribute(JAXP_SCHEMA_SOURCE,
           new String[] { CATALOG_NAMESPACE_06, CATALOG_NAMESPACE_10, XLINK_NAMESPACE, DQC_NAMESPACE_02, DQC_NAMESPACE_03 } );
      }
      catch (IllegalArgumentException e) {
        System.out.println("***Warning: XML Parser does not support JAXP 1.2. Library will continue without validation.\n"+e);
        schemaValidationOk = false;
      }
    }

    if (debugFactory) showFactoryInfo( docBuilderFactory); */

    // set up entity resolution - see MyEntityResolver class
    // catalog 0.6 dtd
    initEntity( "http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.dtd",
                "/resources/thredds/schemas/InvCatalog.0.6.dtd",
                "http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.dtd");

    // catalog 1.0 schema
    initEntity( CATALOG_NAMESPACE_10,
                "/resources/thredds/schemas/InvCatalog.1.0.3.xsd",
                "http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.3.xsd");

    // catalog 0.6 schema
    initEntity( CATALOG_NAMESPACE_06,
                "/resources/thredds/schemas/InvCatalog.0.6.xsd",
                "http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.0.6.xsd");

    // DQC schema 0.2
    initEntity( DQC_NAMESPACE_02,
                "/resources/thredds/schemas/queryCapability.0.2.xsd",
                "http://www.unidata.ucar.edu/schemas/thredds/queryCapability.0.2.xsd");

    // DQC schema 0.3
    initEntity( DQC_NAMESPACE_03,
                "/resources/thredds/schemas/queryCapability.0.3.xsd",
                "http://www.unidata.ucar.edu/schemas/thredds/queryCapability.0.3.xsd");

    // DQC schema 0.4
    initEntity( DQC_NAMESPACE_04,
                "/resources/thredds/schemas/queryCapability.0.4.xsd",
                "http://www.unidata.ucar.edu/schemas/thredds/queryCapability.0.4.xsd");

    // nj22 schema
    initEntity( NJ22_NAMESPACE,
                "/resources/nj22/schemas/ncml-2.2.xsd",
                "http://www.unidata.ucar.edu/schemas/netcdf/ncml-2.2.xsd");

    // XLink schema
    initEntity( XLINK_NAMESPACE,
                "/resources/thredds/schemas/xlink.xsd",
                "http://www.unidata.ucar.edu/schemas/other/xlink.xsd");

    // catgen 0.5 dtd
    initEntity( "http://www.unidata.ucar.edu/projects/THREDDS/xml/CatalogGenConfig.0.5.dtd",
                "/resources/thredds/schemas/CatalogGenConfig.0.5.dtd",
                "http://www.unidata.ucar.edu/projects/THREDDS/xml/CatalogGenConfig.0.5.dtd");

    String javaVersion = System.getProperty("java.version").substring(2,3);  // ie 1.5_02
    int v = Integer.parseInt(javaVersion);
    hasXerces = (v >= 5);
  }

  static private boolean hasXerces = true;

  static private String externalSchemas;
  static public String getExternalSchemas() {
    if (externalSchemas == null) { externalSchemas =
      XMLEntityResolver.CATALOG_NAMESPACE_10 + " " + "http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.3.xsd" +" "+
      XMLEntityResolver.NJ22_NAMESPACE + " " + "http://www.unidata.ucar.edu/schemas/netcdf/ncml-2.2.xsd" +" " +
      XMLEntityResolver.DQC_NAMESPACE_04 + " " + "http://www.unidata.ucar.edu/schemas/thredds/queryCapability.0.4.xsd" +" " +
      XMLEntityResolver.DQC_NAMESPACE_03 + " " + "http://www.unidata.ucar.edu/schemas/thredds/queryCapability.0.3.xsd" +" " +
      XMLEntityResolver.DQC_NAMESPACE_02 + " " + "http://www.unidata.ucar.edu/schemas/thredds/queryCapability.0.2.xsd" +" " +
      XMLEntityResolver.CATALOG_NAMESPACE_06 + " " + "http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.0.6.xsd";
    }
    return externalSchemas;
  }

  /**
   * Add an entity for resolution. Specify a local resource, and/or a URL. Look
   * for the local Resource first.
   *
   * @param entityName name of entity, eg the namespace String
   * @param resourceName resolve using this Resource, found on the class path
   * @param urlName resolve using this Resource, found on the class path
   */
  static public void initEntity( String entityName, String resourceName, String urlName) {
    String entity = null;

    try { // try to read from local file resource, eg from catalog.jar
      ByteArrayOutputStream sbuff = new ByteArrayOutputStream(3000);
      InputStream is = ucar.nc2.util.IO.getFileResource( resourceName);
      if (is != null) {
        IO.copy(is, sbuff);
        entity = sbuff.toString();
        if (debugEntityResolution) System.out.println(" *** entity "+entityName+" mapped to local resource at "+resourceName);

      } else if (urlName != null) { // otherwise, get from network
        entity = IO.readURLcontentsWithException(urlName);
        if (debugEntityResolution) System.out.println(" *** entity "+entityName+" mapped to remote URL at "+urlName);
      }

    } catch (IOException e) {
      System.out.println(" *** FAILED to map entity "+entityName+" locally at "+resourceName+" or remotely at "+urlName);
      // e.printStackTrace();
    }

    entityHash.put( entityName, entity);
    entityHash.put( urlName, entity); // also map it to the remote URL
  }

  static public String getDocumentBuilderFactoryVersion() {
    try {
      Class version = Class.forName("org.apache.xerces.impl.Version");
      Method m = version.getMethod("getVersion", (Class []) null);
      return (String) m.invoke(null, (Object []) null);
    } catch (Exception e) {
      return "Error= "+e.getMessage();
    }
  }

  static private void showFactoryInfo(DocumentBuilderFactory factory) {
    System.out.println("------------------------");
    System.out.println("DocumentBuilderFactory class= "+factory.getClass().getName());
    try {
      //ClassLoader cl = factory.getClass().getClassLoader();
      //Class version = cl.loadClass("org.apache.xerces.impl.Version");
      Class version = Class.forName("org.apache.xerces.impl.Version");
      Method m = version.getMethod("getVersion", (Class []) null);
      System.out.println(" org.apache.xerces.impl.Version.version()="+m.invoke(null, (Object []) null));

      //thredds.util.reflect.ProxyGenerator.showMethods( factory.getClass(), System.out);
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println();

    System.out.println(" factory.isValidating()="+factory.isValidating());
    System.out.println(" factory.isNamespaceAware()="+factory.isNamespaceAware());
    System.out.println(" factory.isIgnoringElementContentWhitespace()="+factory.isIgnoringElementContentWhitespace());
    System.out.println(" factory.isExpandEntityReferences()="+factory.isExpandEntityReferences());
    System.out.println(" factory.isIgnoringComments()="+factory.isIgnoringComments());
    System.out.println(" factory.isCoalescing()="+factory.isCoalescing());

  }

  static private void showBuilderInfo( DocumentBuilder builder) {
    System.out.println("-----------------------");
    System.out.println(" builder.isValidating()="+builder.isValidating());
    System.out.println(" builder.isNamespaceAware()="+builder.isNamespaceAware());

    System.out.println("DocumentBuilder class= "+builder.getClass().getName());
    /* try {
      thredds.util.reflect.ProxyGenerator.showMethods( builder.getClass(), System.out);
      thredds.util.reflect.ProxyGenerator.showMethods( builder.getDOMImplementation().getClass(), System.out);
    } catch (Exception e) {
      e.printStackTrace();
    } */

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  /* private DocumentBuilder builder; // JAXP parser

  private StringBuffer warnMessages = new StringBuffer();
  private StringBuffer errMessages = new StringBuffer();
  private StringBuffer fatalMessages = new StringBuffer();
  private String version = ""; */

  /* Constructor.
   * @param validate : do XML validation or not.
   *
  public XMLEntityResolver(boolean validate) {

    try {
      synchronized (docBuilderFactory) { // Only use one DocumentBuilderFactory, so needs to be synchronized
          docBuilderFactory.setValidating(validate && schemaValidationOk);
          builder = docBuilderFactory.newDocumentBuilder();
      }
      builder.setEntityResolver(this);
      builder.setErrorHandler(new MyErrorHandler());

      if (debugFactory) showBuilderInfo(builder);

    } catch (ParserConfigurationException e) {
      System.out.println("The underlying parser does not support the requested features.");
      throw new UnsupportedOperationException();

    } catch (FactoryConfigurationError e) {
      System.out.println("Error occurred obtaining Document Builder Factory.");
      throw new UnsupportedOperationException();
    }

  } */

  private SAXBuilder saxBuilder;
  private StringBuilder warnMessages = new StringBuilder();
  private StringBuilder errMessages = new StringBuilder();
  private StringBuilder fatalMessages = new StringBuilder();

  public XMLEntityResolver(boolean validate) {
    saxBuilder = hasXerces ? new SAXBuilder( validate) : new SAXBuilder("org.apache.xerces.parsers.SAXParser", validate);
    saxBuilder.setErrorHandler( new MyErrorHandler() );
    if (validate) {
      saxBuilder.setFeature( "http://apache.org/xml/features/validation/schema", true);
      saxBuilder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
        XMLEntityResolver.getExternalSchemas());
    }
    saxBuilder.setEntityResolver( this);
  }

  public SAXBuilder getSAXBuilder() { return saxBuilder; }
  public StringBuilder getWarningMessages() { return warnMessages; }
  public StringBuilder getErrorMessages() { return errMessages; }
  public StringBuilder getFatalMessages() { return fatalMessages; }
  // public String getVersion() { return version; }

  // we read the DTD/schema locally if we can.
  public org.xml.sax.InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    if (debugEntityResolution) System.out.print("  publicId="+publicId+" systemId="+systemId);
    String entity = entityHash.get( systemId);
    if (entity != null) {
      if (debugEntityResolution) System.out.println(" *** resolved  with local copy");
      return new MyInputSource(entity);
    }

    if (systemId.indexOf("InvCatalog.0.6.dtd") >= 0) {
      entity = entityHash.get( "http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.dtd");
      if (entity != null) {
        if (debugEntityResolution) System.out.println(" *** resolved2 with local copy");
        return new MyInputSource(entity);
      }
    }

    if (debugEntityResolution) System.out.println(" *** not resolved");
    return null;
  }


  private class MyErrorHandler implements org.xml.sax.ErrorHandler {

    public void warning(SAXParseException e) throws SAXException {
      warnMessages.append("*** XML parser warning ").append(showError(e)).append("\n");
    }

    public void error(SAXParseException e) throws SAXException {
      errMessages.append("*** XML parser error ").append(showError(e)).append("\n");
    }

    public void fatalError(SAXParseException e) throws SAXException {
      fatalMessages.append("*** XML parser fatalError ").append(showError(e)).append("\n");
    }

    private String showError( SAXParseException e) {
      return "("+e.getLineNumber()+":"+e.getColumnNumber()+")= "+e.getMessage();
    }

  }

  private class MyInputSource extends org.xml.sax.InputSource {
    MyInputSource( String entity) {
      setCharacterStream(new StringReader(entity));
    }
  }

  /* private class MyErrorHandler implements org.xml.sax.ErrorHandler {

    public void warning(SAXParseException e) throws SAXException {
      warnMessages.append("*** XML parser warning="+e.getMessage()+"\n");
      if (debugMessages) System.out.println("*** XML parser warning="+e.getMessage()+"\n");
    }

    public void error(SAXParseException e) throws SAXException {
      errMessages.append("*** XML parser error="+e.getMessage()+"\n");
      if (debugMessages) System.out.println("*** XML parser error="+e.getMessage()+"\n");
    }

    public void fatalError(SAXParseException e) throws SAXException {
      fatalMessages.append("*** XML parser fatalError="+e.getMessage()+"\n");
      if (debugMessages) System.out.println("*** XML parser fatalError="+e.getMessage()+"\n");
    }

  } */


  /************************************************************************/
  // test

  /* private static void doOne( XMLEntityResolver fac, DocumentBuilder builder, String url) {
    System.out.println("***read "+url);
    fac.getFatalMessages().setLength(0);
    fac.getErrorMessages().setLength(0);
    try {
      builder.parse( url);
      System.out.println(" fatal= " + fac.getFatalMessages());
      System.out.println(" errors= " + fac.getErrorMessages());
      System.out.println(" warn= " + fac.getWarningMessages());
    } catch (Exception e) { e.printStackTrace(); }

  }

  public static void main (String[] args) throws Exception {
    XMLEntityResolver fac = new XMLEntityResolver(true);
    DocumentBuilder builder = fac.getDocumentBuilder();

    /* cat 0.6
    doOne( fac, builder, "file:///C:/dev/thredds/catalog/test/data/InvCatalog.0.6.xml");
    doOne( fac, builder, "file:///C:/dev/thredds/catalog/test/data/ParseFails.xml");
    doOne( fac, builder, "file:///C:/dev/thredds/catalog/test/data/MalFormed.xml");

    // cat 1.0
    doOne( fac, builder, "file:///C:/dev/thredds/catalog/test/data/Example1.0rc7.xml"); //

    /* dqc 0.2
    doOne( fac, builder, "file:///C:/dev/thredds/catalog/test/data/dqc/zoneDQC.xml");

    // dqc 0.3
    doOne( fac, builder, "file:///C:/dev/thredds/catalog/test/data/dqc/exampleDqc.xml"); //
  } */

}