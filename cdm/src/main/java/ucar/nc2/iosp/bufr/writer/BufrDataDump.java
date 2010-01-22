/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.bufr.writer;

import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.iosp.bufr.BufrIosp;
import ucar.nc2.iosp.bufr.MessageScanner;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.SequenceDS;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.util.Indent;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.ma2.*;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * reads a file with BUFR messages in it and prints out the data values.
 *
 * @author caron
 * @since Dec 17, 2009
 */
public class BufrDataDump {

  private PrintStream out;
  private Indent indent = new Indent(2);

  /**
   * Open file as a stream of BUFR messages and print data
   *
   * @param filename open this file
   * @param os       print to here
   * @throws IOException on IO error
   */
  public BufrDataDump(String filename, OutputStream os) throws IOException {
    out = new PrintStream(os);
    out.format("Dump %s%n", filename);
    indent.setIndentLevel(0);

    try {
      scanBufrFile(filename);

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }

    out.flush();

  }

  // open the file and extract BUFR messages
  public void scanBufrFile(String filename) throws Exception {
    int count = 0;
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(filename, "r");

      MessageScanner scan = new MessageScanner(raf);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) continue;
        out.format("%sMessage %d header=%s%n", indent, count++, m.getHeader());
        processBufrMessageAsDataset(scan, m);
      }
    } finally {
      if (raf != null)
        raf.close();
    }
  }

  // convert one message ino a NetcdfDataset and print data
  private void processBufrMessageAsDataset(MessageScanner scan, Message m) throws Exception {
    byte[] mbytes = scan.getMessageBytes(m);
    NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes, "ucar.nc2.iosp.bufr.BufrIosp");
    NetcdfDataset ncd = new NetcdfDataset(ncfile);
    SequenceDS obs = (SequenceDS) ncd.findVariable(BufrIosp.obsRecord);
    StructureDataIterator sdataIter = obs.getStructureIterator(-1);
    //writeSequence(obs, sdataIter);
    extractFirst(sdataIter, new Extract());
  }

  private class Extract {
    double platformId;
    int year,month,day,hour,min,sec,incr,incrS;
    Array value;

    @Override
    public String toString() {
      return "Extract{" +
              "platformId=" + platformId +
              ", year=" + year +
              ", month=" + month +
              ", day=" + day +
              ", hour=" + hour +
              ", min=" + min +
              ", sec=" + sec +
              ", incr=" + incr +
              ", incrS=" + incrS +
              ", value=" + value +
              '}';
    }
  }

  // iterate through the observations
private void extractFirst(StructureDataIterator sdataIter, Extract result) throws IOException {
  while (sdataIter.hasNext()) {
    StructureData sdata = sdataIter.next();

    for (StructureMembers.Member m : sdata.getMembers()) {
      if (m.getName().equals("Buoy/platform identifier"))
        result.platformId = sdata.convertScalarDouble(m);

      else if (m.getDataType() == DataType.SEQUENCE) {
        ArraySequence data = (ArraySequence) sdata.getArray(m);
        extractNested(data.getStructureDataIterator(), result);
      }
    }
  }
}

  // iterate through the observations
private void extractNested(StructureDataIterator sdataIter, Extract result) throws IOException {
  while (sdataIter.hasNext()) {
    StructureData sdata = sdataIter.next();

    for (StructureMembers.Member m : sdata.getMembers()) {
      if (m.getName().equals("Year"))
        result.year = sdata.convertScalarInt(m);
      else if (m.getName().equals("Month"))
        result.month = sdata.convertScalarInt(m);
      else if (m.getName().equals("Day"))
        result.day = sdata.convertScalarInt(m);
      else if (m.getName().equals("Hour"))
        result.hour = sdata.convertScalarInt(m);
      else if (m.getName().equals("Minute"))
        result.min = sdata.convertScalarInt(m);
      else if (m.getName().equals("Second"))
        result.sec = sdata.convertScalarInt(m);
      else if (m.getName().equals("Time increment"))
        result.incr = sdata.convertScalarInt(m);
      else if (m.getName().equals("Short time increment"))
        result.incrS = sdata.convertScalarInt(m);
      else if (m.getName().equals("Water column height")) {
        result.value = sdata.getArray(m);
        out.format("%s%n", result.toString());
      }
    }
  }
}

  // iterate through the observations
  private void writeSequence(StructureDS s, StructureDataIterator sdataIter) throws IOException {
    indent.incr();
    int count = 0;
    while (sdataIter.hasNext()) {
      out.format("%sSequence %s count=%d%n", indent, s.getShortName(), count++);
      StructureData sdata = sdataIter.next();
      indent.incr();

      for (StructureMembers.Member m : sdata.getMembers()) {
        Variable v = s.findVariable(m.getName());

        if (m.getDataType().isString() || m.getDataType().isNumeric()) {
          writeVariable((VariableDS) v, sdata.getArray(m));

        } else if (m.getDataType() == DataType.STRUCTURE) {
          StructureDS sds = (StructureDS) v;
          ArrayStructure data = (ArrayStructure) sdata.getArray(m);
          writeSequence(sds, data.getStructureDataIterator());

        } else if (m.getDataType() == DataType.SEQUENCE) {
          SequenceDS sds = (SequenceDS) v;
          ArraySequence data = (ArraySequence) sdata.getArray(m);
          writeSequence(sds, data.getStructureDataIterator());
        }
      }
      indent.decr();
    }

    indent.decr();
  }

  private void writeVariable(VariableDS v, Array mdata) throws IOException {
    int count = 0;
    String name = v.getShortName();
    String units = v.getUnitsString();
    out.format("%svar='%s' units='%s' : ", indent, name, units);

    mdata.resetLocalIterator();
    while (mdata.hasNext()) {
      if (count++ > 0) out.format(",");

      if (v.getDataType().isNumeric()) {

        double val = mdata.nextDouble();
        if (v.isMissing(val)) { // check if missing
          out.format("missing");
        } else {
          out.format("%s", Double.toString(val));
        }

      } else {  // not numeric
        out.format("%s", mdata.next());
      }
    }
    out.format("%n");
  }

  public static void main(String[] args) throws IOException {
    new BufrDataDump("D:/work/michelle/TimeIncr.bufr", System.out);
    //new BufrDataDump("D:/work/michelle/DART.bufr", System.out);
  }

}
