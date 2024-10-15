package Util;

import java.util.HashMap;

public class PhysicalReg {
    public String name;
    public static PhysicalReg[] regFile;
    public static HashMap<String, Integer> regMap;

    public PhysicalReg(String name) {
        this.name = name;
    }

    static {
        regFile = new PhysicalReg[32];
        regMap = new HashMap<>();
        regFile[0] = new PhysicalReg("zero");
        regMap.put("zero", 0);
        regFile[1] = new PhysicalReg("ra");
        regMap.put("ra", 1);
        regFile[2] = new PhysicalReg("sp");
        regMap.put("sp", 2);
        regFile[3] = new PhysicalReg("gp");
        regMap.put("gp", 3);
        regFile[4] = new PhysicalReg("tp");
        regMap.put("tp", 4);
        regFile[5] = new PhysicalReg("t0");
        regMap.put("t0", 5);
        regFile[6] = new PhysicalReg("t1");
        regMap.put("t1", 6);
        regFile[7] = new PhysicalReg("t2");
        regMap.put("t2", 7);
        regFile[8] = new PhysicalReg("s0");
        regMap.put("s0", 8);
        regFile[9] = new PhysicalReg("s1");
        regMap.put("s1", 9);
        for (int i = 0; i < 8; ++i) {
            regFile[i + 10] = new PhysicalReg("a" + i);
            regMap.put("a" + i, i + 10);
        }
        for (int i = 2; i < 12; ++i) {
            regFile[i + 16] = new PhysicalReg("s" + i);
            regMap.put("s" + i, i + 16);
        }
        for (int i = 3; i < 7; ++i) {
            regFile[i + 25] = new PhysicalReg("t" + i);
            regMap.put("t" + i, i + 25);
        }
    }

    public static PhysicalReg get(String name) {
        return regFile[getNo(name)];
    }

    public static int getNo(String name) {
        return regMap.get(name);
    }

    public static boolean isCalleeSaved(String name) {
        return name.charAt(0) == 's';
    }

    @Override
    public String toString() {
        return name;
    }
}
