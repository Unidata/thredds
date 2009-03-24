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

import thredds.filesystem.MDirectory;
import thredds.filesystem.Manager;
import thredds.servlet.PathMatcher;

import java.io.*;

import ucar.unidata.util.StringUtil;

/**
 * Class Description.
 *
 * @author caron
 * @since Mar 23, 2009
 */
public class TestFileSystem {
  static String prefix = "/thredds/catalog/";

  static String roots =
          "terminal/level3/IDD,/data/ldm/pub/native/radar/level3/\n" +
          "terminal/level3,/data/ldm/pub/native/radar/level3/\n" +
          "station/soundings,/data/ldm/pub/native/profiler/bufr/profiler3/\n" +
          "station/profiler/wind/1hr,/data/ldm/pub/native/profiler/wind/01hr/\n" +
          "station/profiler/wind/06min,/data/ldm/pub/native/profiler/wind/06min/\n" +
          "station/profiler/RASS/1hr,/data/ldm/pub/native/profiler/RASS/01hr/\n" +
          "station/profiler/RASS/06min,/data/ldm/pub/native/profiler/RASS/06min/\n" +
          "station/metar,/data/ldm/pub/decoded/netcdf/surface/metar/\n" +
          "satellite/WV,/data/ldm/pub/native/satellite/WV/\n" +
          "satellite/VIS,/data/ldm/pub/native/satellite/VIS/\n" +
          "satellite/SOUND-VIS,/data/ldm/pub/native/satellite/SOUND-VIS/\n" +
          "satellite/SOUND-7.43,/data/ldm/pub/native/satellite/SOUND-7.43/\n" +
          "satellite/SOUND-7.02,/data/ldm/pub/native/satellite/SOUND-7.02/\n" +
          "satellite/SOUND-6.51,/data/ldm/pub/native/satellite/SOUND-6.51/\n" +
          "satellite/SOUND-4.45,/data/ldm/pub/native/satellite/SOUND-4.45/\n" +
          "satellite/SOUND-3.98,/data/ldm/pub/native/satellite/SOUND-3.98/\n" +
          "satellite/SOUND-14.06,/data/ldm/pub/native/satellite/SOUND-14.06/\n" +
          "satellite/SOUND-11.03,/data/ldm/pub/native/satellite/SOUND-11.03/\n" +
          "satellite/SFC-T/SUPER-NATIONAL_1km,/data/ldm/pub/native/satellite/SFC-T/SUPER-NATIONAL_1km/\n" +
          "satellite/PW,/data/ldm/pub/native/satellite/PW/\n" +
          "satellite/LI,/data/ldm/pub/native/satellite/LI/\n" +
          "satellite/IR,/data/ldm/pub/native/satellite/IR/\n" +
          "satellite/CTP,/data/ldm/pub/native/satellite/CTP/\n" +
          "satellite/3.9/WEST-CONUS_4km,/data/ldm/pub/native/satellite/3.9/WEST-CONUS_4km/\n" +
          "satellite/3.9/PR-REGIONAL_4km,/data/ldm/pub/native/satellite/3.9/PR-REGIONAL_4km/\n" +
          "satellite/3.9/HI-REGIONAL_4km,/data/ldm/pub/native/satellite/3.9/HI-REGIONAL_4km/\n" +
          "satellite/3.9/EAST-CONUS_4km,/data/ldm/pub/native/satellite/3.9/EAST-CONUS_4km/\n" +
          "satellite/3.9/AK-REGIONAL_8km,/data/ldm/pub/native/satellite/3.9/AK-REGIONAL_8km/\n" +
          "satellite/13.3,/data/ldm/pub/native/satellite/13.3/\n" +
          "satellite/12.0,/data/ldm/pub/native/satellite/12.0/\n" +
          "restrict,/opt/tds-test/content/thredds/public/\n" +
          "nexrad/level3/IDD,/data/ldm/pub/native/radar/level3/\n" +
          "nexrad/level3/CCS039,/data/ldm/pub/casestudies/ccs039/images/radar/nids\n" +
          "nexrad/level3,/data/ldm/pub/native/radar/level3/\n" +
          "nexrad/level2/IDD,/data/ldm/pub/native/radar/level2\n" +
          "nexrad/level2/CCS039,/data/ldm/pub/casestudies/ccs039/images/radar/level2\n" +
          "nexrad/level2,/data/ldm/pub/native/radar/level2/\n" +
          "nexrad/composite/nws,/data/ldm/pub/native/radar/10km_mosaic/\n" +
          "nexrad/composite/gini,/data/ldm/pub/native/radar/composite/gini/\n" +
          "nexrad/composite/1km/files,/data/ldm/pub/native/radar/composite/grib2/\n" +
          "modelsNc/NCEP/SST/Global_5x2p5deg,/data/ldm/pub/decoded/netcdf/grid/NCEP/SST/Global_5x2p5deg/\n" +
          "modelsNc/NCEP/SST/Global_2x2deg,/data/ldm/pub/decoded/netcdf/grid/NCEP/SST/Global_2x2deg/\n" +
          "modelsNc/NCEP/RUC2/CONUS_40km,/data/ldm/pub/decoded/netcdf/grid/NCEP/RUC2/CONUS_40km/\n" +
          "modelsNc/NCEP/RUC/CONUS_80km,/data/ldm/pub/decoded/netcdf/grid/NCEP/RUC/CONUS_80km/\n" +
          "modelsNc/NCEP/OCEAN/Global_5x2p5deg,/data/ldm/pub/decoded/netcdf/grid/NCEP/OCEAN/Global_5x2p5deg/\n" +
          "modelsNc/NCEP/NAM/CONUS_80km,/data/ldm/pub/decoded/netcdf/grid/NCEP/NAM/CONUS_80km/\n" +
          "modelsNc/NCEP/GFS/Global_5x2p5deg,/data/ldm/pub/decoded/netcdf/grid/NCEP/GFS/Global_5x2p5deg/\n" +
          "modelsNc/NCEP/GFS/Extended_Global_5p0deg,/data/ldm/pub/decoded/netcdf/grid/NCEP/GFS/Extended_Global_5p0deg/\n" +
          "modelsNc/NCEP/GFS/CONUS_80km,/data/ldm/pub/decoded/netcdf/grid/NCEP/GFS/CONUS_80km/\n" +
          "model/NCEP/RUC2/CONUS_20km/surface,/data/ldm/pub/native/grid/NCEP/RUC2/CONUS_20km/surface/\n" +
          "model/NCEP/RUC2/CONUS_20km/pressure,/data/ldm/pub/native/grid/NCEP/RUC2/CONUS_20km/pressure/\n" +
          "model/NCEP/RUC2/CONUS_20km/hybrid,/data/ldm/pub/native/grid/NCEP/RUC2/CONUS_20km/hybrid/\n" +
          "model/NCEP/RUC/CONUS_80km,/data/ldm/pub/native/grid/NCEP/RUC/CONUS_80km/\n" +
          "model/NCEP/NDFD/CONUS_5km,/data/ldm/pub/native/grid/NCEP/NDFD/CONUS_5km/\n" +
          "model/NCEP/NAM/Polar_90km,/data/ldm/pub/native/grid/NCEP/NAM/Polar_90km/\n" +
          "model/NCEP/NAM/CONUS_80km,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_80km/\n" +
          "model/NCEP/NAM/CONUS_40km/conduit,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_40km/conduit/\n" +
          "model/NCEP/NAM/CONUS_20km/surface,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_20km/surface/\n" +
          "model/NCEP/NAM/CONUS_20km/selectsurface,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_20km/selectsurface/\n" +
          "model/NCEP/NAM/CONUS_20km/noaaport,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_20km/noaaport/\n" +
          "model/NCEP/NAM/CONUS_12km,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_12km/\n" +
          "model/NCEP/NAM/Alaska_95km,/data/ldm/pub/native/grid/NCEP/NAM/Alaska_95km/\n" +
          "model/NCEP/NAM/Alaska_45km/noaaport,/data/ldm/pub/native/grid/NCEP/NAM/Alaska_45km/noaaport/\n" +
          "model/NCEP/NAM/Alaska_45km/conduit,/data/ldm/pub/native/grid/NCEP/NAM/Alaska_45km/conduit/\n" +
          "model/NCEP/NAM/Alaska_22km,/data/ldm/pub/native/grid/NCEP/NAM/Alaska_22km/\n" +
          "model/NCEP/NAM/Alaska_11km,/data/ldm/pub/native/grid/NCEP/NAM/Alaska_11km/\n" +
          "model/NCEP/GFS/Puerto_Rico_191km,/data/ldm/pub/native/grid/NCEP/GFS/Puerto_Rico_191km/\n" +
          "model/NCEP/GFS/N_Hemisphere_381km,/data/ldm/pub/native/grid/NCEP/GFS/N_Hemisphere_381km/\n" +
          "model/NCEP/GFS/Hawaii_160km,/data/ldm/pub/native/grid/NCEP/GFS/Hawaii_160km/\n" +
          "model/NCEP/GFS/Global_onedeg,/data/ldm/pub/native/grid/NCEP/GFS/Global_onedeg/\n" +
          "model/NCEP/GFS/Global_2p5deg,/data/ldm/pub/native/grid/NCEP/GFS/Global_2p5deg/\n" +
          "model/NCEP/GFS/Global_0p5deg,/data/ldm/pub/native/grid/NCEP/GFS/Global_0p5deg/\n" +
          "model/NCEP/GFS/CONUS_95km,/data/ldm/pub/native/grid/NCEP/GFS/CONUS_95km/\n" +
          "model/NCEP/GFS/CONUS_80km,/data/ldm/pub/native/grid/NCEP/GFS/CONUS_80km/\n" +
          "model/NCEP/GFS/CONUS_191km,/data/ldm/pub/native/grid/NCEP/GFS/CONUS_191km/\n" +
          "model/NCEP/GFS/Alaska_191km,/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/\n" +
          "model/NCEP/DGEX/CONUS_12km,/data/ldm/pub/native/grid/NCEP/DGEX/CONUS_12km/\n" +
          "model/NCEP/DGEX/Alaska_12km,/data/ldm/pub/native/grid/NCEP/DGEX/Alaska_12km/\n" +
          "gis/test,/data/thredds/gis/\n" +
          "galeon/testdata,/data/thredds/galeon/\n" +
          "galeon/ndfd/testdata,/data/ldm/pub/native/grid/NCEP/NDFD/CONUS_5km/\n" +
          "galeon/global/testdata,/data/ldm/pub/native/grid/NCEP/GFS/Global_0p5deg/\n" +
          "fmrc/Unidata/rtmodel/nmm/files,/data/ldm/pub/rtmodel/\n" +
          "fmrc/Unidata/rtmodel/nmm-alt/files,/data/ldm/pub/rtmodel/\n" +
          "fmrc/NCEP/SREF/CONUS_40km/pgrb_biasc,/data/ldm/pub/native/grid/NCEP/SREF/CONUS_40km/pgrb_biasc/\n" +
          "fmrc/NCEP/SREF/CONUS_40km/ensprod_biasc,/data/ldm/pub/native/grid/NCEP/SREF/CONUS_40km/ensprod_biasc/\n" +
          "fmrc/NCEP/RUC2/CONUS_40km,/data/ldm/pub/native/grid/NCEP/RUC2/CONUS_40km/\n" +
          "fmrc/NCEP/RUC2/CONUS_20km/surface,/data/ldm/pub/native/grid/NCEP/RUC2/CONUS_20km/surface/\n" +
          "fmrc/NCEP/RUC2/CONUS_20km/pressure,/data/ldm/pub/native/grid/NCEP/RUC2/CONUS_20km/pressure/\n" +
          "fmrc/NCEP/RUC2/CONUS_20km/hybrid,/data/ldm/pub/native/grid/NCEP/RUC2/CONUS_20km/hybrid/\n" +
          "fmrc/NCEP/RUC/CONUS_80km,/data/ldm/pub/native/grid/NCEP/RUC/CONUS_80km/\n" +
          "fmrc/NCEP/NEWGBXNDFD/CONUS_5km,/data/ldm/pub/native/grid/NCEP/NEWGBXNDFD/CONUS_5km/\n" +
          "fmrc/NCEP/NDFD/CONUS_5km,/data/ldm/pub/native/grid/NCEP/NDFD/CONUS_5km/\n" +
          "fmrc/NCEP/NAM/Polar_90km,/data/ldm/pub/native/grid/NCEP/NAM/Polar_90km/\n" +
          "fmrc/NCEP/NAM/CONUS_80km,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_80km/\n" +
          "fmrc/NCEP/NAM/CONUS_40km/conduit,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_40km/conduit/\n" +
          "fmrc/NCEP/NAM/CONUS_20km/surface,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_20km/surface/\n" +
          "fmrc/NCEP/NAM/CONUS_20km/selectsurface,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_20km/selectsurface/\n" +
          "fmrc/NCEP/NAM/CONUS_20km/noaaport,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_20km/noaaport/\n" +
          "fmrc/NCEP/NAM/CONUS_12km/conduit,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_12km_conduit/\n" +
          "fmrc/NCEP/NAM/CONUS_12km,/data/ldm/pub/native/grid/NCEP/NAM/CONUS_12km/\n" +
          "fmrc/NCEP/NAM/Alaska_95km,/data/ldm/pub/native/grid/NCEP/NAM/Alaska_95km/\n" +
          "fmrc/NCEP/NAM/Alaska_45km/noaaport,/data/ldm/pub/native/grid/NCEP/NAM/Alaska_45km/noaaport/\n" +
          "fmrc/NCEP/NAM/Alaska_45km/conduit,/data/ldm/pub/native/grid/NCEP/NAM/Alaska_45km/conduit/\n" +
          "fmrc/NCEP/NAM/Alaska_22km,/data/ldm/pub/native/grid/NCEP/NAM/Alaska_22km/\n" +
          "fmrc/NCEP/NAM/Alaska_11km,/data/ldm/pub/native/grid/NCEP/NAM/Alaska_11km/\n" +
          "fmrc/NCEP/GFS/Puerto_Rico_191km,/data/ldm/pub/native/grid/NCEP/GFS/Puerto_Rico_191km/\n" +
          "fmrc/NCEP/GFS/N_Hemisphere_381km,/data/ldm/pub/native/grid/NCEP/GFS/N_Hemisphere_381km/\n" +
          "fmrc/NCEP/GFS/Hawaii_160km,/data/ldm/pub/native/grid/NCEP/GFS/Hawaii_160km/\n" +
          "fmrc/NCEP/GFS/Global_onedeg,/data/ldm/pub/native/grid/NCEP/GFS/Global_onedeg/\n" +
          "fmrc/NCEP/GFS/Global_2p5deg,/data/ldm/pub/native/grid/NCEP/GFS/Global_2p5deg/\n" +
          "fmrc/NCEP/GFS/Global_1p0deg_Ensemble,/data/ldm/pub/native/grid/NCEP/GFS/Global_1p0deg_Ensemble/\n" +
          "fmrc/NCEP/GFS/Global_0p5deg,/data/ldm/pub/native/grid/NCEP/GFS/Global_0p5deg/\n" +
          "fmrc/NCEP/GFS/CONUS_95km,/data/ldm/pub/native/grid/NCEP/GFS/CONUS_95km/\n" +
          "fmrc/NCEP/GFS/CONUS_80km,/data/ldm/pub/native/grid/NCEP/GFS/CONUS_80km/\n" +
          "fmrc/NCEP/GFS/CONUS_191km,/data/ldm/pub/native/grid/NCEP/GFS/CONUS_191km/\n" +
          "fmrc/NCEP/GFS/Alaska_191km,/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/\n" +
          "fmrc/NCEP/DGEX/CONUS_12km,/data/ldm/pub/native/grid/NCEP/DGEX/CONUS_12km/\n" +
          "fmrc/NCEP/DGEX/Alaska_12km,/data/ldm/pub/native/grid/NCEP/DGEX/Alaska_12km/\n" +
          "casestudies/vgee_demo,/data/ldm/pub/casestudies/vgee_demo\n" +
          "casestudies/july18_2002/grids,/data/ldm/pub/casestudies/july18_2002/grids\n" +
          "casestudies/idvtest/grids,/data/ldm/pub/casestudies/idvtest/grids\n" +
          "casestudies/ccs039/grids,/data/ldm/pub/casestudies/ccs039/grids\n" +
          "casestudies/ccs034/netcdf,/data/ldm/pub/casestudies/ccs034/netcdf\n" +
          "casestudies/ccs034/grib,/data/ldm/pub/casestudies/ccs034/grib\n" +
          "casestudies/ccs034/acars,/data/ldm/pub/casestudies/ccs034/acars\n" +
          "GEMPAK/model,/data/ldm/pub/decoded/gempak/model";

  class MyFilter implements TdsLogParser.LogFilter {

    public boolean pass(TdsLogParser.Log log) {
      return log.path.startsWith(prefix);
    }
  }

  class MyFF implements FileFilter {

    public boolean accept(File f) {
      String name =  f.getName();
      return name.startsWith("access") && name.endsWith(".log");
    }
  }

  class MyClosure implements TdsLogParser.Closure {

    public void process(TdsLogParser.Log log) {
      String path = log.path.substring(prefix.length());
      Object match = pathMatcher.match( path);
      if (match == null) {
        System.out.printf("No root for path %s %n", path);
        return;
      }

      String dirName = (String) match; // convert(path);
      MDirectory mdir = manager.get(dirName);
      if (mdir == null)
        System.out.printf("Dir %s from path %s doesnt exist%n", dirName, path);
      else
        System.out.printf("Dir %s from path %s ok%n", dirName, path);

    }

  }

  //////////////////////////////////////////////////////

  private static String ehLocation = "/data/thredds/ehcache/";
  private static String config =
            "<ehcache>\n" +
            "    <diskStore path='"+ehLocation+"'/>\n" +
            "    <defaultCache\n" +
            "              maxElementsInMemory='10000'\n" +
            "              eternal='false'\n" +
            "              timeToIdleSeconds='120'\n" +
            "              timeToLiveSeconds='120'\n" +
            "              overflowToDisk='true'\n" +
            "              maxElementsOnDisk='10000000'\n" +
            "              diskPersistent='false'\n" +
            "              diskExpiryThreadIntervalSeconds='120'\n" +
            "              memoryStoreEvictionPolicy='LRU'\n" +
            "              />\n" +
            "    <cache name='directory'\n" +
            "            maxElementsInMemory='1000'\n" +
            "            eternal='false'\n" +
            "            timeToIdleSeconds='864000'\n" +
            "            timeToLiveSeconds='0'\n" +
            "            overflowToDisk='true'\n" +
            "            maxElementsOnDisk='0'\n" +
            "            diskPersistent='true'\n" +
            "            diskExpiryThreadIntervalSeconds='3600'\n" +
            "            memoryStoreEvictionPolicy='LRU'\n" +
            "            />\n" +
            "</ehcache>";

  Manager manager;
  PathMatcher pathMatcher;
  TestFileSystem() {
    manager = new Manager(new StringBufferInputStream(config));
    System.out.printf(" Ehcache at %s%n", ehLocation);

    pathMatcher = new PathMatcher();
    readRoots(roots);
    System.out.printf(" Roots initialized%n");
  }

  void readRoots(String rootString) {
    rootString = StringUtil.replace(rootString, "\n", ",");
    String[] roots = rootString.split(",");
    for (int i=0; i<roots.length; i+=2) {
      System.out.printf("  %-40s %-40s%n", roots[i], roots[i+1]);
      pathMatcher.put(roots[i], roots[i+1]);
    }
  }
  
  void process(String logDir) throws IOException {
    TdsLogParser reader = new TdsLogParser();

    long startElapsed = System.nanoTime();
    TdsLogParser.Stats stats = new TdsLogParser.Stats();

    reader.readAll(new File(logDir), new MyFF(), new MyClosure(), new MyFilter(), stats);

    long elapsedTime = System.nanoTime() - startElapsed;
    System.out.printf(" total= %d passed=%d%n", stats.total, stats.passed);
    System.out.printf(" elapsed=%d secs%n", elapsedTime / (1000 * 1000 * 1000));
  }

  void close() {
    manager.close();
  }

  public static void main(String args[]) throws IOException {
    System.out.printf("TestFileSystem%n");
    TestFileSystem reader = new TestFileSystem();
    
    String path = "D:/motherlode/logs/one/";
    //String path = args[1];
    System.out.printf(" Reading logs from %s%n", path);
    reader.process(path);
    reader.close();
  }
}
