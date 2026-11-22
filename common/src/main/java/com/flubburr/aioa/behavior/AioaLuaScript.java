package com.flubburr.aioa.behavior;

import org.luaj.vm2.Globals;
import org.luaj.vm2.Lua;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class AioaLuaScript {
    public static final int SOURCE_LIMIT = 8192;
    private static final String[] ACTIONS = {
            "say", "actionbar", "rotate", "glow", "aggressive", "no_ai", "stop",
            "heal", "damage_target", "move_to_target", "set_phase", "tag"
    };
    private static final Map<String, Compiled> CACHE = new LinkedHashMap<>(32, 0.75f, true);

    private AioaLuaScript() {}

    public static String validate(String source) {
        return compile(source).error();
    }

    static String run(String source, Map<String, LuaValue> inputs, Map<String, Double> variables,
                      BiConsumer<String, String> applyAction) {
        Compiled compiled = compile(source);
        if (compiled.error() != null) return compiled.error();
        Globals globals = new Globals();
        globals.debuglib = new Budget();
        inputs.forEach(globals::set);
        List<Action> actions = new ArrayList<>();
        Map<String, Double> writes = new LinkedHashMap<>();
        for (String operation : ACTIONS) {
            globals.set(operation, new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue value) {
                    if (actions.size() >= 16) throw new LuaError("Too many mob actions");
                    actions.add(new Action(operation, argument(operation, value)));
                    return NIL;
                }
            });
        }
        globals.set("get", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue key) {
                String name = variable(key);
                return valueOf(writes.getOrDefault(name, variables.getOrDefault(name, 0.0)));
            }
        });
        globals.set("set", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue key, LuaValue value) {
                String name = variable(key);
                if (!variables.containsKey(name) && !writes.containsKey(name) && variables.size() + writes.size() >= 128) {
                    throw new LuaError("Mob variable limit reached");
                }
                writes.put(name, number(value));
                return NIL;
            }
        });
        try {
            new LuaClosure(compiled.prototype(), globals).call();
        } catch (LuaError error) {
            return error.getMessage();
        }
        variables.putAll(writes);
        for (Action action : actions) applyAction.accept(action.name(), action.argument());
        return null;
    }

    private static synchronized Compiled compile(String source) {
        if (source.length() > SOURCE_LIMIT) return new Compiled(null, "Lua is limited to 8,192 characters.");
        Compiled cached = CACHE.get(source);
        if (cached != null) return cached;
        Globals compiler = new Globals();
        LuaC.install(compiler);
        Compiled result;
        try {
            result = new Compiled(compiler.compilePrototype(new StringReader(source), "mob.lua"), null);
        } catch (LuaError | IOException | StackOverflowError error) {
            String message = error.getMessage();
            result = new Compiled(null, message == null ? "Lua source is too deeply nested." : message);
        }
        if (CACHE.size() >= 128) CACHE.remove(CACHE.keySet().iterator().next());
        CACHE.put(source, result);
        return result;
    }

    private static String variable(LuaValue key) {
        String name = key.checkjstring();
        if (!name.matches("[A-Za-z_][A-Za-z0-9_]{0,47}")) throw new LuaError("Invalid mob variable name");
        return name;
    }

    private static double number(LuaValue value) {
        double number = value.checkdouble();
        if (!Double.isFinite(number)) throw new LuaError("Expected a finite number");
        return Math.max(-1_000_000, Math.min(1_000_000, number));
    }

    private static String argument(String name, LuaValue value) {
        return switch (name) {
            case "say", "actionbar", "tag" -> {
                String text = value.checkjstring();
                yield "\"" + text.substring(0, Math.min(256, text.length())) + "\"";
            }
            case "glow", "aggressive", "no_ai" -> Boolean.toString(value.checkboolean());
            case "stop" -> "";
            default -> Double.toString(number(value));
        };
    }

    private record Action(String name, String argument) {}
    private record Compiled(Prototype prototype, String error) {}

    private static final class Budget extends DebugLib {
        private record Frame(LuaClosure closure, LuaValue[] registers) {}
        private final ArrayDeque<Frame> frames = new ArrayDeque<>();
        private int instructions = 2000;
        private int line = 1;

        @Override
        public void onCall(LuaFunction function) {}

        @Override
        public String traceback(int level) {
            return "mob.lua:" + line;
        }

        @Override
        public void onCall(LuaClosure closure, Varargs args, LuaValue[] registers) {
            if (frames.size() >= 32) throw new LuaError("Lua call depth exceeded");
            frames.push(new Frame(closure, registers));
        }

        @Override
        public void onReturn() {
            if (!frames.isEmpty()) frames.pop();
        }

        @Override
        public void onInstruction(int pc, Varargs args, int top) {
            Frame frame = frames.peek();
            this.line = frame.closure().p.lineinfo[pc];
            if (--instructions < 0) throw new LuaError("Lua instruction budget exceeded");
            int opcode = frame.closure().p.code[pc];
            if (Lua.GET_OPCODE(opcode) != Lua.OP_CONCAT) return;
            int bytes = 0;
            for (int i = Lua.GETARG_B(opcode); i <= Lua.GETARG_C(opcode); i++) {
                LuaValue value = frame.registers()[i];
                bytes += value.isstring() ? value.strvalue().m_length : 0;
                if (bytes > SOURCE_LIMIT) throw new LuaError("Lua string is too long");
            }
        }
    }
}
