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
          out.format(" DDS doesnt match pqact= %s %n", pats);
          first.dumpHeader(out);
          m.dumpHeader(out);
          out.format("%n");
          mixedDDS++;
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
