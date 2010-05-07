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

import junit.framework.TestCase;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.ServiceType;

/**
 *
 */
public class TestDatasetNamer extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestDatasetNamer.class);

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

  private boolean addLevel1 = false;
  private boolean addLevel2 = false;

  private DatasetNamerType type1 = null;
  private DatasetNamerType type2 = null;

  private String matchPattern1 = null;
  private String matchPattern2 = null;

  private String substitutePattern1 = null;
  private String substitutePattern2 = null;

  private String attribContainer1 = null;
  private String attribContainer2 = null;

  private String attribName1 = null;
  private String attribName2 = null;


  private DatasetSource source1 = null;
  private DatasetSource source2 = null;
  private DatasetSource source3 = null;

  private ResultService resService1 = null;

  public TestDatasetNamer( String name )
  {
    super( name );
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

    addLevel1 = true;
    addLevel2 = false;

    type1 = DatasetNamerType.REGULAR_EXPRESSION;
    type2 = DatasetNamerType.DODS_ATTRIBUTE;

    matchPattern1 = "match pattern 1";
    matchPattern2 = "match pattern 2";

    substitutePattern1 = "sub pattern 1";
    substitutePattern2 = "sub pattern 2";

    attribContainer1 = "attrib container 1";
    attribContainer2 = "attrib container 2";

    attribName1 = "attrib name 1";
    attribName2 = "attrib name 2";

    resService1 = new ResultService( "fred", ServiceType.DODS, "http://server/dods", null, "access ");

    source1 = DatasetSource.newDatasetSource( "ds name 1",
            DatasetSourceType.LOCAL, DatasetSourceStructure.FLAT,
            "access Point 1", resService1 );
    source2 = DatasetSource.newDatasetSource( "ds name 2",
            DatasetSourceType.DODS_DIR,
            DatasetSourceStructure.DIRECTORY_TREE,
            "access Point 1", resService1 );
    // An invalid DatasetSource
    source3 = DatasetSource.newDatasetSource( null,
            DatasetSourceType.DODS_FILE_SERVER,
            DatasetSourceStructure.DIRECTORY_TREE,
            "access Point 1", resService1 );
  }

  public void testGetters()
  {
    DatasetNamer dsNamer = new DatasetNamer( parentDs1, name1, addLevel1,
                                             type1, matchPattern1, substitutePattern1, null, null );
    assertEquals( parentDs1, dsNamer.getParentDataset() );
    assertEquals( name1, dsNamer.getName() );
    assertTrue( dsNamer.getAddLevel() == addLevel1 );
    assertEquals( type1, dsNamer.getType() );
    assertEquals( matchPattern1, dsNamer.getMatchPattern() );
    assertEquals( substitutePattern1, dsNamer.getSubstitutePattern() );
    assertNull( dsNamer.getAttribContainer() );
    assertNull( dsNamer.getAttribName() );

    dsNamer = new DatasetNamer( parentDs2, name2, addLevel2,
                                type2, null, null, attribContainer2, attribName2 );
    assertEquals( type2, dsNamer.getType() );
    assertEquals( attribContainer2, dsNamer.getAttribContainer() );
    assertEquals( attribName2, dsNamer.getAttribName() );
  }

  public void testVariousValidityConstraints()
  {
    StringBuilder stringBuilder = new StringBuilder();

    // Test DatasetNamer.validate( StringBuilder) for rules on the type.
    // 1) Null type is invalid
    DatasetNamer dsNamer = new DatasetNamer( parentDs1, name1, addLevel1,
                                             null, matchPattern1, substitutePattern1, null, null );
    boolean isValid = dsNamer.validate( stringBuilder );
    assertFalse( stringBuilder.toString(), isValid);

    dsNamer = new DatasetNamer( parentDs1, name1, addLevel1,
                                type1, matchPattern1, substitutePattern1, null, null );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertTrue( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs1, name1, addLevel1,
                                type1, matchPattern1, null, null, null );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertFalse( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs1, name1, addLevel1,
                                type1, null , substitutePattern1, null, null );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertFalse( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs1, name1, addLevel1,
                                type1, matchPattern1, substitutePattern1, attribContainer1, null );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertTrue( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs1, name1, addLevel1,
                                type1, matchPattern1, substitutePattern1, null, attribName1 );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertTrue( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs1, name1, addLevel1,
                                type1, matchPattern1, substitutePattern1, attribContainer1, attribName1 );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertTrue( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs2, name2, addLevel2,
                                type2, null, null, attribContainer2, attribName2 );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertTrue( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs2, name2, addLevel2,
                                type2, null, null, null, attribName2 );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertFalse( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs2, name2, addLevel2,
                                type2, null, null, attribContainer2, null );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertFalse( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs2, name2, addLevel2,
                                type2, matchPattern2, null, attribContainer2, attribName2 );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertTrue( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs2, name2, addLevel2,
                                type2, matchPattern2, substitutePattern2, attribContainer2, attribName2 );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertTrue( stringBuilder.toString(), isValid );

    dsNamer = new DatasetNamer( parentDs2, name2, addLevel2,
                                type2, null, substitutePattern2, attribContainer2, attribName2 );
    stringBuilder = new StringBuilder();
    isValid = dsNamer.validate( stringBuilder );
    assertTrue( stringBuilder.toString(), isValid );
  }

  public void testNameDataset()
  {
    // Test for REGULAR_EXPRESSION type namers:
    // - on a dataset with urlPath that matches
    // - on a dataset with urlPath that doesn't match
    // - on a dataset with urlPath of ""
    // - on a dataset with urlPath of null

    // http://motherlode.ucar.edu/cgi-bin/dods/DODS-3.2.1/nph-dods/dods/model/
    // dods/model/2004050812_eta_211.nc

    log.debug( "testNameDataset(): start");
    String parentDsName = "Eta";
    String dsUrlPath = "dods/model/2004050812_eta_211.nc";
    String namerName = "Eta Namer";
    String namerMatchPattern = "([0-9][0-9][0-9][0-9])([0-9][0-9])([0-9][0-9])([0-9][0-9])_eta_211.nc$";
    String namerSubstitutionPattern = "NCEP Eta $1-$2-$3 $4:00:00 GMT";
    String dsName = "NCEP Eta 2004-05-08 12:00:00 GMT";

    InvDatasetImpl dsParent = new InvDatasetImpl( null, parentDsName );
    InvDatasetImpl ds = new InvDatasetImpl( null, null, null, null, dsUrlPath );
    DatasetNamer dsNamer = new DatasetNamer( dsParent, namerName, true, DatasetNamerType.REGULAR_EXPRESSION,
                            namerMatchPattern, namerSubstitutionPattern,
                            null, null );
    assertTrue( "Namer <" + dsNamer.getName() + "> failed to name dataset <urlPath=" + ds.getUrlPath() + ">",
                dsNamer.nameDataset( ds));
    assertTrue( "Dataset name <" + ds.getName() + "> does not match expected <" + dsName + ">.",
                ds.getName().equals( dsName ));

    //       DatasetNamer( InvDataset parentDs,
    //                   String name, boolean addLevel, DatasetNamerType type,
    //                   String matchPattern, String substitutePattern,
    //                   String attribContainer, String attribName)

    // How test for DODS_ATTRIB type namers?
    // - on a dataset that doesn't contain the attrib container
    // - on a dataset that doesn't contain the attrib named
    // - on a dataset that does contain the attribute
  }
}