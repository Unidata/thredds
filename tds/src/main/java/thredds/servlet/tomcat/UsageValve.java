/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
