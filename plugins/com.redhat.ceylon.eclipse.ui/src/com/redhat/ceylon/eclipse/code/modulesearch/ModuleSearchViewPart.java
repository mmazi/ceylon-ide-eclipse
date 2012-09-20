package com.redhat.ceylon.eclipse.code.modulesearch;

import static com.redhat.ceylon.eclipse.code.hover.DocHover.addImageAndLabel;
import static com.redhat.ceylon.eclipse.code.hover.DocHover.fileUrl;
import static com.redhat.ceylon.eclipse.code.outline.CeylonLabelProvider.getLabel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.ui.actions.CollapseAllAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.github.rjeschke.txtmark.Configuration;
import com.github.rjeschke.txtmark.Processor;
import com.redhat.ceylon.common.config.Repositories.Repository;
import com.redhat.ceylon.eclipse.code.hover.DocHover;
import com.redhat.ceylon.eclipse.code.hover.DocHover.CeylonBlockEmitter;
import com.redhat.ceylon.eclipse.ui.CeylonPlugin;
import com.redhat.ceylon.eclipse.ui.CeylonResources;

@SuppressWarnings("restriction")
public class ModuleSearchViewPart extends ViewPart {
    
    private class RemoveSelectedAction extends Action implements ISelectionChangedListener {

        public RemoveSelectedAction() {
            super("Remove Selected");
            setToolTipText("Remove Selected");
            setEnabled(false);
            
            ISharedImages workbenchImages = PlatformUI.getWorkbench().getSharedImages();
            setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));
            setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));

            moduleTreeViewer.addSelectionChangedListener(this);
        }

        @Override
        public void run() {
            List<?> selectedElements = ((IStructuredSelection) moduleTreeViewer.getSelection()).toList();
            if (selectedElements != null) {
                int lastSelectedModuleIndex = -1;
                for (Object selectedElement : selectedElements) {
                    int selectedModuleIndex = moduleSearchManager.getModules().indexOf(selectedElement);
                    if (selectedModuleIndex != -1) {
                        lastSelectedModuleIndex = selectedModuleIndex;
                    }
                }

                if( lastSelectedModuleIndex != -1 ) {
                    moduleSearchManager.remove(selectedElements);

                    int nextSelectedModuleIndex = -1;
                    if (moduleSearchManager.getModules() != null) {
                        if (lastSelectedModuleIndex > moduleSearchManager.getModules().size() - 1) {
                            nextSelectedModuleIndex = moduleSearchManager.getModules().size() - 1;
                        } else {
                            nextSelectedModuleIndex = lastSelectedModuleIndex;
                        }
                    }

                    update(false);

                    if (nextSelectedModuleIndex != -1) {
                        moduleTreeViewer.setSelection(new StructuredSelection(moduleSearchManager.getModules().get(nextSelectedModuleIndex)));
                    }
                }
            }
        }

        @Override
        public void selectionChanged(SelectionChangedEvent e) {
            boolean isEnabled = false;
            
            List<?> selectedElements = ((IStructuredSelection) e.getSelection()).toList();
            if( selectedElements != null ) {
                for( Object selectedElement : selectedElements ) {
                    if( selectedElement instanceof ModuleNode ) {
                        isEnabled = true;
                        break;
                    }
                }
            }
            
            setEnabled(isEnabled);
        }

    }
    
    private class RemoveAllAction extends Action {

        public RemoveAllAction() {
            super("Remove All");
            setToolTipText("Remove All");
            
            ISharedImages workbenchImages = PlatformUI.getWorkbench().getSharedImages();
            setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL));
            setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL));
        }

        @Override
        public void run() {
            moduleSearchManager.clear();
            update(true);
            searchCombo.setText("");
        }

    }
    
    private class ExpandAllAction extends Action {

        public ExpandAllAction() {
            super("Expand All");
            setToolTipText("Expand All");

            ImageDescriptor expandAllImage = CeylonPlugin.getInstance().getImageRegistry().getDescriptor(CeylonResources.EXPAND_ALL);
            setImageDescriptor(expandAllImage);
            setHoverImageDescriptor(expandAllImage);
        }

        @Override
        public void run() {
            moduleTreeViewer.expandAll();
        }

    }
    
    private class FetchNextAction extends Action {

        public FetchNextAction() {
            super("Fetch Next");
            setToolTipText("Fetch Next");
            setEnabled(false);

            ImageDescriptor fetchNextImage = CeylonPlugin.getInstance().getImageRegistry().getDescriptor(CeylonResources.PAGING);
            setHoverImageDescriptor(fetchNextImage);
            setImageDescriptor(fetchNextImage);
        }

        @Override
        public void run() {
            updateBeforeSearch(false);
            moduleSearchManager.fetchNextModules();
        }

    }
    
    private class CopyImportModuleAction extends Action implements ISelectionChangedListener {

        public CopyImportModuleAction() {
            super("Copy Import Module");
            setToolTipText("Copy Import Module");
            setEnabled(false);
            
            ISharedImages workbenchImages = PlatformUI.getWorkbench().getSharedImages();
            setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
            setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
            
            moduleTreeViewer.addSelectionChangedListener(this);
        }

        @Override
        public void run() {
            Object selectedElement = ((IStructuredSelection) moduleTreeViewer.getSelection()).getFirstElement();

            ModuleVersionNode versionNode = null;
            if (selectedElement instanceof ModuleNode) {
                versionNode = ((ModuleNode) selectedElement).getLastVersion();
            } else if (selectedElement instanceof ModuleVersionNode) {
                versionNode = (ModuleVersionNode) selectedElement;
            }

            if (versionNode != null) {
                String importStatement = "import " + versionNode.getModule().getName() + " '" + versionNode.getVersion() + "';";
                Clipboard clipboard = new Clipboard(parent.getDisplay());
                clipboard.setContents(new String[] { importStatement }, new Transfer[] { TextTransfer.getInstance() });
                clipboard.dispose();
            }
        }

        @Override
        public void selectionChanged(SelectionChangedEvent e) {
            setEnabled(!e.getSelection().isEmpty());
        }
        
    }
    
    private class ShowDocAction extends Action {
        
        private static final String IS_CHECKED = "ModuleSearch.ShowDocAction.isChecked";

        public ShowDocAction() {
            super("Show/Hide Documentation", AS_CHECK_BOX);
            setToolTipText("Show/Hide Documentation");

            ImageDescriptor showDocImage = CeylonPlugin.getInstance().getImageRegistry().getDescriptor(CeylonResources.SHOW_DOC);
            setImageDescriptor(showDocImage);
            setHoverImageDescriptor(showDocImage);
            
            setChecked(CeylonPlugin.getInstance().getPreferenceStore().getBoolean(IS_CHECKED));
            run();
        }

        @Override
        public void run() {
            if (isChecked()) {
                sashForm.setMaximizedControl(null);
            } else {
                sashForm.setMaximizedControl(moduleTreeViewer.getTree());
            }
            CeylonPlugin.getInstance().getPreferenceStore().setValue(IS_CHECKED, isChecked());
        }

    }
    
    private class ShowRepositoriesAction extends Action {
        
        public ShowRepositoriesAction() {
            super("Show Repositories");
            setToolTipText("Show Repositories");

            ImageDescriptor showRepositoriesImage = CeylonPlugin.getInstance().getImageRegistry().getDescriptor(CeylonResources.REPOSITORIES);
            setImageDescriptor(showRepositoriesImage);
            setHoverImageDescriptor(showRepositoriesImage);
        }

        @Override
        public void run() {
            ShowRepositoriesDialog showRepositoriesDialog = new ShowRepositoriesDialog();
            showRepositoriesDialog.open();
        }

    }
    
    private class ShowRepositoriesDialog extends TitleAreaDialog {

        public ShowRepositoriesDialog() {
            super(parent.getShell());
        }
        
        @Override
        public void create() {
            setHelpAvailable(false);
            super.create();
            setTitle("Ceylon repositories");
            setMessage("The following repositories will be searched.\nThe configuration file is located in the user home directory (~/.ceylon/config).");
        }
        
        @Override
        protected Control createDialogArea(Composite parent) {
            parent.setLayout(new GridLayout(1, false));

            TableViewer tableViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

            TableViewerColumn colName = new TableViewerColumn(tableViewer, SWT.NONE);
            colName.getColumn().setWidth(200);
            colName.getColumn().setText("Name");
            colName.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((Repository)element).getName();
                }
            });

            TableViewerColumn colUrl = new TableViewerColumn(tableViewer, SWT.NONE);
            colUrl.getColumn().setWidth(200);
            colUrl.getColumn().setText("URL");
            colUrl.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((Repository)element).getUrl();
                }
            });            

            tableViewer.setContentProvider(ArrayContentProvider.getInstance());
            tableViewer.setInput(moduleSearchManager.getGlobalLookupRepositories());
            tableViewer.getTable().setHeaderVisible(true);
            tableViewer.getTable().setLinesVisible(true);

            GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableViewer.getTable());

            return parent;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }

    }

    private ModuleSearchManager moduleSearchManager = new ModuleSearchManager(this);
    
    private RemoveSelectedAction removeSelectedAction;
    private RemoveAllAction removeAllAction;
    private ExpandAllAction expandAllAction;
    private CollapseAllAction collapseAllAction;
    private FetchNextAction fetchNextAction;
    private CopyImportModuleAction copyImportModuleAction;
    private ShowDocAction showDocAction;
    private ShowRepositoriesAction showRepositoriesAction;
    
    private Composite parent;
    private Combo searchCombo;
    private Button searchButton;
    private Link searchInfo;
    private SashForm sashForm;
    private TreeViewer moduleTreeViewer;
    private Browser docBrowser;
    private String docStyleSheet = DocHover.getStyleSheet();
    private RGB docForegroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_INFO_FOREGROUND).getRGB();
    private RGB docBackgroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND).getRGB();
    private List<String> queryHistory = new ArrayList<String>();
    
    @Override
    public void setFocus() {
        searchCombo.setFocus();
    }

    @Override
    public void createPartControl(Composite parent) {
        this.parent = parent;
        
        initSearchCombo();       
        initSearchButton();
        initSearchInfo();
        initSashForm();
        initModuleTreeViewer();
        initDocBrowser();
        initActions();
        initLayout();
    }

    private void initSearchCombo() {
        searchCombo = new Combo(parent, SWT.SINGLE | SWT.BORDER);
        searchCombo.setVisibleItemCount(10);
        searchCombo.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    updateBeforeSearch(true);
                    moduleSearchManager.searchModules(searchCombo.getText());
                }
            }
        });
    }

    private void initSearchButton() {
        searchButton = new Button(parent, SWT.PUSH);
        searchButton.setText("&Search");
        searchButton.setAlignment(SWT.CENTER);
        searchButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
                updateBeforeSearch(true);
                moduleSearchManager.searchModules(searchCombo.getText());
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
        });
    }

    private void initSearchInfo() {
        searchInfo = new Link(parent, 0);
        searchInfo.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                updateBeforeSearch(false);
                moduleSearchManager.fetchNextModules();
            }
        });
        updateInfoLabel();
    }

    private void initSashForm() {
        sashForm = new SashForm(parent, SWT.HORIZONTAL);        
    }

    private void initModuleTreeViewer() {
        moduleTreeViewer = new TreeViewer(sashForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        moduleTreeViewer.setContentProvider(new ModuleSearchViewContentProvider());
        moduleTreeViewer.setLabelProvider(new ModuleSearchViewLabelProvider());
        moduleTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent e) {
                if( e.getSelection() instanceof IStructuredSelection ) {
                    Object selectedElement = ((IStructuredSelection)e.getSelection()).getFirstElement();
                    if( selectedElement != null ) {
                        boolean isExpanded = moduleTreeViewer.getExpandedState(selectedElement);
                        moduleTreeViewer.setExpandedState(selectedElement, !isExpanded);
                    }
                }
            }
        });
    }

    private void initDocBrowser() {
        docBrowser = new Browser(sashForm, SWT.NONE);
        docBrowser.setMenu(new Menu(parent.getShell(), SWT.NONE));
        
        docBrowser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(LocationEvent e) {
                if ( e.location.startsWith("module:") ) {
                    e.doit = false;
                    
                    String[] split = e.location.split(":");
                    String moduleName = split[1];
                    String moduleVersion = split[2];
                    
                    moduleSearchManager.fetchDocumentation(moduleName, moduleVersion);
                }
            }
        });
        
        moduleTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent e) {
                updateDoc();                
            }
        });
        
        updateDoc();
    }

    private void initActions() {
        removeSelectedAction = new RemoveSelectedAction();
        removeAllAction = new RemoveAllAction();
        expandAllAction = new ExpandAllAction();
        collapseAllAction = new CollapseAllAction(moduleTreeViewer);
        fetchNextAction = new FetchNextAction();
        copyImportModuleAction = new CopyImportModuleAction();
        showDocAction = new ShowDocAction();
        showRepositoriesAction = new ShowRepositoriesAction();

        IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
        toolBarManager.add(fetchNextAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(removeSelectedAction);
        toolBarManager.add(removeAllAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(expandAllAction);
        toolBarManager.add(collapseAllAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(showDocAction);
        toolBarManager.add(showRepositoriesAction);

        MenuManager menuManager = new MenuManager();
        menuManager.add(copyImportModuleAction);
        menuManager.add(new Separator());
        menuManager.add(removeSelectedAction);
        menuManager.add(removeAllAction);
        menuManager.add(new Separator());
        menuManager.add(expandAllAction);
        menuManager.add(collapseAllAction);

        Menu menu = menuManager.createContextMenu(moduleTreeViewer.getTree());
        moduleTreeViewer.getTree().setMenu(menu);
    }
    
    private void initLayout() {
        parent.setLayout(new GridLayout(2, false));
        
        GridDataFactory.swtDefaults().hint(300, SWT.DEFAULT).applyTo(searchCombo);
        GridDataFactory.swtDefaults().hint(100, SWT.DEFAULT).applyTo(searchButton);
        GridDataFactory.swtDefaults().span(2, 1).applyTo(searchInfo);
        GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).span(2, 1).applyTo(sashForm);
    }

    public void update(boolean newModel) {
        if (newModel) {
            moduleTreeViewer.setInput(moduleSearchManager.getModules());
        } else {
            moduleTreeViewer.refresh();
        }
        
        updateInfoLabel();
        updateEnabledState();
        updateFocusAndSelection(newModel);
        updateSearchComboHistory(newModel);
        parent.setCursor(null);
    }

    private void updateInfoLabel() {
        StringBuilder info = new StringBuilder();
        if( moduleSearchManager.getModules() != null ) {
            if (moduleSearchManager.getModules().isEmpty()) {
                info.append("No module found"); 
            } else {
                info.append("Found ");
                if (moduleSearchManager.canFetchNext()) {
                    info.append("first ");
                }
                info.append(moduleSearchManager.getModules().size());
                if( moduleSearchManager.getModules().size() == 1 ) {
                    info.append(" module");
                } else {
                    info.append(" modules");
                }
            }
    
            /*if( !moduleSearchManager.getLastQuery().isEmpty() ) {
                info.append(" for query '");
                info.append(moduleSearchManager.getLastQuery());
                info.append("'");
            }*/
    
            if (moduleSearchManager.canFetchNext()) {
                info.append(", click here to <a>see more</a> results");
            }
        } else {
            info.append("Click 'Search' to find modules by module name");
        }
        if (info.length() != 0 && info.charAt(info.length() - 1) != '.') {
            info.append(".");
        }
        searchInfo.setText(info.toString());
        searchInfo.pack();
    }

    private void updateEnabledState() {
        searchCombo.setEnabled(true);
        searchButton.setEnabled(true);
        if (moduleSearchManager.canFetchNext()) {
            fetchNextAction.setEnabled(true);
        } else {
            fetchNextAction.setEnabled(false);
        }
    }

    private void updateFocusAndSelection(boolean newModel) {
        if (moduleSearchManager.getModules() == null || moduleSearchManager.getModules().isEmpty()) {
            searchCombo.setFocus();
        } else {
            moduleTreeViewer.setSelection(new StructuredSelection(moduleSearchManager.getModules().get(0)));
            moduleTreeViewer.getTree().setFocus();
        }
    }

    private void updateSearchComboHistory(boolean newModel) {
        String lastQuery = moduleSearchManager.getLastQuery();
        if (newModel && lastQuery != null && !lastQuery.isEmpty() ) {
            if (queryHistory.contains(lastQuery)) {
                queryHistory.remove(lastQuery);
            }
            queryHistory.add(0, lastQuery);
            searchCombo.setItems(queryHistory.toArray(new String[queryHistory.size()]));
            searchCombo.setText(lastQuery);
        }
    }
    
    private void updateBeforeSearch(boolean newModel) {
        searchInfo.setText("Searching module repositories...");
        searchInfo.pack();
        //searchCombo.setEnabled(false);
        searchButton.setEnabled(false);
        if( newModel ) {
            moduleTreeViewer.setInput(null);
        }
        parent.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_WAIT));
    }

    public void updateDoc() {
        ModuleVersionNode versionNode = null;
        
        Object selectedElement = ((IStructuredSelection) moduleTreeViewer.getSelection()).getFirstElement();
        if (selectedElement instanceof ModuleNode) {
            versionNode = ((ModuleNode) selectedElement).getLastVersion();
        } else if (selectedElement instanceof ModuleVersionNode) {
            versionNode = (ModuleVersionNode) selectedElement;
        }
        
        StringBuffer docBuilder = new StringBuffer();
        HTMLPrinter.insertPageProlog(docBuilder, 0, docForegroundColor, docBackgroundColor, docStyleSheet);
        
        if (versionNode != null) {
        	addImageAndLabel(docBuilder, null, fileUrl("jar_l_obj.gif").toExternalForm(), 
    				16, 16, "<b><tt>" + versionNode.getModule().getName() + " '" + versionNode.getVersion() + "'" +"</tt></b>", 20, 4);
        	docBuilder.append("<hr/>");
//            docBuilder.append("<h1>");
//            docBuilder.append(versionNode.getModule().getName() );
//            docBuilder.append(" '");
//            docBuilder.append(versionNode.getVersion());
//            docBuilder.append("'</h1>");
            
            if (versionNode.isFilled()) {
                
                if( versionNode.getDoc() == null || versionNode.getDoc().isEmpty() ) {
                    docBuilder.append("<p>");
                    docBuilder.append("<i>Module does not have documentation.</i>");
                } else {
                    docBuilder.append(markdown(versionNode.getDoc()));
                }
                
                if (versionNode.getAuthors() != null && !versionNode.getAuthors().isEmpty()) {
                    docBuilder.append("<p>");
                    docBuilder.append("<b>Authors: </b>");
                    docBuilder.append(versionNode.getAuthorsCommaSeparated());
                }
                
                if( versionNode.getLicense() != null && !versionNode.getLicense().isEmpty() ) {
                    docBuilder.append("<p>");
                    docBuilder.append("<b>License: </b>");
                    docBuilder.append(versionNode.getLicense());
                }
                
            } else {
                docBuilder.append("<p>");
                docBuilder.append("<i>Click here to <a href='module:" + versionNode.getModule().getName() + ":" + versionNode.getVersion() + "'>fetch documentation</a></i> for this module version.");
            }
        }
        
        HTMLPrinter.addPageEpilog(docBuilder);
        
        docBrowser.setText(docBuilder.toString());
    }
    
    private String markdown(String text) {
        if( text == null || text.length() == 0 ) {
            return text;
        }
        
        Configuration config = Configuration.builder().
                forceExtentedProfile().
                setCodeBlockEmitter(new CeylonBlockEmitter()).
                build();
        
        return Processor.process(text, config);
    }    
    
}