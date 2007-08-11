/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.ncml2;

import ucar.nc2.*;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dataset.NetcdfDatasetFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.StringUtil;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;

import java.util.*;
import java.io.IOException;
import java.io.File;

import thredds.util.DateFromString;

/**
 * @author caron
 * @since Aug 8, 2007
 */
public class DatasetCollection {
  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetCollection.class);

  protected List<DirectoryScan> scanList = new ArrayList<DirectoryScan>(); // current set of DirectoryScan for scan elements

  private Aggregation.Type type;
  private String dimName;
  private boolean debugOpenFile, debugRead;

  /**
   * Add a scan elemnt
   *
   * @param dirName             scan this directory
   * @param suffix              filter on this suffix (may be null)
   * @param regexpPatternString include if full name matches this regular expression (may be null)
   * @param dateFormatMark      create dates from the filename (may be null)
   * @param enhance             should files bne enhanced?
   * @param subdirs             equals "false" if should not descend into subdirectories
   * @param olderThan           files must be older than this time (now - lastModified >= olderThan); must be a time unit, may ne bull
   * @throws IOException if I/O error
   */
  public void addDirectoryScan(String dirName, String suffix, String regexpPatternString, String dateFormatMark, String enhance, String subdirs, String olderThan) throws IOException {
    DirectoryScan d = new DirectoryScan(dirName, suffix, regexpPatternString, dateFormatMark, enhance, subdirs, olderThan);
    scanList.add(d);
    if (dateFormatMark != null)
      isDate = true;
  }
  
  /**
   * Scan the directory(ies) and create nested Aggregation.Dataset objects.
   * Directories are scanned recursively, by calling File.listFiles().
   * Sort by date if it exists, else filename.
   *
   * @param result     add to this List objects of type Aggregation.Dataset
   * @param cancelTask allow user to cancel
   * @throws IOException if io error
   */
  protected void scan(List<Dataset> result, CancelTask cancelTask) throws IOException {

    // Directories are scanned recursively, by calling File.listFiles().
    List<MyFile> fileList = new ArrayList<MyFile>();
    for (DirectoryScan dir : scanList) {
      dir.scanDirectory(fileList, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }

    // extract date if possible, before sorting
    for (MyFile myf : fileList) {
      // optionally parse for date
      if (null != myf.dir.dateFormatMark) {
        String filename = myf.file.getName();
        myf.dateCoord = DateFromString.getDateUsingDemarkatedCount(filename, myf.dir.dateFormatMark, '#');
        myf.dateCoordS = formatter.toDateTimeStringISO(myf.dateCoord);
        if (debugScan) System.out.println("  adding " + myf.file.getAbsolutePath() + " date= " + myf.dateCoordS);
      } else {
        if (debugScan) System.out.println("  adding " + myf.file.getAbsolutePath());
      }
    }

    // Sort by date if it exists, else filename.
    Collections.sort(fileList, new Comparator<MyFile>() {
      public int compare(MyFile mf1, MyFile mf2) {
        if (isDate)
          return mf1.dateCoord.compareTo(mf2.dateCoord);
        else
          return mf1.file.getName().compareTo(mf2.file.getName());
      }
    });

    // now add the ordered list of Datasets to the result List
    for (MyFile myf : fileList) {
      String location = myf.file.getAbsolutePath();
      String coordValue = (type == Aggregation.Type.JOIN_NEW) || (type == Aggregation.Type.JOIN_EXISTING_ONE) || (type == Aggregation.Type.FORECAST_MODEL_COLLECTION) ? myf.dateCoordS : null;
      Dataset ds = makeDataset(location, location, null, coordValue, myf.dir.enhance, null);
      ds.coordValueDate = myf.dateCoord;
      result.add(ds);

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }


  /**
   * Encapsolate a file that was scanned.
   * Created in scanDirectory()
   */




}
