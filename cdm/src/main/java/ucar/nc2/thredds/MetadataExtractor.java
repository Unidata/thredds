// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.thredds;

import thredds.catalog.DataType;
import thredds.catalog.ThreddsMetadata;
import thredds.catalog.InvDatasetImpl;

import java.io.IOException;
import java.util.Iterator;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.VariableSimpleIF;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Extract THREDDS metadata from the undelying CDM dataset.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class MetadataExtractor {

  /**
   * Extract the lat/lon/alt bounding boxes from the dataset.
   * @param threddsDataset open this dataset
   * @return ThreddsMetadata.GeospatialCoverage, or null if unable.
   * @throws IOException
   */
  static public ThreddsMetadata.GeospatialCoverage extractGeospatial(InvDatasetImpl threddsDataset) throws IOException {
    ThreddsDataFactory.Result result = null;

    try {
      result = new ThreddsDataFactory().openDatatype(threddsDataset, null);
      if (result.fatalError) {
        System.out.println(" openDatatype errs=" + result.errLog);
        return null;
      }

      if (result.dtype == DataType.GRID) {
        System.out.println(" GRID=" + result.location);
        ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
        LatLonRect llbb = null;
        CoordinateAxis1D vaxis = null;

        GridDataset gridDataset = result.gridDataset;
        Iterator gridsets = gridDataset.getGridSets().iterator();
        while (gridsets.hasNext()) {
          GridDataset.Gridset gridset = (GridDataset.Gridset) gridsets.next();
          GridCoordSystem gsys = gridset.getGeoCoordSystem();
          if (llbb == null)
            llbb = gsys.getLatLonBoundingBox();

          CoordinateAxis1D vaxis2 = gsys.getVerticalAxis();
          if (vaxis == null)
            vaxis = vaxis2;
          else if ((vaxis2 != null) && (vaxis2.getSize() > vaxis.getSize()))
            vaxis = vaxis2;
        }

        gc.setBoundingBox(llbb);
        if (vaxis != null)
          gc.setVertical(vaxis);
        return gc;

      } else if ((result.dtype == DataType.STATION) || (result.dtype == DataType.POINT)) {
        PointObsDataset pobsDataset = result.pobsDataset;
        LatLonRect llbb = pobsDataset.getBoundingBox();
        if (null != llbb) {
          ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
          gc.setBoundingBox(llbb);
          return gc;
        }
      }

    } finally {
      try {
        if (result != null)
          result.close();
      } catch (IOException ioe) {
      }
    }

    return null;
  }

  /**
   * Extract a list of data variables (and their canonical names if possible) from the dataset.
   * @param threddsDataset open this dataset
   * @return ThreddsMetadata.Variables, or null if unable.
   * @throws IOException
   */
  static public ThreddsMetadata.Variables extractVariables(InvDatasetImpl threddsDataset) throws IOException {
    ThreddsDataFactory.Result result = null;

    try {
      result = new ThreddsDataFactory().openDatatype(threddsDataset, null);
      if (result.fatalError) {
        System.out.println(" openDatatype errs=" + result.errLog);
        return null;
      }

      if (result.dtype == DataType.GRID) {
        // System.out.println(" extractVariables GRID=" + result.location);
        GridDataset gridDataset = result.gridDataset;

        String fileFormat = threddsDataset.getDataFormatType().toString();
        if ((fileFormat.equalsIgnoreCase("GRIB-1")) || (fileFormat.equalsIgnoreCase("GRIB-2"))) {
          boolean isGrib1 = fileFormat.equals("GRIB-1");
          ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables(fileFormat);
          java.util.List grids = gridDataset.getGrids();
          for (int i = 0; i < grids.size(); i++) {
            GridDatatype grid = (GridDatatype) grids.get(i);
            ThreddsMetadata.Variable v = new ThreddsMetadata.Variable();
            v.setName( grid.getName());
            v.setDescription( grid.getDescription());
            v.setUnits( grid.getUnitsString());

            //ucar.nc2.Attribute att = grid.findAttributeIgnoreCase("GRIB_param_number");
            //String paramNumber = (att != null) ? att.getNumericValue().toString() : null;
            if (isGrib1) {
              v.setVocabularyName( grid.findAttValueIgnoreCase("GRIB_param_name", "ERROR"));
              v.setVocabularyId( grid.findAttributeIgnoreCase("GRIB_param_id"));
            } else {
              String paramDisc = grid.findAttValueIgnoreCase("GRIB_param_discipline", "");
              String paramCategory = grid.findAttValueIgnoreCase("GRIB_param_category", "");
              String paramName = grid.findAttValueIgnoreCase("GRIB_param_name", "");
              v.setVocabularyName( paramDisc +" / " + paramCategory +" / " + paramName);
              v.setVocabularyId( grid.findAttributeIgnoreCase("GRIB_param_id"));
            }
            vars.addVariable( v);
          }
          vars.sort();
          return vars;

        } else { // GRID but not GRIB
          ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables("CF-1.0");
          java.util.List grids = gridDataset.getGrids();
          for (int i = 0; i < grids.size(); i++) {
            GridDatatype grid = (GridDatatype) grids.get(i);
            ThreddsMetadata.Variable v = new ThreddsMetadata.Variable();
            vars.addVariable( v);

            v.setName( grid.getName());
            v.setDescription( grid.getDescription());
            v.setUnits( grid.getUnitsString());

            ucar.nc2.Attribute att = grid.findAttributeIgnoreCase("standard_name");
            v.setVocabularyName( (att != null) ? att.getStringValue() : "N/A");
          }
          vars.sort();
          return vars;
        }

      } else if ((result.dtype == DataType.STATION) || (result.dtype == DataType.POINT)) {
        PointObsDataset pobsDataset = result.pobsDataset;
        ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables("CF-1.0");
        java.util.List varList = pobsDataset.getDataVariables();
        for (int i = 0; i < varList.size(); i++) {
          VariableSimpleIF vs = (VariableSimpleIF) varList.get(i);
          ThreddsMetadata.Variable v = new ThreddsMetadata.Variable();
          vars.addVariable( v);

          v.setName( vs.getName());
          v.setDescription( vs.getDescription());
          v.setUnits( vs.getUnitsString());

          ucar.nc2.Attribute att = vs.findAttributeIgnoreCase("standard_name");
          v.setVocabularyName( (att != null) ? att.getStringValue() : "N/A");
        }
        vars.sort();
        return vars;
      }

    } finally {
      try {
        if (result != null)
          result.close();
      } catch (IOException ioe) {
      }
    }

    return null;
  }

}
