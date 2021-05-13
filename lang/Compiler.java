package ru.mirea.lang;

import ru.mirea.lang.ast.BinOpNode;
import ru.mirea.lang.ast.ExprNode;
import ru.mirea.lang.ast.NumberNode;
import ru.mirea.lang.ast.VarNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Compiler {

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
            System.out.println("    PUSH QWORD " + num);
        } else if (node instanceof BinOpNode) {
            BinOpNode binOp = (BinOpNode) node;
            compile(binOp.left);
            compile(binOp.right);
            switch (binOp.op.type) {
                case ADD: {
                    System.out.println("    POP RBX\n" + "    POP RAX\n" + "    ADD RAX, RBX\n" + "    PUSH RAX\n");
                    break;
                }
                case SUB: {
                    System.out.println("    POP RBX\n" + "    POP RAX\n" + "    SUB RAX, EBX\n" + "    PUSH RAX\n");
                    break;
                }
                case MUL: {
                    System.out.println("    POP RBX\n" + "    POP RAX\n" + "    IMUL RAX, RBX\n" + "    PUSH RAX\n");
                    break;
                }
                case DIV: {
                    System.out.println("    POP RBX\n" + "    POP RAX\n" + "    MOV RDX, 0\n" + "    PUSH RAX\n" + "    PUSH RAX\n");
                    break;
                }
            }
        } else if (node instanceof VarNode) {
            VarNode var = (VarNode) node;
            System.out.println("    PUSH QWORD [" + var + "]");
        }

    }

    public static void generateASM(Set<String> vars) {
        for (String var: vars) {
            System.out.println("    MOV RCX, promt_" + var);
            System.out.println("    MOV R11, printf");
            System.out.println("    CALL R11");

            System.out.println(" ");
            System.out.println("    MOV RDX, scanf_format");
            System.out.println("    MOV RDX, " + var);
            System.out.println("    MOV R11, scanf");
            System.out.println("    CALL R11\n");
        }

    }

    public static void main(String[] args) {
        String text = "x + y + 2";

        Lexer l = new Lexer(text);
        List<Token> tokens = l.lex();
        tokens.removeIf(t -> t.type == TokenType.SPACE);

        Parser p = new Parser(tokens);
        ExprNode node = p.parseExpression();
        Set<String> vars = new LinkedHashSet<>();
        getVars(vars, node);

        System.out.println("section .text\n" +
                "    global main\n" +
                "    extern printf\n" +
                "    extern scanf\n" +
                "    \n" +
                "main:");
        generateASM(vars);
        compile(node);
        System.out.println("\n    scan_format: \"%d\", 0");
        System.out.println("    MOV RDX, 0\n" + "    POP RDX\n\n" + "    MOV R11, printf \n" +
                "    CALL R11\n" +
                "    ADD RSP, 40" +
                "    RET");
        for (String var: vars
        ) {
            System.out.println("    promt_" + var + ": \"   Input " + var + ";\", 0");
            System.out.println("    " + var + ": dq 0");
        }

        System.out.println("    MOV RDX, 0\n" + "    POP RDX\n\n" + "    MOV R11, printf \n" +
                "    CALL R11\n" +
                "    ADD RSP, 40\n" +
                "    RET");
        System.out.println("    section .data, 0\n" + "    POP RDX\n\n" + "    mov r11, printf \n" +
                "    call r11\n" +
                "    add rsp, 40\n" +
                "    ret");
    }
}