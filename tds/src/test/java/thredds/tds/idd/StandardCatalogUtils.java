package thredds.tds.idd;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StandardCatalogUtils
{
  public static Object[][] getIddMainCatalogUrls()
  {
    return iddMainCatalogUrls;
  }

  public static Object[][] getIddDeepCatalogUrls()
  {
    return iddDeepCatalogUrls;
  }

  public static Object[][] getMlodeMainCatalogUrls()
  {
    return mlodeMainCatalogUrls;
  }

  private static final String[][] iddMainCatalogUrls =
          {
                  {"catalog.xml"},
                  {"idd/models.xml"},
                  {"idd/obsData.xml"},
                  {"idd/radars.xml"},
                  {"idd/satellite.xml"}
          };
  private static final String[][] iddDeepCatalogUrls =
          {
                  {"catalog/station/metar/catalog.xml"},
                  {"catalog/nexrad/composite/nws/catalog.xml"},
                  {"catalog/nexrad/composite/gini/catalog.xml"},
                  {"catalog/nexrad/composite/1km/files/catalog.xml"},
                  {"catalog/nexrad/level2/KFTG/catalog.xml"},
                  {"catalog/nexrad/level3/N0R/VNX/catalog.xml"},
                  {"catalog/station/profiler/wind/1hr/catalog.xml"},
                  {"catalog/station/profiler/RASS/1hr/catalog.xml"},
                  {"catalog/station/soundings/catalog.xml"},
                  {"catalog/satellite/WV/AK-REGIONAL_16km/catalog.xml"}
          };

  private static final String[][] mlodeMainCatalogUrls =
          {
                  {"topcatalog.xml"},
                  {"idd/rtmodel.xml"},
                  {"idv/models.xml"},
                  {"idv/latestModels.xml"},
                  {"idv/rt-models.xml"},            // check that not expired!
                  {"idv/rt-models.1.0.xml"},        // check that not expired!
                  {"cataloggen/catalogs/uniModelsInvCat1.0en.xml"},  // check that not expired!
                  {"galeon/catalog.xml"},
                  {"casestudies/catalog.xml"},
                  {"casestudies/vgeeCatalog.xml"},
                  {"casestudies/ccs034Catalog.xml"},
                  {"casestudies/ccs039Catalog.xml"},
                  {"casestudies/july18_2002cat.xml"},
                  {"servers/radarCollections.xml"}
          };

  /*

  */
}
