/*
 * Copyright (c) 2012-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dap4.cdm.dsp;

import dap4.cdm.CDMTypeFcns;
import dap4.cdm.CDMUtil;
import dap4.cdm.NodeMap;
import dap4.core.data.DataCursor;
import dap4.core.dmr.*;
import dap4.core.util.Convert;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.AbstractDSP;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
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
    static protected final boolean DUMPCDL = false;

    // NetcdfDataset enhancement to use: need only coord systems
    static protected Set<NetcdfDataset.Enhance> ENHANCEMENT = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

    static final protected String FILLVALUE = "_FillValue";

    //////////////////////////////////////////////////
    // Class variables

    static protected boolean nc4loaded = false;

    //////////////////////////////////////////////////
    // Class methods


    //////////////////////////////////////////////////
    // Instance variables

    protected NetcdfDataset ncdfile = null;
    protected boolean closed = false;
    protected DMRFactory dmrfactory = null;

    // Keep various context-dependent maps between
    // CDMNode instances and DapNode instances
    // package access

    // Variable map
    protected NodeMap<Variable, DapVariable> varmap = new NodeMap<>();

    // Variable <-> DapStructure (basetype) map; compound types only
    protected NodeMap<Variable, DapStructure> compoundmap = new NodeMap<>();

    // Map Variable <-> DapSequence vlen type
    protected NodeMap<Variable, DapSequence> vlenmap = new NodeMap<>();

    // Map All other kinds of CDMNode <-> DapNode
    protected NodeMap<CDMNode, DapNode> nodemap = new NodeMap<>();

    //////////////////////////////////////////////////
    // Constructor(s)

    public CDMDSP()
    {
    }

    public CDMDSP(String path)
            throws DapException
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
     * @throws DapException
     */
    @Override
    public CDMDSP
    open(String filepath)
            throws DapException
    {
        try {
            NetcdfFile ncfile = createNetcdfFile(filepath, null);
            NetcdfDataset ncd = new NetcdfDataset(ncfile, ENHANCEMENT);
            return open(ncd);
        } catch (IOException ioe) {
            throw new DapException("CDMDSP: cannot process: " + filepath, ioe);
        }
    }

    /**
     * Provide an extra API for use in testing
     *
     * @param ncd
     * @return
     * @throws DapException
     */
    public CDMDSP open(NetcdfDataset ncd)
            throws DapException
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
        if (this.ncdfile != null)
            this.ncdfile.close();
    }


    @Override
    public DataCursor
    getVariableData(DapVariable var)
            throws DapException
    {
        Variable cdmvar = this.varmap.get(var);
        if (cdmvar == null)
            throw new DapException("Unknown variable: " + var);
        CDMCursor vardata = (CDMCursor) super.getVariableData(var);
        if (vardata == null) {
            DataCursor.Scheme scheme = CDMCursor.schemeFor(var);
            try {
                vardata = new CDMCursor(scheme, this, var, null);
                vardata.setArray(cdmvar.read());
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

    //////////////////////////////////////////////////
    // Nodemap Management

    /* Make sure that we use the proper
     * object in order to avoid identity
     * problems.
     */

    /**
     * Track generic CDMNode <-> DapNode
     */
    protected void
    recordNode(CDMNode cdm, DapNode dap)
    {
        assert this.nodemap.get(cdm) == null && this.nodemap.get(dap) == null;
        this.nodemap.put(cdm, dap);
    }

    /**
     * Track Variable <-> DapVariable
     */
    protected void
    recordVar(Variable cdm, DapVariable dap)
    {
        cdm = CDMUtil.unwrap(cdm);
        assert varmap.get(cdm) == null && varmap.get(dap) == null;
        varmap.put(cdm, dap);
    }

    /**
     * Track Variable <-> DapStructure
     */
    protected void
    recordStruct(Variable cdm, DapStructure dap)
    {
        cdm = CDMUtil.unwrap(cdm);
        assert this.nodemap.get(cdm) == null && this.nodemap.get(dap) == null;
        compoundmap.put(cdm, dap);
    }

    /**
     * Track Variable <-> DapSequence
     */
    protected void
    recordSeq(Variable cdm, DapSequence dap)
    {
        cdm = CDMUtil.unwrap(cdm);
        assert this.vlenmap.get(cdm) == null && this.vlenmap.get(dap) == null;
        vlenmap.put(cdm, dap);
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
/*
    protected DapNode
    lookupNode(CDMNode cdmnode)
    {
        CDMSort sort = cdmnode.getSort();
        if(sort == CDMSort.VARIABLE || sort == CDMSort.STRUCTURE) {
            Variable basev = CDMUtil.unwrap((Variable) cdmnode);
            assert (basev != null) : "Unwrap() failed";
            cdmnode = (CDMNode) basev;
        }
        return this.nodemap.get(cdmnode);
    }
*/

    //////////////////////////////////////////////////

    /**
     * Extract the metadata from the NetcdfDataset
     * and build the DMR.
     */

    public void
    buildDMR()
            throws DapException
    {
        if (getDMR() != null)
            return;
        try {
            if (DUMPCDL) {
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
            if (index >= 0)
                name = name.substring(index + 1, name.length());
            // Initialize the root dataset node
            setDMR((DapDataset) dmrfactory.newDataset(name).annotate(NetcdfDataset.class, this.ncdfile));
            // Map the CDM root group to this group
            recordNode(this.ncdfile.getRootGroup(), getDMR());
            getDMR().setBase(DapUtil.canonicalpath(this.ncdfile.getLocation()));

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

        } catch (DapException e) {
            setDMR(null);
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
        for (Dimension cdmdim : cdmgroup.getDimensions()) {
            DapDimension dapdim = builddim(cdmdim);
        }
        // Create decls in dap group for Enumerations
        for (EnumTypedef cdmenum : cdmgroup.getEnumTypedefs()) {
            String name = cdmenum.getShortName();
            DapEnumeration dapenum = buildenum(cdmenum);
            dapenum.setShortName(name);
            dapgroup.addDecl(dapenum);
        }
        // Create decls in dap group for vlen induced Sequences
        // Do this before building compound types
        for (Variable cdmvar0 : cdmgroup.getVariables()) {
            Variable cdmvar = CDMUtil.unwrap(cdmvar0);
            buildseqtypes(cdmvar);
        }
        // Create decls in dap group for Compound Types
        for (Variable cdmvar0 : cdmgroup.getVariables()) {
            Variable cdmvar = CDMUtil.unwrap(cdmvar0);
            if (cdmvar.getDataType() != DataType.STRUCTURE
                    && cdmvar.getDataType() != DataType.SEQUENCE)
                continue;
            DapStructure struct = buildcompoundtype(cdmvar, dapgroup);
        }

        // Create decls in dap group for Variables
        for (Variable cdmvar0 : cdmgroup.getVariables()) {
            Variable cdmvar = CDMUtil.unwrap(cdmvar0);
            DapNode newvar = buildvariable(cdmvar, dapgroup, cdmvar.getDimensions());
        }
        // Create decls in dap group for subgroups
        for (Group subgroup : cdmgroup.getGroups()) {
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
        if (cdmdim.isVariableLength())
            throw new DapException("* dimensions not supported");
        DapDimension dapdim = null;
        long cdmsize = dapsize(cdmdim);
        String name = cdmdim.getShortName();
        if (name != null && name.length() == 0)
            name = null;
        boolean shared = cdmdim.isShared();
        if (!shared) {
            // Unlike the parser, since we are working
            // from a NetcdfDataset instance, there might
            // be multiple anonymous dimension objects
            // the same size. So, just go ahead and create
            // multiple instances.
            dapdim = (DapDimension) dmrfactory.newDimension(null, cdmsize);
            getDMR().addDecl(dapdim);
        } else { // Non anonymous; create in current group
            dapdim = (DapDimension) dmrfactory.newDimension(name, cdmsize);
            dapdim.setShared(true);
            if (cdmdim.isUnlimited()) {
                dapdim.setUnlimited(true);
            }
            Group cdmparent = cdmdim.getGroup();
            DapGroup dapparent = (DapGroup) this.nodemap.get(cdmparent);
            assert dapparent != null;
            assert (dapparent != null);
            dapparent.addDecl(dapdim);
        }
        recordNode(cdmdim, dapdim);
        return dapdim;
    }

    protected DapEnumeration
    buildenum(EnumTypedef cdmenum)
            throws DapException
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
        DapEnumeration dapenum = (DapEnumeration) dmrfactory.newEnumeration(cdmenum.getShortName(), base);
        recordNode(cdmenum, dapenum);
        // Create the enum constants
        Map<Integer, String> ecvalues = cdmenum.getMap();
        for (Map.Entry<Integer, String> entry : ecvalues.entrySet()) {
            String name = entry.getValue();
            assert (name != null);
            int value = (int) entry.getKey();
            dapenum.addEnumConst(dmrfactory.newEnumConst(name, new Long(value)));
        }
        return dapenum;
    }

    protected DapStructure
    buildcompoundtype(Variable cdmvar, DapNode parent)
            throws DapException
    {
        cdmvar = CDMUtil.unwrap(cdmvar);
        DapStructure struct;
        if (cdmvar.getDataType() == DataType.STRUCTURE)
            struct = (DapStructure) dmrfactory.newStructure(cdmvar.getShortName());
        else if (cdmvar.getDataType() == DataType.SEQUENCE)
            struct = (DapStructure) dmrfactory.newSequence(cdmvar.getShortName());
        else
            throw new DapException("Internal error");
        struct.setParent(parent);
        recordStruct(cdmvar, struct);
        Structure cdmstruct = (Structure) cdmvar;
        List<Variable> fields = cdmstruct.getVariables();
        // recurse to create compound types of any fields c
        for (CDMNode node : fields) {
            Variable cdmfield = (Variable) node;
            List<Dimension> dimset = cdmfield.getDimensions();
            // Recurse on any nested compound types
            if (cdmfield.getDataType() == DataType.STRUCTURE
                    || cdmfield.getDataType() == DataType.SEQUENCE) {
                DapStructure fieldstruct = buildcompoundtype(cdmfield, struct);
            }
        }
        // Add the fields to this compound type
        for (CDMNode node : fields) {
            Variable cdmfield = (Variable) node;
            DapType basetype = null;
            switch (cdmfield.getDataType()) {
                default:
                    basetype = CDMTypeFcns.cdmtype2daptype(cdmfield.getDataType());
                    break;
                case STRUCTURE:
                case SEQUENCE:
                    basetype = compoundmap.get(cdmfield);// since no forward references
                    break;
            }
            // build the field variable
            List<Dimension> fielddims = cdmfield.getDimensions();
            DapVariable dapfield;
            if (CDMUtil.hasVLEN(cdmfield)) {
                // Get the fake sequence associated with this vlen
                DapSequence seq = vlenmap.get(cdmfield);
                List<Dimension> coredims = getCoreDimset(fielddims);
                // We need to build a variable whose basetype is the
                // fake sequence rather than the cdmfield type
                // First, build a variable using the existing cdmfield
                dapfield = buildvariable(cdmfield, struct, coredims);
                // Now modify it to use the fake sequence
                dapfield.setBaseType(seq);
            } else {
                dapfield = buildvariable(cdmfield, struct, fielddims);
            }
            struct.addField(dapfield);
        }
        return struct;
    }

    protected DapVariable
    buildvariable(Variable cdmbasevar, DapNode parent, List<Dimension> cdmdims)
            throws DapException
    {
        DapVariable dapvar = null;
        CDMSort sort = cdmbasevar.getSort();
        switch (sort) {
            case VARIABLE:
                switch (cdmbasevar.getDataType()) {
                    default:
                        dapvar = buildatomicvar(cdmbasevar, parent);
                        break;
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
                    case SEQUENCE:
                        assert false : "Internal error"; // How could this ever happen?
                        break;

                }
                builddimrefs(dapvar, cdmdims);
                break;
            case STRUCTURE:
                dapvar = buildstructvar(cdmbasevar);
                builddimrefs(dapvar, cdmdims);
                break;
            case SEQUENCE:
            default:
                assert false : "Internal Error";
        }
        if (parent != null)
            addToParent(parent, dapvar);
        return dapvar;
    }

    protected DapVariable
    buildatomicvar(Variable cdmvar, DapNode parent)
            throws DapException
    {
        // Atomic => not opaque and not enum
        DapType basetype = CDMTypeFcns.cdmtype2daptype(cdmvar.getDataType());
        if (basetype == null)
            throw new DapException("DapFile: illegal CDM variable base type: " + cdmvar.getDataType());
        DapVariable dapvar = (DapVariable) dmrfactory.newVariable(cdmvar.getShortName(), basetype);
        recordVar(cdmvar, dapvar);
        buildattributes(dapvar, cdmvar.getAttributes());
        if (CDMUtil.hasVLEN(cdmvar)) {
            // Get the fake sequence associated with this vlen
            DapSequence seq = vlenmap.get(cdmvar);
            List<Dimension> coredims = getCoreDimset(cdmvar.getDimensions());
            // We need to build a variable whose basetype is the
            // fake sequence rather than the cdmfield type
            // First, build a variable using the existing cdmfield
            // Now modify it to use the fake sequence
            dapvar.setBaseType(seq);
        }
        return dapvar;
    }

    protected DapVariable
    buildopaquevar(Variable cdmvar)
            throws DapException
    {
        assert (cdmvar.getDataType() == DataType.OPAQUE) : "Internal error";
        DapVariable dapvar = (DapVariable) dmrfactory.newVariable(cdmvar.getShortName(), DapType.OPAQUE);
        recordVar(cdmvar, dapvar);
        buildattributes(dapvar, cdmvar.getAttributes());
        Object osize = cdmvar.annotation(UCARTAGOPAQUE);
        if (osize != null) {
            dapvar.addXMLAttribute(UCARTAGOPAQUE, osize.toString());
        }
        return dapvar;
    }

    protected DapVariable
    buildstringvar(Variable cdmvar)
            throws DapException
    {
        assert (cdmvar.getDataType() == DataType.STRING) : "Internal error";
        DapVariable dapvar = (DapVariable) dmrfactory.newVariable(cdmvar.getShortName(), DapType.STRING);
        recordVar(cdmvar, dapvar);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    protected DapVariable
    buildenumvar(Variable cdmvar)
            throws DapException
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
        DapEnumeration dapenum = (DapEnumeration) this.nodemap.get(trueenumdef);
        assert (dapenum != null);
        DapVariable dapvar = (DapVariable) dmrfactory.newVariable(cdmvar.getShortName(), dapenum);
        recordVar(cdmvar, dapvar);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    protected DapVariable
    buildstructvar(Variable cdmvar)
            throws DapException
    {
        assert (cdmvar.getDataType() == DataType.STRUCTURE) : "Internal error";
        // Find the DapStructure that is the basetype for this var
        DapStructure struct = compoundmap.get(cdmvar);
        assert struct != null : "Internal Error";
        DapVariable dapvar = (DapVariable) dmrfactory.newVariable(cdmvar.getShortName(), struct);
        recordVar(cdmvar, dapvar);
        buildattributes(dapvar, cdmvar.getAttributes());
        return dapvar;
    }

    /* Create a sequence from a variable with a
       variable length last dimension.
       Suppose we have cdm equivalent to this:
            T var[d1]...[dn]][*]
       We convert to the following
           <Sequence name="var">
             <T name="var"/>
             <Dim name=d1/>
             ...
             <Dim name=dn/>
           </Sequence>
    */

    protected DapSequence
    buildseqtype(Variable cdmvar)
            throws DapException
    {
        cdmvar = CDMUtil.unwrap(cdmvar);
        assert (CDMUtil.hasVLEN(cdmvar));
        DataType dt = cdmvar.getDataType();
        DapType daptype = CDMTypeFcns.cdmtype2daptype(dt);
        DapSequence seq = (DapSequence) dmrfactory.newSequence(cdmvar.getShortName());
        // fill DapSequence with a single field; note that the dimensions
        // are elided because they will attach to the sequence variable,
        // not the field
        DapVariable field = dmrfactory.newVariable(cdmvar.getShortName(), daptype);
        seq.addField(field);
        field.setParent(seq);
        recordSeq(cdmvar, seq);
        return seq;
    }

    /**
     * Walk this variable, including fields, to construct sequence types
     * for any contained vlen dimensions
     *
     * @param cdmvar variable to walk
     */

    protected void
    buildseqtypes(Variable cdmvar)
            throws DapException
    {
        if (CDMUtil.hasVLEN(cdmvar)) {
            buildseqtype(cdmvar);
        }
        if (cdmvar.getDataType() == DataType.STRUCTURE
                || cdmvar.getDataType() == DataType.SEQUENCE) {
            Structure struct = (Structure) cdmvar;
            List<Variable> fields = struct.getVariables();
            for (int i = 0; i < fields.size(); i++) {
                Variable field = fields.get(i);
                buildseqtypes(field); // recurse for inner vlen dims
            }
        }
    }

    protected void
    buildattributes(DapNode node, List<Attribute> attributes)
            throws DapException
    {
        for (Attribute attr : attributes) {
            if (!suppress(attr.getShortName())) {
                DapAttribute dapattr = buildattribute(attr);
                node.addAttribute(dapattr);
            }
        }
    }

    protected DapAttribute
    buildattribute(Attribute cdmattr)
            throws DapException
    {
        DapType attrtype = CDMTypeFcns.cdmtype2daptype(cdmattr.getDataType());
        EnumTypedef cdmenum = cdmattr.getEnumType();
        boolean enumfillvalue = (cdmattr.getShortName().equals(FILLVALUE)
                && cdmenum != null);
        DapEnumeration dapenum = null;

        // We need to handle _FillValue specially if the
        // the variable is enum typed.
        if (enumfillvalue) {
            cdmenum = findMatchingEnum(cdmenum);
            // Make sure the cdm attribute has type enumx
            if (!cdmenum.getBaseType().isEnum())
                throw new DapException("CDM _FillValue attribute type is not enumX");
            // Modify the attr
            cdmattr.setEnumType(cdmenum);
            // Now, map to a DapEnumeration
            dapenum = (DapEnumeration) this.nodemap.get(cdmenum);
            if (dapenum == null)
                throw new DapException("Illegal CDM variable attribute type: " + cdmenum);
            attrtype = dapenum;
        }
        if (attrtype == null)
            throw new DapException("DapFile: illegal CDM variable attribute type: " + cdmattr.getDataType());
        DapAttribute dapattr = (DapAttribute) dmrfactory.newAttribute(cdmattr.getShortName(), attrtype);
        recordNode(cdmattr, dapattr);
        // Transfer the values
        Array values = cdmattr.getValues();
        if (!validatecdmtype(cdmattr.getDataType(), values.getElementType()))
            throw new DapException("Attr type versus attribute data mismatch: " + values.getElementType());
        IndexIterator iter = values.getIndexIterator();
        String[] valuelist = null;
        Object vec = CDMTypeFcns.createVector(cdmattr.getDataType(), values.getSize());
        for (int i = 0; iter.hasNext(); i++) {
            java.lang.reflect.Array.set(vec, i, iter.next());
        }
        valuelist = (String[]) Convert.convert(DapType.STRING, attrtype, vec);
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
            throws DapException
    {
        if (cdmdims == null || cdmdims.size() == 0)
            return;
        // It is unfortunately the case that the dimensions
        // associated with the variable are not
        // necessarily the same object as those dimensions
        // as declared, so we need to use a non-trivial
        // matching algorithm.
        for (Dimension cdmdim : cdmdims) {
            DapDimension dapdim = null;
            if (cdmdim.isShared()) {
                Dimension declareddim = finddimdecl(cdmdim);
                if (declareddim == null)
                    throw new DapException("Unprocessed cdm dimension: " + cdmdim);
                dapdim = (DapDimension) this.nodemap.get(declareddim);
                assert dapdim != null;
            } else if (cdmdim.isVariableLength()) {// ignore
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
        for (Variable v0 : g.getVariables()) {
            Variable cdmvar = CDMUtil.unwrap(v0);
            if (cdmvar == null)
                throw new DapException("NetcdfDataset synthetic variable: " + v0);
            DapNode dapvar = this.varmap.get(cdmvar);
            if (dapvar == null)
                throw new DapException("Unknown variable: " + cdmvar);
            if (!(dapvar instanceof DapVariable))
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
        if (var.getSort() == CDMSort.VARIABLE) {
            VariableDS vds = (VariableDS) var;
            css = vds.getCoordinateSystems();
        } else {
            StructureDS sds = (StructureDS) var;
            css = sds.getCoordinateSystems();
        }
        if (css != null && css.size() > 0) {
            // Not sure what to do with multiple coordinate systems
            // For now, only use the first
            CoordinateSystem coordsystems = css.get(0);
            for (CoordinateAxis axis : coordsystems.getCoordinateAxes()) {
                // First step is to find the dap variable
                // corresponding to the map
                VariableDS vds = (VariableDS) axis.getOriginalVariable();
                if (vds != null) {
                    Variable v = CDMUtil.unwrap(vds);
                    if (v != null) {
                        DapVariable mapvar = varmap.get(v);
                        if (mapvar == null)
                            throw new DapException("Illegal map variable:" + v.toString());
                        if (!mapvar.isAtomic())
                            throw new DapException("Non-atomic map variable:" + v.toString());
                        // Ignore maps where the map variable is inside this scope
                        /*
                            if(!mapvar.isTopLevel()) {
                            DapNode parent = mapvar.getContainer();
                            switch (parent.getSort()) {
                            case SEQUENCE:
                            case STRUCTURE:
                                if(dapvar.getBaseType() == parent) // Do we need to do transitive closure?
                                    throw new DapException("Map var cannot be in same structure as map");
                                break;
                            default:
                                assert false : "Unexpected container type";
                            }
                            */
                        DapMap map = (DapMap) dmrfactory.newMap(mapvar);
                        dapvar.addMap(map);
                    }
                }
            }
        }
    }

    protected DapGroup
    buildgroup(Group cdmgroup)
            throws DapException
    {
        DapGroup dapgroup = (DapGroup) dmrfactory.newGroup(cdmgroup.getShortName());
        recordNode(cdmgroup, dapgroup);
        dapgroup.setShortName(cdmgroup.getShortName());
        fillgroup(dapgroup, cdmgroup);
        return dapgroup;
    }

    //////////////////////////////////////////////////
    // Utilities

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
            throws DapException
    {
        List<EnumTypedef> candidates = new ArrayList<>();
        for (Map.Entry<DapNode, CDMNode> entry : this.nodemap.getCDMMap().entrySet()) {
            CDMNode cdmnode = entry.getValue();
            if (cdmnode.getSort() != CDMSort.ENUMERATION)
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
            if (targetmap.size() != varmap.size())
                continue;
            boolean match = true; // until otherwise shown
            for (Map.Entry<Integer, String> tpair : targetmap.entrySet()) {
                String tname = tpair.getValue();
                int value = (int) tpair.getKey();
                boolean found = false;
                for (Map.Entry<Integer, String> vpair : varmap.entrySet()) {
                    if (tname.equals(vpair.getValue()) && value == (int) vpair.getKey()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    match = false;
                    break;
                }
            }
            if (!match)
                continue;

            // Save it unless it is shadowed by a closer enum
            boolean shadowed = false;
            for (EnumTypedef etd : candidates) {
                if (shadows(etd.getGroup(), target.getGroup())) {
                    shadowed = true;
                    break;
                }
            }
            if (!shadowed)
                candidates.add(target);
        }

        switch (candidates.size()) {
            case 0:
                throw new DapException("CDMDSP: No matching enum type decl: " + varenum.getShortName());
            case 1:
                break;
            default:
                throw new DapException("CDMDSP: Multiple matching enum type decls: " + varenum.getShortName());
        }
        return candidates.get(0);
    }

    protected boolean
    shadows(Group parent, Group child)
    {
        if (child == parent)
            return true;
        Group candidate = child;
        do {
            candidate = candidate.getGroup();
        } while (candidate != null && candidate != parent);
        return (candidate == parent);
    }

    // Convert cdm size to DapDimension size
    protected long
    dapsize(Dimension cdmdim)
    {
        assert (!cdmdim.isVariableLength());
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
        for (Map.Entry<DapNode, CDMNode> entry : this.nodemap.getCDMMap().entrySet()) {
            if (entry.getValue().getSort() != CDMSort.DIMENSION)
                continue;
            Dimension d = (Dimension) entry.getValue();
            if (isDimDeclFor(d, dimref))
                return d;
        }
        return null;
    }

    protected boolean isDimDeclFor(Dimension decl, Dimension ref)
    {
        if(!decl.isShared())
            return false; // Has no name
        // First check shortname and size
        if (!decl.getShortName().equals(ref.getShortName()))
            return false;
        if (decl.getLength() != ref.getLength())
            return false;
        // Make sure they are in the same group
        String dprefix = decl.getGroup().getFullName();
        String rprefix = ref.getGroup().getFullName();
        return (dprefix.equals(rprefix));
    }

    /*protected String
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
    } */

    //////////////////////////////////////////////////

    protected NetcdfFile
    createNetcdfFile(String location, CancelTask canceltask)
            throws DapException
    {
        try {
            NetcdfFile ncfile = NetcdfFile.open(location, -1, canceltask, getContext());
            return ncfile;
        } catch (DapException de) {
            if (DEBUG)
                de.printStackTrace();
            throw de;
        } catch (Exception e) {
            if (DEBUG)
                e.printStackTrace();
            throw new DapException(e);
        }
    }

    //////////////////////////////////////////////////
    // Utilities

    /**
     * Strip vlen dimensions from a set of dimensions
     *
     * @param dimset
     * @return subset of dimset with (trailing) vlen removed
     * @throws DapException
     */
    static List<Dimension>
    getCoreDimset(List<Dimension> dimset)
            throws DapException
    {
        if (dimset == null) return null;
        List<Dimension> core = new ArrayList<>();
        int pos = -1;
        int count = 0;
        for (int i = 0; i < dimset.size(); i++) {
            if (dimset.get(i).isVariableLength()) {
                pos = i;
                count++;
            } else
                core.add(dimset.get(i));
        }
        if ((pos != dimset.size() - 1) || count > 1)
            throw new DapException("Unsupported use of (*) Dimension");
        return core;
    }

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
        if (attrname.startsWith("_Coord")) return true;
        if (attrname.equals(CDM.UNSIGNED))
            return true;
        return false;
    }

    protected void
    addToParent(DapNode parent, DapVariable dapvar)
            throws DapException
    {
        assert (parent != null);
        switch (parent.getSort()) {
            case GROUP:
            case DATASET:
                ((DapGroup) parent).addDecl(dapvar);
                break;
            case SEQUENCE:
            case STRUCTURE:
                dapvar.setParent(parent);
                break;
            default:
                assert (false) : "Internal error";
        }
    }

}
