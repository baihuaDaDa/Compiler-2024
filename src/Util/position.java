package Util;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class position {
    private int row, col;

    public position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public position() {}

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }
}
