package org.red5.webapps.admin.controllers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;

import org.red5.webapps.admin.controllers.service.UserDetails;
import org.red5.webapps.admin.utils.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.dao.DaoAuthenticationProvider;
import org.springframework.security.providers.dao.salt.SystemWideSaltSource;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.UsernameNotFoundException;
import org.springframework.security.userdetails.memory.InMemoryDaoImpl;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

public class RegisterUserController extends SimpleFormController {

	protected static Logger log = LoggerFactory.getLogger(RegisterUserController.class);
	
	private static DaoAuthenticationProvider daoAuthenticationProvider;
	
	private static UserDetailsService userDetailsService;
	
	private SystemWideSaltSource saltSource;
	
	private String userPropertiesLocation;
		
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
		log.debug("Password: {}", hashedPassword);
		FileOutputStream fos = null;
		// register user here
        try {
        	String ctxPath = getServletContext().getRealPath("/");
        	Properties props = new Properties();
        	log.debug("Context path: {}", ctxPath);
        	props.load(new FileInputStream(ctxPath + userPropertiesLocation));
        	log.debug("Number of current entries: {}", props.size());
        	props.put(username, hashedPassword+",ROLE_SUPERVISOR");
        	if (props.size() > 0) {
        		fos = new FileOutputStream(ctxPath + userPropertiesLocation);
        		props.store(fos, "");
        		fos.flush();
        	}
        	//setup security user stuff and add them to the current "cache" and current user map
        	GrantedAuthority[] auths = new GrantedAuthority[1];
        	auths[0] = new GrantedAuthorityImpl("ROLE_SUPERVISOR");
        	User usr = new User(username, hashedPassword, true, true, true, true, auths);
        	daoAuthenticationProvider.getUserCache().putUserInCache(usr);
        	
        	((InMemoryDaoImpl) userDetailsService).getUserMap().addUser(usr);
        	
        	Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        	String tmp = "";
        	if (obj instanceof UserDetails) {
        	  tmp = ((UserDetails) obj).getUsername();
        	} else {
        	  tmp = obj.toString();
        	}
        	log.debug("User names match: {}", (username.equals(tmp)));        	

//          try {
//        		UserDetailsService usrDetailSvc = daoAuthenticationProvider..getUserDetailsService();
//        		usrDetailSvc.loadUserByUsername(username);
//			} catch (UsernameNotFoundException e) {
//				log.debug("User {} not found", username);
//			} catch (Exception e) {
//				log.error("{}", e);
//			}
        	
        } catch (IOException e) {
        	log.error("{}", e);
        } finally {
        	if (fos != null) {
        		try {
					fos.close();
				} catch (IOException e) {
				}
        	}
        }

		return new ModelAndView(new RedirectView(getSuccessView()));
	}

	public void setDaoAuthenticationProvider(DaoAuthenticationProvider value) {
		RegisterUserController.daoAuthenticationProvider = value;
	}

	public void setUserPropertiesLocation(String userPropertiesLocation) {
		this.userPropertiesLocation = userPropertiesLocation;
	}

	public void setSaltSource(SystemWideSaltSource saltSource) {
		this.saltSource = saltSource;
	}

	public void setUserDetailsService(UserDetailsService userDetailsService) {
		RegisterUserController.userDetailsService = userDetailsService;
	}
	
}