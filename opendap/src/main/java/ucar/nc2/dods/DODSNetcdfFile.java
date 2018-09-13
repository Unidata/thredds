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
package ucar.nc2.dods;

import com.google.common.base.Joiner;

import net.jcip.annotations.NotThreadSafe;
import ucar.nc2.constants.CF;
import ucar.nc2.util.EscapeStrings;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.Attribute;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.*;

import opendap.dap.*;
import opendap.dap.parsers.*;
import ucar.nc2.util.rc.RC;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.util.*;
import java.util.Enumeration;
import java.nio.channels.WritableByteChannel;

/**
 * Access to DODS datasets through the Netcdf API.
 *
 * @author caron
 * @see ucar.nc2.NetcdfFile
 */
@NotThreadSafe
public class DODSNetcdfFile extends ucar.nc2.NetcdfFile implements Closeable
{
    // temporary flag to control usegroup changes
    static boolean OLDGROUPCODE = false;


    static public boolean debugCE = false;
    static public boolean debugServerCall = false;
    static public boolean debugOpenResult = false;
    static public boolean debugDataResult = false;
    static public boolean debugCharArray = false;
    static public boolean debugConvertData = false;
    static public boolean debugConstruct = false;
    static public boolean debugPreload = false;
    static public boolean debugTime = false;
    static public boolean showNCfile = false;
    static public boolean debugAttributes = false;
    static public boolean debugCached = false;
    static public boolean debugOpenTime = false;

    // Define a utility class to decompse names
    private static class NamePieces
    {
        String prefix = null; // group part of the path
        String var = null;    // struct part of the path
        String name = null;   // last name in a path
    }

    /**
     * Set whether to allow sessions by allowing cookies. This only affects requests to the TDS.
     * Setting this to true can eliminate consistency problems for datasets that are being updated.
     *
     * @param b true or false. default is false.
     */
    static public void setAllowSessions(boolean b)
    {
        DConnect2.setAllowSessions(b);
    }

    static private boolean accept_compress = false;

    /**
     * Set whether to allow messages to be compressed.
     *
     * @param b true or false.
     * @deprecated use setAllowCompression
     */
    static public void setAllowDeflate(boolean b)
    {
        accept_compress = b;
    }

    /**
     * Set whether to allow messages to be compressed.
     *
     * @param b true or false.
     */
    static public void setAllowCompression(boolean b)
    {
        accept_compress = b;
    }

    /**
     * Debugging flags. This is a way to decouple setting flags from particular implementations.
     *
     * @param debugFlag set of debug flags.
     */
    static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag)
    {
        debugCE = debugFlag.isSet("DODS/constraintExpression");
        debugServerCall = debugFlag.isSet("DODS/serverCall");
        debugOpenResult = debugFlag.isSet("DODS/debugOpenResult");
        debugDataResult = debugFlag.isSet("DODS/debugDataResult");
        debugCharArray = debugFlag.isSet("DODS/charArray");
        debugConstruct = debugFlag.isSet("DODS/constructNetcdf");
        debugPreload = debugFlag.isSet("DODS/preload");
        debugTime = debugFlag.isSet("DODS/timeCalls");
        showNCfile = debugFlag.isSet("DODS/showNCfile");
        debugAttributes = debugFlag.isSet("DODS/attributes");
        debugCached = debugFlag.isSet("DODS/cache");
    }

    static private boolean preload = true;
    static private boolean useGroups = false;
    static private int preloadCoordVarSize = 50000; // default 50K

    /**
     * Set whether small variables are preloaded; only turn off for debugging.
     *
     * @param b true if small variables are preloaded (default true)
     */
    static public void setPreload(boolean b)
    {
        preload = b;
    }

    /**
     * If preloading, set maximum size of coordinate variables to be preloaded.
     *
     * @param size maximum size of coordinate variables to be preloaded.
     */
    static public void setCoordinateVariablePreloadSize(int size)
    {
        preloadCoordVarSize = size;
    }

    /**
     * Create the canonical form of the URL.
     * If the urlName starts with "http:" or "https:", change it to start with "dods:", otherwise
     * leave it alone.
     *
     * @param urlName the url string
     * @return canonical form
     */
    public static String canonicalURL(String urlName)
    {
        if (urlName.startsWith("http:"))
            return "dods:" + urlName.substring(5);
        if (urlName.startsWith("https:"))
            return "dods:" + urlName.substring(6);
        return urlName;
    }

    static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DODSNetcdfFile.class);

    //////////////////////////////////////////////////////////////////////////////////
    private ConvertD2N convertD2N = new ConvertD2N();
    private DConnect2 dodsConnection = null;
    private DDS dds;
    private DAS das;

    /**
     * Open a DODS file.
     *
     * @param datasetURL URL of the file. This should start with the protocol "dods:"
     *                   It may also start with protocol "http:".
     * @throws IOException                    on io error
     * @throws java.net.MalformedURLException
     */
    public DODSNetcdfFile(String datasetURL) throws IOException
    {
        this(datasetURL, null);
    }

    /**
     * Open a DODS file, allow user control over preloading string arrays and making structure data
     * available through netcdf API.
     *
     * @param datasetURL URL of the file. This should start with the protocol "dods:" or "http:".
     * @param cancelTask check if task is cancelled. may be null.
     * @throws IOException                    on io error
     * @throws java.net.MalformedURLException
     */
    public DODSNetcdfFile(String datasetURL, CancelTask cancelTask) throws IOException
    {
        super();
        long start = System.currentTimeMillis();

        // canonicalize name
        String urlName = datasetURL; // actual URL uses http:
        this.location = datasetURL; // canonical name uses "dods:"
        if (datasetURL.startsWith("dods:")) {
            urlName = "http:" + datasetURL.substring(5);
        } else if (datasetURL.startsWith("http:")) {
            this.location = "dods:" + datasetURL.substring(5);
        } else if (datasetURL.startsWith("https:")) {
            this.location = "dods:" + datasetURL.substring(6);
        } else if (datasetURL.startsWith("file:")) {
            this.location = datasetURL;
        } else {
            throw new java.net.MalformedURLException(datasetURL + " must start with dods: or http: or file:");
        }

        if (debugServerCall) System.out.println("DConnect to = <" + urlName + ">");
        dodsConnection = new DConnect2(urlName, accept_compress);
        if (cancelTask != null && cancelTask.isCancel()) return;

        // fetch the DDS and DAS
        try {
            dds = dodsConnection.getDDS();
            if (debugServerCall) System.out.println("DODSNetcdfFile readDDS");
            if (debugOpenResult) {
                System.out.println("DDS = ");
                dds.print(System.out);
            }
            if (cancelTask != null && cancelTask.isCancel()) return;

            das = dodsConnection.getDAS();
            if (debugServerCall) System.out.println("DODSNetcdfFile readDAS");
            if (debugOpenResult) {
                System.out.println("DAS = ");
                das.print(System.out);
            }
            if (cancelTask != null && cancelTask.isCancel()) return;

            if (debugOpenResult)
                System.out.println("dodsVersion = " + dodsConnection.getServerVersion());

        } catch (opendap.dap.parsers.ParseException e) {
            logger.info("DODSNetcdfFile " + datasetURL, e);
            if (debugOpenResult)
                System.out.println("open failure = " + e.getMessage());
            throw new IOException(e.getMessage()+"; URL="+datasetURL);

        } catch (opendap.dap.DASException e) {
            logger.info("DODSNetcdfFile " + datasetURL, e);
            if (debugOpenResult)
                System.out.println("open failure = " + e.getClass().getName() + ": " + e.getMessage());
            throw new IOException(e.getClass().getName() + ": " + e.getMessage()+"; URL="+datasetURL);
        } catch (opendap.dap.DDSException e) {
            logger.info("DODSNetcdfFile " + datasetURL, e);
            if (debugOpenResult)
                System.out.println("open failure = " + e.getClass().getName() + ": " + e.getMessage());
            throw new IOException(e.getClass().getName() + ": " + e.getMessage()+"; URL="+datasetURL);
        } catch (DAP2Exception dodsE) {
            //dodsE.printStackTrace();
            if (dodsE.getErrorCode() == DAP2Exception.NO_SUCH_FILE)
                throw new FileNotFoundException(dodsE.getMessage()+"; URL="+datasetURL);
            else {
                dodsE.printStackTrace(System.err);
                throw new IOException("URL="+datasetURL,dodsE);
            }
        } catch (Exception e) {
            logger.info("DODSNetcdfFile " + datasetURL, e);
            if (debugOpenResult)
                System.out.println("open failure = " + e.getClass().getName() + ": " + e.getMessage());
            throw new IOException(e.getClass().getName() + ": " + e.getMessage()+"; URL="+datasetURL);
        }

        // now initialize the DODSNetcdf metadata
        DodsV rootDodsV = DodsV.parseDDS(dds);
        rootDodsV.parseDAS(das);
        if (cancelTask != null && cancelTask.isCancel()) return;

        // LOOK why do we want to do the primitives seperate from compounds?
        constructTopVariables(rootDodsV, cancelTask);
        if (cancelTask != null && cancelTask.isCancel()) return;

        //preload(dodsVlist, cancelTask); LOOK not using preload yet
        //if (cancelTask != null && cancelTask.isCancel()) return;

        constructConstructors(rootDodsV, cancelTask);
        if (cancelTask != null && cancelTask.isCancel()) return;
        finish();

        parseGlobalAttributes(das, rootDodsV, this);
        if (cancelTask != null && cancelTask.isCancel()) return;

        if (RC.getUseGroups()) {
            try {
                reGroup();
            } catch (DAP2Exception dodsE) {
                dodsE.printStackTrace(System.err);
                throw new IOException(dodsE);
            }
        }

        /* look for coordinate variables
       for (Variable v : variables) {
         if (v instanceof DODSVariable)
       ((DODSVariable) v).calcIsCoordinateVariable();
       } */

        // see if theres a CE: if so, we need to reset the dodsConnection without it,
        // since we are reusing dodsConnection; perhaps this is not needed?
        // may be true now that weve consolidated metadata reading
        // no comprende
        int pos;
        if (0 <= (pos = urlName.indexOf('?'))) {
            String datasetName = urlName.substring(0, pos);
            if (debugServerCall) System.out.println(" reconnect to = <" + datasetName + ">");
            dodsConnection = new DConnect2(datasetName, accept_compress);

            // parse the CE for projections
            String CE = urlName.substring(pos + 1);
            StringTokenizer stoke = new StringTokenizer(CE, " ,");
            while (stoke.hasMoreTokens()) {
                String proj = stoke.nextToken();
                int subsetPos = proj.indexOf('[');
                if (debugCE) System.out.println(" CE = " + proj + " " + subsetPos);
                if (subsetPos > 0) {
                    String vname = proj.substring(0, subsetPos);
                    String vCE = proj.substring(subsetPos);
                    if (debugCE) System.out.println(" vCE = <" + vname + "><" + vCE + ">");
                    DODSVariable dodsVar = (DODSVariable) findVariable(vname);
                    if (dodsVar == null)
                        throw new IOException("Variable not found: " + vname);

                    dodsVar.setCE(vCE);
                    dodsVar.setCaching(true);
                }
            }
        }

        // preload scalers, coordinate variables, strings, small arrays
        if (preload) {
            List<Variable> preloadList = new ArrayList<Variable>();
            for (Variable dodsVar : variables) {
                long size = dodsVar.getSize() * dodsVar.getElementSize();
                if ((dodsVar.isCoordinateVariable() && size < preloadCoordVarSize) || dodsVar.isCaching() || dodsVar.getDataType() == DataType.STRING) {
                    dodsVar.setCaching(true);
                    preloadList.add(dodsVar);
                    if (debugPreload) System.out.printf("  preload (%6d) %s%n", size, dodsVar.getFullName());
                }
            }
            if (cancelTask != null && cancelTask.isCancel()) return;
            readArrays(preloadList);
        }

        finish();
        if (showNCfile)
            System.out.println("DODS nc file = " + this);
        long took = System.currentTimeMillis() - start;
        if (debugOpenTime) System.out.printf(" took %d msecs %n", took);
    }

    @Override
    public synchronized void close() throws java.io.IOException
    {
      if (cache != null) {
        if (cache.release(this)) return;
      }

      if (null != dodsConnection) {
          dodsConnection.close();
          dodsConnection = null;
      }

    }

    /* parse the DDS, creating a tree of DodsV objects
   private ArrayList parseDDS(DDS dds) throws IOException {
     ArrayList dodsVlist	 = new ArrayList();

     // recursively get the Variables from the DDS
     Enumeration variables = dds.getVariables();
     parseVariables( null, variables, dodsVlist);

     // assign depth first sequence number
     nextInSequence = 0;
     assignSequence( dodsVlist);
     return dodsVlist;
   }

   // DDS -> {BaseType} for arrays, BaseType = DArray -> {elemType}
   // here we 1) put all Variables into a DodsV, 2) unravel DConstructors (DSequence, DStructure, DGrid)
   // 3) for Darray, we put Variable = elemType, and store the darray seperately, not in the heirarchy.
   // so you need to get the parent from the dodsV.
   private void parseVariables( DodsV parent, Enumeration variables, ArrayList dodsVlist) {
     while (variables.hasMoreElements()) {
       dods.dap.BaseType bt = (dods.dap.BaseType) variables.nextElement();
       DodsV dodsV = new DodsV( parent, bt);
       dodsVlist.add(dodsV);
       if (bt instanceof DConstructor) {
     DConstructor dcon = (DConstructor) bt;
     java.util.Enumeration enumerate2 = dcon.getVariables();
     parseVariables( dodsV, enumerate2, dodsV.children);

       } else if (bt instanceof DArray) {
     DArray da = (DArray) bt;

     BaseType elemType = da.getPrimitiveVector().getTemplate();
     dodsV.bt = elemType;
     dodsV.darray = da;

     if (elemType instanceof DConstructor) { // note that for DataDDS, cant traverse this further to find the data.
       DConstructor dcon = (DConstructor) elemType;
       java.util.Enumeration nestedVariables = dcon.getVariables();
       parseVariables( dodsV, nestedVariables, dodsV.children);
     }
       }
     }
   }

   // assign depth first sequence number
   private int nextInSequence = 0;
   private void assignSequence( ArrayList dodsVlist) {
     for (int i = 0; i < dodsVlist.size(); i++) {
       DodsV dodsV =  (DodsV) dodsVlist.get(i);
       assignSequence( dodsV.children);
       dodsV.seq = nextInSequence;
       nextInSequence++;
     }
   }

   // parse the DAS, assign attribute tables to the DodsV objects.
   // nested attribute tables actuallly follow the tree we construct with dodsV, so its
   // easy to assign to correct dodsV.
   private void parseDAS(DAS das, ArrayList dodsVlist) throws IOException {
     // recursively find the Attributes from the DAS
     Enumeration tableNames = das.getNames();
     parseAttributes( das, tableNames, dodsVlist);
   }

   private void parseAttributes( DAS das, Enumeration tableNames, ArrayList dodsVlist) {
     while (tableNames.hasMoreElements()) {
       String tableName = (String) tableNames.nextElement();
       AttributeTable attTable = das.getAttributeTable( tableName);

       // see if there's a matching variable
       String name = attTable.getName();
       DodsV dodsV = findDodsV( name, dodsVlist, false); // short name matches the table name
       if (dodsV != null) {
     dodsV.attTable = attTable;
     if (debugAttributes) System.out.println("DODSNetcdf getAttributes found <"+name+"> :");
     continue;
       }
       if (debugAttributes) System.out.println("DODSNetcdf getAttributes CANT find <"+name+"> :");
     }
   }

   // search the list for a BaseType with given name
   private DodsV findDodsV( String name, ArrayList dodsVlist, boolean useDone ) {
     for (int i = 0; i < dodsVlist.size(); i++) {
       DodsV dodsV =  (DodsV) dodsVlist.get(i);
       if (useDone && dodsV.isDone) continue;
       if (name.equals( dodsV.bt.getName()))
     return dodsV;
     }
     return null;
   }

   // find the DodsV object in the dataVlist corresponding to the ddsV
   private DodsV findDataV( DodsV ddsV, ArrayList dataVlist ) {
     if (ddsV.parent != null) {
       DodsV parentV = findDataV( ddsV.parent, dataVlist);
       if (parentV == null) // dataDDS may not have the structure wrapper
     return findDodsV( ddsV.bt.getName(), dataVlist, true);
       return findDodsV( ddsV.bt.getName(), parentV.children, true);
     }

     DodsV dataV =  findDodsV( ddsV.bt.getName(), dataVlist, true);
     /* if ((dataV == null) && (ddsV.bt instanceof DGrid)) { // when asking for the Grid array
       DodsV gridArray = (DodsV) ddsV.children.get(0);
       return findDodsV( gridArray.bt.getName(), dataVlist, true);
     } *
     return dataV;
   }

   /* a container for dods basetypes, so we can add some stuff as we process it
   class DodsV implements Comparable {
     //String name; // full name
     BaseType bt;
     DodsV parent;
     ArrayList children = new ArrayList();
     DArray darray; // if its an array
     AttributeTable attTable;
     Array data; // preload
     boolean isDone; // nc var has been made
     int seq; // "depth first" order

     DodsV( DodsV parent, BaseType bt) {
       this.parent = parent;
       this.bt = bt;
     }

     public int compareTo(Object o) {
        return seq - ((DodsV)o).seq;
     }
   } */

    private void parseGlobalAttributes(DAS das, DodsV root, DODSNetcdfFile dodsfile)
    {

        List<DODSAttribute> atts = root.attributes;
        for (ucar.nc2.Attribute ncatt : atts) {
            rootGroup.addAttribute(ncatt);
        }

        // loop over attribute tables, collect global attributes
        Enumeration tableNames = das.getNames();
        while (tableNames.hasMoreElements()) {
            String tableName = (String) tableNames.nextElement();
            AttributeTable attTable = das.getAttributeTableN(tableName);
            if(attTable == null)
                continue; // should probably never happen

            /* if (tableName.equals("NC_GLOBAL") || tableName.equals("HDF_GLOBAL")) {
        java.util.Enumeration attNames = attTable.getNames();
        while (attNames.hasMoreElements()) {
          String attName = (String) attNames.nextElement();
          dods.dap.Attribute att = attTable.getAttribute(attName);

          DODSAttribute ncatt = new DODSAttribute( attName, att);
          addAttribute( null, ncatt);
        }

          } else */

            if (tableName.equals("DODS_EXTRA")) {
                Enumeration attNames = attTable.getNames();
                while (attNames.hasMoreElements()) {
                    String attName = (String) attNames.nextElement();
                    if (attName.equals("Unlimited_Dimension")) {
                        opendap.dap.Attribute att = attTable.getAttribute(attName);
                        DODSAttribute ncatt = new DODSAttribute(attName, att);
                        setUnlimited(ncatt.getStringValue());
                    } else
                        logger.warn(" Unknown DODS_EXTRA attribute = " + attName + " " + location);
                }

            } else if (tableName.equals("EXTRA_DIMENSION")) {
                Enumeration attNames = attTable.getNames();
                while (attNames.hasMoreElements()) {
                    String attName = (String) attNames.nextElement();
                    opendap.dap.Attribute att = attTable.getAttribute(attName);
                    DODSAttribute ncatt = new DODSAttribute(attName, att);
                    int length = ncatt.getNumericValue().intValue();
                    Dimension extraDim = new Dimension(attName, length);
                    addDimension(null, extraDim);
                }

            } /* else if (null == root.findDodsV( tableName, false)) {
	addAttributes(attTable.getName(), attTable);
      } */
        }
    }

    /**
     * Go thru the variables/structure-variables and their attributes
     * and move to the proper groups.
     */
    protected void reGroup()
            throws DAP2Exception
    {
        assert (RC.getUseGroups());
        Group rootgroup = this.getRootGroup();

        // Start by moving global attributes
        // An issue to be addressed is that some attributes that should be attached
        // to variables, instead get made global with name var.att.
        Object[] gattlist = rootgroup.getAttributes().toArray();
        for (Object att : gattlist) {
            Attribute ncatt = (Attribute) att;
            String dodsname = ncatt.getDODSName();
            NamePieces pieces = parseName(dodsname);
            if (pieces.var != null) {
                // Figure out which variable to which this attribute should be moved.
                // In the event that there is no matching
                // variable, then keep the attribute as is.
                String searchname = pieces.var;
                if (pieces.prefix != null) searchname = pieces.prefix + '/' + searchname;
                Variable v = findVariable(searchname);
                if (v != null) {
                    // move attribute
                    rootgroup.remove(ncatt);
                    v.addAttribute(ncatt);
                    // change attribute name to remove var.
                    String newname = pieces.name;
                    ncatt.setName(newname);
                }
            } else if (pieces.prefix != null) {
             // We have a true group global name to move to proper group
                // convert prefix to an actual group
                Group g = rootgroup.makeRelativeGroup(this, dodsname, true);
                rootgroup.remove(ncatt);
                g.addAttribute(ncatt);
if(OLDGROUPCODE) {ncatt.setName(pieces.name);}
            }
        }

        Object[] varlist = rootgroup.getVariables().toArray();

if(false) {    // This should have been done by computegroup()
        // Now move variables
        for (Object var : varlist) {
            if (var instanceof DODSVariable) {
                DODSVariable v = (DODSVariable) var;
                reGroupVariable(rootgroup, v);
            } else
                throw new DAP2Exception("regroup: unexpected variable type: "
                        + var.getClass().getCanonicalName());
        }
}

        // In theory, we should be able to fix variable attributes
        // by just removing the group prefix. However, there is the issue
        // that attribute names sometimes have as a suffix varname.attname.
        // So, we should use that to adjust the attribute to attach to that
        // variable.
        for (Object var : varlist) {
            reGroupVariableAttributes(rootgroup, (Variable)var);
        }
    }

    @Deprecated
    protected void reGroupVariable(Group rootgroup, DODSVariable dodsv)
            throws opendap.dap.DAP2Exception
    {
        String dodsname = dodsv.getDODSName();
        NamePieces pieces = parseName(dodsname);
        if (pieces.prefix != null) {
            // convert prefix to an actual group
            Group gnew = rootgroup.makeRelativeGroup(this, dodsname, true);
            // Get current group for the variable
            Group gold = null;
            gold = dodsv.getParentGroup();
            if (gnew != gold) {
                gold.remove(dodsv);
                dodsv.setParentGroup(gnew);
                gnew.addVariable(dodsv);
if(OLDGROUPCODE) {
                dodsv.setName(pieces.name);
}
            }
        }
    }

    protected void reGroupVariableAttributes(Group rootgroup, Variable v)
            throws opendap.dap.DAP2Exception
    {
        String vname = v.getShortName();
        Group vgroup = v.getParentGroup();
        Object[] attlist = v.getAttributes().toArray();
        for (Object att : attlist) {
            Attribute ncatt = (Attribute) att;
            String adodsname = ncatt.getDODSName();
            NamePieces pieces = parseName(adodsname);
            Group agroup = null;
            if (pieces.prefix != null) {
                // convert prefix to an actual group
                agroup = rootgroup.makeRelativeGroup(this, adodsname, true);
            } else
                agroup = vgroup;

            // If the attribute group is different from the variable group,
            // then we have some kind of inconsistency; presumably from
            // the original dds+das; in any case, use the variable's group
            if (agroup != vgroup)
                agroup = vgroup;

            if (pieces.var != null && !pieces.var.equals(vname)) {
                // move the attribute to the correct variable
                // (presumably in the same group)
                Variable newvar = (Variable) agroup.findVariable(pieces.var);
                if (newvar != null) {// if not found leave the attribute as is
                    // otherwise, move the attribute and rename
                    newvar.addAttribute(ncatt);
                    v.remove(ncatt);
                    ncatt.setShortName(pieces.name);
                }
            }
if(OLDGROUPCODE) {
            if (pieces.prefix != null) {// rename the attribute
                // Rename the attribute to its shortname
                ncatt.setName(pieces.name);
            }
}
        }
    }

    // Utility to decompose a name
    NamePieces parseName(String name)
    {
        NamePieces pieces = new NamePieces();
        int dotpos = name.lastIndexOf('.');
        int slashpos = name.lastIndexOf('/');
        if (slashpos < 0 && dotpos < 0) {
            pieces.name = name;
        } else if (slashpos >= 0 && dotpos < 0) {
            pieces.prefix = name.substring(0, slashpos);
            pieces.name = name.substring(slashpos + 1, name.length());
        } else if (slashpos < 0 && dotpos >= 0) {
            pieces.var = name.substring(0, dotpos);
            pieces.name = name.substring(dotpos + 1, name.length());
        } else {//slashpos >= 0 && dotpos >= 0)
            if (slashpos > dotpos) {
                pieces.prefix = name.substring(0, slashpos);
                pieces.name = name.substring(slashpos + 1, name.length());
            } else {//slashpos < dotpos)
                pieces.prefix = name.substring(0, slashpos);
                pieces.var = name.substring(slashpos + 1, dotpos);
                pieces.name = name.substring(dotpos + 1, name.length());
            }
        }
        // fixup
        if (pieces.prefix != null && pieces.prefix.length() == 0) pieces.prefix = null;
        if (pieces.var != null && pieces.var.length() == 0) pieces.var = null;
        if (pieces.name.length() == 0) pieces.name = null;
        return pieces;
    }

    ///////////////////////////////////////////////////////////////////

    /* private void preload(ArrayList dodsVlist, CancelTask cancelTask) throws IOException {
     //Build up the request
     StringBuffer requestString = new StringBuffer ();
     requestString.append ( "?");
     ArrayList wantList = new ArrayList();
     preloadFindMaps( dodsVlist, requestString, wantList);

     if (wantList.size() == 0) return;

     try {
       DodsV flatDataList = DodsV.parseDDS( readDataDDSfromServer (requestString.toString()));
       if (cancelTask != null && cancelTask.isCancel()) return;

       for (int i = 0; i < wantList.size(); i++) {
     DodsV v = (DodsV) wantList.get(i);
     DodsV dataV = (DodsV) flatDataList.children.get(i);
     v.data = convertMapData( dataV.darray);
     if (cancelTask != null && cancelTask.isCancel()) return;
       }

      } catch (Exception exc) {
       System.err.println ("Error:" + exc);
       exc.printStackTrace ();
       throw new IOException( exc.getMessage());
     }
   }

   private void preloadFindMaps( ArrayList dodsVlist, StringBuffer result, ArrayList want) {
     for (int i = 0; i < dodsVlist.size(); i++) {
       DodsV dodsV =  (DodsV) dodsVlist.get(i);
       if (dodsV.bt instanceof DGrid) {
     List maps = dodsV.children;
     for (int j = 1; j < maps.size(); j++) {
       DodsV map = (DodsV) maps.get(j);
       if (want.size() > 0) result.append( ",");
       want.add( map);
       result.append( makeDODSname( map));
     }
       }
       // recurse
       if (dodsV.bt instanceof DStructure) {
     preloadFindMaps( dodsV.children, result, want);
       }
     }
   }

   private Array convertMapData( DArray da) {
       // gotta be a DVector with primitive type
     dods.dap.PrimitiveVector pv = da.getPrimitiveVector();
     BaseType elemType = pv.getTemplate();

     Object storage = pv.getInternalStorage();
     storage = widenArray( pv, storage); // data conversion if needed

      // create the array, using	 DODS internal array so there's no copying
     return Array.factory( convertToNCType( elemType).getPrimitiveClassType(), makeShape( da), storage);
   } */

    //////////////////////////////////////////////////////////////////////////////////////////////

    private void constructTopVariables(DodsV rootDodsV, CancelTask cancelTask) throws IOException
    {
        List<DodsV> topVariables = rootDodsV.children;
        for (DodsV dodsV : topVariables) {
            if (dodsV.bt instanceof DConstructor) continue;
            addVariable(rootGroup, null, dodsV);
            if (cancelTask != null && cancelTask.isCancel()) return;
        }
    }

    private void constructConstructors(DodsV rootDodsV, CancelTask cancelTask) throws IOException
    {
        List<DodsV> topVariables = rootDodsV.children;
        // do non-grids first
        for (DodsV dodsV : topVariables) {
            if (dodsV.isDone) continue;
            if (dodsV.bt instanceof DGrid) continue;
            addVariable(rootGroup, null, dodsV);
            if (cancelTask != null && cancelTask.isCancel()) return;
        }

        // then do the grids
        for (DodsV dodsV : topVariables) {
            if (dodsV.isDone) continue;
            // If using groups, then if the grid does not have a group name
            // and its array does, then transfer the group name.
            if (RC.getUseGroups() && dodsV.bt instanceof DGrid) {
                DodsV array = dodsV.findByIndex(0);
                if (array != null) {
                    String arrayname = array.getClearName();
                    String gridname = dodsV.getClearName();
                    int ai = arrayname.lastIndexOf('/');
                    int gi = gridname.lastIndexOf('/');
                    if (gi >= 0 && ai < 0) {
                        String gpath = gridname.substring(0, gi);
                        arrayname = gpath + "/" + arrayname;
                        array.getBaseType().setClearName(arrayname);
                    } else if (gi < 0 && ai >= 0) {
                        String apath = arrayname.substring(0, ai);
                        gridname = apath + "/" + gridname;
                        dodsV.getBaseType().setClearName(gridname);
                    } else if (gi >= 0 && ai >= 0) {
                        String apath = arrayname.substring(0, ai);
                        String gpath = gridname.substring(0, gi);
                        if (!gpath.equals(apath)) {// choose gridpath over the array path
                            String arraysuffix = arrayname.substring(gi + 1, arrayname.length());
                            arrayname = gpath + "/" + arraysuffix;
                            array.getBaseType().setClearName(arrayname);
                        }
                    }     // else gi < 0 && ai < 0
                }
            }
            addVariable(rootGroup, null, dodsV);
            if (cancelTask != null && cancelTask.isCancel()) return;
        }
    }

    // recursively make new variables: all new variables come through here

    Variable addVariable(Group parentGroup, Structure parentStructure, DodsV dodsV) throws IOException
    {
        Variable v = makeVariable(parentGroup, parentStructure, dodsV);
        if (v != null) {
            addAttributes(v, dodsV);
            if (parentStructure != null)
                parentStructure.addMemberVariable(v);
            else {
                parentGroup = computeGroup(v.getDODSName(), v, parentGroup);
                parentGroup.addVariable(v);
            }
            dodsV.isDone = true;
        }
        return v;
    }

    Group computeGroup(String path, Variable v, Group parentGroup/*Ostensibly*/)
    {
        if (parentGroup == null)
            parentGroup = getRootGroup();
        if(RC.getUseGroups()) {
            // If the path has '/' in it, then we need to insert
            // this variable into the proper group and rename it. However,
            // if this variable is within a structure, we cannot do it.
            if (v.getParentStructure() == null) {
                // HACK: Since only the grid array is used in converting
                // to netcdf-3, we look for group info on the array.
                String dodsname = v.getDODSName();
                int sindex = dodsname.indexOf('/');
                if (sindex >= 0) {
                    assert (parentGroup != null);
                    Group g = parentGroup.makeRelativeGroup(this, dodsname, true/*ignorelast*/);
                    parentGroup = g;
if(OLDGROUPCODE) {
                    // change variable's name
                    dodsname = dodsname.substring(dodsname.lastIndexOf('/') + 1);
                    v.setName(dodsname);   // change name
                }
                }
            }
        }
        return parentGroup;
    }

    private Variable makeVariable(Group parentGroup, Structure parentStructure, DodsV dodsV) throws IOException
    {
        opendap.dap.BaseType dodsBT = dodsV.bt;
        String dodsShortName = dodsBT.getClearName();
        if (debugConstruct) System.out.print("DODSNetcdf makeVariable try to init <" + dodsShortName + "> :");

        // Strings
        if (dodsBT instanceof DString) {
            if (dodsV.darray == null) {
                if (debugConstruct) System.out.println("  assigned to DString: name = " + dodsShortName);
                return new DODSVariable(this, parentGroup, parentStructure, dodsShortName, dodsBT, dodsV);
            } else {
                if (debugConstruct) System.out.println("  assigned to Array of Strings: name = " + dodsShortName);
                return new DODSVariable(this, parentGroup, parentStructure, dodsShortName, dodsV.darray, dodsBT, dodsV);
            }

            // primitives
        } else if ((dodsBT instanceof DByte) ||
                (dodsBT instanceof DFloat32) || (dodsBT instanceof DFloat64) ||
                (dodsBT instanceof DInt16) || (dodsBT instanceof DInt32) ||
                (dodsBT instanceof DUInt16) || (dodsBT instanceof DUInt32)) {
            if (dodsV.darray == null) {
                if (debugConstruct)
                    System.out.println("	assigned to scalar " + dodsBT.getTypeName() + ": name = " + dodsShortName);
                return new DODSVariable(this, parentGroup, parentStructure, dodsShortName, dodsBT, dodsV);
            } else {
                if (debugConstruct)
                    System.out.println("	assigned to array of type " + dodsBT.getClass().getName() + ": name = " + dodsShortName);
                return new DODSVariable(this, parentGroup, parentStructure, dodsShortName, dodsV.darray, dodsBT, dodsV);
            }
        }

        if (dodsBT instanceof DGrid) {
            if (dodsV.darray == null) {
                if (debugConstruct) System.out.println(" assigned to DGrid <" + dodsShortName + ">");

                //  common case is that the map vectors already exist as top level variables
                // this is how the netccdf servers do it
                for (int i = 1; i < dodsV.children.size(); i++) {
                    DodsV map = dodsV.children.get(i);
                    String shortName = DODSNetcdfFile.makeShortName(map.bt.getEncodedName());
                    Variable mapV = parentGroup.findVariable(shortName); // LOOK WRONG
                    if (mapV == null) {         // if not, add it LOOK need to compare values
                        mapV = addVariable(parentGroup, parentStructure, map);
                        makeCoordinateVariable(parentGroup, mapV, map.data);

                    }
                    // else if (!mapV.isCoordinateVariable()) { // workaround for Grid HDF4 wierdness (see note 1 below)
                    //    makeCoordinateVariable(parentGroup, mapV, map.data);
                    // }
                }

                return new DODSGrid(this, parentGroup, parentStructure, dodsShortName, dodsV);

            } else {
                if (debugConstruct) System.out.println(" ERROR! array of DGrid <" + dodsShortName + ">");
                return null;
            }

        } else if (dodsBT instanceof DSequence) {
            if (dodsV.darray == null) {
                if (debugConstruct) System.out.println(" assigned to DSequence <" + dodsShortName + ">");
                return new DODSStructure(this, parentGroup, parentStructure, dodsShortName, dodsV);
            } else {
                if (debugConstruct) System.out.println(" ERROR! array of DSequence <" + dodsShortName + ">");
                return null;
            }

        } else if (dodsBT instanceof DStructure) {
            DStructure dstruct = (DStructure) dodsBT;
            if (dodsV.darray == null) {
                if (useGroups && (parentStructure == null) && isGroup(dstruct)) { // turn into a group
                    if (debugConstruct) System.out.println(" assigned to Group <" + dodsShortName + ">");
                    Group g = new Group(this, parentGroup, DODSNetcdfFile.makeShortName(dodsShortName));
                    addAttributes(g, dodsV);
                    parentGroup.addGroup(g);

                    for (DodsV nested : dodsV.children) {
                        addVariable(g, null, nested);
                    }
                    return null;
                } else {
                    if (debugConstruct) System.out.println(" assigned to DStructure <" + dodsShortName + ">");
                    return new DODSStructure(this, parentGroup, parentStructure, dodsShortName, dodsV);
                }
            } else {
                if (debugConstruct) System.out.println(" assigned to Array of DStructure <" + dodsShortName + "> ");
                return new DODSStructure(this, parentGroup, parentStructure, dodsShortName, dodsV.darray, dodsV);
            }

        } else {
            logger.warn("DODSNetcdf " + location + " didnt process basetype <" + dodsBT.getTypeName() + "> variable = " + dodsShortName);
            return null;
        }

    }

    private void makeCoordinateVariable(Group parentGroup, Variable v, Array data)
    {
        String name = v.getShortName();

        // replace in Variable
        Dimension oldDimension = v.getDimension(0);
        Dimension newDimension = new Dimension(name, oldDimension.getLength());
        // newDimension.setCoordinateAxis( v); calcCoordinateVaribale will do this
        v.setDimension(0, newDimension);

        // replace old (if it exists) in Group with shared dimension
        Dimension old = parentGroup.findDimension(name);
        parentGroup.remove(old);
        parentGroup.addDimension(newDimension);

        // might as well cache the data
        if (data != null) {
            v.setCachedData(data);
            if (debugCached) System.out.println(" cache for <" + name + "> length =" + data.getSize());
        }
    }

    // make a structure into a group if its scalar and all parents are groups

    private boolean isGroup(DStructure dstruct)
    {
        BaseType parent = (BaseType) dstruct.getParent();
        if (parent == null) return true;
        if (parent instanceof DStructure)
            return isGroup((DStructure) parent);
        return true;
    }

    /* private void addAttributes(String tableName, AttributeTable attTable) {

     java.util.Enumeration attNames = attTable.getNames();
     while (attNames.hasMoreElements()) {
       String attName = (String) attNames.nextElement();
       dods.dap.Attribute att = attTable.getAttribute(attName);
       if (!att.isContainer()) {
     DODSAttribute ncatt = new DODSAttribute( tableName +"." + attName, att);
     addAttribute( null, ncatt);
     if (debugAttributes) System.out.println(" addAttribute = "+tableName +"." + attName);
       }
     }

     try {
       // look for nested ones
       attNames = attTable.getNames();
       while (attNames.hasMoreElements()) {
     String attName = (String) attNames.nextElement();
     dods.dap.Attribute att = attTable.getAttribute(attName);
     if (att.isContainer()) {
       addAttributes(tableName +"."+attName, att.getContainer());
     }
       }
     } catch (Exception e) {} // WRONG
     // } catch (NoSuchAttributeException e) {}
   }

   // an attTable name matches a Variable
   private void addAttributes(Variable v, AttributeTable attTable) {
     if (attTable == null) return;
     if (debugAttributes) System.out.println(" addAttributes to "+v.getName());

     java.util.Enumeration attNames = attTable.getNames();
     while (attNames.hasMoreElements()) {
       String attName = (String) attNames.nextElement();
       dods.dap.Attribute att = attTable.getAttribute(attName);
       if (att == null) continue;
       if (!att.isContainer()) {
     DODSAttribute ncatt = new DODSAttribute( attName, att);
     v.addAttribute( ncatt);
       } else if (v instanceof Structure) {
     Structure s = (Structure) v;
     Variable member = s.findVariable( att.getShortName());
     if (member != null) {
       addAttributes(member, att.getContainer());
     } else {
       if (debugAttributes) System.out.println("Cant find nested Variable "+ att.getShortName()+" in "+v.getName());
     }
       } else {
     if (debugAttributes) System.out.println("Container attribute "+ att.getShortName()+" in non structure variables"+v.getName());
       }
     }
   } */

    private void addAttributes(Variable v, DodsV dodsV)
    {
        List<DODSAttribute> atts = dodsV.attributes;
        for (Attribute ncatt : atts) {
            v.addAttribute(ncatt);
        }

        // this is the case where its (probably) a Grid, and so _Coordinate.Axes has been assigned, but if
        // theres also a coordinates attribute, need to add that info
        Attribute axes = v.findAttribute(CF.COORDINATES);
        Attribute _axes = v.findAttribute(_Coordinate.Axes);
        if ((null != axes) && (null != _axes)) {
            v.addAttribute(combineAxesAttrs(axes, _axes));
        }
    }

    /**
     *
     * Safely combine the multiple axis attributes without duplication
     *
     * @param axis1 axis attribute 1
     * @param axis2 axis attribute 2
     * @return the combined axis attribute
     */
    protected static Attribute combineAxesAttrs(Attribute axis1, Attribute axis2) {

        List axesCombinedValues = new ArrayList<String>();
        // each axis attribute is a whitespace delimited string, so just join the strings to make
        // an uber string of all values
        String axisValuesStr = axis1.getStringValue() + " " + axis2.getStringValue();
        // axis attributes are whitespace delimited, so split on whitespace to get each axis name
        String[] axisValues = axisValuesStr.split("\\s");
        for (String ax : axisValues) {
            // only add if axis name is unique - no dupes
            if (!axesCombinedValues.contains(ax) && !ax.equals("")) {
                axesCombinedValues.add(ax);
            }
        }

        // going to rejoin this list of strings to be one whitespace delimited string
        Joiner joiner = Joiner.on(" ");

        return new Attribute(_Coordinate.Axes, joiner.join(axesCombinedValues));
    }

    private void addAttributes(Group g, DodsV dodsV)
    {
        List<DODSAttribute> atts = dodsV.attributes;
        for (Attribute ncatt : atts) {
            g.addAttribute(ncatt);
        }
    }

    /* an attTable name matches a Group
  private void addAttributes(Group g, AttributeTable attTable) {
    if (attTable == null) return;
    if (debugAttributes) System.out.println(" addAttributes to Group "+g.getName());

    java.util.Enumeration attNames = attTable.getNames();
    while (attNames.hasMoreElements()) {
      String attName = (String) attNames.nextElement();
      dods.dap.Attribute att = attTable.getAttribute(attName);
      if (att == null) continue;
      if (!att.isContainer()) {
    DODSAttribute ncatt = new DODSAttribute( attName, att);
    g.addAttribute( ncatt);
      } /* else {
    Variable member = g.findVariable( attTable.getName());
    if (member != null) {
      addAttributes(member, att.getContainer());
    } else {
      Group nested = g.findGroup( attTable.getName());
      if (nested != null) {
        addAttributes(nested, att.getContainer());
      } else {
        System.out.println("Cant find nested place for nested attribute "+ attTable.getName()+" in group "+g.getName());
      }
    }
      } //
    }
  }  */


    /**
     * Checks to see if this is netcdf char array.
     *
     * @param v must be type STRING
     * @return string length dimension, else null
     */
    Dimension getNetcdfStrlenDim(DODSVariable v)
    {
        AttributeTable table = das.getAttributeTableN(v.getFullName()); // LOOK this probably doesnt work for nested variables
        if (table == null) return null;

        opendap.dap.Attribute dodsAtt = table.getAttribute("DODS");
        if (dodsAtt == null) return null;

        AttributeTable dodsTable = dodsAtt.getContainerN();
        if (dodsTable == null) return null;

        opendap.dap.Attribute att = dodsTable.getAttribute("strlen");
        if (att == null) return null;
        String strlen = att.getValueAtN(0);

        opendap.dap.Attribute att2 = dodsTable.getAttribute("dimName");
        String dimName = (att2 == null) ? null : att2.getValueAtN(0);
        if (debugCharArray) System.out.println(v.getFullName() + " has strlen= " + strlen + " dimName= " + dimName);

        int dimLength;
        try {
            dimLength = Integer.parseInt(strlen);
        } catch (NumberFormatException e) {
            logger.warn("DODSNetcdfFile " + location + " var = " + v.getFullName() + " error on strlen attribute = " + strlen);
            return null;
        }

        if (dimLength <= 0) return null; // LOOK what about unlimited ??
        return new Dimension(dimName, dimLength, dimName != null);
    }

    /**
     * If an equivilent shared dimension already exists, use it, else add d to shared dimensions.
     * Equivilent is same name and length.
     *
     * @param group from this group, if null, use rootGroup
     * @param d     find equivilent shared dimension to this one.
     * @return equivilent shared dimension or d.
     */
    Dimension getSharedDimension(Group group, Dimension d)
    {
        if (d.getShortName() == null) return d;

        if (group == null) group = rootGroup;
        for (Dimension sd : group.getDimensions()) {
            if (sd.getShortName().equals(d.getShortName()) && sd.getLength() == d.getLength())
                return sd;
        }
        d.setShared(true);
        group.addDimension(d);
        return d;
    }

    // construct list of dimensions to use

    List<Dimension> constructDimensions(Group group, opendap.dap.DArray dodsArray)
    {
        if (group == null) group = rootGroup;

        List<Dimension> dims = new ArrayList<Dimension>();
        Enumeration enumerate = dodsArray.getDimensions();
        while (enumerate.hasMoreElements()) {
            opendap.dap.DArrayDimension dad = (opendap.dap.DArrayDimension) enumerate.nextElement();
            String name = dad.getEncodedName();
            if (name != null)
                name = StringUtil2.unescape(name);

            Dimension myd;

            if (name == null) { // if no name, make an anonymous dimension
                myd = new Dimension(null, dad.getSize(), false);

            } else { // see if shared
                if (RC.getUseGroups()) {
                    if (name.indexOf('/') >= 0) {// place dimension in proper group
                        group = group.makeRelativeGroup(this, name, true);
                        // change our name
                        name = name.substring(name.lastIndexOf('/') + 1);
                    }
                }
                myd = group.findDimension(name);
                if (myd == null) { // add as shared
                    myd = new Dimension(name, dad.getSize());
                    group.addDimension(myd);
                } else if (myd.getLength() != dad.getSize()) { // make a non-shared dimension
                    myd = new Dimension(name, dad.getSize(), false);
                } // else use existing, shared dimension
            }
            dims.add(myd); // add it to the list
        }

        return dims;
    }

    private void setUnlimited(String dimName)
    {
        Dimension dim = rootGroup.findDimension(dimName);
        if (dim != null)
            dim.setUnlimited(true);
        else
            logger.error(" DODS Unlimited_Dimension = " + dimName + " not found on " + location);
    }

    protected int[] makeShape(opendap.dap.DArray dodsArray)
    {
        int count = 0;
        Enumeration enumerate = dodsArray.getDimensions();
        while (enumerate.hasMoreElements()) {
            count++;
            enumerate.nextElement();
        }

        int[] shape = new int[count];
        enumerate = dodsArray.getDimensions();
        count = 0;
        while (enumerate.hasMoreElements()) {
            opendap.dap.DArrayDimension dad = (opendap.dap.DArrayDimension) enumerate.nextElement();
            shape[count++] = dad.getSize();
        }

        return shape;
    }

    // kludge for single inheritence

    /**
     * Return a variable name suitable for use in a DAP constraint expression.
     * [Original code seemed wrong because structures can be nested and hence
     *  would have to use the full name just like non-structures]
     *
     * @param var The variable whose name will appear in the CE
     * @return    The name in a form suitable for use in a cE
     */
    static public String getDODSConstraintName(Variable var)
    {
        String vname = var.getDODSName();
	// The vname is backslash escaped, so we need to
	// modify to use DAP %xx escapes.
        return EscapeStrings.backslashToDAP(vname);

        /*
        if (var instanceof DODSVariable)
            return ((DODSVariable) var).getDODSName();
        else if (var instanceof DODSStructure)
            return ((DODSStructure) var).getDODSConstraintName();
        else
            return null;
        */
    }

    /* full name
   private String makeDODSname(Variable dodsV) {
     if (dodsV.isMemberOfStructure())
       return makeDODSname(dodsV.getParentStructure()) + "." + getDODSConstraintName(dodsV);

     Group parent = dodsV.getParentGroup();
     if (parent != rootGroup)
       return parent.getShortName() + "." + getDODSConstraintName(dodsV);
     else
       return getDODSConstraintName(dodsV);
   } */

    // full name

    private String makeDODSname(DodsV dodsV)
    {
        DodsV parent = dodsV.parent;
        if (parent.bt != null)
            return (makeDODSname(parent) + "." + dodsV.bt.getEncodedName());
        return dodsV.bt.getEncodedName();
    }

    static String
    makeShortName(String name)
    {
        String unescaped = EscapeStrings.unescapeDAPIdentifier(name);
        int index = unescaped.lastIndexOf('/');
        if(index < 0) index = -1;
        return unescaped.substring(index+1, unescaped.length());
    }

    static String
    makeDODSName(String name)
    {
      return EscapeStrings.unescapeDAPIdentifier(name);
    }

    /**
     * Get the DODS data class corresponding to the Netcdf data type.
     * This is the inverse of convertToNCType().
     *
     * @param dataType   Netcdf data type.
     * @param isUnsigned if its unsigned
     * @return the corresponding DODS type enum, from opendap.dap.Attribute.XXXX.
     */
    static public int convertToDODSType(DataType dataType, boolean isUnsigned)
    {

        if (dataType == DataType.STRING)
            return opendap.dap.Attribute.STRING;
        if (dataType == DataType.BYTE)
            return opendap.dap.Attribute.BYTE;
        if (dataType == DataType.FLOAT)
            return opendap.dap.Attribute.FLOAT32;
        if (dataType == DataType.DOUBLE)
            return opendap.dap.Attribute.FLOAT64;
        if (dataType == DataType.SHORT)
            return isUnsigned ? opendap.dap.Attribute.UINT16 : opendap.dap.Attribute.INT16;
        if (dataType == DataType.INT)
            return isUnsigned ? opendap.dap.Attribute.UINT32 : opendap.dap.Attribute.INT32;
        if (dataType == DataType.BOOLEAN)
            return opendap.dap.Attribute.BYTE;
        if (dataType == DataType.LONG)
            return opendap.dap.Attribute.INT32; // LOOK no LONG type!

        // shouldnt happen
        return opendap.dap.Attribute.STRING;
    }

    /**
     * Get the Netcdf data type corresponding to the DODS data type.
     * This is the inverse of convertToDODSType().
     *
     * @param dodsDataType DODS type enum, from dods.dap.Attribute.XXXX.
     * @return the corresponding netcdf DataType.
     * @see #isUnsigned
     */
    static public DataType convertToNCType(int dodsDataType)
    {
        switch (dodsDataType) {
        case opendap.dap.Attribute.BYTE:
            return DataType.BYTE;
        case opendap.dap.Attribute.FLOAT32:
            return DataType.FLOAT;
        case opendap.dap.Attribute.FLOAT64:
            return DataType.DOUBLE;
        case opendap.dap.Attribute.INT16:
            return DataType.SHORT;
        case opendap.dap.Attribute.UINT16:
            return DataType.SHORT;
        case opendap.dap.Attribute.INT32:
            return DataType.INT;
        case opendap.dap.Attribute.UINT32:
            return DataType.INT;
        default:
            return DataType.STRING;
        }
    }

    /**
     * Get whether this is an unsigned type.
     *
     * @param dodsDataType DODS type enum, from dods.dap.Attribute.XXXX.
     * @return true if unsigned
     */
    static public boolean isUnsigned(int dodsDataType)
    {
        return (dodsDataType == opendap.dap.Attribute.BYTE) ||
                (dodsDataType == opendap.dap.Attribute.UINT16) ||
                (dodsDataType == opendap.dap.Attribute.UINT32);
    }

    /**
     * Get the Netcdf data type corresponding to the DODS BaseType class.
     * This is the inverse of convertToDODSType().
     *
     * @param dtype DODS BaseType.
     * @return the corresponding netcdf DataType.
     * @see #isUnsigned
     */
    static public DataType convertToNCType(opendap.dap.BaseType dtype)
    {

        if (dtype instanceof DString)
            return DataType.STRING;
        else if ((dtype instanceof DStructure) || (dtype instanceof DSequence) || (dtype instanceof DGrid))
            return DataType.STRUCTURE;
        else if (dtype instanceof DFloat32)
            return DataType.FLOAT;
        else if (dtype instanceof DFloat64)
            return DataType.DOUBLE;
        else if (dtype instanceof DUInt32)
            return DataType.INT;
        else if (dtype instanceof DUInt16)
            return DataType.SHORT;
        else if (dtype instanceof DInt32)
            return DataType.INT;
        else if (dtype instanceof DInt16)
            return DataType.SHORT;
        else if (dtype instanceof DByte)
            return DataType.BYTE;
        else
            throw new IllegalArgumentException("DODSVariable illegal type = " + dtype.getTypeName());
    }

    /**
     * Get whether this is an unsigned type.
     *
     * @param dtype DODS BaseType.
     * @return true if unsigned
     */
    static public boolean isUnsigned(opendap.dap.BaseType dtype)
    {
        return (dtype instanceof DByte) ||
                (dtype instanceof DUInt16) ||
                (dtype instanceof DUInt32);
    }

    /////////////////////////////////////////////////////////////////////////////////////

    /**
     * This does the actual connection to the opendap server and reading of the data.
     * All data calls go through here so we can add debugging.
     *
     * @param CE constraint expression; use empty string if none
     * @return DataDDS
     * @throws java.io.IOException       on io error
     * @throws opendap.dap.parsers.ParseException
     *                                   if error parsing return
     * @throws opendap.dap.DAP2Exception if you have otherwise been bad
     */
    DataDDS readDataDDSfromServer(String CE) throws IOException, opendap.dap.parsers.ParseException, opendap.dap.DAP2Exception
    {
        if (debugServerCall) System.out.println("DODSNetcdfFile.readDataDDSfromServer = <" + CE + ">");

        long start = 0;
        if (debugTime) start = System.currentTimeMillis();

        if (!CE.startsWith("?"))
            CE = "?" + CE;
        DataDDS data;
        synchronized(this) {
            data = dodsConnection.getData(CE, null);
        }
        if (debugTime)
            System.out.println("DODSNetcdfFile.readDataDDSfromServer took = " + (System.currentTimeMillis() - start) / 1000.0);

        if (debugDataResult) {
            System.out.println(" dataDDS return:");
            data.print(System.out);
        }

        return data;
    }

    ///////////////////////////////////////////////////////////////////
    // ALL the I/O goes through these  routines
    // called from ucar.nc2.Variable

    /**
     * Make a single call to the DODS Server to read all the named variable's data
     * in one client/server roundtrip.
     *
     * @param preloadVariables list of type Variable
     * @return list of type Array, contains the data
     * @throws IOException on error
     */
    @Override
    public List<Array> readArrays(List<Variable> preloadVariables) throws IOException
    {
        //For performance tests:
        //if (true) return super.readArrays (variables);
        if (preloadVariables.size() == 0) return new ArrayList<Array>();

        // construct the list of variables, skipping ones with cached data
        List<DodsV> reqDodsVlist = new ArrayList<DodsV>();
        DodsV root;
        for (Variable var : preloadVariables) {
            if (var.hasCachedData()) continue;
            reqDodsVlist.add((DodsV) var.getSPobject());
        }
        Collections.sort(reqDodsVlist); // "depth first" order

        // read the data
        DataDDS dataDDS;
        Map<DodsV, DodsV> map = new HashMap<DodsV, DodsV>(2 * reqDodsVlist.size() + 1);
        if (reqDodsVlist.size() > 0) {

            // Create the request
            StringBuilder requestString = new StringBuilder();
            for (int i = 0; i < reqDodsVlist.size(); i++) {
                DodsV dodsV = reqDodsVlist.get(i);
                requestString.append(i == 0 ? "?" : ",");
                // requestString.append(makeDODSname(dodsV));
                requestString.append(dodsV.getEncodedName());
            }
            String s = requestString.toString();

            try {
                dataDDS = readDataDDSfromServer(requestString.toString());
                root = DodsV.parseDataDDS(dataDDS);

            } catch (Exception exc) {
                logger.error("ERROR readDataDDSfromServer on " + requestString, exc);
                throw new IOException(exc.getMessage());
            }

            // gotta find the corresponding data in "depth first" order
            for (DodsV ddsV : reqDodsVlist) {
                DodsV dataV = root.findDataV(ddsV);
                if (dataV != null) {
                    if (debugConvertData) System.out.println("readArray found dataV= " + makeDODSname(ddsV));
                    dataV.isDone = true;
                    map.put(ddsV, dataV); // thread safe!
                } else {
                    logger.error("ERROR findDataV cant find " + makeDODSname(ddsV) + " on " + location);
                }
            }
        }

        // For each variable either extract the data or use cached data.
        List<Array> result = new ArrayList<Array>();
        for (Variable var : preloadVariables) {
            if (var.hasCachedData()) {
                result.add(var.read());

            } else {
                Array data = null;
                DodsV ddsV = (DodsV) var.getSPobject();

                DodsV dataV = map.get(ddsV);
                if (dataV == null) {
                    logger.error("DODSNetcdfFile.readArrays cant find " + makeDODSname(ddsV) + " in dataDDS; " + location);
                    //dataDDS.print( System.out);
                } else {
                    if (debugConvertData) System.out.println("readArray converting " + makeDODSname(ddsV));
                    dataV.isDone = true;

                    try {
                        if (var.isMemberOfStructure()) {

                            // we want the top structure this variable is contained in.
                            while ((dataV.parent != null) && (dataV.parent.bt != null)) {
                                dataV = dataV.parent;
                            }
                            data = convertD2N.convertNestedVariable(var, null, dataV, true);

                        } else
                            data = convertD2N.convertTopVariable(var, null, dataV);

                    } catch (DAP2Exception de) {
                        logger.error("ERROR convertVariable on " + var.getFullName(), de);
                        throw new IOException(de.getMessage());
                    }

                    if (var.isCaching()) {
                        var.setCachedData(data);
                        if (debugCached)
                            System.out.println(" cache for <" + var.getFullName() + "> length =" + data.getSize());
                    }
                }
                result.add(data);
            }
        }
        return result;
    }

    @Override
    public Array readSection(String variableSection) throws IOException, InvalidRangeException
    {
        ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(this, variableSection);

        //if (unlocked)
        //    throw new IllegalStateException("File is unlocked - cannot use");

        /* run it through the variableso to pick up caching
       if (cer.child == null) {
         Array result = cer.v.read(cer.section);
         result.setUnsigned(cer.v.isUnsigned());
         return result;
       } */

        return readData(cer.v, cer.section);
    }

    @Override
    protected Array readData(ucar.nc2.Variable v, Section section) throws IOException, InvalidRangeException
    {
        //if (unlocked)
        //    throw new IllegalStateException("File is unlocked - cannot use");

        // LOOK: what if theres already a CE !!!!
        // create the constraint expression
        StringBuilder buff = new StringBuilder(100);
        buff.setLength(0);
        buff.append(getDODSConstraintName(v));

        // add the selector if not a Sequence
        if (!v.isVariableLength()) {
            List<Range> dodsSection = section.getRanges();
            if ((v.getDataType() == DataType.CHAR)) { // CHAR is mapped to DString
                int n = section.getRank();
                if (n == v.getRank())  // remove last section if present
                    dodsSection = dodsSection.subList(0, n - 1);
            }
            makeSelector(buff, dodsSection);
        }

        Array dataArray;
        try {
            // DodsV root = DodsV.parseDDS( readDataDDSfromServer(buff.toString()));
            // data = convertD2N( (DodsV) root.children.get(0), v, section, false); // can only be one

            DataDDS dataDDS = readDataDDSfromServer(buff.toString());
            DodsV root = DodsV.parseDataDDS(dataDDS);
            DodsV want = root.children.get(0); // can only be one
            dataArray = convertD2N.convertTopVariable(v, section.getRanges(), want);
        } catch (DAP2Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage()+"; "+v.getShortName()+" -- "+section);
        } catch (ParseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }

        return dataArray;
    }

    @Override
    public long readToByteChannel(ucar.nc2.Variable v, Section section, WritableByteChannel channel)
            throws java.io.IOException, ucar.ma2.InvalidRangeException
    {
        //if (unlocked)
        //    throw new IllegalStateException("File is unlocked - cannot use");

        Array result = readData(v, section);
        return IospHelper.transferData(result, channel);
    }

    /* this is for reading variables that are members of structures
  protected Array readMemberData(ucar.nc2.Variable v, Section section, boolean flatten) throws IOException, InvalidRangeException {
    StringBuffer buff = new StringBuffer(100);
    buff.setLength(0);

    List<Range> ranges = section.getRanges();

    // add the selector
    addParents(buff, v, ranges, 0);

    Array dataArray;
    try {
      //DodsV root = DodsV.parseDDS( readDataDDSfromServer(buff.toString()));
      // data = convertD2N((DodsV) root.children.get(0), v, section, flatten); // can only be one

      DataDDS dataDDS = readDataDDSfromServer(buff.toString());
      DodsV root = DodsV.parseDataDDS(dataDDS);
      DodsV want = root.children.get(0); // can only be one
      dataArray = convertD2N.convertNestedVariable(v, ranges, want, flatten);

    }
    catch (DAP2Exception ex) {
      ex.printStackTrace();
      throw new IOException(ex.getMessage());
    }
    catch (ParseException ex) {
      ex.printStackTrace();
      throw new IOException(ex.getMessage());
    }

    return dataArray;
  }  */

    public Array readWithCE(ucar.nc2.Variable v, String CE) throws IOException
    {

        Array dataArray;
        try {

            DataDDS dataDDS = readDataDDSfromServer(CE);
            DodsV root = DodsV.parseDataDDS(dataDDS);
            DodsV want = root.children.get(0); // can only be one

            if (v.isMemberOfStructure())
                dataArray = convertD2N.convertNestedVariable(v, null, want, true);
            else
                dataArray = convertD2N.convertTopVariable(v, null, want);
        } catch (DAP2Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        } catch (ParseException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }

        return dataArray;
    }

    private int addParents(StringBuilder buff, Variable s, List<Range> section, int start)
    {
        Structure parent = s.getParentStructure();
        if (parent != null) {
            start = addParents(buff, parent, section, start);
            buff.append(".");
        }

        List<Range> subSection = section.subList(start, start + s.getRank());

        buff.append(getDODSConstraintName(s));

        if (!s.isVariableLength()) // have to get the whole thing for a sequence !!
            makeSelector(buff, subSection);

        return start + s.getRank();
    }

    private void makeSelector(StringBuilder buff, List<Range> section)
    {
        for (Range r : section) {
            buff.append("[");
            buff.append(r.first());
            buff.append(':');
            buff.append(r.stride());
            buff.append(':');
            buff.append(r.last());
            buff.append("]");
        }
    }

    // old way
    /*   public List readArrays (List preloadVariables) throws IOException {
     //For performance tests:
     //if (true) return super.readArrays (variables);
     if (preloadVariables.size() == 0) return new ArrayList();

     // construct the list of variables, skipping ones with cached data
     ArrayList reqDodsVlist = new ArrayList();
     DodsV root = null;
     for (int i=0; i<preloadVariables.size(); i++) {
       Variable var = (Variable) preloadVariables.get (i);
       if (var.hasCachedData()) continue;
       reqDodsVlist.add( var.getSPobject());
     }
     Collections.sort(reqDodsVlist); // "depth first" order

     // read the data
     DataDDS dataDDS = null;
     HashMap map = new HashMap( 2 * reqDodsVlist.size()+1);
     if (reqDodsVlist.size() > 0) {

       // Create the request
       StringBuffer requestString = new StringBuffer ();
       for (int i=0; i<reqDodsVlist.size(); i++) {
     DodsV dodsV = (DodsV) reqDodsVlist.get (i);
     requestString.append (i == 0 ? "?" : ",");
     requestString.append (makeDODSname(dodsV));
       }

       try {
     dataDDS = readDataDDSfromServer (requestString.toString());
     root = DodsV.parseDDS( dataDDS);

       } catch (Exception exc) {
     System.err.println ("Error:" + exc);
     exc.printStackTrace ();
     throw new IOException( exc.getMessage());
       }

       // gotta find the corresponding data in "depth first" order
       for (int i=0;i<reqDodsVlist.size (); i++) {
     DodsV ddsV = (DodsV) reqDodsVlist.get(i);

     if (ddsV.bt instanceof DGrid) // really want its child array
       ddsV = (DodsV) ddsV.children.get(0);

     DodsV dataV = root.findDataV( ddsV);
     if (dataV != null) {
       if (debugConvertData) System.out.println("readArray found dataV= "+makeDODSname(ddsV));
       dataV.isDone = true;
       map.put( ddsV, dataV); // thread safe!
     } else {
       logger.error("ERROR findDataV cant find "+makeDODSname(ddsV)+" on "+location);
     }
       }
     }

     // For each variable either extract the data or use cached data.
     List result = new ArrayList();
     for (int i=0;i<preloadVariables.size (); i++) {
       Variable var = (Variable) preloadVariables.get(i);
       if (var.hasCachedData()) {
     result.add (var.read());

       } else {
     Array data = null;
     DodsV ddsV = (DodsV) var.getSPobject();
     if (ddsV.bt instanceof DGrid) // really want its child array
       ddsV = (DodsV) ddsV.children.get(0);

     DodsV dataV = (DodsV) map.get( ddsV);
     if (dataV == null) {
       logger.error("DODSNetcdfFile.readArrays cant find "+makeDODSname(ddsV)+" in dataDDS; "+location);
       //dataDDS.print( System.out);
     } else {
       if (debugConvertData) System.out.println("readArray converting "+makeDODSname(ddsV));
       dataV.isDone = true;

       data = convertD2N( dataV, var, null, false);
       if (var.isCaching()) {
         var.setCachedData( data, false);
         if (debugCached) System.out.println(" cache for <"+var.getName()+"> length ="+ data.getSize());
       }
     }
     result.add (data);
       }
     }
     return result;
   } */

    ///////////////////////////////////////////////////////////////////////////////////////
    // convert DODS data structures to NetCDF data Structures

    /*
   * Covert DODS BaseType into a netCDF Array
   * @param dataV data to convert is contained in this
   * @param ncVar this is the Variable we are reading
   * @param sectionAll the requested section, including parents, or null for all
   * @param flatten whether to unwrap parent structures, only used if its a member variable.
   * @return Array
   *
  private Array convertD2N( DodsV dataV, Variable ncVar, List sectionAll, boolean flatten) {

    // start with the top variable
    Variable topVar = ncVar;
    while (topVar.isMemberOfStructure()) {
      topVar = topVar.getParentStructure();
    }

    // extract top dodsVar from dataDDS
    BaseType dodsVar = dataV.darray == null ? dataV.bt : dataV.darray;
    /*try {
      dodsVar = dds.getVariable( getDODSConstraintName( topVar));
    } catch (NoSuchVariableException e) {
      // can happen when the f**ing thing is a Grid
      dodsVar = dds.getVariable( getDODSConstraintName( ncVar));
      topVar = ncVar;
    } *

    // recursively extract the data
    Array data = convertData( dodsVar, topVar);

    // flatten if needed
    if (flatten && ncVar.isMemberOfStructure()) {

      // deal with sequences
      Variable v = ncVar;
      boolean isSequence = v.isUnknownLength();
      while (v.isMemberOfStructure()) {
    v = v.getParentStructure();
    if (v.isUnknownLength()) isSequence = true;
      }

      int[] shape = Range.getShape( sectionAll);
      if (isSequence) {
    ArrayList shapeList = new ArrayList();
    addShapes( shapeList, data, ncVar.getName(), null);
    shape = Range.getShape(shapeList);

    // gotta make into 1D array !!
    int size = (int) ucar.ma2.Index.computeSize( shape);
    shape = new int[] { size} ;
      }

      if (ncVar instanceof Structure) {
    ArrayStructure as = (ArrayStructure) data;
    ArrayStructureW flatData = new ArrayStructureW( as.getStructureMembers(), shape);
    IndexIterator flatIterator = flatData.getIndexIterator();
    flattenData( data, flatIterator, ncVar.getName());
    data = flatData;

      } else {
    Array flatData = Array.factory( ncVar.getDataType().getPrimitiveClassType(), shape);
    IndexIterator flatIterator = flatData.getIndexIterator();
    flattenData( data, flatIterator, ncVar.getName());
    data = flatData;
      }

    }

    return data;
  }

  // LOOK nested sequences cant be flattened, because they may have different lengths !!
  private void addShapes( ArrayList shapes, Array data, String varName, String sdName) {
    int[] shape = data.getShape();
    shapes.addAll( Range.factory(shape));

    if (varName.equals(sdName)) return; // done

    Class elemClass = data.getElementType();
    if (elemClass.equals( DataType.STRUCTURE.getPrimitiveClassType())) {
      StructureData ds = (StructureData) data.getObject( data.getIndex());
      StructureMembers.Member m = ds.getMember( 0); // only one ??
      addShapes( shapes, ds.getArray(m), varName, ds.getName());
    }
  }

  private void flattenData( Array data, IndexIterator flatIterator, String varName) {
    IndexIterator ii = data.getIndexIterator();
    Class elemClass = data.getElementType();

    if (elemClass.equals( DataType.STRUCTURE.getPrimitiveClassType())) {
      while (ii.hasNext()) {
    StructureData ds = (StructureData) ii.getObjectNext();
    String sdName = ds.getName();
    if (sdName.equals( varName))
      flatIterator.setObjectNext( ds); // this is the target variable, dont recurse!
    else {
      StructureMembers.Member m = ds.getMember(0); // theres only one
      flattenData(	ds.getArray(m), flatIterator, varName); // recurse
    }
      }
    }

    else if (elemClass.equals( DataType.STRING.getPrimitiveClassType())) {
      while (ii.hasNext())
    flatIterator.setObjectNext( ii.getObjectNext());
    }

    else if (elemClass.equals( DataType.DOUBLE.getPrimitiveClassType())) {
      while (ii.hasNext())
    flatIterator.setDoubleNext( ii.getDoubleNext());
    }

    else if (elemClass.equals( DataType.FLOAT.getPrimitiveClassType())) {
      while (ii.hasNext())
    flatIterator.setFloatNext( ii.getFloatNext());
    }

    else if (elemClass.equals( DataType.INT.getPrimitiveClassType())) {
      while (ii.hasNext())
    flatIterator.setIntNext( ii.getIntNext());
    }

    else if (elemClass.equals( DataType.SHORT.getPrimitiveClassType())) {
      while (ii.hasNext())
    flatIterator.setShortNext( ii.getShortNext());
    }

    else if (elemClass.equals( DataType.LONG.getPrimitiveClassType())) {
      while (ii.hasNext())
    flatIterator.setLongNext( ii.getLongNext());
    }

    else if (elemClass.equals( DataType.BYTE.getPrimitiveClassType())) {
      while (ii.hasNext())
    flatIterator.setByteNext( ii.getByteNext());
    }

    else if (elemClass.equals( DataType.BOOLEAN.getPrimitiveClassType())) {
      while (ii.hasNext())
    flatIterator.setBooleanNext( ii.getBooleanNext());
    }

    else throw new IllegalStateException( "DODSNetcdfFile flattenData "+elemClass.getName());

  }

  // convert dodsVar to Array corresponding to the ncVar
  private Array convertData(opendap.dap.BaseType dodsVar, Variable ncVar) {
    if (debugConvertData)
      System.out.println("  convertData of dods type "+dodsVar.getClass().getName()+" ncType "+ncVar.getDataType());

    if (dodsVar instanceof DSequence) {
      DSequence ds = (DSequence) dodsVar;
      Structure s = (Structure) ncVar;

      StructureMembers members = makeStructureMembers( ds, s);

      // make the result array
      int nrows = ds.getRowCount();
      int[] shape = new int[1];
      shape[0] = nrows;
      ArrayStructureW structArray = new ArrayStructureW( members, shape);

      // populate it
      for (int row=0; row<nrows; row++) {
    Vector v = ds.getRow(row);
    StructureData sd = convertStructureData(structArray, v.elements(), s);
    structArray.setStructureData( sd, row);
      }
      return structArray;
    }

    // this is the "new" 2.0 that (correctly) wraps a Grid array in a Structure
    else if ((dodsVar instanceof DStructure) && (ncVar instanceof DODSGrid)){
      DStructure ds = (DStructure) dodsVar;
      try {
    DArray da = (DArray) ds.getVariable(ncVar.getShortName());
    return convertArray(da, ncVar);
      } catch (NoSuchVariableException e) {
    e.printStackTrace();
    return null;
      }
    }

    else if (dodsVar instanceof DStructure) { // scalar structure
      DStructure ds = (DStructure) dodsVar;
      Structure s = (Structure) ncVar;

      StructureMembers members = makeStructureMembers( ds, s);
      ArrayStructureW structArray = new ArrayStructureW( members, new int[0]);

      StructureData sd = convertStructureData( structArray, ds.getVariables(), s);
      structArray.setStructureData( sd, 0);
      return structArray;
    }

    else if (dodsVar instanceof DGrid) { // scalar grid
      DGrid ds = (DGrid) dodsVar;
      try {
    DArray da = (DArray) ds.getVariable(getDODSConstraintName( ncVar));
    return convertArray(da, ncVar);
      } catch (NoSuchVariableException e) {
    e.printStackTrace();
    return null;
      }
    }

    else if (dodsVar instanceof DArray) {
      DArray da = (DArray) dodsVar;
      return convertArray(da, ncVar);

    } else { // dods scalar

      if ((dodsVar instanceof DString) && (ncVar.getDataType() == DataType.CHAR)) // special case
    return convertStringToChar(dodsVar, ncVar);
      else
    return convertScalar(dodsVar, ncVar.getDataType());
    }
  }

  private StructureMembers makeStructureMembers(DConstructor dodsVar, Structure s) {
    StructureMembers members = new StructureMembers( s.getName());
    Enumeration e = dodsVar.getVariables();
    while (e.hasMoreElements()) {
      dods.dap.BaseType bt = (dods.dap.BaseType) e.nextElement();
      String ncName = StringUtil.unescape( bt.getName());
      Variable v2 = s.findVariable( ncName);
      members.addMember( new StructureMembers.Member( v2.getShortName(), v2.getDescription(),
      v2.getUnitsString(), v2.getDataType(), v2.getShape()));
    }
    return members;
  }


  private Array convertArray(DArray da, Variable ncVar) {
    BaseType elemType = da.getPrimitiveVector().getTemplate();
    if (debugConvertData) System.out.println("	DArray type "+ elemType.getClass().getName());

    if (elemType instanceof DStructure) {  // array of structures LOOK no array of DGrid
      Structure s = (Structure) ncVar;
      StructureMembers members = makeStructureMembers((DStructure) elemType, s);
      ArrayStructureW structArray = new ArrayStructureW( members, makeShape( da));

      // populate it
      IndexIterator ii = structArray.getIndexIterator();
      BaseTypePrimitiveVector pv = (BaseTypePrimitiveVector) da.getPrimitiveVector();
      for (int i=0; i<pv.getLength(); i++) {
    BaseType bt = pv.getValue(i);
    DStructure ds = (DStructure) bt;
    StructureData sd = convertStructureData( structArray, ds.getVariables(), s);
    ii.setObjectNext(sd);
      }
      return structArray;

    } else if (elemType instanceof DString) {
      if (ncVar.getDataType() == DataType.STRING)
    return convertStringArray(da, ncVar);
      else if (ncVar.getDataType() == DataType.CHAR)
    return convertStringArrayToChar(da, ncVar);
      else
    throw new IllegalArgumentException("DODSVariable convertArray invalid dataType= "+ncVar.getDataType()+
        " dodsType= "+elemType.getClass().getName());

    } else {

       // otherwise gotta be a DVector with primitive type
       // create the array, using  DODS internal array so there's no copying
      dods.dap.PrimitiveVector pv = da.getPrimitiveVector();
      Object storage = pv.getInternalStorage();
      //storage = widenArray( pv, storage); // data conversion if needed
      return Array.factory( ncVar.getDataType().getPrimitiveClassType(), makeShape( da), storage);
    }
  }

    // convert a DODS scalar value to a netcdf Array
  private Array convertScalar (BaseType dodsScalar, DataType dataType) {;
    Array scalarData = Array.factory( dataType.getPrimitiveClassType(), new int[0]);
    Index scalarIndex = scalarData.getIndex();

    // set the data value, using scalarIndex from Variable
    if (dodsScalar instanceof DString) {
      String sval = ((DString)dodsScalar).getValue();
    scalarData.setObject( scalarIndex, sval);

    } else if (dodsScalar instanceof DUInt32) {
      int ival = ((DUInt32)dodsScalar).getValue();
      long lval = DataType.unsignedIntToLong( ival);	    // LOOK unsigned
      scalarData.setLong( scalarIndex, lval);

    } else if (dodsScalar instanceof DUInt16) {
      short sval = ((DUInt16)dodsScalar).getValue();
      int ival = DataType.unsignedShortToInt(sval);
      scalarData.setInt( scalarIndex, ival);

    } else if (dataType == DataType.FLOAT)
      scalarData.setFloat( scalarIndex, ((DFloat32)dodsScalar).getValue());
    else if (dataType == DataType.DOUBLE)
      scalarData.setDouble( scalarIndex, ((DFloat64)dodsScalar).getValue());
    else if (dataType == DataType.INT)
      scalarData.setInt( scalarIndex, ((DInt32)dodsScalar).getValue());
    else if (dataType == DataType.SHORT)
      scalarData.setShort( scalarIndex, ((DInt16)dodsScalar).getValue());
    else if (dataType == DataType.BYTE)
      scalarData.setByte( scalarIndex, ((DByte)dodsScalar).getValue());
    else if (dataType == DataType.BOOLEAN)
      scalarData.setBoolean( scalarIndex, ((DBoolean)dodsScalar).getValue());
    else
      throw new IllegalArgumentException("DODSVariable extractScalar invalid dataType= "+dataType+
        " dodsScalar= "+dodsScalar.getClass().getName());

    return scalarData;
  }

  StructureData convertStructureData(ArrayStructureW asw, Enumeration dodsVariables, Structure s) {
    StructureDataW structureData = new StructureDataW(asw.getStructureMembers());

    while (dodsVariables.hasMoreElements()) {
      dods.dap.BaseType bt = (dods.dap.BaseType) dodsVariables.nextElement();
      if (debugConvertData) System.out.println("  convertStructureData member: " + bt.getName());
      String ncName = StringUtil.unescape( bt.getName());
      Variable v = s.findVariable( ncName);
      if (v == null)
    throw new IllegalStateException("cant find member <"+ncName+"> in structure = "+s.getName());
      Array data = convertData(bt, v);
      structureData.setMemberData(ncName, data);
    }
    return structureData;
  }

  private Array convertStringArray(DArray dv, Variable ncVar) {
    dods.dap.PrimitiveVector pv = dv.getPrimitiveVector();
    BaseTypePrimitiveVector btpv = (BaseTypePrimitiveVector) pv;
    int nStrings = btpv.getLength();
    String[] storage = new String[nStrings];
    int max_len = 0;
    for (int i=0; i<nStrings; i++) {
      BaseType bb = btpv.getValue(i);
      storage[i] = ((DString)bb).getValue();
      max_len = Math.max( max_len, storage[i].length());
    }

    // LOOK deal with length=1 barfalloney
    if (max_len == 1) {
      int[] shape = ncVar.getShape();
      int rank = shape.length;
      int extraDimSize = shape[rank-1];
      int newSize = (int)ncVar.getSize()/extraDimSize;
      String[] newStorage = new String[newSize];

      // merge last dimension
      StringBuffer sbuff = new StringBuffer();
      int newCount = 0;
      while (newCount < newSize) {
    int mergeCount = 0;
    sbuff.setLength(0);
    while (mergeCount < extraDimSize) {
      String s = storage[extraDimSize * newCount + mergeCount];
      if (s.length() == 0) break;
      sbuff.append( s);
      mergeCount++;
    }
    newStorage[ newCount++] = sbuff.toString();
      }

      // adjust the dimensions
      List dims = ncVar.getDimensions();
      ncVar.setDimensions( dims.subList(0, rank-1));
      int[] newShape = ncVar.getShape();
      return Array.factory( DataType.STRING.getPrimitiveClassType(), newShape, newStorage);
    }

    else
      return Array.factory( DataType.STRING.getPrimitiveClassType(), makeShape( dv), storage);
  }

  private Array convertStringArrayToChar(DArray dv, Variable ncVar) {
    dods.dap.PrimitiveVector pv = dv.getPrimitiveVector();
    BaseTypePrimitiveVector btpv = (BaseTypePrimitiveVector) pv;
    int nStrings = btpv.getLength();

    int rank = ncVar.getRank();
    int[] shape = ncVar.getShape();
    int strLen = shape[ rank-1];
    int total = (int) ncVar.getSize();

    char[] storage = new char[total];
    int pos = 0;
    for (int i=0; i<nStrings; i++) {
      BaseType bb = btpv.getValue(i);
      if (!(bb instanceof DString))
    throw new IllegalArgumentException("DODSVariable extractArray from DVector: should be DString = "+bb.getClass().getName());
      String val = ((DString)bb).getValue();
      int len = Math.min( val.length(), strLen);
      for (int k=0; k<len; k++)
    storage[pos+k] = val.charAt(k);
      pos += strLen;
    }

    return Array.factory( DataType.CHAR.getPrimitiveClassType(), shape, storage);
  }

  private Array convertStringToChar(BaseType dodsScalar, Variable ncVar) {
    String sval = ((DString)dodsScalar).getValue();

    int total = (int) ncVar.getSize();
    char[] storage = new char[total];

    int len = Math.min( sval.length(), total);
    for (int k=0; k<len; k++)
      storage[k] = sval.charAt(k);

    return Array.factory( DataType.CHAR.getPrimitiveClassType(), ncVar.getShape(), storage);
  }

  protected Object widenArray( PrimitiveVector pv, Object storage) {
    if (pv instanceof UInt32PrimitiveVector) {
      UInt32PrimitiveVector org = (UInt32PrimitiveVector) pv;
      int len = pv.getLength();
      long [] lpv = new long[len];
      for (int i=0; i<len; i++)
    lpv[i] = DataType.unsignedIntToLong( org.getValue(i));
      storage = lpv;

    } else if (pv instanceof UInt16PrimitiveVector) {
      UInt16PrimitiveVector org = (UInt16PrimitiveVector) pv;
      int len = pv.getLength();
      int [] ipv = new int[len];
      for (int i=0; i<len; i++)
    ipv[i] = DataType.unsignedShortToInt( org.getValue(i));
      storage = ipv;
    }

    return storage;
  }  */

    ////////////////////////////////////////////////////////////////////////////////
    // debugging

    public void getDetailInfo(Formatter f)
    {
        super.getDetailInfo(f);

        f.format("DDS = %n");
        ByteArrayOutputStream buffOS = new ByteArrayOutputStream(8000);
        dds.print(buffOS);
        f.format("%s%n", new String(buffOS.toByteArray(),Util.UTF8));

        f.format("%nDAS = %n");
        buffOS = new ByteArrayOutputStream(8000);
        das.print(buffOS);
        f.format("%s%n", new String(buffOS.toByteArray(),Util.UTF8));
    }

    public String getFileTypeId()
    {
        return "OPeNDAP";
    }

    public String getFileTypeDescription()
    {
        return "Open-source Project for a Network Data Access Protocol";
    }

    public static void main(String arg[])
    {
        String url = "http://localhost:8080/thredds/dodsC/testContent/testData.nc.ascii?reftime[0:1:0]";

        // "http://ingrid.ldeo.columbia.edu/expert/SOURCES/.LEVITUS94/dods";
        try (DODSNetcdfFile df = new DODSNetcdfFile(url, null)) {
            System.out.println("dods file = " + url + "\n" + df);
        } catch (Exception ioe) {
            System.out.println("error = " + url);
            ioe.printStackTrace();
        }
    }

}

/* Note 1

http://data.nodc.noaa.gov/opendap/pathfinder/Version5.0_Climatologies/Monthly/Day/month01_day.hdf

Dataset {
    Grid {
      Array:
	UInt16 Clim_SST[Latitude = 4096][Longitude = 8192];
      Maps:
	Float64 Latitude[4096];
	Float64 Longitude[8192];
    } Clim_SST;

    Grid {
      Array:
	UInt16 Clim_StandardDeviation[Latitude = 4096][Longitude = 8192];
      Maps:
	Float64 Latitude[4096];
	Float64 Longitude[8192];
    } Clim_StandardDeviation;

    Grid {
      Array:
	Byte Clim_Counts[Latitude = 4096][Longitude = 8192];
      Maps:
	Float64 Latitude[4096];
	Float64 Longitude[8192];
    } Clim_Counts;

    Float64 Longitude[fakeDim2 = 8192];
    Float64 Latitude[fakeDim3 = 4096];

    Grid {
      Array:
	UInt16 Clim_SST_Filled[Latitude = 4096][Longitude = 8192];
      Maps:
	Float64 Latitude[4096];
	Float64 Longitude[8192];
    } Clim_SST_Filled;

} month01_day.hdf;

*/
