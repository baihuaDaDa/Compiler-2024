package Backend.ASMOptimizer;

import Backend.ASMOptimizer.Util.LiveAnalyzer;
import Util.PhysicalReg;
import IR.IRProgram;
import IR.Module.FuncDefMod;

import java.util.*;

public class LinearScanner {
    private final IRProgram program;
    private static final ArrayList<PhysicalReg> regList;

    static {
        // TODO 什么样的寄存器分配顺序是最优的？
        regList = new ArrayList<>();
        // s0-11, t2-6 (t0-1 are used to store temporary value), gp (aggressive), tp (aggressive), a0-a7 (except args of functions)
        for (int i = 3; i <= 6; i++) regList.add(PhysicalReg.get("t" + i));
        regList.addAll(List.of(PhysicalReg.get("gp"), PhysicalReg.get("tp")));
        for (int i = 0; i <= 11; i++) regList.add(PhysicalReg.get("s" + i));
        for (int i = 7; i >= 0 ; --i) regList.add(PhysicalReg.get("a" + i));
    }

    public LinearScanner(IRProgram program) {
        this.program = program;
    }

    public void run() {
        LiveAnalyzer liveAnalyzer = new LiveAnalyzer(program);
        liveAnalyzer.analyze();
        program.funcDefs.forEach(this::runFunc);
        if (program.initFunc != null) runFunc(program.initFunc);
        runFunc(program.mainFunc);
    }

    public void runFunc(FuncDefMod func) {
        int maxRegCnt = regList.size() - Math.min(8, func.params.size());
        var intervals = new ArrayList<>(func.intervalMap.entrySet());
        intervals.sort(Map.Entry.comparingByValue());
        // <被占用区间的右端点, 同一个右端点的所有寄存器的编号的集合> 因为没有MultiSet所以用TreeMap代替
        var active = new TreeMap<Integer, HashSet<Integer>>();
        for (var entry : intervals) {
            // TODO 可以让 a0-a7 也参与寄存器分配
            // 此处指的是当有参数占用a0-a7时也可以在参数生命周期结束后重新分配出去，实际上如果参数不足8个，多余的参数寄存器也会参与分配
            if (func.params.contains(entry.getKey())) {
                if (func.params.indexOf(entry.getKey()) < 8)
                    func.regMap.put(entry.getKey(), PhysicalReg.get("a" + func.params.indexOf(entry.getKey())));
                continue;
            }
            var interval = entry.getValue();
            var availEntry = active.floorEntry(interval.start);
            if (availEntry != null) {
                // 有空闲的寄存器
                var regNo = availEntry.getValue().iterator().next();
                func.regMap.put(entry.getKey(), regList.get(regNo));
                availEntry.getValue().remove(regNo);
                if (availEntry.getValue().isEmpty()) active.remove(availEntry.getKey());
                if (active.containsKey(interval.end)) active.get(interval.end).add(regNo);
                else active.put(interval.end, new HashSet<>(List.of(regNo)));
            } else if (func.activeCnt < maxRegCnt) {
                // 寄存器未全部使用
                int regNo = func.activeCnt++;
                func.regMap.put(entry.getKey(), regList.get(regNo));
                if (active.containsKey(interval.end)) active.get(interval.end).add(regNo);
                else active.put(interval.end, new HashSet<>(List.of(regNo)));
            } else func.spilledVars.add(entry.getKey()); // 溢出
        }
    }
}
