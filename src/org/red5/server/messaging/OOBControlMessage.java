package org.red5.server.messaging;

import java.io.Serializable;
import java.util.Map;

/**
 * Out-of-band control message used by inter-components communication
 * which are connected with pipes.
 * <tt>'Target'</tt> is used to represent the receiver who may be
 * interested for receiving. It's a string of any form.
 * XXX shall we design a standard form for Target, like "class.instance"?
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class OOBControlMessage implements Serializable {
	private static final long serialVersionUID = -6037348177653934300L;

	private String target;
	private String serviceName;
	private Map serviceParamMap;
	
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public Map getServiceParamMap() {
		return serviceParamMap;
	}
	public void setServiceParamMap(Map serviceParamMap) {
		this.serviceParamMap = serviceParamMap;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
}
