/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.servlet.tomcat;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.util.LifecycleSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Formatter;

/**
 * Test writing a Valve, to get at tomcat implementation info
 *
 * @author caron
 * @since Jan 13, 2009
 */
public class UsageValve extends org.apache.catalina.valves.ValveBase implements Lifecycle {
  private Formatter fout;
  private long startTime;
  private long count = 0;

  public void invoke(Request request, Response response) throws IOException, ServletException {
    if (started) {

      int status = response.getStatus();
      if (status == 302) { // redirect
        getNext().invoke(request, response);
        return;
      }

      // Pass this request on to the next valve in our pipeline
      long t1 = System.currentTimeMillis();
      getNext().invoke(request, response);
      long time = System.currentTimeMillis() - t1;
      long minute = (t1 - startTime) / 60000;

      if (count % 10 == 0)
        fout.format("ret msecs bytes minute  ip             context   servlet   path?query          dataset%n");
      count++;


      StringBuilder sb = new StringBuilder();
      //sb.append(request.getContextPath());
      //sb.append(request.getServletPath());
      if (request.getPathInfo() != null)
        sb.append(request.getPathInfo());
      if (request.getQueryString() != null) {
        sb.append("?");
        sb.append(request.getQueryString());
      }

      // stuff that must be passed by TDS
      String dataset = null;
      HttpSession s = request.getSession(false);
      if (s != null) {
        dataset = (String) s.getAttribute("dataset");
      }

      fout.format("%3d %5d %5d %5d %-16s %-10s %-10s %-20s %-20s%n",
              status, time, response.getContentCount(), minute,
              request.getRemoteAddr(),
              request.getContextPath(),
              request.getServletPath(),
              sb.toString(),
              dataset == null ? "" : dataset);

    } else
      getNext().invoke(request, response);
  }

  private boolean started = false;
  protected LifecycleSupport lifecycle = new LifecycleSupport(this);

  public void addLifecycleListener(LifecycleListener lifecycleListener) {
    lifecycle.addLifecycleListener(lifecycleListener);
  }

  public LifecycleListener[] findLifecycleListeners() {
    return lifecycle.findLifecycleListeners();
  }

  public void removeLifecycleListener(LifecycleListener lifecycleListener) {
    lifecycle.removeLifecycleListener(lifecycleListener);
  }

  public void start() throws LifecycleException {
    // Validate and update our current component state
    if (started)
      throw new LifecycleException(sm.getString("accessLogValve.alreadyStarted"));
    lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);
    startTime = System.currentTimeMillis();

    fout = new Formatter(System.out);
    fout.format("AccessValve started%n");
    started = true;
  }

  public void stop() throws LifecycleException {
    // Validate and update our current component state
    if (!started)
      throw new LifecycleException(sm.getString("accessLogValve.notStarted"));
    lifecycle.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);
    started = false;
  }
}
