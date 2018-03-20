/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArraySequence;
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureMA;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureDataIteratorMediated;
import ucar.ma2.StructureDataMediator;
import ucar.ma2.StructureDataProxy;
import ucar.ma2.StructureDataW;
import ucar.ma2.StructureMembers;
import ucar.nc2.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.dataset.StructurePseudo2Dim;
import ucar.nc2.dataset.StructurePseudoDS;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.ft.point.StructureDataIteratorIndexed;
import ucar.nc2.ft.point.StructureDataIteratorLinked;

/**
 * A generalization of a Structure. Main function is to return a StructureDataIterator,
 * iterating over its table rows
 *
 * @author caron
 * @since Jan 20, 2009
 */
public abstract class Table {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Table.class);

  public enum CoordName {
    Lat, Lon, Elev, Time, TimeNominal, StnId, StnDesc, WmoId, StnAlt, FeatureId, MissingVar
  }

  public enum Type {
    ArrayStructure, Construct, Contiguous, LinkedList,
    MultidimInner, MultidimInner3D, MultidimInnerPsuedo, MultidimInnerPsuedo3D, MultidimStructure,
    NestedStructure, ParentId, ParentIndex, Singleton, Structure, Top
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

      case MultidimInner: // the inner struct of a 2D multdim(outer, inner) with unlimited dimension
        return new TableMultidimInner(ds, config);

      case MultidimInner3D: // the inner struct of a 3D multdim(outer, middle, inner) with unlimited dimension
        return new TableMultidimInner3D(ds, config);

      case MultidimStructure: // the outer struct of a multidim structure
        return new TableMultidimStructure(ds, config);

      case MultidimInnerPsuedo: // the inner struct of a 2D multdim(outer, inner) without the unlimited dimension
        // the middle struct of a 3D multdim(outer, middle, inner) without the unlimited dimension
        return new TableMultidimInnerPsuedo(ds, config);

      case MultidimInnerPsuedo3D: // the inner struct of a 3D multdim(outer, middle, inner) without the unlimited dimension
        return new TableMultidimInnerPsuedo3D(ds, config);

      case NestedStructure: // Structure or Sequence is nested in the parent
        return new TableNestedStructure(ds, config);

      case ParentId: // child record has an id for the parent.
        return new TableParentId(ds, config);

      case ParentIndex: // child record has the record index of the parent.
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

  Table parent, child;
  List<Join> extraJoins;

  String lat, lon, elev, time, timeNominal;
  String stnId, stnDesc, stnNpts, stnWmoId, stnAlt, limit;
  String feature_id, missingVar;

  Map<String, VariableSimpleIF> cols = new HashMap<>();  // all variables
  Set<String> nondataVars = new HashSet<>();          // exclude these from the getDataVariables() list

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
    this.feature_id = config.feature_id;
    this.missingVar = config.missingVar;

    if (config.parent != null) {
      parent = Table.factory(ds, config.parent);
      parent.child = this;
    }

    this.extraJoins = config.extraJoin;

    // try to exclude coordinate vars and "structural data" from the list of data variables
    addNonDataVariable(config.time);
    addNonDataVariable(config.lat);
    addNonDataVariable(config.lon);
    addNonDataVariable(config.elev);
    addNonDataVariable(config.timeNominal);
    addNonDataVariable(config.stnId);
    addNonDataVariable(config.stnDesc);
    addNonDataVariable(config.stnWmoId);
    addNonDataVariable(config.stnAlt);
    addNonDataVariable(config.stnNpts);
    addNonDataVariable(config.limit);
    addNonDataVariable(config.feature_id);

    addNonDataVariable(config.parentIndex);
    addNonDataVariable(config.start);
    addNonDataVariable(config.next);
    addNonDataVariable(config.numRecords);
  }

  protected void addNonDataVariable(String name) {
    if (name != null)
      nondataVars.add(name);
  }

  // change shape of the data variables
  protected void replaceDataVars(StructureMembers sm) {
    for (StructureMembers.Member m : sm.getMembers()) {
      VariableSimpleIF org = this.cols.get(m.getName());
      int rank = org.getRank();
      List<Dimension> orgDims = org.getDimensions();
      // only keep the last n
      int n = m.getShape().length;
      List<Dimension> dims = orgDims.subList(rank-n, rank);
      VariableSimpleImpl result = new VariableSimpleImpl(org.getShortName(), org.getDescription(), org.getUnitsString(), org.getDataType(), dims);
      for (Attribute att : org.getAttributes()) result.add(att);
      this.cols.put(m.getName(), result);
    }
  }

  /**
   * Iterate over the rows of this table. Subclasses must implement this.
   *
   * @param cursor     state of comlpete iteration. Table implementations may not modify.
   * @return iterater over the rows of this table.
   * @throws IOException on read error
   */
  abstract public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException;

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

      case FeatureId:
        return feature_id;

      case MissingVar:
        return missingVar;

    }
    return null;
  }

  ///////////////////////////////////////////////////////

  /**
   * A Structure, PsuedoStructure, or Sequence.
   * <p>
   * Structure: defined by config.structName.
   * if config.vars if not null restricts to list of vars, must be members.
   * <p>
   * PsuedoStructure: defined by variables with outer dimension = config.dim
   * So we find all Variables with signature v(outDim, ...) and make them into
   * <pre>
   * Structure {
   *   v1(...);
   *   v2(...);
   * } s
   * </pre>
   * config.vars if not null restricts to list of vars, must be members.
   */
  public static class TableStructure extends Table {
    StructureDS struct;
    Dimension dim, outer;
    TableConfig.StructureType stype;

    TableStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.stype = config.structureType;

      switch (config.structureType) {

        case Structure:
          struct = (StructureDS) ds.findVariable(config.structName);
          if (struct == null)
            throw new IllegalStateException("Cant find Structure " + config.structName);

          dim = struct.getDimension(0);

          if (config.vars != null)
            struct = (StructureDS) struct.select(config.vars); // limit to list of vars
          break;

        case PsuedoStructure:
          this.dim = ds.findDimension(config.dimName);
          assert dim != null;
          String name = config.structName == null ? "anon" : config.structName;
          struct = new StructurePseudoDS(ds, dim.getGroup(), name, config.vars, this.dim);
          break;

        case PsuedoStructure2D:
          this.dim = ds.findDimension(config.dimName);
          this.outer = ds.findDimension(config.outerName);
          assert dim != null;
          assert config.outerName != null;
          struct = new StructurePseudo2Dim(ds, dim.getGroup(), config.structName, config.vars, this.dim, this.outer);
          break;
      }

      config.vars = new ArrayList<>();
      for (Variable v : struct.getVariables()) {
        // remove substructures
        if (v.getDataType() == DataType.STRUCTURE) {
          if (config.structureType == TableConfig.StructureType.PsuedoStructure)
            struct.removeMemberVariable(v);
        } else {
          this.cols.put(v.getShortName(), v);
          config.vars.add(v.getShortName());
        }
      }
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sstruct=%s, dim=%s type=%s%n", indent, struct.getNameAndDimensions(), dim.getShortName(), struct.getClass().getName());
    }

    @Override
    public VariableDS findVariable(String axisName) {
      String structPrefix = struct.getShortName() + ".";
      if (axisName.startsWith(structPrefix))
        axisName = axisName.substring(structPrefix.length());
      return (VariableDS) struct.findVariable(axisName);
    }

    @Override
    public String showDimension() {
      return dim.getShortName();
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      return new StructureDataIteratorMediated(struct.getStructureIterator(), new RestrictToColumns());
    }

    @Override
    public String getName() {
      return stype.toString() + "(" + struct.getShortName() + ")";
    }
  }

  private class RestrictToColumns implements StructureDataMediator {
    StructureMembers members;

    @Override
    public StructureData modify(StructureData sdata) {
      // make members restricted to column names
      if (members == null) {
        StructureMembers orgMembers = sdata.getStructureMembers();
        members = new StructureMembers(orgMembers.getName() + "RestrictToColumns");
        for (String colName : cols.keySet()) {
          StructureMembers.Member m = orgMembers.findMember(colName);
          if (m == null)
            throw new IllegalStateException("Cant find " + colName);
          members.addMember(m);
        }
      }
      return new StructureDataProxy(members, sdata);
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * ArrayStructure is passed in config.as
   * Used by
   * UnidataPointFeature: type StationProfile  (removed now)
   */
  public static class TableArrayStructure extends Table {
    ArrayStructure as;
    Dimension dim;

    TableArrayStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      assert (config.as != null);
      this.as = config.as;
      this.dim = new Dimension(config.structName, (int) config.as.getSize(), false);

      for (StructureMembers.Member m : config.as.getStructureMembers().getMembers())
        cols.put(m.getName(), new VariableSimpleAdapter(m));
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sArrayStruct=%s, dim=%s%n", indent, new Section(as.getShape()), dim.getShortName());
    }

    @Override
    public String showDimension() {
      return dim.getShortName();
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      return as.getStructureDataIterator();
    }

    @Override
    public String getName() {
      return "ArrayStructure(" + name + ")";
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * When theres no seperate station table, but info is duplicated in the obs structure.
   * Must have a ParentId child table
   * No variables are added to cols.
   * <p>
   * Used by:
   * BufrCdm StationProfile type
   */
  public static class TableConstruct extends Table {
    ArrayStructure as; // injected by TableParentId

    TableConstruct(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      return as.getStructureDataIterator();
    }

    @Override
    public String getName() {
      return "Constructed";
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * Contiguous children, using start and numRecords variables in the parent. This assumes column store.
   * TableContiguous is the children, config.struct describes the cols.
   * <p>
   * Used by:
   * UnidataPointObs
   * CFPointObs
   */
  public static class TableContiguous extends TableStructure {
    private String startVarName; // variable name holding the starting index in parent
    private String numRecordsVarName; // variable name holding the number of children in parent
    private int[] startIndex, numRecords;
    private NetcdfDataset ds;
    private boolean isInit;

    TableContiguous(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.ds = ds;
      startVarName = config.getStart();
      numRecordsVarName = config.getNumRecords();

      addNonDataVariable(startVarName);
      addNonDataVariable(numRecordsVarName);
    }

    private void init() {
      if (startVarName == null) {  // read numRecords when startVar is not known  LOOK this should be deffered
        try {
          Variable v = ds.findVariable(numRecordsVarName);
          Array numRecords = v.read();
          int n = (int) numRecords.getSize();

          // construct the start variable
          this.numRecords = new int[n];
          this.startIndex = new int[n];
          int i = 0;
          int count = 0;
          while (numRecords.hasNext()) {
            this.startIndex[i] = count;
            this.numRecords[i] = numRecords.nextInt();
            count += this.numRecords[i];
            i++;
          }
          isInit = true;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sstart=%s, numRecords=%s%n", indent, startVarName, numRecordsVarName);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      if (!isInit) init();

      int firstRecno, numrecs;
      StructureData parentStruct = cursor.getParentStructure();
      if (startIndex != null) {
        int parentIndex = cursor.getParentRecnum();
        firstRecno = startIndex[parentIndex];
        numrecs = numRecords[parentIndex];
      } else {
        firstRecno = parentStruct.getScalarInt(startVarName);
        numrecs = parentStruct.getScalarInt(numRecordsVarName);
      }
      return new StructureDataIteratorLinked(struct, firstRecno, numrecs, null);
    }

    @Override
    public String getName() {
      return "Contig(" + numRecordsVarName + ")";
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * The children have a field containing the index of the parent.
   * For efficiency, we scan this data and construct an IndexMap( parentIndex -> list of children),
   * i.e. we compute the inverse link, parent -> children.
   * TableParentIndex is the children, config.struct describes the cols.
   * <p>
   * Used by:
   * CFPointObs
   */
  public static class TableParentIndex extends TableStructure {
    private Map<Integer, List<Integer>> indexMap;
    private String parentIndexName;

    TableParentIndex(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.parentIndexName = config.parentIndex;

      // construct the map
      try {
        Variable rpIndex = ds.findVariable(config.parentIndex);
        Array index = rpIndex.read();

        int childIndex = 0;
        this.indexMap = new HashMap<>((int) (2 * index.getSize()));
        while (index.hasNext()) {
          int parent = index.nextInt();
          List<Integer> list = indexMap.get(parent);
          if (list == null) {
            list = new ArrayList<>();
            indexMap.put(parent, list);
          }
          list.add(childIndex);
          childIndex++;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      addNonDataVariable(config.parentIndex);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sparentIndexName=%s, indexMap.size=%d%n", indent, parentIndexName, indexMap.size());
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      int parentIndex = cursor.getParentRecnum();
      List<Integer> index = indexMap.get(parentIndex);
      if (index == null) index = new ArrayList<>();
      return new StructureDataIteratorIndexed(struct, index);
    }

    @Override
    public String getName() {
      return "Indexed(" + parentIndexName + ")";
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * The children have a field containing the id of the parent.
   * For efficiency, we scan this data and construct an IndexMap( parentIndex -> list of children),
   * i.e. we compute the inverse link, parent -> children.
   * TableParentIndex is the children, config.struct describes the cols.
   * <p>
   * Used by:
   * CFPointObs
   */
  public static class TableParentId extends TableStructure {
    private ParentInfo[] indexMap;
    private String parentIdName;

    TableParentId(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.parentIdName = config.parentIndex;

      // construct the hash of unique parents, based on the id variable
      Map<Object, ParentInfo> parentHash;
      try {
        Variable rpIndex = ds.findVariable(parentIdName);
        if (rpIndex == null)
          rpIndex = struct.findVariable(parentIdName);

        Array index = rpIndex.read();
        if (index instanceof ArrayChar)
          index = ((ArrayChar) index).make1DStringArray();

        parentHash = new HashMap<>((int) (2 * index.getSize()));

        int childIndex = 0;
        while (index.hasNext()) {
          Object parent = index.next();
          ParentInfo info = parentHash.get(parent);
          if (info == null) {
            info = new ParentInfo();
            parentHash.put(parent, info);
          }
          info.add(childIndex);
          childIndex++;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      // construct the indexMap and the ArrayStructure containing StructureData for the parents
      Collection<ParentInfo> parents = parentHash.values();
      int n = parents.size();
      this.indexMap = new ParentInfo[n];
      StructureData[] parentData = new StructureData[n];
      int count = 0;
      for (ParentInfo info : parents) {
        this.indexMap[count] = info;
        parentData[count++] = info.sdata;
      }
      ArrayStructure as = new ArrayStructureW(struct.makeStructureMembers(), new int[]{n}, parentData);

      // find the parent TableConstruct and inject the ArrayStructure
      Table t = this;
      while (t.parent != null) {
        t = t.parent;
        if (t instanceof TableConstruct) {
          ((TableConstruct) t).as = as;
          break;
        }
      }
      addNonDataVariable(parentIdName);
    }

    private class ParentInfo {
      List<Integer> recnumList = new ArrayList<>();
      StructureData sdata;

      void add(int recnum) throws IOException {
        recnumList.add(recnum);
        if (sdata != null) return;
        try {
          sdata = struct.readStructure(recnum);
        } catch (ucar.ma2.InvalidRangeException e) {
          log.error("TableParentId read recno=" + recnum, e);
          throw new RuntimeException(e.getMessage());
        }
      }
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sparentIdName=%s, indexMap.size=%d%n", indent, parentIdName, indexMap.length);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      int parentIndex = cursor.getParentRecnum();
      ParentInfo info = indexMap[parentIndex];
      List<Integer> index = (info == null) ? new ArrayList<>() : info.recnumList;
      return new StructureDataIteratorIndexed(struct, index);
    }

    @Override
    public String getName() {
      return "ParentId(" + parentIdName + ")";
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * Linked list of children, using start variable in the parent, and next in the child.
   * TableLinkedList is the children, config.struct describes the cols.
   * <p>
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

      addNonDataVariable(config.start);
      addNonDataVariable(config.next);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();
      int firstRecno = parentStruct.getScalarInt(start);
      return new StructureDataIteratorLinked(struct, firstRecno, -1, next);
    }

    @Override
    public String getName() {
      return "Linked(" + start + "->" + next + ")";
    }
  }

  ///////////////////////////////////////////////////////

  // the inner struct of a 2D multdim(outer, inner) with unlimited dimension
  public static class TableMultidimInner extends Table {
    StructureMembers sm; // the inner structure members
    Dimension inner, outer;
    NetcdfDataset ds;

    TableMultidimInner(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.ds = ds;
      assert config.outerName != null;
      assert config.innerName != null;
      this.inner = ds.findDimension(config.innerName);
      this.outer = ds.findDimension(config.outerName);
      assert this.inner != null : config.innerName;
      assert this.outer != null : config.outerName;

      sm = new StructureMembers(config.name);
      if (config.vars != null) {
        for (String name : config.vars) {
          Variable v = ds.findVariable(name);
          if (v == null) continue;
          // cols.add(v);
          int rank = v.getRank();
          int[] shape = new int[rank - 2];
          System.arraycopy(v.getShape(), 2, shape, 0, rank - 2);
          sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
          this.cols.put(v.getShortName(), v);
        }

      } else {
        for (Variable v : ds.getVariables()) {
          if (v.getRank() < 2) continue;
          if (v.getDimension(0).equals(this.outer) && v.getDimension(1).equals(this.inner)) {
            // cols.add(v);
            int rank = v.getRank();
            int[] shape = new int[rank - 2];
            System.arraycopy(v.getShape(), 2, shape, 0, rank - 2);
            sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
            this.cols.put(v.getShortName(), v);
          }
        }
      }

      replaceDataVars(sm);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sStructureMembers=%s, dim=%s,%s%n", indent, sm.getName(), outer.getShortName(), inner.getShortName());
    }

    @Override
    public String showDimension() {
      return inner.getShortName();
    }

    @Override
    public VariableDS findVariable(String axisName) {
      return (VariableDS) ds.findVariable(axisName);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();
      if (parentStruct instanceof StructureDataProxy)
        parentStruct = ((StructureDataProxy) parentStruct).getOriginalStructureData(); // tricky dicky
      ArrayStructureMA asma = new ArrayStructureMA(sm, new int[]{inner.getLength()});
      for (String colName : cols.keySet()) {
        Array data = parentStruct.getArray(colName);
        StructureMembers.Member childm = sm.findMember(colName);
        childm.setDataArray(data);
      }
      return asma.getStructureDataIterator();
    }

    @Override
    public String getName() {
      return "Multidim(" + outer.getShortName() + "," + inner.getShortName() + ")";
    }
  }

  public static class TableMultidimInner3D extends Table {
    StructureMembers sm; // the inner structure members
    Dimension dim, inner, middle;
    NetcdfDataset ds;

    TableMultidimInner3D(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.ds = ds;
      assert config.dimName != null;
      assert config.outerName != null;
      assert config.innerName != null;
      this.dim = ds.findDimension(config.dimName);
      this.inner = ds.findDimension(config.innerName);
      this.middle = ds.findDimension(config.outerName);

      sm = new StructureMembers(config.name);
      if (config.vars != null) {
        for (String name : config.vars) {
          Variable v = ds.findVariable(name);
          if (v == null) continue;
          //cols.add(v);
          int rank = v.getRank();
          int[] shape = new int[rank - 3];
          System.arraycopy(v.getShape(), 3, shape, 0, rank - 3);
          sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
          this.cols.put(v.getShortName(), v);
        }

      } else {
        for (Variable v : ds.getVariables()) {
          if (v.getRank() < 3) continue;
          if (v.getDimension(0).equals(dim) && v.getDimension(1).equals(middle) && v.getDimension(2).equals(inner)) {
            //cols.add(v);
            int rank = v.getRank();
            int[] shape = new int[rank - 3];
            System.arraycopy(v.getShape(), 3, shape, 0, rank - 3);
            sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
            this.cols.put(v.getShortName(), v);
          }
        }
      }

      replaceDataVars(sm);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sStructureMembers=%s, dim=%s%n", indent, sm.getName(), dim.getShortName());
    }

    @Override
    public String showDimension() {
      return dim.getShortName();
    }

    @Override
    public VariableDS findVariable(String axisName) {
      return (VariableDS) ds.findVariable(axisName);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      StructureData parentStruct = cursor.tableData[2];
      if (parentStruct instanceof StructureDataProxy)
        parentStruct = ((StructureDataProxy) parentStruct).getOriginalStructureData(); // tricky dicky
      int middleIndex = cursor.recnum[1];
      ArrayStructureMA asma = new ArrayStructureMA(sm, new int[]{inner.getLength()});
      for (String colName : cols.keySet()) {
        Array data = parentStruct.getArray(colName);
        Array myData = data.slice(0, middleIndex);
        StructureMembers.Member childm = sm.findMember(colName);
        childm.setDataArray(myData.copy());           // must make copy - ArrayStucture doesnt deal with logical views
      }
      return asma.getStructureDataIterator();
    }

    @Override
    public String getName() {
      return "Multidim(" + dim.getShortName() + "," + middle.getShortName() + "," + inner.getShortName() + ")";
    }

  }

  /**
   * Used for PsuedoStructure(station, time).
   * This is used for the inner table.
   * Need: config.inner, config.outer = config.dim, config.vars
   * <p>
   * Used by:
   * CFpointObs
   */
  public static class TableMultidimInnerPsuedo extends Table.TableStructure {
    Dimension inner, outer;
    StructureMembers sm;

    TableMultidimInnerPsuedo(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      assert config.outerName != null;
      assert config.innerName != null;
      this.inner = ds.findDimension(config.innerName);
      this.outer = ds.findDimension(config.outerName);

      sm = new StructureMembers(config.name);
      for (Variable v : struct.getVariables()) {
        int rank = v.getRank();
        int[] shape = new int[rank - 1];
        System.arraycopy(v.getShape(), 1, shape, 0, rank - 1);
        sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
      }

      replaceDataVars(sm);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      int recnum = cursor.recnum[ cursor.currentIndex]; // LOOK
      try {
        StructureData parentStruct = struct.readStructure(recnum);
        ArrayStructureMA asma = new ArrayStructureMA(sm, new int[]{inner.getLength()});
        for (String colName : cols.keySet()) {
          Array data = parentStruct.getArray(colName);
          StructureMembers.Member childm = sm.findMember(colName);
          childm.setDataArray(data);
        }
        return asma.getStructureDataIterator();

      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public String getName() {
      return "MultidimPseudo(" + outer.getShortName() + "," + inner.getShortName() + ")";
    }

  }

  public static class TableMultidimInnerPsuedo3D extends Table.TableStructure {
    Dimension middle;
    Dimension inner;
    StructureMembers sm;

    TableMultidimInnerPsuedo3D(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      assert config.dimName != null;
      assert config.outerName != null; // middle
      assert config.innerName != null;
      this.dim = ds.findDimension(config.dimName);
      this.middle = ds.findDimension(config.outerName);
      this.inner = ds.findDimension(config.innerName);

      sm = new StructureMembers(config.name);
      for (Variable v : struct.getVariables()) {
        int rank = v.getRank();
        int[] shape = new int[rank - 1];
        System.arraycopy(v.getShape(), 1, shape, 0, rank - 1);
        sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
      }

      replaceDataVars(sm);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      int outerIndex = cursor.recnum[2];
      int middleIndex = cursor.recnum[1];
      try {
        Section s = new Section().appendRange(outerIndex, outerIndex).appendRange(middleIndex, middleIndex);
        ArrayStructure result = (ArrayStructure) struct.read(s);
        assert result.getSize() == 1;
        StructureData sdata = result.getStructureData(0); // should only be one
        ArrayStructureMA asma = new ArrayStructureMA(sm, new int[]{inner.getLength()});
        for (String colName : cols.keySet()) {
          Array data = sdata.getArray(colName);
          StructureMembers.Member childm = sm.findMember(colName);
          childm.setDataArray(data);
        }
        return asma.getStructureDataIterator();

      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public String getName() {
      return "MultidimPsuedo(" + dim.getShortName() + "," + middle.getShortName() + "," + inner.getShortName() + ")";
    }

  }

  ///////////////////////////////////////////////////////

  /**
   * Used for Structure(station, time).
   * This is used for the inner table, where the station index gets set, and all the structures for that
   * styation are read in at once. Then we just iterate over that ArrayStructure.
   * <p>
   * Used by:
   * GempakCdm
   */
  public static class TableMultidimStructure extends Table.TableStructure {

    TableMultidimStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      int recnum = cursor.getParentRecnum();
      try {
        Section section = new Section().appendRange(recnum, recnum);
        int count = 1;
        while (count++ < struct.getRank()) // handles multidim case 
          section.appendRange(null);
        ArrayStructure data = (ArrayStructure) struct.read(section); // read all the data for a fixed outer index
        return data.getStructureDataIterator();
      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public String getName() {
      return "MultidimStructure(" + struct.getFullName() + ")";
    }

  }


  ///////////////////////////////////////////////////////

  /**
   * A Structure inside of a parent Structure.
   * Name of child member inside parent Structure is config.nestedTableName
   * obsTable.structName = obsStruct.getName();
   * obsTable.nestedTableName = obsStruct.getShortName();
   * <p>
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
      assert (struct != null) : config.structName;

      for (Variable v : struct.getVariables())
        cols.put(v.getShortName(), v);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sstruct=%s, nestedTableName=%s%n", indent, struct.getNameAndDimensions(), nestedTableName);
    }

    @Override
    public VariableDS findVariable(String axisName) {
      return (VariableDS) struct.findVariable(axisName);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
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

      throw new IllegalStateException("Cant find nested table member = " + nestedTableName);
    }

    @Override
    public String getName() {
      return "NestedStructure(" + nestedTableName + ")";
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * Table is a single StructureData, passed in as config.sdata.
   * Ok for sdata to be null
   * <p>
   * Used by:
   * FslWindProfiler
   */
  public static class TableSingleton extends Table {
    StructureData sdata;

    TableSingleton(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.sdata = config.sdata;
      if (sdata == null) sdata = StructureData.EMPTY;

      for (StructureMembers.Member m : sdata.getStructureMembers().getMembers())
        cols.put(m.getName(), new VariableSimpleAdapter(m));
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sStructureData=%s%n", indent, sdata);
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      return new SingletonStructureDataIterator(sdata);
    }

    @Override
    public String getName() {
      return "Singleton";
    }
  }

  /**
   * Table is a single StructureData, which is empty.
   * NestedTable looks for instance of this, and
   * 1) increments the nesting level
   * 2) looks for coordinate variables at the top level.
   * <p>
   * Adds a table at top of the tree, consisting only of coordinate variables
   * <p>
   * Used by:
   * CFpointObs
   * GempakCdm
   * Ndbc
   */
  public static class TableTop extends Table {
    NetcdfDataset ds;
    StructureDataTop sdata;

    TableTop(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.ds = ds;
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
    }

    @Override
    public StructureDataIterator getStructureDataIterator(Cursor cursor) throws IOException {
      // grab scalars, make sdata
      if (sdata == null) {
        sdata = new StructureDataTop();
        sdata.addVariableAsMember(ds, feature_id);
      }

      return new SingletonStructureDataIterator(sdata);
    }

    @Override
    public String getName() {
      return "TopScalars";
    }
  }

  private static class StructureDataTop extends StructureDataW {

    public StructureDataTop() {
      super(new StructureMembers("top"));
    }

    void addVariableAsMember(NetcdfDataset ds, String scalarVariableName) throws IOException {
      if (scalarVariableName == null) return;
      Variable v = ds.findVariable(scalarVariableName);
      if (v == null) return;
      StructureMembers.Member m = this.members.addMember(v.getFullName(), null, null, v.getDataType(), v.getShape());
      setMemberData(m, v.read());
    }
  }

  // not ok for sdata to be null
  private static class SingletonStructureDataIterator implements StructureDataIterator {
    private int count = 0;
    private StructureData sdata;

    SingletonStructureDataIterator(StructureData sdata) {
      this.sdata = sdata;
      assert sdata != null;
    }

    @Override
    public boolean hasNext() throws IOException {
      return (count == 0);
    }

    @Override
    public StructureData next() throws IOException {
      count++;
      return sdata;
    }

    @Override
    public StructureDataIterator reset() {
      count = 0;
      return this;
    }


    @Override
    public int getCurrentRecno() {
      return count - 1;
    }
  }

  ////////////////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public FeatureType getFeatureType() {
    return featureType;
  }

  // LOOK others should override
  public VariableDS findVariable(String axisName) {
    return null;
  }

  public String showDimension() {
    return "";
  }

  public String toString() {
    Formatter formatter = new Formatter();
    formatter.format(" Table %s on dimension %s type=%s%n", getName(), showDimension(), getClass().toString());
    formatter.format("  Coordinates=");
    formatter.format("%n  Data Variables= %d%n", cols.size());
    formatter.format("  Parent= %s%n", ((parent == null) ? "none" : parent.getName()));
    return formatter.toString();
  }

  public int show(Formatter f, int indent) {
    if (parent != null)
      indent = parent.show(f, indent);

    String s = indent(indent);
    String ftDesc = (featureType == null) ? "" : "featureType=" + featureType.toString();
    f.format("%n%sTable %s: type=%s %s%n", s, getName(), getClass().toString(), ftDesc);
    if (extraJoins != null) {
      f.format("  %sExtraJoins:%n", s);
      for (Join j : extraJoins)
        f.format("   %s  %s %n", s, j);
    }
    showTableExtraInfo(indent(indent + 2), f);
    showCoords(s, f);
    f.format("  %sVariables:%n", s);
    for (String colName : cols.keySet()) {
      f.format("   %s  %s %s%n", s, colName, getKind(colName));
    }
    return indent + 2;
  }

  String indent(int n) {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < n; i++) sbuff.append(' ');
    return sbuff.toString();
  }

  protected abstract void showTableExtraInfo(String indent, Formatter f);


  String getKind(String v) {
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
    boolean gotSome = false;
    for (CoordName coord : CoordName.values()) {
      String varName = findCoordinateVariableName(coord);
      if (varName != null) {
        gotSome = true;
        out.format(" %s Coord %s [%s]%n", indent, varName, coord);
      }
    }
    if (gotSome) out.format("%n");
  }

}
