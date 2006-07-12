// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package thredds.servlet;

import java.io.*;
import javax.servlet.http.*;

/**
 * Abstract superclass for THREDDS servlets.
 * Provides some common services for servlets: debugging, logging, and file serving.
 * @author caron
 * @version $Revision$ $Date$
 */
public abstract class AbstractServlet extends HttpServlet {
  protected org.slf4j.Logger log;
  protected String rootPath, contentPath;

  // must end with "/"
  protected abstract String getPath(); 
  protected abstract void makeDebugActions();

  public void init() throws javax.servlet.ServletException {
    ServletUtil.initDebugging(this); // read debug flags
    rootPath = ServletUtil.getRootPath(this);
    contentPath = ServletUtil.getContentPath(this) + getPath();

    // init logging
    ServletUtil.initLogging(this);
    log = org.slf4j.LoggerFactory.getLogger(getClass());
    ServletUtil.logServerSetup( this.getClass().getName() + ".init()" );

    // first time, create content directory
    String initialContentPath = ServletUtil.getInitialContentPath(this) + getPath();
    File initialContentFile = new File( initialContentPath);
    if (initialContentFile.exists()) {
      try {
        if (ServletUtil.copyDir(initialContentPath, contentPath))
          log.info("copyDir "+initialContentPath+" to "+contentPath);
      } catch (IOException ioe) {
        log.error("failed to copyDir "+initialContentPath+" to "+contentPath, ioe);
      }
    }

    // debug actions
    makeDebugActions();

    log.info("--- initialized "+getClass().getName());
  }

}
