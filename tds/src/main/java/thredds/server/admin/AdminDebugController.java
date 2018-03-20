/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import thredds.util.ContentType;
import ucar.nc2.constants.CDM;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * Handle the /admin/debug interface
 *
 * @author caron
 * @since 4.0
 */
//
@Controller
@RequestMapping(value = "/admin/debug")
public class AdminDebugController {

  @Autowired
  DebugCommands debugCommands;

  @Autowired
  HtmlWriting htmlu;

  // LOOK change to ResponseEntity<String>
  @RequestMapping( method = RequestMethod.GET)
  protected void showDebugPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType(ContentType.html.getContentHeader());
    response.setHeader("Content-Description", "thredds_debug");

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintStream pw = new PrintStream(bos, false, CDM.UTF8);
    pw.println(htmlu.getHtmlDoctypeAndOpenTag());
    pw.println("<head>");
    pw.println("<title>THREDDS Debug</title>");
    pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html\">");
    pw.println(htmlu.getTdsPageCssLink());
    pw.println(htmlu.getGoogleTrackingContent());
    pw.println("</head>");
    pw.println("<body>");
    pw.println(htmlu.getOldStyleHeader());
    pw.println("<br><a href='dir/content/thredds/logs/'>Show TDS Logs</a>");
    pw.println("<br><a href='dir/content/tdm/'>Show TDM Logs</a>");
    pw.println("<br><a href='dir/logs/'>Show Tomcat Logs</a>");
    pw.println("<br><a href='dir/catalogs/'>Show Config Catalogs</a>");
    pw.println("<br><a href='spring/showControllers'>Show Spring Controllers</a>");
    pw.println("<h2>Debug Actions</h2>");
    pw.println("<pre>");

    String cmds = request.getQueryString();
    if ((cmds == null) || (cmds.length() == 0)) {
      showDebugActions(request, pw);

    } else {

      StringTokenizer tz = new StringTokenizer(cmds, ";");
      while (tz.hasMoreTokens()) {
        String cmd = tz.nextToken();
        String target = null;

        pw.println("Cmd= " + cmd);
        int pos = cmd.indexOf('/');
        String dhName = "General";
        if (pos > 0) {
          dhName = cmd.substring(0, pos);
          cmd = cmd.substring(pos + 1);
        }

        pos = cmd.indexOf('=');
        if (pos >= 0) {
          target = cmd.substring(pos + 1);
          cmd = cmd.substring(0, pos);
        }

        DebugCommands.Category dh = debugCommands.findCategory(dhName);
        if (dh == null) {
          pw.println(" Unknown Debug Category=" + dhName + "=");
        } else {
          DebugCommands.Action action = dh.actions.get(cmd);
          if (action == null)
            pw.println(" Unknown action=" + cmd + "=");
          else
            action.doAction(new DebugCommands.Event(request, response, pw, bos, target));
        }
      }
    }
    pw.println("</pre></body></html>");

    response.setStatus(HttpServletResponse.SC_OK);

    // send it out
    PrintWriter responsePS = response.getWriter();
    responsePS.write(bos.toString(CDM.UTF8));
    responsePS.flush();
  }

  private void showDebugActions(HttpServletRequest req, PrintStream pw) {
    for (DebugCommands.Category dh : debugCommands.getCategories()) {
      pw.println("<h2>" + dh.name + "</h2>");

      for (DebugCommands.Action act : dh.actions.values()) {
        if (act.desc == null) continue;

        String url = req.getRequestURI() + "?" + dh.name + "/" + act.name;
        pw.println("   <a href='" + url + "'>" + act.desc + "</a>");
      }
    }
  }

}
