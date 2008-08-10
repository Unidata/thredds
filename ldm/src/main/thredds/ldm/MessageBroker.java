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
import java.util.List;
import java.util.Formatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import ucar.bufr.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.InMemoryRandomAccessFile;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 8, 2008
 */
public class MessageBroker {
  Executor executor;
  Thread messProcessor;
  LinkedBlockingQueue<MessageTask> messQ = new LinkedBlockingQueue<MessageTask>();

  //CompletionService<Result> completionService;
  ucar.unidata.io.KMPMatch matcher = new ucar.unidata.io.KMPMatch("BUFR".getBytes());

  MessageBroker(Executor executor) {
    this.executor = executor;

    // start up a thread for processing messages as they come off the wire
    messProcessor = new Thread(new MessageProcessor());
    messProcessor.start();
    //ExecutorService messProcessor = Executors.newSingleThreadExecutor();
    //this.completionService = new ExecutorCompletionService<Result>(executor);
  }

  void exit() {
    messProcessor.interrupt();
  }

  private class MessageProcessor implements Runnable {
    private boolean cancel = false;

    public void run() {
      while (true) {
        try {
          if (cancel && messQ.peek() == null) break;
          process(messQ.take()); // bloccks until a task is ready
        } catch (InterruptedException e) {
          cancel = true;
        }
      }
    }

    void process(MessageTask mtask) {
      System.out.println("*** process messsage " + mtask.id);
      try {
        Message m = getMessage(new InMemoryRandomAccessFile("BUFR", mtask.mess));
        new Dump().dumpHeader(new Formatter(System.out), m);
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

    MessageTask(int messlen) {
      this.len = messlen;
      this.mess = new byte[messlen];
      this.have = 0;
      this.id = seqId.getAndIncrement();
    }

  }

  public void process(InputStream is) throws IOException {

    while (true) {
      Buffer b = readBuffer(is);
      process(b, is);
      if (b.done) break;
    }
  }

  private void process(Buffer b, InputStream is) throws IOException {
    int start = 0;
    while (start < b.have) {
      int matchPos = matcher.indexOf(b.buff, start, b.have - start);
      if (matchPos < 0) break; // LOOK

      // read BUFR message length
      int b1 = (b.buff[matchPos + 4] & 0xff);
      int b2 = (b.buff[matchPos + 5] & 0xff);
      int b3 = (b.buff[matchPos + 6] & 0xff);
      int messLen = b1 << 16 | b2 << 8 | b3;
      System.out.println("match at=" + matchPos + " len= " + messLen);
      int last = matchPos + messLen;

      // create a task for this message
      MessageTask task = new MessageTask(messLen);

      // copy message bytes into it
      if (last > b.have) {
        task.have = b.have - matchPos;
        System.arraycopy(b.buff, matchPos, task.mess, 0, task.have);

        // read the rest of the message
        if (!readBuffer(is, task.mess, task.have, messLen - task.have)) {
          System.out.println("Failed to read remaining BUFR message");
          break;
        }

      } else {
        task.have = messLen;
        System.arraycopy(b.buff, matchPos, task.mess, 0, task.have);
      }

      // check on ending
      for (int i = messLen - 4; i < messLen; i++) {
        int bb = task.mess[i];
        if (bb != 55)
          System.out.println("Missing End of BUFR message at pos=" + i + " " + bb);
      }

      messQ.add(task);
      System.out.println(" added message " + task.id + " start=" + matchPos + " end= " + (matchPos + messLen));

      start = matchPos + messLen + 1;
    }
  }

  // read until buffer is full or end of stream
  private boolean readBuffer(InputStream is, byte[] dest, int start, int want) throws IOException {
    int done = 0;
    while (done < want) {
      int got = is.read(dest, start + done, want - done);
      if (got < 0)
        return false;
      done += got;
    }
    //System.out.println("Read buffer " + done);
    return true;
  }

  // read until buffer is full or end of stream
  private Buffer readBuffer(InputStream is) throws IOException {
    Buffer b = new Buffer();
    int want = buffsize;
    while (b.have < want) {
      int got = is.read(b.buff, b.have, want - b.have);
      if (got < 0) {
        b.done = true;
        break;
      }
      b.have += got;
    }
    //System.out.println("Read buffer " + b.have);
    return b;
  }

  private int buffsize = 15000;

  private class Buffer {
    byte[] buff;
    int have;
    boolean done;

    Buffer() {
      buff = new byte[buffsize];
      have = 0;
      done = false;
    }
  }

}
