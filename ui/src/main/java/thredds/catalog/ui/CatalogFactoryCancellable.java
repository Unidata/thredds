/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.catalog.ui;

import thredds.catalog.*;
import thredds.ui.ProgressMonitorTask;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
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
   * @return true if not completed last task.
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

        int statusCode = client.executeMethod(m);

        if (statusCode == 404)
          throw new FileNotFoundException(m.getPath() + " " + m.getStatusLine());

        if (statusCode >= 300)
          throw new IOException(m.getPath() + " " + m.getStatusLine());
        
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