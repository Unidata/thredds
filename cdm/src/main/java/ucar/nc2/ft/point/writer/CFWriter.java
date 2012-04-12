package ucar.nc2.ft.point.writer;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonRect;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 4/11/12
 */
public class CFWriter {
  private static boolean debug = false;

  public static int writeFeatureCollection(FeatureDatasetPoint fdpoint, String fileOut) throws IOException {
    int count = 0;
    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (fc instanceof PointFeatureCollection) || (fc instanceof NestedPointFeatureCollection) : fc.getClass().getName();

      if (fc instanceof PointFeatureCollection) {
        return writePointFeatureCollection(fdpoint, (PointFeatureCollection) fc, fileOut);

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        return writeStationFeatureCollection(fdpoint, (StationTimeSeriesFeatureCollection) fc, fileOut);

      } else if (fc instanceof ProfileFeatureCollection) {
        return writeProfileFeatureCollection(fdpoint, (ProfileFeatureCollection) fc, fileOut);

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

  private static int writePointFeatureCollection(FeatureDatasetPoint fdpoint, PointFeatureCollection pfc, String fileOut) throws IOException {
    if (debug) System.out.printf("write to file %s%n ", fileOut);

    WriterCFPointCollection writer = new WriterCFPointCollection(fileOut, "Rewrite as CF: original = " + fileOut);

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

  private static int writeStationFeatureCollection(FeatureDatasetPoint fdpoint, StationTimeSeriesFeatureCollection fds, String fileOut) throws IOException {
    if (debug) System.out.printf("write to file %s%n ", fileOut);

    WriterCFStationCollection writer = new WriterCFStationCollection(fileOut, "Rewrite as CF: original = " + fileOut);

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

  private static int writeProfileFeatureCollection(FeatureDatasetPoint fdpoint, ProfileFeatureCollection pds, String fileOut) throws IOException {
    if (debug) System.out.printf("write to file %s%n ", fileOut);

    WriterCFProfileCollection writer = new WriterCFProfileCollection(fileOut, "Rewrite as CF: original = " + fileOut);

    int count = 0;
    
    List<String> profiles = new ArrayList<String>();
    pds.resetIteration();
    while (pds.hasNext()) {
      profiles.add( pds.next().getName());
    }
    
    pds.resetIteration();
    while (pds.hasNext()) {
      ucar.nc2.ft.ProfileFeature profile = pds.next();

      profile.resetIteration();
      while (profile.hasNext()) {
        ucar.nc2.ft.PointFeature pf = profile.next();
        if (count == 0) {
           writer.writeHeader(profiles, fdpoint.getDataVariables(), pf.getTimeUnit(), null);
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

  protected static final String recordDimName = "record";
  protected static final String latName = "latitude";
  protected static final String lonName = "longitude";
  protected static final String altName = "altitude";
  protected static final String timeName = "time";

  protected NetcdfFileWriteable ncfile;
  protected String altUnits;
  protected LatLonRect llbb = null;

  protected Set<Dimension> dimSet = new HashSet<Dimension>(20);
  protected CalendarDate minDate = null;
  protected CalendarDate maxDate = null;

  protected CFWriter(String fileOut, String title) throws IOException {
    ncfile = NetcdfFileWriteable.createNew(fileOut, false);
    ncfile.setFill( false);

    ncfile.addGlobalAttribute(CDM.CONVENTIONS, "CF-1.6");
    ncfile.addGlobalAttribute(CDM.TITLE, title);
    ncfile.addGlobalAttribute(CDM.HISTORY, "Written by CFWriter");

    // dummys, update in finish()
    ncfile.addGlobalAttribute("time_coverage_start", CalendarDateFormatter.toDateStringPresent());
    ncfile.addGlobalAttribute("time_coverage_end",  CalendarDateFormatter.toDateStringPresent());
    ncfile.addGlobalAttribute("geospatial_lat_min", 0.0);
    ncfile.addGlobalAttribute("geospatial_lat_max", 0.0);
    ncfile.addGlobalAttribute("geospatial_lon_min", 0.0);
    ncfile.addGlobalAttribute("geospatial_lon_max", 0.0);
  }

  public void setLength(long size) {
    ncfile.setLength( size);
  }

  protected void trackBB(EarthLocation loc) {
    if (llbb == null) {
      llbb = new LatLonRect(loc.getLatLon(), .001, .001);
      return;
    }
    llbb.extend(loc.getLatLon());
  }

  public void finish() throws IOException {
    ncfile.updateAttribute(null, new Attribute("geospatial_lat_min", llbb.getLowerLeftPoint().getLatitude()));
    ncfile.updateAttribute(null, new Attribute("geospatial_lat_max", llbb.getUpperRightPoint().getLatitude()));
    ncfile.updateAttribute(null, new Attribute("geospatial_lon_min", llbb.getLowerLeftPoint().getLongitude()));
    ncfile.updateAttribute(null, new Attribute("geospatial_lon_max", llbb.getUpperRightPoint().getLongitude()));

    // if there is no data
    if (minDate == null) minDate = CalendarDate.present();
    if (maxDate == null) maxDate = CalendarDate.present();

    ncfile.updateAttribute(null, new Attribute("time_coverage_start",  CalendarDateFormatter.toDateTimeString(minDate)));
    ncfile.updateAttribute(null, new Attribute("time_coverage_end",  CalendarDateFormatter.toDateTimeString(maxDate)));

    ncfile.close();
  }


}
