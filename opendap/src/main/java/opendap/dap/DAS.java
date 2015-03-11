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

import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.parsers.*;
import opendap.util.Debug;

import java.io.*;

/**
 * The Data Attribute Structure is a set of name-value pairs used to
 * describe the data in a particular dataset.  The name-value pairs
 * are called the "attributes."  The values may be of any of the
 * OPeNDAP simple data types (DByte, DInt32, DUInt32, DFloat64, DString and
 * DURL), and may be scalar or vector.  (Note that all values are
 * actually stored as string data.)
 * <p/>
 * A value may also consist of a set of other name-value pairs.  This
 * makes it possible to nest collections of attributes, giving rise
 * to a hierarchy of attributes.  OPeNDAP uses this structure to provide
 * information about variables in a dataset.
 * <p/>
 * In the following example of a DAS, several of the attribute
 * collections have names corresponding to the names of variables in
 * a hypothetical dataset.  The attributes in that collection are said to
 * belong to that variable.  For example, the <code>lat</code> variable has an
 * attribute <code>units</code> of <code>degrees_north</code>.
 * <p/>
 * <blockquote><pre>
 *  Attributes {
 *      GLOBAL {
 *          String title "Reynolds Optimum Interpolation (OI) SST";
 *      }
 *      lat {
 *          String units "degrees_north";
 *          String long_name "Latitude";
 *          Float64 actual_range 89.5, -89.5;
 *      }
 *      lon {
 *          String units "degrees_east";
 *          String long_name "Longitude";
 *          Float64 actual_range 0.5, 359.5;
 *      }
 *      time {
 *          String units "days since 1-1-1 00:00:00";
 *          String long_name "Time";
 *          Float64 actual_range 726468., 729289.;
 *          String delta_t "0000-00-07 00:00:00";
 *      }
 *      sst {
 *          String long_name "Weekly Means of Sea Surface Temperature";
 *          Float64 actual_range -1.8, 35.09;
 *          String units "degC";
 *          Float64 add_offset 0.;
 *          Float64 scale_factor 0.0099999998;
 *          Int32 missing_value 32767;
 *      }
 *  }
 * </pre></blockquote>
 * <p/>
 * Attributes may have arbitrary names, although in most datasets it
 * is important to choose these names so a reader will know what they
 * describe.  In the above example, the <code>GLOBAL</code> attribute provides
 * information about the entire dataset.
 * <p/>
 * Data attribute information is an important part of the the data
 * provided to a OPeNDAP client by a server, and the DAS is how this
 * data is packaged for sending (and how it is received).
 *
 * @author jehamby
 * @version $Revision: 22644 $
 * @see DDS
 * @see AttributeTable
 * @see Attribute
 */
public class DAS extends AttributeTable
{

    // Used by resolveAliases() method
    private AttributeTable currentAT = null;
    private Alias currentAlias = null;

    private BaseTypeFactory factory = null;
    /**
     * Create a new empty <code>DAS</code>.
     */
    public DAS()
    {
        super("Attributes");
        factory = new DefaultFactory();
    }

   public boolean parse(InputStream stream) throws ParseException, DAP2Exception
   {
        try {
            String text = DConnect2.captureStream(stream);
            //System.err.println("---------"+text+"\n----------------"); System.err.flush();
            return parse(text);
        } catch (IOException ioe) {
            throw new ParseException("Cannot read DAS",ioe);
        }
   }

    public boolean parse(String text) throws ParseException,DAP2Exception
    {
        Dap2Parser parser = new Dap2Parser(factory);
        int result = parser.dasparse(text,this);
        if(result == Dap2Parse.DapERR)
            throw parser.getERR();
        return (result == Dap2Parse.DapDAS ? true : false);
    }

    /**
     * Returns the <code>AttributeTable</code> with the given name.
     *
     * @param name the name of the <code>AttributeTable</code> to return.
     * @return the <code>AttributeTable</code> with the specified name, or null
     *         if there is no matching <code>AttributeTable</code>.
     * @throws NoSuchAttributeException There is no AttributeTable with the passed name.
     * @see AttributeTable
     */
    public final AttributeTable getAttributeTable(String name) throws NoSuchAttributeException
    {
        AttributeTable at = null;
        Attribute a = getAttribute(name);
        if (a != null) {
            if (a.isContainer()) {
                at = a.getContainer();
            }
        }
        return (at);
    }

    /**
     * Returns the <code>AttributeTable</code> with the given name.
     *
     * @param name the name of the <code>AttributeTable</code> to return.
     * @return the <code>AttributeTable</code> with the specified name, or null
     *         if there is no matching <code>AttributeTable</code>.
     * @see AttributeTable
     */
    public final AttributeTable getAttributeTableN(String name)
    {
        AttributeTable at = null;
        Attribute a = getAttribute(name);
        if (a != null) {
            if (a.isContainer()) {
                at = a.getContainerN();
            }
        }
        return (at);
    }

    /**
     * Adds an <code>AttributeTable</code> to the DAS.
     *
     * @param name the name of the <code>AttributeTable</code> to add.
     * @param a    the <code>AttributeTable</code> to add.
     * @see AttributeTable
     */
    public void addAttributeTable(String name, AttributeTable a) throws AttributeExistsException
    {
        addContainer(name, a);
    }

    /**
     * This method searchs through the <code>DAS</code>
     * for Alias members. When an Alias is found the method attempts to
     * resolve it to a specific Attribute.
     * <p/>
     * This method is invoked by <code>parse(InputStream is)</code>, and is
     * used to search for Aliases in AttributeTables found in the DAS.
     * <p/>
     * If you are building a DAS from it's API it is important to call
     * this method prior to returning said DAS to an application. If
     * this call is not made, Aliases will not work correctly.
     *
     * @see Alias
     * @see DDS#resolveAliases()
     */
    public void resolveAliases() throws MalformedAliasException, UnresolvedAliasException, NoSuchAttributeException
    {

        resolveAliases(this);

        // Enforce the rule that Aliases at the highest level of the DAS
        // must point to a container (AttributeTable)
        Enumeration e = getNames();
        while (e.hasMoreElements()) {
            String aName = (String) e.nextElement();

            if (Debug.isSet("DAS")) {
		DAPNode.log.debug("DAS.resolveAliases() - aName: " + aName);
	    }
            Attribute at = getAttribute(aName);
            if (at == null || !at.isContainer()) {

                throw new MalformedAliasException("Aliases at the top-level of a DAS MUST reference a container (AttributeTable), not a simple Attribute");
            }
        }


    }

    /**
     * This method recursively searchs through the passed <code>AttributeTable</code> parameter
     * at for Alias members. When an Alias is found the method attempts to
     * resolve it to a specific Attribute.
     * <p/>
     * This method gets called is invoked by <code>reolveAliases(BaseType bt)</code>, and is
     * used to search for Aliases in AttributeTables found in a BaseTypes Attributes.
     * <p/>
     * This method manipulates the global variable <code>currentBT</code>.
     *
     * @param at The <code>AttributeTable</code> in which to search for and resolve Alias members
     */


    private void resolveAliases(AttributeTable at) throws MalformedAliasException, UnresolvedAliasException, NoSuchAttributeException
    {
        // Cache the current (parent) Attribute table. This value is
        // null if this method is called from parse();
        AttributeTable cacheAT = currentAT;
        try {

        // Set the current AttributeTable to the one that we are searching.
        currentAT = at;

        if (Debug.isSet("DAS")) {
		    DAPNode.log.debug("DAS.resolveAliases(at=" + at + ")");
	    }

        //getall of the Attributes from the table.
        Enumeration aNames = at.getNames();
        while (aNames.hasMoreElements()) {

            String aName = (String) aNames.nextElement();
            if (Debug.isSet("DAS")) {
		DAPNode.log.debug("DAS.resolveAliases(at=" + at + ") - aName: " + aName);
	    }

            opendap.dap.Attribute thisA = currentAT.getAttribute(aName);

            if (Debug.isSet("DAS")) {
		DAPNode.log.debug("thisA.getClass().getName(): " + thisA.getClass().getName());
	    }

            if (thisA.isAlias()) {
                //Is Alias? Resolve it!
                resolveAlias((Alias) thisA);
                if (Debug.isSet("DAS")) {
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
     * <li>1) Tokenizing the Alias's variable and attribute fields (see <code>Alias</code>) </li>
     * <li>2) Evaluating the tokenized fields to determine if the Alias is defined in terms
     * of a relative or absolute path </li>
     * <li>3) Searching the DAS or the currentAT (depending on results
     * of 2) for the target Attribute </li>
     * <li>4) Setting the Aliases intrnal reference to it's Attribute
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
        String name = alias.getClearName();
        String attribute = alias.getAliasedToAttributeFieldAsClearString();

        // Get ready!
        Enumeration e = null;
        currentAlias = alias;

        if (Debug.isSet("DAS")) {
		DAPNode.log.debug("\n\nFound: Alias " + name +
                    "  " + attribute);
	    }

        // Let's go
        // see if we can find an Attribute within that DAS that matches the attribute field
        // in the Alias declartion.

        // The Attribute field MAY NOT be empty.
        if (attribute.equals("")) {
            throw new MalformedAliasException("The attribute 'attribute' in the Alias " +
                    "element must have a value other than an empty string.");
        }


        if (Debug.isSet("DAS")) {
		DAPNode.log.debug("Attribute: `" + attribute + "'");
	    }

        // Tokenize the attribute field.

        Vector aNames = opendap.dap.DDS.tokenizeAliasField(attribute);

        if (Debug.isSet("DAS")) {
	    DAPNode.log.debug("Attribute name tokenized to " + aNames.size() + " elements");
            e = aNames.elements();
            while (e.hasMoreElements()) {
                String aname = (String) e.nextElement();
                DAPNode.log.debug("name: " + aname);
            }
        }


        opendap.dap.Attribute targetAT = null;

        // Absolute paths for attributes names must start with the dot character.
        boolean isAbsolutePath = aNames.get(0).equals(".");

        if (isAbsolutePath) { //Is it an absolute path?

            if (aNames.size() == 1) {
                throw new MalformedAliasException("Aliases must reference an Attribute. " +
                        "An attribute field of dot (.) references the entire " +
                        "DAS, which is not allowed.");
            } else {
                // Dump the dot from the vector of tokens and go try to find
                // the Attribute in the DAS.
                aNames.remove(0);

                targetAT = getAliasAttribute(this, aNames);
            }
        } else {
            throw new MalformedAliasException("In the Alias '" + name + "'" +
                    " the attribute 'attribute' does not begin with the character dot (.). " +
                    "The 'attribute' field must always be an absoulute path name from the " +
                    "top level of the dataset, and thus must always begin with the dot (.) character.");
        }

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
     * @param att    The <code>AttributeTable</code> to search
     * @param aNames The <code>Vector</code> of names to match to the nodes of <b>at</b>
     */

    private opendap.dap.Attribute getAliasAttribute(AttributeTable att, Vector aNames)
            throws MalformedAliasException, UnresolvedAliasException
    {

        // Get the first node name form the vector.
        String aName = (String) aNames.get(0);

        // Get the list of child nodes from the AttributeTable
        Enumeration e = att.getNames();
        while (e.hasMoreElements()) {

            // Get an Attribute
            String atName = (String) e.nextElement();
            opendap.dap.Attribute a = att.getAttribute(atName);

            // Get the Attributes name and Normalize it.
            String normName = opendap.dap.DDS.normalize(a.getEncodedName());

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
                        return (getAliasAttribute(a.getContainer(), aNames));

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
        throw new UnresolvedAliasException("The alias `" + currentAlias.getEncodedName() +
                "` references the attribute: `" + aName + "` which cannot be found.");


    }

    /**
     * Returns a clone of this <code>Attribute</code>.
     * See DAPNode.cloneDag()
     *
     * @param map track previously cloned nodes
     * @return a clone of this <code>Attribute</code>.
     */
    public DAPNode cloneDAG(CloneMap map)
        throws CloneNotSupportedException
    {
        DAS das = (DAS) super.cloneDAG(map);
        return das;
    }

}


