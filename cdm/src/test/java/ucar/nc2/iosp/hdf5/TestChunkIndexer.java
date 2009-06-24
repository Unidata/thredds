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
package ucar.nc2.iosp.hdf5;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.iosp.Indexer;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Group;
import ucar.nc2.util.CompareNetcdf;

import java.io.IOException;

public class TestChunkIndexer extends TestCase {

  public TestChunkIndexer(String name) {
    super(name);
  }

  public void test() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestH5.openH5("IASI/IASI.h5");
    Group root = ncfile.getRootGroup();
    Group g1 = root.findGroup("U-MARF");
    Group g2 = g1.findGroup("EPS");
    Group g3 = g2.findGroup("IASI_xxx_1C");
    Group g4 = g3.findGroup("DATA");
    Variable v2 = g4.findVariable("IMAGE_DATA");  // chunked short

    Array alData = v2.read();

    int[] origin = new int[]{46000, 122};
    int[] shape = new int[]{80, 12};
    Section section = new Section(origin, shape);

    Array data = v2.read(section); // force btree to be filled
    assert data.getSize() == section.computeSize();

    H5chunkLayout index = new H5chunkLayout(v2, v2.getDataType(), section);
    assert index.getTotalNelems() == section.computeSize();
    int count = 0;
    while (index.hasNext()) {
      Indexer.Chunk chunk = index.next();
      count++;
    }
  }

  public void testSection() throws InvalidRangeException, IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    NetcdfFile ncfile = TestH5.openH5("IASI/IASI_xxx_1C_M02_20070704193256Z_20070704211159Z_N_O_20070704211805Z.h5");
    Variable v2 = ncfile.findVariable("U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_DATA");  // chunked short
    assert v2 != null;

    Array allData = v2.read();
    System.out.println("allData.section ="+allData.shapeToString());

    int[] origin = new int[]{100, 100};
    int[] shape = new int[]{10, 300};
    Section section = new Section(origin, shape);
    Array dataSection = v2.read(section);
    System.out.println("dataSection.section ="+dataSection.shapeToString());
    CompareNetcdf.compareData(dataSection, allData.section( section.getRanges()));

    int[] origin2 = new int[]{100, 100};
    int[] shape2 = new int[]{130, 97};
    int[] stride2 = new int[]{3, 7};
    Section sectionStrided = new Section(origin2, shape2, stride2);
    Array dataSection2 = v2.read(sectionStrided);
    System.out.println("dataSection.sectionStrided ="+dataSection2.shapeToString());
    CompareNetcdf.compareData(dataSection2, allData.section( sectionStrided.getRanges()));
  }

  public void readSectionStrided() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestH5.openH5("IASI/IASI_xxx_1C_M02_20070704193256Z_20070704211159Z_N_O_20070704211805Z.h5");
    Variable v2 = ncfile.findVariable("/U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_DATA");  // chunked short
    assert v2 != null;

    int[] origin2 = new int[]{100, 100};
    int[] shape2 = new int[]{130, 97};
    int[] stride2 = new int[]{3, 7};
    Section sectionStrided = new Section(origin2, shape2, stride2);
    Array dataSection2 = v2.read(sectionStrided);
    System.out.println("dataSection.sectionStrided ="+dataSection2.shapeToString());
  }


  public void testa() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestH5.openH5("IASI/IASI.h5");
    Variable v2 = ncfile.findVariable("U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_DATA");  // chunked short
    assert v2 != null;

    long start = System.currentTimeMillis();
    Array data = v2.read();
    assert data.getElementType() == short.class;
    long took = System.currentTimeMillis() - start;
    double rate = 0.001* data.getSize() / took;
    System.out.println(" that took = "+took+" msec = "+rate+" Mb/sec ");
  }

  public void test2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestH5.openH5("support/uvlstr.h5");
    Group root = ncfile.getRootGroup();
    Variable v2 = root.findVariable("Space1"); // VLEN String
    assert v2 != null;

    Array data = v2.read();
    IndexIterator ii =data.getIndexIterator();
    while (ii.hasNext()) {
      String s = (String) ii.getObjectNext();
      System.out.println(s);
    }
    Section section = new Section().appendRange(new Range(1,9,3));
    Array dataSection = data.section(section.getRanges());
    ii = dataSection.getIndexIterator();

    Array data2 = v2.read(section);
    IndexIterator ii2 = data2.getIndexIterator();
    while (ii2.hasNext()) {
      String s = (String) ii2.getObjectNext();
      System.out.println(s);
      String ss = (String) ii.getObjectNext();
      assert (s.equals(ss));
    }
  }

  public void ntest3() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestH5.openH5("support/MSG1_8bit_HRV.H5");
    Group root = ncfile.getRootGroup();
    Group g = root.findGroup("image1");
    Variable v2 = g.findVariable("image_data");
    assert v2 != null;

    Array data = v2.read();
    assert data.getElementType() == byte.class;
  }
}
