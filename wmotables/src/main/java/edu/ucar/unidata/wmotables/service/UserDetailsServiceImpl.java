package edu.ucar.unidata.wmotables.service;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


import edu.ucar.unidata.wmotables.repository.UserDao;


/**
 * Service for processing User objects with respect to authentication and access control. 
 */
public class UserDetailsServiceImpl implements UserDetailsService { 

    protected static Logger logger = Logger.getLogger(UserDetailsServiceImpl.class);

    public UserDao userDao;

    /**
     * Sets the data access object. 
     * 
     * @param userDao  The service mechanism data access object representing a User. 
     */
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Retrieves a user record containing the user's credentials and access. 
     * 
     * @param userName  The userName of the person authenticating.
     * @return  The userDetails used by Spring.
     * @thows UsernameNotFoundException  If unable to find the user in the database.
     * @thows DataAccessException  If we encounter an issue with accessing the data.
     */
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException, DataAccessException {
        UserDetails userDetails = null;
        try {    
            edu.ucar.unidata.wmotables.domain.User user = userDao.lookupUser(userName);
            // org.springframework.security.core.userdetails.User
            String username =  user.getUserName();
            String password = user.getPassword();
            boolean enabled = true;
            boolean accountNonExpired = true; 
            boolean credentialsNonExpired = true;
            boolean accountNonLocked = true;
            Collection<? extends GrantedAuthority> authorities = getAuthorities(user.getAccessLevel());
            userDetails =  new org.springframework.security.core.userdetails.User (username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        } catch (DataAccessException e) {
            logger.error("DataAccessException: error in retrieving user: " + e.getMessage());
        } catch (UsernameNotFoundException e) {
            logger.error("UsernameNotFoundException: error in retrieving user: " + e.getMessage());
        }
        return userDetails;
    }

    /**
     * Retrieves the correct ROLE type depending on the access level, where access level is an int.
     * @param access  The int value representing the access of the user.
     * @return The collection of granted authorities.
     */
    public Collection<GrantedAuthority> getAuthorities(int access) {
        // Create a list of grants for this user
        List<GrantedAuthority> authList = new ArrayList<GrantedAuthority>();
        // User access 
        if (access == 1) {
            authList.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        // Admin access
        if (access == 2) {
            authList.add(new SimpleGrantedAuthority("ROLE_USER"));
            authList.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } 
        return authList;
    }
}
