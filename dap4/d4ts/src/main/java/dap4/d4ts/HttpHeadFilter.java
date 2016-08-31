/*
 * Source code from blog post: Transparently supporting HTTP HEAD requests in Java and Spring MVC
 * http://axelfontaine.com/blog/http-head.html
 *
 * Copyright 2009 Axel Fontaine
 *
 * Parts of this code have been inspired by code found in the Servlet API 2.5.
 *
 * This code is provided free of charge. You agree to use and modify this code at your own risk.
 *
 * Feedback is always welcome !
 */
package dap4.d4ts;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Servlet filter that presents a HEAD request as a GET. The application doesn't need to know the difference, as this
 * filter handles all the details.
 */
public class HttpHeadFilter implements Filter {
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    //Do nothing
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;

    if (isHttpHead(httpServletRequest)) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      NoBodyResponseWrapper noBodyResponseWrapper = new NoBodyResponseWrapper(httpServletResponse);

      chain.doFilter(new ForceGetRequestWrapper(httpServletRequest), noBodyResponseWrapper);
      noBodyResponseWrapper.setContentLength();
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {
    //Do nothing
  }

  /**
   * Checks whether the HTTP method of this request is HEAD.
   *
   * @param request The request to check.
   * @return {@code true} if it is HEAD, {@code false} if it isn't.
   */
  private boolean isHttpHead(HttpServletRequest request) {
    return "HEAD".equals(request.getMethod());
  }

  /**
   * Request wrapper that lies about the Http method and always returns GET.
   */
  private class ForceGetRequestWrapper extends HttpServletRequestWrapper {
    /**
     * Initializes the wrapper with this request.
     *
     * @param request The request to initialize the wrapper with.
     */
    public ForceGetRequestWrapper(HttpServletRequest request) {
      super(request);
    }

    /**
     * Lies about the HTTP method. Always returns GET.
     *
     * @return Always returns GET.
     */
    @Override
    public String getMethod() {
      return "GET";
    }
  }

  /**
   * Response wrapper that swallows the response body, leaving only the headers.
   */
  private class NoBodyResponseWrapper extends HttpServletResponseWrapper {
    /**
     * Outputstream that discards the data written to it.
     */
    private final NoBodyOutputStream noBodyOutputStream = new NoBodyOutputStream();

    private PrintWriter writer;

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response The response to wrap.
     */
    public NoBodyResponseWrapper(HttpServletResponse response) {
      super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      return noBodyOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws UnsupportedEncodingException {
      if (writer == null) {
        writer = new PrintWriter(new OutputStreamWriter(noBodyOutputStream, getCharacterEncoding()));
      }

      return writer;
    }

    /**
     * Sets the content length, based on what has been written to the outputstream so far.
     */
    void setContentLength() {
      super.setContentLength(noBodyOutputStream.getContentLength());
    }
  }

  /**
   * Outputstream that only counts the length of what is being written to it while discarding the actual data.
   */
  private class NoBodyOutputStream extends ServletOutputStream {
    /**
     * The number of bytes written to this stream so far.
     */
    private int contentLength = 0;

    /**
     * @return The number of bytes written to this stream so far.
     */
    int getContentLength() {
      return contentLength;
    }

    @Override
    public void write(int b) {
      contentLength++;
    }

    @Override
    public void write(byte buf[], int offset, int len) throws IOException {
      contentLength += len;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      throw new UnsupportedOperationException("NoBodyOutputStream does not support setWriteListener");
    }
  }
}
