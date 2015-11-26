package org.jboss.tools.m2e.properties.ui.internal.actions;

import java.util.Properties;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.m2e.properties.core.MavenPropertiesCoreActivator;
import org.jboss.tools.m2e.properties.core.internal.management.PropertiesManager;
import org.jboss.tools.m2e.properties.ui.Messages;
import org.jboss.tools.m2e.properties.ui.internal.dialog.ChangePropertiesDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesChangeHandler extends AbstractHandler {
	
	private static final Logger log = LoggerFactory.getLogger(PropertiesChangeHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil
				.getActiveWorkbenchWindowChecked(event);
		IProject[] projects = getSelectedProjects(event);
		if (projects.length == 1) {
			return execute(window.getShell(), projects[0]);
		}
		return null;
	}

	private IProject[] getSelectedProjects(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IProject[] projects = SelectionUtil.getProjects(selection, false);
		if (projects.length == 0) {
			IEditorInput input = HandlerUtil.getActiveEditorInput(event);
			if (input instanceof IFileEditorInput) {
				IFileEditorInput fileInput = (IFileEditorInput) input;
				projects = new IProject[] { fileInput.getFile().getProject() };
			}
		}
		return projects;
	}

	public IStatus execute(Shell shell, IProject project)
			throws ExecutionException {
		IMavenProjectFacade facade = getMavenProject(project);
		if (facade == null) {
			return null;
		}

		GetPropertiesJob job = new GetPropertiesJob(facade);
		job.addJobChangeListener(onPropertiesFetched(job, facade, shell));
		job.setUser(true);
		job.schedule();
		return Status.OK_STATUS;
	}

	private IJobChangeListener onPropertiesFetched(
			final GetPropertiesJob getPropertiesJob,
			final IMavenProjectFacade facade, final Shell shell) {

		return new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				if (getPropertiesJob.getResult().isOK()) {
					shell.getDisplay().syncExec(new Runnable() {

						public void run() {
							Properties properties = getPropertiesJob.getProperties();
							Properties propertiesBackup = null;
							if(properties!=null){
								propertiesBackup = (Properties)properties.clone();
							}
							Properties defaultProperties = getPropertiesJob.getDefaultProperties();
							final ChangePropertiesDialog dialog = new ChangePropertiesDialog(shell, facade, properties,defaultProperties);
							if (dialog.open() == Dialog.OK) {
								if(dialog.getProperties() != null && !dialog.getProperties().equals(propertiesBackup)){
									PropertiesManager pm = MavenPropertiesCoreActivator.getDefault().getPropertiesManager();
									Job job = new UpdatePropertiesJob(facade, dialog.getProperties(),pm);
									job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
									job.schedule();
								}
								
							}
						}
					});

				}
			}
		};
	}
	
	class UpdatePropertiesJob extends WorkspaceJob{
		 
		private Properties properties;
		private PropertiesManager manager;
		private IMavenProjectFacade facade;

		public UpdatePropertiesJob(IMavenProjectFacade facade, Properties properties, PropertiesManager propertiesManager) {
			super("Change Properties");
			this.properties = properties;
			this.manager = propertiesManager;
			this.facade = facade;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
			try {
				SubMonitor progress = SubMonitor.convert(monitor, Messages.PropertiesChangeHandler_Update_Properties_Job, 100);
				manager.updateProperties(facade, properties, false, false, progress.newChild(100));
			} catch (CoreException ex) {
				return ex.getStatus();
			}
			return Status.OK_STATUS;
		}
		
	}

	private IMavenProjectFacade getMavenProject(IProject project) {
		IMavenProjectFacade facade = null;
		try {
			IProgressMonitor monitor = new NullProgressMonitor();
			if (project != null && project.isAccessible()
					&& project.hasNature(IMavenConstants.NATURE_ID)) {
				IFile pom = project.getFile(IMavenConstants.POM_FILE_NAME);
				facade = MavenPlugin.getMavenProjectRegistry().create(pom,
						true, monitor);
			}
		} catch (CoreException e) {
			log.error("Unable to select Maven project", e);
		}

		return facade;
	}

	class GetPropertiesJob extends Job {

		private IMavenProjectFacade facade;
		private Properties properties;
		private Properties defaultProperties;

		public GetPropertiesJob(final IMavenProjectFacade facade) {
			super("Loading properties");
			this.facade = facade;
		}

		@Override
		protected IStatus run(IProgressMonitor arg0) {
			ResolverConfiguration resolverConfiguration = MavenPlugin.getProjectConfigurationManager()
					.getResolverConfiguration(facade.getProject());
			
			properties = resolverConfiguration.getProperties();
			IProgressMonitor monitor = new NullProgressMonitor();
			try {
				defaultProperties = (Properties)facade.getMavenProject(monitor).getProperties().clone();
			} catch (CoreException e) {
				log.error("Unable to get Maven project", e);
			}
			//no, we want to store only properties we changed
			//if(properties == null || properties.isEmpty()){
		//		properties = defaultProperties;
		//	}
			return Status.OK_STATUS;
		}

		public Properties getProperties() {
			return properties;
		}
		
		public Properties getDefaultProperties(){
			return defaultProperties;
		}

	}
}
