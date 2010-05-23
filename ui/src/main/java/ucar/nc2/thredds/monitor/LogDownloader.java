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

package ucar.nc2.thredds.monitor;

import ucar.nc2.util.net.HttpClientManager;

import java.io.*;
import org.apache.commons.httpclient.auth.CredentialsProvider;

import javax.swing.*;

/**
 * Manage remote log files.
 *
 * @author caron
 * @since Apr 24, 2009
 */
public class LogDownloader {
  static private final String latestServletLog = "threddsServlet.log";

  private String server, type;
  private File localDir;
  private JTextArea ta;

  LogDownloader(JTextArea ta, String server, boolean isAccess) throws IOException {
    this.ta = ta;
    this.server = server;
    this.type = isAccess ? "access" : "thredds";

    localDir = LogLocalManager.getDirectory(server, isAccess);
    if (!localDir.exists() && !localDir.mkdirs()) {
      ta.setText(String.format("Failed to create local directory in = %s%n%n", localDir));
      return;
    }
  }

  public void getRemoteFiles() throws IOException {

    String urls = "http://" + server + "/thredds/admin/log/"+type+"/";
    final String contents = HttpClientManager.getUrlContents(urls, 500);
    if (contents == null) {
      ta.append(String.format("Failed to access logs at URL = %s%n%n", urls));
      return;
    }

    // update text area in background  http://technobuz.com/2009/05/update-jtextarea-dynamically/
    SwingWorker worker = new SwingWorker<String, Void>() {

      @Override
      protected String doInBackground() throws Exception {
        ta.append(String.format("Local log files stored in = %s%n%n", localDir));
        String[] lines = contents.split("\n");
        for (String line : lines) {
          new RemoteLog(line.trim());
          //System.out.printf(" %s == %s %n", line, remoteLog);
          //result.add(remoteLog);
        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      public void done() {
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

    RemoteLog(String line) {
      String[] tokes = line.split(" ");
      name = tokes[0];
      size = Long.parseLong(tokes[1]);

      localFile = new File(localDir, name);
      if (!localFile.exists() || (localFile.length() > size) || name.equals(latestServletLog)) {
        ta.append(String.format("Read RemoteLog length=%d local=%d for %s%n", size, localFile.length(), name));
        read();
      } else if (localFile.length() < size) {
        ta.append(String.format("Append RemoteLog length=%d local=%d for %s%n", size, localFile.length(), name));
        append();
      } else {
        ta.append(String.format("Ok RemoteLog length=%d local=%d for %s%n", size, localFile.length(), name));
      }
      ta.setCaretPosition(ta.getText().length());   // needed to get the text to update ?

    }

    void read() {
      String urls = "http://" + server + "/thredds/admin/log/"+type+"/" + name;
      HttpClientManager.copyUrlContentsToFile(urls, localFile);
      ta.append(String.format(" read %s to %s size=%d%n", urls, localFile.getPath(), localFile.length()));
    }

    void append() {
      String urls = "http://" + server + "/thredds/admin/log/"+type+"/" + name;
      long start = localFile.length();
      long want = size - start;
      long got = HttpClientManager.appendUrlContentsToFile(urls, localFile, start, size);
      if (want == got)
        ta.append(String.format(" append %d bytes to %s %n", got, localFile.getPath()));
      else
        ta.append(String.format(" append got=%d want=%d bytes to %s %n", got, want, localFile.getPath()));
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
