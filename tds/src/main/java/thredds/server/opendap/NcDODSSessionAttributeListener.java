package thredds.server.opendap;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

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