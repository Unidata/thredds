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

import org.apache.commons.httpclient.auth.CredentialsProvider;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateFromString;
import ucar.nc2.util.net.HttpClientManager;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;/*
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

/**
 * Class Description
 *
 * @author caron
 * @since May 6, 2010
 */


public class LogLocalManager {
  static File topDir;

  static {
    // decide where to put the logs locally
    String dataDir = System.getProperty( "tdsMonitor.dataDir" );
    if (dataDir != null) {
      topDir = new File(dataDir);
    } else {
      String homeDir = System.getProperty( "user.home" );
      topDir = new File(homeDir, "tdsMonitor");
    }
    System.out.printf("logs stored at= %s%n", topDir);
  }

  static File getDirectory(String server, boolean isAccess) throws UnsupportedEncodingException {
    String type = isAccess ? "access" : "thredds";
    String cleanServer = java.net.URLEncoder.encode(server, "UTF8");
    return new File(topDir, cleanServer+"/"+type);
  }

  private String server;
  private boolean isAccess;

  LogLocalManager(String server, boolean isAccess) {
    this.server = server;
    this.isAccess = isAccess;
  }

  List<File> localFiles;

  public List<File> getLocalFiles(Date start, Date end) throws IOException {
    File localDir = getDirectory(server, isAccess);
    String demark = isAccess ? "access.#yyyy-MM-dd" : "threddsServlet.log.yyyy-MM-dd-HH";
    DateFormatter df = new DateFormatter();

    localFiles = new ArrayList<File>();
    for (File f : localDir.listFiles()) {
      Date d = DateFromString.getDateUsingDemarkatedCount(f.getName(), demark, '#');
      System.out.printf(" %s == %s%n", f.getName(), df.toDateTimeStringISO(d));
      if (start != null && d.before(start)) continue;
      if (end != null && d.after(end)) continue;
      localFiles.add(f);
    }
    return localFiles;
  }

  Date getStartDate() {
    if (localFiles == null) return null;
    if (localFiles.size() == 0) return null;
    File f = localFiles.get(0);
    String demark = isAccess ? "access.#yyyy-MM-dd" : "threddsServlet.log.yyyy-MM-dd-HH";
    return DateFromString.getDateUsingDemarkatedCount(f.getName(), demark, '#');
  }

  Date getEndDate() {
    if (localFiles == null) return null;
    if (localFiles.size() == 0) return null;
    File f = localFiles.get(localFiles.size()-1);
    String demark = isAccess ? "access.#yyyy-MM-dd" : "threddsServlet.log.yyyy-MM-dd-HH";
    return DateFromString.getDateUsingDemarkatedCount(f.getName(), demark, '#');
  }

}
