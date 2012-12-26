package ucar.nc2.ft.grid.impl;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.ft.grid.Coverage;
import ucar.nc2.ft.grid.CoverageCS;
import ucar.nc2.ft.grid.Subset;

import java.io.IOException;
import java.util.List;

/**
 * Description
 *
 * @author John
 * @since 12/25/12
 */
public class CoverageImpl implements Coverage {
  private NetcdfDataset ds;
  private CoverageCS ccs;
  private VariableEnhanced ve;

  CoverageImpl(NetcdfDataset ds, CoverageCS ccs, VariableEnhanced ve) {
    this.ds = ds;
    this.ccs = ccs;
    this.ve = ve;
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
    return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
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
    return null; // ve.findAttValueIgnoreCase(attName, defaultValue);
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
  public boolean hasMissingData() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean isMissingData(double val) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public MAMath.MinMax getMinMaxSkipMissingData(Array data) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Array readDataSlice(Subset subset) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Coverage makeSubset(Subset subset) throws InvalidRangeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
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
}
