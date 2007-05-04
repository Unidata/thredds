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
public class TestTdsIddPing extends TestCase
{

  private String host = "motherlode.ucar.edu:8080";
  private String targetTomcatUrl;
  private String targetTdsUrl;

  private String tdsConfigUser;
  private String tdsConfigWord;

  public TestTdsIddPing( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    Properties env = System.getProperties();
    host = env.getProperty( "thredds.tds.site", host );

    targetTomcatUrl = "http://" + host + "/";
    targetTdsUrl = "http://" + host + "/thredds/";
  }

  public void testMainCatalog()
  {
    TestAll.openAndValidateCatalog( targetTdsUrl + "/catalog.xml" );
  }

  public void testModelsCatalog()
  {
    TestAll.openAndValidateCatalog( targetTdsUrl + "/idd/models.xml" );
  }

  public void testAllNcModelsCatalog()
  {
    TestAll.openAndValidateCatalog( targetTdsUrl + "/idd/allModels.TDS-nc.xml" );
  }

}
