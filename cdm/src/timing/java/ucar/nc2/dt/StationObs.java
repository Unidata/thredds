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

package ucar.nc2.dt;

import ucar.nc2.dt.point.PointObsDatasetFactory;

import java.io.IOException;

/**
 * From Eric Russell
 */
public class StationObs {

  public static void main(String args[]) {
    long start = System.currentTimeMillis();

    try {
      //String url = "http://localhost:8080/thredds/dodsC/metarCollection/Surface_METAR_20060629_0000.nc";
      String url = "dods://motherlode.ucar.edu:8080/thredds/dodsC/station/metar/Surface_METAR_20060629_0000.nc";
      PointObsDataset dataset = PointObsDatasetFactory.open(url);
      System.out.println("running test on " + dataset.getLocationURI());
      DataIterator iter = dataset.getDataIterator(0);
      int count = 0;

      Runtime runtime = Runtime.getRuntime();

      try {
        while (iter.hasNext()) {
          PointObsDatatype pobs = (PointObsDatatype) iter.nextData();
          count += 1;

          if (count % 1000 == 0)
            System.out.println(count+" mem = " + runtime.freeMemory() *.001*.001 + " "+runtime.totalMemory() *.001*.001);
          //if (count > 83327)
          //  System.out.println(count+" mem = " + runtime.freeMemory() *.001*.001+ " " + runtime.totalMemory() *.001*.001);
        }

        long took = System.currentTimeMillis() - start;
        System.out.println("that took = " + took);

      } catch (OutOfMemoryError e) {
        e.printStackTrace();
        System.err.println("OutOfMemoryError after reading " + count + " records (of " + dataset.getDataCount() + ")");
        return;
      } finally {
        dataset.close();
      }

      System.out.println("successfully read " + count + " records");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void showMem(int count, Runtime runtime) {
    System.out.println(count+" mem = " + runtime.freeMemory() *.001*.001+
            " total= " + runtime.totalMemory() *.001*.001 +
            " max= " + runtime.maxMemory() *.001*.001);

  }

}
