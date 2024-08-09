package Util.Error;

import Util.Position;

abstract public class Error extends RuntimeException {
    public Position pos = null;
    public String msg = null;

    public Error(String msg, Position pos) {
        this.msg = msg;
        this.pos = pos;
    }

    @Override
    public String toString() {
        return msg + " at " + pos.toString();
    }
}
