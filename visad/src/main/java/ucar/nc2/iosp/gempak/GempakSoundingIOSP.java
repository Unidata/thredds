/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package ucar.nc2.iosp.gempak;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * An IOSP for Gempak Sounding (SN) data.
 *
 * @author dmurray
 */
public class GempakSoundingIOSP extends GempakStationFileIOSP {
  /*
  dmlib.txt:
                   Sounding (SN) Library

  The sounding library subroutines allow the programmer to access
  GEMPAK upper-air data files.  These files contain meteorological
  observations from many locations for different times.  The
  library contains modules which create and open files and read or
  write data to these files.

  There are two types of GEMPAK sounding files:  merged and unmerged.
  Merged files may contain an arbitrary set of parameters which
  report at every level.  Unmerged files store mandatory and
  significant data separately in the following parts with the
  given parameters:

    TTAA  mandatory data below 100 mb  PRES TEMP DWPT DRCT SPED HGHT
    TTBB  sig temp data below 100 mb   PRES TEMP DWPT
    PPBB  sig wind data below 100 mb   HGHT DRCT SPED or
                                       PRES DRCT SPED
    TTCC  mandatory data above 100 mb  PRES TEMP DWPT DRCT SPED HGHT
    TTDD  sig temp data above 100 mb   PRES TEMP DWPT
    PPDD  sig wind data above 100 mb   HGHT DRCT SPED or
                                       PRES DRCT SPED

  When wind data appear on pressure surfaces, the first pressure is
  set to the negative of its value as a flag.

  Data that are to be written to an unmerged file must be in the
  specified order.  When data are returned from an unmerged file,
  data from all the parts will be merged.  Interpolation will be
  used to fill in the significant data levels.

  Merged data files can be created using SN_CREF or SN_CRFP;
  unmerged files can be created using SN_CRUA.  SN_OPNF will open
  either file type.  SN_RDAT will read data from all files;
  unmerged data will be returned as a merged data set.  SN_RTYP can
  be called to determine whether each level is mandatory, significant
  temperature, or significant wind level data.  SN_MAND can
  be called to request that only mandatory data below 100 mb be
  returned when SN_RDAT is called.  SN_WDAT writes to merged files;
  SN_WPRT writes to unmerged files.

  The subroutines to create or open a sounding file return a file
  number which must be used in later subroutines to access the file.

  The file GEMPRM.PRM contains the maximum values for array
  dimensions when using GEMPAK subroutines.  A copy of this file has
  been included in the appendix for easy reference.  MMFILE is the
  maximum number of files that can be open.  LLMXTM is the maximum
  number of times that can be saved in a GEMPAK5 file. The maximum
  number of stations is LLSTFL and the maximum number of parameters
  is MMPARM.

  After a file is opened, both the time and station must be selected
  before data can be read or written.  There are two groups of
  subroutines that perform this function.

  If data from many stations are to be accessed for a particular
  time, the time can be set using SN_STIM.  The stations to be
  selected may be defined using LC_SARE or LC_UARE, which select
  stations using the GEMPAK variable, AREA.  The LC subroutines may
  be called before or after SN_STIM.  Stations within the area are
  returned using SN_SNXT.

  If data for many times at a particular station are required, the
  station may be selected using SN_TSTN.  The time may then be
  defined using SN_TTIM.  Alternatively, times may be returned using
  SN_TNXT.

  All GEMPAK files contain information about the station in station
  headers.  The station header names, contents, and the data types
  returned from the SN library are:

          STID    Station identifier            CHARACTER*4
          STNM    Station number                INTEGER
          SLAT    Station latitude              REAL
          SLON    Station longitude             REAL
          SELV    Station elevation in meters   REAL
          STAT    State                         CHARACTER*2
          COUN    Country                       CHARACTER*2
    STD2	Extended station ID	      CHARACTER*4

  Only SLAT and SLON are required for sounding files.  The other
  header variables are optional.

  The subroutines SN_FTIM and SN_FSTN can be used to find a time
  and station in a data set.  They will execute faster than the
  subroutines above, but can only be used with files where the times
  are in rows and the stations are in columns (or vice versa).  They were
  designed to be used in real-time data ingest applications and should
  not be used for normal applications which use general sounding
  files.

  Some examples of subroutine sequences for accessing the data follow.

  A sequence of subroutines to retrieve sounding data for many
  stations at one time is:

          Initialize GEMPAK                       (IN_BDTA)

    Open the file				(SN_OPNF)
    Define the time				(SN_STIM)
    Define the area search			(LC_SARE)
    Loop:
       Get the next station			(SN_SNXT)
       Read the data			(SN_RDAT)
    End loop
    Close the file				(SN_CLOS)

  A sequence of subroutines to retrieve sounding data for many times
  at one station is:

    Open the sounding file			(SN_OPNF)
    Get times in file                       (SN_GTIM)
    Get times to use                        (TI_FIND)
    Set the station				(SN_TSTN)
    Loop:
       Get the next time			(SN_TTIM)
       Read the data			(SN_RDAT)
    End loop
    Close the file				(SN_CLOS)

  ================================================================================


  Also, check out Don's GEMPAK Support reply to Patrick Marsh
  about the GEMPAK data format:

  http://www.unidata.ucar.edu/support/help/MailArchives/gempak/msg05580.html

  There is some documention in the GEMPAK distribution in the
  gempak/txt/gemlib directory.

  In particular, dmlib.txt explains the basic DM (Data Management)
  structure.  The other files explain specific implementations
  for grids (gdlib.txt), surface (sflib.txt) and sounding (snlib.txt).

  =====

    Another thing is this from an email Michal sent me:


  SNLIST functions to look at

  snlpdt.f
  https://github.com/Unidata/gempak/blob/master/gempak/source/programs/sn/snlist/snlpdt.f

  starting on line 133 is IF/then for different listing routines, SNLWMN, SNLWTM, SNLWSG, etc.

  snlpdt called SN_RPRT to read mandatory level data (merged data uses SN_RDAT)
  https://github.com/Unidata/gempak/blob/master/gempak/source/snlib/snrprt.f

  DM_RDTR and DM_RFLT are used to read data from DM files

  https://github.com/Unidata/gempak/blob/master/gempak/source/gemlib/dm/dmrdtr.f
  https://github.com/Unidata/gempak/blob/master/gempak/source/gemlib/dm/dmrflt.f

 */

  /**
   * static for shared dimension of length 4
   */
  protected final static Dimension DIM_MAXMERGELEVELS = new Dimension("maxMergeLevels", 50, true);

  /**
   * Make the station reader for this type
   *
   * @return a GempakSoundingFileReader
   */
  protected AbstractGempakStationFileReader makeStationReader() {
    return new GempakSoundingFileReader();
  }

  /**
   * Is this a valid file?
   *
   * @param raf RandomAccessFile to check
   * @return true if a valid Gempak grid file
   * @throws IOException problem reading file
   */
  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    if (!super.isValidFile(raf)) {
      return false;
    }
    // TODO:  handle other types of surface files
    return gemreader.getFileSubType().equals(GempakSoundingFileReader.MERGED) ||
            gemreader.getFileSubType().equals(GempakSoundingFileReader.UNMERGED);
  }

  /**
   * Get the file type id
   *
   * @return the file type id
   */
  @Override
  public String getFileTypeId() {
    return "GempakSounding";
  }

  /**
   * Get the file type description
   *
   * @return the file type description
   */
  @Override
  public String getFileTypeDescription() {
    return "GEMPAK Sounding Obs Data";
  }

  /**
   * Get the CF feature type
   *
   * @return the feature type
   */
  public String getCFFeatureType() {
    return CF.FeatureType.timeSeriesProfile.toString();
  }

  /**
   * Read the data for the variable
   *
   * @param v2      Variable to read
   * @param section section infomation
   * @return Array of data
   * @throws IOException           problem reading from file
   * @throws InvalidRangeException invalid Range
   */
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    if (gemreader == null) {
      throw new IllegalStateException("reader not initialized");
    }
    return readSoundingData(v2, section, gemreader.getFileSubType().equals(GempakSoundingFileReader.MERGED));
  }

  /**
   * Read in the data for the variable.  In this case, it should be
   * a Structure.  The section should be rank 2 (station, time).
   *
   * @param v2       variable to read
   * @param section  section of the variable
   * @param isMerged flag for merged data or not
   * @return the array of data
   * @throws IOException problem reading the file
   */
  private Array readSoundingData(Variable v2, Section section, boolean isMerged) throws IOException {

    Array array = null;
    if (v2 instanceof Structure) {

      Range stationRange = section.getRange(0);
      Range timeRange = section.getRange(1);
      int size = stationRange.length() * timeRange.length();

      Structure pdata = (Structure) v2;
      StructureMembers members = pdata.makeStructureMembers();
      ArrayStructureBB.setOffsets(members);
      ArrayStructureBB abb = new ArrayStructureBB(members, new int[]{size});
      ByteBuffer buf = abb.getByteBuffer();

      //Trace.call1("GEMPAKSIOSP: readMergedData" , section.toString());
      for (int y = stationRange.first(); y <= stationRange.last(); y += stationRange.stride()) {
        for (int x = timeRange.first(); x <= timeRange.last(); x += timeRange.stride()) {
          List<String> parts = (isMerged) ? ((GempakSoundingFileReader) gemreader).getMergedParts()
                  : ((GempakSoundingFileReader) gemreader).getUnmergedParts();
          boolean allMissing = true;
          for (String part : parts) {

            List<GempakParameter> params = gemreader.getParameters(part);
            GempakFileReader.RData vals = gemreader.DM_RDTR(x + 1, y + 1, part);
            ArraySequence aseq;
            Sequence seq = (Sequence) pdata.findVariable(part);
            if (vals == null) {
              aseq = makeEmptySequence(seq);
            } else {
              allMissing = false;
              aseq = makeArraySequence(seq, params, vals.data);
            }
            int index = abb.addObjectToHeap(aseq);
            buf.putInt(index);
          }
          buf.put((byte) (allMissing ? 1 : 0));
        }
      }
      array = abb;
    }
    return array;
  }

  /**
   * Create an empty ArraySequence for missing data
   *
   * @param seq the Sequence variable
   * @return the empty sequence
   */
  private ArraySequence makeEmptySequence(Sequence seq) {
    StructureMembers members = seq.makeStructureMembers();
    return new ArraySequence(members, new EmptyStructureDataIterator(), -1);
  }

  /**
   * Create an ArraySequence to hold the data
   *
   * @param seq    the Sequence variable
   * @param params the list of all GempakParameters possible in that sequence
   * @param values the values that were read
   * @return the ArraySequence
   */
  private ArraySequence makeArraySequence(Sequence seq, List<GempakParameter> params, float[] values) {

    if (values == null) {
      return makeEmptySequence(seq);
    }

    int numLevels = values.length / params.size();
    StructureMembers members = seq.makeStructureMembers();
    int offset = ArrayStructureBB.setOffsets(members);

    int size = offset * numLevels;
    byte[] bytes = new byte[size];
    ByteBuffer buf = ByteBuffer.wrap(bytes);
    ArrayStructureBB abb = new ArrayStructureBB(members, new int[]{numLevels}, buf, 0);
    int var = 0;
    for (int i = 0; i < numLevels; i++) {
      for (GempakParameter param : params) {
        if (members.findMember(param.getName()) != null) {
          buf.putFloat(values[var]);
        }
        var++;
      }
    }
    return new ArraySequence(members, new SequenceIterator(numLevels, abb), numLevels);
  }

  /**
   * Build the netCDF file
   *
   * @throws IOException problem reading the file
   */
  protected void fillNCFile() throws IOException {
    String fileType = gemreader.getFileSubType();
    buildFile(fileType.equals(GempakSoundingFileReader.MERGED));
  }

  /**
   * Build a standard station structure
   *
   * @param isMerged true if this is a merged file
   */
  private void buildFile(boolean isMerged) {

    // Build station list
    List<GempakStation> stations = gemreader.getStations();
    Dimension station = new Dimension("station", stations.size(), true);
    ncfile.addDimension(null, station);
    ncfile.addDimension(null, DIM_LEN8);
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
    int numTimes = timeList.size();
    Dimension times = new Dimension(TIME_VAR, numTimes, true);
    ncfile.addDimension(null, times);
    Array varArray;
    Variable timeVar = new Variable(ncfile, null, null, TIME_VAR, DataType.DOUBLE, TIME_VAR);
    timeVar.addAttribute(new Attribute(CDM.UNITS, "seconds since 1970-01-01 00:00:00"));
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
    List<Dimension> stationTime = new ArrayList<>();
    stationTime.add(station);
    stationTime.add(times);
    String structName = (isMerged) ? GempakSoundingFileReader.MERGED : GempakSoundingFileReader.UNMERGED;
    structName = structName + "Sounding";
    Structure sVar = new Structure(ncfile, null, null, structName);
    sVar.setDimensions(stationTime);
    sVar.addAttribute(new Attribute(CF.COORDINATES, "time SLAT SLON SELV"));
    List<String> sequenceNames;
    if (isMerged) {
      sequenceNames = new ArrayList<>();
      sequenceNames.add(GempakSoundingFileReader.SNDT);
    } else {
      sequenceNames = ((GempakSoundingFileReader) gemreader).getUnmergedParts();
    }
    for (String seqName : sequenceNames) {
      Sequence paramData = makeSequence(sVar, seqName, false);
      if (paramData == null) {
        continue;
      }
      sVar.addMemberVariable(paramData);
    }
    sVar.addMemberVariable(makeMissingVariable());
    ncfile.addAttribute(null, new Attribute("CF:featureType", CF.FeatureType.timeSeriesProfile.toString()));
    ncfile.addVariable(null, sVar);
  }


  /**
   * Make a Sequence for the part
   *
   * @param parent         parent structure
   * @param partName       partname
   * @param includeMissing true to include the missing variable
   * @return a Structure
   */
  protected Sequence makeSequence(Structure parent, String partName, boolean includeMissing) {
    List<GempakParameter> params = gemreader.getParameters(partName);
    if (params == null) {
      return null;
    }
    Sequence sVar = new Sequence(ncfile, null, parent, partName);
    sVar.setDimensions("");
    for (GempakParameter param : params) {
      Variable v = makeParamVariable(param, null);
      addVerticalCoordAttribute(v);
      sVar.addMemberVariable(v);
    }
    if (includeMissing) {
      sVar.addMemberVariable(makeMissingVariable());
    }
    return sVar;
  }

  /**
   * Add the vertical coordinate variables if necessary
   *
   * @param v the variable
   */
  private void addVerticalCoordAttribute(Variable v) {

    GempakSoundingFileReader gsfr = (GempakSoundingFileReader) gemreader;
    int vertType = gsfr.getVerticalCoordinate();
    String pName = v.getFullName();
    if (gemreader.getFileSubType().equals(GempakSoundingFileReader.MERGED)) {
      if ((vertType == GempakSoundingFileReader.PRES_COORD) && pName.equals("PRES")) {
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.name()));
      } else if ((vertType == GempakSoundingFileReader.HGHT_COORD) && (pName.equals("HGHT") || pName.equals("MHGT") || pName.equals("DHGT"))) {
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.name()));
      }
    } else if (pName.equals("PRES")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.name()));
    }
  }

  /**
   * An empty sequence iterator
   *
   * @author Unidata Development Team
   */
  static class EmptyStructureDataIterator implements StructureDataIterator {

    /**
     * Do we have more?
     *
     * @return false
     * @throws IOException problem with read
     */
    @Override
    public boolean hasNext() throws IOException {
      return false;
    }

    /**
     * Get the next data
     *
     * @return null
     * @throws IOException problem with read
     */
    @Override
    public StructureData next() throws IOException {
      return null;
    }

    /**
     * Set the buffer size
     *
     * @param bytes the buffer size
     */
    @Override
    public void setBufferSize(int bytes) {
    }

    /**
     * Reset the iterator
     *
     * @return this
     */
    @Override
    public StructureDataIterator reset() {
      return this;
    }

    /**
     * Get the current record number
     *
     * @return -1
     */
    @Override
    public int getCurrentRecno() {
      return -1;
    }

    @Override
    public void finish() {
      // ignored
    }
  }

  /**
   * A data iterator for a sequence
   *
   * @author Unidata Development Team
   */
  static private class SequenceIterator implements StructureDataIterator {

    /**
     * the number of records
     */
    // private int count;

    /**
     * the backing structure
     */
    private ArrayStructure abb;

    /**
     * the iterator
     */
    private StructureDataIterator siter;

    /**
     * Create a new iterator for the ArrayStructure
     *
     * @param count the number of records
     * @param abb   the backing store
     */
    SequenceIterator(int count, ArrayStructure abb) {
      // this.count = count;
      this.abb = abb;
    }

    /**
     * Do we have more?
     *
     * @return true if we do
     * @throws IOException problem with read
     */
    @Override
    public boolean hasNext() throws IOException {
      if (siter == null) {
        siter = abb.getStructureDataIterator();
      }
      return siter.hasNext();
    }

    /**
     * Get the next StructureData
     *
     * @return the next StructureData
     * @throws IOException problem with read
     */
    @Override
    public StructureData next() throws IOException {
      return siter.next();
    }

    /**
     * Set the buffer size
     *
     * @param bytes the buffer size
     */
    @Override
    public void setBufferSize(int bytes) {
      siter.setBufferSize(bytes);
    }

    /**
     * Reset the iterator
     *
     * @return this
     */
    @Override
    public StructureDataIterator reset() {
      siter = null;
      return this;
    }

    /**
     * Get the current record number
     *
     * @return the current record number
     */
    @Override
    public int getCurrentRecno() {
      return siter.getCurrentRecno();
    }

    @Override
    public void finish() {
      siter.finish();
    }

  }

}

