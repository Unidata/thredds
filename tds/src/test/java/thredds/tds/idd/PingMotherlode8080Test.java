package thredds.tds.idd;

import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;
import org.junit.Test;

import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import thredds.tds.ethan.TestAll;
import thredds.catalog.InvCatalogImpl;

import static org.junit.Assert.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
@RunWith(Parameterized.class)
public class PingMotherlode8080Test
{
  private final String mlode8080TdsUrl = "http://motherlode.ucar.edu:8080/thredds/";

  private String catUrl;

  public PingMotherlode8080Test( String catUrl )
  {
    super();
    this.catUrl = catUrl;
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

    String url = this.mlode8080TdsUrl + this.catUrl;
    InvCatalogImpl cat = TestAll.openValidateAndCheckExpires( url, msgLog );

    assertNotNull( "Catalog [" + url + "] failed to open, failed to validate, or was expired: " + msgLog,
                   cat);
  }
}
