package thredds.tds.idd;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StandardCatalogUtils
{
    private StandardCatalogUtils() {}

    public static Collection<Object[]> getIddMainCatalogUrlArrayCollection() {
        List<Object[]> result = new ArrayList<Object[]>();
        for ( String curCatalogUrl : iddMainCatalogUrls)
            result.add( new String[] { curCatalogUrl } );

        return result;
    }

    public static List<String> getIddMainCatalogUrlList() {
        return Arrays.asList( iddMainCatalogUrls );
    }

    private static final String[] iddMainCatalogUrls = {
            "catalog.xml",
            "idd/models.xml",
            "idd/obsData.xml",
            "idd/radars.xml",
            "idd/satellite.xml"
    };

    public static Collection<Object[]> getIddDeepCatalogUrlArrayCollection()
    {
        List<Object[]> result = new ArrayList<Object[]>();
        for ( String curCatalogUrl : iddDeepCatalogUrls )
            result.add( new String[]{curCatalogUrl} );

        return result;
    }

    public static List<String> getIddDeepCatalogUrlList() {
        return Arrays.asList( iddDeepCatalogUrls);
    }

    private static final String[] iddDeepCatalogUrls = {
            "catalog/station/metar/catalog.xml",
            "catalog/nexrad/composite/nws/catalog.xml",
            "catalog/nexrad/composite/gini/catalog.xml",
            "catalog/nexrad/composite/1km/files/catalog.xml",
            "catalog/nexrad/level2/KFTG/catalog.xml",
            "catalog/nexrad/level3/N0R/VNX/catalog.xml",
            "catalog/station/profiler/wind/1hr/catalog.xml",
            "catalog/station/profiler/RASS/1hr/catalog.xml",
            "catalog/station/soundings/catalog.xml",
            "catalog/satellite/WV/AK-REGIONAL_16km/catalog.xml"
    };

    public static Collection<Object[]> getNsfMainCatalogUrlArrayCollection()
    {
        List<Object[]> result = new ArrayList<Object[]>();
        for ( String curCatalogUrl : nsfMainCatalogUrls )
            result.add( new String[]{curCatalogUrl} );

        return result;
    }

    public static List<String> getNsfMainCatalogUrlList() {
        return Arrays.asList( nsfMainCatalogUrls);
    }

    private static final String[] nsfMainCatalogUrls = {
            "idv/models.xml",
            "servers/radarCollections.xml"
    };

    public static Collection<Object[]> getMlodeMainCatalogUrlArrayCollection()
    {
        List<Object[]> result = new ArrayList<Object[]>();
        for ( String curCatalogUrl : mlodeMainCatalogUrls )
            result.add( new String[]{curCatalogUrl} );

        return result;
    }

    public static List<String> getMlodeMainCatalogUrlList() {
        return Arrays.asList( mlodeMainCatalogUrls);
    }

    private static final String[] mlodeMainCatalogUrls = {
            "topcatalog.xml",
            "idd/rtmodel.xml",
            "idv/models.xml",
            "idv/latestModels.xml",
            "idv/rt-models.xml",            // check that not expired!
            "idv/rt-models.1.0.xml",        // check that not expired!
            "gempakTestCat.xml",
            "cataloggen/catalogs/uniModelsInvCat1.0en.xml",  // check that not expired!
            "galeon/catalog.xml",
            "casestudies/catalog.xml",
            "casestudies/vgeeCatalog.xml",
            "casestudies/ccs034Catalog.xml",
            "casestudies/ccs039Catalog.xml",
            "casestudies/july18_2002cat.xml",
            "servers/radarCollections.xml"
    };

  public static Collection<Object[]> getMlodeDeepCatalogUrlArrayCollection()
  {
    List<Object[]> result = new ArrayList<Object[]>();
    for ( String curCatalogUrl : mlodeDeepCatalogUrls )
      result.add( new String[]{curCatalogUrl} );

    return result;
  }

  public static List<String> getMlodeDeepCatalogUrlList()
  {
    return Arrays.asList( mlodeDeepCatalogUrls );
  }

  private static final String[] mlodeDeepCatalogUrls = {
          "catalog/GEMPAK/model/gfs/catalog.xml",
          "catalog/GEMPAK/model/nam-ak/catalog.xml",
          "catalog/GEMPAK/model/ukmet/catalog.xml"
  };


}
