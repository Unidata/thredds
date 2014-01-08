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

import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;

/**
 * Provide an HTTP SSL CredentialsProvider
 * The getCredentials method is used in a
 * non-standard way
 */

public class HTTPSSLProvider implements CredentialsProvider, Credentials, Serializable
{
    String keystore = null;
    String keypass = null;
    String truststore = null;
    String trustpass = null;

    public HTTPSSLProvider()
    {
        this(null,"",null,"");
    }

    public HTTPSSLProvider(String keystore,String keypass,
                           String truststore,String trustpass)
    {
	this.keystore = keystore;
	this.keypass = keypass;
	this.truststore = truststore;
	this.trustpass = trustpass;
    }     

    public HTTPSSLProvider(String keystore, String keypass)
    {
	this(keystore,keypass,null,null);
    }     

    // Provide accessors
    public String getKeystore() {return keystore;}
    public String getKeypassword() {return keypass;}
    public String getTruststore() {return truststore;}
    public String getTrustpassword() {return trustpass;}

    // Credentials Provider Interface is abused

    public Credentials
    getCredentials(AuthScope scope) //AuthScheme authscheme,String host,int port,boolean isproxy)
    {
	    return (Credentials) this;
    }

    public void
    setCredentials(AuthScope scope, Credentials creds)
    {

    }

    public void
    clear()
    {

    }

    // Credentials Interface
    public Principal
    getUserPrincipal()
    {
        return null;
    }

    public String
    getPassword()
    {
       return null;
    }


    // Serializable Interface
    private void writeObject(java.io.ObjectOutputStream oos)
        throws IOException
    {
        oos.writeObject(this.keystore);
        oos.writeObject(this.keypass);
        oos.writeObject(this.truststore);
        oos.writeObject(this.trustpass);
    }

    private void readObject(java.io.ObjectInputStream ois)
            throws IOException, ClassNotFoundException
    {
        this.keystore = (String)ois.readObject();
        this.keypass = (String)ois.readObject();
        this.truststore = (String)ois.readObject();
        this.trustpass = (String)ois.readObject();
    }



}
