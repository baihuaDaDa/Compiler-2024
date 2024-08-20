package IR;

import IR.Instruction.Instruction;
import IR.Module.FuncDefMod;

import java.util.ArrayList;

public class IRBlock {
    public ArrayList<Instruction> instructions = null;
    public FuncDefMod parent;
}
