/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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

package ucar.atd.dorade;

import ucar.atd.dorade.DoradeDescriptor.DescriptorException;
import ucar.nc2.constants.CDM;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.Date;

public class DoradeSweep {

  /**
   * Bad data value flag
   *
   * @see #getRayData
   */
  public static final float BAD_VALUE = DoradePARM.BAD_VALUE;

  DoradeSSWB mySSWB;
  DoradeVOLD myVOLD;
  DoradeSWIB mySWIB;
  boolean littleEndian;

  public static class DoradeSweepException extends IOException {
    protected DoradeSweepException(String message) {
      super(message);
    }

    protected DoradeSweepException(Exception ex) {
      super(ex);
    }
  }

  public static class MovingSensorException extends IOException {
    protected MovingSensorException(String message) {
      super(message);
    }

    protected MovingSensorException(Exception ex) {
      super(ex);
    }
  }

  /**
   * Construct a <code>DoradeSweep</code> using the named DORADE sweep file.
   *
   * @param filename the DORADE sweepfile to load
   * @throws DoradeSweepException
   * @throws java.io.FileNotFoundException
   */
  public DoradeSweep(String filename)
          throws DoradeSweepException, java.io.FileNotFoundException {

    try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
      littleEndian = DoradeDescriptor.sweepfileIsLittleEndian(file);

      mySSWB = new DoradeSSWB(file, littleEndian);
      myVOLD = new DoradeVOLD(file, littleEndian);
      mySWIB = new DoradeSWIB(file, littleEndian, myVOLD);
    } catch (Exception ex) {
      throw new DoradeSweepException(ex);
    }
  }

  public DoradeSweep(RandomAccessFile file)
          throws DoradeSweepException, java.io.FileNotFoundException {
    //file = new RandomAccessFile(filename, "r");
    try {
      littleEndian = DoradeDescriptor.sweepfileIsLittleEndian(file);

      mySSWB = new DoradeSSWB(file, littleEndian);
      myVOLD = new DoradeVOLD(file, littleEndian);
      mySWIB = new DoradeSWIB(file, littleEndian, myVOLD);
    } catch (Exception ex) {
      throw new DoradeSweepException(ex);
    }
  }

  /**
   * Is this sweepfile little-endian?
   *
   * @return a boolean indicating whether the file is little-endian
   */
  public boolean isLittleEndian() {
    return littleEndian;
  }

  /**
   * Get the array of available parameters.
   *
   * @return the array of available parameters
   */
  public DoradePARM[] getParamList() {
    return myVOLD.getParamList();
  }

  /**
   * Get the parameter descriptor with the given name, ignoring case.
   * If no such descriptor exists, <code>null</code> is returned.
   *
   * @param name the name of the desired parameter
   * @return the <code>DoradePARM</code> from this sweep with the given name,
   * or <code>null</code> if no such parameter exists.
   */
  public DoradePARM lookupParamIgnoreCase(String name) {
    DoradePARM[] list = getParamList();
    for (DoradePARM aList : list) {
      if (aList.getName().equalsIgnoreCase(name))
        return aList;
    }
    return null;
  }

  /**
   * Get the number of rays in this sweep.
   *
   * @return the number of rays in the sweep
   */
  public int getNRays() {
    return mySWIB.getNRays();
  }

  /**
   * Get the number of sensors associated with this sweep.
   *
   * @return the number of sensors associated with the sweep
   */
  public int getNSensors() {
    return myVOLD.getNSensors();
  }

  /**
   * Get the name of a selected sensor.
   *
   * @param which the index of the sensor of interest
   * @return the name of the sensor
   * @see #getNSensors
   */
  public String getSensorName(int which) {
    return myVOLD.getSensorName(which);
  }

  /**
   * Return whether the selected sensor is moving.
   *
   * @param which the index of the sensor of interest
   * @return whether the selected sensor is moving
   * @throws DoradeSweepException
   * @see #getNSensors
   */
  public boolean sensorIsMoving(int which) throws DoradeSweepException {
    try {
      getLatitude(which);
      getLongitude(which);
      getAltitude(which);
    } catch (MovingSensorException mpex) {
      return true;
    }
    return false;
  }

  /**
   * Get the sensor latitude for a non-moving sensor
   *
   * @param which the index of the sensor of interest
   * @return static latitude in degrees
   * @throws MovingSensorException if the sensor is moving
   * @see #getLatitudes
   * @see #sensorIsMoving
   * @see #getNSensors
   */
  public float getLatitude(int which) throws MovingSensorException {
    //
    // Look for per-ray latitudes first.  Return the fixed sensor latitude
    // if no per-ray positions are available.
    //
    float[] lats = mySWIB.getLatitudes();
    if (lats == null)
      return myVOLD.getRADD(which).getLatitude();
    //
    // We have per-ray locations.  Loop through them, and return
    // null if this is not a static platform
    //
    for (int r = 1; r < lats.length; r++)
      if (lats[r] != lats[0])
        throw new MovingSensorException("sensor is not static");

    return lats[0];
  }

  /**
   * Get the sensor longitude for a non-moving sensor
   *
   * @param which the index of the sensor of interest
   * @return static longitude in degrees
   * @throws MovingSensorException if the sensor is moving
   * @see #getLongitudes
   * @see #sensorIsMoving
   * @see #getNSensors
   */
  public float getLongitude(int which) throws MovingSensorException {
    //
    // Look for per-ray longitudes first.  Return the fixed sensor longitude
    // if no per-ray positions are available.
    //
    float[] lons = mySWIB.getLongitudes();
    if (lons == null)
      return myVOLD.getRADD(which).getLongitude();
    //
    // We have per-ray locations.  Loop through them, and return
    // null if this is not a static platform
    //
    for (int r = 1; r < lons.length; r++)
      if (lons[r] != lons[0])
        throw new MovingSensorException("sensor is not static");

    return lons[0];
  }

  /**
   * Get the sensor altitude for a non-moving sensor
   *
   * @param which the index of the sensor of interest
   * @return static altitude in km MSL
   * @throws MovingSensorException if the sensor is moving
   * @see #getAltitudes
   * @see #sensorIsMoving
   * @see #getNSensors
   */
  public float getAltitude(int which) throws MovingSensorException {
    //
    // Look for per-ray altitudes first.  Return the fixed sensor altitude
    // if no per-ray positions are available.
    //
    float[] alts = mySWIB.getAltitudes();
    if (alts == null)
      return myVOLD.getRADD(which).getAltitude();
    //
    // We have per-ray locations.  Loop through them, and return
    // null if this is not a static platform
    //
    for (int r = 1; r < alts.length; r++)
      if (alts[r] != alts[0])
        throw new MovingSensorException("sensor is not static");

    return alts[0];
  }

  /**
   * Get the per-ray array of sensor latitudes.
   *
   * @param which the index of the sensor of interest
   * @return a per-ray array of sensor latitudes
   * @see #getLatitude
   * @see #getNSensors
   */
  public float[] getLatitudes(int which) {
    //
    // Try for per-ray location information.
    //
    float[] lats = mySWIB.getLatitudes();
    //
    // If we don't have per-ray location information, just replicate
    // the sensor fixed latitude.
    //
    if (lats == null) {
      float fixedLat = myVOLD.getRADD(which).getLatitude();
      lats = new float[getNRays()];
      for (int r = 0; r < getNRays(); r++)
        lats[r] = fixedLat;
    }
    return lats;
  }

  /**
   * Get the per-ray array of sensor longitudes.
   *
   * @param which the index of the sensor of interest
   * @return a per-ray array of sensor longitudes
   * @see #getLongitude
   * @see #getNSensors
   */
  public float[] getLongitudes(int which) {
    //
    // Try for per-ray location information.
    //
    float[] lons = mySWIB.getLongitudes();
    //
    // If we don't have per-ray location information, just replicate
    // the sensor fixed latitude.
    //
    if (lons == null) {
      float fixedLon = myVOLD.getRADD(which).getLongitude();
      lons = new float[getNRays()];
      for (int r = 0; r < getNRays(); r++)
        lons[r] = fixedLon;
    }
    return lons;
  }

  /**
   * Get the per-ray array of sensor altitudes.
   *
   * @param which the index of the sensor of interest
   * @return a per-ray array of sensor altitudes
   * @see #getAltitude
   * @see #getNSensors
   * @see #getNRays
   */
  public float[] getAltitudes(int which) {
    //
    // Try for per-ray location information.
    //
    float[] alts = mySWIB.getAltitudes();
    //
    // If we don't have per-ray location information, just replicate
    // the sensor fixed altitude.
    //
    if (alts == null) {
      float fixedAlt = myVOLD.getRADD(which).getAltitude();
      alts = new float[getNRays()];
      for (int r = 0; r < getNRays(); r++)
        alts[r] = fixedAlt;
    }
    return alts;
  }

  /**
   * Get the fixed angle for this sweep.
   *
   * @return the fixed angle of the sweep, in degrees
   */
  public float getFixedAngle() {
    return mySWIB.getFixedAngle();
  }

  // unidata added
  public int getSweepNumber() {
    return mySWIB.getSweepNumber();
  }

  /**
   * Get the (start) time for this sweep.
   *
   * @return the start time of the sweep
   */
  public Date getTime() {
    return mySSWB.getStartTime();
  }

  // unidata added
  public Date[] getTimes() {
    return mySWIB.getTimes();
  }

  // unidata added
  public Date getRayTime(int ray) {
    return mySWIB.getRayTime(ray);
  }

  /**
   * Get the array of data for the given parameter and ray.
   *
   * @param param the parameter of interest
   * @param ray   the index of the ray of interest
   * @return an array containing unpacked values for every cell for the given
   * parameter and ray.  Cells having bad or missing data will hold the
   * value <code>BAD_VALUE</code>
   * @throws DoradeSweepException
   * @see #getNCells
   * @see #getNRays
   * @see #BAD_VALUE
   */
  public float[] getRayData(DoradePARM param, int ray)
          throws DoradeSweepException {
    return getRayData(param, ray, null);
  }


  /**
   * Get the array of data for the given parameter and ray.
   *
   * @param param        the parameter of interest
   * @param ray          the index of the ray of interest
   * @param workingArray If non-null and the same length as what is needed use this instead.
   * @return an array containing unpacked values for every cell for the given
   * parameter and ray.  Cells having bad or missing data will hold the
   * value <code>BAD_VALUE</code>
   * @throws DoradeSweepException
   * @see #getNCells
   * @see #getNRays
   * @see #BAD_VALUE
   */
  public float[] getRayData(DoradePARM param, int ray, float[] workingArray)
          throws DoradeSweepException {
    try {
      return mySWIB.getRayData(param, ray, workingArray);
    } catch (DescriptorException ex) {
      throw new DoradeSweepException(ex);
    }
  }

  /**
   * Get the range to the leading edge of the first cell for the given sensor.
   *
   * @param which index of the sensor of interest
   * @return range to the leading edge of the first cell for the given sensor,
   * in meters
   * @see #getNSensors
   */
  public float getRangeToFirstCell(int which) {
    return myVOLD.getRADD(which).getRangeToFirstCell();
  }

  /**
   * Return the cell spacing for the given sensor, if constant, otherwise
   * return -1
   *
   * @param which index of the sensor of interest
   * @return the constant cell spacing in meters, or -1 if the cell spacing
   * varies
   * @see #getNSensors
   */
  public float getCellSpacing(int which) {
    try {
      return myVOLD.getRADD(which).getCellSpacing();
    } catch (DescriptorException ex) {
      return -1;
    }
  }

  /**
   * Get the number of data cells (gates) per ray for the given sensor.
   *
   * @param which index of the sensor of interest
   * @return the number of cells per ray for the given sensor
   * @see #getNSensors
   */
  public int getNCells(int which) {
    return myVOLD.getRADD(which).getNCells();
  }

  public short getVolumnNumber() {
    return myVOLD.getVolumeNumber();
  }

  public String getProjectName() {
    return myVOLD.getProjectName();
  }


  /**
   * Get the azimuths for all rays in the sweep.
   *
   * @return array of azimuths for all rays in the sweep, in degrees
   * @see #getNRays
   */
  public float[] getAzimuths() {
    return mySWIB.getAzimuths();
  }

  /**
   * Get the elevations for all rays in the sweep.
   *
   * @return array of elevations for all rays in the sweep, in degrees
   * @see #getNRays
   */
  public float[] getElevations() {
    return mySWIB.getElevations();
  }

  /**
   * Return a string with a reasonable representation in UTC of the
   * given <code>Date</code>.
   *
   * @param date <code>Date</code> to be represented
   * @return a string containing a UTC representation of the date
   */
  public static String formatDate(Date date) {
    return DoradeDescriptor.formatDate(date);
  }

  /**
   * <code>DoradeSweep</code> class test method.  Usage:
   * <blockquote><code>
   * $ java ucar.atd.dorade.DoradeSweep &lt;DORADE_sweepfile&gt;
   * </code></blockquote>
   */
  public static void main(String[] args) {
    try {
      if (args.length == 0) {
        System.err.println("Usage: DoradeSweep <filename>");
        System.exit(1);
      }

      DoradeSweep sweepfile = new DoradeSweep(args[0]);
      DoradePARM[] params = sweepfile.getParamList();
      System.out.println(params.length + " params in file");
      for (DoradePARM param : params) mainGetParam(sweepfile, param);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private static void mainGetParam(DoradeSweep sweepfile, DoradePARM param) throws DoradeSweepException {
    System.out.print("getting " + param.getName());
    for (int r = 0; r < sweepfile.getNRays(); r++) {
      float[] vals = sweepfile.getRayData(param, r);
      int nCells = vals.length;
      if (r == 0)
        System.out.println(" (" + nCells + " cells x " +
                sweepfile.getNRays() + " rays)");
    }
  }

  public ScanMode getScanMode() {
    return myVOLD.getRADD(0).getScanMode();
  }


  // unidata added
  public static boolean isDoradeSweep(RandomAccessFile file) throws DoradeSweepException {
    try {
      if (findName(file, "SSWB") || findName(file, "COMM"))
        return true;
    } catch (Exception ex) {
      throw new DoradeSweepException(ex);
    }

    return false;
  }

  // Unidata added: finding the first 4 byte for string matching
  private static boolean findName(RandomAccessFile file, String expectedName) throws IOException {
    byte[] nameBytes = new byte[4];

    //try {
      long filepos = file.getFilePointer();
      file.seek(0);
      if (file.read(nameBytes, 0, 4) == -1)
        return false;  // EOF
      file.seek(filepos);
    //} catch (Exception ex) {
    //  throw new IOException();
    //}

    return expectedName.equals(new String(nameBytes, CDM.utf8Charset));
  }

  // unidata added
  public ScanMode getScanMode(int which) {
    return myVOLD.getRADD(which).getScanMode();
  }

  // unidata added
  public float getUnambiguousVelocity(int which) {
    return myVOLD.getRADD(which).getUnambiguousVelocity();
  }

  //unidata added
  public float getunambiguousRange(int which) {
    return myVOLD.getRADD(which).getunambiguousRange();
  }

  // unidata added
  public float getradarConstant(int which) {
    return myVOLD.getRADD(which).getradarConstant();
  }

  // unidata added
  public float getrcvrGain(int which) {
    return myVOLD.getRADD(which).getrcvrGain();
  }

  // unidata added
  public float getantennaGain(int which) {
    return myVOLD.getRADD(which).getantennaGain();
  }

  // unidata added
  public float getsystemGain(int which) {
    return myVOLD.getRADD(which).getsystemGain();
  }

  // unidata added
  public float gethBeamWidth(int which) {
    return myVOLD.getRADD(which).gethBeamWidth();
  }

  // unidata added
  public float getpeakPower(int which) {
    return myVOLD.getRADD(which).getpeakPower();
  }

  // unidata added
  public float getnoisePower(int which) {
    return myVOLD.getRADD(which).getnoisePower();
  }

}
