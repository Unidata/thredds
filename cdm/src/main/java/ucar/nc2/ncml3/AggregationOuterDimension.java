/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.ncml3;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dataset.NetcdfDatasetFactory;
import ucar.nc2.util.CancelTask;
import ucar.nc2.*;
import ucar.ma2.*;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Date;

/**
 * Superclass for Aggregations on the outer dimension: joinNew, joinExisting, Fmrc
 * @author caron
 * @since Aug 10, 2007
 */
public abstract class AggregationOuterDimension extends Aggregation {

  protected AggregationOuterDimension(NetcdfDataset ncd, String dimName, Type type, String recheckS) {
    super(ncd, dimName, type, recheckS);
  }

  private int totalCoords = 0;  // the aggregation dimension size
  
  protected void buildCoords(CancelTask cancelTask) throws IOException {
    List<Dataset> nestedDatasets = getDatasets();

    if (type == Type.FORECAST_MODEL_COLLECTION) {
      for (Dataset nested : nestedDatasets) {
        nested.ncoord = 1;
      }
    }

    totalCoords = 0;
    for (Dataset nested : nestedDatasets) {
      totalCoords += nested.setStartEnd(totalCoords, cancelTask);
    }
  }

  protected int getTotalCoords() {
    return totalCoords;
  }

  /**
   * Read an aggregation variable: A variable whose data spans multiple files.
   *
   * @param mainv      the aggregation variable
   * @param cancelTask allow the user to cancel
   * @return the data array
   * @throws IOException
   */
  public Array read(Variable mainv, CancelTask cancelTask) throws IOException {

    // the case of the agg coordinate var LOOK all but AggregationFmrcSingle ??
    if (((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION)) && mainv.getShortName().equals(dimName))
      return readAggCoord(mainv, cancelTask);

    // read the original type - if its been promoted to a new type, the conversion happens after this read
    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array allData = Array.factory(dtype, mainv.getShape());
    int destPos = 0;

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      Array varData = vnested.read(mainv, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      // arraycopy( Array arraySrc, int srcPos, Array arrayDst, int dstPos, int len)
      Array.arraycopy(varData, 0, allData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return allData;
  }

  protected Array readAggCoord(Variable aggCoord, CancelTask cancelTask) throws IOException {
    DataType dtype = aggCoord.getDataType();
    Array allData = Array.factory(dtype, aggCoord.getShape());
    IndexIterator result = allData.getIndexIterator();

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {

      try {
        readAggCoord(aggCoord, cancelTask, vnested, dtype, result, null, null, null);
      } catch (InvalidRangeException e) {
        e.printStackTrace();  // cant happen
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    // cache it so we dont have to come here again; make sure we invalidate cache when data changes!
    aggCoord.setCachedData(allData, false);

    return allData;
  }

  /**
   * Read a section of an aggregation variable.
   *
   * @param mainv      the aggregation variable
   * @param cancelTask allow the user to cancel
   * @param section    read just this section of the data, array of Range
   * @return the data array section
   * @throws IOException
   */
  public Array read(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    // If its full sized, then use full read, so that data gets cached.
    long size = section.computeSize();
    if (size == mainv.getSize())
      return read(mainv, cancelTask);

    // the case of the agg coordinate var LOOK all but AggregationFmrcSingle ??
    if (((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION)) && mainv.getShortName().equals(dimName))
      return readAggCoord(mainv, section, cancelTask);

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array sectionData = Array.factory(dtype, section.getShape());
    int destPos = 0;

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> nestedSection = new ArrayList<Range>(ranges); // get copy
    List<Range> innerSection = ranges.subList(1, ranges.size());

    if (debug) System.out.println("   agg wants range=" + mainv.getName() + "(" + joinRange + ")");

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset nested : nestedDatasets) {
      Range nestedJoinRange = nested.getNestedJoinRange(joinRange);
      if (nestedJoinRange == null)
        continue;
      //if (debug)
      //  System.out.println("   agg use " + nested.aggStart + ":" + nested.aggEnd + " range= " + nestedJoinRange + " file " + nested.getLocation());

      Array varData;
      if ((type == Type.JOIN_NEW) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        varData = nested.read(mainv, cancelTask, innerSection);
      } else {
        nestedSection.set(0, nestedJoinRange);
        varData = nested.read(mainv, cancelTask, nestedSection);
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      Array.arraycopy(varData, 0, sectionData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return sectionData;
  }

  protected Array readAggCoord(Variable aggCoord, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    DataType dtype = aggCoord.getDataType();
    Array allData = Array.factory(dtype, section.getShape());
    IndexIterator result = allData.getIndexIterator();

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> nestedSection = new ArrayList<Range>(ranges); // get copy
    List<Range> innerSection = ranges.subList(1, ranges.size());

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      Range nestedJoinRange = vnested.getNestedJoinRange(joinRange);
      if (nestedJoinRange == null)
        continue;
      //if (debug)
      //  System.out.println("   agg use " + vnested.aggStart + ":" + vnested.aggEnd + " range= " + nestedJoinRange + " file " + vnested.getLocation());

      readAggCoord(aggCoord, cancelTask, vnested, dtype, result, nestedJoinRange, nestedSection, innerSection);

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    return allData;
  }

  // handle the case of cached agg coordinate variables
  private void readAggCoord(Variable aggCoord, CancelTask cancelTask, Dataset vnested, DataType dtype, IndexIterator result,
          Range nestedJoinRange, List<Range> nestedSection, List<Range> innerSection) throws IOException, InvalidRangeException {

    // we have the coordinates as a String
    if (vnested.coordValue != null) {

      // joinNew, fmrc only can have 1 coord
      if ((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        if (dtype == DataType.STRING) {
          result.setObjectNext(vnested.coordValue);
        } else {
          double val = Double.parseDouble(vnested.coordValue);
          result.setDoubleNext(val);
        }

      } else {

        // joinExisting can have multiple coords
        int count = 0;
        StringTokenizer stoker = new StringTokenizer(vnested.coordValue, " ,");
        while (stoker.hasMoreTokens()) {
          String toke = stoker.nextToken();
          if ((nestedJoinRange != null) && !nestedJoinRange.contains(count))
            continue;

          if (dtype == DataType.STRING) {
            result.setObjectNext(toke);
          } else {
            double val = Double.parseDouble(toke);
            result.setDoubleNext(val);
          }
          count++;
        }

        if (count != vnested.ncoord)
          logger.error("readAggCoord incorrect number of coordinates dataset=" + vnested.getLocation());
      }

    } else { // we gotta read it

      Array varData;
      if (nestedJoinRange == null) {  // all data
        varData = vnested.read(aggCoord, cancelTask);

      } else
      if ((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        varData = vnested.read(aggCoord, cancelTask, innerSection);
      } else {
        nestedSection.set(0, nestedJoinRange);
        varData = vnested.read(aggCoord, cancelTask, nestedSection);
      }

      // copy it to the result
      MAMath.copy(dtype, varData.getIndexIterator(), result);
    }

  }

  //////////////////////////////////////////////////////////////////
}
