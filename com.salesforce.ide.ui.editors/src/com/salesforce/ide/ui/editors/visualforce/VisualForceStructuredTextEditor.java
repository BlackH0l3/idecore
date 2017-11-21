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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ContentAssistantFacade;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;

import com.salesforce.ide.core.internal.utils.Constants;
import com.salesforce.ide.ui.editors.actions.ToggleCommentAction;
import com.salesforce.ide.ui.editors.internal.utils.EditorMessages;

public class VisualForceStructuredTextEditor extends StructuredTextEditor {

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

//        IAction action = new TextOperationAction(
//            EditorMessages.getResourceBundle(),
//            "ApexEditor.ContentAssistProposal.",
//            this, ISourceViewer.CONTENTASSIST_PROPOSALS);
//        action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
//        setAction(ACTION_CONTENT_ASSIST_PROPOSAL, action);
//
//        action = new TextOperationAction(
//            EditorMessages.getResourceBundle(),
//            "ApexEditor.ContentAssistTip.",
//            this,
//            ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
//        action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
//        setAction(ACTION_CONTENT_ASSIST_TIP, action);
//
//        action = new DefineFoldingRegionAction(
//            EditorMessages.getResourceBundle(),
//            "ApexEditor.DefineFoldingRegion.",
//            this);
//        setAction(ACTION_DEFINE_FOLDING_REGION, action);
        
        IAction action = new ToggleCommentAction(EditorMessages.getResourceBundle(),"ApexEditor.ToggleComment",this);
        setAction(ToggleCommentAction.ACTION_TOGGLE_COMMENT, action);
        
        
    }
 
}
