package ucar.nc2.adde;

import ucar.nc2.Structure;
import ucar.ma2.DataType;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * A Variable implemented through an ADDE server.
 *  *
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
 */
public class AddeVariable extends ucar.nc2.dataset.VariableDS {
  private int nparam;

  public AddeVariable( NetcdfDataset ncfile, Structure parentStructure, String shortName,
       DataType dataType, String dims, String units, String desc, int nparam) {

    super(ncfile, null, parentStructure, shortName, dataType, dims, units, desc);
    this.nparam = nparam;
  }

  int getParam() { return nparam; }

}
