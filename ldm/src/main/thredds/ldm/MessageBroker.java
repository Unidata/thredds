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

import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import ucar.bufr.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.InMemoryRandomAccessFile;

/**
 * Recieves data from an InputStream, breaks into BUFR messages by scanning for "BUFR" header.
 * Writes them out using regexp on the header, like pqact does.
 *
 * Multithreaded:
 *   main thread for reading input stream, and breaking into messages
 *   seperate thread for processing messages
 *   seperate thread for each output.
 *
 * @author caron
 * @since Aug 8, 2008
 */
public class MessageBroker {
  private static final ucar.unidata.io.KMPMatch matcher = new ucar.unidata.io.KMPMatch("BUFR".getBytes());

  private Executor executor;
  private Thread messProcessor;
  private LinkedBlockingQueue<MessageTask> messQ = new LinkedBlockingQueue<MessageTask>(10);
  private Formatter out = new Formatter(System.out);

  private MessageDispatchDDS dispatch;

  //CompletionService<Result> completionService;

  public MessageBroker(Executor executor) throws IOException {
    this.executor = executor;

    // start up a thread for processing messages as they come off the wire
    messProcessor = new Thread(new MessageProcessor());
    messProcessor.start();
    //ExecutorService messProcessor = Executors.newSingleThreadExecutor();
    //this.completionService = new ExecutorCompletionService<Result>(executor);

    dispatch = new MessageDispatchDDS("D:/bufr/dispatch.txt");
  }

  public void exit() throws IOException {
    // wait util all tasks are complete
    while (messQ.peek() != null) {
      try {
        Thread.currentThread().sleep(2000); // wait 2 sec
      } catch (InterruptedException e) {
        // not sure what to do
      }
    }

    messProcessor.interrupt();
    dispatch.exit();
  }

  int bad_msgs = 0;
  int total_msgs = 0;

  private class MessageProcessor implements Runnable {

    public void run() {
      while (true) {
        try {
          if (Thread.currentThread().isInterrupted()) break;
          process(messQ.take()); // blocks until a task is ready

        } catch (InterruptedException e) {
          break; // exit thread
        }
      }
      System.out.println("exit MessageProcessor");
    }

    void process(MessageTask mtask) {
      //out.format("    %d start process %n", mtask.id);
      try {
        Message m = getMessage(new InMemoryRandomAccessFile("BUFR", mtask.mess));
        m.setHeader( mtask.header);
        m.setRawBytes( mtask.mess);

        dispatch.dispatch(m);
        //out.format("    %d", mtask.id);
        //new Dump().dumpHeaderShort(out, m);
        
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
      DataSection dataSection = new DataSection(dataPos, dataLength);

      if ((is.getBufrEdition() > 4) || (is.getBufrEdition() < 2)) {
        System.out.println("Edition " + is.getBufrEdition() + " is not supported");
        return null;
      }

      return new Message(raf, is, ids, dds, dataSection);
    }
  }

  private static AtomicInteger seqId = new AtomicInteger();

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

  }

  public void process(InputStream is) throws IOException {
    int pos = -1;
    Buffer b = null;

    while (true) {
      b = (pos < 0) ? readBuffer(is) : readBuffer(is, b, pos);
      pos = process(b, is);
      if (b.done) break;
    }
  }

  // return where in the buffer we got to.
  private int process(Buffer b, InputStream is) throws IOException {
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
      task.header = extractHeader(start, matchPos, b);

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

      try {
        if (ok) messQ.put(task);
        total_msgs++;
        //System.out.println(" added message " + task.id + " start=" + matchPos + " end= " + (matchPos + messLen));
      } catch (InterruptedException e) {
        System.out.println(" interrupted queue put - assume process exit");
        break;
      }

      start = matchPos + messLen + 1;
    }

    return -1;
  }

  private String extractHeader(int startScan, int messagePos, Buffer buff) {
    int sizeHeader = messagePos - startScan;
    if (sizeHeader > 30) sizeHeader = 30;
    byte[] header = new byte[sizeHeader];
    int startHeader = messagePos - sizeHeader;
    System.arraycopy(buff.buff, startHeader, header, 0, sizeHeader);

    // cleanup
    int start = 0;
    for (start=0; start<header.length; start++) {
      byte b = header[start];
      if ((b == 73) || (b == 74)) // I or J
        break;
    }

    byte[] bb = new byte[sizeHeader];
    int count = 0;
    for (int i=start; i<header.length; i++) {
      byte b = header[i];
      if (b >= 32 && b < 127) // ascii only
        bb[count++] = b;
    }
    return new String(bb, 0, count);
  }

  private boolean showRead = false;

  // read into dest byte array, until buffer is full or end of stream
  private boolean readBuffer(InputStream is, byte[] dest, int start, int want) throws IOException {
    int done = 0;
    while (done < want) {
      int got = is.read(dest, start + done, want - done);
      if (got < 0)
        return false;
      done += got;
    }

    if (showRead) System.out.println("Read buffer at " + bytesRead+" len="+done);
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
    if (showRead) System.out.println("Read buffer at " + bytesRead+" len="+b.have);
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

    if (showRead) System.out.println("Read buffer at " + bytesRead+" len="+b.have);
    bytesRead += b.have;
    return b;
  }

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

}
