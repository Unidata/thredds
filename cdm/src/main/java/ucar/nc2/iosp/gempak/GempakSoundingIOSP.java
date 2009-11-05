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


package ucar.nc2.iosp.gempak;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.iosp.AbstractIOServiceProvider;

import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil;

import visad.util.Trace;

import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.PrintStream;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;


/**
 * An IOSP for Gempak Sounding (SN) data.
 *
 * @author Unidata Java Development Team
 */
public class GempakSoundingIOSP extends GempakStationFileIOSP {

    /** static for shared dimension of length 4 */
    protected final static Dimension DIM_MAXMERGELEVELS =
        new Dimension("maxMergeLevels", 50, true);

    /**
     * _more_
     *
     * @return _more_
     */
    protected AbstractGempakStationFileReader makeStationReader() {
        return new GempakSoundingFileReader();
    }

    /**
     * Is this a valid file?
     *
     * @param raf  RandomAccessFile to check
     *
     * @return true if a valid Gempak grid file
     *
     * @throws IOException  problem reading file
     */
    public boolean isValidFile(RandomAccessFile raf) throws IOException {
        if ( !super.isValidFile(raf)) {
            return false;
        }
        // TODO:  handle other types of surface files 
        return gemreader.getFileSubType()
            .equals(GempakSoundingFileReader.MERGED) || gemreader
            .getFileSubType().equals(GempakSoundingFileReader.UNMERGED);
    }

    /**
     * Get the file type id
     *
     * @return the file type id
     */
    public String getFileTypeId() {
        return "GempakSounding";
    }

    /**
     * Get the file type description
     *
     * @return the file type description
     */
    public String getFileTypeDescription() {
        return "GEMPAK Sounding Obs Data";
    }

    /**
     * Read the data for the variable
     * @param v2  Variable to read
     * @param section   section infomation
     * @return Array of data
     *
     * @throws IOException problem reading from file
     * @throws InvalidRangeException  invalid Range
     */
    public Array readData(Variable v2, Section section)
            throws IOException, InvalidRangeException {
        if (gemreader == null) {
            return null;
        }
        //long start = System.currentTimeMillis();
        //System.out.println("looking for " + v2);
        //System.out.println("Section = " + section);
        //Trace.call1("GEMPAKSIOSP: readData");
        Array array = readSoundingData(v2, section, gemreader.getFileSubType().equals(GempakSoundingFileReader.MERGED));
        /*
        if (gemreader.getFileSubType().equals(
                GempakSoundingFileReader.UNMERGED)) {
            array = readUnmergedData(v2, section, false);
        } else if (gemreader.getFileSubType().equals(
                GempakSoundingFileReader.MERGED)) {
            array = readMergedData(v2, section, true);
        }
        */
        //long took = System.currentTimeMillis() - start;
        //System.out.println("  read data took=" + took + " msec ");
        //Trace.call2("GEMPAKSIOSP: readData");
        return array;
    }

    /**
     * Read in the data for the variable.  In this case, it should be
     * a Structure.  The section should be rank 2 (station, time).
     *
     * @param v2  variable to read
     * @param section  section of the variable
     *
     * @return the array of data
     *
     * @throws IOException  problem reading the file
     */
    private Array readSoundingData(Variable v2, Section section, boolean isMerged)
            throws IOException {

        Array array = null;
        if (v2 instanceof Structure) {

            Range stationRange = section.getRange(0);
            Range timeRange    = section.getRange(1);
            int   size         = stationRange.length() * timeRange.length();

            Structure                     pdata    = (Structure) v2;
            StructureMembers members = pdata.makeStructureMembers();
            List<StructureMembers.Member> mmembers = members.getMembers();
            ArrayStructureBB.setOffsets(members);
            ArrayStructureBB abb = new ArrayStructureBB(members,
                                       new int[] {size});
            ByteBuffer buf = abb.getByteBuffer();

            //Trace.call1("GEMPAKSIOSP: readMergedData" , section.toString());
            for (int y = stationRange.first(); y <= stationRange.last();
                    y += stationRange.stride()) {
                for (int x = timeRange.first(); x <= timeRange.last();
                        x += timeRange.stride()) {
                    List<String> parts = 
                           (isMerged) ? ((GempakSoundingFileReader) gemreader).getMergedParts() : ((GempakSoundingFileReader) gemreader).getUnmergedParts();
                    boolean allMissing = true;
                    for (String part : parts) {

                        List<GempakParameter> params = gemreader.getParameters(part);
                        GempakFileReader.RData vals =
                                    gemreader.DM_RDTR(x + 1, y + 1, part);
                        if (vals == null) {
                            for (StructureMembers.Member member : mmembers) {
                                if (member.getDataType().equals(
                                        DataType.SEQUENCE)) {
                                    Sequence seq = (Sequence) pdata.findVariable(
                                                       member.getName());
                                    ArraySequence aseq = makeEmptySequence(seq);
                                    int index          =
                                        abb.addObjectToHeap(aseq);
                                    buf.putInt(index);
                                } 
                                //else {
                                //    buf.put((byte) 1);
                                //}
                            }
                        } else {
                            allMissing = false;
                            for (StructureMembers.Member member : mmembers) {
                                if (member.getDataType().equals(
                                        DataType.SEQUENCE)) {
                                    Sequence seq = (Sequence) pdata.findVariable(
                                                       member.getName());
                                    ArraySequence aseq = makeArraySequence(seq,
                                                             params, vals.data);
                                    int index          =
                                        abb.addObjectToHeap(aseq);
                                    buf.putInt(index);
                                } 
                                //else if {
                                //buf.put((byte) 0);
                                //}
                            }
                        }
                    }
                    buf.put((byte) (allMissing?1:0));
                }
            }
            array = abb;
            Trace.call2("GEMPAKSIOSP: readMergedData");
        }
        return array;
    }

    /**
     * _more_
     *
     * @param seq _more_
     *
     * @return _more_
     */
    private ArraySequence makeEmptySequence(Sequence seq) {
        StructureMembers members = seq.makeStructureMembers();
        return new ArraySequence(members, new EmptyStructureDataIterator(),
                                 0);
    }

    /**
     * _more_
     *
     * @param seq _more_
     * @param params _more_
     * @param values _more_
     *
     * @return _more_
     */
    private ArraySequence makeArraySequence(Sequence seq,
                                            List<GempakParameter> params,
                                            float[] values) {

        if (values == null) {
            return makeEmptySequence(seq);
        }
        int numLevels = values.length / params.size();
        StructureMembers              members = seq.makeStructureMembers();
        List<StructureMembers.Member> mbers         = members.getMembers();
        int                           offset        = 0;
        int                           numBytes      = 0;
        int                           totalNumBytes = 0;
        for (StructureMembers.Member member : mbers) {
            member.setDataParam(offset);
            offset += 4;
        }
        members.setStructureSize(offset);  // add a byte for missing

        int        size  = offset * numLevels;
        byte[]     bytes = new byte[size];
        ByteBuffer buf   = ByteBuffer.wrap(bytes);
        ArrayStructureBB abb = new ArrayStructureBB(members,
                                   new int[] {numLevels}, buf, 0);
        int var = 0;
        for (int i = 0; i < numLevels; i++) {
            for (GempakParameter param : params) {
                if (members.findMember(param.getName()) != null) {
                    buf.putFloat(values[var]);
                }
                var++;
            }
        }
        return new ArraySequence(members,
                                 new SequenceIterator(numLevels, abb),
                                 numLevels);
    }

    /**
     * Read in the data for the record variable.  In this case, it should be
     * a Structure of record dimension.  We can handle a subset of the
     * variables in a structure.
     *
     * @param v2  variable to read
     * @param section  section of the variable
     *
     * @return the array of data
     *
     * @throws IOException  problem reading the file
     */
    private Array readUnmergedData(Variable v2, Section section)
            throws IOException {

        Array array = null;
        /*
        if (v2 instanceof Structure) {
            List<GempakParameter> params =
                gemreader.getParameters(GempakSurfaceFileReader.SFDT);
            Structure                     pdata     = (Structure) v2;
            StructureMembers members = pdata.makeStructureMembers();
            List<StructureMembers.Member> mbers     = members.getMembers();
            int                           ssize     = 0;
            int                           stnVarNum = 0;
            List<String> stnKeyNames = gemreader.getStationKeyNames();
            for (StructureMembers.Member member : mbers) {
                if (stnKeyNames.contains(member.getName())) {
                    int varSize = getStnVarSize(member.getName());
                    member.setDataParam(ssize);
                    ssize += varSize;
                } else if (member.getName().equals(TIME_VAR)) {
                    member.setDataParam(ssize);
                    ssize += 8;
                } else if (member.getName().equals(MISSING_VAR)) {
                    member.setDataParam(ssize);
                    ssize += 1;
                } else {
                    member.setDataParam(ssize);
                    ssize += 4;
                }
            }
            members.setStructureSize(ssize);

            // TODO:  figure out how to get the missing value for data
            //float[] missing = new float[mbers.size()];
            //int     missnum = 0;
            //for (Variable v : pdata.getVariables()) {
            //    Attribute att = v.findAttribute("missing_value");
            //    missing[missnum++] = (att == null)
            //                         ? GempakConstants.RMISSD
            //                         : att.getNumericValue().floatValue();
            //}


            Range recordRange = section.getRange(0);
            int   size        = recordRange.length();
            // Create a ByteBuffer using a byte array
            byte[]     bytes = new byte[ssize * size];
            ByteBuffer buf   = ByteBuffer.wrap(bytes);
            array = new ArrayStructureBB(members, new int[] { size }, buf, 0);
            List<GempakStation> stationList    = gemreader.getStations();
            List<Date>          dateList       = gemreader.getDates();
            boolean             needToReadData = !pdata.isSubset();
            if ( !needToReadData) {  // subset, see if we need some param data
                for (GempakParameter param : params) {
                    if (members.findMember(param.getName()) != null) {
                        needToReadData = true;
                        break;
                    }
                }
            }
            boolean hasTime = (members.findMember(TIME_VAR) != null);

            //Trace.call1("GEMPAKSIOSP: readUnmergedData", section.toString());
            // fill out the station information
            for (int x = recordRange.first(); x <= recordRange.last();
                    x += recordRange.stride()) {
                GempakStation stn = stationList.get(x);
                for (String varname : stnKeyNames) {
                    if (members.findMember(varname) == null) {
                        continue;
                    }
                    String temp = null;
                    if (varname.equals(GempakStation.STID)) {
                        temp = StringUtil.padRight(stn.getName(), 8);
                    } else if (varname.equals(GempakStation.STNM)) {
                        buf.putInt((int) (stn.getSTNM()));
                    } else if (varname.equals(GempakStation.SLAT)) {
                        buf.putFloat((float) stn.getLatitude());
                    } else if (varname.equals(GempakStation.SLON)) {
                        buf.putFloat((float) stn.getLongitude());
                    } else if (varname.equals(GempakStation.SELV)) {
                        buf.putFloat((float) stn.getAltitude());
                    } else if (varname.equals(GempakStation.STAT)) {
                        temp = StringUtil.padRight(stn.getSTAT(), 2);
                    } else if (varname.equals(GempakStation.COUN)) {
                        temp = StringUtil.padRight(stn.getCOUN(), 2);
                    } else if (varname.equals(GempakStation.STD2)) {
                        temp = StringUtil.padRight(stn.getSTD2(), 4);
                    } else if (varname.equals(GempakStation.SPRI)) {
                        buf.putInt(stn.getSPRI());
                    } else if (varname.equals(GempakStation.SWFO)) {
                        temp = StringUtil.padRight(stn.getSWFO(), 4);
                    } else if (varname.equals(GempakStation.WFO2)) {
                        temp = StringUtil.padRight(stn.getWFO2(), 4);
                    }
                    if (temp != null) {
                        buf.put(temp.getBytes());
                    }
                }
                if (members.findMember(TIME_VAR) != null) {
                    // put in the time
                    Date time = dateList.get(x);
                    buf.putDouble(time.getTime() / 1000.d);
                }

                if (needToReadData) {
                    int column = stn.getIndex();
                    GempakFileReader.RData vals = gemreader.DM_RDTR(1,
                                                      column, GempakSurfaceFileReader.SFDT);
                    if (vals == null) {
                        for (GempakParameter param : params) {
                            if (members.findMember(param.getName()) != null) {
                                buf.putFloat(GempakConstants.RMISSD);
                            }
                        }
                        buf.put((byte) 1);
                    } else {
                        float[] reals = vals.data;
                        int     var   = 0;
                        for (GempakParameter param : params) {
                            if (members.findMember(param.getName()) != null) {
                                buf.putFloat(reals[var]);
                            }
                            var++;
                        }
                        buf.put((byte) 0);
                    }
                }
            }
            //Trace.call2("GEMPAKSIOSP: readUnmergedData");
        }
        */
        return array;
    }

    /**
     * Test this.
     *
     * @param args file name
     *
     * @throws IOException  problem reading the file
     */
    public static void main(String[] args) throws IOException {
        IOServiceProvider mciosp = new GempakSoundingIOSP();
        RandomAccessFile  rf     = new RandomAccessFile(args[0], "r", 2048);
        NetcdfFile ncfile = new MakeNetcdfFile(mciosp, rf, args[0], null);
        if (args.length > 1) {
            ucar.nc2.FileWriter.writeToFile(ncfile, args[1]);
        } else {
            System.out.println(ncfile);
        }
    }

    /**
     *   TODO:  generalize this
     *   static class for testing
     */
    protected static class MakeNetcdfFile extends NetcdfFile {

        /**
         * Ctor
         *
         * @param spi IOServiceProvider
         * @param raf RandomAccessFile
         * @param location   location of file?
         * @param cancelTask CancelTask
         *
         * @throws IOException problem opening the file
         */
        MakeNetcdfFile(IOServiceProvider spi, RandomAccessFile raf,
                       String location, CancelTask cancelTask)
                throws IOException {
            super(spi, raf, location, cancelTask);
        }
    }

    /**
     * Build the netCDF file
     *
     * @throws IOException   problem reading the file
     */
    protected void fillNCFile() throws IOException {
        String fileType = gemreader.getFileSubType();
        buildFile(fileType.equals(GempakSoundingFileReader.MERGED));
    }

    /**
     * Build a standard station structure
     *
     * @param isMerged  true if this is a merged file
     */
    private void buildFile(boolean isMerged) {

        // Build station list
        List<GempakStation> stations = gemreader.getStations();
        Dimension station = new Dimension("station", stations.size(), true);
        ncfile.addDimension(null, station);
        ncfile.addDimension(null, DIM_LEN4);
        ncfile.addDimension(null, DIM_LEN2);
        List<Variable> stationVars = makeStationVars(stations, station);
        // loop through and add to ncfile
        for (Variable stnVar : stationVars) {
            ncfile.addVariable(null, stnVar);
        }


        // Build variable list (var(station,time))
        // time
        List<Date> timeList = gemreader.getDates();
        int        numTimes = timeList.size();
        Dimension  times    = new Dimension(TIME_VAR, numTimes, true);
        ncfile.addDimension(null, times);
        Array varArray = null;
        Variable timeVar = new Variable(ncfile, null, null, TIME_VAR,
                                        DataType.DOUBLE, TIME_VAR);
        timeVar.addAttribute(
            new Attribute("units", "seconds since 1970-01-01 00:00:00"));
        timeVar.addAttribute(new Attribute("long_name", TIME_VAR));
        varArray = new ArrayDouble.D1(numTimes);
        int i = 0;
        for (Date date : timeList) {
            ((ArrayDouble.D1) varArray).set(i, date.getTime() / 1000.d);
            i++;
        }
        timeVar.setCachedData(varArray, false);
        ncfile.addVariable(null, timeVar);


        // build the data structure
        List<Dimension> stationTime = new ArrayList<Dimension>();
        stationTime.add(station);
        stationTime.add(times);
        String structName = (isMerged)
                            ? GempakSoundingFileReader.MERGED
                            : GempakSoundingFileReader.UNMERGED;
        structName = structName + "Sounding";
        Structure sVar = new Structure(ncfile, null, null, structName);
        sVar.setDimensions(stationTime);
        sVar.addAttribute(new Attribute("coordinates",
                                        "time SLAT SLON SELV"));
        List<String> sequenceNames;
        if (isMerged) {
            sequenceNames = new ArrayList<String>();
            sequenceNames.add(GempakSoundingFileReader.SNDT);
        } else {
            sequenceNames =
                ((GempakSoundingFileReader) gemreader).getUnmergedParts();
        }
        for (String seqName : sequenceNames) {
            Sequence paramData = makeSequence(sVar, seqName, false);
            if (paramData == null) {
                continue;
            }
            sVar.addMemberVariable(paramData);
        }
        sVar.addMemberVariable(makeMissingVariable());
        ncfile.addAttribute(
            null,
            new Attribute(
                "CF:featureType", CF.FeatureType.stationProfile.toString()));
        ncfile.addVariable(null, sVar);
    }


    /**
     * Make a structure for the part
     *
     *
     * @param parent  parent structure
     * @param partName   partname
     * @param includeMissing  true to include the missing variable
     *
     * @return  a Structure
     */
    protected Sequence makeSequence(Structure parent, String partName,
                                    boolean includeMissing) {
        List<GempakParameter> params = gemreader.getParameters(partName);
        if (params == null) {
            return null;
        }
        Sequence sVar = new Sequence(ncfile, null, parent, partName);
        sVar.setDimensions("");
        for (GempakParameter param : params) {
            sVar.addMemberVariable(makeParamVariable(param, null));
        }
        if (includeMissing) {
            sVar.addMemberVariable(makeMissingVariable());
        }
        return sVar;
    }




    /**
     * Class EmptyStructureDataIterator _more_
     *
     *
     * @author Unidata Development Team
     */
    class EmptyStructureDataIterator implements StructureDataIterator {

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        @Override public boolean hasNext() throws IOException {
            return false;
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        @Override public StructureData next() throws IOException {
            return null;
        }

        /**
         * _more_
         *
         * @param bytes _more_
         */
        @Override public void setBufferSize(int bytes) {}

        /**
         * _more_
         *
         * @return _more_
         */
        @Override public StructureDataIterator reset() {
            return this;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        @Override public int getCurrentRecno() {
            return -1;
        }
    }

    /**
     * Class SequenceIterator _more_
     *
     *
     * @author Unidata Development Team
     */
    private class SequenceIterator implements StructureDataIterator {

        /** _more_          */
        private int count;

        /** _more_          */
        private ArrayStructure abb;

        /** _more_          */
        private StructureDataIterator siter;

        /**
         * _more_
         *
         * @param count _more_
         * @param abb _more_
         */
        SequenceIterator(int count, ArrayStructure abb) {
            this.count = count;
            this.abb   = abb;
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        @Override public boolean hasNext() throws IOException {
            if (siter == null) {
                siter = abb.getStructureDataIterator();
            }
            return siter.hasNext();
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        @Override public StructureData next() throws IOException {
            return siter.next();
        }

        /**
         * _more_
         *
         * @param bytes _more_
         */
        @Override public void setBufferSize(int bytes) {
            siter.setBufferSize(bytes);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        @Override public StructureDataIterator reset() {
            siter = null;
            return this;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        @Override public int getCurrentRecno() {
            return siter.getCurrentRecno();
        }

    }
}

