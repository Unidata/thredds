package thredds.tds.idd;

import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;
import org.junit.Test;

import java.util.Collection;

import thredds.tds.ethan.TestAll;
import thredds.catalog.InvCatalogImpl;

import static org.junit.Assert.*;

/**
 * Ping a TDS assuming an IDD setup.
 *
 * @author edavis
 * @since 4.0
 */
@RunWith(Parameterized.class)
public class PingMotherlodeTdsTest
{
  private String tdsUrl = "http://motherlode.ucar.edu/thredds/";

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
    InvCatalogImpl cat = TestAll.openValidateAndCheckExpires( url, msgLog );

    assertNotNull( "Catalog [" + url + "] failed to open, failed to validate, or was expired: " + msgLog,
                   cat);
  }
}
