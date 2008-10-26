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
    String location = TestAll.upcShareTestDataDir + "bufr/sample.bufr";
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
        Structure s = (Structure) ncd.findVariable("obsRecord");
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



