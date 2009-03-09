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
package thredds.servlet;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class DebugHandler {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( DebugHandler.class );
  
  static private List<DebugHandler> dhList = new ArrayList<DebugHandler>();

  static public DebugHandler get( String name) {
    for (DebugHandler dh : dhList) {
      if (name.equals( dh.name)) return dh;
    }
    return new DebugHandler(name);
  }

  static public void doDebug(HttpServlet thisServlet,  HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    response.setContentType("text/html");
    response.setHeader("Content-Description", "thredds_debug");

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintStream pw = new PrintStream(bos);
    pw.println( HtmlWriter.getInstance().getHtmlDoctypeAndOpenTag());
    pw.println("<head>");
    pw.println("<title> THREDDS Debug</title>");
    pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html\">");
    pw.println(HtmlWriter.getInstance().getTdsPageCssLink());
    pw.println("</head>");
    pw.println("<body>");
    pw.println(HtmlWriter.getInstance().getOldStyleHeader());
    pw.println("<br><a href='content/logs/'>Show TDS Logs</a>");
    pw.println("<br><a href='logs/'>Show Tomcat Logs</a>");
    pw.println("<h2>Debug Actions</h2>");
    pw.println("<pre>");

    String cmds = request.getQueryString();
    if ((cmds == null) || (cmds.length() == 0)) {
      showDebugActions(request, response, pw);

    } else {

      StringTokenizer tz = new StringTokenizer( cmds, ";");
      while (tz.hasMoreTokens()) {
        String cmd=tz.nextToken();
        String target = null;

        pw.println("Cmd= "+cmd);
        int pos = cmd.indexOf('/');
        String dhName = "General";
        if (pos > 0) {
          dhName = cmd.substring(0,pos);
          cmd = cmd.substring(pos+1);
        }

        pos = cmd.indexOf('=');
        if (pos >= 0) {
          target = cmd.substring(pos+1);
          cmd = cmd.substring(0,pos);
        }

        DebugHandler dh = find( dhName);
        if (dh == null) {
          pw.println(" Unknown DebugHandler="+dhName+"=");
        } else {
          Action action = dh.actions.get( cmd);
          if (action == null)
            pw.println(" Unknown action="+cmd+"=");
          else
            action.doAction( new Event( request, response, pw, bos, target));
        }
      }
    }
    pw.println("</pre></body></html>");

    response.setStatus( HttpServletResponse.SC_OK );

    // send it out
    byte[] result = bos.toByteArray();
    PrintStream responsePS = new PrintStream(response.getOutputStream());
    responsePS.write(result);
    responsePS.flush();
    log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, result.length ));
  }

  static private void showDebugActions(HttpServletRequest req, HttpServletResponse res, PrintStream pw) {
    for (DebugHandler dh : dhList) {
      pw.println("<h2>" + dh.name + "</h2>");

      for (Action act : dh.actions.values()) {
        if (act.desc == null) continue;

        String url = req.getRequestURI() + "?" + dh.name + "/" + act.name;
        pw.println("   <a href='" + url + "'>" + act.desc + "</a>");
      }
    }
  }

  static private DebugHandler find(String name) {
    for (DebugHandler dh : dhList) {
      if (dh.name.equals(name))
        return dh;
    }
    return null;
  }

  //////////////////////////////////////////////////////
  private Map<String, Action> actions = new LinkedHashMap<String, Action>();
  private String name;

  private DebugHandler( String name) {
    this.name = name;
    dhList.add( this);
  }

  public void addAction(Action act) {
    actions.put( act.name, act);
  }

  static public abstract class Action {
    public String name, desc;
    public Object userObject; // for subclass to save stuff
    public Action(String name, String desc) {
      this.name = name;
      this.desc = desc;
    }
    public Action(String name, String desc, Object userObject) {
      this.name = name;
      this.desc = desc;
      this.userObject = userObject;
    }
    public abstract void doAction(Event e);
  }

  static public class Event {
    public HttpServletRequest req;
    public HttpServletResponse res;
    public PrintStream pw;
    public ByteArrayOutputStream bos;
    public String target;

    public Event(HttpServletRequest req, HttpServletResponse res, PrintStream pw, ByteArrayOutputStream bos, String target) {
      this.req = req;
      this.res = res;
      this.pw = pw;
      this.bos = bos;
      this.target = target;
    }
  }

}