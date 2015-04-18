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

package thredds.server.admin;

import java.io.*;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import thredds.core.DataRoot;
import thredds.core.DataRootManager;
import thredds.server.config.TdsContext;
import thredds.core.PathMatcher;
import thredds.servlet.ServletUtil;
import thredds.util.TdsPathUtils;
import ucar.nc2.constants.CDM;

/**
 * Handle the /admin/log interface
 *
 * @author caron
 * @since 4.0
 */
@Controller
@RequestMapping(value="/admin/log", method=RequestMethod.GET)
public class AdminLogController {
	
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  private File accessLogDirectory;
  private List<File> accessLogFiles = new ArrayList<File>(10);

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private DataRootManager matcher;

  public void setAccessLogDirectory(String accessLogDirectory) {
    this.accessLogDirectory = new File(accessLogDirectory);
    init();
  }

  private void init() {
    File[] files = accessLogDirectory.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith("access.");
      }
    });
    if (files == null) return;

    for (File f : files) {
      accessLogFiles.add(f);
    }
  }

  @RequestMapping( "**")
  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    String path = TdsPathUtils.extractPath(req, "/admin/log");

    File file = null;
    if (path.equals("/access/current")) {

      File dir = tdsContext.getTomcatLogDirectory();
      File[] files = dir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.startsWith("access");
        }
      });
      if ((files == null) || (files.length == 0)) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
      }

      List fileList = Arrays.asList(files);
      Collections.sort(fileList);
      file = (File) fileList.get(fileList.size() - 1); // last one

    } else if (path.equals("/access/")) {
      showFiles(tdsContext.getTomcatLogDirectory(), "access", res);

    } else if (path.startsWith("/access/")) {
      file = new File(tdsContext.getTomcatLogDirectory(), path.substring(8));
      ServletUtil.returnFile(req, res, file, "text/plain");
      return null;

    } else if (path.equals("/thredds/current")) {
      file = new File(tdsContext.getContentDirectory(), "logs/threddsServlet.log");

    } else if (path.equals("/thredds/")) {
      showFiles(new File(tdsContext.getContentDirectory(),"logs"), "thredds", res);

    } else if (path.startsWith("/thredds/")) {
      file = new File(tdsContext.getContentDirectory(), "logs/" + path.substring(9));
      ServletUtil.returnFile(req, res, file, "text/plain");
      return null;

    } else {
      PrintWriter pw = res.getWriter();
      pw.format("/log/access/current%n");
      pw.format("/log/access/%n");
      pw.format("/log/thredds/current%n");
      pw.format("/log/thredds/%n");
      pw.flush();
    }

    if (file != null)
      return new ModelAndView("threddsFileView", "file", file);
    else
      return null;
  }

  @RequestMapping("/dataroots.txt")
  protected String showRoots() throws Exception {
    Formatter f = new Formatter();
    matcher.showRoots(f);
    return f.toString();
  }

  private void showFiles(File dir, final String filter, HttpServletResponse res) throws IOException {
    File[] files = dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith(filter);
      }
    });

    if ((files == null) || (files.length == 0)) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    List<File> fileList = Arrays.asList(files);
    Collections.sort(fileList);

    PrintWriter pw = res.getWriter();
    for (File f : fileList)
      pw.format("%s %d%n", f.getName(), f.length());
    pw.flush();
  }

}