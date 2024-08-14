package Util.Error;

import Util.Position;

public class SemanticError extends Error {
    public SemanticError(String msg, Position pos) {
        super(msg, pos);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
