package com.redhat.ceylon.eclipse.code.hover;

import static com.redhat.ceylon.eclipse.code.hover.DocumentationHover.getHoverInfo;
import static com.redhat.ceylon.eclipse.code.hover.DocumentationHover.getLinkedModel;
import static com.redhat.ceylon.eclipse.code.hover.DocumentationHover.gotoDeclaration;
import static com.redhat.ceylon.eclipse.code.hover.DocumentationHover.internalGetHoverInfo;
import static org.eclipse.jdt.ui.PreferenceConstants.APPEARANCE_JAVADOC_FONT;
import static org.eclipse.ui.ISharedImages.IMG_TOOL_BACK;
import static org.eclipse.ui.ISharedImages.IMG_TOOL_BACK_DISABLED;
import static org.eclipse.ui.ISharedImages.IMG_TOOL_FORWARD;
import static org.eclipse.ui.ISharedImages.IMG_TOOL_FORWARD_DISABLED;
import static org.eclipse.ui.PlatformUI.getWorkbench;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.part.ViewPart;

import com.redhat.ceylon.compiler.typechecker.model.Referenceable;
import com.redhat.ceylon.eclipse.code.browser.BrowserInput;
import com.redhat.ceylon.eclipse.code.editor.CeylonEditor;

public class DocumentationView extends ViewPart {
    
    private static DocumentationView instance;
    
    public static DocumentationView getInstance() {
        return instance;
    }
    
    public DocumentationView() {
        instance = this;
    }
    
    private Browser control;
    private CeylonEditor editor;
    private CeylonBrowserInput info;
    private BackAction back;
    private ForwardAction forward;
    
    @Override
    public void createPartControl(Composite parent) {
        IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
        back = new BackAction();
        back.setEnabled(false);
        tbm.add(back);
        forward = new ForwardAction();
        forward.setEnabled(false);
        tbm.add(forward);
        control = new Browser(parent, SWT.NONE); 
        control.setJavascriptEnabled(false);
        Display display = getSite().getShell().getDisplay();
        Color fg = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
        Color bg = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
        control.setForeground(fg);
        control.setBackground(bg);
        parent.setForeground(fg);
        parent.setBackground(bg);
        FontData fontData = JFaceResources.getFontRegistry()
                .getFontData(APPEARANCE_JAVADOC_FONT)[0];
        control.setFont(new Font(Display.getDefault(), fontData));
        control.addLocationListener(new LocationListener() {
            @Override
            public void changing(LocationEvent event) {
                String location = event.location;
                
                //necessary for windows environment (fix for blank page)
                //somehow related to this: https://bugs.eclipse.org/bugs/show_bug.cgi?id=129236
                if (!"about:blank".equals(location)) {
                    event.doit = false;
                }
                
                if (location.startsWith("dec:")) {
                    Referenceable target = getLinkedModel(info, editor, location);
                    if (target!=null) {
                        gotoDeclaration(editor, target);
                    }
                }
                else if (location.startsWith("doc:")) {
                    Referenceable target = getLinkedModel(info, editor, location);
                    if (target!=null) {
                        info = getHoverInfo(target, info, editor, null);
                        if (info!=null) control.setText(info.getHtml());
                        back.update();
                        forward.update();
                    }
                }
            }
            @Override
            public void changed(LocationEvent event) {}
        });
    }

    @Override
    public void setFocus() {}
    
    public void update(CeylonEditor editor, int offset, int length) { 
        this.editor = editor;
        info = internalGetHoverInfo(editor, new Region(offset, length));
        if (info!=null && info.getAddress()!=null) {
            control.setText(info.getHtml());
            back.update();
            forward.update();
        }
    }
    
    @Override
    public void dispose() {
        instance = null;
        super.dispose();
    }

    class BackAction extends Action {
        
        public BackAction() {
            setText("Back");
            ISharedImages images = getWorkbench().getSharedImages();
            setImageDescriptor(images.getImageDescriptor(IMG_TOOL_BACK));
            setDisabledImageDescriptor(images.getImageDescriptor(IMG_TOOL_BACK_DISABLED));

            update();
        }
        
        @Override
        public void run() {
            BrowserInput previous = info.getPrevious();
            if (previous != null) {
                control.setText(previous.getHtml());
                info = (CeylonBrowserInput) previous;
                update();
                forward.update();
            }
        }
        
        public void update() {
            if (info != null && info.getPrevious() != null) {
                BrowserInput previous = info.getPrevious();
                setToolTipText("Back to " + previous.getInputName());
                setEnabled(true);
            }
            else {
                setToolTipText("Back");
                setEnabled(false);
            }
        }
        
    }
    
    class ForwardAction extends Action {
        
        public ForwardAction() {
            setText("Forward");
            ISharedImages images = getWorkbench().getSharedImages();
            setImageDescriptor(images.getImageDescriptor(IMG_TOOL_FORWARD));
            setDisabledImageDescriptor(images.getImageDescriptor(IMG_TOOL_FORWARD_DISABLED));

            update();
        }
        
        @Override
        public void run() {
            BrowserInput next = info.getNext();
            if (next != null) {
                control.setText(next.getHtml());
                info = (CeylonBrowserInput) next;
                update();
                forward.update();
            }
        }
        
        public void update() {
            if (info != null && info.getNext() != null) {
                BrowserInput next = info.getNext();
                setToolTipText("Forward to " + next.getInputName());
                setEnabled(true);
            }
            else {
                setToolTipText("Forward");
                setEnabled(false);
            }
        }
        
    }
    
}