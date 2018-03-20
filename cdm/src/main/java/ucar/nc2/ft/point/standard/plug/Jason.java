/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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

