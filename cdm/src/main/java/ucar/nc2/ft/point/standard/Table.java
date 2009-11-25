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
import ucar.nc2.dataset.*;
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
    this.feature_id = config.feature_id;
    this.missingVar = config.missingVar;

    if (config.parent != null) {
      parent = Table.factory(ds, config.parent);
      parent.child = this;
    }

    this.extraJoins = config.extraJoin;

    // try to exclude coordinate vars and "structural data" from the list of data variables
    /* checkNonDataVariable(config.lat);
    checkNonDataVariable(config.lon);
    checkNonDataVariable(config.elev);
    checkNonDataVariable(config.timeNominal);
    checkNonDataVariable(config.stnId);
    checkNonDataVariable(config.stnDesc);
    checkNonDataVariable(config.stnWmoId);
    checkNonDataVariable(config.stnAlt); */
    checkNonDataVariable(config.stnNpts);
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

      case FeatureId:
        return feature_id;

      case MissingVar:
        return missingVar;

    }
    return null;
  }

  ///////////////////////////////////////////////////////

  /**
   * A Structure or PsuedoStructure.
   * <p/>
   * Structure: defined by config.structName.
   * if config.vars if not null restricts to list of vars, must be members.
   * <p/>
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
          struct = new StructurePseudoDS(ds, dim.getGroup(), config.structName, config.vars, this.dim);
          break;

        case PsuedoStructure2D:
          this.dim = ds.findDimension(config.dimName);
          this.outer = ds.findDimension(config.outerName);
          assert dim != null;
          assert config.outerName != null;
          struct = new StructurePseudo2Dim(ds, dim.getGroup(), config.structName, config.vars, this.dim, this.outer);
          break;
      }

      config.vars = new ArrayList<String>();
      for (Variable v : struct.getVariables()) {
        // remove substructures
        if (v.getDataType() == DataType.STRUCTURE) {
          if (config.structureType == TableConfig.StructureType.PsuedoStructure)
            struct.removeMemberVariable(v);
        } else {
          this.cols.add(v);
          config.vars.add(v.getShortName());
        }
      }
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sstruct=%s, dim=%s type=%s%n", indent, struct.getNameAndDimensions(), dim.getName(), struct.getClass().getName());
    }

    @Override
    public VariableDS findVariable(String axisName) {
      return (VariableDS) struct.findVariable(axisName);
    }

    @Override
    public String showDimension() {
      return dim.getName();
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      return struct.getStructureIterator(bufferSize);
    }

    @Override
    public String getName() {
      return stype.toString()+"("+struct.getName()+")";
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
      this.dim = new Dimension(config.structName, (int) config.as.getSize(), false);
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

        @Override
    public String getName() {
      return "ArrayStructure("+name+")";
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * When theres no seperate station table, but info is duplicated in the obs structure.
   * Must have a ParentId child table
   * No variables are added to cols.
   * <p/>
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
     public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      return as.getStructureDataIterator();
    }

    @Override
    public String getName() {
      return "Constructed";
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
    private String startVarName; // variable name holding the starting index in parent
    private String numRecordsVarName; // variable name holding the number of children in parent
    private int[] startIndex, numRecords;

    TableContiguous(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.startVarName = config.start;
      this.numRecordsVarName = config.numRecords;

      if (startVarName == null) {  // read numRecords when startVar is not known
        try {
          Variable v = ds.findVariable(config.numRecords);
          Array numRecords = v.read();
          int n = (int) v.getSize();

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
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      checkNonDataVariable(config.start);
      checkNonDataVariable(config.numRecords);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sstart=%s, numRecords=%s%n", indent, startVarName, numRecordsVarName);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
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
      return "Contig("+numRecordsVarName+")";
    }
  }

  ///////////////////////////////////////////////////////

  /**
   * The children have a field containing the index of the parent.
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
      this.parentIndexName = config.parentIndex;

      // construct the map
      try {
        Variable rpIndex = ds.findVariable(config.parentIndex);
        Array index = rpIndex.read();

        int childIndex = 0;
        this.indexMap = new HashMap<Integer, List<Integer>>((int) (2 * index.getSize()));
        while (index.hasNext()) {
          int parent = index.nextInt();
          List<Integer> list = indexMap.get(parent);
          if (list == null) {
            list = new ArrayList<Integer>();
            indexMap.put(parent, list);
          }
          list.add(childIndex);
          childIndex++;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      checkNonDataVariable(config.parentIndex);
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sparentIndexName=%s, indexMap.size=%d%n", indent, parentIndexName, indexMap.size());
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      int parentIndex = cursor.getParentRecnum();
      List<Integer> index = indexMap.get(parentIndex);
      if (index == null) index = new ArrayList<Integer>();
      return new StructureDataIteratorIndexed(struct, index);
    }

    @Override
    public String getName() {
      return "Indexed("+parentIndexName+")";
    }
  }

  ///////////////////////////////////////////////////////
  /**
   * The children have a field containing the id of the parent.
   * For efficiency, we scan this data and construct an IndexMap( parentIndex -> list of children),
   * i.e. we compute the inverse link, parent -> children.
   * TableParentIndex is the children, config.struct describes the cols.
   * <p/>
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
          index = ((ArrayChar)index).make1DStringArray();

        parentHash = new HashMap<Object, ParentInfo>((int) (2 * index.getSize()));

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
      ArrayStructure as = new ArrayStructureW(struct.makeStructureMembers(), new int[] {n}, parentData);

      // find the parent TableConstruct and inject the ArrayStructure
      Table t = this;
      while (t.parent != null) {
        t = t.parent;
        if (t instanceof TableConstruct) {
          ((TableConstruct) t).as = as;
          break;
        }
      }
      checkNonDataVariable(parentIdName);
    }

    private class ParentInfo {
      List<Integer> recnumList = new ArrayList<Integer>();
      StructureData sdata;

      void add(int recnum) throws IOException {
        recnumList.add(recnum);
        if (sdata != null) return;
        try {
          sdata = struct.readStructure( recnum);
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

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      int parentIndex = cursor.getParentRecnum();
      ParentInfo info = indexMap[parentIndex];
      List<Integer> index = (info == null) ? new ArrayList<Integer>() : info.recnumList;
      return new StructureDataIteratorIndexed(struct, index);
    }

    @Override
    public String getName() {
      return "ParentId("+parentIdName+")";
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

        @Override
    public String getName() {
      return "Linked("+start+"->"+next+")";
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
           cols.add(v);
           int rank = v.getRank();
           int[] shape = new int[rank - 2];
           System.arraycopy(v.getShape(), 2, shape, 0, rank - 2);
           sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
         }

       } else {
         for (Variable v : ds.getVariables()) {
           if (v.getRank() < 2) continue;
           if (v.getDimension(0).equals(this.outer) && v.getDimension(1).equals(this.inner)) {
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
       f.format("%sStructureMembers=%s, dim=%s,%s%n", indent, sm.getName(), outer.getName(), inner.getName());
     }

     @Override
     public String showDimension() {
       return inner.getName();
     }

     @Override
     public VariableDS findVariable(String axisName) {
       return (VariableDS) ds.findVariable(axisName);
     }

     public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
       StructureData parentStruct = cursor.getParentStructure();
       ArrayStructureMA asma = new ArrayStructureMA(sm, new int[]{inner.getLength()});
       for (VariableSimpleIF v : cols) {
         Array data = parentStruct.getArray(v.getShortName());
         StructureMembers.Member childm = sm.findMember(v.getShortName());
         childm.setDataArray(data);
       }
       return asma.getStructureDataIterator();
     }

      @Override
      public String getName() {
        return "Multidim(" + outer.getName()+"," + inner.getName() + ")";
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
           cols.add(v);
           int rank = v.getRank();
           int[] shape = new int[rank - 3];
           System.arraycopy(v.getShape(), 3, shape, 0, rank - 3);
           sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
         }

       } else {
         for (Variable v : ds.getVariables()) {
           if (v.getRank() < 3) continue;
           if (v.getDimension(0).equals(dim) && v.getDimension(1).equals(middle)  && v.getDimension(2).equals(inner )) {
             cols.add(v);
             int rank = v.getRank();
             int[] shape = new int[rank - 3];
             System.arraycopy(v.getShape(), 3, shape, 0, rank - 3);
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
     public VariableDS findVariable(String axisName) {
       return (VariableDS) ds.findVariable(axisName);
     }

     public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
       StructureData parentStruct = cursor.tableData[2];
       int middleIndex = cursor.recnum[1];
       ArrayStructureMA asma = new ArrayStructureMA(sm, new int[]{inner.getLength()});
       for (VariableSimpleIF v : cols) {
         Array data = parentStruct.getArray(v.getShortName());
         Array myData = data.slice(0, middleIndex);
         StructureMembers.Member childm = sm.findMember(v.getShortName());
         childm.setDataArray (myData.copy()); // must make copy - ARrayStucture doesnt deal with logical views
       }
       return asma.getStructureDataIterator();
     }

      @Override
      public String getName() {
        return "Multidim(" + dim.getName()+"," + middle.getName() +"," + inner.getName() + ")";
      }

   }

   /**
   * Used for PsuedoStructure(station, time).
   * This is used for the inner table.
   * Need: config.inner, config.outer = config.dim, config.vars
   * <p/>
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

           @Override
      public String getName() {
        return "MultidimPseudo(" + outer.getName()+"," + inner.getName() + ")";
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
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      int outerIndex = cursor.recnum[2];
      int middleIndex = cursor.recnum[1];
      try {
        Section s = new Section().appendRange(outerIndex,outerIndex).appendRange(middleIndex,middleIndex);
        ArrayStructure result = (ArrayStructure) struct.read(s);
        assert result.getSize() == 1;
        StructureData sdata = result.getStructureData(0); // should only be one
        ArrayStructureMA asma = new ArrayStructureMA(sm, new int[]{inner.getLength()});
        for (VariableSimpleIF v : cols) {
          Array data = sdata.getArray(v.getShortName());
          StructureMembers.Member childm = sm.findMember(v.getShortName());
          childm.setDataArray(data);
        }
        return asma.getStructureDataIterator();

      } catch (InvalidRangeException e) {
        throw new IllegalStateException(e);
      }
    }

      @Override
      public String getName() {
        return "MultidimPsuedo(" + dim.getName()+"," + middle.getName() +"," + inner.getName() + ")";
      }

  }

  ///////////////////////////////////////////////////////

  /**
   * Used for Structure(station, time).
   * This is used for the inner table, where the station index gets set, and all the structures for that
   * styation are read in at once. Then we just iterate over that ArrayStructure.
   * <p/>
   * Used by:
   * GempakCdm
   */
  public static class TableMultidimStructure extends Table.TableStructure {

    TableMultidimStructure(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
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
      return "MultidimStructure(" + struct.getName() + ")";
    }

  }


  ///////////////////////////////////////////////////////

  /**
   * A Structure inside of a parent Structure.
   * Name of child member inside parent Structure is config.nestedTableName
    obsTable.structName = obsStruct.getName();
    obsTable.nestedTableName = obsStruct.getShortName();
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

    public VariableDS findVariable(String axisName) {
      return (VariableDS) struct.findVariable(axisName);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
      StructureData parentStruct = cursor.getParentStructure();

      StructureMembers members = parentStruct.getStructureMembers();
      StructureMembers.Member m = members.findMember(nestedTableName);
      members.hideMember(m); // LOOK ??
      if (m == null)
        System.out.println("HEY");
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
   * <p/>
   * Used by:
   * FslWindProfiler
   */
  public static class TableSingleton extends Table {
    StructureData sdata;

    TableSingleton(NetcdfDataset ds, TableConfig config) {
      super(ds, config);
      this.sdata = config.sdata;
      if (sdata == null) return;

      for (StructureMembers.Member m : sdata.getStructureMembers().getMembers())
        cols.add(new VariableSimpleAdapter(m));
    }

    @Override
    protected void showTableExtraInfo(String indent, Formatter f) {
      f.format("%sStructureData=%s%n", indent, sdata);
    }

    public StructureDataIterator getStructureDataIterator(Cursor cursor, int bufferSize) throws IOException {
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

        @Override
    public String getName() {
      return "TopScalars";
    }
  }

  // ok for sdata to be null
  private static class SingletonStructureDataIterator implements StructureDataIterator {
    private int count = 0;
    private StructureData sdata;

    SingletonStructureDataIterator(StructureData sdata) {
      this.sdata = sdata;
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
    public void setBufferSize(int bytes) {
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

  /* public List<? super VariableSimpleIF> getDataVariables() {
    return cols;
  } */

  // LOOK others should override
  public VariableDS findVariable(String axisName) {
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
