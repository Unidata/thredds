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
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Attribute;
import ucar.unidata.util.DateUtil;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.EPSG_OGC_CF_Helper;

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

  private boolean showDebug;
  private boolean verbose;

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
    verbose = Boolean.parseBoolean( System.getProperty( "thredds.tds.test.verbose", "false" ) );

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
    StringBuffer log = new StringBuffer();

    try
    {
      for ( int i = 0; i < catalogList.length; i++ )
      {
        pass &= TestAll.crawlCatalogOpenRandomDataset( targetTdsUrl + catalogList[i], log, verbose );
      }
    }
    catch ( Exception e )
    {
      e.printStackTrace();
    }

    assertTrue( "Failed to open dataset(s):\n========================================\n" +
                log.toString() + "\n========================================\n",
                pass );

    if ( log.length() > 0 )
    {
      System.out.println( log.toString() );
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
    catch ( Exception e )
    {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream( os );
      e.printStackTrace( ps );
      ps.close();
      failureMsgs.put( curResolvedDsPath, "[" + curSysTimeAsString + "] Exception opening dataset <" + curResolvedDsPath + ">: " + os.toString() );
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
    catch ( Exception e )
    {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream( os );
      e.printStackTrace( ps );
      ps.close();
      failureMsgs.put( curResolvedDsPath, "[" + curSysTimeAsString + "] Exception opening typed dataset <" + curResolvedDsPath + ">: " + os.toString() );
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

  public static boolean crawlCatalogOpenRandomDataset( String catalogUrl, StringBuffer log, boolean verbose )
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
        catch ( Exception e )
        {
          ByteArrayOutputStream os = new ByteArrayOutputStream();
          PrintStream ps = new PrintStream( os );
          e.printStackTrace( ps);
          ps.close();
          failureMsgs.put( ds.getName(), "Exception while trying to open: " + os.toString() + ( localLog.length() > 0 ? "\n" + localLog.toString() : "" ) );
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
      log.append( "\nFailed to open some datasets:" );
      for ( String curPath : failureMsgs.keySet() )
      {
        String curMsg = failureMsgs.get( curPath );
        log.append( "\n  " ).append( curPath ).append( ": " ).append( curMsg );
      }

      pass = false;
    }

    if ( verbose && crawlMsg.length() > 0 )
    {
      log.append( "\n\nMessage from catalog crawling:\n" ).append( crawlMsg );
    }

    return pass;
  }

  /**
   * Ethan's attempt to gather projection and vertical coordinate
   * information for motherlode NCEP model data.
   *
   * @param args not used
   */
  public static void main( String[] args )
  {
    final ThreddsDataFactory threddsDataFactory = new ThreddsDataFactory();
    final Map<String, String> failureMsgs = new HashMap<String, String>();
    final StringBuffer gcsMsg = new StringBuffer();
    final StringBuffer otherMsg = new StringBuffer();

    List<String> catList = new ArrayList<String>();
//    catList.add( "fmrc/NCEP/GFS/Alaska_191km");
//    catList.add( "fmrc/NCEP/GFS/CONUS_80km");
//    catList.add( "fmrc/NCEP/GFS/CONUS_95km");
//    catList.add( "fmrc/NCEP/GFS/CONUS_191km");
//    catList.add( "fmrc/NCEP/GFS/Global_0p5deg");
//    catList.add( "fmrc/NCEP/GFS/Global_onedeg");
    catList.add( "fmrc/NCEP/GFS/Global_2p5deg");
//    catList.add( "fmrc/NCEP/GFS/Hawaii_160km");
//    catList.add( "fmrc/NCEP/GFS/N_Hemisphere_381km");
//    catList.add( "fmrc/NCEP/GFS/Puerto_Rico_191km");
//    catList.add( "fmrc/NCEP/NAM/Alaska_11km");
//    catList.add( "fmrc/NCEP/NAM/Alaska_22km");
//    catList.add( "fmrc/NCEP/NAM/Alaska_45km/noaaport");
//    catList.add( "fmrc/NCEP/NAM/Alaska_45km/conduit");
//    catList.add( "fmrc/NCEP/NAM/Alaska_95km");
//    catList.add( "fmrc/NCEP/NAM/CONUS_12km");
//    catList.add( "fmrc/NCEP/NAM/CONUS_20km/surface");
//    catList.add( "fmrc/NCEP/NAM/CONUS_20km/selectsurface");
//    catList.add( "fmrc/NCEP/NAM/CONUS_20km/noaaport");
//    catList.add( "fmrc/NCEP/NAM/CONUS_40km/conduit");
//    catList.add( "fmrc/NCEP/NAM/CONUS_80km");
//    catList.add( "fmrc/NCEP/NAM/Polar_90km");
//    catList.add( "fmrc/NCEP/RUC2/CONUS_20km/surface");
//    catList.add( "fmrc/NCEP/RUC2/CONUS_20km/pressure");
//    catList.add( "fmrc/NCEP/RUC2/CONUS_20km/hybrid");
//    catList.add( "fmrc/NCEP/RUC2/CONUS_40km");
//    catList.add( "fmrc/NCEP/RUC/CONUS_80km");
//    catList.add( "fmrc/NCEP/DGEX/CONUS_12km");
//    catList.add( "fmrc/NCEP/DGEX/Alaska_12km");
//    catList.add( "fmrc/NCEP/NDFD/CONUS_5km");

    CatalogCrawler.Listener listener = new CatalogCrawler.Listener()
    {
      public void getDataset( InvDataset ds )
      {
        StringBuffer localLog = new StringBuffer();

        gcsMsg.append( ds.getFullName()).append("\n");
        NetcdfDataset ncd;
        try
        {
          ncd = threddsDataFactory.openDataset( ds, false, null, localLog );
        }
        catch ( IOException e )
        {
          failureMsgs.put( ds.getName(), "I/O error while trying to open: " + e.getMessage() + ( localLog.length() > 0 ? "\n" + localLog.toString() : "" ) );
          return;
        }
        catch ( Exception e )
        {
          ByteArrayOutputStream os = new ByteArrayOutputStream();
          PrintStream ps = new PrintStream( os );
          e.printStackTrace( ps );
          ps.close();
          failureMsgs.put( ds.getName(), "Exception while trying to open: " + os.toString() + ( localLog.length() > 0 ? "\n" + localLog.toString() : "" ) );
          return;
        }

        if ( ncd == null )
        {
          failureMsgs.put( ds.getName(), "Failed to open dataset: " + ( localLog.length() > 0 ? "\n" + localLog.toString() : "" ) );
          return;
        }

        GridDataset gridDs = new ucar.nc2.dt.grid.GridDataset( ncd );

        boolean getVertCoordInfo = false;
        if ( getVertCoordInfo)
        {
          // Figure out vertical coord info
          for ( GridDataset.Gridset curGridset : gridDs.getGridsets())
          {
            gcsMsg.append( "  GeoCoordSys Name=").append( curGridset.getGeoCoordSystem().getName()).append( "\n");
            ProjectionImpl proj = curGridset.getGeoCoordSystem().getProjection();
            if ( proj != null)
            {
              gcsMsg.append( "    Projection:\n")
                    .append( "      Name=").append( proj.getName()).append( "\n")
                    .append( "      ClassName=").append( proj.getClassName()).append( "\n")
                    .append( "      Params=").append( proj.paramsToString()).append( "\n")
                    .append( "      oString()=").append( proj.toString()).append( "\n");
            }
            CoordinateAxis1D verticalAxis = curGridset.getGeoCoordSystem().getVerticalAxis();
            if ( verticalAxis != null )
            {
              gcsMsg.append( "    VerticalAxis:\n")
                    .append( "      Name =").append( verticalAxis.getName()).append( "\n")
                    .append( "      Description =").append( verticalAxis.getDescription()).append( "\n")
                    .append( "      UnitsString =").append( verticalAxis.getUnitsString()).append( "\n");
            }
            VerticalCT vct = curGridset.getGeoCoordSystem().getVerticalCT();
            if ( vct != null )
            {
              gcsMsg.append( "    VerticalCT:\n" )
                    .append( "        name=" ).append( vct.getName()).append( "\n");
            }

            VerticalTransform vt = curGridset.getGeoCoordSystem().getVerticalTransform();
            if ( vt != null )
            {
              gcsMsg.append( "    VerticalTransform:\n" )
                    .append( "        unit=" ).append( vt.getUnitString() ).append( "\n" );
            }
          }
        }
        else
        {
// Figure out X-Y projection information
          GridDataset.Gridset aGridset = gridDs.getGridsets().get( 0);
          Attribute gridMappingAtt = aGridset.getGrids().get( 0 ).findAttributeIgnoreCase( "grid_mapping" );
          if ( gridMappingAtt != null )
          {
            String gridMapping = gridMappingAtt.getStringValue();
            VariableSimpleIF gridMapVar = gridDs.getDataVariable( gridMapping );

            gcsMsg.append( "    GridDataset GridMap <" ).append( gridMapVar.getName() ).append( "> Params:\n" );
            for ( Attribute curAtt : gridMapVar.getAttributes() )
            {
              gcsMsg.append( "      " ).append( curAtt.toString() ).append( "\n" );
            }
          }
          else
          {
            GridCoordSystem geoCoordSystem = aGridset.getGeoCoordSystem();
            if ( geoCoordSystem != null )
            {
              ProjectionImpl proj = geoCoordSystem.getProjection();
              if ( proj != null )
              {
                gcsMsg.append( "    Projection:\n" )
                        .append( "      Name=" ).append( proj.getName() ).append( "\n" )
                        .append( "      ClassName=" ).append( proj.getClassName() ).append( "\n" )
                        .append( "      Params=" ).append( proj.paramsToString() ).append( "\n" )
                        .append( "      oString()=" ).append( proj.toString() ).append( "\n" );

                String crsId = EPSG_OGC_CF_Helper.getWcs1_0CrsId( proj );
                gcsMsg.append( "    CRS:\n" )
                        .append( "      ID=" ).append( crsId ).append( "\n" );

              }
            }
          }
        }


      }
    };
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_RANDOM_DIRECT, false, listener );

    for ( String curCat : catList )
    {
      gcsMsg.append( "********************\n<h4>" ).append( curCat ).append( "</h4>\n\n<pre>\n" );
      curCat = "http://motherlode.ucar.edu:8080/thredds/catalog/" + curCat + "/files/catalog.xml";
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PrintStream out = new PrintStream( os );
      int numDs = 0;
      try
      {
        numDs = crawler.crawl( curCat, null, out );
      }
      catch ( Exception e )
      {
        gcsMsg.append( "**********\n  Exception:\n" ).append( e.getMessage() ).append( "\n**********\n" );
        otherMsg.append( "**********\n  Exception:\n" ).append( e.getMessage() ).append( "\n**********\n" );
      }
      out.close();
      otherMsg.append( "********************\n" )
              .append( curCat )
              .append( "\n******************** <").append( numDs).append(">\n")
              .append( os.toString());
      gcsMsg.append( "</pre>\n\n");
    }


    if ( !failureMsgs.isEmpty() )
    {
      otherMsg.append( "\n********************\nFailed to open some datasets:" );
      for ( String curPath : failureMsgs.keySet() )
      {
        String curMsg = failureMsgs.get( curPath );
        otherMsg.append( "\n  " ).append( curPath ).append( ": " ).append( curMsg );
      }
    }

    System.out.println( gcsMsg );
    System.out.println( "\n\n\n\n********************\nMessage from catalog crawling:\n" + otherMsg);

    return;
  }

  /**
   * Find all catalogRef elements in a dataset list.
   *
   * @param datasets the list of datasets from which to find all the catalogRefs
   * @param log StringBuffer into which any messages will be written
   * @param onlyRelativeUrls only include catalogRefs with relative HREF URLs if true, otherwise include all catalogRef datasets
   * @return the list of catalogRef datasets
   */
  private static List<InvCatalogRef> findAllCatRefs( List<InvDataset> datasets, StringBuffer log, boolean onlyRelativeUrls )
  {
    List<InvCatalogRef> catRefList = new ArrayList<InvCatalogRef>();
    for ( InvDataset invds : datasets )
    {
      InvDatasetImpl curDs = (InvDatasetImpl) invds;

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
