package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.ASTArtifact;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.IAdvancedPreProcessor;
import uk.ac.warwick.dcs.sherlock.api.util.HaskellASTNode;
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
        HaskellASTNode astRoot = builder.visit(tree);

        return new ASTArtifact(astRoot);
    }
}

class HaskellASTBuilder extends HaskellParserBaseVisitor<HaskellASTNode> {

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

    private static final Map<String, HaskellASTNode.Kind> RULE_MAP = Map.ofEntries(

            // ── Top-level structure ──────────────────────────────────────────────
            Map.entry("module",         HaskellASTNode.Kind.MODULE),
            Map.entry("module_content", HaskellASTNode.Kind.MODULE),
            Map.entry("body",           HaskellASTNode.Kind.BLOCK),
            Map.entry("impdecl",        HaskellASTNode.Kind.IMPORT),
            Map.entry("exprt",          HaskellASTNode.Kind.EXPORT),

            // ── Type-level declarations ──────────────────────────────────────────
            // ty_decl is further dispatched in visitTy_decl to data/newtype/type
            Map.entry("cl_decl",             HaskellASTNode.Kind.CLASS_DECL),
            Map.entry("inst_decl",           HaskellASTNode.Kind.INSTANCE_DECL),
            Map.entry("standalone_deriving", HaskellASTNode.Kind.DERIVING),
            Map.entry("derivings",           HaskellASTNode.Kind.DERIVING),
            Map.entry("deriving",            HaskellASTNode.Kind.DERIVING),
            Map.entry("standalone_kind_sig", HaskellASTNode.Kind.KIND_SIG),

            // ── Value-level declarations ─────────────────────────────────────────
            // decl_no_th and sigdecl have dedicated overrides below
            Map.entry("rhs",         HaskellASTNode.Kind.BLOCK),               // '=' exp wherebinds? | gdrhs wherebinds?
            Map.entry("gdrhs",       HaskellASTNode.Kind.BLOCK),               // container: gdrh+
            Map.entry("gdrh",        HaskellASTNode.Kind.GUARD_EXPR),  // one guarded line: '|' guards '=' exp
            Map.entry("wherebinds",  HaskellASTNode.Kind.WHERE_BLOCK), // 'where' binds  (in rhs)
            Map.entry("where_decls", HaskellASTNode.Kind.WHERE_BLOCK), // 'where' open_ decls? close  (pattern synonyms)
            Map.entry("where_cls",   HaskellASTNode.Kind.WHERE_BLOCK),
            Map.entry("where_inst",  HaskellASTNode.Kind.WHERE_BLOCK),
            Map.entry("binds",       HaskellASTNode.Kind.BLOCK),
            Map.entry("decllist",    HaskellASTNode.Kind.BLOCK),

            // ── Case alternatives ────────────────────────────────────────────────
            Map.entry("alt",    HaskellASTNode.Kind.EQUATION),     // one case branch: pat alt_rhs
            Map.entry("gdpat",  HaskellASTNode.Kind.GUARD_EXPR),   // guarded case arm: '|' guards '->' exp
            Map.entry("gdpats", HaskellASTNode.Kind.BLOCK),
            Map.entry("alts",   HaskellASTNode.Kind.BLOCK),

            // ── Data constructors / fields ───────────────────────────────────────
            Map.entry("constr",    HaskellASTNode.Kind.CONSTRUCTOR),
            Map.entry("constrs1",  HaskellASTNode.Kind.BLOCK),
            Map.entry("fielddecl", HaskellASTNode.Kind.RECORD_FIELD),

            // ── Expressions ─────────────────────────────────────────────────────
            // aexp, aexp2, exp, infixexp, exp10 all have dedicated overrides below
            Map.entry("fexp", HaskellASTNode.Kind.APP_EXPR),  // function application spine: aexp+

            // ── Do-notation ──────────────────────────────────────────────────────
            // qual has a dedicated override for bind/let/stmt classification
            // stmt delegates to qual; override handles 'rec' blocks
            Map.entry("stmtlist", HaskellASTNode.Kind.BLOCK),
            Map.entry("stmts",    HaskellASTNode.Kind.BLOCK),

            // ── List comprehension qualifier containers ──────────────────────────
            Map.entry("flattenedpquals", HaskellASTNode.Kind.BLOCK),
            Map.entry("pquals",          HaskellASTNode.Kind.BLOCK),
            Map.entry("squals",          HaskellASTNode.Kind.BLOCK),

            // ── Types ────────────────────────────────────────────────────────────
            // type_ has a dedicated override; ctype has an override for forall/constraint
            Map.entry("ktype",        HaskellASTNode.Kind.TYPE_APP),
            Map.entry("ktypedoc",     HaskellASTNode.Kind.TYPE_APP),
            Map.entry("btype",        HaskellASTNode.Kind.TYPE_APP),
            Map.entry("atype",        HaskellASTNode.Kind.TYPE_APP),
            Map.entry("opt_kind_sig", HaskellASTNode.Kind.KIND_SIG),
            Map.entry("tycl_hdr",     HaskellASTNode.Kind.TYPE_CONSTRAINT),
            Map.entry("tycl_context", HaskellASTNode.Kind.TYPE_CONSTRAINT)
    );

    private static final Map<Integer, HaskellASTNode.Kind> TOKEN_MAP = Map.ofEntries(
            Map.entry(HaskellLexer.VARID,       HaskellASTNode.Kind.VARID),
            Map.entry(HaskellLexer.CONID,       HaskellASTNode.Kind.CONID),
            // All three integer literal forms map to the same Kind
            Map.entry(HaskellLexer.DECIMAL,     HaskellASTNode.Kind.INTEGER_LITERAL),
            Map.entry(HaskellLexer.OCTAL,       HaskellASTNode.Kind.INTEGER_LITERAL),
            Map.entry(HaskellLexer.HEXADECIMAL, HaskellASTNode.Kind.INTEGER_LITERAL),
            Map.entry(HaskellLexer.FLOAT,       HaskellASTNode.Kind.FLOAT_LITERAL),
            Map.entry(HaskellLexer.CHAR,        HaskellASTNode.Kind.CHAR_LITERAL),
            Map.entry(HaskellLexer.STRING,      HaskellASTNode.Kind.STRING_LITERAL)
    );

    @Override
    public HaskellASTNode visitChildren(RuleNode node) {
        ParserRuleContext ctx = (ParserRuleContext) node;
        String ruleName = HaskellParser.ruleNames[ctx.getRuleIndex()];
        HaskellASTNode.Kind kind = RULE_MAP.get(ruleName);

        List<HaskellASTNode> children = new ArrayList<>();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            HaskellASTNode childAST = ctx.getChild(i).accept(this);
            if (childAST != null) {
                children.add(childAST);
            }
        }

        if (kind == null) {
            if (children.isEmpty()) return null;
            if (children.size() == 1) return children.get(0); // transparent unwrap
            HaskellASTNode wrapper = new HaskellASTNode(HaskellASTNode.Kind.UNKNOWN);
            for (HaskellASTNode child : children) addChild(wrapper, child);
            attachMetadata(wrapper, ctx);
            return wrapper;
        }

        HaskellASTNode nodeAST = new HaskellASTNode(kind);
        for (HaskellASTNode child : children) addChild(nodeAST, child);
        attachMetadata(nodeAST, ctx);
        return nodeAST;
    }


    @Override
    public HaskellASTNode visitTerminal(TerminalNode node) {
        int type = node.getSymbol().getType();

        HaskellASTNode.Kind kind = TOKEN_MAP.get(type);
        if (kind == null && OPERATOR_TOKEN_TYPES.contains(type)) {
            kind = HaskellASTNode.Kind.OPERATOR;
        }
        if (kind == null) return null;

        HaskellASTNode ast = new HaskellASTNode(kind, node.getText());
        Token token = node.getSymbol();
        ast.setMetadata("startLine", token.getLine());
        ast.setMetadata("startChar", token.getCharPositionInLine());
        ast.setMetadata("endLine",   token.getLine());
        ast.setMetadata("endChar",   token.getCharPositionInLine() + token.getText().length());
        return ast;
    }

    @Override
    public HaskellASTNode visitTy_decl(HaskellParser.Ty_declContext ctx) {
        String first = ctx.getChild(0).getText();
        HaskellASTNode.Kind kind;
        switch (first) {
            case "data":    kind = HaskellASTNode.Kind.DATA_DECL;    break;
            case "newtype": kind = HaskellASTNode.Kind.NEWTYPE_DECL; break;
            default:        kind = HaskellASTNode.Kind.TYPE_DECL;    break;
        }
        HaskellASTNode node = new HaskellASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public HaskellASTNode visitSigdecl(HaskellParser.SigdeclContext ctx) {
        // Pragma sigdecl alternatives start with '{-#' — no structural interest
        if (ctx.getChildCount() > 0 && ctx.getChild(0).getText().startsWith("{-#")) {
            return visitChildren(ctx);
        }

        HaskellASTNode.Kind kind = HaskellASTNode.Kind.FIXITY_DECL; // default
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if ("::".equals(ctx.getChild(i).getText())) {
                kind = HaskellASTNode.Kind.TYPE_SIG;
                break;
            }
        }

        HaskellASTNode node = new HaskellASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public HaskellASTNode visitDecl(HaskellParser.DeclContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public HaskellASTNode visitDecl_no_th(HaskellParser.Decl_no_thContext ctx) {
        // sigdecl child — visitSigdecl handles it; transparent here
        if (ctx.getChildCount() == 1) return visitChildren(ctx);

        // Classify the binding
        HaskellASTNode.Kind kind = classifyBinding(ctx);
        HaskellASTNode node = new HaskellASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public HaskellASTNode visitAexp(HaskellParser.AexpContext ctx) {
        if (ctx.getChildCount() == 0) return null;

        // LCASE token: LambdaCase extension (\case)
        if (ctx.getChild(0) instanceof TerminalNode) {
            int ttype = ((TerminalNode) ctx.getChild(0)).getSymbol().getType();
            if (ttype == HaskellLexer.LCASE) {
                HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.LAMBDA_EXPR);
                visitChildrenInto(node, ctx);
                attachMetadata(node, ctx);
                return node;
            }
        }

        String first = ctx.getChild(0).getText();
        HaskellASTNode.Kind kind;
        switch (first) {
            case "\\":   kind = HaskellASTNode.Kind.LAMBDA_EXPR; break;
            case "let":  kind = HaskellASTNode.Kind.LET_EXPR;   break;
            case "if":   kind = HaskellASTNode.Kind.IF_EXPR;    break;
            case "case": kind = HaskellASTNode.Kind.CASE_EXPR;  break;
            case "do":
            case "mdo":  kind = HaskellASTNode.Kind.DO_EXPR;    break;
            case "~":    kind = HaskellASTNode.Kind.PAT_IRREFUTABLE; break;
            default:
                // As-pattern: qvar '@' aexp — second token is '@'
                if (ctx.getChildCount() >= 3 && "@".equals(ctx.getChild(1).getText())) {
                    kind = HaskellASTNode.Kind.PAT_AS;
                    break;
                }
                // All other aexp alternatives fall through to aexp1 — transparent
                return visitChildren(ctx);
        }

        HaskellASTNode node = new HaskellASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public HaskellASTNode visitAexp2(HaskellParser.Aexp2Context ctx) {
        if (ctx.getChildCount() == 0) return null;
        String first = ctx.getChild(0).getText();

        if ("(".equals(first)) {
            // '(' tup_exprs ')' — the second child is a tup_exprs rule node
            if (ctx.getChildCount() >= 2 && ctx.getChild(1) instanceof HaskellParser.Tup_exprsContext) {
                HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.TUPLE_EXPR);
                visitChildrenInto(node, ctx);
                attachMetadata(node, ctx);
                return node;
            }
            // '(' texp ')' — check for sections
            if (ctx.getChildCount() == 3) {
                HaskellASTNode.Kind sectionKind = detectSection(ctx.getChild(1));
                if (sectionKind != null) {
                    HaskellASTNode node = new HaskellASTNode(sectionKind);
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
            HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.PAT_WILDCARD);
            attachMetadata(node, ctx);
            return node;
        }

        // Literals, variables, constructors — transparent; leaves are captured by visitTerminal
        return visitChildren(ctx);
    }

    @Override
    public HaskellASTNode visitList_(HaskellParser.List_Context ctx) {
        HaskellASTNode.Kind kind = HaskellASTNode.Kind.LIST_EXPR; // default

        for (int i = 0; i < ctx.getChildCount(); i++) {
            String t = ctx.getChild(i).getText();
            if ("|".equals(t)) {
                kind = HaskellASTNode.Kind.LIST_COMPREHENSION;
                break;
            }
            if ("..".equals(t)) {
                kind = HaskellASTNode.Kind.ARITHMETIC_SEQ;
                break;
            }
        }

        HaskellASTNode node = new HaskellASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public HaskellASTNode visitInfixexp(HaskellParser.InfixexpContext ctx) {
        if (ctx.getChildCount() == 1) return visit(ctx.getChild(0));

        HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.INFIX_EXPR);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public HaskellASTNode visitExp10(HaskellParser.Exp10Context ctx) {
        if (ctx.getChildCount() >= 2 && "-".equals(ctx.getChild(0).getText())) {
            HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.NEGATE_EXPR);
            visitChildrenInto(node, ctx);
            attachMetadata(node, ctx);
            return node;
        }
        return visitChildren(ctx); // transparent: just fexp
    }

    @Override
    public HaskellASTNode visitExp(HaskellParser.ExpContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if ("::".equals(ctx.getChild(i).getText())) {
                HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.TYPE_ANNOTATION);
                visitChildrenInto(node, ctx);
                attachMetadata(node, ctx);
                return node;
            }
        }
        return visitChildren(ctx);
    }

    @Override
    public HaskellASTNode visitType_(HaskellParser.Type_Context ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if ("->".equals(ctx.getChild(i).getText())) {
                HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.TYPE_FUN);
                visitChildrenInto(node, ctx);
                attachMetadata(node, ctx);
                return node;
            }
        }
        return visitChildren(ctx);
    }

    @Override
    public HaskellASTNode visitCtype(HaskellParser.CtypeContext ctx) {
        if (ctx.getChildCount() > 0 && "forall".equals(ctx.getChild(0).getText())) {
            HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.TYPE_FORALL);
            visitChildrenInto(node, ctx);
            attachMetadata(node, ctx);
            return node;
        }
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if ("=>".equals(ctx.getChild(i).getText())) {
                HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.TYPE_CONSTRAINT);
                visitChildrenInto(node, ctx);
                attachMetadata(node, ctx);
                return node;
            }
        }
        return visitChildren(ctx);
    }

    @Override
    public HaskellASTNode visitQual(HaskellParser.QualContext ctx) {
        HaskellASTNode.Kind kind = HaskellASTNode.Kind.DO_STMT; // plain expression statement

        for (int i = 0; i < ctx.getChildCount(); i++) {
            String t = ctx.getChild(i).getText();
            if ("<-".equals(t)) { kind = HaskellASTNode.Kind.DO_BIND; break; }
            if ("let".equals(t)) { kind = HaskellASTNode.Kind.DO_LET;  break; }
        }

        HaskellASTNode node = new HaskellASTNode(kind);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public HaskellASTNode visitStmt(HaskellParser.StmtContext ctx) {
        if (ctx.getChildCount() > 0 && "rec".equals(ctx.getChild(0).getText())) {
            HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.DO_EXPR);
            visitChildrenInto(node, ctx);
            attachMetadata(node, ctx);
            return node;
        }
        return visitChildren(ctx); // transparent to qual
    }

    @Override
    public HaskellASTNode visitTransformqual(HaskellParser.TransformqualContext ctx) {
        HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.COMP_GUARD);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public HaskellASTNode visitGuard_(HaskellParser.Guard_Context ctx) {
        HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.COMP_GUARD);
        visitChildrenInto(node, ctx);
        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public HaskellASTNode visitModid(HaskellParser.ModidContext ctx) {
        HaskellASTNode node = new HaskellASTNode(HaskellASTNode.Kind.MODID, ctx.getText());
        attachMetadata(node, ctx);
        return node;
    }

    private HaskellASTNode.Kind classifyBinding(ParserRuleContext ctx) {
        if (ctx.getChildCount() == 0) return HaskellASTNode.Kind.FUNCTION_DECL;
        String firstText = ctx.getChild(0).getText();
        if (!firstText.isEmpty() && Character.isUpperCase(firstText.charAt(0))) {
            return HaskellASTNode.Kind.PATTERN_BIND;
        }
        if ("(".equals(firstText) || "[".equals(firstText)) {
            return HaskellASTNode.Kind.PATTERN_BIND;
        }
        return HaskellASTNode.Kind.FUNCTION_DECL;
    }

    private HaskellASTNode.Kind detectSection(ParseTree inner) {
        if (!(inner instanceof ParserRuleContext)) return null;
        ParserRuleContext texp = (ParserRuleContext) inner;
        if (!"texp".equals(HaskellParser.ruleNames[texp.getRuleIndex()])) return null;

        if (texp.getChildCount() >= 2) {
            ParseTree last = texp.getChild(texp.getChildCount() - 1);
            if (last instanceof ParserRuleContext) {
                String lastRule = HaskellParser.ruleNames[((ParserRuleContext) last).getRuleIndex()];
                if ("qop".equals(lastRule) || "qopm".equals(lastRule)) {
                    return HaskellASTNode.Kind.SECTION_LEFT;
                }
            }
            ParseTree first = texp.getChild(0);
            if (first instanceof ParserRuleContext) {
                String firstRule = HaskellParser.ruleNames[((ParserRuleContext) first).getRuleIndex()];
                if ("qopm".equals(firstRule)) {
                    return HaskellASTNode.Kind.SECTION_RIGHT;
                }
            }
        }
        return null;
    }

    private void visitChildrenInto(HaskellASTNode parent, ParserRuleContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            HaskellASTNode child = ctx.getChild(i).accept(this);
            addChild(parent, child);
        }
    }

    private void attachMetadata(HaskellASTNode node, ParserRuleContext ctx) {
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

    private void addChild(HaskellASTNode parent, HaskellASTNode child) {
        if (child != null) {
            parent.addChild(child);
            child.setParent(parent);
        }
    }
}