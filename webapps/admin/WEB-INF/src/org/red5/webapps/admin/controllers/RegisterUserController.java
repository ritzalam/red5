package org.red5.webapps.admin.controllers;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

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

	public ModelAndView onSubmit(Object command) throws ServletException {

		log.debug("onSubmit {}", command);
		
		SystemWideSaltSource saltSource = (SystemWideSaltSource) daoAuthenticationProvider
				.getSaltSource();

		String salt = saltSource.getSystemWideSalt();
		String username = ((UserDetails) command).getUsername();
		String password = ((UserDetails) command).getPassword();

		PasswordGenerator passwordGenerator = new PasswordGenerator(salt,
				password);

		log.debug("Password: {}", passwordGenerator.getPassword());
		// register user here

		//return new ModelAndView();
		return new ModelAndView(new RedirectView(getSuccessView()));
	}

	protected Object formBackingObject(HttpServletRequest request)
			throws ServletException {
		UserDetails userDetails = new UserDetails();
		userDetails.setUsername("admin");
		return userDetails;
	}

	public void setDaoAuthenticationProvider(DaoAuthenticationProvider value) {
		daoAuthenticationProvider = value;
	}

}