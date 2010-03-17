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

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.PointConfigXML;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.TableConfigurerImpl;

import java.io.IOException;
import java.util.Formatter;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Mar 17, 2010
 * Time: 2:24:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class Jason extends TableConfigurerImpl {
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String mission = ds.findAttValueIgnoreCase(null, "mission_name", null);
    String center = ds.findAttValueIgnoreCase(null, "processing_center", null);
    if( center != null && center.equals("ESPC")){
        return mission != null && mission.equals("OSTM/Jason-2");
    } else
        return false;
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    PointConfigXML reader = new PointConfigXML();

    return reader.readConfigXMLfromResource("resources/nj22/pointConfig/Jason2.xml", wantFeatureType, ds, errlog);

  }

}

