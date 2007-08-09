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

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

import org.apache.log4j.LogManager;

import java.util.Date;

/**
 * Listen for lifecycle events
 */
public class ServletListener implements ServletContextListener { //, HttpSessionListener {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServletListener.class);

  // ServletContextListener
  public void contextInitialized(ServletContextEvent event) {
    //System.out.println("+++ Thredds webapp contextInitialized");
  }

  public void contextDestroyed(ServletContextEvent event) {
    LogManager.shutdown();
    //System.out.println("+++ Thredds webapp contextDestroyed");
  }

  // HttpSessionListener
  public void sessionCreated(HttpSessionEvent e) {
    if (log.isDebugEnabled()) log.debug("++ Session created "+e.getSession().getId());
  }

  public void sessionDestroyed(HttpSessionEvent e) {
    HttpSession session = e.getSession();
    Date created = new Date(session.getCreationTime());
    Date last = new Date(session.getLastAccessedTime());
    if (log.isDebugEnabled()) log.debug("++ Session deleted "+session.getId()+" created"+created+" lastAccessed"+last);
  }
}
