// $Id: $
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
package ucar.nc2.iosp.netcdf3;

import ucar.ma2.*;
import ucar.nc2.*;

import java.io.*;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.ByteBuffer;

/**
 *  Experimental
 * @author john
 */
public class N3channelWriter extends N3streamWriter {
  static private int buffer_size = 1000 * 1000;
  static private boolean debugWrite = false;

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  //private ucar.nc2.NetcdfFile ncfile;
  //private List<Vinfo> vinfoList = new ArrayList<Vinfo>(); // output order of the variables
  //private boolean debugPos = false, debugWriteData = false;
  //private int dataStart, dataOffset = 0;

  public N3channelWriter(ucar.nc2.NetcdfFile ncfile) {
    super(ncfile);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  public void writeDataAll(WritableByteChannel channel) throws IOException, InvalidRangeException {
    for (Vinfo vinfo : vinfoList) {
      if (!vinfo.isRecord) {
        Variable v = vinfo.v;
        assert filePos == vinfo.offset;
        if (debugPos) System.out.println(" writing at "+filePos+" should be "+vinfo.offset+" "+v.getName());
        int nbytes = (int) v.readToByteChannel(v.getShapeAsSection(), channel);
        filePos += nbytes;
        filePos += pad(channel, nbytes);
        if (debugPos) System.out.println(" vinfo="+vinfo);
      }
    }

    // must use record dimension if it exists+
    boolean useRecordDimension = ncfile.hasUnlimitedDimension();
    if (useRecordDimension) {
      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

      Structure recordVar = (Structure) ncfile.findVariable("record");
      Section section = new Section().appendRange(null);

      long bytesDone = 0;
      long done = 0;
      long nrecs = (int) recordVar.getSize();
      int structureSize = recordVar.getElementSize();
      int readAtaTime = Math.max( 10, buffer_size / structureSize);

      for (int count = 0; count < nrecs; count+=readAtaTime) {
        long last = Math.min(nrecs, done+readAtaTime); // dont go over nrecs
        int need = (int) (last - done); // how many to read this time

        section.setRange(0, new Range(count, count+need-1));
        try {
          bytesDone += recordVar.readToByteChannel( section, channel);
          done += need;
        } catch (InvalidRangeException e) {
          e.printStackTrace();
          break;
        }
      }
      assert done == nrecs;
      bytesDone /= 1000 * 1000;
      if (debugWrite) System.out.println("write record var; total = " + bytesDone + " Mbytes # recs=" + done);

      // remove the record structure this is rather fishy, perhaps better to leave it
      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE);
      ncfile.finish();
    }

  }

  // pad to a 4 byte boundary
  private ByteBuffer padddingBB;
  private int pad(WritableByteChannel channel, int nbytes) throws IOException {
    int pad = N3header.padding(nbytes);
    if ((null != channel) && (pad > 0)) {
      if (padddingBB == null) padddingBB = ByteBuffer.allocate(4); // just 4 zero bytes
      padddingBB.position(0);
      padddingBB.limit(pad);
      channel.write( padddingBB);
    }
    return pad;
  }

  ////////////////////////////////////////

  public static void writeFromFile(NetcdfFile fileIn, String fileOutName) throws IOException, InvalidRangeException {

    FileOutputStream stream = new FileOutputStream(fileOutName);
    WritableByteChannel channel = stream.getChannel();
    DataOutputStream dout = new DataOutputStream(Channels.newOutputStream(channel));

    N3channelWriter writer = new N3channelWriter(fileIn);
    int numrec = fileIn.getUnlimitedDimension() == null ? 0 : fileIn.getUnlimitedDimension().getLength();

    writer.writeHeader(dout, numrec);
    dout.flush();

    writer.writeDataAll(channel);
    channel.close();
  }

  /**
   * Write ncfile to a WritableByteChannel.
   *
   * @param ncfile the file to write
   * @param wbc write to this WritableByteChannel.
   *   If its a Socket, must have been opened through a call to java.nio.channels.SocketChannel.open()
   * @throws IOException on IO error
   * @throws InvalidRangeException range error
   */
  public static void writeToChannel(NetcdfFile ncfile, WritableByteChannel wbc) throws IOException, InvalidRangeException {
    DataOutputStream stream = new DataOutputStream(new BufferedOutputStream( Channels.newOutputStream(wbc), 8000));
    //DataOutputStream stream = new DataOutputStream(Channels.newOutputStream(wbc));  // buffering seems to improve by 5%
    N3channelWriter writer = new N3channelWriter(ncfile);
    int numrec = ncfile.getUnlimitedDimension() == null ? 0 : ncfile.getUnlimitedDimension().getLength();    
    writer.writeHeader(stream, numrec);
    stream.flush();
    writer.writeDataAll(wbc);
  }


}