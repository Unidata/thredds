package thredds.crawlabledataset;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import thredds.crawlabledataset.filter.MultiSelectorFilter;
import thredds.crawlabledataset.filter.RegExpMatchOnNameFilter;
import thredds.crawlabledataset.filter.WildcardMatchOnNameFilter;
import thredds.crawlabledataset.mock.MockCrawlableDataset;
import thredds.crawlabledataset.mock.MockCrawlableDatasetTreeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests to figure out problem described in JIRA issue
 * <a href="https://bugtracking.unidata.ucar.edu/browse/TDS-466">TDS-466</a>.
 * Basically:
 *
 * <p>
 *   As of 2013-08-23, LDM pqact-s for THREDDS place NEXRAD and TDWR level3
 *   products in the same directory structure. The directory structure looks
 *   like "level3/{Product}/{Stn}/{date}/Level3_{Stn}_{Product}_{date}_{time}.nids".
 * </p>
 * <p>
 *   NEXRAD and TDWR stations are distinct. Each has a distinct set of products
 *   as well as an overlapping set of products.
 * </p>
 * <p>
 *   Planning to separate into level3/nexrad and level3/tdwr directories.
 *   However, the datasetScan filter configuration currently in
 *   tds/src/main/webapp/WEB-INF/altContent/idd/thredds/idd/radars.xml
 *   is no longer working to separate the NEXRAD and TDWR data in the resulting
 *   catalogs.
 * </p>
 *
 * @author edavis
 */
public class ComplexFilterProblemTest_Jira_TDS_466 {

  private static MockCrawlableDataset topCrDs;

  private static String[] stnNamesNexrad = {
          "ABC", "ABR", "ABX", "AHG", "AIH", "AKC", "AKQ", "AMA", "AMX", "APX",
          //"ARX", "ATX", "BBX", "BGM", "BHX", "BIS", "BLX", "BMX", "BOX", "BRO",
          //"BUF", "BYX", "CAE", "CBW", "CBX", "CCX", "CLE", "CLX", "CRP", "CXX",
          //"CYS", "DAX", "DDC", "DFX", "DGX", "DIX", "DLH", "DMX", "DOX", "DTX",
          "DVN", "DYX", "EAX", "EMX", "ENX", "EOX", "EPZ", "ESX", "EVX", "EWX",
          //"EYX", "FCX", "FDR", "FDX", "FFC", "FSD", "FSX", "FTG", "FWS", "GGW",
          "GJX", "GLD", "GRB", "GRK", "GRR", "GSP", "GUA", "GWX", "GYX", "HDX",
          //"HGX", "HKI", "HKM", "HMO", "HNX", "HPX", "HTX", "HWA", "ICT", "ICX",
          "ILN", "ILX", "IND", "INX", "IWA", "IWX", "JAX", "JGX", "JKL", "JUA",
          //"LBB", "LCH", "LGX", "LIX", "LNX", "LOT", "LRX", "LSX", "LTX", "LVX",
          "LWX", "LZK", "MAF", "MAX", "MBX", "MHX", "MKX", "MLB", "MOB", "MPX",
          //"MQT", "MRX", "MSX", "MTX", "MUX", "MVX", "MXX", "NKX", "NQA", "OAX",
          "OHX", "OKX", "OTX", "PAH", "PBZ", "PDT", "POE", "PUX", "RAX", "RGX",
          //"RIW", "RLX", "RTX", "SFX", "SGF", "SHV", "SJT", "SOX", "SRX", "TBW",
          //"TFX", "TLH", "TLX", "TWX", "TYX", "UDX", "UEX", "VAX", "VBX", "VNX",
          "VTX", "VWX", "YUX"
  };

  private static String[] stnNamesTdwr = {
          //"ADW", "ATL", "BNA", "BOS", "BWI", "CLT", "CMH", "CVG", "DAL", "DAY",
          "DCA", "DEN", "DFW", "DTW", "EWR", "FLL", "HOU", "IAD", "IAH", "ICH",
          //"IDS", "JFK", "LAS", "LVE", "MCI", "MCO", "MDW", "MEM", "MIA", "MKE",
          "MSP", "MSY", "OKC", "ORD", "PBI", "PHL", "PHX", "PIT", "RDU", "SDF",
          "SJU", "SLC", "STL", "TPA", "TUL"};

  private static String[] productsForNexradOnly = {
          "DAA", "DOD", "DPR", "DSD", "DTA", "DU3", "DU6", "DVL", "EET", "HHC",
          //"N0C", "N0H", "N0K", "N0M", "N0Q", "N0R", "N0S", "N0U", "N0V", "N0X",
          "N0Z", "N1C", "N1H", "N1K", "N1M", "N1Q", "N1R", "N1S", "N1U", "N1V",
          //"N1X", "N2C", "N2H", "N2K", "N2M", "N2Q", "N2R", "N2S", "N2U", "N2X",
          "N3C", "N3H", "N3K", "N3M", "N3Q", "N3R", "N3S", "N3U", "N3X", "NAC",
          //"NAH", "NAK", "NAM", "NAQ", "NAU", "NAX", "NBC", "NBH", "NBK", "NBM",
          "NBQ", "NBU", "NBX", "OHA", "PTA"
  };
  private static String[] productsForBothNexradAndTdwr = {
          "DHR", "DPA", "DSP", "N1P", "NCR", "NET", "NMD", "NST", "NTP",
          "NVL", "NVW"
  };
  private static String[] productsForTdwrOnly = {
           "TR0", "TR1", "TR2", "TV0", "TV1", "TV2", "TZL"
  };

  private static String[] dates = {
          "20130822", "20130823", "20130824", "20130825"
  };

  private static String[] times = {
          "0006", "0715", "1455", "2332"
  };

  @BeforeClass
  public static void buildListsAndMaps() {
    topCrDs =  buildCrDsTree();
  }

  public static MockCrawlableDataset buildCrDsTree() {
    MockCrawlableDatasetTreeBuilder builder
            = new MockCrawlableDatasetTreeBuilder( "/data/ldm/pub/native/radar/level3", true );
    for ( String curProductName : productsForBothNexradAndTdwr ) {
      builder.addChild( curProductName, true );
      addStnDirsToCurrentProductDir(builder, curProductName, stnNamesNexrad);
      addStnDirsToCurrentProductDir(builder, curProductName, stnNamesTdwr);
    }
    for ( String curProductName : productsForNexradOnly ) {
      builder.addChild( curProductName, true );
      addStnDirsToCurrentProductDir( builder, curProductName, stnNamesNexrad);
    }
    for ( String curProductName : productsForTdwrOnly ) {
      builder.addChild( curProductName, true );
      addStnDirsToCurrentProductDir( builder, curProductName, stnNamesTdwr);
    }
    return builder.build();
  }

  private static void addStnDirsToCurrentProductDir( MockCrawlableDatasetTreeBuilder builder,
                                                     String curProductName, String[] stnNames ) {
    builder.moveDown( curProductName );
    for ( String curStnName : stnNames) {
      builder.addChild(curStnName, true);
      addDateDirsToCurrentStnDir(builder, curProductName, curStnName);
    }
    builder.moveUp();
  }

  private static void addDateDirsToCurrentStnDir(MockCrawlableDatasetTreeBuilder builder,
                                                 String curProductName, String curStnName) {
    builder.moveDown( curStnName );
    for ( String curDate : dates) {
      builder.addChild(curDate, true);
      addLevel3FilesToCurrentDateDir(builder, curProductName, curStnName, curDate);
    }
    builder.moveUp();
  }

  private static void addLevel3FilesToCurrentDateDir(
      MockCrawlableDatasetTreeBuilder builder,
      String curProductName, String curStnName, String curDate)
  {
    builder.moveDown(curDate);
    for ( String curTime : times) {
      String fileName = String.format( "Level3_%s_%s_%s_%s.nids", curStnName, curProductName, curDate, curTime);
      builder.addChild(fileName, false);
    }
    builder.moveUp();
  }

  private CrawlableDatasetFilter buildTdwrFilter() {
    List<MultiSelectorFilter.Selector> selectorList = new ArrayList<MultiSelectorFilter.Selector>();
    // Include Level3 atomic datasets
    selectorList.add( buildFileInclude() );
    // Include TDWR products collection datasets
    for ( String curProductName : productsForBothNexradAndTdwr )
      selectorList.add( this.buildSelector( curProductName, true, false, true));
    for ( String curProductName : productsForTdwrOnly )
      selectorList.add( this.buildSelector( curProductName, true, false, true));
    // Include TDWR stations collection datasets
    for ( String curStnName : stnNamesTdwr )
      selectorList.add( this.buildSelector( curStnName, true, false, true));
    // Include all directories with names of the form "yyyymmdd"
    selectorList.add( buildDateDirInclude());
    // Exclude all atomic datasets that start with a dot (".")
    selectorList.add( buildDotNameFileExclude());

    return new MultiSelectorFilter( selectorList );
  }

  private CrawlableDatasetFilter buildNexradFilter() {
    List<MultiSelectorFilter.Selector> selectorList = new ArrayList<MultiSelectorFilter.Selector>();
    // Include Level3 atomic datasets
    selectorList.add( buildFileInclude() );
    // Exclude collection datasets with TDWR only product names
    for ( String curProductName : productsForTdwrOnly ) {
      selectorList.add( this.buildSelector( curProductName, false, false, true ));
    }
    // Exclude collection datasets with TDWR station names
    for ( String curStnName : stnNamesTdwr ) {
      selectorList.add( this.buildSelector( curStnName, false, false, true ));
    }
    // Only excluders for collection datasets, so everything not explicitly excluded is included.
    // So don't explicitly
    // ... Include all directories with names of the form "yyyymmdd"
    // selectorList.add( buildDateDirInclude());
    // Exclude all atomic datasets that start with a dot (".")
    selectorList.add( buildDotNameFileExclude());

    return new MultiSelectorFilter( selectorList );

  }

  private MultiSelectorFilter.Selector buildFileInclude() {
    return new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( "Level3*.nids" ),
                                             true, true, false);
  }

  private MultiSelectorFilter.Selector buildDateDirInclude() {
    return new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( "[0-9]{8}" ),
                                             true, false, true);
  }

  private MultiSelectorFilter.Selector buildDotNameFileExclude() {
    return new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( ".*" ),
                                             false, false, true);
  }

  private MultiSelectorFilter.Selector buildSelector( String wildcardPattern,
                                           boolean include, boolean applyToAtomic, boolean applyToCollection )
  {
    return new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( wildcardPattern ),
                                             include, applyToAtomic, applyToCollection);
  }

  @Test
  public void checkNexrad()  throws IOException {
    CrawlableDatasetFilter nexradFilter = buildNexradFilter();

    List<CrawlableDataset> nexradProductList = topCrDs.listDatasets( nexradFilter );
    assertNotNull( nexradProductList);
    assertEquals(productsForNexradOnly.length + productsForBothNexradAndTdwr.length, nexradProductList.size());

    List<CrawlableDataset> nexradStnList = nexradProductList.get( 0).listDatasets( nexradFilter);
    assertNotNull( nexradStnList);
    assertEquals(stnNamesNexrad.length, nexradStnList.size());

    List<CrawlableDataset> dateList = nexradStnList.get( 0).listDatasets( nexradFilter);
    assertNotNull( dateList);
    assertEquals( dates.length, dateList.size());

    List<CrawlableDataset> datasetList = dateList.get( 0).listDatasets( nexradFilter);
    assertNotNull( datasetList);
    assertEquals( times.length, datasetList.size());
  }

  @Test
  public void checkTdwr()  throws IOException {
    CrawlableDatasetFilter tdwrFilter = buildTdwrFilter();

    List<CrawlableDataset> tdwrProductList = topCrDs.listDatasets( tdwrFilter );
    assertNotNull( tdwrProductList);
    assertEquals(productsForTdwrOnly.length + productsForBothNexradAndTdwr.length, tdwrProductList.size());

    List<CrawlableDataset> tdwrStnList = tdwrProductList.get( 0).listDatasets( tdwrFilter);
    assertNotNull( tdwrStnList);
    assertEquals( stnNamesTdwr.length, tdwrStnList.size());

    List<CrawlableDataset> dateList = tdwrStnList.get( 0).listDatasets( tdwrFilter);
    assertNotNull( dateList);
    assertEquals( dates.length, dateList.size());

    List<CrawlableDataset> datasetList = dateList.get( 0).listDatasets( tdwrFilter);
    assertNotNull( datasetList);
    assertEquals( times.length, datasetList.size());
  }

//  @Test
//  public void tryCrDsScanWithTdwrFilter() throws IOException
//  {
//    CrawlableDatasetFilter crDsFilterForTdwr = this.buildTdwrFilter();
//    CrawlableDatasetFilter crDsFilterForNexrad = this.buildNexradFilter();
//    CrawlableDatasetFile crDsFile = new CrawlableDatasetFile( this.testDir );
//
//    List<CrawlableDataset> tdwrList = crDsFile.listDatasets( crDsFilterForTdwr );
//    List<CrawlableDataset> nexradList = crDsFile.listDatasets( crDsFilterForNexrad );
//
//    List<CrawlableDataset> tdwrList2 = new ArrayList<CrawlableDataset>();
//    List<CrawlableDataset> nexradList2 = new ArrayList<CrawlableDataset>();
//    List<CrawlableDataset> tdwrList3 = new ArrayList<CrawlableDataset>();
//    List<CrawlableDataset> nexradList3 = new ArrayList<CrawlableDataset>();
//
//    for ( CrawlableDataset crDs : tdwrList) {
//      tdwrList2.addAll( crDs.listDatasets( crDsFilterForTdwr ));
//    }
//    for ( CrawlableDataset crDs : nexradList) {
//      nexradList2.addAll( crDs.listDatasets( crDsFilterForNexrad ) );
//    }
//
//    for ( CrawlableDataset crDs : tdwrList2) {
//      tdwrList3.addAll( crDs.listDatasets( crDsFilterForTdwr ));
//    }
//    for ( CrawlableDataset crDs : nexradList2) {
//      nexradList3.addAll( crDs.listDatasets( crDsFilterForNexrad ));
//    }
//
//    System.out.println( String.format( "Size of TDWR lists:   %d, %d, %d", tdwrList.size(), tdwrList2.size(), tdwrList3.size()));
//    System.out.println( String.format( "Size of NEXRAD lists: %d, %d, %d", nexradList.size(), nexradList2.size(), nexradList3.size() ) );
//  }
//
//  @Test
//  public void assertNexradConfigDsWithComplexFilterActsAsExpected() throws URISyntaxException
//  {
//    URI catURI = new URI( "http://jira_tds-466.complexDsScanFilterProb.test/nexrad/cat.xml" );
//    CrawlableDatasetFilter crDsFilter = this.buildNexradFilter();
//
//    InvDatasetScan dsScan = buildDatasetScanInParentConfigCat( catURI, crDsFilter );
//
//    // Test scan at product directory (top) level.
//    String catRequestUrlFormat = String.format( "testPath/%s/catalog.xml", this.testDir.getName() );
//    InvCatalogImpl cat = dsScan.makeCatalogForDirectory( catRequestUrlFormat, catURI );
//    List<InvDataset> datasets = cat.getDatasets();
//    assertTrue( datasets.size() == 1);
//
//    InvDataset topDs = datasets.get( 0);
//    datasets = topDs.getDatasets();
//    assertTrue( datasets.size() == productListNexradOnly.size() + productListBoth.size() );
//
//    for ( InvDataset productDs : datasets ) {
//      assertTrue( String.format( "Product dataset name [%s] not in productListNexradOnly %s or productListBoth %s.",
//                                 productDs.getName(), productListNexradOnly.toString(), productListBoth.toString()),
//                  productListNexradOnly.contains( productDs.getName())
//                  || productListBoth.contains( productDs.getName()) );
//    }
//
//    // Test scan at station directory (second) level.
//    catRequestUrlFormat = String.format( "testPath/%s/%s/catalog.xml",
//                                         this.testDir.getName(),
//                                         productListNexradOnly.get(0) );
//    cat = dsScan.makeCatalogForDirectory( catRequestUrlFormat, catURI );
//    datasets = cat.getDatasets();
//    assertTrue( datasets.size() == 1);
//
//    topDs = datasets.get( 0);
//    datasets = topDs.getDatasets();
//    assertTrue( datasets.size() == stnListNexrad.size() );
//
//    for( InvDataset stnDs : datasets ) {
//      assertTrue( String.format( "Product dataset name [%s] not in stnListNexrad %s.",
//                                 stnDs.getName(), stnListNexrad.toString() ),
//                  stnListNexrad.contains( stnDs.getName()));
//    }
//
//    // Test scan at data file directory (third) level.
//    catRequestUrlFormat = String.format( "testPath/%s/%s/%s/catalog.xml",
//                                         this.testDir.getName(),
//                                         productListNexradOnly.get(0),
//                                         stnListNexrad.get( 5) );
//    cat = dsScan.makeCatalogForDirectory( catRequestUrlFormat, catURI );
//    datasets = cat.getDatasets();
//    assertTrue( datasets.size() == 1);
//
//    topDs = datasets.get( 0);
//    datasets = topDs.getDatasets();
//    assertTrue( datasets.size() == dates.size() );
//    for ( InvDataset fileDs : datasets)
//    {
//      String fileNameRegex = "Level3_.*\\.nids";
//      assertTrue( String.format( "File dataset name [%s] doesn't match \"%s\" regExp.", fileDs.getName(), fileNameRegex),
//                  fileDs.getName().matches( fileNameRegex ));
//    }
//
//    // Test scan at data file directory (third) level.
//    catRequestUrlFormat = String.format( "testPath/%s/%s/%s/catalog.xml",
//                                         this.testDir.getName(),
//                                         productListNexradOnly.get(0),
//                                         stnListNexrad.get( 5) );
//    cat = dsScan.makeCatalogForDirectory( catRequestUrlFormat, catURI );
//    datasets = cat.getDatasets();
//    assertTrue( datasets.size() == 1);
//
//    topDs = datasets.get( 0);
//    datasets = topDs.getDatasets();
//    assertTrue( datasets.size() ==  times.size() );
//    for ( InvDataset fileDs : datasets)
//    {
//      String fileNameRegex = "Level3_.*\\.nids";
//      assertTrue( String.format( "File dataset name [%s] doesn't match \"%s\" regExp.", fileDs.getName(), fileNameRegex),
//                  fileDs.getName().matches( fileNameRegex ));
//    }
//
//    cat.getName();
//
//  }
//
//  @Test
//  public void assertTdwrConfigDsWithComplexFilterActsAsExpected() throws URISyntaxException
//  {
//    URI catURI = new URI( "http://jira_tds-466.complexDsScanFilterProb.test/tdwr/cat.xml" );
//    CrawlableDatasetFilter crDsFilter = this.buildTdwrFilter();
//
//    InvDatasetScan dsScan = buildDatasetScanInParentConfigCat( catURI, crDsFilter );
//
//    // Test scan at product directory (top) level.
//    String catRequestUrlFormat = String.format( "testPath/%s/catalog.xml", this.testDir.getName() );
//    InvCatalogImpl cat = dsScan.makeCatalogForDirectory( catRequestUrlFormat, catURI );
//    List<InvDataset> datasets = cat.getDatasets();
//    assertTrue( datasets.size() == 1);
//
//    InvDataset topDs = datasets.get( 0);
//    datasets = topDs.getDatasets();
//    assertTrue( datasets.size() == productListTdwrOnly.size() + productListBoth.size() );
//
//    for ( InvDataset productDs : datasets ) {
//      assertTrue( String.format( "Product dataset name [%s] not in productListTdwrOnly %s or productListBoth %s.",
//                                 productDs.getName(), productListTdwrOnly.toString(), productListBoth.toString()),
//                  productListTdwrOnly.contains( productDs.getName())
//                  || productListBoth.contains( productDs.getName()) );
//    }
//
//    // Test scan at station directory (second) level.
//    catRequestUrlFormat = String.format( "testPath/%s/%s/catalog.xml",
//                                         this.testDir.getName(),
//                                         productListTdwrOnly.get(0) );
//    cat = dsScan.makeCatalogForDirectory( catRequestUrlFormat, catURI );
//    datasets = cat.getDatasets();
//    assertTrue( datasets.size() == 1);
//
//    topDs = datasets.get( 0);
//    datasets = topDs.getDatasets();
//    assertTrue( datasets.size() == stnListTdwr.size() );
//
//    for( InvDataset stnDs : datasets ) {
//      assertTrue( String.format( "Product dataset name [%s] not in stnListTdwr %s.",
//                                 stnDs.getName(), stnListTdwr.toString() ),
//                  stnListTdwr.contains( stnDs.getName()));
//    }
//
//    // Test scan at data file directory (third) level.
//    catRequestUrlFormat = String.format( "testPath/%s/%s/%s/catalog.xml",
//                                         this.testDir.getName(),
//                                         productListTdwrOnly.get(0),
//                                         stnListTdwr.get( 2) );
//    cat = dsScan.makeCatalogForDirectory( catRequestUrlFormat, catURI );
//    datasets = cat.getDatasets();
//    assertTrue( datasets.size() == 1);
//
//    topDs = datasets.get( 0);
//    datasets = topDs.getDatasets();
//    assertTrue( datasets.size() == dates.size() * times.size() );
//    for ( InvDataset fileDs : datasets)
//    {
//      String fileNameRegex = "Level3_.*\\.nids";
//      assertTrue( String.format( "File dataset name [%s] doesn't match \"%s\" regExp.", fileDs.getName(), fileNameRegex),
//                  fileDs.getName().matches( fileNameRegex ));
//    }
//
//    cat.getName();
//
//  }
//
//  private InvDatasetScan buildDatasetScanInParentConfigCat( URI catURI, CrawlableDatasetFilter crDsFilter )
//  {
//    InvCatalogImpl configCat = new InvCatalogImpl( "testCat", "1.0.2", catURI );
//    configCat.addService( new InvService( "odap", "OPENDAP", "/thredds/dodsC/", null, null ) );
//    InvDatasetImpl topConfigDs = new InvDatasetImpl(null, "testDs", null, "odap", null );
//    configCat.addDataset( topConfigDs);
//
//    InvDatasetScan dsScan = new InvDatasetScan( null, null, "test", "testPath/" + this.testDir.getName(),
//                                                this.testDir.getAbsolutePath(),
//                                                crDsFilter, true, null, true, null, null, null);
//    dsScan.setServiceName( "odap" );
//    topConfigDs.addDataset( dsScan );
//    configCat.finish();
//    return dsScan;
//  }
}