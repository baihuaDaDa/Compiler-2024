package Util.Error;

import Util.Position;

public class SyntaxError extends Error {
    public SyntaxError(String type, String msg, Position pos) {
        super(type, msg, pos);
    }

    @Override
    public String toString() {
        return "[Invalid Identifier] " + super.toString();
    }
}
