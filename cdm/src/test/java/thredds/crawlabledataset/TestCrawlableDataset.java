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
package thredds.crawlabledataset;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * A description
 *
 * @author edavis
 * @since 20 January 2006 13:22:59 -0600
 */
public class TestCrawlableDataset
{
  @Test
  public void testEmptyPath()
  {
    String path = "";

    CrawlableDataset crDs = checkCrDs( path, path);
    assertFalse("Unexpected exist()==true for CrDs(\"\").", crDs.exists());
  }

  @Test
  public void testRootPath()
  {
    String path = "/";
    String name = "";

    CrawlableDataset crDs = checkCrDs( path, name );
    assertTrue("CrDs(\"/\") doesn't exist.", crDs.exists());
  }

  @Test
  public void testResourcePath() throws URISyntaxException {
    File iospResourcesDir = new File(getClass().getResource("/resources/nj22/iosp").toURI());
    String path = iospResourcesDir.getAbsolutePath();
    String name = iospResourcesDir.getName();
    List<String> expectedChildrenNames = Arrays.asList("ghcnm.ncml", "igra-monthly.ncml", "igra-por.ncml");

    checkCrDsChildren(path, name, expectedChildrenNames);
  }

  // ToDo Get test working with manufactured directory.
  //@Test
  public void testSrcMainJavaPath()
  {
    String path = "src/main/java";
    String name = "java";
    List results = new ArrayList();
    results.add( "dods" );
    results.add("src/test/java/thredds");
    results.add("ucar");

    checkCrDsChildren( path, name, results );
  }

  // ToDo Get test working with manufactured directory.
  //@Test
  public void testSrcMainJavaDotDotPath()
  {
    String path = "src/main/java/..";
    String name = "target/generated-sources";
    List results = new ArrayList();
    results.add( "java" );
    results.add("resources");

    checkCrDsChildren( path, name, results );
  }

  // ToDo Get test working with manufactured directory.
  //@Test
  public void testSrcMainJavaDotDotSlashDotDotPath()
  {
    String path = "src/main/java/../..";
    String name = "target/generated-sources";
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
}
