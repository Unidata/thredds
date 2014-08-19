/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp.bufr.writer;

import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.bufr.BufrTableLookup;
import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;

import java.io.*;
import java.util.*;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;

/**
 * BUFR Message Dispatch using DDS hashmap to sort the messages.
 * This is a singleton used on a single thread.
 *
 * @author caron
 * @since Aug 11, 2008
 */
public class MessageDispatchDDS {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageDispatchDDS.class);

  private File dispatchDir;
  private String inputFilenameOut;

  private Set<Integer> badHashSet = new HashSet<>(200);
  private Map<String, Integer> nameMap = new HashMap<>(200);
  private Map<Integer, MessType> typeMap = new HashMap<>(200);
  private Map<String, MessageWriter> writers = new HashMap<>(100);
  private List<Message> bufrTableMessages = new ArrayList<>();

  int total_msgs = 0;
  int match = 0;
  int badCount = 0;
  long badBytes = 0;
  int failWrite = 0;
  int ignored = 0;

  long total_bytes;
  int total_obs;

  //boolean checkBad = true;
  boolean showMatch = false;
  boolean showBad = false;
  boolean showConfig = false;

  boolean dayRollover = true;
  boolean useCatSubdirs = true;
  boolean useHeader = false;

  boolean writeIndex = false;
  boolean writeSamples = false;

  WritableByteChannel badWbc;
  WritableByteChannel sampleWbc;
  WritableByteChannel allWbc;

  //private final CompletionService<IndexerTask> executor;

  /**
   * @param configFilename
   * @param dispatchDir
   * @throws IOException
   */
  MessageDispatchDDS(String configFilename, File dispatchDir) throws IOException {
    this.dispatchDir = dispatchDir;

    /* if (writeSamples) {
      File file = new File(dispatchDir + "abad.bufr");
      FileOutputStream fos = new FileOutputStream(file);
      badWbc = fos.getChannel();

      file = new File(dispatchDir + "asample.bufr");
      fos = new FileOutputStream(file);
      sampleWbc = fos.getChannel();

      file = new File(dispatchDir + "asampleAll.bufr");
      fos = new FileOutputStream(file);
      allWbc = fos.getChannel();
    } */

    // MessageWriter.setRootDir(dispatchDir);

    // read config file
    if (configFilename != null) {
      File inputFile = new File(configFilename);
      if (inputFile.exists()) {
        try (BufferedReader dataIS = new BufferedReader(new InputStreamReader(
                new FileInputStream(inputFile), CDM.utf8Charset))) {
          while (true) {
            String line = dataIS.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.length() == 0) break;
            if (line.charAt(0) == '#') continue;

            String[] toke = line.split(",");
            //String hashS = toke[0].substring(2); // remove the "0x"
            Integer hash = Integer.parseInt(toke[0]);
            String bitsOk = (toke.length > 8) ? toke[7].trim() : "";
            typeMap.put(hash, new MessType(hash, toke[1], toke[2], toke[3], bitsOk));
          }
        }
      }
    }
  }

  public void setInputFilenameOut(String inputFilenameOut) {
    this.inputFilenameOut = inputFilenameOut;
  }

  void resetBufrTableMessages() {
    bufrTableMessages = new ArrayList<>();
  }

  void dispatch(Message m) throws IOException {
    total_msgs++;
    total_bytes += m.getRawBytes().length;
    total_obs += m.getNumberDatasets();

    // if its a BUFR Table
    boolean isTable = m.containsBufrTable();
    if (isTable) {
      bufrTableMessages.add(m);
      return;
    }

    // find the message type, based on message hashcode
    MessType matched = typeMap.get(m.hashCode());
    boolean newOne = false;

    if (matched == null) {
      matched = new MessType(m);
      typeMap.put(m.hashCode(), matched);
      writeSample(m, allWbc);
      newOne = true;

    } else if (matched.proto == null) {
      matched.proto = m;
      writeSample(m, allWbc);
      newOne = true;
    }

    matched.count++;
    matched.countObs += m.getNumberDatasets();

    if (matched.ignore) {
      ignored++;
      return;
    }

    if (matched.checkBad && checkIfBad(m)) {
      matched.nbad++;
      return; // dont write messages that fail bit counter;
    }

    boolean written = matched.scheduleWrite(m);
    if (written) matched.countBytes += m.getRawBytes().length + m.getHeader().length();

    if (written && newOne) {
      writeSample(m, sampleWbc);  // keep samples
    }
  }

  boolean checkIfBad(Message m) {
    boolean isBad;
    try {
      isBad = m.isTablesComplete() && !m.isBitCountOk();
    } catch (Exception e) {
      isBad = true;
    }

    if (isBad) {
      if (!badHashSet.contains(m.hashCode())) { // only write one kind of each bad message
        badHashSet.add(m.hashCode());
        try {
          badWbc.write(ByteBuffer.wrap(m.getHeader().getBytes(CDM.utf8Charset)));
          badWbc.write(ByteBuffer.wrap(m.getRawBytes()));

        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      if (showBad) logger.warn("bad <" + m.getHeader() + ">");
      badCount++;
      badBytes += m.getRawBytes().length + m.getHeader().length();
    }
    return isBad;
  }

  void writeSample(Message m, WritableByteChannel wbc) {
    if (!writeSamples) return;

    try {
      if (m.getHeader() != null)
        wbc.write(ByteBuffer.wrap(m.getHeader().getBytes(CDM.utf8Charset)));
      wbc.write(ByteBuffer.wrap(m.getRawBytes()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void exit(Formatter out) {
    try {
      for (MessageWriter writer : writers.values())
        writer.close();

      if (badWbc != null) badWbc.close();
      if (sampleWbc != null) sampleWbc.close();
      if (allWbc != null) allWbc.close();

    } catch (IOException ioe) {
      logger.error("MessageDispatchDDS.exit", ioe);
    }

    //for (MessType mtype : typeMap.values())
    //  if (mtype.indexer != null) mtype.indexer.close();

    // report out
    if (out != null) out.format("%n===============================================%n");

    int avg_msg = (total_msgs == 0) ? 0 : (int) (total_bytes / total_msgs);
    int avg_obs = (total_msgs == 0) ? 0 : (total_obs / total_msgs);
    if (out != null) {
      out.format("total_msgs=%d total_obs=%d total_bytes=%d avg_msg_size=%d avg_obs/msg=%d %n",
              total_msgs, total_obs, total_bytes, avg_msg, avg_obs);
      out.format("matched=%d types=%d bad=%d badTypes=%d badBytes=%d ignored=%d failWrite=%d %n",
              match, typeMap.size(), badCount, badHashSet.size(), badBytes, ignored, failWrite);
    }

    if ((inputFilenameOut != null)) {
      try (FileOutputStream cout = new FileOutputStream(inputFilenameOut)) {
        Formatter cfg = new Formatter(cout, CDM.utf8Charset.name());
        cfg.format("#    hash, filename, wmo, index, nmess, nobs, kBytes, complete, bitsOk, nbad, center, table, edition, category%n");
        List<MessType> mtypes = new ArrayList<>(typeMap.values());
        Collections.sort(mtypes, new MessTypeSorter());
        for (MessType mtype : mtypes) {
          if (mtype.proto == null) {
            if (out != null) out.format(" MessType %s count=%d fileout= %s%n", mtype.name, mtype.count, mtype.fileout);
            cfg.format("Ox%x, %s, %s, %s, %5d, %8d, %5f %n",
                    mtype.hash, mtype.fileout, mtype.name, mtype.index,
                    mtype.count, mtype.countObs, mtype.countBytes / 1000);
            continue;
          }
          Message m = mtype.proto;
          boolean hasBadMessages = badHashSet.contains(m.hashCode()); // did we find any messages that fail bit count ??
          if (out != null) out.format(" MessType %s count=%d fileout= %s%n", mtype.name, mtype.count, mtype.fileout);
          cfg.format("Ox%x, %s, %s, %s, %5d, %8d, %5f, %5s, %5s, %d, %s, %s, %s, %s%n",
                  mtype.hash, mtype.fileout, mtype.name, mtype.index,
                  mtype.count, mtype.countObs, mtype.countBytes / 1000,
                  m.isTablesComplete(), !hasBadMessages, mtype.nbad,
                  m.getLookup().getCenterNo(), m.getLookup().getTableName(), m.is.getBufrEdition(), m.getLookup().getCategoryNo());
        }
        if (out != null) out.format("%n");

      } catch (IOException ioe) {
        logger.error("MessageDispatchDDS.exit", ioe);
      }
    }
  }

  // Message type based on Message.hashCode
  private class MessType {
    int hash;
    String name;
    String fileout;
    String index;
    // Indexer indexer;
    short fileno = 0;

    Message proto;
    int count, countObs, nbad;
    float countBytes;
    boolean ignore = false; // completely ignore
    boolean checkBad = false; // default is to check for bad bits

    MessType(int hash, String filename, String name, String index, String bitsOk) {
      this.hash = hash;
      this.fileout = filename.trim();
      this.name = name.trim();

      nameMap.put(this.name, 0);
      this.ignore = fileout.equalsIgnoreCase("ignore");
      if (bitsOk.equalsIgnoreCase("true"))
        this.checkBad = false; // dont need to check bits

      if (writeIndex) {
        this.index = index.trim();
        if (!ignore && !this.index.equalsIgnoreCase("no")) {
          try {
            // LOOK indexer = BerkeleyDBIndexer.factory( dispatchDir + fileout, index);
          } catch (Exception e) {
            logger.error("Cant open BerkeleyDBIndexer", e);
          }
        }
      }

      if (showConfig) System.out.printf(" add hash=%d name=%s filename=%s index=%s%n", hash, name, fileout, index);
    }

    MessType(Message proto) {
      this.proto = proto;
      this.hash = proto.hashCode();

      if (useHeader) {
        this.name = extractName(proto.getHeader());
        if (this.name == null) this.name = Integer.toHexString(this.hash);
        Integer nameCount = nameMap.get(this.name);
        if (nameCount != null) {
          nameCount++;
          nameMap.put(this.name, nameCount); // increment this name counter
          this.name = name + "-" + nameCount;  // form a new one to avoid filename collision
        }
        nameMap.put(this.name, 0);

      } else {
        this.name = Integer.toHexString(this.hash);
      }

      this.fileout = name;
      if (showConfig) System.out.println(" add new MessType <" + name + "> fileout= " + fileout);
    }

    String extractName(String header) {
      if (header.length() < 12) return null;
      return header.substring(7, 11) + "-" + header.substring(0, 6);
    }

    boolean scheduleWrite(Message m) throws IOException {
      CalendarDate cd = m.ids.getReferenceTime();
      String writerName = (dayRollover) ? fileout + "." + CalendarDateFormatter.toDateString(cd) : fileout;

      // fetch or create the MessageWriter
      MessageWriter writer = writers.get(writerName);
      if (writer == null) {
        try {
          File file2write;

          if (useCatSubdirs) {
            BufrTableLookup lookup = m.getLookup();
            //String centerDir = "Center-"+lookup.getCenter()+"."+lookup.getSubCenter();
            //File dir1 = new File(dispatchDir, centerDir);
            //if (!dir1.exists() && !dir1.mkdirs()) logger.warn("Failed to make "+dir1.getPath());
            String catDir = "Category-" + lookup.getCategory() + "." + lookup.getSubCategory();
            File dir2 = new File(dispatchDir, catDir);
            if (!dir2.exists() && !dir2.mkdirs()) logger.warn("Failed to make " + dir2.getPath());
            String filename = (dayRollover) ? fileout + "." + CalendarDateFormatter.toDateString(cd) : fileout;
            file2write = new File(dir2, filename + ".bufr");

            System.out.printf("Start %s internalTableMessages=%d%n   %s%n   %s%n", file2write, bufrTableMessages.size(), lookup.getCenterName(), lookup.getCategoryFullName());

          } else {
            file2write = new File(dispatchDir, writerName + ".bufr");
          }

          writer = new MessageWriter(file2write, fileno, bufrTableMessages);
          System.out.printf("Start %s internalTableMessages=%d%n", file2write, bufrTableMessages.size());

          writers.put(writerName, writer);
          fileno++;

        } catch (FileNotFoundException e) {
          e.printStackTrace();
          return false;
        }
      }

      writer.write(m); // put it on the write queue
      if (showMatch) System.out.println("match <" + m.getHeader() + ">");
      match++;

      return true;
    }

  }

  private static class MessTypeSorter implements Comparator<MessType> {
    public int compare(MessType o1, MessType o2) {
      //return o2.count - o1.count; // largest first
      return o1.name.compareTo(o2.name);
    }
  }
}

