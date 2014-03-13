/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser;

/**
 Provide an enumeration to mark all the possible Sax Parser
 generated events (except error events)
 */

/**
 * This should be essentially 1-1 for all the handler callbacks.
 * Exceptions: TBD
 */

public enum SaxEventType
{
    CHARACTERS,
    ENDDOCUMENT,
    ENDELEMENT,
    ENDPREFIXMAPPING,
    IGNORABLEWHITESPACE,
    NOTATIONDECL,
    PROCESSINGINSTRUCTION,
    SETDOCUMENTLOCATOR,
    SKIPPEDENTITY,
    STARTDOCUMENT,
    STARTELEMENT,
    STARTPREFIXMAPPING,
    UNPARSEDENTITYDECL,
    // Following is added to support each attribute as an eventtype
    ATTRIBUTE;
}; // enum SaxEventType
