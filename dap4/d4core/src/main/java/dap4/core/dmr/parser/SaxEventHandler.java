/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

abstract public class SaxEventHandler extends DefaultHandler
{
    //////////////////////////////////////////////////
    // Constants

    static boolean TRACE = false;

    static Charset UTF8 = Charset.forName("UTF-8");

    static final String LOAD_EXTERNAL_DTD
            = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    //////////////////////////////////////////////////
    // static types

    //////////////////////////////////////////////////
    // static fields

    //////////////////////////////////////////////////
    // Instance variables

    protected String document = null;

    // Sax parser state
    protected Locator locator = null;
    protected SAXParserFactory spf = null;
    protected SAXParser saxparser = null;
    protected ByteArrayInputStream input = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public SaxEventHandler()
    {

    }

    //////////////////////////////////////////////////
    // Abstract method(s)

    // Send the lexeme to the the subclass to process
    abstract public void yyevent(SaxEvent token) throws SAXException;

    //////////////////////////////////////////////////
    // Get/Set
    public Locator
    getLocator()
    {
        return this.locator;
    }
    //////////////////////////////////////////////////
    // Public API

    public boolean parse(String document)
            throws SAXException
    {
        this.document = document;
        // Create the sax parser that will drive us with events
        try {
            spf = SAXParserFactory.newInstance();
            spf.setValidating(false);
            spf.setNamespaceAware(true);
            spf.setFeature(LOAD_EXTERNAL_DTD, false);
            saxparser = spf.newSAXParser();
            // Set up for the parse
            input = new ByteArrayInputStream(document.getBytes(UTF8));
            saxparser.parse(input, this); //'this' is link to subclass parser
            return true;
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    //////////////////////////////////////////////////
    // DefaultHandler Overrides

    // We feed only a subset of the possible events into
    // the subclass handler. This can be changed by
    // overriding the suppressing eventtype handlers below.

    @Override
    public void setDocumentLocator(Locator locator)
    {
        this.locator = locator;
    }

    @Override
    public void startDocument()
            throws SAXException
    {
        SaxEvent token = new SaxEvent(SaxEventType.STARTDOCUMENT, locator);
        if(TRACE) trace("eventtype.%s: %s%n", token.eventtype.name(), token.toString());
        locatedEvent(token);
    }

    @Override
    public void endDocument()
            throws SAXException
    {
        SaxEvent token = new SaxEvent(SaxEventType.ENDDOCUMENT, locator);
        if(TRACE) trace("eventtype.%s: %s%n", token.eventtype.name(), token.toString());
        locatedEvent(token);
    }

    @Override
    public void startElement(String nsuri, String name, String qualname,
                             Attributes attributes)
            throws SAXException
    {
        SaxEvent token = new SaxEvent(SaxEventType.STARTELEMENT, locator, name, qualname, nsuri);
        if(TRACE) trace("eventtype.%s: %s%n", token.eventtype.name(), token.toString());
        locatedEvent(token);
        // Now pass the attributes as tokens
        int nattr = attributes.getLength();
        for(int i = 0; i < nattr; i++) {
            String aname = attributes.getLocalName(i);
            if("".equals(aname)) aname = attributes.getQName(i);
            String value = attributes.getValue(i);
            token = new SaxEvent(SaxEventType.ATTRIBUTE, locator, aname);
            token.value = value;
            if(TRACE) trace("eventtype.%s: %s%n", token.eventtype.name(), token.toString());
            locatedEvent(token);
        }
    }

    @Override
    public void endElement(String nsuri, String name, String qualname)
            throws SAXException
    {
        SaxEvent token = new SaxEvent(SaxEventType.ENDELEMENT, locator, name, qualname, nsuri);
        if(TRACE) trace("eventtype.%s: %s%n", token.eventtype.name(), token.toString());
        locatedEvent(token);
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException
    {
        SaxEvent token = new SaxEvent(SaxEventType.CHARACTERS, locator);
        token.text = new String(ch, start, length);
        if(TRACE) trace("eventtype.%s: %s%n", token.eventtype.name(), token.toString());
        locatedEvent(token);
    }

    // Following events are suppressed

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException
    {
        // should never see this since not validating
        return;
    }

    @Override
    public void endPrefixMapping(String prefix)
            throws SAXException
    {
        return;
    }

    @Override
    public void notationDecl(String name, String publicId, String systemId)
            throws SAXException
    {
        return;
    }

    @Override
    public void processingInstruction(String target, String data)
            throws SAXException
    {
        return;
    }

    @Override
    public void skippedEntity(String name)
            throws SAXException
    {
        return;
    }

    @Override
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException
    {
        return;
    }

    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName)
            throws SAXException
    {
        return;
    }

    //////////////////////////////////////////////////
    // Entity resolution (Ignored)

    @Override
    public InputSource resolveEntity(String publicId, String systemId)
    {
        if(TRACE) trace("eventtype.RESOLVEENTITY: %s.%s%n", publicId, systemId);
        return null;
    }

    //////////////////////////////////////////////////
    // Error handling Events

    @Override
    public void fatalError(SAXParseException e)
            throws SAXException
    {
        throw new SAXParseException(
                String.format("Sax fatal error: %s; %s%n", e, report(this.locator)),
                this.locator);
    }

    @Override
    public void error(SAXParseException e)
            throws SAXException
    {
        System.err.printf("Sax error: %s; %s%n", e, report(this.locator));
    }

    @Override
    public void warning(SAXParseException e)
            throws SAXException
    {
        System.err.printf("Sax warning: %s; %s%n", e, report(this.locator));
    }

    protected String
    report(Locator locator)
    {
        int lineno = locator.getLineNumber();
        int colno = locator.getColumnNumber();
        String text = this.document;
        String[] lines = text.split("[\n]");
        for(int i = lines.length; i <= lineno+1; i++) {
            //Coverity[FB.SBSC_USE_STRINGBUFFER_CONCATENATION]
            text = text + " \n";
        }
        lines = text.split("[\n]");
        String msg;
        try {
            msg = lines[lineno];
            while(msg.length() <= colno) {
                msg = msg + ' ';
            }
            msg = msg.substring(0, colno) + '^' + msg.substring(colno, msg.length());
            msg = locator.toString() + '|' + msg + '|';
        } catch (ArrayIndexOutOfBoundsException t) {
            msg = locator.toString();
        }
        return msg;
    }


    //////////////////////////////////////////////////
    // Location printing

    protected void
    locatedEvent(SaxEvent token)
            throws SAXException
    {
        try {
            yyevent(token);
        } catch (SAXException se) {
            throw new SAXException(locatedError(se.getMessage()));
        }
    }


    protected String
    locatedError(String msg)
    {
        String locmsg = msg + String.format("; near %s%n", this.locator.toString());
        return locmsg;
    }


    protected void
    trace(String msg, Object... args)
    {
        if(TRACE) System.err.printf(locatedError(String.format(msg, args)));
    }

} // class SaxEventHandler
