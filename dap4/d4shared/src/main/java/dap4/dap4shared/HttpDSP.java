/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.dmr.DapDataset;
import dap4.core.util.*;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Make a request to a server and convert the reply
 * to a DapDataset from the returned bytes.
 */

public class HttpDSP extends D4DSP
{

    //////////////////////////////////////////////////
    // Constants

    static protected final String DAPVERSION = "4.0";
    static protected final String DMRVERSION = "1.0";

    static protected final String DAP4PROTO = "dap4";
    static protected final String FILEPROTO = "file";

    static protected final String DMRSUFFIX = "dmr";
    static protected final String DATASUFFIX = "dap";
    static protected final String DSRSUFFIX = "dsr";

    static protected final String QUERYSTART = "?";
    static protected final String CONSTRAINTTAG = "dap4.ce";
    static protected final String CHECKSUMTAG = "checksum";
    static protected final String PROTOTAG = "protocol";

    static protected final int DFALTPRELOADSIZE = 50000; // databuffer

    static protected final String[] DAPEXTENSIONS = new String[]{
        "dmr", "dap", "dds", "das", "ddx", "dods"
    };

    static protected final String[] DAP4EXTENSIONS = new String[]{
        "dmr", "dap"
    };

    //////////////////////////////////////////////////
    // Instance variables

    protected String originalurl = null;
    protected boolean allowCompression = true;
    protected String basece = null; // the constraint(s) from the original url

    protected int status = HttpStatus.SC_OK;    // response
    protected XURI xuri = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public HttpDSP()
    {
        super();
    }

    //////////////////////////////////////////////////
    // DSP API

    /**
     * A path is a DAP4 path if at least one of the following is true.
     * 1. it has "dap4:" as its leading protocol
     * 2. it has #protocol=dap4 in its fragment
     *
     * @param url
     * @param context Any parameters that may help to decide.
     * @return true if this url appears to be processible by this DSP
     */
    static public boolean match(String url, DapContext context)
    {
        try {
            XURI xuri = new XURI(url);
            return (xuri.getLeadProtocol().equals(DAP4PROTO)
                || xuri.getParameters().get(PROTOTAG).equals(DAP4PROTO));
        } catch (URISyntaxException use) {
        }
        return false;
    }

    @Override
    public DSP open(String url, DapContext context) throws DapException
    {
        setContext(context);
        this.originalurl = url;
        // See if this is a local vs remote request
        setURL(url);
        this.basece = this.xuri.getFields().get(CONSTRAINTTAG);
        build();
        return this;

    }

    @Override
    public void close()
    {
    }

    /*@Override
    public String getPath()
    {
        return this.originalurl;
    } */

    //////////////////////////////////////////////////
    // Request/Response methods

    /**
     * Open a connection and make a request for the (possibly constrained) DMR.
     *
     * @throws DapException
     */

    protected void
    build()
        throws DapException
    {
        String methodurl = buildURL(this.xuri.assemble(XURI.URLONLY), DATASUFFIX, this.dmr, this.basece);
        this.checksummode = ChecksumMode.modeFor(xuri.getParameters().get(CHECKSUMTAG));
        if(this.checksummode == null) {
            this.checksummode = ChecksumMode.DAP;
        }

        InputStream stream;
        // Make the request and return an input stream for accessing the databuffer
        // Should fill in bigendian and stream fields
        stream = callServer(methodurl);

        try {
            ChunkInputStream reader;
            if(DEBUG) {
                byte[] raw = DapUtil.readbinaryfile(stream);
                ByteArrayInputStream bis = new ByteArrayInputStream(raw);
                DapDump.dumpbytestream(raw, order, "httpdsp.build");
                reader = new ChunkInputStream(bis, RequestMode.DAP, getOrder());
            } else {
                // Wrap the input stream as a ChunkInputStream
                reader = new ChunkInputStream(stream, RequestMode.DAP, getOrder());
            }

            // Extract and "compile" the server response
            String document = reader.readDMR();
            // Extract all the remaining bytes
            byte[] bytes = DapUtil.readbinaryfile(reader);
            // use super.build to compile
            super.build(document, bytes, getOrder());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new DapException(t);
        } finally {
            try {
                stream.close();
            } catch (IOException ioe) {/*ignore*/}
        }
    }

    protected InputStream
    callServer(String methodurl)
        throws DapException
    {
        URI uri;

        try {
            uri = HTTPUtil.parseToURI(methodurl);
        } catch (URISyntaxException mue) {
            throw new DapException("Malformed url: " + methodurl);
        }

        long start = System.currentTimeMillis();
        long stop = 0;
        this.status = 0;
        HTTPMethod method = null;
        try {   // Note that we cannot use try with resources because we export the method stream, so method
            // must not be closed.
            method = HTTPFactory.Get(methodurl);
            if(allowCompression)
                method.setCompression("deflate,gzip");

            this.status = method.execute();

            if(this.status != HttpStatus.SC_OK) {
                String msg = method.getResponseAsString();
                throw new DapException("Request failure: " + method.getStatusText() + ": " + methodurl)
                    .setCode(status);
            }
            // Pull headers of interest
            /*not legal
            Header encodingheader = method.getResponseHeader("Content-Encoding");
            String byteorder = (encodingheader != null ? encodingheader.getValue() : null);
            setOrder(byteorder.equalsIgnoreCase("Big-Endian") ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            if(getOrder() == null)
                throw new DapException("Missing or ill-formed Content-Encoding header");
            */
            // Get the response body stream => do not close the method
            return method.getResponseAsStream();

        } catch (Exception e) {
            if(method != null)
                method.close();
            throw new DapException(e);
        }
    }

    /**
     * Provide a method for getting the capabilities document.
     *
     * @param url for accessing the document
     * @throws DapException
     */

    public String
    getCapabilities(String url)
        throws IOException
    {
        // Save the original url
        String saveurl = this.xuri.getOriginal();
        setURL(url);
        String fdsurl = buildURL(this.xuri.assemble(XURI.URLALL), DSRSUFFIX, null, null);
        try {
            // Make the request and return an input stream for accessing the databuffer
            // Should fill in context bigendian and stream fields
            InputStream stream = callServer(fdsurl);
            // read the result, convert to string and return.
            byte[] bytes = DapUtil.readbinaryfile(stream);
            String document = new String(bytes, DapUtil.UTF8);
            return document;
        } finally {
            setURL(saveurl);
        }
    }

    //////////////////////////////////////////////////
    // Utilities

    static protected String
    buildURL(String baseurl, String suffix, DapDataset template, String ce)
    {
        StringBuilder methodurl = new StringBuilder();
        methodurl.append(baseurl);
        if(suffix != null) {
            methodurl.append('.');
            methodurl.append(suffix);
        }
        if(ce != null && ce.length() > 0) {
            methodurl.append(QUERYSTART);
            methodurl.append(CONSTRAINTTAG);
            methodurl.append('=');
            methodurl.append(ce);
        }
        return methodurl.toString();
    }

    protected void
    setURL(String url)
        throws DapException
    {
        try {
            this.xuri = new XURI(url);
        } catch (URISyntaxException use) {
            throw new DapException(use);
        }
    }
}
