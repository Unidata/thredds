package edu.ucar.unidata.wmotables.domain;

import java.util.Date;
import java.util.List;
import java.io.Serializable;

/**
 * Object representing a User.  
 *
 * A User is person with an account in the wmotables web app. 
 * The User attributes correspond to database columns.
 */

public class User implements Serializable {

    private int userId;
    private String emailAddress;
    private String fullName;
    private String affiliation;
    private Date dateCreated;
    private Date dateModified;

    /**
     * Returns the id of the user in the database.
     * 
     * @return  The user id. 
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Sets the id of the user in the database.
     * 
     * @param userId  The user id. 
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * Returns the email address of the user.
     * 
     * @return  The user's email address.  
     */ 
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * Sets the email address of the user.
     * 
     * @param emailAddress  The user's email address. 
     */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * Returns the full name of the user.
     * 
     * @return  The user's full name.  
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the full name of the user.
     * 
     * @param fullName  The user's full name. 
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Returns the affiliation of the user.
     * 
     * @return  The user's affiliation.  
     */
    public String getAffiliation() {
        return affiliation;
    }

    /**
     * Sets the affiliation of the user.
     * 
     * @param affiliation  The user's affiliation. 
     */
    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    /**
     * Returns date the user was created.
     * 
     * @return  The user's creation date.
     */
    public Date getDateCreated() {
        return dateCreated;
    }
    
    /**
     * Sets the date the user was created.
     * 
     * @param dateCreated   The user's creation date.
     */    
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * Returns date the user's account was last modified.
     * 
     * @return  The user account last modified date.
     */
    public Date getDateModified() {
        return dateModified;
    }
    
    /**
     * Sets the date the user's account was last modified.
     * 
     * @param dateModified   The user account last modified date.
     */    
    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }


}
