package org.red5.webapps.admin.controllers;

import java.io.IOException;

import javax.servlet.ServletException;


//import org.red5.webapps.admin.controllers.service.UserDetails;
import org.red5.webapps.admin.utils.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.dao.DaoAuthenticationProvider;
import org.springframework.security.providers.dao.salt.SystemWideSaltSource;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.jdbc.JdbcUserDetailsManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

public class RegisterUserController extends SimpleFormController {

	protected static Logger log = LoggerFactory.getLogger(RegisterUserController.class);
	
	private static DaoAuthenticationProvider daoAuthenticationProvider;
	
	private static UserDetailsService userDetailsService;
	
	private SystemWideSaltSource saltSource;
		
	public ModelAndView onSubmit(Object command) throws ServletException {
		log.debug("onSubmit {}", command);

		String salt = saltSource.getSystemWideSalt();
		UserDetails userDetails = (UserDetails) command;
		String username = userDetails.getUsername();
		String password = userDetails.getPassword();
		log.debug("User details: username={} password={}", username, password);
		PasswordGenerator passwordGenerator = new PasswordGenerator(password,
				salt);
		String hashedPassword = passwordGenerator.getPassword();
		log.debug("Password hash: {}", hashedPassword);

		try {
    		// register user here

			if (!((JdbcUserDetailsManager) userDetailsService).userExists("admin"))
			{
    			GrantedAuthority[] auths = new GrantedAuthority[1];
            	auths[0] = new GrantedAuthorityImpl("ROLE_SUPERVISOR");
            	User usr = new User(username, hashedPassword, true, true, true, true, auths);
            	((JdbcUserDetailsManager) userDetailsService).createUser(usr);
            	
            	if (((JdbcUserDetailsManager) userDetailsService).userExists(username)) {
                		//setup security user stuff and add them to the current "cache" and current user map	
            		daoAuthenticationProvider.getUserCache().putUserInCache(usr);
                } else {
                	log.warn("User registration failed");
                }
			} else {
				log.warn("User admin already exists");
			}
        } catch (Exception e) {
        	log.error("Error during registration", e);
        }

		return new ModelAndView(new RedirectView(getSuccessView()));
	}

	public void setDaoAuthenticationProvider(DaoAuthenticationProvider value) {
		RegisterUserController.daoAuthenticationProvider = value;
	}

	public void setSaltSource(SystemWideSaltSource saltSource) {
		this.saltSource = saltSource;
	}

	public void setUserDetailsService(UserDetailsService userDetailsService) {
		RegisterUserController.userDetailsService = userDetailsService;
	}
	
}