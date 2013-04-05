package edu.ucar.unidata.wmotables.controller;

import org.apache.log4j.Logger;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import edu.ucar.unidata.wmotables.domain.Table;
import edu.ucar.unidata.wmotables.domain.User;
import edu.ucar.unidata.wmotables.service.TableManager;
import edu.ucar.unidata.wmotables.service.UserManager;
import edu.ucar.unidata.wmotables.service.TableValidator;

/**
 * Controller to handle and modify a WMO table. 
 */

@Controller
public class TableController implements HandlerExceptionResolver {

    protected static Logger logger = Logger.getLogger(TableController.class);

    @Resource(name="userManager")
    private UserManager userManager;
    @Resource(name="tableManager")
    private TableManager tableManager;
    
    @Value("${table.types}")
    private String tableTypeString;

    @Autowired
    private TableValidator tableValidator;

    @InitBinder("table")
    public void initBinder(WebDataBinder binder) {
        binder.setValidator(tableValidator);  
    }   

    private String authName;

    /**
     * Accepts a GET request for a List of all Table objects.
     *
     * View is a list of all Tables in the database. The view can
     * handle an empty list of Tables if no Table objects have been
     * persisted in the database yet.
     * 
     * @param model  The Model used by the View.
     * @return  The path for the ViewResolver.
     */
    @RequestMapping(value="/table", method=RequestMethod.GET)
    public String listAllTables(Model model) { 
        List<Table> tables = tableManager.getTableList();
        model.addAttribute("tables", tables);   
        Map<String, User> users = new HashMap<String, User>();
        Iterator<Table> iterator = tables.iterator();
	    while (iterator.hasNext()) {
		    Table table =  iterator.next();
            User user = userManager.lookupUser(table.getUserId());
            users.put(new Integer(table.getUserId()).toString(), user);
        }   
        model.addAttribute("users", users); 
        return "listTables";
    }

    /**
     * Accepts a GET request for a specific Table object.
     *
     * View is the requested Table object, or a list of all Table 
     * objects if unable to find the requested Table in the database.
     * 
     * @param checksum  The 'checksum' as provided by @PathVariable.
     * @param model  The Model used by the View.
     * @return  The path for the ViewResolver.
     */
    @RequestMapping(value="/table/{checksum}", method=RequestMethod.GET)
    public String viewTable(@PathVariable String checksum, Model model) { 
        try {
            Table table = tableManager.lookupTable(checksum);
            model.addAttribute("table", table);     
            User user = userManager.lookupUser(table.getUserId());   
            model.addAttribute("user", user);    
            return "viewTable";
        } catch (RecoverableDataAccessException e) {
            model.addAttribute("error", e.getMessage()); 
            List<Table> tables = tableManager.getTableList();
            model.addAttribute("tables", tables);   
            Map<String, User> users = new HashMap<String, User>();
            Iterator<Table> iterator = tables.iterator();
	        while (iterator.hasNext()) {
		        Table table =  iterator.next();
                User user = userManager.lookupUser(table.getUserId());
                users.put(new Integer(table.getUserId()).toString(), user);
            }   
            model.addAttribute("users", users); 
            return "listTables";
        }
    }

    /**
     * Accepts a GET request to view a specific Table.
     *
     * No view is produced.  The file is streamed to the client for viewing.
     * 
     * @param checksum  The 'checksum' as provided by @PathVariable.
     * @param response  The HttpServletResponse response.
     */
    @RequestMapping(value="/table/view/{checksum}", method=RequestMethod.GET)
    public void viewTableFile(@PathVariable String checksum, HttpServletResponse response) { 
        Table table = tableManager.lookupTable(checksum);
        tableManager.downloadTableFile("view", table, response);
    }

    /**
     * Accepts a GET request to download a specific Table.
     *
     * No view is produced.  The file is streamed to the client for download.
     * 
     * @param checksum  The 'checksum' as provided by @PathVariable.
     * @param response  The HttpServletResponse response.
     */
    @RequestMapping(value="/table/download/{checksum}", method=RequestMethod.GET)
    public void downloadTable(@PathVariable String checksum, HttpServletResponse response) { 
        Table table = tableManager.lookupTable(checksum);
        tableManager.downloadTableFile("download", table, response);
    }

    /**
     * Accepts a GET request to create a new Table object. 
     *
     * View is a web form to upload a new Table.
     *
     * Only the User/owner and Users with a role of 'ROLE_ADMIN'  
     * are allowed to create Tables for the user account.
     * 
     * @param userName  The 'userName' as provided by @PathVariable. 
     * @param model  The Model used by the View.
     * @return  The path for the ViewResolver.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userName == authentication.name")
    @RequestMapping(value="/table/create/{userName}", method=RequestMethod.GET)
    public String createTable(@PathVariable String userName, Model model) {
        model.addAttribute("table", new Table());  
        User user = userManager.lookupUser(userName);  
        model.addAttribute("user", user);    
        model.addAttribute("formAction", "create");  
        List<String> tableTypesList = getTableTypes();   
        model.addAttribute("tableTypeList", tableTypesList);  
        return "tableForm";
    }

    /**
     * Accepts a POST request to create a new Table object and persist it. 
     * 
     * View is either the newly created Table object or the web form to create a 
     * new Table if: 
     * 1) a Table of the same checksum has already exists in the database, 
     * 2) or if there are validation errors with the user input. 
     *
     * Only the User/owner and Users with a role of 'ROLE_ADMIN' are allowed 
     * to create new Table objects.
     * 
     * @param table  The Table to persist. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the View.
     * @return  The redirect to the needed View.
     * @throws IOException  If an IO error occurs when writing the table to the file system.
     * @thows AccessDeniedException  If logged in user does not have permission to create the table.
     */
    @RequestMapping(value="/table/create", method=RequestMethod.POST)
    public ModelAndView createTable(@Valid Table table, BindingResult result, Model model) throws IOException, AccessDeniedException {   
        // The first thing we do is validate that the user has permission to create a table
        User user = userManager.lookupUser(table.getUserId()); 
        // These methods are not part of the validator's validate method.
        tableValidator.validateFileSize(table.getFile(), result);  
        tableValidator.validateFileType(table.getMimeType(), result);  
        if (isAuthorized(user.getUserName())) {
            if (result.hasErrors()) {
                model.addAttribute("user", user); 
                model.addAttribute("formAction", "create");  
                List<String> tableTypesList = getTableTypes();   
                model.addAttribute("tableTypeList", tableTypesList);
                return new ModelAndView("tableForm"); 
            } else { 
                try {
                    table.setVisibility(1);
                    tableManager.createTable(table);
                    model.addAttribute("table", table);
                    model.addAttribute("user", user);     
                    return new ModelAndView(new RedirectView("/table/" + table.getChecksum(), true));
                } catch (RecoverableDataAccessException e) {
                    model.addAttribute("error", e.getMessage());
                    model.addAttribute("user", user); 
                    model.addAttribute("formAction", "create");  
                    List<String> tableTypesList = getTableTypes();   
                    model.addAttribute("tableTypeList", tableTypesList);
                    return new ModelAndView("tableForm"); 
                }       
            }       
        } else {   
           throw new AccessDeniedException ("User: " + getAuthName()  + " is attempting to create under the user account: "  + user.getUserName());
        }
    }

    /**
     * Accepts a GET request to update an existing Table object.
     *
     * View is a web form to update an existing Table.
     *
     * Only the User/owner and Users with a role of 'ROLE_ADMIN' 
     * are allowed to update Table objects.
     * 
     * @param checksum  The checksum as provided by @PathVariable. 
     * @param model  The Model used by the View.
     * @return  The path for the ViewResolver.
     * @thows AccessDeniedException  If logged in user does not have permission to update the table.
     */
    @RequestMapping(value="/table/update/{checksum}", method=RequestMethod.GET)
    public String updateTable(@PathVariable String checksum, Model model) throws AccessDeniedException { 
        Table table = tableManager.lookupTable(checksum);
        // The first thing we do is validate that the user has permission to update a table
        User user = userManager.lookupUser(table.getUserId());   
        if (isAuthorized(user.getUserName())) {
           model.addAttribute("table", table);         
           model.addAttribute("user", user);       
           model.addAttribute("formAction", "update");  
           List<String> tableTypesList = getTableTypes();   
           model.addAttribute("tableTypeList", tableTypesList); 
           return "tableForm";
        } else {   
           throw new AccessDeniedException ("User: " + getAuthName()  + " is attempting to update a table that belongs to the user account: "  + user.getUserName());
        }
    }

    /**
     * Accepts a POST request to update an existing Table object. 
     *
     * View is the updated Table object, or the web form to update
     * the Table if:
     * 1) unable to locate the Table in the database,
     * 2) or if there are validation errors with the user input.
     *
     * Only the User/owner and Users with a role of 'ROLE_ADMIN' are 
     * allowed to update Table objects.
     * 
     * @param table  The Table to update. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the View.
     * @return  The redirect to the needed View.
     * @thows AccessDeniedException  If logged in user does not have permission to update the table.
     */
    @RequestMapping(value="/table/update", method=RequestMethod.POST)
    public ModelAndView updateTable(@Valid Table table, BindingResult result, Model model) throws AccessDeniedException {  
        // The first thing we do is validate that the user has permission to create a table
        User user = userManager.lookupUser(table.getUserId()); 
        if (isAuthorized(user.getUserName())) {
            if (result.hasErrors()) {
                model.addAttribute("user", user); 
                model.addAttribute("formAction", "update");  
                List<String> tableTypesList = getTableTypes();   
                model.addAttribute("tableTypeList", tableTypesList);
                return new ModelAndView("tableForm"); 
            } else { 
                try {
                    tableManager.updateTable(table);
                    table = tableManager.lookupTable(table.getTableId());
                    model.addAttribute("table", table);         
                    model.addAttribute("user", user);     
                    return new ModelAndView(new RedirectView("/table/" + table.getChecksum(), true));
                } catch (RecoverableDataAccessException e) {
                    model.addAttribute("error", e.getMessage());
                    model.addAttribute("user", user);       
                    model.addAttribute("formAction", "update");  
                    List<String> tableTypesList = getTableTypes();   
                    model.addAttribute("tableTypeList", tableTypesList); 
                    return new ModelAndView("tableForm"); 
                }
            }
        } else {   
           throw new AccessDeniedException ("User: " + getAuthName()  + " is attempting to update a table that belongs to the user account: "  + user.getUserName());
        }
    }

    /**
     * Accepts a POST request to hide a new Table object. 
     * 
     * View is the hidden Table object in "hidden" view mode.
     *
     * Only the User/owner and Users with a role of 'ROLE_ADMIN' 
     * are allowed to hide tables for the user account.
     * 
     * @param table  The Table to hide. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the View.
     * @return  The redirect to the needed View.
     * @thows AccessDeniedException  If logged in user does not have permission to delete the table.
     */
    @RequestMapping(value="/table/hide", method=RequestMethod.POST)
    public ModelAndView hideTable(Table table, BindingResult result, Model model) throws AccessDeniedException {  
        int visibility = table.getVisibility();
        table = tableManager.lookupTable(table.getTableId());
        // The first thing we do is validate that the user has permission to create a table
        User user = userManager.lookupUser(table.getUserId()); 
        if (isAuthorized(user.getUserName())) {
            try {
                if (visibility == 1) {
                    table.setVisibility(0);
                } else {
                    table.setVisibility(1);
                }
                tableManager.toggleTableVisibility(table);
                model.addAttribute("table", table);         
                model.addAttribute("user", user);     
                return new ModelAndView(new RedirectView("/table/" + table.getChecksum(), true));
            } catch (RecoverableDataAccessException e) {
                model.addAttribute("error", e.getMessage());
                model.addAttribute("table", table);     
                model.addAttribute("user", user);    
                return new ModelAndView(new RedirectView("/table/" + table.getChecksum(), true));
            }
        } else {   
           throw new AccessDeniedException ("User: " + getAuthName()  + " is attempting to hide a table that belongs to the user account: "  + user.getUserName());
        }
    }

    /**
     * This method gracefully handles any uncaught exception that are fatal 
     * in nature and unresolvable by the user.
     * 
     * @param request   The current HttpServletRequest request.
     * @param response  The current HttpServletRequest response.
     * @param handler  The executed handler, or null if none chosen at the time of the exception.  
     * @param exception  The  exception that got thrown during handler execution.
     * @return  The error page containing the appropriate message to the user. 
     */
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        String message = "";
        ModelAndView modelAndView = new ModelAndView();
        Map<String, Object> model = new HashMap<String, Object>();
        modelAndView.setViewName("fatalError"); 
        if (exception instanceof AccessDeniedException){ 
            message = exception.getMessage();
            modelAndView.setViewName("denied");
        } else if (exception instanceof NullPointerException) {
            message = "Problem with the tableStashDir argument during File creation.  Verify the wmotables.home value in the wmotables.properties file is correct: " + exception.getMessage();
        } else if (exception instanceof FileNotFoundException) {
            message = "Unable to create FileOutputStream for File: " + exception.getMessage();
        } else if (exception instanceof IOException) {
            message = "An IO error occured with the FileOutputStream for File: " + exception.getMessage();
        } else if (exception instanceof SecurityException) {
            message = "JVM security manager configuration conflict.  Unable to write File: " + exception.getMessage();
        } else {
            message = "An error has occurred: " + exception.getClass().getName() + ": " + exception.getMessage();  
        }        
        logger.error(message);
        model.put("message", message);
        modelAndView.addAllObjects(model);
        return modelAndView;
    }


    /**
     * Checks the authenticated user's credentials and roles to determine if the user
     * has permission to perform the required method action.  Returns true if authorized.
     * 
     * @param userName  The user name we will evaluate the logged in user against. 
     * @return  The boolean determining if the user is authorized to perform the method action.
     */
    public boolean isAuthorized(String userName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Get logged in username
        String authName = auth.getName(); 
        if (authName.equals(userName)) {
            return true;
        } else {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
            while (iterator.hasNext()) {
                GrantedAuthority grantedAuthority = iterator.next();
                String authority = grantedAuthority.getAuthority();
                if (authority.equals("ROLE_ADMIN")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns the authenticated user name.
     * 
     * @return  The authenticated user name.
     */
    public String getAuthName() {
        return authName;
    }

    /**
     * Sets the authenticated user name.
     * 
     * @param authName  The authenticated user name.
     */
    public void setAuthName(String authName) {
        this.authName = authName;
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
