/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the LICENCE file for more information. */

package dap4.servlet;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4shared.XURI;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.net.URISyntaxException;

/**
 * Servlet specific info is captured once
 * and stored here.
 *
 * @author Dennis Heimbigner
 */


public class ServletInfo
{

    //////////////////////////////////////////////////
    // Constants

    static public final String WEBINFPPATH = "WEB-INF";
    static public final String RESOURCEDIRNAME = "resources";

    //////////////////////////////////////////////////
    // Instance variables

    protected HttpServlet servlet = null;
    protected ServletConfig servletconfig = null;
    protected ServletContext servletcontext = null;
    protected String servletname = null;
    protected String server = null;
    protected String realpath = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public ServletInfo(HttpServlet sv)
            throws DapException
    {
        this.servlet = sv;
        this.servletconfig = sv.getServletConfig();
        if(this.servletconfig == null)
            throw new DapException("Cannot locate servlet config object");
        this.servletcontext = this.servletconfig.getServletContext();
        this.servletname = this.servletconfig.getServletName();
        // Look around to see where the /resources dir is located
        // relative to realpath.
        this.realpath = DapUtil.canonicalpath(this.servletcontext.getRealPath(""));
    }
    //////////////////////////////////////////////////
    // Accessors

    public HttpServlet getServlet()
    {
        return servlet;
    }

    public ServletConfig getServletconfig()
    {
        return servletconfig;
    }

    public ServletContext getServletcontext()
    {
        return servletcontext;
    }

    public String getServletname()
    {
        return servletname;
    }

    /**
     * Return the absolute path for the "webapp/d4ts" directory
     * (or its equivalent)
     *
     * @return the absolute path for the /WEB-INF/resources directory
     */

    protected String getRealPath(String virtual)
    {
        if(virtual.startsWith("/"))
            virtual = virtual.substring(1);
        return DapUtil.canonicalpath(this.realpath +"/"+ virtual);
    }

    public String
    getContextPath()
    {
        return servletcontext.getContextPath();
    }

    public String getServer()
    {
        return this.server;
    }

    // We can only set this after we get some kind of call
    public void setServer(String url)
    {
        try {
            XURI xurl = new XURI(url);
            String simpleurl = xurl.getLeadProtocol() + "://" + xurl.getHost();
            if(this.server != null && !this.server.equals(simpleurl))
                DapLog.warn("ServletInfo.setServer: server mismatch: " + this.server + " :: " + simpleurl);
            this.server = simpleurl;
        } catch (URISyntaxException use) {
            DapLog.warn("ServletInfo.setServer: malformed url: " + url);
        }
    }
}
