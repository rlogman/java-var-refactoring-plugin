package com.rlogman.varrefactoring.lsp;

import com.rlogman.varrefactoring.core.RefactoringOptions;
import com.rlogman.varrefactoring.core.VarRefactoringProcessor;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LSP text document service for handling Java var refactoring operations.
 */
public class VarRefactoringTextDocumentService implements TextDocumentService {
    
    private final VarRefactoringProcessor processor;
    private final Map<String, String> documentContents = new HashMap<>();
    private final String javaVersion = "11"; // Default Java version
    
    public VarRefactoringTextDocumentService() {
        // Create refactoring processor with default options
        this.processor = new VarRefactoringProcessor(new RefactoringOptions());
    }
    
    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }
    
    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        List<Either<Command, CodeAction>> codeActions = new ArrayList<>();
        
        // Check if we're dealing with a Java file
        String uri = params.getTextDocument().getUri();
        if (!uri.endsWith(".java")) {
            return CompletableFuture.completedFuture(codeActions);
        }
        
        // Create a code action for converting to var
        CodeAction convertToVarAction = new CodeAction("Convert to 'var'");
        convertToVarAction.setKind(CodeActionKind.RefactorRewrite);
        
        // Create a command for this action
        Command command = new Command("Convert to 'var'", "java.var.refactor", 
                                      Collections.singletonList(uri));
        convertToVarAction.setCommand(command);
        
        codeActions.add(Either.forRight(convertToVarAction));
        
        return CompletableFuture.completedFuture(codeActions);
    }
    
    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        // Store the document content
        documentContents.put(params.getTextDocument().getUri(), 
                           params.getTextDocument().getText());
    }
    
    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        // Update the document content
        List<TextDocumentContentChangeEvent> changes = params.getContentChanges();
        if (!changes.isEmpty()) {
            // Use the latest change's full content
            String newContent = changes.get(changes.size() - 1).getText();
            documentContents.put(params.getTextDocument().getUri(), newContent);
        }
    }
    
    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        // Remove document from the cache
        documentContents.remove(params.getTextDocument().getUri());
    }
    
    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // Nothing to do on save
    }
    
    /**
     * Process a document for var refactoring.
     * This would be called when the user triggers the refactoring action.
     * 
     * @param uri Document URI
     * @return WorkspaceEdit with the changes
     */
    public WorkspaceEdit processDocument(String uri) {
        String content = documentContents.get(uri);
        if (content == null) {
            return new WorkspaceEdit();
        }
        
        // Process the content to replace explicit types with 'var'
        String processedContent = processor.processFile(content, javaVersion);
        
        // Create a workspace edit with the changes
        WorkspaceEdit edit = new WorkspaceEdit();
        
        // Create a text edit for the entire document
        TextEdit textEdit = new TextEdit(
            new Range(new Position(0, 0), new Position(Integer.MAX_VALUE, 0)),
            processedContent
        );
        
        // Add the edit to the workspace edit
        Map<String, List<TextEdit>> changes = new HashMap<>();
        changes.put(uri, Collections.singletonList(textEdit));
        edit.setChanges(changes);
        
        return edit;
    }
    
    // Required methods from TextDocumentService interface with minimal implementations
    
    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
    }
    
    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return CompletableFuture.completedFuture(unresolved);
    }
    
    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        return CompletableFuture.completedFuture(new Hover());
    }
    
    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
        return CompletableFuture.completedFuture(new SignatureHelp());
    }
    
    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> declaration(DeclarationParams params) {
        return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
    }
    
    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
    }
    
    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(TypeDefinitionParams params) {
        return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
    }
    
    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> implementation(ImplementationParams params) {
        return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
    }
    
    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    // These methods are not required by the TextDocumentService interface
    // so we'll remove them
    
    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        return CompletableFuture.completedFuture(unresolved);
    }
    
    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        return CompletableFuture.completedFuture(new WorkspaceEdit());
    }
    
    @Override
    public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<DocumentLink> documentLinkResolve(DocumentLink params) {
        return CompletableFuture.completedFuture(params);
    }
    
    @Override
    public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<List<ColorPresentation>> colorPresentation(ColorPresentationParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}