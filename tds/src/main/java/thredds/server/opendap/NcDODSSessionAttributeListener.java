package thredds.server.opendap;

import thredds.server.opendap.NcDODSServlet;
import thredds.server.opendap.GuardedDatasetImpl;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Dec 6, 2005
 * Time: 10:14:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class NcDODSSessionAttributeListener  implements HttpSessionAttributeListener {

   // HttpSessionAttributeListener
    public void attributeRemoved(HttpSessionBindingEvent e) {
      //System.out.println( "HttpSessionAttribute removed "+e.getName()+" "+e.getSession().getId());

      if (e.getName().equals(NcDODSServlet.GDATASET)) {
        GuardedDatasetImpl gdataset = (GuardedDatasetImpl) e.getValue();
        if (null != gdataset) {
          gdataset.close();
          //System.out.println( "  gdataset closed "+gdataset);
        }
      }
    }

    public void attributeAdded(HttpSessionBindingEvent httpSessionBindingEvent) { }
    public void attributeReplaced(HttpSessionBindingEvent httpSessionBindingEvent) { }
}