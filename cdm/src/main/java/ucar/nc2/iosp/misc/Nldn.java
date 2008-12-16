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

package ucar.nc2.iosp.misc;

import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.*;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * National Lightning Detection Network (NLDN)
 * @see "http://www.unidata.ucar.edu/data/lightning.html"
 * @author caron
 * @since Nov 19, 2008
 */
public class Nldn extends AbstractIOServiceProvider {
  /*
     Field               Example
     -------------------+---------------------
     date/time (msec)    09/22/93 10:22:33.334
     latitude                            47.33
     longitude                         -87.116
     polarity/signal strength           -188.7
     multiplicity                            6
     ellipse angle                         174
     semi-major axis                       6.0
     eccentricity                          2.0
     chi-square                            1.0

The specifics for the binary NLDN data record contained in the IDD is:

     Size     Name      Description
     --------+---------+----------------------------------------------------
     char[4]  NLDN      'NLDN' marks the start of record
     int[4]   tsec      time in seconds since 1970
     int[4]   nsec      nanoseconds since tsec (seems to be thousandths)
     int[4]   lat       latitude [deg] * 1000
     int[4]   lon       longitude [deg] * 1000
     short[2] fill      padding
     short[2] sgnl      signal strength * 10 [150 NLDN measures ~= 30 kAmps]
     short[2] fill      padding
     short[2] mult      multiplicity [#strokes per flash]
     char[1]  fill      padding
     char[1]  semimaj   semi-major axis
     char[1]  eccent    eccentricity
     char[1]  angle     ellipse angle
     char[1]  chisqr    chi-square

   */

  private static final String MAGIC = "NLDN";
  private RandomAccessFile raf;
  private Structure seq;
  private StructureMembers sm;

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    int n = MAGIC.length();
    byte[] b = new byte[n];
    raf.read(b);
    String got = new String(b);
    return got.equals(MAGIC);
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;

    //int n = (int) raf.length() / recSize;
    //Dimension recordDim = new Dimension("record", n);
    //ncfile.addDimension(null, recordDim);

    seq = new Sequence(ncfile, null, null, "record");
    //seq.setDimensions("record");
    ncfile.addVariable(null, seq);

    Variable v = new Variable(ncfile, null, seq, "tsec");
    v.setDataType(DataType.INT);
    v.setDimensions("");
    v.addAttribute(new Attribute("long_name", "date of strike"));
    v.addAttribute(new Attribute("units", "seconds since 1970-01-01 00:00:00"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    seq.addMemberVariable(v);

    v = new Variable(ncfile, null, seq, "nsec");
    v.setDataType(DataType.INT);
    v.setDimensions("");
    v.addAttribute(new Attribute("long_name", "nanoseconds since tsec"));
    v.addAttribute(new Attribute("units", "1.0e-9 s"));
    seq.addMemberVariable(v);

    v = new Variable(ncfile, null, seq, "lat");
    v.setDataType(DataType.INT);
    v.setDimensions("");
    v.addAttribute(new Attribute("long_name", "latitude"));
    v.addAttribute(new Attribute("units", "degrees_north"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    v.addAttribute(new Attribute("scale_factor", new Float(1.0e-3)));
    seq.addMemberVariable(v);

    v = new Variable(ncfile, null, seq, "lon");
    v.setDataType(DataType.INT);
    v.setDimensions("");
    v.addAttribute(new Attribute("long_name", "longitude"));
    v.addAttribute(new Attribute("units", "degrees_east"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    v.addAttribute(new Attribute("scale_factor", new Float(1.0e-3)));
    seq.addMemberVariable(v);

    v = new Variable(ncfile, null, seq, "signal");
    v.setDataType(DataType.SHORT);
    v.setDimensions("");
    v.addAttribute(new Attribute("long_name", "signal strength * 10 [150 NLDN measures ~= 30 kAmps]"));
    v.addAttribute(new Attribute("units", ""));
    seq.addMemberVariable(v);

    v = new Variable(ncfile, null, seq, "multiplicity");
    v.setDataType(DataType.BYTE);
    v.setDimensions("");
    v.addAttribute(new Attribute("long_name", "multiplicity [#strokes per flash]"));
    v.addAttribute(new Attribute("units", ""));
    seq.addMemberVariable(v);

    v = new Variable(ncfile, null, seq, "fill");
    v.setDataType(DataType.BYTE);
    v.setDimensions("");
    seq.addMemberVariable(v);

    v = new Variable(ncfile, null, seq, "semimaj");
    v.setDataType(DataType.BYTE);
    v.setDimensions("");
    v.addAttribute(new Attribute("long_name", "semi-major axis"));
    v.addAttribute(new Attribute("units", ""));
    seq.addMemberVariable(v);

    v = new Variable(ncfile, null, seq, "eccent");
    v.setDataType(DataType.BYTE);
    v.setDimensions("");
    v.addAttribute(new Attribute("long_name", "eccentricity"));
    v.addAttribute(new Attribute("units", ""));
    seq.addMemberVariable(v);

    v = new Variable(ncfile, null, seq, "angle");
    v.setDataType(DataType.BYTE);
    v.setDimensions("");
    v.addAttribute(new Attribute("long_name", "ellipse angle"));
    v.addAttribute(new Attribute("units", ""));
    seq.addMemberVariable(v);

    v = new Variable(ncfile, null, seq, "chisqr");
    v.setDataType(DataType.BYTE);
    v.setDimensions("");
    v.addAttribute(new Attribute("long_name", "chi-square"));
    v.addAttribute(new Attribute("units", ""));
    seq.addMemberVariable(v);

    ncfile.addAttribute(null, new Attribute("title", "NLDN Lightning Data"));
    ncfile.addAttribute(null, new Attribute("history", "Read directly by Netcdf Java IOSP"));

    ncfile.addAttribute(null, new Attribute("Conventions", "CF-1"));
    ncfile.addAttribute(null, new Attribute("CFfeatureType", "point"));

    ncfile.finish();

    sm = seq.makeStructureMembers();
    sm.findMember("tsec").setDataParam(0);
    sm.findMember("nsec").setDataParam(4);
    sm.findMember("lat").setDataParam(8);
    sm.findMember("lon").setDataParam(12);
    sm.findMember("signal").setDataParam(18);
    sm.findMember("multiplicity").setDataParam(22);
    sm.findMember("fill").setDataParam(23);
    sm.findMember("semimaj").setDataParam(24);
    sm.findMember("eccent").setDataParam(25);
    sm.findMember("angle").setDataParam(26);
    sm.findMember("chisqr").setDataParam(27);
    sm.setStructureSize(recSize);
  }
  /* The specifics for the binary NLDN data record contained in the IDD is:

     Size     Name      Description
     --------+---------+----------------------------------------------------
     char[4]  NLDN      'NLDN' marks the start of record
     int[4]   tsec      time in seconds since 1970
     int[4]   nsec      nanoseconds since tsec (seems to be thousandths)
     int[4]   lat       latitude [deg] * 1000
     int[4]   lon       longitude [deg] * 1000
     short[2] fill      padding
     short[2] sgnl      signal strength * 10 [150 NLDN measures ~= 30 kAmps]
     short[2] fill      padding
     short[2] mult      multiplicity [#strokes per flash]
     char[1]  fill      padding
     char[1]  semimaj   semi-major axis
     char[1]  eccent    eccentricity
     char[1]  angle     ellipse angle
     char[1]  chisqr    chi-square
   */

  /* public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Range r = section.getRange(0);
    int nrecs = r.length();
    byte[] bb = new byte[nrecs * recSize];

    int pos = 0;
    Range.Iterator iter = r.getIterator();
    while (iter.hasNext()) {
      int index = iter.next();
      raf.seek(recHeader + index * recSize);
      raf.read(bb, pos, recSize);
      pos += recSize;
    }

    return new ArrayStructureBB(sm, new int[]{nrecs}, ByteBuffer.wrap(bb), 0);
  }  */

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    return new ArraySequence(sm, new SeqIter(), 0);
  }

  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    return new SeqIter();
  }

  private static final int recHeader = 84;
  private static final int recSize = 28;

  private class SeqIter implements StructureDataIterator {
    private int done = 0;
    private int alreadyRead = 0;
    private int nextIndex = 0;
    private ArrayStructureBB asbb = null;

    private long totalBytes;
    private long bytesRead;

    SeqIter() throws IOException {
      totalBytes = (int) raf.length();
      raf.seek(0);
    }

    public StructureDataIterator reset() {
      done = 0;
      alreadyRead = 0;
      bytesRead = 0;
      nextIndex = 0;
      
      try {
        raf.seek(0);
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      return this;
    }

    public boolean hasNext() throws IOException {
      if (done < alreadyRead) return true;
      return readHeader();
    }

    public StructureData next() throws IOException {
      done++;
      return asbb.getStructureData( nextIndex++);
    }

    private boolean readHeader() throws IOException {
      if ((bytesRead + recHeader) > totalBytes) return false;

      byte[] b = new byte[recHeader];
      raf.readFully(b);
      bytesRead += recHeader;

      ByteBuffer bb = ByteBuffer.wrap(b);
      int count = bb.getInt(8);
      if (count == 0) return readHeader();

      if ((bytesRead + count * recSize) > totalBytes) return false;
      byte[] data = new byte[count * recSize];
      raf.read(data);
      bytesRead += count * recSize;
      alreadyRead += count;
      nextIndex = 0;

      ByteBuffer bbdata = ByteBuffer.wrap(data);
      asbb = new ArrayStructureBB(sm, new int[]{count}, bbdata, 0);
      return true;
    }

    public void setBufferSize(int bytes) {
    }
  }



  // this is thye start of a buffererd iterator
  /* private class MySDIter implements StructureDataIterator {
    private int done = 0;
    private int readStart = 0;
    private int recsAlreadyRead = 0;
    private int readAtaTime;
    private ArrayStructureBB asbb = null;

    private int recsLeft;
    private int recsDone;

    MySDIter(int bufferSize) throws IOException {
      setBufferSize( bufferSize);
      recsLeft = (int) raf.length() / recSize;
      recsDone = 0;
    }

    public boolean hasNext() {
      if (done < recsAlreadyRead) return true;
      return (recsLeft > 0);
    }

    public StructureDataIterator reset() {
      done = 0;
      readStart = 0;
      readRead = 0;
      return this;
    }

    public StructureData next() throws IOException {
      if (done >= readStart) {
        readNextBuffer();
      }
      done++;
      return asbb.getStructureData( readRead++);
    }

    private void readNextBuffer() throws IOException {
      bytesLeft = (int)(raf.length() - raf.getFilePointer());
      int recsLeft = bytesLeft / recSize;
      int recsToRead = Math.min(recsLeft, readAtaTime);

      byte[] bb = new byte[recsToRead * recSize];
      ByteBuffer.wrap(bb);
      asbb = new ArrayStructureBB(sm, new int[]{nrecs}, bb, 0);

      recsAlreadyRead += recsToRead;
      readRead = 0;
    }

    public void setBufferSize(int bytes) {
      if (done > 0) return; // too late
      if (bytes <= 0)
        bytes = defaultBufferSize;
      readAtaTime = Math.max( 10, bytes / recSize);
    }
  } */

  public void close() throws IOException {
    raf.close();
  }
}
