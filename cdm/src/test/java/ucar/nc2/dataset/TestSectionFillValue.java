// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.dataset;

import ucar.nc2.*;
import ucar.nc2.util.*;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class TestSectionFillValue {
  static NetcdfDataset ncDataset;
  static NetcdfFile ncFile;
  static String location;

  static CancelTask cTask;
  static List liste;
  static List listeAxes;
  static List listeCoordinateSystem;
  static List listeCoordinateAxis;
  static List listeVariables;
  static List listeAttributsGlobaux;
  static List listeDimensions;

  static Variable variableField;
  static String nomVariableField;

  public static void main(String[] args) throws IOException, InvalidRangeException {

    location = TestAll.getUpcSharePath() + "/testdata/grid/netcdf/cf/CLE_year.nc";
    // "dods://opendap.mercator-ocean.fr/thredds/dodsC/mercatorPsy3v1R1v_glo_mean_best_estimate";
    ncDataset = NetcdfDataset.acquireDataset(location, cTask);
    VariableDS v = (VariableDS) ncDataset.findVariable("D2_SO4");
    assert (v != null);
    assert (v.hasFillValue());
    assert (v.findAttribute("_FillValue") != null);
    System.out.println("_FillValue="+v.findAttribute("_FillValue"));

    int rank = v.getRank();
    ArrayList ranges = new ArrayList();
    ranges.add(null);
    for (int i = 1; i < rank; i++) {
      ranges.add(new Range(0, 1));
    }

    VariableDS v_section = (VariableDS) v.section(ranges);
    assert (v_section.findAttribute("_FillValue") != null);
    System.out.println("_FillValue="+v_section.findAttribute("_FillValue"));
    assert (v_section.hasFillValue()); // fails
  }
}
