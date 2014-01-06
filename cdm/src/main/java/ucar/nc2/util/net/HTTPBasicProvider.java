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


//////////////////////////////////////////////////
// Provide a non-interactive CredentialsProvider to hold
// a username + password that might have been taken from e.g.
// the user info part of a URL.

public class HTTPBasicProvider implements CredentialsProvider, Credentials, Serializable
{
    String username = null;
    String password = null;

    public HTTPBasicProvider(String username, String password)
    {
	this.username = username;
	this.password = password;
    }

    // Credentials Provider Interface
    public Credentials
    getCredentials(AuthScope scope) //AuthScheme authscheme, String host, int port, boolean isproxy)
    {
	    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
	    return creds;
    }

    public void
    setCredentials(AuthScope authscope, Credentials credentials)
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
	oos.writeObject(this.username);
	oos.writeObject(this.password);
    }

    private void readObject(java.io.ObjectInputStream ois)
	throws IOException, ClassNotFoundException
    {
	this.username = (String) ois.readObject();
	this.password = (String) ois.readObject();
    }
}
