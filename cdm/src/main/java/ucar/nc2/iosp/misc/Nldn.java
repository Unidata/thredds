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


package ucar.nc2.iosp.misc;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import java.nio.ByteBuffer;


/**
 * National Lightning Detection Network (NLDN)
 * @see "http://www.unidata.ucar.edu/data/lightning.html"
 * @author caron
 * @since Nov 19, 2008
 */
public class Nldn extends AbstractLightningIOSP {

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

    /** The magic mushroom */
    private static final String MAGIC = "NLDN";

    /** The data structure */
    private Structure seq;

    /** The structure members */
    private StructureMembers sm;

    /** The time in seconds variable name */
    private static final String TSEC = "tsec";

    /** The time in nanoseconds from TSEC variable name */
    private static final String NSEC = "nsec";

    /** The chi squared variable name */
    private static final String CHISQR = "chisqr";

    /** The fill variable name */
    private static final String FILL = "fill";

    /** header size */
    private static final int recHeader = 84;

    /** record size */
    private static final int recSize = 28;

    /**
     * Check if this is a valid file for this IOServiceProvider.
     * You must make this method thread safe, ie dont keep any state.
     *
     * @param raf RandomAccessFile
     * @return true if valid.
     * @throws IOException if read error
     */
    public boolean isValidFile(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        int    n = MAGIC.length();
        byte[] b = new byte[n];
        raf.read(b);
        String got = new String(b);
        return got.equals(MAGIC);
    }

    /**
     * Get a unique id for this file type.
     * @return registered id of the file type
     * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
     */
    public String getFileTypeId() {
        return "NLDN";
    }

    /**
     * Get a human-readable description for this file type.
     * @return description of the file type
     * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
     */
    public String getFileTypeDescription() {
        return "National Lightning Detection Network";
    }

    /**
     * Open existing file, and populate ncfile with it. This method is only
     * called by the NetcdfFile constructor on itself. The provided NetcdfFile
     * object will be empty except for the location String and the
     * IOServiceProvider associated with this NetcdfFile object.
     *
     * @param raf the file to work on, it has already passed the
     *            isValidFile() test.
     * @param ncfile add objects to this empty NetcdfFile
     * @param cancelTask used to monitor user cancellation; may be null.
     * @throws IOException if read error
     */
    public void open(RandomAccessFile raf, NetcdfFile ncfile,
                     CancelTask cancelTask)
            throws IOException {
        this.raf = raf;

        seq      = new Sequence(ncfile, null, null, RECORD);
        ncfile.addVariable(null, seq);

        /*
        makeLightningVariable(NetcdfFile ncfile, Group group,
                              Structure seq, String name,
                              DataType dataType, String dims,
                              String longName, String cfName,
                              String units, AxisType type) {
        */
        Variable v =
            makeLightningVariable(ncfile, null, seq, TSEC, DataType.INT,
                                  "", "time of stroke", null,
                                  secondsSince1970,
                                  AxisType.Time);
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, "nsec", DataType.INT,
                                  "", "nanoseconds since tsec", null,
                                  "1.0e-9 s", null);
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, LAT, DataType.INT, "",
                                  "latitude", "latitude", "degrees_north",
                                  AxisType.Lat);
        v.addAttribute(new Attribute("scale_factor", new Float(1.0e-3)));
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, LON, DataType.INT, "",
                                  "longitude", "longitude", "degrees_east",
                                  AxisType.Lon);
        v.addAttribute(new Attribute("scale_factor", new Float(1.0e-3)));
        seq.addMemberVariable(v);

        v = makeLightningVariable(
            ncfile, null, seq, SIGNAL, DataType.SHORT, "",
            "signal strength/polarity [150 NLDN measures ~= 30 kAmps]", null, "",
            null);
        v.addAttribute(new Attribute("scale_factor", new Float(1.0e-1)));
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, MULTIPLICITY,
                                  DataType.BYTE, "",
                                  "multiplicity [#strokes per flash]", null,
                                  "", null);
        seq.addMemberVariable(v);

        v = new Variable(ncfile, null, seq, FILL);
        v.setDataType(DataType.BYTE);
        v.setDimensions("");
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, MAJOR_AXIS,
                                  DataType.BYTE, "",
                                  "error ellipse semi-major axis", null, "",
                                  null);
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, ECCENTRICITY,
                                  DataType.BYTE, "",
                                  "error ellipse eccentricity ", null, "",
                                  null);
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, ELLIPSE_ANGLE,
                                  DataType.BYTE, "",
                                  "error ellipse axis angle of orientation ",
                                  null, "degrees", null);
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, CHISQR, DataType.BYTE,
                                  "", "chi-squared", null, "", null);
        seq.addMemberVariable(v);

        addLightningGlobalAttributes(ncfile);

        ncfile.finish();

        sm = seq.makeStructureMembers();
        sm.findMember(TSEC).setDataParam(0);
        sm.findMember(NSEC).setDataParam(4);
        sm.findMember(LAT).setDataParam(8);
        sm.findMember(LON).setDataParam(12);
        sm.findMember(SIGNAL).setDataParam(18);
        sm.findMember(MULTIPLICITY).setDataParam(22);
        sm.findMember(FILL).setDataParam(23);
        sm.findMember(MAJOR_AXIS).setDataParam(24);
        sm.findMember(ECCENTRICITY).setDataParam(25);
        sm.findMember(ELLIPSE_ANGLE).setDataParam(26);
        sm.findMember(CHISQR).setDataParam(27);
        sm.setStructureSize(recSize);
    }

    /**
     * Add the global attributes.
     *
     * @param ncfile  the file to add to
     */
    protected void addLightningGlobalAttributes(NetcdfFile ncfile) {
        super.addLightningGlobalAttributes(ncfile);
        ncfile.addAttribute(null,
                            new Attribute("title", "NLDN Lightning Data"));

        ncfile.addAttribute(null, new Attribute("Conventions", "NLDN-CDM"));
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

    /**
     * Read data from a top level Variable and return a memory resident Array.
     * This Array has the same element type as the Variable, and the requested shape.
     *
     * @param v2 a top-level Variable
     * @param section the section of data to read.
     *   There must be a Range for each Dimension in the variable, in order.
     *   Note: no nulls allowed. IOSP may not modify.
     * @return the requested data in a memory-resident Array
     * @throws IOException if read error
     * @throws InvalidRangeException if invalid section
     * @see ucar.ma2.Range
     */
    public Array readData(Variable v2, Section section)
            throws IOException, InvalidRangeException {
        return new ArraySequence(sm, new SeqIter(), nelems);
    }
    private int nelems = -1;

    /**
     * Get the structure iterator
     *
     * @param s  the Structure
     * @param bufferSize  the buffersize
     *
     * @return the data iterator
     *
     * @throws java.io.IOException if problem reading data
     */
    public StructureDataIterator getStructureIterator(Structure s,
            int bufferSize)
            throws java.io.IOException {
        return new SeqIter();
    }


    /**
     * Sequence Iterator
     *
     * @author Unidata Development Team
     */
    private class SeqIter implements StructureDataIterator {

        /** done? */
        private int done = 0;

        /** number bytes already read */
        private int alreadyRead = 0;

        /** next index*/
        private int nextIndex = 0;

        /** the wrapped asbb */
        private ArrayStructureBB asbb = null;

        /** total bytes */
        private long totalBytes;

        /** bytes read */
        private long bytesRead;

        /**
         * Create a new one
         *
         * @throws IOException problem reading the file
         */
        SeqIter() throws IOException {
            totalBytes = (int) raf.length();
            raf.seek(0);
        }

        @Override public StructureDataIterator reset() {
            done        = 0;
            alreadyRead = 0;
            bytesRead   = 0;
            nextIndex   = 0;

            try {
                raf.seek(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        @Override public boolean hasNext() throws IOException {
            if (done < alreadyRead) {
                return true;
            }
            return readHeader();
        }

        @Override public StructureData next() throws IOException {
            done++;
            return asbb.getStructureData(nextIndex++);
        }

        /**
         * Read the header
         *
         * @return  true if okay
         *
         * @throws IOException problem reading file
         */
        private boolean readHeader() throws IOException {
            if ((bytesRead + recHeader) > totalBytes) {
                nelems = done; // record the number of elements for next time
                return false;
            }

            byte[] b = new byte[recHeader];
            raf.readFully(b);
            bytesRead += recHeader;

            ByteBuffer bb    = ByteBuffer.wrap(b);
            int        count = bb.getInt(8);
            if (count == 0) {
                return readHeader();
            }

            if ((bytesRead + count * recSize) > totalBytes) {
                return false;
            }
            byte[] data = new byte[count * recSize];
            raf.read(data);
            bytesRead   += count * recSize;
            alreadyRead += count;
            nextIndex   = 0;

            ByteBuffer bbdata = ByteBuffer.wrap(data);
            asbb = new ArrayStructureBB(sm, new int[] { count }, bbdata, 0);
            return true;
        }

        @Override public void setBufferSize(int bytes) {}

        @Override public int getCurrentRecno() {
            return done - 1;
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


}

