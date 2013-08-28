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

package ucar.util.prefs;

import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.beans.ExceptionListener;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.*;
import ucar.nc2.util.IO;
import ucar.nc2.util.Indent;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import java.io.*;
import java.util.*;

/**
 *  This implements an XML-based backing store for PreferencesExt.
 *  XMLStores can be chained together to provide independent collections of
 * "stored defaults", eg store1 -> store2 -> store3. In this case, a call to getXXX( key)
 * first looks in store1, and if not found, then in store2, etc. A call to
 * putXXX(key, value) first checks to see if the (key, value) pair already exists in any of
 * the chained stores; if not, then it is added to store1.
 * Normally, only store1 would be changed and saved.
 *
 * <p> A chain of stored defaults might look like:
 * <pre>
 * try {
 *   XMLStore store3 = XMLStore.createFromResource("/auxdata/system.xml", null);
 *   XMLStore store2 = XMLStore.createFromFile("/usr/local/metapps/GDV/site.xml", store3);
 *   XMLStore store1 = XMLStore.createFromFile("/home/username/.GDV/user.xml", store2);
 *   PreferencesExt prefs = store1.getPreferences();
 * } catch (IOException e) {
 *   System.out.println("XMLStore Creation failed "+e);
 * }
 * </pre>
 *
 * If you plan to call Preferences.userRoot(), you must explicilty set it, eg:
 * <pre>
 *   PreferencesExt.setUserRoot( prefs);
 * </pre>
 *
 * <p> Before exiting the application, in order to save changes, you must call:
 * <pre>
 * try {
 *   store1.save();
 * } catch (IOException e) {
 *   System.out.println("XMLStore Save failed "+e);
 * }
 *  </pre>
 *
 *
 * @see ucar.util.prefs.PreferencesExt
 * @see java.util.prefs.Preferences
 * @author John Caron
 */

public class XMLStore {

  /**
   * Create an XMLStore reading from the specified filename.
   *
   * @param fileName The XMLStore is stored in this files.
   * @param storedDefaults This contains the "stored defaults", or null if none.
   * @return new XMLStore object
   * @throws java.io.IOException on error
   */
  static public XMLStore createFromFile(String fileName, XMLStore storedDefaults) throws java.io.IOException {
    File prefsFile = new File(fileName);

    // open file if it exists
    InputStream primIS = null, objIS = null;
    if (prefsFile.exists()) {
      primIS = new BufferedInputStream(new FileInputStream( prefsFile));
      objIS = new BufferedInputStream(new FileInputStream( prefsFile));
    }

    if (debugWhichStore) System.out.println("XMLStore read from file "+fileName);
    XMLStore store = new XMLStore( primIS, objIS, storedDefaults);
    store.prefsFile = prefsFile;
    return store;
  }

  /**
   * Create an XMLStore reading from an input stream. Because of some peculiariteis, you must open the
   *  input stream wtice, and pass both in.
   *
   * @param is1 the first copy of the input stream.
   * @param is2 the second copy of the input stream.
   * @param storedDefaults This contains the "stored defaults", or null if none.
   * @return new XMLStore object
   * @throws java.io.IOException on error
   */
  static public XMLStore createFromInputStream(InputStream is1, InputStream is2, XMLStore storedDefaults) throws java.io.IOException {
    if (debugWhichStore) System.out.println("XMLStore read from input stream "+is1);
    return new XMLStore( is1, is2, storedDefaults);
  }

  /**
   * Create a read-only XMLStore reading from the specified resource, opened as a Resource stream using the
   * XMLStore ClassLoader. This allows you to find files that are in jar files on
   * the application CLASSPATH.
   *
   * @param resourceName The XMLStore is stored in this resource. By convention it has .xml suffix.
   * @param storedDefaults This contains the "stored defaults", or null if none.
   * @throws java.io.IOException if Resource not found or error reading it
   * @return new XMLStore object
   */
  static public XMLStore createFromResource(String resourceName, XMLStore storedDefaults)
         throws java.io.IOException {

    // open files if exist
    Class c = XMLStore.class;
    InputStream primIS = c.getResourceAsStream(resourceName);
    InputStream objIS = c.getResourceAsStream(resourceName);

    // debug
//    InputStream debugIS = c.getResourceAsStream(fileName);
  //  System.out.println("Resource stream= "+fileName);
    //thredds.util.IO.copy(debugIS, System.out);

    if (primIS == null) {
      //System.out.println("classLoader="+new XMLStore().getClass().getClassLoader());
      throw new java.io.IOException("XMLStore.createFromResource cant find <"+resourceName+">");
    }

    if (debugWhichStore) System.out.println("XMLStore read from resource "+resourceName);
    return new XMLStore( primIS, objIS, storedDefaults);
  }

  private static boolean debugConvert = false, debugWhichStore = false;
  private static boolean debugWriteNested = false, debugWriteBean = false;

  private File prefsFile = null;
  private PreferencesExt rootPrefs = new PreferencesExt(null, ""); // root node
  private boolean showDecoderExceptions = true; // debugging

  public XMLStore() {}

  /**
   * Constructor. Needs two copies of the same input stream, one for our parser and one for XMLDecoder.
   * @param primIS: store input stream. may be null.
   * @param objIS: store input stream. may be null only if primIS is null.
   * @param storedDefaults: chain to this one.
   * @throws IOException on errpr
   */
  private XMLStore(InputStream primIS, InputStream objIS, XMLStore storedDefaults) throws java.io.IOException {

    // read file if it exists
    if (null != primIS) {
      InputStream convertIS = convert2XmlDecoder(objIS);
      objIS.close();

      // get a SAX parser from JAXP layer
      SAXParserFactory factory = SAXParserFactory.newInstance();
      try {
        SAXParser saxParser = factory.newSAXParser();
        MySaxHandler handler = new MySaxHandler( convertIS);

        // the work is done here
        saxParser.parse(primIS, handler);

      } catch (ParserConfigurationException e) {
        e.printStackTrace();

      } catch (SAXException se) {
        System.out.println("SAXException = "+se.getMessage());
        se.printStackTrace();

        Exception see = se.getException();
        if (see != null) {
          System.out.println("from = "+see.getMessage());
          see.printStackTrace();
        }

      } catch (IOException ioe) {
        ioe.printStackTrace();
      }

      primIS.close();
      convertIS.close();
    }

    // chain
    if (storedDefaults != null)
      rootPrefs.setStoredDefaults( storedDefaults.getPreferences());
  }

  private XMLDecoder openBeanDecoder( InputStream objIS) {
    // filter stream for XMLDecoder
      XMLDecoder beanDecoder = new XMLDecoder( objIS, null, new ExceptionListener() {
        public void exceptionThrown(Exception e) {
          if (showDecoderExceptions)
            System.out.println("***XMLStore.read() got Exception= "+e.getClass().getName()+" "+e.getMessage());
            e.printStackTrace();
        }
      });

      //System.out.println("openBeanDecoder at "+objIS);
      return beanDecoder;
  }

  private InputStream convert2XmlDecoder(InputStream is) throws IOException {
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(false);
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    } finally {
      if (is != null) is.close();
    }

    if (debugConvert) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("***XMLStore.convert2XmlDecoder = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element root = doc.getRootElement();
    org.jdom2.Document convertDoc = new Document();
    Element convertRoot = new Element("java");
    convertRoot.setAttribute("version", "1.4.1_01");
    convertRoot.setAttribute("class", "java.beans.XMLDecoder");
    convertDoc.setRootElement(convertRoot);
    add(root, convertRoot);

    XMLOutputter xmlOut = new XMLOutputter();
    xmlOut.setFormat(Format.getPrettyFormat());
    if (debugConvert) {
      System.out.printf("%n************************************%n");
      System.out.println("***Convert2Version2 converted = \n" + xmlOut.outputString(convertDoc) + "\n*******");
    }

    return new StringBufferInputStream(xmlOut.outputString(convertDoc));
  }

  private void add(Element elem, Element parent) {
    if (elem.getName().equals("object")) {
      parent.addContent( (Element) elem.clone());
      return;
    }

    for (Object child : elem.getChildren()) {
      add((Element) child, parent);
    }
  }


  /* private XMLDecoder openBeanDecoder( InputStream objIS) {
    // filter stream for XMLDecoder
    try {
      InputMunger im = new InputMunger( objIS);
      XMLDecoder beanDecoder = new XMLDecoder( im, null, new ExceptionListener() {
        public void exceptionThrown(Exception e) {
          if (showDecoderExceptions)
            System.out.println("***XMLStore.read() got Exception= "+e.getClass().getName()+" "+e.getMessage());
            e.printStackTrace();
        }
      });

      //System.out.println("openBeanDecoder at "+objIS);
      return beanDecoder;

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return null;
    }

  } */

  /**
   * Convenience routine for creating an XMLStore file in a standard place.
   *
   * <p> Initialize:
   * <ol>
   *   <li> $user.home = System.getProperty("user.home"), if not exist, use "."
   *   <li> create directory "$(user_home)/appName/" , if not exist, create it
   *   <li> return  "$(user_home)/appName/storeName" for use in createFromFile()
   * </ol>
   * @param appName application name
   * @param storeName store name
   * @return  standard file name
   */
  static public String makeStandardFilename(String appName, String storeName) {
      // the directory
    String userHome = null;
    try {
      userHome = System.getProperty("user.home");
    } catch (Exception e) {
      System.out.println( "XMLStore.makeStandardFilename: error System.getProperty(user.home) "+e);
    }
    if (null == userHome) userHome = ".";

    String dirFilename = userHome+"/"+appName;
    File f = new File(dirFilename);
    if (!f.exists())
      f.mkdirs(); // now ready for file creation in writeXML
    return dirFilename +"/"+ storeName;
  }


  /**
   * Open/Create a read-only XMLStore from the specified InputStream.
   * @param InputStream inStream: read the XML from this InputStream.
   * @param XMLStore chain: This contains the "stored defaults", or null if none.
   *
  public XMLStore(InputStream prefsIS, XMLStore storedDefaults) {

    // read in values
    readXmlInput( new BufferedInputStream(inStream), rootPrefs);

    // chain
    if (storedDefaults != null)
      rootPrefs.setStoredDefaults( storedDefaults.getPreferences());
  } */

  /**
   * Get the root Preferences node. All manipulation is done through it.
   */
  public PreferencesExt getPreferences() { return rootPrefs; }

  /////////////////////////////////////////////////
  // reading

  // SAX callback handler
  private class MySaxHandler extends org.xml.sax.helpers.DefaultHandler {
    private boolean debug = false, debugDetail = false;
    private InputStream objIS;
    private XMLDecoder beanDecoder = null; // handles <beanObject> - arbitrary beans

    MySaxHandler (InputStream objIS) throws IOException {
      super();
      this.objIS = objIS;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      if (qName.equalsIgnoreCase("root"))
        startRoot(attributes);
      else if (qName.equalsIgnoreCase("map"))
        startMap(attributes);
      else if (qName.equalsIgnoreCase("node"))
        startNode(attributes);
      else if (qName.equalsIgnoreCase("entry"))
        startEntry(attributes);
      else if (qName.equalsIgnoreCase("bean"))
        startBean(attributes);
      else if (qName.equalsIgnoreCase("beanCollection"))
        startBeanCollection(attributes);
      else if (qName.equalsIgnoreCase("beanObject"))
        startBeanObject(attributes);
      else if (debugDetail)
        System.out.println(" unprocessed startElement = " + qName);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (qName.equalsIgnoreCase("node"))
        endNode();
      if (qName.equalsIgnoreCase("beanCollection"))
        endBeanCollection();
      else if (debugDetail)
        System.out.println(" unprocessed endElement = " + qName);
    }

    private Bean.Collection currentBeanCollection = null;
    private Stack stack;
    private PreferencesExt current;
    private void startRoot( Attributes attributes) {
      if (debugDetail) System.out.println(" startRoot ");
      stack = new Stack();
      current = rootPrefs;
     }
    private void startMap( Attributes attributes) {
      if (debugDetail) System.out.println(" startMap ");
     }
    private void startNode( Attributes attributes) {
      String name = attributes.getValue("name");
      if (debug) System.out.println(" node = "+name);
      if (name.equals("dumpPane") && debug)
        System.out.println("");
      stack.push( current);
      current = (PreferencesExt) current.node(name); // create new node
     }
    private void startEntry( Attributes attributes) {
      String key = attributes.getValue("key");
      String values = attributes.getValue("value"); // values or value ??
      if (debug) System.out.println(" entry = "+key+" "+values);
      current.put(key, values);
     }
    private void startBean( Attributes attributes) {
      String key = attributes.getValue("key");
      try {
        if (currentBeanCollection != null) {
          Object value = currentBeanCollection.readProperties( attributes);
          if (debug) System.out.println(" bean(collection) = "+key+" value= "+value);
        } else {
          Object value = new Bean( attributes);
          if (debug) System.out.println(" bean = "+key+" value= "+value);
          current.putObject(key, value);
        }
      } catch (Exception e) {
        e.printStackTrace();
      } // ??
    }
    private void startBeanCollection( Attributes attributes) {
      String key = attributes.getValue("key");
      try {
        currentBeanCollection = new Bean.Collection( attributes);
        if (debug) System.out.println(" beanCollection = "+key);
        current.putObject(key, currentBeanCollection);
      } catch (Exception e) { } // ??
    }
    private void startBeanObject( Attributes attributes) {
      if (beanDecoder == null)
        beanDecoder = openBeanDecoder( objIS);

      String key = attributes.getValue("key");
      try {
        if (debug) System.out.print(" beanObject = "+key+" ");
        Object value = beanDecoder.readObject(); // read from filtered stream
        if (debug) System.out.println(" value= "+value);
        current.putObject(key, value);
      } catch (Exception e) {
        System.out.println("#ERROR beanDecoder; beanObject key = "+key);
        e.printStackTrace();
      }
    }

    private void endBeanCollection( ) { currentBeanCollection = null; }
    private void endNode( ) { current = (PreferencesExt) stack.pop(); }
  }

  String findAttribute(Attributes atts, String what) {
    for (int i=0; i< atts.getLength(); i++) {
      if (atts.getLocalName(i).equals(what))
        return atts.getValue(i);
    }
    return null;
  }

  /* Filter out the prefs stuff, add the header and trailer.
   * this is needed to present to XMLDecoder a clean IOstream.
   * rather lame, XMLDecoder should be modified to take a Filter or something.
   *
  private static final int BUFF_SIZE = 1024;
  private static final String header = "<?xml version='1.0' encoding='UTF-8'?>\n<java version='1.4.1_01' class='java.beans.XMLDecoder'>\n";
  private static final String trailer = "</java>\n";
  class InputMunger extends java.io.BufferedInputStream { // java.io.FilterInputStream {
    // buffer
    private byte buf[];
    private int count = 0;
    private int pos = 0;

    // insert header
    boolean isHeader = true;
    int countHeader = 0;
    int sizeHeader = header.length();

    // insert trailer
    boolean isTrailer = false;
    int countTrailer = 0;
    int sizeTrailer = trailer.length();

    InputMunger(InputStream in) throws IOException {
       this( in, BUFF_SIZE);
    }

    InputMunger(InputStream in, int size) throws IOException {
      super(in);
      buf = new byte[size];
      fill(0);
      pos = prologue.length(); // skip the prologue, its in the header
    }

    public int read(byte[] b, int off, int len) throws IOException {
      for (int i = 0; i < len; i++) {
        int bval = read();
        if (bval < 0) return i > 0 ? i : -1;
        b[off+i] = (byte) bval;
      }
      return len;
    }

    public int read() throws IOException {
      if (isHeader) {
        isHeader = countHeader+1 < sizeHeader;
        return (int) header.charAt ( countHeader++);
      } else if (isTrailer) {
        return (countTrailer < sizeTrailer) ? (int) trailer.charAt ( countTrailer++) : -1;
      } else {
        return read1();
      }
    }

        // read 1 byte from buffer
    private int read1() throws IOException {
      if (pos >= count) { // need a new buffer
        fill(0);
        if (pos >= count) {
          isTrailer = true; // switch to trailer
          return read();
        }
      }
      return buf[pos++] & 0xff;
    }

    // fill buffer from underlying stream, saving the last "save" chars
    // pos always set to 0
    private int fill(int save) throws IOException {
      int start = count - save;
      if (save > 0)
        System.arraycopy(buf, start, buf, 0, save);

      pos = 0;
      count = save;
      int n = in.read(buf, save, buf.length - save);
      if (n > 0) count += n;
      return n;
    }

  } // InputMunger  */


  /////////////////////////////////////////////////
  // writing

  /**
   * Save the current state of the Preferences tree to disk, using the
   * original filename. The XMLStore must have been constructed from a
   * writeable XML file.
   * @throws UnsupportedOperationException: if XMLStore was created from createFromResource.
   * @throws java.io.IOException on read error
   */
  public void save() throws java.io.IOException {
    if (prefsFile == null)
      throw new UnsupportedOperationException("XMLStore is read-only");

      // get temporary file to write to
    File prefTemp;
    String parentFilename = prefsFile.getParent();
    if (parentFilename == null) {
      prefTemp = File.createTempFile("pref", ".xml");
    } else {
      File parentFile = new File(parentFilename);
      prefTemp = File.createTempFile("pref", ".xml", parentFile);
    }
    prefTemp.deleteOnExit();

    // save to the temp file
    FileOutputStream fos = new FileOutputStream( prefTemp, false);
    save( fos);
    fos.close();

    // success - rename files
    File xmlBackup = new File(prefsFile.getAbsolutePath() + ".bak");
    if (xmlBackup.exists())
      xmlBackup.delete();
    prefsFile.renameTo( xmlBackup);
    prefTemp.renameTo( prefsFile);
    prefTemp.delete();
  }

  /**
   * Save the current state of the Preferences tree to the given OutputStream.
   */
  public void save(OutputStream out) throws java.io.IOException {
    outputExceptionMessage = null;

    // the OutputMunger strips off the XMLEncoder header
    OutputMunger bos = new OutputMunger( out);
    PrintWriter pw = new PrintWriter( bos);

    XMLEncoder beanEncoder = new XMLEncoder( bos);
    beanEncoder.setExceptionListener(new ExceptionListener() {
      public void exceptionThrown(Exception exception) {
        System.out.println("XMLStore.save() got Exception: abort saving the preferences!");
        exception.printStackTrace();
        outputExceptionMessage = exception.getMessage();
      }
    });

    pw.printf("<?xml version='1.0' encoding='UTF-8'?>%n");
    pw.printf("<preferences EXTERNAL_XML_VERSION='1.0'>%n");
    if (!rootPrefs.isUserNode())
      pw.printf("  <root type='system'>%n");
    else
      pw.printf("  <root type='user'>%n");

    Indent indent = new Indent(2);
    indent.incr();
    writeXmlNode(bos, pw, rootPrefs, beanEncoder, indent);
    if (outputExceptionMessage != null)
      throw new IOException(outputExceptionMessage);    

    pw.printf("  </root>%n");
    pw.printf("</preferences>%n");
    pw.flush();
  }
  private String outputExceptionMessage;

  private void writeXmlNode( OutputMunger bos, PrintWriter out, PreferencesExt prefs, XMLEncoder beanEncoder, Indent indent) throws IOException {
    indent.incr();

    if (debugWriteNested) System.out.println(" writeXmlNode "+prefs);
    if (debugWriteBean) {
      ClassLoader l = Thread.currentThread().getContextClassLoader();
      System.out.println("  ClassLoader "+l.getClass().getName());
    }

    try {
      String[] keys = prefs.keysNoDefaults();
      if (keys.length == 0) {
        out.printf("%s<map/>%n", indent);
      } else {
        out.printf("%s<map>%n", indent);
        for (int i=0; i<keys.length; i++) {
          Object value =  prefs.getObjectNoDefaults(keys[i]);
          // LOOK! test if in stored defaults ??

          if (value instanceof String) {
            if (debugWriteNested) System.out.println("  write entry "+keys[i]+" "+value);
            out.printf("%s  <entry key='%s' value='%s' />%n", indent, keys[i], quote((String) value));

          } else if (value instanceof Bean.Collection) {
            Bean.Collection bean = (Bean.Collection) value;
            if (debugWriteNested) System.out.println("  write bean collection "+keys[i]);

            if (bean.getCollection().isEmpty()) // skip empty ??
              continue;

            out.printf("%s  <beanCollection key='%s' class='%s' >%n", indent, keys[i], bean.getBeanClass().getName());

            for (Object o : bean.getCollection()) {
              out.printf("%s    <bean ", indent);
              bean.writeProperties(out, o);
              out.println("/>");
            }
            out.printf("%s  </beanCollection>", indent);

          } else if (value instanceof Bean) {
            Bean bean = (Bean) value;
            if (debugWriteNested) System.out.println("  write bean "+keys[i]+" "+value);
            out.printf("%s  <bean key='%s' class='%s' ", indent, keys[i], bean.getBeanClass().getName());
            bean.writeProperties( out);
            out.printf("/>%n");

          } else { // not a String or Bean
            out.printf("%s  <beanObject key='%s' >%n", indent, keys[i]);
            out.flush();
            bos.enterBeanStream();
            try {
              if (debugWriteNested || debugWriteBean) System.out.println("  write beanObject "+keys[i]+" "+value+" "+value.getClass().getName());
              beanEncoder.writeObject( value);
              if (debugWriteBean) System.out.println("  write bean done ");
            } catch (Exception e) {
              System.out.println("Exception beanEncoder: "+e);
              e.printStackTrace();
              throw new IOException(e.getMessage());
            }
            beanEncoder.flush();
            bos.exitBeanStream();
            out.printf("%s  </beanObject>%n", indent);
          }
        }
        out.printf("%s</map>%n", indent);
      }

      String[] kidName = prefs.childrenNames();
      for (int i=0; i<kidName.length; i++) {
        PreferencesExt pkid = (PreferencesExt) prefs.node( kidName[i]);
        out.printf("%s<node name='%s' >%n", indent, pkid.name());
        writeXmlNode(bos, out, pkid, beanEncoder, indent);
        out.printf("%s</node>%n", indent);
      }

    } catch (java.util.prefs.BackingStoreException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage());
    }

    indent.decr();
  }

  static private char[] replaceChar = {'&', '<', '>', '\'', '"', '\r', '\n'};
  static private String[] replaceWith = {"&amp;", "&lt;", "&gt;", "&apos;", "&quot;", "&#13;", "&#10;" };

  static String quote(String x) {
    // common case no replacement
    boolean ok = true;
    for (int i=0; i<replaceChar.length; i++) {
      int pos = x.indexOf(replaceChar[i]);
      ok &= (pos < 0);
    }
    if (ok) return x;

    // gotta do it
    StringBuffer result = new StringBuffer(x);
    for (int i=0; i<replaceChar.length; i++) {
      int pos = x.indexOf(replaceChar[i]);
      if (pos >= 0)
        replace( result, replaceChar[i], replaceWith[i]);
    }

    return result.toString();
  }

  static private void replace(StringBuffer sb, char out, String in) {
    for (int i = 0; i < sb.length(); i++) {
      if (sb.charAt(i) == out) {
        sb.replace(i, i+1, in);
        i += in.length();
      }
    }
  }


  //private final int DIE = 0; // 97;
  private class OutputMunger extends java.io.BufferedOutputStream {
    boolean done = false;
    boolean bean = false;
    int countNL = 0;

    OutputMunger(OutputStream out) { super(out, 1024); }

    void enterBeanStream() { bean = true; }

    void exitBeanStream() { bean = false; }

    public void write(int b) throws IOException {
      if (done || !bean) super.write(b);
      else {
        if (b == '\n') countNL++;
        if (countNL == 2) done = true; // skip 2 lines
      }
    }

    public void write(byte b[], int off, int len) throws IOException {
      if (done || !bean) super.write(b, off, len);
      else {
        for (int i=0; i<len; i++)
          write(b[off+i]);
      }
    }

  }

  /** testing */
  public static void main(String args[]) throws IOException {
    InputStream is = new FileInputStream( "C:\\Users\\caron\\.unidata\\NetcdfUI22.xml");
    XMLStore store = new XMLStore();
    IO.copy(is, System.out);
    is.close();
  }

}