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

package ucar.nc2.iosp.bufr;

import junit.framework.TestCase;

import java.io.IOException;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.*;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 18, 2008
 */
public class TestReadIDD extends TestCase {

  public void testReadSamples() throws IOException {
    String location = TestAll.testdataDir + "bufr/sample.bufr";
    RandomAccessFile raf = new RandomAccessFile(location, "r");
    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      if (!m.isBitCountOk()) {
        System.out.println(" skip " + m.getHeader());
        continue;
      }
      System.out.println(" read " + m.getHeader());

      NetcdfDataset ncd = null;
      try {
        byte[] mbytes = scan.getMessageBytes(m);
        NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes);
        ncd = new NetcdfDataset(ncfile);
        Structure s = (Structure) ncd.findVariable(BufrIosp.obsRecord);
        assert s != null;
        readAll(s);

      } finally {
        if (ncd != null) ncd.close();
      }

      count++;
    }
  }

  private void readAll(Structure s) throws IOException {
    ArrayStructure data = (ArrayStructure) s.read();

    for (Variable v : s.getVariables()) {
      //System.out.println("  check "+v.getName()+" "+v.getDataType());

      if (v.getDataType() == DataType.SEQUENCE) {
        System.out.println("  *** seq "+v.getDataType()+" "+v.getName());
        for (int recno=0; recno<data.getSize(); recno++) {
          ArraySequence seq = data.getArraySequence(recno, data.findMember(v.getShortName()));
          StructureDataIterator iter = seq.getStructureDataIterator();
          while (iter.hasNext()) {
            StructureData sdata = iter.next();
          }
        }
      }
    }

  }
}



