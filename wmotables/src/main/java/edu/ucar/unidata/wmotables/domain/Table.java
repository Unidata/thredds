package edu.ucar.unidata.wmotables.domain;

import java.util.Date;
import java.io.Serializable;

import org.springframework.web.multipart.commons.CommonsMultipartFile;


/**
 * Object representing an Table.  
 *
 * A Table is a WMO table file persisted on disk.
 * Table attributes correspond to database columns 
 * containing metadata about the WMO file.
 */
public class Table implements Serializable {

    private CommonsMultipartFile file = null;
    private int tableId;
    private String title;
    private String description;
    private String originalName;
    private String version;
    private String mimeType;
    private String tableType;
    private String checksum;
    private int visibility;
    private int userId;
    private Date dateCreated;
    private Date dateModified;

    /**
     * Returns the uploaded file in CommonsMultipartFile format.
     * 
     * @return  The CommonsMultipartFile file. 
     */
    public CommonsMultipartFile getFile() {
        return file;
    }

    /**
     * Sets the uploaded file as a CommonsMultipartFile file. 
     * 
     * @param file  The CommonsMultipartFile file. 
     */
    public void setFile(CommonsMultipartFile file) {
        setOriginalName(file.getOriginalFilename());
        this.file = file;
    }

    /**
     * Returns the id of the wmo table in the database.
     * 
     * @return  The table id.
     */
    public int getTableId() {
        return tableId;
    }

    /**
     * Sets the id of the wmo table in the database.
     * 
     * @param tableId  The table id. 
     */
    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    /**
     * Returns the title of the table.
     * 
     * @return  The table's title.  
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the table.
     * 
     * @param title  The table's title. 
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the description of the table.
     * 
     * @return  The table's description.  
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the table.
     * 
     * @param description  The table's description. 
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the original name of the table as it was when upload to the registry.
     * 
     * @return  The table's original name. 
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * Sets the original name of the table as it was when upload to the registry.
     * 
     * @param originalName  The table's original name. 
     */
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    /**
     * Returns the version of the table.
     * 
     * @return  The table's version. 
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the table.
     * 
     * @param version  The table's version. 
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the table's mimeType.
     * 
     * @return  The mimeType. 
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the table's mimeType.
     * 
     * @param mimeType  The mimeType. 
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Returns the table's tableType.
     * 
     * @return  The tableType. 
     */
    public String getTableType() {
        return tableType;
    }

    /**
     * Sets the table's tableType.
     * 
     * @param tableType  The tableType. 
     */
    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    /**
     * Returns the table's checksum.
     * 
     * @return  The checksum. 
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Sets the table's checksum.
     * 
     * @param checksum  The checksum. 
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * Returns the visibility attribute of the table.
     * 
     * @return  The visibility attribute.
     */
    public int getVisibility() {
        return visibility;
    }

    /**
     * Sets the visibility attribute of the table.
     * 
     * @param visibility  The visibility attribute. 
     */
    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    /**
     * Returns the id of the User who owns the table.
     * 
     * @return  The user id.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Sets the id of the User who owns the table.
     * 
     * @param userId  The user id. 
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * Returns date the table entry was created.
     * 
     * @return  The table entry's creation date.
     */
    public Date getDateCreated() {
        return dateCreated;
    }
    
    /**
     * Sets the date the table entry was created.
     * 
     * @param dateCreated   The table entry's creation date.
     */    
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * Returns date the table was last modified.
     * 
     * @return  The table last modified date.
     */
    public Date getDateModified() {
        return dateModified;
    }
    
    /**
     * Sets the date the table was last modified.
     * 
     * @param dateModified   The table last modified date.
     */    
    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }
}
