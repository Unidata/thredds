/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.TableConfigurerImpl;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.PointConfigXML;

import java.util.Formatter;
import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since Nov 10, 2009
 */
public class Nldn extends TableConfigurerImpl {
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String center = ds.findAttValueIgnoreCase(null, CDM.CONVENTIONS, null);
    return center != null && center.equals("NLDN-CDM");
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    PointConfigXML reader = new PointConfigXML();
    return reader.readConfigXMLfromResource("resources/nj22/pointConfig/Nldn.xml", wantFeatureType, ds, errlog);
  }
}
