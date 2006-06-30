// $Id: TestDLwriter.java,v 1.1 2006/05/08 02:47:21 caron Exp $
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

package thredds.tds;

import junit.framework.*;
import java.io.IOException;

public class TestDLwriter extends TestCase {

  public TestDLwriter( String name) {
    super(name);
  }

  public void testDLwriter() throws IOException {
    String url = "/DLwriter?type=ADN&catalog=/thredds/catalog/testEnhanced/catalog.xml";

    System.out.println("Response from "+TestTDSAll.topCatalog+url);
    String result = thredds.util.IO.readURLcontents(TestTDSAll.topCatalog+url);
    System.out.println(result);
  }
}
