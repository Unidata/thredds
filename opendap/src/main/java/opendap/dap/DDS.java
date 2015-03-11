/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////


package opendap.dap;
import ucar.nc2.util.EscapeStrings;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Stack;
import java.io.*;

import opendap.dap.parsers.DDSXMLParser;
import opendap.dap.parsers.*;
import opendap.util.Debug;
import org.jdom2.Document;

/**
 * The OPeNDAP Data Descriptor Object (DDS) is a data structure used by
 * the OPeNDAP software to describe datasets and subsets of those
 * datasets.  The DDS may be thought of as the declarations for the
 * data structures that will hold data requested by some OPeNDAP client.
 * Part of the job of a OPeNDAP server is to build a suitable DDS for a
 * specific dataset and to send it to the client.  Depending on the
 * data access API in use, this may involve reading part of the
 * dataset and inferring the DDS.  Other APIs may require the server
 * simply to read some ancillary data file with the DDS in it.
 * <p/>
 * On the server side, in addition to the data declarations, the DDS
 * holds the clauses of any constraint expression that may have
 * accompanied the data request from the OPeNDAP client.  The DDS object
 * includes methods for modifying the DDS according to the given
 * constraint expression.  It also has methods for directly modifying
 * a DDS, and for transmitting it from a server to a client.
 * <p/>
 * For the client, the DDS object includes methods for reading the
 * persistent form of the object sent from a server. This includes parsing
 * the ASCII representation of the object and, possibly, reading data
 * received from a server into a data object.
 * <p/>
 * Note that the class DDS is used to instantiate both DDS and DataDDS
 * objects. A DDS that is empty (contains no actual data) is used by servers
 * to send structural information to the client. The same DDS can becomes a
 * DataDDS when data values are bound to the variables it defines.
 * <p/>
 * For a complete description of the DDS layout and protocol, please
 * refer to <em>The OPeNDAP User Guide</em>.
 * <p/>
 * The DDS has an ASCII representation, which may be transmitted
 * from a OPeNDAP server to a client.  Here is the DDS representation of
 * an entire dataset containing a time series of worldwide grids of
 * sea surface temperatures:
 * <p/>
 * <blockquote><pre>
 *  Dataset {
 *      Float64 lat[lat = 180];
 *      Float64 lon[lon = 360];
 *      Float64 time[time = 404];
 *      Grid {
 *       ARRAY:
 *          Int32 sst[time = 404][lat = 180][lon = 360];
 *       MAPS:
 *          Float64 time[time = 404];
 *          Float64 lat[lat = 180];
 *          Float64 lon[lon = 360];
 *      } sst;
 *  } weekly;
 * </pre></blockquote>
 * <p/>
 * If the data request to this dataset includes a constraint
 * expression, the corresponding DDS might be different.  For
 * example, if the request was only for northern hemisphere data
 * at a specific time, the above DDS might be modified to appear like
 * this:
 * <p/>
 * <blockquote><pre>
 *  Dataset {
 *      Grid {
 *       ARRAY:
 *          Int32 sst[time = 1][lat = 90][lon = 360];
 *       MAPS:
 *          Float64 time[time = 1];
 *          Float64 lat[lat = 90];
 *          Float64 lon[lon = 360];
 *      } sst;
 *  } weekly;
 * </pre></blockquote>
 * <p/>
 * Since the constraint has narrowed the area of interest, the range
 * of latitude values has been halved, and there is only one time
 * value in the returned array.  Note that the simple arrays (<em>lat</em>,
 * <em>lon</em>, and <em>time</em>) described in the dataset are also part of
 * the <em>sst</em> Grid object.  They can be requested by themselves or as
 * part of that larger object.
 * <h3>DDX</h3>
 * The DDS also has an XML representation. This is known as a DDX. Since
 * <code>BaseType</code> variables now each have their own set of <code>Attributes</code>
 * it has become necessary to have a representation of the DDS that captures these
 * relationships. Consider the previous example. A correctly constructed
 * DAS for that DDS might look like:
 * <blockquote><pre>
 * Attributes {
 * lat {
 * String fullName "latitude";
 * String units "degrees North";
 * }
 * lon {
 * String fullName "longitude";
 * String units "degrees East";
 * }
 * time {
 * String units "seconds";
 * }
 * sst {
 * String fullName "Sea Surface Temperature";
 * String units "degrees centigrade";
 * sst {
 * Alias fullName .sst.fullName;
 * Alias units .sst.units;
 * }
 * time {
 * Alias units .time.units;
 * }
 * lat {
 * Alias fullName .lat.fullName;
 * Alias units .lat.units;
 * }
 * lon {
 * Alias fullName .lon.fullName;
 * Alias units .lon.units;
 * }
 * }
 * }
 * </pre></blockquote>
 * <p/>
 * <p/>
 * Combined with the DDS and expressed as a DDX it would look like:
 * <p/>
 * <blockquote><pre>
 * <p/>
 * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 * &lt;Dataset name=&quot;weekly&quot;
 * xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot;
 * xmlns=&quot;http://xml.opendap.org/ns/DAP2&quot;
 * xsi:schemaLocation=&quot;http://xml.opendap.org/ns/DAP2  http://xml.opendap.org/dap/dap2.xsd&quot; &gt;
 * <p/>
 * <p/>
 * &lt;Array name=&quot;lat&quot;&gt;
 * &lt;Attribute name=&quot;fullName&quot; type=&quot;String&quot;&gt;
 * &lt;value&gt;&amp;quot;latitude&amp;quot;&lt;/value&gt;
 * &lt;/Attribute&gt;
 * &lt;Attribute name=&quot;units&quot; type=&quot;String&quot;&gt;
 * &lt;value&gt;&amp;quot;degrees North&amp;quot;&lt;/value&gt;
 * &lt;/Attribute&gt;
 * &lt;Float64/&gt;
 * &lt;dimension name=&quot;lat&quot; size=&quot;180&quot;/&gt;
 * &lt;/Array&gt;
 * &lt;Array name=&quot;lon&quot;&gt;
 * &lt;Attribute name=&quot;fullName&quot; type=&quot;String&quot;&gt;
 * &lt;value&gt;&amp;quot;longitude&amp;quot;&lt;/value&gt;
 * &lt;/Attribute&gt;
 * &lt;Attribute name=&quot;units&quot; type=&quot;String&quot;&gt;
 * &lt;value&gt;&amp;quot;degrees East&amp;quot;&lt;/value&gt;
 * &lt;/Attribute&gt;
 * &lt;Float64/&gt;
 * &lt;dimension name=&quot;lon&quot; size=&quot;360&quot;/&gt;
 * &lt;/Array&gt;
 * &lt;Array name=&quot;time&quot;&gt;
 * &lt;Attribute name=&quot;units&quot; type=&quot;String&quot;&gt;
 * &lt;value&gt;&amp;quot;seconds&amp;quot;&lt;/value&gt;
 * &lt;/Attribute&gt;
 * &lt;Float64/&gt;
 * &lt;dimension name=&quot;time&quot; size=&quot;404&quot;/&gt;
 * &lt;/Array&gt;
 * &lt;Grid  name=&quot;sst&quot;&gt;
 * &lt;Attribute name=&quot;fullName&quot; type=&quot;String&quot;&gt;
 * &lt;value&gt;&amp;quot;Sea Surface Temperature&amp;quot;&lt;/value&gt;
 * &lt;/Attribute&gt;
 * &lt;Attribute name=&quot;units&quot; type=&quot;String&quot;&gt;
 * &lt;value&gt;&amp;quot;degrees centigrade&amp;quot;&lt;/value&gt;
 * &lt;/Attribute&gt;
 * &lt;Array name=&quot;sst&quot;&gt;
 * &lt;Alias name=&quot;fullName&quot; Attribute=&quot;.sst.fullName&quot;/&gt;
 * &lt;Alias name=&quot;units&quot; Attribute=&quot;.sst.units&quot;/&gt;
 * &lt;Int32/&gt;
 * &lt;dimension name=&quot;time&quot; size=&quot;404&quot;/&gt;
 * &lt;dimension name=&quot;lat&quot; size=&quot;180&quot;/&gt;
 * &lt;dimension name=&quot;lon&quot; size=&quot;360&quot;/&gt;
 * &lt;/Array&gt;
 * &lt;Map name=&quot;time&quot;&gt;
 * &lt;Alias name=&quot;units&quot; Attribute=&quot;.time.units&quot;/&gt;
 * &lt;Float64/&gt;
 * &lt;dimension name=&quot;time&quot; size=&quot;404&quot;/&gt;
 * &lt;/Map&gt;
 * &lt;Map name=&quot;lat&quot;&gt;
 * &lt;Alias name=&quot;fullName&quot; Attribute=&quot;.lat.fullName&quot;/&gt;
 * &lt;Alias name=&quot;units&quot; Attribute=&quot;.lat.units&quot;/&gt;
 * &lt;Float64/&gt;
 * &lt;dimension name=&quot;lat&quot; size=&quot;180&quot;/&gt;
 * &lt;/Map&gt;
 * &lt;Map name=&quot;lon&quot;&gt;
 * &lt;Alias name=&quot;fullName&quot; Attribute=&quot;.lon.fullName&quot;/&gt;
 * &lt;Alias name=&quot;units&quot; Attribute=&quot;.lon.units&quot;/&gt;
 * &lt;Float64/&gt;
 * &lt;dimension name=&quot;lon&quot; size=&quot;360&quot;/&gt;
 * &lt;/Map&gt;
 * &lt;/Grid&gt;
 * <p/>
 * &lt;dataBLOB href=&quot;cid:ContentIdOfTheMIMEAttcahmentContainingTheDataBlob&quot;/&gt;
 * &lt;/Dataset&gt;
 * <p/>
 * <p/>
 * </pre></blockquote>
 * The DDX can also be sent from a server to a client.
 * <p/>
 * <h3>Using the DDS's API to construct a DDS</h3>
 * Many developers choose not to use the <code>DDSParser</code> to
 * build <code>DDS's</code> but to build them
 * using the <code>DDS</code> API. This is typical of devlopers
 * writing servers that work with
 * information rich data formats such as NetCDF or HDF. With the
 * addition of <code>Attributes</code>
 * (and <code>Attribute</code> containers) to all of the datatypes in the DAP
 * it is now possible to construct a <code>DDS</code>
 * that contains all of the source meta-data from the original data source. This is
 * an extremly useful thing. However, when building a <code>DDS</code>
 * using the <code>DDS</code> API be sure
 * to call the functions <code>DDS.checkForAttributeNameConflict()</code> and
 * <code>DDS.resolveAliases()</code></li> on the new DDS prior to releasing it from
 * the code that builds it. Otherwise the DDS may have functional problems!
 * <p/>
 * <p/>
 * See <em>The OPeNDAP User Guide</em>, or the documentation of the
 * BaseType class for descriptions of the OPeNDAP data types.
 *
 * @author ndp
 * @version $Revision: 22951 $
 * @opendap.ddx.experimental Many parts of this class have been modified to support
 * the DDX functionality. This API is going to change!
 * @see BaseType
 * @see BaseTypeFactory
 * @see DAS
 * @see opendap.dap.parsers.DDSXMLParser
 * @see #checkForAttributeNameConflict
 * @see #resolveAliases
 */
public class DDS extends DStructure
{

    private static final boolean _Debug = false;

    // Used by resolveAliases()
    private BaseType currentBT;
    private AttributeTable currentAT;

    // Some handy definitons.
    private static final char slash = '\\';
    private static final char quote = '\"';
    private static final char dot = '.';


    /**
     * Factory for new DAP variables.
     */
    private BaseTypeFactory factory;


    private static final String defaultSchemaLocation = "http://xml.opendap.org/dap/dap2.xsd";
    private static final String opendapNameSpace = "http://xml.opendap.org/ns/DAP2";
    private String schemaLocation;


    private String _dataBlobID = null;

    private String url = null;

    /**
     * Creates an empty <code>DDS</code>.
     */
    public DDS()
    {
        this(null, new DefaultFactory());
    }

    /**
     * Creates an empty <code>DDS</code> with the given dataset name.
     *
     * @param clearname the dataset name
     */
    public DDS(String clearname)
    {
        this(clearname, new DefaultFactory());
    }

    /**
     * Creates an empty <code>DDS</code> with the given
     * <code>BaseTypeFactory</code>.  This will be used for OPeNDAP servers which
     * need to construct subclasses of the various <code>BaseType</code> objects
     * to hold additional server-side information.
     *
     * @param factory the server <code>BaseTypeFactory</code> object.
     */
    public DDS(BaseTypeFactory factory)
    {
        this("", factory);
    }

    /**
     * Creates an empty <code>DDS</code> with the given dataset name and
     * <code>BaseTypeFactory</code>.  This will be used for OPeNDAP servers which
     * need to construct subclasses of the various <code>BaseType</code> objects
     * to hold additional server-side information.
     *
     * @param clearname       the dataset name
     * @param factory the server <code>BaseTypeFactory</code> object.
     */
    public DDS(String clearname, BaseTypeFactory factory)
    {
        this(clearname, factory, defaultSchemaLocation);
    }

    /**
     * Creates an empty <code>DDS</code> with the given dataset name and
     * <code>BaseTypeFactory</code>.  This will be used for OPeNDAP servers which
     * need to construct subclasses of the various <code>BaseType</code> objects
     * to hold additional server-side information.
     *
     * @param clearname       the dataset name
     * @param factory the server <code>BaseTypeFactory</code> object.
     * @param schema  the URL where the parser can find an instance of the
     *                OPeNDAP namespace schema.
     * @opendap.ddx.experimental
     */
    public DDS(String clearname, BaseTypeFactory factory, String schema)
    {
        super(clearname);
        vars = new Vector();
        this.factory = factory;
        schemaLocation = schema;
    }

    public void setURL(String url) {this.url = url;}

    public boolean parse(InputStream stream)
	throws ParseException, DAP2Exception
    {
        try {
            String text = DConnect2.captureStream(stream);
            //System.err.println("----------"+text+"\n----------------"); System.err.flush();
            return parse(text);
        } catch (IOException ioe) {
            throw new ParseException("Cannot read DDS",ioe);
        }
    }

     public boolean parse(String text)
	throws ParseException, DAP2Exception
    {
        Dap2Parser parser = new Dap2Parser(factory);
        parser.setURL(url);

	int result = parser.ddsparse(text,this);

	if(result == Dap2Parse.DapERR)
	    throw parser.getERR();
	return (result == Dap2Parse.DapDDS ? true : false);
    }


    /**
     * Get the Class factory.  This is the machine that builds classes
     * for the internal representation of the data set.
     *
     * @return the BaseTypeFactory.
     */
    public final BaseTypeFactory getFactory()
    {
        return factory;
    }

    /**
     * Get the Class factory.  This is the machine that builds classes
     * for the internal representation of the data set.
     */
    public final void setFactory(BaseTypeFactory btf)
    {
        factory = btf;
    }


    /**
     * Set's the dataBLOB reference for this DDS.
     * The dataBLOB element has an attribute, href, which is used to reference the MIME part
     * of a the Multipart MIME document in a DAP4 data response that contains the binary encoded
     * data described by the DDX document.
     *
     * @param contentID A <code>String</code> containing the Content-ID of the MIME part that contains
     *                  the binary encoded data represented by this DDS.
     * @opendap.ddx.experimental
     */
    public void setBlobContentID(String contentID)
    {
        _dataBlobID = contentID;
    }

    /**
     * Get's the dataBLOB Contnet-ID for this DDS.
     * The dataBLOB element has an attribute, href, which is used to reference the MIME part
     * of a the Multipart MIME document in a DAP4 data response that contains the binary encoded
     * data described by the DDX document.
     *
     * @return A <code>String</code> containing the URL of the servers BLOB response for
     *         this DDS.
     * @opendap.ddx.experimental
     */
    public String getBlobContentID()
    {
        return (_dataBlobID);
    }

    /**
     * Creates a DAS object from the collection of <code>BaseType</code> variables and their
     * associated <code>Attributes</code>. This DAS is correctly formed (vis-a-vis the DAP
     * specification) for this DDS.
     *
     * @return A correctly formed <code>DAS</code> object for this DDS.
     * @throws DASException
     * @see DAS
     * @see BaseType
     */
    public DAS getDAS() throws DASException
    {

        DAS myDAS = new DAS();

        try {

            // Since the DDS can contain Attributes, in addtion to Attribute containers (AttributeTables)
            // at the top (dataset) level and the DAS cannot, it is required that these Attributes be
            // bundled into a container at the top level of the DAS.
            // In the code that follows this container is called "looseEnds"

            // Make the container.
            AttributeTable looseEnds = new AttributeTable(getLooseEndsTableName());

            // Carfully populate it from the one at our top-level. Since we are using the
            // API and also copying containers here in order to build a new version,
            // we must use a clone of this (the DDS's) AttributeTable.
            AttributeTable atTbl = (AttributeTable) getAttributeTable().clone();

            int countLooseAttributes = 0;
            // Work on each Attribute in the container.
            Enumeration e = atTbl.getNames();
            while (e.hasMoreElements()) {
                String aName = (String) e.nextElement();
                Attribute a = atTbl.getAttribute(aName);
                String clearname = a.getClearName();

                if (a.isAlias()) { // copy an alias.

                    String attribute = ((Alias) a).getAliasedToAttributeFieldAsClearString();

                    looseEnds.addAlias(clearname, convertDDSAliasFieldsToDASAliasFields(attribute));
                    countLooseAttributes++;

                } else if (a.isContainer()) {
                    // A reference copy. This is why we are working with a clone.
                    myDAS.addAttributeTable(clearname, a.getContainer());
                } else { // copy an Attribute and it's values...

                    int type = a.getType();

                    Enumeration vals = a.getValues();
                    while (vals.hasMoreElements()) {
                        String value = (String) vals.nextElement();
                        looseEnds.appendAttribute(clearname, type, value, true);
                    }
                    countLooseAttributes++;

                }
            }

            if (_Debug) {
		DAPNode.log.debug("Found " + countLooseAttributes + " top level Attributes.");
	    }

            //if (_Debug) myDAS.print(LogStream.dbg);

            // Only add this AttributeTable if actually contains Attributes!
            if (countLooseAttributes > 0) {
                if (_Debug) DAPNode.log.debug("Creating looseEnds table: " + looseEnds.getEncodedName());
                myDAS.addAttributeTable(looseEnds.getEncodedName(), looseEnds);
            }

// Walk through the variables at the top level.
            e = getVariables();
            while (e.hasMoreElements()) {
                BaseType bt = (BaseType) e.nextElement();
                // Build the correct AttributeTable for it at the Toplevel of the DAS
                buildDASAttributeTable(bt, myDAS);
            }

            //if (_Debug) myDAS.print(LogStream.dbg);

            // Make sure that the Aliases resolve correctly. Since we are moving from a
            // DDS/DDX space to a DFAS space the Aliases may not be resolvable. In that
            // case an exception will get thrown...
            myDAS.resolveAliases();

        } catch (Exception e) {
            e.printStackTrace();
            throw new DASException(opendap.dap.DAP2Exception.UNKNOWN_ERROR,
                    "Could not create a DAS from this DDX object.\n" +
                            "Because of the structural differences between the DDX and the DAS it is " +
                            "possible for the DDX to contain sets of Attributes that cannot be represented " +
                            "in a DAS object.\n" +
                            "The specific problem was an execption of type " + e.getClass().getName() + " with an " +
                            "error message of: \n" +
                            e.getMessage());
        }

        return (myDAS);

    }


    /**
     * This method just makes sure that the attribute field in each Aliases resolves correctly
     * if there ends up being a "looseEnds" Attribute Table at the top level.
     *
     * @param attribute
     * @return
     * @throws MalformedAliasException
     */
    private String convertDDSAliasFieldsToDASAliasFields(String attribute) throws MalformedAliasException
    {

        String prefix = "";

        Vector aNames = tokenizeAliasField(attribute);

        // We know that the first token should be a dot, we look at the
        // second token to see if it references a variable in the DDS.

        String topName = (String) aNames.get(1);

        boolean foundIt = false;
        Enumeration e = getVariables();
        while (e.hasMoreElements()) {
            BaseType bt = (BaseType) e.nextElement();
            String normName = normalize(bt.getEncodedName());

            if (topName.equals(normName))
                foundIt = true;
        }

        if (!foundIt) {
// The Attribute referenced is at the top level of the DDS itself.
            // The Attributes at the top level of the DDS get repackaged into
            // a special AttributeTable, this makes the Aliases that point to
            // any of these Attribute resolve correctly.
            prefix = "." + getLooseEndsTableName();

        }

        return (prefix + attribute);

    }


    /**
     * Make a name for an attribute table at the top level in which to
     * place any loose attributes at the top level.
     *
     * @return
     * @see #getDAS
     */
    private String getLooseEndsTableName()
    {

        return (checkLooseEndsTableNameConflict(this.getClearName(), 0));
    }

    /**
     * Make A helper function for <code>getLooseEndsTableName</code>
     * insures that there are no naming conflicts when creating a looseEnds
     * <code>AttributeTable</code>
     *
     * @param clearname
     * @param attempt
     * @return
     * @see #getLooseEndsTableName
     * @see #getDAS
     */
    private String checkLooseEndsTableNameConflict(String clearname, int attempt)
    {
        Enumeration e = getVariables();
        while (e.hasMoreElements()) {
            BaseType bt = (BaseType) e.nextElement();
            String btName = bt.getEncodedName();

            //LogStream.out.println("bt: '"+btName+"'  dataset: '"+name+"'");

            if (btName.equals(clearname)) {
                clearname = repairLooseEndsTableConflict(clearname, attempt++);
                clearname = checkLooseEndsTableNameConflict(clearname, attempt);
            }
        }

        AttributeTable at = getAttributeTable();
        e = at.getNames();
        while (e.hasMoreElements()) {
            String aName = (String) e.nextElement();
            if (aName.equals(clearname)) {
                clearname = repairLooseEndsTableConflict(clearname, attempt++);
                clearname = checkLooseEndsTableNameConflict(clearname, attempt);
            }
        }
        return (clearname);
    }

    /**
     * A helper function for <code>checkLooseEndsTableNameConflict</code>
     * insures that there are no naming conflicts when creating a looseEnds
     * <code>AttributeTable</code>
     *
     * @param badName
     * @param attempt
     * @return
     * @see #checkLooseEndsTableNameConflict
     * @see #getLooseEndsTableName
     * @see #getDAS
     */
    private String repairLooseEndsTableConflict(String badName, int attempt)
    {

        DAPNode.log.debug("Repairing toplevel attribute table name conflict. Attempt: " + attempt);

        String name = "";

        switch (attempt) {

        case 0:
            name = badName + "_DatasetAttributes_0";
            break;
        default:
            int last_ = badName.lastIndexOf("_");
            name = badName.substring(0, last_) + "_" + attempt;
            break;
        }

        return (name);

    }


    /**
     * Builds AttributeTables (from BaseType variables) for us in
     * a DAS created by getDAS()
     *
     * @param bt
     * @param atbl
     * @throws DASException
     * @see #getDAS()
     */
    private void buildDASAttributeTable(BaseType bt, AttributeTable atbl)
            throws DASException
    {

        // Get this BaseType's AttributeTable. Since we are using the AttributeTable
        // interface to build the DAS (which will have a different structure than the
        // table we are getting anyway), we don't need a copy, only the reference.
        AttributeTable tBTAT = bt.getAttributeTable();

        // if the table is empty, then do nothing
        if(tBTAT == null || tBTAT.size() == 0)
            return;

        // Start a new (child) AttributeTable (using the name of the one we are
        // copying) in the (parent) AttributeTable we are working on.
        AttributeTable newAT = atbl.appendContainer(tBTAT.getEncodedName());

        if (_Debug) {
		DAPNode.log.debug("newAT.getName(): " + newAT.getEncodedName());
	    }

        // Get each Attribute in the AttributeTable that we are copying,
        // and then put it's values into our new AttributeTable;
        Enumeration e = tBTAT.getNames();
        while (e.hasMoreElements()) {
            String attrName = (String) e.nextElement();
            Attribute attr = tBTAT.getAttribute(attrName);
            populateAttributeTable(newAT, attr);
        }

        // If this BaseType is a "container" type (aka complex type, aka DConstructor)
        // Then we have to search it's children for Attributes and then  them
        // and put them in our new Attribute Table.
        if (bt instanceof DConstructor) {
            Enumeration v = ((DConstructor) bt).getVariables();

            while (v.hasMoreElements()) {
                BaseType thisBT = (BaseType) v.nextElement();
                buildDASAttributeTable(thisBT, newAT);
            }
        }


    }


    /**
     * Builds AttributeTables (from BaseType variables) for us in
     * a DAS created by getDAS()
     *
     * @param atTable
     * @param attr
     * @throws DASException
     * @see #buildDASAttributeTable(BaseType, AttributeTable)
     * @see #getDAS()
     */
    private void populateAttributeTable(AttributeTable atTable, Attribute attr)
            throws DASException
    {
        // Always check for Aliases first! They return the values for their targets
        // when asked if they are containers!
        if (attr.isAlias()) {

            String alias = attr.getEncodedName();
            String attribute = ((Alias) attr).getAliasedToAttributeFieldAsClearString();
            if (_Debug) DAPNode.log.debug("Adding Alias name: " + alias);
            atTable.addAlias(alias, convertDDSAliasFieldsToDASAliasFields(attribute));
        } else if (attr.isContainer()) {
            // If this Attribute is a container of other Attributes (an AttributeTable)
            // then we need to recurse to get it's children.

            // Get this Attribute's container (AttributeTable).
            // Since we are using the AttributeTable
            // interface to build the DAS (which will have a different structure than the
            // table we are getting anyway), we don't need a copy, only the reference.
            AttributeTable thisTable = attr.getContainer();

            // Start a new (child) AttributeTable (using the name of the one we are
            // copying) in the (parent) AttributeTable we are working on.
            if (_Debug) DAPNode.log.debug("Appending AttributeTable name: " + thisTable.getEncodedName());
            AttributeTable newTable = atTable.appendContainer(thisTable.getEncodedName());

            // Get each Attribute in the AttributeTable that we are copying,
            // and then put it's values into our new AttributeTable;
            Enumeration e = thisTable.getNames();
            while (e.hasMoreElements()) {
                String attrName = (String) e.nextElement();
                Attribute thisAttr = thisTable.getAttribute(attrName);
                populateAttributeTable(newTable, thisAttr);
            }
        } else {
            // Since the Attribute is a "leaf" and not a container we need to
            // push it's contents into the AttributeTable that we are building.
            int type = attr.getType();
            String name = attr.getEncodedName();
            Enumeration v = attr.getValues();
            while (v.hasMoreElements()) {
                String value = (String) v.nextElement();
                if (_Debug)
                    DAPNode.log.debug("AtributeTable: " + atTable.getEncodedName() + " Appending Attribute name: " + name + "  type: " + type + " value: " + value);
                atTable.appendAttribute(name, type, value);
            }
        }
    }


    /**
     * Print a DAS constructed from this DDS and it's BaseType variables.
     *
     * @param os The <code>OutputStream</code> to print to.
     */
    public void printDAS(OutputStream os)
    {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os,Util.UTF8)));
        printDAS(pw);
        pw.flush();
    }


    /**
     * Print a DAS constructed from this DDS and it's BaseType variables.
     *
     * @param pw The <code>PrintWriter</code> to print to.
     */
    public void printDAS(PrintWriter pw)
    {

        DAS myDAS = null;

        try {
            myDAS = this.getDAS();
            myDAS.print(pw);
        } catch (DASException dasE) {
            pw.println("\n\nCould not get a DAS object to print!\n" +
                    "DDS.getDAS() threw an Exception. Message: \n" +
                    dasE.getMessage());
        }
    }


    /**
     * Removes a variable from the <code>DDS</code>.
     * Does nothing if the variable can't be found.
     * If there are multiple variables with the same name, only the first
     * will be removed.  To detect this, call the <code>checkSemantics</code>
     * method to verify that each variable has a unique name.
     *
     * @param name the name of the variable to remove.
     * @see DDS#checkSemantics(boolean)
     */
    public void delVariable(String name)
    {
        try {
            BaseType bt = getVariable(name);
            vars.removeElement(bt);
        } catch (NoSuchVariableException e) {
        }
    }

    /**
     * Is the variable <code>var</code> a vector of DConstructors? Return
     * true if it is, false otherwise. This mess will recurse into a
     * DVector's template BaseType (which is a BaseTypePrimivitiveVector) and
     * look to see if that is either a DConstructor or <em>contains</em> a
     * DConstructor. So the <code>List Strucutre { ... } g[10];</code> should
     * be handled correctly. <p>
     * <p/>
     * Note that the List type modifier may only appear once.
     */
    private DConstructor isVectorOfDConstructor(BaseType var)
    {
        if (!(var instanceof DVector))
            return null;
        if (!(((DVector) var).getPrimitiveVector()
                instanceof BaseTypePrimitiveVector))
            return null;
        // OK. We have a DVector whose template is a BaseTypePrimitiveVector.
        BaseTypePrimitiveVector btpv = (BaseTypePrimitiveVector)
                ((DVector) var).getPrimitiveVector();
        // After that nasty cast, is the template a DConstructor?
        if (btpv.getTemplate() instanceof DConstructor)
            return (DConstructor) btpv.getTemplate();
        else
            return isVectorOfDConstructor(btpv.getTemplate());
    }

    /**
     * Returns a reference to the named variable.
     *
     * @param name the name of the variable to return.
     * @return the variable named <code>name</code>.
     * @throws NoSuchVariableException if the variable isn't found.
     */
    public BaseType getVariable(String name) throws NoSuchVariableException
    {
        Stack s = new Stack();
        s = search(name, s);
        return (BaseType) s.pop();
    }

    /**
     * Adds a variable to the container. This overrides the
     * getVariable() in DStructure in order to keep the
     * parent value from getting set. Otherwise the
     * name of the DDS (which is typically the name of the Dataset)
     * will appear in calls to BaseType.getLongName().
     * <h2>
     * This might be a mistake!!! Watch out for bugs induced by this
     * method.
     * </h2>
     *
     * @param v    the variable to add.
     * @param part ignored for <code>DSequence</code>.
     */
    public void addVariable(BaseType v, int part)
    {
        v.setParent(this);
        vars.addElement(v);
    }

    /**
     * Look for <code>name</code> in the DDS. Start the search using the
     * ctor variable (or array/list of ctors) found on the top of the Stack
     * <code>compStack</code> (for component stack). When the named variable
     * is found, return the stack compStack modified so that it now contains
     * each ctor-type variable that on the path to the named variable. If the
     * variable is not found after exhausting all possibilities, throw
     * NoSuchVariable.<p>
     * <p/>
     * Note: This method takes the stack as a parameter so that it can be
     * used by a parser that is working through a list of identifiers that
     * represents the path to a variable <em>as well as</em> a shorthand
     * notation for the identifier that is the equivalent to the leaf node
     * name alone. In the form case the caller helps build the stack by
     * repeatedly calling <code>search</code>, in the latter case this method
     * must build the stack itself. This method is over kill for the first
     * case.
     *
     * @param name      Search for the named variable.
     * @param compStack The component stack. This holds the BaseType variables
     *                  that match the each component of a specific variable's name. This
     *                  method starts its search using the element at the top of the stack and
     *                  adds to the stack. The order of BaseType variables on the stack is the
     *                  reverse of the tree-traverse order. That is, the top most element on
     *                  the stack is the BaseType for the named variable, <em>under</em> that
     *                  is the named variable's parent and so on.
     * @return A stack of BaseType variables which hold the path from the top
     *         of the DDS to the named variable.
     * @throws NoSuchVariableException
     */
    public Stack search(String name, Stack compStack)
            throws NoSuchVariableException
    {
        DDSSearch ddsSearch = new DDSSearch(compStack);

        if (ddsSearch.deepSearch(name))
            return ddsSearch.components;
        else
            throw new NoSuchVariableException("The variable `" + name
                    + "' was not found in the dataset.");
    }

    /**
     * Find variables in the DDS when users name them with either fully- or
     * partially-qualified names.
     */
    private final class DDSSearch
    {
        Stack components;

        DDSSearch(Stack c)
        {
            components = c;
        }

        BaseType simpleSearch(String name, BaseType start)
        {
            Enumeration e = null;
            DConstructor dcv;
            if (start == null)
                e = getVariables(); // Start with the whole DDS
            else if (start instanceof DConstructor)
                e = ((DConstructor) start).getVariables();
            else if ((dcv = isVectorOfDConstructor(start)) != null)
                e = dcv.getVariables();
            else
                return null;

            // The name is DAP encoded, the variable name is not, but its bt is;
            // so compare directly
            while (e.hasMoreElements()) {
                BaseType bt = (BaseType) e.nextElement();
                if (bt.getEncodedName().equals(name)) {
                    return bt;
                }
            }

            return null;    // Not found
        }

        /**
         * Look for the variable named <code>name</code>. First perform the
         * shallow search (see simpleSearch) and then look through all the
         * ctor variables. If there are no more ctors to check and the
         * variable has not been found, return false.
         * <p/>
         * Note that this method uses the return value to indicate whether a
         * particular invocation found <code>name</code>.
         */
        boolean deepSearch(String name) throws NoSuchVariableException
        {

            BaseType start = components.empty() ? null
                    : (BaseType) components.peek();

            BaseType found;

            if ((found = simpleSearch(name, start)) != null) {
                components.push(found);
                return true;
            }

            Enumeration e;
            DConstructor dcv;
            if (start == null)
                e = getVariables(); // Start with the whole DDS
            else if (start instanceof DConstructor)
                e = ((DConstructor) start).getVariables();
            else if ((dcv = isVectorOfDConstructor(start)) != null)
                e = dcv.getVariables();
            else
                return false;

            while (e.hasMoreElements()) {
                BaseType v = (BaseType) e.nextElement();
                components.push(v);
                if (deepSearch(name))
                    return true;
                else
                    components.pop();
            }

            // This second return takes care of the case where a dataset
            // lists a bunch of ctor variable, one after another. Once the
            // first ctor (say a Grid) has been searched returning false to
            // the superior invocation of deepSearch pops it off the stack
            // and the while loop will search starting with the next variable
            // in the DDS.
            return false;
        }
    }

    /**
     * Returns an <code>Enumeration</code> of the dataset variables.
     *
     * @return an <code>Enumeration</code> of <code>BaseType</code>.
     */
    public final Enumeration getVariables()
    {
        return vars.elements();
    }

    /**
     * Returns the number of variables in the dataset.
     *
     * @return the number of variables in the dataset.
     */
    public final int numVariables()
    {
        return vars.size();
    }

    /**
     * Reads a <b>DDX</b> from the named <code>InputStream</code>. This
     * method calls a generated parser to interpret an XML representation of a
     * <code>DDS</code> (aka a <b>DDX</b>), and instantiate that
     * <code>DDS</code> in memory. This method does the following:
     * <ul>
     * <li> Gets a new <code>DDSXMLParser</code> using the <code>BaseTypeFactory</code>
     * held in this (the <code>DDS</code>) class. </li>
     * <li> Uses the <code>DDSXMLParser</code> to parse the DDX waiting
     * in the <code>InputStream</code> <i>is</i>. </li>
     * <li> Calls <code>DDS.checkForAttributeNameConflict()</code></li>
     * <li> Calls <code>DDS.resolveAliases()</code></li>
     * </ul>
     * <p/>
     * The last two items should be called EVERY time a <code>DDS</code>
     * is populated with variables ( by a parser, or through the <code>DDS</code> API)
     * and prior to releasing it for use to any calling program.
     *
     * @param is         the InputStream containing the <code>DDS</code> to parse.
     * @param validation Is a boolean indicating whether or not the parser should
     *                   validate the XML document using the Schema (typically referenced in the
     *                   document itself). In general server side applications should always vaidate,
     *                   while clients shouldn't bother (since they are ostensibly receiving the
     *                   document from a server that has already done so.)
     * @throws DDSException thrown on an error constructing the
     *                      <code>DDS</code>.
     * @opendap.ddx.experimental
     * @see opendap.dap.parsers.DDSXMLParser
     * @see #checkForAttributeNameConflict
     * @see #resolveAliases
     */
    public void parseXML(InputStream is, boolean validation) throws DAP2Exception
    {

        DDSXMLParser dp = new DDSXMLParser(opendapNameSpace);

        dp.parse(is, this, factory, validation);

        // Check for name conflicts. IN the XML representation
        // of the DDS it is syntactically possible for a
        // variable container (Dconstructor) to possess an
        // Attribute that has the same name as one of the container
        // variable's member variables. That's a NO-NO!.
        // Check for it here and throw a nice fat exception if we find it.

        checkForAttributeNameConflict();

        // Resolve the aliases. Aliases are basically analagous
        // to softlinks in a UNIX filesystem. Since an alias
        // can point any Attribute in the dataset, the vailidity
        // of the alias cannot be checked until all of the
        // members of the Dataset (both variables and their
        // Attributes) have been built. Once that is done
        // we can check to make sure that every alias points
        // at a vaild Attribute (and not another alias, non-existent
        // Attribute, etc)

        resolveAliases();


    }


    /**
     * Reads a <b>DDX</b> from the named <code>Document</code>. This
     * method calls a generated parser to interpret an XML representation of a
     * <code>DDS</code> (aka a <b>DDX</b>), and instantiate that
     * <code>DDS</code> in memory. This method does the following:
     * <ul>
     * <li> Gets a new <code>DDSXMLParser</code> using the <code>BaseTypeFactory</code>
     * held in this (the <code>DDS</code>) class. </li>
     * <li> Uses the <code>DDSXMLParser</code> to parse the DDX waiting
     * in the <code>InputStream</code> <i>is</i>. </li>
     * <li> Calls <code>DDS.checkForAttributeNameConflict()</code></li>
     * <li> Calls <code>DDS.resolveAliases()</code></li>
     * </ul>
     * <p/>
     * <p/>
     * The last two items should be called EVERY time a <code>DDS</code>
     * is populated with variables ( by a parser, or through the <code>DDS</code> API)
     * and prior to releasing it for use to any calling program.
     *
     * @param ddxDoc     A JDOM Document containing the <code>DDS</code> to parse.
     * @param validation Is a boolean indicating whether or not the parser should
     *                   validate the XML document using the Schema (typically referenced in the
     *                   document itself). In general server side applications should always vaidate,
     *                   while clients shouldn't bother (since they are ostensibly receiving the
     *                   document from a server that has already done so.)
     * @throws DDSException thrown on an error constructing the
     *                      <code>DDS</code>.
     * @opendap.ddx.experimental
     * @see opendap.dap.parsers.DDSXMLParser
     * @see #checkForAttributeNameConflict
     * @see #resolveAliases
     */
    public void parseXML(Document ddxDoc, boolean validation) throws DAP2Exception
    {

        DDSXMLParser dp = new DDSXMLParser(opendapNameSpace);

        dp.parse(ddxDoc, this, factory, validation);

        // Check for name conflicts. IN the XML representation
        // of the DDS it is syntactically possible for a
        // variable container (Dconstructor) to possess an
        // Attribute that has the same name as one of the container
        // variable's member variables. That's a NO-NO!.
        // Check for it here and throw a nice fat exception if we find it.

        checkForAttributeNameConflict();

        // Resolve the aliases. Aliases are basically analagous
        // to softlinks in a UNIX filesystem. Since an alias
        // can point any Attribute in the dataset, the vailidity
        // of the alias cannot be checked until all of the
        // members of the Dataset (both variables and their
        // Attributes) have been built. Once that is done
        // we can check to make sure that every alias points
        // at a vaild Attribute (and not another alias, non-existent
        // Attribute, etc)

        resolveAliases();


    }


    /**
     * Check the semantics of the <code>DDS</code>. If
     * <code>all</code> is true, check not only the semantics of the
     * <code>DDS</code> itself, but also recursively check all variables
     * in the dataset.
     *
     * @param all this flag indicates whether to check the semantics of the
     *            member variables, too.
     * @throws BadSemanticsException if semantics are bad
     */
    public void checkSemantics(boolean all)
            throws BadSemanticsException
    {
        if (getEncodedName() == null) {
            DAPNode.log.error("A dataset must have a name");
            throw new BadSemanticsException("DDS.checkSemantics(): A dataset must have a name");
        }
        Util.uniqueNames(vars, getEncodedName(), "Dataset");

        if (all) {
            for (Enumeration e = vars.elements(); e.hasMoreElements();) {
                BaseType bt = (BaseType) e.nextElement();
                bt.checkSemantics(true);
            }
        }
    }


    /**
     * Print the <code>DDS</code> on the given <code>PrintWriter</code>.
     *
     * @param os the <code>PrintWriter</code> to use for output.
     */
    public void print(PrintWriter os)
    {
        os.println("Dataset {");
        for (Enumeration e = vars.elements(); e.hasMoreElements();) {
            BaseType bt = (BaseType) e.nextElement();
            bt.printDecl(os);
        }
        os.print("} ");
        if (getEncodedName() != null)
            os.print(getEncodedName());
        os.println(";");
    }

    /**
     * Print the <code>DDS</code> on the given <code>OutputStream</code>.
     *
     * @param os the <code>OutputStream</code> to use for output.
     * @see DDS#print(PrintWriter)
     */
    public final void print(OutputStream os)
    {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os,Util.UTF8)));
        print(pw);
        pw.flush();
    }


    /**
     * Before the DDS can be used all of the Aliases in the various
     * AttributeTables must be resolved. This means that it is necessary to verify that
     * each Alias references an Attribute that exists, and is not another Alias. This
     * is accomplished by searching the DDS's variable's attribute holdings for Aliases
     * Everytime an Alias is located, a new search begins to find the Attribute that the
     * Alias is attemoting to reference.
     * <p/>
     * This method recursively searchs through the passed <code>BaseType</code> parameter
     * bt for Alias members of AttributeTables, and when they are found attempts to
     * resolve them to a specific Attribute.
     * <p/>
     * This method should be called ONLY after the entire
     * DDS has been parsed and /or built using the DDS API.
     * <h2>
     * This method should/must always be called prior to using a DDS!
     * </h2>
     * This method manipulates the global (private) variable <code>currentBT</code>.
     * This method manipulates the global (private) variable <code>currentAT</code>.
     */
    public void resolveAliases() throws MalformedAliasException, UnresolvedAliasException, NoSuchAttributeException
    {
        currentBT = null;
        currentAT = null;
        resolveAliases(this);
    }


    /**
     * Before the DDS can be used all of the Aliases in the various
     * AttributeTables must be resolved. This means that it is necessary to verify that
     * each Alias references an Attribute that exists, and is not another Alias. This
     * is accomplished by searching the DDS's variable's attribute holdings for Aliases
     * Everytime an Alias is located, a new search begins to find the Attribute that the
     * Alias is attempting to reference.
     * <p/>
     * This method recursively searchs through the passed <code>BaseType</code> parameter
     * bt for Alias members of AttributeTables, and when they are found attempts to
     * resolve them to a specific Attribute.
     * <p/>
     * This method gets called at the top level at the parser ONLY after the entire
     * DDS has been parsed and built. It's intial invocation get passed the DDS (which
     * is in fact a <code>BaseType</code>)
     * <p/>
     * <p/>
     * This method manipulates the global variable <code>currentBT</code>.
     *
     * @param bt The <code>BaseType</code> in which to search for and resolve Alias members
     */

    private void resolveAliases(BaseType bt) throws MalformedAliasException, UnresolvedAliasException, NoSuchAttributeException
    {

        // cache the current/parent BaseType (a container)
        BaseType cacheBT = currentBT;
        try {
        // Make the one we are about to search the current one.
        currentBT = bt;

        // Make the current AttributeTable null to indicate that we are at the top
        // AttributeTable of a new current BaseType.
        currentAT = null;


        if (Debug.isSet("DDS.resolveAliases"))
            DAPNode.log.debug("Searching for Aliases in the Attributes of Variable: " + bt.getEncodedName());

        // Process the Attributes of this BaseType.
        resolveAliases(bt.getAttributeTable());

        // Now if this current BaseType is a container type, then we better
        // search and resolve Aliases in it's children.
        if (bt instanceof DConstructor) {
            if (Debug.isSet("DDS.resolveAliases"))
                DAPNode.log.debug("Searching for Aliases in the children of Variable: " + bt.getEncodedName());

            Enumeration bte = ((DConstructor) bt).getVariables();

            while (bte.hasMoreElements()) {
                BaseType thisBT = (BaseType) bte.nextElement();

                // Recursive call...
                resolveAliases(thisBT);
            }
        }
        } finally {
        // Restore the previous current BaseType state.
        currentBT = cacheBT;
        }
    }


    /**
     * This method recursively searchs through the passed <code>AttributeTable</code> parameter
     * at for Alias members. When an Alias is found the method attempts to
     * resolve it to a specific Attribute.
     * <p/>
     * This method is invoked by <code>resolveAliases(BaseType bt)</code>, and is
     * used to search for Aliases in AttributeTables found in a BaseTypes Attributes.
     * <p/>
     * This method manipulates the global variable <code>currentBT</code>.
     *
     * @param at The <code>AttributeTable</code> in which to search for and resolve Alias members
     */


    private void resolveAliases(AttributeTable at) throws MalformedAliasException, UnresolvedAliasException, NoSuchAttributeException
    {

        // Cache the current (parent) Attribute table. This value is
        // null if this method is call from resolveAliases(BasetType bt)
        AttributeTable cacheAT = currentAT;
        try {
        // Set the current AttributeTable to the one that we are searching.
        currentAT = at;

        //getall of the Attributes from the table.
        Enumeration aNames = currentAT.getNames();
        while (aNames.hasMoreElements()) {

            String aName = (String) aNames.nextElement();
            opendap.dap.Attribute thisA = currentAT.getAttribute(aName);

            if (thisA.isAlias()) {
                //Is Alias? Resolve it!
                resolveAlias((Alias) thisA);
                if (Debug.isSet("DDS.resolveAliases")) {
		DAPNode.log.debug("Resolved Alias: '" + thisA.getEncodedName() + "'\n");
	    }
            } else if (thisA.isContainer()) {
                //Is AttributeTable (container)? Search it!
                resolveAliases(thisA.getContainer());
            }
        }
        } finally {
        // Restore the previous currentAT state.
        currentAT = cacheAT;
        }

    }


    /**
     * This method attempts to resolve the past Alias to a specific Attribute in the DDS.
     * It does this by:
     * <ul>
     * <li>1) Tokenizing the Alias's attribute field (see <code>Alias</code>) </li>
     * <li>2) Evaluating the tokenized field to locate the longest
     * possible variable name represented as a consecutive set of tokens </li>
     * <li>2) Evaluating the the remaining tokenized field to locate the Attribute
     * that this Alias is attempting to reference</li>
     * <li>4) Setting the Aliases internal references for it's Variable and it's Attribute.
     * </ul>
     * <p/>
     * If an Attribute matching the definition of the Alias cannot be located,
     * an Exception is thrown
     *
     * @param alias The <code>Alias</code> which needs to be resolved
     */

    private void resolveAlias(Alias alias) throws MalformedAliasException, UnresolvedAliasException
    {

        //Get the crucial stuff out of the Alias
        String name = alias.getEncodedName();
        String attribute = alias.getAliasedToAttributeFieldAsClearString();

        if (Debug.isSet("DDS.resolveAliases")) {
		DAPNode.log.debug("\n\nFound: Alias " + name +
                    "  " + attribute);
	    }

        // The Attribute field MAY NOT be empty.
        if (attribute.equals("")) {
            throw new MalformedAliasException("The attribute 'attribute' in the Alias " +
                    "element (name: '" + name + "') must have a value other than an empty string.");
        }


        if (Debug.isSet("DDS.resolveAliases")) {
		DAPNode.log.debug("Attribute: `" + attribute + "'");
	    }

        // Tokenize the attribute field.
        Vector aNames = tokenizeAliasField(attribute);

        if (Debug.isSet("DDS.resolveAliases")) {
            DAPNode.log.debug("Attribute name tokenized to " + aNames.size() + " elements");
            Enumeration e = aNames.elements();
            while (e.hasMoreElements()) {
                String aname = (String) e.nextElement();
                DAPNode.log.debug("name: " + aname);
            }
        }

        // The variable reference is the first part of the attribute field.
        // Let's go find it...


        BaseType targetBT = null;

        // Absolute paths for attributes names must start with the dot character.
        boolean isAbsolutePath = aNames.get(0).equals(".");

        if (!isAbsolutePath) { //Is it not an absolute path?
            throw new MalformedAliasException("In the Alias '" + name + "'" +
                    " the value of the attribute 'attribute' does not begin with the character dot (.). " +
                    "The value of the 'attribute' field must always be an absolute path name from the " +
                    "top level of the variable reference, and thus must always begin with the dot (.) character.");
        }

        if (aNames.size() == 1) { // Is it only a dot?
            throw new MalformedAliasException("In the Alias '" + name + "'" +
                    " the value of the attribute 'attribute' contains only the character dot (.). " +
                    "The value of the 'attribute' field must always reference an Attribute using an absolute path name from the " +
                    "top level of the DAS, and must reference an attribute within the DAS. A simple dot is not allowed.");

        }

        aNames.remove(0); // Remove the first token, which by now we know is a single dot.


        targetBT = getDeepestMatchingVariable(this, aNames);

        if (targetBT == null) { // No matching BaseType?

            // Then assume the attribute field references a
            // top (Dataset) level Attribute.
            targetBT = this;
        }

        //LogStream.out.println("Alias references variable:	."+targetBT.getLongName());

        // Now that we have found a target BaseType variable that matches the reference in
        // the variable field of the Alias (b.t.w. that's a good thing) let's go
        // see if we can find an Attribute within that targetBT that matches the attribute field
        // in the Alias decleration.

        Attribute targetAT = null;

        if (aNames.size() == 0) {
            // If there are no remaining tokens in the attribute field then
            // we are referencing the attribute container of the targetBT.
            targetAT = targetBT.getAttribute();

        } else {
            // Go try to find the Attribute in the targetBT.
            targetAT = getAttribute(targetBT.getAttributeTable(), aNames);
        }

        alias.setMyVariable(targetBT);
        alias.setMyAttribute(targetAT);


    }


    /**
     * This method executes a (recursive) search of the <code>AttributeTable</code>
     * parameter <b>at</b> for an <code>Attribute</code> whose name resolves to
     * the vector of names contained in the <code>Vector</code> parameter
     * <b>aNames</b>. An Attribute is considered a match if each of it's node
     * names in the hierarchy of AttributeTables contained in the
     * one passed as parameter <b>at</b> matches (equals) the corresponding name
     * in the Vector <b>aNames</b>.
     *
     * @param at     The <code>AttributeTable</code> to search
     * @param aNames The <code>Vector</code> of names to match to the nodes of <b>at</b>
     * @opendap.ddx.experimental
     */

    private opendap.dap.Attribute getAttribute(AttributeTable at, Vector aNames)
            throws MalformedAliasException, UnresolvedAliasException
    {

        // Get the first node name form the vector.
        String aName = (String) aNames.get(0);

        // Get the list of child nodes from the AttributeTable
        Enumeration e = at.getNames();
        while (e.hasMoreElements()) {

            // Get an Attribute
            String atName = (String) e.nextElement();
            opendap.dap.Attribute a = at.getAttribute(atName);

            // Get the Attributes name and Normalize it.
            String normName = normalize(a.getEncodedName());

            // Are they the same?
            if (normName.equals(aName)) {

                // Make sure this reference doesn't pass through an Alias.
                if (a.isAlias()) {
                    throw new MalformedAliasException("Aliases may NOT point to other aliases");
                }

                //dump the name from the list of names.
                aNames.remove(0);

                // Are there more?
                if (aNames.size() == 0) {
                    //No! We found it!
                    return (a);

                } else if (a.isContainer()) { // Is this Attribute a container (it better be)

                    try {
                        // Recursively search for the rest of the name vector in the container.
                        return (getAttribute(a.getContainer(), aNames));

                    } catch (NoSuchAttributeException nsae) {
                        throw new MalformedAliasException("Attribute " + a.getEncodedName() +
                                " is not an attribute container. (AttributeTable) " +
                                " It may not contain the attribute: " +
                                aName);
                    }

                } else { // Dead-end, through an exception!

                    throw new MalformedAliasException("Attribute " + a.getEncodedName() +
                            " is not an attribute container. (AttributeTable) " +
                            " It may not contain the attribute: " +
                            aName);
                }

            }
        }
        // Nothing Matched, so this search failed.
        throw new UnresolvedAliasException("The alias `" +
                "` references the attribute: `" + aName + "` which cannot be found.");


    }


    /**
     * This method executes a (recursive) search of the <code>DConstructor</code>
     * parameter <b>dcBT</b> for a <code>BaseType</code> variable whose name resolves to
     * the vector of names contained in the <code>Vector</code> parameter
     * <b>vNames</b>. A variable is considered a match if each of it's node
     * names in the hierarchy of containers in the
     * one passed as parameter <b>dcBT</b> matches (equals) the corresponding name
     * in the Vector <b>vNames</b>.
     *
     * @param dcBT   The <code>DConstructor</code> to search
     * @param vNames The <code>Vector</code> of names to match to the nodes of <b>at</b>
     */

    private BaseType getDeepestMatchingVariable(DConstructor dcBT, Vector vNames)
    {

        // Get the first name from the Vector
        String vName = (String) vNames.get(0);

        // Get all of the child variables from the Dconstructor
        Enumeration bte = dcBT.getVariables();
        while (bte.hasMoreElements()) {
            // Get this variable
            BaseType bt = (BaseType) bte.nextElement();

            // Get and normalize it's name.
            String normName = normalize(bt.getClearName());

            // Compare the names
            if (normName.equals(vName)) {

                // They match!

                // Remove the name from the vector.
                vNames.remove(0);


                if (vNames.size() == 0) { // are there more names?
                    // Nope! We Found it!
                    return bt;
                }

                if (bt instanceof DConstructor) {
                    // If there are more names then this thing better be a container
                    // recursively search it for the remaining names...
                    BaseType nextBT = getDeepestMatchingVariable((DConstructor) bt, vNames);

                    if (nextBT != null)
                        return (nextBT);

                    return (bt);
                }

                return (bt);
            }
        }
        return (null);
    }


    /**
     * The <code>normalize</code> method is used to normalize variable and
     * attribute name strings prior
     * to their comparison with the normalized tokens extracted from the
     * variable and name fields in an Alias declaration.
     * <p/>
     * The rule for this normalization is as follows:
     * <p/>
     * <ul>
     * <li> The &quot; (double quote) and the \ (backslash, aka escape)
     * characters MUST be escaped (using the \ character) in the <b>variable</b>
     * and <b>attribute</b> fields.</li>
     * </ul>
     *
     * @param field The string to be normalized.
     * @return The "normalized" string.
     */
    public static String normalize(String field)
    {
        boolean Debug = false;
        StringBuffer sb = new StringBuffer(field);

        for (int offset = 0; offset < sb.length(); offset++) {

            char c = sb.charAt(offset);

            // for every quote and slach in the string, add a slash in front of it.
            if (c == slash || c == quote) {
                sb.insert(offset, slash);
                offset++;
            }

        }
        //Coverity[DEADCODE]
        if (Debug) {
		DAPNode.log.debug("String: `" + field + "` normalized to: `" + sb + "`");
	    }

        return (sb.toString());

    }


    /**
     * The <code>tokenizeAliasFiled()</code> method is used to tokenize
     * the <b>variable</b> and the <b>attribute</b> fields in the alias
     * declaration. It is required that these fields be <b>normalized</b>
     * in the XML instance document. The rules for this normalization
     * are as follows:
     * <ul>
     * <p/>
     * <li> The &quot; (double quote) and the \ (backslash, aka escape)
     * characters MUST be escaped (using the \ character) in the <b>variable</b>
     * and <b>attribute</b> fields.</li>
     * <p/>
     * <li> The <b>variable</b> and <b>attribute</b> fields must be enclosed
     * in double quotes if their values contain the dot (.) character.</li>
     * <p/>
     * <li> Fully qualified <b>variable</b> and <b>attribute</b> names always
     * begin with the dot (.) character.</li>
     * </ul>
     *
     * @param field The string to be tokenized.
     * @return The tokenized string.
     * @throws MalformedAliasException
     */
    public static Vector tokenizeAliasField(String field) throws MalformedAliasException
    {

        boolean Debug = false;

        // find the index of the last element in the field.
        int lastIndex = field.length() - 1;

        // make a place to put the tokens.
        Vector tokens = new Vector();

        //Coverity[DEADCODE]
        if (Debug) {
		DAPNode.log.debug("lastIndexOf(dot): " + field.lastIndexOf(dot) + "   lastIndex: " + lastIndex);
	    }

        // Does this thing start with a quote?
        if (field.charAt(0) == quote) {
            // find the closing quote.
            // Because this token starts with a quote, it must be normalized
            // (see method description). The closing quote must exist,
            // and it cannont be escaped.

            // The first character in the token is the one following the
            // leadin quote.
            int start = 1;

            // prepare to search for a closing quote.
            int end = -1;
            boolean done = false;
            boolean escaped = false;

            // search for the quote

            for (int i = 1; i <= lastIndex || !done; i++) {
                char c = field.charAt(i);
                //LogStream.out.println("Checking for clear quote on char: "+c+" escaped="+escaped+"  done="+done);

                // Was this character escaped (with a slash)?
                if (escaped) {
                    // then ignore it and unset the escaped flag
                    // since the escape has been consumed.
                    escaped = false;
                } else {
                    // otherwise, is it an escape (slash) character
                    if (c == slash) {
                        // the set the escaoed flag to true.
                        escaped = true;
                    } else if (c == quote) {  // if it's not an escape (slash) then is it a quote?

                        //LogStream.out.println("Found quote!");

                        end = i;
                        done = true;
                    }

                }
            }

            //LogStream.out.println("start="+start+"  end="+end+"  lastIndex="+lastIndex);

            // if the end is less than 0 then it didn't get set
            // during the search for the quote, and thus the closing quote wasn't
            // found. Throw an exception!
            if (end < 0)
                throw new MalformedAliasException("Alias fields that begin with the quote (\") sign " +
                        "must have a closing quote.");

            // If there is more stuff, and that stuff is not seperated from the
            // closing quote by a dot character, then it's bad syntax.
            if (lastIndex > end && field.charAt(end + 1) != dot)
                throw new MalformedAliasException("Alias fields must be seperated by the dot (.) character.");

            // The last caharcter in the field may not be an (unquoted) dot.
            if (field.charAt(lastIndex) == dot)
                throw new MalformedAliasException("Alias fields may not end with the dot (.) character.");

            // Looks like we found a complete token.
            // Get it.
            String firstToken = field.substring(start, end);

            // Add it to the tokens Vector.
            tokens.add(firstToken);

            // if there is more stuff, then tokenize it.
            if (end < lastIndex) {

                // get the rest of the stuff
                String theRest = field.substring(end + 2);

                // tokenize it and add each of the returned tokens to
                // this tokens Vector.
                // Recursive call.
                Enumeration tkns = tokenizeAliasField(theRest).elements();
                while (tkns.hasMoreElements())
                    tokens.add(tkns.nextElement());
            }

            return (tokens);


        }

        // Find the first dot. This simplistic search is appropriate because
        // if this field contained a dot as part of it's name it should have
        // been encased in quotes and handled by the previous logic.

        int firstDot = field.indexOf(dot);

        if (firstDot == 0) { // Does this thing start with dot?

            // Then it must be an absolute path.
            // NOTE: This should be true ONLY for the first token
            // in the list. By that I mean that a leading dot in
            // the field string should only occur when the
            // variable or alias field begins a dot. A secondary
            // token may only start with a dot if the dot is
            // actually part of the field, and thus it should be
            // encased in quotes.
            String thisToken = ".";
            tokens.add(thisToken);
            // Check to see if there are more characters in the field to be tokenized.
            // If there are, tokenize them.
            if (lastIndex > 0) {
                String theRest = field.substring(1);
                // Recursive call
                Enumeration tkns = tokenizeAliasField(theRest).elements();

                // Take the tokens from the rest of the fields and
                // add them to this token vector.
                while (tkns.hasMoreElements())
                    tokens.add(tkns.nextElement());
            }
            return (tokens);
        }


        if (firstDot > 0) {
            // A secondary token may only contain a dot if the dot is
            // actually part of the field, and thus the field should have been
            // encased in quotes. Since we already check for a leading quote,
            // the first dor MUST be the end of the token.
            String firstToken = field.substring(0, firstDot);
            tokens.add(firstToken);

            // A quick syntax check.
            if (lastIndex == firstDot)
                throw new MalformedAliasException("Alias fields may not end with the dot (.) character.");

            // Get the rest of the field string
            String theRest = field.substring(firstDot + 1);

            // tokenize it, and add it's tokens to this token Vector.
            Enumeration tkns = tokenizeAliasField(theRest).elements();
            while (tkns.hasMoreElements())
                tokens.add(tkns.nextElement());

            return (tokens);

        }

        // This field string might be the final token, if we
        // get here it must be so add it to the tokens vector
        tokens.add(field);

        return (tokens);


    }

    /**
     * Prints the peristent representation of the <code>DDS</code> as an XML document.
     * This XML document is know as a <b>DDX</b>. The DDX can be parsed using the
     * <code>DDSXMLParser</code>
     *
     * @param pw The <code>PrintWriter</code> to print to.
     * @opendap.ddx.experimental
     */
    public void printXML(PrintWriter pw)
    {
        printXML(pw, "", false);
    }

    /**
     * Prints the peristent representation of the <code>DDS</code> as an XML document.
     * This XML document is know as a <b>DDX</b>. The DDX can be parsed using the
     * <code>DDSXMLParser</code>
     *
     * @param pw          The <code>PrintWriter</code> to print to.
     * @param pad         A <code>String</code> to print at the begining of each line.
     *                    typically this is some white space.
     * @param constrained A <code>boolean</code> that indcates if the this call should
     *                    print the constrained version of the DDS. A value of <i>true</i> will cause the
     *                    only the projected variables of the DDX to be printed .
     * @opendap.ddx.experimental
     */
    public void printXML(PrintWriter pw, String pad, boolean constrained)
    {

        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        pw.print("<Dataset");
        if(getEncodedName() != null)
            pw.print("name=\"" + DDSXMLParser.normalizeToXML(getEncodedName()) + "\"");
        pw.println();
        pw.println("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        pw.println("xmlns=\"" + opendapNameSpace + "\"");
        pw.print("xsi:schemaLocation=\"");
        pw.print(opendapNameSpace + "  ");
        pw.print(schemaLocation);
        pw.println("\" >");
        pw.println("");

        Enumeration e = getAttributeNames();
        while (e.hasMoreElements()) {
            String aName = (String) e.nextElement();


            Attribute a = getAttribute(aName);
            if (a != null)
                a.printXML(pw, pad + "\t", constrained);

        }

        pw.println("");

        Enumeration ve = getVariables();
        while (ve.hasMoreElements()) {
            BaseType bt = (BaseType) ve.nextElement();
            bt.printXML(pw, pad + "\t", constrained);
        }

        pw.println("");
        if (_dataBlobID != null) {
            pw.println(pad + "\t" + "<dataBLOB href=\"" +
                    DDSXMLParser.normalizeToXML(_dataBlobID) + "\"/>");
        }
        pw.println(pad + "</Dataset>");

    }


    /**
     * Takes the passed parameter <code>das</code> and attempts
     * to incorporate it's contents into the Attributes of the DDS
     * variables. If an <code>Attribute</code> in the <code>DAS</code>
     * can't be associated with a variable in a logical manner then
     * it is placed at the top level of the DDS. (Basically it becomes
     * a toplevel attribute in the dataset)
     *
     * @param das The <code>DAS</code> to ingest.
     * @opendap.ddx.experimental
     */
    public void ingestDAS(DAS das)
    {
        try {

            ingestAttributeTable(das, this);
            resolveAliases();

        } catch (DASException de) {

            DAPNode.log.error("DDS.ingestDAS(): " + de.getMessage());
        }

    }

    /**
     * A helper methods for ingestDAS().
     *
     * @param a
     * @param bt
     * @throws DASException
     * @see #ingestDAS(DAS)
     * @see #ingestAttribute(Attribute, BaseType)
     * @see #ingestAttributeTable(AttributeTable, BaseType)
     * @see #ingestAttributeTable(AttributeTable, DConstructor)
     */
    private void ingestAttribute(Attribute a, BaseType bt) throws DASException
    {


        if (a.isAlias()) { // copy an alias.
            String name = a.getEncodedName();
            String attribute = ((Alias) a).getAliasedToAttributeFieldAsClearString();

            bt.addAttributeAlias(name, attribute);

        } else if (a.isContainer()) {
            AttributeTable at = a.getContainer();
            ingestAttributeTable(at, bt);


        } else { // copy an Attribute and it's values...

            String name = a.getEncodedName();
            int type = a.getType();

            Enumeration vals = a.getValues();
            while (vals.hasMoreElements()) {
                String value = (String) vals.nextElement();
                bt.appendAttribute(name, type, value, true);
            }

        }

    }


    /**
     * A helper methods for ingestDAS().
     *
     * @param at
     * @param dc
     * @throws DASException
     * @see #ingestDAS(DAS)
     * @see #ingestAttribute(Attribute, BaseType)
     * @see #ingestAttributeTable(AttributeTable, BaseType)
     */
    private void ingestAttributeTable(AttributeTable at, DConstructor dc) throws DASException
    {


        Enumeration ate = at.getNames();

        while (ate.hasMoreElements()) {

            String aName = (String) ate.nextElement();
            Attribute a = at.getAttribute(aName);
            boolean foundIt = false;

            Enumeration bte = dc.getVariables();
            while (bte.hasMoreElements()) {

                BaseType thisBT = (BaseType) bte.nextElement();
                String bName = thisBT.getEncodedName();

                if (bName.equals(aName)) {

                    if (a.isContainer() && thisBT instanceof DConstructor) {

                        ingestAttributeTable(a.getContainer(), (DConstructor) thisBT);
                    } else {
                        ingestAttribute(a, thisBT);
                    }
                    foundIt = true;
                }
            }

            if (!foundIt) {
                ingestAttribute(a, dc);
            }
        }

    }

    /**
     * A helper methods for ingestDAS().
     *
     * @see #ingestDAS(DAS)
     * @see #ingestAttribute(Attribute, BaseType)
     * @see #ingestAttributeTable(AttributeTable, DConstructor)
     */
    private void ingestAttributeTable(AttributeTable at, BaseType bt) throws DASException
    {


        try {

            String atName = at.getEncodedName();
            String bName = bt.getEncodedName();
            //LogStream.out.println("ingestATTbl: atName:"+atName+" bName: "+bName);

            if (bName.equals(atName)) {

                //LogStream.out.println("adding each attribute!");
                Enumeration e = at.getNames();
                while (e.hasMoreElements()) {
                    String aName = (String) e.nextElement();
                    Attribute a = at.getAttribute(aName);

                    ingestAttribute(a, bt);
                }
            } else {
                //LogStream.out.println("addingcontainer!");
                bt.addAttributeContainer(at);
            }
        } catch (AttributeExistsException ase) {

            Enumeration e = at.getNames();
            while (e.hasMoreElements()) {
                String aName = (String) e.nextElement();
                Attribute a = at.getAttribute(aName);

                ingestAttribute(a, bt);
            }

        }

    }


    /**
     * Check for name conflicts. In the XML representation
     * of the DDS it is syntactically possible for a
     * variable container (Dconstructor) to possess an
     * Attribute that has the same name as one of the container
     * variable's member variables. That's a NO-NO!.
     * Check for it here and throw a nice fat exception if we find it.
     */
    public void checkForAttributeNameConflict() throws BadSemanticsException
    {
        checkForAttributeNameConflict(this);
    }


    /**
     * Check for name conflicts. In the XML representation
     * of the DDS it is syntactically possible for a
     * variable container (Dconstructor) to possess an
     * Attribute that has the same name as one of the container
     * variable's member variables. That's a NO-NO!.
     * Check for it here and throw a nice fat exception if we find it.
     *
     * @param dc The <code>DConstructor</code> to search for name conflicts.
     */
    private void checkForAttributeNameConflict(DConstructor dc) throws BadSemanticsException
    {

	if(_Debug) {
		DAPNode.log.debug("Checking "+dc.getTypeName()+" "+dc.getClearName()+" for name conflicts.");
	    }

        Enumeration bte = dc.getVariables();
        while (bte.hasMoreElements()) {
            BaseType bt = (BaseType) bte.nextElement();

//LogStream.out.println("     member: "+bt.getTypeName()+" "+bt.getName());
            Enumeration ate = dc.getAttributeNames();
            while (ate.hasMoreElements()) {

                String aName = (String) ate.nextElement();

//LogStream.out.println("         attribute: "+aName);
                if (aName.equals(bt.getEncodedName())) {
                    throw new BadSemanticsException("The variable '" + dc.getLongName() +
                            "' has an Attribute with the same name ('" + aName + "') as one of it's " +
                            "member variables (" + bt.getTypeName() + " " + bt.getEncodedName() + ")\n" +
                            "This is NOT allowed.");
                }
            }

            if (bt instanceof DConstructor) {
                // LogStream.out.println("     member '"+bt.getName()+"' is a container. Better Check it!");
                // Recursive call!!
                checkForAttributeNameConflict((DConstructor) bt);
            }

        }


    }


    /**
     * This a wrapper method for <code>DDS.print()</code>.
     *
     * @return The output of <code>DDS.print()</code> as
     *         <code>String</code>
     * @see #print(PrintWriter)
     */
    public String getDDSText()
    {
        StringWriter sw = new StringWriter();
        this.print(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * This a wrapper method for <code>DDS.printXML()</code>.
     *
     * @return The output of <code>DDS.printXML()</code> as
     *         <code>String</code>
     * @opendap.ddx.experimental
     * @see #printXML(PrintWriter)
     */
    public String getDDXText()
    {
        StringWriter sw = new StringWriter();
        this.printXML(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Returns a clone of this <code>DDS</code>.
     * See DAPNode.cloneDag()
     *
     * @param map track previously cloned nodes
     * @return a clone of this DDS.
     */
    public DAPNode cloneDAG(CloneMap map)
        throws CloneNotSupportedException
    {
            DDS d = (DDS) super.cloneDAG(map);
            d.vars = new Vector();
            for (int i = 0; i < vars.size(); i++) {
                BaseType element = (BaseType) vars.elementAt(i);
                d.vars.addElement(cloneDAG(map,element));
            }
            d.setEncodedName(this.getEncodedName());

            // Question:
            // What about copying the BaseTypeFactory?
            // Do we want a reference to the same one? Or another             // Is there a difference? Should we be building the clone
            // using "new DDS(getFactory())"??

            // Answer:
            // Yes. Use the same type factory!

            d.factory = this.factory;
            return d;
    }

}
