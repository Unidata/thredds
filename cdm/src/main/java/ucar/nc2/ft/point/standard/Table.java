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
import ucar.nc2.ft.point.StructureDataIteratorIndexed;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

/**
 * A generalization of a Structure. Main function is to return a StructureDataIterator,
 *   iterating over its table rows
 *
 * @author caron
 * @since Jan 20, 2009
 */
public abstract class Table {

  public enum CoordName {
    Lat, Lon, Elev, Time, TimeNominal, StnId, StnDesc, WmoId, StnAlt
  }

  public enum Type {
    ArrayStructure, Construct, Contiguous, LinkedList, MultiDimInner, MultiDimOuter, MultiDimStructure,
    NestedStructure, ParentIndex, Singleton, Structure, Top
  }

  public static Table factory(NetcdfDataset ds, TableConfig config) {

    switch (config.type) {

      case ArrayStructure: // given array of StructureData
        return new TableArrayStructure(ds, config);

      case Construct: // construct the table from its children
        return new TableConstruct(ds, config);

      case Contiguous: // contiguous list of child record, using indexes
        return new TableContiguous(ds, config);

      case LinkedList: // linked list of child records, using indexes
        return new TableLinkedList(ds, config);

      case MultiDimInner: // inner struct of a multdim
        return new TableMultiDimInner(ds, config);

      case MultiDimOuter: // outer struct of a multdim
        return new TableMultiDimOuter(ds, config);

      case MultiDimStructure: // obs is a Structure
        return new TableMultiDimStructure(ds, config);

      case NestedStructure: // Structure or Sequence is nested in the parent
        return new TableNestedStructure(ds, config);

      case ParentIndex: // linked list of child records, using indexes
        return new TableParentIndex(ds, config);

      case Singleton: // singleton row, with given StructureData
        return new TableSingleton(ds, config);

      case Structure: // Structure or PsuedoStructure
        return new TableStructure(ds, config);

      case Top: // singleton consisting of top variables and constants
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

  /**
   * Iterate over the rows of this table. Subclasses must implement this.
   * @param cursor state of comlpete iteration. Table implementations may not modify.
   * @param bufferSize hit on how much memory (in bytes) can be used to buffer.
   * @return iterater over the rows of this table.
   * @throws IOException on read error
   */
  abstract public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException;

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
    boolean addIndex;

    TableStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);

      if (config.isPsuedoStructure) {
        this.dim = config.dim;
        assert dim != null;
        //Group parent = ds.findGroup(config.groupName); // will return rootGroup if null
        struct = new StructurePseudo(ds, dim.getGroup(), config.structName, config.dim);

      } else {
        struct = (Structure) ds.findVariable(config.structName);
        if (struct == null)
          throw new IllegalStateException("Cant find Structure " + config.structName);

        dim = struct.getDimension(0);
      }

      for (Variable v : struct.getVariables()) {
        // remove substructures
        if (v.getDataType() == DataType.STRUCTURE) {
          if (config.isPsuedoStructure)
            struct.removeMemberVariable( v);
        } else {
          this.cols.add(v);
        }
      }
    }

    @Override
    protected void showExtra(Formatter f) {
      f.format("    struct=%s, dim=%s pseudo=%s%n", struct.getNameAndDimensions(), dim.getName(),
              (struct instanceof StructurePseudo));
    }


    @Override
    public Variable findVariable(String axisName) {
      return struct.findVariable(axisName);
    }

    @Override
    public String showDimension() {
      return dim.getName();
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
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
    protected void showExtra(Formatter f) {
      f.format("    ArrayStruct=%s, dim=%s%n", new Section(as.getShape()), dim.getName());
    }

    @Override
    public String showDimension() {
      return dim.getName();
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
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

    @Override
    protected void showExtra(Formatter f) {
      f.format("    ArrayStruct=%s%n", struct.getNameAndDimensions());
    }

    public Variable findVariable(String axisName) {
      return struct.findVariable(axisName);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      return struct.getStructureIterator(bufferSize);
    }
  }

  ///////////////////////////////////////////////////////
  public static class TableContiguous extends TableStructure {
    private String start; // variable name holding the starting index in parent
    private String numRecords; // variable name holding the number of children in parent

    TableContiguous(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.start = config.start;
      this.numRecords = config.numRecords;
    }

    @Override
    protected void showExtra(Formatter f) {
      f.format("    start=%s, numRecords=%s%n", start, numRecords);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();
      int firstRecno = parentStruct.getScalarInt(start);
      int n = parentStruct.getScalarInt(numRecords);
      return new StructureDataIteratorLinked(struct, firstRecno, n, null);
    }
  }

  ///////////////////////////////////////////////////////
  public static class TableParentIndex extends TableStructure {
    private Map<Integer, List<Integer>> indexMap;
    private String parentIndexName;

    TableParentIndex(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.indexMap = config.indexMap;
      this.parentIndexName = config.parentIndex;
    }

    @Override
    protected void showExtra(Formatter f) {
      f.format("    parentIndexName=%s, indexMap.size=%d%n", parentIndexName, indexMap.size());
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();
      int parentIndex = parentStruct.getScalarInt( parentIndexName);
      List<Integer> index = indexMap.get( parentIndex);
      return new StructureDataIteratorIndexed(struct, index);
    }
  }

  ///////////////////////////////////////////////////////
  public static class TableLinkedList extends TableStructure {
    private String start; // variable name holding the starting index in parent
    private String next; // variable name holding the next index in child

    TableLinkedList(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.start = config.start;
      this.next = config.next;
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();
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
    protected void showExtra(Formatter f) {
      f.format("    StructureMembers=%s, dim=%s%n", sm.getName(), dim.getName());
    }

    @Override
    public String showDimension() {
      return dim.getName();
    }

    @Override
    public Variable findVariable(String axisName) {
      return ds.findVariable(axisName);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();
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
   public static class TableMultiDimStructure extends Table.TableStructure {

     TableMultiDimStructure(NetcdfDataset ds, TableConfig config) {
       super(ds, config);
     }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      int recnum = cursor.getParentRecnum();
      try {
        Section section = new Section().appendRange(recnum, recnum).appendRange(null);
        ArrayStructure data = (ArrayStructure) struct.read(section);
        return data.getStructureDataIterator();
      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e);
      }
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

    @Override
    protected void showExtra(Formatter f) {
      f.format("    struct=%s, nestedTableName=%s%n", struct.getNameAndDimensions(), nestedTableName);
    }

    public Variable findVariable(String axisName) {
      return struct.findVariable(axisName);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();

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

    @Override
    protected void showExtra(Formatter f) {
      f.format("    StructureData=%s%n", sdata.getName());
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      return new SingletonStructureDataIterator(sdata);
    }
  }

  public static class TableTop extends Table {
    NetcdfDataset ds;

    TableTop(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.ds = ds;
    }

    @Override
    protected void showExtra(Formatter f) {
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
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
    f.format("%n%sTable %s: type=%s %s%n", s, getName(), getClass().toString(), ftDesc);
    showExtra(f);
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

  protected abstract void showExtra(Formatter f);

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
