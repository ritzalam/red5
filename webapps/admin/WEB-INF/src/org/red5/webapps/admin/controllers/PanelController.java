package org.red5.webapps.admin.controllers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.providers.dao.DaoAuthenticationProvider;
import org.acegisecurity.userdetails.memory.InMemoryDaoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class PanelController implements Controller {

	protected static Logger log = LoggerFactory.getLogger(PanelController.class);	
	
	private Boolean hasPass = true;

	private ProviderManager authenticationManager;

	private DaoAuthenticationProvider daoAuthenticationProvider;

	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		log.debug("handle request");
		
		InMemoryDaoImpl detailsService = (InMemoryDaoImpl) daoAuthenticationProvider
				.getUserDetailsService();

		if (detailsService.getUserMap().getUserCount() > 0) {
			log.debug("Creating adminPanel");
			return new ModelAndView("panel");
		} else {
			log.debug("Redirecting to register");
			return new ModelAndView("redirect:register.jsp");
		}
	}

	public ModelAndView doRequest(HttpServletRequest request,
			HttpServletResponse response) {
		return new ModelAndView();
	}

	public void setDaoAuthenticationProvider(DaoAuthenticationProvider value) {
		log.debug("setDaoAuthenticationProvider");
		daoAuthenticationProvider = value;
	}

	public void setAuthenticationManager(ProviderManager value) {
		log.debug("setAuthenticationManager");
		authenticationManager = value;
	}
}