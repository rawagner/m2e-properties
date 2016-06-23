package org.jboss.tools.m2e.properties.core.internal.management;

import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;

public class PropertiesManager {

	public void updateProperties(final IMavenProjectFacade mavenProjectFacade, final Properties properties, final boolean isOffline, 
			final boolean isForceUpdate, IProgressMonitor monitor) throws CoreException {
		if (mavenProjectFacade == null) {
			return;
		}
		
		final IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

		IProject project = mavenProjectFacade.getProject();

		final ResolverConfiguration configuration = configurationManager.getResolverConfiguration(project);
		configuration.setProperties(properties);
		boolean isSet = configurationManager.setResolverConfiguration(project,
				configuration);
		if (isSet && properties != null && !properties.isEmpty()) {
			MavenUpdateRequest request = new MavenUpdateRequest(project,
					isOffline, isForceUpdate);
			configurationManager.updateProjectConfiguration(request, monitor);
		}

	}

}
