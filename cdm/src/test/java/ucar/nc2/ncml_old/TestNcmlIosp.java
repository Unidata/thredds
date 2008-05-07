// $Id: $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2.ncml_old;

import junit.framework.TestCase;

import java.io.IOException;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;

// test iosp and iosp param
public class TestNcmlIosp extends TestCase {

  public TestNcmlIosp( String name) {
    super(name);
  }

  public void testIOSP() throws IOException, InvalidRangeException {
    String filename = "file:"+TestNcML.topDir + "iosp.ncml";

    NetcdfFile ncfile = new NcMLReader().readNcML(filename, null);
    System.out.println(" testIOSP.open "+ filename);
    System.out.println(" "+ncfile);

    ncfile.close();
  }
}