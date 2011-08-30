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

import ucar.grib.GribGridRecord;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.iosp.grid.*;
import ucar.grib.grib2.Grib2GridTableLookup;
import ucar.grib.grib1.Grib1GridTableLookup;
import java.util.*;

/**
 * Create a Netcdf File from a Grib Index
 *
 * @author caron
 */
public class GribIndexToNC extends GridIndexToNC {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribIndexToNC.class);

  public GribIndexToNC(String filename) {
    super(filename);
  }

  public GribIndexToNC(ucar.unidata.io.RandomAccessFile raf) {
    super(raf);
  }

  @Override
  protected void addExtraAttributes(GridRecord firstRecord, GridTableLookup lookup, NetcdfFile ncfile) {

    String center = null;
    String subcenter = null;
    if ( lookup instanceof Grib2GridTableLookup ) {
      Grib2GridTableLookup g2lookup = (Grib2GridTableLookup) lookup;
      GribGridRecord ggr = (GribGridRecord) firstRecord;
      center = g2lookup.getFirstCenterName();
      ncfile.addAttribute(null, new Attribute("Originating_center", center));
      subcenter = g2lookup.getFirstSubcenterName();
      if (subcenter != null)
        ncfile.addAttribute(null, new Attribute("Originating_subcenter", subcenter));

      String model = g2lookup.getModel();
      if (model != null)
        ncfile.addAttribute(null, new Attribute("Generating_Model", model));
      if (null != g2lookup.getFirstProductStatusName())
        ncfile.addAttribute(null, new Attribute("Product_Status", g2lookup.getFirstProductStatusName()));
      ncfile.addAttribute(null, new Attribute("Product_Type", g2lookup.getFirstProductTypeName()));

    } else if ( lookup instanceof Grib1GridTableLookup ) {
      Grib1GridTableLookup g1lookup = (Grib1GridTableLookup) lookup;
      ncfile.addAttribute(null, new Attribute("Originating_center_id", ((Grib1GridTableLookup) lookup).getFirstCenterId()));
      ncfile.addAttribute(null, new Attribute("Originating_subcenter_id", ((Grib1GridTableLookup) lookup).getFirstSubcenterId()));
      ncfile.addAttribute(null, new Attribute("Table_version", ((Grib1GridTableLookup) lookup).getFirstTableVersion()));

      center = g1lookup.getFirstCenterName();
      subcenter = g1lookup.getFirstSubcenterName();
      ncfile.addAttribute(null, new Attribute("Originating_center", center));
       if (subcenter != null) {
        ncfile.addAttribute(null, new Attribute("Originating_subcenter", subcenter));
      }

      String model = g1lookup.getModel();
      if (model != null)
        ncfile.addAttribute(null, new Attribute("Generating_Model", model));
      if (null != g1lookup.getFirstProductStatusName())
        ncfile.addAttribute(null, new Attribute("Product_Status", g1lookup.getFirstProductStatusName()));
      ncfile.addAttribute(null, new Attribute("Product_Type", g1lookup.getFirstProductTypeName()));
    }
  }

  @Override
  protected GridEnsembleCoord addEnsembles(List<GridEnsembleCoord> ensembleCoords, List<GridRecord> recordList) {
    GridEnsembleCoord useEnsembleCoord = null;
    GridEnsembleCoord ensembleCoord = new GribEnsembleCoord(recordList);
    for (GridEnsembleCoord gec : ensembleCoords) {
      if (ensembleCoord.equals(gec)) {
        useEnsembleCoord = gec;
        break;
      }
    }
    if (useEnsembleCoord == null) {
      useEnsembleCoord = ensembleCoord;
      ensembleCoords.add(ensembleCoord);
    }
    return useEnsembleCoord;
  }

  protected GridHorizCoordSys makeGridHorizCoordSys(GridDefRecord gds, GridTableLookup lookup, Group g) {
    return new GribHorizCoordSys(gds, lookup, g);
  }

  protected GridVariable makeGridVariable(String indexFilename, String name, GridHorizCoordSys hcs, GridTableLookup lookup) {
    return new GribVariable(indexFilename, name, hcs, lookup);
  }

  protected GridTimeCoord makeGridTimeCoord(List<GridRecord> recordList, String location){
    return new GribTimeCoord(recordList, location);
  }

  protected GridVertCoord makeGridVertCoord(List<GridRecord> recordList, String vname, GridTableLookup lookup, GridHorizCoordSys hcs) {
    return new GribVertCoord(recordList, vname, lookup, hcs);
  }

}
