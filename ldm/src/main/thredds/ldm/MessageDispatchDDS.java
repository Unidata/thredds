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

package thredds.ldm;

import ucar.nc2.iosp.bufr.Message;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletionService;
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

  private String dispatchDir;
  private String inputFilenameOut;

  private Set<Integer> badHashSet = new HashSet<Integer>(200);
  private Map<String, Integer> nameMap = new HashMap<String, Integer>(200);
  private Map<Integer, MessType> typeMap = new HashMap<Integer, MessType>(200);

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
  boolean showResults = false;
  boolean useSubdirs = false;
  boolean writeIndex = false;
  boolean writeSamples = true;

  WritableByteChannel badWbc;
  WritableByteChannel sampleWbc;
  WritableByteChannel allWbc;

  private final CompletionService<IndexerTask> executor;

  /**
   * Wired by Spring
   * @param executor
   * @param inputFilename leave null for fresh start
   * @param dispatchDir
   * @throws IOException
   */
  MessageDispatchDDS(CompletionService<IndexerTask> executor, String inputFilename, String dispatchDir) throws IOException {
    this.executor = executor;
    this.dispatchDir = dispatchDir;

    File dispatchDirFile = new File(dispatchDir);
    dispatchDirFile.mkdirs();

    if (writeSamples) {
      File file = new File(dispatchDir + "abad.bufr");
      FileOutputStream fos = new FileOutputStream(file);
      badWbc = fos.getChannel();

      file = new File(dispatchDir + "asample.bufr");
      fos = new FileOutputStream(file);
      sampleWbc = fos.getChannel();

      file = new File(dispatchDir + "asampleAll.bufr");
      fos = new FileOutputStream(file);
      allWbc = fos.getChannel();
    }

    // MessageWriter.setRootDir(dispatchDir);

    // read config file
    if (inputFilename != null) {
      File inputFile = new File(inputFilename);
      if (inputFile.exists()) {
        BufferedReader dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
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

  public void setInputFilenameOut(String inputFilenameOut) {
    this.inputFilenameOut = inputFilenameOut;
  }

  void dispatch(Message m) {
    total_msgs++;
    total_bytes += m.getRawBytes().length;
    total_obs += m.getNumberDatasets();

    // find the message type, based on dds
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
          badWbc.write(ByteBuffer.wrap(m.getHeader().getBytes()));
          badWbc.write(ByteBuffer.wrap(m.getRawBytes()));

        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      if (showBad) System.out.println("bad <" + m.getHeader() + ">");
      badCount++;
      badBytes += m.getRawBytes().length + m.getHeader().length();
    }
    return isBad;
  }

  void writeSample(Message m, WritableByteChannel wbc) {
    if (!writeSamples) return;

    try {
      wbc.write(ByteBuffer.wrap(m.getHeader().getBytes()));
      wbc.write(ByteBuffer.wrap(m.getRawBytes()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void exit() {
    try {
      for (MessageWriter writer : writers.values())
        writer.close();
      
      for (MessType mtype : typeMap.values())
        if (mtype.indexer != null) mtype.indexer.close();

      if (badWbc != null) badWbc.close();
      if (sampleWbc != null) sampleWbc.close();
      if (allWbc != null) allWbc.close();

      Formatter out = new Formatter(System.out);
      Formatter cfg = new Formatter(new FileOutputStream(inputFilenameOut));

      if (showResults) out.format("\n===============================================\n");
      cfg.format("#    hash, filename, wmo, index, nmess, nobs, kBytes, complete, bitsOk, nbad, center, table, edition, category%n");

      int avg_msg = (total_msgs == 0) ? 0 : (int) (total_bytes / total_msgs);
      int avg_obs = (total_msgs == 0) ? 0 : (total_obs / total_msgs);
      if (showResults) {
        out.format("total_msgs=%d total_obs=%d total_bytes=%d avg_msg_size=%d avg_obs/msg=%d \n",
                total_msgs, total_obs, total_bytes, avg_msg, avg_obs);
        out.format("matched=%d types=%d bad=%d badTypes=%d badBytes=%d ignored=%d failWrite=%d %n",
                match, typeMap.size(), badCount, badHashSet.size(), badBytes, ignored, failWrite);
      }

      List<MessType> mtypes = new ArrayList<MessType>(typeMap.values());
      Collections.sort(mtypes, new MessTypeSorter());
      for (MessType mtype : mtypes) {
        if (mtype.proto == null) {
          if (showResults) out.format(" MessType %s count=%d fileout= %s\n", mtype.name, mtype.count, mtype.fileout);
          cfg.format("Ox%x, %s, %s, %s, %5d, %8d, %5f %n",
                  mtype.hash, mtype.fileout, mtype.name, mtype.index,
                  mtype.count, mtype.countObs, mtype.countBytes / 1000);
          continue;
        }
        Message m = mtype.proto;
        boolean hasBadMessages = badHashSet.contains(m.hashCode()); // did we find any messages that fail bit count ??
        if (showResults) out.format(" MessType %s count=%d fileout= %s\n", mtype.name, mtype.count, mtype.fileout);
        cfg.format("Ox%x, %s, %s, %s, %5d, %8d, %5f, %5s, %5s, %d, %s, %s, %s, %s%n",
                mtype.hash, mtype.fileout, mtype.name, mtype.index,
                mtype.count, mtype.countObs, mtype.countBytes / 1000,
                m.isTablesComplete(), !hasBadMessages, mtype.nbad,
                m.getCenterNo(), m.getTableName(), m.is.getBufrEdition(), m.getCategoryNo());
      }
      if (showResults) out.format("\n");
      cfg.close();
    } catch (IOException ioe) {
      logger.error("MessageDispatchDDS.exit", ioe);
    }
  }

  // Message type based on Message.hashCode
  private class MessType {
    int hash;
    String name;
    String fileout;
    String index;
    Indexer indexer;
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

      this.name = extractName(proto.getHeader());
      Integer nameCount = nameMap.get(this.name);
      if (nameCount != null) {
        nameCount++;
        nameMap.put(this.name, nameCount); // increment this name counter
        this.name = name + "-" + nameCount;  // form a new one to avoid filename collision
      }
      nameMap.put(this.name, 0);

      this.fileout = name;
      System.out.println(" add new MessType <" + name + "> fileout= " + fileout);
    }

    String extractName(String header) {
      return header.substring(7, 11) + "-" + header.substring(0, 6);
    }

    boolean scheduleWrite(Message m) {
      String writerName;
      Calendar cal = null;

      if (useSubdirs) {
        GregorianCalendar gcal = new GregorianCalendar(); // ??

        cal = Calendar.getInstance();
        cal.setTime(m.ids.getReferenceTime(gcal));  // LOOK
        int day = cal.get(Calendar.DAY_OF_MONTH);
        writerName = fileout + "-" + day;
      } else {
        writerName = fileout;
      }

      // fetch or create the MessageWriter
      MessageWriter writer = writers.get(writerName);
      if (writer == null) {
        try {
          File file;

          if (useSubdirs) {
            File dir = new File(dispatchDir + fileout);
            dir.mkdirs();
            String date = cal.get( Calendar.YEAR)+"-"+(1+cal.get( Calendar.MONTH))+"-"+cal.get( Calendar.DAY_OF_MONTH);
            file = new File(dir, fileout+"-"+date+  ".bufr");

          } else {
            File dir = new File(dispatchDir);
            dir.mkdirs();
            file = new File(dir, writerName + ".bufr");
          }

          writer = new MessageWriter(executor, indexer, file, fileno);
          writers.put(writerName, writer);
          fileno++;
          
        } catch (FileNotFoundException e) {
          e.printStackTrace();
          return false;
        }
      }

      writer.scheduleWrite(m); // put it on the write queue
      if (showMatch) System.out.println("match <" + m.getHeader() + ">");
      match++;

      return true;
    }

  }

  Map<String, MessageWriter> writers = new HashMap<String, MessageWriter>(100);

  private static class MessTypeSorter implements Comparator<MessType> {
    public int compare(MessType o1, MessType o2) {
      //return o2.count - o1.count; // largest first
      return o1.name.compareTo(o2.name);
    }
  }
}
