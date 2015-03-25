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
import thredds.cataloggen.DatasetEnhancer1;

import java.io.*;

/**
 *
 */
public class TestDatasetSource
{
  //static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestDatasetSource.class);

  private boolean debugShowCatalogs = false;

  private DatasetSource me1 = null;
  private DatasetSource me2 = null;
  private DatasetSource me3 = null;

  private InvDatasetImpl parentDs1 = null;

  private String name1 = null;
  private String name2 = null;

  private String typeName1 = null;
  private String typeName2 = null;
  private String typeName3 = null;

  private String structName1 = null;
  private String structName2 = null;

  private String accessPoint1 = null;
  private String accessPoint2 = null;

  private StringBuilder out = null;

  @Before
  public void setUp()
  {
    String parentDsName1 = "parent dataset 1";
    parentDs1 = new InvDatasetImpl( null, parentDsName1 );

    name1 = "name 1";
    name2 = "name 2";

    typeName1 =  "Local";
    typeName2 =  "DodsFileServer";
    typeName3 =  "DodsDir";
    DatasetSourceType type1 = DatasetSourceType.getType( typeName1 );
    DatasetSourceType type2 = DatasetSourceType.getType( typeName2 );
    DatasetSourceType type3 = DatasetSourceType.getType( typeName3 );

    structName1 = "Flat";
    structName2 = "DirTree";
    DatasetSourceStructure structure1 = DatasetSourceStructure.getStructure( structName1 );

    accessPoint1 = "access point 1";
    accessPoint2 = "access point 2";

    out = new StringBuilder();

    ResultService rService = new ResultService( "fred", ServiceType.DODS, "http://server/dods", null, "access point header 1");

    me1 = DatasetSource.newDatasetSource( name1,
                                          type1, structure1, accessPoint1, rService );
    me2 = DatasetSource.newDatasetSource( name1,
                                          type2, structure1, accessPoint1, rService );
    me3 = DatasetSource.newDatasetSource( name1,
                                          type3, structure1, accessPoint1, rService );
  }

  //protected void tearDown()
  //{
  //  out = null;
  //  //parentDs = null;
  //}

  // Test expand on a flat collection dataset.
  //@Test
  public void testExpandFlat() throws IOException
  {
    File expectedCatDocFile = new File( "src/test/data/thredds/cataloggen/config/test1ResultCatalog1.0.dss.xml" );

    String service1Name = "myServer";
    String service1Type = "DODS";
    String service1Base = "/dods/";
    String service1Suffix = null;
    String service1AccessPointHeader = "./src/test/data/thredds/cataloggen/";
    ResultService service1 = new ResultService( service1Name, ServiceType.getType( service1Type),
                                                service1Base, service1Suffix,
                                                service1AccessPointHeader);

    String accessPoint = "./src/test/data/thredds/cataloggen/testData/model";
    me1 = DatasetSource.newDatasetSource( "NCEP Eta 80km CONUS model data",
                                          DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT,
                                          accessPoint, service1 );

    DatasetFilter dsF = new DatasetFilter( me1, "Accept netCDF Eta 211 files only",
                                           DatasetFilter.Type.REGULAR_EXPRESSION,
                                           "/[0-9]*_eta_211\\.nc$");
    me1.addDatasetFilter( dsF);
    dsF = new DatasetFilter( me1, "reject all subdirs", DatasetFilter.Type.REGULAR_EXPRESSION, ".*", true, false, true);
    me1.addDatasetFilter( dsF );

    InvDataset ds = me1.expand();

    TestInvDatasetScan.compareCatalogToCatalogDocFile(ds.getParentCatalog(), expectedCatDocFile, debugShowCatalogs);
  }

  // Expand a nested collection dataset using directory filtering.
  //@Test
  public void testExpandNotFlatWithDirFilter() throws IOException
  {
    File expectedCatalogDocFile = new File( "src/test/data/thredds/cataloggen/config/testDsfDirFilter1.ResultCatalog1.0.xml");

    String service1Name = "myServer";
    String service1Type = "DODS";
    String service1Base = "/dods/";
    String service1Suffix = null;
    String service1AccessPointHeader = "./src/test/data/thredds/cataloggen/";
    ResultService service1 = new ResultService( service1Name, ServiceType.getType( service1Type),
                                                service1Base, service1Suffix,
                                                service1AccessPointHeader);

    String service1AccessPoint = "./src/test/data/thredds/cataloggen/testData/modelNotFlat";
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
    me1.addDatasetFilter( dsF2);
    DatasetFilter dsF3 = new DatasetFilter( me1, "Reject .svn directory only",
                                            DatasetFilter.Type.REGULAR_EXPRESSION,
                                            ".svn$", true, false, true);
    me1.addDatasetFilter( dsF3);

    InvDataset ds = me1.expand();

    // Compare the resulting catalog an the expected catalog resource.
    TestInvDatasetScan.compareCatalogToCatalogDocFile(ds.getParentCatalog(), expectedCatalogDocFile, debugShowCatalogs);
  }

  // Expand a nested collection dataset creating catalogRefs for all sub-collection datasets.
  //@Test
  public void testExpandNotFlatWithAllCatalogRef() throws IOException
  {
    File expectedCatalogDocFile = new File( "src/test/data/thredds/cataloggen/config/testDatasetSource.allCatalogRef.result.xml");

    String service1Name = "myServer";
    String service1Type = "DODS";
    String service1Base = "/dods/";
    String service1Suffix = null;
    String service1AccessPointHeader = "./src/test/data/thredds/cataloggen/";
    ResultService service1 = new ResultService( service1Name, ServiceType.getType( service1Type ),
                                                service1Base, service1Suffix,
                                                service1AccessPointHeader );

    String service1AccessPoint = "./src/test/data/thredds/cataloggen/testData/modelNotFlat";
    me1 = DatasetSource.newDatasetSource( "NCEP Eta 80km CONUS model data",
                                          DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT,
                                          service1AccessPoint, service1 );
    me1.setCreateCatalogRefs( true );
    DatasetFilter dsF = new DatasetFilter( me1, "Accept 'eta_211' and 'gfs_211' directories",
                                           DatasetFilter.Type.REGULAR_EXPRESSION,
                                           "_211$", true, false, false );
    me1.addDatasetFilter( dsF );


    InvDataset ds = me1.expand();

    // Compare the resulting catalog an the expected catalog resource.
    TestInvDatasetScan.compareCatalogToCatalogDocFile(ds.getParentCatalog(), expectedCatalogDocFile, debugShowCatalogs);
  }

  // Expand a nested collection dataset creating catalogRefs for all sub-collection datasets.
  // ToDo Fix this test. (Why String compare passes but object compare fails?)
  //@Test
  public void testExpandFlatAddTimecoverage() throws IOException
  {
    File expectedCatalogDocFile = new File( "src/test/data/thredds/cataloggen/config/testDsSource.expandFlatAddTimecoverage.result.xml" );

    String serviceName = "localServer";
    String serviceType = "DODS";
    String serviceBase = "http://localhost:8080/thredds/dodsC/";
    String serviceSuffix = null;
    String serviceAccessPointHeader = "./src/test/data/thredds/cataloggen/testData";
    ResultService service = new ResultService( serviceName, ServiceType.getType( serviceType),
                                               serviceBase, serviceSuffix,
                                               serviceAccessPointHeader);

    String service1AccessPoint = "./src/test/data/thredds/cataloggen/testData/model";
    me1 = DatasetSource.newDatasetSource( "NCEP Eta 80km CONUS model data",
                                          DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT,
                                          service1AccessPoint, service );
    me1.setCreateCatalogRefs( true);
    me1.addDatasetFilter( new DatasetFilter( me1, "no CVS", DatasetFilter.Type.REGULAR_EXPRESSION, "CVS", true, false, true));
    me1.addDatasetEnhancer(
            DatasetEnhancer1.createAddTimeCoverageEnhancer(
                    "([0-9][0-9][0-9][0-9])([0-9][0-9])([0-9][0-9])([0-9][0-9])",
                    "$1-$2-$3T$4:00:00", "60 hours" ) );

    InvCatalog cat = me1.fullExpand();

    // Compare the resulting catalog an the expected catalog resource.
    TestInvDatasetScan.compareCatalogToCatalogDocFile(cat, expectedCatalogDocFile, true);
  }

  @Test
  public void testGetSet()
  {
    assertTrue( me1.getName().equals( name1));
    me1.setName( name2);
    assertTrue( me1.getName().equals( name2));
  }

  @Test
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

  @Test
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

  @Test
  public void testAccessPoint()
  {
    // Test DatasetSource.getAccessPoint()
    assertTrue( me1.getAccessPoint().equals( accessPoint1));

    // Test DatasetSource.setAccessPoint( String)
    me1.setAccessPoint( accessPoint2);
    assertTrue( me1.getAccessPoint().equals( accessPoint2));
  }

  @Test
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

  @Test
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

  @Test
  public void testValid()
  {
    boolean bool;
    bool = me1.validate( out);
    assertTrue( out.toString(), bool );
  }

}
