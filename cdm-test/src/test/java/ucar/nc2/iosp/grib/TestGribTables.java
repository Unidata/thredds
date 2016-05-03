/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.grib;

import org.junit.*;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib1.tables.Grib1ParamTableReader;
import ucar.nc2.grib.grib2.table.Grib2Table;
import ucar.nc2.grib.grib2.table.KmaLocalTables;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;

/**
 * Description
 *
 * @author John
 * @since 12/18/2014
 */
public class TestGribTables {

  @Test
  public void testKmaTable() {
    Grib2Table.Id id = new Grib2Table.Id(40,-1,-1,-1,-1);
    Grib2Table table = Grib2Table.getTable(id);
    KmaLocalTables kma = KmaLocalTables.getCust(table);
    assert kma != null;
    assert kma.getParameters().size() > 0;
    for (GribTables.Parameter p : kma.getParameters())
      System.out.printf("%s%n", p);
  }

  @Test
  public void testNclParameterTable() throws IOException {
    String dirS = "../grib/src/main/resources/resources/grib1/ncl";
    File dir = new File(dirS);
    assert (dir.listFiles() != null);
    for (File f : dir.listFiles()) {
      if (!f.getName().endsWith(".h")) continue;
      Grib1ParamTableReader table = new Grib1ParamTableReader(f.getPath());
      //  60:	 1:		180:	WMO_GRIB1.60-1.180.xml
      System.out.printf("%5d: %5d: %5d: %s%n", table.getCenter_id(), table.getSubcenter_id(), table.getVersion(), table.getName());
    }
  }

}
