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

package ucar.httpservices;

import java.util.*;
import java.io.*;

import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;

import javax.crypto.*;
import javax.crypto.spec.*;

import static org.apache.http.auth.AuthScope.*;
import static ucar.httpservices.HTTPAuthScope.*;


/**
 * HTTPAuthStore maps (Principal X AuthScope) to
 * a credentials provider. It can be used as a singleton class
 * to store "default"/"global" authorizations. It can also be
 * instantiated in an HTTPSession object to provide per-session
 * authorization information.
 * In practice the map is stored as a pair: an authscope and a credentialsprovider.
 * <p/>
 * Access is provided in a thread safe manner.
 * It currently supports serial access, but can be extended
 * to support single writer / multiple reader semantics
 * using the procedures
 * {acquire,release}writeaccess
 * and
 * {acquire,release}readaccess
 * <p/>
 */

public class HTTPAuthStore implements Serializable
{
    //////////////////////////////////////////////////////////////////////////

    static public org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(HTTPSession.class);


    //////////////////////////////////////////////////
    // Constants

    static public final String DEFAULT_SCHEME = HTTPAuthPolicy.BASIC;

    static public final String ANY_PRINCIPAL = null;

    //////////////////////////////////////////////////
    // Type Decls
    //Note: this class has a natural ordering that is inconsistent with equals.
    static public class Entry implements Serializable, Comparable<Entry>
    {
        public String principal;
        public AuthScope scope;
        public CredentialsProvider provider;

        public Entry(String principal, AuthScope scope, CredentialsProvider provider)
        {
            this.principal = principal;
            this.scope = scope;
            this.provider = provider;
        }

        public String toString()
        {
            String p = (this.principal == ANY_PRINCIPAL ? "*" : this.principal);
            return String.format("%s@%s{%s}", p, scope.toString(), provider.toString());
        }

        private void writeObject(ObjectOutputStream oos)
                throws IOException
        {
            oos.writeObject(principal);
            HTTPAuthScope.serializeScope(scope, oos);
            if(provider instanceof Serializable)
                oos.writeObject(provider);
            else
                oos.writeObject(provider.getClass());
        }

        private void readObject(ObjectInputStream ois)
                throws IOException, ClassNotFoundException
        {
            try {
                this.principal = (String) ois.readObject();
                this.scope = HTTPAuthScope.deserializeScope(ois);
                Object o = ois.readObject();
                if(o instanceof Class)
                    this.provider = (CredentialsProvider) ((Class) o).newInstance();
                else
                    this.provider = (CredentialsProvider) o;
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        public int compareTo(Entry e2)
        {
            Entry e1 = this;
            // assert e1.scope equivalent e2.scope
            if(e1 == null || e2 == null
                    || e1.scope == null || e2.scope == null)
                throw new NullPointerException();
            String p1 = e1.principal;
            String p2 = e2.principal;
            if(p1 == ANY_PRINCIPAL || p2 == ANY_PRINCIPAL)
                return 0;
            return p1.compareTo(p2);
        }

        public boolean
        equals(Object o)
        {
            if(!(o instanceof Entry))
                return false;
            return (compareTo((Entry)o) == 0);
        }

        public int
        hashCode()
	{
	    return this.principal.hashCode();
        }

    }

    //////////////////////////////////////////////////
    // Equivalence interface

    /**
     * Equivalence algorithm:
     * if any field is ANY_XXX, then they are equivalent.
     * Scheme, port, host must all be identical else return false
     * If this.path is prefix of other.path
     * or other.path is prefix of this.path
     * or they are string equals, then return true
     * else return false.
     */
    static boolean equivalent(Entry e1, Entry e2)
    {
        if(e1 == null ^ e2 == null)
            return false;
        if(e1 == e2)
            return true;
        AuthScope a1 = e1.scope;
        AuthScope a2 = e2.scope;
        if(!HTTPAuthScope.identical(a1, a2))
            return false;
        if(e1.principal == ANY_PRINCIPAL || e2.principal == ANY_PRINCIPAL)
            return true;
        return (e1.principal.equals(e2.principal));
    }

    //////////////////////////////////////////////////
    // Class variables    

    static public boolean TESTING = false;

    static protected HTTPAuthStore DEFAULT;

    static {
	    DEFAULT = new HTTPAuthStore(true);
    }

    //COVERITY[GUARDED_BY_VIOLATION]
    static public synchronized HTTPAuthStore getDefault() {return DEFAULT;}

    //////////////////////////////////////////////////
    // Instance variables

    protected boolean isdefault;

    protected List<Entry> rows;

    //////////////////////////////////////////////////
    // Constructor(s)

    public HTTPAuthStore()
    {
        this(false);
    }

    public HTTPAuthStore(boolean isdefault)
    {
        this.isdefault = isdefault;
        this.rows = new ArrayList<Entry>();
        if(isdefault) {
 	    // For back compatibility, check key/trust store flags
            // and add appropriate entries
   	    HTTPSession.setGlobalKeyStore();
        }
    }

    //////////////////////////////////////////////////
    // API

    /**
     * @param scope
     * @param provider
     * @return old provider if entry already existed and was replaced, else null.
     */

    synchronized public CredentialsProvider
    insert(String principal, AuthScope scope, CredentialsProvider provider)
            throws HTTPException
    {
        return insert(new Entry(principal, scope, provider));
    }

    /**
     * @param entry
     * @return old provider if entry already existed and was replaced, else null.
     */
    synchronized public CredentialsProvider
    insert(Entry entry)
            throws HTTPException
    {
        Entry found = null;

        if(entry == null)
            throw new HTTPException("HTTPAuthStore.insert: invalid entry: " + entry);

        for(Entry e : rows) {
            if(equivalent(e, entry)) {
                found = e;
                break;
            }
        }
        // If the entry already exists, then overwrite it and return old
        CredentialsProvider old = null;
        if(found != null) {
            old = entry.provider;
            found.provider = entry.provider;
        } else {
            Entry newentry = new Entry(ANY_PRINCIPAL, entry.scope, entry.provider);
            rows.add(newentry);
        }
        return old;
    }

    /**
     * @param entry
     * @return old entry if entry existed and was removed
     */

    synchronized public Entry
    remove(Entry entry)
            throws HTTPException
    {
        Entry found = null;

        if(entry == null)
            throw new HTTPException("HTTPAuthStore.remove: invalid entry: " + entry);

        for(Entry e : rows) {
            if(equivalent(e, entry)) {
                found = e;
                break;
            }
        }
        if(found != null) rows.remove(found);
        return (found);
    }

    /**
     * Remove all auth store entries
     */
    synchronized public void
    clear()
    {
        rows.clear();
    }

    /**
     * Return all entries in the auth store
     */
    synchronized public List<Entry>
    getAllRows()
    {
        return this.rows;
    }

    /**
     * Search for all equivalent rows, then sort on the path.
     *
     * @param scope
     * @return list of matching entries
     */
    synchronized public List<Entry>
    search(String principal, AuthScope scope)
    {
        List<Entry> matches;
        if(isdefault || DEFAULT == null)
            matches = new ArrayList<Entry>();
        else
            matches = DEFAULT.search(principal, scope);

        if(scope == null || rows.size() == 0)
            return matches;

        for(Entry e : getAllRows()) {
            if(principal != ANY_PRINCIPAL && e.principal.equals(principal))
                continue;
            if(HTTPAuthScope.equivalent(scope, e.scope))
                matches.add(e);
        }

        Collections.sort(matches);
        return matches;
    }

    //////////////////////////////////////////////////
    /**
     * n-readers/1-writer semantics
     * Note not synchronized because only called
     * by other synchronized procedures
     */

    static int nwriters = 0;
    static int nreaders = 0;
    static boolean stop = false;

    private void
    acquirewriteaccess() throws HTTPException
    {
        nwriters++;
        while(nwriters > 1) {
            try {
                wait();
            } catch (InterruptedException e) {
                if(stop) throw new HTTPException("interrupted");
            }
        }
        while(nreaders > 0) {
            try {
                wait();
            } catch (InterruptedException e) {
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
            try {
                wait();
            } catch (InterruptedException e) {
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

    public void
    print(PrintStream p)
            throws IOException
    {
        print(new PrintWriter(new OutputStreamWriter(p, Escape.utf8Charset), true));
    }

    public void
    print(PrintWriter p)
            throws IOException
    {
        List<Entry> elist = getAllRows();
        for(int i = 0; i < elist.size(); i++) {
            Entry e = elist.get(i);
            p.printf("[%02d] %s%n", i, e.toString());
        }
    }

    ///////////////////////////////////////////////////
    // Seriablizable interface
    // Encrypted (De-)Serialize

    synchronized public void
    serialize(OutputStream ostream, String password)
            throws HTTPException
    {
        try {

            // Create Key
            byte deskey[] = password.getBytes(Escape.utf8Charset);
            DESKeySpec desKeySpec = new DESKeySpec(deskey);
            //Coverity[RISKY_CRYPTO]
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

            // Create Cipher
            //Coverity[RISKY_CRYPTO]
            Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            desCipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Create crypto stream
            BufferedOutputStream bos = new BufferedOutputStream(ostream);
            CipherOutputStream cos = new CipherOutputStream(bos, desCipher);
            ObjectOutputStream oos = new ObjectOutputStream(cos);

            oos.writeInt(getAllRows().size());

            for(Entry e : getAllRows()) {
                oos.writeObject(e);
            }

            oos.flush();
            oos.close();

        } catch (Exception e) {
            throw new HTTPException(e);
        }

    }

    synchronized public void
    deserialize(InputStream istream, String password)
            throws HTTPException
    {
        ObjectInputStream ois = null;
        try {
            ois = openobjectstream(istream, password);
            List<Entry> entries = getDeserializedEntries(ois);
            for(Entry e : entries) {
                insert(e);
            }
        } finally {
            if(ois != null) try {
                ois.close();
            } catch (IOException e) {/*ignore*/}
        }
    }

    static public ObjectInputStream  // public to allow testing
    openobjectstream(InputStream istream, String password)
            throws HTTPException
    {
        try {
            // Create Key
            byte key[] = password.getBytes(Escape.utf8Charset);
            DESKeySpec desKeySpec = new DESKeySpec(key);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

            // Create Cipher
            //Coverity[RISKY_CRYPTO]
            Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            desCipher.init(Cipher.DECRYPT_MODE, secretKey);

            // Create crypto stream
            BufferedInputStream bis = new BufferedInputStream(istream);
            CipherInputStream cis = new CipherInputStream(bis, desCipher);
            ObjectInputStream ois = new ObjectInputStream(cis);
            return ois;
        } catch (Exception e) {
            throw new HTTPException(e);
        }
    }

    static public HTTPAuthStore    // public to allow testing
    getDeserializedStore(ObjectInputStream ois)
            throws HTTPException
    {
        List<Entry> entries = getDeserializedEntries(ois);
        HTTPAuthStore store = new HTTPAuthStore();
        store.rows = entries;
        return store;
    }

    static protected List<Entry>    // public to allow testing
    getDeserializedEntries(ObjectInputStream ois)
            throws HTTPException
    {
        try {
            List<Entry> entries = new ArrayList<Entry>();
            int count = ois.readInt();
            for(int i = 0; i < count; i++) {
                Entry e = (Entry) ois.readObject();
                entries.add(e);
            }
            return entries;
        } catch (Exception e) {
            throw new HTTPException(e);
        }
    }

}
    
