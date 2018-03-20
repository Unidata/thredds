/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.conv;

import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Mar 17, 2010
 * Time: 2:27:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Jason2Convention  extends CoordSysBuilder {
      /**
     * @param ncfile the NetcdfFile to test
     * @return true if we think this is a Zebra file.
     */
    public static boolean isMine(NetcdfFile ncfile) {

        //   :start_time = 9.17028312E8; // double
        // :stop_time = 9.170284104681826E8; // double

        if ((null == ncfile.findDimension("time"))) {
            return false;
        }
        // if (null == ncfile.findGlobalAttribute( "start_time")) return false;
        // if (null == ncfile.findGlobalAttribute( "stop_time")) return false;

        String center = ncfile.findAttValueIgnoreCase(null, "processing_center", null);
        if( (center != null) && center.equals("ESPC")) {
          String mission = ncfile.findAttValueIgnoreCase(null, "mission_name", null);
          return mission != null && mission.equals("OSTM/Jason-2");
        } else
          return false;
    }

    /**
     * _more_
     */
    public Jason2Convention() {
        this.conventionName = "Jason2";
    }
    
    /**
     * _more_
     *
     * @param ncDataset _more_
     * @param v _more_
     *
     * @return _more_
     */
    protected AxisType getAxisType(NetcdfDataset ncDataset,
                                   VariableEnhanced v) {
        String name = v.getShortName();
        if (name.equals("time")) {
            return AxisType.Time;
        }
        if (name.equals("lat")) {
            return AxisType.Lat;
        }
        if (name.equals("lon")) {
            return AxisType.Lon;
        }
        // if (name.equals("xLeo") ) return AxisType.GeoX;
        // if (name.equals("yLeo") ) return AxisType.GeoY;
        if (name.equals("alt")) {
            return AxisType.Height;
        }
        return null;
    }

}
