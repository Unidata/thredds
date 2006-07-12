// $Id$
package thredds;

import junit.framework.TestCase;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;

import thredds.catalog.*;

/**
 * Testing tool for comparing two catalogs. Information about the two catalogs
 * to compare is given in thredds/tds/src/test/data/thredds/testCompareTwoCatalogs.config.xml.
 *
 * @author edavis
 * @since 15 July 2005 15:50:59 -0600
 */
public class TestCompareTwoCatalogs extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestCompareTwoCatalogs.class);

  private String configFilePath = "src/test/data/thredds/testCompareTwoCatalogs.config.xml";

  private int desiredDepth;
  private boolean doAssert;

  private String host1;
  private String host2;

  private String catPath1;
  private String catPath2;

  public TestCompareTwoCatalogs( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    File configFile = new File( configFilePath );
    SAXBuilder builder = new SAXBuilder();
    Document configDoc;
    try
    {
      configDoc = builder.build( configFile );
    }
    catch ( JDOMException e )
    {
      log.error( "setUp(): failed to read config document: " + e.getMessage());
      assertTrue( "Failed to read config doc: " + e.getMessage(),
                  false);
      return;
    }
    catch ( IOException e )
    {
      assertTrue( "Failed to read config doc: " + e.getMessage(),
                  false );
      return;
    }

    Element root = configDoc.getRootElement();
    String configDocRootName = "testCompareTwoCatalogsConfig";
    if ( ! root.getName().equals( configDocRootName ) )
    {
      assertTrue( "Config document root element <" + configDoc.getRootElement() + "> not as expected <" + configDocRootName + ">.",
                  false);
      return;
    }
    Attribute depthAtt = root.getAttribute( "depth");
    if ( depthAtt == null )
      desiredDepth = 0;
    else
    {
      try
      {
        desiredDepth = depthAtt.getIntValue();
      }
      catch ( DataConversionException e )
      {
        assertTrue( "Config document depth <" + depthAtt.getValue() + "> not an integer: " + e.getMessage(),
                    false );
        return;
      }
    }
    Attribute doAssertAtt = root.getAttribute( "doAssert" );
    if ( doAssertAtt == null )
      doAssert = false;
    else
    {
      try
      {
        doAssert = doAssertAtt.getBooleanValue();
      }
      catch ( DataConversionException e )
      {
        assertTrue( "Config document doAssert <" + depthAtt.getValue() + "> not boolean: " + e.getMessage(),
                    false );
        return;
      }
    }

    Element catElem = root.getChild( "firstCatalog");
    host1 = catElem.getAttributeValue( "host");
    catPath1 = catElem.getAttributeValue( "catPath");

    catElem = root.getChild( "secondCatalog" );
    host2 = catElem.getAttributeValue( "host" );
    catPath2 = catElem.getAttributeValue( "catPath" );
  }

  /** Compare the given catalogs */
  public void testCompareGivenCats()
  {
    URI cat1Uri = null;
    URI cat2Uri = null;
    String cat1UriString = "http://" + host1 + "/" + catPath1;
    String cat2UriString = "http://" + host2 + "/" + catPath2;
    try
    {
      cat1Uri = new URI( cat1UriString );
      cat2Uri = new URI( cat2UriString );
    }
    catch ( URISyntaxException e )
    {
      assertTrue( "Invalid URI <" + cat1UriString + "> or <" + cat2UriString + ">: " + e.getMessage(),
                  false );
      return;
    }
    compareTwoCatalogs( cat1Uri, cat2Uri, 0, desiredDepth );
  }

  private void compareTwoCatalogs( URI cat1Uri, URI cat2Uri, int curDepth, int desiredDepth )
  {
    // Stop at desired depth.
    if ( curDepth > desiredDepth ) return;

    // Write comparison header.
    System.out.println( "--------------------\n" +
                        "Comparing two catalogs:\n" +
                        "  catalog1=" + cat1Uri.toString() + "\n" +
                        "  catalog2=" + cat2Uri.toString() + "\n" +
                        "--------------------" );

    boolean doCompare = true;

    // Check that cat1 still on top cat1 host.
    String cat1Host = "http://" + host1;
    if ( ! cat1Uri.toString().startsWith( cat1Host ) )
    {
      System.out.println( "Catalog1 <" + cat1Uri.toString() + "> not on parent host <" + cat1Host + ">." );
      doCompare = false;
    }

    // Check that cat2 still on top cat2 host.
    String cat2Host = "http://" + host2;
    if ( ! cat2Uri.toString().startsWith( cat2Host ) )
    {
      System.out.println( "Catalog2 <" + cat2Uri.toString() + "> not on parent host <" + cat2Host + ">." );
      doCompare = false;
    }

    if ( doCompare )
    {
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      InvCatalogImpl catalog1 = fac.readXML( cat1Uri );
      InvCatalogImpl catalog2 = fac.readXML( cat2Uri );

      try
      {
        System.out.println( fac.writeXML( catalog1 ) );
        System.out.println( fac.writeXML( catalog2 ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }

      // @todo Change to warning message instead of assert
      if ( doAssert )
      {
        assertTrue( "CATALOG DO NOT MATCH:\n" +
                    "  <catalog1=" + cat1Uri.toString() + "> and\n" +
                    "  <catalog2=" + cat2Uri.toString() + ">.",
                    catalog1.equals( catalog2 ) );
      }
      else
      {
        if ( ! catalog1.equals( catalog2 ) )
        {
          System.out.println(
                  "CATALOG DO NOT MATCH:\n" +
                  "  <catalog1=" + cat1Uri.toString() + "> and\n" +
                  "  <catalog2=" + cat2Uri.toString() + ">." );
        }
        if ( cat1Uri.toString()
                .equals( "http://motherlode.ucar.edu:8080/thredds/idd/ruc_model.xml" ) )
        {
          String junk = "fred";
        }
      }

      List catRefList1 = new ArrayList();
      List catRefList2 = new ArrayList();
      findAllCatRefsInTwoCatalogs( catalog1, catalog2, catRefList1, catRefList2 );

      for ( int i = 0; i < catRefList1.size(); i++ )
      {
        InvCatalogRef catRef1 = (InvCatalogRef) catRefList1.get( i);
        InvCatalogRef catRef2 = (InvCatalogRef) catRefList2.get( i);

        compareTwoCatalogs( catRef1.getURI(), catRef2.getURI(), curDepth + 1, desiredDepth );
      }
    }
  }

  private void findAllCatRefsInTwoCatalogs( InvCatalogImpl catalog1,
                                            InvCatalogImpl catalog2,
                                            List catRefList1,
                                            List catRefList2 )
  {
    List dsList1 = catalog1.getDatasets();
    List dsList2 = catalog2.getDatasets();
    findAllCatRefsInTwoDsLists( dsList1, dsList2, catRefList1, catRefList2 );
  }

  private void findAllCatRefsInTwoDatasets( InvDatasetImpl dataset1,
                                            InvDatasetImpl dataset2,
                                            List catRefList1,
                                            List catRefList2 )
  {
    if ( ! ( dataset1 instanceof InvCatalogRef ) &&
         ! ( dataset2 instanceof InvCatalogRef) )
    {
      List dsList1 = dataset1.getDatasets();
      List dsList2 = dataset2.getDatasets();
      findAllCatRefsInTwoDsLists( dsList1, dsList2, catRefList1, catRefList2 );
    }
  }

  private void findAllCatRefsInTwoDsLists( List dsList1, List dsList2, List catRefList1, List catRefList2 )
  {
    for ( int i = 0; i < dsList1.size(); i++ )
    {
      InvDatasetImpl curDs1 = (InvDatasetImpl) dsList1.get( i);
      InvDatasetImpl curDs2 = (InvDatasetImpl) dsList2.get( i);
      if ( curDs1 instanceof InvCatalogRef &&
           ! ( curDs1 instanceof InvDatasetScan ) )
      {
        if ( curDs2 instanceof InvCatalogRef &&
             ! ( curDs2 instanceof InvDatasetScan ))
        {
          catRefList1.add( curDs1 );
          catRefList2.add( curDs2 );
        }
      }
      else if ( curDs1.hasNestedDatasets() && curDs2.hasNestedDatasets() )
      {
        findAllCatRefsInTwoDatasets( curDs1, curDs2, catRefList1, catRefList2 );
      }
    }
  }
}

/*
 * $Log: TestCompareTwoCatalogs.java,v $
 * Revision 1.5  2006/03/20 19:26:18  edavis
 * Clean up test.
 *
 * Revision 1.4  2006/01/23 18:51:07  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.3  2006/01/20 02:08:26  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.2  2006/01/17 22:56:47  edavis
 * Remove use of CrawlableDatasetAlias for now until figure out how to deal with
 * things like ".scour*" being a regular file. Also, update some documentation.
 *
 * Revision 1.1  2005/12/30 23:05:31  edavis
 * Add test to compare two catalogs and follow (to a given depth) any catalogRefs and comparing referenced catalogs.
 *
 *
 */