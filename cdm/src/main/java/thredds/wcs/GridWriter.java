// $Id:GridWriter.java 63 2006-07-12 21:50:51Z edavis $
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
package thredds.wcs;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.Parameter;

import java.io.*;
import java.util.*;

/**
 * Not used.
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */

public class GridWriter {
  private static boolean debug = false;


  /**
   * Wrie a Grid to a physical file, using Netcdf-3 file format.
   *
   * @param grid write this grid
   * @param fileOutName write to this local file
   * @param timeIdx which time index ?
   *
   * @return NetcdfFile that was written. It remains open for reading or writing.
   */
  public static NetcdfFile writeToFile(GridDatatype grid, String fileOutName, int timeIdx) throws IOException {

    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew( fileOutName, false);
    if (debug) {
      System.out.println("GridWriter write "+grid.getName()+" to "+fileOutName);
    }

    ncfile.addGlobalAttribute("Conventions", _Coordinate.Convention);

    GridCoordSystem gcs = grid.getGridCoordSystem();
    CoordinateAxis1DTime taxis = gcs.getTimeAxis1D();
    ncfile.addGlobalAttribute("time", taxis.getCoordName(timeIdx));

    // copy all dimensions except time
    Dimension timeDim = grid.getTimeDimension();

    HashMap dimHash = new HashMap();
    Iterator iter = grid.getDimensions().iterator();
    while (iter.hasNext()) {
      Dimension oldD = (Dimension) iter.next();
      if (oldD.equals(timeDim)) continue;

      Dimension newD = ncfile.addDimension(oldD.getName(), oldD.isUnlimited() ? -1 : oldD.getLength(),
          oldD.isShared(), oldD.isUnlimited(), oldD.isVariableLength());
      dimHash.put( newD.getName(), newD);
      if (debug) System.out.println("add dim= "+newD);
    }

    // Grid variable
    defineVariable(ncfile, grid.getVariable(), dimHash, timeDim);

    // Coordinate Variables
    defineVariable(ncfile, gcs.getXHorizAxis(), dimHash, timeDim);
    defineVariable(ncfile, gcs.getYHorizAxis(), dimHash, timeDim);
    defineVariable(ncfile, gcs.getVerticalAxis(), dimHash, timeDim);
    //defineVariable(ncfile, gcs.getTimeAxis(), dimHash);

    // projection
    ProjectionImpl  proj = grid.getProjection();
    if (proj != null) {
      if (proj instanceof LambertConformal) {
        LambertConformal lc = (LambertConformal) proj;
        ncfile.addGlobalAttribute("spatial_ref", lc.toWKS());
      }
      defineProjection( ncfile, proj);
    }

    // create the file
    ncfile.create();
    if (debug)
      System.out.println("File Out= "+ncfile.toString());

    // write the data
    Array data = grid.readVolumeData( timeIdx);
    try {
      ncfile.write(grid.getName(), data);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException( e.getMessage());
    }

    // write the coordinates
    writeVariable(ncfile, gcs.getXHorizAxis());
    writeVariable(ncfile, gcs.getYHorizAxis());
    writeVariable(ncfile, gcs.getVerticalAxis());
    //writeVariable(ncfile, gcs.getTimeAxis());

    ncfile.flush();
    if (debug) System.out.println("FileWriter done");

    return ncfile;
  }

  private static void defineVariable(NetcdfFileWriteable ncfile, VariableEnhanced oldVar, HashMap dimHash, Dimension timeDim) {
    if (oldVar == null)
      return;

    // copy dimensions
    ArrayList dims = new ArrayList();
    List dimvList = oldVar.getDimensions();
    for (int j = 0; j < dimvList.size(); j++) {
      Dimension oldD = (Dimension) dimvList.get(j);
      if (!oldD.equals(timeDim))
        dims.add( dimHash.get(oldD.getName()));
    }

    ncfile.addVariable(oldVar.getName(), oldVar.getDataType(), dims);
    if (debug) System.out.println("add var= " + oldVar.getName());

    // attributes
    List attList = oldVar.getAttributes();
    for (int j = 0; j < attList.size(); j++) {
      Attribute att = (Attribute) attList.get(j);
      if (att.isString())
        ncfile.addVariableAttribute(oldVar.getName(), att.getName(), att.getStringValue());
      else if (att.isArray())
        ncfile.addVariableAttribute(oldVar.getName(), att.getName(), att.getValues());
      else
        ncfile.addVariableAttribute(oldVar.getName(), att.getName(), att.getNumericValue());
    }

  }

  private static void defineProjection(NetcdfFileWriteable ncfile, Projection proj) {
    String varName = "Projection";
    ncfile.addVariable("Projection", DataType.CHAR, new ArrayList());
    List params = proj.getProjectionParameters();
    for (int i = 0; i < params.size(); i++) {
      Parameter p = (Parameter) params.get(i);
      if (p.isString())
        ncfile.addVariableAttribute(varName, p.getName(), p.getStringValue());
      else {
        double[] data = p.getNumericValues();
        Array dataA = Array.factory(double.class, new int[] {data.length}, data);
        ncfile.addVariableAttribute(varName, p.getName(), dataA);
      }
    }
    ncfile.addVariableAttribute(varName, _Coordinate.TransformType, TransformType.Projection.toString());
    ncfile.addVariableAttribute(varName, _Coordinate.AxisTypes, "GeoX GeoY");
  }

  private static void writeVariable(NetcdfFileWriteable ncfile, VariableEnhanced oldVar) throws IOException {
    if (oldVar == null)
      return;

      Array data = oldVar.read(); // LOOK: read all in one gulp!!
      try {
        ncfile.write(oldVar.getName(), data);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }

  }

  public static void main( String arg[]) throws java.net.MalformedURLException, IOException {
    String datasetIn = (arg.length > 0) ? arg[0] : "C:/Program Files/Apache Group/jakarta-tomcat-5.0.28/content/thredds/wcs/testdata/eta.nc"; // "dods://dods.coas.oregonstate.edu:8080/dods/dts/ingrid";
    String filenameOut = (arg.length > 1) ? arg[1] : "C:/temp/thredds/testGridWriter.nc";

    GridDataset gridDs  = ucar.nc2.dt.grid.GridDataset.open(datasetIn);
    GridDatatype grid = gridDs.findGridDatatype("T");
    NetcdfFile ncfileOut = GridWriter.writeToFile( grid, filenameOut, 0);

    gridDs.close();
    ncfileOut.close();
  }

}


