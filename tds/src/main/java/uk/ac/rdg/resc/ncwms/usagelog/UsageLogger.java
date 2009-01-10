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

package uk.ac.rdg.resc.ncwms.usagelog;

import javax.sql.DataSource;

/**
 * Interface describing a class that logs usage of the ncWMS server.  This
 * will log requests for images and metadata.  Note that all entries use the
 * same UsageLogEntry class.
 * @todo Do we really need this interface?  Can't we just use the H2 database
 * for everything?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public interface UsageLogger
{
    /**
     * Make an entry in the usage log.  This method does not throw an
     * Exception: all problems with the usage logger must be recorded
     * in the log4j text log.  Implementing methods should make sure they
     * set the time to process the request, by taking System.currentTimeMs()
     * and subtracting logEntry.getRequestTime().
     */
    public void logUsage(UsageLogEntry logEntry);
    
    /**
     * Gets a JDBC DataSource object for accessing the usage log directly.
     * @return the DataSource object.  Subclasses may choose to return a new
     * DataSource with each invocation, or may choose to return the same
     * object each time.
     */
    public DataSource getDataSource();
}
