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
package ucar.nc2.dataset;

import org.junit.Ignore;
import org.junit.Test;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.NetcdfFile;
import ucar.nc2.iosp.bufr.BufrIosp2;
import ucar.ma2.StructureData;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.ArrayStructure;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Test that nested structures get enhanced.
 * @author caron
 * @since Jul 5, 2008
 */
public class TestNestedStructuresEnhancement {

  @Ignore("cant deal with BUFR at the moment")
  @Test
  public void testNestedTable() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmLocalTestDataDir + "dataset/nestedTable.bufr";
    NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDataset.openFile(filename, null);
    System.out.printf("Open %s%n", ncfile.getLocation());
    Sequence outer = (Sequence) ncfile.findVariable(BufrIosp2.obsRecord);
    assert outer != null;

    StructureDataIterator iter = outer.getStructureIterator();
    StructureData data = null;
    if (iter.hasNext())
      data = iter.next();

    assert data != null;
    assert data.getScalarShort("Latitude_coarse_accuracy") == 32767;

    ArrayStructure as = data.getArrayStructure("Geopotential");
    assert as != null;
    assert as.getScalarShort(0, as.findMember("Wind_speed")) == 61;

    iter.finish();
    ncfile.close();
  }

  @Ignore("cant deal with BUFR at the moment")
  @Test
  public void testNestedTableEnhanced() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmLocalTestDataDir + "dataset/nestedTable.bufr";
    NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    System.out.printf("Open %s%n", ncfile.getLocation());
    SequenceDS outer = (SequenceDS) ncfile.findVariable(BufrIosp2.obsRecord);
    assert outer != null;

    StructureDataIterator iter = outer.getStructureIterator();
    StructureData data = null;
    if (iter.hasNext())
      data = iter.next();

    assert data != null;
    assert Double.isNaN( data.getScalarFloat("Latitude_coarse_accuracy"));

    ArrayStructure as = data.getArrayStructure("Geopotential");
    assert as != null;
    assert Misc.closeEnough(as.getScalarFloat(0, as.findMember("Wind_speed")), 6.1);

    iter.finish();
    ncfile.close();
  }

}
