// $Id$
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

package ucar.nc2.thredds.server;

import thredds.catalog.crawl.CatalogCrawler;
import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvAccess;
import thredds.catalog.DataType;

import java.io.PrintStream;
import java.io.IOException;
import java.util.List;

import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.dt.GridDatatype;
import ucar.ma2.Array;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: May 20, 2006
 * Time: 4:31:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimeTDS {
  ThreddsDataFactory tdf = new ThreddsDataFactory();

  TimeTDS(String catUrl) {

    CatalogCrawler.Listener listener = new CatalogCrawler.Listener() {
      public void getDataset(InvDataset dd) {
        extractDatasetInfo(dd, System.out);
      }
    };
    CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.USE_ALL_DIRECT, false, listener);

    long start = System.currentTimeMillis();
    crawler.crawl(catUrl, null, System.out);
    long took = System.currentTimeMillis() - start;
    System.out.println("that took= " + took + " msecs");
  }

  // breadth first
  void extractDatasetInfo(InvDataset dd, PrintStream out) {
    System.out.println(" -dataset= " + dd.getName());

    ThreddsDataFactory.Result tdata = null;
    try {
      try {
        tdata = tdf.openDatatype(dd, null);
        if (tdata.fatalError) {
          out.println(" --ERROR " + tdata.errLog);
          return;
        }
        out.println(" *Opened TYPE " + tdata.dtype + " " + tdata.location);
        if (tdata.dtype == DataType.GRID) {
          List grids = tdata.gridDataset.getGrids();
          if (grids.size() > 0) {
            GridDatatype grid = (GridDatatype) grids.get(0);
            Array data = grid.readDataSlice(0, -1, -1, -1);
            out.println(" -- read " + data.getSize());
          } else {
            out.println(" -- NO GRIDS! ");            
          }
        }
        
      } catch (Throwable e) {
        out.println(" --ERROR ");
        e.printStackTrace(out);
        return;
      }

    } finally {
      try {
        if (tdata != null) tdata.close();
      } catch (IOException ioe) {
      }
    }
  }

  public static void main(String args[]) {

    new TimeTDS("http://localhost:8080/thredds/catalog.xml");
  }

}
