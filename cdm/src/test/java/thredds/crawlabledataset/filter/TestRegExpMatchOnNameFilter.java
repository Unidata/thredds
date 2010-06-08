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
// $Id: TestRegExpMatchOnNameFilter.java 51 2006-07-12 17:13:13Z caron $
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
public class TestRegExpMatchOnNameFilter extends TestCase
{


  public TestRegExpMatchOnNameFilter( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testRegExpMatch()
  {
    checkRegExpMatch( "build\\.xml", "build.xml" );
    checkRegExpMatch( "build\\.xm.?", "build.xml" );
    checkRegExpMatch( "build\\..*", "build.xml" );

    checkRegExpMatch( "visad\\.jar", "lib/visad.jar" );
    checkRegExpMatch( "visad\\.j.?r", "lib/visad.jar" );
    checkRegExpMatch( ".*ad\\.jar", "lib/visad.jar" );
  }

  private void checkRegExpMatch( String regExpString, String dsPath )
  {
    RegExpMatchOnNameFilter me = new RegExpMatchOnNameFilter( regExpString );

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
    assertTrue( "Failed to construct RegExpMatchOnNameFilter <" + regExpString + ">.",
                me != null );

    assertTrue( "RegExp string <" + regExpString + "> did not match dataset <" + ds.getName() + ">.",
                me.accept( ds) );
  }
}
