package org.jboss.tools.m2e.properties.ui.internal.dialog;

import java.util.EventObject;
import java.util.Properties;
import java.util.regex.Pattern;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.m2e.properties.ui.Messages;

public class ChangePropertiesDialog extends TitleAreaDialog implements IMenuListener{
	
	private IMavenProjectFacade facade;
	private Properties changedProperties;
	private Properties defaultProperties;
	private Properties defaultPropertiesBackup;
	private TableViewer defaultPropertiesTableViewer;
	private TableViewer changedPropertiesTableViewer;
	private ChangePropertiesDefaultAction changeAction = new ChangePropertiesDefaultAction();
	private PropertiesFilter propertiesFilter = new PropertiesFilter();
	
	private TableViewerColumn changedValuesColumn;
	private TableViewerColumn valuesColumn;
	private TableViewerColumn changedPropertiesColumn;
	private TableViewerColumn propertiesColumn;

	public ChangePropertiesDialog(Shell parentShell, IMavenProjectFacade facade, Properties properties, Properties defaultProperties) {
		super(parentShell);
		setShellStyle(super.getShellStyle() | SWT.RESIZE | SWT.MODELESS);
		this.facade = facade;
		if(properties == null){
			properties = new Properties();
		}
		this.changedProperties = properties;
		this.defaultProperties = defaultProperties;
		this.defaultPropertiesBackup = (Properties)defaultProperties.clone();
		for(Object key: changedProperties.keySet()){
			defaultProperties.remove(key);
		}
		
	}
	
	public Properties getProperties(){
		return changedProperties;
	}
	

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		Composite container = new Composite(area, SWT.NONE);
        container.setEnabled(true);
        
		GridLayout layout = new GridLayout(3, false);
		layout.marginLeft = 12;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		setTitle(Messages.ChangePropertiesDialog_Change_Maven_Properties);
		setMessage(NLS.bind(Messages.ChangePropertiesDialog_Change_Maven_Properties_Message,facade.getProject().getName()));
		boolean hasProperties = !defaultProperties.isEmpty();

		if (hasProperties) {
			createFilterText(container);
			Button button = new Button(container, SWT.NONE);
			button.setLayoutData(new GridData(SWT.FILL, SWT.UP,
					false, false, 1, 1));
			button.setText(Messages.ChangePropertiesDialog_Restore_Defaults);
			button.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					defaultProperties = (Properties)defaultPropertiesBackup.clone();
					changedProperties = new Properties();
					defaultPropertiesTableViewer.setInput(defaultProperties);
					defaultPropertiesTableViewer.refresh();
					changedPropertiesTableViewer.setInput(changedProperties);
					changedPropertiesTableViewer.refresh();
				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
			displayPropertiesTable(container);
			
			return button;
		}
		return area;
	}
	
	private void createFilterText(Composite container){
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		Text t = new Text(container, SWT.NONE);
		t.setFocus();
		t.setLayoutData(gd);
		t.setMessage("filter properties");
		t.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent ke) {
				String filter = ((Text)ke.widget).getText();
				if(filter.trim().isEmpty()){
					propertiesFilter.setSearchString("");
				} else {
					propertiesFilter.setSearchString(filter);
				}
				defaultPropertiesTableViewer.refresh();
				changedPropertiesTableViewer.refresh();
		      }
		});
	}
	
	private void displayPropertiesTable(Composite container) {
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 4);
		gd.heightHint = 200;
		gd.widthHint = 500;
		
		GridData labelData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd.heightHint = 200;
		
		Label l = new Label(container, SWT.LEFT);
		l.setText("Project properties:");
		l.setLayoutData(labelData);
		defaultPropertiesTableViewer = new TableViewer(container,SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		l = new Label(container, SWT.LEFT);
		l.setText("Changed project properties:");
		l.setLayoutData(labelData);
		changedPropertiesTableViewer = new TableViewer(container,SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		
		Table table = defaultPropertiesTableViewer.getTable();
		table.setLayoutData(gd);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		
		table = changedPropertiesTableViewer.getTable();
		table.setLayoutData(gd);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(defaultPropertiesTableViewer, new FocusCellOwnerDrawHighlighter(defaultPropertiesTableViewer));

		ColumnViewerEditorActivationStrategy activationSupport = new ColumnViewerEditorActivationStrategy(defaultPropertiesTableViewer) {
		    protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
		        // Enable editor only with mouse double click
		        if (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION) {
		            EventObject source = event.sourceEvent;
		            if (source instanceof MouseEvent && ((MouseEvent)source).button == 3){
		                return false;
		            }
		            return true;
		        }

		        return false;
		    }
		};

		TableViewerEditor.create(defaultPropertiesTableViewer, focusCellManager, activationSupport, ColumnViewerEditor.TABBING_HORIZONTAL | 
		    ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | 
		    ColumnViewerEditor.TABBING_VERTICAL |
		    ColumnViewerEditor.TABBING_HORIZONTAL |
		    ColumnViewerEditor.KEYBOARD_ACTIVATION);
		
		propertiesColumn = new TableViewerColumn(defaultPropertiesTableViewer, SWT.NONE);
		propertiesColumn.getColumn().setWidth(200);
		propertiesColumn.getColumn().setText(Messages.ChangePropertiesDialog_Property);
		
		propertiesColumn.setLabelProvider(new ColumnLabelProvider() {
			  @Override
			  public String getText(Object element) {
				return (String)element;
			  }
		});
		
		
		changedPropertiesColumn = new TableViewerColumn(changedPropertiesTableViewer, SWT.NONE);
		changedPropertiesColumn.getColumn().setWidth(200);
		changedPropertiesColumn.getColumn().setText(Messages.ChangePropertiesDialog_Property);
		
		changedPropertiesColumn.setLabelProvider(new ColumnLabelProvider() {
			  @Override
			  public String getText(Object element) {
				return (String)element;
			  }
		});
		
		valuesColumn = new TableViewerColumn(defaultPropertiesTableViewer, SWT.NONE);
		
		valuesColumn.getColumn().setWidth(200);
		valuesColumn.getColumn().setText(Messages.ChangePropertiesDialog_Property_Value);
		valuesColumn.setEditingSupport(new PropertiesEditingSupport(defaultPropertiesTableViewer, changedPropertiesTableViewer));
		
		valuesColumn.setLabelProvider(new ColumnLabelProvider() {
			  @Override
			  public String getText(Object element) {
				return defaultProperties.getProperty((String)element);
			  }
		});
		
		
		changedValuesColumn = new TableViewerColumn(changedPropertiesTableViewer, SWT.NONE);
		
		changedValuesColumn.getColumn().setWidth(200);
		changedValuesColumn.getColumn().setText(Messages.ChangePropertiesDialog_Property_Value);
		changedValuesColumn.setLabelProvider(new ColumnLabelProvider() {
			  @Override
			  public String getText(Object element) {
				return changedProperties.getProperty((String)element);
			  }
		});
		
		defaultPropertiesTableViewer.setContentProvider(new PropertyContentProvider());
		changedPropertiesTableViewer.setContentProvider(new PropertyContentProvider());
		
		defaultPropertiesTableViewer.setInput(defaultProperties);
		defaultPropertiesTableViewer.addFilter(propertiesFilter);
		
		changedPropertiesTableViewer.setInput(changedProperties);
		changedPropertiesTableViewer.addFilter(propertiesFilter);
		
		createMenu();
	}
	
	class PropertiesFilter extends ViewerFilter {
		
		private String searchString;
		
		public void setSearchString(String s){
			this.searchString = ".*" + s + ".*";
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (searchString == null || searchString.length() == 0) {
				return true;
			}
			Pattern pattern = Pattern.compile(searchString, Pattern.CASE_INSENSITIVE);
			String s = (String) element;
			return pattern.matcher(s).matches();
		}
		
	}
	
	class PropertiesEditingSupport extends EditingSupport {

		  private final TableViewer viewer;
		  private final TableViewer changedPropsViewer;
		  private final CellEditor editor;

		  public PropertiesEditingSupport(TableViewer viewer, TableViewer changedPropsViewer) {
		    super(viewer);
		    this.viewer = viewer;
		    this.editor = new TextCellEditor(viewer.getTable());
		    this.changedPropsViewer = changedPropsViewer;
		  }

		  @Override
		  protected CellEditor getCellEditor(Object element) {
		    return editor;
		  }

		  @Override
		  protected boolean canEdit(Object element) {
		    return true;
		  }

		  @Override
		  protected Object getValue(Object element) {
			Properties properties = (Properties)defaultPropertiesTableViewer.getInput();
		    return properties.getProperty((String)element);
		  }

		  @Override
		  protected void setValue(Object element, Object userInputValue) {
			  if(defaultProperties.get(element).equals(userInputValue)){
				  return;
			  }
			  if(changedProperties == null){
				  changedProperties = new Properties();
			  }
			  changedProperties.setProperty((String)element, String.valueOf(userInputValue));
			  defaultProperties.remove((String)element);
			  changedPropsViewer.refresh();
			  viewer.remove(element);
			  viewer.refresh();
		  }
		} 
	
	class PropertyContentProvider implements IStructuredContentProvider{

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
			// TODO Auto-generated method stub
		}

		@Override
		public Object[] getElements(Object arg0) {
			Properties properties = (Properties) arg0;
			return properties.keySet().toArray(new String[properties.keySet().size()]);
		}
		
	}
	
	private void createMenu() {
		MenuManager menuMgr = new MenuManager();
		Menu contextMenu = menuMgr.createContextMenu(changedPropertiesTableViewer
				.getControl());
		menuMgr.addMenuListener(this);
		changedPropertiesTableViewer.getControl().setMenu(contextMenu);
		menuMgr.setRemoveAllWhenShown(true);
	}

	@Override
	public void menuAboutToShow(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) changedPropertiesTableViewer.getSelection();
		if (!selection.isEmpty()) {
			changeAction.setText(Messages.ChangePropertiesDialog_Restore_Default);
			manager.add(changeAction);
		}
		
	}
	
	private class ChangePropertiesDefaultAction extends Action {

		@Override
		public void run() {
			IStructuredSelection selection = (IStructuredSelection) changedPropertiesTableViewer.getSelection();
			if (!selection.isEmpty()) {
				String propertyKey = (String)selection.getFirstElement();
				changedProperties.remove(propertyKey);
				changedPropertiesTableViewer.remove(propertyKey);
				changedPropertiesTableViewer.refresh();
				
				defaultProperties.put(propertyKey,defaultPropertiesBackup.get(propertyKey));
				defaultPropertiesTableViewer.refresh();
			}
			super.run();
		}
	}

}
