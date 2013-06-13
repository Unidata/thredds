package ucar.nc2.iosp.bufr.writer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private final String name;
  private final short fileno;

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
    this.fileno = fileno;
    fos = new FileOutputStream(file, true); // append
    wbc = fos.getChannel();
    name = file.getPath();

    for (Message m : bufrTableMessages)
      write(m);
    System.out.printf("Start %s ntables=%d%n", file, bufrTableMessages.size());
  }


  public void write(Message m) throws IOException {
    wbc.write(ByteBuffer.wrap(m.getHeader().getBytes()));
    wbc.write(ByteBuffer.wrap(m.getRawBytes()));
    lastModified = System.currentTimeMillis();
    isScheduled.getAndSet(false);
  }

  // last time the file was written to
  public long getLastModified() { return lastModified; }

  void close() throws IOException {
    if (wbc != null)
      wbc.close();
    if (fos != null)
      fos.close();
    System.out.printf("%s closed%n", name);
  }

}

