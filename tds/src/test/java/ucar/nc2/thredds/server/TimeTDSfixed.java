// $Id: TimeTDSfixed.java 51 2006-07-12 17:13:13Z caron $
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

package ucar.nc2.thredds.server;

import thredds.catalog.crawl.CatalogCrawler;
import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalogRef;
import ucar.nc2.constants.FeatureType;

import java.io.PrintStream;
import java.io.IOException;
import java.util.List;

import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.ma2.Array;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: May 20, 2006
 * Time: 4:31:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimeTDSfixed {
  public static final int EPSILON = 20;

  ThreddsDataFactory tdf = new ThreddsDataFactory();
  long start, count, testTime;
  CancelTaskImpl cancel;
  PrintStream out;

  TimeTDSfixed(String catUrl, int secs, PrintStream ps) {
    testTime = 1000 * secs;
    this.out = ps;

    CatalogCrawler.Listener listener = new CatalogCrawler.Listener() {
      public void getDataset(InvDataset dd, Object context) {
        extractDatasetInfo(dd, out);
        count++;
        long took = System.currentTimeMillis() - start;
        if (took > testTime) {
          cancel.isCancel = true;
        }
      }
      public boolean getCatalogRef(InvCatalogRef dd, Object context) { return true; }
      
    };
    CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.USE_ALL_DIRECT, false, listener);

    cancel = new CancelTaskImpl();
    start = System.currentTimeMillis();
    crawler.crawl(catUrl, cancel, this.out, null);
    long took = System.currentTimeMillis() - start;
    System.out.println("FINISHED "+count+" datasets in " + took + " msecs ");
  }

  class CancelTaskImpl implements CancelTask {
    boolean isCancel = false;
    public boolean isCancel() { return isCancel; }
    public void setError(String msg) { }
  }

  // breadth first
  void extractDatasetInfo(InvDataset dd, PrintStream out) {
    if (out != null) out.println(" -dataset= " + dd.getName());

    ThreddsDataFactory.Result tdata = null;
    try {
      try {
        tdata = tdf.openFeatureDataset(dd, null);
        if (tdata.fatalError) {
          if (out != null) out.println(" --ERROR " + tdata.errLog);
          return;
        }
        if (out != null) out.println(" *Opened TYPE " + tdata.featureType + " " + tdata.location);
        if (tdata.featureType == FeatureType.GRID) {
          GridDataset gds = (GridDataset) tdata.featureDataset;
          List grids = gds.getGrids();
          if (grids.size() > 0) {
            GridDatatype grid = (GridDatatype) grids.get(0);
            Array data = grid.readDataSlice(0, -1, -1, -1);
            if (out != null) out.println(" -- read " + data.getSize());
          } else {
            if (out != null) out.println(" -- NO GRIDS! ");
          }
        }

      } catch (Throwable e) {
        if (out != null) out.println(" --ERROR ");
        e.printStackTrace(out);
        return;
      }

    } finally {
      try {
        if ((tdata != null) && (tdata.featureDataset != null)) tdata.featureDataset.close();
      } catch (IOException ioe) {
      }
    }
  }

  public static void main(String args[]) {

    //new TimeTDSfixed("http://localhost:8080/thredds/catalog.xml", 5, null);
    new TimeTDSfixed("http://motherlode.ucar.edu:8080/thredds/catalog.xml", 90, null);
    new TimeTDSfixed("http://motherlode.ucar.edu:9080/thredds/catalog.xml", 90, null);
    new TimeTDSfixed("http://motherlode.ucar.edu:8080/thredds/catalog.xml", 60, null);
    new TimeTDSfixed("http://motherlode.ucar.edu:9080/thredds/catalog.xml", 60, null);
  }

}
