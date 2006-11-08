package thredds.servlet;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class DebugHandler {

  static private ArrayList dhList = new ArrayList();

  static public DebugHandler get( String name) {
    for (int i = 0; i < dhList.size(); i++) {
      DebugHandler dh = (DebugHandler) dhList.get(i);
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
    pw.println(HtmlWriter.getInstance().getUserCSS());
    pw.println("</head>");
    pw.println("<body>");
    pw.println(HtmlWriter.getInstance().getUserHead());
    pw.println("<br><a href='content/logs/'>Show Logs</a>");
    pw.println("<h2>Debug Actions</h2>");
    pw.println("<body><pre>");

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
          Action action = (Action) dh.actions.get( cmd);
          if (action == null)
            pw.println(" Unknown action="+cmd+"=");
          else
            action.doAction( new Event( request, response, pw, bos, target));
        }
      }
    }
    pw.println("</pre></body>");

    // send it out
    byte[] result = bos.toByteArray();
    PrintStream responsePS = new PrintStream(response.getOutputStream());
    responsePS.write(result);
    responsePS.flush();
    response.setStatus(HttpServletResponse.SC_OK);
    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, result.length );
  }

  static private void showDebugActions(HttpServletRequest req, HttpServletResponse res, PrintStream pw) {
    Iterator dhIter = dhList.iterator();
    while (dhIter.hasNext()) {
      DebugHandler dh =  (DebugHandler) dhIter.next();
      pw.println("<h2>"+dh.name+"</h2>");

      Iterator actIter = dh.actions.values().iterator();
      while (actIter.hasNext()) {
        Action act = (Action) actIter.next();
        if (act.desc == null) continue;

        String url = req.getRequestURI() + "?" + dh.name+"/"+act.name;
        pw.println("   <a href='" +url+"'>"+act.desc+"</a>");
      }
    }
  }

  static private DebugHandler find(String name) {
    Iterator dhIter = dhList.iterator();
    while (dhIter.hasNext()) {
      DebugHandler dh =  (DebugHandler) dhIter.next();
      if (dh.name.equals(name))
        return dh;
    }
    return null;
  }

  //////////////////////////////////////////////////////
  private LinkedHashMap actions = new LinkedHashMap();
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