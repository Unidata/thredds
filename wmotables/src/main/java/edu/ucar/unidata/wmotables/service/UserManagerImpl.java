package edu.ucar.unidata.wmotables.service;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import edu.ucar.unidata.wmotables.domain.User;
import edu.ucar.unidata.wmotables.repository.UserDao;


/**
 * Service for processing User objects. 
 */
public class UserManagerImpl implements UserManager {

    private UserDao userDao;

    /*
     * Sets the data access object which will acquire and persist the data 
     * passed to it via the methods of this UserManager. 
     * 
     * @param userDao  The service mechanism data access object representing a User. 
     */
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    /*
     * Looks up and retrieves a user from the persistence mechanism using the user id.
     * 
     * @param userId  The id of the user we are trying to locate (will be unique for each user).
     * @return  The user represented as a User object.   
     */
    public User lookupUser(int userId) {
        return userDao.lookupUser(userId);
    }
   
    /*
     * Requests a List of all users from the persistence mechanism.
     * 
     * @return  A List of users.   
     */
    public List<User> getUserList() {
        return userDao.getUserList();
    }

    /*
     * Queries the persistence mechanism and returns the number of users.
     * 
     * @return  The total number of users as an int.   
     */
    public int getUserCount(){
        return userDao.getUserCount();
    }

    /*
     * Finds and removes the user from the persistence mechanism.
     * 
     * @param userId  The user id in the persistence mechanism.  
     */
    public void deleteUser(int userId) {
        userDao.deleteUser(userId);
    }

    /*
     * Creates a new user.
     * 
     * @param user  The user to be created. 
     */
    public void createUser(User user) {
        Date now = new Date(System.currentTimeMillis());
        user.setDateCreated(now);
        user.setDateModified(now);
        userDao.createUser(user);
    }

    /*
     * Saves changes made to an existing user . 
     * 
     * @param user   The existing user with changes that needs to be saved. 
     */
    public void updateUser(User user) {
        Date now = new Date(System.currentTimeMillis());
        user.setDateModified(now);
        userDao.updateUser(user);
    }
}
