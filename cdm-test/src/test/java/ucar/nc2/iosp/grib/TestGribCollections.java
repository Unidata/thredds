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

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Description
 *
 * @author John
 * @since 10/13/2014
 */
public class TestGribCollections {

  @Test
  public void testGC() throws IOException {
    read("F:/data/grib/idd/dgex/20131204/DGEX-test-20131204-20131204-060000.ncx2");
  }


  private void read(String filename) throws IOException {
    System.out.println("\n\nReading File " + filename);
    try (GridDataset gds = GridDataset.open(filename)) {
      for (GridDatatype gdt: gds.getGrids()) {
        Dimension rtDim = gdt.getRunTimeDimension();
        Dimension tDim = gdt.getTimeDimension();
        Dimension zDim = gdt.getZDimension();

        Count count = new Count();
        if (rtDim != null) {
          for (int rt=0; rt<rtDim.getLength(); rt++)
            read(gdt, count, rt, tDim, zDim);
        } else {
          read(gdt, count, -1, tDim, zDim);
        }
        System.out.printf("%50s == %d/%d%n", gdt.getFullName(), count.nmiss, count.nread);
      }
    }
  }

  private void read(GridDatatype gdt, Count count, int rtIndex, Dimension timeDim, Dimension zDim) throws IOException {
    if (timeDim != null) {
      for (int t=0; t<timeDim.getLength(); t++)
        read(gdt, count, rtIndex, t, zDim);
    } else {
      read(gdt, count, -1, -1, -1);
    }
  }


  private void read(GridDatatype gdt, Count count, int rtIndex, int tIndex, Dimension zDim) throws IOException {
    if (zDim != null) {
      for (int y=0; y<zDim.getLength(); y++)
        read(gdt, count, rtIndex, tIndex, y);
    } else {
      read(gdt, count, -1, -1, -1);
    }
  }

  private void read(GridDatatype gdt, Count count, int rtIndex, int tIndex, int zIndex) throws IOException {
    // int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index
    Array data = gdt.readDataSlice(rtIndex, -1, tIndex, zIndex, 10, 10);
    if (data.getSize() != 1)
      System.out.printf("%s size=%d rank=%d%n", gdt.getFullName(), data.getSize(), data.getRank());
    assert data.getSize() == 1 : gdt.getFullName() +" size = "+data.getSize();
    boolean ok = data.hasNext();
    assert ok;
    float val = data.nextFloat();
    //System.out.printf("%s size=%d rank=%d val=%f%n", gdt.getFullName(), data.getSize(), data.getRank(), val);
    if (Float.isNaN(val))
      count.nmiss++;
    count.nread++;
  }

  private class Count {
    int nread;
    int nmiss;
  }
}
