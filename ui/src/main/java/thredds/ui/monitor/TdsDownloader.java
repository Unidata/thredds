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

package thredds.ui.monitor;

import ucar.nc2.util.net.*;
import ucar.nc2.util.CancelTask;

import java.io.*;
import javax.swing.*;

/**
 * Download files from TDS, must have remote management turned on.
 * <pre>
 *   1) log files
 *   2) roots
 * </pre>
 * @author caron
 * @since Apr 24, 2009
 */
public class TdsDownloader {
  static private final String latestServletLog = "threddsServlet.log";

  public enum Type {access, thredds}

  private ManageForm.Data config;
  private Type type;

  private File localDir;
  private JTextArea ta;
  private CancelTask cancel;
  private HTTPSession session;

  TdsDownloader(JTextArea ta, ManageForm.Data config, Type type) throws IOException {
    this.ta = ta;
    this.config = config;
    this.type = type;

    HTTPSession.setGlobalUserAgent("TdsMonitor");
    session = HTTPFactory.newSession(config.getServerPrefix());

    localDir = LogLocalManager.getDirectory(config.server, type.toString());
    if (!localDir.exists() && !localDir.mkdirs()) {
      ta.setText(String.format("Failed to create local directory in = %s%n%n", localDir));
      return;
    }
  }

  // copy remote files to localDir
  public void getRemoteFiles(final CancelTask _cancel) {
    this.cancel = _cancel;

    String urls = config.getServerPrefix() + "/thredds/admin/log/"+type+"/";
    ta.append(String.format("Download URL = %s%n", urls));

    final String contents;
    try {
      contents = HttpClientManager.getContentAsString(session, urls);
      if ((contents == null) || (contents.length() == 0)) {
        ta.append(String.format("Failed to get logs at URL = %s%n%n", urls));
        return;
      } else {
        ta.append(String.format("Logs at URL = %s%n%s%n", urls, contents));
      }
    } catch (Throwable t) {
      ta.append(String.format("Failed to get logs at URL = %s error = %s%n%n", urls, t.getMessage()));
      t.printStackTrace();
      return;
    }

    // update text area in background  http://technobuz.com/2009/05/update-jtextarea-dynamically/
    SwingWorker worker = new SwingWorker<String, Void>() {

      @Override
      protected String doInBackground() throws Exception {
        try {
          ta.append(String.format("Local log files stored in = %s%n%n", localDir));
          String[] lines = contents.split("\n");
          for (String line : lines) {
            new RemoteLog(line.trim());
            if (cancel.isCancel()) {
              break;
            }
          }
        } catch (Throwable t) {
          t.printStackTrace();
        }

        return null;
      }

      public void done() {
        if (cancel.isCancel())
          ta.append(String.format("Download was cancelled for %s%n", type));
        else
          ta.append(String.format("Download complete for %s%n", type));
      }
    };

    // do in background
    worker.execute();
  }

  private class RemoteLog {
    String name;
    long size;
    File localFile;

    RemoteLog(String line) throws HTTPException {
      String[] tokes = line.split(" ");
      name = tokes[0];
      size = Long.parseLong(tokes[1]);

      localFile = new File(localDir, name);
      if (!localFile.exists() || (localFile.length() > size) || name.equals(latestServletLog)) {
        ta.append(String.format("Read RemoteLog length=%6d Kb for %s%n", size/1000, name));
        read();
      } else if (localFile.length() < size) {
        ta.append(String.format("Append RemoteLog length=%6d Kb to local=%6d Kb for %s%n", size/1000, localFile.length()/1000, name));
        append();
      } else {
        ta.append(String.format("Ok RemoteLog length=%6d local=%6d (kb) for %s%n", size/1000, localFile.length()/1000, name));
      }
      ta.setCaretPosition(ta.getText().length());   // needed to get the text to update ?

    }

    void read() throws HTTPException {
      String urls = config.getServerPrefix() + "/thredds/admin/log/"+type+"/" + name;
      ta.append(String.format(" reading %s to %s%n", urls, localFile.getPath()));
      HttpClientManager.copyUrlContentsToFile(session, urls, localFile);
    }

    void append() throws HTTPException {
      String urls = config.getServerPrefix() + "/thredds/admin/log/"+type+"/" + name;
      long start = localFile.length();
      long want = size - start;
      long got = HttpClientManager.appendUrlContentsToFile(session, urls, localFile, start, size);
      if (want == got)
        ta.append(String.format(" append %d bytes to %s %n", got, localFile.getPath()));
      else
        ta.append(String.format(" *** append got=%d want=%d bytes to %s %n", got, want, localFile.getPath()));
    }

    @Override
    public String toString() {
      return "RemoteLog{" +
              "name='" + name + '\'' +
              ", size=" + size +
              '}';
    }
  }

}
