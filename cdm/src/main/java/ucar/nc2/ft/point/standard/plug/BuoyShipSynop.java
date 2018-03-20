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
 * Ship/Buoy decoded netcdf files
 *
 * @author caron
 * @since Nov 5, 2009
 */


public class BuoyShipSynop extends TableConfigurerImpl {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String title = ds.findAttValueIgnoreCase(null, "title", null);
    return title != null && (title.equals( "BUOY definition") || title.equals( "SYNOPTIC definition"));
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    PointConfigXML reader = new PointConfigXML();
    return reader.readConfigXMLfromResource("resources/nj22/pointConfig/BuoyShipSynop.xml", wantFeatureType, ds, errlog);
  }
}