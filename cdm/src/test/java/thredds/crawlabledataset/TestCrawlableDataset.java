// $Id: TestCrawlableDataset.java 61 2006-07-12 21:36:00Z edavis $
package thredds.crawlabledataset;

import junit.framework.TestCase;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.IOException;

/**
 * A description
 *
 * @author edavis
 * @since 20 January 2006 13:22:59 -0600
 */
public class TestCrawlableDataset extends TestCase
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( TestCrawlableDatasetAlias.class );


  public TestCrawlableDataset( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testEmptyPath()
  {
    String path = "";

    checkCrDsFail( path );
  }

  public void testRootPath()
  {
    String path = "/";
    String name = "";

    checkCrDs( path, name );
  }

  public void testDotPath()
  {
    String path = ".";
    String name = ".";
    List results = new ArrayList();
    results.add( "build.xml" );

    checkCrDsChildren( path, name, results );
  }

  public void testDotDotPath()
  {
    String path = "..";
    String name = "..";
    List results = new ArrayList();
    results.add( "thredds" );
    results.add( "netcdf-java-2.2" );

    checkCrDsChildren( path, name, results );
  }

  public void testDotDotSlashDotDotPath()
  {
    String path = "../..";
    String name = "..";
    List results = new ArrayList();
    results.add( "devel" );
    results.add( "idv" );

    checkCrDsChildren( path, name, results );
  }

  private CrawlableDataset checkCrDs( String path, String name )
  {
    // Create CrawlableDataset.
    CrawlableDataset cd = null;
    try
    {
      cd = CrawlableDatasetFactory.createCrawlableDataset( path, null, null );
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to create CrawlableDataset <" + path + ">: " + e.getMessage(),
                  false );
      return null;
    }

    assertTrue( "CD path <" + cd.getPath() + "> not as expected <" + path + ">.",
                cd.getPath().equals( path ) );
    assertTrue( "CD name <" + cd.getName() + "> not as expected <" + name + ">.",
                cd.getName().equals( name ) );

    return cd;
  }

  private void checkCrDsChildren( String path, String name, List children )
  {
    CrawlableDataset cd = checkCrDs( path, name );

    // Test the list of datasets.
    List list = null;
    try
    {
      list = cd.listDatasets();
    }
    catch ( IOException e )
    {
      assertTrue( "IOException getting children datasets <" + cd.getName() + ">: " + e.getMessage(),
                  false );
      return;
    }

    assertTrue( "Number of datasets <" + list.size() + "> not as expected < >=" + children.size() + ">.",
                list.size() >= children.size() );

    List crDsNameList = new ArrayList();
    for ( Iterator it = list.iterator(); it.hasNext(); )
    {
      CrawlableDataset curCd = (CrawlableDataset) it.next();
      crDsNameList.add( curCd.getName() );
    }

    for ( Iterator it = children.iterator(); it.hasNext(); )
    {
      String curName = (String) it.next();
      assertTrue( "Result path <" + curName + "> not as expected <" + children + ">.",
                  crDsNameList.contains( curName ) );
    }
  }

  private void checkCrDsFail( String path )
  {
    // Create CrawlableDataset.
    try
    {
      CrawlableDatasetFactory.createCrawlableDataset( path, null, null );
    }
    catch ( Exception e )
    {
      // An exception is expected since File("").exists() is false.
      System.out.println( "Expected exception: " + e.getMessage() );
      return;
    }
    assertTrue( "Unexpected success creating CrawlableDataset <" + path + ">.",
                false );
  }

//  public void testUncPaths()
//  {
//    //String dir = "\\\\Zero\\winxx";
//    //String dir = "//Zero/winxx";
//    String dir = "test///data///thredds";
//    File f = new File( dir );
//    URL furl = null;
//    boolean urlok = true;
//    try
//    {
//      furl = f.toURL();
//    }
//    catch ( MalformedURLException e )
//    {
//      System.out.println( "  Malformed URL <"+f.toString()+">" );
//      urlok = false;
//    }
//    URI furi = f.toURI();
//    System.out.println( "Dir=" + dir );
//    System.out.println( "File=" + f + (f.isDirectory() ? " - isDir" : " - notDir"));
//    if ( urlok)
//    {
//      System.out.println( "FileURL=" + furl.toString() + (new File( furl.toString()).isDirectory() ? " - isDir" : " - notDir" ) );
//    }
//    System.out.println( "FileURI=" + furi.toString() + (new File( furi).isDirectory() ? " - isDir" : " - notDir" ));
//  }
}

/*
 * $Log: TestCrawlableDataset.java,v $
 * Revision 1.1  2006/01/23 18:51:06  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.4  2005/12/30 00:18:56  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.3  2005/12/16 23:19:39  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.2  2005/11/18 23:51:06  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.1  2005/11/15 18:40:51  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.2  2005/08/22 17:40:24  edavis
 * Another round on CrawlableDataset: make CrawlableDatasetAlias a subclass
 * of CrawlableDataset; start generating catalogs (still not using in
 * InvDatasetScan or CatalogGen, yet).
 *
 * Revision 1.1  2005/06/24 22:08:33  edavis
 * Second stab at the CrawlableDataset interface.
 *
 *
 */