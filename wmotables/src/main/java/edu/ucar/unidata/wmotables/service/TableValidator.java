package edu.ucar.unidata.wmotables.service;

import org.apache.log4j.Logger;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import edu.ucar.unidata.wmotables.domain.Table;

@Component
public class TableValidator implements Validator {

    protected static Logger logger = Logger.getLogger(TableValidator.class);

    private String[] NAUGHTY_STRINGS = {"<script>", "../", "javascript", "::", "&quot;", "fromcharCode", "%3", "$#", "alert(", ".js", ".source", "\\", "scriptlet", ".css", "binding:", ".htc", "vbscript", "mocha:", "livescript:", "base64", "\00", "xss:", "%77", "0x", "IS NULL;", "1;", "; --", "1=1"}; 
    private String[] NAUGHTY_CHARS = {"<", ">", "`", "^", "|", "}", "{"}; 

	private Pattern pattern;
	private Matcher matcher;
 
	private static final String CONTENT_TYPE_PATTERN = "((.*)(csv|text|xml)(.*))";

    @Value("${table.types}")
    private String tableTypeString;

    public boolean supports(Class clazz) {
        return Table.class.equals(clazz);
    }

    /**
     * Validates the user input contained in the Table object.
     * 
     * @param obj  The target object to validate.
     * @param error  Object in which to store any validation errors.
     */
    public void validate(Object obj, Errors errors) {
        Table table = (Table) obj;
        validateTitle(table.getTitle(), errors);  
        validateDescription(table.getDescription(), errors); 
        validateLocalVersion(table.getLocalVersion(), errors); 
        validateCenter(table.getCenter(), errors);
        validateSubCenter(table.getSubCenter(), errors);
        validateTableType(table.getTableType(), errors);
    }

    /**
     * Validates the size of the uploaded File.
     * 
     * @param file  The uploaded CommonsMultipartFile file.
     * @param error  Object in which to store any validation errors.
     */
    public void validateFileSize(CommonsMultipartFile file, Errors errors) {
        if (file.isEmpty()) {
            errors.rejectValue("file", "table.error", "You're attempting to upload an empty file.");
            return;
        }
        if (file.getSize() > 1048576) {
            errors.rejectValue("file", "table.error", "File upload too big! File size is restricted to 1MB max.");
            return;
        } 
    }

    /**
     * Validates the content type of the uploaded File.
     * 
     * @param contentType  The content type of the uploaded file.
     * @param error  Object in which to store any validation errors.
     */
    public void validateFileType(String contentType, Errors errors) {
        pattern = Pattern.compile(CONTENT_TYPE_PATTERN);
        matcher = pattern.matcher(StringUtils.lowerCase((contentType)));
        if (!matcher.matches()) {
            errors.rejectValue("file", "table.error", "Bad content type for file.  Only files of type text, csv and xml allowed.");
            return;
        } 
    }

    /**
     * Validates the user input for the title field.
     * 
     * @param input  The table input to validate.
     * @param error  Object in which to store any validation errors.
     */
    public void validateTitle(String input, Errors errors) {
        if (StringUtils.isBlank(input)) {
            errors.rejectValue("title", "table.error", "Table title is required!");
            return;
        }
        if ((StringUtils.length(input) < 3) || (StringUtils.length(input) > 75)) {
            errors.rejectValue("title", "table.error", "The table title must be between 3 and 75 characters in length.");
            return;
        }        
        validateInput("title", input, errors); 
    }

    /**
     * Validates the user input for the description field.
     * 
     * @param input  The table input to validate.
     * @param error  Object in which to store any validation errors.
     */
    public void validateDescription(String input, Errors errors) {
        if (StringUtils.isBlank(input)) {
            errors.rejectValue("description", "table.error", "Table description is required!");
            return;
        }
        if ((StringUtils.length(input) < 6) || (StringUtils.length(input) > 225)) {
            errors.rejectValue("description", "table.error", "The description must be between 6 and 225 characters in length.");
            return;
        }  
        validateInput("description", input, errors); 
    }
    
    /**
     * Validates the user input for the local version field.
     * 
     * @param input  The table input to validate.
     * @param error  Object in which to store any validation errors.
     */    
    public void validateLocalVersion(int input, Errors errors) { 
        if (StringUtils.isBlank(new Integer(input).toString())) {
            errors.rejectValue("localVersion", "table.error", "Local Version is required!");
            return;
        }
    }

    
    /**
     * Validates the user input for the center field.
     * 
     * @param input  The user input to validate.
     * @param error  Object in which to store any validation errors.
     */    
    public void validateCenter(int input, Errors errors) {
        if (StringUtils.isBlank(new Integer(input).toString())) {
            errors.rejectValue("center", "table.error", "Table center is required!");
            return;
        }
    }

    /**
     * Validates the user input for the sub center field.
     * 
     * @param input  The user input to validate.
     * @param error  Object in which to store any validation errors.
     */    
    public void validateSubCenter(int input, Errors errors) {
        if (StringUtils.isBlank(new Integer(input).toString())) {
            errors.rejectValue("subCenter", "table.error", "Table sub center is required!");
            return;
        }
    }

    /**
     * Validates the user input for the tableType field.
     * 
     * @param input  The table input to validate.
     * @param error  Object in which to store any validation errors.
     */    
    public void validateTableType(String input, Errors errors) { 
        List<String> tableTypesList = getTableTypes(); 
        if (!tableTypesList.contains(input)) {
            errors.rejectValue("tableType", "table.error", "You must select a table type!");
            return;
        }
    }

    /**
     * A generic utility method to validate user input against known bad characters and strings.
     * 
     * @param formField  The form field corresponding to the table input.
     * @param input  The table input to validate.
     * @param error  Object in which to store any validation errors.
     */
    public void validateInput(String formField, String input, Errors errors) {
        String badChar = checkForNaughtyChars(input);
        if (badChar != null) {
            logger.warn("Bad value submitted for " + formField + " : " + badChar);
            errors.rejectValue(formField, "table.error", "Bad value submitted: " + badChar);
            return;
        }
        String badString = checkForNaughtyStrings(input);
        if (badString != null) {
            logger.warn("Bad value submitted for " + formField + " : " + badString);
            errors.rejectValue(formField, "table.error", "Bad value submitted: " + badString);
            return;
        }
    }

    /**
     * Validates the user input against known bad strings.
     * 
     * @param itemToCheck  The user input to validate.
     */
    public String checkForNaughtyStrings(String itemToCheck) {
          for (String item : NAUGHTY_STRINGS) {              
              if (StringUtils.contains(StringUtils.lowerCase(itemToCheck), item)) {
                  return item;
              } 
          }
          return null;
    }

    /**
     * Validates the user input against known bad characters.
     * 
     * @param itemToCheck  The user input to validate.
     */
    public String checkForNaughtyChars(String itemToCheck) {
          for (String item : NAUGHTY_CHARS) {
              if (StringUtils.contains(itemToCheck, item)) {
                  return item;
              } 
          }
          return null;
    }

    /**
     * Grabs the tableTypeString and returns as a List.
     * 
     * @return  The list fo table types for display.
     */
    public List<String> getTableTypes() {
        String[] a = StringUtils.split(tableTypeString, ",");
        List<String> tableTypes = Arrays.asList(a);
        return tableTypes;
    }
}
