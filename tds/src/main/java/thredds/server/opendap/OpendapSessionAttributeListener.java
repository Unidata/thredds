/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.opendap;

import opendap.servlet.GuardedDataset;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

public class OpendapSessionAttributeListener implements HttpSessionAttributeListener {
  //static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OpendapSessionAttributeListener.class);

   // HttpSessionAttributeListener
    public void attributeRemoved(HttpSessionBindingEvent e) {

      if (e.getValue() instanceof GuardedDataset) {
        GuardedDataset gdataset = (GuardedDataset) e.getValue();
        gdataset.close();
        //System.out.printf(" close gdataset %s in session %s %n", gdataset, e.getSession().getId());
        //if (log.isDebugEnabled()) log.debug(" close gdataset " + gdataset + " in session " + e.getSession().getId());
      }
    }

    public void attributeAdded(HttpSessionBindingEvent httpSessionBindingEvent) { }
    public void attributeReplaced(HttpSessionBindingEvent httpSessionBindingEvent) { }
}