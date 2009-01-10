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

package uk.ac.rdg.resc.ncwms.config;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * JavaBean holding contact details for the ncWMS configuration file
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="contact")
public class Contact
{
    
    @Element(name="name", required=false)
    private String name;
    @Element(name="organization", required=false)
    private String org; 
    @Element(name="telephone", required=false)
    private String tel;
    @Element(name="email", required=false)
    private String email;
    
    /** Creates a new instance of Contact */
    public Contact()
    {
        // Simple XML complains if tags are empty, hence we add a space
        this.name = " ";
        this.org = " ";
        this.tel = " ";
        this.email = " ";
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = Config.checkEmpty(name);
    }

    public String getOrg()
    {
        return org;
    }

    public void setOrg(String org)
    {
        this.org = Config.checkEmpty(org);
    }

    public String getTel()
    {
        return tel;
    }

    public void setTel(String tel)
    {
        this.tel = Config.checkEmpty(tel);
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = Config.checkEmpty(email);
    }
    
}
