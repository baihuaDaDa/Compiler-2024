package IR.Module;

import IR.IRVisitor;
import Util.Type.IRType;

import java.util.ArrayList;

public class StructDefMod extends Module {
    public String typename;
    public ArrayList<IRType> members;

    public StructDefMod(String typename) {
        this.typename = typename;
        members = new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("%").append(typename).append(" = type { ");
        for (int i = 0; i < members.size(); ++i) {
            ret.append(members.get(i));
            if (i != members.size() - 1)
                ret.append(", ");
        }
        ret.append(" }\n");
        return ret.toString();
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
