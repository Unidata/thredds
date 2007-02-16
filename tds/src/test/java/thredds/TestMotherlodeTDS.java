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
  static private org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TestMotherlodeTDS.class );

  private String mlodeTds = "http://motherlode.ucar.edu:8080/thredds";

  public TestMotherlodeTDS( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testMainCatalog()
  {
    openAndValidateCatalog( mlodeTds + "/catalog.xml" );
  }

  public void testTopCatalog()
  {
    openAndValidateCatalog( mlodeTds + "/topcatalog.xml" );
  }

  public void testIdvModelsCatalog()
  {
    openAndValidateCatalog( mlodeTds + "/idv/models.xml" );
  }

  public void testIdvLatestModelsCatalog()
  {
    openValidateAndCheckLatestCatalog( mlodeTds + "/idv/latestModels.xml" );
  }

  public void testIdvRtModels10Catalog()
  {
    openAndValidateCatalog( mlodeTds + "/idv/rt-models.1.0.xml" );
  }

  public void testIdvRtModels06Catalog()
  {
    openAndValidateCatalog( mlodeTds + "/idv/rt-models.xml" );
  }

  public void testCatGenIdvRtModels10Catalog()
  {
    openValidateAndCheckExpires( mlodeTds + "/cataloggen/catalogs/idv-rt-models.InvCat1.0.xml" );
  }

  public void testCatGenIdvRtModels06Catalog()
  {
    openValidateAndCheckExpires( mlodeTds + "/cataloggen/catalogs/idv-rt-models.xml" );
  }

  public void testCatGenCdpCatalog()
  {
    openAndValidateCatalog( mlodeTds + "/cataloggen/catalogs/uniModelsInvCat1.0en.xml" );
  }

  public void testVgeeCatalog()
  {
    openAndValidateCatalog( mlodeTds + "/casestudy/vgeeCatalog.xml" );
  }

  public void testAllNcModelsCatalog()
  {
    openAndValidateCatalog( mlodeTds + "/idd/allModels.TDS-nc.xml" );
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
        log.info( "Valid catalog <" + catUrl + ">. Validation messages:\n" + validationMsg.toString() );
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
