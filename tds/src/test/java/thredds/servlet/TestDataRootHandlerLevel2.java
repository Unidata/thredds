package thredds.servlet;

import junit.framework.TestCase;
import thredds.TestAll;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.filter.MultiSelectorFilter;
import thredds.crawlabledataset.filter.WildcardMatchOnNameFilter;
import thredds.catalog.*;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;

import ucar.unidata.util.TestUtil;

/**
 * _more_
 *
 * @author edavis
 * @since Mar 21, 2007 1:07:18 PM
 */
public class TestDataRootHandlerLevel2 extends TestCase
{
  private String level2DirPath = TestAll.upcShareThreddsDataDir + "TestDataRootHandlerLevel2";

  public TestDataRootHandlerLevel2( String name )
  {
    super( name );
  }

  /**
   * Test
   */
  public void testTwoCatRefsToSameConfigCatViaSymbolicLink()
  {
    File testDir = new File( new File( level2DirPath), "testTwoCatRefsBlahBlah");
    String fullCanonicalTestDirPath = null;
    try
    {
      fullCanonicalTestDirPath = testDir.getCanonicalPath() + "/";
    }
    catch ( IOException e )
    {
      fail( "I/O error getting canonical path for test directory <" + testDir.getPath() + ">: " + e.getMessage() );
      return;
    }
    fullCanonicalTestDirPath = fullCanonicalTestDirPath.replace( '\\', '/' );

    String testConfigCatName = "catalog.xml";
    String regularCatName = "regularCat.xml";
    String symLinkToRegCatName = "symLinkToRegCat.xml";
    File testConfigCatFile = new File( testDir, testConfigCatName );
    File regularCatFile = new File( testDir, regularCatName );
    File symLinkToRegCatFile = new File( testDir, symLinkToRegCatName );

    // Make sure symLinkCat is actually a symbolic link.
    File file1, file2;
    assertFalse( "Regular cat <" + regularCatFile.getPath() + "> same as symbolic link cat <" + symLinkToRegCatFile.getPath() + ">.",
                 regularCatFile.equals( symLinkToRegCatFile ));

    try
    {
      file1 = regularCatFile.getCanonicalFile();
      file2 = symLinkToRegCatFile.getCanonicalFile();
    }
    catch ( IOException e )
    {
      fail( "I/O error getting canonical files <" + regularCatFile.getPath() + " or " + symLinkToRegCatFile.getPath() + ">: " + e.getMessage());
      return;
    }
//    assertTrue( "Symbolic link catalog <" + symLinkToRegCatFile.getPath() + "> is not a symbolic link.",
//                file1.equals( file2 ));

    // Call DataRootHandler.init() to point to contentPath directory
    DataRootHandler.init( fullCanonicalTestDirPath, "/thredds" );
    DataRootHandler drh = DataRootHandler.getInstance();

    // Call DataRootHandler.initCatalog() on the config catalog
    try
    {
      drh.reinit();
      drh.initCatalog( testConfigCatName );
    }
    catch ( FileNotFoundException e )
    {
      fail( e.getMessage() );
      return;
    }
    catch ( IOException e )
    {
      fail( "I/O error while initializing catalog <" + testConfigCatName + ">: " + e.getMessage() );
      return;
    }
    catch ( IllegalArgumentException e )
    {
      fail( "IllegalArgumentException while initializing catalog <" + testConfigCatName + ">: " + e.getMessage() );
      return;
    }

    // Make sure that DRH has the main, regular, and symLink catalogs.
    StringBuffer checkMsg = new StringBuffer();
    InvCatalogImpl testConfigCat = (InvCatalogImpl) drh.getCatalog( testConfigCatName, testConfigCatFile.toURI() );
    InvCatalogImpl regularCat = (InvCatalogImpl) drh.getCatalog( regularCatName, regularCatFile.toURI() );
    InvCatalogImpl symLinkToRegCat = (InvCatalogImpl) drh.getCatalog( symLinkToRegCatName, symLinkToRegCatFile.toURI());

    if ( testConfigCat == null )
    {
      fail( "Main test catalog <" + testConfigCatName + "> not found by DataRootHandler.");
      return;
    }
    assertTrue( "Main test catalog <" + testConfigCatName + "> not valid: " + checkMsg.toString(),
                testConfigCat.check( checkMsg));
    if ( checkMsg.length() > 0 )
    {
      System.out.println( "Main test catalog <" + testConfigCatName + "> valid but had message: " + checkMsg.toString() );
      checkMsg = new StringBuffer();
    }

    if ( regularCat == null )
    {
      fail( "Regular catalog <" + regularCatName + "> not found by DataRootHandler.");
      return;
    }
    assertTrue( "Regular catalog <" + regularCatName + "> not valid: " + checkMsg.toString(),
                regularCat.check( checkMsg));
    if ( checkMsg.length() > 0 )
    {
      System.out.println( "Regular catalog <" + regularCatName + "> valid but had message: " + checkMsg.toString() );
      checkMsg = new StringBuffer();
    }

    if ( symLinkToRegCat == null )
    {
      fail( "Symbolic link catalog <" + symLinkToRegCatName + "> not found by DataRootHandler.");
      return;
    }
    assertTrue( "Symbolic link catalog <" + symLinkToRegCatName + "> not valid: " + checkMsg.toString(),
                symLinkToRegCat.check( checkMsg));
    if ( checkMsg.length() > 0 )
    {
      System.out.println( "Symbolic link catalog <" + symLinkToRegCatName + "> valid but had message: " + checkMsg.toString() );
      //checkMsg = new StringBuffer();
    }
  }

}
