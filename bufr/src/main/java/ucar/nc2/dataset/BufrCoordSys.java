package ucar.nc2.dataset;

import org.jdom2.Element;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.bufr.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Indent;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 8/12/13
 */
public class BufrCoordSys extends CoordSysBuilder {


  public static boolean isMine(NetcdfFile ncfile) {
    IOServiceProvider iosp = ncfile.getIosp();
    return iosp != null && iosp instanceof BufrIosp2;
  }

  // needed for ServiceLoader
  public BufrCoordSys() {
    this.conventionName = "BUFR/CDM";
  }

  public void augmentDataset(NetcdfDataset ncd, CancelTask cancelTask) throws IOException {
    BufrIosp2 iosp = (BufrIosp2) ncd.getIosp();
    BufrConfig config = iosp.getConfig();
    Formatter f = new Formatter();
    config.show(f);
    System.out.printf("%s%n", f);

    Element iospParam = iosp.getElem();
    if (iospParam != null)
      show(iospParam.getChild("bufr2nc", NcMLReader.ncNS ), new Indent(2));

    processSeq(ncd);
  }

  private void show(Element parent, Indent indent) {
    if (parent == null) return;
    for (Element child : parent.getChildren("fld", NcMLReader.ncNS)) {
      String idx = child.getAttributeValue("idx");
      String fxy = child.getAttributeValue("fxy");
      String name = child.getAttributeValue("name");
      String action = child.getAttributeValue("action");
      System.out.printf("%sidx='%s' fxy='%s' name='%s' action='%s'%n", indent, idx, fxy, name, action);
      indent.incr();
      show(child, indent);
      indent.decr();
    }
  }

  private void processSeq(NetcdfDataset ncd) throws IOException {
    SequenceDS obs = (SequenceDS) ncd.findVariable(BufrIosp2.obsRecord);
    Variable rv = obs.findVariable("BYTCNT");
    if (rv == null) System.out.printf("failed to remove %s%n", "BYTCNT");
    else obs.removeMemberVariable(rv);
  }

  /* private void processSeq(StructureDataIterator sdataIter, FieldConverter parent) throws IOException {
     try {
       while (sdataIter.hasNext()) {
         StructureData sdata = sdataIter.next();

         for (StructureMembers.Member m : sdata.getMembers()) {
           if (m.getDataType() == DataType.SEQUENCE) {
             FieldConverter fld = parent.findChild(m.getName());
             if (fld == null) {
               log.error("BufrConfig cant find Child member= {} for file = {}", m, filename);
               continue;
             }
             ArraySequence data = (ArraySequence) sdata.getArray(m);
             int n = data.getStructureDataCount();
             fld.trackSeqCounts(n);
             processSeq(data.getStructureDataIterator(), fld);
           }
         }
       }
     } finally {
       sdataIter.finish();
     }
   } */


}
