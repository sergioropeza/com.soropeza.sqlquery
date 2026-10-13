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

import org.adempiere.base.AnnotationBasedModelFactory;
import org.adempiere.base.IModelFactory;
import org.osgi.service.component.annotations.Component;

import com.soropeza.webui.apps.model.X_SOP_SQLQueryHistory;

/**
 * Model factory for the SOP_* tables of this plugin.
 * Classes are discovered through the @Model annotation of the generated X_ classes.
 */
@Component(immediate = true, service = IModelFactory.class, property = "service.ranking:Integer=1")
public class SOP_ModelFactory extends AnnotationBasedModelFactory {

	@Override
	protected String[] getPackages() {
		return new String[] {X_SOP_SQLQueryHistory.class.getPackageName()};
	}
}
