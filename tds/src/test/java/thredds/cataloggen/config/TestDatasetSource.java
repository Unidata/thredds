// $Id: TestDatasetSource.java,v 1.8 2006/01/20 02:08:25 caron Exp $

/*
 * Copyright 2002 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.cataloggen.config;

import junit.framework.*;
import thredds.catalog.*;
import thredds.cataloggen.TestCatalogGen;
import thredds.cataloggen.DatasetEnhancer1;

import java.io.*;

/**
 *
 */
public class TestDatasetSource extends TestCase
{
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestDatasetSource.class);

  private boolean debugShowCatalogs_expandFlat = true;

  private String configResourcePath = "/thredds/cataloggen/config";
  private String test1DatasetSourceResultCatalog_1_0_ResourceName = "test1ResultCatalog1.0.dss.xml";
  private String testDsfDirFilter1_ResultCatalog_1_0_ResourceName = "testDsfDirFilter1.ResultCatalog1.0.xml";
  private String testDatasetSource_allCatalogRef_ResultCatalog_ResourceName = "testDatasetSource.allCatalogRef.result.xml";
  private String testDsSource_expandFlatAddTimecoverage_ResourceName = "testDsSource.expandFlatAddTimecoverage.result.xml";

  private InvCatalogFactory factory;
  private InvCatalogImpl expectedCatalog;

  private DatasetSource me1 = null;
  private DatasetSource me2 = null;
  private DatasetSource me3 = null;

  private ResultService rService1 = null;

  private String parentDsName1 = null;
  private String parentDsName2 = null;

  private InvDatasetImpl parentDs1 = null;
  private InvDatasetImpl parentDs2 = null;

  private String name1 = null;
  private String name2 = null;

  private String typeName1 = null;
  private String typeName2 = null;
  private String typeName3 = null;

  private DatasetSourceType type1 = null;
  private DatasetSourceType type2 = null;
  private DatasetSourceType type3 = null;

  private String structName1 = null;
  private String structName2 = null;
  private DatasetSourceStructure structure1 = null;
  private DatasetSourceStructure structure2 = null;

  private String accessPoint1 = null;
  private String accessPoint2 = null;

  private StringBuffer out = null;


  public TestDatasetSource( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    parentDsName1 = "parent dataset 1";
    parentDsName2 = "parent dataset 2";
    parentDs1 = new InvDatasetImpl( null, parentDsName1 );
    parentDs2 = new InvDatasetImpl( null, parentDsName2 );

    name1 = "name 1";
    name2 = "name 2";

    typeName1 =  "Local";
    typeName2 =  "DodsFileServer";
    typeName3 =  "DodsDir";
    type1 = DatasetSourceType.getType( typeName1 );
    type2 = DatasetSourceType.getType( typeName2 );
    type3 = DatasetSourceType.getType( typeName3 );

    structName1 = "Flat";
    structName2 = "DirTree";
    structure1 = DatasetSourceStructure.getStructure( structName1 );
    structure2 = DatasetSourceStructure.getStructure( structName2 );

    accessPoint1 = "access point 1";
    accessPoint2 = "access point 2";

    out = new StringBuffer();

    rService1 = new ResultService( "fred", ServiceType.DODS, "http://server/dods", null, "access point header 1");

    me1 = DatasetSource.newDatasetSource( name1,
                                          type1, structure1, accessPoint1, rService1 );
    me2 = DatasetSource.newDatasetSource( name1,
                                          type2, structure1, accessPoint1, rService1 );
    me3 = DatasetSource.newDatasetSource( name1,
                                          type3, structure1, accessPoint1, rService1 );

    this.factory = new InvCatalogFactory( "default", true );
  }

  //protected void tearDown()
  //{
  //  out = null;
  //  //parentDs = null;
  //}

  // Test expand on a flat collection dataset.
  public void testExpandFlat()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + test1DatasetSourceResultCatalog_1_0_ResourceName;

    String service1Name = "myServer";
    String service1Type = "DODS";
    String service1Base = "/dods/";
    String service1Suffix = null;
    String service1AccessPointHeader = "./build/test/classes/thredds/cataloggen/";
    ResultService service1 = new ResultService( service1Name, ServiceType.getType( service1Type),
                                                service1Base, service1Suffix,
                                                service1AccessPointHeader);

    String accessPoint = "./build/test/classes/thredds/cataloggen/testData/model";
    me1 = DatasetSource.newDatasetSource( "NCEP Eta 80km CONUS model data",
                                          DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT,
                                          accessPoint, service1 );

    DatasetFilter dsF = new DatasetFilter( me1, "Accept netCDF Eta 211 files only",
                                           DatasetFilter.Type.REGULAR_EXPRESSION,
                                           "/[0-9][^/]*_eta_211\\.nc$");
    me1.addDatasetFilter( dsF);

    InvDataset ds = null;
    try
    {
      ds = me1.expand();
    }
    catch ( IOException e )
    {
      throw new IllegalArgumentException( "Given directory is not a collection dataset <" + accessPoint + ">.");
    }

    if ( debugShowCatalogs_expandFlat )
    {
      // Print catalog to std out.
      InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
      try
      {
        System.out.println( fac.writeXML( (InvCatalogImpl) ds.getParentCatalog() ) );
      }
      catch ( IOException e )
      {
        System.out.println( "IOException trying to write catalog to sout: " + e.getMessage() );
      }
    }
    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( ds.getParentCatalog(), expectedCatalogResourceName );
  }

  // Expand a nested collection dataset using directory filtering.
  public void testExpandNotFlatWithDirFilter()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDsfDirFilter1_ResultCatalog_1_0_ResourceName;

    String service1Name = "myServer";
    String service1Type = "DODS";
    String service1Base = "/dods/";
    String service1Suffix = null;
    String service1AccessPointHeader = "./build/test/classes/thredds/cataloggen/";
    ResultService service1 = new ResultService( service1Name, ServiceType.getType( service1Type),
                                                service1Base, service1Suffix,
                                                service1AccessPointHeader);

    String service1AccessPoint = "./build/test/classes/thredds/cataloggen/testData/modelNotFlat";
    me1 = DatasetSource.newDatasetSource( "NCEP Eta 80km CONUS model data",
                                          DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT,
                                          service1AccessPoint, service1 );

    DatasetFilter dsF = new DatasetFilter( me1, "Accept netCDF Eta 211 files only",
                                           DatasetFilter.Type.REGULAR_EXPRESSION,
                                           "/[0-9][^/]*_eta_211\\.nc$");
    me1.addDatasetFilter( dsF);
    DatasetFilter dsF2 = new DatasetFilter( me1, "Accept Eta 211 directory only",
                                            DatasetFilter.Type.REGULAR_EXPRESSION,
                                            "eta_211$", true, false, false);
    dsF2.setApplyToCollectionDatasets( true);
    dsF2.setApplyToAtomicDatasets( false);
    me1.addDatasetFilter( dsF2);

    InvDataset ds = null;
    try
    {
      ds = me1.expand();
    }
    catch ( IOException e )
    {
      throw new IllegalArgumentException( "Given directory is not a collection dataset <" + service1AccessPoint + ">.");

    }
    try
    {
      this.factory.writeXML( (InvCatalogImpl) ds.getParentCatalog(), System.out);
    }
    catch ( IOException e )
    {
      assertTrue( "IOException on test write of expanded catalog.",
                  false);
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource(ds.getParentCatalog(), expectedCatalogResourceName);
  }

  // Expand a nested collection dataset creating catalogRefs for all sub-collection datasets.
  public void testExpandNotFlatWithAllCatalogRef()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDatasetSource_allCatalogRef_ResultCatalog_ResourceName;

    String service1Name = "myServer";
    String service1Type = "DODS";
    String service1Base = "/dods/";
    String service1Suffix = null;
    String service1AccessPointHeader = "./build/test/classes/thredds/cataloggen/";
    ResultService service1 = new ResultService( service1Name, ServiceType.getType( service1Type ),
                                                service1Base, service1Suffix,
                                                service1AccessPointHeader );

    String service1AccessPoint = "./build/test/classes/thredds/cataloggen/testData/modelNotFlat";
    me1 = DatasetSource.newDatasetSource( "NCEP Eta 80km CONUS model data",
                                          DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT,
                                          service1AccessPoint, service1 );
    me1.setCreateCatalogRefs( true );

    InvDataset ds = null;
    try
    {
      ds = me1.expand();
    }
    catch ( IOException e )
    {
      throw new IllegalArgumentException( "Given directory is not a collection dataset <" + service1AccessPoint + ">." );

    }
    try
    {
      this.factory.writeXML( (InvCatalogImpl) ds.getParentCatalog(), System.out );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException on test write of expanded catalog.",
                  false );
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( ds.getParentCatalog(), expectedCatalogResourceName );
  }

  // Expand a nested collection dataset creating catalogRefs for all sub-collection datasets.
  public void testExpandFlatAddTimecoverage()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDsSource_expandFlatAddTimecoverage_ResourceName;

    String serviceName = "localServer";
    String serviceType = "DODS";
    String serviceBase = "http://localhost:8080/thredds/dodsC/";
    String serviceSuffix = null;
    String serviceAccessPointHeader = "./test/data/thredds/cataloggen/testData";
    ResultService service = new ResultService( serviceName, ServiceType.getType( serviceType),
                                               serviceBase, serviceSuffix,
                                               serviceAccessPointHeader);

    String service1AccessPoint = "./test/data/thredds/cataloggen/testData/model";
    me1 = DatasetSource.newDatasetSource( "NCEP Eta 80km CONUS model data",
                                          DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT,
                                          service1AccessPoint, service );
    me1.setCreateCatalogRefs( true);
    me1.addDatasetFilter( new DatasetFilter( me1, "no CVS", DatasetFilter.Type.REGULAR_EXPRESSION, "CVS", true, false, true));
    me1.addDatasetEnhancer(
            DatasetEnhancer1.createAddTimeCoverageEnhancer(
                    "([0-9][0-9][0-9][0-9])([0-9][0-9])([0-9][0-9])([0-9][0-9])",
                    "$1-$2-$3T$4:00:00", "60 hours" ) );

    InvCatalog cat = null;
    try
    {
      cat = me1.fullExpand();
    }
    catch ( IOException e )
    {
      throw new IllegalArgumentException( "Given directory is not a collection dataset <" + service1AccessPoint + ">.");

    }
    try
    {
      this.factory.writeXML( (InvCatalogImpl) cat, System.out);
    }
    catch ( IOException e )
    {
      assertTrue( "IOException on test write of expanded catalog.",
                  false);
    }

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( cat, expectedCatalogResourceName);
  }

  public void testGetSet()
  {
    assertTrue( me1.getName().equals( name1));
    me1.setName( name2);
    assertTrue( me1.getName().equals( name2));
  }

  public void testType()
  {
    // Make sure the type names set above are correct.
    assertTrue( DatasetSourceType.LOCAL.toString().equals( typeName1));
    assertTrue( DatasetSourceType.DODS_FILE_SERVER.toString().equals( typeName2));
    assertTrue( DatasetSourceType.DODS_DIR.toString().equals( typeName3));

    // Test the DatasetSource.getType() function for each DatasetSource type.
    assertTrue( me1.getType().equals( DatasetSourceType.getType( typeName1)));
    assertTrue( me2.getType().equals( DatasetSourceType.getType( typeName2)));
    assertTrue( me3.getType().equals( DatasetSourceType.getType( typeName3)));
  }

  public void testStructure()
  {
    // Make sure the structure names set above are correct.
    assertTrue( DatasetSourceStructure.FLAT.toString().equals( structName1));
    assertTrue( DatasetSourceStructure.DIRECTORY_TREE.toString().equals( structName2));

    // Test the DatasetSource.getStructure() function.
    assertTrue( me1.getStructure().equals( DatasetSourceStructure.getStructure( structName1)));

    // Test DatasetSource.setStructure( DatasetSourceStructure)
    me1.setStructure( DatasetSourceStructure.getStructure( structName2 ) );
    assertTrue( me1.getStructure().toString().equals( structName2 ) );

  }

  public void testAccessPoint()
  {
    // Test DatasetSource.getAccessPoint()
    assertTrue( me1.getAccessPoint().equals( accessPoint1));

    // Test DatasetSource.setAccessPoint( String)
    me1.setAccessPoint( accessPoint2);
    assertTrue( me1.getAccessPoint().equals( accessPoint2));
  }

  public void testResultService()
  {
    // Test DatasetSource.getResultService() when no ResultService.
    assertTrue( me1.getResultService().getAccessPointHeader()
                .equals( "access point header 1"));

    // Test ResultService getter and setter.
    me1.setResultService( new ResultService( "service name", ServiceType.DODS,
                                             "base url", "suffix", "access point header" ) );
    assertTrue( me1.getResultService().getAccessPointHeader()
            .equals( "access point header"));
   }

  public void testDatasetNamer()
  {
    // Test DatasetSource.getDatasetNamerList() when no namers.
    assertTrue( me1.getDatasetNamerList().isEmpty() );

    // Test DatasetSource.addDatasetNamer( DatasetNamer)
    DatasetNamer namer = new DatasetNamer(
            parentDs1, "dsNamer name", true,
            DatasetNamerType.REGULAR_EXPRESSION,
            "match pattern", "substitute pattern",
            "attrib container", "attrib name" );
    me1.addDatasetNamer( namer );

    assertTrue( me1.getDatasetNamerList().contains( namer) );
  }

  public void testValid()
  {
    boolean bool;
    bool = me1.validate( out);
    assertTrue( out.toString(), bool );
  }

}

/*
 * $Log: TestDatasetSource.java,v $
 * Revision 1.8  2006/01/20 02:08:25  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.7  2005/12/06 19:39:21  edavis
 * Last CatalogBuilder/CrawlableDataset changes before start using in InvDatasetScan.
 *
 * Revision 1.6  2005/11/18 23:51:05  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.5  2005/07/08 18:35:00  edavis
 * Fix problem dealing with service URLs that are relative
 * to the catalog (base="") and those that are relative to
 * the collection (base URL is not empty).
 *
 * Revision 1.4  2005/06/28 18:36:31  edavis
 * Fixes to adding TimeCoverage and ID to datasets.
 *
 * Revision 1.3  2005/06/24 22:00:58  edavis
 * Write DatasetEnhancer1 to allow adding metadata to datasets.
 * Implement DatasetEnhancers for adding timeCoverage and for
 * adding ID to datasets. Also fix DatasetFilter so that 1) if
 * no filter is applicable for collection datasets, allow all
 * collection datasets and 2) if no filter is applicable for
 * atomic datasets, allow all atomic datasets.
 *
 * Revision 1.2  2005/06/07 22:50:24  edavis
 * Fixed catalogRef links so relative to catalog instead of to service.
 * Fixed all tests in TestAllCatalogGen (including changing directory
 * filters because catalogRef names no longer contain slashes ("/").
 *
 * Revision 1.1  2005/03/30 05:41:19  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.8  2005/01/14 21:24:33  edavis
 * Add handling of datasetSource@createCatalogRefs to DTD/XSD and
 * CatGenConfigMetadataFactory and testing.
 *
 * Revision 1.7  2004/12/29 21:53:20  edavis
 * Added catalogRef generation capability to DatasetSource: 1) a catalogRef
 * is generated for all accepted collection datasets; 2) once a DatasetSource
 * is expanded, information about each catalogRef is available. Added tests
 * for new catalogRef generation capability.
 *
 * Revision 1.6  2004/12/22 22:29:00  edavis
 * 1) Fix collection vs atomic dataset filtering includes fix so that default values are handled properly for the DatasetFilter attributes applyToCollectionDataset, applyToAtomicDataset, and invertMatchMeaning.
 * 2) Convert DatasetSource subclasses to use isCollection(), getTopLevelDataset(), and expandThisLevel() instead of expandThisType().
 *
 * Revision 1.5  2004/12/15 17:51:03  edavis
 * Changes to clean up ResultService. Changes to add a server title to DirectoryScanner (becomes the title of the top-level dataset).
 *
 * Revision 1.4  2004/11/30 22:19:26  edavis
 * Clean up some CatalogGen tests and add testing for DatasetSource (without and with filtering on collection datasets).
 *
 * Revision 1.3  2004/06/03 20:39:51  edavis
 * Added tests to check that CatGen config files are parsed correctly and
 * expanded catalogs are written correctly.
 *
 * Revision 1.2  2004/05/11 16:29:07  edavis
 * Updated to work with new thredds.catalog 0.6 stuff and the THREDDS
 * servlet framework.
 *
 * Revision 1.1  2003/08/20 17:23:42  edavis
 * Initial version.
 *
 */