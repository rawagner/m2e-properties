package org.jboss.tools.m2e.properties.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.m2e.properties.ui.messages"; //$NON-NLS-1$

	public static String ChangePropertiesDialog_Change_Maven_Properties;
	public static String ChangePropertiesDialog_Property;
	public static String ChangePropertiesDialog_Property_Value;
	public static String PropertiesChangeHandler_Update_Properties_Job;
	public static String ChangePropertiesDialog_Change_Maven_Properties_Message;
	public static String ChangePropertiesDialog_Restore_Defaults;
	public static String ChangePropertiesDialog_Restore_Default;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
