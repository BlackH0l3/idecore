package com.salesforce.ide.ui.editors.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

import com.salesforce.ide.ui.editors.apex.ApexCodeEditor;
import com.salesforce.ide.ui.editors.visualforce.VisualForceStructuredTextEditor;

/**
 * Toggle comment which is missing in apex editor
 * 
 * @author iBrother
 * @since 17/11/2017
 *
 */
public class ToggleCommentAction extends TextEditorAction {
	private ITextOperationTarget operationTarget;
	public static final String ACTION_TOGGLE_COMMENT = "ToggleComment";

	public ToggleCommentAction(ResourceBundle bundle, String prefix, ApexCodeEditor editor) {
		super(bundle, prefix, editor);
		init();
	}

	public ToggleCommentAction(ResourceBundle bundle, String prefix, VisualForceStructuredTextEditor editor) {
		super(bundle, prefix, editor);
		init();

	}

	private void init() {
		setText("Toggle comment");
		setActionDefinitionId("com.salesforce.ide.apex.ui.command.togglecomment");
		update();
	}

	public void run() {
		if (this.operationTarget == null) {
			return;
		}
		ITextEditor editor = getTextEditor();
		if (editor == null) {
			return;
		}

		if (!validateEditorInputState()) {
			return;
		}
		Display display = null;

		IWorkbenchPartSite site = editor.getSite();
		Shell shell = site.getShell();
		if ((shell != null) && (!shell.isDisposed())) {
			display = shell.getDisplay();
		}

		BusyIndicator.showWhile(display, (Runnable) new Runnable() {
			@Override
			public void run() {
				 if(getTextEditor() instanceof ApexCodeEditor)
				 {
					 toggle(APEX_PARTITIONING);
				
				 }else if(getTextEditor() instanceof VisualForceStructuredTextEditor)
				 {
					 toggle(VF_PARTITIONING);
				 }
			}
		});
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

	final static String VF_PARTITIONING = "org.eclipse.wst.sse.core.default_structured_text_partitioning";
	final static String APEX_PARTITIONING = "__apex_partitioning";
	private Map<String, String[]> getMapKeyComment() {
		Map<String, String[]> mapKeyComment =  new HashMap<String, String[]>();
		mapKeyComment.put("__dftl_partition_content_type",new String[] { "//" });
		mapKeyComment.put("org.eclipse.wst.html.HTML_DEFAULT",new String[] { "<!--","-->" });
		mapKeyComment.put("org.eclipse.wst.html.SCRIPT",new String[] { "//" });
		mapKeyComment.put("org.eclipse.wst.css.STYLE",new String[] { "/*","*/" });  
		mapKeyComment.put("org.eclipse.wst.html.HTML_COMMENT",new String[] { "<!--","-->" });  
		mapKeyComment.put("org.eclipse.wst.xml.XML_COMMENT",new String[] { "<!--","-->" });  
		mapKeyComment.put("org.eclipse.wst.xml.XML_DEFAULT",new String[] { "<!--","-->" });   
		
		
		return mapKeyComment;
	}

	private void toggle(String partioning) {
		Map<String, String[]> mapKeyComment =  getMapKeyComment(); 
		TextViewer tv = (TextViewer) operationTarget;

		if (tv.getUndoManager() != null)
			tv.getUndoManager().beginCompoundChange();

		IDocument d = tv.getDocument();
		Map<String, IDocumentPartitioner> partitioners = null;
		DocumentRewriteSession rewriteSession = null;
		try {
			ITextSelection selection = (ITextSelection) tv.getSelection();
			int startLine = selection.getStartLine(); 
			int endLine = selection.getEndLine();
			
			IRegion block = getTextBlockFromSelection(selection, tv);
			ITypedRegion[] regions = TextUtilities.computePartitioning(d, partioning, block.getOffset(),
					block.getLength(), false);

			int lineCount = endLine-startLine;

			if (d instanceof IDocumentExtension4) {
				IDocumentExtension4 extension = (IDocumentExtension4) d;
				rewriteSession = extension.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
			} else {
				tv.setRedraw(false);
			}
			if (lineCount >= 20)
				partitioners = TextUtilities.removeDocumentPartitioners(d);
			
			String[] prefix_suffix = (String[]) selectContentTypePlugin(regions[0].getType(), mapKeyComment);
			if (prefix_suffix != null && prefix_suffix.length > 0 && startLine >= 0 && endLine >= 0)
			{  
				if("org.eclipse.wst.html.SCRIPT".equals(regions[0].getType()) || "__dftl_partition_content_type".equals(regions[0].getType()))
				{
					doToggle(startLine, endLine, prefix_suffix,tv,false);
				}
				else
				{
					doToggle(startLine, endLine, prefix_suffix,tv,true);
				}
			}
			 

		} catch (BadLocationException x) {
			 if (TextViewer.TRACE_ERRORS)
				 	x.printStackTrace();
				 
		} finally {

			if (partitioners != null)
				TextUtilities.addDocumentPartitioners(d, partitioners);

			if (d instanceof IDocumentExtension4) {
				IDocumentExtension4 extension = (IDocumentExtension4) d;
				extension.stopRewriteSession(rewriteSession);
			} else {
				tv.setRedraw(true);
			}

			if (tv.getUndoManager() != null)
				tv.getUndoManager().endCompoundChange();
		}
	}

	private IRegion getTextBlockFromSelection(ITextSelection selection, TextViewer tv) throws BadLocationException {
		IDocument document = tv.getDocument();
		int start = document.getLineOffset(selection.getStartLine());
		int end;
		int endLine = selection.getEndLine();
		if (document.getNumberOfLines() > endLine + 1) {
			end = document.getLineOffset(endLine + 1);
		} else {
			end = document.getLength();
		}
		return new Region(start, end - start);
	}

	private Object selectContentTypePlugin(String type, Map<String, ?> plugins) {
		if (plugins == null)
			return null;

		return plugins.get(type);
	}
	
	private void doToggle(int startLine, int endLine, String[] prefix_suffix,TextViewer tv,boolean isPrefixAndSuffix) {
		try {
			IDocument d= tv.getDocument();
			IRegion regStart = d.getLineInformation(startLine);
			String textLineStart = d.get(regStart.getOffset(), regStart.getLength());
			boolean isCommented = textLineStart.trim().startsWith(prefix_suffix[0]);
			
			if(!isPrefixAndSuffix)// for: js, java comment (//)
			{
				
				while (startLine <= endLine) {
					regStart = d.getLineInformation(startLine);
					textLineStart = d.get(regStart.getOffset(), regStart.getLength());
					String newTextLineStart = isCommented? StringUtils.replaceOnce(textLineStart,prefix_suffix[0],""):prefix_suffix[0]+textLineStart;
					d.replace(d.getLineOffset(startLine++), textLineStart.length(), newTextLineStart); 
				}
			}else
			{
				IRegion reg = d.getLineInformation(endLine);
				String suffixEnd = isCommented?"":prefix_suffix[1];
				String textRep = isCommented?replaceFirst(textLineStart,prefix_suffix[0], ""):prefix_suffix[0]+textLineStart;
				
				d.replace(d.getLineOffset(startLine), textLineStart.length()  ,textRep);
				
				if(startLine==endLine)//Single line comment
				{
					 reg = d.getLineInformation(startLine);
					 textLineStart = d.get(reg.getOffset(), reg.getLength());
					if(isCommented)
					{
						textLineStart = replaceFirst(textLineStart,prefix_suffix[1], "");
					}else
					{
						textLineStart += suffixEnd;
					}
					
					d.replace(d.getLineOffset(startLine),reg.getLength(),textLineStart);
				}else//multi-lines
				{
					IRegion regEnd = d.getLineInformation(endLine);
					String textLineEnd = d.get(regEnd.getOffset(), regEnd.getLength());
					if(isCommented)
					{
						textLineEnd = replaceLast(textLineEnd,prefix_suffix[1],"");
					}else
					{
						textLineEnd += suffixEnd;
					}
					d.replace(d.getLineOffset(endLine),regEnd.getLength(),  textLineEnd ); 
				}
			}

		} catch (BadLocationException x) {
			if (TextViewer.TRACE_ERRORS)
				x.printStackTrace();
		}
	}
	private String replaceFirst(String source,String textTofind,String textRep)
	{
		String textFind2 = textTofind.replace("*", "\\*");
		return source.replaceFirst("(?s)"+textFind2+"(?!.*?"+textFind2+")", textRep);
	}
	private String replaceLast(String source,String textFind,String textRep)
	{
		String textFind2 = textFind.replace("*", "\\*");
		return source.replaceFirst("(?s)"+textFind2+"(?!.*?"+textFind2+")", textRep);
	}
}
