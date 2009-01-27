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
package thredds.tds.ethan;

import junit.framework.TestCase;

import java.util.Properties;

import thredds.catalog.InvCatalogImpl;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 30, 2006 11:13:36 AM
 */
public class TestTdsPingMotherlode extends TestCase
{

  private String host = "motherlode.ucar.edu:8080";
  private String targetTomcatUrl;
  private String targetTdsUrl;

  public TestTdsPingMotherlode( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    Properties env = System.getProperties();
    host = env.getProperty( "thredds.tds.test.server", host );

    targetTomcatUrl = "http://" + host;
    targetTdsUrl = "http://" + host + "/thredds";
  }

  public void testMainCatalog()
  {
    String catUrl = targetTdsUrl + "/catalog.xml";

    StringBuilder msg = new StringBuilder();
    InvCatalogImpl catalog = TestAll.openAndValidateCatalog( catUrl, msg, false );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }
  }

  public void testTopCatalog()
  {
    String catUrl = targetTdsUrl + "/topcatalog.xml";

    StringBuilder msg = new StringBuilder();
    InvCatalogImpl catalog = TestAll.openAndValidateCatalog( catUrl, msg, false );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }
  }

  public void testIdvModelsCatalog()
  {
    String catUrl = targetTdsUrl + "/idv/models.xml";

    StringBuilder msg = new StringBuilder();
    InvCatalogImpl catalog = TestAll.openAndValidateCatalog( catUrl, msg, false );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }
  }

  public void testIdvLatestModelsCatalog()
  {
    TestAll.openValidateAndCheckAllLatestModelsInCatalogTree( targetTdsUrl + "/idv/latestModels.xml" );
  }

  public void testIdvRtModels10Catalog()
  {
    String catUrl = targetTdsUrl + "/idv/rt-models.1.0.xml";

    StringBuilder msg = new StringBuilder();
    InvCatalogImpl catalog = TestAll.openValidateAndCheckExpires( catUrl, msg );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }

    if ( msg.length() > 0 ) System.out.println( msg.toString() );
  }

  public void testIdvRtModels06Catalog()
  {
    String catUrl = targetTdsUrl + "/idv/rt-models.xml";

    StringBuilder msg = new StringBuilder();
    InvCatalogImpl catalog = TestAll.openValidateAndCheckExpires( catUrl, msg );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }

    if ( msg.length() > 0 ) System.out.println( msg.toString() );
  }

  public void testCatGenCdpCatalog()
  {
    String catUrl = targetTdsUrl + "/cataloggen/catalogs/uniModelsInvCat1.0en.xml";

    StringBuilder msg = new StringBuilder();
    InvCatalogImpl catalog = TestAll.openValidateAndCheckExpires( catUrl, msg );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }

    if ( msg.length() > 0 ) System.out.println( msg.toString() );
  }

  public void testCasestudiesCatalogs()
  {
    // ToDo crawl rather than ping the catalog(s) and check some datasets.

    String [] catUrls = {
            targetTdsUrl + "/casestudies/catalog.xml",
            targetTdsUrl + "/casestudies/ccs034Catalog.xml",
            targetTdsUrl + "/casestudies/ccs039Catalog.xml",
            targetTdsUrl + "/casestudies/july18_2002cat.xml",
            targetTdsUrl + "/casestudies/vgeeCatalog.xml"
    };

    boolean pass = true;
    StringBuilder msg = new StringBuilder();

    for ( int i = 0; i < catUrls.length; i++ )
    {
      pass &= null != TestAll.openAndValidateCatalog( catUrls[i], msg, false );
    }
    assertTrue( "Invalid catalog(s): " + msg.toString(),
                pass );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testAllNcModelsCatalog()
  {
    String catUrl = targetTdsUrl + "/idd/allModels.TDS-nc.xml";

    StringBuilder msg = new StringBuilder();
    InvCatalogImpl catalog = TestAll.openAndValidateCatalog( catUrl, msg, false );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }
  }

  public void testDqcServletCatalog()
  {
    TestAll.openAndValidateDqcDoc( targetTomcatUrl + "/dqcServlet/latestModel.xml" );
  }
}
