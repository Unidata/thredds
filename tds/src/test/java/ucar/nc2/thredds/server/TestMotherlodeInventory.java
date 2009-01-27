// $Id: $
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

import ucar.nc2.util.IO;

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
    static String server = "http://motherlode.ucar.edu:8080/";
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
      "NAM/CONUS_12km_conduit/",
      "NAM/CONUS_20km/surface/",
      "NAM/CONUS_20km/selectsurface/",
      "NAM/CONUS_20km/noaaport/",
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
    System.out.println("Copy inventory files to "+outputDir);
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
