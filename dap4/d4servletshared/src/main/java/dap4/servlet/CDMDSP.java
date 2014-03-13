/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.core.data.*;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4shared.*;
import dap4.cdmshared.*;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.jni.netcdf.Nc4Iosp;

import java.io.IOException;
import java.util.*;

/**
 * CDM->DAP DSP
 */

public class CDMDSP extends AbstractDSP
{

    //////////////////////////////////////////////////
    // Constants

    static protected final boolean DEBUG = false;

    static protected final Object FACTORYKEY = DapFactory.class;

    // Define the extension that marks synthetic data.
    static protected final String SYNTHETICTAG = ".syn";

    static protected final String DAPVERSION = "4.0";
    static protected final String DMRVERSION = "1.0";

    static protected final Class NC4CLASS = ucar.nc2.jni.netcdf.Nc4Iosp.class;

    // NetcdfDataset enhancement to use: need only coord systems
    static protected Set<NetcdfDataset.Enhance> ENHANCEMENT = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

    //////////////////////////////////////////////////
    // Class variables

    static protected boolean nc4loaded = false;

    //////////////////////////////////////////////////
    // Class methods

    /**
     * Make sure that NC4Iosp is registered and library loaded
     */
    static public void
    loadNc4Iosp()
        throws DapException
    {
        if(nc4loaded) return;
        nc4loaded = true;
        if(!NetcdfFile.iospRegistered(NC4CLASS)) {
            try {
                // register before H5Iosp
                NetcdfFile.registerIOProvider(NC4CLASS, false);
                // Use null to force use of JNA_PATH
                Nc4Iosp.setLibraryAndPath(null, "netcdf");
            } catch (Throwable e) {
                DapLog.error("Cant load IOSP Nc4Iosp");
                throw new DapException(e.getMessage(), e.getCause());
            }
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected NetcdfDataset ncfile = null;

    protected DapDataset dmr = null; // Root of the DMR
    protected DapFactory factory = null;

    // Map between cdmnode and dapnode
    protected NodeMap nodemap = new NodeMap();

    protected boolean closed = false;

    protected CDMDataDataset data = null; // root of the DataXXX tree

    //////////////////////////////////////////////////
    // Constructor(s)

    public CDMDSP()
    {
    }

    public CDMDSP(String path)
        throws DapException
    {
        this(path,null);
    }

    public
    CDMDSP(String path, DapContext cxt)
        throws DapException
    {
        super(path, cxt);
        ncfile = (NetcdfDataset) createNetcdfFile();
        if(ncfile == null)
            throw new DapException("CDMDSP: cannot open: " + path);
        this.factory = (DapFactory)cxt.get(FACTORYKEY);
        if(this.factory == null)
            this.factory = new DapFactoryDMR();
        build();
        buildDataDataset();
    }

    //////////////////////////////////////////////////
    // DSP Interface

    @Override
    public boolean match(String path, DapContext context)
    {
        return (path.indexOf(SYNTHETICTAG) < 0);
    }

    @Override
    public DSP open(String path) throws DapException
    {
        return open(path,new DapContext());
    }

    @Override
    public DSP open(String path, DapContext context) throws DapException
    {
        return new CDMDSP(path, context);
    }

    @Override
    public void close()
    {
        try {
            if(ncfile != null)
                ncfile.close();
        } catch (IOException ioe) {/*ignore*/}
    }

    @Override
    public DataDataset
    getDataDataset()
    {
        return data;
    }

    //////////////////////////////////////////////////
    // CDMDSP Specific Accessors

    public NetcdfDataset getNetcdfDataset()
    {
        return ncfile;
    }

    public void setDataDataset(CDMDataDataset data)
    {
        this.data = data;
    }

    /**
     * Lookup a dapnode in the dap nodemap.
     *
     * @param dapnode The DapNode
     * @return The corresponding CDMNode or null
     */
    public CDMNode
    getCDMNode(DapNode dapnode)
    {
        return nodemap.get(dapnode);
    }

    //////////////////////////////////////////////////
    // Nodemap control

    /**
     * Record a cdmnode+dapnode in the nodemap.
     * Make sure that we use the proper
     * object in order to avoid identity
     * problems.
     *
     * @param cdmnode The CMDNode (variable, dimension, etc) to record
     * @param dapnode The DapNode to which the cdmnode is to be mapped.
     */
    protected void
    recordNode(CDMNode cdmnode, DapNode dapnode)
    {
        CDMSort sort = cdmnode.getSort();
        switch (sort) {
        case VARIABLE:
        case STRUCTURE:
        case SEQUENCE:
            Variable basev = CDMUtil.unwrap((Variable) cdmnode);
            assert (basev != null) : "Unwrap() failed";
            cdmnode = (CDMNode) basev;
            break;
        }
        nodemap.put(dapnode, cdmnode);
    }

    /**
     * Lookup a cdmnode in the cdmnodemap.
     * Make sure that we use the proper
     * object in order to avoid identity
     * problems.
     *
     * @param cdmnode The CMDNode (variable, dimension, etc) to record
     * @return The DapNode to which the cdmnode is to be mapped or null.
     */
    protected DapNode
    lookupNode(CDMNode cdmnode)
    {
        CDMSort sort = cdmnode.getSort();
        if(sort == CDMSort.VARIABLE || sort == CDMSort.STRUCTURE) {
            Variable basev = CDMUtil.unwrap((Variable) cdmnode);
            assert (basev != null) : "Unwrap() failed";
            cdmnode = (CDMNode) basev;
        }
        return nodemap.get(cdmnode);
    }

    //////////////////////////////////////////////////

    /**
     * Extract the metadata from the NetcdfDataset
     * and build the DMR.
     */

    public void
    build()
        throws DapException
    {
        if(dmr != null)
            return;
        try {
            if(DEBUG) {
                System.out.println("writecdl:");
                ncfile.writeCDL(System.out, false);
                System.out.flush();
            }

            // Initialize the root dataset node
            dmr = (DapDataset) newNode(DapSort.DATASET);
            // Map the CDM root group to this group
            recordNode(ncfile.getRootGroup(), dmr);

            // Use the file path to define the dataset name
            String name = ncfile.getLocation();
            // Normalize the name
            name = DapUtil.canonicalpath(name, false);
            // Remove any path prefix
            int index = name.lastIndexOf('/');
            if(index >= 0)
                name = name.substring(index + 1, name.length());
            dmr.setShortName(name);
            dmr.setDataset(this.dmr);
            dmr.setDapVersion(DAPVERSION);
            dmr.setDMRVersion(DMRVERSION);
            dmr.setBase(DapUtil.canonicalpath(ncfile.getLocation(), false));
            dmr.setNS("http://xml.opendap.org/ns/DAP/4.0#");

            // Now recursively build the tree. Start by
            // Filling the dataset with the contents of the ncfile
            // root group.
            fillgroup(dmr, ncfile.getRootGroup());

            // Add an order index to the tree
            dmr.sort();

            // Now locate the coordinate variables for maps

            /* Walk looking for VariableDS instances */
            processmappedvariables(ncfile.getRootGroup());

            // Not set the view
            super.setDataset(dmr);

        } catch (DapException e) {
            throw new DapException(e);
        }
    }

    //////////////////////////////////////////////////
    // Actions

    protected void
    fillgroup(DapGroup dapgroup, Group cdmgroup)
        throws DapException
    {
        // Create decls in dap group for Dimensions
        for(Dimension cdmdim : cdmgroup.getDimensions()) {
            DapDimension dapdim = builddim(cdmdim);
        }
        // Create decls in dap group for Enumerations
        for(EnumTypedef cdmenum : cdmgroup.getEnumTypedefs()) {
            String name = cdmenum.getShortName();
            DapEnum dapenum = buildenum(cdmenum);
            dapenum.setShortName(name);
            dapgroup.addDecl(dapenum);
        }
        // Create decls in dap group for Variables
        for(Variable cdmvar : cdmgroup.getVariables()) {
            DapNode newvar = buildvariable(cdmvar, dapgroup);
        }
        // Create decls in dap group for subgroups
        for(Group subgroup : cdmgroup.getGroups()) {
            DapGroup newgroup = buildgroup(subgroup);
            dapgroup.addDecl(newgroup);
        }
        // Create decls in dap group for group-level attributes
        buildattributes(dapgroup, cdmgroup.getAttributes());
    }

    //////////////////////////////////////////////////
    // Declaration Builders

    protected DapDimension
    builddim(Dimension cdmdim)
        throws DapException
    {
        DapDimension dapdim = null;
        long cdmsize = dapsize(cdmdim);
        String name = cdmdim.getShortName();
        if(name != null && name.length() == 0)
            name = null;
        boolean shared = cdmdim.isShared();
        if(!shared) {
            // Unlike the parser, since we are working
            // from a NetcdfDataset instance, there might
            // be multiple anonymous dimension objects
            // the same size. So, just go ahead and create
            // multiple instances.
            dapdim = (DapDimension) newNode(DapSort.DIMENSION);
            if(cdmdim.isVariableLength())
                dapdim.setSize(DapDimension.VARIABLELENGTH);
            else
                dapdim.setSize(cdmsize);
            dmr.addDecl(dapdim);
        } else { // Non anonymous; create in current group
            dapdim = (DapDimension) newNode(DapSort.DIMENSION);
            dapdim.setShortName(name);
            dapdim.setSize(cdmsize);
            dapdim.setShared(true);
            Group cdmparent = cdmdim.getGroup();
            DapGroup dapparent = (DapGroup) lookupNode(cdmparent);
            assert (dapparent != null);
            dapparent.addDecl(dapdim);
        }
        recordNode(cdmdim, dapdim);
        return dapdim;
    }

    protected DapEnum
    buildenum(EnumTypedef cdmenum)
        throws DapException
    {
        DapEnum dapenum = (DapEnum) newNode(DapSort.ENUMERATION);
        recordNode(cdmenum, dapenum);
        dapenum.setShortName(cdmenum.getShortName());
        // Set the enum's basetype
        switch (cdmenum.getBaseType()) {
        case ENUM1:
            dapenum.setBaseType(DapType.INT8);
            break;
        case ENUM2:
            dapenum.setBaseType(DapType.INT16);
            break;
        case ENUM4:
        default:
            dapenum.setBaseType(DapType.INT32);
            break;
        }
        // Create the enum constants
        Map<Integer, String> ecvalues = cdmenum.getMap();
        for(Integer ecval : ecvalues.keySet()) {
            String name = ecvalues.get(ecval);
            assert (name != null);
            int value = (int) ecval;
            dapenum.addEnumConst(name, new Long(value));
        }
        return dapenum;
    }

    protected DapNode
    buildvariable(Variable cdmvar, DapNode parent)
        throws DapException
    {
        cdmvar = CDMUtil.unwrap(cdmvar);
        List<Dimension> cdmdims = cdmvar.getDimensions();
        return buildvariable(cdmvar, cdmdims, parent);
    }


    protected DapNode
    buildvariable(Variable cdmbasevar, List<Dimension> cdmdims, DapNode parent)
        throws DapException
    {
        DapVariable dapvar = null;
        /* If this var has a variable length last dimension,
           then we must convert to a sequence.
	*/
        if(cdmdims != null && cdmdims.size() > 0
            && cdmdims.get(cdmdims.size() - 1).isVariableLength()) {
            // Create the sequence using the dimensions of the variable
            // and the variable as the sequence field.
            dapvar = buildsequence(cdmbasevar);
        } else
            switch (cdmbasevar.getSort()) {
            case VARIABLE:
                switch (cdmbasevar.getDataType()) {
                case ENUM1:
                case ENUM2:
                case ENUM4:
                    dapvar = buildenumvar(cdmbasevar);
                    break;
                case OPAQUE:
                    dapvar = buildopaquevar(cdmbasevar);
                    break;
                case STRING:
                    dapvar = buildstringvar(cdmbasevar);
                    break;
                case STRUCTURE:
                    // How does this ever happen?
                    dapvar = buildstructvar(cdmbasevar);
                    break;
                default:
                    dapvar = buildatomicvar(cdmbasevar);
                }
                break;
            case STRUCTURE:
                dapvar = buildstructvar(cdmbasevar);
                break;
            default:
                assert false : "Internal Error";
            }

        if(parent != null) {
            switch (parent.getSort()) {
            case GROUP:
            case DATASET:
                ((DapGroup) parent).addDecl(dapvar);
                break;
            case STRUCTURE:
            case SEQUENCE:
                ((DapStructure) parent).addField(dapvar);
                break;
            default:
                assert (false) : "Internal error";
            }
        }
        builddimrefs(dapvar, cdmdims);
        return dapvar;
    }

    protected DapAtomicVariable
    buildatomicvar(Variable cdmvar)
        throws DapException
    {
        // Atomic => not opaque and not enum
        DapType basetype = CDMUtil.cdmtype2daptype(cdmvar.getDataType(), cdmvar.isUnsigned());
        if(basetype == null)
            throw new DapException("DapFile: illegal CDM variable base type: " + cdmvar.getDataType());
        DapAtomicVariable dapvar
            = (DapAtomicVariable) newNode(DapSort.ATOMICVARIABLE);
        dapvar.setShortName(cdmvar.getShortName());
        dapvar.setBaseType(basetype);
        recordNode(cdmvar, dapvar);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    protected DapAtomicVariable
    buildopaquevar(Variable cdmvar)
        throws DapException
    {
        assert (cdmvar.getDataType() == DataType.OPAQUE) : "Internal error";
        DapAtomicVariable dapvar
            = (DapAtomicVariable) newNode(DapSort.ATOMICVARIABLE);
        recordNode(cdmvar, dapvar);
        dapvar.setShortName(cdmvar.getShortName());
        dapvar.setBaseType(DapType.OPAQUE);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    protected DapAtomicVariable
    buildstringvar(Variable cdmvar)
        throws DapException
    {
        assert (cdmvar.getDataType() == DataType.STRING) : "Internal error";
        DapAtomicVariable dapvar
            = (DapAtomicVariable) newNode(DapSort.ATOMICVARIABLE);
        recordNode(cdmvar, dapvar);
        dapvar.setShortName(cdmvar.getShortName());
        dapvar.setBaseType(DapType.STRING);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    protected DapAtomicVariable
    buildenumvar(Variable cdmvar)
        throws DapException
    {
        assert (
            cdmvar.getDataType() == DataType.ENUM1
                || cdmvar.getDataType() == DataType.ENUM2
                || cdmvar.getDataType() == DataType.ENUM4
        ) : "Internal error";
        DapAtomicVariable dapvar
            = (DapAtomicVariable) newNode(DapSort.ATOMICVARIABLE);
        recordNode(cdmvar, dapvar);
        dapvar.setShortName(cdmvar.getShortName());
        // Now, we need to locate the actual enumeration decl
        EnumTypedef enumdef = cdmvar.getEnumTypedef();
        // Now, map to a DapEnum
        DapEnum dapenum = (DapEnum) lookupNode(enumdef);
        assert (dapenum != null);
        dapvar.setBaseType(dapenum);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    protected DapStructure
    buildstructvar(Variable cdmvar)
        throws DapException
    {
        assert (cdmvar.getDataType() == DataType.STRUCTURE) : "Internal error";
        DapStructure dapvar
            = (DapStructure) newNode(DapSort.STRUCTURE);
        recordNode(cdmvar, dapvar);
        dapvar.setShortName(cdmvar.getShortName());
        // We need to build the field variables
        Structure structvar = (Structure) cdmvar;
        for(CDMNode node : structvar.getVariables()) {
            Variable var = (Variable) node;
            buildvariable(var, dapvar);
        }
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    /* Create a sequence from a variable with a
       variable length last dimension.
       Suppose we have cdm equivalent to this:
            int32 var[d1][*]
       We convert to the following
           <Sequence name="var">
             <Int32 name="var"/>
             <Dim name=d1/>
           </Sequence>
    */

    protected DapSequence
    buildsequence(Variable cdmbasevar)
        throws DapException
    {
        DapSequence seq = (DapSequence) newNode(DapSort.SEQUENCE);
        recordNode(cdmbasevar, seq);
        seq.setShortName(cdmbasevar.getShortName());
        // We need to build the sequence field from cdmvar
        // But dimensionless and as field of the sequence.
        DapVariable field = (DapVariable) buildvariable(cdmbasevar, null, seq);
        return seq;
    }

    protected void
    buildattributes(DapNode node, List<Attribute> attributes)
        throws DapException
    {
        for(Attribute attr : attributes) {
            if(!suppress(attr.getShortName())) {
                DapAttribute dapattr = buildattribute(attr);
                node.addAttribute(dapattr);
            }
        }
    }

    protected DapAttribute
    buildattribute(Attribute attr)
        throws DapException
    {
        DapAttribute dapattr = (DapAttribute) newNode(DapSort.ATTRIBUTE);
        recordNode(attr, dapattr);
        dapattr.setShortName(attr.getShortName());
        DapType basetype = CDMUtil.cdmtype2daptype(attr.getDataType(), attr.isUnsigned());
        if(basetype == null)
            throw new DapException("DapFile: illegal CDM variable attribute type: " + attr.getDataType());
        dapattr.setBaseType(basetype);
        // Transfer the values
        Array values = attr.getValues();
        if(!validatecdmtype(attr.getDataType(), attr.isUnsigned(), values.getElementType()))
            throw new DapException("DapFile: attr type versus attribute data mismatch: " + values.getElementType());
        IndexIterator iter = values.getIndexIterator();
        while(iter.hasNext()) {
            Object o = iter.next();
            dapattr.addValue(o);
        }
        return dapattr;
    }

    /**
     * Assign dimensions to a variable
     *
     * @param dapvar  The variable to which we wish to assign dimensions
     * @param cdmdims The cdm dimensions from which we will find the dimension info
     */
    protected void
    builddimrefs(DapVariable dapvar, List<Dimension> cdmdims)
        throws DapException
    {
        if(cdmdims == null || cdmdims.size() == 0)
            return;
        // It is unfortunately the case that the dimensions
        // associated with the variable are not
        // necessarily the same object as those dimensions
        // as declared, so we need to use a non-trivial
        // matching algorithm.
        for(Dimension cdmdim : cdmdims) {
            DapDimension dapdim = null;
            if(cdmdim.isShared()) {
                Dimension declareddim = finddimdecl(cdmdim);
                dapdim = (DapDimension) lookupNode(declareddim);
            } else if(cdmdim.isVariableLength()) {// ignore
                continue;
            } else {//anonymous
                dapdim = builddim(cdmdim);
            }
            assert (dapdim != null) : "Internal error";
            dapvar.addDimension(dapdim);
        }
    }

    protected void
    processmappedvariables(Group g)
        throws DapException
    {
        for(Variable v0 : g.getVariables()) {
            Variable cdmvar = CDMUtil.unwrap(v0);
            if(cdmvar == null)
                throw new DapException("NetcdfDataset synthetic variable: " + v0);
            DapNode dapvar = lookupNode(cdmvar);
            if(dapvar == null)
                throw new DapException("Unknown variable: " + cdmvar);
            if(!(dapvar instanceof DapVariable))
                throw new DapException("CDMVariable not mapping to dap variable: " + cdmvar);
            buildmaps((DapVariable) dapvar, v0);
        }
    }


    /**
     * @param dapvar The variable to which we wish to assign maps
     * @param var    The NetcdfDataset variable from which to extract coord system
     */
    protected void
    buildmaps(DapVariable dapvar, Variable var)
        throws DapException
    {
        // See if this cdm variable has one (or more) coordinate system
        List<CoordinateSystem> css = null;
        if(var.getSort() == CDMSort.VARIABLE) {
            VariableDS vds = (VariableDS) var;
            css = vds.getCoordinateSystems();
        } else {
            StructureDS sds = (StructureDS) var;
            css = sds.getCoordinateSystems();
        }
        if(css != null && css.size() > 0) {
            // Not sure what to do with multiple coordinate systems
            // For now, only use the first
            CoordinateSystem coordsystems = css.get(0);
            for(CoordinateAxis axis : coordsystems.getCoordinateAxes()) {
                // First step is to find the dap variable
                // corresponding to the map
                VariableDS vds = (VariableDS) axis.getOriginalVariable();
                if(vds != null) {
                    Variable v = CDMUtil.unwrap(vds);
                    if(v != null) {
                        DapVariable mapvar = (DapVariable) lookupNode((CDMNode) v);
                        if(mapvar == null)
                            throw new DapException("Illegal map variable:" + v.toString());
                        if(mapvar.getSort() != DapSort.ATOMICVARIABLE)
                            throw new DapException("Non-atomic map variable:" + v.toString());
                        // Ignore maps where the map variable is inside this scope
                        if(!mapvar.isTopLevel()) {
                            DapVariable parent = (DapVariable) mapvar.getContainer();
                            assert parent.getSort() == DapSort.STRUCTURE;
                            if(dapvar != parent) {// Do we need to do transitive closure?
                                DapMap map = (DapMap) newNode(DapSort.MAP);
                                map.setVariable((DapAtomicVariable) mapvar);
                                dapvar.addMap(map);
                            }
                        }
                    }
                }
            }
        }
    }

    protected DapGroup
    buildgroup(Group cdmgroup)
        throws DapException
    {
        DapGroup dapgroup = (DapGroup) newNode(DapSort.GROUP);
        recordNode(cdmgroup, dapgroup);
        dapgroup.setShortName(cdmgroup.getShortName());
        fillgroup(dapgroup, cdmgroup);
        return dapgroup;
    }

    //////////////////////////////////////////////////
    // Utilities

    // Create new node
    protected DapNode
    newNode(DapSort sort)
    {
        DapNode node = (DapNode) factory.newNode(sort);
        if(dmr != null) node.setDataset(dmr);
        return node;
    }

    // Convert cdm size to DapDimension size
    protected long
    dapsize(Dimension cdmdim)
    {
        if(cdmdim.isVariableLength()) return DapDimension.VARIABLELENGTH;
        return (long) cdmdim.getLength();
    }

    protected boolean
    validatecdmtype(DataType datatype, boolean unsigned, Class typeclass)
    {
        switch (datatype) {
        case CHAR:
            return typeclass == char.class;
        case BYTE:
            if(unsigned)
                return typeclass == short.class;
            else
                return typeclass == byte.class;
        case SHORT:
            if(unsigned)
                return typeclass == int.class;
            else
                return typeclass == short.class;
        case INT:
            if(unsigned)
                return typeclass == long.class;
            else
                return typeclass == int.class;
        case LONG:
            if(unsigned)
                return typeclass == long.class;
            else
                return typeclass == long.class;
        case FLOAT:
            return typeclass == float.class;
        case DOUBLE:
            return typeclass == double.class;
        case STRING:
            return typeclass == String.class;
        case OPAQUE:
            return typeclass == Byte[].class;
        // For these, return the integer basetype
        case ENUM1:
            return typeclass == byte.class;
        case ENUM2:
            return typeclass == short.class;
        case ENUM4:
            return typeclass == int.class;

        // Undefined
        case SEQUENCE:
        case STRUCTURE:
        default:
            break;
        }
        return false;
    }


    protected Dimension
    finddimdecl(Dimension dimref)
    {
        // Search on the full name, but be careful,
        // the rule is that the declared dimension's fqn
        // must be a prefix of the dimension reference.
        for(Map.Entry<DapNode, CDMNode> entry : nodemap.getCDMMap().entrySet()) {
            if(entry.getValue().getSort() != CDMSort.DIMENSION)
                continue;
            Dimension d = (Dimension) entry.getValue();
            if(isdeclfor(d, dimref))
                return d;
        }
        return null;
    }

    protected boolean isdeclfor(Dimension decl, Dimension ref)
    {
        // First check shortname and size
        if(!decl.getShortName().equals(ref.getShortName()))
            return false;
        if(decl.getLength() != ref.getLength())
            return false;
        // Make sure they are in the same group
        String dprefix = decl.getGroup().getFullName();
        String rprefix = ref.getGroup().getFullName();
        return (dprefix.equals(rprefix));
    }


    //////////////////////////////////////////////////
    // Data Compilation

    /**
     * Build a tree of Data objects corresponding
     * to the DMR.
     */
    protected DataDataset
    buildDataDataset()
        throws DataException
    {
        List<Variable> cdmvars = ncfile.getVariables();
        CDMDataDataset dds = new CDMDataDataset(this, dmr);
        for(Variable v : cdmvars) {
            DapVariable dv = (DapVariable) nodemap.get(v);
            if(dv == null)
                throw new DataException("Unknown cdm variable: " + v.getShortName());
            DataVariable cdv = buildData(dv);
            dds.addVariable(cdv);
        }
        return dds;
    }

    protected DataVariable
    buildData(DapVariable dap)
        throws DataException
    {
        Variable cdmv = null;
        DataVariable dv = null;
        List<DapDimension> dimset = dap.getDimensions();
        long product = dap.getCount();
        cdmv = (Variable) nodemap.get(dap);
        if(cdmv == null)
            throw new DataException("Node has no cdm match: " + dap.getShortName());
        switch (dap.getSort()) {
        case ATOMICVARIABLE:
            try {
                Array array = cdmv.read();
                dv = new CDMDataAtomic(this, (DapAtomicVariable) dap, array);
            } catch (IOException ioe) {
                throw new DataException(ioe);
            }
            break;
        case STRUCTURE:
            DapStructure ds = (DapStructure) dap;
            ArrayStructure array;
            try {
                array = (ArrayStructure)cdmv.read();
            } catch (IOException ioe) {
                throw new DataException(ioe);
            }
            if(ds.getRank() == 0) {
                CDMDataStructure cds = new CDMDataStructure(this,ds,null, 0, array.getStructureData(0));
                dv = cds;
            } else {
                CDMDataCompoundArray cdca = new CDMDataCompoundArray(this, ds, array);
                dv = cdca;
            }
            break;
        case SEQUENCE:
            throw new DataException("Sequence not yet supported");
        default:
            throw new DataException("Unexpected DapNode: " + dap.getSort());
        }
        return dv;
    }

    //////////////////////////////////////////////////
    // Subclass Overrideable

    protected NetcdfFile
    createNetcdfFile()
        throws DapException
    {
        try {
            path = DapUtil.canonicalpath(this.path, false);
            int dotpos = this.path.lastIndexOf('.');
            NetcdfFile ncfile = null;
            if(dotpos > 0 && dotpos + 1 < this.path.length()) {
                String extension = this.path.substring(dotpos + 1, this.path.length());
                if(extension.equals("nc")) {
                    loadNc4Iosp();
                    Nc4Iosp nc4Iosp = new Nc4Iosp(NetcdfFileWriter.Version.netcdf4);
                    // try to open it using Nc4Iosp
                    ncfile = nc4Iosp.open(this.path);
                    ncfile = new NetcdfDataset(ncfile, ENHANCEMENT);
                    return ncfile;
                }
            }
            // Not sure if the file is netcdf4, so just let openDataset handle it
            ncfile = NetcdfDataset.openDataset(this.path,
                ENHANCEMENT,
                -1,    // buffer size
                null,  //canceltask
                null); // spi object
            return ncfile;
        } catch (Exception e) {
            return null;
        }
    }

    //////////////////////////////////////////////////
    // Utilities

    /**
     * Some attributes that are added by the NetcdfDataset
     * need to be kept out of the DMR. This function
     * defines that set.
     *
     * @param attrname A non-escaped attribute name to be tested for suppression
     * @return true if the attribute should be suppressed, false otherwise.
     */
    protected boolean suppress(String attrname)
    {
        if(attrname.startsWith("_Coord")) return true;
        if(attrname.equals("_Unsigned"))
            return true;
        return false;
    }


}
