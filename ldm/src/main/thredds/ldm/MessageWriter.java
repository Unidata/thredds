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

import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encapsolates writing BUFR message to one particular file.
 *
 * @author caron
 * @since Aug 19, 2008
 */
public class MessageWriter implements Callable<IndexerTask> {
  //private static String rootDir = "./";
  //static void setRootDir( String _rootDir) {
  //  rootDir = _rootDir;
  //}

  private final ConcurrentLinkedQueue<Message> q; // unbounded, threadsafe
  private final WritableByteChannel wbc;
  private final FileOutputStream fos;

  private final CompletionService<IndexerTask> executor;
  private final Indexer indexer;
  private final short fileno;

  private final AtomicBoolean isScheduled = new AtomicBoolean(false);
  private long lastModified;
  private long filePos = 0;

  //MessageWriter(CompletionService<IndexerTask> executor, Indexer indexer, short fileno, String fileout, Calendar mcal) throws FileNotFoundException {
  MessageWriter(CompletionService<IndexerTask> executor, Indexer indexer, File file, short fileno) throws FileNotFoundException {
    this.executor = executor;
    this.indexer = indexer;
    this.fileno = fileno;
    /* File dir = new File(rootDir + fileout);
    dir.mkdirs();

    String date = mcal.get( Calendar.YEAR)+"-"+(1+mcal.get( Calendar.MONTH))+"-"+mcal.get( Calendar.DAY_OF_MONTH);

    File file = new File(rootDir + fileout + "/"+fileout+"-"+date+  ".bufr");  */
    if (file.exists())
      filePos = file.length();

    fos = new FileOutputStream(file, true); // append
    wbc = fos.getChannel();
    q = new ConcurrentLinkedQueue<Message>();
  }

  // last time the file was written to
  public long getLastModified() { return lastModified; }

  // put a message on the queue, schedule writing if not already scheduled.
  void scheduleWrite(Message m) {
    q.add(m);
    if (!isScheduled.getAndSet(true)) {
      executor.submit( this);
    }
  }

  void close() throws IOException {
    if (wbc != null)
      wbc.close();
    if (fos != null)
      fos.close();
  }

  public IndexerTask call() throws IOException {
    IndexerTask task = new IndexerTask();
    task.indexer = this.indexer;
    if (this.indexer != null) {
      task.mlist = new ArrayList<Message>(10);
      task.fileno = this.fileno;
    }

    while (!q.isEmpty()) {
      Message m = q.remove();
      wbc.write(ByteBuffer.wrap(m.getHeader().getBytes()));
      wbc.write(ByteBuffer.wrap(m.getRawBytes()));

      if (this.indexer != null) { // track info we need to write the index
        m.setStartPos( filePos);
        filePos += m.getHeader().length() + m.getRawBytes().length;
        task.mlist.add(m);
      }
    }
    lastModified = System.currentTimeMillis();
    isScheduled.getAndSet(false);
    return task; // the result becomes available in the Future of the CompletionService
  }

}
