package IR;

import IR.Module.FuncDefMod;
import IR.Module.GlobalVarDefMod;
import IR.Module.StructDefMod;

import java.util.ArrayList;

public class IRProgram {
    public ArrayList<StructDefMod> structDefs = null;
    public ArrayList<FuncDefMod> funcDefs = null;
    public ArrayList<GlobalVarDefMod> globalVarDefs = null;

    public IRProgram() {
        structDefs = new ArrayList<>();
        funcDefs = new ArrayList<>();
        globalVarDefs = new ArrayList<>();
    }

    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
