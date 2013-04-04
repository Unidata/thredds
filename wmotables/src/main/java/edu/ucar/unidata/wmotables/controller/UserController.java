package edu.ucar.unidata.wmotables.controller;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.servlet.HandlerExceptionResolver;

import edu.ucar.unidata.wmotables.domain.Table;
import edu.ucar.unidata.wmotables.domain.User;
import edu.ucar.unidata.wmotables.service.TableManager;
import edu.ucar.unidata.wmotables.service.UserManager;
import edu.ucar.unidata.wmotables.service.UserValidator;

/**
 * Controller to handle and modify a User. 
 */

@Controller
public class UserController implements HandlerExceptionResolver {

    protected static Logger logger = Logger.getLogger(UserController.class);
    @Resource(name="userManager")
    private UserManager userManager;
    @Resource(name="tableManager")
    private TableManager tableManager; 
    
    @Autowired
    private UserValidator userValidator;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setValidator(userValidator);  
    }   


    /**
     * Accepts a GET request for a List of all User objects.
     *
     * View is a list of all Users in the database. The view can
     * handle an empty list of Users if no User objects have been
     * persisted in the database yet.
     * 
     * @param model  The Model used by the View.
     * @return  The path for the ViewResolver.
     */
    @RequestMapping(value="/user", method=RequestMethod.GET)
    public String listUsers(Model model) { 
        List<User> users = userManager.getUserList();           
        model.addAttribute("users", users);    
        return "listUsers";
    }

    /**
     * Accepts a GET request for a specific User object.
     *
     * View is the requested User, or a list of all User objects
     * if unable to find the requested User in the database.
     * 
     * @param userName  The 'userName' as provided by @PathVariable.
     * @param model  The Model used by the View.
     * @return  The path for the ViewResolver.
     */
    @RequestMapping(value="/user/{userName}", method=RequestMethod.GET)
    public String viewUser(@PathVariable String userName, Model model) { 
        try{
            User user = userManager.lookupUser(userName);           
            model.addAttribute("user", user);    
            List<Table> tables = tableManager.getTableList(user.getUserId());
            model.addAttribute("tables", tables);         
            return "viewUser";
        } catch (RecoverableDataAccessException e) {
            model.addAttribute("error", e.getMessage());    
            List<User> users = userManager.getUserList();           
            model.addAttribute("users", users);    
            return "listUsers";
        }
    }

    /**
     * Accepts a GET request to create a new User object. 
     *
     * View is a web form to create the new User.
     *
     * Only Users with a role of 'ROLE_ADMIN' are allowed 
     * to create new User objects.
     * 
     * @param model  The Model used by the View.
     * @return  The path for the ViewResolver.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/user/create", method=RequestMethod.GET)
    public String createUser(Model model) {   
        model.addAttribute("formAction", "create");  
        model.addAttribute("user", new User());  
        return "userForm";
    }

    /**
     * Accepts a POST request to create a new User object and persist it. 
     *
     * View is either the newly created User object, or the web form to create a 
     * new User if: 
     * 1) a User of the same user name has already exists in the database, 
     * 2) or if there are validation errors with the user input. 
     *
     * Only Users with a role of 'ROLE_ADMIN' are allowed to create new User objects.
     * 
     * @param user  The User to persist. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the View.
     * @return  The redirect to the needed View. 
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/user/create", method=RequestMethod.POST)
    public ModelAndView createUser(@Valid User user, BindingResult result, Model model) {  
        if (result.hasErrors()) {
           model.addAttribute("formAction", "create");  
           return new ModelAndView("userForm"); 
        } else {
            try {
                user.setPassword("changeme");
                userManager.createUser(user);
                user = userManager.lookupUser(user.getUserName());  
                model.addAttribute("user", user);     
                List<Table> tables = tableManager.getTableList(user.getUserId());
                model.addAttribute("tables", tables);  
                return new ModelAndView(new RedirectView("/user/" + user.getUserName(), true)); 
            } catch (RecoverableDataAccessException e) {
                result.rejectValue("userName", "user.error", e.getMessage());
                model.addAttribute("formAction", "create");  
                return new ModelAndView("userForm"); 
            }
        }         
    }

    /**
     * Accepts a GET request to update an existing User object. 
     * 
     * View is a web form to update an existing User.
     *
     * Only the User/owner and Users with a role of 'ROLE_ADMIN' are 
     * allowed to update the User account.
     * 
     * @param userName  The 'userName' as provided by @PathVariable. 
     * @param model  The Model used by the View.
     * @return  The path for the ViewResolver.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userName == authentication.name")
    @RequestMapping(value="/user/update/{userName}", method=RequestMethod.GET)
    public String updateUser(@PathVariable String userName, Model model) {   
        User user = userManager.lookupUser(userName);   
        model.addAttribute("user", user);   
        model.addAttribute("formAction", "update");      
        return "userForm";
    }

    /**
     * Accepts a POST request to update an existing User object.
     *
     * View is either the updated User object, or the web form to update
     * the User if:
     * 1) unable to locate the User in the database,
     * 2) or if there are validation errors with the user input.
     *
     * Only the User/owner and Users with a role of 'ROLE_ADMIN' are 
     * allowed to update the User account.
     * 
     * @param user  The User to update. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the View.
     * @return  The redirect to the needed View.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN') or #user.userName == authentication.name")
    @RequestMapping(value="/user/update", method=RequestMethod.POST)
    public ModelAndView updateUser(@Valid User user, BindingResult result, Model model) {
        if (result.hasErrors()) {
           model.addAttribute("formAction", "update");  
           return new ModelAndView("userForm"); 
        } else {   
            try {
                userManager.updateUser(user);
                model.addAttribute("user", user);     
                List<Table> tables = tableManager.getTableList(user.getUserId());
                model.addAttribute("tables", tables);           
                return new ModelAndView(new RedirectView("/user/" + user.getUserName(), true));   
            } catch (RecoverableDataAccessException e) {
                model.addAttribute("error", e.getMessage());  
                model.addAttribute("formAction", "update");  
                return new ModelAndView("userForm"); 
            }
        }    
    }

    /**
     * Accepts a POST request to delete an existing User object. 
     *
     * View is either a list of all remaining User objects, or the View of the User
     * we are trying to delete if we are unable to find the User in the database.
     *
     * TODO: handle tables owned by user.
     * Only Users with a role of 'ROLE_ADMIN' are allowed to delete users.
     * 
     * @param user  The User to delete. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the View.
     * @return  The redirect to the needed View.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/user/delete", method=RequestMethod.POST)
    public ModelAndView deleteUser(User user, BindingResult result, Model model) {   
        try {
            userManager.deleteUser(user.getUserId());
            List<User> users = userManager.getUserList();           
            model.addAttribute("users", users);           
            return new ModelAndView(new RedirectView("/user", true));
        } catch (RecoverableDataAccessException e) {
            model.addAttribute("error", e.getMessage());  
            List<Table> tables = tableManager.getTableList(user.getUserId());
            model.addAttribute("tables", tables);         
            return new ModelAndView(new RedirectView("/user" + user.getUserName(), true)); 
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
        if (exception instanceof AccessDeniedException){ 
            message = exception.getMessage();
            modelAndView.setViewName("denied");
        } else  {
            message = "An error has occurred: " + exception.getClass().getName() + ": " + exception.getMessage();  
            modelAndView.setViewName("fatalError"); 
        }
        logger.error(message);       
        model.put("message", message);
        modelAndView.addAllObjects(model);
        return modelAndView;
    }
  

}
