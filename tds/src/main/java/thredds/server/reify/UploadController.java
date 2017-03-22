/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import thredds.server.config.TdsContext;
import thredds.util.ContentType;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

import static thredds.server.reify.ReifyUtils.SendError;


/**
 * File upload to a specific directory.
 * As a rule, this needs to coordinate with
 * the upload catalog entry specified
 * in the catalog (see the documentation for upload/download).
 * The tag for this controller is .../upload/*
 * <p>
 * The goal for this controller is to allow external code to have the
 * thredds server upload a file and store it in a specific place.
 * <p>
 * Assumptions:
 * 1. The external code has authenticated to the thredds server
 * using some authentication mechanism such as
 * client-side certificates or tomcat roles.
 */

@Controller
@RequestMapping("/upload")
public class UploadController
{
    //////////////////////////////////////////////////
    // Constants

    static final protected boolean DEBUG = true;

    static final protected String DEFAULTSERVLETNAME = "thredds";


    //////////////////////////////////////////////////
    // Static variables

    static org.slf4j.Logger logServerStartup;
    static org.slf4j.Logger log;

    static {
        logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
        log = org.slf4j.LoggerFactory.getLogger(UploadController.class);
    }

    static public TdsContext testTdsContext = null;

    //////////////////////////////////////////////////
    // Instance variables

    @Autowired
    protected TdsContext tdsContext = null;

    protected boolean initialized = false;
    protected boolean getInitialized = false;

    protected String server = null; // Our host + port
    protected String requestname = null;
    protected String threddsname = null;

    protected String uploaddir = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public UploadController()
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

        // Get server host + port name
        StringBuilder buf = new StringBuilder();
        buf.append(req.getServerName());
        int port = req.getServerPort();
        if(port > 0) {
            buf.append(":");
            buf.append(port);
        }
        this.server = buf.toString();

        // Get the upload dir
        File updir = null;
        if(tdsContext == null) tdsContext = testTdsContext;
        if(tdsContext != null)
            updir = tdsContext.getUploadDir();
        if(updir == null)
            throw new SendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No tds.upload.dir specified");
        else
            this.uploaddir = HTTPUtil.canonicalpath(updir.getAbsolutePath());
    }

    //////////////////////////////////////////////////
    // Controller entry points

    @RequestMapping(value = "", method = RequestMethod.GET)
    public void
    doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException
    {
        try {
            if(!getInitialized) initGet(req);
            // Send back the upload form
            String form = String.format(FORM, this.server, this.uploaddir);
            res.setContentType(ContentType.html + ";charset=UTF-8");
            res.getWriter().print(form);
            res.flushBuffer();
        } catch (SendError se) {
            sendError(se.httpcode, se.msg,res);
        } catch (Exception e) {
            String msg = ReifyUtils.getStackTrace(e);
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg,res);
        }
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    //@MultipartConfig(maxFileSize=1024*1024*500)
    public void
    doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException
    {
        try {
            if(this.uploaddir == null)
                throw new HTTPException("No upload directory defined on server")
                        .setCode(HttpStatus.SC_FORBIDDEN);

            // Check authorization of this user to do uploads using Tomcat
            // authorization
            throw new HTTPException("UnAuthorized to upload files")
                    .setCode(HttpStatus.SC_FORBIDDEN);

            // Get and upload the file(s) in the form
            for(Part part : req.getParts()) {
                String filename = extractFileName(part);
                filename = HTTPUtil.canonicalpath(filename);
                filename = HTTPUtil.relpath(filename);
                String fullpath = HTTPUtil.canonjoin(uploaddir, filename);
                try {
                    part.write(fullpath);
                } catch (Exception e) {
                    throw new HTTPException("Upload Failed", e)
                            .setCode(HttpStatus.SC_FORBIDDEN);
                }
            }
            // Return response

        } catch (HTTPException he) {
            sendError(he.getCode(), he.getMessage(),res);
        } catch (IOException ioe) {
            sendError(HttpStatus.SC_NOT_FOUND, ioe.getMessage(),res);
        }

    }

    //////////////////////////////////////////////////
    protected void
    sendError(int code, String msg, HttpServletResponse res)
    {
        if(DEBUG)
            System.err.println(String.format("xxx: error: code=%d msg=%s%n", code, msg));
        try {
            res.setContentType(ContentType.text.getContentHeader());
            try {
                PrintWriter pw = res.getWriter();
                pw.print(msg);
                pw.close();
                pw.flush();
            } catch (IOException ioe) {
                log.error(ioe.getMessage());
            }
        } catch (Exception e) {
            res.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
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
                codep[0] = res.SC_FORBIDDEN;
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

    protected String FORM = ""
            + "<html>"
            + "<body>"
            + "<h2>File Upload Form for: %s</h2>"
            + "<p>"
            + "<form action=\"/\">"
            + "  File to upload:&nbsp;"
            + "  <input type=\"text\" name=\"upload\" value=\"\">"
            + "  &nbsp;&nbsp;"
            + "  (absolute path)"
            + "  <p>"
            + "  (Optional) Destination File:&nbsp;"
            + "  <input type=\"text\" name=\"destination\" value=\"\">"
            + "  &nbsp;&nbsp;"
            + "  (relative path)"
            + "  <p><p>"
            + "  <input type=\"submit\" value=\"Submit\">"
            + "</form>"
            + "</body>"
            + "</html>";

}
