package com.redhat.ceylon.eclipse.code.refactor;

import static com.redhat.ceylon.eclipse.util.FindUtils.getAbstraction;
import static org.eclipse.ltk.core.refactoring.RefactoringStatus.createWarningStatus;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.texteditor.ITextEditor;

import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Alias;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Identifier;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;

public class EnterAliasRefactoring extends AbstractRefactoring {
    
    private String newName;
    private Tree.ImportMemberOrType element;
    
    public Tree.ImportMemberOrType getElement() {
        return element;
    }
    
    public EnterAliasRefactoring(ITextEditor editor) {
        super(editor);
        if (node instanceof Tree.ImportMemberOrType) {
            element = (Tree.ImportMemberOrType) node;
            final Alias alias = element.getAlias();
            if (alias==null) {
                newName = element.getIdentifier().getText();
            }
            else {
                newName = alias.getIdentifier().getText();
            }
        }
    }
    
    @Override
    public boolean isEnabled() {
        return node instanceof Tree.ImportMemberOrType &&
                ((Tree.ImportMemberOrType) node).getDeclarationModel()!=null;
    }

    public String getName() {
        return "Enter Alias";
    }

    public boolean forceWizardMode() {
        Declaration existing = node.getScope()
                .getMemberOrParameter(node.getUnit(), newName, null, false);
        return existing!=null;
    }
    
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        // Check parameters retrieved from editor context
        return new RefactoringStatus();
    }

    public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        Declaration existing = node.getScope()
                .getMemberOrParameter(node.getUnit(), newName, null, false);
        if (null!=existing) {
            return createWarningStatus("An existing declaration named '" +
                    newName + "' already exists in the same scope");
        }
        return new RefactoringStatus();
    }

    public TextChange createChange(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        TextChange tfc = newLocalChange();
        renameInFile(tfc);
        return tfc;
    }
    
    int renameInFile(final TextChange tfc) throws CoreException {
        tfc.setEdit(new MultiTextEdit());
        Tree.ImportMemberOrType element = (Tree.ImportMemberOrType) node;
        Tree.Alias alias = element.getAlias();
        final Declaration dec = element.getDeclarationModel();
        
        final int adjust;
        boolean same = newName.equals(dec.getName());
        if (alias==null) {
//            if (!same) {
                tfc.addEdit(new InsertEdit(element.getStartIndex(), 
                        newName + "="));
                adjust = newName.length()+1;
//            }
//            else {
//                adjust = 0;
//            }
        }
        else {
            int start = alias.getStartIndex();
            int length = alias.getIdentifier().getText().length();
            if (same) {
                int stop = element.getIdentifier().getStartIndex();
                tfc.addEdit(new DeleteEdit(start, stop-start));
                adjust = start - stop; 
            }
            else {
                tfc.addEdit(new ReplaceEdit(start, length, newName));
                adjust = newName.length()-length;
            }
        }
        
        new Visitor() {
            @Override
            public void visit(Tree.StaticMemberOrTypeExpression that) {
                super.visit(that);
                addEdit(document, that.getIdentifier(), 
                        that.getDeclaration());
            }
            @Override
            public void visit(Tree.SimpleType that) {
                super.visit(that);
                addEdit(document, that.getIdentifier(), 
                        that.getDeclarationModel());
            }
            @Override
            public void visit(Tree.MemberLiteral that) {
                super.visit(that);
                addEdit(document, that.getIdentifier(), 
                        that.getDeclaration());
            }
            private void addEdit(final IDocument document,
                    Identifier id, Declaration d) {
                if (id!=null && d!=null && 
                        dec.equals(getAbstraction(d))) {
                    int pos = id.getStartIndex();
                    int len = id.getText().length();
                    tfc.addEdit(new ReplaceEdit(pos, len, newName));
                }
            }
        }.visit(rootNode);
        
        return adjust;
        
    }

    public void setNewName(String text) {
        newName = text;
    }
    
    public String getNewName() {
        return newName;
    }
    
}