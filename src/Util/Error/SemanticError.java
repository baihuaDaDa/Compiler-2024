package Util.Error;

import Util.Position;

public class SemanticError extends Error {
    public SemanticError(String type, String msg, Position pos) {
        super(type, msg, pos);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
