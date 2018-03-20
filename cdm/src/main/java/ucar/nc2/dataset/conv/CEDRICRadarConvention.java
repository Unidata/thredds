/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.conv;

import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.ma2.ArrayDouble;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Dec 8, 2009
 * Time: 10:40:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class CEDRICRadarConvention extends CF1Convention {

  /**
   * @param ncfile test this NetcdfFile
   * @return true if we think this is a ATDRadarConvention file.
   */
  public static boolean isMine(NetcdfFile ncfile) {
    // not really sure until we can examine more files
    Dimension s = ncfile.findDimension("cedric_general_scaling_factor");
    Variable v = ncfile.findVariable("cedric_run_date");
    return v != null && s != null;
  }

  public CEDRICRadarConvention() {
    this.conventionName = "CEDRICRadar";
  }
      
  public void augmentDataset(NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
  /*  float lat = 40.45f;
    float lon = -104.64f;
    ProjectionImpl projection = new FlatEarth(lat, lon);

    Variable ct = new Variable( ncDataset, null, null, projection.getClassName());
    ct.setDataType( DataType.CHAR);
    ct.setDimensions( "");

    ct.addAttribute( new Attribute("grid_mapping_name", "flat_earth"));
    ct.addAttribute( new Attribute(_Coordinate.TransformType, "Projection"));
    ct.addAttribute( new Attribute(_Coordinate.Axes, "GeoX GeoY"));
    ncDataset.addVariable(null, ct);
  */
        NcMLReader.wrapNcMLresource(ncDataset, CoordSysBuilder.resourcesDir + "CEDRICRadar.ncml", cancelTask);
        Variable lat = ncDataset.findVariable("radar_latitude");
        Variable lon = ncDataset.findVariable("radar_longitude");
        float    latv = (float)lat.readScalarDouble();
        float    lonv = (float)lon.readScalarDouble();
        Variable pv = ncDataset.findVariable("Projection");
        pv.addAttribute(new Attribute("longitude_of_projection_origin", lonv) );
        pv.addAttribute(new Attribute("latitude_of_projection_origin", latv) );

        Variable tvar = ncDataset.findVariable("time");
        /*Date dt = null;
        Variable sdate = ncDataset.findVariable("start_date");
        Variable stime = ncDataset.findVariable("start_time");
        String dateStr = sdate.readScalarString();
        String timeStr = stime.readScalarString();
        try {
          dt = DateUtil.parse(dateStr + " " + timeStr);
        } catch (Exception e) {}  */

        int nt = 1;

        ArrayDouble.D1 data = new ArrayDouble.D1(nt);

        // fake
        data.setDouble(0, 0);
        // data.setDouble(0, dt.getTime()/1000);

        tvar.setCachedData(data, false);

        super.augmentDataset(ncDataset, cancelTask);

   //     System.out.println("here\n");

  //  ncDataset.finish();
  }
}


