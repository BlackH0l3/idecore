/*******************************************************************************
 * Copyright (c) 2014 Salesforce.com, inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Salesforce.com, inc. - initial API and implementation
 ******************************************************************************/
package com.salesforce.ide.ui.editors.visualforce;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.editors.text.ITextEditorHelpContextIds;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.texteditor.AddMarkerAction;
import org.eclipse.ui.texteditor.ResourceAction;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import com.salesforce.ide.core.internal.utils.Constants;
import com.salesforce.ide.ui.editors.actions.ToggleCommentAction;
import com.salesforce.ide.ui.editors.internal.utils.EditorMessages;

public class VisualForceStructuredTextEditor extends StructuredTextEditor {
	ResourceAction bookmarkAction;
    public VisualForceStructuredTextEditor() {
        super();
    }

    @Override
    protected void initializeEditor() {
        super.initializeEditor();
        setHelpContextId(Constants.DOCUMENTATION_PLUGIN_PREFIX + "." + this.getClass().getSimpleName());
    }
    
    @Override
    protected void createActions() {
        super.createActions();

        IAction action = new ToggleCommentAction(EditorMessages.getResourceBundle(),"ApexEditor.ToggleComment",this);
        setAction(ToggleCommentAction.ACTION_TOGGLE_COMMENT, action);
        
        bookmarkAction = new AddMarkerAction(EditorMessages.getResourceBundle(), "ApexEditor.AddBookmark.", this, IMarker.BOOKMARK, true);
		bookmarkAction.setHelpContextId(ITextEditorHelpContextIds.BOOKMARK_ACTION);
		bookmarkAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_ADD_BOOKMARK);
		setAction(IDEActionFactory.BOOKMARK.getId(), bookmarkAction);
    }
    
   
    @Override
    protected void addExtendedRulerContextMenuActions(IMenuManager menu) {
    	menu.add(bookmarkAction);
	}
}
