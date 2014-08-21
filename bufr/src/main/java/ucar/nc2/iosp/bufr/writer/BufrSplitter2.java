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

import ucar.nc2.iosp.bufr.*;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Read BUFR files and split them.
 * Currently only files.
 *
 * @author caron
 * @since 8/26/13
 */
public class BufrSplitter2 {
    File dirOut;
    MessageDispatchDDS dispatcher;
    Formatter out;
    int total_msgs = 0;

    public BufrSplitter2(String dirName, Formatter out) throws IOException {
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
      try (RandomAccessFile mraf = new RandomAccessFile(filename, "r")) {
        MessageScanner scanner = new MessageScanner(mraf);

        while (scanner.hasNext()) {
          Message m = scanner.next();
          if (m == null) continue;
          total_msgs++;
          if (m.getNumberDatasets() == 0) continue;

          // LOOK check on tables complete etc ??

          m.setRawBytes(scanner.getMessageBytes(m));

          // decide what to do with the message
          dispatcher.dispatch(m);
        }

        dispatcher.resetBufrTableMessages();
      }
    }

    public void exit() {
      dispatcher.exit(out);
    }

}
