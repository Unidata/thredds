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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.bufr.*;
import ucar.unidata.io.InMemoryRandomAccessFile;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;

/**
 * Reads BUFR files and splits them into separate files based on Message.hashCode()
 *
 * @author caron
 * @since 6/12/13
 */
public class BufrSplitter {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BufrSplitter.class);
  private static final ucar.unidata.io.KMPMatch matcher = new ucar.unidata.io
          .KMPMatch(new byte[]{'B', 'U', 'F', 'R'});

  File dirOut;
  MessageDispatchDDS dispatcher;
  Formatter out;

  public BufrSplitter(String dirName, Formatter out) throws IOException {
    this.out = out;
    dirOut = new File(dirName);
    if (dirOut.exists() && !dirOut.isDirectory())  {
      throw new IllegalArgumentException(dirOut+" must be a directory");
    } else if (!dirOut.exists()) {
      if (!dirOut.mkdirs())
        throw new IllegalArgumentException(dirOut+" filed to create");
    }
    dispatcher = new MessageDispatchDDS(null, dirOut);
  }

  // LOOK - needs to be a directory, or maybe an MFILE collection
  public void execute(String filename) throws IOException {
    File input = new File(filename);
    out.format("BufrSplitter on %s length=%d%n", input.getPath(), input.length());
    try (InputStream is = new FileInputStream(input)) {
      processStream(is);
    }
  }

  public void exit() {
    dispatcher.exit(out);
  }

  //////////////////////////////////////////////////////
  // Step 1 - read and extract a Bufr Message

  int total_msgs = 0;
  int bad_msgs = 0;

  // process all the bytes in the stream
  public void processStream(InputStream is) throws IOException {
    int pos = -1;
    Buffer b = null;
    while (true) {
      b = (pos < 0) ? readBuffer(is) : readBuffer(is, b, pos);
      pos = processBuffer(b, is);
      if (b.done) break;
    }
  }

  /**
   *
   * @param b buffer of input data
   * @param is InputStream to read
   * @return pos in the buffer we got to
   * @throws IOException
   */
  private int processBuffer(Buffer b, InputStream is) throws IOException {
    int start = 0;
    while (start < b.have) {
      int matchPos = matcher.indexOf(b.buff, start, b.have - start);

      // didnt find "BUFR" match
      if (matchPos < 0) {
        if (start == 0) // discard all but last 3 bytes
          return b.have - 3;
        else
          return start; // indicates part of the buffer thats not processed
      }

      // do we have the length already read ??
      if (matchPos + 6 >= b.have) {
        return start; // this will save the end of the buffer and read more in.
      }

      // read BUFR message length
      int b1 = (b.buff[matchPos + 4] & 0xff);
      int b2 = (b.buff[matchPos + 5] & 0xff);
      int b3 = (b.buff[matchPos + 6] & 0xff);
      int messLen = b1 << 16 | b2 << 8 | b3;
      System.out.println("match at=" + matchPos + " len= " + messLen);

      // create a task for this message
      //int headerLen = matchPos - start;
      MessageTask task = new MessageTask(messLen);
      task.extractHeader(start, matchPos, b);

      // copy message bytes into it
      int last = matchPos + messLen;
      if (last > b.have) {
        task.have = b.have - matchPos;
        System.arraycopy(b.buff, matchPos, task.mess, 0, task.have);

        // read the rest of the message
        if (!readBuffer(is, task.mess, task.have, task.len - task.have)) {
          logger.warn("Failed to read remaining BUFR message");
          break;
        }

      } else {
        task.have = task.len;
        System.arraycopy(b.buff, matchPos, task.mess, 0, task.have);
      }

      boolean ok = true;

      // check on ending
      for (int i = task.len - 4; i < task.len; i++) {
        int bb = task.mess[i];
        if (bb != 55) {
          //System.out.println("Missing End of BUFR message at pos=" + i + " " + bb);
          ok = false;
          bad_msgs++;
        }
      }

      if (ok) processMessageTask(task);
      total_msgs++;

      start = matchPos + messLen + 1;
    }

    return -1;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  // extract and parse a BUFR message so that we know its DDS type

  private static class MessageTask {
    byte[] mess;
    int len, have;
    String header;

    MessageTask(int messlen) {
      this.len = messlen;
      this.mess = new byte[messlen];
      this.have = 0;
    }

    void extractHeader(int startScan, int messagePos, Buffer buff) {
      int sizeHeader = messagePos - startScan;
      if (sizeHeader > 30) sizeHeader = 30;
      byte[] headerb = new byte[sizeHeader];
      int startHeader = messagePos - sizeHeader;
      System.arraycopy(buff.buff, startHeader, headerb, 0, sizeHeader);

      // cleanup
      int start;
      for (start = 0; start < headerb.length; start++) {
        byte b = headerb[start];
        if ((b == 73) || (b == 74)) // I or J
          break;
      }

      byte[] bb = new byte[sizeHeader];
      int count = 0;
      for (int i = start; i < headerb.length; i++) {
        byte b = headerb[i];
        if (b >= 32 && b < 127) // ascii only
          bb[count++] = b;
      }
      header = new String(bb, 0, count, CDM.utf8Charset);
    }

  }

  void processMessageTask(MessageTask mtask) {
    //out.format("    %d start process %n", mtask.id);
    try {
      Message m = getMessage(new InMemoryRandomAccessFile("BUFR", mtask.mess));
      if (null == m) return;
      m.setHeader(mtask.header);
      m.setRawBytes(mtask.mess);

      // decide what to do with the message
      dispatcher.dispatch(m);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Message getMessage(RandomAccessFile raf) throws IOException {
    raf.seek(4);
    BufrIndicatorSection is = new BufrIndicatorSection(raf);
    BufrIdentificationSection ids = new BufrIdentificationSection(raf, is);
    BufrDataDescriptionSection dds = new BufrDataDescriptionSection(raf);

    long dataPos = raf.getFilePointer();
    int dataLength = BufrNumbers.uint3(raf);
    BufrDataSection dataSection = new BufrDataSection(dataPos, dataLength);

    if ((is.getBufrEdition() > 4) || (is.getBufrEdition() < 2)) {
      logger.warn("Edition " + is.getBufrEdition() + " is not supported");
      bad_msgs++;
      return null;
    }

    return new Message(raf, is, ids, dds, dataSection);
  }


  /////////////////////////////////////////////////////////////////////////////
  // efficiently read from an input stream

  private boolean showRead = false;
  private long bytesRead = 0;
  private int BUFFSIZE = 15000;

  private class Buffer {
    byte[] buff;
    int have;
    boolean done;

    Buffer() {
      buff = new byte[BUFFSIZE];
      have = 0;
      done = false;
    }
  }

  // read into dest byte array, until buffer is full or end of stream
  private boolean readBuffer(InputStream is, byte[] dest, int start, int want) throws IOException {
    int done = 0;
    while (done < want) {
      int got = is.read(dest, start + done, want - done);
      if (got < 0)
        return false;
      done += got;
    }

    if (showRead) System.out.println("Read buffer at " + bytesRead + " len=" + done);
    bytesRead += done;
    return true;
  }

  // read into new Buffer, until buffer is full or end of stream
  private Buffer readBuffer(InputStream is) throws IOException {
    Buffer b = new Buffer();
    int want = BUFFSIZE;
    while (b.have < want) {
      int got = is.read(b.buff, b.have, want - b.have);
      if (got < 0) {
        b.done = true;
        break;
      }
      b.have += got;
    }
    if (showRead) System.out.println("Read buffer at " + bytesRead + " len=" + b.have);
    bytesRead += b.have;
    return b;
  }

  // read into new Buffer, until buffer is full or end of stream
  private Buffer readBuffer(InputStream is, Buffer prev, int pos) throws IOException {
    Buffer b = new Buffer();

    // copy remains of last buffer here
    int remain = prev.have - pos;
    //if (remain > BUFFSIZE /2)
    //  out.format(" remain = "+remain+" bytesRead="+bytesRead);

    System.arraycopy(prev.buff, pos, b.buff, 0, remain);
    b.have = remain;


    int want = BUFFSIZE;
    while (b.have < want) {
      int got = is.read(b.buff, b.have, want - b.have);
      if (got < 0) {
        b.done = true;
        break;
      }
      b.have += got;
    }

    if (showRead) System.out.println("Read buffer at " + bytesRead + " len=" + b.have);
    bytesRead += b.have;
    return b;
  }

  ///////////////////////////////////////////////////////////////////////////

  private static class CommandLine {
    @Parameter(names = {"--fileSpec"}, description = "File specification", required = true)
    public File fileSpec;

    @Parameter(names = {"--dirOut"}, description = "Output directory", required = true)
    public File dirOut;

    @Parameter(names = {"-h", "--help"}, description = "Display this help and exit", help = true)
    public boolean help = false;

    private final JCommander jc;

    public CommandLine(String progName, String[] args) throws ParameterException {
      this.jc = new JCommander(this, args);  // Parses args and uses them to initialize *this*.
      jc.setProgramName(progName);           // Displayed in the usage information.
    }

    public void printUsage() {
      jc.usage();
    }
  }

  public static void main(String[] args) throws Exception {
    String progName = "BufrSpitter";

    try {
      CommandLine cmdLine = new CommandLine(progName, args);

      if (cmdLine.help) {
        cmdLine.printUsage();
        return;
      }

      BufrSplitter splitter = new BufrSplitter(cmdLine.dirOut.getAbsolutePath(), new Formatter(System.out));
      splitter.execute(cmdLine.fileSpec.getAbsolutePath());
      splitter.exit();
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.err.printf("Try \"%s --help\" for more information.%n", progName);
    }
  }
}
