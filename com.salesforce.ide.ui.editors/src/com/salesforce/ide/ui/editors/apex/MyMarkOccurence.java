package com.salesforce.ide.ui.editors.apex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.core.manipulation.search.IOccurrencesFinder;
import org.eclipse.jdt.internal.core.manipulation.search.IOccurrencesFinder.OccurrenceLocation;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISelectionValidator;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class MyMarkOccurence {
	
	private ITextViewer textViewer;
	Object editorInput;
	IDocumentProvider documentProvider;
	private OccurrencesFinderJob fOccurrencesFinderJob;
	private Annotation[] fOccurrenceAnnotations= null;
	private ISelection fForcedMarkOccurrencesSelection;
	private long fMarkOccurrenceModificationStamp= IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
	private IRegion fMarkOccurrenceTargetRegion;
	
	public MyMarkOccurence(ITextViewer textViewer,Object editorInput,IDocumentProvider documentProvider)
	{
		this.textViewer = textViewer;
		this.editorInput = editorInput;
		this.documentProvider = documentProvider;
	}

	public void perform(IDocument document,TextSelection selection) throws BadLocationException
	{
    	IRegion region = JavaWordFinder.findWord(document, selection.getOffset());
    	String word = "";
    	try {
			word = document.get(region.getOffset(), region.getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
    	if(StringUtils.isBlank(word))return;
    	
    	List<OccurrenceLocation> listLocations= new ArrayList<OccurrenceLocation>();
    	FindReplaceDocumentAdapter findAdaptor = new FindReplaceDocumentAdapter(document);
		int startOffset = 0;
    	do {
			IRegion match = findAdaptor.find(startOffset, word, true, false, true, false);
			if (match == null) {
				break;
			}
			//flag: text type. string, method,... 0:gray, 1: yellow,...
			OccurrenceLocation oc = new OccurrenceLocation(match.getOffset(),match.getLength(),0,"");
			listLocations.add(oc);
			startOffset = match.getOffset() + match.getLength();
			 
		} while (startOffset < document.getLength());
    	
    	fOccurrencesFinderJob= new OccurrencesFinderJob(document,  listLocations.toArray(new OccurrenceLocation[listLocations.size()] ), selection);
    	fOccurrencesFinderJob.run(new NullProgressMonitor()); 
    	
	}
	
	class OccurrencesFinderJob extends Job {

		private final IDocument fDocument;
		private final ISelection fSelection;
		private final ISelectionValidator fPostSelectionValidator;
		private boolean fCanceled= false;
		private final OccurrenceLocation[] fLocations;

		public OccurrencesFinderJob(IDocument document, OccurrenceLocation[] locations, ISelection selection) {
			super("Occurrences Marker");
			fDocument= document;
			fSelection= selection;
			fLocations= locations;

			if (getSelectionProvider() instanceof ISelectionValidator)
				fPostSelectionValidator= (ISelectionValidator)getSelectionProvider();
			else
				fPostSelectionValidator= null;
		}

		// cannot use cancel() because it is declared final
		void doCancel() {
			fCanceled= true;
			cancel();
		}

		private boolean isCanceled(IProgressMonitor progressMonitor) {
			return fCanceled || progressMonitor.isCanceled()
				||  fPostSelectionValidator != null && !(fPostSelectionValidator.isValid(fSelection) || fForcedMarkOccurrencesSelection == fSelection)
				|| LinkedModeModel.hasInstalledModel(fDocument);
		}

		/*
		 * @see Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public IStatus run(IProgressMonitor progressMonitor) {
			if (isCanceled(progressMonitor))
				return Status.CANCEL_STATUS;

			ITextViewer textViewer= getViewer();
			if (textViewer == null)
				return Status.CANCEL_STATUS;

			IDocument document= textViewer.getDocument();
			if (document == null)
				return Status.CANCEL_STATUS;

			IDocumentProvider documentProvider= getDocumentProvider();
			if (documentProvider == null)
				return Status.CANCEL_STATUS;

			IAnnotationModel annotationModel= documentProvider.getAnnotationModel(getEditorInput());
			if (annotationModel == null)
				return Status.CANCEL_STATUS;

			// Add occurrence annotations
			int length= fLocations.length;
			Map<Annotation, Position> annotationMap= new HashMap<>(length);
			for (int i= 0; i < length; i++) {

				if (isCanceled(progressMonitor))
					return Status.CANCEL_STATUS;

				OccurrenceLocation location= fLocations[i];
				Position position= new Position(location.getOffset(), location.getLength());

				String description= location.getDescription();
				String annotationType= (location.getFlags() == IOccurrencesFinder.F_WRITE_OCCURRENCE) ? "org.eclipse.jdt.ui.occurrences.write" : "org.eclipse.jdt.ui.occurrences"; //$NON-NLS-1$ //$NON-NLS-2$

				annotationMap.put(new Annotation(annotationType, false, description), position);
			}

			if (isCanceled(progressMonitor))
				return Status.CANCEL_STATUS;

			synchronized (getLockObject(annotationModel)) {
				if (annotationModel instanceof IAnnotationModelExtension) {
					((IAnnotationModelExtension)annotationModel).replaceAnnotations(fOccurrenceAnnotations, annotationMap);
				} else {
					removeOccurrenceAnnotations();
					Iterator<Entry<Annotation, Position>> iter= annotationMap.entrySet().iterator();
					while (iter.hasNext()) {
						Entry<Annotation, Position> mapEntry= iter.next();
						annotationModel.addAnnotation(mapEntry.getKey(), mapEntry.getValue());
					}
				}
				fOccurrenceAnnotations= annotationMap.keySet().toArray(new Annotation[annotationMap.keySet().size()]);
			}

			return Status.OK_STATUS;
		}
		
	}
	
	protected ITextViewer getViewer() 
	{
		return textViewer;
	}
	
	protected ISelectionValidator getSelectionProvider() {
		return null;
	}

	protected Object getEditorInput() {
		return editorInput;
	}

	protected IDocumentProvider getDocumentProvider() {
		return documentProvider;
	}
	
	private Object getLockObject(IAnnotationModel annotationModel) {
		if (annotationModel instanceof ISynchronizable) {
			Object lock= ((ISynchronizable)annotationModel).getLockObject();
			if (lock != null)
				return lock;
		}
		return annotationModel;
	}
	
	private void removeOccurrenceAnnotations() {
		fMarkOccurrenceModificationStamp= IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
		fMarkOccurrenceTargetRegion= null;

		IDocumentProvider documentProvider= getDocumentProvider();
		if (documentProvider == null)
			return;

		IAnnotationModel annotationModel= documentProvider.getAnnotationModel(getEditorInput());
		if (annotationModel == null || fOccurrenceAnnotations == null)
			return;

		synchronized (getLockObject(annotationModel)) {
			if (annotationModel instanceof IAnnotationModelExtension) {
				((IAnnotationModelExtension)annotationModel).replaceAnnotations(fOccurrenceAnnotations, null);
			} else {
				for (int i= 0, length= fOccurrenceAnnotations.length; i < length; i++)
					annotationModel.removeAnnotation(fOccurrenceAnnotations[i]);
			}
			fOccurrenceAnnotations= null;
		}
	}
}
