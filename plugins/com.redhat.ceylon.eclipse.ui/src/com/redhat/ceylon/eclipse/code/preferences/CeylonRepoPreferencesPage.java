package com.redhat.ceylon.eclipse.code.preferences;

import static com.redhat.ceylon.compiler.typechecker.TypeChecker.LANGUAGE_MODULE_VERSION;
import static com.redhat.ceylon.eclipse.code.preferences.CeylonPreferencesPage.getDefaultOutputPath;
import static com.redhat.ceylon.eclipse.core.builder.CeylonBuilder.getCeylonModulesOutputPath;
import static com.redhat.ceylon.eclipse.core.builder.CeylonBuilder.getDefaultUserRepositories;
import static com.redhat.ceylon.eclipse.core.builder.CeylonBuilder.getRepositoryPaths;
import static com.redhat.ceylon.eclipse.core.builder.CeylonBuilder.interpolateVariablesInRepositoryPath;
import static com.redhat.ceylon.eclipse.core.builder.CeylonBuilder.isExplodeModulesEnabled;
import static com.redhat.ceylon.eclipse.core.builder.CeylonBuilder.showWarnings;
import static com.redhat.ceylon.eclipse.core.builder.CeylonNature.NATURE_ID;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.PropertyPage;

import com.redhat.ceylon.eclipse.core.builder.CeylonNature;
import com.redhat.ceylon.eclipse.ui.CeylonPlugin;

public class CeylonRepoPreferencesPage extends PropertyPage {

    boolean builderEnabled = false;
    private List<String> repositoryPaths;

    //TODO: fix copy/paste!
    public boolean isRepoValid() {
    	for (String repositoryPath: repositoryPaths) {
    		String carPath = interpolateVariablesInRepositoryPath(repositoryPath) + 
    				"/ceylon/language/" + LANGUAGE_MODULE_VERSION + 
    				"/ceylon.language-" + LANGUAGE_MODULE_VERSION + ".car";
    		if (new File(carPath).exists()) {
    			return true;
    		}
    	}
    	return false;
    }
    
    @Override
    public boolean performOk() {
        if (!isRepoValid()) return false;
        store();
        return super.performOk();
    }
    
    @Override
    protected void performApply() {
        if (isRepoValid()) {
            store();
            super.performApply();
        }
    }
    
    @Override
    public boolean okToLeave() {
        if (!isRepoValid()) return false;
        return super.okToLeave();
    }
    
    @Override
    protected void performDefaults() {
        repositoryPaths = Arrays.asList(getDefaultUserRepositories());
        repoFolders.removeAll();
        for(String repo : repositoryPaths)
        	addRepoToTable(repo);
        if (isRepoValid()) {
        	setErrorMessage(null);
        }
        super.performDefaults();
    }
    
    private void store() {
        final IProject project = getSelectedProject();
		//if (!embeddedRepo) ExportModuleWizard.persistDefaultRepositoryPath(repositoryPath);
        boolean explodeModules = isExplodeModulesEnabled(project);
        boolean showCompilerWarnings = showWarnings(project);
        IPath outputPath = getCeylonModulesOutputPath(project);
        if (outputPath==null) {
        	outputPath = getDefaultOutputPath(project);
        }
		new CeylonNature(outputPath, repositoryPaths,
				explodeModules, !showCompilerWarnings)
		                .addToProject(project);
    }

    private IProject getSelectedProject() {
        return (IProject) getElement().getAdapter(IProject.class);
    }

    private Table repoFolders;
    
    private static Image repo = CeylonPlugin.getInstance().image("runtime_obj.gif").createImage();
    
	private void addSelectRepoSection(Composite parent) {
		//Label title = new Label(parent, SWT.LEFT | SWT.WRAP);
        //title.setText("The Ceylon module repository contains dependencies:");
        //final Composite composite= new Composite(parent, SWT.NONE);
        Group composite = new Group(parent, SWT.SHADOW_ETCHED_IN);
        composite.setText("Ceylon module repositories");
        GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gd.grabExcessHorizontalSpace=true;
        composite.setLayoutData(gd);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 10;
        layout.marginHeight = 10;
        layout.numColumns = 4;
        composite.setLayout(layout);        
        
        Label folderLabel = new Label(composite, SWT.LEFT | SWT.WRAP);
        folderLabel.setText("Module repositories on build path: ");
        GridData flgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        flgd.horizontalSpan = 4;
        flgd.grabExcessHorizontalSpace = true;
        folderLabel.setLayoutData(flgd);

        repoFolders = new Table(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData fgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        fgd.horizontalSpan = 3;
        fgd.verticalSpan = 4;
        fgd.heightHint = 250;
        fgd.grabExcessHorizontalSpace = true;
        fgd.widthHint = 200;
        repoFolders.setLayoutData(fgd);
        repoFolders.setEnabled(builderEnabled);
        
        for (String repositoryPath: repositoryPaths) {
        	addRepoToTable(repositoryPath);
        }
        Composite buttons = new Composite(composite, SWT.NONE);
        GridData bgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        bgd.horizontalSpan = 1;
        bgd.verticalSpan = 4;
        bgd.verticalAlignment = SWT.TOP;
        buttons.setLayoutData(bgd);
        GridLayout l = new GridLayout();
        l.marginWidth = 0;
        l.marginHeight = 0;
        l.numColumns = 1;
        buttons.setLayout(l);
                
        Button selectRepoFolder = new Button(buttons, SWT.PUSH);
        selectRepoFolder.setText("Add Repository...");
        GridData sfgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        sfgd.horizontalSpan = 1;
        selectRepoFolder.setLayoutData(sfgd);
        selectRepoFolder.setEnabled(builderEnabled);
        selectRepoFolder.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String dir = new DirectoryDialog(getShell(), SWT.SHEET).open();
                if(dir != null)
                    addRepo(dir);
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });

        Button selectRemoteRepo = new Button(buttons, SWT.PUSH);
        selectRemoteRepo.setText("Add Remote Repository...");
        GridData srrgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        srrgd.horizontalSpan = 1;
        selectRemoteRepo.setLayoutData(srrgd);
        selectRemoteRepo.setEnabled(builderEnabled);
        selectRemoteRepo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                InputDialog input = new InputDialog(getShell(), "Add Remote Repository", "Enter a remote repository URI", "http://", 
                        new IInputValidator(){
                    @Override
                    public String isValid(String val) {
                        try {
                            new URI(val);
                            // FIXME: we might want to validate it more than that: for example to check that it's http/https
                            return null;
                        } catch (URISyntaxException e) {
                            return "Invalid URI: " + e.getReason();
                        }
                    }
                    
                });
                int ret = input.open();
                if(ret == InputDialog.OK){
                    addRepo(input.getValue());
                }
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });

        Button selectHerdRepo = new Button(buttons, SWT.PUSH);
        selectHerdRepo.setText("Add Ceylon Herd");
        GridData srfgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        srfgd.horizontalSpan = 1;
        selectHerdRepo.setLayoutData(srfgd);
        selectHerdRepo.setEnabled(builderEnabled);
        selectHerdRepo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String url = "http://modules.ceylon-lang.org/test";
                addRepo(url);
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });

        Button deleteRepoFolder = new Button(buttons, SWT.PUSH);
        deleteRepoFolder.setText("Remove Repository");
        GridData dfgd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        dfgd.horizontalSpan = 1;
        deleteRepoFolder.setLayoutData(dfgd);
        deleteRepoFolder.setEnabled(builderEnabled);
        deleteRepoFolder.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	int[] selection = repoFolders.getSelectionIndices();
				repoFolders.remove(selection);
            	Iterator<String> iter = repositoryPaths.iterator();
            	int i=0;
            	while (iter.hasNext()) {
            		iter.next();
            		if (Arrays.binarySearch(selection, i++)>=0) {
            			iter.remove();
            		}
            	}
                if (!isRepoValid()) {
                    setErrorMessage("Please select a module repository containing the language module");
                }
                else {
                    setErrorMessage(null);
                }
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
	}

    protected void addRepo(String dir) {
        repositoryPaths.add(dir);
        addRepoToTable(dir);
        if (!isRepoValid()) {
            setErrorMessage("Please select a module repository containing the language module");
        } else {
            setErrorMessage(null);
        }
    }

    private void addRepoToTable(String repositoryPath) {
		TableItem item = new TableItem(repoFolders,SWT.NONE);
		item.setText(repositoryPath);
		item.setImage(repo);
	}

    @Override
    protected Control createContents(Composite composite) {
        
        IProject project = getSelectedProject();
        
        if (project.isOpen()) {
        
			try {
	            builderEnabled = project.hasNature(NATURE_ID);
	        } 
	        catch (CoreException e) {
	            e.printStackTrace();
	        }
	        
			repositoryPaths = new ArrayList<String>();
			repositoryPaths.addAll(Arrays.asList(getRepositoryPaths(project)));
        }

        addSelectRepoSection(composite);
        return composite;
    }

}