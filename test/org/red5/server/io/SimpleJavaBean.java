package org.red5.server.io;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright © 2006 by respective authors. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

public class SimpleJavaBean {

	private String nameOfBean = "jeff";

	/**
     * Getter for property 'nameOfBean'.
     *
     * @return Value for property 'nameOfBean'.
     */
    public String getNameOfBean() {
		return nameOfBean;
	}

	/**
     * Setter for property 'nameOfBean'.
     *
     * @param nameOfBean Value to set for property 'nameOfBean'.
     */
    public void setNameOfBean(String nameOfBean) {
		this.nameOfBean = nameOfBean;
	}

	/** {@inheritDoc} */
    @Override
	public boolean equals(Object obj){
		if(obj instanceof SimpleJavaBean){
			SimpleJavaBean sjb = (SimpleJavaBean) obj;
			return sjb.getNameOfBean().equals(sjb.getNameOfBean());
		}
		return false;
	}


}
