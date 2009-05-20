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

package thredds.server.ncSubset;

import ucar.nc2.units.DateFromString;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;

/**
 * Manage collection of files.
 * temp kludge until we consolidate NcML agg and DatasetScan (CrawlableDataset)
 *
 * @author caron
 */
public class CollectionManager {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CollectionManager.class);
  private ArrayList<MyFile> scanList = new ArrayList<MyFile>(); // current set of DirectoryScan for scan elements
  private Date latest = null;
  private String dirName;

  public CollectionManager(String dirName, FileFilter ff, String dateFormatString) {
    this.dirName = dirName;
    File dir = new File(dirName);
    File[] files = dir.listFiles(ff);
    for (File f : files) {
      Date d = DateFromString.getDateUsingSimpleDateFormat(f.getName(), dateFormatString);
      add(f, d);
    }
  }

  public String toString() { return dirName; }

  
  public List<MyFile> getList() {
    return scanList;
  }

  public Date getLatest() {
    return latest;
  }

  public void add(File file, Date d) {
    scanList.add(new MyFile(file, d));
    if ((latest == null) || d.after(latest))
      latest = d;
  }

  public boolean remove(MyFile file) {
    return scanList.remove(file);
  }

  public MyFile find(File file) {
    int index = scanList.indexOf(file);
    return (index >= 0) ?  scanList.get(index) : null;
  }

  public List<MyFile> after( Date date) {
    ArrayList<MyFile> result = new ArrayList<MyFile>();
    for (MyFile f : scanList) {
      if ((date == null) || f.date.after(date))
        result.add(f);
    }
    return result;
  }

  /**
   * Encapsolate a file that was scanned.
   */
  public class MyFile implements Comparable {
    public File file;
    public Date date;

    MyFile(File file, Date d) {
      this.date = d;
      this.file = file;
    }

    public int compareTo(Object o) {
      MyFile om = (MyFile) o;
      return date.compareTo( om.date);
    }

    public String toString() { return file.getAbsolutePath(); }
  }


}
