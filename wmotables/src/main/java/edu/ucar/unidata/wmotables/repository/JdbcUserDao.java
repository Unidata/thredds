package edu.ucar.unidata.wmotables.repository;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import edu.ucar.unidata.wmotables.domain.User;

/**
 * The UserDao implementation.  Persistence mechanism is a database. 
 */

public class JdbcUserDao extends JdbcDaoSupport implements UserDao {

    protected static Logger logger = Logger.getLogger(JdbcUserDao.class);

    private SimpleJdbcInsert insertActor;

    /**
     * Looks up and retrieves a user from the persistence mechanism using the userId.
     * 
     * @param userId  The id of the user we are trying to locate (will be unique for each user). 
     * @return  The user represented as a User object.  
     * @throws RecoverableDataAccessException  If unable to lookup table with the given user id. 
     */
    public User lookupUser(int userId) {
        String sql = "SELECT * FROM users WHERE userId = ?";
        List<User> users = getJdbcTemplate().query(sql, new UserMapper(), userId);        
        if (users.isEmpty()) {
            throw new RecoverableDataAccessException("Unable to find user with Id: " + new Integer(userId).toString());
        }   
        return users.get(0);
    }

    /**
     * Looks up and retrieves a user from the persistence mechanism using the userName.
     * 
     * @param userName  The userName of the user we are trying to locate (will be unique for each user). 
     * @return  The user represented as a User object.
     * @throws RecoverableDataAccessException  If unable to lookup table with the given user id. 
     */
    public User lookupUser(String userName) {
        String sql = "SELECT * FROM users WHERE userName = ?";
        List<User> users = getJdbcTemplate().query(sql, new UserMapper(), userName);        
        if (users.isEmpty()) {
            throw new RecoverableDataAccessException("Unable to find user with user name: " + userName);
        }   
        return users.get(0);
    }

    /**
     * Requests a List of all users from the persistence mechanism.
     * 
     * @return  A List of users.   
     */
    public List<User> getUserList() {
        String sql = "SELECT * FROM users ORDER BY dateCreated DESC";               
        List<User> users = getJdbcTemplate().query(sql, new UserMapper());
        return users;
    }

    /**
     * Queries the persistence mechanism and returns the number of users.
     * 
     * @return  The total number of users as an int.   
     */
    public int getUserCount() {
        String sql = "SELECT count(*) FROM users";
        List<User> users = getJdbcTemplate().query(sql, new UserMapper());
        return users.size();
    }

    /**
     * Finds and removes the user from the persistence mechanism.
     * 
     * @param userId  The userId in the persistence mechanism.
     * @throws RecoverableDataAccessException  If unable to find and delete the user. 
     */
    public void deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE userId = ?";
        int rowsAffected  = getJdbcTemplate().update(sql, userId);
        if (rowsAffected <= 0) {
            throw new RecoverableDataAccessException("Unable to delete user. No user found with Id: " + new Integer(userId).toString());
        }   
    }

    /**
     * Finds and removes the user from the persistence mechanism.
     * 
     * @param userName  The userName in the persistence mechanism
     * @throws RecoverableDataAccessException  If unable to find and delete the user. 
     */
    public void deleteUser(String userName) {
        String sql = "DELETE FROM users WHERE userName = ?";
        int rowsAffected  = getJdbcTemplate().update(sql, userName);
        if (rowsAffected <= 0) {
            throw new RecoverableDataAccessException("Unable to delete user. No user found with user name: " + userName);
        }   
    }

    /**
     * Creates a new user.
     * 
     * @param user  The user to be created. 
     * @throws RecoverableDataAccessException  If the user we are trying to create already exists.
     */
    public void createUser(User user) {
        String sql = "SELECT * FROM users WHERE userName = ?";
        List<User> users = getJdbcTemplate().query(sql, new UserMapper(), user.getUserName());        
        if (!users.isEmpty()) {
            throw new RecoverableDataAccessException("User with user name \"" +  user.getUserName() + "\" already exists.");
        } else {
            this.insertActor = new SimpleJdbcInsert(getDataSource()).withTableName("users").usingGeneratedKeyColumns("userId");
            SqlParameterSource parameters = new BeanPropertySqlParameterSource(user);
            Number newUserId = insertActor.executeAndReturnKey(parameters);
            user.setUserId(newUserId.intValue());
        }   
    }

    /**
     * Saves changes made to an existing user.
     * 
     * @param user   The existing user with changes that needs to be saved. 
     * @throws RecoverableDataAccessException  If unable to find the user to update. 
     */
    public void updateUser(User user)  {
        String sql = "UPDATE users SET emailAddress = ?, fullName = ?, center= ?, subCenter= ?, dateModified = ? WHERE userId = ?";
        int rowsAffected  = getJdbcTemplate().update(sql, new Object[] {
            // order matters here
            user.getEmailAddress(), 
            user.getFullName(),
            user.getCenter(), 
            user.getSubCenter(), 
            user.getDateModified(),
            user.getUserId()
        });
        if (rowsAffected  <= 0) {
            throw new RecoverableDataAccessException("Unable to update user.  No user for with user name: " + user.getUserName());
        }     
    } 

    /**
     * Updates the User's Password
     * 
     * @param user  The user to whose password we need to update. 
     * @throws RecoverableDataAccessException  If unable to find the user to update. 
     */
    public void updatePassword(User user)  {
        String sql = "UPDATE users SET password = ?, dateModified = ? WHERE userId = ?";
        int rowsAffected  = getJdbcTemplate().update(sql, new Object[] {
            // order matters here
            user.getPassword(), 
            user.getDateModified(),
            user.getUserId()
        });
        if (rowsAffected  <= 0) {
            throw new RecoverableDataAccessException("Unable to update user.  User not found: " + user.toString());
        }     
    } 


    /***
     * Maps each row of the ResultSet to a User object.
     */
    private static class UserMapper implements RowMapper<User> {
        /**
         * Maps each row of data in the ResultSet to the User object.
         * 
         * @param rs  The ResultSet to be mapped.
         * @param rowNum  The number of the current row.
         * @return  The populated User object.
         * @throws SQLException  If a SQLException is encountered getting column values.
         */
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getInt("userId"));
            user.setUserName(rs.getString("userName"));
            user.setPassword(rs.getString("password"));
            user.setAccessLevel(rs.getInt("accessLevel"));
            user.setEmailAddress(rs.getString("emailAddress"));
            user.setFullName(rs.getString("fullName"));
            user.setCenter(rs.getInt("center"));
            user.setSubCenter(rs.getInt("subCenter"));
            user.setDateCreated(rs.getTimestamp("dateCreated"));
            user.setDateModified(rs.getTimestamp("dateModified"));
            return user;
        }
    }

}
