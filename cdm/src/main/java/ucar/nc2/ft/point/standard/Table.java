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
 * iterating over its table rows
 *
 * @author caron
 * @since Jan 20, 2009
 */
public abstract class Table {

  public enum CoordName {
    Lat, Lon, Elev, Time, TimeNominal, StnId, StnDesc, WmoId, StnAlt
  }

  public enum Type {
    ArrayStructure, Construct, Contiguous, LinkedList, MultiDimInner, MultiDimStructure, MultiDimStructurePsuedo,
    NestedStructure, ParentIndex, Singleton, Structure, Top
  }

  public static Table factory(NetcdfDataset ds, TableConfig config) {

    switch (config.type) {

      case ArrayStructure: // given array of StructureData, stored in config.as
        return new TableArrayStructure(ds, config);

      case Construct: // construct the table from its children - theres no seperate station table, stn info is duplicated in the obs structure.
        return new TableConstruct(ds, config);

      case Contiguous: // contiguous list of child record, using indexes
        return new TableContiguous(ds, config);

      case LinkedList: // linked list of child records, using indexes
        return new TableLinkedList(ds, config);

      case MultiDimInner: // inner struct of a multdim
        return new TableMultiDimInner(ds, config);

      case MultiDimStructure: // a multidim structure
        return new TableMultiDimStructure(ds, config);

      case MultiDimStructurePsuedo: // a multidim structure
        return new TableMultiDimStructurePsuedo(ds, config);

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
  List<Join> extraJoins;

  String lat, lon, elev, time, timeNominal;
  String stnId, stnDesc, stnNpts, stnWmoId, stnAlt, limit;
  //public int nstations;

  List<VariableSimpleIF> cols = new ArrayList<VariableSimpleIF>();    // all variables
  List<String> nondataVars = new ArrayList<String>(); // exclude these from the getDataVariables() list

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

    this.extraJoins = config.extraJoin;

    // try to exclude coordinate vars and "structural data" from the list of data variables
    checkNonDataVariable(config.lat);
    checkNonDataVariable(config.lon);
    checkNonDataVariable(config.elev);
    checkNonDataVariable(config.timeNominal);
    checkNonDataVariable(config.stnId);
    checkNonDataVariable(config.stnDesc);
    checkNonDataVariable(config.stnNpts);
    checkNonDataVariable(config.stnWmoId);
    checkNonDataVariable(config.stnAlt);
    checkNonDataVariable(config.limit);
  }

  protected void checkNonDataVariable(String name) {
    if (name != null)
      nondataVars.add(name);
  }

  /**
   * Iterate over the rows of this table. Subclasses must implement this.
   *
   * @param cursor     state of comlpete iteration. Table implementations may not modify.
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

  /**
   * A Structure or PsuedoStructure.
   * <p/>
   * PsuedoStructure defined by variables with outer dimension = config.dim
   * So we find all Variables with signature v(outDim, ...) and make them into
   * <p/>
   * Structure {
   * v1(...);
   * v2(...);
   * } s
   * <p/>
   * config.vars if not null restricts to list of vars, must be members.
   */
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
        if (config.vars != null)
          struct = new StructurePseudo(ds, dim.getGroup(), config.structName, config.vars, config.dim);
        else
          struct = new StructurePseudo(ds, dim.getGroup(), config.structName, config.dim);

      } else {
        struct = (Structure) ds.findVariable(config.structName);
        if (struct == null)
          throw new IllegalStateException("Cant find Structure " + config.structName);

        dim = struct.getDimension(0);

        if (config.vars != null) {
          struct = struct.select(config.vars); // limit to list of vars
        }
      }


      for (Variable v : struct.getVariables()) {
        // remove substructures
        if (v.getDataType() == DataType.STRUCTURE) {
          if (config.isPsuedoStructure)
            struct.removeMemberVariable(v);
        } else {
          this.cols.add(v);
        }
      }
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sstruct=%s, dim=%s pseudo=%s%n", indent, struct.getNameAndDimensions(), dim.getName(),
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

  /**
   * ArrayStructure is passed in config.as
   * Used by
   * UnidataPointFeature: type StationProfile  (deprecated)
   */
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
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sArrayStruct=%s, dim=%s%n", indent, new Section(as.getShape()), dim.getName());
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

  /**
   * When theres no seperate station table, but info is duplicated in the obs structure.
   * The name of the structure is in config.structName.
   * Just return the obs structure iterator, extraction is done elsewhere.
   * TableConstruct is the parent table, config.structName is the child table.
   * No variables are added to cols.
   * <p/>
   * Used by:
   * BufrCdm StationProfile type
   */
  public static class TableConstruct extends Table {
    Structure struct;

    TableConstruct(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      struct = (Structure) ds.findVariable(config.structName);
      if (struct == null)
        throw new IllegalStateException("Cant find Structure " + config.structName);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sArrayStruct=%s%n", indent, struct.getNameAndDimensions());
    }

    public Variable findVariable(String axisName) {
      return struct.findVariable(axisName);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      return struct.getStructureIterator(bufferSize);
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * Contiguous children, using start and numRecords variables in the parent.
   * TableContiguous is the children, config.struct describes the cols.
   * <p/>
   * Used by:
   * UnidataPointObs
   * CFPointObs
   */
  public static class TableContiguous extends TableStructure {
    private String start; // variable name holding the starting index in parent
    private String numRecords; // variable name holding the number of children in parent

    TableContiguous(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.start = config.start;
      this.numRecords = config.numRecords;

      checkNonDataVariable(config.start);
      checkNonDataVariable(config.numRecords);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sstart=%s, numRecords=%s%n", indent, start, numRecords);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();
      int firstRecno = parentStruct.getScalarInt(start);
      int n = parentStruct.getScalarInt(numRecords);
      return new StructureDataIteratorLinked(struct, firstRecno, n, null);
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * The children have a parentIndex, child -> parent.
   * For efficiency, we scan this data and construct an IndexMap( parentIndex -> list of children),
   * i.e. we compute the inverse link, parent -> children.
   * TableParentIndex is the children, config.struct describes the cols.
   * <p/>
   * Used by:
   * CFPointObs
   */
  public static class TableParentIndex extends TableStructure {
    private Map<Integer, List<Integer>> indexMap;
    private String parentIndexName;

    TableParentIndex(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.indexMap = config.indexMap;
      this.parentIndexName = config.parentIndex;

      checkNonDataVariable(config.parentIndex);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sparentIndexName=%s, indexMap.size=%d%n", indent, parentIndexName, indexMap.size());
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();
      int parentIndex = parentStruct.getScalarInt(parentIndexName);
      List<Integer> index = indexMap.get(parentIndex);
      return new StructureDataIteratorIndexed(struct, index);
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * Linked list of children, using start variable in the parent, and next in the child.
   * TableLinkedList is the children, config.struct describes the cols.
   * <p/>
   * Used by:
   * UnidataPointObs
   */
  public static class TableLinkedList extends TableStructure {
    private String start; // variable name holding the starting index in parent
    private String next; // variable name holding the next index in child

    TableLinkedList(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.start = config.start;
      this.next = config.next;

      checkNonDataVariable(config.start);
      checkNonDataVariable(config.next);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();
      int firstRecno = parentStruct.getScalarInt(start);
      return new StructureDataIteratorLinked(struct, firstRecno, -1, next);
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * A collection of Multdimensional Variables:
   * <p/>
   * Variable stn(outDim)
   * Variable v1(outDim, innerDim, ...)
   * Variable v2(outDim, innerDim)
   * <p/>
   * can be thought of as a structure:
   * <p/>
   * Structure {
   * stn;
   * v1(innerDim, ...)
   * v2(innerDim);
   * } so(outerDim);
   * <p/>
   * and as nested structures:
   * <p/>
   * Structure {
   * stn;
   * Structure {
   * v1(...),
   * v2
   * } si(innerDim);
   * } so(outerDim);
   * <p/>
   * 1) When outerDim is the record variable, (ie it really is a structure) it makes sense to read the entire record at once:
   * <p/>
   * Structure {
   * v1(innerDim, ...)
   * v2(innerDim);
   * stn;
   * } so(outerDim);
   * <p/>
   * and return the StructureData with the inner variables removed:
   * <p/>
   * StructureData {
   * stn1;
   * stn2
   * } so(outerDim);
   * <p/>
   * LOOK (This may be hard, when is subset done ?? since inner need access to other members)
   * <p/>
   * And for the inner iterator, given the original StructureData for outerDim=fixed
   * <p/>
   * StructureData {
   * v1(innerDim, ...)
   * v2(innerDim);
   * stn;
   * } so(outerDim=fixed);
   * <p/>
   * rearrange it into an ArrayStructure:
   * <p/>
   * ArrayStructure(innerDim) {
   * StructureData {
   * v1(...);
   * v2;
   * }
   * }
   * <p/>
   * Use Table types MultdimOuter, MultidimInner for this case
   * <p/>
   * 2) When its not, it makes sense to read the outer variables seperately:
   * <p/>
   * outer iterator is over outDim
   * <p/>
   * Variable stn1(outDim)
   * Variable stn2(outDim)
   * <p/>
   * inner iterator over innerDim:
   * <p/>
   * Variable v1(outDim=fixed, innerDim, ...)
   * Variable v2(outDim=fixed, innerDim)
   * <p/>
   * Use Table types Structure(psuedo, with vars set), TableMultiDimStructure (psuedo) for this case
   */

  /* where
  *   config.outer = outerDim
  *   config.dim = innerDim
  *
  *  So we find all Variables with signature v(outDim, innerDim, ...) and make them into
  *
  *  Structure {
  *    v1(...);
  *    v2(...);
  *  } si
  *
  * The parent table is the PsuedoStructure:
  *   Structure {
  *     v1(innerDim, ...);
  *     v2(innerDim, ...);
  *  } parent(outDim)
  *
  * and the parent StructureData is passed into getStructureDataIterator():
  *   StructureData {
  *    v1(innerDim, ...);
  *    v2(innerDim, ...);
  *  } s
  *
  * So we just rearrange this into an ArrayStructure:
  *    ArrayStructure(innerDim) {
  *      v1(...);
  *      v2(...);
  *    }
  *
  * and return the iterator over it.
  *
  * Used by:
  *   FSLWindProfiler
  *   GempakCdm
  *   Iridl
  *   UnidataPointObs
  *
  */
  public static class TableMultiDimInner extends Table {
    StructureMembers sm; // the inner structure members
    Dimension dim;
    NetcdfDataset ds;

    TableMultiDimInner(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.ds = ds;
      this.dim = config.dim;
      assert dim != null;

      sm = new StructureMembers(config.name);
      if (config.vars != null) {
        for (String name : config.vars) {
          Variable v = ds.findVariable(name);
          if (v == null) continue;
          cols.add(v);
          int rank = v.getRank();
          int[] shape = new int[rank - 2];
          System.arraycopy(v.getShape(), 2, shape, 0, rank - 2);
          sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
        }

      } else {
        for (Variable v : ds.getVariables()) {
          if (v.getRank() < 2) continue;
          if (v.getDimension(0).equals(config.outer) && v.getDimension(1).equals(config.dim)) {
            cols.add(v);
            int rank = v.getRank();
            int[] shape = new int[rank - 2];
            System.arraycopy(v.getShape(), 2, shape, 0, rank - 2);
            sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
          }
        }

      }
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sStructureMembers=%s, dim=%s%n", indent, sm.getName(), dim.getName());
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

  /**
   * Used for Structure(station, time).
   * This is used for the inner table.
   * <p/>
   * Used by:
   * GempakCdm
   */
  public static class TableMultiDimStructure extends Table.TableStructure {

    TableMultiDimStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      int recnum = cursor.getParentRecnum();
      try {
        Section section = new Section().appendRange(recnum, recnum);
        int count = 1;
        while (count++ < struct.getRank()) // handles multidim case 
          section.appendRange(null);
        ArrayStructure data = (ArrayStructure) struct.read(section);
        return data.getStructureDataIterator();
      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e);
      }
    }

  }

  /**
   * Used for PsuedoStructure(station, time).
   * This is used for the inner table.
   * <p/>
   * Used by:
   * CFpointObs
   */
  public static class TableMultiDimStructurePsuedo extends Table.TableStructure {
    Dimension inner;
    StructureMembers sm;

    TableMultiDimStructurePsuedo(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.inner = config.inner;
      assert inner != null;

      sm = new StructureMembers(config.name);
      for (Variable v : struct.getVariables()) {
        int rank = v.getRank();
        int[] shape = new int[rank - 1];
        System.arraycopy(v.getShape(), 1, shape, 0, rank - 1);
        sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
      }
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      int recnum = cursor.getParentRecnum();

      try {
        StructureData parentStruct = struct.readStructure(recnum);
        ArrayStructureMA asma = new ArrayStructureMA(sm, new int[]{inner.getLength()});
        for (VariableSimpleIF v : cols) {
          Array data = parentStruct.getArray(v.getShortName());
          StructureMembers.Member childm = sm.findMember(v.getShortName());
          childm.setDataArray(data);
        }
        return asma.getStructureDataIterator();

      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e);
      }
    }

  }


  ///////////////////////////////////////////////////////

  /**
   * A Structure inside of a parent Structure.
   * Name of child member inside parent Structure is config.nestedTableName
   * <p/>
   * Used by:
   * BufrCdm
   */
  public static class TableNestedStructure extends Table {
    String nestedTableName; // short name of structure
    Structure struct;

    TableNestedStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.nestedTableName = config.nestedTableName;
      struct = (Structure) ds.findVariable(config.structName);
      assert (struct != null);

      for (Variable v : struct.getVariables())
        cols.add(v);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sstruct=%s, nestedTableName=%s%n", indent, struct.getNameAndDimensions(), nestedTableName);
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

  /**
   * Table is a single StructureData, passed in as config.sdata.
   * <p/>
   * Used by:
   * Cosmic
   */
  public static class TableSingleton extends Table {
    StructureData sdata;

    TableSingleton(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.sdata = config.sdata;
      assert (this.sdata != null);

      for (StructureMembers.Member m : sdata.getStructureMembers().getMembers())
        cols.add(new VariableSimpleAdapter(m));
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sStructureData=%s%n", indent, sdata.getName());
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      return new SingletonStructureDataIterator(sdata);
    }
  }

  /**
   * Table is a single StructureData, which is empty.
   * NestedTable looks for instance of this, and
   * 1) increments the nesting level
   * 2) looks for coordinat variables at the top level.
   * <p/>
   * Essentialy adds a table at top of the tree, constisting only of coordinate variables
   * <p/>
   * Used by:
   * CFpointObs
   * GempakCdm
   * Ndbc
   */
  public static class TableTop extends Table {
    NetcdfDataset ds;

    TableTop(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.ds = ds;
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
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

    public void finish() {
    }
  }

  ////////////////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public FeatureType getFeatureType() {
    return featureType;
  }

  /* public List<? super VariableSimpleIF> getDataVariables() {
    return cols;
  } */

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
    if (extraJoins != null) {
      f.format("  %sExtraJoins:\n", s);
      for (Join j : extraJoins)
        f.format("   %s  %s \n", s, j);
    }
    showTableExtraInfo(indent(indent + 2), f);
    showCoords(s, f);
    f.format("  %sVariables:\n", s);
    for (VariableSimpleIF v : cols) {
      f.format("   %s  %s %s\n", s, v.getName(), getKind(v.getShortName()));
    }
    return indent + 2;
  }

  String indent(int n) {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < n; i++) sbuff.append(' ');
    return sbuff.toString();
  }

  protected abstract void showTableExtraInfo(String indent, Formatter f);

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

  private void showCoords(String indent, Formatter out) {
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
