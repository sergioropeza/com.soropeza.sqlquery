/***********************************************************************
 * This file is part of the com.soropeza.sqlquery plugin for           *
 * iDempiere ERP - http://www.idempiere.org                            *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * Contributors:                                                       *
 * - Sergio Oropeza - com.soropeza                                     *
 **********************************************************************/
package com.soropeza.webui.factory;

import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;

import com.soropeza.webui.apps.form.WSQLQueryEnhanced;


public class SOP_FormFactory implements IFormFactory {

	@Override
	public ADForm newFormInstance(String formName) {
		if (formName.equals("org.adempiere.webui.apps.form.WSQLQuery")) {
			return new WSQLQueryEnhanced();
		}
		return null;
	}
}
