/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.dap4lib;

import dap4.core.util.DapException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accept: header processing
 */

public class Accept
{
    //////////////////////////////////////////////////
    // Type decls

    static protected class Range
    {
        public String type;
        public String subtype;
        public Map<String, String> params = new HashMap<>();

        public Range(String t, String st)
        {
            type = t;
            subtype = st;
        }
    }

    //////////////////////////////////////////////////
    // static variables

    //////////////////////////////////////////////////
    // Static methods

    //////////////////////////////////////////////////
    // Factory method

    static public ContentType
    parse(String header, List<ContentType> implemented)
            throws DapException
    {
        List<Range> ranges = new ArrayList<>();
        // The header argument is everything after "Accept:"
        header = header.trim();
        // Split along commas
        String[] alts = header.split("[ 	]*,[ 	]*");
        for(int i = 0; i < alts.length; i++) {
            String alt = alts[i].trim();
            // Split along semicolons
            String[] parms = alt.split("[ 	]*;[ 	]*");
            String type = parms[0].trim();
            // Split the leading type/subtype pair
            String[] pair = type.split("[/]");
            String maintype = null;
            String subtype = null;
            if(pair.length == 2) {
                maintype = pair[0].trim();
                subtype = pair[1].trim();
            } else if(pair.length == 1) {
                maintype = pair[0].trim();
                subtype = "*";
            } else
                throw new DapException("Malformed Accept: Header");
            if(subtype.length() == 0)
                subtype = "*";
            if(maintype.length() == 0)
                throw new DapException("Malformed Accept: Header");
            Range r = new Range(maintype, subtype);
            // parse any parameters
            for(int j = 1; j < parms.length; j++) {
                String parm = parms[1].trim();
                // Split at '='
                pair = parm.split("[ 	]*=[ 	]*");
                String name = null;
                String value = null;
                if(pair.length == 2) {
                    name = pair[0].trim();
                    value = pair[1].trim();
                } else if(pair.length == 1) {
                    name = pair[0].trim();
                    value = "";
                } else
                    throw new DapException("Malformed Accept: parameter");
                if(name.length() == 0)
                    throw new DapException("Malformed Accept: parameter");
                // Strip off any quotes
                if(value.startsWith("\""))
                    value = value.substring(1);
                if(value.endsWith("\""))
                    value = value.substring(0, value.length() - 1);
                r.params.put(name, value);
            } // parse params
            // save ranges in reverse order
            ranges.add(0, r);
        } // parse alternatives
        // Walk the alternatives in priority order looking for
        // an acceptable choice.
        ContentType result = null;
        for(int i = 0; i < ranges.size(); i++) {
            Range r = ranges.get(i);
            for(ContentType ct: implemented) {
                // Decompose the ct.mimetype
                String[] pieces = ct.getMimeType().split("[/]");
                String maintype = pieces[0];
                String subtype = (pieces.length == 1 ? "*" : pieces[1]);
                // Match?
                boolean match = maintype.equalsIgnoreCase(r.type);
                if(!subtype.equals("*"))
                    match &= subtype.equalsIgnoreCase(r.subtype);
                if(match) {
                    result = ct;
                    break;
                }
            }
        }
        return result;
    }

}
