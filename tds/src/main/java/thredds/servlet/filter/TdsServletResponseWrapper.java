/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.servlet.filter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Wrap HttpServletResponse to capture state that is otherwise not accessible through standard API.
 *
 * @author edavis
 * @see RequestBracketingLogMessageFilter
 * @since 4.1
 */
public class TdsServletResponseWrapper extends HttpServletResponseWrapper {
  //private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

  private int httpStatusCode = 200;
  private long httpResponseBodyLength = -1;

  public TdsServletResponseWrapper(HttpServletResponse response) {
    super(response);
  }

  public int getHttpStatusCode() {
    return this.httpStatusCode;
  }

  private void setHttpStatusCode(int statusCode) {
    this.httpStatusCode = statusCode;
  }

  public long getHttpResponseBodyLength() {
    return this.httpResponseBodyLength;
  }

  private void setHttpResponseBodyLength(long responseBodyLength) {
    this.httpResponseBodyLength = responseBodyLength;
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    this.setHttpStatusCode(sc);
    super.sendError(sc, msg);
  }

  @Override
  public void sendError(int sc) throws IOException {
    this.setHttpStatusCode(sc);
    super.sendError(sc);
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    this.setHttpStatusCode(HttpServletResponse.SC_FOUND);
    super.sendRedirect(location);
  }

  @Override
  public void setStatus(int sc) {
    this.setHttpStatusCode(sc);
    super.setStatus(sc);
  }

  @Override
  public void setStatus(int sc, String sm) {
    this.setHttpStatusCode(sc);
    super.setStatus(sc, sm);
  }

  @Override
  public void setContentLength(int len) {
    this.setHttpResponseBodyLength(len);
    super.setContentLength(len);
  }
}
