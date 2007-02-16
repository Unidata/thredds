package thredds.tds.ethan;

import junit.framework.TestCase;

import java.util.Properties;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 30, 2006 11:13:36 AM
 */
public class TestTdsPingMotherlode extends TestCase
{

  private String host = "motherlode.ucar.edu:8080";
  private String targetTomcatUrl;
  private String targetTdsUrl;

  private String tdsConfigUser;
  private String tdsConfigWord;

  public TestTdsPingMotherlode( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    Properties env = System.getProperties();
    host = env.getProperty( "thredds.tds.site", host );
    tdsConfigUser = env.getProperty( "thredds.tds.config.user" );
    tdsConfigWord = env.getProperty( "thredds.tds.config.password" );

    targetTomcatUrl = "http://" + host;
    targetTdsUrl = "http://" + host + "/thredds";
  }

  public void testMainCatalog()
  {
    TestAll.openAndValidateCatalog( targetTdsUrl + "/catalog.xml" );
  }

  public void testTopCatalog()
  {
    TestAll.openAndValidateCatalog( targetTdsUrl + "/topcatalog.xml" );
  }

  public void testIdvModelsCatalog()
  {
    TestAll.openAndValidateCatalog( targetTdsUrl + "/idv/models.xml" );
  }

  public void testIdvLatestModelsCatalog()
  {
    TestAll.openValidateAndCheckLatestCatalog( targetTdsUrl + "/idv/latestModels.xml" );
  }

  public void testIdvRtModels10Catalog()
  {
    TestAll.openValidateAndCheckExpires( targetTdsUrl + "/idv/rt-models.1.0.xml" );
  }

  public void testIdvRtModels06Catalog()
  {
    TestAll.openValidateAndCheckExpires( targetTdsUrl + "/idv/rt-models.xml" );
  }

//  public void testCatGenIdvRtModels10Catalog()
//  {
//    openValidateAndCheckExpires( targetTdsUrl + "/cataloggen/catalogs/idv-rt-models.InvCat1.0.xml" );
//  }

//  public void testCatGenIdvRtModels06Catalog()
//  {
//    openValidateAndCheckExpires( targetTdsUrl + "/cataloggen/catalogs/idv-rt-models.xml" );
//  }

  public void testCatGenCdpCatalog()
  {
    TestAll.openAndValidateCatalog( targetTdsUrl + "/cataloggen/catalogs/uniModelsInvCat1.0en.xml" );
  }

  public void testVgeeCatalog()
  {
    TestAll.openAndValidateCatalog( targetTdsUrl + "/casestudy/vgeeCatalog.1.0.xml" );
  }

  public void testAllNcModelsCatalog()
  {
    TestAll.openAndValidateCatalog( targetTdsUrl + "/idd/allModels.TDS-nc.xml" );
  }

  public void testDqcServletCatalog()
  {
    TestAll.openAndValidateCatalog( targetTomcatUrl + "/dqcServlet/latestModel.xml" );
  }

}
