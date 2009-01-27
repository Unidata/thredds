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
// $Id: TestDatasetFilter.java 61 2006-07-12 21:36:00Z edavis $

package thredds.cataloggen.config;

import thredds.catalog.*;

import junit.framework.*;

import java.io.IOException;

/**
 *
 *
 *
 *
 */
public class TestDatasetFilter extends TestCase
{
  private String parentDsName1 = null;
  private String parentDsName2 = null;
  private String parentDsName3 = null;
  private String parentDsUrl1 = null;
  private String parentDsUrl2 = null;
  private String parentDsUrl3 = null;
  private InvDatasetImpl parentDs1 = null;
  private InvDatasetImpl parentDs2 = null;
  private InvDatasetImpl parentDs3 = null;

  private String name1 = null;
  private String name2 = null;

  private DatasetFilter.Type type1 = null;

  private String matchPattern1 = null;
  private String matchPattern2 = null;

  private DatasetSource dsSource = null;

  private StringBuilder out = null;

  private DatasetFilter me = null;

  public TestDatasetFilter( String name)
  {
    super( name);
  }

  protected void setUp()
  {
    parentDsName1 = "parent dataset 1";
    parentDsName2 = "parent dataset 2";
    parentDsName3 = "Parent dataset 3"; // Capitalized for RegExp match test

    parentDsUrl1 = "http://server/parentDatasetUrl1";
    parentDsUrl2 = "http://server/parentDatasetUrl2";
    parentDsUrl3 = "http://server/ParentDatasetUrl3";

    parentDs1 = new InvDatasetImpl( null, parentDsName1);
    parentDs1.setUrlPath( parentDsUrl1);
    parentDs2 = new InvDatasetImpl( null, parentDsName2);
    parentDs2.setUrlPath( parentDsUrl2);
    parentDs3 = new InvDatasetImpl( null, parentDsName3);
    parentDs3.setUrlPath( parentDsUrl3);

    name1 = "name 1";
    name2 = "name 2";

    type1 = DatasetFilter.Type.REGULAR_EXPRESSION;

    matchPattern1 = "match pattern 1";
    matchPattern2 = "match pattern 2";

    dsSource = DatasetSource.newDatasetSource( "dsSource", DatasetSourceType.LOCAL, DatasetSourceStructure.DIRECTORY_TREE, "access point 1", null );

    out = new StringBuilder();

    me = new DatasetFilter( dsSource, name1, type1, matchPattern1);

  }

//  protected void tearDown()
//  {
//  }

  public void testParentDataset()
  {
    // Test DatasetFilter.getParentDataset()
    assertTrue( me.getParentDatasetSource().getName().equals( "dsSource"));

  }

  public void testName()
  {
    // Test DatasetFilter.getName()
    assertTrue( me.getName().equals( name1));

    // Test DatasetFilter.setName( String)
    me.setName( name2);
    assertTrue( me.getName().equals( name2));
  }

  public void testType()
  {
    // Test DatasetFilter.getType()
    assertTrue( me.getType().equals( type1) );

    // Test DatasetFilter.setType( DatasetFilter.Type)
    me.setType( null);
    assertTrue( me.getType()  == null );
  }

  public void testMatchPattern()
  {
    // Test DatasetFilter.getMatchPattern()
    assertTrue( me.getMatchPattern().equals( matchPattern1));

    // Test DatasetFilter.setMatchPattern( String)
    me.setMatchPattern( matchPattern2);
    assertTrue( me.getMatchPattern().equals( matchPattern2));
  }

  public void testValid()
  {
    // Test DatasetFilter.validate() on valid DatasetFilter
    boolean bool;
    bool = me.validate( out);
    assertTrue( out.toString(), bool );

    // Test DatasetFilter.validate() when name is null
    out = new StringBuilder();
    me.setName( null);
    bool = me.validate( out);
    assertFalse( out.toString(), bool );
    me.setName( name1);

    // Test DatasetFilter.validate() when name is ""
    out = new StringBuilder();
    me.setName( "");
    bool = me.validate( out);
    assertTrue( out.toString(), bool );
    me.setName( name1);

    // Test DatasetFilter.validate() when type is null
    out = new StringBuilder();
    me.setType( null);
    bool = me.validate( out);
    assertFalse( out.toString(), bool );
    me.setType( type1);

    // Test DatasetFilter.validate():
    //   if type is RegEx, matchPattern can't be null
    out = new StringBuilder();
    me.setType( type1);
    me.setMatchPattern( null);
    bool = me.validate( out);
    assertFalse( out.toString(), bool );
    me.setMatchPattern( matchPattern1);

//    // Test DatasetFilter.validate():
//    //   if type is not RegEx, matchPattern must be null
//    out = new StringBuffer();
//    me.setType( type2);
//    bool = me.validate( out);
//    assertFalse( out.toString(), bool );
//    me.setType( type1);

  }

  public void testAccept()
  {
    String dssName = "ds source";
    String dssAccessPoint = "./test/data/thredds/cataloggen/testData/modelNotFlat";
    String dssAccessPointHeader = "./test/data/thredds/cataloggen/testData";
    String dsLoc = "./test/data/thredds/cataloggen/testData/modelNotFlat/eta_211/2004050300_eta_211.nc";
    InvDataset ds = null;
    ResultService rs = new ResultService( "srv", ServiceType.DODS, "", null,
                                          dssAccessPointHeader ); 
    DatasetSource dsSource = DatasetSource.newDatasetSource( dssName, DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT, dssAccessPoint, rs );
    DatasetFilter me2 = new DatasetFilter( dsSource, "dsF", DatasetFilter.Type.REGULAR_EXPRESSION, "*.nc");
    try
    {
      ds = dsSource.createDataset( dsLoc, null );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException creating dataset <" + dsLoc + ">: " + e.getMessage(),
                  false);
    }
    me2.accept( ds);
  }

}
/*
 * $Log: TestDatasetFilter.java,v $
 * Revision 1.6  2006/01/20 02:08:25  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.5  2005/11/18 23:51:05  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.4  2005/07/20 22:44:55  edavis
 * Allow InvDatasetScan to work with a service that is not catalog relative.
 * (DatasetSource can now add a prefix path name to resulting urlPaths.)
 *
 * Revision 1.3  2005/07/14 20:01:26  edavis
 * Make ID generation mandatory for datasetScan generated catalogs.
 * Also, remove log4j from some tests.
 *
 * Revision 1.2  2005/03/31 23:12:20  edavis
 * Some fixes for CatalogGen tests.
 *
 * Revision 1.1  2005/03/30 05:41:18  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.4  2004/12/29 21:53:20  edavis
 * Added catalogRef generation capability to DatasetSource: 1) a catalogRef
 * is generated for all accepted collection datasets; 2) once a DatasetSource
 * is expanded, information about each catalogRef is available. Added tests
 * for new catalogRef generation capability.
 *
 * Revision 1.3  2004/11/30 22:19:25  edavis
 * Clean up some CatalogGen tests and add testing for DatasetSource (without and with filtering on collection datasets).
 *
 * Revision 1.2  2004/05/11 16:29:07  edavis
 * Updated to work with new thredds.catalog 0.6 stuff and the THREDDS
 * servlet framework.
 *
 * Revision 1.1  2003/08/20 17:23:42  edavis
 * Initial version.
 *
 * Revision 1.1  2002/12/23 19:32:28  edavis
 * Added first unit tests.
 *
 *
 */