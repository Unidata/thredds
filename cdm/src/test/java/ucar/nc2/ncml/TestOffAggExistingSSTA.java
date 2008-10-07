// $Id: $
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

package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.io.RandomAccessFile;

public class TestOffAggExistingSSTA extends TestCase {

  public TestOffAggExistingSSTA( String name) {
    super(name);
  }

  String ncml =
    "<?xml version='1.0' encoding='UTF-8'?>\n" +
    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
    "    <aggregation dimName='time' type='joinExisting' recheckEvery='15 min'>\n" +
    "      <variableAgg name='ATssta' />\n" +
    "      <scan dateFormatMark='AT#yyyyDDD_HHmmss' location='//zero/share/testdata/ncml/nc/pfeg/' suffix='.nc' />\n" +
    "    </aggregation>\n" +
    "</netcdf>";

  public void testSSTA() throws IOException, InvalidRangeException {
    String filename = "file:"+TestNcML.topDir + "offsite/aggExistingSSTA.xml";

    RandomAccessFile.setDebugLeaks( true);
    List<String> openfiles = RandomAccessFile.getOpenFiles();
    int count = openfiles.size();
    System.out.println("count files="+count);

    NetcdfDataset.disableNetcdfFileCache();
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null);
    System.out.println(" TestNcmlAggExisting.open "+ filename);
    //System.out.println(" "+ncfile);

    Array ATssta = ncfile.readSection("ATssta(:,0,0,0)");

    int count1 = RandomAccessFile.getOpenFiles().size();
    System.out.println("count files="+count1);

    ncfile.close();

    int count2 = RandomAccessFile.getOpenFiles().size();
    System.out.println("count files="+count2);
    assert count == count2 : "openFile count "+count +"!="+ count2;

  }
}
