package org.jboss.tools.m2e.properties.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.m2e.properties.core.internal.management.PropertiesManager;
import org.osgi.framework.BundleContext;

public class MavenPropertiesCoreActivator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.m2e.properties.core"; //$NON-NLS-1$
	
	// The shared instance
	private static MavenPropertiesCoreActivator plugin;
	
	private PropertiesManager propertiesManager;

	/**
	 * The constructor
	 */
	public MavenPropertiesCoreActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		this.propertiesManager = new PropertiesManager();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MavenPropertiesCoreActivator getDefault() {
		return plugin;
	}

	public static IStatus getStatus(String message) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message);
	}
	
	public static IStatus getStatus(String message, Throwable e) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message,e);
	}

	public static void log(Throwable e) {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, e.getLocalizedMessage(), e);
		getDefault().getLog().log(status);
	}

	public static void log(String message) {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message);
		getDefault().getLog().log(status);
	}
	
	public PropertiesManager getPropertiesManager(){
		return propertiesManager;
	}
}
