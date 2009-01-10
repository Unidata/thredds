/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.controller;

import java.util.HashMap;
import java.util.Map;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

/**
 * Class that contains the parameters of the user's request.  Parameter names
 * are not case sensitive.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class RequestParams
{
    private Map<String, String> paramMap = new HashMap<String, String>();
    
    /**
     * Creates a new RequestParams object from the given Map of parameter names
     * and values (normally gained from HttpServletRequest.getParameterMap()).
     * The Map matches parameter names (Strings) to parameter values (String
     * arrays).
     */
    public RequestParams(Map httpRequestParamMap)
    {
        for (Object name : httpRequestParamMap.keySet())
        {
            String[] values = (String[])httpRequestParamMap.get(name);
            assert values.length >= 1;
            this.paramMap.put(unquotePlus(name.toString().trim()).toLowerCase(),
                unquotePlus(values[0].trim()));
        }
    }
    
    /**
     * Replaces URL escape sequences with their correct characters, and replaces
     * plus signs with spaces.  Nearly a direct port of urllib.unquote_plus() in
     * Python 2.3.
     * @todo doesn't handle "%%" correctly as an escape sequence for "%"
     */
    private static final String unquotePlus(String s)
    {
        s = s.replaceAll("\\+", " ");
        String[] items = s.split("%");
        StringBuffer buf = new StringBuffer(items[0]);
        for (int i = 1; i < items.length; i++)
        {
            System.out.println(items[i]);
            // The first two characters of each item will be a hex representation
            // of the ASCII code of the escaped character
            if (items[i].length() >= 2)
            {
                try
                {
                    int charNum = Integer.parseInt(items[i].substring(0, 2), 16);
                    buf.append((char)charNum);
                    buf.append(items[i].substring(2));
                }
                catch(NumberFormatException nfe)
                {
                    // It wasn't a valid hex code
                    buf.append("%" + items[i]);
                }
            }
            else
            {
                buf.append("%" + items[i]);
            }
        }
        return buf.toString();
    }
    
    /**
     * Returns the value of the parameter with the given name as a String, or null if the
     * parameter does not have a value.  This method is not sensitive to the case
     * of the parameter name.  Use getWmsVersion() to get the requested WMS version.
     */
    public String getString(String paramName)
    {
        return this.paramMap.get(paramName.toLowerCase());
    }
    
    /**
     * Returns the value of the parameter with the given name, throwing a
     * WmsException if the parameter does not exist.  Use getMandatoryWmsVersion()
     * to get the requested WMS version.
     */
    public String getMandatoryString(String paramName) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            throw new WmsException("Must provide a value for parameter "
                + paramName.toUpperCase());
        }
        return value;
    }
    
    /**
     * Finds the WMS version that the user has requested.  This looks for both
     * WMTVER and VERSION, the latter taking precedence.  WMTVER is used by
     * older versions of WMS and older clients may use this in version negotiation.
     * @return The request WMS version as a string, or null if not set
     */
    public String getWmsVersion()
    {
        String version = this.getString("version");
        if (version == null)
        {
            version = this.getString("wmtver");
        }
        return version; // might be null
    }
    
    /**
     * Finds the WMS version that the user has requested, throwing a WmsException
     * if a version has not been set.
     * @return The request WMS version as a string
     * @throws WmsException if neither VERSION nor WMTVER have been requested
     */
    public String getMandatoryWmsVersion() throws WmsException
    {
        String version = this.getWmsVersion();
        if (version == null)
        {
            throw new WmsException("Must provide a value for VERSION");
        }
        return version;
    }
    
    /**
     * Returns the value of the parameter with the given name as a positive integer,
     * or the provided default if no parameter with the given name has been supplied.
     * Throws a WmsException if the parameter does not exist or if the value
     * is not a valid positive integer.  Zero is counted as a positive integer.
     */
    public int getPositiveInt(String paramName, int defaultValue) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null) return defaultValue;
        return parsePositiveInt(paramName, value);
    }
    
    /**
     * Returns the value of the parameter with the given name as a positive integer,
     * throwing a WmsException if the parameter does not exist or if the value
     * is not a valid positive integer.  Zero is counted as a positive integer.
     */
    public int getMandatoryPositiveInt(String paramName) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            throw new WmsException("Must provide a value for parameter "
                + paramName.toUpperCase());
        }
        return parsePositiveInt(paramName, value);
    }
    
    private static int parsePositiveInt(String paramName, String value) throws WmsException
    {
        try
        {
            int i = Integer.parseInt(value);
            if (i < 0)
            {
                throw new WmsException("Parameter " + paramName.toUpperCase() +
                    " must be a valid positive integer");
            }
            return i;
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsException("Parameter " + paramName.toUpperCase() +
                " must be a valid positive integer");
        }
    }
    
    /**
     * Returns the value of the parameter with the given name, or the supplied
     * default value if the parameter does not exist.
     */
    public String getString(String paramName, String defaultValue)
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            return defaultValue;
        }
        return value;
    }
    
    /**
     * Returns the value of the parameter with the given name, or the supplied
     * default value if the parameter does not exist.
     * @throws WmsException if the value of the parameter is not a valid
     * floating-point number
     */
    public float getFloat(String paramName, float defaultValue) throws WmsException
    {
        String value = this.getString(paramName);
        if (value == null)
        {
            return defaultValue;
        }
        try
        {
            return Float.parseFloat(value);
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsException("Parameter " + paramName.toUpperCase() +
                " must be a valid floating-point number");
        }
    }
    
}
