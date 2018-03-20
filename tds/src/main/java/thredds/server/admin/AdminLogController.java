/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.admin;

import java.io.*;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import thredds.core.DataRootManager;
import thredds.server.config.TdsContext;
import thredds.servlet.ServletUtil;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;

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

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private DataRootManager matcher;

  @RequestMapping("/dataroots.txt")
  protected ResponseEntity<String> showRoots() throws Exception {
    Formatter f = new Formatter();
    matcher.showRoots(f);

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set(ContentType.HEADER, ContentType.text.getContentHeader());
    return new ResponseEntity<>(f.toString(), responseHeaders, HttpStatus.OK);
  }

  @RequestMapping( "**")
  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    String path = TdsPathUtils.extractPath(req, "/admin/log");

    File file = null;
    if (path.equals("access/current")) {

      File dir = tdsContext.getTomcatLogDirectory();
      File[] files = dir.listFiles((dir1, name) -> {
        return name.startsWith("access");
      });
      if ((files == null) || (files.length == 0)) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
      }

      List fileList = Arrays.asList(files);
      Collections.sort(fileList);
      file = (File) fileList.get(fileList.size() - 1); // last one

    } else if (path.equals("access/")) {
      showFiles(tdsContext.getTomcatLogDirectory(), "access", res);

    } else if (path.startsWith("access/")) {
      file = new File(tdsContext.getTomcatLogDirectory(), path.substring(7));
      ServletUtil.returnFile(req, res, file, "text/plain");
      return null;

    } else if (path.equals("thredds/current")) {
      file = new File(tdsContext.getThreddsDirectory(), "logs/threddsServlet.log");

    } else if (path.equals("thredds/")) {
      showFiles(new File(tdsContext.getThreddsDirectory(),"logs"), "thredds", res);

    } else if (path.startsWith("thredds/")) {
      file = new File(tdsContext.getThreddsDirectory(), "logs/" + path.substring(8));
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

  private void showFiles(File dir, final String filter, HttpServletResponse res) throws IOException {
    File[] files = dir.listFiles((dir1, name) -> {
      return name.contains(filter);
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