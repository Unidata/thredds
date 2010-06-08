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
// $Id: TestWildcardMatchOnNameFilter.java 51 2006-07-12 17:13:13Z caron $
package thredds.crawlabledataset.filter;

import junit.framework.*;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 5, 2005 1:26:20 PM
 */
public class TestWildcardMatchOnNameFilter extends TestCase
{


  public TestWildcardMatchOnNameFilter( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testWildcardStringConversionToRegExp()
  {
    checkWildcardStringAsRegExp( "aFile.nc", "aFile\\.nc" );
    checkWildcardStringAsRegExp( "a?ile.nc", "a.?ile\\.nc");
    checkWildcardStringAsRegExp( "a*e.nc", "a.*e\\.nc");
    checkWildcardStringAsRegExp( "a*e.n?", "a.*e\\.n.?");
  }

  public void testWildcardMatch()
  {
    checkWildcardMatch( "build.xml", "build.xml" );
    checkWildcardMatch( "build.xm?", "build.xml" );
    checkWildcardMatch( "build.*", "build.xml" );

    checkWildcardMatch( "visad.jar", "lib/visad.jar" );
    checkWildcardMatch( "visad.j?r", "lib/visad.jar" );
    checkWildcardMatch( "*ad.jar", "lib/visad.jar" );
  }

  private void checkWildcardStringAsRegExp( String wildcardString, String wildcardStringAsRegExp )
  {
    WildcardMatchOnNameFilter me = new WildcardMatchOnNameFilter( wildcardString );
    assertTrue( "Failed to construct WildcardMatchOnNameFilter <" + wildcardString + ">.",
                me != null );

    assertTrue( "Wildcard string <" + wildcardString + "> as regExp <" +
                me.wildcardString + "> not as expected <" + wildcardStringAsRegExp + ">.",
                me.wildcardString.equals( wildcardStringAsRegExp));
  }

  private void checkWildcardMatch( String wildcardString, String dsPath )
  {
    WildcardMatchOnNameFilter me = new WildcardMatchOnNameFilter( wildcardString );

    CrawlableDataset ds = null;
    try
    {
      ds = CrawlableDatasetFactory.createCrawlableDataset( dsPath, null, null );
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to create CrawlableDataset <" + dsPath + ">: " + e.getMessage(),
                  false );
    }
    assertTrue( "Failed to construct WildcardMatchOnNameFilter <" + wildcardString + ">.",
                me != null );

    assertTrue( "Wildcard string <" + wildcardString + "> did not match dataset <" + ds.getName() + ">.",
                me.accept( ds) );
  }
}
/*
 * $Log: TestWildcardMatchOnNameFilter.java,v $
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
 */