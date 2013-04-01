package edu.ucar.unidata.wmotables.controller;

import org.apache.log4j.Logger;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.ui.Model;

import edu.ucar.unidata.wmotables.domain.Table;
import edu.ucar.unidata.wmotables.domain.User;
import edu.ucar.unidata.wmotables.service.TableManager;
import edu.ucar.unidata.wmotables.service.UserManager;

/**
 * Controller to handle and modify a WMO table. 
 */

@Controller
public class AuthenticationController {

    protected static Logger logger = Logger.getLogger(AuthenticationController.class);
    @Resource(name="userManager")
    private UserManager userManager;
    @Resource(name="tableManager")
    private TableManager tableManager;

    /**
     * Accepts a GET request for the login page.
     * View is the login page.
     * 
     * @param error  An error (if provided).
     * @param model  The Model used by the View.
     * @return  The 'login' path for the ViewResolver.
     */
    @RequestMapping(value = "/auth/login", method = RequestMethod.GET)
    public String getLoginPage(@RequestParam(value="error", required=false) boolean error, Model model) {
        if (error == true) {
            model.addAttribute("error", "You have entered an invalid username or password!");
        }    
        return "login";

    }

    /**
     * Accepts a GET request for the denied page. This is shown 
     * whenever a regular user tries to access an admin/user only page.
     * View is a denied page.
     *
     * @return  The 'denied' path for the ViewResolver.
     */
    @RequestMapping(value = "/auth/denied", method = RequestMethod.GET)
    public String getDeniedPage() { 
        return "denied";
    }




}
