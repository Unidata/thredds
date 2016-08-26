/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.core.TdsRequestedDataset;
import thredds.server.config.TdsContext;
import thredds.util.ContentType;
import ucar.httpservices.HTTPUtil;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.nc2.iosp.netcdf3.N3header;
import ucar.nc2.util.CancelTaskImpl;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.unidata.io.RandomAccessFile;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static thredds.server.reify.ReifyUtils.SendError;


/**
 * Local File Materialization (aka reification)
 * for server-side computing.
 * The tag for this controller is .../download/*
 * <p>
 * The goal for this controller is to allow external code to have the
 * thredds server materialize a dataset into the file system.
 * <p>
 * Note that this functionality is not strictly necessary
 * since it could all be done on the client side.
 * Since the server is doing all of the work of converting
 * a dataset to a file, the client does not need to have
 * its own code for this. This means:
 * 1. It is lightweight WRT the client
 * 2. It is language independent
 * <p>
 * Assumptions:
 * 1. The external code has authenticated to the thredds server
 * using some authentication mechanism such as
 * client-side certificates or tomcat roles.
 * <p>
 * 2. The external code is running on the same machine as the thredds
 * server, or at least has a common file system so that file system
 * writes by thredds are visible to the external code.
 * <p>
 * Note that in order to support downloading of non-file datasets
 * (e.g. via DAP2 or DAP4) this controller will
 * re-call the server to obtain the output of the request. Experimentation
 * shows this is not a problem. Circularity is tested to ensure
 * the no download loop occurs.
 * <p>
 * Issues:
 * 1. What file formats are allowed for materialized datasets?
 * - Currently only netcdf-3 (classic) and netcdf-4 (enhanced).
 * <p>
 * A set of query parameters control the operation of this servlet.
 * Note that all of the query parameter values (but not keys) are
 * assumed to be url-encoded (%xx), so beware.
 * Also, all return values are url-encoded.
 * <p>
 * Download Request
 * ----------------
 * Download parameters:
 * - request=download -- specify the action
 * - format=netcdf3|netcdfd4 -- specify the download format
 * - url={a thredds server dataset access url} -- specify the actual dataset.
 * - target={path of the downloaded file} -- if it already exists, then it
 * will be overwritten.
 * Notes:
 * 1. the host, port and servlet prefix of the url will be
 * ignored and replaced with the "<host>+<port>/thredds" of the thredds
 * server. This is to prevent attempts to use the thredds server to access
 * external data sources, which would otherwise provide a security leak.
 * 2. The target path must be a relative path. It is interpreted as relative
 * to the value of a -Dtds.download.dir java flag, which must specify
 * an absolute path to a directory into which the downloads are stored.
 * Assuming the Tomcat server is being used, then the path must be
 * writeable by user tomcat, which means that its owner will end up
 * being user tomcat or at least tomcat must be in the group assigned to
 * this directory.
 * <p>
 * Return value: download=<absolute path to which file was downloaded>
 * <p>
 * Misc Parameters:
 * - testinfo={testing info} -- for testing purposes
 * <p>
 * Inquire Request
 * ----------------
 * Download parameters:
 * - request=inquire -- Inquire about various parameters
 * - inquire={semicolon separated arguments} -- Inquire about the default download dir
 * <p>
 * Return value: <key>={value of the requested key}
 */

@Controller
@RequestMapping("/download")
public class ReifyController
{
    //////////////////////////////////////////////////
    // Constants

    static final protected boolean DEBUG = true;

    static final protected String DEFAULTSERVLETNAME = "thredds";
    static final protected String DEFAULTREQUESTNAME = "download";

    static final protected String DEFAULTDOWNLOADDIR = "download";

    static final protected String STATUSCODEHEADER = "x-download-code";

    static final protected String FILESERVERSERVLET = "/fileServer/";

    //////////////////////////////////////////////////
    // Static variables

    static org.slf4j.Logger logServerStartup;
    static org.slf4j.Logger log;

    static {
        logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
        log = org.slf4j.LoggerFactory.getLogger(ReifyController.class);
    }

    static public TdsContext testTdsContext = null;

    //////////////////////////////////////////////////
    // Instance variables

    @Autowired
    protected TdsContext tdsContext = null;

    protected HttpServletRequest req = null;
    protected HttpServletResponse res = null;
    protected Parameters params = null;
    protected String downloaddir = null;

    protected boolean initialized = false;
    protected boolean getInitialized = false;

    protected String server = null; // Our host + port
    protected String requestname = null;
    protected String threddsname = null;

    protected Nc4Chunking.Strategy strategy = Nc4Chunking.Strategy.standard;
    protected Nc4Chunking chunking = new Nc4ChunkingDefault();

    //////////////////////////////////////////////////
    // Constructor(s)

    public ReifyController()
            throws ServletException
    {
        // Do not know how to get spring to invoke init when mocking.
        if(!initialized) init();
    }

    //////////////////////////////////////////////////
    // Servlet API (Selected)

    public void init()
            throws ServletException
    {
        try {
            if(initialized)
                return;
            initialized = true;
            logServerStartup.info(getClass().getName() + " initialization");
            System.setProperty("file.encoding", "UTF-8");
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Invoked on first get so that everything is available,
     * especially Spring stuff.
     */
    public void initGet(HttpServletRequest req)
            throws SendError
    {
        if(getInitialized)
            return;
        getInitialized = true;
        log.info(getClass().getName() + " GET initialization");

        // Obtain servlet path info
        String tmp = HTTPUtil.canonicalpath(req.getContextPath());
        this.threddsname = HTTPUtil.relpath(tmp);
        tmp = HTTPUtil.canonicalpath(req.getServletPath());
        this.requestname = HTTPUtil.relpath(tmp);

        if(this.threddsname == null || this.threddsname.length() == 0)
            this.threddsname = DEFAULTSERVLETNAME;
        if(this.requestname == null || this.requestname.length() == 0)
            this.requestname = DEFAULTREQUESTNAME;

        // Get server host + port name
        StringBuilder buf = new StringBuilder();
        buf.append(req.getServerName());
        int port = req.getServerPort();
        if(port > 0) {
            buf.append(":");
            buf.append(port);
        }
        this.server = buf.toString();

        // Get the download dir
        File downdir = null;
        if(tdsContext == null) tdsContext = testTdsContext;
        if(tdsContext != null)
            downdir = tdsContext.getDownloadDir();
        if(downdir == null)
            throw new SendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No tds.download.dir specified");
        else
            this.downloaddir = HTTPUtil.canonicalpath(downdir.getAbsolutePath());
    }

    //////////////////////////////////////////////////
    // Controller entry point

    @RequestMapping("**")
    public void
    doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException
    {
        try {

            this.req = req;
            this.res = res;

            if(!getInitialized) initGet(req);

            try {
                this.params = new Parameters(req);
            } catch (IOException ioe) {
                throw new SendError(res.SC_BAD_REQUEST, ioe);
            }

            Map<String, String> result = new HashMap<>();
            switch (this.params.command) {
            case DOWNLOAD:
                download(result);
                break;
            case INQUIRE:
                inquire(result);
                break;
            }
            reply(result);
        } catch (SendError se) {
            sendError(se.httpcode, se.msg);
        } catch (Exception e) {
            String msg = ReifyUtils.getStackTrace(e);
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
    }

    protected void
    sendError(int code, String msg)
    {
        if(DEBUG)
            System.err.println(String.format("xxx: error: code=%d msg=%s%n", code, msg));
        try {
            //this.res.sendError(code, msg);
            this.res.setIntHeader(STATUSCODEHEADER, code);
            reply(msg);
        } catch (Exception e) {
            assert false : "Unexpected failure";
        }
    }

    protected void
    reply(Map<String, String> result)
    {
        String sresult = ReifyUtils.toString(result, true, "download");
        reply(sresult);
    }

    protected void
    reply(String sresult)
    {
        this.res.setContentType(ContentType.text.getContentHeader());
        try {
            ServletOutputStream out = this.res.getOutputStream();
            PrintStream pw = new PrintStream(out, false, "US-ASCII");
            pw.print(sresult);
            pw.close();
            out.flush();
        } catch (IOException ioe) {
            log.error(ioe.getMessage());
        }
    }

    protected void
    download(Map<String, String> result)
    {
        if(this.params.target == null)
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "No target specified");
        // Make sure target path does not contain '..'
        if(this.params.target.indexOf("..") >= 0)
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "Target parameter contains '..': " + this.params.target);
        // See if the target is relative or absolute
        File ftarget = new File(this.params.target);
        if(ftarget.isAbsolute())
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "Target parameter must be a relative path: " + this.params.target);
        // Make relative to download dir, if any
        // See if we have a download dir
        if(this.downloaddir == null)
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "No download directory specified for relative target: " + this.params.target);
        // Convert to absolute path
        StringBuilder b = new StringBuilder();
        b.append(this.downloaddir);
        b.append('/');
        b.append(this.params.target);
        ftarget = new File(b.toString());
        String fulltarget = HTTPUtil.canonicalpath(ftarget.getAbsolutePath());

        // Make sure that all intermediate directories exist
        File parent = ftarget.getParentFile();
        if(!parent.exists() && !ftarget.getParentFile().mkdirs())
            throw new SendError(HttpServletResponse.SC_FORBIDDEN, "Target file parent directory cannot be created: " + fulltarget);

        // If file exists, delete it
        if(ftarget.exists() && !ftarget.delete())
            throw new SendError(HttpServletResponse.SC_FORBIDDEN, "Target file exists and cannot be deleted: " + fulltarget);

        String url = this.params.url;
        if(url == null)
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "no source url specified");
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed source url:" + url);
        }

        // Make sure we are not recursing
        String path = uri.getPath();
        if(path.toLowerCase().indexOf(this.requestname) >= 0)
            throw new SendError(HttpServletResponse.SC_FORBIDDEN,
                    String.format("URL is recursive on /%s: %s", this.requestname, path));

        // Make sure that path starts with "/thredds"
        if(!path.startsWith("/" + this.threddsname))
            throw new SendError(HttpServletResponse.SC_FORBIDDEN,
                    String.format("URL does not reference %s: %s",
                            this.threddsname, path));

        // Rebuild the url to keep it under our control
        b.setLength(0); // reuse
        b.append(uri.getScheme());
        b.append("://");
        b.append(this.server);
        b.append(path);
        String trueurl = b.toString();

        // Get NetcdfFile object
        // Now, shortcircuit requests for fileServer case
        boolean directcopy = false;
        if(path.indexOf(FILESERVERSERVLET) >= 0) {
            String truepath = directAccess(path, this.req, this.res);
            directcopy = canCopy(truepath, this.params.format);
            if(directcopy) try {
                Path src = new File(truepath).toPath();
                Path dst = new File(fulltarget).toPath();
                Files.deleteIfExists(dst);
                Files.copy(src, dst);
            } catch (IOException e) {
                throw new SendError(HttpServletResponse.SC_FORBIDDEN, truepath);
            }
        }
        if(!directcopy) {
            NetcdfFile ncfile = null;
            try {
                CancelTaskImpl cancel = new CancelTaskImpl();
                ncfile = NetcdfDataset.openFile(trueurl, cancel);
                switch (this.params.format) {
                case NETCDF3:
                    makeNetcdf3(ncfile, fulltarget);
                    break;
                case NETCDF4:
                    makeNetcdf4(ncfile, fulltarget);
                    break;
                default:
                    throw new SendError(HttpServletResponse.SC_NOT_IMPLEMENTED,
                            String.format("%s: return format %s not implemented",
                                    path, params.format.getName()));
                }
            } catch (IOException ioe) {
                throw new SendError(res.SC_BAD_REQUEST, ioe);
            }
        }
        // Return the absolute path of the target as the content
        result.put("download", fulltarget);
    }

    protected void
    inquire(Map<String, String> result)
    {
        String s = this.params.inquire;
        List<String> keys = ReifyUtils.parseList(s, ';', true);
        for(String key : keys) {
            ReifyUtils.Inquiry inq = ReifyUtils.Inquiry.parse(key);
            if(inq == null) continue; // ignore unknown keys
            switch (inq) {
            case DOWNLOADDIR:
                result.put(inq.getKey(), downloaddir == null ? "null" : downloaddir);
                break;
            case USERNAME:
                String uname = System.getProperty("user.name");
                result.put(inq.getKey(), uname == null ? "null" : uname);
            default: //ignore
                break;
            }
        }
    }

    //////////////////////////////////////////////////
    // Reifiers

    protected void
    makeNetcdf4(NetcdfFile ncfile, String target)
            throws IOException
    {
        try {
            CancelTaskImpl cancel = new CancelTaskImpl();
            FileWriter2 writer = new FileWriter2(ncfile, target,
                    NetcdfFileWriter.Version.netcdf4,
                    chunking);
            writer.getNetcdfFileWriter().setLargeFile(true);
            NetcdfFile ncfileOut = writer.write(cancel);
            if(ncfileOut != null) ncfileOut.close();
            cancel.setDone(true);
        } catch (IOException ioe) {
            throw ioe; // temporary
        }
    }

    protected void
    makeNetcdf3(NetcdfFile ncfile, String target)
            throws IOException
    {
        try {
            CancelTaskImpl cancel = new CancelTaskImpl();
            FileWriter2 writer = new FileWriter2(ncfile, target,
                    NetcdfFileWriter.Version.netcdf3,
                    chunking);
            writer.getNetcdfFileWriter().setLargeFile(true);
            NetcdfFile ncfileOut = writer.write(cancel);
            if(ncfileOut != null) ncfileOut.close();
            cancel.setDone(true);
        } catch (IOException ioe) {
            throw ioe; // temporary
        }
    }

    //////////////////////////////////////////////////
    // Utilities
    /*
    protected File
    resolve(String relpath, int[] codep)
    {
        File file = null;
        if(this.testinfo != null) {
            for(String s : this.testdirs) {
                String path = HTTPUtil.canonjoin(s, relpath);
                File f = new File(path);
                if(f.exists()) {
                    file = f;
                    break;
                }
            }
        } else {
            relpath = HTTPUtil.relpath(HTTPUtil.canonicalpath(relpath));
            if(!TdsRequestedDataset.resourceControlOk(this.req, this.res, relpath)) {
                codep[0] = res.SC_UNAUTHORIZED;
                return null;
            }
            file = TdsRequestedDataset.getFile(relpath);
            if(file == null || !file.exists()) {
                codep[0] = res.SC_NOT_FOUND;
                return null;
            }
        }
        return file;
    } */

    /**
     * Given a typical string, insert backslashes
     * before '"' and '\\' characters and control characters.
     */
    static protected String escapeString(String s)
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            switch (c) {
            case '"':
                buf.append("\\\"");
                break;
            case '\\':
                buf.append("\\\\");
                break;
            case '\n':
                buf.append('\n');
                break;
            case '\r':
                buf.append('\r');
                break;
            case '\t':
                buf.append('\r');
                break;
            case '\f':
                buf.append('\f');
                break;
            default:
                if(c < ' ')
                    buf.append(String.format("\\x%02x", (c & 0xff)));
                else
                    buf.append((char) c);
                break;
            }
        }
        return buf.toString();
    }

    protected String
    directAccess(String relpath, HttpServletRequest req, HttpServletResponse res)
    {
        // Strip off /fileServer and everythihg before it
        int index = relpath.indexOf(FILESERVERSERVLET);
        assert index >= 0;
        relpath = relpath.substring(index + FILESERVERSERVLET.length(), relpath.length());
        relpath = HTTPUtil.abspath(HTTPUtil.canonicalpath(relpath));
        String realpath = TdsRequestedDataset.getLocationFromRequestPath(relpath);
        File f = new File(realpath);
        if(!f.exists() || !f.canRead())
            throw new SendError(res.SC_NOT_FOUND, "Not found: " + realpath);
        if(!TdsRequestedDataset.resourceControlOk(req, res, realpath))
            throw new SendError(res.SC_FORBIDDEN, "Permissions failure: " + realpath);
        return realpath;
    }


    protected boolean
    canCopy(String truepath, ReifyUtils.FileFormat targetformat)
    {
        try {
            RandomAccessFile raf = new RandomAccessFile(truepath, "r");
            // See if thhis is a netcdf-3 or netcdf-4 file already
            if(N3header.isValidFile(raf)
                    && targetformat == ReifyUtils.FileFormat.NETCDF3)
                return true;
            if(H5header.isValidFile(raf)
                    && targetformat == ReifyUtils.FileFormat.NETCDF4)
                return true;
        } catch (IOException e) {
            return false;
        }
        return false;
    }

}
