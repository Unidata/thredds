/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
// $Id: TestCatGenConfigMetadataFactory.java 61 2006-07-12 21:36:00Z edavis $
package thredds.cataloggen.config;

import junit.framework.TestCase;
import thredds.catalog.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * A description
 * <p/>
 * User: edavis
 * Date: May 19, 2004
 * Time: 3:44:33 PM
 */
public class TestCatGenConfigMetadataFactory extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestCatGenConfigMetadataFactory.class);

  private String configResourcePath = "/thredds/cataloggen/config";
  private String catGenConf_0_6_ResourceName = "test1CatGenConfig0.6.xml";
  private String catGenConf_1_0_ResourceName = "test1CatGenConfig1.0.xml";

  private String catName = "THREDDS CatalogGen test config file";
  private String dsSourceName = "model data source";
  private String resultServiceName = "mlode";
  private String resultServiceType = "DODS";
  private String resultServiceBase = "http://localhost:8080/thredds/cataloggen/";
  //private String resultServiceAccessPointHeader = "./content/thredds/cataloggen/";
  private String resultServiceAccessPointHeader = "./test/data/thredds/cataloggen/";
  private String dsFilterName="Accept netCDF files only";
  private String dsFilterType="RegExp";
  private String dsFilterMatchPattern="/[0-9][^/]*_eta_211\\.nc$";
  private String dsNamerName="NCEP Eta 80km CONUS model data";
  private String dsNamerType="RegExp";
  private String dsNamerAddLevel="false";
  private String dsNamerMatchPattern="([0-9][0-9][0-9][0-9])([0-9][0-9])([0-9][0-9])([0-9][0-9])_eta_211.nc$";
  private String dsNamerSubstitutePattern="NCEP Eta 80km CONUS $1-$2-$3 $4:00:00 GMT";

  private InvCatalogFactory factory = null;

  public TestCatGenConfigMetadataFactory( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    log.debug( "setUp(): starting." );

    // Setup catalog factory to read documents with CatalogGenConfig metadata.
    this.factory = new InvCatalogFactory( "default", true );
    CatGenConfigMetadataFactory catGenConfMdataFactory = new CatGenConfigMetadataFactory();
    this.factory.registerMetadataConverter( MetadataType.CATALOG_GEN_CONFIG.toString(),
                                            catGenConfMdataFactory );
    this.factory.registerMetadataConverter( CatalogGenConfig.CATALOG_GEN_CONFIG_NAMESPACE_URI_0_5,
                                            catGenConfMdataFactory );
  }

  /**
   * Test parsing of InvCatalog 1.0 catalog.
   */
  public void testParse_1_0()
  {
    String tmpMsg = "Test InvCatalog 1.0 parsing of metadata content.";
    log.debug( "testParse(): " + tmpMsg );

    // Open a CatalogGenConfig document resource as an InputStream.
    String resourceName = configResourcePath + "/" + catGenConf_1_0_ResourceName;
    String resourceURIName = "http://TestCatGenConfigMetadataFactory.resource/" + resourceName;
    URI resourceURI = null;
    try
    {
      resourceURI = new URI( resourceURIName );
    }
    catch ( URISyntaxException e )
    {
      tmpMsg = "URISyntaxException thrown creating URI w/ " + resourceURIName + ": " + e.getMessage();
      log.debug( "testParse(): " + tmpMsg, e);
      assertTrue( tmpMsg, false);
    }
    InputStream is = this.getClass().getResourceAsStream( resourceName );

    // Parse the CatalogGenConfig document.
    InvCatalog catalog = this.factory.readXML( is, resourceURI );

    // Close the InputStream.
    try
    {
      is.close();
    }
    catch ( IOException e )
    {
      tmpMsg = "IOException thrown while closing input stream to resource <" + resourceName + ">: " + e.getMessage();
      log.debug( "testParse(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }

    this.parsedCatalogTests( catalog);
  }

  /**
   * Test parsing of InvCatalog 0.6 catalog.
   */
  public void testParse_0_6()
  {
    String tmpMsg = "Test InvCatalog 0.6 parsing of metadata content.";
    log.debug( "testParse(): " + tmpMsg );

    // Open a CatalogGenConfig document resource as an InputStream.
    String resourceName = configResourcePath + "/" + catGenConf_0_6_ResourceName;
    String resourceURIName = "http://TestCatGenConfigMetadataFactory.resource/" + resourceName;
    URI resourceURI = null;
    try
    {
      resourceURI = new URI( resourceURIName );
    }
    catch ( URISyntaxException e )
    {
      tmpMsg = "URISyntaxException thrown creating URI w/ " + resourceURIName + ": " + e.getMessage();
      log.debug( "testParse(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    InputStream is = this.getClass().getResourceAsStream( resourceName );

    // Parse the CatalogGenConfig document.
    InvCatalog catalog = this.factory.readXML( is, resourceURI );

    // Close the InputStream.
    try
    {
      is.close();
    }
    catch ( IOException e )
    {
      tmpMsg = "IOException thrown while closing input stream to resource <" + resourceName + ">: " + e.getMessage();
      log.debug( "testParse(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }

    this.parsedCatalogTests( catalog );
  }

  private void parsedCatalogTests( InvCatalog catalog)
  {
    // Check catalog name.
    assertTrue( "Catalog name <" + catalog.getName() + "> != expected name <" + catName + ">.",
                catalog.getName().equals( catName) );
    // Check only one top level dataset.
    List list = catalog.getDatasets();
    assertTrue( "Catalog does not contain one and only one dataset (" + list.size() + ").",
                list.size() == 1 );
    InvDataset topDs = (InvDataset) list.get( 0);
    assertTrue( "Top level dataset name <" + topDs.getName() + "> != expected name <" + catName + ">.",
                topDs.getName().equals( catName));

    list = topDs.getDatasets();
    assertTrue( "Top level dataset does not contain two and only two datasets (" + list.size() + ").",
                list.size() == 2 );

    List list2 = null;
    InvDataset curDs = (InvDataset) list.get( 0);

    // Check for the CatalogGenConfig metadata record.
    ThreddsMetadata curDsTm = ( (InvDatasetImpl) curDs ).getLocalMetadata();
    list2 = curDsTm.getMetadata();
    assertTrue( "Top level dataset does not contain one and only one InvMetadata.",
                list2.size() == 1 );
    InvMetadata mdata = (InvMetadata) list2.get( 0 );
    assertTrue( "InvMetadata is not of type CATALOG_GEN_CONFIG.",
                mdata.getMetadataType().equals( MetadataType.CATALOG_GEN_CONFIG.toString() ) );

    // Check that only one CatalogGenConfig element contained in the metadata element.
    list2 = (List) mdata.getContentObject();
    assertTrue( "Metadata record does not contain one and only one CatalogGenConfig.",
                list2.size() == 1 );
    CatalogGenConfig catGenConfig = (CatalogGenConfig) list2.get( 0 );
    assertTrue( "CatalogGenConfig is not of type CATALOG.",
                catGenConfig.getType().equals( CatalogGenConfig.Type.CATALOG ) );

    // Check that CatalogGenConfig contains the expected DatasetSource.
    DatasetSource dsSource = catGenConfig.getDatasetSource();
    assertTrue( "DatasetSource name <" + dsSource.getName() + "> != expected name <" + dsSourceName + ">.",
                dsSource.getName().equals( dsSourceName ) );

    // Check the type and structure of the DatasetSource

    // Check that ResultService has expected values for name, serviceType, base, and accessPointHeader.
    ResultService resultService = (ResultService) dsSource.getResultService();
    assertTrue( "ResultService name <" + resultService.getName() + "> != expected name <" + resultServiceName + ">.",
                resultService.getName().equals( resultServiceName ) );
    assertTrue( "ResultService type <" + resultService.getServiceType().toString() + "> != expected type <" + resultServiceType + ">.",
                resultService.getServiceType().toString().equals( resultServiceType ) );
    assertTrue( "ResultService base URL <" + resultService.getBase() + "> != expected base URL <" + resultServiceBase + ">.",
                resultService.getBase().equals( resultServiceBase ) );
    assertTrue( "ResultService access point header <" + resultService.getAccessPointHeader() + "> != expected access point header <" + resultServiceAccessPointHeader + ">.",
                resultService.getAccessPointHeader().equals( resultServiceAccessPointHeader ) );

    // Check that only one DatasetFilter in DatasetSource and that it has
    // the expected name, serviceType, and matchPattern.
    list2 = dsSource.getDatasetFilterList();
    assertTrue( "DatasetSource does not contain one and only one DatasetFilter.",
                list2.size() == 1 );
    DatasetFilter dsFilter = (DatasetFilter) list2.get( 0 );
    assertTrue( "DatasetFilter name <" + dsFilter.getName() + "> != expected name <" + dsFilterName + ">.",
                dsFilter.getName().equals( dsFilterName ) );
    assertTrue( "DatasetFilter type <" + dsFilter.getType().toString() + "> != expected type <" + dsFilterType + ">.",
                dsFilter.getType().toString().equals( dsFilterType ) );
    assertTrue( "DatasetFilter match pattern <" + dsFilter.getMatchPattern() + "> != expected match pattern <" + dsFilterMatchPattern + ">.",
                dsFilter.getMatchPattern().equals( dsFilterMatchPattern ) );

    // Check that only one DatasetNamer in DatasetSource and that it has
    // the expected name, serviceType, addLevel, matchPattern, and substitutePattern.
    list2 = dsSource.getDatasetNamerList();
    assertTrue( "DatasetSource does not contain one and only one DatasetNamer.",
                list2.size() == 1 );
    DatasetNamer dsNamer = (DatasetNamer) list2.get( 0 );
    assertTrue( "DatasetNamer name <" + dsNamer.getName() + "> != expected name <" + dsNamerName + ">.",
                dsNamer.getName().equals( dsNamerName ) );
    assertTrue( "DatasetNamer type <" + dsNamer.getType().toString() + "> != expected type <" + dsNamerType + ">.",
                dsNamer.getType().toString().equals( dsNamerType ) );
    assertTrue( "DatasetNamer addLevel <" + dsNamer.getAddLevel() + "> != expected addLevel <" + dsNamerAddLevel + ">.",
                Boolean.toString( dsNamer.getAddLevel() ).equals( dsNamerAddLevel ) );
    assertTrue( "DatasetNamer match pattern <" + dsNamer.getMatchPattern() + "> != expected match pattern <" + dsNamerMatchPattern + ">.",
                dsNamer.getMatchPattern().equals( dsNamerMatchPattern ) );
    assertTrue( "DatasetNamer substitute pattern <" + dsNamer.getSubstitutePattern() + "> != expected substitute pattern <" + dsNamerSubstitutePattern + ">.",
                dsNamer.getSubstitutePattern().equals( dsNamerSubstitutePattern ) );

  }


//    // Write the parsed catalog to a string.
//    String catParsedToString = null;
//    try
//    {
//      catParsedToString = fac.writeXML( (InvCatalogImpl) catalog);
//    }
//    catch ( IOException e )
//    {
//      tmpMsg = "IOException thrown writing catalog to string: " + e.getMessage();
//      log.debug( "testParse(): " + tmpMsg, e);
//      assertTrue( tmpMsg, false);
//    }
//
//    // Read the CatalogGenConfig resource as a string.
//    is = this.getClass().getResourceAsStream( resourceName );
//
//    String catString = is.toString();
//
//    // Close the InputStream.
//    try
//    {
//      is.close();
//    }
//    catch ( IOException e )
//    {
//      tmpMsg = "IOException thrown while closing input stream to resource <" + resourceName + ">: " + e.getMessage();
//      log.debug( "testParse(): " + tmpMsg, e );
//      assertTrue( tmpMsg, false );
//    }
//
//    // Compare the two versions of the CatalogGenConfig document.
//    assertTrue( catParsedToString.equals( catString) );

}

/*
 * $Log: TestCatGenConfigMetadataFactory.java,v $
 * Revision 1.4  2006/01/20 02:08:25  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.3  2005/07/14 20:01:26  edavis
 * Make ID generation mandatory for datasetScan generated catalogs.
 * Also, remove log4j from some tests.
 *
 * Revision 1.2  2005/03/30 19:55:09  edavis
 * Continue simplifying build process (build.xml fixes and update tests.
 *
 * Revision 1.1  2005/03/30 05:41:18  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.3  2004/11/30 22:19:25  edavis
 * Clean up some CatalogGen tests and add testing for DatasetSource (without and with filtering on collection datasets).
 *
 * Revision 1.2  2004/08/23 16:45:17  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.1  2004/06/03 20:39:51  edavis
 * Added tests to check that CatGen config files are parsed correctly and
 * expanded catalogs are written correctly.
 *
 */