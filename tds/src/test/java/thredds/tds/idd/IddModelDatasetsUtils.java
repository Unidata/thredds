package thredds.tds.idd;

import java.util.Collection;
import java.util.Arrays;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class IddModelDatasetsUtils
{
  private static final String[][] modelIds =
          {
                  { "NCEP/NDFD/CONUS_5km"},                       // idd and/or idv ?
                  { "NCEP/NDFD/conduit/CONUS_5km"},                       // idd and/or idv ?

                  { "NCEP/DGEX/Alaska_12km"},                     // idd and/or idv ?
                  { "NCEP/DGEX/CONUS_12km"},                      // idd and/or idv ?

                  { "NCEP/GFS/Alaska_191km"},                     // idd and/or idv ?
                  { "NCEP/GFS/CONUS_80km"},                       // idd and/or idv ?
                  { "NCEP/GFS/CONUS_95km"},                       // idd and/or idv ?
                  { "NCEP/GFS/CONUS_191km"},                      // idd and/or idv ?
                  { "NCEP/GFS/Global_0p5deg"},                    // idd and/or idv ?
                  { "NCEP/GFS/Global_onedeg"},                    // idd and/or idv ?
                  { "NCEP/GFS/Global_2p5deg"},                    // idd and/or idv ?
                  { "NCEP/GFS/Hawaii_160km"},                     // idd and/or idv ?
                  { "NCEP/GFS/N_Hemisphere_381km"},               // idd and/or idv ?
                  { "NCEP/GFS/Puerto_Rico_191km"},                // idd and/or idv ?

                  { "NCEP/NAM/Alaska_11km"},                      // idd and/or idv ?
                  { "NCEP/NAM/Alaska_22km"},                      // idd and/or idv ?
                  { "NCEP/NAM/Alaska_45km/conduit"},              // idd and/or idv ?
                  { "NCEP/NAM/Alaska_45km/noaaport"},             // idd and/or idv ?
                  { "NCEP/NAM/Alaska_95km"},                      // idd and/or idv ?
                  { "NCEP/NAM/CONUS_12km"},                       // idd and/or idv ?
                  { "NCEP/NAM/CONUS_12km/conduit"},               // idd and/or idv ?
                  { "NCEP/NAM/CONUS_20km/noaaport"},              // idd and/or idv ?
                  { "NCEP/NAM/CONUS_20km/surface"},               // idd and/or idv ?
                  { "NCEP/NAM/CONUS_20km/selectsurface"},         // idd and/or idv ?
                  { "NCEP/NAM/CONUS_40km/conduit"},               // idd and/or idv ?
                  { "NCEP/NAM/CONUS_80km"},                       // idd and/or idv ?
                  { "NCEP/NAM/Polar_90km"},                       // idd and/or idv ?

                  { "NCEP/RUC2/CONUS_20km/hybrid"},               // idd and/or idv ?
                  { "NCEP/RUC2/CONUS_20km/pressure"},             // idd and/or idv ?
                  { "NCEP/RUC2/CONUS_20km/surface"},              // idd and/or idv ?
                  { "NCEP/RUC2/CONUS_40km"},                      // idd and/or idv ?

                  { "NCEP/RUC/CONUS_80km"},                       // idd and/or idv ?

                  { "NCEP/GEFS/Global_1p0deg_Ensemble/derived" }, // idd and/or idv ?
                  // datasetScan not datasetFmrc { "NCEP/GEFS/Global_1p0deg_Ensemble/member" }, // idd and/or idv ?

                  { "NCEP/SREF/Alaska_45km/ensprod"},                // idd and/or idv ?
                  { "NCEP/SREF/CONUS_40km/ensprod"},               // idd and/or idv ?
                  { "NCEP/SREF/CONUS_40km/ensprod_biasc"},         // idd and/or idv ?
                  // datasetScan not datasetFmrc { "NCEP/SREF/CONUS_40km/pgrb_biasc"}, // idd and/or idv ?
                  { "NCEP/SREF/PacificNE_0p4/ensprod"},            // idd and/or idv ?
                  
                  { "NCEP/WW3/Coastal_Alaska"},
                  { "NCEP/WW3/Coastal_US_East_Coast"},
                  { "NCEP/WW3/Coastal_US_West_Coast"},
                  { "NCEP/WW3/Global"},
                  { "NCEP/WW3/Regional_Alaska"},
                  {"NCEP/WW3/Regional_Eastern_Pacific"},
                  {"NCEP/WW3/Regional_US_East_Coast"},
                  {"NCEP/WW3/Regional_US_West_Coast"}

          };

  public static Object[][] getModelIds()
  {
    return modelIds;
  }

  private static final String[][] gfsIds =
          {
                  { "NCEP/GFS/Alaska_191km"},                     // idd and/or idv ?
                  { "NCEP/GFS/CONUS_80km"},                       // idd and/or idv ?
                  { "NCEP/GFS/CONUS_95km"},                       // idd and/or idv ?
                  { "NCEP/GFS/CONUS_191km"},                      // idd and/or idv ?
                  { "NCEP/GFS/Global_0p5deg"},                    // idd and/or idv ?
                  { "NCEP/GFS/Global_onedeg"},                    // idd and/or idv ?
                  { "NCEP/GFS/Global_2p5deg"},                    // idd and/or idv ?
                  { "NCEP/GFS/Hawaii_160km"},                     // idd and/or idv ?
                  { "NCEP/GFS/N_Hemisphere_381km"},               // idd and/or idv ?
                  { "NCEP/GFS/Puerto_Rico_191km"},                // idd and/or idv ?

          };

  public static Object[][] getGfsModelIds()    {
    return gfsIds;
  }


}
