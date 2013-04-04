package edu.ucar.unidata.wmotables.service;

import org.apache.log4j.Logger;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import edu.ucar.unidata.wmotables.domain.User;

@Component
public class UserValidator implements Validator  {

    protected static Logger logger = Logger.getLogger(UserValidator.class);

    private String[] NAUGHTY_STRINGS = {"<script>", "../", "javascript", "::", "&quot;", "fromcharCode", "%3", "$#", "alert(", ".js", ".source", "\\", "scriptlet", ".css", "binding:", ".htc", "vbscript", "mocha:", "livescript:", "base64", "\00", "xss:", "%77", "0x", "IS NULL;", "1;", "; --", "1=1"}; 
    private String[] NAUGHTY_CHARS = {"<", ">", "`", "^", "|", "}", "{"}; 

	private Pattern pattern;
	private Matcher matcher;
 
	private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
		                                        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

	private static final String USER_NAME_PATTERN = "^[a-zA-Z0-9_-]{6,50}$";


    public boolean supports(Class clazz) {
        return User.class.equals(clazz);
    }

    /**
     * Validates the user input contained in the User object.
     * 
     * @param obj  The target object to validate.
     * @param error  Object in which to store any validation errors.
     */
    public void validate(Object obj, Errors errors) {
        User user = (User) obj;
        validateUserName(user.getUserName(), errors);  
        validateFullName(user.getFullName(), errors); 
        validateEmailAddress(user.getEmailAddress(), errors); 
        validateCenter(user.getCenter(), errors);
        validateSubCenter(user.getSubCenter(), errors);
    }

    /**
     * Validates the user input for the userName field.
     * 
     * @param input  The user input to validate.
     * @param error  Object in which to store any validation errors.
     */
    public void validateUserName(String input, Errors errors) {
        if (StringUtils.isBlank(input)) {
            errors.rejectValue("userName", "user.error", "User name is required!");
            return;
        }
        if ((StringUtils.length(input) < 6) || (StringUtils.length(input) > 50)) {
            errors.rejectValue("userName", "user.error", "The user name must be between 6 and 50 characters in length.");
            return;
        }        
        pattern = Pattern.compile(USER_NAME_PATTERN);
        matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            errors.rejectValue("userName", "user.error", "Only alphanumeric characters, dashed and underscores are allowed. (Spaces are NOT allowed.)");
            return;
        }
    }

    /**
     * Validates the user input for the fullName field.
     * 
     * @param input  The user input to validate.
     * @param error  Object in which to store any validation errors.
     */
    public void validateFullName(String input, Errors errors) {
        if (StringUtils.isBlank(input)) {
            errors.rejectValue("fullName", "user.error", "Full user name is required!");
            return;
        }
        if ((StringUtils.length(input) < 6) || (StringUtils.length(input) > 75)) {
            errors.rejectValue("fullName", "user.error", "The full user name must be between 6 and 75 characters in length.");
            return;
        }  
        validateInput("fullName", input, errors); 
    }
    
    /**
     * Validates the user input for the center field.
     * 
     * @param input  The user input to validate.
     * @param error  Object in which to store any validation errors.
     */    
    public void validateCenter(int input, Errors errors) {
        if (StringUtils.isBlank(new Integer(input).toString())) {
            errors.rejectValue("center", "user.error", "User center is required!");
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
            errors.rejectValue("subCenter", "user.error", "User sub center is required!");
            return;
        }
    }
    
    /**
     * Validates the user input for the emailAddress field.
     * 
     * @param input  The user input to validate.
     * @param error  Object in which to store any validation errors.
     */    
     public void validateEmailAddress(String input, Errors errors) {
        if (StringUtils.isBlank(input)) {
            errors.rejectValue("emailAddress", "user.error", "User email address is required!");
            return;
        }
        pattern = Pattern.compile(EMAIL_PATTERN);
        matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            errors.rejectValue("emailAddress", "user.error", "This is not a well-formed email address.");
            return;
        }  
    }

    /**
     * A generic utility method to validate user input against known bad characters and strings.
     * 
     * @param formField  The form field corresponding to the user input.
     * @param input  The user input to validate.
     * @param error  Object in which to store any validation errors.
     */
    public void validateInput(String formField, String input, Errors errors) {
        String badChar = checkForNaughtyChars(input);
        if (badChar != null) {
            logger.warn("Bad value submitted for " + formField + " : " + badChar);
            errors.rejectValue(formField, "user.error", "Bad value submitted: " + badChar);
            return;
        }
        String badString = checkForNaughtyStrings(input);
        if (badString != null) {
            logger.warn("Bad value submitted for " + formField + " : " + badString);
            errors.rejectValue(formField, "user.error", "Bad value submitted: " + badString);
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
}
