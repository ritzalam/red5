package org.red5.webapps.admin.controllers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.red5.webapps.admin.controllers.service.UserDAO;
import org.red5.webapps.admin.controllers.service.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class PanelController implements Controller {

	protected static Logger log = LoggerFactory.getLogger(PanelController.class);	

	private static UserDetailsService userDetailsService;
	
	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		log.debug("handle request");

		org.springframework.security.userdetails.UserDetails userDetails = null;
		try {
			userDetails = userDetailsService.loadUserByUsername("admin");
		} catch (Exception e) {
			log.debug("Admin lookup error", e);
		}
		//try the dao
		if (userDetails == null) {
			log.debug("User details were null from service, trying dao");
			userDetails = UserDAO.getUser("admin");
		}
		
		//if there arent any users then send to registration
		if (userDetails != null) {
			log.debug("Creating adminPanel");
			return new ModelAndView("panel");
		} else {
			//check for model
			log.debug("{}", ToStringBuilder.reflectionToString(request));
			if (request.getMethod().equalsIgnoreCase("POST")) {
    			//no model then redirect...
    			log.debug("Redirecting to register with user details");	
    			return new ModelAndView("register");		
			} else {
    			//no model then redirect...
    			log.debug("Redirecting to register");
    			userDetails = new UserDetails();
    			((UserDetails) userDetails).setUsername("admin");
    			return new ModelAndView("register", "userDetails", userDetails);
			}
		}
	}

	public ModelAndView doRequest(HttpServletRequest request,
			HttpServletResponse response) {
		return new ModelAndView();
	}

	public void setUserDetailsService(UserDetailsService userDetailsService) {
		PanelController.userDetailsService = userDetailsService;
	}
	
}
