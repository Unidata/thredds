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

package ucar.nc2.util.rc;

import ucar.nc2.util.log.LogStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.IOException;
import java.util.*;

public class RC
{


//////////////////////////////////////////////////
// Predefined flags

static public boolean useGroups = false;

//////////////////////////////////////////////////


static final String DFALTRCFILE = ".threddsrc";

static final char LTAG = '[';
static final char RTAG = ']';

static final String[] rcfilelist = new String[] {".dodsrc", ".tdsrc" };

static public int urlCompare(URL u1, URL u2)
{
    int relation;
    if(u1 == null && u2 == null) return 0;
    if(u1 == null) return -1;
    if(u2 == null) return +1;
    // 1. host test
    String host1 = (new StringBuilder(u1.getHost())).reverse().toString();
    String host2 = (new StringBuilder(u2.getHost())).reverse().toString();
    // Use lexical order on the reversed host names
    relation = host1.compareTo(host2);
    if(relation != 0) return relation;
    // 2. path test
    relation = (u1.getPath().compareTo(u2.getPath()));
    if(relation != 0) return relation;
    // 3. port number
    relation = (u1.getPort() - u2.getPort());
    if(relation != 0) return relation;
    // 4. note: all other fields are ignored
    return 0;
}

// Match has different semantics than urlCompare
static public boolean
urlMatch(URL pattern, URL url)
{
    int relation;

    if(pattern == null)
	return (url == null);

    if(!(url.getHost().endsWith(pattern.getHost())))
	return false; // e.g. pattern=x.y.org url=y.org

    if(!(url.getPath().startsWith(pattern.getPath())))
	return false; // e.g. pattern=y.org/a/b url=y.org/a

    if(pattern.getPort() > 0 && pattern.getPort() != url.getPort())
	return false;

    // note: all other fields are ignored

    return true;
}

static class Triple implements Comparable
{
    public String key; // also sort key
    public String value;
    public URL url;

    public boolean equals(Object o)
    {
	if(o == null || !(o instanceof Triple)) return false;
	return (compareTo((Triple)o) == 0);
    }

    public int compareTo(Object o)
    {
	if(o == null) throw new NullPointerException();
        Triple t = (Triple)o;
	int relation = key.compareTo(t.key);
	if(relation != 0) return relation;
	relation = urlCompare(this.url,t.url);
        return relation;
    }

    // toString produces an rc line
    public String toString()
    {
	StringBuilder line = new StringBuilder();
	if(url != null)	{
	    line.append("[");
	    line.append(url.toString());
	    line.append("]");
	}
	line.append(key);
	line.append("=");
	line.append(value);
	return line.toString();
    }
}

// Define a singlton RC instance for general global use
static RC dfaltRC = null;

static {
    RC.loadDefaults();
    RC.setWellKnown();
}

/**
 * Record some well known parameters
 */
static public void
setWellKnown()
{
   Triple triple = dfaltRC.lookup("usegroups");
   useGroups = booleanize(triple);
}

static boolean
booleanize(Triple triple)
{
    // canonical boolean values
    if(triple == null || "0".equals(triple.value) || "false".equalsIgnoreCase(triple.value))
        return false;
    if("1".equals(triple.value) || "true".equalsIgnoreCase(triple.value))
        return true;
    return triple.value != null; // any non-null value?

}

static public void
loadDefaults()
{
    RC rc0 = new RC();
    String[] locations = new String[] {
	System.getProperty("user.home"),
        System.getProperty("user.dir")
    } ;
    boolean found1 = false;
    for(String loc: locations) {
	if(loc == null) continue;
	String dir = loc.replace('\\','/');
	if(dir.endsWith("/")) dir = dir.substring(0,dir.length()-1);
        for(String rcpath: rcfilelist) {
	    String filepath = loc + "/" + rcpath;
	    if(rc0.load(filepath)) found1 = true;
	}
    }
    if(!found1)
        LogStream.getLog().warn("CDM: No .rc file found");
    dfaltRC = rc0;
}

static RC getDefault()
{
    return dfaltRC;
}

//////////////////////////////////////////////////
// Instance Data

Map<String,List<Triple>> triplestore;

//////////////////////////////////////////////////
// constructors

public RC()
{
    triplestore = new HashMap<String,List<Triple>>();
}

//////////////////////////////////////////////////
// Loaders

// Load this triple store from an rc file 
// overwrite existing entries

public boolean
load(String abspath)
{
    abspath = abspath.replace('\\','/');
    File rcFile = new File(abspath) ;
    if(!rcFile.exists() || !rcFile.canRead()) {
	return false;
    }
    LogStream.getLog().info("Loading rc file: "+abspath);
    FileReader frdr = null;
    try {
        frdr = new FileReader(rcFile);
    } catch (FileNotFoundException fe) {
        LogStream.getLog().info("Loading rc file: "+abspath);
        return false;
    }
    BufferedReader rdr = new BufferedReader(frdr);
    try {
        for(int lineno=1;;lineno++) {
            URL url = null;
            String line = rdr.readLine();
            if(line == null) break;
            // trim leading blanks
            line = line.trim();
            if(line.length()==0) continue; // empty line
            if(line.charAt(0) == '#') continue; // check for comment
            // parse the line
            if(line.charAt(0) == LTAG) {
                int rindex = line.indexOf(RTAG);
                if(rindex < 0) return false;
                LogStream.getLog().error("Malformed [url] at "+abspath+"."+lineno);
                String surl = line.substring(1,rindex);
                try {
                    url = new URL(surl);
                } catch (MalformedURLException mue) {
                    LogStream.getLog().error("Malformed [url] at "+abspath+"."+lineno);
                }
                line = line.substring(rindex+1);
                // trim again
                line = line.trim();
            }
            // Get the key,value part
            String[] pieces = line.split("\\s*=\\s*");
            assert(pieces.length == 1 || pieces.length == 2);
            // Create the triple
            Triple triple = new Triple();
            triple.url = url;
            triple.key = pieces[0].trim();
            if(pieces.length == 2) {
                triple.value = pieces[1].trim();
            } else {
                triple.value = "1";
            }
            List<Triple> prev = triplestore.get(triple.key);
            triplestore.put(triple.key,addtriple(prev,triple));
        }
        rdr.close();
        frdr.close();
    } catch (IOException ioe) {
        LogStream.getLog().error("File "+abspath+": IO exception: "+ioe.getMessage());
        return false;
    }
    return true;
}

List<Triple>
addtriple(List<Triple> list, Triple triple)
{
    if(list == null) list = new ArrayList<Triple>();
    // Look for duplicates    
    int prev = list.indexOf(triple);
    if(prev >= 0) list.remove(prev);
    list.add(triple);
    Collections.sort(list);
    return list;
}

public Set<String> keySet() {return triplestore.keySet();}

public List<Triple> getTriples(String key)
{
    List<Triple> list = triplestore.get(key);
    if(list == null) list = new ArrayList<Triple>();
    return list;
}

public Triple lookup(String key) {return lookup(key,(URL)null);}

public Triple lookup(String key, String url)
{
    if(url == null || url.length() == 0)
	return lookup(key);
    try {
	URL u = new URL(url);
	return lookup(key,u);
    } catch (MalformedURLException m) {}
    return null;
}

public Triple lookup(String key, URL url)
{
    List<Triple> list = triplestore.get(key);
    if(list == null) return null;
    if(url == null) {
	if(list.size() == 0) return null;
	return list.get(0);
    } else for(Triple t: list) {
	if(urlMatch(t.url,url)) return t;
    }
    return null;
}

// Output in .rc form
public String
toString()
{
    StringBuilder rc = new StringBuilder();
    for(String key: keySet()) {
	List<Triple> list = getTriples(key);
	for(Triple triple: list) {	
	    String line = triple.toString();    
	    rc.append(line);
	    rc.append("\n");
	}
    }
    return rc.toString();
}

} // class RC

