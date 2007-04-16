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

package ucar.nc2.thredds.server;

import thredds.util.IO;

import java.io.File;
import java.io.IOException;

import ucar.unidata.util.StringUtil;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class TestMotherlodeInventory {
    static String server = "http://motherlode.ucar.edu:9080/";
    static String pathStart = "/thredds/modelInventory/fmrc/NCEP/";
    static String[] invPaths = {
      "DGEX/CONUS_12km/",
      "DGEX/Alaska_12km/",

      "GFS/Alaska_191km/",
      "GFS/CONUS_80km/",
      "GFS/CONUS_95km/",
      "GFS/CONUS_191km/",
      "GFS/Global_0p5deg/",
      "GFS/Global_onedeg/",
      "GFS/Global_2p5deg/",
      "GFS/Hawaii_160km/",
      "GFS/N_Hemisphere_381km/",
      "GFS/Puerto_Rico_191km/",

      "NAM/Alaska_11km/",
      "NAM/Alaska_22km/", 
      "NAM/Alaska_45km/noaaport/",
      "NAM/Alaska_45km/conduit/",
      "NAM/Alaska_95km/",
      "NAM/CONUS_12km/",
      "NAM/CONUS_20km/surface/",
      "NAM/CONUS_20km/selectsurface/",
      "NAM/CONUS_20km/noaaport/",
      "NAM/CONUS_40km/noaaport/",
      "NAM/CONUS_40km/conduit/",
      "NAM/CONUS_80km/",
      "NAM/Polar_90km/",

      "RUC2/CONUS_20km/surface/",
      "RUC2/CONUS_20km/pressure/",
      "RUC2/CONUS_20km/hybrid/",
      "RUC2/CONUS_40km/",
      "RUC/CONUS_80km/",

      "NDFD/CONUS_5km/",
    };

  public static void main(String args[]) {
    String outputDir = "R:/testdata/motherlode/grid/inv/";
    for (int i = 0; i < invPaths.length; i++) {
      String invPath = invPaths[i];
      String name = StringUtil.replace(invPath,'/',"-");
      File file = new File(outputDir+name+".html");
      try {
        IO.readURLtoFileWithExceptions(server+pathStart+invPath, file);
        System.out.println("Copied "+invPath);
      } catch (IOException ioe) {
        System.out.println("Failed "+invPath);
      }
    }
  }



}
