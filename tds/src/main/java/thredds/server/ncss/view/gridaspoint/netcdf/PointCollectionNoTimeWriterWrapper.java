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

package thredds.server.ncss.view.gridaspoint.netcdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thredds.server.ncss.dataservice.StructureDataFactory;
import thredds.server.ncss.util.NcssRequestUtils;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.ft.point.writer.CFPointWriterConfig;
import ucar.nc2.ft.point.writer.WriterCFPointCollection;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.StationImpl;

class PointCollectionNoTimeWriterWrapper implements CFPointWriterWrapper {

  static private Logger log = LoggerFactory.getLogger(PointCollectionNoTimeWriterWrapper.class);

  NetcdfFileWriter.Version version;
  String filePath;
  List<Attribute> atts;
  private WriterCFPointCollection writer;

  private GridAsPointDataset gap;

  private PointCollectionNoTimeWriterWrapper(NetcdfFileWriter.Version version, String filePath, List<Attribute> atts) throws IOException {
    this.version = version;
    this.filePath = filePath;
    this.atts = atts;

  }

  @Override
  public boolean header(Map<String, List<String>> groupedVars,
                        GridDataset gds, List<CalendarDate> wDates, List<Attribute> timeDimAtts,
                        LatLonPoint point, Double vertCoord) {

    CFPointWriterConfig config = new CFPointWriterConfig(version);
    config.noTimeCoverage = true;

    boolean headerDone = false;
    List<Attribute> atts = new ArrayList<>();
    atts.add(new Attribute(CDM.TITLE, "Extract point data from Grid file " + gds.getLocation()));

    NetcdfDataset ncfile = (NetcdfDataset) gds.getNetcdfFile(); // fake-arino
    List<String> vars = (new ArrayList<>(groupedVars.values())).get(0);
    gap = NcssRequestUtils.buildGridAsPointDataset(gds, vars);
    List<VariableSimpleIF> wantedVars = NcssRequestUtils.wantedVars2VariableSimple(vars, gds, ncfile);
    CoordinateAxis1D zAxis = gds.findGridDatatype(vars.get(0)).getCoordinateSystem().getVerticalAxis();
    String zAxisUnitString = null;
    if (zAxis != null) {
      zAxisUnitString = zAxis.getUnitsString();
    }

    //Create the list of stations (only one)
    String stnName = "Grid Point";
    String desc = "Grid Point at lat/lon=" + point.getLatitude() + "," + point.getLongitude();
    Station s = new StationImpl(stnName, desc, "", point.getLatitude(), point.getLongitude(), Double.NaN);
    List<Station> stnList = new ArrayList<>();
    stnList.add(s);

    /*  LoOK WTF ?
    try {

      // LOOK fake
      this.writer = new WriterCFPointCollection(filePath, atts, wantedVars, null, null, null, config);
      writer.writeHeader(null);
      headerDone = true;

    } catch (IOException ioe) {
      log.error("Error writing header", ioe);
    }  */

    return headerDone;


  }

  @Override
  public boolean write(Map<String, List<String>> groupedVars,
                       GridDataset gridDataset, CalendarDate date, LatLonPoint point,
                       Double targetLevel) {

    boolean allDone = false;

    List<String> vars = (new ArrayList<>(groupedVars.values())).get(0);
    //Create the structure with no time!!
    StructureData sdata = StructureDataFactory.getFactory().createSingleStructureData(gridDataset, point, vars, false);

    EarthLocation earthLocation = null;
    // Iterating vars
    Iterator<String> itVars = vars.iterator();
    int cont = 0;
    try {
      while (itVars.hasNext()) {
        String varName = itVars.next();
        GridDatatype grid = gridDataset.findGridDatatype(varName);

        //if (gap.hasTime(grid, date) ) {
        GridAsPointDataset.Point p = gap.readData(grid, null, point.getLatitude(), point.getLongitude());
        //sdata.findMember("latitude").getDataArray().setDouble(0, p.lat );
        //sdata.findMember("longitude").getDataArray().setDouble(0, p.lon );
        sdata.findMember(varName).getDataArray().setDouble(0, p.dataValue);
        earthLocation = new EarthLocationImpl(p.lat, p.lon, Double.NaN);

        //}else{ //Set missing value
        //sdata.findMember("latitude").getDataArray().setDouble(0, point.getLatitude() );
        //sdata.findMember("longitude").getDataArray().setDouble(0, point.getLongitude() );
        //	sdata.findMember(varName).getDataArray().setDouble(0, gap.getMissingValue(grid) );

        //}
        cont++;
      }

      // LOOK fake
      //sobsWriter.writeRecord( (String)sdata.findMember("station").getDataArray().getObject(0), date.toDate() , sdata);
      writer.writeRecord(null, sdata);
      allDone = true;

    } catch (IOException ioe) {
      log.error("Error writing data", ioe);
    }


    return allDone;

  }

  @Override
  public boolean trailer() {

    boolean finished = false;
    try {
      writer.finish();
      finished = true;

    } catch (IOException ioe) {
      log.error("Error finishing  WriterCFPointCollection" + ioe);
    }

    return finished;
  }


  static PointCollectionNoTimeWriterWrapper createWrapper(NetcdfFileWriter.Version version, String filePath, List<Attribute> atts) throws IOException {

    return new PointCollectionNoTimeWriterWrapper(version, filePath, atts);

  }


}
