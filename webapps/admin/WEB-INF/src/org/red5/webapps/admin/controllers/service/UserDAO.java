
package org.red5.webapps.admin.controllers.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.red5.webapps.admin.UserDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;


/**
 * Simple DAO for manipulation of the user database.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class UserDAO {

	private static Logger log = LoggerFactory.getLogger(UserDAO.class);		
	
	public static boolean addUser(String username, String hashedPassword) {
		boolean result = false;
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			//set properties
			Properties props = new Properties();
			props.setProperty("username", UserDatabase.getUserName());
			props.setProperty("password", UserDatabase.getPassword());
			//create the db and get a connection
			conn = DriverManager.getConnection("jdbc:derby:" + UserDatabase.getDatabase() + ";", props);
			//make a statement
			stmt = conn.prepareStatement("INSERT INTO APPUSER (username, password, enabled) VALUES (?, ?, 'enabled')");
			stmt.setString(1, username);
			stmt.setString(2, hashedPassword);
			log.debug("Add user: {}", stmt.execute());			
			//add role
			stmt = conn.prepareStatement("INSERT INTO APPROLE (username, authority) VALUES (?, 'ROLE_SUPERVISOR')");
			stmt.setString(1, username);
			log.debug("Add role: {}", stmt.execute());			
			//
			result = true;
		} catch (Exception e) {
			log.error("Error connecting to db", e);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
		return result;
	}
	
	public static UserDetails getUser(String username) {
		UserDetails details = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			//set properties
			Properties props = new Properties();
			props.setProperty("username", UserDatabase.getUserName());
			props.setProperty("password", UserDatabase.getPassword());
			//create the db and get a connection
			conn = DriverManager.getConnection("jdbc:derby:" + UserDatabase.getDatabase() + ";", props);
			//make a statement
			stmt = conn.prepareStatement("SELECT * FROM APPUSER WHERE username = ?");
			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				log.debug("User found");			
				details = new UserDetails();
				details.setEnabled("enabled".equals(rs.getString("enabled")));
				details.setPassword(rs.getString("password"));
				details.setUserid(rs.getInt("userid"));
				details.setUsername(rs.getString("username"));
				//
				rs.close();
				//get role				
				stmt = conn.prepareStatement("SELECT authority FROM APPROLE WHERE username = ?");
				stmt.setString(1, username);
				rs = stmt.executeQuery();
				if (rs.next()) {
	            	GrantedAuthority[] authorities = new GrantedAuthority[1];
	            	authorities[0] = new GrantedAuthorityImpl(rs.getString("authority"));
	            	details.setAuthorities(authorities);
	            	//
	            	//if (daoAuthenticationProvider != null) {
    	            	//User usr = new User(username, details.getPassword(), true, true, true, true, authorities);
    	            	//daoAuthenticationProvider.getUserCache().putUserInCache(usr);					
	            	//}
				}			
			}
			rs.close();
		} catch (Exception e) {
			log.error("Error connecting to db", e);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
		return details;
	}
	
}
