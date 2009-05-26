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
                  { "NCEP/GFS/Alaska_191km"},           // both
                  { "NCEP/GFS/CONUS_80km"},             // both
                  { "NCEP/GFS/CONUS_95km"},             // both
                  { "NCEP/GFS/CONUS_191km"},            // both
                  { "NCEP/GFS/Global_0p5deg"},          // both
                  { "NCEP/GFS/Global_onedeg"},          // both
                //  { "NCEP/GFS/Global_1p0deg_Ensemble"}, // Not in IDV models catalog
                  { "NCEP/GFS/Global_2p5deg"},          // both
                  { "NCEP/GFS/Hawaii_160km"},           // both
                  { "NCEP/GFS/N_Hemisphere_381km"},     // both
                  { "NCEP/GFS/Puerto_Rico_191km"},      // both

                  { "NCEP/NAM/Alaska_11km"},            // both
                  { "NCEP/NAM/Alaska_22km"},            // both
                  { "NCEP/NAM/Alaska_45km/noaaport"},   // both
                  { "NCEP/NAM/Alaska_45km/conduit"},    // both
                  { "NCEP/NAM/Alaska_95km"},            // both
                  { "NCEP/NAM/CONUS_12km"},             // both
                  //      { "NCEP/NAM/CONUS_12km/conduit"},      // not in idv
                  { "NCEP/NAM/CONUS_20km/surface"},      // both
                  { "NCEP/NAM/CONUS_20km/selectsurface"},// both
                  { "NCEP/NAM/CONUS_20km/noaaport"},     // both
                  { "NCEP/NAM/CONUS_40km/conduit"},      // both
                  { "NCEP/NAM/CONUS_80km"},              // both
                  { "NCEP/NAM/Polar_90km"},              // both

                  { "NCEP/RUC2/CONUS_20km/surface"},      // both
                  { "NCEP/RUC2/CONUS_20km/pressure"},     // both
                  { "NCEP/RUC2/CONUS_20km/hybrid"},       // both
                  //     { "NCEP/RUC2/CONUS_40km"},              // not in idv

                  { "NCEP/RUC/CONUS_80km"},               // both

                  { "NCEP/DGEX/CONUS_12km"},              // both
                  { "NCEP/DGEX/Alaska_12km"},             // both

                  { "NCEP/SREF/CONUS_40km/ensprod_biasc"}, // both
                  { "NCEP/SREF/CONUS_40km/pgrb_biasc"},    // both

                  { "NCEP/NDFD/CONUS_5km"},                // both
                  //     { "NCEP/NEWGBXNDFD/CONUS_5km"}
          };

  public static Object[][] getModelIds()
  {
    return modelIds;
  }
}
