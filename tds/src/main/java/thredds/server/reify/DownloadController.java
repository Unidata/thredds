/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;

import org.apache.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import thredds.core.TdsRequestedDataset;
import ucar.httpservices.HTTPUtil;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.NCheader;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.nc2.iosp.netcdf3.N3header;
import ucar.nc2.util.CancelTaskImpl;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.unidata.io.RandomAccessFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static  ucar.nc2.iosp.NCheader.*;

/**
 * Local File Materialization
 * for server-side computing.
 * The tag for this controller is .../download/* or .../restrictedAccess/download
 * <p>
 * The goal for this controller is to allow external code to have the
 * thredds server materialize a dataset into the file system .
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
 * client-side certificates or tomcat roles. This code performs
 * no authentication.
 * <p>
 * 2. The external code is running on the same machine as the thredds
 * server, or at least has a common file system so that file system
 * writes by thredds are visible to the external code.
 * <p>
 * WARNING: In order to support downloading of non-file datasets
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
 * Using query parameters means that it is possible to use an Http Form
 * to send them; see TestDownload.
 * <p>
 * Download Request
 * ----------------
 * Download parameters:
 * - request=download -- materialize a file into the download directory
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
 * Return value: <key>={value of the requested key}; one key-value pair per line.
 */

@Controller
@RequestMapping(value = {"/download", "/restrictedAccess/download"})
public class DownloadController extends LoadCommon
{
    //////////////////////////////////////////////////
    // Constants

    static final protected boolean DEBUG = true;

    static final protected String DEFAULTREQUESTNAME = "download";

    static final protected String DEFAULTDOWNLOADDIR = "download";

    static final protected String FILESERVERSERVLET = "/fileServer/";

    //////////////////////////////////////////////////
    // Instance variables

    protected Nc4Chunking.Strategy strategy = Nc4Chunking.Strategy.standard;
    protected Nc4Chunking chunking = new Nc4ChunkingDefault();
    protected DownloadParameters params = null;
    protected String downloadform = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public DownloadController()
            throws ServletException
    {
        super();
    }

    //////////////////////////////////////////////////

    /**
     * Invoked once on first request so that everything is available,
     * especially Spring stuff.
     */
    public void doonce(HttpServletRequest req)
            throws SendError
    {
        if(once)
            return;
        super.initOnce(req);
        if(this.downloaddir == null)
            throw new SendError(HttpStatus.SC_PRECONDITION_FAILED, "Download disabled");
        this.downloaddirname = new File(this.downloaddir).getName();

        // Get the download form
        File downform = null;
        downform = tdsContext.getDownloadForm();
        if(downform == null) {   // Look in WEB-INF directory
            File root = tdsContext.getServletRootDirectory();
            downform = new File(root, DEFAULTDOWNLOADFORM);
        }
        try {
            this.downloadform = loadForm(downform);
        } catch (IOException ioe) {
            throw new SendError(HttpStatus.SC_PRECONDITION_FAILED, ioe);
        }
    }

    // Setup for each request
    public void setup(HttpServletRequest req, HttpServletResponse resp)
            throws SendError
    {
        this.req = req;
        this.res = resp;
        if(!once)
            doonce(req);

        // Parse any query parameters
        try {
            this.params = new DownloadParameters(req);
        } catch (IOException ioe) {
            throw new SendError(res.SC_BAD_REQUEST, ioe);
        }
    }

    //////////////////////////////////////////////////
    // Controller entry point(s)

    @RequestMapping(value = "**", method = RequestMethod.GET)
    public void
    doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException
    {
        try {
            setup(req, res);
            String sresult = null;
            switch (this.params.command) {
            case DOWNLOAD:
                try {
                    String fulltargetpath = download();
                    if(this.params.fromform) {
                        sendForm("Download succeeded: result file: " + fulltargetpath);
                    } else {
                        Map<String, String> result = new HashMap<>();
                        result.put("download", fulltargetpath);
                        sresult = mapToString(result, true, "download");
                        sendOK(sresult);
                    }
                } catch (SendError se) {
                    if(this.params.fromform) {
                        // Send back the download form with error msg
                        sendForm("Download failed: " + se.getMessage());
                    } else
                        throw se;
                }
                break;
            case INQUIRE:
                sresult = inquire();
                // Send back the inquiry answers
                sendOK(sresult);
                break;
            case NONE: // Use form-based download
                // Send back the download form
                sendForm("No files downloaded");
                break;
            }
        } catch (SendError se) {
            sendError(se);
        } catch (Exception e) {
            String msg = getStackTrace(e);
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg, e);
        }
    }

    //////////////////////////////////////////////////

    /**
     * @return absolute path to the downloaded file
     * @throws SendError if bad request
     */
    protected String
    download()
    {
        String target = this.params.target;

        if(target == null)
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "No target specified");

        // Make sure target path does not contain '..'
        if(target.indexOf("..") >= 0)
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "Target parameter contains '..': " + target);

        // See if the target is relative or absolute
        File ftarget = new File(target);
        if(ftarget.isAbsolute())
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "Target parameter must be a relative path: " + target);

        // See if we have a download dir
        if(this.downloaddir == null)
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "No download directory specified for relative target: " + target);

        // Make target relative to download dir and convert to absolute path
        StringBuilder b = new StringBuilder();
        b.append(this.downloaddir);
        b.append('/');
        b.append(target);
        ftarget = new File(b.toString());
        String fulltarget = HTTPUtil.canonicalpath(ftarget.getAbsolutePath());

        // Make sure that all intermediate directories exist
        File parent = ftarget.getParentFile();
        if(!parent.exists() && !ftarget.getParentFile().mkdirs())
            throw new SendError(HttpServletResponse.SC_FORBIDDEN, "Target file parent directory cannot be created: " + fulltarget);

        // If file exists, delete it if requested
        if(!this.params.overwrite && ftarget.exists())
            throw new SendError(HttpServletResponse.SC_FORBIDDEN, "Target file exists and overwrite is not set");
        else if(this.params.overwrite && ftarget.exists()) {
            if(!ftarget.delete())
                throw new SendError(HttpServletResponse.SC_FORBIDDEN, "Target file exists and cannot be deleted");
        }

        String surl = this.params.url;
        URL url;
        try {
            url = new URL(surl);
        } catch (MalformedURLException mue) {
            throw new SendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed URL: " + surl);
        }

        // Make sure we are not recursing
        String path = url.getPath();
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
        b.append(url.getProtocol());
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
        return (fulltarget);
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
    canCopy(String truepath, FileFormat targetformat)
    {
        try {
            RandomAccessFile raf = new RandomAccessFile(truepath, "r");
            int format = NCheader.checkFileType(raf);
            switch (format) {
            case NC_FORMAT_CLASSIC:
            case NC_FORMAT_64BIT_OFFSET:
                return targetformat == FileFormat.NETCDF3;
            case NC_FORMAT_NETCDF4:
            case NC_FORMAT_64BIT_DATA:
            case NC_FORMAT_HDF4:
                return targetformat == FileFormat.NETCDF4;
            default: break;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    //////////////////////////////////////////////////

    @Override
    protected String
    buildForm(String msg)
    {
        StringBuilder svc = new StringBuilder();
        svc.append(this.server);
        svc.append("/");
        svc.append(this.threddsname);
        String form = String.format(this.downloadform,
                svc.toString(),
                msg
        );
        return form;
    }

}
