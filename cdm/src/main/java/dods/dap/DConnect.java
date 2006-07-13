/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap;
import java.net.*;
import java.io.*;
import dods.dap.parser.ParseException;
import java.util.zip.InflaterInputStream;
import java.util.Map;
import java.util.Iterator;

/**
 * This class provides support for common DODS client-side operations such as
 * dereferencing a DODS URL, communicating network activity status
 * to the user and reading local DODS objects.
 * <p>
 * Unlike its C++ counterpart, this class does not store instances of the DAS,
 * DDS, etc. objects. Rather, the methods <code>getDAS</code>, etc. return
 * instances of those objects.
 *
 * @version $Revision: 48 $
 * @author jehamby
 */
public class DConnect {
  static private  boolean allowSessions = false;
  static public void setAllowSessions( boolean b) { allowSessions = b; }

  private boolean dumpStream = false, dumpDAS = false, debugHeaders = false, debugConnect = false;

  /** InputStream to use for connection to a file instead of a remote host. */
  private InputStream fileStream;

  /** The last URLConnection used to communicate with the DODS server.
   * Note: This is not an HttpURLConnection because file: URL's are allowed.
   * Theoretically, one could    also run DODS over FTP, NFS, or any other
   * protocol, although this is probably not very useful.
   */
  private URLConnection connection;

  /**
   * The current DODS URL, as a String (will be converted to URL inside of
   * getDAS(), getDDS(), and getData()), without Constraint Expression.
   */
  private String urlString;

  /** The projection portion of the current DODS CE (including leading "?"). */
  private String projString;

  /** The selection portion of the current DODS CE (including leading "&"). */
  private String selString;

  /** Whether to accept compressed documents. */
  private boolean acceptDeflate;

  // various stuff that comes from the HTTP headers
  private String cookies = null;
  private String lastModified = null;
  private String lastExtended = null;
  private String lastModifiedInvalid = null;

  /** The DODS server version. */
  private ServerVersion ver;

  /**
   * Creates an instance bound to url which accepts compressed documents.
   * @param urlString connect to this URL.
   * @exception FileNotFoundException thrown if <code>urlString</code> is not
   *     a valid URL, or a filename which exists on the system.
   * @see DConnect#DConnect(String, boolean)
   */
  public DConnect(String urlString) throws FileNotFoundException {
    this(urlString, true);
  }

  /**
   * Creates an instance bound to url. If <code>acceptDeflate</code> is true
   * then HTTP Request headers will indicate to servers that this client can
   * accept compressed documents.
   *
   * @param urlString Connect to this URL.  If urlString is not a valid URL,
   *   it is assumed to be a filename, which is opened.
   * @param acceptDeflate true if this client can accept responses encoded
   *   with deflate.
   * @exception FileNotFoundException thrown if <code>urlString</code> is not
   *   a valid URL, or a filename which exists on the system.
   */
  public DConnect(String urlString, boolean acceptDeflate) throws FileNotFoundException {
    int ceIndex = urlString.indexOf('?');
    if(ceIndex != -1) {
      this.urlString = urlString.substring(0, ceIndex);
      String expr = urlString.substring(ceIndex);
      int selIndex = expr.indexOf('&');
      if(selIndex != -1) {
        this.projString = expr.substring(0, selIndex);
        this.selString = expr.substring(selIndex);
      } else {
        this.projString = expr;
        this.selString = "";
      }
    } else {
      this.urlString = urlString;
      this.projString = this.selString = "";
    }
    this.acceptDeflate = acceptDeflate;

    // Test if the URL is really a filename, and if so, open the file
    try {
      URL testURL = new URL(urlString);
    }
    catch (MalformedURLException e) {
      fileStream = new FileInputStream(urlString);
    }
  }

  /**
   * Creates an instance bound to an already open <code>InputStream</code>.
   * @param is the <code>InputStream</code> to open.
   */
  public DConnect(InputStream is) {
    this.fileStream = is;
  }

  /**
   * Returns whether a file name or <code>InputStream</code> is being used
   *     instead of a URL.
   * @return true if a file name or <code>InputStream</code> is being used.
   */
  public final boolean isLocal() {
    return (fileStream != null);
  }

  /**
   * Returns the constraint expression supplied with the URL given to the
   * constructor. If no CE was given this returns an empty <code>String</code>.
   * <p>
   * Note that the CE supplied to one of this object's constructors is
   * "sticky"; it will be used with every data request made with this object.
   * The CE passed to <code>getData</code>, however, is not sticky; it is used
   * only for that specific request. This method returns the sticky CE.
   *
   * @return the constraint expression associated with this connection.
   */
  public final String CE() {
    return projString + selString;
  }

  /**
   * Returns the URL supplied to the constructor. If the URL contained a
   * constraint expression that is not returned.
   *
   * @return the URL of this connection.
   */
  public final String URL() {
    return urlString;
  }

  /**
   * Open a connection to the DODS server.
   * @param url the URL to open.
   * @return the opened <code>InputStream</code>.
   * @exception IOException if an IO exception occurred.
   * @exception DODSException if the DODS server returned an error.
   */
  private InputStream openConnection(URL url) throws IOException, DODSException {
    connection = url.openConnection();
    if (acceptDeflate)
      connection.setRequestProperty("Accept-Encoding", "deflate");

    // enable sessions
    if (allowSessions) {
      connection.setRequestProperty("X-Accept-Session", "true");
      if (cookies != null)
        connection.setRequestProperty("Cookie", cookies);
    }

    connection.connect();

    // theory is that some errors happen "naturally" (under heavy loads i think)
    // so try it 3 times
    InputStream is = null;
    int retry = 1;
    long backoff = 100L;
    while (true) {
      try {
        is = connection.getInputStream(); // get the HTTP InputStream
        break;
        /* if (is.available() > 0)
          break;
        System.out.println("DConnect available==0; retry open ("+retry+") "+url);
        try { Thread.currentThread().sleep(backoff); }
        catch (InterruptedException ie) {} */

      } catch (NullPointerException e) {
        if (debugConnect) System.out.println("DConnect NullPointer; retry open ("+retry+") "+url);
        try { Thread.currentThread().sleep(backoff); }
        catch (InterruptedException ie) {}

      } catch (FileNotFoundException e) {
        if (debugConnect) System.out.println("DConnect FileNotFound; retry open ("+retry+") "+url);
        try { Thread.currentThread().sleep(backoff); }
        catch (InterruptedException ie) {}
      }

      if (retry == 3)
        throw new DODSException("Connection cannot be opened");
      retry++;
      backoff *= 2;
    }

    // check headers
    String type = connection.getHeaderField("content-description");
    handleContentDesc(is, type);
    ver = new ServerVersion(connection.getHeaderField("xdods-server"));
    checkHeaders();

    return handleContentEncoding(is, connection.getContentEncoding());
  }

  public void closeSession() {
    if (allowSessions && cookies != null) {
      try {
        URL url = new URL(urlString + ".close");
        connection = url.openConnection();
        connection.setRequestProperty("Cookie", cookies);
        connection.connect();

        // we dont really care about reading the content, but i think its good form to clear it out
        InputStream is = connection.getInputStream();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1000];
        while (true) {
          int bytesRead = is.read(buffer);
          if (bytesRead == -1) break;
          bout.write(buffer, 0, bytesRead);
        }

      } catch (IOException ioe) {

      }
    }
  }

  private void checkHeaders() {
    if (debugHeaders) System.out.println("OpenConnection Headers for "+connection.getURL());
    Map headerMap = connection.getHeaderFields();
    Iterator iter = headerMap.keySet().iterator();
    while (iter.hasNext()) {
      Object key = iter.next();
      if (key == null) continue;

      java.util.List values = (java.util.List) headerMap.get(key);
      for (int i = 0; i < values.size(); i++) {
        String value = (String) values.get(i);
        if (debugHeaders) System.out.println("  " + key + " == " + value);

        if (key.equals("Set-Cookie")) {
          cookies = value;
          if (debugHeaders) System.out.println(" **found cookies = " + cookies);

        } else if (key.equals("Last-Modified")) {
          lastModified = value;
          if (debugHeaders) System.out.println(" **found lastModified = " + lastModified);

        } else if (key.equals("X-Last-Extended")) {
          lastExtended = value;
          if (debugHeaders) System.out.println(" **found lastExtended = " + lastExtended);

        } else if (key.equals("X-Last-Modified-Invalid")) {
          lastModifiedInvalid = value;
          if (debugHeaders) System.out.println(" **found lastModifiedInvalid = " + lastModifiedInvalid);
        }
      } // iter over values
    } // iter over headers
  }

  /**
   * Returns the DAS object from the dataset referenced by this object's URL.
   * The DAS object is referred to by appending `.das' to the end of a DODS
   * URL.
   *
   * @return the DAS associated with the referenced dataset.
   * @exception MalformedURLException if the URL given to the
   *   constructor has an error
   * @exception IOException if an error connecting to the remote server
   * @exception ParseException if the DAS parser returned an error
   * @exception DASException on an error constructing the DAS
   * @exception DODSException if an error returned by the remote server
   */
  public DAS getDAS() throws MalformedURLException, IOException,
                             ParseException, DASException, DODSException {
    InputStream is;
    if (fileStream != null)
      is = parseMime(fileStream);
    else {
      URL url = new URL(urlString + ".das" + projString + selString);
      if (dumpDAS) {
        System.out.println("--DConnect.getDAS to "+url);
        copy( url.openStream(), System.out);
        System.out.println("\n--DConnect.getDAS END1");
        dumpBytes( url.openStream(), 100);
        System.out.println("\n-DConnect.getDAS END2");
      }
      is = openConnection(url);
    }
    DAS das = new DAS();
    try {
      das.parse(is);
    } finally {
      is.close();  // stream is always closed even if parse() throws exception
      if (connection instanceof HttpURLConnection)
        ((HttpURLConnection)connection).disconnect();
    }
    return das;
  }

  /**
   * Returns the DDS object from the dataset referenced by this object's URL.
   * The DDS object is referred to by appending `.dds' to the end of a DODS
   * URL.
   *
   * @return the DDS associated with the referenced dataset.
   * @exception MalformedURLException if the URL given to the constructor
   *    has an error
   * @exception IOException if an error connecting to the remote server
   * @exception ParseException if the DDS parser returned an error
   * @exception DDSException on an error constructing the DDS
   * @exception DODSException if an error returned by the remote server
   */
  public DDS getDDS() throws MalformedURLException, IOException,
                             ParseException, DDSException, DODSException {
    InputStream is;
    if (fileStream != null)
      is = parseMime(fileStream);
    else {
      URL url = new URL(urlString + ".dds" + projString + selString);
      is = openConnection(url);
    }
    DDS dds = new DDS();
    try {
      dds.parse(is);
    } finally {
      is.close();  // stream is always closed even if parse() throws exception
      if (connection instanceof HttpURLConnection)
        ((HttpURLConnection)connection).disconnect();
    }
    return dds;
  }

  /**
   * Returns the `Data object' from the dataset referenced by this object's
   * URL given the constraint expression CE. Note that the Data object is
   * really just a DDS object with data bound to the variables. The DDS will
   * probably contain fewer variables (and those might have different
   * types) than in the DDS returned by getDDS() because that method returns
   * the entire DDS (but without any data) while this method returns
   * only those variables listed in the projection part of the constraint
   * expression.
   * <p>
   * Note that if CE is an empty String then the entire dataset will be
   * returned, unless a "sticky" CE has been specified in the constructor.
   *
   * @param CE The constraint expression to be applied to this request by the
   *    server.  This is combined with any CE given in the constructor.
   * @param statusUI the <code>StatusUI</code> object to use for GUI updates
   *    and user cancellation notification (may be null).
   * @return The <code>DataDDS</code> object that results from applying the
   *    given CE, combined with this object's sticky CE, on the referenced
   *    dataset.
   * @exception MalformedURLException if the URL given to the constructor
        has an error
   * @exception IOException if any error connecting to the remote server
   * @exception ParseException if the DDS parser returned an error
   * @exception DDSException on an error constructing the DDS
   * @exception DODSException if any error returned by the remote server
   */
  public DataDDS getData(String CE, StatusUI statusUI, BaseTypeFactory btf) throws MalformedURLException, IOException,
          ParseException, DDSException, DODSException {


    if (fileStream != null)
      return getDataFromFileStream(fileStream, statusUI, btf);

    String localProjString, localSelString;
    int selIndex = CE.indexOf('&');
    if (selIndex != -1) {
      localProjString = CE.substring(0, selIndex);
      localSelString = CE.substring(selIndex);
    } else {
      localProjString = CE;
      localSelString = "";
    }
    URL url = new URL(urlString + ".dods" + projString + localProjString +
            selString + localSelString);


    return getDataFromUrl(url, statusUI, btf);

    // Removed retry 5/5/06 jcaron
    /* String errorMsg = "DConnect getData failed " + url;
    int errorCode = DODSException.UNKNOWN_ERROR;
    //int retry = 1;
    //long backoff = 100L;
    //while (true) {
    try {
      return getDataFromUrl(url, statusUI, btf);
    }
    catch (DODSException e) {
      if (debugConnect) System.out.println("DConnect getData failed; retry (" + retry + "," + backoff + ") " + url);
      errorMsg = e.getErrorMessage();
      errorCode = e.getErrorCode();

      try {
        Thread.currentThread().sleep(backoff);
      }
      catch (InterruptedException ie) {
      }
    }

    if (retry == 5)
       throw new DODSException(errorCode,errorMsg);
     retry++;
     backoff *= 2;
   } */
  }


  private DataDDS getDataFromFileStream(InputStream fileStream, StatusUI statusUI, BaseTypeFactory btf) throws IOException,
                             ParseException, DDSException, DODSException {

    InputStream is = parseMime(fileStream);
    DataDDS dds = new DataDDS(ver, btf);

    try {
      dds.parse(new HeaderInputStream(is));    // read the DDS header
      // NOTE: the HeaderInputStream will have skipped over "Data:" line
      dds.readData(is, statusUI); // read the data!

    } finally {
      is.close();  // stream is always closed even if parse() throws exception
    }
    return dds;
  }

  public DataDDS getDataFromUrl(URL url, StatusUI statusUI, BaseTypeFactory btf) throws MalformedURLException, IOException,
                             ParseException, DDSException, DODSException {

    InputStream is = openConnection(url);
    DataDDS dds = new DataDDS(ver, btf);

    // DEBUG
    ByteArrayInputStream bis = null;
    if (dumpStream) {
      System.out.println("DConnect to "+url);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      copy(is, bos);
      bis = new ByteArrayInputStream(bos.toByteArray());
      is = bis;
    }

    try {

      if (dumpStream) {
        bis.mark( 1000);
        System.out.println("DConnect parse header: ");
        dump( bis);
        bis.reset();
      }

      dds.parse(new HeaderInputStream(is));    // read the DDS header
      // NOTE: the HeaderInputStream will have skipped over "Data:" line

      if (dumpStream) {
        bis.mark( 20);
        System.out.println("DConnect done with header, next bytes are: ");
        dumpBytes( bis, 20);
        bis.reset();
      }

      dds.readData(is, statusUI); // read the data!

    } catch (Exception e) {
      System.out.println("DConnect dds.parse: "+url+"\n "+e);
      e.printStackTrace();
      /* DEBUG
      if (dumpStream) {
        System.out.println("DConnect dump "+url);
        bis.reset();
        dump(bis);
        bis.reset();
        File saveFile = null;
        try {
          saveFile = File.createTempFile("debug","tmp", new File("."));
          System.out.println("try Save file = "+ saveFile.getAbsolutePath());
          FileOutputStream saveFileOS = new FileOutputStream(saveFile);
          copy(bis, saveFileOS);
          saveFileOS.close();
          System.out.println("wrote Save file = "+ saveFile.getAbsolutePath());
        } catch (java.io.IOException ioe) {
          System.out.println("failed Save file = "+ saveFile.getAbsolutePath());
          ioe.printStackTrace();
        }
      } */

      throw new DODSException("Connection cannot be read "+url+" "+e.getMessage());

    } finally {
      is.close();  // stream is always closed even if parse() throws exception
      if (connection instanceof HttpURLConnection)
        ((HttpURLConnection)connection).disconnect();
    }

    return dds;
  }

  // DEBUG JC
  private void copy(InputStream in, OutputStream out) {
    try {
      byte[] buffer = new byte[256];
      while (true) {
        int bytesRead = in.read(buffer);
        if (bytesRead == -1) break;
        out.write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {}
  }


  // DEBUG JC
  private void dump( InputStream is) throws IOException {
    DataInputStream d = new DataInputStream(is);

    try {
      System.out.println( "dump lines avail="+is.available());
      while ( true ) {
        String line = d.readLine();
        System.out.println( line);
        if (null == line) return;
        if (line.equals("Data:")) break;
      }
      System.out.println( "dump bytes avail="+is.available());
      dumpBytes( is, 20);

    } catch (java.io.EOFException e) {
    }
  }

  private void dumpBytes( InputStream is, int n) {
    try{
      DataInputStream d = new DataInputStream(is);
      int count = 0;
      while ( (count < n) && (d.available() > 0)) {
        System.out.println( count +" "+d.readByte());
        count++;
      }
    } catch (java.io.IOException e) {
    }
  }

  /**
   * Returns the `Data object' from the dataset referenced by this object's
   * URL given the constraint expression CE. Note that the Data object is
   * really just a DDS object with data bound to the variables. The DDS will
   * probably contain fewer variables (and those might have different
   * types) than in the DDS returned by getDDS() because that method returns
   * the entire DDS (but without any data) while this method returns
   * only those variables listed in the projection part of the constraint
   * expression.
   * <p>
   * Note that if CE is an empty String then the entire dataset will be
   * returned, unless a "sticky" CE has been specified in the constructor.
   *
   * @param CE The constraint expression to be applied to this request by the
   *    server.  This is combined with any CE given in the constructor.
   * @param statusUI the <code>StatusUI</code> object to use for GUI updates
   *    and user cancellation notification (may be null).
   * @return The <code>DataDDS</code> object that results from applying the
   *    given CE, combined with this object's sticky CE, on the referenced
   *    dataset.
   * @exception MalformedURLException if the URL given to the constructor
        has an error
   * @exception IOException if any error connecting to the remote server
   * @exception ParseException if the DDS parser returned an error
   * @exception DDSException on an error constructing the DDS
   * @exception DODSException if any error returned by the remote server
   */
  public DataDDS getData(String CE, StatusUI statusUI) throws MalformedURLException, IOException,
                             ParseException, DDSException, DODSException {

    return getData(CE, statusUI, new DefaultFactory());
  }




  /**
   * Return the data object with no local constraint expression.  Same as
   * <code>getData("", statusUI)</code>.
   *
   * @param statusUI the <code>StatusUI</code> object to use for GUI updates
   *    and user cancellation notification (may be null).
   * @return The <code>DataDDS</code> object that results from applying
   *    this object's sticky CE, if any, on the referenced dataset.
   * @exception MalformedURLException if the URL given to the constructor
        has an error
   * @exception IOException if any error connecting to the remote server
   * @exception ParseException if the DDS parser returned an error
   * @exception DDSException on an error constructing the DDS
   * @exception DODSException if any error returned by the remote server
   * @see DConnect#getData(String, StatusUI)
   */
  public final DataDDS getData(StatusUI statusUI) throws MalformedURLException, IOException,
                             ParseException, DDSException, DODSException {
    return getData("", statusUI, new DefaultFactory());
  }

  /**
   * Returns the <code>ServerVersion</code> of the last connection.
   * @return the <code>ServerVersion</code> of the last connection.
   */
  public final ServerVersion getServerVersion() {
    return ver;
  }

  /** get value of Last-Modified Header, may be null */
  public String getLastModifiedHeader() { return lastModified; }

  /** get value of X-Last-Modified-Invalid Header, may be null */
  public String getLastModifiedInvalidHeader() { return lastModifiedInvalid; }

  /** get value of Last-Extended Header, may be null */
  public String getLastExtendedHeader() { return lastExtended; }

  /**
   * A primitive parser for the MIME headers used by DODS.  This is used when
   * reading from local sources of DODS Data objects. It is called by
   * <code>readData</code> to simulate the important actions of the
   * <code>URLConnection</code> MIME header parsing performed in
   * <code>openConnection</code> for HTTP URL's.
   * <p>
   * <b><i>NOTE:</b></i> Because BufferedReader seeks ahead, and therefore
   * removescharacters from the InputStream which are needed later, and
   * because there is no way to construct an InputStream from a
   * BufferedReader, we have to use DataInputStream to read the header
   * lines, which triggers an unavoidable deprecated warning from the
   * Java compiler.
   *
   * @param is the InputStream to read.
   * @return the InputStream to read data from (after attaching any
   *    necessary decompression filters).
   * @exception IOException if any IO error.
   * @exception DODSException if the server returned an Error.
   */
  private InputStream parseMime(InputStream is)
       throws IOException, DODSException {

    // NOTE: because BufferedReader seeks ahead, and therefore removes
    // characters from the InputStream which are needed later, and
    // because there is no way to construct an InputStream from a
    // BufferedReader, we have to use DataInputStream to read the header
    // lines, which triggers an unavoidable deprecated warning from the
    // Java compiler.

    DataInputStream d = new DataInputStream(is);

    String description = null;
    String encoding = null;

    // while there are more header (non-blank) lines
    String line;
    while (!(line = d.readLine()).equals("")) {
        int spaceIndex = line.indexOf(' ');
        // all header lines should have a space in them, but if not, skip ahead
        if (spaceIndex == -1)
            continue;
        String header = line.substring(0, spaceIndex);
        String value = line.substring(spaceIndex+1);

        if (header.equals("Server:")) {
            ver = new ServerVersion(value);
        }
        else if (header.equals("Content-Description:")) {
            description = value;
        }
        else if (header.equals("Content-Encoding:")) {
            encoding = value;
        }
    }
    handleContentDesc(is, description);
    return handleContentEncoding(is, encoding);
  }

  /**
   * This code handles the Content-Description: header for
   * <code>openConnection</code> and <code>parseMime</code>.
   * Throws a <code>DODSException</code> if the type is
   * <code>dods_error</code>.
   *
   * @param is the InputStream to read.
   * @param type the Content-Description header, or null.
   * @exception IOException if any error reading from the server.
   * @exception DODSException if the server returned an error.
   */
  private void handleContentDesc(InputStream is, String type)
       throws IOException, DODSException {
    if (type != null && type.equals("dods_error")) {
      // create server exception object
      DODSException ds = new DODSException();
      // parse the Error object from stream and throw it
      ds.parse(is);
      throw ds;
    }
  }

  /**
   * This code handles the Content-type: header for
   * <code>openConnection</code> and <code>parseMime</code>
   * @param is the InputStream to read.
   * @param encoding the Content-type header, or null.
   * @return the new InputStream, after applying an
   *    <code>InflaterInputStream</code> filter if necessary.
   */
  private InputStream handleContentEncoding(InputStream is, String encoding) {
    if (encoding != null && encoding.equals("deflate")) {
      InflaterInputStream inflater = new InflaterInputStream(is);
      return new BufferedInputStream(inflater, 10 * 1000);   // JC added
    } else {
      return is;
    }
  }
}