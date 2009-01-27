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
package thredds.cataloggen;

import junit.framework.TestCase;
import thredds.catalog.*;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * A description
 * <p/>
 * User: edavis
 * Date: Dec 13, 2004
 * Time: 1:00:42 PM
 */
public class TestDirectoryScanner extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestDirectoryScanner.class);

  private boolean debugShowCatalogs = true;

  private String serviceId = "myServer";
  private String serviceTitle = "My server of data";
  private ServiceType serviceType = ServiceType.DODS;
  private String serviceBase = "http://localhost:8080/thredds/dodsC/";
  private String serviceAccessPoint = "src/test/data/thredds/cataloggen/testData/modelNotFlat/";
  private String serviceAccessPointHeader = "src/test/data/thredds/cataloggen/";

  private String configResourcePath = "/thredds/cataloggen";
  private String testDirScan_topLevelDirNoCatRefs_ResourceName = "testDirScan.topLevelDirNoCatRefs.result.xml";
  private String testDirScan_topLevelDirNoName_ResourceName = "testDirScan.topLevelDirNoName.result.xml";
  private String testDirScan_topLevelDir_ResourceName = "testDirScan.topLevelDir.result.xml";
  private String testDirScan_withFilter_ResourceName = "testDirScan.withFilter.result.xml";
  private String testDirScan_withDirPattern_ResourceName = "testDirScan.withDirPattern.result.xml";
  private String testDirScan_withAliasDirAndFilePattern_ResourceName = "testDirScan.withAliasDirAndFilePattern.result.xml";
  private String testDirScan_catRefRepeatedDirProblem_ResourceName = "testDirScan.catRefRepeatedDirProblem.result.xml";

  public TestDirectoryScanner( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testTopLevelDirNoCatRefs()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_topLevelDirNoCatRefs_ResourceName;

    // Test on lower directory level than servers root directory (so top-level dataset name is some path)
    // Request is http://localhost:8080/thredds/dodsC/testData/modelNotFlat/catalog.xml
    // where base directory contains testData/
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( this.serviceAccessPointHeader), null, false );
    assertTrue( me != null );

    InvCatalog catalog = me.getDirCatalog( new File( this.serviceAccessPoint), null, false, false);

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
  }

  public void testTopLevelDirNoName()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_topLevelDirNoName_ResourceName;

    // Test on the servers root directory (so top-level dataset name is "")
    // Request is http://localhost:8080/thredds/dodsC/testData/modelNotFlat/catalog.xml
    // where base directory is modelNotFlat/
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( this.serviceAccessPoint), null, true );
    assertTrue( me != null );

    InvCatalog catalog = me.getDirCatalog( new File( this.serviceAccessPoint), null, false, false);

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
  }

  public void testTopLevelDir()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_topLevelDir_ResourceName;

    // Test on lower directory level than servers root directory (so top-level dataset name is some path)
    // Request is http://localhost:8080/thredds/dodsC/testData/modelNotFlat/catalog.xml
    // where base directory contains testData/
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( this.serviceAccessPointHeader), null, true );
    assertTrue( me != null );

    InvCatalog catalog = me.getDirCatalog( new File( this.serviceAccessPoint), null, false, false);

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
  }

  public void testWithFilter()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_withFilter_ResourceName;

    File dirToScan = new File( this.serviceAccessPoint + "eta_211/" );
    String filterPattern = ".*12_eta_211\\.nc$";

    // Test on lower directory level than servers root directory (so top-level dataset name is some path)
    // Request is http://localhost:8080/thredds/dodsC/testData/modelNotFlat/eta_211/catalog.xml
    // where base directory contains testData/
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( this.serviceAccessPointHeader), null, true );
    assertTrue( me != null );

    InvCatalog catalog = me.getDirCatalog( dirToScan, filterPattern, false, false);

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
  }

  public void testWithAliasDirPattern()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_withDirPattern_ResourceName;

    String dataDirHeader = "src/test/data/thredds/cataloggen/testData";
    String dataDir = "src/test/data/thredds/cataloggen/testData/uahRadarLevelII/2004*/KBMX";
    File dataDirFile = new File( dataDir );
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( dataDirHeader ), null, true );
    assertTrue( me != null );

    CrawlableDataset catalogCrDs;
    try
    {
      catalogCrDs = CrawlableDatasetFactory.createCrawlableDataset( dataDirFile.getAbsolutePath(), null, null );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException creating dataset <" + dataDirFile.getAbsolutePath() + ">: " + e.getMessage(),
                  false);
      return;
    }
    catch ( ClassNotFoundException e )
    {
      throw new IllegalArgumentException( "Did not find class: " + e.getMessage() );
    }
    catch ( NoSuchMethodException e )
    {
      throw new IllegalArgumentException( "Required constructor not found in class: " + e.getMessage() );
    }
    catch ( IllegalAccessException e )
    {
      throw new IllegalArgumentException( "Did not have necessary access to class: " + e.getMessage() );
    }
    catch ( InvocationTargetException e )
    {
      throw new IllegalArgumentException( "Could not invoke required method in class: " + e.getMessage() );
    }
    catch ( InstantiationException e )
    {
      throw new IllegalArgumentException( "Could not instatiate class: " + e.getMessage() );
    }
    if ( ! catalogCrDs.isCollection() )
      throw new IllegalArgumentException( "catalog directory is not a directory <" + dataDir + ">." );

    InvCatalog catalog = me.getDirCatalog( catalogCrDs, ".*", true, "idBase", false, null, null, null);

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
  }

  public void testWithAliasDirAndFilePattern()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_withAliasDirAndFilePattern_ResourceName;

    String dataDirHeader = "src/test/data/thredds/cataloggen/testData";
    String dataDir = "src/test/data/thredds/cataloggen/testData/uahRadarLevelII/2004*/KBMX/*bz2";
    File dataDirFile = new File( dataDir );
    InvService service = new InvService( this.serviceId, this.serviceType.toString(), this.serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, this.serviceTitle, new File( dataDirHeader ), null, true );
    assertTrue( me != null );

    CrawlableDataset catalogCrDs;
    try
    {
      catalogCrDs = CrawlableDatasetFactory.createCrawlableDataset( dataDirFile.getAbsolutePath(), null, null );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException creating dataset <" + dataDirFile.getAbsolutePath() + ">: " + e.getMessage(),
                  false );
      return;
    }
    catch ( ClassNotFoundException e )
    {
      throw new IllegalArgumentException( "Did not find class: " + e.getMessage() );
    }
    catch ( NoSuchMethodException e )
    {
      throw new IllegalArgumentException( "Required constructor not found in class: " + e.getMessage() );
    }
    catch ( IllegalAccessException e )
    {
      throw new IllegalArgumentException( "Did not have necessary access to class: " + e.getMessage() );
    }
    catch ( InvocationTargetException e )
    {
      throw new IllegalArgumentException( "Could not invoke required method in class: " + e.getMessage() );
    }
    catch ( InstantiationException e )
    {
      throw new IllegalArgumentException( "Could not instatiate class: " + e.getMessage() );
    }
    if ( ! catalogCrDs.isCollection() )
      throw new IllegalArgumentException( "catalog directory is not a directory <" + dataDir + ">." );

    InvCatalog catalog = me.getDirCatalog( catalogCrDs, ".*", true, "idBase", false, null, null, null);

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
  }

  public void testCatRefRepeatedDirProblem()
  {
    String expectedCatalogResourceName = configResourcePath + "/" + testDirScan_catRefRepeatedDirProblem_ResourceName;

    String serviceId = "ncdods";
    String serviceTitle = "nc and dods server";
    ServiceType serviceType = ServiceType.DODS;
    String serviceBase = "http://localhost:8080/thredds/dodsC/";

    String dataDir = "src\\test\\data";
    String reqDir = "trajectory/aircraft";

    File dataDirFile = new File( dataDir);
    File dirToScan = new File( dataDir + "/" + reqDir );
    String filterPattern = ".*nc$";

    log.info( "Calling DirectoryScanner( new InvService( \""+serviceId+"\", \""+serviceType.toString()+"\", \""+serviceBase+"\", null, null), \""+serviceTitle+"\", \""+dataDirFile.getPath()+"\", true)");
    InvService service = new InvService( serviceId, serviceType.toString(), serviceBase, null, null );
    DirectoryScanner me = new DirectoryScanner( service, serviceTitle, dataDirFile, null, true );
    assertTrue( me != null );

    log.info( "Calling getDirCatalog( \""+dirToScan+"\", \""+filterPattern+"\", false)");
    InvCatalog catalog = me.getDirCatalog( dirToScan, filterPattern, false, false );

    // Compare the resulting catalog an the expected catalog resource.
    TestCatalogGen.compareCatalogToCatalogResource( catalog, expectedCatalogResourceName, debugShowCatalogs );
  }
}
