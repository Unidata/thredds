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

import ucar.nc2.constants.FeatureType;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.bufr.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.SequenceDS;
import ucar.nc2.util.Indent;
import ucar.nc2.NetcdfFile;
import ucar.ma2.*;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.*;

/**
 * reads a file with BUFR messages in it and prints out the data values.
 *
 * @author caron
 * @since Dec 17, 2009
 */
public class Bufr2nc {

  private Formatter out;

  private StandardFields.Extract standardFields;


  /**
   * Open file as a stream of BUFR messages and print data
   *
   * @param filename open this file
   * @param out       print to here
   * @throws IOException on IO error
   */
  public Bufr2nc(String filename, Formatter out) throws IOException {
    this.out = out;
    out.format("Dumdp %s%n", filename);

    try {
      scanBufrFile(filename);

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  // open the file and extract BUFR messages

  public void scanBufrFile(String filename) throws Exception {
    int count = 0;
    RandomAccessFile raf = null;
    int messHash = 0;

    try {
      raf = new RandomAccessFile(filename, "r");

      MessageScanner scan = new MessageScanner(raf);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) continue;
        if (m.containsBufrTable()) continue;
        if (count == 0) {
          messHash = m.hashCode();
          makeFieldConverter(m);
        }

        processBufrMessageAsDataset(raf, m);
        count++;
      }
    } finally {
      if (raf != null)
        raf.close();
    }

    // show the results
    Indent indent = new Indent(2);
    out.format("<bufr2nc location='%s' hash='%s' featureType='%s'>%n", filename, Integer.toHexString(messHash),
            guessFeatureType());
    indent.incr();
    for (FieldConverter fld : rootConvert.flds) {
      fld.show(out, indent);
    }
    indent.decr();
    out.format("</bufr2nc>%n");
  }

  private void processBufrMessageAsDataset(RandomAccessFile raf, Message m) throws Exception {
    NetcdfDataset ncd = getBufrMessageAsDataset(raf, m);
    SequenceDS obs = (SequenceDS) ncd.findVariable(BufrIosp.obsRecord);
    StructureDataIterator sdataIter = obs.getStructureIterator(-1);
    //writeSequence(obs, sdataIter);
    processSeq(sdataIter, rootConvert);
  }

  // convert one message ino a NetcdfDataset and print data
  private NetcdfDataset getBufrMessageAsDataset(RandomAccessFile raf, Message m) throws IOException {
    BufrIosp iosp = new BufrIosp();
    BufrNetcdf ncfile = new BufrNetcdf(iosp, raf.getLocation());
    iosp.open(raf, ncfile, m);
    return new NetcdfDataset(ncfile);
  }

  private class BufrNetcdf extends NetcdfFile {
    protected BufrNetcdf(IOServiceProvider spi, String location) throws IOException {
      super(spi, location);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////

  private FieldConverter rootConvert;

  private class FieldConverter {
    DataDescriptor dds;
    List<FieldConverter> flds;
    StandardFields.Type type;
    int min = Integer.MAX_VALUE;
    int max = 0;
    boolean isSeq;

    private FieldConverter(DataDescriptor dds) {
      this.dds = dds;
      if (dds.getSubKeys() != null)
        this.flds = new ArrayList<FieldConverter>(dds.getSubKeys().size());
      this.type = StandardFields.findStandardField(dds.getFxyName());
    }

    FieldConverter findChild(StructureMembers.Member m) {
      for (FieldConverter child : flds) {
        String name = child.dds.getName();
        if (name.equals(m.getName()))
          return child;
      }
      return null;
    }

    void trackSeqCounts(int n) {
      isSeq = true;
      if (n > max) max = n;
      if (n < min) min = n;
    }

    void showRange(Formatter f) {
      if (!isSeq) return;
      if (max == min) f.format(" isConstant='%d'", max);
      else if (max < 2) f.format(" isBinary='true'");
      else f.format(" range='[%d,%d]'", min, max);
    }

    String makeAction() {
      if (!isSeq) return "";
      if (max == 0) return "remove";
      if (max < 2) return "asMissing";
      if (max == min) return "asArray";
      else return "";
    }

    void show(Formatter f, Indent indent) {
      boolean hasContent = false;
      if (isSeq)
        f.format("%s<fld name='%s'", indent, dds.getName());
      else
        f.format("%s<fld fxy='%s' name='%s' desc='%s' units='%s' bits='%d'", indent, dds.getFxyName(), dds.getName(), dds.getDesc(), dds.getUnits(), dds.getBitWidth());

      showRange(f);
      f.format(" action='%s'", makeAction());

      if (type != null) {
        f.format(">%n");
        indent.incr();
        f.format("%s<type>%s</type>%n", indent, type);
        indent.decr();
        hasContent = true;
      }

      if (flds != null) {
        f.format(">%n");
        indent.incr();
        for (FieldConverter cc : flds) {
          cc.show(f, indent);
        }
        indent.decr();
        hasContent = true;
      }

      if (hasContent)
        f.format("%s</fld>%n", indent);
      else
        f.format(" />%n");
    }
  }

  private void makeFieldConverter( Message m) throws IOException {
    standardFields = StandardFields.extract(m);
    out.format("Extract Standard Fields%n%s%n", standardFields);

    rootConvert = new FieldConverter(m.getRootDataDescriptor());
    makeFieldConverter(m.getRootDataDescriptor(), rootConvert);
  }

  private void makeFieldConverter(DataDescriptor dds, FieldConverter convert) {

    for (DataDescriptor subdds : dds.getSubKeys()) {
      FieldConverter subfld = new FieldConverter(subdds);
      convert.flds.add(subfld);

      if (subdds.getSubKeys() != null) {
        makeFieldConverter(subdds, subfld);
      }
    }
  }

  FeatureType guessFeatureType() {
    if (standardFields.hasStation()) return FeatureType.STATION;
    if (standardFields.hasTime()) return FeatureType.POINT;
    return FeatureType.NONE;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  // iterate through the observations
  private void processSeq(StructureDataIterator sdataIter, FieldConverter parent) throws IOException {
    try {
      while (sdataIter.hasNext()) {
        StructureData sdata = sdataIter.next();

        for (StructureMembers.Member m : sdata.getMembers()) {
          if (m.getDataType() == DataType.SEQUENCE) {
            FieldConverter seq = parent.findChild(m);
            if (seq == null) {
              System.err.printf("cant find Child %s%n", m);
              continue;
            }
            ArraySequence data = (ArraySequence) sdata.getArray(m);
            int n = data.getStructureDataCount();
            seq.trackSeqCounts(n);
            processSeq(data.getStructureDataIterator(), seq);
          }
        }
      }
    } finally {
      sdataIter.finish();
    }
  }




  /* iterate through the observations

  private void processNested(StructureDataIterator sdataIter, SeqExtract result) throws IOException {
    try {
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
    } finally {
      sdataIter.finish();
    }
  }  */

  /* iterate through the observations

  private void writeSequence(StructureDS s, StructureDataIterator sdataIter) throws IOException {
    indent.incr();
    int count = 0;
    try {
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
    } finally {
      sdataIter.finish();
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
  }  */

  public static void main(String[] args) throws IOException {
    Formatter out = new Formatter();
    new Bufr2nc("G:/work/manross/split/872d794d.bufr", out);
    System.out.printf("Result=%n%s%n", out);
  }

}
