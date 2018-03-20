/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.bufr.writer;

import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.iosp.bufr.BufrIosp2;
import ucar.nc2.iosp.bufr.MessageScanner;
import ucar.nc2.iosp.bufr.BufrNumbers;
import ucar.nc2.util.Indent;
import ucar.nc2.Variable;
import ucar.nc2.*;
import ucar.ma2.*;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;

/**
 * reads a file with BUFR messages in it and prints out the data values.
 *
 * @author caron
 * @since Dec 17, 2009
 */
public class BufrDataProcess {

  private PrintStream out;
  private Indent indent = new Indent(2);
  private boolean showData = false;
  private boolean showMess = false;
  private boolean showFile = true;

  public BufrDataProcess(String filename, OutputStream os, FileFilter ff) throws IOException {
    File f = new File(filename);
    if (!f.exists()) {
      System.out.println(filename + " does not exist");
      return;
    }

    if (f.isDirectory()) {
      Counter gtotal = new Counter();
      int nmess = processAllInDir(f, os, ff, gtotal);
      out.format("%nGrand Total nmess=%d count=%d miss=%d %f %% %n", nmess, gtotal.nvals, gtotal.nmiss, gtotal.percent());
    } else {
      processOneFile(f.getPath(), os, null);
    }
  }

  public int processAllInDir(File dir, OutputStream os, FileFilter ff, Counter gtotal) throws IOException {
    int nmess = 0;

    System.out.println("---------------Reading directory " + dir.getPath());
    File[] allFiles = dir.listFiles();

    if (allFiles == null)
        throw new IOException("Error reading " + dir.getPath());

    for (File f : allFiles) {
      if (f.isDirectory())
        continue;
      if ((ff == null) || ff.accept(f)) {
        nmess += processOneFile(f.getPath(), os, gtotal);
      }
    }

    for (File f : allFiles) {
      if (f.isDirectory())
        nmess += processAllInDir(f, os, ff, gtotal);
    }

    return nmess;
  }

  int processOneFile(String filename, OutputStream os, Counter gtotal) throws IOException {
    out = new PrintStream(os, false, CDM.utf8Charset.name());
    if (showFile) out.format("Process %s%n", filename);
    indent.setIndentLevel(0);
    int nmess;

    try {
      Counter total = new Counter();
      nmess = scanBufrFile(filename, total);
      if (showFile)
        out.format("%nTotal nmess=%d count=%d miss=%d %f %% %n", nmess, total.nvals, total.nmiss, total.percent());
      if (gtotal != null) gtotal.add(total);

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }

    out.flush();
    return nmess;
  }

  // open the file and extract BUFR messages

  public int scanBufrFile(String filename, Counter total) throws Exception {
    int count = 0;
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {

      MessageScanner scan = new MessageScanner(raf);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) continue;
        try {
          if (showMess) out.format("%sMessage %d header=%s%n", indent, count, m.getHeader());
          count++;
          Counter counter = new Counter();
          processBufrMessageAsDataset(scan, m, counter);
          if (showMess) out.format("%scount=%d miss=%d%n", indent, counter.nvals, counter.nmiss);
          total.add(counter);

        } catch (Exception e) {
          System.out.printf("  BARF:%s on %s%n", e.getMessage(), m.getHeader());
          indent.setIndentLevel(0);
        }
      }
    }
    return count;
  }

  // convert one message ino a NetcdfDataset and print data

  private void processBufrMessageAsDataset(MessageScanner scan, Message m, Counter counter) throws Exception {
    byte[] mbytes = scan.getMessageBytes(m);
    NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes, "ucar.nc2.iosp.bufr.BufrIosp");
    Sequence obs = (Sequence) ncfile.findVariable(BufrIosp2.obsRecord);
    StructureDataIterator sdataIter = obs.getStructureIterator(-1);
    processSequence(obs, sdataIter, counter);
  }

  // iterate through the observations

  private void processSequence(Structure s, StructureDataIterator sdataIter, Counter counter) throws IOException {
    indent.incr();
    int count = 0;
    try {
      while (sdataIter.hasNext()) {
        if (showData) out.format("%sSequence %s count=%d%n", indent, s.getShortName(), count++);
        StructureData sdata = sdataIter.next();
        indent.incr();

        for (StructureMembers.Member m : sdata.getMembers()) {
          Variable v = s.findVariable(m.getName());

          if (m.getDataType().isString() || m.getDataType().isNumeric()) {
            processVariable(v, sdata.getArray(m), counter);

          } else if (m.getDataType() == DataType.STRUCTURE) {
            Structure sds = (Structure) v;
            ArrayStructure data = (ArrayStructure) sdata.getArray(m);
            processSequence(sds, data.getStructureDataIterator(), counter);

          } else if (m.getDataType() == DataType.SEQUENCE) {
            Sequence sds = (Sequence) v;
            ArraySequence data = (ArraySequence) sdata.getArray(m);
            processSequence(sds, data.getStructureDataIterator(), counter);
          }
        }
        indent.decr();
      }
    } finally {
      sdataIter.close();
    }

    indent.decr();
  }

  private void processVariable(Variable v, Array mdata, Counter count) throws IOException {
    String name = v.getShortName();
    String units = v.getUnitsString();
    Attribute bwAtt = v.findAttribute("BUFR:bitWidth");
    int bitWidth = bwAtt == null ? 0 : bwAtt.getNumericValue().intValue();

    if (showData) out.format("%svar='%s' units='%s' : ", indent, name, units);

    mdata.resetLocalIterator();
    while (mdata.hasNext()) {
      count.nvals++;
      if (v.getDataType().isUnsigned()) {
        if (isMissingUnsigned(v, mdata, bitWidth)) count.nmiss++;
      } else {
        if (isMissing(v, mdata, bitWidth)) count.nmiss++;
      }
    }
    if (showData) out.format("%n");
  }

  private boolean isMissing(Variable v, Array mdata, int bitWidth) {

    if (v.getDataType().isNumeric()) {
      long val = mdata.nextLong();
      boolean result = BufrNumbers.isMissing(val, bitWidth);
      if (showData) out.format("%d %s,", val, result ? "(miss)" : "");
      return result;
    }

    Object s = mdata.next();
    if (showData) out.format("%s,", s);
    return false;
  }

  private boolean isMissingUnsigned(Variable v, Array mdata, int bitWidth) {
    long val;
    switch (v.getDataType()) {
      case ENUM1:
      case BYTE:
        val = DataType.unsignedByteToShort(mdata.nextByte());
        break;
      case ENUM2:
      case SHORT:
        val = DataType.unsignedShortToInt(mdata.nextShort());
        break;
      case ENUM4:
      case INT:
        val = DataType.unsignedIntToLong(mdata.nextInt());
        break;
      default:
        throw new RuntimeException("illegal datatype " + v.getDataType());
    }

    boolean result = BufrNumbers.isMissing(val, bitWidth);
    if (showData) out.format("%d %s,", val, result ? "(miss)" : "");
    return result;
  }

  private static class Counter {
    int nvals;
    int nmiss;

    void add(Counter c) {
      nvals += c.nvals;
      nmiss += c.nmiss;
    }

    double percent() {
      return 100.0 * nmiss / nvals;
    }
  }

  public static void main(String[] args) throws IOException {
    new BufrDataProcess("C:/data/formats/bufrRoy/", System.out, null);
    //new BufrDataDump("D:/work/michelle/DART.bufr", System.out);
  }

}
