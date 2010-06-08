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

import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.InvalidRangeException;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.channels.WritableByteChannel;

/**
 * Test scan bufr messages
 * @author caron
 * @since May 9, 2008
 */
public class Scanner {

  /////////////////////////////////////////////////////////////////////

  interface MClosure {
    void run(String filename) throws IOException;
  }

  static void test(String filename, boolean subdirs, MClosure closure) throws IOException {
    File f = new File(filename);
    if (!f.exists()) {
      System.out.println(filename + " does not exist");
      return;
    }
    if (f.isDirectory()) {
      testAllInDir(f, subdirs, closure);
    } else {
      try {
        closure.run(f.getPath());
      } catch (Exception ioe) {
        System.out.println("Failed on " + f.getPath() + ": " + ioe.getMessage());
        ioe.printStackTrace();
      }
    }
  }


  static void testAllInDir(File dir, boolean subdirs, MClosure closure) {
    List<File> list = Arrays.asList(dir.listFiles());
    Collections.sort(list);

    for (File f : list) {
      if (f.getName().endsWith("bfx")) continue;
      if (f.getName().endsWith("txt")) continue;
      if (f.getName().endsWith("zip")) continue;
      if (f.getName().endsWith("csh")) continue;
      if (f.getName().endsWith("rtf")) continue;

      if (f.isDirectory()) {
        if (subdirs) testAllInDir(f, subdirs, closure);
      } else {
          try {
            closure.run(f.getPath());
          } catch (Exception ioe) {
            System.out.println("Failed on " + f.getPath() + ": " + ioe.getMessage());
            ioe.printStackTrace();
          }
        }
    }
  }

  //////////////////////////////////////////////////////////////

  static void scanDDS(String filename) throws IOException {
    long start = System.nanoTime();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    out.format("\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      m.dump(out);

      if (!m.isTablesComplete()) {
        out.format("**INCOMPLETE ");
      }

      long startPos = m.is.getStartPos();
      out.format(" msg= %d time=%s starts=%d len=%d end=%d dataEnd=%d\n",
              count, m.getReferenceTime(), startPos, m.is.getBufrLength(), (startPos + m.is.getBufrLength()),
              (m.dataSection.getDataPos() + m.dataSection.getDataLength()));
      out.format("  ndatasets=%d isCompressed=%s datatype=0x%x header=%s\n",
              m.getNumberDatasets(), m.dds.isCompressed(), m.dds.getDataType(), m.getHeader());

      count++;
      break;
    }
    raf.close();
  }

  //////////////////////////////////////////////////////////////

  // o = minimal, 1=header, 2=dump dds
  static int dumpMessages(String filename, int mode) throws IOException {
    long start = System.nanoTime();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    out.format("\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      //if (count == 0) new BufrDump2().dump(out, m);

      if (!m.isTablesComplete()) {
        out.format("**INCOMPLETE ");
      }

      if (mode >= 0) {
        long startPos = m.is.getStartPos();
        out.format(" msg= %d time=%s starts=%d len=%d end=%d dataEnd=%d hash=[0x%x]\n",
                count, m.getReferenceTime(), startPos, m.is.getBufrLength(), (startPos + m.is.getBufrLength()),
                (m.dataSection.getDataPos() + m.dataSection.getDataLength()), m.hashCode());
        out.format("  ndatasets=%d isCompressed=%s datatype=0x%x header=%s\n",
                m.getNumberDatasets(), m.dds.isCompressed(), m.dds.getDataType(), m.getHeader());
      }

      if (mode == 2)
        m.dump(out);
      else if (mode == 1)
        m.dumpHeader(out);

      if (mode >= 0) out.format("%n");
      count++;
    }
    raf.close();

    long took = (System.nanoTime() - start);
    double rate = (took > 0) ? ((double) (1000 * 1000) * count / took) : 0.0;
    out.format(" nmsgs= %d nobs = %d took %d msecs rate = %f msgs/msec\n", count, scan.getTotalObs(), took / (1000 * 1000), rate);
    return scan.getTotalObs();
  }

  //////////////////////////////////////////////////////////////

  static void scanTimes(String filename) throws IOException {
    long start = System.nanoTime();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    out.format("\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      //if (count == 0) new BufrDump2().dump(out, m);

      out.format(" %s time=%s\n", m.getHeader(), m.getReferenceTime());
      count++;
    }
    raf.close();
  }

  //////////////////////////////////////////////////////////////

  static Map<List<String>, String> mixedSet = new HashMap<List<String>, String>();

  static void scanMixedMessageTypes(String filename) throws IOException {

    //RandomAccessFile raf = new RandomAccessFile("C:\\data\\bufr\\edition3\\idd\\radiosonde\\SoundingVerticalRadiosonde4.bufr", "r");
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    out.format("\n-----\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;

      List<String> desc = m.dds.getDescriptors();
      String key = mixedSet.get(desc);
      if (null == key) {
        out.format(" new Message Type msg=%d <%s> ndesc=%d hashCode=%d\n", count, m.getHeader(), desc.size(), desc.hashCode());
        m.getRootDataDescriptor();
        m.dump(out);
        mixedSet.put(desc, filename);
      } else if (!key.equals(filename)) {
        out.format(" msg type from file= %s, hashcode=%d\n", key, desc.hashCode());
      }
      count++;
    }
    raf.close();
    out.format("nmsgs= %d nobs = %d\n", count, scan.getTotalObs());
  }

  //////////////////////////////////////////////////////////////

  static void scanMessageSizes(String filename, Formatter formatter) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    out.format("\n-----\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      if (!m.isTablesComplete()) {
        out.format("Message "+count+" INCOMPLETE TABLES%n");
        count++;
        break;
      }

      DataDescriptor root = m.getRootDataDescriptor(); // make sure the dds has been formed
      m.dumpHeader(out);

      m.calcTotalBits(formatter);
      int nbitsCounted = m.getTotalBits();
      int nbitsGiven = 8 * (m.dataSection.getDataLength() - 4);

      boolean ok = Math.abs(m.getCountedDataBytes()- m.dataSection.getDataLength()) <= 1; // radiosondes dataLen not even number of bytes

      if (!ok) out.format("*** BAD ");
        long last = m.dataSection.getDataPos() + m.dataSection.getDataLength();
        out.format("Message %d nds=%d compressed=%s vlen=%s countBits= %d givenBits=%d data start=0x%x end=0x%x",
                count, m.getNumberDatasets(), m.dds.isCompressed(), root.isVarLength(),
                nbitsCounted, nbitsGiven, m.dataSection.getDataPos(), last);
        out.format(" countBytes= %d dataSize=%d", m.getCountedDataBytes(), m.dataSection.getDataLength());
        out.format("%n");
      
      /* if (m.getCountedDataBytes() != m.dataSection.dataLength) {
        out.format(" extra=");
        showBytes(out, raf, m.dataSection.dataPos + m.getCountedDataBytes(), m.dataSection.dataLength - m.getCountedDataBytes());
      } */
      count++;
    }
    raf.close();
    out.format("nmsgs= %d nobs = %d\n", count, scan.getTotalObs());
  }

  static private void showBytes(Formatter out, RandomAccessFile raf, long start, int count) throws IOException {
    raf.seek(start);
    for (int i = 0; i < count; i++)
      out.format("%d=<%d>", i, raf.read());
  }

  //////////////////////////////////////////////////////////////

  static Map<Message, int[]> badMap = new HashMap<Message, int[]>();
  static Map<Message, Map<String, Counter>> typeMap = new HashMap<Message, Map<String, Counter>>();
  static Map<String, List<Message>> headerMap = new HashMap<String, List<Message>>();
  static Map<String, Counter> headerCount = new HashMap<String, Counter>();
  static int total_msgs, bad_msgs, bad_wmo, bad_tables, bad_operation, total_different, good_msgs, total_obs;
  static long file_size = 0;

  static void scanMessageTypes(String filename) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    //out.format("\n-----\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);
    file_size += raf.length();

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {

      Message m = scan.next();
      if (m == null) {
        bad_msgs++;
        continue;
      }

      // incomplete tables
      try {
        if (!m.isTablesComplete()) {
          int[] nbad = badMap.get(m);
          if (nbad == null) {
            nbad = new int[1];
            badMap.put(m, nbad);
          }
          nbad[0]++;
          bad_tables++;
          //continue; dont exclude 
        }
      } catch (UnsupportedOperationException e) {
        m.dumpHeader(out);
        bad_operation++;
        continue;
      }

      // track desc to headers
      String ttaaii = extractWMO(m.getHeader());
      if (ttaaii == null) {
        bad_wmo++;
        continue;
      }

      // map dds -> wmoheader
      Map<String, Counter> keys = typeMap.get(m);
      if (null == keys) {
        //out.format("  new Descriptor Type msg %d ttaaii=%s hashCode=%d cat=%s\n",
        //        count, ttaaii, m.hashCode(), m.getCategory());
        //new BufrDump2().dump(out, m);
        keys = new HashMap<String, Counter>();
        keys.put(ttaaii, new Counter(ttaaii));
        typeMap.put(m, keys);
      }
      Counter c = keys.get(ttaaii);
      if (c == null) {
        //out.format("  msg %d has different ttaaii = %s hashcode=%d cat=%s\n",
        //        count, ttaaii, m.hashCode(), m.getCategory());
        c = new Counter(ttaaii);
        keys.put(ttaaii, c);
      }
      c.count++;
      c.countObs += m.getNumberDatasets();

      // map wmoheader to dds
      List<Message> mtypes = headerMap.get(ttaaii);
      if (mtypes == null) {
        mtypes = new ArrayList<Message>();
        headerMap.put(ttaaii, mtypes);
        mtypes.add(m);
      } else if (!mtypes.contains(m)) {
        //out.format(" Different desc for header %s hashCode=%d prev hashcode=%d\n", ttaaii, desc.hashCode(), hdesc.hashCode());
        mtypes.add(m);
        total_different++;
      }

      // track header count
      Counter hc = headerCount.get(ttaaii);
      if (hc == null) {
        hc = new Counter(ttaaii);
        headerCount.put(ttaaii, hc);
      }
      hc.count++;

      count++;
    }
    raf.close();
    out.format(filename+" total_msgs= %d good=%d total_obs = %d\n", scan.getTotalMessages(), count, scan.getTotalObs());
    total_msgs += scan.getTotalMessages();
    good_msgs += count;
    total_obs += scan.getTotalObs();
  }

  static void showTypes(Formatter csv) throws IOException {

    out.format("\n===============================================\n");
    out.format("total_msgs=%d good_msgs=%d bad_msgs=%d incomplete_tables=%d bad_operation=%d total_obs=%d\n",
            total_msgs, good_msgs, bad_msgs, bad_tables, bad_operation, total_obs);
    int avg_msg = (int) (file_size / total_msgs);
    int avg_obs = (int) (total_obs / total_msgs);
    out.format("total_bytes=%d avg_msg_size=%d avg_obs/msg=%d \n", file_size, avg_msg, avg_obs);

    int good_dds = typeMap.keySet().size();
    int bad_dds = badMap.keySet().size();
    int total_wmo = headerMap.keySet().size();
    out.format(" good_dds=%d good_msgs=%d\n",good_dds, good_msgs);
    out.format(" incomplete_dds=%d incomplete_msgs=%d\n",bad_dds, bad_tables);
    out.format(" wmoHeaders=%d bad_wmo=%d wmo_multipleDDS=%d\n", total_wmo, bad_wmo, total_different);

    out.format("\nIncomplete Tables\n");
    for (Message m : badMap.keySet()) {
      int[] nbad = badMap.get(m);
      out.format(" nbad messages = %d\n", nbad[0]);
      m.dumpHeader(out);
      out.format(" missing keys= ");
      m.showMissingFields(out);
      out.format("\n\n");
    }
    out.format("\n");

    if (csv != null)
      csv.format("hash, header, nMess, nObs, complete, center, table, edition, category %n");

    out.format("Map dds -> wmoHeaders\n");
    for (Message m : typeMap.keySet()) {
      out.format(" [0x%x] ", m.hashCode());
      m.dumpHeaderShort(out);
      //out.format("Message Type %d ndesc=%d cat = %s\n", m.hashCode(), m.dds.getDescriptors().size(), m.getCategory());
      int totalm = 0, totalo = 0;
      Map<String, Counter> keys = typeMap.get(m);
      for (String key : keys.keySet()) {
        Counter cc = keys.get(key);
        out.format("  %s count=%d\n", key, cc.count);
        totalm += cc.count;
        totalo += cc.countObs;
      }
      if (csv != null)
        csv.format("Ox%x, %s, %d, %d, %s, %s, %s, %s, %s %n",
          m.hashCode(), extractWMO(m.getHeader()), totalm, totalo, m.isTablesComplete(),
          m.getCenterNo(), m.getTableName(), m.is.getBufrEdition(), m.getCategoryNo());
    }

    int nwmo = 0;
    int total_wmo_count = 0;
    out.format("\nMap wmoHeaders -> dds (unique)\n");
    Set<String> headerSet = headerMap.keySet();
    List<String> headers = new ArrayList<String>();
    headers.addAll(headerSet);
    Collections.sort(headers);
    for (String wmo : headers) {
      List<Message> msgs = headerMap.get(wmo);
      if (msgs.size() > 1) continue;
      Message m = msgs.get(0);
      Counter hc = headerCount.get( extractWMO(m.getHeader()));
      int count = (hc == null) ? 0 : hc.count;
      out.format(" WMO %s (%d) cat= %s (%s), center = %s (%s) [0x%x]\n", wmo, count,
              m.getCategoryName(), m.getCategoryNo(), m.getCenterName(), m.getCenterNo(), m.hashCode());
      total_wmo_count += count;
      nwmo++;
    }
    out.format("  nwmo=%d total_wmo_count=%d %n", nwmo, total_wmo_count);

    out.format("\nMap wmoHeaders -> dds (multiple)\n");
    for (String wmo : headers) {
      List<Message> msgs = headerMap.get(wmo);
      if (msgs.size() <= 1) continue;
      out.format("--WMO %s has %d types %n", wmo, msgs.size());
    }

    for (String wmo : headers) {
      List<Message> msgs = headerMap.get(wmo);
      if (msgs.size() <= 1) continue;

      out.format("--WMO %s has %d types %n", wmo, msgs.size());
      for (Message m : msgs) {
        Counter hc = headerCount.get(extractWMO(m.getHeader()));
        int count = (hc == null) ? 0 : hc.count;
        m.dumpHeader(out);
        out.format(" hash=%d\n\n", m.hashCode());
      }
    }
  }

  static String extractWMO2(String header) {
    // default is to get second space-delineated token
    StringTokenizer stoker = new StringTokenizer(header);
    if (stoker.hasMoreTokens()) stoker.nextToken();
    if (stoker.hasMoreTokens()) {
      String result = stoker.nextToken();
      char first = result.charAt(0);
      if ((first == 'I') || (first == 'J')) // must start with I or J
        return result;
    }

    // try pulling off the 3rd to last token
    int pos = header.lastIndexOf(' ');
    if (pos > 0) {
      int pos2 = header.lastIndexOf(' ', pos - 1);
      if (pos2 > 0) {
        int pos3 = header.lastIndexOf(' ', pos2 - 1);
        if (pos3 > 0)
          return header.substring(pos3 + 1, pos2);
      }
    }

    out.format("***header= %s\n", header);
    return null;
    /* int pos = header.indexOf('I');
    if (pos > 0)
      header = header.substring(pos);
    pos = header.indexOf(' ');
    if (pos > 0)
      header = header.substring(0, pos);
    return header; */
  }

  private static final Pattern wmoPattern = Pattern.compile(".*([IJ]..... ....) .*");

  static String extractWMO(String header) {
    Matcher matcher = wmoPattern.matcher( header);
    if (!matcher.matches()) {
      //out.format("***header failed= %s\n", header);
      return null;
    }
    return matcher.group(1);
  }

  static String extractWMO3(String header) {
    // LOOK - replace with regexp
    int pos1 = header.indexOf('I');
    int pos2 = header.indexOf('J');
    int pos = Math.min(pos1, pos2);
    if (pos < 0)
      pos = Math.max(pos1, pos2);
    if (pos < 0) {
      out.format("***header= %s\n", header);
      return null;
    }
    header = header.substring(pos);
    //pos = header.indexOf(' ');
    //if (pos > 0)
    //  header = header.substring(0, pos);
    return header;
  }

  static class Counter implements Comparable<Counter> {
    int count, countObs, countBytes;
    String s;
    Message m;
    DataDescriptor dkey;

    Counter(String s) {
      this.s = s;
    }

    public int hashCode() {
      return s.hashCode();
    }

    public boolean equals(Object o) {
      if (!(o instanceof Counter)) return false;
      Counter oo = (Counter) o;
      return s.equals(oo.s);
    }

    public int compareTo(Counter o) {
      return o.countObs - countObs; // largest first
    }
  }

  ///////////////////////////////////////////////////////////

  static Set<Message> messSet = new HashSet<Message>();

  static void writeUniqueDDS(String filename, WritableByteChannel wbc) throws IOException {
    System.out.printf("open %s ",filename);
    RandomAccessFile raf = new RandomAccessFile(filename, "r");

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {

      Message m = scan.next();
      if (m == null) {
        bad_msgs++;
        System.out.printf("Bad Message%n");
        continue;
      }

      if (!messSet.contains(m)) {
        scan.writeCurrentMessage(wbc);
        messSet.add(m);
      }

      count++;
    }
    raf.close();
    System.out.printf(" read  = %d%n ", count);

  }


  //////////////////////////////////////////////////////////////

  static Map<Message, Counter> messMap = new HashMap<Message, Counter>();
  static Map<Short, Counter> ddsMap = new HashMap<Short, Counter>();
  static Map<Short, Counter> descMap = new HashMap<Short, Counter>();

  static void scanMessageDDS(String filename) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    //out.format("\n-----\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);
    file_size += raf.length();

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {

      Message m = scan.next();
      if (m == null) {
        bad_msgs++;
        continue;
      }

      // incomplete tables
      try {
        if (!m.isTablesComplete()) {
          int[] nbad = badMap.get(m);
          if (nbad == null) {
            nbad = new int[1];
            badMap.put(m, nbad);
          }
          nbad[0]++;
          bad_tables++;
          //continue;
        }
      } catch (UnsupportedOperationException e) {
        m.dumpHeader(out);
        bad_operation++;
        continue;
      }

      String ttaaii = extractWMO(m.getHeader());

      Counter c = messMap.get(m);
      if (null == c) {
        c = new Counter(ttaaii);
        c.m = m;
        messMap.put(m, c);

        addDDS(ttaaii, m, m.getRootDataDescriptor());
        addDesc(m, m.dds.getDescriptors());
      }
      c.count++;
      c.countObs += m.getNumberDatasets();
      c.countBytes += m.getMessageSize();

      count++;
    }
    raf.close();
    out.format("total_msgs= %d good=%d total_obs = %d\n", scan.getTotalMessages(), count, scan.getTotalObs());
    total_msgs += scan.getTotalMessages();
    good_msgs += count;
    total_obs += scan.getTotalObs();
  }

  static void addDDS( String ttaaii, Message m, DataDescriptor parent) {
    for (DataDescriptor dkey : parent.getSubKeys()) {
      Counter c = ddsMap.get( dkey.fxy);
      if (null == c) {
        c = new Counter(ttaaii);
        c.dkey = dkey;
        c.m = m;
        ddsMap.put( dkey.fxy, c);
      }
      c.count++;

      if (dkey.getSubKeys() != null)
        addDDS(ttaaii, m, dkey);
    }
  }

  static private List<DataDescriptor> addDesc(Message m, List<String> keyDesc) {
    if (keyDesc == null) return null;
    TableLookup lookup = m.getTableLookup();

    List<DataDescriptor> keys = new ArrayList<DataDescriptor>();
    for (String desc : keyDesc) {
      short key = Descriptor.getFxy(desc);
      DataDescriptor dd = new DataDescriptor(key, lookup);

      Counter c = descMap.get( dd.fxy);
      if (null == c) {
        c = new Counter(desc);
        c.m = m;
        descMap.put( dd.fxy, c);
      }
      c.count++;

      if (dd.f == 3) {
        List<String> subDesc = lookup.getDescriptorsTableD( dd.getFxyName());
        addDesc(m, subDesc);
      }
    }
    return keys;
  }


  static void showDDS(Formatter messCsv, Formatter ddsCsv) throws IOException {

    out.format("\n===============================================\n");
    out.format("total_msgs=%d good_msgs=%d bad_msgs=%d incomplete_tables=%d bad_operation=%d total_obs=%d\n",
            total_msgs, good_msgs, bad_msgs, bad_tables, bad_operation, total_obs);
    int avg_msg = (int) (file_size / total_msgs);
    int avg_obs = (int) (total_obs / total_msgs);
    out.format("total_bytes=%d avg_msg_size=%d avg_obs/msg=%d \n", file_size, avg_msg, avg_obs);

    int good_dds = messMap.keySet().size();
    int bad_dds = badMap.keySet().size();
    out.format(" good_dds=%d good_msgs=%d\n",good_dds, good_msgs);
    out.format(" incomplete_dds=%d incomplete_msgs=%d\n", bad_dds, bad_tables);

    List<Counter> cc = new ArrayList<Counter>(messMap.values());
    Collections.sort(cc);

    out.format("Unique message DDS\n");
    if (messCsv != null)
      messCsv.format("header, nMess, nObs, Kbytes, hash, center, table, edition, category %n");
    for (Counter c : cc) {
      c.m.dumpHeader(out);
      out.format(" %s [0x%x] ", c.s, c.m.hashCode());
      //out.format("Message Type %d ndesc=%d cat = %s\n", m.hashCode(), m.dds.getDescriptors().size(), m.getCategory());
      out.format(" Count msg=%d obs=%d bytes=%d %n%n", c.count, c.countObs, c.countBytes);
      if (messCsv != null) {
        messCsv.format("%s, %d, %d, %d, 0x%x, %s, %s, %s, %s %n",
            c.s, c.count, c.countObs, c.countBytes/1000, c.m.hashCode(),
            scrub(c.m.getCenterName()), c.m.getTableName(), c.m.is.getBufrEdition(), scrub(c.m.getCategoryFullName()));
      }
    }

    List<Counter> ddsCollection = new ArrayList<Counter>(ddsMap.values());
    Collections.sort(ddsCollection, new CompareDDS());
    out.format("DataDescriptors\n");
    for (Counter c : ddsCollection) {
      out.format(" %d %10s %s [0x%x] %n",  c.count, c.dkey.getFxyName(), c.s, c.m.hashCode());
    }

    List<Short> descCollection = new ArrayList<Short>(descMap.keySet());
    Collections.sort(descCollection);
    out.format("%n%nRaw Descriptors%n");
    if (ddsCsv != null)
      ddsCsv.format("fxy, name, count, header, table, center %n");
    for (Short fxy: descCollection) {
      Counter c = descMap.get(fxy);
      out.format(" %5d %-10s %n",  c.count, Descriptor.makeString(fxy));
      if (ddsCsv != null) {
        ddsCsv.format("'%s', %s, %d, %s, %s, %s%n", Descriptor.makeString(fxy),
            scrub(Descriptor.getName(fxy.shortValue(), c.m.getTableLookup())),
            c.count, extractWMO(c.m.getHeader()), c.m.getTableName(), scrub(c.m.getCenterName()));
      }
    }
  }

  static private String scrub(String s) {
    return s.replace(',',' ');
  }

  static private String makeName(String name) {
    return (name == null) ? "" : scrub(name);
  }

  static private class CompareDDS implements Comparator<Counter> {

    public int compare(Counter o1, Counter o2) {
      if ((o1.dkey == null) || (o2.dkey == null))
        System.out.println("HEY");
      return o1.dkey.fxy - o2.dkey.fxy;
    }
  }


  ////////////////////////////////////////////////////////

  // extract the msgno-th message to fileOut

  static void scanReader(String filein) throws IOException {
    Formatter f = new Formatter(System.out);

    RandomAccessFile raf = new RandomAccessFile(filein, "r");
    MessageScanner scan = new MessageScanner(raf);
    while (scan.hasNext()) {
      Message m = scan.next();
      m.dumpHeader(out);
      if (!m.dds.isCompressed()) {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        reader.readData(null, m, raf, null, false, null);
      } else {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        reader.readData(null, m, raf, null, f);
      }

      int nbitsGiven = 8 * (m.dataSection.getDataLength() - 4);
      System.out.printf("nbits counted = %d expected=%d %n", m.msg_nbits, nbitsGiven);
      System.out.printf("nbytes counted = %d expected=%d %n", m.getCountedDataBytes(), m.dataSection.getDataLength());
      if (m.isTablesComplete() && !m.isBitCountOk())
        System.out.printf("BAD BIT COUNT %n%n");
    }
  }

  static void scanReader2(String filein) throws IOException, InvalidRangeException {
    System.out.printf("scanReader2 %s%n", filein);
    BufrIosp.doon(filein);
    //NetcdfFile ncfile = NetcdfFile.open(filein);
    //BufrIosp iosp =  (BufrIosp) ncfile.getIosp();
    //iosp.compare((Structure) ncfile.findVariable("obsRecord"));
  }


  // extract the msgno-th message to fileOut
  static void extractNthMessage(String filein, int msgno, String fileout) throws IOException {
    FileOutputStream fos = new FileOutputStream(fileout);
    WritableByteChannel wbc = fos.getChannel();

    RandomAccessFile raf = new RandomAccessFile(filein, "r");
    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (msgno == count) {
        scan.writeCurrentMessage(wbc);
        wbc.close();
        raf.close();
        return;
      }
      count++;
    }
  }

  // extract the message matching listHash
  static void extractMessageByListhash(String filein, int want, String fileout) throws IOException {
    FileOutputStream fos = new FileOutputStream(fileout);
    WritableByteChannel wbc = fos.getChannel();

    int count = 0;
    RandomAccessFile raf = new RandomAccessFile(filein, "r");
    MessageScanner scan = new MessageScanner(raf);
    while (scan.hasNext()) {
      Message m = scan.next();
      int listHash = m.dds.getDataDescriptors().hashCode();

      if (listHash == want) {
        scan.writeCurrentMessage(wbc);
        wbc.close();
        raf.close();
        System.out.printf("output %d from %s %n",count, filein);
        return;
      }
      count++;
    }
  }

  // extract n messages to fileOut
  static void extractNMessages(String filein, int n, String fileout) throws IOException {
    FileOutputStream fos = new FileOutputStream(fileout);
    WritableByteChannel wbc = fos.getChannel();

    RandomAccessFile raf = new RandomAccessFile(filein, "r");
    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext() && (count < n)) {
      Message m = scan.next();
        scan.writeCurrentMessage(wbc);
      count++;
    }
    wbc.close();
    raf.close();
  }

  // extract the first message that contains the header string to fileOut
  static void extractFirstMessageWithHeader(String filein, String header, String fileout) throws IOException {
    FileOutputStream fos = new FileOutputStream(fileout);
    WritableByteChannel wbc = fos.getChannel();

    RandomAccessFile raf = new RandomAccessFile(filein, "r");
    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m.getHeader().contains(header)) {
        scan.writeCurrentMessage(wbc);
        wbc.close();
        raf.close();
        return;
      }
      count++;
    }
  }

    // extract all messages that contains the header string to fileOut
  static void extractAllWithHeader(String filein, Pattern p, WritableByteChannel wbc) throws IOException {
    System.out.println("extract "+filein);
    RandomAccessFile raf = new RandomAccessFile(filein, "r");
    MessageScanner scan = new MessageScanner(raf);
    while (scan.hasNext()) {
      Message m = scan.next();
      Matcher matcher = p.matcher(m.getHeader());
      if (matcher.matches()) {
        scan.writeCurrentMessage(wbc);
        System.out.println(" found match "+m.getHeader());
      }
    }
    raf.close();
  }

  static Formatter out = new Formatter(System.out);
  static public void main(String args[]) throws IOException {
    //extractNMessages("D:/formats/bufr/tmp/dispatch/KNES-ISXX03.bufr", 3, "D:/formats/bufr/tmp/ISXX03-3.bufr");
    extractMessageByListhash("C:\\data\\formats\\bufrRoy/US058MCUS-BUFtdp.SPOUT_00011_sfc_ship_20091101042700.bufr", 1118454047, "C:\\data\\formats\\bufrRoy/out.bufr");
    
    //extract("D:/bufr/dispatch/EGRR-IUAD01.bufr", 0, "D:/bufr/out/EGRR-IUAD01-1.bufr");
    //extract("D:/bufr/dispatch/IUPT0KBOU.bufr", 0, "D:/bufr/out/IUPT0KBOU-1.bufr");
    //extract("D:/bufr/mlodeRaw/20080709_0200.bufr", "IUAD01 EGRR", "D:/bufr/out/IUAD01EGRR-1.bufr");

    /* extract messages
    FileOutputStream fos = new FileOutputStream("D:/bufr/out/JSMF14KWNO.bufr");
    final WritableByteChannel wbc = fos.getChannel();
    final Pattern p = Pattern.compile(".*JSMF14 KWNO.*");
    test("D:\\bufr\\mlodeRaw", new MClosure() {
      public void run(String filename) throws IOException {
        extract(filename, p, wbc);
      }
    });
    wbc.close();
    fos.close();
    // */

    /* dump messages
    test("D:/formats/bufr/idd/", new MClosure() {
      public void run(String filename) throws IOException {
        dumpMessages(filename, -1);
      }
    }); // */

    // look for mixed message types in the files
    /* also for missing table entries R:/testdata2/bufr/edition3/idd/singleLevelSatellite/
    test("R:/testdata2/bufr/edition3/newIdd", new MClosure() {
       public void run(String filename) throws IOException {
         scanMixedMessageTypes(filename);
       }
     }); // */

    /* look for all message types in the files
     test("D:/formats/bufr/idd/", new MClosure() {
        public void run(String filename) throws IOException {
          scanMessageTypes(filename);
        }
      });
    Formatter ddsCsv = new Formatter( new FileOutputStream("D:/formats/bufr/tmp/idd.csv"));
    showTypes(ddsCsv);
    ddsCsv.close(); // */

     /* see if we can count bits
     //final Formatter showDetails = new Formatter( new FileOutputStream("C:/tmp/scan.txt"));
     final Formatter showDetails = new Formatter( System.out);
     test("C:/data/bufr2/asampleAll.bufr", new MClosure() {
       public void run(String filename) throws IOException {
         scanMessageSizes(filename, showDetails);
       }
     }); // */

    /* extract unique DDS
    FileOutputStream fos = new FileOutputStream("D:/formats/bufr/uniqueExamples.bufr");
    final WritableByteChannel wbc = fos.getChannel();
    test("D:/formats/bufr/examples/", true, new MClosure() {
       public void run(String filename) throws IOException {
         writeUniqueDDS(filename, wbc);
       }
     });
    System.out.printf("# messages = %d %n",messSet.size());
    wbc.close();  */

     /* extract unique DDS  // 20080707_1900.bufr
     test("D:/formats/bufr/brasil/", true, new MClosure() {
       public void run(String filename) throws IOException {
         scanMessageDDS(filename);
       }
     });
    Formatter messCsv = new Formatter( new FileOutputStream("D:/formats/bufr/brasil/mess.csv"));
    Formatter ddsCsv = new Formatter( new FileOutputStream("D:/formats/bufr/brasil/dds.csv"));
    showDDS(messCsv, ddsCsv);
    //showDDS(null, null);
    ddsCsv.close();
    messCsv.close();
    // */

    /* dump DDS
     test("D:/bad/", new MClosure() {
       public void run(String filename) throws IOException {
         scanDDS(filename);
       }
     }); // */

    // new reader
    /* test("D:/formats/bufr/tmp/dispatch/asample.bufr", new MClosure() {
    test("D:/formats/bufr/tmp/split/", false, new MClosure() {
      public void run(String filename) throws IOException {
        if (!(filename.endsWith(".bufr"))) return;
        try {
          scanReader2(filename);
        } catch (Exception e) {
          System.err.printf("ERROR %s %n", filename);
          e.printStackTrace();
        }
      }
    }); // */


  }

}
