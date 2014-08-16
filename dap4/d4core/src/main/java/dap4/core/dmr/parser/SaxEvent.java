/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser;

import org.xml.sax.Locator;

/**
 * Define a class to hold information provided to each kind of eventtype
 */
public class SaxEvent
{
    public SaxEventType eventtype = null;
    public String name = null;
    public String fullname = null;
    public String namespace = null;

    public String value = null; // for attributes
    public String text = null;  // for text

    //public String publicid = null;
    //public String systemid = null;

    //Coverity[FB.URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD]
    public Locator locator = null;

    public SaxEvent()
    {
    }

    public SaxEvent(SaxEventType eventtype, Locator locator)
    {
        this.eventtype = eventtype;
        this.locator = locator;
    }

    public SaxEvent(SaxEventType eventtype, Locator locator, String name)
    {
        this(eventtype, locator);
        this.name = name;
    }

    public SaxEvent(SaxEventType eventtype, Locator locator,
                    String name, String fullname, String uri)
    {
        this(eventtype, locator, name);
        this.fullname = fullname;
        this.namespace = uri;
    }

    public String toString()
    {
        StringBuilder text = new StringBuilder();
        if(eventtype == null)
            text.append("notype");
        else
            text.append(eventtype.toString());
        text.append(" ");
        if(fullname != null)
            text.append(fullname);
        else if(name != null)
            text.append(name);
        else text.append("noname");
        if(value != null) {
            text.append(" = ");
            text.append(value);
        }
        if(this.text != null) {
            text.append(" = ");
            text.append(this.text);
        }
        return text.toString();
    }

} // class SaxEvent

