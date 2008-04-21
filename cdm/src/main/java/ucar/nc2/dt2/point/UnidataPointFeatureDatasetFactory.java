/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.dt2.point;

import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.units.DateRange;
import ucar.nc2.dt2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.ma2.StructureData;

import java.util.*;
import java.io.IOException;

import ucar.nc2.constants.FeatureType;

/**
 * This handles point datasets in "Unidata Observation Dataset v1.0"
 *
 * @author caron
 * @since Feb 29, 2008
 */

public class UnidataPointFeatureDatasetFactory implements FeatureDatasetFactory {

  // FeatureDatasetFactory
  public boolean isMine(FeatureType ftype, NetcdfDataset ds) {
    // find datatype
    String datatype = ds.findAttValueIgnoreCase(null, "cdm_datatype", null);
    if (datatype == null)
      datatype = ds.findAttValueIgnoreCase(null, "cdm_data_type", null);
    if (datatype == null)
      return false;
    if (!datatype.equalsIgnoreCase(FeatureType.POINT.toString()) && !datatype.equalsIgnoreCase(FeatureType.STATION.toString()))
      return false;
    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equalsIgnoreCase("Unidata Observation Dataset v1.0"))
        return true;
    }

    return false;
  }

  // since isMine can be expensive, make copy instead of reanalyze
  public FeatureDatasetFactory copy() {
    return new UnidataPointFeatureDatasetFactory();
  }

  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    return new UnidataPointFeatureDataset(ncd, errlog);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private static class UnidataPointFeatureDataset extends PointDatasetImpl {
    private RecordDatasetHelper recordHelper;

    private UnidataPointFeatureDataset(NetcdfDataset ds, StringBuffer errlog) throws IOException {
      super(ds, FeatureType.POINT);
      parseInfo.append(" PointFeatureDatasetImpl=").append(getClass().getName()).append("\n");
 
      // coordinate variables
      Variable latVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Lat);
      Variable lonVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Lon);
      Variable timeVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Time);

      if (latVar == null)
        throw new IllegalStateException("Missing latitude variable");
      if (lonVar == null)
        throw new IllegalStateException("Missing longitude coordinate variable");
      if (timeVar == null)
        throw new IllegalStateException("Missing time coordinate variable");

      Variable altVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Height);
      Variable timeNominalVar = UnidataPointDatasetHelper.findVariable(ds, "record.time_nominal");
      String recDimName = ds.findAttValueIgnoreCase(null, "observationDimension", null);

      // fire up the record helper
      recordHelper = new RecordDatasetHelper(ds, timeVar.getName(), timeNominalVar == null ? null : timeNominalVar.getName(),
          dataVariables, recDimName, parseInfo);
      recordHelper.setLocationInfo(latVar.getName(), lonVar.getName(), altVar == null ? null : altVar.getName());
      recordHelper.setShortNames(latVar.getShortName(), lonVar.getShortName(), altVar == null ? null : altVar.getShortName(),
          timeVar.getShortName(), timeNominalVar == null ? null : timeNominalVar.getShortName());

      removeDataVariable(timeVar.getName());
      if (timeNominalVar != null)
        removeDataVariable(timeNominalVar.getName());
      removeDataVariable(latVar.getName());
      removeDataVariable(lonVar.getName());
      if (altVar != null)
        removeDataVariable(altVar.getName());

      Date startDate = UnidataPointDatasetHelper.getStartDate(ds, recordHelper.timeUnit);
      Date endDate = UnidataPointDatasetHelper.getEndDate(ds, recordHelper.timeUnit);
      setDateRange(new DateRange(startDate, endDate));
      setBoundingBox(UnidataPointDatasetHelper.getBoundingBox(ds));

      title = ds.findAttValueIgnoreCase(null, "title", null);
      desc = ds.findAttValueIgnoreCase(null, "description", null);

     setPointFeatureCollection(new PointCollectionImpl("UnidataPointFeatureDataset") {
        public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
          return  new MyPointFeatureIterator(recordHelper.recordVar, -1, null);
        }
      });
    }

    private class MyPointFeatureIterator extends PointIteratorImpl {

      protected PointFeature makeFeature(int recnum, StructureData sdata) {
        return recordHelper.new RecordPointObs(sdata, recnum);
      }

      MyPointFeatureIterator(Structure struct, int bufferSize, PointFeatureIterator.Filter filter) throws IOException {
        super(struct.getStructureIterator(), filter, false);
        setBufferSize( bufferSize);
      }

    }
  }

  public static void main(String args[]) throws IOException {
    //String filename = "C:/data/199707010200.CHRTOUT_DOMAIN2";
    String filename = "C:/data/metars/Surface_METAR_20070331_0000.nc";
    UnidataPointFeatureDataset upod = new UnidataPointFeatureDataset(NetcdfDataset.openDataset(filename), new StringBuffer());
    System.out.println("\n\n");
    upod.getDetailInfo( new Formatter( System.out));
  }


}

