// $Id: TestResultService.java,v 1.3 2006/01/20 02:08:25 caron Exp $

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

import junit.framework.*;
import thredds.catalog.ServiceType;
import thredds.catalog.InvDatasetImpl;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class TestResultService extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestResultService.class);

  private String name;
  private String base;
  private String suffix;
  private ServiceType type = null;
  private InvDatasetImpl parent;
  private String accessPointHeader1;
  private String accessPointHeader2;

  private ResultService me = null;

  private StringBuffer out = null;

  public TestResultService( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    name = "service name";
    base = "http://www.unidata.ucar.edu/";
    suffix = "suffix";
    type = ServiceType.getType( "DODS");
    parent = new InvDatasetImpl( null, "parent dataset");

    accessPointHeader1 = "access point header1";
    accessPointHeader2 = "access point header2";

    me = new ResultService( name, type, base, suffix, accessPointHeader1 );

    out = new StringBuffer();
  }

//  protected void tearDown()
//  {
//  }

  // Some minor testing of InvServiceImpl because
  // not sure how much testing is done on InvServiceImpl.
  public void testInvServiceImpl()
  {
    // Test Service.getName()
    assertTrue( me.getName().equals( name ));

    // Test Service.getBase()
    assertTrue( me.getBase().equals( base ) );

    // Test Service.getServiceType()
    assertTrue( me.getServiceType().equals( type));

    // Test Service. getSuffix()
    assertTrue( me.getSuffix().equals( suffix));
  }

  public void testAccessPointHeader()
  {
    // Test ResultService.getAccessPointHeader()
    assertTrue( me.getAccessPointHeader().equals( accessPointHeader1));

    // Test ResultService.setAccessPointHeader( String)
    me.setAccessPointHeader( accessPointHeader2);
    assertTrue( me.getAccessPointHeader().equals( accessPointHeader2));
  }

  public void testIsValid()
  {
    boolean bool;

    // Test ResultService.validate()
    bool = me.validate( out);
    assertTrue( out.toString(), bool);

    // Test ResultService.validate() where accessPointHeader is empty string
    out = new StringBuffer();
    me.setAccessPointHeader( "");
    bool = me.validate( out);
    assertTrue( out.toString(), bool);

    // Test ResultService.validate() where accessPointHeader is null.
    out = new StringBuffer();
    me.setAccessPointHeader( null);
    bool = me.validate( out);
    assertFalse( out.toString(), bool);
  }

}

/*
 * $Log: TestResultService.java,v $
 * Revision 1.3  2006/01/20 02:08:25  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.2  2005/07/14 20:01:26  edavis
 * Make ID generation mandatory for datasetScan generated catalogs.
 * Also, remove log4j from some tests.
 *
 * Revision 1.1  2005/03/30 05:41:19  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.4  2004/12/15 17:51:03  edavis
 * Changes to clean up ResultService. Changes to add a server title to DirectoryScanner (becomes the title of the top-level dataset).
 *
 * Revision 1.3  2004/06/03 20:39:51  edavis
 * Added tests to check that CatGen config files are parsed correctly and
 * expanded catalogs are written correctly.
 *
 * Revision 1.2  2004/05/11 16:29:07  edavis
 * Updated to work with new thredds.catalog 0.6 stuff and the THREDDS
 * servlet framework.
 *
 * Revision 1.1  2003/08/20 17:23:43  edavis
 * Initial version.
 *
 */