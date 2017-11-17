package com.salesforce.ide.ui.editors.actions;

import java.util.ResourceBundle;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

import com.salesforce.ide.ui.editors.apex.ApexCodeEditor;

/**
 * Toggle comment which is missing in apex editor
 * @author iBrother
 * @since 17/11/2017
 *
 */
public class ToggleCommentAction extends TextEditorAction {
	private ITextOperationTarget operationTarget;

	public ToggleCommentAction(ResourceBundle bundle, String prefix, ApexCodeEditor editor) {
		super(bundle, prefix, editor);

		setText("Toggle comment");
		setActionDefinitionId("com.salesforce.ide.apex.ui.command.togglecomment");
		update();
	}

	public void run() {
//		System.out.println("---toggle comment---fOperationTarget: " + this.fOperationTarget);
		if (this.operationTarget == null) {
			return;
		}
		ITextEditor editor = getTextEditor();
		if (editor == null) {
			return;
		}
		TextSelection sel = (TextSelection) editor.getSelectionProvider().getSelection();
//		System.out.println(">>>>>>>>>>>sel: " + sel.getText());
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		try {
		
			if (!validateEditorInputState()) {
				return;
			}
			int operation = getOperation(doc,sel);
//			System.out.println(">>>>>>>>>>>operation: " + operation);
			
			Display display = null;

			IWorkbenchPartSite site = editor.getSite();
			Shell shell = site.getShell();
			if ((shell != null) && (!shell.isDisposed())) {
				display = shell.getDisplay();
			}
			
			BusyIndicator.showWhile(display, (Runnable) new Runnable() {
				@Override
				public void run() {
					String[] dfp = new String[] { "//" };

					TextViewer tv = (TextViewer) operationTarget;
					tv.setDefaultPrefixes(dfp, "__dftl_partition_content_type");
					ToggleCommentAction.this.operationTarget.doOperation(operation);
				}
			});
			
		} catch (BadLocationException e) {
			// log.error("In toggle comment: ", e);
			 e.printStackTrace();
		}
	}
	
	private int getOperation(IDocument doc,TextSelection sel) throws BadLocationException
	{
		int lineNum = doc.getLineOfOffset(sel.getOffset());
		int numLineSelected = doc.getNumberOfLines(sel.getOffset(), sel.getLength());
		
		// comment: 11, uncomment: 12
		int operation  = ITextOperationTarget.STRIP_PREFIX; 
		for(int y=0;y<numLineSelected;y++)
		{
			IRegion reg = doc.getLineInformation(lineNum+y);
			String s = doc.get(reg.getOffset(), reg.getLength());
//			System.out.println(">>>startWith:"+(s.trim().startsWith("//"))+" >>:"+s); 
			if(!(s+"").trim().startsWith("//"))
			{
				operation = ITextOperationTarget.PREFIX;
				break;
			}
		}
		
			
		return operation;
			
	}
	
	public void update() {
		super.update();
		if (!isEnabled()) {
			return;
		}
		if (!canModifyEditor()) {
			setEnabled(false);
			return;
		}
		ITextEditor editor = getTextEditor();
		if ((this.operationTarget == null) && (editor != null)) {
			this.operationTarget = ((ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class));
		}
	}

	public void setEditor(ITextEditor editor) {
		super.setEditor(editor);
		this.operationTarget = null;
	}
 
}
