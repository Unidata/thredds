/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.ft.point.standard.TableConfigurerImpl;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.PointConfigXML;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;

import java.util.Formatter;
import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since Nov 3, 2009
 */
public class Suomi extends TableConfigurerImpl {
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String center = ds.findAttValueIgnoreCase(null, "Convention", null);
    return center != null && center.equals("Suomi-Station-CDM");
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    PointConfigXML reader = new PointConfigXML();
    return reader.readConfigXMLfromResource("resources/nj22/pointConfig/Suomi.xml", wantFeatureType, ds, errlog);
  }
}
