/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm.dsp;

import dap4.cdm.CDMTypeFcns;
import dap4.cdm.CDMUtil;
import dap4.cdm.NodeMap;
import dap4.core.data.DSP;
import dap4.core.data.DataCursor;
import dap4.core.dmr.*;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapSort;
import dap4.core.util.DapUtil;
import dap4.dap4lib.AbstractDSP;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Wrap CDM source (NetcdfDataset) to be a DSP
 * This basically means wrapping various (CDM)Array
 * object to look like DataVariable objects.
 * Currently only used on server side
 */

public class CDMDSP extends AbstractDSP
{

    //////////////////////////////////////////////////
    // Constants

    static protected final boolean DEBUG = false;

    static protected final String DAPVERSION = "4.0";
    static protected final String DMRVERSION = "1.0";
    static protected final String DMRNS = "http://xml.opendap.org/ns/DAP/4.0#";

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
            throws dap4.core.util.DapException
    {
        if(nc4loaded) return;
        nc4loaded = true;
        if(!NetcdfFile.iospRegistered(NC4CLASS)) {
            try {
                // register before H5Iosp
                NetcdfFile.registerIOProvider(NC4CLASS, false);
                Nc4Iosp.setLibraryAndPath(null, null); // use defaults
            } catch (Throwable e) {
                throw new dap4.core.util.DapException(e.getMessage(), e.getCause());
            }
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected NetcdfDataset ncdfile = null;

    protected DMRFactory dmrfactory = null;

    // Map between cdmnode and dapnode
    protected NodeMap nodemap = new NodeMap();

    protected boolean closed = false;

//    protected HttpServletRequest request = null;
//    protected HttpServletResponse response = null;


    //////////////////////////////////////////////////
    // Constructor(s)

    public CDMDSP()
    {
    }

    public CDMDSP(String path)
            throws dap4.core.util.DapException
    {
        super();
        setLocation(path);
    }

    //////////////////////////////////////////////////
    // DSP Interface

    // This is intended to be the last DSP checked
    @Override
    public boolean dspMatch(String path, DapContext context)
    {
        return true;
    }

    /**
     * @param filepath - absolute path to a file
     * @return CDMDSP instance
     * @throws dap4.core.util.DapException
     */
    @Override
    public DSP
    open(String filepath)
            throws dap4.core.util.DapException
    {
        try {
            NetcdfFile ncfile = createNetcdfFile(filepath, null);
            NetcdfDataset ncd = new NetcdfDataset(ncfile, ENHANCEMENT);
            return open(ncd);
        } catch (IOException ioe) {
            throw new dap4.core.util.DapException("CDMDSP: cannot open: " + filepath, ioe);
        }
    }

    /**
     * Provide an extra API for use in testing
     *
     * @param ncd
     * @return
     * @throws dap4.core.util.DapException
     */
    public DSP open(NetcdfDataset ncd)
            throws dap4.core.util.DapException
    {
        assert this.context != null;
        this.dmrfactory = new DMRFactory();
        this.ncdfile = ncd;
        setLocation(this.ncdfile.getLocation());
        buildDMR();
        return this;
    }


    @Override
    public void close()
            throws IOException
    {
        if(this.ncdfile != null)
            this.ncdfile.close();
    }


    @Override
    public DataCursor
    getVariableData(DapVariable var)
            throws DapException
    {
        Variable cdmvar = (Variable)nodemap.get(var);
        if(cdmvar == null)
            throw new DapException("Unknown variable: " + var);
        CDMCursor vardata = (CDMCursor) super.getVariableData(var);
        if(vardata == null) {
            DataCursor.Scheme scheme;
            switch (var.getSort()) {
            case ATOMICVARIABLE:
                scheme = DataCursor.Scheme.ATOMIC;
                break;
            case STRUCTURE:
                scheme = (var.getRank() == 0 ? DataCursor.Scheme.STRUCTURE
                        : DataCursor.Scheme.STRUCTARRAY);
                break;
            case SEQUENCE:
                scheme = (var.getRank() == 0 ? DataCursor.Scheme.SEQUENCE
                        : DataCursor.Scheme.SEQARRAY);
                break;
            default:
                throw new DapException("Unexpected cursor type: " + var);
            }
            try {
                vardata = new CDMCursor(scheme, var, this).setArray(cdmvar.read());
            } catch (IOException e) {
               throw new DapException(e);
            }
            super.addVariableData(var, vardata);
        }
        return vardata;
    }

    //////////////////////////////////////////////////
    // Abstract DSP Abstract methods

    //////////////////////////////////////////////////
    // CDMDSP Specific Accessors

    public NetcdfDataset getNetcdfDataset()
    {
        return this.ncdfile;
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
     * Record a cdmnode <-> dapnode in the nodemap.
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
    buildDMR()
            throws dap4.core.util.DapException
    {
        if(getDMR() != null)
            return;
        try {
            if(DEBUG) {
                System.out.println("writecdl:");
                this.ncdfile.writeCDL(System.out, false);
                System.out.flush();
            }
            // Use the file path to define the dataset name
            String name = this.ncdfile.getLocation();
            // Normalize the name
            name = DapUtil.canonicalpath(name);
            // Remove any path prefix
            int index = name.lastIndexOf('/');
            if(index >= 0)
                name = name.substring(index + 1, name.length());
            // Initialize the root dataset node
            setDMR((DapDataset) dmrfactory.newDataset(name).annotate(this.ncdfile));
            // Map the CDM root group to this group
            recordNode(this.ncdfile.getRootGroup(), getDMR());
            getDMR().setDataset(getDMR());
            getDMR().setDapVersion(DAPVERSION);
            getDMR().setDMRVersion(DMRVERSION);
            getDMR().setBase(DapUtil.canonicalpath(this.ncdfile.getLocation()));
            getDMR().setNS(DMRNS);

            // Now recursively build the tree. Start by
            // Filling the dataset with the contents of the ncfile
            // root group.
            fillgroup(getDMR(), this.ncdfile.getRootGroup());

            // Add an order index to the tree
            getDMR().sort();

            // Now locate the coordinate variables for maps

            /* Walk looking for VariableDS instances */
            processmappedvariables(this.ncdfile.getRootGroup());

            // Now set the view
            getDMR().finish();

        } catch (dap4.core.util.DapException e) {
            setDMR(null);
            throw new dap4.core.util.DapException(e);
        }
    }

    //////////////////////////////////////////////////
    // Actions

    protected void
    fillgroup(DapGroup dapgroup, Group cdmgroup)
            throws dap4.core.util.DapException
    {
        // Create decls in dap group for Dimensions
        for(Dimension cdmdim : cdmgroup.getDimensions()) {
            DapDimension dapdim = builddim(cdmdim);
        }
        // Create decls in dap group for Enumerations
        for(EnumTypedef cdmenum : cdmgroup.getEnumTypedefs()) {
            String name = cdmenum.getShortName();
            DapEnumeration dapenum = buildenum(cdmenum);
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
            throws dap4.core.util.DapException
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
            dapdim = (DapDimension) dmrfactory.newDimension(null, 0).annotate(cdmdim);
            if(cdmdim.isVariableLength())
                dapdim.setSize(DapDimension.VARIABLELENGTH);
            else
                dapdim.setSize(cdmsize);
            getDMR().addDecl(dapdim);
        } else { // Non anonymous; create in current group
            dapdim = (DapDimension) dmrfactory.newDimension(name, cdmsize).annotate(cdmdim);
            dapdim.setShared(true);
            Group cdmparent = cdmdim.getGroup();
            DapGroup dapparent = (DapGroup) lookupNode(cdmparent);
            assert (dapparent != null);
            dapparent.addDecl(dapdim);
        }
        recordNode(cdmdim, dapdim);
        return dapdim;
    }

    protected DapEnumeration
    buildenum(EnumTypedef cdmenum)
            throws dap4.core.util.DapException
    {
        // Set the enum's basetype
        DapType base = null;
        switch (cdmenum.getBaseType()) {
        case ENUM1:
            base = DapType.INT8;
            break;
        case ENUM2:
            base = DapType.INT16;
            break;
        case ENUM4:
        default:
            base = DapType.INT32;
            break;
        }
        DapEnumeration dapenum = (DapEnumeration) dmrfactory.newEnumeration(cdmenum.getShortName(), base).annotate(cdmenum);
        recordNode(cdmenum, dapenum);
        // Create the enum constants
        Map<Integer, String> ecvalues = cdmenum.getMap();
        for(Map.Entry<Integer, String> entry : ecvalues.entrySet()) {
            String name = entry.getValue();
            assert (name != null);
            int value = (int) entry.getKey();
            dapenum.addEnumConst(dmrfactory.newEnumConst(name, new Long(value)));
        }
        return dapenum;
    }

    protected DapNode
    buildvariable(Variable cdmvar, DapNode parent)
            throws dap4.core.util.DapException
    {
        cdmvar = CDMUtil.unwrap(cdmvar);
        List<Dimension> cdmdims = cdmvar.getDimensions();
        return buildvariable(cdmvar, cdmdims, parent);
    }


    protected DapNode
    buildvariable(Variable cdmbasevar, List<Dimension> cdmdims, DapNode parent)
            throws dap4.core.util.DapException
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
            throws dap4.core.util.DapException
    {
        // Atomic => not opaque and not enum
        DapType basetype = CDMTypeFcns.cdmtype2daptype(cdmvar.getDataType());
        if(basetype == null)
            throw new dap4.core.util.DapException("DapFile: illegal CDM variable base type: " + cdmvar.getDataType());
        DapAtomicVariable dapvar = (DapAtomicVariable) dmrfactory.newAtomicVariable(cdmvar.getShortName(), basetype).annotate(cdmvar);
        recordNode(cdmvar, dapvar);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    protected DapAtomicVariable
    buildopaquevar(Variable cdmvar)
            throws dap4.core.util.DapException
    {
        assert (cdmvar.getDataType() == DataType.OPAQUE) : "Internal error";
        DapAtomicVariable dapvar = (DapAtomicVariable) dmrfactory.newAtomicVariable(cdmvar.getShortName(), DapType.OPAQUE).annotate(cdmvar);
        recordNode(cdmvar, dapvar);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    protected DapAtomicVariable
    buildstringvar(Variable cdmvar)
            throws dap4.core.util.DapException
    {
        assert (cdmvar.getDataType() == DataType.STRING) : "Internal error";
        DapAtomicVariable dapvar = (DapAtomicVariable) dmrfactory.newAtomicVariable(cdmvar.getShortName(), DapType.STRING).annotate(cdmvar);
        recordNode(cdmvar, dapvar);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    protected DapAtomicVariable
    buildenumvar(Variable cdmvar)
            throws dap4.core.util.DapException
    {
        assert (
                cdmvar.getDataType() == DataType.ENUM1
                        || cdmvar.getDataType() == DataType.ENUM2
                        || cdmvar.getDataType() == DataType.ENUM4
        ) : "Internal error";

        // Now, we need to locate the actual enumeration decl
        EnumTypedef enumdef = cdmvar.getEnumTypedef();
        EnumTypedef trueenumdef = findMatchingEnum(enumdef);
        // Modify the cdmvar
        cdmvar.setEnumTypedef(trueenumdef);
        // Now, map to a DapEnumeration
        DapEnumeration dapenum = (DapEnumeration) lookupNode(trueenumdef);
        assert (dapenum != null);
        DapAtomicVariable dapvar = (DapAtomicVariable) dmrfactory.newAtomicVariable(cdmvar.getShortName(), dapenum).annotate(cdmvar);
        recordNode(cdmvar, dapvar);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    /**
     * Unfortunately, the CDM Iosp does not
     * actually use the declared enums. Rather,
     * for every enum type'd variable, a new
     * enum decl is defined. So, we need
     * to find the original enum decl that matches
     * the variable's enum.
     */

    protected EnumTypedef
    findMatchingEnum(EnumTypedef varenum)
            throws dap4.core.util.DapException
    {
        List<EnumTypedef> candidates = new ArrayList<>();
        for(Map.Entry<DapNode, CDMNode> entry : nodemap.getCDMMap().entrySet()) {
            CDMNode cdmnode = entry.getValue();
            if(cdmnode.getSort() != CDMSort.ENUMERATION)
                continue;
            // Compare the enumeration (note names will differ)
            EnumTypedef target = (EnumTypedef) cdmnode;
            /* Ideally, we should test the types of the enums,
               but, unfortunately, the var enum is always enum4.
            if(target.getBaseType() != varenum.getBaseType())
                continue;
            */
            Map<Integer, String> targetmap = target.getMap();
            Map<Integer, String> varmap = varenum.getMap();
            if(targetmap.size() != varmap.size())
                continue;
            boolean match = true; // until otherwise shown
            for(Map.Entry<Integer, String> tpair : targetmap.entrySet()) {
                String tname = tpair.getValue();
                int value = (int) tpair.getKey();
                boolean found = false;
                for(Map.Entry<Integer, String> vpair : varmap.entrySet()) {
                    if(tname.equals(vpair.getValue()) && value == (int) vpair.getKey()) {
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    match = false;
                    break;
                }
            }
            if(!match)
                continue;

            // Save it unless it is shadowed by a closer enum
            boolean shadowed = false;
            for(EnumTypedef etd : candidates) {
                if(shadows(etd.getGroup(), target.getGroup())) {
                    shadowed = true;
                    break;
                }
            }
            if(!shadowed)
                candidates.add(target);
        }

        switch (candidates.size()) {
        case 0:
            throw new dap4.core.util.DapException("CDMDSP: No matching enum type decl: " + varenum.getShortName());
        case 1:
            break;
        default:
            throw new dap4.core.util.DapException("CDMDSP: Multiple matching enum type decls: " + varenum.getShortName());
        }
        return candidates.get(0);
    }

    protected boolean
    shadows(Group parent, Group child)
    {
        if(child == parent)
            return true;
        Group candidate = child;
        do {
            candidate = candidate.getGroup();
        } while(candidate != null && candidate != parent);
        return (candidate == parent);
    }

    protected DapStructure
    buildstructvar(Variable cdmvar)
            throws dap4.core.util.DapException
    {
        assert (cdmvar.getDataType() == DataType.STRUCTURE) : "Internal error";
        DapStructure dapvar = (DapStructure) dmrfactory.newStructure(cdmvar.getShortName()).annotate(cdmvar);
        recordNode(cdmvar, dapvar);
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
    buildsequence(Variable cdmvar)
            throws dap4.core.util.DapException
    {
        DapSequence seq = (DapSequence) dmrfactory.newSequence(cdmvar.getShortName()).annotate(cdmvar);
        recordNode(cdmvar, seq);
        // We need to build the sequence field from cdmvar
        // But dimensionless and as fields of the sequence.
        Sequence seqvar = (Sequence) cdmvar;
        for(CDMNode node : seqvar.getVariables()) {
            Variable var = (Variable) node;
            buildvariable(var, seq);
        }
        return seq;
    }

    protected void
    buildattributes(DapNode node, List<Attribute> attributes)
            throws dap4.core.util.DapException
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
            throws dap4.core.util.DapException
    {
        DapType basetype = CDMTypeFcns.cdmtype2daptype(attr.getDataType());
        if(basetype == null)
            throw new dap4.core.util.DapException("DapFile: illegal CDM variable attribute type: " + attr.getDataType());
        DapAttribute dapattr = (DapAttribute) dmrfactory.newAttribute(attr.getShortName(), basetype).annotate(attr);
        recordNode(attr, dapattr);
        // Transfer the values
        Array values = attr.getValues();
        if(!validatecdmtype(attr.getDataType(), values.getElementType()))
            throw new dap4.core.util.DapException("DapFile: attr type versus attribute data mismatch: " + values.getElementType());
        IndexIterator iter = values.getIndexIterator();
        Object[] valuelist = new Object[(int) values.getSize()];
        for(int i = 0; iter.hasNext(); i++) {
            valuelist[i] = fixvalue(iter.next(), basetype);
        }
        dapattr.setValues(valuelist);
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
            throws dap4.core.util.DapException
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
                if(declareddim == null)
                    throw new dap4.core.util.DapException("Unprocessed cdm dimension: " + cdmdim);
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
            throws dap4.core.util.DapException
    {
        for(Variable v0 : g.getVariables()) {
            Variable cdmvar = CDMUtil.unwrap(v0);
            if(cdmvar == null)
                throw new dap4.core.util.DapException("NetcdfDataset synthetic variable: " + v0);
            DapNode dapvar = lookupNode(cdmvar);
            if(dapvar == null)
                throw new dap4.core.util.DapException("Unknown variable: " + cdmvar);
            if(!(dapvar instanceof DapVariable))
                throw new dap4.core.util.DapException("CDMVariable not mapping to dap variable: " + cdmvar);
            buildmaps((DapVariable) dapvar, v0);
        }
    }


    /**
     * @param dapvar The variable to which we wish to assign maps
     * @param var    The NetcdfDataset variable from which to extract coord system
     */
    protected void
    buildmaps(DapVariable dapvar, Variable var)
            throws dap4.core.util.DapException
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
                            throw new dap4.core.util.DapException("Illegal map variable:" + v.toString());
                        if(mapvar.getSort() != DapSort.ATOMICVARIABLE)
                            throw new dap4.core.util.DapException("Non-atomic map variable:" + v.toString());
                        // Ignore maps where the map variable is inside this scope
                        if(!mapvar.isTopLevel()) {
                            DapVariable parent = (DapVariable) mapvar.getContainer();
                            assert parent.getSort() == DapSort.STRUCTURE;
                            if(dapvar != parent) {// Do we need to do transitive closure?
                                DapMap map = (DapMap) dmrfactory.newMap(mapvar).annotate(v);
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
            throws dap4.core.util.DapException
    {
        DapGroup dapgroup = (DapGroup) dmrfactory.newGroup(cdmgroup.getShortName()).annotate(cdmgroup);
        recordNode(cdmgroup, dapgroup);
        dapgroup.setShortName(cdmgroup.getShortName());
        fillgroup(dapgroup, cdmgroup);
        return dapgroup;
    }

    //////////////////////////////////////////////////
    // Utilities

    // Convert cdm size to DapDimension size
    protected long
    dapsize(Dimension cdmdim)
    {
        if(cdmdim.isVariableLength()) return DapDimension.VARIABLELENGTH;
        return (long) cdmdim.getLength();
    }

    protected boolean
    validatecdmtype(DataType datatype, Class typeclass)
    {
        switch (datatype) {
        case CHAR:
            return typeclass == char.class;
        case BYTE:
        case UBYTE:
            return typeclass == byte.class;
        case SHORT:
        case USHORT:
            return typeclass == short.class;
        case INT:
        case UINT:
            return typeclass == int.class;
        case LONG:
        case ULONG:
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

    protected Object
    fixvalue(Object o, DapType datatype)
    {
        TypeSort atype = datatype.getTypeSort();
        if(o instanceof Character) {
            long l = (long) ((Character) o).charValue();
            if(atype.isUnsigned())
                l = l & 0xffL;
            o = Long.valueOf(l);
        } else if(o instanceof Float || o instanceof Double) {
            if(atype == TypeSort.Float32)
                o = Float.valueOf(((Number) o).floatValue());
            else if(atype == TypeSort.Float64)
                o = Double.valueOf(((Number) o).doubleValue());
            else
                assert false : "Internal error";
        } else if(o instanceof Number) {
            long l = ((Number) o).longValue();
            switch (atype) {
            case Char:
            case UInt8:
                l = l & 0xffL;
                break;
            case UInt16:
                l = l & 0xffffL;
                break;
            case UInt32:
                l = l & 0xffffffffL;
                break;
            default:
                break;
            }
            o = Long.valueOf(l);
        } else if(o instanceof String) {
            o = o.toString();
        } else if(o instanceof ByteBuffer) {
            // leave it unchanged
        } else if(o instanceof byte[]) {
            o = ByteBuffer.wrap((byte[]) o);
        } else if(o instanceof Byte[]) {
            Byte[] ob = (Byte[]) o;
            byte[] bb = new byte[ob.length];
            for(int i = 0; i < bb.length; i++) {
                bb[i] = (byte) ob[i];
            }
            o = ByteBuffer.wrap(bb);
        } //else { // leave it unchanged
        return o;
    }

    //////////////////////////////////////////////////

    protected NetcdfFile
    createNetcdfFile(String location, CancelTask canceltask)
            throws dap4.core.util.DapException
    {
        try {
            NetcdfFile ncfile = NetcdfFile.open(location, canceltask);
            return ncfile;
        } catch (dap4.core.util.DapException de) {
            if(DEBUG)
                de.printStackTrace();
            throw de;
        } catch (Exception e) {
            if(DEBUG)
                e.printStackTrace();
            throw new dap4.core.util.DapException(e);
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
