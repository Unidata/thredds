/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.DataDataset;
import dap4.core.dmr.*;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.util.*;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.xml.sax.SAXException;
import ucar.httpclient.HTTPFactory;
import ucar.httpclient.HTTPMethod;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * DAP4 Serial to DSP
 * This code should be completely free of references to ucar.*
 * but not necessarily of org.apache.http.*
 */

public class D4DSP extends AbstractDSP
{

    //////////////////////////////////////////////////
    // Constants

    static protected final boolean PARSEDEBUG = false;

    static protected final boolean DEBUG = false;

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
        "dmr", "databuffer", "dds", "das", "ddx", "dods"
    };

    static protected final String[] DAP4EXTENSIONS = new String[]{
        "dmr", "databuffer"
    };

    //////////////////////////////////////////////////
    // Type Decls

    /**
     * Store compilation of information about
     * a request for meta-databuffer or databuffer from a client
     * and some info from the response
     */

    static protected class State
    {
        public RequestMode requestmode = null;          // request
        public ChecksumMode checksummode = null; // request

        public int status = HttpStatus.SC_OK;               // response
        public boolean bigendian = false;        // response

        public State()
        {
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected boolean isfile = false;
    protected D4DataDataset d4data = null; // root of the DataXXX tree
    protected boolean allowCompression = true;
    protected int preloadsize = DFALTPRELOADSIZE;
    protected String basece; // the constraint(s) from the original url
    protected ByteBuffer databuffer = null; // Complete serialized binary databuffer

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4DSP()
    {
        super();
    }

    public D4DSP(String path, DapContext cxt)
        throws DapException
    {
        super(path, cxt);
    }

    public D4DSP(String path)
        throws DapException
    {
        super(path, null);
    }

    //////////////////////////////////////////////////
    // DSP API
    // Note that none of these is static because I want
    // to enforce an API on all instances. Just a limitation
    // of java (sigh!).

    public D4DSP open(String path) throws DapException
    {
        return this.open(path, null);
    }

    @Override
    public D4DSP open(String path, DapContext context) throws DapException
    {
        // See if this is a local vs remote request
        try {
            this.xuri = new XURI(path);
            this.isfile = this.xuri.getBaseProtocol().equals(FILEPROTO);
            build();
            return this;
        } catch (URISyntaxException use) {
            throw new DapException(use);
        }
    }

    /**
     * A path is a DAP4 path if at least one of the following is true.
     * 1. it has "dap4:" as its leading protocol
     * 2. it has #protocol=dap4 in its fragment
     *
     * @param path
     * @param context Any parameters that may help to decide (like a File object).
     * @return true if this path appears to be processible by this DSP
     */
    @Override
    public boolean match(String path, DapContext context)
    {
        try {
            XURI xuri = new XURI(path);
            return (xuri.getLeadProtocol().equals(DAP4PROTO)
                || xuri.getParameters().get(PROTOTAG).equals(DAP4PROTO));
        } catch (URISyntaxException use) {
        }
        return false;
    }

    @Override
    public void close()
    {
    }

    @Override
    public DataDataset
    getDataDataset()
    {
        return d4data;
    }

    //////////////////////////////////////////////////
    // D4DSP specific Accessors

    public ByteBuffer getDatabuffer()
    {
        return databuffer;
    }

    public void setD4Dataset(D4DataDataset dds)
    {
        this.d4data = dds;
    }

    //////////////////////////////////////////////////

    protected D4DSP
    build()
        throws DapException
    {
        if(!isfile) {
            readDMR(); // Start by extracting and compiling DMR.
        }
        readDATA(); // read the datadmr and databuffer
        return this;
    }

    //////////////////////////////////////////////////
    // Request/Response methods

    /**
     * Open a connection and make a request for the (possibly constrained) DMR.
     *
     * @throws DapException
     */

    protected void
    readDAP(RequestMode mode)
        throws DapException
    {
        String methodurl = null;
        if(isfile) {
            mode = RequestMode.DAP; // Always DAP because the file is a serialization
        } else {
            if(mode == RequestMode.DMR)
                methodurl = buildURL(this.xuri.pureURI(), DMRSUFFIX, this.dmr, basece);
            else
                methodurl = buildURL(this.xuri.pureURI(), DATASUFFIX, this.dmr, basece);
        }
        State cxt = new State();
        cxt.requestmode = mode;
        cxt.checksummode = ChecksumMode.modeFor(xuri.getParameters().get(CHECKSUMTAG));
        if(cxt.checksummode == null) {
            cxt.checksummode = ChecksumMode.DAP;
        }

        InputStream stream;
        if(isfile) {
            // Open the file and return an input stream for accessing the databuffer
            // Should fill in context bigendian and stream fields
            File f = new File(this.xuri.getPath());
            if(!f.canRead())
                throw new DapException("File not readable: " + f);
            try {
                stream = new FileInputStream(f);
            } catch (FileNotFoundException nsfe) {
                throw new DapException("File not found: " + f);
            }
        } else {
            // Make the request and return an input stream for accessing the databuffer
            // Should fill in context bigendian and stream fields
            stream = callServer(methodurl, cxt);
        }
        // Wrap the input stream as a ChunkInputStream
        ChunkInputStream rdr = new ChunkInputStream(stream, cxt.requestmode, cxt.bigendian);
        try {
            if(cxt.requestmode == RequestMode.DAP)
                parseDATA(cxt, rdr);
            else
                parseDMR(cxt, rdr);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new DapException(t);
        } finally {
            try {
                stream.close();
            } catch (IOException ioe) {/*ignore*/}
        }
    }

    /**
     * Open a connection and make a request for the (possibly constrained) DMR.
     *
     * @throws DapException
     */

    protected void
    readDMR()
        throws DapException
    {
        readDAP(RequestMode.DMR);
    }

    /**
     * Open a connection and make a request for the constrained DATA
     * Note: for now, the whole of the databuffer is read, so any
     * secondary constraint is ignored in reading. This
     * will eventually change.
     *
     * @throws IOException
     */

    protected void
    readDATA()
        throws DapException
    {
        readDAP(RequestMode.DAP);
    }

    /**
     * Parse a straight DMR request
     *
     * @param cxt    from which to read databuffer
     * @param reader the chunk-based input stream
     * @throws DapException
     */

    public void
    parseDMR(State cxt, ChunkInputStream reader)
        throws IOException
    {
        // Process the DMR
        String text = reader.readDMR();
        if(DEBUG) {
            System.err.println("DapNetcdfFile: DMR:");
            System.err.println(text);
        }
        Dap4Parser pushparser
            = new Dap4Parser(new DapFactoryDMR());
        if(PARSEDEBUG)
            pushparser.setDebugLevel(1);
        try {
            if(!pushparser.parse(text))
                throw new DapException("DMR Parse failed");
        } catch (SAXException se) {
            throw new DapException("DMR Parse failed", se);
        }
        this.dmr = pushparser.getDMR();
        ErrorResponse err = pushparser.getErrorResponse();
        if(err != null)
            throw err.buildException();
        if(this.dmr == null)
            throw new DapException("DMR Parse failed: no DMR created");
    }

    /**
     * Extract and "compile" the databuffer section of the server response
     *
     * @param cxt    from which to read databuffer
     * @param reader the chunk-based input stream
     * @throws DapException
     */

    public void
    parseDATA(State cxt, ChunkInputStream reader)
        throws IOException
    {
        // Parse the DataDMR
        parseDMR(cxt, reader);
        // Extract all the remaining databuffer into a bytebuffer
        byte[] bytes = DapUtil.readbinaryfile(reader);
        this.databuffer = ByteBuffer.wrap(bytes).order(reader.getByteOrder());
        DataCompiler compiler = new DataCompiler(this, cxt.checksummode, this.databuffer);
        compiler.compile();
    }

    protected InputStream
    callServer(String methodurl, State context)
        throws DapException
    {
        URL url;

        try {
            url = new URL(methodurl);
        } catch (MalformedURLException mue) {
            throw new DapException("Malformed url: " + methodurl);
        }

        HTTPMethod method = null;

        long start = System.currentTimeMillis();
        long stop = 0;
        try {
            context.status = 0;

            method = HTTPFactory.Get(methodurl);
            if(allowCompression)
                method.setRequestHeader("Accept-Encoding", "deflate,gzip");

            context.status = method.execute();

            if(context.status != HttpStatus.SC_OK) {
                String msg = method.getResponseAsString();
                throw new DapException("Request failure: " + method.getStatusText() + ": " + methodurl)
                    .setCode(context.status);
            }

            if(false) {// not required by spec
                // Pull headers of interest
                if(context.requestmode == RequestMode.DAP || context.requestmode == RequestMode.DMR) {
                    Header encodingheader = method.getResponseHeader("Content-Encoding");
                    String byteorder = (encodingheader != null ? encodingheader.getValue() : null);
                    if(byteorder == null || (
                        !byteorder.equals("Big-Endian")
                            && !byteorder.equals("Little-Endian")))
                        throw new DapException("Missing or ill-formed Content-Encoding header");
                    context.bigendian = byteorder.equals("Big-Endian");
                }
            }

            // Get the response body stream => do not close the method
            return method.getResponseAsStream();

        } catch (Exception e) {
            method.close();
            throw new DapException(e);
        } finally {
            stop = System.currentTimeMillis();
        }
    }

    /**
     * Provide a method for getting the capabilities document.
     *
     * @param url        for accessing the document
     * @throws DapException
     */

    public String
    getCapabilities(String url)
        throws IOException
    {
        // Save the original url
        String saveurl = this.xuri.getOriginal();
        setPath(url);
        String fdsurl = buildURL(this.xuri.pureURI(), DSRSUFFIX, null, null);
        long start = System.currentTimeMillis();
        State cxt = new State();
        cxt.requestmode = RequestMode.CAPABILITIES;
        try {
            // Make the request and return an input stream for accessing the databuffer
            // Should fill in context bigendian and stream fields
            InputStream stream = callServer(fdsurl, cxt);
            // read the result, convert to string and return.
            byte[] bytes = DapUtil.readbinaryfile(stream);
            String document = new String(bytes, DapUtil.UTF8);
            return document;
        } finally {
            setPath(saveurl);
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


}
