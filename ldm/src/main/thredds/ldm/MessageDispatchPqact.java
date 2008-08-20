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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;

/**
 * BUFR Message Dispatch using pattern matching on the WMO header
 *
 * @author caron
 * @since Aug 11, 2008
 */
public class MessageDispatchPqact {
  private List<Pqact> pqactList;

  int total_msgs = 0;
  int match = 0;
  int nomatch = 0;
  int mixedDDS = 0;
  int duplicates = 0;
  int ignored = 0;

  int total_bytes, total_obs;

  boolean showMatch = false;

  Dump dumper = new Dump();
  Formatter out = new Formatter(System.out);
  WritableByteChannel unmatchWbc;


  void dispatch(Message m) {
    total_msgs++;
    total_bytes += m.getRawBytes().length;
    total_obs += m.getNumberDatasets();

    // run through the pattern matching
    Pqact matched = null;
    for (Pqact pqact : pqactList) {
      boolean match = pqact.match(m.getHeader(), m);
      if ((matched != null) && match) {
        duplicates++;
        System.out.println("double match <" + m.getHeader() + "> with " + matched.pats + " and " + pqact.pats);
      }
      if (match)
        matched = pqact;
    }

    if (matched == null) {
      try {
        unmatchWbc.write(ByteBuffer.wrap(m.getHeader().getBytes()));
        unmatchWbc.write(ByteBuffer.wrap(m.getRawBytes()));
        if (showMatch) System.out.println("no match <" + m.getHeader() + ">");
        nomatch++;

      } catch (IOException e) {
        e.printStackTrace();
      }

      if (showMatch) System.out.println("no match <" + m.getHeader() + ">");
      nomatch++;

    } else if (matched.ignore) {
      ignored++;

    } else {
      try {
        WritableByteChannel wbc = matched.getWBC();
        wbc.write(ByteBuffer.wrap(m.getHeader().getBytes()));
        wbc.write(ByteBuffer.wrap(m.getRawBytes()));
        if (showMatch) System.out.println("match <" + m.getHeader() + ">");
        match++;

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  void exit() throws IOException {
    for (Pqact pqact : pqactList) {
      pqact.close();
    }
    unmatchWbc.close();

    out.format("\n===============================================\n");


    int avg_msg = (int) (total_bytes / total_msgs);
    int avg_obs = (int) (total_obs / total_msgs);
    out.format("total_msgs=%d total_obs=%d total_bytes=%d avg_msg_size=%d avg_obs/msg=%d \n", total_msgs, total_obs, total_bytes, avg_msg, avg_obs);
    out.format("matched=%d nomatch=%d ignored=%d mixedDDS=%d duplicates=%d%n", match, nomatch, ignored, mixedDDS, duplicates);

    Collections.sort(pqactList, new PqactSorter());
    for (Pqact pqact : pqactList) {
      if (pqact.count > 0)
        out.format(" Pqact %s count=%d fileout= %s\n", pqact.pats, pqact.count, pqact.fileout);
      //if (pqact.first != null) {
      //  dumper.dumpHeader(out, pqact.first);
      //out.format("  Example file=%s\n", pqact.exampleFile);
    }
    out.format("\n");
  }


  MessageDispatchPqact(String filename) throws IOException {
    pqactList = new ArrayList<Pqact>();

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
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
        pqactList.add(new Pqact(".*" + pats + ".*"));
      else  {
        String fileout = tokes[1].trim();
        if (fileout.length() > 0)
          pqactList.add(new Pqact(".*" + pats + ".*", fileout));
        else
          pqactList.add(new Pqact(".*" + pats + ".*"));

      }

      count++;
    }

    File file = new File("D:/bufr/dispatch/unmatched.bufr");
    FileOutputStream fos = new FileOutputStream(file);
    unmatchWbc = fos.getChannel();
  }


  private class Pqact {
    String pats;
    Pattern pattern;
    Message first;
    String fileout;
    int count;
    boolean ignore;

    Pqact(String pats) {
      this.pats = pats;
      this.fileout = cleanup(pats);
      System.out.println(" add <"+pats+">");
      pattern = Pattern.compile(pats);
    }

    Pqact(String pats, String fileout) {
      this.pats = pats;
      this.fileout = fileout;
      this.ignore = fileout.equalsIgnoreCase("ignore");
      System.out.println(" add <"+pats+"> fileout= "+fileout);
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
      } else if (m.hashCode() != first.hashCode() && !ignore) {
        try {
          out.format(" DDS doesnt match pqact= %s %n", pats);
          dumper.dumpHeader(out, first);
          dumper.dumpHeader(out, m);
          out.format("%n");
          mixedDDS++;
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

  private static class PqactSorter implements Comparator<Pqact> {
    public int compare(Pqact o1, Pqact o2) {
      return o2.count - o1.count; // largest first
    }
  }
}
