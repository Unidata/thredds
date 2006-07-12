// $Id$

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

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class TestDatasetNamerType extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestDatasetNamerType.class);

  private String typeName1;
  private String typeName2;

  public TestDatasetNamerType( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    typeName1 = "RegExp"; // REGULAR_EXPRESSION
    typeName2 = "DodsAttrib"; // DODS_ATTRIBUTE
  }

//  protected void tearDown()
//  {
//  }

  public void testDatasetNamerType()
  {
    assertTrue( DatasetNamerType.REGULAR_EXPRESSION.toString().equals( typeName1));
    assertTrue( DatasetNamerType.REGULAR_EXPRESSION.equals( DatasetNamerType.getType( typeName1)));

    assertTrue( DatasetNamerType.DODS_ATTRIBUTE.toString().equals( typeName2));
    assertTrue( DatasetNamerType.DODS_ATTRIBUTE.equals( DatasetNamerType.getType( typeName2)));
  }

}

/*
 * $Log: TestDatasetNamerType.java,v $
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
 * Revision 1.2  2004/05/11 16:29:07  edavis
 * Updated to work with new thredds.catalog 0.6 stuff and the THREDDS
 * servlet framework.
 *
 * Revision 1.1  2003/08/20 17:23:42  edavis
 * Initial version.
 *
 */