// $Id: CatalogFactoryCancellable.java 50 2006-07-12 16:30:06Z caron $
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

package thredds.catalog.ui;

import thredds.catalog.*;
import thredds.ui.ProgressMonitorTask;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpClient;
import ucar.nc2.util.net.HttpClientManager;

/**
 * A subclass of InvCatalogFactory that allows the reading of a catalog to be cancelled by the user.
 * Pops up a ProgressMonitor widget.
 * @author John Caron
 * @version $Id: CatalogFactoryCancellable.java 50 2006-07-12 16:30:06Z caron $
 */

public class CatalogFactoryCancellable extends InvCatalogFactory {
  private java.awt.Component parent;
  private boolean callbackDone = true, taskDone = true;
  private boolean debug = false;

  /**
   * Constructor.
   *
   * @param parent : put ProgressMonotpr on top of his component; may be null.
   * @param name : name of the InvCatalogFactory
   * @param validate : should CML validation be done?
   *
   * @see thredds.catalog.InvCatalogFactory
   * @see thredds.ui.ProgressMonitor
   */
  public CatalogFactoryCancellable(java.awt.Component parent, String name, boolean validate) {
    super(name, validate);
    this.parent = parent;
  }

  /**
   * Pops up a ProgressMonitor to allow user cancellation while reading the named catalog.
   * This method immediately returns, and the reading is done on a background thread.
   * If successfully read, callback.setCatalog() is called on the awt event thread.
   * If failure, the user will be given a popup error message, and callback.failure() is called..
   *
   * @param catalogName : the URI name that the XML doc is at.
   * @param callbacker : this will be called (from AWT thread) if catalog was successfully called.
   */
  public void readXMLasynch( String catalogName, CatalogSetCallback callbacker) {
    this.callback = callbacker;
    callbackDone = false;
    taskDone = false;

    openTask = new OpenCatalogTask(catalogName);

    thredds.ui.ProgressMonitor pm = new thredds.ui.ProgressMonitor(openTask, 5000, 5000);
    pm.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (debug) System.out.println("ProgressMonitor event  "+e.getActionCommand());
        if (e.getActionCommand().equals("success")) {
          checkFailure();
        } else
          callback.failed();
        callbackDone = true;
      }
    });
    pm.start( parent, "Open catalog "+catalogName, 20);
  }


  private void checkFailure() {
    StringBuilder buff = new StringBuilder();
    openTask.catalog.check( buff);

    if (openTask.catalog.hasFatalError()) {
      String catalogName = openTask.catalog.getName();
      javax.swing.JOptionPane.showMessageDialog(null, "Catalog Read Failed on "+ catalogName+
         "\n"+buff.toString());
      callback.failed();
      return;
    }

    callback.setCatalog( openTask.catalog);
  }

  /**
   * See if this object can be reused.
   * @return true if not compled last task.
   */
  public boolean isBusy() { return !taskDone || !callbackDone; }

  private OpenCatalogTask openTask;
  private CatalogSetCallback callback;

  private class OpenCatalogTask extends ProgressMonitorTask {
    String catalogName;
    URI catalogURI;
    InvCatalogImpl catalog;

    OpenCatalogTask(String catalogName) {
      this.catalogName = catalogName;
    }

    public void run() {

      try {
        catalogURI = new URI(catalogName);
      } catch (URISyntaxException e) {
        catalog = new InvCatalogImpl(catalogName, null, null);
        catalog.appendErrorMessage("**Fatal:  InvCatalogFactory.readXML URISyntaxException on URL (" +
                catalogName + ") " + e.getMessage() + "\n", true);
        success = false;
        done = true;
        taskDone = true;
        return;
      }
      if (debug) System.out.println("CatalogFactoryCancellable run task on " + catalogName);

      if (catalogURI.getScheme().equals("file")) {
        catalog = CatalogFactoryCancellable.super.readXML( catalogURI);
        success = !cancel;
        done = true;
        taskDone = true;
        return;
      }

      GetMethod m = null;
      try {
        m = new GetMethod(catalogName);
        m.setFollowRedirects(true);

        HttpClient client = HttpClientManager.getHttpClient();
        client.executeMethod(m);
        InputStream stream =  m.getResponseBodyAsStream();
        catalog = CatalogFactoryCancellable.super.readXML( stream, catalogURI);

      } catch (IOException e) {
        catalog = new InvCatalogImpl(catalogName, null, null);
        catalog.appendErrorMessage("**Fatal:  InvCatalogFactory.readXML IOException on URL (" +
                catalogName + ") " + e.getMessage() + "\n", true);
        success = false;
        done = true;
        taskDone = true;
        return;

      } finally {
        if (null != m) m.releaseConnection();
      }

      success = !cancel;
      done = true;
      taskDone = true;
    }

    /* old way
  public void run() {
    if (debug) System.out.println("CatalogFactoryCancellable run task on "+catalogName);
    catalog = CatalogFactoryCancellable.super.readXML( catalogName);
    success = !cancel;
    done = true;
    taskDone = true;
  }  */

  }


}