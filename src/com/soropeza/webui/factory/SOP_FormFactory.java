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
