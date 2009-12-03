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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.GregorianCalendar;

import ucar.nc2.iosp.bufr.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.InMemoryRandomAccessFile;

/**
 * Recieves data from an InputStream, breaks into BUFR messages by scanning for "BUFR" header.
 * Multithreaded:
 * main thread for reading input stream, and breaking into messages
 * seperate thread for processing messages (MessageDispatch)
 * seperate thread pool (using executor) for writing output.
 * seperate thread for indexing messsage (Indexer)
 * <p/>
 * Processing steps: <ol>
 * <li> the main thread reads from InputStream, finds message boundaries, places messaages onto the message Queue.
 * <li> messProcessor thread waits on message Queue, sends messages to the MessageDispatch, which decides
 * what should be done with a message (ignore, check bad bits, write to file). Messages to be written are
 * placed on a queue in a MessageWriter, which controls all access to a single file.
 * The MessageWriter task is submitted to the completionService.
 * So no IO is done in this thread.
 * <li> The completionService uses threads from the ExecutorService to call the MessageWriter to append messages
 * to the file.
 * <li> indexProcessor waits for Futures from the MessageWriter. These contain the info to write indices for
 * some of the bufr message types.
 * </ol>
 * <p/>
 * TDB: close MessageWriter periodically. sync indexer. delete old messages, and their indices.
 *
 * @author caron
 * @since Aug 8, 2008
 */
public class MessageBroker {
  private static final ucar.unidata.io.KMPMatch matcher = new ucar.unidata.io.KMPMatch("BUFR".getBytes());
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageBroker.class);

  private ExecutorService executor;
  private BlockingQueue<Future<IndexerTask>> completionQ; // = new ArrayBlockingQueue<Future<IndexerTask>>(1000); // unbounded, threadsafe
  private CompletionService<IndexerTask> completionService;

  private Thread messProcessor;
  private Thread indexProcessor;
  private ArrayBlockingQueue<MessageTask> messQ = new ArrayBlockingQueue<MessageTask>(1000); // unbounded, threadsafe

  private MessageDispatchDDS dispatch;

  int bad_msgs = 0;
  int total_msgs = 0;

  public MessageBroker(ExecutorService executor, BlockingQueue<Future<IndexerTask>> blockingQueue,
          CompletionService<IndexerTask> completionService, MessageDispatchDDS dispatcher) throws IOException {

    this.executor = executor;
    this.completionQ = blockingQueue;
    this.completionService = completionService; // completionService manages Callable objects that write bufr message to files
    this.dispatch = dispatcher;

    //a thread for processing messages as they come off the wire
    messProcessor = new Thread(new MessageProcessor());

    // a thread for indexing messages after they have been written
    indexProcessor = new Thread(new IndexProcessor());

    // start up the threads
    messProcessor.start();
    indexProcessor.start();
  }

  public void exit() throws IOException {
    try {
      Thread.currentThread().sleep(1000); // wait a sec
      } catch (InterruptedException e) {
      }

    System.out.println("On exit 1: messageQ size = "+messQ.size());
    while (messQ.peek() != null) {
      try {
        Thread.currentThread().sleep(1000); // wait a sec
      } catch (InterruptedException e) {
        System.out.println("Thread interrupted messQ size = "+messQ.size());
        break; // ok get out of here
      }
    }
    System.out.println("On exit 2: messageQ size = "+messQ.size());
    // shut down the message processor
    messProcessor.interrupt();

    // wait for all io to be complete
    System.out.println("On exit 3: completionQ size = "+completionQ.size());
    while (completionQ.peek() != null) {
      try {
        Thread.currentThread().sleep(1000); // wait a sec
      } catch (InterruptedException e) {
        System.out.println("Thread interrupted messQ size = "+completionQ.size());
        break; // ok get out of here
      }
    }
    System.out.println("On exit 4: completionQ size = "+completionQ.size());

    shutdownAndAwaitTermination( executor);

    // shut down the index processor
    indexProcessor.interrupt();

    // shut down the indexers
    dispatch.exit();
  }

  // from javadoc
  private void shutdownAndAwaitTermination(ExecutorService pool) {
    pool.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(60, TimeUnit.SECONDS))
            System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      pool.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
    System.out.println("Pool terminated");
  }


  //////////////////////////////////////////////////////
  // Step 4 - optionally write an index

  private class IndexProcessor implements Runnable {
    private boolean cancel = false;

    public void run() {
      while (true) {
        try {
          Future<IndexerTask> f = completionService.poll(); // see if ones ready
          if (f == null) {
            if (cancel) break; // check if interrupted
            f = completionService.take(); // block until ready
          }

          IndexerTask itask = f.get();
          itask.process(); // write index

        } catch (InterruptedException e) {
          cancel = true;

        } catch (Exception e) {
          logger.error("MessageBroker.IndexProcessor ", e);
        }
      }
      System.out.println("exit IndexProcessor");
    }
  }

  //////////////////////////////////////////////////////////////////
  // Step 2 - dispatch message

  private class MessageProcessor implements Runnable {
    private boolean cancel = false;

    public void run() {
      while (true) {
        try {
          MessageTask mtask = messQ.poll(); // see if ones ready
          if (mtask == null) {
            if (cancel) break; // check if interrupted
            mtask = messQ.take(); // block until ready
          }
          process(mtask);

        } catch (InterruptedException e) {
          cancel = true;
        }
      }

      System.out.println("exit MessageProcessor");
    }

    void process(MessageTask mtask) {
      //out.format("    %d start process %n", mtask.id);
      try {
        Message m = getMessage(new InMemoryRandomAccessFile("BUFR", mtask.mess));
        if (null == m) return;
        m.setHeader(mtask.header);
        m.setRawBytes(mtask.mess);

        // decide what to do with the message
        // Step 3 (write to file) is done in the dispatcher
        dispatch.dispatch(m);

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

      GregorianCalendar cal = new GregorianCalendar(); // ??      
      return new Message(raf, is, ids, dds, dataSection, cal);
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

  //////////////////////////////////////////////////////
  // Step 1 - read and extract a Bufr Message

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
    int start;
    for (start = 0; start < header.length; start++) {
      byte b = header[start];
      if ((b == 73) || (b == 74)) // I or J
        break;
    }

    byte[] bb = new byte[sizeHeader];
    int count = 0;
    for (int i = start; i < header.length; i++) {
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
