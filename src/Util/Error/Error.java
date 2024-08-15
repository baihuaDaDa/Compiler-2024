package Util.Error;

import Util.Position;

public class Error extends RuntimeException {
    protected Position pos = null;
    protected String type = null;
    protected String msg = null;

    public Error(String type, String msg, Position pos) {
        this.type = type;
        this.msg = msg;
        this.pos = pos;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + msg + " at " + pos.toString();
    }

    public String errorType() {
        return type;
    }
}
