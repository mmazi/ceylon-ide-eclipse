package com.redhat.ceylon.eclipse.ui;

import static com.redhat.ceylon.eclipse.ui.CeylonPlugin.PLUGIN_ID;
import static org.eclipse.ui.PlatformUI.getWorkbench;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;

public class CeylonStartup implements IStartup {

    private static final class WindowListener implements IWindowListener {
        IContextActivation contextActivation1 = null;
        IContextActivation contextActivation2 = null;

        public void updateContext(IPerspectiveDescriptor perspective) {
            IContextService service = (IContextService) getWorkbench().getActiveWorkbenchWindow()
                    .getService(IContextService.class);
            // in case of previous crash, perspective may be null
            if (perspective != null && perspective.getId() != null && 
                    perspective.getId().equals(PLUGIN_ID + ".perspective")) {
                contextActivation1 = service.activateContext(PLUGIN_ID + ".perspectiveContext");
                contextActivation2 = service.activateContext(PLUGIN_ID + ".wizardContext");
            }
            else {
                if (contextActivation1!=null) {
                    service.deactivateContext(contextActivation1);
                }
                if (contextActivation2!=null) {
                    service.deactivateContext(contextActivation2);
                }
            }
        }

        @Override
        public void windowOpened(IWorkbenchWindow window) {
            updateContext(window.getActivePage().getPerspective());
            window.addPerspectiveListener(new IPerspectiveListener() {
                @Override
                public void perspectiveChanged(IWorkbenchPage page,
                        IPerspectiveDescriptor perspective, String changeId) {}
                @Override
                public void perspectiveActivated(IWorkbenchPage page,
                        IPerspectiveDescriptor perspective) {
                    updateContext(perspective);
                }
            });
        }

        @Override
        public void windowDeactivated(IWorkbenchWindow window) {}
        @Override
        public void windowClosed(IWorkbenchWindow window) {}
        @Override
        public void windowActivated(IWorkbenchWindow window) {}
    }

    @Override
    public void earlyStartup() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                WindowListener listener = new WindowListener();
                getWorkbench().addWindowListener(listener);
                for (IWorkbenchWindow window: getWorkbench().getWorkbenchWindows()) {
                    listener.windowOpened(window);
                }
            }
        });
    }

}
