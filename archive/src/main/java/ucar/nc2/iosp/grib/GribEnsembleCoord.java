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

package ucar.nc2.iosp.grib;

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.grib.GribGridRecord;
import ucar.grib.grib2.Grib2Tables;
import ucar.nc2.iosp.grid.GridEnsembleCoord;
import ucar.nc2.iosp.grid.GridRecord;

import java.util.*;

/**
 * Handles the Ensemble coordinate dimension.
 * Assumes GribGridRecord
 */
public class GribEnsembleCoord extends GridEnsembleCoord {
  static private org.slf4j.Logger log =  org.slf4j.LoggerFactory.getLogger(GribEnsembleCoord.class);

  /**
   * Create a new GridEnsembleCoord with the list of records
   *
   * @param records records to use
   */
  GribEnsembleCoord(List<GridRecord> records) {
    Map<Integer,EnsCoord> map = new HashMap<Integer,EnsCoord>();

    for( GridRecord record : records ) {
      GribGridRecord ggr = (GribGridRecord) record;
      int ensNumber = ggr.getPds().getPerturbationNumber();
      int ensType = ggr.getPds().getPerturbationType();
      int h = 1000 * ensNumber + ensType; // unique perturbation number and type
      map.put(h, new EnsCoord(ensNumber, ensType));
    }

    ensCoords = new ArrayList<EnsCoord>(map.values());
    Collections.sort(ensCoords);
  }

   /**
   * Add this as a variable to the netCDF file
   *
   * @param ncfile the netCDF file
   * @param g      the group in the file
   */
  @Override
  protected void addToNetcdfFile(NetcdfFile ncfile, Group g) {
    Variable v = new Variable(ncfile, g, null, getName());
    v.setDimensions(v.getShortName());
    v.setDataType(DataType.STRING);
    v.addAttribute(new Attribute("long_name", "ensemble"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Ensemble.toString()));

    String[] data = new String[getNEnsembles()];

    for (int i = 0; i < getNEnsembles(); i++) {
      EnsCoord ec = ensCoords.get(i);
      data[i] = Grib2Tables.codeTable4_6( ec.type) +" "+ ec.number;
    }
    Array dataArray = Array.factory(DataType.STRING, new int[]{getNEnsembles()}, data);
    v.setCachedData(dataArray, false);

    ncfile.addVariable(g, v);
  }

  /**
   * Get the index of a GridRecord
   *
   * @param ggr the grib record
   * @return the index or -1 if not found
   */
  int getIndex(GribGridRecord ggr) {
    int ensNumber = ggr.getPds().getPerturbationNumber();
    int ensType = ggr.getPds().getPerturbationType();

    int count = 0;
    for (EnsCoord coord : ensCoords) {
      if ((coord.number == ensNumber) && (coord.type == ensType)) return count;
      count++;
    }
    return -1;
  }

}
