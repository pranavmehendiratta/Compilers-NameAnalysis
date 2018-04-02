import java.util.*;

public class Sym {
    private String type;
    private HashMap<String, Sym> structScopeVariables;
    private List<String> funcParamsList;
    private boolean struct;
    private boolean function;
    
    public Sym(String type) {
        this.type = type;
	this.structScopeVariables = null;
	this.funcParamsList = null;
	this.struct = false;
	this.function = false;
    }
    
    public String getType() {
        return type;
    }
    
    public String toString() {
        return type;
    }

    public void setStructType(boolean struct) {
	this.struct = struct;
    }

    public boolean isStructType() {
	return this.struct;
    }

    public void setFunctionType(boolean function) {
	this.function = function;
    }

    public boolean isFunctionType() {
	return this.function;
    }

    public void setStructScopeVariables(HashMap<String, Sym> structVariables) {
	this.structScopeVariables = structVariables;
    }

    public void setFuncParamsList(List<String> parametersList) {
	this.funcParamsList = parametersList;
    }

    public HashMap<String, Sym> getStructScopeVariables() {
	return this.structScopeVariables;
    }

    public List<String> getFuncParamsList() {
	return this.funcParamsList;
    }
}
