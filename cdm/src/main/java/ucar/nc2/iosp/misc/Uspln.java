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
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.text.ParseException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;


/**
 * IOSP for the USPLN/NAPLN original and extended formats.
 * @see "http://www.unidata.ucar.edu/data/lightning.html"
 * @author caron, dmurray
 */
public class Uspln extends AbstractLightningIOSP {

    /*
      USPLN/NAPLN data format:

      Each 1 minute packet sent has an ASCII header, followed by a record for
      each lightning detection during the past 1 minute.

      Header
      The ASCII header provides information on the creation time of the one
      minute packet and ending date and time of the file.

      Sample Header:

      (original format)
      LIGHTNING-USPLN1,2004-10-11T20:45:02,2004-10-11T20:45:02

      (extended format)
      LIGHTNING-USPLN1EX,2004-10-11T20:45:02,2004-10-11T20:45:02

      Description:
      Name of Product: LIGHTNING-USPLN1
      Creation of 1 min Packet (yyyy-mm-ddThh:mm:ss): 2004-10-11T20:45:02
      Ending of 1 min Packet (yyyy-mm-ddThh:mm:ss): 2004-10-11T20:45:02

      NOTE: All times in UTC

      Stroke Record Following the header, an individual record is provided for
      each lightning stroke in a comma delimited format.

      Sample Stroke Records (original format):
      2004-10-11T20:44:02,32.6785331,-105.4344587,-96.1,1
      2004-10-11T20:44:05,21.2628231,-86.9596634,53.1,1
      2004-10-11T20:44:05,21.2967119,-86.9702106,50.3,1
      2004-10-11T20:44:06,19.9044769,-100.7082608,43.1,1
      2004-10-11T20:44:11,21.4523434,-82.5202274,-62.8,1
      2004-10-11T20:44:11,21.8155306,-82.6708778,80.9,1

      Sample Stroke Records (extended format):
      2004-10-11T20:44:02,32.6785331,-105.4344587,-96.1,0.5,0.25,0
      2004-10-11T20:44:05,21.2628231,-86.9596634,53.1,0.25,0.25,41
      2004-10-11T20:44:05,21.2967119,-86.9702106,50.3,0.25,0.25,78
      2004-10-11T20:44:06,19.9044769,-100.7082608,43.1,0.75,0.25,-51
      2004-10-11T20:44:11,21.4523434,-82.5202274,-62.8,0.25,0.25,-58
      2004-10-11T20:44:11,21.8155306,-82.6708778,80.9,0.25,0.25,-86

      Description:

      Stroke Date/Time (yyyy-mm-ddThh:mm:ss): 2004-10-11T20:44:02

      Stroke Latitude (deg): 32.6785331
      Stroke Longitude (deg): -105.4344587
      Stroke Amplitude (kAmps, see note below): -96.1

      (original format)
      Stroke Count (number of strokes per flash): 1
      Note: At the present time USPLN data are only provided in stroke format,
      so the stroke count will always be 1.

      (extended format)
      Error Ellipse Major Axis (km): 0.5
      Error Ellipse Minor Axis (km): 0.25
      Error Ellipse Major Axis Orientation (degrees): 0

      Notes about Decoding Stroke Amplitude
      The amplitude field is utilized to indicate the amplitude of strokes and
      polarity of strokes for all Cloud-to- Ground Strokes.

      For other types of detections this field is utilized to provide
      information on the type of stroke detected.

      The amplitude number for Cloud-to-Ground strokes provides the amplitude
      of the stroke and the sign (+/-) provides the polarity of the stroke.

      An amplitude of 0 indicates USPLN Cloud Flash Detections rather than
      Cloud-to-Ground detections.

      Cloud flash detections include cloud-to-cloud, cloud-to-air, and
      intra-cloud flashes.

      An amplitude of -999 or 999 indicates a valid cloud-to-ground stroke
      detection in which an amplitude was not able to be determined. Typically
      these are long-range detections.
    */

    /** Magic string for determining if this is my type of file. */
    private static final String MAGIC = "LIGHTNING-..PLN1";

    /** Magic string for determining if this is my type of file. */
    private static final String MAGIC_OLD = "..PLN-LIGHTNING";

    /** Magic string for determining if this is and extended type of file. */
    private static final String MAGIC_EX = ".*PLN1EX.*";

    /** original time format */
    private static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /** extended time format */
    private static final String TIME_FORMAT_EX = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /** is this extended data */
    private boolean isExtended = false;

    /** offsets int the file */
    private long[] offsets;

    /** max/min latitude */
    private double lat_min, lat_max;

    /** max/min longitude */
    private double lon_min, lon_max;

    /** max/min time */
    private double time_min, time_max;

    /** The structure members */
    private StructureMembers sm;

    /** the date format */
    private SimpleDateFormat isoDateFormat = null;

    /**
     * Check if this is a valid file for this IOServiceProvider.
     *
     * @param raf RandomAccessFile
     * @return true if valid.
     * @throws IOException if read error
     */
    public boolean isValidFile(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        int n = MAGIC.length();
        if (raf.length() < n) {
            return false;
        }

        byte[] b = new byte[n];
        raf.read(b);
        String got = new String(b);
        return StringUtil.regexpMatch(got, MAGIC)
               || StringUtil.regexpMatch(got, MAGIC_OLD);
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

        this.raf      = raf;
        isExtended    = checkFormat();
        isoDateFormat = new SimpleDateFormat();
        isoDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        isoDateFormat.applyPattern(isExtended
                                   ? TIME_FORMAT_EX
                                   : TIME_FORMAT);
        Sequence seq = makeSequence(ncfile);
        ncfile.addVariable(null, seq);
        addLightningGlobalAttributes(ncfile);
        ncfile.finish();
        sm = seq.makeStructureMembers();
        int size = ArrayStructureBB.setOffsets(sm);
        sm.setStructureSize(size);

    }

    /**
     * _more_
     *
     * @param ncfile _more_
     *
     * @return _more_
     */
    protected Sequence makeSequence(NetcdfFile ncfile) {

        Sequence seq = new Sequence(ncfile, null, null, RECORD);

        Variable v = makeLightningVariable(ncfile, null, seq, TIME,
                                           DataType.DOUBLE, "",
                                           "time of stroke", null,
                                           secondsSince1970, AxisType.Time);
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, LAT, DataType.DOUBLE,
                                  "", "latitude", "latitude",
                                  "degrees_north", AxisType.Lat);
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, LON, DataType.DOUBLE,
                                  "", "longitude", "longitude",
                                  "degrees_east", AxisType.Lon);
        seq.addMemberVariable(v);

        v = makeLightningVariable(ncfile, null, seq, SIGNAL, DataType.FLOAT,
                                  "",
                                  "signed peak amplitude (signal strength)",
                                  null, "kAmps", null);
        v.addAttribute(new Attribute("missing_value", new Double(999)));
        seq.addMemberVariable(v);

        if (isExtended) {  // extended
            v = makeLightningVariable(ncfile, null, seq, MAJOR_AXIS,
                                      DataType.FLOAT, "",
                                      "error ellipse semi-major axis", null,
                                      "km", null);
            seq.addMemberVariable(v);

            v = makeLightningVariable(ncfile, null, seq, MINOR_AXIS,
                                      DataType.FLOAT, "",
                                      "error ellipse minor axis ", null,
                                      "km", null);
            seq.addMemberVariable(v);

            v = makeLightningVariable(
                ncfile, null, seq, ELLIPSE_ANGLE, DataType.INT, "",
                "error ellipse axis angle of orientation ", null, "degrees",
                null);
            seq.addMemberVariable(v);

        } else {  // original format
            v = makeLightningVariable(ncfile, null, seq, MULTIPLICITY,
                                      DataType.INT, "",
                                      "multiplicity [#strokes per flash]",
                                      null, "", null);
            seq.addMemberVariable(v);
        }
        return seq;
    }

    /**
     * Add the global attributes.
     *
     * @param ncfile  the file to add to
     */
    protected void addLightningGlobalAttributes(NetcdfFile ncfile) {
        super.addLightningGlobalAttributes(ncfile);
        ncfile.addAttribute(null,
                            new Attribute("title", "USPLN Lightning Data"));
        ncfile.addAttribute(null,
                            new Attribute("file_format",
                                          "USPLN1 " + (isExtended
                ? "(extended)"
                : "(original)")));

        /*
        ncfile.addAttribute(null,
                            new Attribute("time_coverage_start",
                                          time_min + " " + secondsSince1970));
        ncfile.addAttribute(null,
                            new Attribute("time_coverage_end",
                                          time_max + " " + secondsSince1970));

        ncfile.addAttribute(null,
                            new Attribute("geospatial_lat_min",
                                          new Double(lat_min)));
        ncfile.addAttribute(null,
                            new Attribute("geospatial_lat_max",
                                          new Double(lat_max)));

        ncfile.addAttribute(null,
                            new Attribute("geospatial_lon_min",
                                          new Double(lon_min)));
        ncfile.addAttribute(null,
                            new Attribute("geospatial_lon_max",
                                          new Double(lon_max)));
        */

    }

    /**
     * Read all the data and return the number of strokes
     * @return true if extended format
     *
     * @throws IOException  if read error
     */
    private boolean checkFormat() throws IOException {

        raf.seek(0);
        boolean extended = false;
        while (true) {
            long   offset = raf.getFilePointer();
            String line   = raf.readLine();
            if (line == null) {
                break;
            }
            if (StringUtil.regexpMatch(line, MAGIC)
                    || StringUtil.regexpMatch(line, MAGIC_OLD)) {
                extended = StringUtil.regexpMatch(line, MAGIC_EX);
                break;
            }
        }
        return extended;
    }

    /**
     * Read all the data and return the number of strokes
     * @param raf the file to read
     * @return the number of strokes
     *
     * @throws IOException  if read error
     * @throws NumberFormatException  if problem parsing data
     * @throws ParseException  if parse problem
     */
    int readAllData(RandomAccessFile raf)
            throws IOException, NumberFormatException, ParseException {
        ArrayList offsetList = new ArrayList();

        java.text.SimpleDateFormat isoDateTimeFormat =
            new java.text.SimpleDateFormat(TIME_FORMAT);
        isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

        lat_min  = 1000.0;
        lat_max  = -1000.0;
        lon_min  = 1000.0;
        lon_max  = -1000.0;
        time_min = Double.POSITIVE_INFINITY;
        time_max = Double.NEGATIVE_INFINITY;

        raf.seek(0);
        int     count        = 0;
        boolean knowExtended = false;
        while (true) {
            long   offset = raf.getFilePointer();
            String line   = raf.readLine();
            if (line == null) {
                break;
            }
            if (StringUtil.regexpMatch(line, MAGIC)
                    || StringUtil.regexpMatch(line, MAGIC_OLD)) {
                if ( !knowExtended) {
                    isExtended = StringUtil.regexpMatch(line, MAGIC_EX);
                    if (isExtended) {
                        isoDateTimeFormat.applyPattern(TIME_FORMAT_EX);
                    }
                    knowExtended = true;
                }
                continue;
            }

            // 2006-10-23T17:59:39,18.415434,-93.480526,-26.8,1             (original)
            // 2006-10-23T17:59:39,18.415434,-93.480526,-26.8,0.25,0.5,80   (extended)

            StringTokenizer stoker = new StringTokenizer(line, ",\r\n");
            while (stoker.hasMoreTokens()) {
                Date   date     = isoDateTimeFormat.parse(stoker.nextToken());
                double lat      = Double.parseDouble(stoker.nextToken());
                double lon      = Double.parseDouble(stoker.nextToken());
                double amp      = Double.parseDouble(stoker.nextToken());
                int    nstrokes = 1;
                double axisMaj  = Double.NaN;
                double axisMin  = Double.NaN;
                int    orient   = 0;
                if (isExtended) {
                    axisMaj = Double.parseDouble(stoker.nextToken());
                    axisMin = Double.parseDouble(stoker.nextToken());
                    orient  = Integer.parseInt(stoker.nextToken());
                } else {
                    nstrokes = Integer.parseInt(stoker.nextToken());
                }

                Stroke s = isExtended
                           ? new Stroke(date, lat, lon, amp, axisMaj,
                                        axisMin, orient)
                           : new Stroke(date, lat, lon, amp, nstrokes);
                lat_min  = Math.min(lat_min, s.lat);
                lat_max  = Math.max(lat_max, s.lat);
                lon_min  = Math.min(lon_min, s.lon);
                lon_max  = Math.max(lon_max, s.lon);
                time_min = Math.min(time_min, s.secs);
                time_max = Math.max(time_max, s.secs);
            }

            offsetList.add(new Long(offset));
            count++;
        }

        offsets = new long[count];
        for (int i = 0; i < offsetList.size(); i++) {
            Long off = (Long) offsetList.get(i);
            offsets[i] = off.longValue();
        }

        //System.out.println("processed " + count + " records");
        return count;
    }

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
        return new ArraySequence(sm, getStructureIterator(null, 0), nelems);
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
        return new UsplnSeqIter();
    }


    /**
     * Uspln Sequence Iterator
     *
     * @author Unidata Development Team
     */
    private class UsplnSeqIter implements StructureDataIterator {

        /** the wrapped asbb */
        private ArrayStructureBB asbb = null;

        /** number read */
        int numFlashes = 0;

        /**
         * Create a new one
         *
         * @throws IOException problem reading the file
         */
        UsplnSeqIter() throws IOException {
            raf.seek(0);
        }

        @Override public StructureDataIterator reset() {
            numFlashes = 0;
            try {
                raf.seek(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        @Override public boolean hasNext() throws IOException {
            return readStroke();
        }

        @Override public StructureData next() throws IOException {
            numFlashes++;
            return asbb.getStructureData(0);
        }

        /**
         * Read the header
         *
         * @return  true if okay
         *
         * @throws IOException problem reading file
         */
        private boolean readStroke() throws IOException {
            String line = raf.readLine();
            if (line == null) {
                nelems = numFlashes; // track this for next time
                return false;
            }
            if (StringUtil.regexpMatch(line, MAGIC)
                    || StringUtil.regexpMatch(line, MAGIC_OLD)) {
                return readStroke();
            }

            // 2006-10-23T17:59:39,18.415434,-93.480526,-26.8,1             (original)
            // 2006-10-23T17:59:39,18.415434,-93.480526,-26.8,0.25,0.5,80   (extended)
            Stroke          stroke = null;

            StringTokenizer stoker = new StringTokenizer(line, ",\r\n");
            try {
                while (stoker.hasMoreTokens()) {
                    Date   date     = isoDateFormat.parse(stoker.nextToken());
                    double lat      = Double.parseDouble(stoker.nextToken());
                    double lon      = Double.parseDouble(stoker.nextToken());
                    double amp      = Double.parseDouble(stoker.nextToken());
                    int    nstrokes = 1;
                    double axisMaj  = Double.NaN;
                    double axisMin  = Double.NaN;
                    int    orient   = 0;
                    if (isExtended) {
                        axisMaj = Double.parseDouble(stoker.nextToken());
                        axisMin = Double.parseDouble(stoker.nextToken());
                        orient  = Integer.parseInt(stoker.nextToken());
                    } else {
                        nstrokes = Integer.parseInt(stoker.nextToken());
                    }

                    stroke = isExtended
                             ? new Stroke(date, lat, lon, amp, axisMaj,
                                          axisMin, orient)
                             : new Stroke(date, lat, lon, amp, nstrokes);
                }
            } catch (Exception e) {
                //System.out.println("bad flash: " + line.trim());
                return false;
            }

            byte[]     data   = new byte[sm.getStructureSize()];
            ByteBuffer bbdata = ByteBuffer.wrap(data);
            for (String mbrName : sm.getMemberNames()) {
                if (mbrName.equals(TIME)) {
                    bbdata.putDouble(stroke.secs);
                } else if (mbrName.equals(LAT)) {
                    bbdata.putDouble(stroke.lat);
                } else if (mbrName.equals(LON)) {
                    bbdata.putDouble(stroke.lon);
                } else if (mbrName.equals(SIGNAL)) {
                    bbdata.putFloat((float) stroke.amp);
                } else if (mbrName.equals(MULTIPLICITY)) {
                    bbdata.putInt(stroke.n);
                } else if (mbrName.equals(MAJOR_AXIS)) {
                    bbdata.putFloat((float) stroke.axisMajor);
                } else if (mbrName.equals(MINOR_AXIS)) {
                    bbdata.putFloat((float) stroke.axisMinor);
                } else if (mbrName.equals(ELLIPSE_ANGLE)) {
                    bbdata.putInt(stroke.axisOrient);
                }
            }
            asbb = new ArrayStructureBB(sm, new int[] { 1 }, bbdata, 0);
            return true;
        }

        @Override public void setBufferSize(int bytes) {}

        @Override public int getCurrentRecno() {
            return numFlashes - 1;
        }
    }


    /**
     * Get a unique id for this file type.
     * @return registered id of the file type
     * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
     */
    public String getFileTypeId() {
        return "USPLN";
    }

    /**
     * Get a human-readable description for this file type.
     * @return description of the file type
     * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
     */
    public String getFileTypeDescription() {
        return "US Precision Lightning Network";
    }

    /**
     * Get the version of this file type.
     * @return version of the file type
     * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
     */
    public String getFileTypeVersion() {
        return "1";
    }

    /**
     * Class to hold stroke information
     */
    private class Stroke {

        /** the date as seconds since the epoch */
        double secs;

        /** lat, lon and amplitude */
        double lat, lon, amp;

        /** number of strokes */
        int n = 1;

        /** major axis */
        double axisMajor;

        /** minor axis */
        double axisMinor;

        /** major axis orientation */
        int axisOrient;

        /**
         * Create a Stroke from the file data and time formatter
         *
         * @param offset offset into the file
         * @param isoDateTimeFormat  the time formatter
         *
         * @throws IOException  problem reading the file
         * @throws ParseException  problem parsing the file
         */
        Stroke(long offset, java.text.SimpleDateFormat isoDateTimeFormat)
                throws IOException, ParseException {
            raf.seek(offset);
            String line = raf.readLine();
            if ((line == null) || StringUtil.regexpMatch(line, MAGIC)
                    || StringUtil.regexpMatch(line, MAGIC_OLD)) {
                throw new IllegalStateException();
            }

            StringTokenizer stoker = new StringTokenizer(line, ",\r\n");
            Date            date =
                isoDateTimeFormat.parse(stoker.nextToken());
            makeSecs(date);
            lat = Double.parseDouble(stoker.nextToken());
            lon = Double.parseDouble(stoker.nextToken());
            amp = Double.parseDouble(stoker.nextToken());
            if (isExtended) {
                n          = 1;
                axisMajor  = Double.parseDouble(stoker.nextToken());
                axisMinor  = Double.parseDouble(stoker.nextToken());
                axisOrient = Integer.parseInt(stoker.nextToken());
            } else {
                n = Integer.parseInt(stoker.nextToken());
            }

        }

        /**
         * Create a stroke from the info
         *
         * @param date    The Date
         * @param lat  the latitude
         * @param lon  the longitude
         * @param amp  the amplitude
         * @param n    the number of strokes
         */
        Stroke(Date date, double lat, double lon, double amp, int n) {
            makeSecs(date);
            this.lat = lat;
            this.lon = lon;
            this.amp = amp;
            this.n   = n;
        }

        /**
         * Create a stroke from the info
         *
         * @param date    The Date
         * @param lat  the latitude
         * @param lon  the longitude
         * @param amp  the amplitude
         * @param maj  error ellipse major axis
         * @param min  error ellipse minor axis
         * @param orient  orientation of the major axis
         */
        Stroke(Date date, double lat, double lon, double amp, double maj,
                double min, int orient) {
            makeSecs(date);
            this.lat        = lat;
            this.lon        = lon;
            this.amp        = amp;
            this.axisMajor  = maj;
            this.axisMinor  = min;
            this.axisOrient = orient;
        }

        /**
         * Make the date variable
         *
         * @param date the date
         */
        void makeSecs(Date date) {
            this.secs = (int) (date.getTime() / 1000);
        }

        /**
         * Get a String representation of this object
         *
         * @return the String representation
         */
        public String toString() {
            return (isExtended)
                   ? lat + " " + lon + " " + amp + " " + axisMajor + "/"
                     + axisMinor + " " + axisOrient
                   : lat + " " + lon + " " + amp + " " + n;
        }

    }

    /**
     * Class to hold index information
     */
    private class IospData {

        /** the variable number */
        int varno;

        /**
         * Create a holder for the variable number
         *
         * @param varno  the number
         */
        IospData(int varno) {
            this.varno = varno;
        }
    }

}

