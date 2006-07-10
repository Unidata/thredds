// $Id: DqcFactory.java,v 1.6 2006/01/17 01:46:51 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.catalog.query;

import thredds.catalog.XMLEntityResolver;

import org.jdom.input.SAXBuilder;
import org.jdom.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Reads an XML document and constructs an QueryCapability object.
 *
 * <h3>Example of normal use:</h3>
 *
 * <pre>
    DqcFactory fac = new DqcFactory(true);
    QueryCapability dqc = fac.readXML(url);
    System.out.println(" dqc hasFatalError= "+dqc.hasFatalError());
    System.out.println(" dqc messages= \n"+dqc.getErrorMessages());
    fac.writeXML(dqc, System.out);
   </pre>
 *
 * Implementation details: Uses JAXP to load an XML Parser and construct a DOM tree.
 * Uses a pluggable "converter" to transform the DOM to the thredds.catalog.query objects.
 *
 * @author John Caron
 * @version $Id: DqcFactory.java,v 1.6 2006/01/17 01:46:51 caron Exp $
 */

public class DqcFactory {
  public static boolean debugURL = false, debugVersion = false;
  public static boolean showParsedXML = false;

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  private SAXBuilder builder; // JAXP parser
  private DqcConvertIF defaultConverter;

  private HashMap versionHash = new HashMap(10);
  private StringBuffer warnMessages, errMessages, fatalMessages;


  /** Constructor.
   * Can use this to read as many catalogs as you want, but should only
   * use in single thread.
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
      Class fac2 = Class.forName("thredds.catalog.parser.jdom.DqcConvert2");
      Object fac2o = fac2.newInstance();
       registerConverter( XMLEntityResolver.DQC_NAMESPACE_02, (DqcConvertIF) fac2o);

      Class fac3 = Class.forName("thredds.catalog.parser.jdom.DqcConvert3");
      Object fac3o = fac3.newInstance();
      defaultConverter = (DqcConvertIF) fac3o;
      registerConverter( XMLEntityResolver.DQC_NAMESPACE_03, (DqcConvertIF) fac3o);

    } catch ( ClassNotFoundException e) {
      throw new RuntimeException("DqcFactory: no implementing class found: "+e.getMessage());
    } catch ( InstantiationException e) {
      throw new RuntimeException("DqcFactory: instantition failed: "+e.getMessage());
    } catch ( IllegalAccessException e) {
      throw new RuntimeException("DqcFactory: access failed: "+e.getMessage());
    }
  }

  private void registerConverter(String namespace, DqcConvertIF converter) {
    versionHash.put( namespace, converter);
    // converter.setCatalogFactory( this);
  }

  public void appendErr( String err) { errMessages.append( err); }
  public void appendFatalErr( String err) { fatalMessages.append( err); }
  public void appendWarning( String err) { warnMessages.append( err); }


  /**
   * Create an QueryCapability from an XML document at a named URL.
   * check dqc.isValid, dqc.getErrorMessages() to see if ok.
   *
   * @param uriString : the URI name that the XML doc is at.
   * @return an QueryCapability object
   */
  public QueryCapability readXML( String uriString) throws IOException, MalformedURLException {
    URI uri = null;
    try {
      uri = new URI( uriString);
    } catch (URISyntaxException e) {
      throw new MalformedURLException(e.getMessage());
    }

    // get ready for XML parsing
    warnMessages.setLength(0);
    errMessages.setLength(0);
    fatalMessages.setLength(0);

    Document doc = null;
    try {
      doc = builder.build(uriString);
    } catch (JDOMException e) {
      fatalMessages.append( e.getMessage()); // makes it invalid
    }
    return readXML( doc, uri);
  }

  /**
   * Create an QueryCapability from an InputStream.
   * check dqc.isValid, dqc.getErrorMessages() to see if ok.
   *
   * @param docIs : the InputStream to read from
   * @return an QueryCapability object
   */
  public QueryCapability readXML( InputStream docIs, URI uri) throws IOException {

    // get ready for XML parsing
    warnMessages.setLength(0);
    errMessages.setLength(0);
    fatalMessages.setLength(0);

    Document doc = null;
    try {
      doc = builder.build(docIs);
    } catch (JDOMException e) {
      fatalMessages.append( e.getMessage()); // makes it invalid
    }

    return readXML( doc, uri);
  }

  /**
   * Create an InvCatalog from an a DOM tree.
   * check dqc.isValid, dqc.getErrorMessages() to see if ok.
   *
   * @param uri : the URI of the document, used for resolving reletive references.
   * @return an InvCatalogImpl object
   */
  public QueryCapability readXML( org.jdom.Document doc, URI uri) throws IOException {

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
    DqcConvertIF fac = (DqcConvertIF) versionHash.get( namespace);
    if (fac == null) {
      fac = defaultConverter; // LOOK
      if (debugVersion) System.out.println("use default converter "+fac.getClass().getName()+"; no namespace "+namespace);
    } else
      if (debugVersion) System.out.println("use converter "+fac.getClass().getName()+" based on namespace "+namespace);

    // convert to object model
    QueryCapability dqc = fac.parseXML( this, doc, uri);
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
   * @param dqc : write this QueryCapability to an XML representation.
   * @return string containing XML representation
   * @throws IOException
   */
  public String writeXML(QueryCapability dqc) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
    writeXML( dqc, os);
    return os.toString();
  }

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param dqc : write this QueryCapability to an XML representation.
   * @param os write to this OutputStream
   * @throws IOException on an error.
   */
  public void writeXML(QueryCapability dqc, OutputStream os) throws IOException {
    defaultConverter.writeXML( dqc, os);
  }

  /**
   * Write the catalog as an XML document to the specified filename.
   *
   * @param dqc : write this QueryCapability to an XML representation.
   * @param filename write to this filename
   * @return true if success
   */
  public boolean writeXML(QueryCapability dqc, String filename) {
    try {
      BufferedOutputStream os = new BufferedOutputStream (new FileOutputStream(filename));
      writeXML( dqc, os);
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /************************************************************************/

  private static void doOne( DqcFactory fac, String url) {
    System.out.println("***read "+url);
    try {
        QueryCapability dqc = fac.readXML(url);
        System.out.println(" dqc hasFatalError= "+dqc.hasFatalError());
        System.out.println(" dqc messages= \n"+dqc.getErrorMessages());
        // fac.writeXML(dqc, System.out);
      } catch (Exception e) {
        e.printStackTrace();
      }

  }

   /** testing */
  public static void main (String[] args) throws Exception {
    DqcFactory fac = new DqcFactory(true);

    // dqc 0.2
    doOne( fac, "file:///C:/dev/thredds/catalog/test/data/dqc/zoneDQC.xml");
    doOne( fac, "file:///C:/dev/thredds/catalog/test/data/dqc/zoneDQCinvalid.xml");

    // dqc 0.3
    // doOne( fac, "file:///C:/dev/thredds/catalog/test/data/dqc/exampleDqc.xml");
    // doOne( fac, "file:///C:/dev/thredds/catalog/test/data/dqc.JplQuikScat.xml");
  }

}

/* Change History:
   $Log: DqcFactory.java,v $
   Revision 1.6  2006/01/17 01:46:51  caron
   use jdom instead of dom everywhere

   Revision 1.5  2005/04/20 00:05:37  caron
   *** empty log message ***

   Revision 1.4  2004/09/24 03:26:29  caron
   merge nj22

   Revision 1.3  2004/06/12 02:01:10  caron
   dqc 0.3

   Revision 1.2  2004/06/09 00:27:27  caron
   version 2.0a release; cleanup javadoc

   Revision 1.1  2004/05/11 23:30:29  caron
   release 2.0a

 */