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

    CrawlableDataset crDs = checkCrDs( path, path);
    if ( crDs.exists() )
    {
      assertTrue( "Unexpected exist()==true for CrDs(\"\").",
                  false);
    }
  }

  public void testRootPath()
  {
    String path = "/";
    String name = "";

    CrawlableDataset crDs = checkCrDs( path, name );
    if ( ! crDs.exists() )
    {
      assertTrue( "CrDs(\"/\") doesn't exist.",
                  false );
    }

  }

  public void testDotPath()
  {
    String path = ".";
    String name = ".";
    List results = new ArrayList();
    results.add( "build.xml" );

    checkCrDsChildren( path, name, results );
  }

  public void testSrcMainJavaPath()
  {
    String path = "src/main/java";
    String name = "java";
    List results = new ArrayList();
    results.add( "dods" );
    results.add( "thredds" );
    results.add( "ucar" );

    checkCrDsChildren( path, name, results );
  }

  public void testSrcMainJavaDotDotPath()
  {
    String path = "src/main/java/..";
    String name = "..";
    List results = new ArrayList();
    results.add( "java" );
    results.add( "resources" );

    checkCrDsChildren( path, name, results );
  }

  public void testSrcMainJavaDotDotSlashDotDotPath()
  {
    String path = "src/main/java/../..";
    String name = "..";
    List results = new ArrayList();
    results.add( "main" );
    results.add( "test" );
    results.add( "timing" );

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

  private void checkCrDsChildren( String path, String name, List expectedChildrenNames )
  {
    CrawlableDataset crDs = checkCrDs( path, name );
    if ( ! crDs.exists() )
    {
      assertTrue( "CrDs(\"" + path + "\" doesn't exist.",
                  false);
      return;
    }

    if ( ! crDs.isCollection() )
    {
      assertTrue( "CrDs(\"" + path + "\" is not a collection.",
                  false );
      return;
    }

    // Test the list of datasets.
    List list = null;
    try
    {
      list = crDs.listDatasets();
    }
    catch ( IOException e )
    {
      assertTrue( "IOException getting children datasets <" + crDs.getName() + ">: " + e.getMessage(),
                  false );
      return;
    }

    assertTrue( "Number of datasets <" + list.size() + "> not as expected <" + expectedChildrenNames.size() + ">.",
                list.size() >= expectedChildrenNames.size() );

    List crDsNameList = new ArrayList();
    for ( Iterator it = list.iterator(); it.hasNext(); )
    {
      CrawlableDataset curCd = (CrawlableDataset) it.next();
      crDsNameList.add( curCd.getName() );
    }

    for ( Iterator it = expectedChildrenNames.iterator(); it.hasNext(); )
    {
      String curName = (String) it.next();
      assertTrue( "Result path <" + curName + "> not as expected <" + expectedChildrenNames + ">.",
                  crDsNameList.contains( curName ) );
    }
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
