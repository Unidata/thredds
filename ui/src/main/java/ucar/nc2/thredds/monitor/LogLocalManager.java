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

import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateFromString;
import ucar.nc2.units.DateRange;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;/*

/**
 * Manage local log files.
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

  static File getDirectory(String server, boolean isAccess) {
    String type = isAccess ? "access" : "thredds";
    String cleanServer = null;
    try {
      cleanServer = java.net.URLEncoder.encode(server, "UTF8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);   // wont happen
    }
    return new File(topDir, cleanServer+"/"+type);
  }

  /////////////////////////////////////////////
  private static final String specialLog = "threddsServlet.log";
  private String server;
  private boolean isAccess;
  private List<FileDateRange> localFiles;
  private SimpleDateFormat localFormat;
  private int filenameDatePos; // where the date starts in the filename

  LogLocalManager(String server, boolean isAccess) {
    this.server = server;
    this.isAccess = isAccess;

    // default is local time zone
    filenameDatePos = isAccess ? "access.".length() : "threddsServlet.log.".length();
    String format = isAccess ? "yyyy-MM-dd" : "yyyy-MM-dd-HH";
    localFormat = new SimpleDateFormat(format, Locale.US );
  }

  public List<FileDateRange> getLocalFiles(Date start, Date end) {
    File localDir = getDirectory(server, isAccess);

    List<FileDateRange> list = new ArrayList<FileDateRange>();
    for (File f : localDir.listFiles()) {
      if (f.getName().endsWith(".zip")) continue;
      list.add(new FileDateRange(f));
    }
    Collections.sort(list, new ServletFileCompare());

    // assign time range
    FileDateRange prev = null;
    for (FileDateRange fdr : list) {
      if (prev != null)
        prev.end = new Date(fdr.start.getTime()-1);
      prev = fdr;
    }
    // deal with last one
    if (list.size() > 1) {
      FileDateRange first = list.get(0);
      long interval = first.end.getTime() - first.start.getTime();
      if (isAccess) {
        FileDateRange last = list.get(list.size()-1);
        last.end = new Date(last.start.getTime()+interval);
      } else {
        FileDateRange nextLast = list.get(list.size()-2);
        nextLast.end = new Date(nextLast.start.getTime()+interval);
        FileDateRange last = list.get(list.size()-1);
        last.start = nextLast.end;
        last.end = new Date(last.start.getTime()+interval);
      }
    }

    // filter by time range
    localFiles = new ArrayList<FileDateRange>();
    for (FileDateRange have : list) {
      if (start != null && start.after(have.end)) continue;
      if (end != null && have.start.after(end)) continue;
      localFiles.add(have);
    }
    return localFiles;
  }

  private class ServletFileCompare implements Comparator<FileDateRange> {
    public int compare(FileDateRange o1, FileDateRange o2) {
      if (o1.f.getName().equals(specialLog)) return 1;
      if (o2.f.getName().equals(specialLog)) return -1;
      return o1.f.getName().compareTo(o2.f.getName());
    }
  }

  Date getStartDate() {
    if (localFiles == null) return null;
    if (localFiles.size() == 0) return null;
    FileDateRange f = localFiles.get(0);
    return f.start;
  }

  Date getEndDate() {
    if (localFiles == null) return null;
    if (localFiles.size() == 0) return null;
    FileDateRange f = localFiles.get(localFiles.size()-1);
    return f.end;
  }

  Date extractDate(String name) {
    if (!isAccess && name.equals(specialLog))
      return new Date(); // LOOK LAME
    else {
      String filenameDate = name.substring( filenameDatePos);
      try {
        return localFormat.parse( filenameDate );
      } catch (ParseException e) {
        e.printStackTrace();
        return null;
      }
    }
  }

  public String getServer() {
    return server;
  }

  private DateFormatter df = new DateFormatter();

  public class FileDateRange {
    File f;
    Date start, end;

    FileDateRange(File f) {
      this.f = f;
      this.start = extractDate(f.getName());
      System.out.printf(" %s == %s%n", f.getName(), df.toDateTimeStringISO(start));
    }
  }

}
