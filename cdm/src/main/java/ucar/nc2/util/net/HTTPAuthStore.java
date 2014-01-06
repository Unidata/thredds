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

package ucar.nc2.util.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.io.*;

import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;

import javax.crypto.*;
import javax.crypto.spec.*;


/**

HTTPAuthStore stores tuples of authorization information in a thread
safe manner.  It currently supports serial access, but can be extended
to support single writer / multiple reader semantics
using the procedures
{acquire,release}writeaccess
and
{acquire,release}readaccess

 */

//Package local scope
class HTTPAuthStore implements Serializable
{


//////////////////////////////////////////////////////////////////////////
static public org.slf4j.Logger log
                = org.slf4j.LoggerFactory.getLogger(HTTPSession.class);
//////////////////////////////////////////////////////////////////////////


//////////////////////////////////////////////////
/**

The auth store is (conceptually) a set of tuples (rows) of the form
HTTPAuthScheme(scheme) X String(url) X CredentialsProvider(creds).
The creds column specifies the kind of authorization
(e.g. basic, keystore, etc) and the info to support it.
The functional relationship is (scheme,url)=>creds.
*/

static public class Entry implements Serializable, Comparable
{
    public HTTPAuthScheme scheme;
    public String url; // possibly including user info
    public CredentialsProvider creds;

    public Entry()
    {
	    this(ANY_ENTRY);
    }

    /**
     * @param entry
     */

    public Entry(Entry entry)
    {
	if(entry == null) entry = ANY_ENTRY;
	constructor(entry.scheme,entry.url,entry.creds);
    }

    /**
     * @param scheme
     * @param uri
     * @param creds
     */

    public Entry(HTTPAuthScheme scheme, String uri, CredentialsProvider creds)
    {
	constructor(scheme, uri, creds);
    }

    /**
     * Shared constructor code
     * @param scheme
     * @param uri
     * @param creds
     */

    protected void constructor(HTTPAuthScheme scheme, String uri, CredentialsProvider creds)
    {
	if(uri == null) uri = ANY_URL;
        if(scheme == null) scheme = DEFAULT_SCHEME;
	this.scheme = scheme;
	this.url = uri;
	this.creds = creds;
    }

    public boolean valid()
    {
	return (scheme != null && url != null);
    }

    public String toString()
    {
	String creds = (this.creds == null ? "null" : this.creds.toString());
        return String.format("%s:%s{%s}",scheme, url,creds);
    }

    private void writeObject(java.io.ObjectOutputStream oos)
        throws IOException
    {
        oos.writeObject(this.scheme);
        oos.writeObject(this.url);
        // serializing the credentials provider is a bit tricky
        // since it might not support the serializable interface.
        boolean isser = (this.creds instanceof Serializable);
        oos.writeObject(isser);
        if(isser)
            oos.writeObject(this.creds);
        else {
            oos.writeObject(this.creds.getClass());
        }
    }

    private void readObject(java.io.ObjectInputStream ois)
            throws IOException, ClassNotFoundException
    {
        this.scheme = (HTTPAuthScheme)ois.readObject();
        this.url = (String)ois.readObject();
        // serializing the credentials provider is a bit tricky
        // since it might not support the serializable interface.
        boolean isser = (Boolean)ois.readObject();
        Object o = ois.readObject();
        if(isser)
            this.creds = (CredentialsProvider)o;
        else {
            try {
                this.creds = (CredentialsProvider)((Class)o).newInstance();
            } catch (Exception e) {
                throw new ClassNotFoundException("Cannot create CredentialsProvider instance",e);
            }
        }
    }

    /**
      * return 0 if e1 == e2, 1 if e1 > e2
      * and -1 if e1 < e2 using the following tests
      * null, => 0
      * !null,null => -1
      * null,!null => +1
      * e1.scheme :: e2.scheme
      * compareURI(e1.url,e2.url) => e2.url.compare(e1.url) (note reverse order)
      *
      * Assume that the first argument comes from the pattern
      * and the second comes from the AuthStore
      */

    //////////////////////////////////////////////////
    // Comparable interface

    public int compareTo(Object o1)
    {
        if(!(o1 instanceof Entry)) return +1;
        Entry e1 = this;
        Entry e2 = (Entry)o1;
        if(e1 == null && e2 == null) return 0;
        if(e1 != null && e2 == null) return +1;
        if(e1 == null && e2 != null) return -1;
        int cmp = e1.scheme.compareTo(e2.scheme);
        if(cmp != 0) return cmp;
        if(compatibleURL(e1.url,e2.url))
            return e2.url.compareTo(e1.url);
        return e1.url.compareTo(e2.url);
    }

    //////////////////////////////////////////////////
    // Comparator interface

    public boolean equals(Entry e)
    {
        return this.compareTo(e) == 0;
    }

    //////////////////////////////////////////////////
    // Additional comparison functions

    // Used in search
    static protected boolean matches(Entry e1, Entry e2)
    {
	if(e1 == null && e2 == null) return true;
	if(e1 != null && e2 == null) return false;
	if(e1 == null && e2 != null) return false;

        if(e1.scheme != ANY_SCHEME && e2.scheme != ANY_SCHEME
           && e1.scheme != e2.scheme)
            return false;

	if(!compatibleURL(e1.url,e2.url))
            return false;
        
        return true;
    }

    // Uses in insert and remove
    static public boolean identical(Entry e1, Entry e2)
    {
        return (e1.scheme == e2.scheme &&
                (e1.url == e2.url // null test
                 || e1.url.equals(e2.url)));
    }


}

//////////////////////////////////////////////////

static public final boolean          SCHEME = true;
static public final String           ANY_URL = "";
static public final HTTPAuthScheme   ANY_SCHEME = HTTPAuthScheme.ANY;
static public final HTTPAuthScheme   DEFAULT_SCHEME = HTTPAuthScheme.BASIC;

static final public Entry            ANY_ENTRY = new Entry(ANY_SCHEME,ANY_URL,null);

static private List<Entry> rows;


static {
     rows = new ArrayList<Entry>();

    // For back compatibility, check some system properties
    // and add appropriate entries
    // 1. ESG keystore support
    String kpath = System.getProperty("keystore");
    if(kpath != null) {
        String tpath = System.getProperty("truststore");
        String kpwd = System.getProperty("keystorepassword");
        String tpwd = System.getProperty("truststorepassword");
        kpath = kpath.trim();
        if(tpath != null) tpath = tpath.trim();
        if(kpwd != null) kpwd = kpwd.trim();
        if(tpwd != null) tpwd = tpwd.trim();
        if(kpath.length() == 0) kpath = null;
        if(tpath.length() == 0) tpath = null;
        if(kpwd.length() == 0) kpwd = null;
        if(tpwd.length() == 0) tpwd = null;

        CredentialsProvider creds = new HTTPSSLProvider(kpath,kpwd,tpath,tpwd);
        try {
            insert(new Entry(HTTPAuthScheme.SSL,ANY_URL,creds));
        }   catch (HTTPException he) {
            log.error("HTTPAuthStore: could not insert default SSL data");
        }
     }

}

/**
 * Define URI compatibility.
 */
static protected boolean compatibleURL(String u1, String u2)
{   
    if(u1 == u2) return true;
    if(u1 == null) return false;
    if(u2 == null) return false;

    if(u1.equals(u2)
       || u1.startsWith(u2)
       || u2.startsWith(u1)) return true;

    // Check piece by piece
    URI uu1;
    URI uu2;
    try {
        uu1 = new URI(u1);
    } catch (URISyntaxException use) {
        return false;
    }
    try {
        uu2 = new URI(u2);
    } catch (URISyntaxException use) {
        return false;
    }

    // For the following we want this truth table
    // s1    s2    t/f
    // ---------------
    //  null  null  match
    //  null !null  !match
    // !null  null  !match
    // !null !null  match = s1.equals(s2)
    // The if statement condition is the negation of match, namely:
    // if((s1 != null || s2 != null)
    //    && s1 != null && s2 != null && !s1.equals(s2))
    //    return false; // => !match

    // protocols comparison
    String s1 = uu1.getScheme();
    String s2 = uu2.getScheme();
    if((s1 != null || s2 != null)
       && s1 != null && s2 != null && !s1.equals(s2))
        return false;

    // Match user info; differs from table above
    // because we allow added user info to match null
    //  null  null  match
    //  null !null  match <-- different
    // !null  null  !match
    // !null !null  match = s1.equals(s2)
    s1 = uu1.getUserInfo();
    s2 = uu2.getUserInfo();
    if(s1 != null
       && (s2 == null || !s1.equals(s2)))
        return false;

    // hosts must be same
    s1 = uu1.getHost();
    s2 = uu2.getHost();
    if((s1 != null || s2 != null)
       && s1 != null && s2 != null && !s1.equals(s2))
        return false;

    // ports must be the same
    if(uu1.getPort() != uu2.getPort())
        return false;

    // paths must have prefix relationship
    // and missing is a prefix of anything
    // s1    s2    t/f
    // ---------------
    //  null  null  match
    //  null !null  !match
    // !null  null  !match
    // !null !null  match = (s1.startsWith(s2)||s2.startsWith(s1))
    s1 = uu1.getRawPath();
    s2 = uu2.getRawPath();
    if((s1 != null || s2 != null)
       && s1 != null && s2 != null && !(s1.startsWith(s2) || s2.startsWith(s1)))
        return false;

    return true;
}

//////////////////////////////////////////////////
/**
Primary external interface
 */

/**
 * @param entry
 * @return true if entry already existed and was replaced
 */

static synchronized public boolean
insert(Entry entry)
    throws HTTPException
{
    boolean rval = false;
    Entry found = null;

    if(entry == null || !entry.valid())
	throw new HTTPException("HTTPAuthStore.insert: invalid entry: " + entry);

    for(Entry e: rows) {
        if(Entry.identical(e,entry)) {
	    found = e;
	    break;
	}
    }
    // If the entry already exists, then overwrite it and return true
    if(found != null) {
        found.creds = entry.creds;
	rval = true;
    } else {
        Entry newentry = new Entry(entry);
        rows.add(newentry);
    }
    return rval;
}

/**
 * @param entry
 * @return true if entry existed and was removed
 */

static synchronized public boolean
remove(Entry entry)
    throws HTTPException
{
    Entry found = null;

    if(entry == null || !entry.valid())
	    throw new HTTPException("HTTPAuthStore.remove: invalid entry: " + entry);

    for(Entry e: rows) {
	if(Entry.identical(e,entry)) {
	    found = e;
	    break;
	}
    }
    if(found != null) rows.remove(found);
    return (found != null);
}

/**
 * Remove all auth store entries
 */
static synchronized public void
clear() throws HTTPException
{
   rows.clear();
}

/**
 * Return all entries in the auth store
 */
static public List<Entry>
getAllRows()
{
  return rows;
}

/**
 * Search:
 * 
 * Search match is defined by the compatibleURI function above.
 * The return list is ordered from most restrictive to least restrictive.
 * 
 * @param entry
 * @return list of matching entries
 */
static synchronized public Entry[]
search(Entry entry)
{

    if(entry == null || !entry.valid() || rows.size() == 0)
        return new Entry[0];

    List<Entry> matches = new ArrayList<Entry>();

    for(Entry e: rows) {
        Entry e1 = e;
        if(Entry.matches(entry,e1)) {
            matches.add(e);
        }
    }
    // Sort by scheme then by url, where any_url is last
    Entry[] matchvec = matches.toArray(new Entry[matches.size()]);
    Arrays.sort(matchvec);
    return matchvec;
}

//////////////////////////////////////////////////
// Misc.


static public AuthScope
getAuthScope(Entry entry)
{
    if(entry == null) return null;
    URI uri;
    try {
	uri = new URI(entry.url);
    } catch(URISyntaxException use) {return null;}

    String host = uri.getHost();
    int port = uri.getPort();
    String realm = uri.getRawPath();
    String scheme = (entry.scheme == null ? null : entry.scheme.getSchemeName());

    if(host == null) host = AuthScope.ANY_HOST;
    if(port <= 0) port = AuthScope.ANY_PORT;
    if(realm == null) realm = AuthScope.ANY_REALM;
    AuthScope as = new AuthScope(host,port,realm);
    return as;
}


//////////////////////////////////////////////////
/**
n-readers/1-writer semantics
Note not synchronized because only called
by other synchronized procedures
 */

static int nwriters = 0;
static int nreaders = 0;
static boolean stop = false;

private void
acquirewriteaccess() throws HTTPException
{
    nwriters++;
    while(nwriters > 1) {
	try { wait(); } catch (InterruptedException e) {
	    if(stop) throw new HTTPException("interrupted");
        }
    }
    while(nreaders > 0) {
	try { wait(); } catch (InterruptedException e) {
	    if(stop) throw new HTTPException("interrupted");
        }
    }
}

private void
releasewriteaccess()
{
    nwriters--;
    notify();
}

private void
acquirereadaccess() throws HTTPException
{
    nreaders++;
    while(nwriters > 0) {
	try { wait(); } catch (InterruptedException e) {
	    if(stop) throw new HTTPException("interrupted");
        }
    }
}

private void
releasereadaccess()
{
    nreaders--;
    if(nreaders == 0) notify(); //only affects writers
}

///////////////////////////////////////////////////
// Print functions

static public void
print(PrintStream p)
	throws IOException
{
    print(new PrintWriter(p,true));
}

static public void
print(PrintWriter p)
	throws IOException
{
    List<Entry> elist = getAllRows();
    for(int i=0;i<elist.size();i++) {
	Entry e = elist.get(i);
	p.printf("[%02d] %s\n",e.toString());
    }
}

///////////////////////////////////////////////////
// Seriablizable interface
// Encrypted (De-)Serialize

static public void
serialize(OutputStream ostream, String password)
	throws HTTPException
{
    try {
        
    // Create Key
    byte key[] = password.getBytes();
    DESKeySpec desKeySpec = new DESKeySpec(key);
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
    SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

    // Create Cipher
    Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
    desCipher.init(Cipher.ENCRYPT_MODE, secretKey);

    // Create crypto stream
    BufferedOutputStream bos = new BufferedOutputStream(ostream);
    CipherOutputStream cos = new CipherOutputStream(bos, desCipher);
    ObjectOutputStream oos = new ObjectOutputStream(cos);

    oos.writeInt(getAllRows().size());

    for(Entry e: rows) {
        oos.writeObject(e);
    }

    oos.flush();
    oos.close();

    } catch (Exception e) {throw new HTTPException(e);}

}

static public void
deserialize(InputStream istream, String password)
    throws HTTPException
{
    List<Entry> entries = getDeserializedEntries(istream,password);
    for(Entry e: entries) {
        insert(e);
    }
}

static public List<Entry>
getDeserializedEntries(InputStream istream, String password)
	throws HTTPException
{
    try {

    // Create Key
    byte key[] = password.getBytes();
    DESKeySpec desKeySpec = new DESKeySpec(key);
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
    SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

    // Create Cipher
    Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
    desCipher.init(Cipher.DECRYPT_MODE, secretKey);

    // Create crypto stream
    BufferedInputStream bis = new BufferedInputStream(istream);
    CipherInputStream cis = new CipherInputStream(bis, desCipher);
    ObjectInputStream ois = new ObjectInputStream(cis);

    List<Entry> entries = new ArrayList<Entry>();
    int count = ois.readInt();
    for(int i=0;i<count;i++) {
        Entry e = (Entry)ois.readObject();
        entries.add(e);
    }
    return entries;
    } catch (Exception e) {throw new HTTPException(e);}
}



}
