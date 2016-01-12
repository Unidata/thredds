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


package opendap.dap;

import opendap.dap.parsers.ParseException;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.auth.InvalidCredentialsException;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Rewritten 1/15/07 jcaron to use HttpCLient library instead of jdk UrlConnection class.
 * Need more robust redirect and authentication.
 * <p>
 * This class provides support for common DODS client-side operations such as
 * dereferencing a OPeNDAP URL, communicating network activity status
 * to the user and reading local OPeNDAP objects.
 * <p>
 * Unlike its C++ counterpart, this class does not store instances of the DAS,
 * DDS, etc. objects. Rather, the methods <code>getDAS</code>, etc. return
 * instances of those objects.
 *
 * @author jehamby
 */
public class DConnect2 implements Closeable
{

    static private boolean allowSessions = false;

    static final Charset UTF8 = Charset.forName("UTF-8");

    static public synchronized void setAllowSessions(boolean b)
    {
        allowSessions = b;
    }


    private String urlString; // The current DODS URL without Constraint Expression
    private String filePath = null; // if url is file://
    private InputStream stream = null; //if reading from a stream

    /**
     * The projection portion of the current DODS CE (including leading "?").
     */
    private String projString;

    /**
     * The selection portion of the current DODS CE (including leading "&").
     */
    private String selString;

    /**
     * Whether to accept compressed documents.
     */
    private boolean acceptCompress;

    // various stuff that comes from the HTTP headers
    private String lastModified = null;
    private String lastExtended = null;
    private String lastModifiedInvalid = null;
    private boolean hasSession = true;
    protected HTTPSession _session = null;

    private ServerVersion ver; // The OPeNDAP server version.

    private boolean debugHeaders = false, debugStream = false;


    public void setServerVersion(int major, int minor)
    {
        //LogStream.out.println("ServerVersion made with int,int: " + major + "," + minor);

        ver = new ServerVersion(major, minor);
        //LogStream.out.println("ServerVersion.getMajor(): " + ver.getMajor());
        //LogStream.out.println("ServerVersion.getMinor(): " + ver.getMinor());
    }

    /**
     * Creates an instance bound to url which accepts compressed documents.
     *
     * @param urlString connect to this URL.
     * @throws FileNotFoundException thrown if <code>urlString</code> is not
     *                               a valid URL, or a filename which exists on the system.
     */
    public DConnect2(String urlString) throws HTTPException
    {
        this(urlString, true);
    }

    /**
     * Creates an instance bound to url. If <code>acceptDeflate</code> is true
     * then HTTP Request headers will indicate to servers that this client can
     * accept compressed documents.
     *
     * @param urlString      Connect to this URL.
     * @param acceptCompress true if this client will accept compressed responses
     * @throws FileNotFoundException thrown if <code>urlString</code> is not
     *                               a valid URL, or a filename which exists on the system.
     */
    public DConnect2(String urlString, boolean acceptCompress)
            throws HTTPException
    {
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
        this.acceptCompress = acceptCompress;
        // Check out the URL to see if it is file://
        try {
            URL testURL = new URL(urlString);
            if("file".equals(testURL.getProtocol())) {
                filePath = testURL.getPath();
                // See if .dds and .dods files exist
                File f = new File(filePath + ".dds");
                if(!f.canRead()) {
                    throw new HTTPException("file not readable: " + urlString + ".dds");
                }
                f = new File(filePath + ".dods");
                if(!f.canRead()) {
                    throw new HTTPException("file not readable: " + urlString + ".dods");
                }
            } else {
                _session = HTTPFactory.newSession(this.urlString);
            }
    /* Set the server version cause we won't get it from anywhere */
            ver = new ServerVersion(ServerVersion.DAP2_PROTOCOL_VERSION, ServerVersion.XDAP);
        } catch (DAP2Exception ex) {
            throw new HTTPException("Cannot set server version");
        } catch (MalformedURLException e) {
            throw new HTTPException("Malformed URL: " + urlString);
        }
    }

/*
 * Creates an instance bound to an Inputstream.
 * @param stream   to get data from
 * @throws IOException  thrown if <code>stream</code> read fails.
 */

    public DConnect2(InputStream stream) throws DAP2Exception
    {
        this.stream = stream;
  /* Set the server version cause we won't get it from anywhere */
        ver = new ServerVersion(ServerVersion.DAP2_PROTOCOL_VERSION, ServerVersion.XDAP);
    }

/*
 * 
 * @return  if reading from file:// or stream
 */

    public boolean isLocal()
    {
        return (stream != null || filePath != null);
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
    public final String CE()
    {
        return projString + selString;
    }

    /**
     * Returns the URL supplied to the constructor. If the URL contained a
     * constraint expression that is not returned.
     *
     * @return the URL of this connection.
     */
    public final String URL()
    {
        return urlString;
    }

    /**
     * Return the session associated with this connection
     *
     * @return this connections session (or null)
     */
    public HTTPSession getSession()
    {
        return _session;
    }

    /**
     * Open a connection to the DODS server.
     *
     * @param urlString the URL to open; assume already properly encoded
     * @param command   execute this command on the input stream
     * @throws IOException   if an IO exception occurred.
     * @throws DAP2Exception if the DODS server returned an error.
     */
    private void openConnection(String urlString, Command command) throws IOException, DAP2Exception
    {
        InputStream is = null;

        try {
            try (HTTPMethod method = HTTPFactory.Get(_session, urlString)) {

                if(acceptCompress)
                    method.setCompression("deflate,gzip");

                // enable sessions
                if(allowSessions)
                    method.setUseSessions(true);
                int statusCode;
                for(;;) {
                    statusCode = method.execute();
                    if(statusCode != HttpStatus.SC_SERVICE_UNAVAILABLE)
                        break;
                    Thread.sleep(5000);
                    System.err.println("Service Unavailable");
                }

                // debug
                // if (debugHeaders) ucar.httpservices.HttpClientManager.showHttpRequestInfo(f, method);

                if(statusCode == HttpStatus.SC_NOT_FOUND) {
                    throw new DAP2Exception(DAP2Exception.NO_SUCH_FILE, method.getStatusText() + ": " + urlString);
                }

                if(statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    throw new InvalidCredentialsException(method.getStatusText());
                }

                if(statusCode != HttpStatus.SC_OK) {
                    throw new DAP2Exception("Method failed:" + method.getStatusText() + " on URL= " + urlString);
                }

                // Get the response body.
                is = method.getResponseAsStream();

                // check if its an error
                Header header = method.getResponseHeader("Content-Description");
                if(header != null && (header.getValue().equals("dods-error")
                        || header.getValue().equals("dods_error"))) {
                    // create server exception object
                    DAP2Exception ds = new DAP2Exception();
                    // parse the Error object from stream and throw it
                    ds.parse(is);
                    throw ds;
                }

                ver = new ServerVersion(method);

                checkHeaders(method);

                // check for deflator
                Header h = method.getResponseHeader("content-encoding");
                String encoding = (h == null) ? null : h.getValue();
                //if (encoding != null) LogStream.out.println("encoding= " + encoding);

                if(encoding != null && encoding.equals("deflate")) {
                    is = new BufferedInputStream(new InflaterInputStream(is), 1000);

                } else if(encoding != null && encoding.equals("gzip")) {
                    is = new BufferedInputStream(new GZIPInputStream(is), 1000);
                }

                command.process(is);
            }
        } catch (Exception e) {
            Util.check(e);
            e.printStackTrace();
            throw new DAP2Exception(e);
        }
    }

    public void close()
    {
        try {
            if(allowSessions && hasSession) {
                openConnection(urlString + ".close", new Command()
                {
                    public void process(InputStream is) throws IOException
                    {
                        captureStream(is);
                    }
                });
            }
            if(_session != null)
                _session.close();
        } catch (Throwable t) {
            // ignore
        }
    }

    static public String captureStream(InputStream is)
            throws IOException
    {
        ByteArrayOutputStream text = new ByteArrayOutputStream();
        int b;
        while((b = is.read()) >= 0) {
            text.write(b);
        }
        return new String(text.toByteArray(), UTF8);
    }

// Extract the leading DDS from a DataDDS stream

    static final byte[] tag1 = "\nData:\n".getBytes(UTF8);
    static final byte[] tag2 = "\nData:\r\n".getBytes(UTF8);

    static public String captureDataDDS(InputStream is)
            throws IOException
    {
        byte[] text = new byte[4096];
        int pos = 0;
        int len = text.length;
        int b;
        boolean eof = true;
        while((b = is.read()) >= 0) {
            text = need(1, pos, len, text);
            len = text.length;
            text[pos++] = (byte) b;
            if(b == '\n') { // write until \nData:\n || \nData:\r\n
                // look for datatags
                if(endswith(tag1, pos, text)) {
                    pos -= tag1.length; // dont return tag
                    eof = false;
                    break;
                } else if(endswith(tag2, pos, text)) {
                    pos -= tag2.length; // dont return tag
                    eof = false;
                    break;
                }
            }
        }
        if(eof)
            throw new IOException("DatDDS has no 'Data:' marker");
        return new String(text, 0, pos, UTF8);
    }

    static private boolean endswith(byte[] tag, int pos, byte[] text)
    {
        int i, j;
        int taglen = tag.length;

        if(pos < taglen) return false;

        for(j = 0, i = pos - taglen; i < pos; i++, j++) {
            if(text[i] != tag[j]) return false;
        }
        return true;
    }

    static private byte[] need(int n, int pos, int len, byte[] text)
    {
        if(len - pos >= n) return text;
        int newlen = len * 2 + n;
        while(newlen < n) {
            newlen++;
        }
        byte[] newtext = new byte[newlen];
        System.arraycopy(text, 0, newtext, 0, len);
        return newtext;
    }

    /**
     * Returns the <code>ServerVersion</code> of the last connection.
     *
     * @return the <code>ServerVersion</code> of the last connection.
     */
    public final ServerVersion getServerVersion()
    {
        return ver;
    }

    /**
     * @return value of Last-Modified Header from last connection, may be null
     */
    public String getLastModifiedHeader()
    {
        return lastModified;
    }

    /**
     * @return value of X-Last-Modified-Invalid Header from last connection, may be null
     */
    public String getLastModifiedInvalidHeader()
    {
        return lastModifiedInvalid;
    }

    /**
     * @return value of Last-Extended Header from last connection, may be null
     */
    public String getLastExtendedHeader()
    {
        return lastExtended;
    }

    private void checkHeaders(HTTPMethod method)
    {
        if(debugHeaders) {
            DAPNode.log.debug("\nOpenConnection Headers for " + method.getPath());
            DAPNode.log.debug("Status Line: " + method.getStatusLine());
        }

        Header[] responseHeaders = method.getResponseHeaders();
        for(int i1 = 0; i1 < responseHeaders.length; i1++) {
            Header responseHeader = responseHeaders[i1];
            if(debugHeaders) DAPNode.log.debug("  " + responseHeader);
            String key = responseHeader.getName();
            String value = responseHeader.getValue();

            if(key.equals("Last-Modified")) {
                lastModified = value;
                if(debugHeaders)
                    DAPNode.log.debug(" **found lastModified = " + lastModified);

            } else if(key.equals("X-Last-Extended")) {
                lastExtended = value;
                if(debugHeaders)
                    DAPNode.log.debug(" **found lastExtended = " + lastExtended);

            } else if(key.equals("X-Last-Modified-Invalid")) {
                lastModifiedInvalid = value;
                if(debugHeaders)
                    DAPNode.log.debug(" **found lastModifiedInvalid = " + lastModifiedInvalid);
            }
        }

        if(debugHeaders)
            DAPNode.log.debug("OpenConnection Headers for " + method.getPath());

    }

    private interface Command
    {
        void process(InputStream is) throws DAP2Exception, IOException;
    }

    /**
     * Returns the DAS object from the dataset referenced by this object's URL.
     * The DAS object is referred to by appending `.das' to the end of a DODS
     * URL.
     *
     * @return the DAS associated with the referenced dataset.
     * @throws MalformedURLException if the URL given to the
     *                               constructor has an error
     * @throws IOException           if an error connecting to the remote server
     * @throws DASException          on an error constructing the DAS
     * @throws DAP2Exception         if an error returned by the remote server
     */
    public DAS getDAS() throws IOException, DAP2Exception
    {
        DASCommand command = new DASCommand();
        if(filePath != null) { // url was file:
            File daspath = new File(filePath + ".das");
            // See if the das file exists
            if(daspath.canRead()) {
                try (FileInputStream is = new FileInputStream(daspath)) {
                    command.process(is);
                }
            }
        } else if(stream != null) {
            command.process(stream);
        } else { // assume url is remote
            try {
                openConnection(urlString + ".das" + projString + selString, command);
            } catch (DAP2Exception de) {
                //if(de.getErrorCode() != DAP2Exception.NO_SUCH_FILE)
                //throw de;  // rethrow
            }
        }
        return command.das;
    }

    static private class DASCommand implements Command
    {
        DAS das = new DAS();

        public void process(InputStream is) throws DAP2Exception, ParseException
        {
            das.parse(is);
        }
    }

    /**
     * Returns the DDS object from the dataset referenced by this object's URL.
     * The DDS object is referred to by appending `.dds' to the end of a OPeNDAP
     * URL.
     *
     * @return the DDS associated with the referenced dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if an error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if an error returned by the remote server
     */
    public DDS getDDS() throws IOException, ParseException, DAP2Exception
    {
        return getDDS("");
    }

    /**
     * Returns the DDS object from the dataset referenced by this object's URL.
     * The DDS object is referred to by appending `.dds' to the end of a OPeNDAP
     * URL.
     *
     * @param CE The constraint expression to be applied to this request by the
     *           server.  This is combined with any CE given in the constructor.
     * @return the DDS associated with the referenced dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if an error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if an error returned by the remote server
     */
    public DDS getDDS(String CE) throws IOException, ParseException, DAP2Exception
    {
        DDSCommand command = new DDSCommand();
        command.setURL(CE == null || CE.length() == 0 ? urlString : urlString + "?" + CE);
        if(filePath != null) {
            try (FileInputStream is = new FileInputStream(filePath + ".dds")) {
                command.process(is);
            }
        } else if(stream != null) {
            command.process(stream);
        } else { // must be a remote url
            openConnection(urlString + ".dds" + (getCompleteCE(CE)), command);
        }
        return command.dds;
    }

    static private class DDSCommand implements Command
    {
        DDS dds = new DDS();
        String url = null;

        public void setURL(String url)
        {
            this.url = url;
            if(dds != null && url != null) dds.setURL(url);
        }

        public void process(InputStream is) throws ParseException, DAP2Exception
        {
            dds.parse(is);
        }
    }

    /**
     * Use some sense when assembling the CE. Since this DConnect
     * object may have constructed using a CE, any new CE will
     * have to be integrated into it for subsequent requests.
     * Try to do this in a sensible manner!
     *
     * @param CE The new CE from the client.
     * @return The complete CE (the one this object was built
     * with integrated with the clients)
     */
    private String getCompleteCE(String CE)
    {
        String localProjString = null;
        String localSelString = null;
        if(CE == null)
            return "";
        //remove any leading '?'
        if(CE.startsWith("?")) CE = CE.substring(1);
        int selIndex = CE.indexOf('&');
        if(selIndex == 0) {
            localProjString = "";
            localSelString = CE;
        } else if(selIndex > 0) {
            localSelString = CE.substring(selIndex);
            localProjString = CE.substring(0, selIndex);
        } else {// selIndex < 0
            localProjString = CE;
            localSelString = "";
        }

        String ce = projString;

        if(!localProjString.equals("")) {
            if(!ce.equals("") && localProjString.indexOf(',') != 0)
                ce += ",";
            ce += localProjString;
        }

        if(!selString.equals("")) {
            if(selString.indexOf('&') != 0)
                ce += "&";
            ce += selString;
        }

        if(!localSelString.equals("")) {
            if(localSelString.indexOf('&') != 0)
                ce += "&";
            ce += localSelString;
        }

        if(ce.length() > 0) ce = "?" + ce;

        if(false) {
            DAPNode.log.debug("projString: '" + projString + "'");
            DAPNode.log.debug("localProjString: '" + localProjString + "'");
            DAPNode.log.debug("selString: '" + selString + "'");
            DAPNode.log.debug("localSelString: '" + localSelString + "'");
            DAPNode.log.debug("Complete CE: " + ce);
        }
        return ce;   // escaping will happen elsewhere
    }

    /**
     * Returns the DDS object from the dataset referenced by this object's URL.
     * The DDS object is referred to by appending `.ddx' to the end of a OPeNDAP
     * URL. The server should send back a DDX (A DDS in XML format) which
     * will get parsed here (locally) and a new DDS instantiated using
     * the DDSXMLParser.
     *
     * @return the DDS associated with the referenced dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if an error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if an error returned by the remote server
     * @opendap.ddx.experimental
     */
    public DDS getDDX() throws IOException, ParseException, DAP2Exception
    {
        return (getDDX(""));
    }


    /**
     * Returns the DDS object from the dataset referenced by this object's URL.
     * The DDS object is referred to by appending `.ddx' to the end of a OPeNDAP
     * URL. The server should send back a DDX (A DDS in XML format) which
     * will get parsed here (locally) and a new DDS instantiated using
     * the DDSXMLParser.
     *
     * @param CE The constraint expression to be applied to this request by the
     *           server.  This is combined with any CE given in the constructor.
     * @return the DDS associated with the referenced dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if an error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if an error returned by the remote server
     * @opendap.ddx.experimental
     */
    public DDS getDDX(String CE) throws IOException, ParseException, DDSException, DAP2Exception
    {
        DDXCommand command = new DDXCommand();
        openConnection(urlString + ".ddx" + (getCompleteCE(CE)), command);
        return command.dds;
    }

    static private class DDXCommand implements Command
    {
        DDS dds = new DDS();

        public void process(InputStream is) throws DAP2Exception, ParseException
        {
            dds.parseXML(is, false);
        }
    }

    /**
     * Returns the DataDDS object from the dataset referenced by this object's URL.
     * The DDS object is referred to by appending `.ddx' to the end of a OPeNDAP
     * URL. The server should send back a DDX (A DDS in XML format) which
     * will get parsed here (locally) and a new DDS instantiated using
     * the DDSXMLParser.
     *
     * @return the DataDDS associated with the referenced dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if an error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if an error returned by the remote server
     * @opendap.ddx.experimental
     */
    public DataDDS getDataDDX() throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception
    {

        return getDataDDX("", new DefaultFactory());
    }


    /**
     * Returns the DataDDS object from the dataset referenced by this object's URL.
     * The DDS object is referred to by appending `.ddx' to the end of a OPeNDAP
     * URL. The server should send back a DDX (A DDS in XML format) which
     * will get parsed here (locally) and a new DDS instantiated using
     * the DDSXMLParser.
     *
     * @param CE The constraint expression to use for this request.
     * @return the DataDDS associated with the referenced dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if an error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if an error returned by the remote server
     * @opendap.ddx.experimental
     */
    public DataDDS getDataDDX(String CE) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception
    {

        return getDataDDX(CE, new DefaultFactory());
    }

    /**
     * Returns the DataDDS object from the dataset referenced by this object's URL.
     * The DDS object is referred to by appending `.ddx' to the end of a OPeNDAP
     * URL. The server should send back a DDX (A DDS in XML format) which
     * will get parsed here (locally) and a new DDS instantiated using
     * the DDSXMLParser.
     *
     * @param CE  The constraint expression to use for this request.
     * @param btf The <code>BaseTypeFactory</code> to build the member
     *            variables in the DDS with.
     * @return the DataDDS associated with the referenced dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if an error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if an error returned by the remote server
     * @opendap.ddx.experimental
     * @see BaseTypeFactory
     */
    public DataDDS getDataDDX(String CE, BaseTypeFactory btf) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception
    {

        DataDDXCommand command = new DataDDXCommand(btf, this.ver);
        openConnection(urlString + ".ddx" + (getCompleteCE(CE)), command);
        return command.dds;
    }

    static private class DataDDXCommand implements Command
    {
        DataDDS dds;

        DataDDXCommand(BaseTypeFactory btf, ServerVersion ver)
        {
            dds = new DataDDS(ver, btf);
        }

        public void process(InputStream is) throws DAP2Exception, ParseException
        {
            dds.parseXML(is, false);
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
     * @param CE       The constraint expression to be applied to this request by the
     *                 server.  This is combined with any CE given in the constructor.
     * @param statusUI the <code>StatusUI</code> object to use for GUI updates
     *                 and user cancellation notification (may be null).
     * @param btf      The <code>BaseTypeFactory</code> to build the member
     *                 variables in the DDS with.
     * @return The <code>DataDDS</code> object that results from applying the
     * given CE, combined with this object's sticky CE, on the referenced
     * dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if any error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if any error returned by the remote server
     */
    public DataDDS getData(String CE, StatusUI statusUI, BaseTypeFactory btf) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception
    {
        if(CE != null && CE.trim().length() == 0) CE = null;
        DataDDS dds = new DataDDS(ver, btf);
        DataDDSCommand command = new DataDDSCommand(dds, statusUI);
        command.setURL(urlString + (CE == null ? "" : "?" + CE));
        if(filePath != null) { // url is file:
            File dodspath = new File(filePath + ".dods");
            // See if the dods file exists
            if(dodspath.canRead()) {
      /* WARNING: any constraints are ignored in reading the file */
                try (FileInputStream is = new FileInputStream(dodspath)) {
                    command.process(is);
                }
            }
        } else if(stream != null) {
            command.process(stream);
        } else {
            String urls = urlString + ".dods" + (CE == null ? "" : getCompleteCE(CE));
            openConnection(urls, command);
        }
        return command.dds;
    }

    static private class DataDDSCommand implements Command
    {
        DataDDS dds = null;
        StatusUI statusUI;
        //Coverity[FB.URF_UNREAD_FIELD]
        String url = null;

        DataDDSCommand(DataDDS dds, StatusUI statusUI)
        {
            this.dds = dds;
            this.statusUI = statusUI;
        }

        public void setURL(String url)
        {
            this.url = url;
        }

        ;

        public void process(InputStream is) throws ParseException, DAP2Exception, IOException
        {
            if(!dds.parse(is))
                throw new DAP2Exception("DataDDS DDS parse failed");
            dds.readData(is, statusUI);
        }
    }

    public DataDDS getData(String CE) throws IOException, ParseException, DAP2Exception
    {
        return getData(CE, null, new DefaultFactory());
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
 * <p/>
 * Note that if CE is an empty String then the entire dataset will be
 * returned, unless a "sticky" CE has been specified in the constructor.
 * <p/>
 * <p/>
 * This method uses the 2 step method for aquiring data from a server using
 * a DDX and a BLOB. First, a DDX (an XML representation of a DDS) is requested.
 * The DDX is parsed and a DataDDS is created.
 * The DDX contains a URL that points to the servers BLOB service. The BLOB
 * service returns only the serialized binary content of the DataDDS. The DataDDS
 * then deserializes the BLOB and fills itself with data.
 *
 * @param CE       The constraint expression to be applied to this request by the
 *                 server.  This is combined with any CE given in the constructor.
 * @param statusUI the <code>StatusUI</code> object to use for GUI updates
 *                 and user cancellation notification (may be null).
 * @return The <code>DataDDS</code> object that results from applying the
 *         given CE, combined with this object's sticky CE, on the referenced
 *         dataset.
 * @throws MalformedURLException if the URL given to the constructor
 *                               has an error
 * @throws IOException           if any error connecting to the remote server
 * @throws ParseException        if the DDS parser returned an error
 * @throws DDSException          on an error constructing the DDS
 * @throws DAP2Exception         if any error returned by the remote server
 * @opendap.ddx.experimental
 *
public DataDDS getDDXData(String CE, StatusUI statusUI, BaseTypeFactory btf) throws MalformedURLException, IOException,
ParseException, DDSException, DAP2Exception {

String urls = urlString + ".ddx" + getCompleteCE(CE);

DataDDS dds = new DataDDS(ver, btf);
DataDDXCommand command = new DataDDXCommand(dds, statusUI);
openConnection(urls, command);

return command.dds;
}

private class DataDDXCommand implements Command {
DataDDS dds;
StatusUI statusUI;

DataDDXCommand(DataDDS dds, StatusUI statusUI) {
this.dds = dds;
this.statusUI = statusUI;
}

/*
 * Returns the `Data object' from the dataset referenced by this object's
 * URL given the constraint expression CE. Note that the Data object is
 * really just a DDS object with data bound to the variables. The DDS will
 * probably contain fewer variables (and those might have different
 * types) than in the DDS returned by getDDS() because that method returns
 * the entire DDS (but without any data) while this method returns
 * only those variables listed in the projection part of the constraint
 * expression.
 * <p/>
 * Note that if CE is an empty String then the entire dataset will be
 * returned, unless a "sticky" CE has been specified in the constructor.
 * <p/>
 * <p/>
 * This method uses the 2 step method for aquiring data from a server using
 * a DDX and a BLOB. First, a DDX (an XML representation of a DDS) is requested.
 * The DDX is parsed and a DataDDS is created.
 * The DDX contains a URL that points to the servers BLOB service. The BLOB
 * service returns only the serialized binary content of the DataDDS. The DataDDS
 * then deserializes the BLOB and fills itself with data.

public void process(InputStream is) throws DAP2Exception, ParseException, IOException {
dds.parseXML(is, false);    // read the DDX
getBlobData(dds, statusUI);
}
}  */

/**
 * Opens the BLOB uRL in the DDS supplied and deserializes that binary content
 * sent from the server cooresponding to the DDS.
 * <p/>
 * <p/>
 * This method is the 2nd step in the 2 step process for aquiring data from a server using
 * a DDX and a BLOB. First, a DDX (an XML representation of a DDS) is requested.
 * The DDX is parsed and a DataDDS is created.
 * The DDX contains a URL that points to the servers BLOB service. The BLOB
 * service returns only the serialized binary content of the DataDDS. The DataDDS
 * then deserializes the BLOB and fills itself with data.
 *
 * @param dds      The DDS containing the BLOB URL and into which the BLOB
 *                 (serialized binary content) will be deserialized.
 * @param statusUI the <code>StatusUI</code> object to use for GUI updates
 *                 and user cancellation notification (may be null).
 * @throws MalformedURLException if the URL given to the constructor
 *                               has an error
 * @throws IOException           if any error connecting to the remote server
 * @throws ParseException        if the DDS parser returned an error
 * @throws DDSException          on an error constructing the DDS
 * @throws DAP2Exception         if any error returned by the remote server
 *
private void getBlobData(DataDDS dds, StatusUI statusUI)
throws MalformedURLException, IOException,
ParseException, DDSException, DAP2Exception {


/* boolean dumpStreamErr = false; // opendap.util.util.Debug.isSet("dumpStreamErr");

DAPNode.debug("dds.getBlobURL(): " + dds.getBlobContentID());
LogStream.dbg.logflush();

if (dds.getBlobContentID() == null) {
throw new MalformedURLException("Blob URL was 'null'. " +
"This may indicate that this OPeNDAP Server does not support the full use of DDX.");
}


URL blobURL = new URL(dds.getBlobContentID());

DAPNode.debug("Opening BLOB URL: " + blobURL);
LogStream.dbg.logflush();

InputStream is = openConnection(blobURL);


try {

dds.readData(is, statusUI); // read the data!

} catch (Throwable e) {
LogStream.err.println("DConnect dds.readData problem with: " + blobURL + "\nStack Trace:");
e.printStackTrace(LogStream.err);
LogStream.err.logflush();

throw new DAP2Exception("Connection problem when reading: " + blobURL + "\n" +
"Error Message - " + e.toString());

} finally {
is.close();  // stream is always closed even if parse() throws exception
if (connection instanceof HttpURLConnection)
((HttpURLConnection) connection).disconnect();
}


} */

/**
 * Returns the `Data object' from the dataset referenced by this object's
 * URL given the constraint expression CE. Note that the Data object is
 * really just a DDS object with data bound to the variables. The DDS will
 * probably contain fewer variables (and those might have different
 * types) than in the DDS returned by getDDS() because that method returns
 * the entire DDS (but without any data) while this method returns
 * only those variables listed in the projection part of the constraint
 * expression.
 * <p/>
 * Note that if CE is an empty String then the entire dataset will be
 * returned, unless a "sticky" CE has been specified in the constructor.
 * <p/>
 * <p/>
 * This method uses the 2 step method for aquiring data from a server using
 * a DDX and a BLOB. First, a DDX (an XML representation of a DDS) is requested.
 * The DDX is parsed and a DataDDS is created.
 * The DDX contains a URL that points to the servers BLOB service. The BLOB
 * service returns only the serialized binary content of the DataDDS. The DataDDS
 * then deserializes the BLOB and fills itself with data.
 *
 * @param url      The complete URL of the dataset. Constraint Expression included.
 * @param statusUI the <code>StatusUI</code> object to use for GUI updates
 *                 and user cancellation notification (may be null).
 * @param btf      The <code>BaseTypeFactory</code> to build the member
 *                 variables in the DDS with.
 * @return The <code>DataDDS</code> object that results from applying the
 *         given CE, combined with this object's sticky CE, on the referenced
 *         dataset.
 * @throws MalformedURLException if the URL given to the constructor
 *                               has an error
 * @throws IOException           if any error connecting to the remote server
 * @throws ParseException        if the DDS parser returned an error
 * @throws DDSException          on an error constructing the DDS
 * @throws DAP2Exception         if any error returned by the remote server
 * @opendap.ddx.experimental
 *
public DataDDS getDDXDataFromURL(URL url, StatusUI statusUI, BaseTypeFactory btf)
throws IOException,
ParseException, DDSException, DAP2Exception {

DAPNode.debug("Opening DDX URL: " + url);
LogStream.dbg.logflush();

InputStream is = openConnection(url);
DataDDS dds = new DataDDS(ver, btf);

boolean dumpStreamErr = false; // opendap.util.util.Debug.isSet("dumpStreamErr");


try {


dds.parseXML(is, false);    // read the DDX

//dds.parse(new HeaderInputStream(is));    // read the DDS header
// NOTE: the HeaderInputStream will have skipped over "Data:" line

} catch (Throwable e) {
LogStream.err.println("DConnect ddx.parse problem with: " + url + "\nStack Trace:");
e.printStackTrace(LogStream.err);
LogStream.err.logflush();

throw new DAP2Exception("Connection problem when reading: " + url + "\n" +
"Error Message - " + e.toString());

} finally {
is.close();  // stream is always closed even if parse() throws exception
if (connection instanceof HttpURLConnection)
((HttpURLConnection) connection).disconnect();
}


getBlobData(dds, statusUI);


return dds;
} */

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
     * @param CE       The constraint expression to be applied to this request by the
     *                 server.  This is combined with any CE given in the constructor.
     * @param statusUI the <code>StatusUI</code> object to use for GUI updates
     *                 and user cancellation notification (may be null).
     * @return The <code>DataDDS</code> object that results from applying the
     * given CE, combined with this object's sticky CE, on the referenced
     * dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if any error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if any error returned by the remote server
     */
    public DataDDS getData(String CE, StatusUI statusUI) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception
    {

        return getData(CE, statusUI, new DefaultFactory());
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
 * <p/>
 * Note that if CE is an empty String then the entire dataset will be
 * returned, unless a "sticky" CE has been specified in the constructor.
 * <p/>
 * <p/>
 * This method uses the 2 step method for aquiring data from a server using
 * a DDX and a BLOB. First, a DDX (an XML representation of a DDS) is requested.
 * The DDX is parsed and a DataDDS is created.
 * The DDX contains a URL that points to the servers BLOB service. The BLOB
 * service returns only the serialized binary content of the DataDDS. The DataDDS
 * then deserializes the BLOB and fills itself with data.
 *
 * @param CE       The constraint expression to be applied to this request by the
 *                 server.  This is combined with any CE given in the constructor.
 * @param statusUI the <code>StatusUI</code> object to use for GUI updates
 *                 and user cancellation notification (may be null).
 * @return The <code>DataDDS</code> object that results from applying the
 *         given CE, combined with this object's sticky CE, on the referenced
 *         dataset.
 * @throws MalformedURLException if the URL given to the constructor
 *                               has an error
 * @throws IOException           if any error connecting to the remote server
 * @throws ParseException        if the DDS parser returned an error
 * @throws DDSException          on an error constructing the DDS
 * @throws DAP2Exception         if any error returned by the remote server
 * @opendap.ddx.experimental
 *
public DataDDS getDDXData(String CE, StatusUI statusUI) throws MalformedURLException, IOException,
ParseException, DDSException, DAP2Exception {

return getDDXData(CE, statusUI, new DefaultFactory());
} */


    /**
     * Return the data object with no local constraint expression.  Same as
     * <code>getData("", statusUI)</code>.
     *
     * @param statusUI the <code>StatusUI</code> object to use for GUI updates
     *                 and user cancellation notification (may be null).
     * @return The <code>DataDDS</code> object that results from applying
     * this object's sticky CE, if any, on the referenced dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if any error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if any error returned by the remote server
     */
    public final DataDDS getData(StatusUI statusUI) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception
    {
        return getData("", statusUI, new DefaultFactory());
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
 * <p/>
 * Note that if CE is an empty String then the entire dataset will be
 * returned, unless a "sticky" CE has been specified in the constructor.
 * <p/>
 * <p/>
 * This method uses the 2 step method for aquiring data from a server using
 * a DDX and a BLOB. First, a DDX (an XML representation of a DDS) is requested.
 * The DDX is parsed and a DataDDS is created.
 * The DDX contains a URL that points to the servers BLOB service. The BLOB
 * service returns only the serialized binary content of the DataDDS. The DataDDS
 * then deserializes the BLOB and fills itself with data.
 *
 * @param statusUI the <code>StatusUI</code> object to use for GUI updates
 *                 and user cancellation notification (may be null).
 * @return The <code>DataDDS</code> object that results from applying the
 *         given CE, combined with this object's sticky CE, on the referenced
 *         dataset.
 * @throws MalformedURLException if the URL given to the constructor
 *                               has an error
 * @throws IOException           if any error connecting to the remote server
 * @throws ParseException        if the DDS parser returned an error
 * @throws DDSException          on an error constructing the DDS
 * @throws DAP2Exception         if any error returned by the remote server
 * @opendap.ddx.experimental
 *
public final DataDDS getDDXData(StatusUI statusUI) throws MalformedURLException, IOException,
ParseException, DDSException, DAP2Exception {
return getDDXData("", statusUI, new DefaultFactory());
} */


    /**
     * @param args command line arguments
     */
    public static void main(String[] args)
    {
/*
  DConnect2 dc;

  for (String url : args) {
    try {
      LogStream.out.println("");
      LogStream.out.println("");
      LogStream.out.println("########################################################");
      LogStream.out.println("\nConnecting to " + url + "\n");
      dc = new DConnect2(url);

      LogStream.out.println("\n- - - - - - - - - - - - - - - - - - -");

      LogStream.out.println("Retrieving DDS:\n");
      DDS dds = dc.getDDS();
      dds.print(LogStream.out);

      LogStream.out.println("\n- - - - - - - - - - - - - - - - - - -");
      LogStream.out.println("Retrieving DAS:\n");
      DAS das = dc.getDAS();
      das.print(LogStream.out);

      LogStream.out.println("\n- - - - - - - - - - - - - - - - - - -");
      LogStream.out.println("Retrieving DATA:\n");
      dds = dc.getData("");
      dds.printVal(LogStream.out, "");


      LogStream.out.println("\n- - - - - - - - - - - - - - - - - - -");
      LogStream.out.println("Retrieving DDX:\n");
      dds = dc.getDDX();
      dds.printXML(LogStream.out);

      LogStream.out.logflush();
    }
    catch (Throwable t) {
      t.printStackTrace(LogStream.err);
    }     */

    }

}



