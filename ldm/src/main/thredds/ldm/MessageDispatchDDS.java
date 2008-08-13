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

package thredds.ldm;

import ucar.bufr.Message;
import ucar.bufr.Dump;

import java.io.*;
import java.util.*;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;

/**
 * BUFR Message Dispatch using pattern matching on the WMO header
 *
 * @author caron
 * @since Aug 11, 2008
 */
public class MessageDispatchDDS {
  private Set<Integer> badHashSet = new HashSet<Integer>(200);
  private Map<String,Integer> nameMap = new HashMap<String,Integer>(200);
  private Map<Integer, MessType> typeMap = new HashMap<Integer, MessType>(200);

  int total_msgs = 0;
  int match = 0;
  int badCount = 0;
  //int mixedDDS = 0;
  //int duplicates = 0;
  int ignored = 0;

  int total_bytes, total_obs;

  boolean showMatch = false;
  boolean showBad = false;

  Dump dumper = new Dump();
  WritableByteChannel badWbc;

  void dispatch(Message m) {
    total_msgs++;
    total_bytes += m.getRawBytes().length;
    total_obs += m.getNumberDatasets();

    boolean bad = false;
    try {
      bad = m.isTablesComplete() && !m.isBitCountOk();
    } catch (Exception e) {
      bad = true;
    }
    if (bad) {
      if (!badHashSet.contains(m.hashCode())) { // only write one kind of each bad message
        badHashSet.add(m.hashCode());
        try {
          badWbc.write(ByteBuffer.wrap(m.getHeader().getBytes()));
          badWbc.write(ByteBuffer.wrap(m.getRawBytes()));

        } catch (IOException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }

      if (showBad) System.out.println("bad <" + m.header + ">");
      badCount++;
    }

    // find the message type, based on dds
    MessType matched = typeMap.get(m.hashCode());

    if (matched == null) {
      matched = new MessType(m);
      typeMap.put(m.hashCode(), matched);

    } else if (matched.ignore) {
      ignored++;

    }

    matched.count++;
    matched.countObs += m.getNumberDatasets();
    matched.countBytes += m.getRawBytes().length + m.getHeader().length();
    try {
      WritableByteChannel wbc = matched.getWBC();
      wbc.write(ByteBuffer.wrap(m.getHeader().getBytes()));
      wbc.write(ByteBuffer.wrap(m.getRawBytes()));
      if (showMatch) System.out.println("match <" + m.header + ">");
      match++;

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void exit() throws IOException {
    for (MessType mtype : typeMap.values())
      mtype.close();
    badWbc.close();

    Formatter out = new Formatter(System.out);
    Formatter cfg = new Formatter(new FileOutputStream("D:/bufr/dispatch.csv"));

    out.format("\n===============================================\n");
    cfg.format("    hash, name, nmess, nobs, kBytes, complete, bitsOk, center, table, edition, category, filename%n");

    int avg_msg = (int) (total_bytes / total_msgs);
    int avg_obs = (int) (total_obs / total_msgs);
    out.format("total_msgs=%d total_obs=%d total_bytes=%d avg_msg_size=%d avg_obs/msg=%d \n",
            total_msgs, total_obs, total_bytes, avg_msg, avg_obs);
    out.format("matched=%d types=%d bad=%d badTypes=%d ignored=%d %n",
            match, typeMap.size(), badCount, badHashSet.size(), ignored);

    List<MessType> mtypes = new ArrayList<MessType>(typeMap.values());
    Collections.sort(mtypes, new MessTypeSorter());
    for (MessType mtype : mtypes) {
      Message m = mtype.proto;
      boolean hasBadMessages = badHashSet.contains(m.hashCode()); // did we find any messages that fail bit count ??

      out.format(" MessType %s count=%d fileout= %s\n", mtype.name, mtype.count, mtype.fileout);
      cfg.format("0x%x, %s, %5d, %8d, %5d, %5s, %5s, %s, %s, %s, %s, %s %n", m.hashCode(), mtype.name,
              mtype.count, mtype.countObs, mtype.countBytes/1000,
              m.isTablesComplete(), !hasBadMessages,
              m.getCenterNo(), m.getTableName(), m.is.getBufrEdition(), m.getCategoryNo(),
              mtype.fileout);
    }
    out.format("\n");
    cfg.close();
  }

  MessageDispatchDDS(String filename) throws IOException {
    /* BufferedReader dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.length() == 0) break;
      if (line.charAt(0) == '#') continue;

      String[] tokes = line.split(";");
      String pats = tokes[0].trim();

      if (tokes.length == 1)
        typeMap.add(new MessType(".*" + pats + ".*"));
      else  {
        String fileout = tokes[1].trim();
        if (fileout.length() > 0)
          pqactList.add(new Pqact(".*" + pats + ".*", fileout));
        else
          pqactList.add(new Pqact(".*" + pats + ".*"));

      }

      count++;
    } */

    File file = new File("D:/bufr/dispatch/bad.bufr");
    FileOutputStream fos = new FileOutputStream(file);
    badWbc = fos.getChannel();
  }


  private class MessType {
    Message proto;
    String name;
    String fileout;
    int count, countObs, countBytes;
    boolean ignore;

    MessType(Message proto) {
      this.proto = proto;

      this.name = extractName(proto.getHeader());
      Integer nameCount = nameMap.get(this.name);
      if (nameCount != null) {
        nameCount = new Integer( nameCount + 1);
        nameMap.put(this.name, nameCount); // increment this name counter
        this.name = name+"-"+nameCount;  // form a new one to avoid filename collision
      }
      nameMap.put(this.name, 0);

      this.fileout = name;
      this.ignore = fileout.equalsIgnoreCase("ignore");
      System.out.println(" add <" + name + "> fileout= " + fileout);
    }

    String extractName(String header) {
      return header.substring(7, 11) + "-" + header.substring(0, 6);
    }

    WritableByteChannel wbc;
    FileOutputStream fos;

    WritableByteChannel getWBC() throws FileNotFoundException {
      if (wbc == null) {
        File file = new File("D:/bufr/dispatch/", fileout + ".bufr");
        fos = new FileOutputStream(file);
        wbc = fos.getChannel();
      }
      return wbc;
    }

    void close() throws IOException {
      if (wbc != null)
        wbc.close();
      if (fos != null)
        fos.close();
    }

  }

  private static class MessTypeSorter implements Comparator<MessType> {
    public int compare(MessType o1, MessType o2) {
      //return o2.count - o1.count; // largest first
      return o1.name.compareTo(o2.name); 
    }
  }
}
