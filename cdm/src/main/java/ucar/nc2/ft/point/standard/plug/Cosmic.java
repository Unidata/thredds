/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.TableConfigurerImpl;
import ucar.nc2.ft.point.standard.PointConfigXML;

import java.util.*;
import java.io.IOException;

/**
 * TableConfigurer for cosmic data
 *
 * @author caron
 * @since Jan 26, 2009
 */
public class Cosmic extends TableConfigurerImpl {
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String center = ds.findAttValueIgnoreCase(null, "center", null);
    return center != null && center.equals("UCAR/CDAAC");
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    PointConfigXML reader = new PointConfigXML();
    if(ds.getConventionUsed().equalsIgnoreCase("Cosmic1"))
        return reader.readConfigXMLfromResource("resources/nj22/pointConfig/Cosmic1.xml", wantFeatureType, ds, errlog);
    else if(ds.getConventionUsed().equalsIgnoreCase("Cosmic2"))
        return reader.readConfigXMLfromResource("resources/nj22/pointConfig/Cosmic2.xml", wantFeatureType, ds, errlog);
    else if(ds.getConventionUsed().equalsIgnoreCase("Cosmic3"))
        return reader.readConfigXMLfromResource("resources/nj22/pointConfig/Cosmic3.xml", wantFeatureType, ds, errlog);
    else
        return null;
      //return reader.readConfigXML("C:\\dev\\tds\\thredds\\cdm\\src\\main\\resources\\resources\\nj22\\pointConfig\\Cosmic1.xml", wantFeatureType, ds, errlog);
  }
}
