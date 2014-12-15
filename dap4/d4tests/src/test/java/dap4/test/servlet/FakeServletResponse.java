package dap4.test.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class FakeServletResponse implements javax.servlet.http.HttpServletResponse
{
    //////////////////////////////////////////////////
    // Instance Variables

    FakeServletOutputStream stream;
    PrintWriter pw;
    String contenttype = null;
    long contentlength = 0;

    Map<String, List<String>> fakeheaders;

    int fakestatus = 200;

    //////////////////////////////////////////////////
    // Constructor(s)

    public FakeServletResponse()
    {
        this.stream = new FakeServletOutputStream();
        pw = new PrintWriter(stream);
        fakeheaders = new HashMap<String, List<String>>();
    }

    //////////////////////////////////////////////////
    // ServletResponse Interface

    public String getCharacterEncoding()
    {
        return "UTF-8";
    }

    public String getContentType()
    {
        return contenttype;
    }

    public javax.servlet.ServletOutputStream getOutputStream()
        throws IOException
    {
        return stream;
    }

    public PrintWriter getWriter() throws IOException
    {
        return pw;
    }

    public void setCharacterEncoding(String s)
    {
        return;
    }

    public void setContentLength(int i)
    {
        contentlength = i;
    }

    public void setContentLengthLong(long i) {
        contentlength = i;
    }

    public void setContentType(String s)
    {
        contenttype = s;
    }

    public void setBufferSize(int i)
    {
        return;
    }

    public int getBufferSize()
    {
        return 0;
    }

    public void flushBuffer() throws IOException
    {
        return;
    }

    public void resetBuffer()
    {
        return;
    }

    public boolean isCommitted()
    {
        return false;
    }

    public void reset()
    {
        return;
    }

    public void setLocale(Locale locale)
    {
        return;
    }

    public Locale getLocale()
    {
        return null;
    }

    // HttpServletResponse Interface

    public void addCookie(javax.servlet.http.Cookie cookie)
    {
        return;
    }

    public boolean containsHeader(String s)
    {
        return (fakeheaders.keySet().contains(s));
    }

    public String encodeURL(String s)
    {
        return s;
    }

    public String encodeRedirectURL(String s)
    {
        return s;
    }

    public void sendError(int i, String s)
        throws IOException
    {
	    setStatus(i);
        throw new RuntimeException(s);
    }

    public void sendError(int i)
        throws IOException
    {
        sendError(i, "Unknown Error");
    }

    public void sendRedirect(String s) throws IOException
    {
        return;
    }

    public void setDateHeader(String s, long l)
    {
        return;
    }

    public void addDateHeader(String s, long l)
    {
        return;
    }

    public void setHeader(String s, String s1)
    {
        fakeheaders.remove(s);
        addHeader(s, s1);
    }

    public void addHeader(String s, String s1)
    {
        List<String> values = fakeheaders.get(s);
        if(values == null) {
            values = new ArrayList<String>();
            fakeheaders.put(s,values);
        }
        if(!values.contains(s1))
            values.add(s1);
    }

    public void setIntHeader(String s, int i)
    {
        setHeader(s,Integer.toString(i));
    }

    public void addIntHeader(String s, int i)
    {
        addHeader(s, Integer.toString(i));
    }

    public void setStatus(int i)
    {
	this.fakestatus = i;
    }


    /**
     * @deprecated
     */
    public String encodeUrl(String s)
    {
        return s;
    }

    @Deprecated
    public String encodeRedirectUrl(String s)
    {
        return s;
    }

    @Deprecated
    public void setStatus(int i, String s)
    {
        return;
    }

    // Servlet API 3.0 Additions

    public int getStatus() {return fakestatus;}

    public String getHeader(String name)
    {
	List<String> matches = fakeheaders.get(name);
	if(matches == null || matches.size() == 0)
	    return null;
	else
	    return matches.get(0);
    }

    public Collection<String> getHeaderNames() {return fakeheaders.keySet();}

    public Collection<String> getHeaders(java.lang.String p0)
    {
	return fakeheaders.get(p0);
    }


}



    
