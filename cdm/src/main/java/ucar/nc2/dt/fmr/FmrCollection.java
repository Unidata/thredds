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

package ucar.nc2.dt.fmr;

import ucar.nc2.dataset.*;
import ucar.nc2.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.ma2.InvalidRangeException;

import java.util.Date;
import java.util.List;
import java.io.IOException;

/**
 * Forecast Model Run Collection.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class FmrCollection {
  private NetcdfDataset org; // should cache these NetcdfDataset, release ds from cache when all references are null
  private String runDim = "run";
  private boolean debug = true;

  public FmrCollection(NetcdfDataset org) {
    this.org = org;
  }

  public NetcdfDataset makeRun(int runIndex) throws InvalidRangeException {
    NetcdfDataset result = new NetcdfDataset();
    Group root = result.getRootGroup();

    NcMLReader.transferGroupAttributes(org.getRootGroup(), root);

    List vars = org.getVariables();
    for (int i=0; i<vars.size(); i++) {
      VariableDS v = (VariableDS) vars.get(i);
      if (v.isScalar() || !runDim.equals(v.getDimension(0).getName()) || runDim.equals(v.getName())) {
        VariableDS vnew = new VariableDS( root, v, false);
        root.addVariable( vnew);
        if (debug) System.out.println("FmrCollection: added non-grid "+vnew.getNameAndDimensions());
        continue;
      }

      VariableDS vnew = (VariableDS) v.slice(0, runIndex);
      root.addVariable( vnew);
      if (debug) System.out.println("FmrCollection: added grid "+vnew.getNameAndDimensions());
    }

    return null;
  }

  private void addVariable(Group root, Variable v, String what) {
    if (null == v) return;
    if (null == root.findVariable(v.getShortName())) {
      root.addVariable( v); // reparent
      v.setDimensions( v.getDimensionsString()); // rediscover dimensions
      if (debug) System.out.println(" added "+ what+" "+v.getName());
    }
  }

  public NetcdfDataset makeConstantOffset(double hourOffset) {
    return null;
  }

  public NetcdfDataset makeConstantForecastDate(Date forecast) {
    return null;
  }

  public NetcdfDataset makeBest() {
    return null;
  }


  public static void main(String args[]) throws IOException, InvalidRangeException {
    String filename = "file:./test/data/ncml/aggFmrc.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" agg dataest = "+ ncfile.toString()+"\n");

    FmrCollection fmrc = new FmrCollection( (NetcdfDataset) ncfile);
    fmrc.makeRun(0);
  }

}
