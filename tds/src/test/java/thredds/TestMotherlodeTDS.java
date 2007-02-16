package thredds;

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
    openAndValidateCatalog( targetTdsUrl + "/catalog.xml" );
  }

  public void testTopCatalog()
  {
    openAndValidateCatalog( targetTdsUrl + "/topcatalog.xml" );
  }

  public void testIdvModelsCatalog()
  {
    openAndValidateCatalog( targetTdsUrl + "/idv/models.xml" );
  }

  public void testIdvLatestModelsCatalog()
  {
    openValidateAndCheckLatestCatalog( targetTdsUrl + "/idv/latestModels.xml" );
  }

  public void testIdvRtModels10Catalog()
  {
    openValidateAndCheckExpires( targetTdsUrl + "/idv/rt-models.1.0.xml" );
  }

  public void testIdvRtModels06Catalog()
  {
    openValidateAndCheckExpires( targetTdsUrl + "/idv/rt-models.xml" );
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
    openAndValidateCatalog( targetTdsUrl + "/cataloggen/catalogs/uniModelsInvCat1.0en.xml" );
  }

  public void testVgeeCatalog()
  {
    openAndValidateCatalog( targetTdsUrl + "/casestudies/vgeeCatalog.xml" );
  }

  public void testAllNcModelsCatalog()
  {
    openAndValidateCatalog( targetTdsUrl + "/idd/allModels.TDS-nc.xml" );
  }

  public void testDqcServletCatalog()
  {
    openAndValidateCatalog( targetTomcatUrl + "dqcServlet/latestModel.xml" );
  }

  private InvCatalogImpl openAndValidateCatalog( String catUrl )
  {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false );
    StringBuffer validationMsg = new StringBuffer();
    try
    {
      InvCatalogImpl cat = catFactory.readXML( catUrl );
      boolean isValid = cat.check( validationMsg, false );
      if ( ! isValid )
      {
        assertTrue( "Invalid catalog <" + catUrl + ">:\n" + validationMsg.toString(),
                    false );
        return null;
      }
      else
      {
        String tmpMsg = "Valid catalog <" + catUrl + ">." + (validationMsg.length() > 0 ? "" : " Validation messages:\n" + validationMsg.toString() );
        System.out.println( tmpMsg );
        return cat;
      }
    }
    catch ( Exception e )
    {
      e.printStackTrace();
      assertTrue( "Exception while parsing catalog <" + catUrl + ">: " + e.getMessage(),
                  false );
      return null;
    }
  }

  private void openValidateAndCheckExpires( String catalogUrl )
  {
    InvCatalogImpl catalog = openAndValidateCatalog( catalogUrl );
    if ( catalog != null )
    {
      // Check if the catalog has expired.
      DateType expiresDateType = catalog.getExpires();
      if ( expiresDateType != null )
      {
        if ( expiresDateType.getDate().getTime() < System.currentTimeMillis() )
        {

          assertTrue( "Expired catalog <" + catalogUrl + ">: " + expiresDateType.toDateTimeStringISO() + ".",
                      false );
          return;
        }
      }
    }
  }

  private void openValidateAndCheckLatestCatalog( String catalogUrl )
  {
    InvCatalogImpl catalog = openAndValidateCatalog( catalogUrl );
    if ( catalog != null )
    {
      List resDs = findAllResolverDatasets( catalog.getDatasets() );
      Map fail = new HashMap();

      for ( Iterator it = resDs.iterator(); it.hasNext(); )
      {
        InvDatasetImpl curDs = (InvDatasetImpl) it.next();
        InvAccess curAccess = curDs.getAccess( ServiceType.RESOLVER );

        String dsPath = curAccess.getStandardUri().toString();
        NetcdfDataset ncd;
        try
        {
          ncd = NetcdfDataset.openDataset( dsPath );
        }
        catch ( IOException e )
        {
          fail.put( dsPath, "I/O error opening dataset <" + dsPath + ">: " + e.getMessage() );
          continue;
        }

        if ( ncd == null )
        {
          fail.put( dsPath, "Failed to open dataset <" + dsPath + ">." );
          continue;
        }

        StringBuffer buf = new StringBuffer();
        TypedDataset typedDs;
        try
        {
          typedDs = TypedDatasetFactory.open( null, ncd, null, buf);
        }
        catch ( IOException e )
        {
          fail.put( dsPath, "I/O error opening typed dataset <" + dsPath + ">: " + e.getMessage() );
          continue;
        }
        if ( typedDs == null )
        {
          fail.put( dsPath, "Failed to open typed dataset <" + dsPath + ">." );
          //continue;
        }

        //Date startDate = typedDs.getStartDate();
        //if ( startDate.getTime() < System.currentTimeMillis()) ...
      }

      if ( ! fail.isEmpty())
      {
        StringBuffer failMsg = new StringBuffer( "Some resolver datasets failed to open:");
        for ( Iterator it = fail.keySet().iterator(); it.hasNext(); )
        {
          String curPath = (String) it.next();
          String curMsg = (String) fail.get( curPath );
          failMsg.append( "\n").append( curPath).append( ": ").append( curMsg);
        }
        assertTrue( failMsg.toString(),
                    false);
      }
    }
  }

  private List findAllResolverDatasets( List datasets)
  {
    List resolverDsList = new ArrayList();
    for ( Iterator iterator = datasets.iterator(); iterator.hasNext(); )
    {
      InvDatasetImpl curDs = (InvDatasetImpl) iterator.next();

      if ( ! (curDs instanceof InvDatasetImpl) ) continue;

      if ( curDs.hasNestedDatasets() )
      {
        resolverDsList.addAll( findAllResolverDatasets( curDs.getDatasets() ) );
      }
      else if ( curDs.hasAccess() && curDs.getAccess( ServiceType.RESOLVER ) != null )
      {
        resolverDsList.add( curDs );
      }
    }
    return resolverDsList;
  }
}
