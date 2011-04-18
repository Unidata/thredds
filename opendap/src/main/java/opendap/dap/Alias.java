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
import java.io.PrintWriter;

import opendap.util.Debug;
import opendap.util.EscapeStrings;

/**
 * The Alias type is a special attribute. It is simply a reference
 * (like a "soft link" in a UNIX file system) to another attribute.
 * <p/>
 * The rules for the definiton and interpretation an Alias are as follows:
 * <p/>
 * In the persistent representation of the DDS (XML, or old DAS & DDS syntax)
 * Alias definitions will contain 2 fields: name, attribute. Example from a DDS:
 * <pre>
 * Alias CalDate  .profiler.cast.conductivity.calibration.date
 * </pre>
 * Or a DDX:
 * <pre>
 * &lt;Alias name=&quot;CalDate&quot; attribute=&quot;.profiler.cast.conductivity.calibration.date&quot;/&gt;
 * </pre>
 * <p/>
 * The rules for the interpretation of these fields are as follows:
 * <ul>
 * <li> Then Alias defintion begins with the word <code>Alias</code></li>
 * <p/>
 * <li> The name field is consumed in it's entirety (unparsed) and used
 * as the name of the Alias.
 * <p/>
 * <li> There a number of rules specific to the representation and interpretation
 * of the  attribute field in the Alias declaration.</li>
 * <ul>
 * <li> The &quot; (double quote) and the \ (backslash aka escape)
 * characters MUST be escaped (using \ character) in the value of the attribute field.</li>
 * <p/>
 * <li> Each variable and attribute name in the attribute field must be enclosed
 * in double quotes if their values contain the dot (.) character. For
 * example: .sometimes."names.contain".dots would be three names <i>sometimes</i>,
 * <i>names.contain</i>, and <i>dots</i>.</li>
 * <p/>
 * <li> Fully qualified attribute names always begin with
 * the dot (.) character.</li>
 * <p/>
 * <li> All attribute names always MUST be fully qualified (begin with a dot).
 * The leading dot represents a fully
 * qualifed path attribute reference, starting at top level of DDX, or the DAS.</li>
 * <p/>
 * <p/>
 * <li> If the attribute field contains only the dot (.) character, then it is
 * referencing the collection of attributes associated with the highest level
 * of the Dataset (global attributes) This collection always exists, but may be empty. </li>
 * <p/>
 * <li> The attribute field MUST NOT be empty.</li>
 * <li> The attribute field MUST NOT point to another Alias.</li>
 * </ul>
 * <p/>
 * <li> After the parser has completely parsed the DDX (or DAS) it will attempt to
 * reslove each alias to a specific attribute. If an alias cannot be resolved to
 * some attribute, and exception will be thrown by the parser.</li>
 * <p/>
 * </ul>
 * <p/>
 * <b>Warning:</b> <code>DAS</code> and <code>DDS</code> objects built using methods other than
 * <code>DDS.parse()</code>, <code>DDS.parseXML</code>,
 * <code>DDS.getDAS()</code>, or <code>DAS.parse()</code>
 * must call <code>DDS.resolveAliases()</code> or <code>DAS.resolveAliases()</code> prior to
 * allowing client software access to
 * these objects. Since an <code>Alias</code> essentially represents a "pointer" to some (other)
 * <code>Attribute</code>, that
 * <code>Attribute</code> object must be found. Once this has been done (by calling the correct
 * <code>resolveAliases()</code> method)
 * the <code>Aliases</code> will act transparently as references to their target <code>Attributes</code>.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see AttributeTable
 * @see DDS
 * @see DDS#parse
 * @see DDS#parseXML
 * @see DDS#getDAS
 * @see DAS
 * @see DAS#resolveAliases
 */
public class Alias extends Attribute
{
    /**
     * If <code>is_alias</code> is true, the name of the <code>Attribute</code>
     * we are aliased to.
     */
    private String aliasedToAttributeNamed;


    /**
     * The Attribute to which this Alias points.
     */
    private Attribute targetAttribute;

    /**
     * The BaseType variable that contains the Attribute
     * to which this Alias points.
     */
    private BaseType targetVariable;

    /**
     * Construct an Alias. This constructor is used by the DDSXMLParser to
     * and the DAS parser build Aliases for the DDX.
     *
     * @param aName         a <code>String</code> containing the name of the alias.
     * @param attributeName the <code>String</code> containing the normalized name
     *                      of the variable and attribute that this Alias references.
     */
    public Alias(String aName, String attributeName) {
        super(aName, ALIAS);
        aliasedToAttributeNamed = attributeName;
        targetAttribute = null;
        targetVariable = null;
    }

    public void setMyAttribute(Attribute a) {
        targetAttribute = a;
    }

    public void setMyVariable(BaseType v) {
        targetVariable = v;
    }

    private Attribute getMyAttribute() {

        return (targetAttribute);

    }

//    private BaseType getMyVariable() {
//        return (targetVariable);
//    }


    /**
     * Returns the attribute type constant.
     *
     * @return the attribute type constant.
     */
    public int getType() {

        return (getMyAttribute().getType());
    }


    /**
     * Returns true if the attribute is a container.
     *
     * @return true if the attribute is a container.
     */
    public boolean isContainer() {
        return (getMyAttribute().getType() == CONTAINER);
    }

    /**
     * Returns true if the attribute is an alias.
     *
     * @return true if the attribute is an alias.
     */
    public boolean isAlias() {
        return true;
    }

    /**
     * Returns the name of the attribute aliased to.
     *
     * @return the name of the attribute aliased to.
     */
    public String getAliasedTo() {
        return "";
    }


    /**
     * Returns the name of the attribute aliased to.
     *
     * @return the name of the attribute aliased to.
     */
    public String getAliasedToAttributeFieldAsClearString() {
        return aliasedToAttributeNamed;
    }

    /**
     * Returns the name of the attribute aliased to.
     *
     * @return the name of the attribute aliased to.
     */
    public String getAliasedToAttributeField() {
        return EscapeStrings.id2www(aliasedToAttributeNamed);
    }

    /**
     * Returns the <code>AttributeTable</code> container.
     *
     * @return the <code>AttributeTable</code> container.
     */
    public AttributeTable getContainer() throws NoSuchAttributeException {
        return (getMyAttribute().getContainer());
    }

    /**
     * Returns the values of this attribute as an <code>Enumeration</code>
     * of <code>String</code>.
     *
     * @return an <code>Enumeration</code> of <code>String</code>.
     */
    public Enumeration getValues() throws NoSuchAttributeException {
        return (getMyAttribute().getValues());
    }

    /**
     * Returns the attribute value at <code>index</code>.
     *
     * @param index the index of the attribute value to return.
     * @return the attribute <code>String</code> at <code>index</code>.
     */
    public String getValueAt(int index) throws NoSuchAttributeException {
        return (getMyAttribute().getValueAt(index));
    }

    /**
     * Append a value to this attribute. Always checks the validity of the
     * attribute's value.
     *
     * @param value the attribute <code>String</code> to add.
     * @throws AttributeBadValueException thrown if the value is not a legal
     *                                    member of type
     */
    public void appendValue(String value)
            throws AttributeBadValueException {

        throw new AttributeBadValueException("It is illegal to add values to an Alias. " +
                "Values can only be added to an Attribute");
    }

    /**
     * Append a value to this attribute.
     *
     * @param value the attribute <code>String</code> to add.
     * @param check if true, check the validity of he attribute's value, if
     *              false don't.
     * @throws AttributeBadValueException thrown if the value is not a legal
     *                                    member of type
     */
    public void appendValue(String value, boolean check)
            throws AttributeBadValueException {

        throw new AttributeBadValueException("It is illegal to add values to an Alias. " +
                "Values can only be added to an Attribute");
    }

    /**
     * Remove the <code>i</code>'th <code>String</code> from this attribute.
     *
     * @param index the index of the value to remove.
     */
    public void deleteValueAt(int index) throws AttributeBadValueException {
        throw new AttributeBadValueException("It is illegal to remove values from an Alias. " +
                "Values can only be removed from an Attribute");
    }


    public void print(PrintWriter os, String pad) {

        if (Debug.isSet("Alias")) System.out.println("  Attribute \"" + getClearName() + "\" is an Alias.");

        os.println(pad + "Alias " + getName() + " " + getAliasedToAttributeField() + ";");

    }


    public void printXML(PrintWriter pw, String pad, boolean constrained) {


        if (Debug.isSet("Alias")) pw.println("    Printing Alias \"" + getClearName() + "\"");

/*
	if (Debug.isSet("Alias")) pw.println("       constrained:    "+constrained);
	if (Debug.isSet("Alias")) pw.println("       targetVariable: '"+targetVariable.getName()+
                               "' (Projected: "+((ServerMethods) targetVariable).isProject()+")");
*/

        if (constrained &&
                targetVariable != null
                ) {


              if(!targetVariable.isProject())
                return;
        }


        pw.println(pad + "<Alias name=\"" +
                opendap.dap.XMLparser.DDSXMLParser.normalizeToXML(getClearName()) + "\" " +
                "Attribute=\"" +
                opendap.dap.XMLparser.DDSXMLParser.normalizeToXML(getAliasedToAttributeFieldAsClearString()) + "\"/>");

        if (Debug.isSet("Alias")) pw.println("Leaving Alias.print()");
        pw.flush();

    }

    /**
     * Returns a clone of this <code>Alias</code>.
     * See DAPNode.cloneDag()
     *
     * @param map track previously cloned nodes
     * @return a clone of this <code>Alias</code>.
     */

    public DAPNode cloneDAG(CloneMap map)
        throws CloneNotSupportedException
    {
        Alias a = (Alias) super.cloneDAG(map);
        a.aliasedToAttributeNamed = this.aliasedToAttributeNamed;
        a.targetAttribute = (Attribute)cloneDAG(map,this.targetAttribute);
        a.targetVariable = (BaseType)cloneDAG(map,this.targetVariable);
        return a;
    }


}

// $Log: Alias.java,v $
//
// Revision 1.1  2003/08/12 23:51:24  ndp
// Mass check in to begin Java-OPeNDAP development work
//
// Revision 1.7  2011/01/09 dmh
// change to cloneDAG
//
// Revision 1.6  2002/08/27 04:30:11  ndp
//
// Revision 1.5  2003/02/12 16:41:15  ndp
// *** empty log message ***
//
// Revision 1.4  2002/12/03 22:44:15  ndp
// *** empty log message ***
//
// Revision 1.3  2002/10/26 01:19:40  ndp
// *** empty log message ***
//
// Revision 1.2  2003/09/02 17:49:34  ndp
// *** empty log message ***
//
// Revision 1.1  2002/10/08 23:03:23  ndp
// *** empty log message ***
//


