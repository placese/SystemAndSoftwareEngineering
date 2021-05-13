package ru.mirea.lang;

import ru.mirea.lang.ast.BinOpNode;
import ru.mirea.lang.ast.ExprNode;
import ru.mirea.lang.ast.NumberNode;
import ru.mirea.lang.ast.VarNode;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.mirea.lang.TokenType.NUMBER;

public class CompilerOptimize {

    private static List<String> asm = new ArrayList<String>();

    static void emit(String instr) {
        asm.add(instr);
    }

    static void listOut(List<String> lines) {
        for(int i = 0; i < lines.size(); i++){
            System.out.println(lines.get(i));
        }
    }

    public static void getVars(Set<String> vars,  ExprNode node) {
        if (node instanceof NumberNode) {
        } else if (node instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) node;
            getVars(vars, binOp.left);
            getVars(vars,   binOp.right);
        } else if (node instanceof VarNode) {
            VarNode var = (VarNode) node;
            vars.add(var.id.text);
        }
    }

    public static void compile(ExprNode node) {
        if (node instanceof NumberNode) {
            NumberNode num = (NumberNode) node;
            emit("    PUSH QWORD " + num);
        } else if (node instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) node;
            compile(binOp.left);
            compile(binOp.right);
            switch (binOp.op.type) {
                case ADD: {
                    emit("    POP RBX"); emit("    POP RAX"); emit("    ADD RAX, RBX"); emit("    PUSH RAX");
                    break;
                }
                case SUB: {
                    emit("    POP RBX"); emit("    POP RAX"); emit("    SUB RAX, RBX");  emit("    PUSH RAX");
                    break;
                }
                case MUL: {
                    emit("    POP RBX"); emit("    POP RAX"); emit("    IMUL RAX, RBX"); emit("    PUSH RAX");
                    break;
                }
                case DIV: {
                    emit("    POP RBX"); emit("    POP RAX"); emit("    MOV RDX, 0"); emit("    PUSH RAX"); emit("    PUSH RAX");
                    break;
                }
            }
        } else if (node instanceof VarNode) {
            VarNode var = (VarNode) node;
            emit("    PUSH QWORD [" + var + "]");
        }

    }

    public static void generateASM(Set<String> vars) {
        for (String var: vars) {
            emit("    MOV RCX, promt_" + var);
            emit("    MOV R11, printf");
            emit("    CALL R11");

            emit("    MOV RCX, scanf_format");
            emit("    MOV RDX, " + var);
            emit("    MOV R11, scanf");
            emit("    CALL R11");
        }

    }

    public static ExprNode foldConstants(ExprNode node) {
        if (node instanceof VarNode) {
            return node;
        }
        else if (node instanceof NumberNode) {
            return node;
        }
        else if (node instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) node;
            ExprNode l = foldConstants(((BinOpNode) node).left);
            ExprNode r = foldConstants(((BinOpNode) node).right);
            if (l instanceof NumberNode && r instanceof  NumberNode) {

                int lvalue = Integer.parseInt(((NumberNode) l).number.text);
                int rvalue = Integer.parseInt(((NumberNode) r).number.text);
                int result;
                switch (binOp.op.type) {
                    case ADD:
                        result = lvalue + rvalue;
                        break;
                    case SUB:
                        result = lvalue - rvalue;
                        break;
                    case MUL:
                        result = lvalue * rvalue;
                        break;
                    case DIV:
                        result = lvalue / rvalue;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + binOp.op.type);
                }
                return new NumberNode(new Token(NUMBER, Integer.toString(result), binOp.op.pos));
            } else {
                return new BinOpNode(binOp.op, l, r);
            }
        }
        throw new IllegalStateException();
    }

    static void peepholeOptimize() {
        Pattern push = Pattern.compile("\\s*PUSH (.+)");
        Pattern pop = Pattern.compile("\\s*POP (.+)");
        for(int i = 0; i < asm.size() - 1; i++) {
            String instr = asm.get(i);
            String nextInstr = asm.get(i + 1);

            Matcher m1 = push.matcher(instr);
            Matcher m2 = pop.matcher(nextInstr);
            if (m1.matches() && m2.matches()) {
                String arg1 = m1.group(1);
                String arg2 = m2.group(1);
                asm.set(i, "    MOV " + arg2 + ", " + arg1);
                asm.remove(i + 1);
            }
        }
    }

    public static void main(String[] args) {
        String text = "x + y * z - 2";

        Lexer l = new Lexer(text);
        List<Token> tokens = l.lex();
        tokens.removeIf(t -> t.type == TokenType.SPACE);

        Parser p = new Parser(tokens);
        ExprNode node = p.parseExpression();
        Set<String> vars = new LinkedHashSet<>();
        node = foldConstants(node);

        getVars(vars, node);

        emit("section .text");
        emit("    global main");
        emit("    extern printf");
        emit("    extern scanf");
        emit("main:");
        emit("sub rsp, 40");

        generateASM(vars);
        compile(node);

        emit("    MOV RDX, 0");
        emit("    POP RDX");
        emit("    mov rcx, message");
        emit("    mov r11, printf ");
        emit("    call r11");
        emit("    add rsp, 40");
        emit("    ret");

        emit("section .data");
        emit("scanf_format: db \"%d\", 0");
        for (String var: vars
        ) {
            emit("    promt_" + var + ":  db \"   Input " + var + ";\", 0");
            emit("    " + var + ": dq 0");
        }
        emit("message:");
        emit("db 'Result %d' ,0");
        peepholeOptimize();
        listOut(asm);
    }
}
