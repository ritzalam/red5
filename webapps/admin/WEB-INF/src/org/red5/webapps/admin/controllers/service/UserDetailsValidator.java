package org.red5.webapps.admin.controllers.service;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class UserDetailsValidator implements Validator {

	private int minLength = 4;

	public boolean supports(Class clazz) {
		return UserDetails.class.equals(clazz);
	}

	public void validate(Object obj, Errors errors) {
		UserDetails ud = (UserDetails) obj;
		if (ud == null) {
			errors.rejectValue("percentage", "error.not-specified", null,
					"Value required.");
		} else {

			if (ud.getUsername().equals("")) {
				errors.rejectValue("username", "error.missing-username",
						new Object[] {}, "Username Required.");
			}
			if (ud.getPassword().equals("")) {
				errors.rejectValue("password", "error.missing-password",
						new Object[] {}, "Password Required.");
			}

			if (ud.getPassword().length() < minLength) {
				errors.rejectValue("password", "error.too-low",
						new Object[] { new Integer(minLength) },
						"Password Length Is Too Small.");
			}
		}
	}

	public void setMinLength(int i) {
		minLength = i;
	}

	public int getMinLength() {
		return minLength;
	}
}