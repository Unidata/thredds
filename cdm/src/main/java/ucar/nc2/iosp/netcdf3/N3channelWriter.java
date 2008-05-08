// $Id: $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.netcdf3;

import ucar.ma2.*;
import ucar.nc2.*;

import java.io.*;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.ByteBuffer;

/**
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
        int nbytes = (int) ncfile.readToByteChannel(v, v.getShapeAsSection(), channel);
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
          bytesDone += ncfile.readToByteChannel(recordVar, section, channel);
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
    writer.writeHeader(dout);
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
    writer.writeHeader(stream);
    stream.flush();
    writer.writeDataAll(wbc);
  }


}