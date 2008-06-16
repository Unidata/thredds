/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2;

import junit.framework.TestCase;

import java.io.IOException;

import ucar.ma2.*;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 16, 2008
 */
public class TestWriteMiscProblems extends TestCase {
  private boolean show = false;

  public TestWriteMiscProblems( String name) {
    super(name);
  }

  public void testWriteBigString() throws IOException {
    String filename = TestLocal.cdmTestDataDir +"testWriteMisc.nc";
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename, false);

    int len = 120000;
    ArrayChar.D1 arrayCharD1 = new ArrayChar.D1(len);
    for (int i=0; i<len; i++)
      arrayCharD1.set(i,'1');
    ncfile.addGlobalAttribute("tooLongChar", arrayCharD1);
    
    char[] carray = new char[len];
    for (int i=0; i<len; i++)
      carray[i] = '2';
    String val = new String(carray);
    ncfile.addGlobalAttribute("tooLongString", val);


    ncfile.create();
    ncfile.close();
  }

}
