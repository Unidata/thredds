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

import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * HTTPAuthStore maps (AuthSchemes X AuthScope) to
 * a credentials provider. It can be used as a singleton class
 * to store "default"/"global" authorizations. It can also be
 * instantiated in an HTTPSession object to provide per-session
 * authorization information.
 * In practice the map is stored as  Scheme x (Scope x Provider).
 * where the latter map is actuall a list.
 * <p/>
 * Access is provided in a thread safe manner.
 * It currently supports serial access.
 */

public class HTTPAuthStore implements Serializable
{
    //////////////////////////////////////////////////////////////////////////

    static public org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(HTTPSession.class);


    //////////////////////////////////////////////////
    // Constants

    //////////////////////////////////////////////////
    // Type Decls

    static public class Scope
    {
        public AuthScope scope;
        public CredentialsProvider provider;

        public Scope(AuthScope scope, CredentialsProvider provider)
        {
            this.scope = scope;
            this.provider = provider;
        }

        public String toString()
        {
            String p2 = (this.provider == null ? "<>" : provider.toString());
            return String.format("%s{%s}", scope.toString(), p2);
        }
    }

    //////////////////////////////////////////////////
    // Class variables

    static public boolean TESTING = false;

    //////////////////////////////////////////////////
    // Instance variables

    protected Map<String, List<Scope>> rows;

    protected HTTPAuthStore parent = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public HTTPAuthStore()
    {
        this.rows = new HashMap<String, List<Scope>>();
    }

    public HTTPAuthStore(HTTPAuthStore parent)
    {
	this();
        this.parent = parent;
    }

    //////////////////////////////////////////////////
    // Public API

    /**
     * @param scope
     * @param provider
     * @return old provider if entry already existed and was replaced, else null.
     */

    synchronized public CredentialsProvider
    insert(AuthScope scope, CredentialsProvider provider)
            throws HTTPException
    {
        if(scope == null || scope.getScheme() == null || provider == null)
            throw new HTTPException("HTTPAuthStore.insert: null argument");
        // Make realm from host+port
        String scheme = scope.getScheme();
        List<Scope> scopes = getScopes(scheme);
        CredentialsProvider old = null;
        for(Scope sc : scopes) {
            if(HTTPAuthUtil.equals(sc.scope,scope)) {
                old = sc.provider;
                sc.provider = provider;
                break;
            }
        }
        if(old == null)
            scopes.add(new Scope(scope, provider));
        return old;
    }

    /**
     * @param scope
     * @return old entry if entry existed and was removed
     */

    synchronized public CredentialsProvider
    remove(AuthScope scope)
            throws HTTPException
    {
        if(scope == null)
            throw new HTTPException("HTTPAuthStore.remove: null scope");
        String scheme = scope.getScheme();
        if(scheme == null)
            throw new HTTPException("HTTPAuthStore.remove: null scheme");
        List<Scope> scopes = getScopes(scheme);
        Scope old = null;
        for(Scope e : scopes) {
            if(e.scope.equals(scope)) {
                old = e;
                break;
            }
        }

        if(old != null)
            scopes.remove(old);
        return old.provider;
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
    synchronized public List<Scope>
    getAllRows()
    {
        List<Scope> allrows = new ArrayList<>();
        for(Map.Entry<String, List<Scope>> e : rows.entrySet()) {
            allrows.addAll((List<Scope>) e.getValue());
        }
        return allrows;
    }

    synchronized public CredentialsProvider
    lookup(AuthScope scope)
    {
        if(scope == null || scope.getScheme() == null)
            throw new IllegalArgumentException("null value");
        List<Scope> scopes = getScopes(scope.getScheme());
        for(Scope sc: scopes) {
            if(HTTPAuthUtil.equals(sc.scope,scope))
                return sc.provider;
        }
	// Try parent,if any
	if(parent == null)
            return null;
	return parent.lookup(scope);
    }

    protected List<Scope>
    getScopes(String scheme)
    {
        List<Scope> scopes = rows.get(scheme);
        if(scopes == null) {
            scopes = new ArrayList<Scope>();
            rows.put(scheme, scopes);
        }
        return scopes;
    }

}
