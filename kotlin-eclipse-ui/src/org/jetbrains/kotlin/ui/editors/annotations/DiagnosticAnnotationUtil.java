/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.editors.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.diagnostics.Severity;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;

public class DiagnosticAnnotationUtil {

    public static final DiagnosticAnnotationUtil INSTANCE = new DiagnosticAnnotationUtil();
    
    private DiagnosticAnnotationUtil() {
    }
    
    @NotNull
    public Map<IFile, List<DiagnosticAnnotation>> handleDiagnostics(@NotNull Diagnostics diagnostics) {
        Map<IFile, List<DiagnosticAnnotation>> annotations = new HashMap<IFile, List<DiagnosticAnnotation>>();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getTextRanges().isEmpty()) {
                continue;
            }
            
            VirtualFile virtualFile = diagnostic.getPsiFile().getVirtualFile();
            if (virtualFile == null) {
                continue;
            }
            
            IFile curFile = ResourcesPlugin.getWorkspace().getRoot().
                    getFileForLocation(new Path(virtualFile.getPath()));

            if (curFile == null) {
                continue;
            }
            
            if (!annotations.containsKey(curFile)) {
                annotations.put(curFile, new ArrayList<DiagnosticAnnotation>());
            }
            
            try {
		    DiagnosticAnnotation annotation = createKotlinAnnotation(diagnostic, curFile);
		    annotations.get(curFile).add(annotation);
	    } catch (BadLocationException e) {
	    }
        }
        
        return annotations;
    }
    
    public void addParsingDiagnosticAnnotations(@NotNull IFile file, @NotNull Map<IFile, List<DiagnosticAnnotation>> annotations) {
        List<DiagnosticAnnotation> parsingAnnotations = createParsingDiagnosticAnnotations(file);
        
        if (annotations.containsKey(file)) {
            annotations.get(file).addAll(parsingAnnotations);
        } else {
            annotations.put(file, parsingAnnotations);
        }
    }
    
    @NotNull
    public List<DiagnosticAnnotation> createParsingDiagnosticAnnotations(@NotNull IFile file) {
        KtFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        List<DiagnosticAnnotation> result = new ArrayList<DiagnosticAnnotation>();
        if (file == null) {
            return result;
        }
        for (PsiErrorElement syntaxError : AnalyzingUtils.getSyntaxErrorRanges(jetFile)) {
            try {
                result.add(createKotlinAnnotation(syntaxError, file));
            } catch (BadLocationException e) {
                //KotlinLogger.logAndThrow(e);
            } catch (NullPointerException e) {
	    }
        }
        
        return result;
    }
    
    @NotNull
    private DiagnosticAnnotation createKotlinAnnotation(@NotNull PsiErrorElement psiErrorElement, @NotNull IFile file) throws BadLocationException {
        PsiFile psiFile = psiErrorElement.getContainingFile();
        
        TextRange range = psiErrorElement.getTextRange();
        int startOffset = range.getStartOffset();
        int length = range.getLength();
        String markedText = psiErrorElement.getText();
        
        if (range.isEmpty()) {
            startOffset--;
            length = 1;
            markedText = psiFile.getText().substring(startOffset, startOffset + length);
        }
        
        if (file == null) throw new NullPointerException();
        IDocument document = EditorUtil.getDocument(file);
        int offset = LineEndUtil.convertLfToDocumentOffset(psiFile.getText(), startOffset, document);
        
        return new DiagnosticAnnotation(
                document.getLineOfOffset(offset),
                offset,
                length,
                AnnotationManager.ANNOTATION_ERROR_TYPE,
                psiErrorElement.getErrorDescription(),
                markedText,
                file,
                null);
    }
    
    @NotNull
    private DiagnosticAnnotation createKotlinAnnotation(@NotNull Diagnostic diagnostic, @NotNull IFile file) throws BadLocationException {
        TextRange range = diagnostic.getTextRanges().get(0);
        
        IDocument document = EditorUtil.getDocument(file);
        int offset = LineEndUtil.convertLfToDocumentOffset(diagnostic.getPsiFile().getText(), 
                range.getStartOffset(), document);
        
        int lineOfOffset = 0;
        try {
            lineOfOffset = document.getLineOfOffset(offset);
        } catch (BadLocationException e) {
            throw e;
            //KotlinLogger.logAndThrow(e);
        }
        
        return new DiagnosticAnnotation(lineOfOffset,
                offset,
                range.getLength(),
                getAnnotationType(diagnostic.getSeverity()), 
                DefaultErrorMessages.render(diagnostic),
                diagnostic.getPsiElement().getText(),
                file,
                diagnostic);
    }
    
    @NotNull
    private String getAnnotationType(@NotNull Severity severity) {
        String annotationType = null;
        switch (severity) {
            case ERROR:
                annotationType = AnnotationManager.ANNOTATION_ERROR_TYPE;
                break;
            case WARNING:
                annotationType = AnnotationManager.ANNOTATION_WARNING_TYPE;
                break;
            case INFO:
                throw new UnsupportedOperationException("Diagnostics with severith 'INFO' are not supported");
        }
        
        assert annotationType != null;
        
        return annotationType;
    }
    
    public void updateAnnotations(
            @NotNull AbstractTextEditor editor, 
            @NotNull Map<IFile, List<DiagnosticAnnotation>> annotations) {
        List<DiagnosticAnnotation> newAnnotations;
        IFile file = EditorUtil.getFile(editor);
        if (file != null && annotations.containsKey(file)) {
            newAnnotations = annotations.get(file);
            assert newAnnotations != null : "Null element in annotations map for file " + file.getName();
        } else {
            newAnnotations = Collections.emptyList();
        }
        
        AnnotationManager.INSTANCE.updateAnnotations(editor, newAnnotations);
    }
    
    @Nullable
    public DiagnosticAnnotation getAnnotationByOffset(@NotNull AbstractTextEditor editor, int offset) {
        IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        Iterator<?> annotationIterator = annotationModel.getAnnotationIterator();
        while (annotationIterator.hasNext()) {
            Annotation annotation = (Annotation) annotationIterator.next();
            if (annotation instanceof DiagnosticAnnotation) {
                DiagnosticAnnotation diagnosticAnnotation = (DiagnosticAnnotation) annotation;
                
                if (diagnosticAnnotation.getOffset() <= offset && offset <= DiagnosticAnnotationKt.getEndOffset(diagnosticAnnotation)) {
                    return diagnosticAnnotation;
                }
            }
        }
        
        return null;
    }
    
    @Nullable
    public IMarker getMarkerByOffset(@NotNull IFile file, int offset) {
        try {
            IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            for (IMarker marker: markers) {
                int startOffset = (int) marker.getAttribute(IMarker.CHAR_START);
                int endOffset = (int) marker.getAttribute(IMarker.CHAR_END);
                if (startOffset <= offset && offset <= endOffset) {
                    return marker;
                }
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
    
    public static boolean isQuickFixable(@NotNull DiagnosticFactory<?> diagnostic) {
        return isUnresolvedReference(diagnostic);
    }

    public static boolean isUnresolvedReference(@NotNull DiagnosticFactory<?> diagnostic) {
        return Errors.UNRESOLVED_REFERENCE.equals(diagnostic);
    }
}
