/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.logs;

import ucar.unidata.util.StringUtil2;

/**
 * Class Description.
 *
 * @author caron
 * @since Mar 23, 2009
 */
public class LogCategorizer {
  static boolean showRoots = false;

  /* default roots
  static private String roots =
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

  public static String[] getRoots() {
    String rootString = StringUtil2.replace(roots, "\n", ",");
    String[] root2 = rootString.split(",");
    String[] result = new String[root2.length/2];
    for (int i = 0; i < root2.length; i += 2) {
      result[i/2] = root2[i];
    }
    return result;
  } */

  private static String roots;

  public static void setRoots(String raw) {
    if (null != raw)
      roots = raw;
  }

  public static PathMatcher readRoots() {
    PathMatcher pathMatcher = new PathMatcher();
    String rootString = StringUtil2.replace(roots, "\n", ",");
    String[] roots = rootString.split(",");
    for (int i = 0; i < roots.length; i += 2) {
      if (showRoots) System.out.printf("  %-40s %-40s%n", roots[i], roots[i + 1]);
      pathMatcher.put(roots[i]);
    }

    return pathMatcher;
  }

  private static PathMatcher pathMatcher = null;

  public static String getDataroot(String path, int status) {
    if (pathMatcher == null)
      pathMatcher = readRoots();

    String ss =  getServiceSpecial( path);
    if (ss != null) return  "service-"+ss;

    if (path.startsWith("//thredds/"))
      path = path.substring(1);

    if (!path.startsWith("/thredds/"))
      return "zervice-root";

    String dataRoot = null;
    String spath = path.substring(9); // remove /thredds/
    String service = findService(spath);
    if (service != null) {
      //if (service.equals("radarServer")) dataRoot = "radarServer";
        //else if (service.equals("casestudies")) dataRoot = "casestudies";
     // else if (service.equals("dqc")) dataRoot = "dqc";
      if (spath.length() > service.length()) {
        spath = spath.substring(service.length() + 1);
        PathMatcher.Match match = pathMatcher.match(spath);
        if (match != null) dataRoot = match.root;
      }
    }

    if (dataRoot == null) {
      if (status >= 400)
        dataRoot = "zBad";
    }

    if (dataRoot == null) {
      service = getService(path);
      dataRoot = (service != null) ? "zervice-"+service : "unknown";
    }
    return dataRoot;
  }

  // the ones that dont start with thredds
  public static String getServiceSpecial(String path) {
    String ss = null;
    if (path.startsWith("/dqcServlet"))
      ss = "dqcServlet";
    else if (path.startsWith("/cdmvalidator"))
      ss = "cdmvalidator";
    return ss;
  }

  public static String getService(String path) {
    String service =  getServiceSpecial( path);
    if (service != null) return service;

    if (path.startsWith("/dts"))
      return "dts";

    if (path.startsWith("/dqcServlet"))
      return "dqcServlet";

    if (path.startsWith("/thredds/")) {
      String spath = path.substring(9);
      service = findService(spath);
      if ((service == null) && (spath.startsWith("ncml") || spath.startsWith("uddc") || spath.startsWith("iso")))
        service = "ncIso";
      if ((service == null) && (spath.endsWith("xml") || spath.endsWith("html")))
        service = "catalog";

      if (service == null) {
        int pos = spath.indexOf('?');
        if (pos > 0) {
          String req = spath.substring(0, pos);
          if (req.endsWith("xml") || req.endsWith("html"))
            service = "catalog";
        }
      }
    }

    if (service == null)
      service = "other";
    return service;
  }

  public static String[] services = new String[] {
          "admin", "cataloggen", "catalog", "cdmremote", "cdmrfeature", "dodsC", "dqc", "fileServer", "godiva2",
          "ncss", "ncstream", "radarServer", "remoteCatalogService", "view", "wcs", "wms"
  };

  public static String findService(String path) {
    for (String service : services) {
      if (path.startsWith(service)) return service;
    }
    return null;
  }

}
