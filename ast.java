import java.io.*;
import java.util.*;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a C-- program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of 
// children) or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Kids
//     --------            ----
//     ProgramNode         DeclListNode
//     DeclListNode        linked list of DeclNode
//     DeclNode:
//       VarDeclNode       TypeNode, IdNode, int
//       FnDeclNode        TypeNode, IdNode, FormalsListNode, FnBodyNode
//       FormalDeclNode    TypeNode, IdNode
//       StructDeclNode    IdNode, DeclListNode
//
//     FormalsListNode     linked list of FormalDeclNode
//     FnBodyNode          DeclListNode, StmtListNode
//     StmtListNode        linked list of StmtNode
//     ExpListNode         linked list of ExpNode
//
//     TypeNode:
//       IntNode           -- none --
//       BoolNode          -- none --
//       VoidNode          -- none --
//       StructNode        IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       RepeatStmtNode      ExpNode, DeclListNode, StmtListNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       DotAccessNode       ExpNode, IdNode
//       AssignNode          ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of kids, or
// internal nodes with a fixed number of kids:
//
// (1) Leaf nodes:
//        IntNode,   BoolNode,  VoidNode,  IntLitNode,  StrLitNode,
//        TrueNode,  FalseNode, IdNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of kids:
//        ProgramNode,     VarDeclNode,     FnDeclNode,     FormalDeclNode,
//        StructDeclNode,  FnBodyNode,      StructNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, ReadStmtNode,   WriteStmtNode   
//        IfStmtNode,      IfElseStmtNode,  WhileStmtNode,  RepeatStmtNode,
//        CallStmtNode
//        ReturnStmtNode,  DotAccessNode,   AssignExpNode,  CallExpNode,
//        UnaryExpNode,    BinaryExpNode,   UnaryMinusNode, NotNode,
//        PlusNode,        MinusNode,       TimesNode,      DivideNode,
//        AndNode,         OrNode,          EqualsNode,     NotEqualsNode,
//        LessNode,        GreaterNode,     LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
// ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode { 
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent);

    // this method can be used by the unparse methods to do indenting
    protected void doIndent(PrintWriter p, int indent) {
        for (int k = 0; k < indent; k++) p.print(" ");
    }
    
    // Whenever an error is thrown this method is called
    public void errorHandler(String msg) {
	System.err.println(msg);
	
	// Even if this function is called once set the name analysis status
	// to false
	ErrMsg.setNameAnalysisStatus(false);
    }

    public void checkSymbolTable(IdNode id, Sym sym, SymTable symTable) {
	////System.out.println("--- Inside symbol table ---");


	String idName = id.getIdName();
	if (id.getIdNodeSym().isStructType()) {
	    idName = "struct " + idName; 
	}

	////System.out.println("idName in checkSymbolTable: " + idName);

	try {
	    symTable.addDecl(idName, sym);
	    id.setValid(true);
	    ////System.out.println(sym.getType()); 
	    //id.setIdNodeSym(sym);
	    ////System.out.println(id.getIdNodeSym().getType()); 
	} catch (EmptySymTableException e) {
	    id.error("Scope error in checkSymbolTable");
	    id.setValid(false);
	} catch (DuplicateSymException e) {
	    id.error("Multiply declared identifier");
	    id.setValid(false);
	} catch (WrongArgumentException e) {
	    id.error("Wrong argument in checkSymbolTable");
	    id.setValid(false);
	}

	//symTable.print();

	////System.out.println("--- Done symbol table ---");
    }

}

// **********************************************************************
// ProgramNode,  DeclListNode, FormalsListNode, FnBodyNode,
// StmtListNode, ExpListNode
// **********************************************************************

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;
	symTable = new SymTable();
    }

    public void nameAnalysis() {
	myDeclList.nameAnalysis(symTable);
    
	//System.out.println("====== Final Symbol table =====");
	//symTable.print();
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }

    /* This function is used to check if the name analysis 
     * was successful before proceeding with unparsing the
     * the input
     */
    public boolean getNameAnalysisStatus() {
	return ErrMsg.getNameAnalysisStatus();
    }	

    // 1 kid
    private DeclListNode myDeclList;
    private SymTable symTable;
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("--- Inside name analysis DeclListNode ---");
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode)it.next()).nameAnalysis(symTable);
            }
        } catch (NoSuchElementException ex) {
            errorHandler("unexpected NoSuchElementException in DeclListNode.nameAnalysis");
        }
	//System.out.println("--- Done with DeclListNode ---\n");
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode)it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    public HashMap<String, Sym> getVarDeclaration(SymTable symTable) {
	//System.out.println("\n--- Inside getVarDeclaration ---");

	HashMap<String, Sym> map = new HashMap<String, Sym>();
        Iterator it = myDecls.iterator();
        
	
	symTable.addScope();
	while(it.hasNext()) {
	    
	    VarDeclNode node = (VarDeclNode)(it.next());
	    
	    // Checking if the current var declaration is valid
	    
	    //System.out.println("Name analysis: ");
	    node.nameAnalysis(symTable);

	    //System.out.println("Getting the id\n");
	    
	    
	    if (node.getValid()) {
		IdNode id = node.getIdNode();
	    
		Sym sym;

		// Id a struct declaration
		if (id.getIdNodeSym().isStructType()) {
		    sym = new Sym(node.getType().getType());
		    sym.setStructScopeVariables(id.getIdNodeSym().getStructScopeVariables());
		    sym.setStructType(true);
		} else {
		    // Id bool or int (because name analysis on node is already done)
		    sym = new Sym(node.getType().getType());
		}

		// putting the values directly because name analysis is already done
		// in current scope
		map.put(id.getIdName(), sym);
		id.setIdNodeSym(sym);
	    } else {
		errorHandler("Error: Name analysis on VarDeclNode in structBody was unsuccessful");
	    }
	}

	//symTable.print();

	// Remove the new scope that was added
	try {
	    symTable.removeScope();
	} catch (EmptySymTableException ex) {
	    errorHandler("symtable empty in declListNode when called to add struct variables");
	}
	
	//System.out.println("--- Done with getVarDeclaration---\n");
    
	return map;
    }


    // list of kids (DeclNodes)
    private List<DeclNode> myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("--- Inside name analysis FormalsListNode ---");

	// Functions have their own scope
	symTable.addScope();

        // Checking parameters passed
	Iterator<FormalDeclNode> it = myFormals.iterator();
        try {
	    while (it.hasNext()) {  // print the rest of the list
		it.next().nameAnalysis(symTable);
	    }
        } catch (NoSuchElementException ex) {
            errorHandler("unexpected NoSuchElementException in FormalsListNode.nameAnalysis");
        }
    
	//System.out.println("--- Done with FormalsListNode ---");
    }

    public List<String> getFormalsNamesList(SymTable symTable) {	
	//System.out.println("\n--- Inside getFormalsNamesList ---");

	// This list will saved in Sym object
	List<String> formals = new ArrayList<String>();
	Iterator<FormalDeclNode> it = myFormals.iterator();
	try {
	    while (it.hasNext()) {
		FormalDeclNode node = it.next();
		String type = node.getType().getType();
		formals.add(type);
	    }
	} catch (NoSuchElementException ex) {
	    errorHandler("unexpected NoSuchElementException in FormalsListNode.getFormalsNamesList");
	}

	//System.out.println(Arrays.toString(formals.toArray()));

	//System.out.println("--- Done with getFormalsNamesList ---\n");
	return formals;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }

    // list of kids (FormalDeclNodes)
    private List<FormalDeclNode> myFormals;
}

class FnBodyNode extends ASTnode {
    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("--- Inside name analysis FnBodyNode ---");

	//System.out.println("=== Sym table at the start of fnbody ===");
	//symTable.print();

	
	myDeclList.nameAnalysis(symTable);
	myStmtList.nameAnalysis(symTable);
	
	// Remove the function scope after the function body is processed
	try {
	    symTable.removeScope();
	} catch (EmptySymTableException e) {
	    errorHandler("SymTable empty in Function Body");
	}
	//System.out.println("--- Done with FnBodyNode ---");
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }

    // 2 kids
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}
 
class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("--- Inside name analysis StmtListNode ---");
        Iterator<StmtNode> it = myStmts.iterator();
	try { 
	    while (it.hasNext()) {
		it.next().nameAnalysis(symTable);
	    }
        } catch (NoSuchElementException ex) {
            errorHandler("unexpected NoSuchElementException in StmtListNode.nameAnalysis");
        }
	//System.out.println("--- Done with StmtListNode ---");
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    }

    // list of kids (StmtNodes)
    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("--- Inside name analysis ExpListlNode ---");
	try {
	    Iterator<ExpNode> it = myExps.iterator();
            while (it.hasNext()) {  // print the rest of the list
                it.next().nameAnalysis(symTable);
            }    
        } catch (NoSuchElementException ex) {
            errorHandler("unexpected NoSuchElementException in ExpListNode.nameAnalysis");
        }
	//System.out.println("--- Done with ExpListlNode ---");
    }

    public void unparse(PrintWriter p, int indent) {
        System.out.println("Inside ExpListNode unparse. indent: " + indent);
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }

    // list of kids (ExpNodes)
    private List<ExpNode> myExps;
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    
    public void nameAnalysis(SymTable symTable) {
	////System.out.println("Inside name analysis DeclNode");
    }

}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("\n--- Inside name analysis VarDeclNode ---");
	
	Sym sym;
	String declType = myType.getType();

	// Valid declarations
	if (declType.equals("int") || declType.equals("bool")) {
	    
	    sym = new Sym(declType);
	    myId.setIdNodeSym(sym); 
	    checkSymbolTable(myId, sym, symTable);
	    valid = myId.getValid();

	} else if (declType.equals("void")) {
	    // Id of void type
	    
	    myId.error("Non-function declared void");
	    valid = false;

	} else {
	    // if struct type
	    
	    // Getting the idNode for struct
	    IdNode struct = ((StructNode)myType).getIdNode();

	    // Adding struct to the name of symbol
	    String structName = "struct " + struct.getIdName();
	    
	    //System.out.println("structName: " + structName + ", struct.getIdNode(): " + struct.getIdName() );

	    // Looking for the symbol
	    Sym structSym = symTable.lookupGlobal(structName);

	    if (structSym == null) {
		myId.error("Invalid name of struct type");
		valid = false;
	    
	    } else {
	   
		// setting the symbol type for struct
		struct.setIdNodeSym(structSym);

		// Setting the type of current declaration
		sym = new Sym(struct.getIdName());

		// Setting this symbol to be of struct type
		sym.setStructType(true);
		
		// TODO: Update the hashmap of struct
		sym.setStructScopeVariables(structSym.getStructScopeVariables());

		myId.setIdNodeSym(sym);


		valid = true;
	
		//System.out.println("Struct of given type found");
		//System.out.println("myId: " +  myId.getIdName());
		//System.out.println("sym type: " + sym.getType());


		// Add declaration in the symbol table
		try {
		    symTable.addDecl(myId.getIdName(), sym);
		} catch (EmptySymTableException e) {
		    myId.error("Scope error in varDeclNode");
		} catch (DuplicateSymException e) {
		    myId.error("Multiply declared identifier");
		} catch (WrongArgumentException e) {
		    myId.error("Wrong argument in varDeclNode");
		}
		
		//symTable.print();

	    }
	}
	//System.out.println("--- Done with VarDeclNode ---");
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.println(";");
    }

    public TypeNode getType() {
	return myType;
    }

    public IdNode getIdNode() {
	return myId; 
    }

    public boolean getValid() {
	return valid;
    }

    public void setValid(boolean v) {
	valid = v;
    }


    // 3 kids
    private TypeNode myType;
    private IdNode myId;
    private int mySize;  // use value NOT_STRUCT if this is not a struct type
    private boolean valid;

    public static int NOT_STRUCT = -1;
}

class FnDeclNode extends DeclNode {
    public FnDeclNode(TypeNode type,
                      IdNode id,
                      FormalsListNode formalList,
                      FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }
    
    public void nameAnalysis(SymTable symTable) {
	//System.out.println("\n--- Inside name analysis FnDeclNode ---");
	
	// Process the formals and Check if this id already 
	// exists either as variable or function

	List<String> formals = myFormalsList.getFormalsNamesList(symTable); 
	Sym newFuncSym = new Sym(myType.getType());
	newFuncSym.setFunctionType(true);
	newFuncSym.setFuncParamsList(formals);
	
	Sym oldSym = symTable.lookupLocal(myId.getIdName()); 

	// If there is a sumbol with same id	    
    	if (oldSym != null) {
	    if (oldSym.isFunctionType()) {
		//System.out.println("Same name function found in table");
		myFormalsList.nameAnalysis(symTable);
		myId.setProcFormals(true);
	    } else {
		// The symbol is a variable
		// Continue processing with function body
		//System.out.println("Same name variable found in table");
		symTable.addScope();
		myId.setProcFormals(false);
	    }	    
	    // oldSym is a variable throw an error 
	    myId.error("Multiply declared identifier");
	} else {
	    // If the new symbol generated does not exist in the current scope
	    myId.setProcFormals(true);

	    try {
		symTable.addDecl(myId.getIdName(), newFuncSym);
	    } catch (EmptySymTableException e) {
		errorHandler("Scope error in checkSymbolTable");
	    } catch (DuplicateSymException e) {
		errorHandler("Multiply declared identifier");
	    } catch (WrongArgumentException e) {
		errorHandler("Wrong argument in checkSymbolTable");
	    }
	    
	    
	    myId.setIdNodeSym(newFuncSym);
	    myFormalsList.nameAnalysis(symTable);
	}

	myBody.nameAnalysis(symTable);
	//symTable.print();

	//System.out.println("--- Done with FnDeclNode ---\n");
    }


    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent+4);
        p.println("}\n");
    }

    // 4 kids
    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("--- Inside name analysis FormalDeclNode ---");
	
	Sym sym;     
	if(myType.getType().equals("void")){
	    myId.error("Non-function declared void");
	} else {
	    sym = new Sym(myType.getType());
	    myId.setIdNodeSym(sym);
	    checkSymbolTable(myId, sym, symTable);
	}
	
	//System.out.println("--- Done with FormalDeclNode ---");
    }

    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
    }

    public TypeNode getType() {
	return myType;
    }

    public IdNode getId() {
	return myId;
    }

    // 2 kids
    private TypeNode myType;
    private IdNode myId;
}

class StructDeclNode extends DeclNode {
    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
    }


    public void nameAnalysis(SymTable symTable) {
	//System.out.println("\n---Inside name analysis StructDeclNode---");
	
	//String structName = "struct " + myId.getIdName();
	
	// Id should be of type struct
	Sym sym = new Sym("struct");

	sym.setStructType(true);

	// TODO: Update the hashmap of this struct
	sym.setStructScopeVariables(myDeclList.getVarDeclaration(symTable));

	// Checking if correct variables stored in struct sym scope
	//System.out.println("Scope variables for struct");
	//System.out.println(Arrays.asList(sym.getStructScopeVariables()));	

	myId.setIdNodeSym(sym);
	checkSymbolTable(myId, sym, symTable);
	
	//System.out.println("The current symbol table:");
	//symTable.print();
	
	//System.out.println("---Done with structDeclNode---\n");
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("struct ");
        myId.unparse(p, 0);
        p.println("{");
        myDeclList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("};\n");

    }

    // 2 kids
    private IdNode myId;
    private DeclListNode myDeclList;
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
    abstract String getType();
}

class IntNode extends TypeNode {
    public IntNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }
    
    public String getType() {
	return "int";	
    }
}

class BoolNode extends TypeNode {
    public BoolNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }
    
    public String getType() {
	return "bool";	
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }
    
    public String getType() {
	return "void";	
    }
}

class StructNode extends TypeNode {
    public StructNode(IdNode id) {
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
        myId.unparse(p, 0);
    }
    
    
    public String getType() {
	return myId.getIdName();	
    }

    public IdNode getIdNode() {
	return myId;
    }
    
    // 1 kid
    private IdNode myId;
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    
    public void nameAnalysis(SymTable symTable) {
	//System.out.println("Inside name analysis StmtNode");
    }

}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignNode assign) {
        myAssign = assign;
    }

    public void nameAnalysis(SymTable symTable) {
	myAssign.nameAnalysis(symTable);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(";");
    }

    // 1 kid
    private AssignNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(SymTable symTable) {
	myExp.nameAnalysis(symTable);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, -1);
        p.println("++;");
    }

    // 1 kid
    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(SymTable symTable) {
	myExp.nameAnalysis(symTable);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, -1);
        p.println("--;");
    }

    // 1 kid
    private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }
    
    public void nameAnalysis(SymTable symTable) {
	myExp.nameAnalysis(symTable);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cin >> ");
        myExp.unparse(p, -1);
        p.println(";");
    }

    // 1 kid (actually can only be an IdNode or an ArrayExpNode)
    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }
    
    public void nameAnalysis(SymTable symTable) {
	myExp.nameAnalysis(symTable);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cout << ");
        myExp.unparse(p, -1);
        p.println(";");
    }

    // 1 kid
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("Inside name analysis IfStmtNode");
	myExp.nameAnalysis(symTable);

	//if has its own scope
	symTable.addScope();

	myDeclList.nameAnalysis(symTable);
	myStmtList.nameAnalysis(symTable);

	try {
	    symTable.removeScope();
	} catch (EmptySymTableException e) {
	    errorHandler("Unable to remove scope after if in ifStmtNode");
	}
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, -1);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    // e kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
                          StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("Inside name analysis IfElseStmtNode");
	myExp.nameAnalysis(symTable);

	//if has its own scope
	symTable.addScope();

	myThenDeclList.nameAnalysis(symTable);
	myThenStmtList.nameAnalysis(symTable);

	try {
	    symTable.removeScope();
	} catch (EmptySymTableException e) {
	   errorHandler("Unable to remove scope after if in ifElseStmtNode");
	}

	//else its own scope
	symTable.addScope();

	myElseDeclList.nameAnalysis(symTable);
	myElseStmtList.nameAnalysis(symTable);

	try {
	    symTable.removeScope();
	} catch (EmptySymTableException e) {
	    errorHandler("Unable to remove scope after else in ifElseStmtNode");
	}

    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, -1);
        p.println(") {");
        myThenDeclList.unparse(p, indent+4);
        myThenStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
        doIndent(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent+4);
        myElseStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");        
    }

    // 5 kids
    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }
    
    public void nameAnalysis(SymTable symTable) {
	//System.out.println("Inside name analysis WhileStmtNode");
	myExp.nameAnalysis(symTable);

	//while has its own scope
	symTable.addScope();

	myDeclList.nameAnalysis(symTable);
	myStmtList.nameAnalysis(symTable);

	try {
	    symTable.removeScope();
	} catch (EmptySymTableException e) {
	    errorHandler("Unable to remove scope after while");
	}

    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("while (");
        myExp.unparse(p, -1);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    // 3 kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class RepeatStmtNode extends StmtNode {
    public RepeatStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }
	
    public void nameAnalysis(SymTable symTable) {
	//System.out.println("Inside name analysis RepeatStmtNode");
	myExp.nameAnalysis(symTable);

	//Repeat has its own scope
	symTable.addScope();

	myDeclList.nameAnalysis(symTable);
	myStmtList.nameAnalysis(symTable);

	try {
	    symTable.removeScope();
	} catch (EmptySymTableException e) {
	    errorHandler("Unable to remove scope after repeat");
	}

    }

    public void unparse(PrintWriter p, int indent) {
	doIndent(p, indent);
        p.print("repeat (");
        myExp.unparse(p, -1);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    // 3 kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("Inside name analysis CallStmtNode");
	myCall.nameAnalysis(symTable);
    }

    public void unparse(PrintWriter p, int indent) {
        System.out.println("Inside CallStmtNode unparse");
	doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    }

    // 1 kid
    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("Inside name analysis ReturnStmtNode");
	if (myExp != null) {
	    myExp.nameAnalysis(symTable);
	}
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, -1);
        }
        p.println(";");
    }

    // 1 kid
    private ExpNode myExp; // possibly null
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("Inside name analysis ExpNode");
    }
    
    public boolean isIdNode() {
	return false;
    }


}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
}

class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    }

    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void nameAnalysis(SymTable symTable) {
	////System.out.println("$$$$$$$$$ Inside name analysis Idnode\n");
	mySym = symTable.lookupLocal(myStrVal);
	if (mySym == null) {
	    mySym = symTable.lookupGlobal(myStrVal);
	    if (mySym == null) {
		error("Undeclared identifier");
	    }
	}
    }

    public void unparse(PrintWriter p, int indent) {
        // Using Indent Value to print the type correctly
	if (indent == -1) {
	    p.print(myStrVal);
	    if (mySym != null) {
		p.print("(" + mySym.getType() +")");
	    }
	} else if (indent == -2) {
	    //p.print(myStrVal);
	    if (mySym != null) {
		p.print(mySym.getType());
	    }
	} else {
	    p.print(myStrVal);
	}
    }

    public String getIdName() {
	return myStrVal;
    }

    public int getCharNum() {
	return myCharNum;
    }

    public int getLineNum() {
	return myLineNum;
    }

    public void setIdNodeSym(Sym sym) {
	mySym = sym;
    }

    public Sym getIdNodeSym() {
	return mySym;
    }
    
    public void setValid(boolean v) {
	valid = v;
    }
    
    public boolean getValid() {
	return valid;
    }
    
    public void setProcFormals(boolean f) {
	procFormals = f;
    }
    
    public boolean getProcFormals() {
	return procFormals;
    }
    
    public void setProcBody(boolean f) {
	procBody = f;
    }
    
    public boolean getProcBody() {
	return procBody;
    }
    
    public void error(String msg) {
	ErrMsg.fatal(myLineNum, myCharNum, msg); 
    }
    
    public boolean isIdNode() {
	return true;
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private Sym mySym;
    private boolean valid;
    private boolean procFormals;
    private boolean procBody;
}

class DotAccessExpNode extends ExpNode {
    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;    
        myId = id;
    }

    public void nameAnalysis(SymTable symTable) {
	//System.out.println("--- Inside name analysis Dot Access node ---\n");
	
	Sym sym = Loc(symTable, myLoc, myId);
	
	if (sym != null) {
	    //System.out.println("Sym: " + sym.getType());
	    myId.setIdNodeSym(sym);
	}

	//System.out.println("--- Done with Dot Access node ---\n");
    }

    public Sym Loc(SymTable symTable, ExpNode loc, IdNode id) {

	//System.out.println("myLoc: " + myLoc.isIdNode() + " myId: " + myId.isIdNode());
    
	if (!loc.isIdNode()) {
	    // We need to go to next level
	    DotAccessExpNode lhs = (DotAccessExpNode)loc;
	    ExpNode newLoc = lhs.getLoc();
	    IdNode newIdNode = lhs.getIdNode();

	    Sym sym = Loc(symTable, newLoc, newIdNode);
	    
	    String idName;
	    if (sym != null) {
		newIdNode.setIdNodeSym(sym);
		if (sym.isStructType()) {
		    idName = "struct " + sym.getType();
		    IdNode newNewLoc = new IdNode(0, 0, idName);
		    newNewLoc.setIdNodeSym(sym);
		    sym = Loc(symTable, newNewLoc, id);
		    id.setIdNodeSym(sym);
		} else {
		    newIdNode.error("Dot-access of non-struct type");
		}
	    } else {
		newIdNode.error("Undeclared identifier");
	    }

	    return sym;

	} else {
	    // Its just an Id
	    IdNode lhsId = (IdNode)loc;

	    Sym sym = symTable.lookupGlobal(lhsId.getIdName());
	    lhsId.setIdNodeSym(sym);

	    if (sym == null) {
		// If the lhs side symbol does not exist
		lhsId.error("Undeclared identifier");
	    } else if (!sym.isStructType()) {
		// If lhs is not of struct type
		lhsId.error("Dot-access of non-struct type");
	    } else {
		// If lhs is of struct type. we have to check whether
		// the scope of struct has rhs as valid variable

		HashMap<String, Sym> map = sym.getStructScopeVariables();
		if (!map.containsKey(id.getIdName())) {
		    id.error("Invalid struct field name");
		} else {
		    return map.get(id.getIdName());
		}
	    }

	}
   
	return null;
    }


    public void unparse(PrintWriter p, int indent) {
	////System.out.println("### Inside dotaccess node ###");
	//p.print("(");
        myLoc.unparse(p, -1);
        //p.print(").");
        p.print(".");
        myId.unparse(p, -1);
    }


    public ExpNode getLoc() {
	return myLoc;
    }

    public IdNode getIdNode() {
	return myId;
    }

    // 2 kids
    private ExpNode myLoc;    
    private IdNode myId;
}

class AssignNode extends ExpNode {
    public AssignNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }

    public void nameAnalysis(SymTable symTable) {
	myLhs.nameAnalysis(symTable);
	myExp.nameAnalysis(symTable);
    }

    public void unparse(PrintWriter p, int indent) {
        //if (indent != -1)  p.print("(");
        myLhs.unparse(p, -1);
        p.print(" = ");
        myExp.unparse(p, -1);
        //if (indent != -1)  p.print(")");
    }

    // 2 kids
    private ExpNode myLhs;
    private ExpNode myExp;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    public void nameAnalysis(SymTable symTable) {
        System.out.println("Inside CallExpNode unparse");
	type = symTable.lookupGlobal(myId.getIdName());

	if (type == null) {
	    myId.error("Undeclared Identifier");
	}

	myExpList.nameAnalysis(symTable);
    }

    // ** unparse **
    public void unparse(PrintWriter p, int indent) {
        myId.unparse(p, 0);
        p.print("(");
        //if (myExpList != null) {
	    myExpList.unparse(p, -2);
        //}
	p.print("->");
	p.print(type.getType());
        p.print(")");

	p.print("(");
	if (myExpList != null) {
	    myExpList.unparse(p, -1);
	}
	p.print(")");
    }

    // 2 kids
    private IdNode myId;
    private ExpListNode myExpList;  // possibly null
    private Sym type;
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(SymTable symTable) {
	myExp.nameAnalysis(symTable);
    }

    // one child
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }

    public void nameAnalysis(SymTable symTable) {
	myExp1.nameAnalysis(symTable);
	myExp2.nameAnalysis(symTable);
    }

    // two kids
    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("-");
        myExp.unparse(p, -1);
        //p.print(")");
    }
}

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("!");
        myExp.unparse(p, -1);
        //p.print(")");
    }
}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" + ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class MinusNode extends BinaryExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" - ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class TimesNode extends BinaryExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" * ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class DivideNode extends BinaryExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" / ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class AndNode extends BinaryExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" && ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class OrNode extends BinaryExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" || ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class EqualsNode extends BinaryExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" == ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class NotEqualsNode extends BinaryExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" != ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class LessNode extends BinaryExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" < ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class GreaterNode extends BinaryExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" > ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class LessEqNode extends BinaryExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" <= ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}

class GreaterEqNode extends BinaryExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        //p.print("(");
        myExp1.unparse(p, -1);
        p.print(" >= ");
        myExp2.unparse(p, -1);
        //p.print(")");
    }
}
