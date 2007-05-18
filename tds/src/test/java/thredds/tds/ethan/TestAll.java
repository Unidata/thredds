package thredds.tds.ethan;

import junit.framework.*;

import java.util.*;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

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
  public static Test suite()
  {
    TestSuite suite = new TestSuite();

    String tdsTestLevel = System.getProperty( "thredds.tds.test.level", "ping-catalogs" );
    System.out.println( "Test level: " + tdsTestLevel );

    if ( tdsTestLevel.equalsIgnoreCase( "ping-mlode" ) )
    {
      //System.setProperty( "thredds.tds.test.server", "motherlode.ucar.edu:8080" );
      suite.addTestSuite( TestTdsPingMotherlode.class );
    }
    else if ( tdsTestLevel.equalsIgnoreCase( "ping-catalogs" ) )
    {
      //System.setProperty( "thredds.tds.test.server", "motherlode.ucar.edu:8080" );
      //System.setProperty( "thredds.tds.test.catalogs", "catalog.xml" );
      suite.addTest( new TestAll( "testPingCatalogs" ) );
    }
    else if ( tdsTestLevel.equalsIgnoreCase( "crawl-catalogs" ) )
    {
      //System.setProperty( "thredds.tds.test.server", "motherlode.ucar.edu:8080" );
      //System.setProperty( "thredds.tds.test.catalogs", "catalog.xml" );
      suite.addTest( new TestAll( "testCrawlCatalogs" ) );
    }
    else if ( tdsTestLevel.equalsIgnoreCase( "crawl-catalogs-and1DsPerCollection" ) )
    {
      //System.setProperty( "thredds.tds.test.server", "motherlode.ucar.edu:8080" );
      //System.setProperty( "thredds.tds.test.catalogs", "catalog.xml" );
      suite.addTest( new TestAll( "testCrawlCatalogsOpenOneDatasetInEachCollection" ) );
    }
    else if ( tdsTestLevel.equalsIgnoreCase( "ping-idd" ) )
    {
      System.setProperty( "thredds.tds.test.catalogs", "catalog.xml,idd/models.xml" );
      suite.addTest( new TestAll( "testPingCatalogs" ) );
    }
    else if ( tdsTestLevel.equalsIgnoreCase( "crawl-catalogs-oneLevelDeep" ) )
    {
      //System.setProperty( "thredds.tds.test.server", "motherlode.ucar.edu:8080" );
      //System.setProperty( "thredds.tds.test.catalogs", "catalog.xml" );
      suite.addTest( new TestAll( "testCrawlCatalogsOneLevelDeep" ) );
    }
    else if ( tdsTestLevel.equalsIgnoreCase( "crawl-topcatalog" ) )
    {
      //System.setProperty( "thredds.tds.test.server", "motherlode.ucar.edu:8080" );
      System.setProperty( "thredds.tds.test.catalogs", "topcatalog.xml" );
      suite.addTest( new TestAll( "testCrawlCatalogsOneLevelDeep" ) );
    }
    else
    {
      suite.addTestSuite( thredds.tds.ethan.TestTdsPingMotherlode.class );
    }


    return suite;
  }

  private boolean showDebug = false;

  private String host = "motherlode.ucar.edu:8080";
  private String[] catalogList;

  private String targetTdsUrl;

  public TestAll( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    host = System.getProperty( "thredds.tds.test.server", host );
    targetTdsUrl = "http://" + host + "/thredds/";

    showDebug = Boolean.parseBoolean( System.getProperty( "thredds.tds.test.showDebug", "false" ) );

    String catalogListString = System.getProperty( "thredds.tds.test.catalogs", null );
    if ( catalogListString == null )
      catalogListString = System.getProperty( "thredds.tds.test.catalog", "catalog.xml" );
    catalogList = catalogListString.split( "," );
  }

  public void testPingCatalogs()
  {
    boolean pass = true;
    StringBuffer msg = new StringBuffer();

    for ( int i = 0; i < catalogList.length; i++ )
    {
      pass &= null != TestAll.openAndValidateCatalog( targetTdsUrl + catalogList[i], msg, showDebug );
    }
    assertTrue( "Ping failed on catalog(s): " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testCrawlCatalogs()
  {
    boolean pass = true;
    StringBuffer msg = new StringBuffer();

    for ( int i = 0; i < catalogList.length; i++ )
    {
      pass &= TestAll.openAndValidateCatalogTree( targetTdsUrl + catalogList[i], msg, true );
    }
    assertTrue( "Invalid catalog(s): " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testCrawlCatalogsOneLevelDeep()
  {
    boolean pass = true;
    StringBuffer msg = new StringBuffer();

    for ( int i = 0; i < catalogList.length; i++ )
    {
      pass &= TestAll.openAndValidateCatalogOneLevelDeep( targetTdsUrl + catalogList[i], msg, false );
    }

    assertTrue( "Invalid catalog(s): " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testCrawlCatalogsOpenOneDatasetInEachCollection()
  {
    boolean pass = true;
    StringBuffer msg = new StringBuffer();

    for ( int i = 0; i < catalogList.length; i++ )
    {
      pass &= TestAll.crawlCatalogOpenRandomDataset( targetTdsUrl + catalogList[i], msg );
    }

    assertTrue( "Failed to open dataset(s):\n" + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
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

  public static InvCatalogImpl openAndValidateCatalog( String catUrl, StringBuffer log, boolean logToStdOut )
  {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false );
    StringBuffer validationMsg = new StringBuffer();
    StringBuffer tmpMsg = new StringBuffer();
    String curSysTimeAsString = null;
    try
    {
      curSysTimeAsString = DateUtil.getCurrentSystemTimeAsISO8601();

      InvCatalogImpl cat = catFactory.readXML( catUrl );
      boolean isValid = cat.check( validationMsg, false );
      if ( !isValid )
      {
        tmpMsg.append( "Invalid catalog <" ).append( catUrl ).append( ">." ).append( validationMsg.length() > 0 ? " Validation messages:\n" + validationMsg.toString() : "" );
        if ( logToStdOut ) System.out.println( tmpMsg.toString() );
        log.append( log.length() > 0 ? "\n" : "").append( tmpMsg );
        return null;
      }
      else
      {
        tmpMsg.append( "Valid catalog <" ).append( catUrl ).append( ">." ).append( validationMsg.length() > 0 ? " Validation messages:\n" + validationMsg.toString() : "" );
        if ( logToStdOut ) System.out.println( tmpMsg.toString() );
        log.append( log.length() > 0 ? "\n" : "" ).append( tmpMsg );
        return cat;
      }
    }
    catch ( Exception e )
    {
      tmpMsg.append( "[" ).append( curSysTimeAsString ).append( "] Exception while parsing catalog <" ).append( catUrl ).append( ">: " ).append( e.getMessage() );
      if ( logToStdOut ) System.out.println( tmpMsg.toString() );
      log.append( log.length() > 0 ? "\n" : "" ).append( tmpMsg );
      return null;
    }
  }

  public static boolean openAndValidateCatalogTree( String catUrl, StringBuffer log, boolean onlyRelativeUrls )
  {
    InvCatalogImpl cat = openAndValidateCatalog( catUrl, log, true );
    if ( cat == null )
      return false;

    boolean ok = true;
    List<InvCatalogRef> catRefList = findAllCatRefs( cat.getDatasets(), log, onlyRelativeUrls );
    for ( InvCatalogRef catRef : catRefList )
    {
      if ( onlyRelativeUrls )
      {
        URI uri = null;
        String href = ( (InvCatalogRef) catRef ).getXlinkHref();
        String title = ( (InvCatalogRef) catRef ).getName();
        try
        {
          uri = new URI( href );
        }
        catch ( URISyntaxException e )
        {
          log.append( log.length() > 0 ? "\n" : "" ).append( "Bad catalogRef <" ).append( title ).append( ">: " ).append( href );
          continue;
        }
        if ( uri.isAbsolute())
        {
          continue;
        }
      }
      ok &= openAndValidateCatalogTree( catRef.getURI().toString(), log, onlyRelativeUrls);
    }

    return ok;
  }

  public static boolean openAndValidateCatalogOneLevelDeep( String catUrl, StringBuffer log, boolean onlyRelativeUrls )
  {
    InvCatalogImpl cat = openAndValidateCatalog( catUrl, log, true );
    if ( cat == null )
      return false;

    boolean ok = true;
    List<InvCatalogRef> catRefList = findAllCatRefs( cat.getDatasets(), log, onlyRelativeUrls );
    for ( InvCatalogRef catRef : catRefList )
    {
      InvCatalogImpl cat2 = openAndValidateCatalog( catRef.getURI().toString(), log, true );
      ok &= cat2 != null;
    }

    return ok;
  }

  public static InvCatalogImpl openValidateAndCheckExpires( String catalogUrl, StringBuffer log )
  {
    InvCatalogImpl catalog = openAndValidateCatalog( catalogUrl, log, false );
    if ( catalog == null )
    {
      return null;
    }

    // Check if the catalog has expired.
    DateType expiresDateType = catalog.getExpires();
    if ( expiresDateType != null )
    {
      if ( expiresDateType.getDate().getTime() < System.currentTimeMillis() )
      {
        log.append( log.length() > 0 ? "\n" : "" ).append( "Expired catalog <" ).append( catalogUrl ).append( ">: " ).append( expiresDateType.toDateTimeStringISO() ).append( "." );
        return null;
      }
    }

    return catalog;
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
    StringBuffer buf = new StringBuffer();
    InvCatalogImpl curResolvedCat = openAndValidateCatalog( curResDsPath, buf, false );
    if ( curResolvedCat == null )
    {
      failureMsgs.put( curResDsPath, "Failed to open and validate resolver catalog: " + buf.toString() );
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
    buf = new StringBuffer();
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
    int numDs = crawler.crawl( catalogUrl, null, out );
    out.close();
    String crawlMsg = os.toString();

    log.append( log.length() > 0 ? "\n\n" : "")
       .append( "Crawled and opened datasets <" ).append( numDs ).append( "> in catalog <" ).append( catalogUrl ).append( ">." );

    boolean pass = true;
    if ( ! failureMsgs.isEmpty() )
    {
      log.append( "\n" ).append( "Failed to open some datasets:" );
      for ( String curPath : failureMsgs.keySet() )
      {
        String curMsg = failureMsgs.get( curPath );
        log.append( "\n  " ).append( curPath ).append( ": " ).append( curMsg );
      }
      pass = false;
    }
    if ( crawlMsg.length() > 0 )
    {
      log.append( "\n" ).append( "Message from catalog crawling:" ).append( "\n  " ).append( crawlMsg );
    }

    return pass;
  }

  /**
   * Find all catalogRef elements in a dataset list.
   *
   * @param datasets the list of datasets from which to find all the catalogRefs
   * @param log StringBuffer into which any messages will be written
   * @param onlyRelativeUrls only include catalogRefs with relative HREF URLs if true, otherwise include all catalogRef datasets
   * @return the list of catalogRef datasets
   */
  private static List<InvCatalogRef> findAllCatRefs( List<InvDatasetImpl> datasets, StringBuffer log, boolean onlyRelativeUrls )
  {
    List<InvCatalogRef> catRefList = new ArrayList<InvCatalogRef>();
    for ( InvDatasetImpl curDs : datasets )
    {
      if ( curDs instanceof InvDatasetScan ) continue;
      if ( curDs instanceof InvDatasetFmrc ) continue;

      if ( curDs instanceof InvCatalogRef )
      {
        InvCatalogRef catRef = (InvCatalogRef) curDs;
        String name = catRef.getName();
        String href = catRef.getXlinkHref();
        URI uri;
        try
        {
          uri = new URI( href);
        }
        catch ( URISyntaxException e )
        {
          log.append( log.length() > 0 ? "\n" : "" ).append( "***WARN - CatalogRef with bad HREF <" ).append( name ).append( " - " ).append( href ).append( "> " );
          continue;
        }
        if ( uri.isAbsolute()) continue;

        catRefList.add( catRef );
        continue;
      }

      if ( curDs.hasNestedDatasets() )
      {
        catRefList.addAll( findAllCatRefs( curDs.getDatasets(), log, onlyRelativeUrls ) );
      }
    }
    return catRefList;
  }

}
