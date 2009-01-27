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
// $Id: TestLatestModel.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server;

import junit.framework.TestCase;

import java.io.*;
import java.util.HashMap;

/**
 * A description
 *
 * User: edavis
 * Date: Dec 24, 2003
 * Time: 2:14:20 PM
 */
public class TestLatestModel extends TestCase
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TestDqcHandler.class );

  private String testResourceDirectory = "./build/test/classes"; // for access as File
  private String configResourcePath = "/thredds/dqc/server";
  private String goodLatestModelConfigResourceName = "configLatestModelExampleGood.xml";
  private String testLatestModel_configFile = "testLatestModel.configFile.xml";
  private String writableLatestModelConfigResourceName = "configLatestModelWritable.xml";
  private String testDataFileName = "20040102_eta_211.nc";
  private String testDataFileModelName = "eta_211";

  private LatestModel me;
  private LatestModel me2;
  private String writeConfigFileName = "TestWriteOfLatestModelConfigFile.xml";
  private String writeConfigFilePath = ".";

  private String exampleDqcDocumentResourceName = configResourcePath + "/dqcLatestModelExample.xml";

  private String dirName = "./build/test/classes/thredds/dqc/server";
  private String dirNameRoot = "./build/test/classes/thredds/dqc/server";
  private String serviceBaseURL = "http://motherlode.ucar.edu/cgi-bin/dods/DODS-3.2.1/nph-dods/";
  private String matchPattern = "([0-9][0-9][0-9][0-9])([0-9][0-9])([0-9][0-9])([0-9][0-9])_@model@.nc$";
  private String substitutePattern = "$1/$2/$3 $4:00";
  private String invCatSpecVersion = "1.0";
  private String dqcSpecVersion="0.3";


  private HashMap models = new HashMap();

  private String handlerName = null;
  private String handlerDescription = null;
  private String handlerClassName = null;
  private String handlerConfigFileName = null;

  private DqcServletConfigItem handlerInfo = null;

  public TestLatestModel( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    String tmpMsg = null;
    log.debug( "setUp(): starting");

    models.put( "ecmwf_1-12", "ECMWF_1-12");
    models.put( "eta_211", "Eta_211");
    models.put( "gfs_211", "GFS_211");
    models.put( "gfs_37-44", "GFS_37-44");
    models.put( "gfs_21-24", "GFS_21-24");
    models.put( "gfs_25-26", "GFS_25-26");
    models.put( "ngm_211", "NGM_211");
    models.put( "ocean_21-24", "Ocean_21-24");
    models.put( "ruc_211", "RUC_211");
    models.put( "ruc2_236", "RUC2_236");
    models.put( "sst_21-24", "SST_21-24");
    models.put( "sst_61-64", "SST_61-64");
    models.put( "wseta", "Workstation Eta");

    handlerName = "latestModel";
    handlerDescription = "Returns the latest run of each model.";
    handlerClassName = "thredds.dqc.server.LatestModel";
    handlerConfigFileName = goodLatestModelConfigResourceName;

    handlerInfo = new DqcServletConfigItem( handlerName, handlerDescription,
                                            handlerClassName, handlerConfigFileName );

    // Create a LatestModel DQCHandler.
    try
    {
      me = (LatestModel) DqcHandler.factory( handlerInfo, this.configResourcePath );
    }
    catch ( DqcHandlerInstantiationException e )
    {
      tmpMsg = "Unexpected DqcHandlerInstantiationException from DqcHandler.factory("
               + handlerInfo.toString() + "," + this.configResourcePath + "): " + e.getMessage();
      log.debug( "testFactoryGood(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    catch ( IOException e )
    {
      tmpMsg = "Unexpected IOException from DqcHandler.factory("
               + handlerInfo.toString() + "," + this.configResourcePath + "): " + e.getMessage();
      log.debug( "testFactoryGood(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "DqcHandler.factory() returned a null.",
                me != null );

  }

//  public void test1()
//  {
//    me.proxy.
//  }
//
//  public void testDqcCreation()
//  {
//    log.debug( "testDqcCreation(): start.");
//
//    // Create a DQC document for the test LatestModel.
//    QueryCapability dqc = me.createDqcDocument( "/thredds/dqc/" + me.getHandlerInfo().getName() );
//
//    // Can't compare to DQC doc parsed because document URL will be different.
//    // So will compare with what I think it should be.
//    assertTrue( "DQC doc name <" + dqc.getName() + "> is not as expected <" + "latestModel DQC Document" + ">.",
//                dqc.getName().equals( "latestModel DQC Document" ) );
//    assertTrue( "DQC doc version <" + dqc.getVersion() + "> is not as expected <" + "0.3" + ">.",
//                dqc.getVersion().equals( "0.3" ) );
//
//    Query query = dqc.getQuery();
//    assertTrue( "DQC query base URI <" + query.getBase() + "> is not as expected <" + "/thredds/dqc/" + me.getHandlerInfo().getName() + ">.",
//                query.getBase().equals( "/thredds/dqc/" + me.getHandlerInfo().getName()) );
//
//    SelectService ss = (SelectService) dqc.getServiceSelector();
//    assertTrue( "Number of service choices <" + ss.getSize() + "> not as expected <" + 1 + ">.",
//                ss.getSize() == 1 );
//    assertTrue( "DQC service selector id <" + ss.getId() + "> not as expected <" + "service" + ">.",
//                ss.getId().equals( "service") );
//    assertTrue( "DQC service selector title <" + ss.getTitle() + "> not as expected <" + "Select service type." + ">.",
//                ss.getTitle().equals( "Select service type.") );
//    assertTrue( "DQC service selector required <" + ss.isRequired() + "> not as expected <" + false + ">.",
//                ! ss.isRequired());
//    assertTrue( "Number of DQC service selectors <" + ss.getSize() + "> not one.",
//                ss.getSize() == 1 );
//    SelectService.ServiceChoice choice = (SelectService.ServiceChoice) ss.getChoices().get( 0);
//    assertTrue( "ServiceChoice title <" + choice.getTitle() + "> not as expected <" + "OPeNDAP/DODS" + ">.",
//                choice.getTitle().equals(  "OPeNDAP/DODS") );
//    assertTrue( "ServiceChoice value <" + choice.getValue() + "> not as expected <" + "OpenDAP" + ">.",
//                choice.getValue().equals( "OpenDAP") );
//
//    List list =  dqc.getSelectors();
//    assertTrue( "Number of selectors (including service selector) <" + list.size() + "> is not 2.",
//                list.size() == 2 );
//    Selector s = null;
//    SelectList sl = null;
//    Iterator modelIt = null;
//    ListChoice curModel = null;
//    String curModelName = null;
//    String curModelValue = null;
//    for ( int i=0; i<2; i++ )
//    {
//      s = (Selector) list.get( i);
//      if ( s instanceof SelectService ) continue;  // dealt with this above
//      else if ( s instanceof SelectList )
//      {
//        sl = (SelectList) s;
//        assertTrue( "SelectList Id <" + sl.getId() + "> not as expected <" + "models" + ">.",
//                    sl.getId().equals( "models") );
//        assertTrue( "SelectList title <" + sl.getTitle() + "> not as expected <" + "Model name" + ">.",
//                    sl.getTitle().equals( "Model name"));
//        assertTrue( "Number of items in SelectList <" + sl.getSize() + "> not expected number of models <" + me.getNumberModels() + ">.",
//                    sl.getSize() == me.getNumberModels() );
//        modelIt = sl.getChoices().iterator();
//        while ( modelIt.hasNext() )
//        {
//          curModel = (ListChoice) modelIt.next();
//
//          curModelName = curModel.getName();
//          curModelValue = curModel.getValue();
//          assertTrue( "The value of the \"" + curModelName + "\" model <" + me.getModelValue( curModelName ) + "> not as expected <" + curModelValue + ">.",
//                      me.getModelValue( curModelName ).equals( curModelValue) );
//        }
//      }
//      else
//      {
//        assertTrue( "The Class of the non-SelectService selector <" + s.getClass().getName() + "> not a SelectList.",
//                    false);
//      }
//    }
//  }
//
//  public void testCatalogCreation()
//  {
//    log.debug( "testCatalogCreation(): start." );
//
//    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( true );
//    String catalogAsString = null;
//
//    // Create catalog for the latest dataset
//    InvCatalogImpl catalog = me.createCatalog( "testCatName", "testDsName", "DODS",
//                                               "mlode", serviceBaseURL, "testDs.nc" );
//
//    // Test writing as InvCatalog v0.6 with catalog name.
//    try
//    {
//      catalogAsString = fac.writeXML_0_6( catalog );
//    }
//    catch ( IOException e )
//    {
//      assertTrue( "Unexpected IOException writing catalog (as 0.6) to string.",
//                  false);
//    }
//    System.out.println( "========== Catalog 0.6 (with catalog name) ==========" );
//    System.out.print( catalogAsString );
//
//    // Test writing as InvCatalog v1.0 with catalog name.
//    try
//    {
//      catalogAsString = fac.writeXML_1_0( catalog );
//    }
//    catch ( IOException e )
//    {
//      assertTrue( "Unexpected IOException writing catalog (as 1.0) to string.",
//                  false );
//    }
//    System.out.println( "========== Catalog 1.0 (with catalog name) ==========" );
//    System.out.print( catalogAsString );
//
//    // Test writing as InvCatalog v1.0 with null catalog name.
//    catalog = me.createCatalog( null, "testDsName", "DODS",
//                                "mlode", serviceBaseURL, "testDs.nc" );
//    try
//    {
//      InvCatalogFactory10 fac10 = (InvCatalogFactory10) fac.getCatalogConverter( XMLEntityResolver.CATALOG_NAMESPACE_10 );
//      fac10.setVersion( this.invCatSpecVersion );
//      ByteArrayOutputStream osCat = new ByteArrayOutputStream( 10000 );
//      fac10.writeXML( catalog, osCat );
//      catalogAsString = osCat.toString();
//    }
//    catch ( IOException e )
//    {
//      assertTrue( "Unexpected IOException writing catalog (as 1.0) to string.",
//                  false );
//    }
//    System.out.println( "========== Catalog 1.0 (with null catalog name) ==========" );
//    System.out.print( catalogAsString );
//
//  }

}

/*
 * $Log: TestLatestModel.java,v $
 * Revision 1.5  2006/01/20 20:42:06  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.4  2005/10/03 22:35:41  edavis
 * Minor fixes for LatestDqcHandler.
 *
 * Revision 1.3  2005/08/23 23:00:52  edavis
 * Allow override of default output catalog version "1.0.1" to "1.0". This allows existing
 * IDV (which reads catalog version as float) to read InvCatalog 1.0.1 catalogs.
 *
 * Revision 1.2  2005/08/22 19:39:13  edavis
 * Changes to switch /thredds/dqcServlet URLs to /thredds/dqc.
 * Expand testing for server installations: TestServerSiteFirstInstall
 * and TestServerSite. Fix problem with compound services breaking
 * the filtering of datasets.
 *
 * Revision 1.1  2005/03/30 05:41:21  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.3  2004/08/24 23:46:09  edavis
 * Update for DqcServlet version 0.3.
 *
 * Revision 1.2  2004/08/23 16:45:18  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.1  2004/04/05 18:37:33  edavis
 * Added to and updated existing DqcServlet test suite.
 *
 */