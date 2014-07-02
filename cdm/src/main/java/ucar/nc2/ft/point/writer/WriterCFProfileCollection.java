/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.point.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;

/**
 * Write a CF "Discrete Sample" profile collection file.
 * Example H.3.5. Continguous ragged array representation of profiles, H.3.4
 *
 * <p/>
 * <pre>
 *   writeHeader()
 *   iterate { writeRecord() }
 *   finish()
 * </pre>
 *
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8372832"
 * @author caron
 * @since April, 2012
 */
public class WriterCFProfileCollection extends CFPointWriter {
  private static final String profileStructName = "profile";
  private static final String profileDimName = "profile";
  private static final String idName = "profileId";
  private static final String profileRowSizeName = "nobs";
  private static final String profileTimeName = "profileNominalTime";
  private static final boolean debug = false;

  ///////////////////////////////////////////////////
  //private Variable lat, lon, alt, time, id, rowSize;
  protected Structure profileStruct;  // used for netcdf4 extended

  private List<VariableSimpleIF> dataVars;
  private boolean headerDone = false;
  private int nprofiles, name_strlen;

  public WriterCFProfileCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, config);
    this.dataVars = dataVars;
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.profile.name()));
  }

  public void setHeaderInfo(int nprofiles, int name_strlen) {
    this.nprofiles = nprofiles;
    this.name_strlen = name_strlen;
  }

  // LOOK only reason we need profile names ahead of time is to get str_len
  public void writeHeader(int name_strlen, int nprofiles, DateUnit timeUnit, String altUnits) throws IOException {
    this.altUnits = altUnits;
    this.recordDim = writer.addUnlimitedDimension(recordDimName);

    List<VariableSimpleIF> coords = new ArrayList<>();
    coords.add( VariableSimpleImpl.makeScalar(altName, "obs altitude", altUnits, DataType.DOUBLE)
            .add(new Attribute(CF.STANDARD_NAME, "altitude"))
            .add(new Attribute(CF.POSITIVE, CF1Convention.getZisPositive(altName, altUnits))));

    if (writer.getVersion().isExtendedModel()) {
      makeProfileVars(name_strlen, nprofiles, timeUnit, true);
      record = (Structure) writer.addVariable(null, recordName, DataType.STRUCTURE, recordDimName);
      addCoordinatesExtended(record, coords);
      addVariablesExtended(dataVars);
      record.calcElementSize();
      writer.create();
      //writeProfileDataExtended(stns);

    } else {
      makeProfileVars(name_strlen, nprofiles, timeUnit, false);
      addCoordinatesClassic(recordDim, coords);
      addDataVariablesClassic(recordDim, dataVars);
      writer.create();
      record = writer.addRecordStructure(); // netcdf3

      /* lat = writer.findVariable(latName);
      lon = writer.findVariable(lonName);
      alt = writer.findVariable(altName);
      id = writer.findVariable(idName);
      time = writer.findVariable(profileTimeName);
      rowSize = writer.findVariable(profileRowSizeName);  */

      //writeProfileData(profileNames); // write out the station info
    }

  }

  private void makeProfileVars(int name_strlen, int nprofiles, DateUnit timeUnit, boolean isExtended) throws IOException {
    name_strlen = Math.max( name_strlen, 20);

    // LOOK why not unlimited here ?
    Dimension profileDim = writer.addDimension(null, profileDimName, nprofiles);
    // Dimension profileDim = isExtendedModel ?  writer.addUnlimitedDimension(profileDimName) : writer.addDimension(null, profileDimName, nprofiles);

    // add the profile Variables using the profile dimension
    List<VariableSimpleIF> coords = new ArrayList<>();
    coords.add(VariableSimpleImpl.makeScalar(latName, "profile latitude", CDM.LAT_UNITS, DataType.DOUBLE));
    coords.add(VariableSimpleImpl.makeScalar(lonName, "profile longitude", CDM.LON_UNITS, DataType.DOUBLE));
    coords.add(VariableSimpleImpl.makeString(idName, "profile identifier", null, name_strlen)
            .add(new Attribute(CF.CF_ROLE, CF.PROFILE_ID)));         // profileId:cf_role = "profile_id";

    coords.add(VariableSimpleImpl.makeScalar(profileRowSizeName, "number of obs for this profile", null, DataType.INT)
            .add(new Attribute(CF.SAMPLE_DIMENSION, recordDimName)));         // rowSize:sample_dimension = "obs"

    coords.add(VariableSimpleImpl.makeScalar(profileTimeName, "nominal time of profile", timeUnit.getUnitsString(), DataType.DOUBLE));

    if (isExtended) {
      profileStruct = (Structure) writer.addVariable(null, profileStructName, DataType.STRUCTURE, profileDimName);
      addCoordinatesExtended(profileStruct, coords);
    } else {
      addCoordinatesClassic(profileDim, coords);
    }
  }

  public int writeProfile (ProfileFeature profile) throws IOException {
    profile.resetIteration();
    int count = 0;
    while (profile.hasNext()) {
      PointFeature pf = profile.next();
      if (!headerDone) {
        if (name_strlen == 0) name_strlen = Math.min(profile.getName().length(), 12);
        writeHeader(name_strlen, nprofiles, pf.getTimeUnit(), pf.getAltUnits());  // have to get time units, alt units from pointFeature (!)
        headerDone = true;
      }
      writeObsData(pf);
      count++;
    }

    writeProfileData(profile, count, isExtendedModel);
    return count;
  }

  private int profileRecno = 0;
  public void writeProfileData(ProfileFeature profile, int nobs, boolean isExtended) throws IOException {
    trackBB(profile.getLatLon(), CalendarDate.of(profile.getTime()));

    StructureDataScalar profileData = new StructureDataScalar("Coords");
    profileData.addMember(latName, null, null, DataType.DOUBLE, false, profile.getLatLon().getLatitude());
    profileData.addMember(lonName, null, null, DataType.DOUBLE, false, profile.getLatLon().getLongitude());
    profileData.addMember(profileTimeName, null, null, DataType.DOUBLE, false, (double) profile.getTime().getTime());  // LOOK time not always part of profile
    profileData.addMemberString(idName, null, null, profile.getName().trim(), name_strlen);
    profileData.addMember(profileRowSizeName, null, null, DataType.INT, false, nobs);

    // write the recno record
    int[] origin = new int[1];
    origin[0] = profileRecno;
    try {
      super.writeStructureData(isExtendedModel, profileStruct, origin, profileData);

      /* if (!isExtendedModel) {
        timeArray.set(0, timeCoordValue);
        parentArray.set(0, parentIndex);

        writer.write(time, origin, timeArray);
        writer.write(profileIndex, origin, parentArray);
      } */

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    profileRecno++;
  }


  private int recno = 0;
  private ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);
  private ArrayInt.D1 parentArray = new ArrayInt.D1(1);

  public void writeObsData(PointFeature pf) throws IOException {

    StructureDataScalar coords = new StructureDataScalar("Coords");
    coords.addMember(altName, null, null, DataType.DOUBLE, false, pf.getLocation().getAltitude());

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(coords); // coords first so it takes precedence
    sdall.add(pf.getData());

    // write the recno record
    int[] origin = new int[1];
    origin[0] = recno;
    try {
      boolean useStructure = isExtendedModel || (writer.getVersion() == NetcdfFileWriter.Version.netcdf3 && config.recDimensionLength < 0);
      super.writeStructureData(useStructure, record, origin, sdall);

      /* if (!isExtendedModel) {
        timeArray.set(0, timeCoordValue);
        parentArray.set(0, parentIndex);

        writer.write(time, origin, timeArray);
        writer.write(profileIndex, origin, parentArray);
      } */

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    recno++;
  }

}