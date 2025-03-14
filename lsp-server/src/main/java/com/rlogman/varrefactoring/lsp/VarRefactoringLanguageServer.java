package com.rlogman.varrefactoring.lsp;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.concurrent.CompletableFuture;

/**
 * LSP server for Java var refactoring.
 * This can be used to provide the refactoring functionality to editors
 * that support Language Server Protocol.
 */
public class VarRefactoringLanguageServer implements LanguageServer {
    
    private final VarRefactoringTextDocumentService textDocumentService;
    private final VarRefactoringWorkspaceService workspaceService;
    
    public VarRefactoringLanguageServer() {
        this.textDocumentService = new VarRefactoringTextDocumentService();
        this.workspaceService = new VarRefactoringWorkspaceService();
    }
    
    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        // Set up server capabilities
        ServerCapabilities capabilities = new ServerCapabilities();
        
        // Configure which document events the server is interested in
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        
        // Support code actions for refactoring
        capabilities.setCodeActionProvider(true);
        
        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }
    
    @Override
    public CompletableFuture<Object> shutdown() {
        // Clean up resources here
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void exit() {
        // Shut down the server
        System.exit(0);
    }
    
    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }
    
    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }
    
    /**
     * Main entry point for the LSP server.
     */
    public static void main(String[] args) {
        // Start the language server
        VarRefactoringLanguageServer server = new VarRefactoringLanguageServer();
        
        // Connect input/output for LSP communication
        // This follows the standard LSP setup for communication
        var launcher = org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher(
            server, 
            System.in, 
            System.out
        );
        
        // Start listening for requests
        launcher.startListening();
    }
}