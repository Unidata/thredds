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

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.channels.WritableByteChannel;

/**
 * Test scan bufr messages, run through pattern matching like pqact
 *
 * @author caron
 * @since May 9, 2008
 */
public class ScannerPqact extends Scanner {

  static void extract(String filename) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    out.format("Open %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);

    MessageScanner scan = new MessageScanner(raf);
    while (scan.hasNext()) {

      Message m = scan.next();
      if (m == null) {
        out.format(" bad message in file %s %n", filename);
        bad_msgs++;
        continue;
      }
      readBytes += m.is.getBufrLength();

      // incomplete tables
      try {
        if (!m.isTablesComplete()) {
          bad_tables++;
          //out.format(" missing table in file %s %n", filename);
          continue;
        }
      } catch (UnsupportedOperationException e) {
        bad_operation++;
        out.format(" missing operation in file %s, %s %n", filename, e.getMessage());
        continue;

      } catch (Exception e) {
        out.format(" Exception in file %s, %n", filename);
        m.dumpHeader(out);
        continue;
      }

      // track desc to headers
      String ttaaii = extractWMO(m.getHeader());
      if (ttaaii == null) {
        bad_wmo++;
        out.format(" bad wmo header in file %s, %s %n", filename, m.getHeader());
        continue;
      }

      //readBytes += m.is.getBufrLength();

      // run through the pattern matching
      boolean hasMatch = false;
      for (Pqact pqact : pqactList) {
        if (pqact.match(ttaaii, m)) {
          hasMatch = true;
          writeBytes+= scan.writeCurrentMessage(pqact.getWBC());
          writemsg++;
          break;
        }
      }

      if (!hasMatch && (wbc != null)) {
        writemsg++;
        writeBytes+= scan.writeCurrentMessage(wbc);
      }
    }
    raf.close();
    total_msgs += scan.getTotalMessages();

  }

  /////////////////////////////////////////////////////////////////////

  static int nomatch, badmatch, writemsg;
  static long writeBytes, readBytes;

  static void scanMessageTypesPqact(String filename) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    //out.format("\n-----\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);
    file_size += raf.length();

    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {

      Message m = scan.next();
      if (m == null) {
        out.format(" bad message in file %s %n", filename);
        bad_msgs++;
        continue;
      }

      // incomplete tables
      try {
        if (!m.isTablesComplete()) {
          bad_tables++;
          //out.format(" missing table in file %s %n", filename);
          continue;
        }
      } catch (UnsupportedOperationException e) {
        bad_operation++;
        out.format(" missing operation in file %s %n", filename);
        if (wbc != null)
          scan.writeCurrentMessage(wbc);
        continue;
      }

      // track desc to headers
      String ttaaii = extractWMO(m.getHeader());
      if (ttaaii == null) {
        bad_wmo++;
        continue;
      }

      // run through the pattern matching
      Pqact matched = null;
      for (Pqact pqact : pqactList) {
        boolean match = pqact.match(ttaaii, m);
        if ((matched != null) && match)
          System.out.println("double match <" + ttaaii + "> with " + matched.pats + " and " + pqact.pats);
        if (match)
          matched = pqact;
      }
      if (matched == null) {
        System.out.println("no match <" + ttaaii + ">");
        nomatch++;
      }

      count++;
    }
    raf.close();
    //out.format("total_msgs= %d good=%d total_obs = %d\n", scan.getTotalMessages(), count, scan.getTotalObs());
    total_msgs += scan.getTotalMessages();
    good_msgs += count;
    total_obs += scan.getTotalObs();
  }

  static void showTypes() throws IOException {

    out.format("\n===============================================\n");
    out.format("total_msgs=%d good_msgs=%d bad_msgs=%d incomplete_tables=%d bad_operation=%d %n",
            total_msgs, good_msgs, bad_msgs, bad_tables, bad_operation);
    out.format(" nomatch=%d badmatch=%d %n", nomatch, badmatch);

    int avg_msg = (int) (file_size / total_msgs);
    int avg_obs = (int) (total_obs / total_msgs);
    out.format("total_bytes=%d total_obs=%d avg_msg_size=%d avg_obs/msg=%d \n", file_size, total_obs, avg_msg, avg_obs);
    out.format("\n");

    Collections.sort(pqactList, new PqactSorter());
    for (Pqact pqact : pqactList) {
      out.format("Pqact %s count=%d fileout= %s\n", pqact.pats, pqact.count, pqact.fileout);
      if (pqact.first != null) {
        pqact.first.dumpHeader(out);
        //out.format("  Example file=%s\n", pqact.exampleFile);
      }
      out.format("\n");
    }
    out.format("\n");
  }

  static private void readPqactTable(String filename) throws IOException {
    pqactList = new ArrayList<Pqact>();

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.length() == 0) break;
      if (line.charAt(0) == '#') continue;

      int pos = line.indexOf(';');
      if (pos < 0)
        pqactList.add(new Pqact(".*" + line + ".*"));
      else {
        String pats = line.substring(0, pos).trim();
        String fileout = line.substring(pos + 1).trim();
        pqactList.add(new Pqact(".*" + pats + ".*", fileout));
      }

      count++;
    }
  }

  static private List<Pqact> pqactList;

  static private class Pqact {
    String pats;
    Pattern pattern;
    Message first;
    String fileout;
    int count;

    Pqact(String pats) {
      this.pats = pats;
      this.fileout = cleanup(pats);
      //System.out.println(" add <"+pats+">");
      pattern = Pattern.compile(pats);
    }

    Pqact(String pats, String fileout) {
      this.pats = pats;
      this.fileout = fileout;
      //System.out.println(" add <"+pats+">");
      pattern = Pattern.compile(pats);
    }

   String cleanup(String pats) {
      char[] cc = new char[pats.length()];
      int count = 0;
      for (char c : pats.toCharArray()) {
        if (Character.isLetterOrDigit(c))
          cc[count++] = c;
      }
      return new String(cc, 0, count);
    }

    boolean match(String header, Message m) {
      Matcher matcher = pattern.matcher(header);
      if (!matcher.matches()) return false;

      if (first == null) {
        first = m;
      } else if (m.hashCode() != first.hashCode()) {
        try {
          System.out.println(" DDS doesnt match pqact= " + pats);
          first.dumpHeader(out);
          m.dumpHeader(out);
          System.out.println();
          badmatch++;
        } catch (IOException e) {
          e.printStackTrace();
        }
        return false;
      }
      count++;
      return true;
    }

    WritableByteChannel wbc;
    FileOutputStream fos;

    WritableByteChannel getWBC() throws FileNotFoundException {
      if (wbc == null) {
        File file = new File("D:/bufr3/", fileout + ".bufr");
        fos = new FileOutputStream(file);
        wbc = fos.getChannel();
      }
      return wbc;
    }

    void close() throws IOException {
      if (wbc != null)
        wbc.close();
    }

  }

  private static class PqactSorter implements Comparator<Pqact> {
    public int compare(Pqact o1, Pqact o2) {
      return o2.count - o1.count; // largest first
    }
  }

  static WritableByteChannel wbc;
  static Formatter out = new Formatter(System.out);

  static public void main(String args[]) throws IOException {

    // read pattern matching
    readPqactTable("D:/bufr/pqact.txt");

    // for saving messages
    File file = new File("D:/bufr/mlodeSorted/unclaimed.bufr");
    FileOutputStream fos = new FileOutputStream(file);
    wbc = fos.getChannel();  // */

    /* analyze pqact table
    test("C:/data/bufr2/mlode/", new MClosure() {
       public void run(String filename) throws IOException {
         scanMessageTypesPqact(filename);
       }
     });
    showTypes(); // */


    // extract based on pqact table
    test("D:/bufr2/", new MClosure() {
      public void run(String filename) throws IOException {
        extract(filename);
      }
    });
    out.format(" scanned %d msgs %d Kb, bad_tables %d, write %d msgs %d Kb %n", total_msgs, readBytes/1000, bad_tables, writemsg, writeBytes/1000);

    for (Pqact pqact : pqactList) {
      pqact.close();
    }

    // */

    if (wbc != null) wbc.close();
  }

}
