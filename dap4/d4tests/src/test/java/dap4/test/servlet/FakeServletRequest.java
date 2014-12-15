package dap4.test.servlet;

import dap4.core.util.DapUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import java.io.*;
import java.net.URL;
import java.util.*;

public class FakeServletRequest implements javax.servlet.http.HttpServletRequest
{
    //////////////////////////////////////////////////
    // Type declarations

    /**
     * Hack to allow us to use iterable objects when the interface
     * requires an enumeration.
     */
    static final class Iter2Enumeration implements Enumeration
    {
        private final Iterator iter;

        public Iter2Enumeration(final Iterator iter)
        {
            this.iter = iter;
        }

        public boolean hasMoreElements()
        {
            return iter.hasNext();
        }

        public Object nextElement()
        {
            return iter.next();
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    String inputurl;
    URL url;
    FakeServlet servlet;

    String servletname = null;
    String datasetpath = null;

    String filesystemroot = null;

    Map<String, List<String>> fakeheaders = new HashMap<String, List<String>>();

    // Convenience
    void addHeader(String name, String value)
    {
        List<String> matches = fakeheaders.get(name);
        if(matches == null) {
            matches = new ArrayList<String>();
            fakeheaders.put(name, matches);
        }
        if(!matches.contains(value))
            matches.add(value);
    }

    //////////////////////////////////////////////////
    // Constructor(s)

    public FakeServletRequest(String surl, FakeServlet sv)
            throws Exception
    {
        inputurl = surl;
        this.url = new URL(surl);
        this.servlet = sv;
        this.servletname = sv.getServletName();
        // Add some fake headers
        addHeader("User-Agent", "Fake");
        parsePath(url);
    }

    void parsePath(URL url)
            throws Exception
    {
        String path = url.getPath();
        // Assume that the path begins with the servlet name
        String servletprefix = "/" + servletname;
        if(!path.startsWith(servletprefix))
            throw new Exception("URL path does not start with servletname: " + url.toString());
        this.datasetpath = DapUtil.canonicalpath(path.substring(servletprefix.length()));
    }

    /////////////////////////////////////////////////
    // ServletRequest Interface

    public Object getAttribute(String s)
    {
        return null;
    }

    public Enumeration getAttributeNames()
    {
        return null;
    }

    public String getCharacterEncoding()
    {
        return "UTF-8";
    }

    public void setCharacterEncoding(String s)
            throws UnsupportedEncodingException
    {
        return;
    }

    public int getContentLength()
    {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    public String getContentType()
    {
        return null;
    }

    public javax.servlet.ServletInputStream getInputStream() throws IOException
    {
        return null;
    }

    public String getParameter(String s)
    {
        return null;
    }

    public Enumeration getParameterNames()
    {
        return null;
    }

    public String[] getParameterValues(String s)
    {
        return null;
    }

    public Map getParameterMap()
    {
        return null;
    }

    public String getProtocol()
    {
        return url.getProtocol();
    }

    public String getScheme()
    {
        return url.getProtocol();
    }

    public String getServerName()
    {
        return "localhost";
    }

    public int getServerPort()
    {
        return 8080;
    }

    public BufferedReader getReader() throws IOException
    {
        return null;
    }

    public String getRemoteAddr()
    {
        return null;
    }

    public String getRemoteHost()
    {
        return null;
    }

    public void setAttribute(String s, Object o)
    {
        return;
    }

    public void removeAttribute(String s)
    {
        return;
    }

    public Locale getLocale()
    {
        return null;
    }

    public Enumeration getLocales()
    {
        return null;
    }

    public boolean isSecure()
    {
        return false;
    }

    public javax.servlet.RequestDispatcher getRequestDispatcher(String s)
    {
        return null;
    }

    public int getRemotePort()
    {
        return 0;
    }

    public String getLocalName()
    {
        return null;
    }

    public String getLocalAddr()
    {
        return null;
    }

    public int getLocalPort()
    {
        return 0;
    }


    // Servlet API 3.0 additions

    @Override
    public ServletContext getServletContext()
    {
        return servlet.getServletContext();
    }

    @Override
    public Collection<Part> getParts()
    {
        return null;
    }

    @Override
    public Part getPart(String s)
    {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> tClass) throws
            IOException, ServletException {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType()
    {
        return null;
    }

    @Override
    public void logout()
    {
        return;
    }

    @Override
    public void login(String s1, String s2)
    {
        return;
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse)
    {
        return true;
    }

    @Override
    public AsyncContext startAsync()
    {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest req, ServletResponse resp)
    {
        return null;
    }

    @Override
    public boolean isAsyncStarted()
    {
        return false;
    }

    @Override
    public boolean isAsyncSupported()
    {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext()
    {
        return null;
    }

    /**
     * @deprecated
     */
    public String getRealPath(String s)
    {
        return null;
    }

    //////////////////////////////////////////////////
    // HttpServletRequest Interface

    public String getAuthType()
    {
        return null;
    }

    public javax.servlet.http.Cookie[] getCookies()
    {
        return null;
    }

    public long getDateHeader(String s)
    {
        return 0;
    }

    public String getHeader(String s)
    {
        List<String> headers = fakeheaders.get(s);
        if(headers.size() == 0) return null;
        return headers.get(0);
    }

    public Enumeration getHeaders(String s)
    {
        List<String> values = fakeheaders.get(s);
        if(values == null) return null;
        return new Iter2Enumeration(values.iterator());
    }

    public Enumeration getHeaderNames()
    {
        return new Iter2Enumeration(fakeheaders.keySet().iterator());
    }

    public int getIntHeader(String s)
            throws NumberFormatException
    {
        String v = getHeader(s);
        return Integer.parseInt(v);
    }

    public String getMethod()
    {
        return null;
    }

    public String getPathInfo()
    {
        String path = null;
        if(datasetpath != null) {
            path = datasetpath;
            path = DapUtil.absolutize(path);
        }
        return path;
    }

    public String getPathTranslated()
    {
        return null;
    }

    public String getContextPath()
    {
        return "";
    }

    public String getQueryString()
    {
        return url.getQuery();
    }

    public String getRemoteUser()
    {
        return null;
    }

    public boolean isUserInRole(String s)
    {
        return false;
    }

    public java.security.Principal getUserPrincipal()
    {
        return null;
    }

    public String getRequestedSessionId()
    {
        return null;
    }

    public String getRequestURI()
    {
        return getRequestURL().toString();
    }

    public StringBuffer getRequestURL()
    {
        // remove query
        int pos = inputurl.lastIndexOf('?');
        String rqurl = (pos < 0 ? inputurl : inputurl.substring(0, pos));
        return new StringBuffer(rqurl);
    }

    public String getServletPath()
    {
        String path = servletname;
        path = DapUtil.absolutize(path);
        return path;
    }

    public javax.servlet.http.HttpSession getSession(boolean b)
    {
        return null;
    }

    public javax.servlet.http.HttpSession getSession()
    {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    public boolean isRequestedSessionIdValid()
    {
        return false;
    }

    public boolean isRequestedSessionIdFromCookie()
    {
        return false;
    }

    public boolean isRequestedSessionIdFromURL()
    {
        return false;
    }

    @Deprecated
    public boolean isRequestedSessionIdFromUrl()
    {
        return false;
    }

}
