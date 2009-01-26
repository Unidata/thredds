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
import ucar.nc2.ft.point.StructureDataIteratorLinked;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.*;

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

  public enum CoordName {
    Lat, Lon, Elev, Time, TimeNominal, StnId, StnDesc, WmoId, StnAlt
  }

  public enum Type {
    ArrayStructure, Construct, Contiguous, LinkedList, MultiDimInner, MultiDimOuter, NestedStructure, Singleton, Structure, Top
  }

  public static Table factory(NetcdfDataset ds, TableConfig config) {

    switch (config.type) {

      case ArrayStructure:
        return new TableArrayStructure(ds, config);

      case Construct:
        return new TableConstruct(ds, config);

      case Contiguous:
        return new TableContiguous(ds, config);

      case LinkedList:
        return new TableLinkedList(ds, config);

      case MultiDimInner:
        return new TableMultiDimInner(ds, config);

      case MultiDimOuter:
        return new TableMultiDimOuter(ds, config);

      case NestedStructure:
        return new TableNestedStructure(ds, config);

      case Singleton:
        return new TableSingleton(ds, config);

      case Structure:
        return new TableStructure(ds, config);

      case Top:
        return new TableTop(ds, config);
    }

    throw new IllegalStateException("Unimplemented Table type = " + config.type);
  }

  ////////////////////////////////////////////////////////////////////////////////////////

  String name;
  FeatureType featureType;

  Table parent;
  Join extraJoin;

  String lat, lon, elev, time, timeNominal;
  String stnId, stnDesc, stnNpts, stnWmoId, stnAlt, limit;
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
    this.stnAlt = config.stnAlt;
    this.limit = config.limit;

    if (config.parent != null)
      parent = Table.factory(ds, config.parent);

    this.extraJoin = config.extraJoin;
  }

  abstract public StructureDataIterator getStructureDataIterator(StructureData parent, int bufferSize) throws IOException;

  String findCoordinateVariableName(CoordName coordName) {
    switch (coordName) {
      case Elev:
        return elev;
      case Lat:
        return lat;
      case Lon:
        return lon;
      case Time:
        return time;
      case TimeNominal:
        return timeNominal;

      case StnId:
        return stnId;
      case StnDesc:
        return stnDesc;
      case WmoId:
        return stnWmoId;
      case StnAlt:
        return stnAlt;

    }
    return null;
  }

  ///////////////////////////////////////////////////////
  public static class TableStructure extends Table {
    Structure struct;
    Dimension dim;

    TableStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);

      if (config.isPsuedoStructure) {
        this.dim = config.dim;
        assert dim != null;
        struct = new StructurePseudo(ds, null, config.structName, config.dim);

      } else {
        struct = (Structure) ds.findVariable(config.structName);
        if (struct == null)
          throw new IllegalStateException("Cant find Structure " + config.structName);

        dim = struct.getDimension(0);
      }

      for (Variable v : struct.getVariables())
        this.cols.add(v);
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
      return struct.getStructureIterator(bufferSize);
    }
  }

  ///////////////////////////////////////////////////////
  public static class TableArrayStructure extends Table {
    ArrayStructure as;
    Dimension dim;

    TableArrayStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.as = config.as;
      dim = new Dimension(config.name, (int) config.as.getSize(), false);
      assert (this.as != null);

      for (StructureMembers.Member m : config.as.getStructureMembers().getMembers())
        cols.add(new VariableSimpleAdapter(m));
    }

    @Override
    public String showDimension() {
      return dim.getName();
    }

    public StructureDataIterator getStructureDataIterator(StructureData parent, int bufferSize) throws IOException {
      return as.getStructureDataIterator();
    }
  }

  ///////////////////////////////////////////////////////
  public static class TableConstruct extends Table {
    Structure struct;

    TableConstruct(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      struct = (Structure) ds.findVariable(config.structName);
      if (struct == null)
        throw new IllegalStateException("Cant find Structure " + config.structName);
    }

    public Variable findVariable(String axisName) {
      return struct.findVariable(axisName);
    }

    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {
      return struct.getStructureIterator(bufferSize);
    }
  }

  ///////////////////////////////////////////////////////
  public static class TableContiguous extends TableStructure {
    private String start, numRecords;

    TableContiguous(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.start = config.start;
      this.numRecords = config.numRecords;
    }

    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {
      int firstRecno = parentStruct.getScalarInt(start);
      int n = parentStruct.getScalarInt(numRecords);
      return new StructureDataIteratorLinked(struct, firstRecno, n, null);
    }
  }

  ///////////////////////////////////////////////////////
  public static class TableLinkedList extends TableStructure {
    private String start, next;

    TableLinkedList(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.start = config.start;
      this.next = config.next;
    }

    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {
      int firstRecno = parentStruct.getScalarInt(start);
      return new StructureDataIteratorLinked(struct, firstRecno, -1, next);
    }
  }

  ///////////////////////////////////////////////////////
  public static class TableMultiDimOuter extends Table.TableStructure {

    TableMultiDimOuter(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
    }
  }

  ///////////////////////////////////////////////////////
  public static class TableMultiDimInner extends Table {
    StructureMembers sm; // MultiDim
    Dimension dim;
    NetcdfDataset ds;

    TableMultiDimInner(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.ds = ds;
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

    @Override
    public Variable findVariable(String axisName) {
      return ds.findVariable(axisName);
    }

    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {
      ArrayStructureMA asma = new ArrayStructureMA(sm, new int[]{dim.getLength()});
      for (VariableSimpleIF v : cols) {
        Array data = parentStruct.getArray(v.getShortName());
        StructureMembers.Member childm = sm.findMember(v.getShortName());
        childm.setDataArray(data);
      }
      return asma.getStructureDataIterator();
    }

  }

  ///////////////////////////////////////////////////////
  public static class TableNestedStructure extends Table {
    String nestedTableName; // short name of structure
    Structure struct;

    TableNestedStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.nestedTableName = config.nestedTableName;
      struct = (Structure) ds.findVariable(config.structName);
    }

    public Variable findVariable(String axisName) {
      return struct.findVariable(axisName);
    }

    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {

      StructureMembers members = parentStruct.getStructureMembers();
      StructureMembers.Member m = members.findMember(nestedTableName);
      if (m.getDataType() == DataType.SEQUENCE) {
        ArraySequence seq = parentStruct.getArraySequence(m);
        return seq.getStructureDataIterator();

      } else if (m.getDataType() == DataType.STRUCTURE) {
        ArrayStructure as = parentStruct.getArrayStructure(m);
        return as.getStructureDataIterator();
      }

      throw new IllegalStateException("Cant fing memmber " + nestedTableName);
    }
  }

  ///////////////////////////////////////////////////////
  public static class TableSingleton extends Table {
    StructureData sdata;

    TableSingleton(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.sdata = config.sdata;
      assert (this.sdata != null);
    }

    public StructureDataIterator getStructureDataIterator(StructureData parent, int bufferSize) throws IOException {
      return new SingletonStructureDataIterator(sdata);
    }
  }

  public static class TableTop extends Table {
    NetcdfDataset ds;

    TableTop(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.ds = ds;
    }

    public StructureDataIterator getStructureDataIterator(StructureData parent, int bufferSize) throws IOException {
      return new SingletonStructureDataIterator(null);
    }
  }

  private static class SingletonStructureDataIterator implements StructureDataIterator {
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

  ////////////////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public FeatureType getFeatureType() {
    return featureType;
  }

  public List<? super VariableSimpleIF> getDataVariables() {
    return cols;
  }

  // LOOK others should override
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
    formatter.format("  Parent= %s\n", ((parent == null) ? "none" : parent.getName()));
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
    //String joinDesc = (join2parent == null) ? "" : "joinType=" + join2parent.getClass().toString();
//String dimDesc = (config.dim == null) ? "*" : config.dim.getName() + "=" + config.dim.getLength() + (config.dim.isUnlimited() ? " unlim" : "");
    f.format("\n%sTable %s: type=%s %s\n", s, getName(), getClass().toString(), ftDesc);
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
    if (v.equals(stnAlt)) return "[stnAlt]";
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
    gotSome |= showCoord(out, stnAlt, indent);
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
