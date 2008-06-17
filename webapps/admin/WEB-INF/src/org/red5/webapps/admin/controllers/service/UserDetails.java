package org.red5.webapps.admin.controllers.service;

import org.springframework.security.GrantedAuthority;


public class UserDetails implements org.springframework.security.userdetails.UserDetails {
	
	private final static long serialVersionUID = 2801983490L;
	
	private GrantedAuthority[] authorities = new GrantedAuthority[1];
	
    private int userid;	

	private String username;

	private String password;
	
	private Boolean enabled;

	public UserDetails() {		
	}

	public UserDetails(int userid) {
		this.userid = userid;
	}
	
	public int getUserid() {
		return userid;
	}

	public void setUserid(int userid) {
		this.userid = userid;
	}

	public void setUsername(String value) {
		username = value;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(String value) {
		password = value;
	}

	public String getPassword() {
		return password;
	}

    public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public void setEnabled(Integer enabledInt) {
		this.enabled = (enabledInt == 1);
	}

	public void setEnabled(String enabledStr) {
		this.enabled = "enabled".equals(enabledStr);
	}
	
	public void setAuthorities(GrantedAuthority[] authorities) {
		this.authorities = authorities;
	}

	public GrantedAuthority[] getAuthorities() {
		return authorities;
	}

	public boolean isAccountNonExpired() {
		return true;
	}

	public boolean isAccountNonLocked() {
		return true;
	}

	public boolean isCredentialsNonExpired() {
		return true;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/**
     * Returns a hash code value for the object.  This implementation computes
     * a hash code value based on the id fields in this object.
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return userid;
    }	

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserDetails)) {
            return false;
        }
        UserDetails other = (UserDetails) object;
        if (this.userid != other.userid) {
        	return false;
        }
        return true;
    }
    
}