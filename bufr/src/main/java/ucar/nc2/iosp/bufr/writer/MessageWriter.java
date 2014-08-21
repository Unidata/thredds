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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.bufr.Message;

/**
 * Encapsolates writing BUFR message to one particular file.
 *
 * @author caron
 * @since 6/12/13
 */
public class MessageWriter { // implements Callable<IndexerTask> {
  //private static String rootDir = "./";
  //static void setRootDir( String _rootDir) {
  //  rootDir = _rootDir;
  //}

  private final WritableByteChannel wbc;
  private final FileOutputStream fos;

  private final AtomicBoolean isScheduled = new AtomicBoolean(false);
  private long lastModified;

  /**
   *
   * @param file    Write to this file
   * @param fileno  not used
   * @param bufrTableMessages list of BUFR messages containing tables; written first
   * @throws IOException
   */
  MessageWriter(File file, short fileno, List<Message> bufrTableMessages) throws IOException {
    fos = new FileOutputStream(file, true); // append
    wbc = fos.getChannel();

    for (Message m : bufrTableMessages)
      write(m);
  }


  public void write(Message m) throws IOException {
    wbc.write(ByteBuffer.wrap(m.getHeader().getBytes(CDM.utf8Charset)));
    wbc.write(ByteBuffer.wrap(m.getRawBytes()));
    lastModified = System.currentTimeMillis();
    isScheduled.getAndSet(false);
  }

  // last time the file was written to
  public long getLastModified() { return lastModified; }

  void close() throws IOException {
      wbc.close();
      fos.close();
  }

}

