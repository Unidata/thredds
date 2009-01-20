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
package ucar.nc2.ft.point.standard;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.ArrayStructure;

import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;
import java.io.IOException;

/**
 * A generalization of a Structure. Main function is to return a StructureDataIterator
 *
 * @author caron
 * @since Jan 20, 2009
 */
public abstract class Table {

  public static Table factory(NetcdfDataset ds, TableConfig config) {

    switch (config.type) {

      case Structure:
      case PseudoStructure:
        return new TableStructure(ds, config);

      case ArrayStructure:
        return new TableArrayStructure(ds, config);

      case MultiDim:
        return new TableMultDim(ds, config);

      case Singleton:
        return new TableSingleton(ds, config);

      default:
        throw new IllegalStateException("Unimplemented join type = " + config.type);

    }

  }

  ////////////////////////////////////////////////////////////////////////////////////////

  String name;
  FeatureType featureType;

  Table parent;
  Join join2parent; // the join to its parent
  Table extraJoinTable;

  String lat, lon, elev, time, timeNominal;
  String stnId, stnDesc, stnNpts, stnWmoId, limit;
  //public int nstations;

  List<VariableSimpleIF> cols = new ArrayList<VariableSimpleIF>();    // all variables
  List<String> coordVars = new ArrayList<String>(); // just the coord axes names

  protected Table(NetcdfDataset ds, TableConfig config) {
    this.name = config.name;
    this.featureType = config.featureType;

    this.lat = config.lat;
    this.lon = config.lon;
    this.elev = config.elev;
    this.time = config.time;
    this.timeNominal = config.timeNominal;

    this.stnId = config.stnId;
    this.stnDesc = config.stnDesc;
    this.stnNpts = config.stnNpts;
    this.stnWmoId = config.stnWmoId;
    this.limit = config.limit;

    if (config.parent != null)
      parent = Table.factory(ds, config.parent);

    if (config.join != null) {
      //if (config.join.override != null)
      //  join2parent = config.join.override;
      //else
        join2parent = Join.factory(config.join);

      join2parent.joinTables(parent, this);
    }

    if (config.extraJoin != null) {
      extraJoinTable = Table.factory(ds, config.extraJoin);
    }
  }

  abstract public StructureDataIterator getStructureDataIterator(StructureData parent, int bufferSize) throws IOException;

  public static class TableStructure extends Table {
    Structure struct;
    Dimension dim;

    TableStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);

      if (config.type == TableType.Structure) {

        if ((config.parent != null) && (config.parent.type == TableType.Structure)) {
          Structure parent = (Structure) ds.findVariable(config.parent.name);
          struct = (Structure) parent.findVariable(config.name);

        } else {
          struct = (Structure) ds.findVariable(config.name);
        }

        if (struct == null)
          throw new IllegalStateException("Cant find Structure " + config.name);

        dim = struct.getDimension(0);
        for (Variable v : struct.getVariables())
          this.cols.add(v);

      } else if (config.type == TableType.PseudoStructure) {

        struct = new StructurePseudo(ds, null, config.name, config.dim);
        for (Variable v : struct.getVariables())
          this.cols.add(v);
      }

    }

    @Override
    public Variable findVariable(String axisName) {
      return struct.findVariable(axisName);
    }

    @Override
    public String showDimension() {
      return dim.getName();
    }

    public StructureDataIterator getStructureDataIterator(StructureData parent, int bufferSize) throws IOException {
      if (join2parent != null)
        return join2parent.getStructureDataIterator(parent, bufferSize);

      return struct.getStructureIterator(bufferSize);
    }
  }

  public static class TableArrayStructure extends Table {
    ArrayStructure as;
    Dimension dim;

    TableArrayStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.as = config.as;
      dim = new Dimension(config.name, (int) config.as.getSize(), false);

      for (StructureMembers.Member m : config.as.getStructureMembers().getMembers())
        cols.add(new VariableSimpleAdapter(m));
    }

    @Override
    public String showDimension() {
      return dim.getName();
    }

    public StructureDataIterator getStructureDataIterator(StructureData parent, int bufferSize) throws IOException {
      if (join2parent != null)
        return join2parent.getStructureDataIterator(parent, bufferSize);

      return as.getStructureDataIterator();
    }
  }

  public static class TableMultDim extends Table {
    StructureMembers sm; // MultiDim
    Dimension dim;

    TableMultDim(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.dim = config.dim;
      assert dim != null;

      sm = new StructureMembers(config.name);
      for (Variable v : ds.getVariables()) {
        if (v.getRank() < 2) continue;
        if (v.getDimension(0).equals(config.outer) && v.getDimension(1).equals(config.dim)) {
          cols.add(v);
          // make member
          int rank = v.getRank();
          int[] shape = new int[rank - 2];
          System.arraycopy(v.getShape(), 2, shape, 0, rank - 2);
          sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
        }
      }
    }

    @Override
    public String showDimension() {
      return dim.getName();
    }

    public StructureDataIterator getStructureDataIterator(StructureData parent, int bufferSize) throws IOException {
      if (join2parent != null)
        return join2parent.getStructureDataIterator(parent, bufferSize);

      throw new IllegalStateException("TableMultDim cannont be root = " + name);
    }
  }

  public static class TableSingleton extends Table {
    StructureData sdata;

    TableSingleton(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.sdata = config.sdata;
    }

    public StructureDataIterator getStructureDataIterator(StructureData parent, int bufferSize) throws IOException {
      if (join2parent != null)
        return join2parent.getStructureDataIterator(parent, bufferSize);

      return new SingletonStructureDataIterator(sdata);
    }

    private class SingletonStructureDataIterator implements StructureDataIterator {
      private int count = 0;
      private StructureData sdata;

      SingletonStructureDataIterator(StructureData sdata) {
        this.sdata = sdata;
      }

      public boolean hasNext() throws IOException {
        return (count == 0);
      }

      public StructureData next() throws IOException {
        count++;
        return sdata;
      }

      public void setBufferSize(int bytes) {
      }

      public StructureDataIterator reset() {
        count = 0;
        return this;
      }
    }
  }

  public String getName() {
    return name;
  }

  public FeatureType getFeatureType() {
    return featureType;
  }

  public List<? super VariableSimpleIF> getDataVariables() {
    return cols;
  }

  public Variable findVariable(String axisName) {
    return null;
  }

  public String showDimension() {
    return "";
  }

  public String toString() {
    Formatter formatter = new Formatter();
    formatter.format(" Table %s on dimension %s type=%s\n", getName(), showDimension(), getClass().toString());
    formatter.format("  Coordinates=");
    formatter.format("\n  Data Variables= %d\n", cols.size());
    formatter.format("  Parent= %s join = %s\n", ((parent == null) ? "none" : parent.getName()), join2parent);
    return formatter.toString();
  }

  public String showAll() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("Table on dimension ").append(showDimension()).append("\n");
    for (VariableSimpleIF v : cols) {
      sbuff.append("  ").append(v.getName()).append("\n");
    }
    return sbuff.toString();
  }

  public int show(Formatter f, int indent) {
    if (parent != null)
      indent = parent.show(f, indent);

    String s = indent(indent);
    String ftDesc = (featureType == null) ? "" : "featureType=" + featureType.toString();
    String joinDesc = (join2parent == null) ? "" : "joinType=" + join2parent.getClass().toString();
//String dimDesc = (config.dim == null) ? "*" : config.dim.getName() + "=" + config.dim.getLength() + (config.dim.isUnlimited() ? " unlim" : "");
    f.format("\n%sTable %s: type=%s %s %s\n", s, getName(), getClass().toString(), joinDesc, ftDesc);
    showCoords(f, s);
    for (VariableSimpleIF v : cols) {
      f.format("%s  %s %s\n", s, v.getName(), getKind(v.getShortName()));
    }
    return indent + 2;
  }

  String indent(int n) {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < n; i++) sbuff.append(' ');
    return sbuff.toString();
  }

  private String getKind(String v) {
    if (v.equals(lat)) return "[Lat]";
    if (v.equals(lon)) return "[Lon]";
    if (v.equals(elev)) return "[Elev]";
    if (v.equals(time)) return "[Time]";
    if (v.equals(timeNominal)) return "[timeNominal]";
    if (v.equals(stnId)) return "[stnId]";
    if (v.equals(stnDesc)) return "[stnDesc]";
    if (v.equals(stnNpts)) return "[stnNpts]";
    if (v.equals(stnWmoId)) return "[stnWmoId]";
    if (v.equals(limit)) return "[limit]";

    return "";
  }

  private void showCoords(Formatter out, String indent) {
    boolean gotSome;
    gotSome = showCoord(out, lat, indent);
    gotSome |= showCoord(out, lon, indent);
    gotSome |= showCoord(out, elev, indent);
    gotSome |= showCoord(out, time, indent);
    gotSome |= showCoord(out, timeNominal, indent);
    gotSome |= showCoord(out, stnId, indent);
    gotSome |= showCoord(out, stnDesc, indent);
    gotSome |= showCoord(out, stnNpts, indent);
    gotSome |= showCoord(out, stnWmoId, indent);
    gotSome |= showCoord(out, limit, indent);
    if (gotSome) out.format("\n");
  }

  private boolean showCoord(Formatter out, String name, String indent) {
    if (name != null) {
      out.format(" %s Coord %s %s\n", indent, name, getKind(name));
      return true;
    }
    return false;
  }

}
