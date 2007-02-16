package thredds.tds.ethan;

import junit.framework.*;

import thredds.catalog.*;
import thredds.datatype.DateType;

import java.util.*;
import java.io.IOException;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.dt.TypedDataset;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 30, 2006 11:13:36 AM
 */
public class TestMotherlodeTDS extends TestCase
{

  private String host = "motherlode.ucar.edu:8080";
  private String targetTomcatUrl;
  private String targetTdsUrl;

  private String tdsConfigUser;
  private String tdsConfigWord;

  private String tdsTestLevel = "BASIC";

  public TestMotherlodeTDS( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    Properties env = System.getProperties();
    host = env.getProperty( "thredds.tds.site", host );
    tdsConfigUser = env.getProperty( "thredds.tds.config.user" );
    tdsConfigWord = env.getProperty( "thredds.tds.config.password" );
    tdsTestLevel = env.getProperty( "thredds.tds.test.level" );

    targetTomcatUrl = "http://" + host + "/";
    targetTdsUrl = "http://" + host + "/thredds/";
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
    TestAll.openAndValidateCatalog( targetTdsUrl + "/casestudies/vgeeCatalog.xml" );
  }

  public void testAllNcModelsCatalog()
  {
    TestAll.openAndValidateCatalog( targetTdsUrl + "/idd/allModels.TDS-nc.xml" );
  }

  public void testDqcServletCatalog()
  {
    TestAll.openAndValidateCatalog( targetTomcatUrl + "dqcServlet/latestModel.xml" );
  }

}
