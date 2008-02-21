package org.red5.webapps.admin.controllers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;

import org.acegisecurity.providers.dao.DaoAuthenticationProvider;
import org.acegisecurity.providers.dao.salt.SystemWideSaltSource;
import org.red5.webapps.admin.controllers.service.UserDetails;
import org.red5.webapps.admin.utils.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

public class RegisterUserController extends SimpleFormController {

	protected static Logger log = LoggerFactory.getLogger(RegisterUserController.class);
	
	private DaoAuthenticationProvider daoAuthenticationProvider;
	private String userPropertiesLocation;
		
	public ModelAndView onSubmit(Object command) throws ServletException {
		log.debug("onSubmit {}", command);
		
		SystemWideSaltSource saltSource = (SystemWideSaltSource) daoAuthenticationProvider
				.getSaltSource();

		String salt = saltSource.getSystemWideSalt();
		UserDetails userDetails = (UserDetails) command;
		String username = userDetails.getUsername();
		String password = userDetails.getPassword();
		log.debug("User details: username={} password={}", username, password);

		PasswordGenerator passwordGenerator = new PasswordGenerator(salt,
				password);

		String hashedPassword = passwordGenerator.getPassword();
		log.debug("Password: {}", hashedPassword);
		// register user here
        try {
        	Properties props = new Properties();
        	log.debug("Context path: {}", getServletContext().getRealPath("/"));
        	props.load(new FileInputStream(getServletContext().getRealPath("/") + userPropertiesLocation));
        	log.debug("Number of current entries: {}", props.size());
        	props.put(username, hashedPassword);
        	if (props.size() > 0) {
        		FileOutputStream fos = new FileOutputStream(getServletContext().getRealPath("/") + userPropertiesLocation);
        		props.store(fos, "");
        		fos.flush();
        		fos.close();
        	}
        } catch (IOException e) {
        	log.error("{}", e);
        }

		return new ModelAndView(new RedirectView(getSuccessView()));
	}

	public void setDaoAuthenticationProvider(DaoAuthenticationProvider value) {
		daoAuthenticationProvider = value;
	}

	public void setUserPropertiesLocation(String userPropertiesLocation) {
		this.userPropertiesLocation = userPropertiesLocation;
	}

}