package thredds.tds.idd;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.client.catalog.Catalog;
import thredds.tds.ethan.TestAll;
import ucar.unidata.test.util.NotTravis;

import java.util.Collection;

import static org.junit.Assert.assertNotNull;

/**
 * Ping a TDS assuming an IDD setup.
 *
 * @author edavis
 * @since 4.0
 */
@Category(NotTravis.class)
@RunWith(Parameterized.class)
public class PingMotherlodeTdsTest
{
  private String tdsUrl = "http://thredds.ucar.edu/thredds/";

  private String catUrl;

  public PingMotherlodeTdsTest( String catUrl )
  {
    super();
    this.catUrl = catUrl;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> getCatalogUrls()
  {
    Collection<Object[]> catUrls = StandardCatalogUtils.getIddMainCatalogUrlArrayCollection();
    catUrls.addAll( StandardCatalogUtils.getMlodeMainCatalogUrlArrayCollection() );
    return catUrls;
  }

  @Test
  public void pingMotherlodeCatalogs()
  {
    StringBuilder msgLog = new StringBuilder();

    String url = this.tdsUrl + this.catUrl;
    Catalog cat = TestAll.openValidateAndCheckExpires( url, msgLog );

    assertNotNull( "Catalog [" + url + "] failed to open, failed to validate, or was expired: " + msgLog, cat);
  }
}
