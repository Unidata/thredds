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
package thredds.cataloggen.config;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import thredds.catalog.*;

import java.io.*;
import java.util.List;

/**
 * A description
 * <p/>
 * User: edavis
 * Date: May 19, 2004
 * Time: 3:44:33 PM
 */
public class TestCatGenConfigMetadataFactory
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestCatGenConfigMetadataFactory.class);

  private String configResourcePath = "/thredds/cataloggen/config";
  private String catGenConf_1_0_ResourceName = "test1CatGenConfig1.0.xml";

  private String catName = "THREDDS CatalogGen test config file";
  private String dsSourceName = "model data source";
  private String resultServiceName = "mlode";
  private String resultServiceType = "DODS";
  private String resultServiceBase = "http://localhost:8080/thredds/cataloggen/";
  private String resultServiceAccessPointHeader = "src/test/data/thredds/cataloggen/";
  private String dsFilterName="Accept netCDF files only";
  private String dsFilterType="RegExp";
  private String dsFilterMatchPattern="/[0-9][^/]*_eta_211\\.nc$";
  private String dsNamerName="NCEP Eta 80km CONUS model data";
  private String dsNamerType="RegExp";
  private String dsNamerAddLevel="false";
  private String dsNamerMatchPattern="([0-9][0-9][0-9][0-9])([0-9][0-9])([0-9][0-9])([0-9][0-9])_eta_211.nc$";
  private String dsNamerSubstitutePattern="NCEP Eta 80km CONUS $1-$2-$3 $4:00:00 GMT";

  private InvCatalogFactory factory = null;

  @Before
  public void setUp()
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

  @Test
  public void testParse_1_0() throws IOException
  {
    String catalogFileName = resultServiceAccessPointHeader + "config/" + catGenConf_1_0_ResourceName;
    File catalogFile = new File( catalogFileName );
    InputStream is = new FileInputStream( catalogFile );

    InvCatalog catalog = this.factory.readXML( is, catalogFile.toURI() );

    is.close();

    this.parsedCatalogTests( catalog);
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
    assertEquals( resultServiceAccessPointHeader, resultService.getAccessPointHeader());

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