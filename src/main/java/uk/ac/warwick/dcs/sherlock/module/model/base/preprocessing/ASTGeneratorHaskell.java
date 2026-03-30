package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.ASTArtifact;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.IAdvancedPreProcessor;
import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellParser;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellParserBaseVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ASTGeneratorHaskell implements IAdvancedPreProcessor<HaskellLexer> {

    @Override
    public ASTArtifact process(HaskellLexer lexer) {
        lexer.reset();
        HaskellParser parser = new HaskellParser(new CommonTokenStream(lexer));

        // Top-level grammar rule is 'module'
        ParseTree tree = parser.module();

        HaskellASTBuilder builder = new HaskellASTBuilder();
        ASTNode astRoot = builder.visit(tree);

        return new ASTArtifact(astRoot);
    }
}

class HaskellASTBuilder extends HaskellParserBaseVisitor<ASTNode> {

    private static final Set<Integer> OPERATOR_TOKEN_TYPES = Set.of(
            HaskellLexer.Arrow,        // ->
            HaskellLexer.DoubleArrow,  // =>
            HaskellLexer.Revarrow,     // <-
            HaskellLexer.LarrowTail,   // -<
            HaskellLexer.RarrowTail,   // >-
            HaskellLexer.LLarrowTail,  // -<<
            HaskellLexer.RRarrowTail,  // >>-
            HaskellLexer.Plus,         // +
            HaskellLexer.Minus,        // -
            HaskellLexer.Asterisk,     // *
            HaskellLexer.Divide,       // /
            HaskellLexer.Less,         // <
            HaskellLexer.Greater,      // >
            HaskellLexer.Ampersand,    // &
            HaskellLexer.Pipe,         // |
            HaskellLexer.Bang,         // !
            HaskellLexer.Caret,        // ^
            HaskellLexer.Percent,      // %
            HaskellLexer.Tilde,        // ~
            HaskellLexer.Dollar,       // $
            HaskellLexer.DDollar,      // $$
            HaskellLexer.Colon,        // :
            HaskellLexer.Dot,          // .
            HaskellLexer.Eq            // =  (operator context)
    );

    private static final Map<String, ASTNode.NodeKind> RULE_MAP = Map.ofEntries(

            // ── Top-level structure ──────────────────────────────────────────────
            Map.entry("module",         ASTNode.NodeKind.HASKELL_MODULE),
            Map.entry("module_content", ASTNode.NodeKind.HASKELL_MODULE),
            Map.entry("body",           ASTNode.NodeKind.BLOCK),
            Map.entry("impdecl",        ASTNode.NodeKind.HASKELL_IMPORT),
            Map.entry("exprt",          ASTNode.NodeKind.HASKELL_EXPORT),

            // ── Type-level declarations ──────────────────────────────────────────
            // ty_decl is further dispatched in visitTy_decl to data/newtype/type
            Map.entry("cl_decl",             ASTNode.NodeKind.HASKELL_CLASS_DECL),
            Map.entry("inst_decl",           ASTNode.NodeKind.HASKELL_INSTANCE_DECL),
            Map.entry("standalone_deriving", ASTNode.NodeKind.HASKELL_DERIVING),
            Map.entry("derivings",           ASTNode.NodeKind.HASKELL_DERIVING),
            Map.entry("deriving",            ASTNode.NodeKind.HASKELL_DERIVING),
            Map.entry("standalone_kind_sig", ASTNode.NodeKind.HASKELL_KIND_SIG),

            // ── Value-level declarations ─────────────────────────────────────────
            // decl_no_th and sigdecl have dedicated overrides below
            Map.entry("rhs",         ASTNode.NodeKind.BLOCK),               // '=' exp wherebinds? | gdrhs wherebinds?
            Map.entry("gdrhs",       ASTNode.NodeKind.BLOCK),               // container: gdrh+
            Map.entry("gdrh",        ASTNode.NodeKind.HASKELL_GUARD_EXPR),  // one guarded line: '|' guards '=' exp
            Map.entry("wherebinds",  ASTNode.NodeKind.HASKELL_WHERE_BLOCK), // 'where' binds  (in rhs)
            Map.entry("where_decls", ASTNode.NodeKind.HASKELL_WHERE_BLOCK), // 'where' open_ decls? close  (pattern synonyms)
            Map.entry("where_cls",   ASTNode.NodeKind.HASKELL_WHERE_BLOCK),
            Map.entry("where_inst",  ASTNode.NodeKind.HASKELL_WHERE_BLOCK),
            Map.entry("binds",       ASTNode.NodeKind.BLOCK),
            Map.entry("decllist",    ASTNode.NodeKind.BLOCK),

            // ── Case alternatives ────────────────────────────────────────────────
            Map.entry("alt",    ASTNode.NodeKind.HASKELL_EQUATION),     // one case branch: pat alt_rhs
            Map.entry("gdpat",  ASTNode.NodeKind.HASKELL_GUARD_EXPR),   // guarded case arm: '|' guards '->' exp
            Map.entry("gdpats", ASTNode.NodeKind.BLOCK),
            Map.entry("alts",   ASTNode.NodeKind.BLOCK),

            // ── Data constructors / fields ───────────────────────────────────────
            Map.entry("constr",    ASTNode.NodeKind.HASKELL_CONSTRUCTOR),
            Map.entry("constrs1",  ASTNode.NodeKind.BLOCK),
            Map.entry("fielddecl", ASTNode.NodeKind.HASKELL_RECORD_FIELD),

            // ── Expressions ─────────────────────────────────────────────────────
            // aexp, aexp2, exp, infixexp, exp10 all have dedicated overrides below
            Map.entry("fexp", ASTNode.NodeKind.HASKELL_APP_EXPR),  // function application spine: aexp+

            // ── Do-notation ──────────────────────────────────────────────────────
            // qual has a dedicated override for bind/let/stmt classification
            // stmt delegates to qual; override handles 'rec' blocks
            Map.entry("stmtlist", ASTNode.NodeKind.BLOCK),
            Map.entry("stmts",    ASTNode.NodeKind.BLOCK),

            // ── List comprehension qualifier containers ──────────────────────────
            Map.entry("flattenedpquals", ASTNode.NodeKind.BLOCK),
            Map.entry("pquals",          ASTNode.NodeKind.BLOCK),
            Map.entry("squals",          ASTNode.NodeKind.BLOCK),

            // ── Types ────────────────────────────────────────────────────────────
            // type_ has a dedicated override; ctype has an override for forall/constraint
            Map.entry("ktype",        ASTNode.NodeKind.HASKELL_TYPE_APP),
            Map.entry("ktypedoc",     ASTNode.NodeKind.HASKELL_TYPE_APP),
            Map.entry("btype",        ASTNode.NodeKind.HASKELL_TYPE_APP),
            Map.entry("atype",        ASTNode.NodeKind.HASKELL_TYPE_APP),
            Map.entry("opt_kind_sig", ASTNode.NodeKind.HASKELL_KIND_SIG),
            Map.entry("tycl_hdr",     ASTNode.NodeKind.HASKELL_TYPE_CONSTRAINT),
            Map.entry("tycl_context", ASTNode.NodeKind.HASKELL_TYPE_CONSTRAINT)
    );

    private static final Map<Integer, ASTNode.NodeKind> TOKEN_MAP = Map.ofEntries(
            Map.entry(HaskellLexer.VARID,       ASTNode.NodeKind.HASKELL_VARID),
            Map.entry(HaskellLexer.CONID,       ASTNode.NodeKind.HASKELL_CONID),
            // All three integer literal forms map to the same NodeKind
            Map.entry(HaskellLexer.DECIMAL,     ASTNode.NodeKind.HASKELL_INTEGER_LITERAL),
            Map.entry(HaskellLexer.OCTAL,       ASTNode.NodeKind.HASKELL_INTEGER_LITERAL),
            Map.entry(HaskellLexer.HEXADECIMAL, ASTNode.NodeKind.HASKELL_INTEGER_LITERAL),
            Map.entry(HaskellLexer.FLOAT,       ASTNode.NodeKind.HASKELL_FLOAT_LITERAL),
            Map.entry(HaskellLexer.CHAR,        ASTNode.NodeKind.HASKELL_CHAR_LITERAL),
            Map.entry(HaskellLexer.STRING,      ASTNode.NodeKind.HASKELL_STRING_LITERAL)
    );

    @Override
    public ASTNode visitChildren(RuleNode node) {
        ParserRuleContext ctx = (ParserRuleContext) node;
        String ruleName = HaskellParser.ruleNames[ctx.getRuleIndex()];
        ASTNode.NodeKind kind = RULE_MAP.get(ruleName);

        List<ASTNode> children = new ArrayList<>();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ASTNode childAST = ctx.getChild(i).accept(this);
            if (childAST != null) {
                children.add(childAST);
            }
        }

        if (kind == null) {
            if (children.isEmpty()) return null;
            if (children.size() == 1) return children.get(0); // transparent unwrap
            ASTNode wrapper = new ASTNode(ASTNode.NodeKind.UNKNOWN);
            for (ASTNode child : children) addChild(wrapper, child);
            attachMetadata(wrapper, ctx);
            return wrapper;
        }

        ASTNode nodeAST = new ASTNode(kind);
        for (ASTNode child : children) addChild(nodeAST, child);
        attachMetadata(nodeAST, ctx);
        return nodeAST;
    }


    @Override
    public ASTNode visitTerminal(TerminalNode node) {
        int type = node.getSymbol().getType();

        ASTNode.NodeKind kind = TOKEN_MAP.get(type);
        if (kind == null && OPERATOR_TOKEN_TYPES.contains(type)) {
            kind = ASTNode.NodeKind.HASKELL_OPERATOR;
        }
        if (kind == null) return null;

        ASTNode ast = new ASTNode(kind, node.getText());
        Token token = node.getSymbol();
        ast.setMetadata("startLine", token.getLine());
        ast.setMetadata("startChar", token.getCharPositionInLine());
        ast.setMetadata("endLine",   token.getLine());
        ast.setMetadata("endChar",   token.getCharPositionInLine() + token.getText().length());
        return ast;
    }

    @Override
    public ASTNode visitTy_decl(HaskellParser.Ty_declContext ctx) {
        String first = ctx.getChild(0).getText();
        ASTNode.NodeKind kind;
        switch (first) {
            case "data":    kind = ASTNode.NodeKind.HASKELL_DATA_DECL;    break;
            case "newtype": kind = ASTNode.NodeKind.HASKELL_NEWTYPE_DECL; break;
            default:        kind = ASTNode.NodeKind.HASKELL_TYPE_DECL;    break;
        }
        ASTNode node = new ASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public ASTNode visitSigdecl(HaskellParser.SigdeclContext ctx) {
        // Pragma sigdecl alternatives start with '{-#' — no structural interest
        if (ctx.getChildCount() > 0 && ctx.getChild(0).getText().startsWith("{-#")) {
            return visitChildren(ctx);
        }

        ASTNode.NodeKind kind = ASTNode.NodeKind.HASKELL_FIXITY_DECL; // default
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if ("::".equals(ctx.getChild(i).getText())) {
                kind = ASTNode.NodeKind.HASKELL_TYPE_SIG;
                break;
            }
        }

        ASTNode node = new ASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public ASTNode visitDecl(HaskellParser.DeclContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public ASTNode visitDecl_no_th(HaskellParser.Decl_no_thContext ctx) {
        // sigdecl child — visitSigdecl handles it; transparent here
        if (ctx.getChildCount() == 1) return visitChildren(ctx);

        // Classify the binding
        ASTNode.NodeKind kind = classifyBinding(ctx);
        ASTNode node = new ASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public ASTNode visitAexp(HaskellParser.AexpContext ctx) {
        if (ctx.getChildCount() == 0) return null;

        // LCASE token: LambdaCase extension (\case)
        if (ctx.getChild(0) instanceof TerminalNode) {
            int ttype = ((TerminalNode) ctx.getChild(0)).getSymbol().getType();
            if (ttype == HaskellLexer.LCASE) {
                ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_LAMBDA_EXPR);
                visitChildrenInto(node, ctx);
                attachMetadata(node, ctx);
                return node;
            }
        }

        String first = ctx.getChild(0).getText();
        ASTNode.NodeKind kind;
        switch (first) {
            case "\\":   kind = ASTNode.NodeKind.HASKELL_LAMBDA_EXPR; break;
            case "let":  kind = ASTNode.NodeKind.HASKELL_LET_EXPR;   break;
            case "if":   kind = ASTNode.NodeKind.HASKELL_IF_EXPR;    break;
            case "case": kind = ASTNode.NodeKind.HASKELL_CASE_EXPR;  break;
            case "do":
            case "mdo":  kind = ASTNode.NodeKind.HASKELL_DO_EXPR;    break;
            case "~":    kind = ASTNode.NodeKind.HASKELL_PAT_IRREFUTABLE; break;
            default:
                // As-pattern: qvar '@' aexp — second token is '@'
                if (ctx.getChildCount() >= 3 && "@".equals(ctx.getChild(1).getText())) {
                    kind = ASTNode.NodeKind.HASKELL_PAT_AS;
                    break;
                }
                // All other aexp alternatives fall through to aexp1 — transparent
                return visitChildren(ctx);
        }

        ASTNode node = new ASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public ASTNode visitAexp2(HaskellParser.Aexp2Context ctx) {
        if (ctx.getChildCount() == 0) return null;
        String first = ctx.getChild(0).getText();

        if ("(".equals(first)) {
            // '(' tup_exprs ')' — the second child is a tup_exprs rule node
            if (ctx.getChildCount() >= 2 && ctx.getChild(1) instanceof HaskellParser.Tup_exprsContext) {
                ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_TUPLE_EXPR);
                visitChildrenInto(node, ctx);
                attachMetadata(node, ctx);
                return node;
            }
            // '(' texp ')' — check for sections
            if (ctx.getChildCount() == 3) {
                ASTNode.NodeKind sectionKind = detectSection(ctx.getChild(1));
                if (sectionKind != null) {
                    ASTNode node = new ASTNode(sectionKind);
                    visitChildrenInto(node, ctx);
                    attachMetadata(node, ctx);
                    return node;
                }
            }
            // Plain parenthesised expression — transparent
            return visitChildren(ctx);
        }

        // '[' list_ ']' — visitList_ classifies the content
        if ("[".equals(first)) {
            return visitChildren(ctx);
        }

        // '_' — wildcard pattern
        if ("_".equals(first)) {
            ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_PAT_WILDCARD);
            attachMetadata(node, ctx);
            return node;
        }

        // Literals, variables, constructors — transparent; leaves are captured by visitTerminal
        return visitChildren(ctx);
    }

    @Override
    public ASTNode visitList_(HaskellParser.List_Context ctx) {
        ASTNode.NodeKind kind = ASTNode.NodeKind.HASKELL_LIST_EXPR; // default

        for (int i = 0; i < ctx.getChildCount(); i++) {
            String t = ctx.getChild(i).getText();
            if ("|".equals(t)) {
                kind = ASTNode.NodeKind.HASKELL_LIST_COMPREHENSION;
                break;
            }
            if ("..".equals(t)) {
                kind = ASTNode.NodeKind.HASKELL_ARITHMETIC_SEQ;
                break;
            }
        }

        ASTNode node = new ASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public ASTNode visitInfixexp(HaskellParser.InfixexpContext ctx) {
        if (ctx.getChildCount() == 1) return visit(ctx.getChild(0));

        ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_INFIX_EXPR);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public ASTNode visitExp10(HaskellParser.Exp10Context ctx) {
        if (ctx.getChildCount() >= 2 && "-".equals(ctx.getChild(0).getText())) {
            ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_NEGATE_EXPR);
            visitChildrenInto(node, ctx);
            attachMetadata(node, ctx);
            return node;
        }
        return visitChildren(ctx); // transparent: just fexp
    }

    @Override
    public ASTNode visitExp(HaskellParser.ExpContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if ("::".equals(ctx.getChild(i).getText())) {
                ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_TYPE_ANNOTATION);
                visitChildrenInto(node, ctx);
                attachMetadata(node, ctx);
                return node;
            }
        }
        return visitChildren(ctx);
    }

    @Override
    public ASTNode visitType_(HaskellParser.Type_Context ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if ("->".equals(ctx.getChild(i).getText())) {
                ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_TYPE_FUN);
                visitChildrenInto(node, ctx);
                attachMetadata(node, ctx);
                return node;
            }
        }
        return visitChildren(ctx);
    }

    @Override
    public ASTNode visitCtype(HaskellParser.CtypeContext ctx) {
        if (ctx.getChildCount() > 0 && "forall".equals(ctx.getChild(0).getText())) {
            ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_TYPE_FORALL);
            visitChildrenInto(node, ctx);
            attachMetadata(node, ctx);
            return node;
        }
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if ("=>".equals(ctx.getChild(i).getText())) {
                ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_TYPE_CONSTRAINT);
                visitChildrenInto(node, ctx);
                attachMetadata(node, ctx);
                return node;
            }
        }
        return visitChildren(ctx);
    }

    @Override
    public ASTNode visitQual(HaskellParser.QualContext ctx) {
        ASTNode.NodeKind kind = ASTNode.NodeKind.HASKELL_DO_STMT; // plain expression statement

        for (int i = 0; i < ctx.getChildCount(); i++) {
            String t = ctx.getChild(i).getText();
            if ("<-".equals(t)) { kind = ASTNode.NodeKind.HASKELL_DO_BIND; break; }
            if ("let".equals(t)) { kind = ASTNode.NodeKind.HASKELL_DO_LET;  break; }
        }

        ASTNode node = new ASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public ASTNode visitStmt(HaskellParser.StmtContext ctx) {
        if (ctx.getChildCount() > 0 && "rec".equals(ctx.getChild(0).getText())) {
            ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_DO_EXPR);
            visitChildrenInto(node, ctx);
            attachMetadata(node, ctx);
            return node;
        }
        return visitChildren(ctx); // transparent to qual
    }

    @Override
    public ASTNode visitTransformqual(HaskellParser.TransformqualContext ctx) {
        ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_COMP_GUARD);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public ASTNode visitGuard_(HaskellParser.Guard_Context ctx) {
        ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_COMP_GUARD);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public ASTNode visitModid(HaskellParser.ModidContext ctx) {
        ASTNode node = new ASTNode(ASTNode.NodeKind.HASKELL_MODID, ctx.getText());
        attachMetadata(node, ctx);
        return node;
    }

    private ASTNode.NodeKind classifyBinding(ParserRuleContext ctx) {
        if (ctx.getChildCount() == 0) return ASTNode.NodeKind.HASKELL_FUNCTION_DECL;
        String firstText = ctx.getChild(0).getText();
        if (!firstText.isEmpty() && Character.isUpperCase(firstText.charAt(0))) {
            return ASTNode.NodeKind.HASKELL_PATTERN_BIND;
        }
        if ("(".equals(firstText) || "[".equals(firstText)) {
            return ASTNode.NodeKind.HASKELL_PATTERN_BIND;
        }
        return ASTNode.NodeKind.HASKELL_FUNCTION_DECL;
    }

    private ASTNode.NodeKind detectSection(ParseTree inner) {
        if (!(inner instanceof ParserRuleContext)) return null;
        ParserRuleContext texp = (ParserRuleContext) inner;
        if (!"texp".equals(HaskellParser.ruleNames[texp.getRuleIndex()])) return null;

        if (texp.getChildCount() >= 2) {
            ParseTree last = texp.getChild(texp.getChildCount() - 1);
            if (last instanceof ParserRuleContext) {
                String lastRule = HaskellParser.ruleNames[((ParserRuleContext) last).getRuleIndex()];
                if ("qop".equals(lastRule) || "qopm".equals(lastRule)) {
                    return ASTNode.NodeKind.HASKELL_SECTION_LEFT;
                }
            }
            ParseTree first = texp.getChild(0);
            if (first instanceof ParserRuleContext) {
                String firstRule = HaskellParser.ruleNames[((ParserRuleContext) first).getRuleIndex()];
                if ("qopm".equals(firstRule)) {
                    return ASTNode.NodeKind.HASKELL_SECTION_RIGHT;
                }
            }
        }
        return null;
    }

    private void visitChildrenInto(ASTNode parent, ParserRuleContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ASTNode child = ctx.getChild(i).accept(this);
            addChild(parent, child);
        }
    }

    private void attachMetadata(ASTNode node, ParserRuleContext ctx) {
        Token start = ctx.getStart();
        Token stop  = ctx.getStop();
        if (start != null) {
            node.setMetadata("startLine", start.getLine());
            node.setMetadata("startChar", start.getCharPositionInLine());
        }
        if (stop != null) {
            node.setMetadata("endLine", stop.getLine());
            node.setMetadata("endChar", stop.getCharPositionInLine());
        }
    }

    private void addChild(ASTNode parent, ASTNode child) {
        if (child != null) {
            parent.addChild(child);
            child.setParent(parent);
        }
    }
}