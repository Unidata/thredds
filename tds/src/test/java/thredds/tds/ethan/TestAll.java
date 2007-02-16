package thredds.tds.ethan;

import junit.framework.*;

import java.util.*;
import java.io.IOException;

import thredds.catalog.*;
import thredds.datatype.DateType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.TypedDatasetFactory;

/**
 * _more_
 *
 * @author edavis
 * @since Feb 15, 2007 10:10:08 PM
 */
public class TestAll extends TestCase
{
  public TestAll( String name )
  {
    super( name );
  }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();

    Properties env = System.getProperties();
    String tdsTestLevel = env.getProperty( "thredds.tds.test.level" );

    if ( tdsTestLevel.equalsIgnoreCase( "PING") )
      suite.addTestSuite( thredds.tds.ethan.TestTdsPing.class );
    else if ( tdsTestLevel.equalsIgnoreCase( "PING-mlode") )
      suite.addTestSuite( thredds.tds.ethan.TestTdsPingMotherlode.class);
    else if ( tdsTestLevel.equalsIgnoreCase( "CRAWL") )
      // ToDo Need to implement this one.
      suite.addTestSuite( thredds.tds.ethan.TestTdsCrawl.class);
//    else if ( tdsTestLevel.equalsIgnoreCase( "CRAWL-mlode" ) )
//      // ToDo Need to implement this one.
//      suite.addTestSuite( thredds.tds.ethan.TestTdsCrawlMotherlode.class );
    else
    {
      suite.addTestSuite( thredds.tds.ethan.TestTdsPingMotherlode.class );
      // suite.addTestSuite( thredds.tds.ethan.TestTdsCrawlMotherlode.class );
    }


    return suite;
  }

  public static InvCatalogImpl openAndValidateCatalog( String catUrl )
  {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false );
    StringBuffer validationMsg = new StringBuffer();
    try
    {
      InvCatalogImpl cat = catFactory.readXML( catUrl );
      boolean isValid = cat.check( validationMsg, false );
      if ( !isValid )
      {
        assertTrue( "Invalid catalog <" + catUrl + ">:\n" + validationMsg.toString(),
                    false );
        return null;
      }
      else
      {
        String tmpMsg = "Valid catalog <" + catUrl + ">." + ( validationMsg.length() > 0 ? "" : " Validation messages:\n" + validationMsg.toString() );
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

  public static void openValidateAndCheckExpires( String catalogUrl )
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

  public static void openValidateAndCheckLatestCatalog( String catalogUrl )
  {
    InvCatalogImpl catalog = openAndValidateCatalog( catalogUrl );
    if ( catalog != null )
    {
      List resolverDsList = findAllResolverDatasets( catalog.getDatasets() );
      Map fail = new HashMap();

      for ( Iterator it = resolverDsList.iterator(); it.hasNext(); )
      {
        // Resolve the resolver dataset.
        InvDatasetImpl curResolverDs = (InvDatasetImpl) it.next();
        InvAccess curAccess = curResolverDs.getAccess( ServiceType.RESOLVER );
        String curResDsPath = curAccess.getStandardUri().toString();
        InvCatalogImpl curResolvedCat = openAndValidateCatalog( curResDsPath );

        List curDatasets = curResolvedCat.getDatasets();
        if ( curDatasets.size() != 1 )
        {
          fail.put( curResDsPath, "Wrong number of datasets <" + curDatasets.size() + "> in resolved catalog <" + curResDsPath + ">." );
          continue;
        }

        // Open the actual (OPeNDAP) dataset.
        InvDatasetImpl curResolvedDs = (InvDatasetImpl) curDatasets.get( 0);
        InvAccess curResolvedDsAccess = curResolvedDs.getAccess( ServiceType.OPENDAP );
        String curResolvedDsPath = curResolvedDsAccess.getStandardUri().toString(); 

        NetcdfDataset ncd;
        try
        {
          ncd = NetcdfDataset.openDataset( curResolvedDsPath );
        }
        catch ( IOException e )
        {
          fail.put( curResolvedDsPath, "I/O error opening dataset <" + curResolvedDsPath + ">: " + e.getMessage() );
          continue;
        }

        if ( ncd == null )
        {
          fail.put( curResolvedDsPath, "Failed to open dataset <" + curResolvedDsPath + ">." );
          continue;
        }

        StringBuffer buf = new StringBuffer();
        TypedDataset typedDs;
        try
        {
          typedDs = TypedDatasetFactory.open( null, ncd, null, buf );
        }
        catch ( IOException e )
        {
          fail.put( curResolvedDsPath, "I/O error opening typed dataset <" + curResolvedDsPath + ">: " + e.getMessage() );
          continue;
        }
        if ( typedDs == null )
        {
          fail.put( curResolvedDsPath, "Failed to open typed dataset <" + curResolvedDsPath + ">." );
          //continue;
        }

        //Date startDate = typedDs.getStartDate();
        //if ( startDate.getTime() < System.currentTimeMillis()) ...
      }

      if ( !fail.isEmpty() )
      {
        StringBuffer failMsg = new StringBuffer( "Some resolver datasets failed to open:" );
        for ( Iterator it = fail.keySet().iterator(); it.hasNext(); )
        {
          String curPath = (String) it.next();
          String curMsg = (String) fail.get( curPath );
          failMsg.append( "\n" ).append( curPath ).append( ": " ).append( curMsg );
        }
        assertTrue( failMsg.toString(),
                    false );
      }
    }
  }

  public static List findAllResolverDatasets( List datasets )
  {
    List resolverDsList = new ArrayList();
    for ( Iterator iterator = datasets.iterator(); iterator.hasNext(); )
    {
      InvDatasetImpl curDs = (InvDatasetImpl) iterator.next();

      if ( !( curDs instanceof InvDatasetImpl ) ) continue;

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
