package thredds.servlet;

import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

import org.apache.log4j.LogManager;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Dec 6, 2005
 * Time: 9:58:06 AM
 * To change this template use File | Settings | File Templates.
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
