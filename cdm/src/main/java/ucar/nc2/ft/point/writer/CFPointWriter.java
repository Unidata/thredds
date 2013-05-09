package ucar.nc2.ft.point.writer;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;

/**
 * Write netcdf3 files in CF 1.6 point obs conventions
 *
 * @author caron
 * @since 4/11/12
 */
public class CFPointWriter {
  private static boolean debug = false;

  public static int writeFeatureCollection(FeatureDatasetPoint fdpoint, String fileOut, NetcdfFileWriter.Version version) throws IOException {
    int count = 0;
    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (fc instanceof PointFeatureCollection) || (fc instanceof NestedPointFeatureCollection) : fc.getClass().getName();

      if (fc instanceof PointFeatureCollection) {
        return writePointFeatureCollection(fdpoint, (PointFeatureCollection) fc, fileOut, version);

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        return writeStationFeatureCollection(fdpoint, (StationTimeSeriesFeatureCollection) fc, fileOut, version);

      } else if (fc instanceof ProfileFeatureCollection) {
        return writeProfileFeatureCollection(fdpoint, (ProfileFeatureCollection) fc, fileOut, version);

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

  private static int writePointFeatureCollection(FeatureDatasetPoint fdpoint, PointFeatureCollection pfc, String fileOut,
                                                 NetcdfFileWriter.Version version) throws IOException {
    if (debug) System.out.printf("write to file %s%n ", fileOut);

    WriterCFPointCollection writer = new WriterCFPointCollection(version, fileOut, fdpoint.getGlobalAttributes());

    int count = 0;
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      if (count == 0) {
        writer.writeHeader(fdpoint.getDataVariables(), pf.getTimeUnit(), null);
      }
      writer.writeRecord(pf, pf.getData());
      count++;
      if (debug && count % 100 == 0) System.out.printf("%d ", count);
      if (debug && count % 1000 == 0) System.out.printf("%n ");
    }

    writer.finish();
    return count;
  }

  private static int writeStationFeatureCollection(FeatureDatasetPoint fdpoint, StationTimeSeriesFeatureCollection fds,
                                                   String fileOut, NetcdfFileWriter.Version version) throws IOException {
    if (debug) System.out.printf("write to file %s%n ", fileOut);

    WriterCFStationCollection writer = new WriterCFStationCollection(version, fileOut, fdpoint.getGlobalAttributes());

    ucar.nc2.ft.PointFeatureCollection pfc = fds.flatten(null, (CalendarDateRange) null);

    int count = 0;
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      if (count == 0) {
        writer.writeHeader(fds.getStations(), fdpoint.getDataVariables(), pf.getTimeUnit(), "");
      }
      StationPointFeature spf = (StationPointFeature) pf;
      writer.writeRecord(spf.getStation(), pf, pf.getData());
      count++;
      if (debug && count % 100 == 0) System.out.printf("%d ", count);
      if (debug && count % 1000 == 0) System.out.printf("%n ");
    }

    writer.finish();

    return count;
  }

  private static int writeProfileFeatureCollection(FeatureDatasetPoint fdpoint, ProfileFeatureCollection pds,
                                                   String fileOut, NetcdfFileWriter.Version version) throws IOException {
    if (debug) System.out.printf("write to file %s%n ", fileOut);

    WriterCFProfileCollection writer = new WriterCFProfileCollection(fileOut, fdpoint.getGlobalAttributes(), version);

    int count = 0;

    List<String> profiles = new ArrayList<String>();
    pds.resetIteration();
    while (pds.hasNext()) {
      profiles.add(pds.next().getName());
    }

    pds.resetIteration();
    while (pds.hasNext()) {
      ucar.nc2.ft.ProfileFeature profile = pds.next();

      profile.resetIteration();
      while (profile.hasNext()) {
        ucar.nc2.ft.PointFeature pf = profile.next();
        if (count == 0) {
          writer.writeHeader(profiles, fdpoint.getDataVariables(), pf.getTimeUnit(), null); // LOOK altitude units ??
        }

        writer.writeRecord(profile.getName(), pf, pf.getData());

        count++;
        if (debug && count % 100 == 0) System.out.printf("%d ", count);
        if (debug && count % 1000 == 0) System.out.printf("%n ");
      }
    }

    writer.finish();

    return count;
  }

  /////////////////////////////////////////////////
  private static final String[] reservedAtts = new String[]{CDM.CONVENTIONS,
          CDM.LAT_MIN, CDM.LAT_MAX, CDM.LON_MIN, CDM.LON_MAX, CDM.TIME_START, CDM.TIME_END,
          _Coordinate._CoordSysBuilder, CF.featureTypeAtt2, CF.featureTypeAtt3};

  private static final List<String> reservedAttsList = Arrays.asList(reservedAtts);

  protected static final String recordDimName = "obs";
  protected static final String latName = "latitude";
  protected static final String lonName = "longitude";
  protected static final String altName = "altitude";
  protected static final String timeName = "time";

  protected NetcdfFileWriter writer;
  protected String altUnits = null;
  protected LatLonRect llbb = null;

  protected Set<Dimension> dimSet = new HashSet<Dimension>(20);
  
  protected CalendarDate minDate = null;
  protected CalendarDate maxDate = null;

  public CFPointWriter(String fileOut, List<Attribute> atts, NetcdfFileWriter.Version version) throws IOException {
    createWriter(fileOut, version);
    addAtts(atts);
    addDefaultAtts(true);
  }
  /**
   * 
   * @param fileOut name of the output file
   * @param atts attributes to be added
   * @param version netcdf file version
   * @param addTimeCoverage for files that don't have time dimension indicates we won't have time coverage attributes either
   * @throws IOException
   */
  public CFPointWriter(String fileOut, List<Attribute> atts, NetcdfFileWriter.Version version, boolean addTimeCoverage) throws IOException {
	    createWriter(fileOut, version);
	    addAtts(atts);
	    addDefaultAtts(addTimeCoverage);
  }  
  

  private void createWriter(String fileOut, NetcdfFileWriter.Version version) throws IOException {
    writer = NetcdfFileWriter.createNew(version, fileOut, null);
    writer.setFill(false);
  }
  
 

  private void addAtts(List<Attribute> atts) {

    if (writer == null) throw new IllegalStateException("NetcdfFileWriter must be created");

    writer.addGroupAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.6"));
    writer.addGroupAttribute(null, new Attribute(CDM.HISTORY, "Written by CFPointWriter"));
    for (Attribute att : atts) {
      if (!reservedAttsList.contains(att.getShortName()))
        writer.addGroupAttribute(null, att);
    }

  }

  private void addDefaultAtts(boolean addTimeCoverage) {

    if (writer == null) throw new IllegalStateException("NetcdfFileWriter must be created");
    // dummys, update in finish()
    if(addTimeCoverage){
    	CalendarDate now = CalendarDate.of(new Date());
    	writer.addGroupAttribute(null, new Attribute(CDM.TIME_START, CalendarDateFormatter.toDateTimeStringISO(now) ));
    	writer.addGroupAttribute(null, new Attribute(CDM.TIME_END,   CalendarDateFormatter.toDateTimeStringISO(now) ));
    }	
    writer.addGroupAttribute(null, new Attribute(CDM.LAT_MIN, 0.0));
    writer.addGroupAttribute(null, new Attribute(CDM.LAT_MAX, 0.0));
    writer.addGroupAttribute(null, new Attribute(CDM.LON_MIN, 0.0));
    writer.addGroupAttribute(null, new Attribute(CDM.LON_MAX, 0.0));
  }

  public void setLength(long size) {
    writer.setLength(size);
  }

  protected void trackBB(EarthLocation loc, CalendarDate obsDate) {
    if (loc != null) {
      if (llbb == null) {
        llbb = new LatLonRect(loc.getLatLon(), .001, .001);
        return;
      }
      llbb.extend(loc.getLatLon());
    }

    // date is handled specially
    if ((minDate == null) || minDate.isAfter(obsDate)) minDate = obsDate;
    if ((maxDate == null) || maxDate.isBefore(obsDate)) maxDate = obsDate;
  }

  public void finish() throws IOException {
    writer.updateAttribute(null, new Attribute(CDM.LAT_MIN, llbb.getLowerLeftPoint().getLatitude()));
    writer.updateAttribute(null, new Attribute(CDM.LAT_MAX, llbb.getUpperRightPoint().getLatitude()));
    writer.updateAttribute(null, new Attribute(CDM.LON_MIN, llbb.getLowerLeftPoint().getLongitude()));
    writer.updateAttribute(null, new Attribute(CDM.LON_MAX, llbb.getUpperRightPoint().getLongitude()));

 
    if((writer.getNetcdfFile().findAttribute("@"+CDM.TIME_START) != null) &&  (writer.getNetcdfFile().findAttribute("@"+CDM.TIME_END) != null )){
    	
    	if (minDate == null) minDate = CalendarDate.present();
    	if (maxDate == null) maxDate = CalendarDate.present();

    	writer.updateAttribute(null, new Attribute(CDM.TIME_START, CalendarDateFormatter.toDateTimeStringISO(minDate)));
    	writer.updateAttribute(null, new Attribute(CDM.TIME_END, CalendarDateFormatter.toDateTimeStringISO(maxDate)));
    }
    
    writer.close();
  }


}
