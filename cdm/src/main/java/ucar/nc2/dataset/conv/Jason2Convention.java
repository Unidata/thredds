/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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
