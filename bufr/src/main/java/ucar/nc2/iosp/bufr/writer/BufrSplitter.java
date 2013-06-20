package ucar.nc2.iosp.bufr.writer;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import ucar.nc2.iosp.bufr.*;
import ucar.unidata.io.InMemoryRandomAccessFile;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads BUFR files and splits them into seperate files based on DDS hash
 *
 * @author caron
 * @since 6/12/13
 */
public class BufrSplitter {
  private static final ucar.unidata.io.KMPMatch matcher = new ucar.unidata.io.KMPMatch("BUFR".getBytes());

  Options options;
  File dirOut;
  MessageDispatchDDS dispatcher;

  public BufrSplitter(Options options) throws IOException {
    this.options = options;
    printOptions(options);

    dirOut = new File(options.getDirOut());
    if (dirOut.exists() && !dirOut.isDirectory())  {
      throw new IllegalArgumentException(dirOut+" must be a directory");
    } else if (!dirOut.exists()) {
      if (!dirOut.mkdirs())
        throw new IllegalArgumentException(dirOut+" filed to create");
    }

    dispatcher = new MessageDispatchDDS(null, dirOut);
  }

  void execute() throws IOException {
    File input = new File(options.getFileSpec());
    System.out.printf("Input %s length=%d%n", input.getPath(), input.length());
    InputStream is = new FileInputStream(input);
    processStream(is);
    dispatcher.exit();
    is.close();
  }

  //////////////////////////////////////////////////////
  // Step 1 - read and extract a Bufr Message

  private static AtomicInteger seqId = new AtomicInteger();
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
   * @param b
   * @param is
   * @return here in the buffer we got to
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
      // System.out.println("match at=" + matchPos + " len= " + messLen);

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
          System.out.println("Failed to read remaining BUFR message");
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

  private class MessageTask {
    byte[] mess;
    int len, have;
    int id;
    String header;

    MessageTask(int messlen) {
      this.len = messlen;
      this.mess = new byte[messlen];
      this.have = 0;
      this.id = seqId.getAndIncrement();
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
      header = new String(bb, 0, count);
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
      System.out.println("Edition " + is.getBufrEdition() + " is not supported");
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

  public interface Options {
      @Option String getFileSpec();
      @Option String getDirOut();
   }

  void printOptions(Options opt) {
    System.out.printf("Options are ok:%n");
    System.out.printf(" fileSpec= %s%n", options.getFileSpec());
    System.out.printf(" dirOut= %s%n", options.getDirOut());
  }

  public static void main(String[] args) {
    try {
      Options options = CliFactory.parseArguments(Options.class, "--fileSpec", "G:/work/manross/gdas.adpupa.t00z.20120603.bufr.le", "--dirOut", "G:/work/manross/splitupa" );
      BufrSplitter splitter = new BufrSplitter(options);
      splitter.execute();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
