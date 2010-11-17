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

import java.net.*;
import java.io.*;

import opendap.dap.parser.ParseException;

import java.util.zip.InflaterInputStream;

/**
 * This class provides support for common OPeNDAP client-side operations such as
 * dereferencing a OPeNDAP URL, communicating network activity status
 * to the user and reading local OPeNDAP objects.
 * <p/>
 * Unlike its C++ counterpart, this class does not store instances of the DAS,
 * DDS, etc. objects. Rather, the methods <code>getDAS</code>, etc. return
 * instances of those objects.
 *
 * @author jehamby
 * @version $Revision: 22595 $
 */
public class DConnect {
    private boolean dumpStream = false, dumpDAS = false;

    /**
     * InputStream to use for connection to a file instead of a remote host.
     */
    private InputStream fileStream;

    /**
     * The last URLConnection used to communicate with the OPeNDAP server.
     * Note: This is not an HttpURLConnection because file: URL's are allowed.
     * Theoretically, one could    also run OPeNDAP over FTP, NFS, or any other
     * protocol, although this is probably not very useful.
     */
    private URLConnection connection;

    /**
     * The current OPeNDAP URL, as a String (will be converted to URL inside of
     * getDAS(), getDDS(), and getData()), without Constraint Expression.
     */
    private String urlString;

    /**
     * The projection portion of the current OPeNDAP CE (including leading "?").
     */
    private String projString;

    /**
     * The selection portion of the current OPeNDAP CE (including leading "&").
     */
    private String selString;

    /**
     * Whether to accept compressed documents.
     */
    private boolean acceptDeflate;

    /**
     * The OPeNDAP server version.
     */
    private ServerVersion ver;



    public void setServerVersion(int major, int minor) {
        System.out.println("ServerVersion made with int,int: " + major + "," + minor);

        ver = new ServerVersion(major, minor);
        System.out.println("ServerVersion.getMajor(): " + ver.getMajor());
        System.out.println("ServerVersion.getMinor(): " + ver.getMinor());
    }


    /**
     * Creates an instance bound to url which accepts compressed documents.
     *
     * @param urlString connect to this URL.
     * @throws FileNotFoundException thrown if <code>urlString</code> is not
     *                               a valid URL, or a filename which exists on the system.
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
     * @param urlString     Connect to this URL.  If urlString is not a valid URL,
     *                      it is assumed to be a filename, which is opened.
     * @param acceptDeflate true if this client can accept responses encoded
     *                      with deflate.
     * @throws FileNotFoundException thrown if <code>urlString</code> is not
     *                               a valid URL, or a filename which exists on the system.
     */
    public DConnect(String urlString, boolean acceptDeflate) throws FileNotFoundException {
        int ceIndex = urlString.indexOf('?');
        if (ceIndex != -1) {
            this.urlString = urlString.substring(0, ceIndex);
            String expr = urlString.substring(ceIndex);
            int selIndex = expr.indexOf('&');
            if (selIndex != -1) {
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
        } catch (MalformedURLException e) {
            fileStream = new FileInputStream(urlString);
        }
    }

    /**
     * Creates an instance bound to an already open <code>InputStream</code>.
     *
     * @param is the <code>InputStream</code> to open.
     */
    public DConnect(InputStream is) {
        this.fileStream = is;
    }

    /**
     * Returns whether a file name or <code>InputStream</code> is being used
     * instead of a URL.
     *
     * @return true if a file name or <code>InputStream</code> is being used.
     */
    public final boolean isLocal() {
        return (fileStream != null);
    }

    /**
     * Returns the constraint expression supplied with the URL given to the
     * constructor. If no CE was given this returns an empty <code>String</code>.
     * <p/>
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
     * Open a connection to the OPeNDAP server.
     *
     * @param url the URL to open.
     * @return the opened <code>InputStream</code>.
     * @throws IOException   if an IO exception occurred.
     * @throws DAP2Exception if the OPeNDAP server returned an error.
     */
    private InputStream openConnection(URL url) throws IOException, DAP2Exception {
        connection = url.openConnection();
        if (acceptDeflate)
            connection.setRequestProperty("Accept-Encoding", "deflate");

        connection.connect();

        InputStream is = null;
        try {
            is = connection.getInputStream(); // get the HTTP InputStream
        } catch (NullPointerException e) {
            throw new DAP2Exception("Connection cannot be opened; connection is null");
        } catch (FileNotFoundException e) {
            throw new DAP2Exception("Connection cannot be opened; file not found");
        }
        if (is == null)
            throw new DAP2Exception("Unable to open input stream");

        // check headers
        String type = connection.getHeaderField("content-description");
        // LOOK - debug
        // System.err.println("Content Description: " + type);
        // this.dumpBytes(is, 100);
        // System.err.println();

        handleContentDesc(is, type);

        ver = new ServerVersion(connection);

        //System.err.println("Server: " + ver + ": " +
        //                    ver.getMajor() + "," + ver.getMinor());

        String encoding = connection.getContentEncoding();
        //System.err.println("Content Encoding: " + encoding);
        return handleContentEncoding(is, encoding);
    }

    /**
     * Returns the DAS object from the dataset referenced by this object's URL.
     * The DAS object is referred to by appending `.das' to the end of a OPeNDAP
     * URL.
     *
     * @return the DAS associated with the referenced dataset.
     * @throws MalformedURLException if the URL given to the
     *                               constructor has an error
     * @throws IOException           if an error connecting to the remote server
     * @throws ParseException        if the DAS parser returned an error
     * @throws DASException          on an error constructing the DAS
     * @throws DAP2Exception         if an error returned by the remote server
     */
    public DAS getDAS() throws MalformedURLException, IOException,
            ParseException, DASException, DAP2Exception {
        InputStream is;
        if (fileStream != null){
            is = parseMime(fileStream);
        } else {
            URL url = new URL(urlString + ".das" + projString + selString);
            if (dumpDAS) {
                System.out.println("--DConnect.getDAS to " + url);
                copy(url.openStream(), System.out);
                System.out.println("\n--DConnect.getDAS END1");
                dumpBytes(url.openStream(), 100);
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
                ((HttpURLConnection) connection).disconnect();
        }
        return das;
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
    public DDS getDDS() throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {

        return (getDDS(""));

    }


    /**
     * Use some sense when assembling the CE. Since this DConnect
     * object may have constructed using a CE, any new CE will
     * have to be integrated into it for subsequent requests.
     * Try to do this in a sensible manner!
     *
     * @param CE The new CE from the client.
     * @return The complete CE (the one this object was built
     *         with integrated with the clients)
     */
    private String getCompleteCE(String CE) {


        String localProjString, localSelString;
        int selIndex = CE.indexOf('&');
        if (selIndex != -1) {

            if (CE.indexOf('?') == 0)
                localProjString = CE.substring(1, selIndex);
            else
                localProjString = CE.substring(0, selIndex);

            localSelString = CE.substring(selIndex);
        } else {
            if (CE.indexOf('?') == 0)
                localProjString = CE.substring(1);
            else
                localProjString = CE;
            localSelString = "";
        }

        String ce = projString;

        if (!localProjString.equals("")) {
            if (!ce.equals("") && localProjString.indexOf(',') != 0)
                ce += ",";
            ce += localProjString;
        }

        if (!selString.equals("")) {
            if (selString.indexOf('&') != 0)
                ce += "&";
            ce += selString;
        }

        if (!localSelString.equals("")) {
            if (localSelString.indexOf('&') != 0)
                ce += "&";
            ce += localSelString;
        }

        if (ce.indexOf('?') != 0) {
            ce = "?" + ce;
        }

        if (false) {
            System.out.println("projString: '" + projString + "'");
            System.out.println("localProjString: '" + localProjString + "'");
            System.out.println("selString: '" + selString + "'");
            System.out.println("localSelString: '" + localSelString + "'");
            System.out.println("Complete CE: " + ce);
        }
        return (ce);

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
    public DDS getDDS(String CE) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {
        InputStream is;
        if (fileStream != null){
            is = parseMime(fileStream);
        } else {

            URL url = new URL(urlString + ".dds" + getCompleteCE(CE));

            is = openConnection(url);
            //System.out.println("Opened DDS URL: "+url);
        }
        DDS dds = new DDS();
        try {
            dds.parse(is);
        } finally {
            is.close();  // stream is always closed even if parse() throws exception
            if (connection instanceof HttpURLConnection)
                ((HttpURLConnection) connection).disconnect();
        }
        return dds;
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
    public DDS getDDX() throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {

        return (getDDX(""));
    }


    /**
     * Returns the DDS object from the dataset referenced by this object's URL.
     * The DDS object is referred to by appending `.ddx' to the end of a OPeNDAP
     * URL. The server should send back a DDX (A DDS in XML format) which
     * will get parsed here (locally) and a new DDS instantiated using
     * the DDSXMLParser.
     *
     * @param CE
     * @return the DDS associated with the referenced dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws MalformedURLException
     * @throws IOException           if an error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if an error returned by the remote server
     * @opendap.ddx.experimental
     */
    public DDS getDDX(String CE) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {

        InputStream is;

        URL url = new URL(urlString + ".ddx" + getCompleteCE(CE));

        if (fileStream != null){
            is = parseMime(fileStream);
        } else {
            is = openConnection(url);
            System.out.println("Opened DDX URL: " + url);
        }

        DDS dds = new DDS();
        try {
            dds.parseXML(is, false);
        } finally {
            is.close();  // stream is always closed even if parse() throws exception
            if (connection instanceof HttpURLConnection)
                ((HttpURLConnection) connection).disconnect();
        }
        return dds;
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
            ParseException, DDSException, DAP2Exception {

        return (getDataDDX("", new DefaultFactory()));
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
            ParseException, DDSException, DAP2Exception {

        return (getDataDDX(CE, new DefaultFactory()));
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
     * @see BaseTypeFactory
     * @opendap.ddx.experimental
     */
    public DataDDS getDataDDX(String CE, BaseTypeFactory btf) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {

        InputStream is;

        URL url = new URL(urlString + ".ddx" + getCompleteCE(CE));

        if (fileStream != null) {
            is = parseMime(fileStream);
        } else {
            is = openConnection(url);

            System.out.println("Opened DataDDX URL: " + url);
        }

        DataDDS dds = new DataDDS(ver, btf);
        try {
            dds.parseXML(is, false);
        } finally {
            is.close();  // stream is always closed even if parse() throws exception
            if (connection instanceof HttpURLConnection)
                ((HttpURLConnection) connection).disconnect();
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
     * <p/>
     * Note that if CE is an empty String then the entire dataset will be
     * returned, unless a "sticky" CE has been specified in the constructor.
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
     */
    public DataDDS getData(String CE, StatusUI statusUI, BaseTypeFactory btf) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {


        if (fileStream != null)
            return getDataFromFileStream(fileStream, statusUI, btf);

        URL url = new URL(urlString + ".dods" + getCompleteCE(CE));

        String errorMsg = "DConnect getData failed. URL: " + url;
        int errorCode = opendap.dap.DAP2Exception.UNKNOWN_ERROR;
        try {
            return getDataFromUrl(url, statusUI, btf);
        } catch (DAP2Exception e) {
            errorMsg = e.getErrorMessage();
            errorCode = e.getErrorCode();
            return null;
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
     */
    public DataDDS getDDXData(String CE, StatusUI statusUI, BaseTypeFactory btf) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {

        if (fileStream != null)
            throw new MalformedURLException("Cannont read DDX data from a file. " +
                    "The DDX client/server interaction " +
                    "requires 2 request/response pairs. " +
                    "File based input is not currently supported.");


        URL url = new URL(urlString + ".ddx" + getCompleteCE(CE));


        String errorMsg = "DConnect getDDXData failed. URL: " + url;
        int errorCode = opendap.dap.DAP2Exception.UNKNOWN_ERROR;
        int retry = 1;
        long backoff = 100L;
        while (true) {
            try {
                return getDDXDataFromURL(url, statusUI, btf);
            } catch (DAP2Exception e) {
                System.out.println("DConnect getData failed; retry (" + retry + "," + backoff + ") " + url);
                errorMsg = e.getErrorMessage();
                errorCode = e.getErrorCode();

                try {
                    Thread.currentThread().sleep(backoff);
                } catch (InterruptedException ie) {
                }
            }

            if (retry == 5)
                throw new DAP2Exception(errorCode, errorMsg);
            retry++;
            backoff *= 2;
        }
    }


    private DataDDS getDataFromFileStream(InputStream fileStream, StatusUI statusUI, BaseTypeFactory btf) throws IOException,
            ParseException, DDSException, DAP2Exception {

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
     */
    public void getBlobData(DataDDS dds, StatusUI statusUI)
            throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {


        boolean dumpStreamErr = false; // opendap.util.Debug.isSet("dumpStreamErr");

        System.out.println("dds.getBlobURL(): " + dds.getBlobContentID());

        if (dds.getBlobContentID() == null) {
            throw new MalformedURLException("Blob URL was 'null'. " +
                    "This may indicate that this OPeNDAP Server does not support the full use of DDX.");
        }


        URL blobURL = new URL(dds.getBlobContentID());

        System.out.println("Opening BLOB URL: " + blobURL);

        InputStream is = openConnection(blobURL);

        // - - - - -  DEBUG - - - - - -
        ByteArrayInputStream bis = null;
        if (dumpStream) {
            System.out.println("DConnect to " + blobURL);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(is, bos);
            bis = new ByteArrayInputStream(bos.toByteArray());
            is = bis;
        }
        if (dumpStreamErr) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(is, bos);
            bis = new ByteArrayInputStream(bos.toByteArray());
            is = bis;
        }
        // - - - - - - - - - - - - - - -

        try {

            dds.readData(is, statusUI); // read the data!

        } catch (Throwable e) {
            System.out.println("DConnect dds.readData problem with: " + blobURL + "\nStack Trace:");
            e.printStackTrace(System.out);

            // - - - - -  DEBUG - - - - - -
            if (dumpStreamErr) {
                System.out.println("DConnect dump " + blobURL);
                bis.reset();
                dump(bis);
            }
            // - - - - - - - - - - - - - - -

            throw new DAP2Exception("Connection problem when reading: " + blobURL + "\n" +
                    "Error Message - " + e.toString());

        } finally {
            is.close();  // stream is always closed even if parse() throws exception
            if (connection instanceof HttpURLConnection)
                ((HttpURLConnection) connection).disconnect();
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
     */
    public DataDDS getDDXDataFromURL(URL url, StatusUI statusUI, BaseTypeFactory btf)
            throws  IOException,
            ParseException, DDSException, DAP2Exception {

        System.out.println("Opening DDX URL: " + url);
        InputStream is = openConnection(url);
        DataDDS dds = new DataDDS(ver, btf);

        boolean dumpStreamErr = false; // opendap.util.Debug.isSet("dumpStreamErr");

        // - - - - -  DEBUG - - - - - -
        ByteArrayInputStream bis = null;
        if (dumpStream) {
            System.out.println("DConnect to " + url);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(is, bos);
            bis = new ByteArrayInputStream(bos.toByteArray());
            is = bis;
        }
        if (dumpStreamErr) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(is, bos);
            bis = new ByteArrayInputStream(bos.toByteArray());
            is = bis;
        }
        // - - - - - - - - - - - - - - -

        try {

            // - - - - -  DEBUG - - - - - -
            if (dumpStream) {
                bis.mark(1000);
                System.out.println("DConnect parse header: ");
                dump(bis);
                bis.reset();
            }
            // - - - - - - - - - - - - - - -

            dds.parseXML(is, false);    // read the DDX

            //dds.parse(new HeaderInputStream(is));    // read the DDS header
            // NOTE: the HeaderInputStream will have skipped over "Data:" line

            // - - - - -  DEBUG - - - - - -
            if (dumpStream) {
                bis.mark(20);
                System.out.println("DConnect done with header, next bytes are: ");
                dumpBytes(bis, 20);
                bis.reset();
            }
            // - - - - - - - - - - - - - - -

        } catch (Throwable e) {
            System.out.println("DConnect ddx.parse problem with: " + url + "\nStack Trace:");
            e.printStackTrace(System.out);

            // - - - - -  DEBUG - - - - - -
            if (dumpStreamErr) {
                System.out.println("DConnect dump " + url);
                bis.reset();
                dump(bis);
            }
            // - - - - - - - - - - - - - - -

            throw new DAP2Exception("Connection problem when reading: " + url + "\n" +
                    "Error Message - " + e.toString());

        } finally {
            is.close();  // stream is always closed even if parse() throws exception
            if (connection instanceof HttpURLConnection)
                ((HttpURLConnection) connection).disconnect();
        }


        getBlobData(dds, statusUI);


        return dds;
    }


    public DataDDS getDataFromUrl(URL url, StatusUI statusUI, BaseTypeFactory btf) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {

        InputStream is = openConnection(url);
        DataDDS dds = new DataDDS(ver, btf);

        boolean dumpStreamErr = false; // opendap.util.Debug.isSet("dumpStreamErr");

        // DEBUG
        ByteArrayInputStream bis = null;
        if (dumpStream) {
            System.out.println("DConnect to " + url);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(is, bos);
            bis = new ByteArrayInputStream(bos.toByteArray());
            is = bis;
        }
        if (dumpStreamErr) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(is, bos);
            bis = new ByteArrayInputStream(bos.toByteArray());
            is = bis;
        }

        String operation = "";
        try {

            if (dumpStream) {
                bis.mark(1000);
                System.out.println("DConnect parse header: ");
                dump(bis);
                bis.reset();
            }
            operation = "dds.parse";
            dds.parse(new HeaderInputStream(is));    // read the DDS header
            // NOTE: the HeaderInputStream will have skipped over "Data:" line

            if (dumpStream) {
                bis.mark(20);
                System.out.println("DConnect done with header, next bytes are: ");
                dumpBytes(bis, 20);
                bis.reset();
            }

            operation = "dds.readData";
            dds.readData(is, statusUI); // read the data!

        } catch (Throwable e) {

            System.out.println("DConnect " + operation + " problem with: " + url + "\nStack Trace:");
            e.printStackTrace(System.out);

            // DEBUG
            if (dumpStreamErr) {
                System.out.println("DConnect dump " + url);
                bis.reset();
                dump(bis);
                /* bis.reset();
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
                } */
            }

            throw new DAP2Exception("Connection problem when reading: " + url + "\n" +
                    "Error Message - " + e.toString());

        } finally {
            is.close();  // stream is always closed even if parse() throws exception
            if (connection instanceof HttpURLConnection)
                ((HttpURLConnection) connection).disconnect();
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
        } catch (IOException e) {
            // Don't bother to do a thing
        }
    }


    // DEBUG JC
    private void dump(InputStream is) throws IOException {
        DataInputStream d0 = new DataInputStream(is);
        BufferedReader d = new BufferedReader(new InputStreamReader(d0));

        try {
            System.out.println("dump lines avail=" + is.available());
            while (true) {
                String line = d.readLine();
                System.out.println(line);
                if (null == line) return;
                if (line.equals("Data:")) break;
            }
            System.out.println("dump bytes avail=" + is.available());
            dumpBytes(is, 20);

        } catch (java.io.EOFException e) {
            // Don't bother to do a thing
        }
    }

    private void dumpBytes(InputStream is, int n) {
        try {
            DataInputStream d = new DataInputStream(is);
            int count = 0;
            byte[] buff = new byte[1];
            while ((count < n) && (d.available() > 0)) {
                buff[0] = d.readByte();
                System.out.println(count + " " + buff[0] + " = " + (new String(buff)));
                count++;
            }
        } catch (java.io.IOException e) {
            // Don't bother to do a thing
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
     * <p/>
     * Note that if CE is an empty String then the entire dataset will be
     * returned, unless a "sticky" CE has been specified in the constructor.
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
     */
    public DataDDS getData(String CE, StatusUI statusUI) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {

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
     */
    public DataDDS getDDXData(String CE, StatusUI statusUI) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {

        return getDDXData(CE, statusUI, new DefaultFactory());
    }


    /**
     * Return the data object with no local constraint expression.  Same as
     * <code>getData("", statusUI)</code>.
     *
     * @param statusUI the <code>StatusUI</code> object to use for GUI updates
     *                 and user cancellation notification (may be null).
     * @return The <code>DataDDS</code> object that results from applying
     *         this object's sticky CE, if any, on the referenced dataset.
     * @throws MalformedURLException if the URL given to the constructor
     *                               has an error
     * @throws IOException           if any error connecting to the remote server
     * @throws ParseException        if the DDS parser returned an error
     * @throws DDSException          on an error constructing the DDS
     * @throws DAP2Exception         if any error returned by the remote server
     * @see DConnect#getData(String, StatusUI,BaseTypeFactory)
     */
    public final DataDDS getData(StatusUI statusUI) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {
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
     */
    public final DataDDS getDDXData(StatusUI statusUI) throws MalformedURLException, IOException,
            ParseException, DDSException, DAP2Exception {
        return getDDXData("", statusUI, new DefaultFactory());
    }

    /**
     * Returns the <code>ServerVersion</code> of the last connection.
     *
     * @return the <code>ServerVersion</code> of the last connection.
     */
    public final ServerVersion getServerVersion() {
        return ver;
    }

    /**
     * A primitive parser for the MIME headers used by OPeNDAP.  This is used when
     * reading from local sources of OPeNDAP Data objects. It is called by
     * <code>readData</code> to simulate the important actions of the
     * <code>URLConnection</code> MIME header parsing performed in
     * <code>openConnection</code> for HTTP URL's.
     * <p/>
     * <b><i>NOTE:</b></i> Because BufferedReader seeks ahead, and therefore
     * removescharacters from the InputStream which are needed later, and
     * because there is no way to construct an InputStream from a
     * BufferedReader, we have to use DataInputStream to read the header
     * lines, which triggers an unavoidable deprecated warning from the
     * Java compiler.
     *
     * @param is the InputStream to read.
     * @return the InputStream to read data from (after attaching any
     *         necessary decompression filters).
     * @throws IOException   if any IO error.
     * @throws DAP2Exception if the server returned an Error.
     */
    private InputStream parseMime(InputStream is)
            throws IOException, DAP2Exception {

        // NOTE: because BufferedReader seeks ahead, and therefore removes
        // characters from the InputStream which are needed later, and
        // because there is no way to construct an InputStream from a
        // BufferedReader, we have to use DataInputStream to read the header
        // lines, which triggers an unavoidable deprecated warning from the
        // Java compiler.

        DataInputStream d0 = new DataInputStream(is);
        BufferedReader d = new BufferedReader(new InputStreamReader(d0));

        String description = null;
        String encoding = null;

        ver = null;
        // while there are more header (non-blank) lines
        String line;
        while (!(line = d.readLine()).equals("")) {
            int spaceIndex = line.indexOf(' ');
            // all header lines should have a space in them, but if not, skip ahead
            if (spaceIndex == -1)
                continue;
            String header = line.substring(0, spaceIndex);
            String value = line.substring(spaceIndex + 1);

            if (header.equals("XDAP:")) {
                ver = new ServerVersion(value,ServerVersion.XDAP);
            }else if (header.equals("XDODS-Server:") && ver != null ) {
                ver = new ServerVersion(value,ServerVersion.XDODS_SERVER);
            } else if (header.equals("Content-Description:")) {
                description = value;
            } else if (header.equals("Content-Encoding:")) {
                encoding = value;
            }
        }
        handleContentDesc(is, description);
        return handleContentEncoding(is, encoding);
    }

    /**
     * This code handles the Content-Description: header for
     * <code>openConnection</code> and <code>parseMime</code>.
     * Throws a <code>DAP2Exception</code> if the type is
     * <code>dods_error</code>.
     *
     * @param is   the InputStream to read.
     * @param type the Content-Description header, or null.
     * @throws IOException   if any error reading from the server.
     * @throws DAP2Exception if the server returned an error.
     */
    private void handleContentDesc(InputStream is, String type)
            throws IOException, DAP2Exception {
      if (type != null && (type.equals("dods-error") || type.equals("dods_error"))) {
            // create server exception object
            DAP2Exception ds = new DAP2Exception();
            // parse the Error object from stream and throw it
            ds.parse(is);
            throw ds;
        }
    }

    /**
     * This code handles the Content-type: header for
     * <code>openConnection</code> and <code>parseMime</code>
     *
     * @param is       the InputStream to read.
     * @param encoding the Content-type header, or null.
     * @return the new InputStream, after applying an
     *         <code>InflaterInputStream</code> filter if necessary.
     */
    private InputStream handleContentEncoding(InputStream is, String encoding) {
        if (encoding != null && encoding.equals("deflate")) {
            return new InflaterInputStream(is);
        } else {
            return is;
        }
    }
}


