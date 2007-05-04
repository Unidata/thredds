package thredds.tds.ethan;

import junit.framework.*;

import java.util.*;
import java.io.IOException;
import java.text.SimpleDateFormat;

import thredds.catalog.*;
import thredds.catalog.query.DqcFactory;
import thredds.catalog.query.QueryCapability;
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
      suite.addTestSuite( thredds.tds.ethan.TestTdsIddPing.class );
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

  public static QueryCapability openAndValidateDqcDoc( String dqcUrl )
  {
    DqcFactory fac = new DqcFactory( true );
    QueryCapability dqc = null;
    try
    {
      dqc = fac.readXML( dqcUrl );
    }
    catch ( IOException e )
    {
      fail( "I/O error reading DQC doc <" + dqcUrl + ">: " + e.getMessage() );
      return null;
    }

    if ( dqc.hasFatalError() )
    {
      fail( "Fatal error with DQC doc <" + dqcUrl + ">: " + dqc.getErrorMessages() );
      return null;
    }

    String errMsg = dqc.getErrorMessages();
    if ( errMsg != null && errMsg.length() > 0)
    {
      fail( "Error message reading DQC doc <" + dqcUrl + ">:\n" + errMsg );
      return null;
    }

    return dqc;
  }

  public static InvCatalogImpl openAndValidateCatalog( String catUrl )
  {
    StringBuffer log = new StringBuffer();
    InvCatalogImpl cat = openAndValidateCatalogForChaining( catUrl, log );
    if ( cat == null )
    {
      fail( log.toString() );
      return null;
    }

    return cat;
  }

  public static InvCatalogImpl openAndValidateCatalogForChaining( String catUrl, StringBuffer log )
  {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false );
    StringBuffer validationMsg = new StringBuffer();
    String curSysTimeAsString = null;
    try
    {
      curSysTimeAsString = getCurrentSystemTimeAsISO8601();

      InvCatalogImpl cat = catFactory.readXML( catUrl );
      boolean isValid = cat.check( validationMsg, false );
      if ( !isValid )
      {
        log.append( "Invalid catalog <" ).append( catUrl ).append( ">:\n" ).append( validationMsg.toString() );
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
      log.append( "[" ).append( curSysTimeAsString ).append( "] Exception while parsing catalog <" ).append( catUrl ).append( ">: " ).append( e.getMessage() );
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

          fail( "Expired catalog <" + catalogUrl + ">: " + expiresDateType.toDateTimeStringISO() + "." );
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
      List<InvDatasetImpl> resolverDsList = findAllResolverDatasets( catalog.getDatasets() );
      Map<String, String> failureMsgs = new HashMap<String, String>();

      for ( InvDatasetImpl curResolverDs : resolverDsList )
      {
        // Resolve the resolver dataset.
        InvAccess curAccess = curResolverDs.getAccess( ServiceType.RESOLVER );
        String curResDsPath = curAccess.getStandardUri().toString();
        InvCatalogImpl curResolvedCat = null;
        try
        {
          curResolvedCat = openAndValidateCatalog( curResDsPath );
        }
        catch ( AssertionFailedError e )
        {
          failureMsgs.put( curResDsPath, "Failed to open and validate resolver catalog: " + e.getMessage());
          continue;
        }

        List curDatasets = curResolvedCat.getDatasets();
        if ( curDatasets.size() != 1 )
        {
          failureMsgs.put( curResDsPath, "Wrong number of datasets <" + curDatasets.size() + "> in resolved catalog <" + curResDsPath + ">." );
          continue;
        }

        // Open the actual (OPeNDAP) dataset.
        InvDatasetImpl curResolvedDs = (InvDatasetImpl) curDatasets.get( 0);
        InvAccess curResolvedDsAccess = curResolvedDs.getAccess( ServiceType.OPENDAP );
        String curResolvedDsPath = curResolvedDsAccess.getStandardUri().toString(); 

        String curSysTimeAsString = null;
        NetcdfDataset ncd;
        try
        {
          curSysTimeAsString = getCurrentSystemTimeAsISO8601();
          ncd = NetcdfDataset.openDataset( curResolvedDsPath );
        }
        catch ( IOException e )
        {
          failureMsgs.put( curResolvedDsPath, "[" + curSysTimeAsString + "] I/O error opening dataset <" + curResolvedDsPath + ">: " + e.getMessage() );
          continue;
        }

        if ( ncd == null )
        {
          failureMsgs.put( curResolvedDsPath, "[" + curSysTimeAsString + "] Failed to open dataset <" + curResolvedDsPath + ">." );
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
          failureMsgs.put( curResolvedDsPath, "[" + curSysTimeAsString + "] I/O error opening typed dataset <" + curResolvedDsPath + ">: " + e.getMessage() );
          continue;
        }
        if ( typedDs == null )
        {
          failureMsgs.put( curResolvedDsPath, "[" + curSysTimeAsString + "] Failed to open typed dataset <" + curResolvedDsPath + ">." );
          continue;
        }

        //Date startDate = typedDs.getStartDate();
        //if ( startDate.getTime() < System.currentTimeMillis()) ...
      }

      if ( !failureMsgs.isEmpty() )
      {
        StringBuffer failMsg = new StringBuffer( "Some resolver datasets failed to open:" );
        for ( String curPath : failureMsgs.keySet() )
        {
          String curMsg = failureMsgs.get( curPath );
          failMsg.append( "\n" ).append( curPath ).append( ": " ).append( curMsg );
        }
        fail( failMsg.toString());
      }
    }
  }

  public static boolean openValidateAndCrawlCatRefCatalogs( String catalogUrl )
  {
    StringBuffer log = new StringBuffer();
    InvCatalogImpl cat = openAndValidateCatalogForChaining( catalogUrl, log );
    if ( cat == null )
    {
      fail( "Could not open or validate catalog <" + catalogUrl + ">: " + log.toString() );
      return false;
    }
    List<InvDatasetImpl> catRefList = findAllCatRefs( cat.getDatasets() );

    return true;
  }

  private static List<InvDatasetImpl> findAllResolverDatasets( List<InvDatasetImpl> datasets )
  {
    List<InvDatasetImpl> resolverDsList = new ArrayList<InvDatasetImpl>();
    for ( InvDatasetImpl curDs : datasets )
    {
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

  /**
   * Find all catalogRef elements in a catalog.
   * @param datasets
   * @return
   */
  private static List<InvDatasetImpl> findAllCatRefs( List<InvDatasetImpl> datasets )
  {
    List<InvDatasetImpl> catRefList = new ArrayList<InvDatasetImpl>();
    for ( InvDatasetImpl curDs : datasets )
    {
      if ( curDs instanceof InvCatalogRef &&
           ! ( curDs instanceof InvDatasetScan ||
               curDs instanceof InvDatasetFmrc ) )
      {
        catRefList.add( curDs );
        continue;
      }
      if ( ! ( curDs instanceof InvCatalogRef ) ) continue;
      if ( curDs instanceof InvDatasetScan ) continue;
      if ( curDs instanceof InvDatasetFmrc ) continue;

      if ( curDs.hasNestedDatasets() )
      {
        catRefList.addAll( findAllCatRefs( curDs.getDatasets() ) );
      }
    }
    return catRefList;
  }

  private static String getCurrentSystemTimeAsISO8601()
  {
    long curTime = System.currentTimeMillis();
    Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) );
    cal.setTimeInMillis( curTime );
    Date curSysDate = cal.getTime();
    SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd\'T\'HH:mm:ss.SSSz", Locale.US );
    dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

    return dateFormat.format( curSysDate );
  }

}
