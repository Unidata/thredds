/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: $

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
        // assert filePos == vinfo.offset;
        //if (debugPos) System.out.println(" writing at "+filePos+" should be "+vinfo.offset+" "+v.getFullName());
        int nbytes = (int) v.readToByteChannel(v.getShapeAsSection(), channel);
        filePos += nbytes;
        filePos += pad(channel, nbytes);
        if (debugPos) System.out.printf(" read=%d vinfo=%s%n", nbytes, vinfo);
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

    try (FileOutputStream stream = new FileOutputStream(fileOutName)) {
      WritableByteChannel channel = stream.getChannel();
      DataOutputStream dout = new DataOutputStream(Channels.newOutputStream(channel));

      N3channelWriter writer = new N3channelWriter(fileIn);
      int numrec = fileIn.getUnlimitedDimension() == null ? 0 : fileIn.getUnlimitedDimension().getLength();

      writer.writeHeader(dout, numrec);
      dout.flush();

      writer.writeDataAll(channel);
    }
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