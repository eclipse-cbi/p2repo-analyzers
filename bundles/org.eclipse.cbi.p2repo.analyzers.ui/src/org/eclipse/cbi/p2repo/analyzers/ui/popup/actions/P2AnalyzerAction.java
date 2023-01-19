package org.eclipse.cbi.p2repo.analyzers.ui.popup.actions;

import org.eclipse.cbi.p2repo.analyzers.P2RepositoryCheck;
import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.ide.IDE;

public class P2AnalyzerAction implements IObjectActionDelegate {

	private ISelection selection;
	private IWorkbenchPart targetPart;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	public void run(IAction action) {
		if (this.selection instanceof IStructuredSelection structuredSelection) {
			Object first = structuredSelection.getFirstElement();
			if (first instanceof IFolder iFolder) {
				String reportRepoDir = iFolder.getLocation().toOSString();
				IFolder outputFolder = iFolder.getParent().getFolder(new Path("p2-report")); //$NON-NLS-1$
				RepoTestsConfiguration configurations = new RepoTestsConfiguration(reportRepoDir,
						outputFolder.getLocation().toOSString(), null, System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
				new P2RepositoryCheck().runChecks(configurations);
				IFile htmlIFile = outputFolder.getFile("errors-and-moderate_warnings.html"); //$NON-NLS-1$
				try {
					iFolder.getParent().refreshLocal(1, new NullProgressMonitor());
					MessageDialog.openInformation(targetPart.getSite().getShell(), Messages.P2AnalyzerAction_dialog_info_title,
							Messages.P2AnalyzerAction_dialog_info_msg + outputFolder.getFullPath());
					IDE.openEditor(targetPart.getSite().getWorkbenchWindow().getActivePage(), htmlIFile);
				} catch (CoreException e) {
					e.printStackTrace();
				}

			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

}
