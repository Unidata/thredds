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

import org.jdom.*;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.net.*;
import java.util.*;

import ucar.nc2.util.IO;
import thredds.util.PathAliasReplacement;
import thredds.catalog.parser.jdom.InvCatalogFactory10;

/**
 * Reads an XML document and constructs thredds.catalog object.
 * <p/>
 * <h3>Example of normal use:</h3>
 * <p/>
 * <pre>
 * InvCatalogFactory factory = new InvCatalogFactory("default", validate);
 * InvCatalogImpl catalog = (InvCatalogImpl) factory.readXML( catalogURI);
 * StringBuilder buff = new StringBuilder();
 * if (!catalog.check( buff)) {
 *   javax.swing.JOptionPane.showMessageDialog(this, "Invalid catalog <"+ catalogURI+">\n"+
 *   buff.toString());
 * }
 * </pre>
 * <p/>
 * <h3>To write out a catalog to XML:</h3>
 * <p/>
 * <pre>
 * // write out catalog to String
 * try {
 *   System.out.println("\Catalog in XML=\n" + factory.writeXML( catalog));
 * } catch (IOException e) {
 *   e.printStackTrace();
 * }
 * <p/>
 * // write out catalog to a file
 * if (!factory.writeXML( catalog, filename))
 * System.out.println("Catalog failed to write to file=" + filename);
 * <p/>
 * // write out catalog to a stream, catch exceptions
 * try {
 * BufferedOutputStream os = new BufferedOutputStream (new FileOutputStream(filename));
 * factory.writeXML( catalog, os);
 * os.close();
 * } catch (IOException e) {
 * e.printStackTrace();
 * }
 * </pre>
 * <p/>
 * <strong>Implementation details: </strong> Uses JDOM to read XML documents.
 * Uses a pluggable InvCatalogConvertIF to transform the JDOM tree to the thredds.catalog objects.
 * The converters are registered based on the namespace used. We are supporting: <ul>
 * <li> the older "0.6" spec, namespace "http://www.unidata.ucar.edu/thredds"
 * <li> the current 1.0/1.1 spec, namespace "http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
 * </ul>
 * The schemas are read from a local resource, see XMLEntityResolver
 *
 * @author John Caron
 */

public class InvCatalogFactory {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvCatalogFactory.class);

  public static boolean debugURL = false, debugOpen = false, debugVersion = false;
  public static boolean showParsedXML = false, showStackTrace = false;

  public static boolean debugXML = false, debugDBurl = false;
  public static boolean debugXMLopen = false, showCatalogXML = false;

  /**
   * Get new Factory for reading and writing catalogs.
   * For multithreading, get seperate InvCatalogFactory for each thread.
   *
   * @param validate : do XML validation or not.
   * @return default factory
   */
  public static InvCatalogFactory getDefaultFactory(boolean validate) {
    return new InvCatalogFactory("default", validate);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * An InvCatalogFactory may have a name, in case you need to manage several of them.
   *
   * @return name of factory
   */
  public String getName() {
    return (this.name);
  }

  private String name;
  private InvCatalogConvertIF defaultConverter;
  private SAXBuilder saxBuilder;
  private StringBuilder warnMessages, errMessages, fatalMessages;

  private Map<String, InvCatalogConvertIF> converters = new HashMap<String, InvCatalogConvertIF>(10);
  private Map<String, MetadataConverterIF> metadataConverters = new HashMap<String, MetadataConverterIF>(10);

  /**
   * Constructor.
   * Can use this to read as many catalogs as you want, but only use in single thread.
   *
   * @param name     : optional name to keep track of factories
   * @param validate : do XML validation or not.
   */
  public InvCatalogFactory(String name, boolean validate) {
    this.name = name;

    XMLEntityResolver xml = new XMLEntityResolver(validate);
    saxBuilder = xml.getSAXBuilder();
    warnMessages = xml.getWarningMessages();
    errMessages = xml.getErrorMessages();
    fatalMessages = xml.getFatalMessages();

    setDefaults();
  }

  private List<PathAliasReplacement> dataRootLocAliasExpanders = Collections.emptyList();

  public void setDataRootLocationAliasExpanders( List<PathAliasReplacement> dataRootLocAliasExpanders )
  {
    if ( dataRootLocAliasExpanders == null )
      this.dataRootLocAliasExpanders = Collections.emptyList();
    else
      this.dataRootLocAliasExpanders = new ArrayList<PathAliasReplacement>( dataRootLocAliasExpanders );

    for ( InvCatalogConvertIF catConv : this.converters.values())
    {
      // LOOK! ToDo Should be more generic (add setter to InvCatalogConvertIF?)
      if ( catConv instanceof InvCatalogFactory10 )
        ((InvCatalogFactory10) catConv).setDataRootLocationAliasExpanders( this.dataRootLocAliasExpanders );
    }
  }

  public List<PathAliasReplacement> getDataRootLocationAliasExpanders()
  {
    return Collections.unmodifiableList( this.dataRootLocAliasExpanders );
  }

  private void setDefaults() {
    try {
      /* Class fac6 = Class.forName("thredds.catalog.parser.jdom.InvCatalogFactory6");
      Object fac6o = fac6.newInstance();
      registerCatalogConverter(XMLEntityResolver.CATALOG_NAMESPACE_06, (InvCatalogConvertIF) fac6o);  */

      Class fac1 = Class.forName("thredds.catalog.parser.jdom.InvCatalogFactory10");
      Object fac1o = fac1.newInstance();
      defaultConverter = (InvCatalogConvertIF) fac1o;
      registerCatalogConverter(XMLEntityResolver.CATALOG_NAMESPACE_10, (InvCatalogConvertIF) fac1o);

      // registerMetadataConverter( JaxpFactory.CATALOG_NAMESPACE_10, (MetadataConverterIF) fac1o);
      // registerMetadataConverter( MetadataType.THREDDS, new ThreddsMetadata.Parser());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("InvCatalogFactory: no implementing class found: " + e.getMessage());
    } catch (InstantiationException e) {
      throw new RuntimeException("InvCatalogFactory: instantition failed: " + e.getMessage());
    } catch (IllegalAccessException e) {
      throw new RuntimeException("InvCatalogFactory: access failed: " + e.getMessage());
    }
  }

  /**
   * Register converters for creating InvCatalogs from specific catalog XML namespaces.
   * This allows the user to add to or override the way catalogs are made.
   *
   * @param namespace : namespace of catalog; acts as the version
   * @param converter : use this factory for this version
   */
  public void registerCatalogConverter(String namespace, InvCatalogConvertIF converter) {
    converters.put(namespace, converter);
  }

  /**
   * Find the InvCatalogConvertIF registered for this namespace.
   * @param namespace : find InvCatalogConvertIF for this namespace
   * @return the InvCatalogConvertIF registered for this namespace, or null
   */
  public InvCatalogConvertIF getCatalogConverter(String namespace) {
    return converters.get(namespace);
  }

  /**
   * Find the InvCatalogConvertIF registered for this namespace, and set it into the catalog.
   * @param cat set InvCatalogConvertIF on this catalog
   * @param namespace find InvCatalogConvertIF for this namespace
   */
  public void setCatalogConverter(InvCatalogImpl cat, String namespace) {
    cat.setCatalogConverter(getCatalogConverter(namespace));
  }

  /**
   * Register metadata converters for reading metadata objects of a certain type or namespace.
   * This allows allows extensible metadata processing.
   *
   * @param key       : namespace or metadata type string
   * @param converter : use this MetadataConverterIF for the given key
   * @see InvMetadata
   */
  public void registerMetadataConverter(String key, MetadataConverterIF converter) {
    metadataConverters.put(key, converter);
  }

  /**
   * This allows the possibility of reading a catalog in another thread. The default
   * implementation does not do that, but a subclass may override and implement.
   * If the catalog is read successfully, it is passed on to the callback.
   *
   * @param uriString : read this catalog.
   * @param callback  : call this if successfully read.
   * @see CatalogSetCallback
   */
  public void readXMLasynch(String uriString, CatalogSetCallback callback) {
    InvCatalogImpl cat = readXML(uriString);
    callback.setCatalog(cat);
  }

  /**
   * Create an InvCatalog from an XML document at a named URL.
   * Failures and exceptions are handled
   * by causing validate() to fail. Therefore, be sure to call validate() before trying
   * to use the InvCatalog object.
   *
   * @param uriString : the URI name that the XML doc is at.
   * @return an InvCatalogImpl object
   */
  public InvCatalogImpl readXML(String uriString) {
    URI uri;
    try {
      uri = new URI(uriString);
    } catch (URISyntaxException e) {
      InvCatalogImpl cat = new InvCatalogImpl(uriString, null, null);
      cat.appendErrorMessage("**Fatal:  InvCatalogFactory.readXML URISyntaxException on URL (" +
          uriString + ") " + e.getMessage() + "\n", true);
      return cat;
    }

    return readXML(uri);
  }

  /**
   * Read an InvCatalog from an a URI.
   * Failures and exceptions are handled
   * by causing validate() to fail. Therefore, be sure to call validate() before trying
   * to use the InvCatalog object.
   *
   * @param uri : the URI of the document, used for resolving reletive references.
   * @return an InvCatalogImpl object
   */
  public InvCatalogImpl readXML(URI uri) {
    // get ready for XML parsing
    warnMessages.setLength(0);
    errMessages.setLength(0);
    fatalMessages.setLength(0);

    org.jdom.Document jdomDoc;
    InputStream is = null;
    try {
      jdomDoc = saxBuilder.build(uri.toURL());
//      HttpUriResolver httpUriResolver = HttpUriResolver.newDefaultUriResolver();
//      String s = httpUriResolver.getString( uri );
//      //StringReader
//      is = new BufferedInputStream( httpUriResolver.getInputStream( uri ), 1000000 );
//      jdomDoc = saxBuilder.build( is );
    } catch (Exception e) {
      InvCatalogImpl cat = new InvCatalogImpl(uri.toString(), null, null);
      cat.appendErrorMessage("**Fatal:  InvCatalogFactory.readXML failed"
          + "\n Exception= " + e.getClass().getName() + " " + e.getMessage()
          + "\n fatalMessages= " + fatalMessages.toString()
          + "\n errMessages= " + errMessages.toString()
          + "\n warnMessages= " + warnMessages.toString() + "\n", true);
      return cat;
    }
    finally
    {
      if ( is != null) try
      {
        is.close();
      }
      catch ( IOException e )
      {
        log.warn( "Failed to close input stream [" + uri.toString() + "]." );
      }
    }

    if (fatalMessages.length() > 0) {
      InvCatalogImpl cat = new InvCatalogImpl(uri.toString(), null, null);
      cat.appendErrorMessage("**Fatal:  InvCatalogFactory.readXML XML Fatal error(s) =\n" +
          fatalMessages.toString() + "\n", true);
      return cat;
    }

    return readXML(jdomDoc, uri);
  }

  /**
   * Create an InvCatalog by reading catalog XML from a String.
   *
   * Failures and exceptions are handled by causing validate() to
   * fail. Therefore, be sure to call validate() before trying to use
   * the InvCatalog object.
   *
   * @param catAsString : the String from which to read the catalog.
   * @param baseUri : the base URI of the document, used for resolving reletive references.
   * @return an InvCatalogImpl object
   */
  public InvCatalogImpl readXML( String catAsString, URI baseUri )
  {
    return readXML( new StringReader( catAsString ), baseUri );
  }

  /**
   * Create an InvCatalog by reading catalog XML from a StringReader.
   *
   * Failures and exceptions are handled by causing validate() to
   * fail. Therefore, be sure to call validate() before trying to use
   * the InvCatalog object.
   *
   * @param catAsStringReader : the StreamReader from which to read the catalog.
   * @param baseUri : the base URI of the document, used for resolving reletive references.
   * @return an InvCatalogImpl object
   */
  public InvCatalogImpl readXML( StringReader catAsStringReader, URI baseUri )
  {
    XMLEntityResolver resolver = new XMLEntityResolver( false );
    SAXBuilder builder = resolver.getSAXBuilder();

    Document inDoc;
    try
    {
      inDoc = builder.build( catAsStringReader );
    }
    catch ( Exception e )
    {
      InvCatalogImpl cat = new InvCatalogImpl( baseUri.toString(), null, null );
      cat.appendErrorMessage( "**Fatal:  InvCatalogFactory.readXML(String catAsString, URI uri) failed:"
                              + "\n  Exception= " + e.getClass().getName() + " " + e.getMessage()
                              + "\n  fatalMessages= " + fatalMessages.toString()
                              + "\n  errMessages= " + errMessages.toString()
                              + "\n  warnMessages= " + warnMessages.toString() + "\n", true );
      return cat;
    }

    return readXML( inDoc, baseUri );
  }

  /**
   * Create an InvCatalog from an InputStream.
   * Failures and exceptions are handled
   * by causing validate() to fail. Therefore, be sure to call validate() before trying
   * to use the InvCatalog object.
   *
   * @param docIs : the InputStream to read from
   * @param uri : the URI of the document, used for resolving reletive references.
   * @return an InvCatalogImpl object
   */
  public InvCatalogImpl readXML(InputStream docIs, URI uri) {

    // get ready for XML parsing
    warnMessages.setLength(0);
    errMessages.setLength(0);
    fatalMessages.setLength(0);

    org.jdom.Document jdomDoc;
    try {
      jdomDoc = saxBuilder.build(docIs);
    } catch (Exception e) {
      InvCatalogImpl cat = new InvCatalogImpl(uri.toString(), null, uri);
      cat.appendErrorMessage("**Fatal:  InvCatalogFactory.readXML failed"
          + "\n Exception= " + e.getClass().getName() + " " + e.getMessage()
          + "\n fatalMessages= " + fatalMessages.toString()
          + "\n errMessages= " + errMessages.toString()
          + "\n warnMessages= " + warnMessages.toString() + "\n", true);
      return cat;
    }

    if (fatalMessages.length() > 0) {
      InvCatalogImpl cat = new InvCatalogImpl(uri.toString(), null, uri);
      cat.appendErrorMessage("**Fatal:  InvCatalogFactory.readXML XML Fatal error(s) =\n" +
          fatalMessages.toString() + "\n", true);
      return cat;
    }

    return readXML(jdomDoc, uri);
  }

  /**
   * Create an InvCatalog from a JDOM document.
   * Failures and exceptions are handled
   * by causing validate() to fail. Therefore, be sure to call validate() before trying
   * to use the InvCatalog object.
   *
   * @param jdomDoc a parsed JDOM Document
   * @param uri : the URI of the document, used for resolving reletive references.
   * @return an InvCatalogImpl object
   */
  public InvCatalogImpl readXML(org.jdom.Document jdomDoc, URI uri) {

    // decide on converter based on namespace
    Element root = jdomDoc.getRootElement();
    if (!root.getName().equalsIgnoreCase("catalog")) {
      throw new IllegalArgumentException("not a catalog");
    }
    String namespace = root.getNamespaceURI();
    InvCatalogConvertIF fac = converters.get(namespace);
    if (fac == null) {
      fac = defaultConverter; // LOOK
      if (debugVersion)
        System.out.println("use default converter " + fac.getClass().getName() + "; no namespace " + namespace);
    } else if (debugVersion)
      System.out.println("use converter " + fac.getClass().getName() + " based on namespace " + namespace);


    InvCatalogImpl cat = fac.parseXML(this, jdomDoc, uri);
    cat.setCreateFrom(uri.toString());
    cat.setCatalogFactory(this);
    cat.setCatalogConverter(fac);
    cat.finish();

    if (showCatalogXML) {
      System.out.println("*** catalog/showCatalogXML");
      try {
        writeXML(cat, System.out);
      }
      catch (IOException ex) {
        log.warn("Error writing catalog for debugging", ex);
      }
    }

    if (fatalMessages.length() > 0)
      cat.appendErrorMessage(fatalMessages.toString(), true); // makes it invalid
    if (errMessages.length() > 0)
      cat.appendErrorMessage(errMessages.toString(), false); // doesnt make it invalid
    if (warnMessages.length() > 0)
      cat.appendErrorMessage(warnMessages.toString(), false); // doesnt make it invalid
    return cat;
  }

  /* public org.w3c.dom.Element readOtherXML( URI uri) {

    Document doc;
    try {
      doc = builder.parse(uri.toString());
    } catch (Exception e) {
      errMessages.append( "**Error:  InvCatalogFactory.readOtherXML failed on "+ uri+
        "\n Exception= "+e.getClass().getName()+" "+e.getMessage()+"\n");
      return null;
    }

    return doc.getDocumentElement();
  } */

  /**
   * Write the catalog as an XML document to a String.
   *
   * @param catalog write this catalog
   * @return string containing XML representation
   * @throws IOException on write error
   */
  public String writeXML(InvCatalogImpl catalog) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
    writeXML(catalog, os);
    return os.toString();
  }

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param catalog write this catalog
   * @param os      write to this OutputStream
   * @throws IOException on an error.
   */
  public void writeXML(InvCatalogImpl catalog, OutputStream os) throws IOException {
    InvCatalogConvertIF fac = catalog.getCatalogConverter();
    if (fac == null) fac = defaultConverter;
    fac.writeXML(catalog, os);
  }

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param catalog write this catalog
   * @param os      write to this OutputStream
   * @param raw set true for "server side" catalogs, false is default, shows "client side" catalogs
   * @throws IOException on an error.
   */
  public void writeXML(InvCatalogImpl catalog, OutputStream os, boolean raw) throws IOException {
    InvCatalogConvertIF fac = catalog.getCatalogConverter();
    if (fac == null) fac = defaultConverter;
    fac.writeXML(catalog, os, raw);
  }

  /**
   * Write the catalog as an XML document to the specified filename.
   *
   * @param catalog  write this catalog
   * @param filename write to this filename
   * @throws IOException on an error.
   */
  public void writeXML(InvCatalogImpl catalog, String filename) throws IOException {
    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(filename));
    writeXML(catalog, os);
    os.close();
  }

  /**
   * Write the InvCatalogImpl as a InvCatalog 1.0 XML document to a String.
   *
   * @param catalog - the catalog to be written
   * @return a String containing the XML representation
   * @throws IOException           when the OutputStream can't be written
   * @throws IllegalStateException when the factory doesn't know how to write a 1.0 document.
   */
  public String writeXML_1_0(InvCatalogImpl catalog)
      throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
    writeXML_1_0(catalog, os);
    return os.toString();
  }

  /**
   * Write the InvCatalogImpl as a InvCatalog 0.6 XML document to a String.
   *
   * @param catalog - the catalog to be written
   * @return a String containing the XML representation
   * @throws IOException           when the OutputStream can't be written
   * @throws IllegalStateException when the factory doesn't know how to write a 0.6 document.
   */
  public String writeXML_0_6(InvCatalogImpl catalog)
      throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
    writeXML_0_6(catalog, os);
    return os.toString();
  }

  /**
   * Write the InvCatalogImpl to the OutputStream as a InvCatalog 1.0 document.
   *
   * @param catalog - the catalog to be written
   * @param os      - the OutputStream to write to
   * @throws IOException           when the OutputStream can't be written
   * @throws IllegalStateException when the factory doesn't know how to write a 1.0 document.
   */
  public void writeXML_1_0(InvCatalogImpl catalog, OutputStream os)
      throws IOException {
    this.writeXML_ver(XMLEntityResolver.CATALOG_NAMESPACE_10, catalog, os);
  }

  /**
   * Write the InvCatalogImpl to the OutputStream as a InvCatalog 0.6 document.
   *
   * @param catalog - the catalog to be written
   * @param os      - the OutputStream to write to
   * @throws IOException           when the OutputStream can't be written
   * @throws IllegalStateException when the factory doesn't know how to write a 0.6 document.
   */
  public void writeXML_0_6(InvCatalogImpl catalog, OutputStream os)
      throws IOException {
    this.writeXML_ver(XMLEntityResolver.CATALOG_NAMESPACE_06, catalog, os);
  }

  /**
   * Write an InvCatalogImpl to an OutputStream as an InvCatalog document using the given namespace.
   *
   * @param namespace - the namespace of the version of InvCatalog document to be written
   * @param catalog   - the catalog to be written
   * @param os        - the OutputStream to write to
   * @throws IOException           when the OutputStream can't be written
   * @throws IllegalStateException when the factory doesn't know how to write the version of document requested.
   */
  private void writeXML_ver(String namespace, InvCatalogImpl catalog, OutputStream os)
      throws IOException {
    InvCatalogConvertIF converter = this.getCatalogConverter(namespace);
    if (converter == null) {
      String tmpMsg = "This Factory <" + this.getName() + "> does not have a converter for the requested namespace <" + namespace + ">.";
      throw new IllegalStateException(tmpMsg);
    }
    converter.writeXML(catalog, os);
  }

  /**
   * append an error message. Used by the InvCatalogConvertIF
   * @param err append this error message
   */
  public void appendErr(String err) {
    errMessages.append(err);
  }

  /**
   * append a fatal error message
   * @param err append this error message
   */
  public void appendFatalErr(String err) {
    fatalMessages.append(err);
  }

  /**
   * append a warning message
   * @param err append this error message
   */
  public void appendWarning(String err) {
    warnMessages.append(err);
  }

  /**
   * Find the MetadataConverterIF registered for this key
   * @param key search on this key
   * @return  MetadataConverterIF else null
   */
  public MetadataConverterIF getMetadataConverter(String key) {
    if (key == null) return null;
    return metadataConverters.get(key);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  // testing

  private static InvCatalogImpl doOne(InvCatalogFactory fac, String urlString, boolean show) {
    System.out.println("***read " + urlString);
    if (show) System.out.println(" original catalog=\n" + IO.readURLcontents(urlString));
    try {
      InvCatalogImpl cat = fac.readXML(new URI(urlString));
      StringBuilder buff = new StringBuilder();
      boolean isValid = cat.check(buff, false);
      System.out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");
      System.out.println(" validation output=\n" + buff);
      if (show) System.out.println(" parsed catalog=\n" + fac.writeXML(cat));
      //System.out.println(" -----\n"+cat.dump());
      return cat;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

  }

  /**
   * testing
   */
  public static void main(String[] args) throws Exception {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(false);
    //doOne(catFactory, "http://www.unidata.ucar.edu/georesources/idvcatalog.xml", true);
    doOne(catFactory, "file:C:/data/work/maurer/atm_mod.xml", true);
    //Thread.currentThread().sleep(10 * 10000);
    //InvCatalogFactory catFactoryNo = InvCatalogFactory.getDefaultFactory(false);

    // 0.6
    //doOne(catFactory, "http://motherlode.ucar.edu:8088/thredds/casestudy/vgeeCatalog.0.6.xml");
    /* doOne(catFactory, "file:///C:/dev/thredds/catalog/test/data/ParseFails.xml");
    doOne(catFactory, "file:///C:/dev/thredds/catalog/test/data/malFormed.xml"); // */
    //doOne(catFactory, "file:///C:/dev/thredds/server/resources/initialContent/catalog.xml");

    // 1.0
    //doOne(catFactory, "file:///C:/dev/thredds/catalog/test/data/Example1.0.xml");
    /* doOne(catFactory, "file:///C:/dev/thredds/catalog/test/data/TestInherit.1.0.xml"); // */
    //doOne(catFactory, "file:///C:/dev/thredds/catalog/test/data/catalogDev.xml");
    //doOne(catFactory, "http://motherlode.ucar.edu:8088/thredds/catalog.xml"); // */
    //doOne(catFactoryNo, "http://motherlode.ucar.edu:8088/thredds/catalog.xml"); // */

    // 1.1
    //doOne(catFactory, "file:C:/data/catalog/obsData.xml", false);
    /* InvCatalogImpl cat = doOne(catFactory, "file:///C:/dev/thredds/resourceswar/initialContent/dodsC/catalog3.xml");
InvDatasetScan dsScan = (InvDatasetScan) cat.findDatasetByID("testScan");
InvCatalogImpl catScan = dsScan.makeCatalogForDirectory("reqURL", "model/test/", "serviceId", "latestServiceId");
System.out.println(" scanned catalog=\n" + catFactory.writeXML(catScan)); */

    //doOne(catFactory, "http://whoopee:8080/thredds/dodsC/model/test/catalog.xml");

    // catgen
    //catFactory.registerMetadataConverter( MetadataType.CATALOG_GEN_CONFIG.toString(),
    //                                         new CatGenConfigMetadataFactory());
    //doOne(catFactory, "file:///C:/dev/thredds/test/data/thredds/cataloggen/test.xml");
    // doOne(catFactory, "file:///C:/dev/thredds/test/data/testScan.xml");
  }

}

/*
 * <h3>Default catalog parsing</h3>
 *
 * InvCatalogFactory allows parsers that handle different catalog versions to be registered
 *  by the application. These factories may also use different XML parsers. Usually you call
 *  InvCatalogFactory.getDefaultFactory(), which already has the default factories for catalog
 *  versions. The default factories validate the catalog XML, and require jdom.jar and a validating
 *  parser like the one that ships with jdk1.4.
 *
 * <h3>Nonvalidating catalog parsing</h3>
 *  To use a nonvalidating, small parser that does not need jdom.jar or any external parser do the
 *  following before you make any other InvCatalogFactory calls:
 * <pre>
    InvCatalogFactory myFactory = new InvCatalogFactory();
    myFactory.registerCatalogFactory("0.6", new thredds.catalog.parser.nano.Catalog6());
    InvCatalogFactory.setDefaultFactory( myFactory);
 * </pre>
 *
 * <h3>Custom catalog parsing</h3>
 *  You may also instantiate an InvCatalogFactory and register your
 *  own factories, for example:
 * <pre>
    InvCatalogFactory myFactory = new InvCatalogFactory();
    myFactory.registerCatalogFactory("0.6", new MyInvCatalogFactory());
    InvCatalogFactory.setDefaultFactory( myFactory);
   </pre>
 *
 * <h3>Custom metadata parsing</h3>
 * InvCatalogFactory also allows you to register parsers for specialized metadata elements that
 * appear in the catalog.xml. These parsers are called "metadata factories" and must implement
 * the InvMetadataFactoryIF interface.
 * Example:
 * <pre>
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory();
    catFactory.registerMetadataFactory(MetadataType.WMS, new myWMSFactory());
  </pre>
 * In this example, any metadata element of type MetadataType.WMS will be passed
 * to your factory when the catalog is parsed. myWMSFactory will turn it into an
 * Object, which will be available through the InvMetadata.getContentObject() call.

 */