package thredds.tds.idd;

import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.Before;

import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

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
  private String tdsUrl;

  private String catUrl;

  public PingMotherlodeTdsTest( String catUrl )
  {
    super();
    this.catUrl = catUrl;
  }

  @Before
  public void init()
  {
    this.tdsUrl = TdsTestUtils.getTargetTdsUrl();
  }


  @Parameterized.Parameters
  public static Collection<Object[]> getCatalogUrls()
  {
    List<Object[]> catUrls = new ArrayList<Object[]>(
            Arrays.asList( StandardCatalogUtils.getIddMainCatalogUrls()));
    catUrls.addAll( Arrays.asList( StandardCatalogUtils.getMlodeMainCatalogUrls() ));
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
