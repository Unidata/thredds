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

import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Calendar;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encapsolates writing to a particular file.
 *
 * @author caron
 * @since Aug 19, 2008
 */
public class MessageWriter implements Callable<Object> {
  private static final String rootDir = "D:/bufr/dispatch/";

  private final ConcurrentLinkedQueue q; // unbounded, threadsafe
  private final WritableByteChannel wbc;
  private final FileOutputStream fos;

  private final ExecutorService executor;
  private final AtomicBoolean isScheduled = new AtomicBoolean(false);
  private long lastModified;

  MessageWriter(ExecutorService executor, String fileout, Calendar mcal) throws FileNotFoundException {
    this.executor = executor;
    File dir = new File(rootDir + fileout);
    dir.mkdirs();

    String date = mcal.get( Calendar.YEAR)+"-"+(1+mcal.get( Calendar.MONTH))+"-"+mcal.get( Calendar.DAY_OF_MONTH);

    File file = new File(rootDir + fileout + "/"+date+  ".bufr");
    fos = new FileOutputStream(file);
    wbc = fos.getChannel();
    q = new ConcurrentLinkedQueue();
  }

  // put a message on the queue, schedule writing if not already scheduled.
  void scheduleWrite(Message m) throws IOException {
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

  public Object call() throws IOException {
    while (!q.isEmpty()) {
      Message m = (Message) q.remove();
      wbc.write(ByteBuffer.wrap(m.getHeader().getBytes()));
      wbc.write(ByteBuffer.wrap(m.getRawBytes()));
    }
    lastModified = System.currentTimeMillis();
    isScheduled.getAndSet(false);
    return this;
  }
}
