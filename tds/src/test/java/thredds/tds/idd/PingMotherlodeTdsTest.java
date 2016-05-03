package thredds.tds.idd;

import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.client.catalog.Catalog;
import thredds.tds.ethan.TestAll;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

import static org.junit.Assert.assertNotNull;

/**
 * Ping a TDS assuming an IDD setup.
 *
 * @author edavis
 * @since 4.0
 */
@RunWith(Parameterized.class)
@Category(NeedsExternalResource.class)
public class PingMotherlodeTdsTest
{
  private String tdsUrl = "http://"+ TestDir.threddsServer+"/thredds/";

  private String catUrl;

  public PingMotherlodeTdsTest( String catUrl )
  {
    super();
    this.catUrl = catUrl;
  }

  @Parameterized.Parameters(name="{0}")
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
