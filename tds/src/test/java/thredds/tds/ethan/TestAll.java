package thredds.tds.ethan;

import junit.framework.*;

import java.util.*;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

import thredds.catalog.*;
import thredds.catalog.crawl.CatalogCrawler;
import thredds.catalog.query.DqcFactory;
import thredds.catalog.query.QueryCapability;
import thredds.datatype.DateType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.unidata.util.DateUtil;

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

    String tdsTestLevel = System.getProperty( "thredds.tds.test.level", "ping-mlode" );
    System.out.println( "Test level: " + tdsTestLevel );

    if ( tdsTestLevel.equalsIgnoreCase( "ping-mlode" ) )
    {
      //System.setProperty( "thredds.tds.test.server", "motherlode.ucar.edu:8080" );
      suite.addTestSuite( TestTdsPingMotherlode.class );
    }
    else if ( tdsTestLevel.equalsIgnoreCase( "crawl-mlode" ) )
    {
      //System.setProperty( "thredds.tds.test.server", "motherlode.ucar.edu:8080" );
      //System.setProperty( "thredds.tds.test.catalog", "catalog.xml" );
      suite.addTest( new TestTdsBasics( "testCrawlCatalog" ) );
      suite.addTest( new TestTdsBasics( "testCrawlCatalogOpenOneDatasetInEachCollection" ) );
    }
    else if ( tdsTestLevel.equalsIgnoreCase( "crawl-topcatalog" ) )
    {
      //System.setProperty( "thredds.tds.test.server", "motherlode.ucar.edu:8080" );
      System.setProperty( "thredds.tds.test.catalog", "topcatalog.xml" );
      suite.addTest( new TestTdsBasics( "testCrawlCatalogOneLevelDeep" ) );
    }
    else if ( tdsTestLevel.equalsIgnoreCase( "ping-idd" ) )
    {
      suite.addTestSuite( thredds.tds.ethan.TestTdsIddPing.class );
    }
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

  public static boolean openAndValidateCatalogTree( String catUrl, StringBuffer log )
  {
    InvCatalogImpl cat = openAndValidateCatalogForChaining( catUrl, log );
    if ( cat == null )
      return false;

    boolean ok = true;
    List<InvCatalogRef> catRefList = findAllCatRefs( cat.getDatasets() );
    for ( InvCatalogRef catRef : catRefList )
    {
      ok &= openAndValidateCatalogTree( catRef.getURI().toString(), log);
    }

    return ok;
  }

  public static boolean openAndValidateCatalogOneLevelDeep( String catUrl, StringBuffer log )
  {
    InvCatalogImpl cat = openAndValidateCatalogForChaining( catUrl, log );
    if ( cat == null )
      return false;

    boolean ok = true;
    List<InvCatalogRef> catRefList = findAllCatRefs( cat.getDatasets() );
    for ( InvCatalogRef catRef : catRefList )
    {
      InvCatalogImpl cat2 = openAndValidateCatalogForChaining( catRef.getURI().toString(), log );
      ok &= cat2 != null;
    }

    return ok;
  }

  public static InvCatalogImpl openAndValidateCatalogForChaining( String catUrl, StringBuffer log )
  {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false );
    StringBuffer validationMsg = new StringBuffer();
    String curSysTimeAsString = null;
    try
    {
      curSysTimeAsString = DateUtil.getCurrentSystemTimeAsISO8601();

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

  public static void openValidateAndCheckAllLatestModelsInCatalogTree( String catalogUrl )
  {
    final Map<String, String> failureMsgs = new HashMap<String, String>();

    CatalogCrawler.Listener listener = new CatalogCrawler.Listener()
    {
      public void getDataset( InvDataset ds )
      {
        if ( ds.hasAccess() && ds.getAccess( ServiceType.RESOLVER ) != null )
          checkLatestModelResolverDs( ds, failureMsgs );
      }
    };
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_ALL_DIRECT, false, listener );

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream out = new PrintStream( os );
    crawler.crawl( catalogUrl, null, out );
    out.close();
    String crawlMsg = os.toString();

    if ( !failureMsgs.isEmpty() )
    {
      StringBuffer failMsg = new StringBuffer( "Failed to open some datasets:" );
      for ( String curPath : failureMsgs.keySet() )
      {
        String curMsg = failureMsgs.get( curPath );
        failMsg.append( "\n  " ).append( curPath ).append( ": " ).append( curMsg );
      }
      if ( crawlMsg.length() > 0 )
      {
        failMsg.append( "Message from catalog crawling:" ).append( "\n  " ).append( crawlMsg );
      }
      fail( failMsg.toString() );
    }
  }

  private static boolean checkLatestModelResolverDs( InvDataset ds, Map<String, String> failureMsgs )
  {
    // Resolve the resolver dataset.
    InvAccess curAccess = ds.getAccess( ServiceType.RESOLVER );
    String curResDsPath = curAccess.getStandardUri().toString();
    InvCatalogImpl curResolvedCat = null;
    try
    {
      curResolvedCat = openAndValidateCatalog( curResDsPath );
    }
    catch ( AssertionFailedError e )
    {
      failureMsgs.put( curResDsPath, "Failed to open and validate resolver catalog: " + e.getMessage() );
      return false;
    }

    List curDatasets = curResolvedCat.getDatasets();
    if ( curDatasets.size() != 1 )
    {
      failureMsgs.put( curResDsPath, "Wrong number of datasets <" + curDatasets.size() + "> in resolved catalog <" + curResDsPath + ">." );
      return false;
    }

    // Open the actual (OPeNDAP) dataset.
    InvDatasetImpl curResolvedDs = (InvDatasetImpl) curDatasets.get( 0 );
    InvAccess curResolvedDsAccess = curResolvedDs.getAccess( ServiceType.OPENDAP );
    String curResolvedDsPath = curResolvedDsAccess.getStandardUri().toString();

    String curSysTimeAsString = null;
    NetcdfDataset ncd;
    try
    {
      curSysTimeAsString = DateUtil.getCurrentSystemTimeAsISO8601();
      ncd = NetcdfDataset.openDataset( curResolvedDsPath );
    }
    catch ( IOException e )
    {
      failureMsgs.put( curResolvedDsPath, "[" + curSysTimeAsString + "] I/O error opening dataset <" + curResolvedDsPath + ">: " + e.getMessage() );
      return false;
    }

    if ( ncd == null )
    {
      failureMsgs.put( curResolvedDsPath, "[" + curSysTimeAsString + "] Failed to open dataset <" + curResolvedDsPath + ">." );
      return false;
    }

    // Open the dataset as a CDM Scientific Datatype.
    StringBuffer buf = new StringBuffer();
    TypedDataset typedDs;
    try
    {
      typedDs = TypedDatasetFactory.open( null, ncd, null, buf );
    }
    catch ( IOException e )
    {
      failureMsgs.put( curResolvedDsPath, "[" + curSysTimeAsString + "] I/O error opening typed dataset <" + curResolvedDsPath + ">: " + e.getMessage() );
      return false;
    }
    if ( typedDs == null )
    {
      failureMsgs.put( curResolvedDsPath, "[" + curSysTimeAsString + "] Failed to open typed dataset <" + curResolvedDsPath + ">." );
      return false;
    }

    //Date startDate = typedDs.getStartDate();
    //if ( startDate.getTime() < System.currentTimeMillis()) ...

    return true;
  }

  public static boolean crawlCatalogOpenRandomDataset( String catalogUrl, StringBuffer log )
  {
    final ThreddsDataFactory threddsDataFactory = new ThreddsDataFactory();
    final Map<String, String> failureMsgs = new HashMap<String, String>();

    CatalogCrawler.Listener listener = new CatalogCrawler.Listener()
    {
      public void getDataset( InvDataset ds )
      {
        StringBuffer localLog = new StringBuffer();
        NetcdfDataset ncd;
        try
        {
          ncd = threddsDataFactory.openDataset( ds, false, null, localLog );
        }
        catch ( IOException e )
        {
          failureMsgs.put( ds.getName(), "I/O error while trying to open: " + e.getMessage() + (localLog.length() > 0 ? "\n" + localLog.toString() : "") );
          return;
        }

        if ( ncd == null )
        {
          failureMsgs.put( ds.getName(), "Failed to open dataset: " + ( localLog.length() > 0 ? "\n" + localLog.toString() : "" ) );
          return;
        }
      }
    };
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_RANDOM_DIRECT, false, listener );

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream out = new PrintStream( os );
    crawler.crawl( catalogUrl, null, out );
    out.close();
    String crawlMsg = os.toString();


    if ( crawlMsg.length() > 0 )
    {
      log.append( "Message from catalog crawling:" ).append( "\n  " ).append( crawlMsg );
    }
    if ( ! failureMsgs.isEmpty() )
    {
      log.append( "Failed to open some datasets:" );
      for ( String curPath : failureMsgs.keySet() )
      {
        String curMsg = failureMsgs.get( curPath );
        log.append( "\n  " ).append( curPath ).append( ": " ).append( curMsg );
      }
      return false;
    }

    return true;
  }

  /**
   * Find all catalogRef elements in a catalog.
   * @param datasets
   * @return
   */
  private static List<InvCatalogRef> findAllCatRefs( List<InvDatasetImpl> datasets )
  {
    List<InvCatalogRef> catRefList = new ArrayList<InvCatalogRef>();
    for ( InvDatasetImpl curDs : datasets )
    {
      if ( curDs instanceof InvDatasetScan ) continue;
      if ( curDs instanceof InvDatasetFmrc ) continue;

      if ( curDs instanceof InvCatalogRef )
      {
        catRefList.add( (InvCatalogRef) curDs );
        continue;
      }

      if ( curDs.hasNestedDatasets() )
      {
        catRefList.addAll( findAllCatRefs( curDs.getDatasets() ) );
      }
    }
    return catRefList;
  }

}
