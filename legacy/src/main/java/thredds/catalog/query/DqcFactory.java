/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: DqcFactory.java 48 2006-07-12 16:15:40Z caron $


package thredds.catalog.query;

import thredds.catalog.XMLEntityResolver;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.IO;

import org.jdom2.input.SAXBuilder;
import org.jdom2.*;

import java.io.*;
import java.net.*;
import java.util.*;

import ucar.nc2.util.DiskCache2;

/**
 * Reads an XML document and constructs an QueryCapability object.
 * <p/>
 * <h3>Example of normal use:</h3>
 * <p/>
 * <pre>
 * DqcFactory fac = new DqcFactory(true);
 * QueryCapability dqc = fac.readXML(url);
 * System.out.println(" dqc hasFatalError= "+dqc.hasFatalError());
 * System.out.println(" dqc messages= \n"+dqc.getErrorMessages());
 * fac.writeXML(dqc, System.out);
 * </pre>
 * <p/>
 * Implementation details: Uses JAXP to load an XML Parser and construct a DOM tree.
 * Uses a pluggable "converter" to transform the DOM to the thredds.catalog.query objects.
 *
 * @author John Caron
 */

public class DqcFactory {
  public static boolean debugVersion = false;
  public static boolean showParsedXML = false;

  static private DiskCache2 diskCache = null;
  static private int buffer_size = 64000;

  static public void setPersistenceCache(DiskCache2 dc) {
    diskCache = dc;
  }
  ////////////////////////////////////////////////////////////////////////////////////////////////////

  private SAXBuilder builder; // JAXP parser
  private DqcConvertIF defaultConverter;

  private HashMap<String, String> versionToNamespaceHash = new HashMap<>(10);
  private HashMap<String, DqcConvertIF> namespaceToDqcConverterHash = new HashMap<>(10);
  private StringBuilder warnMessages, errMessages, fatalMessages;


  /**
   * Constructor.
   * Can use this to read as many catalogs as you want, but should only
   * use in single thread.
   *
   * @param validate : do XML validation or not.
   */
  public DqcFactory(boolean validate) {
    XMLEntityResolver jaxp = new XMLEntityResolver(validate);
    builder = jaxp.getSAXBuilder();
    warnMessages = jaxp.getWarningMessages();
    errMessages = jaxp.getErrorMessages();
    fatalMessages = jaxp.getFatalMessages();
    setDefaults();
  }

  private void setDefaults() {
    try {
      /* Class fac2 = Class.forName("thredds.catalog.parser.jdom.DqcConvert2");
      Object fac2o = fac2.newInstance();
      registerConverter( "0.2", XMLEntityResolver.DQC_NAMESPACE_02, (DqcConvertIF) fac2o);

      Class fac3 = Class.forName("thredds.catalog.parser.jdom.DqcConvert3");
      Object fac3o = fac3.newInstance();
      registerConverter( "0.3", XMLEntityResolver.DQC_NAMESPACE_03, (DqcConvertIF) fac3o); */

      Class fac4 = Class.forName("thredds.catalog.parser.jdom.DqcConvert4");
      Object fac4o = fac4.newInstance();
      defaultConverter = (DqcConvertIF) fac4o;
      registerConverter( "0.4", XMLEntityResolver.DQC_NAMESPACE_04, (DqcConvertIF) fac4o);

    } catch (ClassNotFoundException e) {
      throw new RuntimeException("DqcFactory: no implementing class found: " + e.getMessage());
    } catch (InstantiationException e) {
      throw new RuntimeException("DqcFactory: instantiation failed: " + e.getMessage());
    } catch (IllegalAccessException e) {
      throw new RuntimeException("DqcFactory: access failed: " + e.getMessage());
    }
  }

  private void registerConverter(String version, String namespace, DqcConvertIF converter) {
    namespaceToDqcConverterHash.put(namespace, converter);
    versionToNamespaceHash.put(version, namespace);
    // converter.setCatalogFactory( this);
  }

  public void appendErr(String err) {
    errMessages.append(err);
  }

  public void appendFatalErr(String err) {
    fatalMessages.append(err);
  }

  public void appendWarning(String err) {
    warnMessages.append(err);
  }

  public QueryCapability readXML( String docAsString, URI uri ) throws IOException
  {
    // get ready for XML parsing
    warnMessages.setLength( 0 );
    errMessages.setLength( 0 );
    fatalMessages.setLength( 0 );

    Document doc = null;
    try
    {
      doc = builder.build( new StringReader( docAsString ) );
    }
    catch ( JDOMException e )
    {
      fatalMessages.append( e.getMessage() ); // makes it invalid
    }

    return readXML( doc, uri );
  }

  /**
   * Create an QueryCapability from an XML document at a named URL.
   * check dqc.isValid, dqc.getErrorMessages() to see if ok.
   * If Disk caching is set, cache the dqc and check IfModifiedSince.
   *
   * @param uriString : the URI name that the XML doc is at.
   * @return an QueryCapability object
   * @throws IOException on failure
   */
  public QueryCapability readXML(String uriString) throws IOException {

    // get URI
    URI uri;
    try {
      uri = new URI(uriString);
    } catch (URISyntaxException e) {
      throw new MalformedURLException(e.getMessage());
    }

    // check if its cached
    if (diskCache != null) {
      File file = diskCache.getCacheFile(uriString);
      if (file != null) {
        HttpURLConnection conn = null;

        try {
          URL url = uri.toURL();
          conn = (HttpURLConnection) url.openConnection();
          conn.setRequestMethod("GET");
          conn.setIfModifiedSince(file.lastModified());

          int code = conn.getResponseCode();
          if (code == HttpURLConnection.HTTP_OK) {
            java.io.InputStream is = conn.getInputStream();
            if (is != null) {
              try (FileOutputStream fout = new FileOutputStream(file)) {
                  IO.copyB(is, fout, buffer_size);  // cache it
              }
              try (InputStream fin = new BufferedInputStream( new FileInputStream(file), 50000)) {
                return readXML(fin, uri);
              }
            }

          } else {
            // use file
            try (FileInputStream fin = new FileInputStream(file)) {
              return readXML(fin, uri);
            }
          }

        } finally {

          if (conn != null)
            conn.disconnect();
        }

      } // has file

      // no file - read and cache
      IO.readURLtoFileWithExceptions(uriString, file, buffer_size);
      try (InputStream fin = new BufferedInputStream(
              new FileInputStream(file), 50000)) {
        return readXML(fin, uri);
      }

    } // has diskCache

    // otherwise just open the URL
    warnMessages.setLength(0);
    errMessages.setLength(0);
    fatalMessages.setLength(0);

    Document doc = null;
    try {
      doc = builder.build(uriString);
    } catch (JDOMException e) {
      fatalMessages.append(e.getMessage()); // makes it invalid
    }

    return readXML(doc, uri);
  }

  /**
   * Create an QueryCapability from an InputStream.
   * check dqc.isValid, dqc.getErrorMessages() to see if ok.
   *
   * @param docIs : the InputStream to read from
   * @param uri   : the URI of the document, used for resolving reletive references.
   * @return an QueryCapability object
   * @throws IOException on failure
   */
  public QueryCapability readXML(InputStream docIs, URI uri) throws IOException {

    // get ready for XML parsing
    warnMessages.setLength(0);
    errMessages.setLength(0);
    fatalMessages.setLength(0);

    Document doc = null;
    try {
      doc = builder.build(docIs);
    } catch (JDOMException e) {
      fatalMessages.append(e.getMessage()); // makes it invalid
    }

    return readXML(doc, uri);
  }

  /**
   * Create an InvCatalog from an a JDOM document.
   * check dqc.isValid, dqc.getErrorMessages() to see if ok.
   *
   * @param doc parse this document
   * @param uri : the URI of the document, used for resolving reletive references.
   * @return an InvCatalogImpl object
   * @throws IOException on failure
   */
  private QueryCapability readXML(org.jdom2.Document doc, URI uri) throws IOException {

    if (doc == null) { // parse failed
      QueryCapability dqc = new QueryCapability();
      if (fatalMessages.length() > 0)
        dqc.appendErrorMessage(fatalMessages.toString(), true); // makes it invalid
      if (errMessages.length() > 0)
        dqc.appendErrorMessage(errMessages.toString(), false); // doesnt make it invalid
      if (errMessages.length() > 0)
        dqc.appendErrorMessage(warnMessages.toString(), false); // doesnt make it invalid
      return dqc;
    }

    // decide on converter based on namespace
    Element root = doc.getRootElement();
    String namespace = root.getNamespaceURI();
    DqcConvertIF fac = namespaceToDqcConverterHash.get(namespace);
    if (fac == null) {
      fac = defaultConverter; // LOOK
      if (debugVersion)
        System.out.println("use default converter " + fac.getClass().getName() + "; no namespace " + namespace);
    } else if (debugVersion)
      System.out.println("use converter " + fac.getClass().getName() + " based on namespace " + namespace);

// convert to object model
    QueryCapability dqc = fac.parseXML(this, doc, uri);
    if (fatalMessages.length() > 0)
      dqc.appendErrorMessage(fatalMessages.toString(), true); // makes it invalid
    if (errMessages.length() > 0)
      dqc.appendErrorMessage(errMessages.toString(), false); // doesnt make it invalid
    if (errMessages.length() > 0)
      dqc.appendErrorMessage(warnMessages.toString(), false); // doesnt make it invalid
    return dqc;
  }

  /**
   * Write the catalog as an XML document to a String.
   *
   * @param dqc : write this QueryCapability to an XML representation.
   * @return string containing XML representation
   * @throws IOException on failure
   */
  public String writeXML(QueryCapability dqc) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
    writeXML(dqc, os);
    return os.toString(CDM.utf8Charset.name());
  }

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param dqc : write this QueryCapability to an XML representation.
   * @param os  write to this OutputStream
   * @throws IOException on an error.
   */
  public void writeXML(QueryCapability dqc, OutputStream os) throws IOException {
    String ns = versionToNamespaceHash.get( dqc.getVersion() );
    DqcConvertIF fac = namespaceToDqcConverterHash.get( ns );

    if ( fac == null )
      fac = defaultConverter;

    fac.writeXML( dqc, os );
  }

  /**
   * Write the catalog as an XML document to the specified filename.
   *
   * @param dqc      : write this QueryCapability to an XML representation.
   * @param filename write to this filename
   * @return true if success
   */
  public boolean writeXML(QueryCapability dqc, String filename) {
    try {
      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(filename));
      writeXML(dqc, os);
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

/************************************************************************/
  /**
   * testing
   */

  private static void doOne(DqcFactory fac, String url) {
    System.out.println("***read " + url);
    try {
      QueryCapability dqc = fac.readXML(url);
      System.out.println(" dqc hasFatalError= " + dqc.hasFatalError());
      System.out.println(" dqc messages= \n" + dqc.getErrorMessages());
      fac.writeXML(dqc, System.out);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public static void main(String[] args) throws Exception {
    DqcFactory fac = new DqcFactory(true);

    doOne(fac, "file:///C:/data/dqc/metarDQC.xml");
  }

}