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
  private String host = "motherlode.ucar.edu:8080";
  private String targetTomcatUrl;
  private String targetTdsUrl;

  private String tdsConfigUser;
  private String tdsConfigWord;

  private String tdsTestLevel = "BASIC";

  public TestAll( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();

    Properties env = System.getProperties();
    String tdsTestLevel = env.getProperty( "thredds.tds.test.level" );

    if ( tdsTestLevel.equalsIgnoreCase( "PING") )
    suite.addTestSuite( thredds.tds.ethan.TestMotherlodeTDS.class );
    else if ( tdsTestLevel.equalsIgnoreCase( "CRAWL") )
    {
      suite.addTestSuite( thredds.tds.ethan.TestTdsCrawl.class);
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
          typedDs = TypedDatasetFactory.open( null, ncd, null, buf );
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
