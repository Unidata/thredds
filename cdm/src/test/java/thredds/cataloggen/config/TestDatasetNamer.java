// $Id: TestDatasetNamer.java 61 2006-07-12 21:36:00Z edavis $

/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

  private DatasetNamer me1 = null;
  private DatasetNamer me2 = null;
  private DatasetNamer me3 = null;

  private StringBuilder out = null;
  private boolean bool;

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

    me1 = new DatasetNamer( parentDs1, name1, addLevel1, type1,
            matchPattern1, substitutePattern1,
            null, null);
    me2 = new DatasetNamer( parentDs2, name2, addLevel2, type2,
            null, null,
            attribContainer2, attribName2);
    me3 = new DatasetNamer( parentDs1, name1, addLevel1, null,
            null, null,
            null, null);

    out = new StringBuilder();
  }

//  protected void tearDown()
//  {
//  }

  public void testParentDataset()
  {
    // Test DatasetNamer.getParentDataset()
    assertTrue( me1.getParentDataset().getName().equals( parentDsName1));

    // Test DatasetNamer.setParentDataset( InvDatasetImpl)
    me1.setParentDataset( parentDs2);
    assertTrue( me1.getParentDataset().getName().equals( parentDsName2));
  }

  public void testName()
  {
    // Test DatasetNamer.getName()
    assertTrue( me1.getName().equals( name1));

    // Test DatasetNamer.setName( String)
    me1.setName( name2);
    assertTrue( me1.getName().equals( name2));

    // Test DatasetNamer.validate( StringBuilder) for rules on name.
    bool = me1.validate( out);
    assertTrue( out.toString(), bool );

    out = new StringBuilder();
    me1.setName( "");
    bool = me1.validate( out);
    assertTrue( out.toString(), bool );

    out = new StringBuilder();
    me1.setName( null);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );
  }

  public void testAddLevel()
  {
    // Test DatasetNamer.getAddLevel()
    assertTrue( me1.getAddLevel() == addLevel1);

    // Test DatasetNamer.setAddLevel( boolean)
    me1.setAddLevel( addLevel2);
    assertTrue( me1.getAddLevel() == addLevel2);
  }

  public void testType()
  {
    // Test DatasetNamer.getType()
    assertTrue( me1.getType().equals( type1) );

    // Test DatasetNamer.setType( DatasetNamerType)
    me1.setType( type2);
    assertTrue( me1.getType().equals( type2) );

    // Test DatasetNamer.setType( String)
    me1.setType( type1.toString());
    assertTrue( me1.getType().equals( type1) );
    me1.setType( type2.toString());
    assertTrue( me1.getType().equals( type2) );

    me1.setType( "invalid type name");
    assertTrue( me1.getType() == null );

    // Test DatasetNamer.validate( StringBuilder) for rules on the type.
    // 1) Null type is invalid
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp1/sp1/ac0/an0 = R1/D0
    //   matchPattern non-null, substitutePattern non-null,
    //   attribContainer null, attribName null =
    //     RegExp is valid, DodsAttrib is invalid
    out = new StringBuilder();
    me1.setType( type1);
    bool = me1.validate( out);
    assertTrue( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type2);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp1/sp1/ac1+/an0 = R0/D0
    out = new StringBuilder();
    me1.setAttribContainer( "not null");
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type1);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp1/sp1/ac1/an1+ = R0/D0
    out = new StringBuilder();
    me1.setAttribName( "not null");
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type2);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp1/sp1/ac0-/an1 = R0/D0
    out = new StringBuilder();
    me1.setAttribContainer(  null);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type1);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp0-/sp1/ac0/an1 = R0/D0
    out = new StringBuilder();
    me1.setMatchPattern(  null);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type2);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp0/sp1/ac0/an0- = R0/D0
    out = new StringBuilder();
    me1.setAttribName( null);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type1);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp0/sp0-/ac0/an0 = R0/D0
    out = new StringBuilder();
    me1.setSubstitutePattern( null);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type2);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp0/sp0/ac1+/an0 = R0/D0
    out = new StringBuilder();
    me1.setAttribContainer( "not null");
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type1);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp0/sp1+/ac1/an0 = R0/D0
    out = new StringBuilder();
    me1.setSubstitutePattern( "not null");
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type2);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp0/sp1/ac1/an1+ = R0/D0
    out = new StringBuilder();
    me1.setAttribName( "not null");
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type1);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp0/sp0-/ac1/an1 = R0/D1
    out = new StringBuilder();
    me1.setSubstitutePattern( null);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type2);
    bool = me1.validate( out);
    assertTrue( out.toString(), bool );

    // mp0/sp0/ac0-/an1 = R0/D0
    out = new StringBuilder();
    me1.setAttribContainer( null);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type1);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp1+/sp0/ac0/an1 = R0/D0
    out = new StringBuilder();
    me1.setMatchPattern( "not null");
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type2);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp1/sp0/ac1+/an1 = R0/D0
    out = new StringBuilder();
    me1.setAttribContainer( "not null");
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type1);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp1/sp0/ac1/an0- = R0/D0
    out = new StringBuilder();
    me1.setAttribName( null);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type1);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    // mp1/sp0/ac0-/an0 = R0/D0
    out = new StringBuilder();
    me1.setAttribContainer( null);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    out = new StringBuilder();
    me1.setType( type2);
    bool = me1.validate( out);
    assertFalse( out.toString(), bool );

    assertTrue( "Failed because not mp1",
            me1.getMatchPattern() != null);
    assertTrue( "Failed because not sp0",
            me1.getSubstitutePattern() == null);
    assertTrue( "Failed because not ac0",
            me1.getAttribContainer() == null);
    assertTrue( "Failed because not an0",
            me1.getAttribName() == null);

  }

  public void testMatchPattern()
  {
    // Test DatasetNamer.getMatchPattern()
    assertTrue( me1.getMatchPattern().equals( matchPattern1));

    // Test DatasetNamer.setMatchPattern( String)
    me1.setMatchPattern( matchPattern2);
    assertTrue( me1.getMatchPattern().equals( matchPattern2));

    // Testing of DatasetNamer.validate( StringBuilder) dealing
    // with matchPattern, substitutePattern, attribContainer, and
    // attribName are handled in testTyp().
  }

  public void testSubstitutePattern()
  {
    // Test DatasetNamer.getSubstitutePattern()
    assertTrue( me1.getSubstitutePattern().equals( substitutePattern1));

    // Test DatasetNamer.setSubstitutePattern( String)
    me1.setSubstitutePattern( substitutePattern2);
    assertTrue( me1.getSubstitutePattern().equals( substitutePattern2));

    // Testing of DatasetNamer.validate( StringBuilder) dealing
    // with matchPattern, substitutePattern, attribContainer, and
    // attribName are handled in testTyp().
  }

  public void testAttribContainer()
  {
    // Test DatasetNamer.getAttribContainer()
    assertTrue( me2.getAttribContainer().equals( attribContainer2));

    // Test DatasetNamer.setAttribContainer( String)
    me2.setAttribContainer( attribContainer1);
    assertTrue( me2.getAttribContainer().equals( attribContainer1));

    // Testing of DatasetNamer.validate( StringBuilder) dealing
    // with matchPattern, substitutePattern, attribContainer, and
    // attribName are handled in testTyp().
  }

  public void testAttribName()
  {
    // Test DatasetNamer.getAttribName()
    assertTrue( me2.getAttribName().equals( attribName2));

    // Test DatasetNamer.setAttribName( String)
    me2.setAttribName( attribName1);
    assertTrue( me2.getAttribName().equals( attribName1));

    // Testing of DatasetNamer.validate( StringBuilder) dealing
    // with matchPattern, substitutePattern, attribContainer, and
    // attribName are handled in testTyp().
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
    me1 = new DatasetNamer( dsParent, namerName, true, DatasetNamerType.REGULAR_EXPRESSION,
                            namerMatchPattern, namerSubstitutionPattern,
                            null, null );
    assertTrue( "Namer <" + me1.getName() + "> failed to name dataset <urlPath=" + ds.getUrlPath() + ">",
                me1.nameDataset( ds));
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

  public void testNameDatasetList()
  {
    // Test for list of datasets
  }


//  public void testIsValid()
//  {
//    assertTrue( i == 5 );
//  }

}

/*
 * $Log: TestDatasetNamer.java,v $
 * Revision 1.3  2006/01/20 02:08:25  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.2  2005/07/14 20:01:26  edavis
 * Make ID generation mandatory for datasetScan generated catalogs.
 * Also, remove log4j from some tests.
 *
 * Revision 1.1  2005/03/30 05:41:18  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.6  2004/12/29 21:53:20  edavis
 * Added catalogRef generation capability to DatasetSource: 1) a catalogRef
 * is generated for all accepted collection datasets; 2) once a DatasetSource
 * is expanded, information about each catalogRef is available. Added tests
 * for new catalogRef generation capability.
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