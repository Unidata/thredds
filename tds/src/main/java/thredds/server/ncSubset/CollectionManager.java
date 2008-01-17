// $Id: $
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
 * @version $Revision$ $Date$
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
