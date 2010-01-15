/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.Table;
import ucar.nc2.ft.point.standard.TableConfigurerImpl;
import ucar.nc2.ft.point.standard.PointConfigXML;
import ucar.nc2.*;
import ucar.nc2.units.SimpleUnit;
import ucar.ma2.StructureDataScalar;
import ucar.ma2.DataType;

import java.util.*;
import java.io.IOException;

/**
 * Class Description.
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
