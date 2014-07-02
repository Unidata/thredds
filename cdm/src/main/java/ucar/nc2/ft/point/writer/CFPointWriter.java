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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.ACDD;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;

/**
 * Write Point Feature Collections into netcdf3/4 files in CF 1.6 point obs conventions.
 * <ul>
 *   <li>netcdf3: use indexed ragged array representation</li>
 * </ul>
 *
 * @author caron
 * @since 4/11/12
 */
public class CFPointWriter implements AutoCloseable {
  private static boolean debug = false;

  public static int writeFeatureCollection(FeatureDatasetPoint fdpoint, String fileOut, NetcdfFileWriter.Version version) throws IOException {
    return writeFeatureCollection(fdpoint, fileOut, new CFPointWriterConfig(version));
  }

  /**
   * Write a FeatureDatasetPoint to a netcd3/4 file.
   *
   * @param fdpoint  the FeatureDatasetPoint; do first FeatureCollection contained within.
   * @param fileOut  write to the is file
   * @param config  configuration
   * @return  count of number of pointFeatures written.
   *
   * @throws IOException
   */
  public static int writeFeatureCollection(FeatureDatasetPoint fdpoint, String fileOut, CFPointWriterConfig config) throws IOException {

    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (fc instanceof PointFeatureCollection) || (fc instanceof NestedPointFeatureCollection) : fc.getClass().getName();

      if (fc instanceof PointFeatureCollection) {
        return writePointFeatureCollection(fdpoint, (PointFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        return writeStationFeatureCollection(fdpoint, (StationTimeSeriesFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof ProfileFeatureCollection) {
        return writeProfileFeatureCollection(fdpoint, (ProfileFeatureCollection) fc, fileOut, config);

      } /* else if (fc instanceof StationProfileFeatureCollection) {
        count = checkStationProfileFeatureCollection((StationProfileFeatureCollection) fc, show);
        if (showStructureData) showStructureData((StationProfileFeatureCollection) fc );

      } else if (fc instanceof SectionFeatureCollection) {
        count = checkSectionFeatureCollection((SectionFeatureCollection) fc, show);

      } else {
        count = checkNestedPointFeatureCollection((NestedPointFeatureCollection) fc, show);
      } */
    }

    return 0;
  }


  private static int writePointFeatureCollection(FeatureDatasetPoint fdpoint, PointFeatureCollection pfc, String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFPointCollection cfWriter = new WriterCFPointCollection(fileOut, fdpoint.getGlobalAttributes(), config)) {

      int count = 0;
      pfc.resetIteration();
      while(pfc.hasNext()) {
        PointFeature pf = pfc.next();
        if (count == 0)
          cfWriter.writeHeader(fdpoint.getDataVariables(), pf.getTimeUnit(), pf.getAltUnits());

        cfWriter.writeRecord(pf, pf.getData());
        count++;
        if (debug && count % 100 == 0) System.out.printf("%d ", count);
        if (debug && count % 1000 == 0) System.out.printf("%n ");
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeStationFeatureCollection(FeatureDatasetPoint fdpoint, StationTimeSeriesFeatureCollection fds, String fileOut,
                                                   CFPointWriterConfig config) throws IOException {

    WriterCFStationCollection cfWriter = new WriterCFStationCollection(fileOut, fdpoint.getGlobalAttributes(), config);
    ucar.nc2.ft.PointFeatureCollection pfc = fds.flatten(null, (CalendarDateRange) null); // LOOK

    int count = 0;
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      if (count == 0)
        cfWriter.writeHeader(fds.getStations(), fdpoint.getDataVariables(), pf.getTimeUnit(), pf.getAltUnits());

      StationPointFeature spf = (StationPointFeature) pf;
      cfWriter.writeRecord(spf.getStation(), pf, pf.getData());
      count++;
      if (debug && count % 100 == 0) System.out.printf("%d ", count);
      if (debug && count % 1000 == 0) System.out.printf("%n ");
    }

    cfWriter.finish();
    return count;
  }

  private static int writeProfileFeatureCollection(FeatureDatasetPoint fdpoint, ProfileFeatureCollection pds, String fileOut,
                                                   CFPointWriterConfig config) throws IOException {

    WriterCFProfileCollection cfWriter = new WriterCFProfileCollection(fileOut, fdpoint.getGlobalAttributes(), fdpoint.getDataVariables(), config);

    // LOOK not always needed
    int count = 0;
    int name_strlen = -1;
    int nprofiles = pds.size();
    if (nprofiles < 0) {
      pds.resetIteration();
      while (pds.hasNext()) {
        pds.next();
        name_strlen = Math.max(name_strlen, pds.getName().length());
        count++;
      }
      cfWriter.setHeaderInfo(count, name_strlen);
    }

    count = 0;
    pds.resetIteration();
    while (pds.hasNext()) {
      ucar.nc2.ft.ProfileFeature profile = pds.next();
      count += cfWriter.writeProfile(profile);
      if (debug && count % 10 == 0) System.out.printf("%d ", count);
      if (debug && count % 100 == 0) System.out.printf("%n ");
    }

    cfWriter.finish();
    return count;
  }

  /////////////////////////////////////////////////

  // attributes with these names will not be copied to the output file
  protected static final List<String> reservedGlobalAtts = Arrays.asList(
          CDM.CONVENTIONS, ACDD.LAT_MIN, ACDD.LAT_MAX, ACDD.LON_MIN, ACDD.LON_MAX, ACDD.TIME_START, ACDD.TIME_END,
            _Coordinate._CoordSysBuilder, CF.featureTypeAtt2, CF.featureTypeAtt3);

  protected static final List<String> reservedVariableAtts = Arrays.asList(
          CF.SAMPLE_DIMENSION, CF.INSTANCE_DIMENSION);

  protected static final String recordName = "obs";
  protected static final String recordDimName = "obs";
  protected static final String latName = "latitude";
  protected static final String lonName = "longitude";
  protected static final String altName = "altitude";
  protected static final String timeName = "time";

  /////////////////////////////////////////////////
  protected final CFPointWriterConfig config;
  protected NetcdfFileWriter writer;
  protected Map<String, Variable> dataVarMap = new HashMap<>(); // used for netcdf4 classic
  protected Structure record;  // used for netcdf3 and netcdf4 extended
  protected Dimension recordDim;

  protected String altUnits = null;
  protected LatLonRect llbb = null;
  protected CalendarDate minDate = null;
  protected CalendarDate maxDate = null;
  protected List<VariableSimpleIF> coordVars = new ArrayList<>();

  protected final boolean noTimeCoverage;
  protected final boolean noUnlimitedDimension;  // experimental , netcdf-3
  protected final boolean isExtendedModel;

  protected CFPointWriter(String fileOut, List<Attribute> atts, NetcdfFileWriter.Version version) throws IOException {
    this(fileOut, atts, new CFPointWriterConfig(version));
  }

  /**
   * Ctor
   * @param fileOut             name of the output file
   * @param atts                global attributes to be added
   * @param config              configure
   * @throws IOException
   */
  protected CFPointWriter(String fileOut, List<Attribute> atts, CFPointWriterConfig config) throws IOException {
    createWriter(fileOut, config);
    this.config = config;
    this.noTimeCoverage = config.noTimeCoverage;
    this.noUnlimitedDimension = (writer.getVersion() == NetcdfFileWriter.Version.netcdf3) && config.recDimensionLength >= 0;
    this.isExtendedModel = writer.getVersion().isExtendedModel();

    addGlobalAtts(atts);
    addNetcdf3UnknownAtts(noTimeCoverage);
  }

  private void createWriter(String fileOut, CFPointWriterConfig config) throws IOException {
    writer = NetcdfFileWriter.createNew(config.version, fileOut, config.chunking);
    writer.setFill(false);
  }

  private void addGlobalAtts(List<Attribute> atts) {
    writer.addGroupAttribute(null, new Attribute(CDM.CONVENTIONS, isExtendedModel ? CDM.CF_EXTENDED : "CF-1.6"));
    writer.addGroupAttribute(null, new Attribute(CDM.HISTORY, "Written by CFPointWriter"));
    for (Attribute att : atts) {
      if (!reservedGlobalAtts.contains(att.getShortName()))
        writer.addGroupAttribute(null, att);
    }
  }

  // netcdf3 has to add attributes up front, but we dont know values until the end.
  // so we have this updateAttribute hack; values set in finish()
  private void addNetcdf3UnknownAtts(boolean noTimeCoverage) {
    // dummy values, update in finish()
    if (!noTimeCoverage) {
      CalendarDate now = CalendarDate.of(new Date());
      writer.addGroupAttribute(null, new Attribute(ACDD.TIME_START, CalendarDateFormatter.toDateTimeStringISO(now)));
      writer.addGroupAttribute(null, new Attribute(ACDD.TIME_END, CalendarDateFormatter.toDateTimeStringISO(now)));
    }
    writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MIN, 0.0));
    writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MAX, 0.0));
    writer.addGroupAttribute(null, new Attribute(ACDD.LON_MIN, 0.0));
    writer.addGroupAttribute(null, new Attribute(ACDD.LON_MAX, 0.0));
  }

   // added as variables with the unlimited (record) dimension
  protected void addCoordinatesClassic(Dimension recordDim, List<VariableSimpleIF> coords) throws IOException {
    Map<String, Dimension> dimMap = addDimensionsClassic(coords);

    for (VariableSimpleIF vs : coords) {
      List<Dimension> dims = makeDimensionList(dimMap, vs.getDimensions());
      dims.add(0, recordDim);
      Variable mv = writer.addVariable(null, vs.getShortName(), vs.getDataType(), dims);
      for (Attribute att : vs.getAttributes())
        mv.addAttribute(att);
      dataVarMap.put(mv.getShortName(), mv);
      coordVars.add(vs);
    }

  }

  // added as members of the given structure
  protected void addCoordinatesExtended(Structure parent, List<VariableSimpleIF> coords) throws IOException {

    for (VariableSimpleIF vs : coords) {
      String dims = Dimension.makeDimensionsString(vs.getDimensions());
      Variable member = writer.addStructureMember(parent, vs.getShortName(), vs.getDataType(), dims);
      for (Attribute att : vs.getAttributes())
        member.addAttribute(att);
      coordVars.add(vs);
    }
    parent.calcElementSize();
  }

   // added as variables with the unlimited (record) dimension
  protected void addDataVariablesClassic(Dimension recordDim, List<? extends VariableSimpleIF> dataVars) throws IOException {
    Map<String, Dimension> dimMap = addDimensionsClassic(dataVars);

    // add the data variables all using the obs dimension
    for (VariableSimpleIF oldVar : dataVars) {
      if (writer.findVariable(oldVar.getShortName()) != null)
        continue;  // eliminate coordinate variables

      List<Dimension> dims = makeDimensionList(dimMap, oldVar.getDimensions());
      dims.add(0, recordDim);

      /*      // make dimension list
      List<Dimension> oldDims = oldVar.getDimensions();
      StringBuilder dimNames = new StringBuilder(recordDimName);
      for (Dimension d : oldDims) {
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getShortName());
      }
      String dimString = dimNames.toString();  */

      Variable newVar;
       if (oldVar.getDataType().equals(DataType.STRING)  && !writer.getVersion().isExtendedModel()) {
         // Group parent = writer.getNetcdfFile().getRootGroup();
         newVar = writer.addStringVariable(null, (Variable) oldVar, dims); // LOOK can we cast to Variable ? LOOK reading oldVar for strlen
       } else {
         newVar = writer.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), dims);
       }

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts) {
        if (!reservedVariableAtts.contains(att.getShortName()))
          newVar.addAttribute(att);
      }

      Formatter coordName = new Formatter();
      for (VariableSimpleIF coord : coordVars)
        coordName.format("%s ", coord.getFullName());
      newVar.addAttribute( new Attribute(CF.COORDINATES, coordName.toString()));

      dataVarMap.put(newVar.getShortName(), newVar);
    }

  }

  // classic model: no private dimensions
  private int fakeDims = 0;
  protected Map<String, Dimension> addDimensionsClassic(List<? extends VariableSimpleIF> vars) throws IOException {
    Set<Dimension> oldDims = new HashSet<>(20);
    Map<String, Dimension> newDims = new HashMap<>(20);

    // find all dimensions needed by these variables
    for (VariableSimpleIF var : vars) {
      List<Dimension> dims = var.getDimensions();
      oldDims.addAll(dims);
    }

    // add them
    for (Dimension d : oldDims) {
      String dimName = (d.getShortName() == null) ? "fake"+fakeDims++ : d.getShortName();
      Dimension newDim = writer.addDimension(null, dimName, d.getLength(), true, false, d.isVariableLength());
      newDims.put(d.getShortName(), newDim);
    }

    return newDims;
  }

  protected List<Dimension> makeDimensionList(Map<String, Dimension> dimMap, List<Dimension> oldDims) throws IOException {
    List<Dimension> result = new ArrayList<>();

    // find all dimensions needed by the coord variables
    for (Dimension dim : oldDims) {
      result.add( dimMap.get(dim.getShortName()));
    }

    return result;
  }

  // add variables to the record structure
  protected void addVariablesExtended(List<? extends VariableSimpleIF> dataVars) throws IOException {

    for (VariableSimpleIF oldVar : dataVars) {
      // skip duplicates
      if (record.findVariable(oldVar.getShortName()) != null) continue;

      // make dimension list
      StringBuilder dimNames = new StringBuilder();
      for (Dimension d : oldVar.getDimensions()) {
        if (d.isUnlimited()) continue;
        if (d.getShortName() == null || !d.getShortName().equals(recordDimName))
          dimNames.append(" ").append(d.getLength());  // anonymous
      }

      Variable m = writer.addStructureMember(record, oldVar.getShortName(), oldVar.getDataType(), dimNames.toString());

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts) {
        String attName = att.getShortName();
        if (!reservedVariableAtts.contains(attName) && !attName.startsWith("_Coordinate"))
          m.addAttribute(att);
      }

      String coordNames = timeName + " " + latName +" "+ lonName;
      if (altUnits != null)
        coordNames = coordNames +" " + altName;
      m.addAttribute(new Attribute(CF.COORDINATES, coordNames));
    }

  }

  // keep track of the bounding box
  protected void trackBB(LatLonPoint loc, CalendarDate obsDate) {
    if (loc != null) {
      if (llbb == null) {
        llbb = new LatLonRect(loc, .001, .001);
        return;
      }
      llbb.extend(loc);
    }

    // date is handled specially
    if ((minDate == null) || minDate.isAfter(obsDate)) minDate = obsDate;
    if ((maxDate == null) || maxDate.isBefore(obsDate)) maxDate = obsDate;
  }

  public void finish() throws IOException {
    if (llbb != null) {
      writer.updateAttribute(null, new Attribute(ACDD.LAT_MIN, llbb.getLowerLeftPoint().getLatitude()));
      writer.updateAttribute(null, new Attribute(ACDD.LAT_MAX, llbb.getUpperRightPoint().getLatitude()));
      writer.updateAttribute(null, new Attribute(ACDD.LON_MIN, llbb.getLowerLeftPoint().getLongitude()));
      writer.updateAttribute(null, new Attribute(ACDD.LON_MAX, llbb.getUpperRightPoint().getLongitude()));
    }

    if (!noTimeCoverage) {
      if (minDate == null) minDate = CalendarDate.present();
      if (maxDate == null) maxDate = CalendarDate.present();
      writer.updateAttribute(null, new Attribute(ACDD.TIME_START, CalendarDateFormatter.toDateTimeStringISO(minDate)));
      writer.updateAttribute(null, new Attribute(ACDD.TIME_END, CalendarDateFormatter.toDateTimeStringISO(maxDate)));
    }

    writer.close();
  }

  protected int writeStructureData(boolean useStructure, Structure s, int[] origin, StructureData sdata) throws IOException, InvalidRangeException {
    if (useStructure) {
      if (s.isUnlimited())
        return writer.appendStructureData(s, sdata);  // can write it all at once along unlimited dimension
      else {
        ArrayStructureW as = new ArrayStructureW(sdata.getStructureMembers(), new int[] {1});
        as.setStructureData(sdata, 0);
        writer.write(s, origin, as);  // can write it all at once along unlimited dimension
        return origin[0];
      }
    } else  {
      for (StructureMembers.Member m : sdata.getMembers()) {  // netcdf4 classic model
        Array org = sdata.getArray(m);
        Array orgPlus1 = Array.makeArrayRankPlusOne(org);  // add dimension on the left (slow)
        int[] useOrigin = origin;

        if (org.getRank() > 0) {                          // if rank 0 (common case, this is a nop, so skip
          useOrigin = new int[org.getRank()+1];
          useOrigin[0] = origin[0]; // the rest are 0
        }

        Variable mv = dataVarMap.get(m.getName());
        if (mv == null)
          continue;     // LOOK
        writer.write(mv, useOrigin, orgPlus1);
      }
      return origin[0];
    }

  }


  @Override
  public void close() throws IOException {
    writer.close();
  }
}
