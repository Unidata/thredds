package ucar.nc2.ft.cover.impl;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.ft.cover.Coverage;
import ucar.nc2.ft.cover.CoverageCS;

import java.io.IOException;
import java.util.List;

/**
 * Coverage Implementation
 *
 * @author John
 * @since 12/25/12
 */
public class CoverageImpl implements Coverage {
  private NetcdfDataset ds;
  private CoverageCS ccs;
  private VariableEnhanced ve;
  private VariableDS vds;

  CoverageImpl(NetcdfDataset ds, CoverageCS ccs, VariableEnhanced ve) {
    this.ds = ds;
    this.ccs = ccs;
    this.ve = ve;
    if (ve instanceof VariableDS) vds = (VariableDS) ve;
  }

  @Override
  public String getName() {
    return ve.getShortName();
  }

  @Override
  public String getFullName() {
    return ve.getFullName();
  }

  @Override
  public String getShortName() {
    return ve.getShortName();
  }

  @Override
  public String getDescription() {
    return ve.getDescription();
  }

  @Override
  public String getUnitsString() {
    return ve.getUnitsString();
  }

  @Override
  public int getRank() {
    return ve.getRank();
  }

  @Override
  public int[] getShape() {
    return ve.getShape();     // LOOK - canonicalize ??
  }

  @Override
  public DataType getDataType() {
    return ve.getDataType();
  }

  @Override
  public List<Attribute> getAttributes() {
    return ve.getAttributes();
  }

  @Override
  public Attribute findAttributeIgnoreCase(String name) {
    return ve.findAttributeIgnoreCase(name);
  }

  @Override
  public String findAttValueIgnoreCase(String attName, String defaultValue) {
    return null; // ds.findAttValueIgnoreCase(ve, attName, defaultValue);
  }

  @Override
  public List<Dimension> getDimensions() {
    return ve.getDimensions();
  }

  @Override
  public CoverageCS getCoordinateSystem() {
    return ccs;
  }

  @Override
  public boolean hasMissing() {
    return (vds != null) && vds.hasMissing();
  }

  @Override
  public boolean isMissing(double val) {
    return (vds != null) && vds.isMissing(val);
  }

  @Override
  public String getInfo() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public int compareTo(VariableSimpleIF o) {
    return getShortName().compareTo(o.getShortName());
  }

  @Override
  public String toString() {
    return ve.toString();
  }

  /////////////////

  @Override
  public Array readData(CoverageCS.Subset subset) throws IOException, InvalidRangeException {
    CoverageCSImpl.SubsetImpl impl = (CoverageCSImpl.SubsetImpl) subset;
    return impl.readData(ve);
  }


}
