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

  public void testGetters()
  {
    assertTrue( me.getParentDatasetSource() == dsSource );
    assertTrue( me.getName().equals( name1 ) );
    assertTrue( me.getType().equals( type1) );
    assertTrue( me.getMatchPattern().equals( matchPattern1 ) );
  }

  public void testValid()
  {
    StringBuilder stringBuilder = new StringBuilder();

    // Test DatasetFilter.validate() on valid DatasetFilter
    DatasetFilter dsFilter = new DatasetFilter( dsSource, name1, type1, matchPattern1 );
    boolean isValid = dsFilter.validate( stringBuilder);
    assertTrue( stringBuilder.toString(), isValid );

    // Test DatasetFilter.validate() when name is null
    stringBuilder = new StringBuilder();
    dsFilter = new DatasetFilter( dsSource, null, type1, matchPattern1 );
    isValid = dsFilter.validate( stringBuilder);
    assertFalse( stringBuilder.toString(), isValid );

    // Test DatasetFilter.validate() when name is ""
    stringBuilder = new StringBuilder();
    dsFilter = new DatasetFilter( dsSource, "", type1, matchPattern1 );
    isValid = dsFilter.validate( stringBuilder);
    assertTrue( stringBuilder.toString(), isValid );

    // Test DatasetFilter.validate() when type is null
    stringBuilder = new StringBuilder();
    dsFilter = new DatasetFilter( dsSource, name1, null, matchPattern1 );
    isValid = dsFilter.validate( stringBuilder);
    assertFalse( stringBuilder.toString(), isValid );

    // Test DatasetFilter.validate():
    //   if type is RegEx, matchPattern can't be null
    stringBuilder = new StringBuilder();
    dsFilter = new DatasetFilter( dsSource, name1, type1, null );
    isValid = dsFilter.validate( stringBuilder);
    assertFalse( stringBuilder.toString(), isValid );

  }

  public void testAccept()
  {
    String dssName = "ds source";
    String dssAccessPoint = "./src/test/data/thredds/cataloggen/testData/modelNotFlat";
    String dssAccessPointHeader = "./src/test/data/thredds/cataloggen/testData";
    String dsLoc = "./src/test/data/thredds/cataloggen/testData/modelNotFlat/eta_211/2004050300_eta_211.nc";

    InvDataset ds = null;
    ResultService rs = new ResultService( "srv", ServiceType.DODS, "", null,
                                          dssAccessPointHeader ); 
    DatasetSource dsSource = DatasetSource.newDatasetSource( dssName, DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT, dssAccessPoint, rs );
    DatasetFilter dsFilter = new DatasetFilter( dsSource, "dsF", DatasetFilter.Type.REGULAR_EXPRESSION, "nc$");
    try
    {
      ds = dsSource.createDataset( dsLoc, null );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException creating dataset <" + dsLoc + ">: " + e.getMessage(),
                  false);
    }
    dsFilter.accept( ds);


    rs = new ResultService( "srv", ServiceType.DODS, "", null, dssAccessPointHeader );
    dsSource = DatasetSource.newDatasetSource( dssName, DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT, dssAccessPoint, rs );
    dsFilter = new DatasetFilter( dsSource, "dsF", DatasetFilter.Type.REGULAR_EXPRESSION, ".*\\.nc");
    try
    {
      ds = dsSource.createDataset( dsLoc, null );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException creating dataset <" + dsLoc + ">: " + e.getMessage(),
                  false );
    }
    dsFilter.accept( ds );

  }

}