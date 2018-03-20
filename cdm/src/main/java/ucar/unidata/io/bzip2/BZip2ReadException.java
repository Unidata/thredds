/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */




package ucar.unidata.io.bzip2;


/**
 * Exception for BZip problems
 *
 * @author IDV Development Team
 * @version $Revision: 1.1 $
 */
public class BZip2ReadException extends RuntimeException {

    /**
     * Create a new BZip2ReadException with no message
     */
    public BZip2ReadException() {
        super();
    }

    /**
     * Create a new BZip2ReadException with the message
     *
     * @param message   detailed message associated with this exception
     */
    public BZip2ReadException(String message) {
        super(message);
    }

}

