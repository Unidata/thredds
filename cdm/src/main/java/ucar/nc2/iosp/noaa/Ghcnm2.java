package ucar.nc2.iosp.noaa;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.ncml.NcmlConstructor;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOMADS Ghcnm2
 *
 * @author caron
 * @since Feb 26, 2011
 */
public class Ghcnm2 extends AbstractIOServiceProvider {
  private static final String p = "(\\d{11})(\\d{4})TAVG([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)"+
            "([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)"+
            "([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)?(.)?(.)?.*";

  private static final Pattern dataPattern = Pattern.compile(p);
  private static final String ncml = 
    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' iosp='ucar.nc2.iosp.noaa.Ghcnm'>\n" +
            "  <dimension name='month' length='12' />\n" +
            "  <attribute name='title' value='Version 3 of the GHCN-Monthly dataset of land surface mean temperatures' />\n" +
            "  <attribute name='Conventions' value='CDM' />\n" +
            "  <attribute name='featureType' value='timeSeries' />\n" +
            "  <attribute name='history' value='direct read of ascii files into CDM' />\n" +
            "  <attribute name='see' value='http://www.ncdc.noaa.gov/ghcnm, ftp://ftp.ncdc.noaa.gov/pub/data/ghcn/v3' />\n" +
            "  <variable name='all_data' shape='*' type='Sequence'>\n" +
            "    <variable name='stnid' shape='' type='long'>\n" +
            "      <attribute name='long_name' value='station stnId' />\n" +
            "    </variable>\n" +
            "    <variable name='year' shape='' type='int'>\n" +
            "      <attribute name='long_name' value='year of the station record' />\n" +
            "    </variable>\n" +
            "    <variable name='value' shape='month' type='float'>\n" +
            "      <attribute name='long_name' value='monthly mean temperature' />\n" +
            "      <attribute name='units' value='Celsius' />\n" +
            "      <attribute name='scale_factor' type='float' value='.01' />\n" +
            "      <attribute name='missing_value' type='float' value='-99.99' />\n" +
            "    </variable>\n" +
            "    <variable name='dm' shape='month' type='char'>\n" +
            "      <attribute name='long_name' value='data management flag' />\n" +
            "    </variable>\n" +
            "    <variable name='qc' shape='month' type='char'>\n" +
            "      <attribute name='long_name' value='quality control flag' />\n" +
            "    </variable>\n" +
            "    <variable name='ds' shape='month' type='char'>\n" +
            "      <attribute name='long_name' value='data source flag' />\n" +
            "    </variable>\n" +
           "  </variable>\n" +
            "</netcdf>";

  /*
            "    <variable name='time' shape='' type='String'>\n" +
            "      <attribute name='long_name' value='year as ISO date string' />\n" +
            "      <attribute name='_CoordinateAxisType' value='Time' />\n" +
            "      <attribute name='missing_value' value='missing' />\n" +
            "    </variable>\n" +
   */

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    String line;
    while (true) {
      line = raf.readLine();
      if (line == null) break;
      if (line.startsWith("#")) continue;
      if (line.trim().length() == 0) continue;
      Matcher matcher = dataPattern.matcher(line);
      boolean ok = matcher.matches();
      return ok;
    }    
    return false;
  }

  @Override
  public String getFileTypeId() {
    return "GHCNM";
  }

  @Override
  public String getFileTypeDescription() {
    return "GLOBAL HISTORICAL CLIMATOLOGY NETWORK MONTHLY";
  }
  
  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    NcmlConstructor ncmlc = new NcmlConstructor();
    if (!ncmlc.populate(ncml, ncfile)) {
      throw new IllegalStateException(ncmlc.getErrlog().toString());
    }
    ncfile.finish();

    Sequence seq = (Sequence) ncfile.findVariable("all_data");
    StructureMembers sm = seq.makeStructureMembers();
    seq.setSPobject(new Vinfo(raf, sm, 0));

    int fldno = 1;
    for (StructureMembers.Member m : sm.getMembers()) {
      Vinfo vinfo = new Vinfo(raf, sm, fldno++);
      Variable v = seq.findVariable(m.getName());
      Attribute att = v.findAttribute("scale_factor");
      if (att != null) {
        vinfo.hasScale = true;
        vinfo.scale = att.getNumericValue().floatValue();
        v.remove(att);
      }
      m.setDataObject( vinfo);
      //fldno += m.getSize();
    }
  }

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Vinfo vinfo = (Vinfo) v2.getSPobject();
    return new ArraySequence( vinfo.sm, new SeqIter(vinfo), vinfo.nelems);
  }

  @Override
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    Vinfo vinfo = (Vinfo) s.getSPobject();
    return new SeqIter(vinfo);
  }

  private class SeqIter implements StructureDataIterator {
    private Vinfo vinfo;
    private long bytesRead;
    private long totalBytes;
    private int recno;
    private StructureData curr;

    SeqIter(Vinfo vinfo) throws IOException {
      this.vinfo = vinfo;
      totalBytes = (int) vinfo.raf.length();
      vinfo.raf.seek(0);
    }

    @Override
    public StructureDataIterator reset() {
      bytesRead = 0;
      recno = 0;

      try {
        vinfo.raf.seek(0);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    @Override
    public boolean hasNext() throws IOException {
      boolean more = (bytesRead < totalBytes); // && (recno < 10);
      if (!more) {
        vinfo.nelems = recno;
        //System.out.printf("nelems=%d%n", recno);
        return false;
      }
      curr = reallyNext();
      more = (curr != null);
      if (!more) {
        vinfo.nelems = recno;
        //System.out.printf("nelems=%d%n", recno);
        return false;
      }
      return more;
    }

    @Override
    public StructureData next() throws IOException {
      return curr;
    }

    private StructureData reallyNext() throws IOException {
      Matcher matcher;
      while (true) {
        String line = vinfo.raf.readLine();
        if (line == null) return null;
        if (line.startsWith("#")) continue;
        if (line.trim().length() == 0) continue;
        matcher = dataPattern.matcher(line);
        if (matcher.matches())
          break;
      }
      //System.out.printf("%s%n", line);
      bytesRead = vinfo.raf.getFilePointer();
      recno++;
      return new StructureDataRegexp(vinfo.sm, matcher);
    }

    @Override
    public void setBufferSize(int bytes) {
    }

    @Override
    public int getCurrentRecno() {
      return recno - 1;
    }
  }
  
  class Vinfo {
    RandomAccessFile raf;
    StructureMembers sm;
    int nelems = -1;
    int fldno;
    int stride = 4;
    float scale;
    boolean hasScale;

    private Vinfo(RandomAccessFile raf, StructureMembers sm, int fldno) {
      this.sm = sm;
      this.raf = raf;
      this.fldno = fldno;
    }
  }
}
