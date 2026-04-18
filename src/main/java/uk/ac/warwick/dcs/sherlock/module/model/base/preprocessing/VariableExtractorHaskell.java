package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import uk.ac.warwick.dcs.sherlock.api.util.IndexedString;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.LineListArtifact;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.IAdvancedPreProcessor;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellParser;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellParserBaseListener;

import java.util.LinkedList;
import java.util.List;

public class VariableExtractorHaskell implements IAdvancedPreProcessor<HaskellLexer> {

    @Override
    public LineListArtifact process(HaskellLexer lexer) {
        List<IndexedString> fields = new LinkedList<>();

        lexer.reset();
        HaskellParser parser = new HaskellParser(new CommonTokenStream(lexer));

        ParseTreeWalker.DEFAULT.walk(new HaskellParserBaseListener() {

            // Top-level and local value bindings:
            // decl_no_th -> infixexp opt_sig? rhs
            // We enter via rhs so we only capture actual value definitions
            // (not bare type signatures, which have no rhs).
            // The infixexp sibling of rhs is the binding LHS, e.g.:
            //   myVar        = ...
            //   myFunc x y   = ...
            //   (x, y)       = ...
            @Override
            public void enterRhs(HaskellParser.RhsContext ctx) {
                if (ctx.getParent() instanceof HaskellParser.Decl_no_thContext) {
                    HaskellParser.Decl_no_thContext decl =
                            (HaskellParser.Decl_no_thContext) ctx.getParent();
                    if (decl.infixexp() != null) {
                        // Strip any inline type annotation from the LHS (e.g. "x :: Int")
                        String lhs = decl.infixexp().getText().split("::")[0];
                        fields.add(new IndexedString(decl.infixexp().start.getLine(), lhs));
                    }
                }
            }

            // do-notation / list-comprehension bind:
            // qual -> bindpat '<-' exp
            // bindpat is the variable pattern on the left of '<-', e.g.:
            //   x      <- getLine
            //   (a, b) <- getSomePair
            @Override
            public void enterQual(HaskellParser.QualContext ctx) {
                // The grammar alternative "bindpat '<-' exp" is identified by
                // the presence of a bindpat child; the other alternatives are
                // "exp" (plain expression) and "'let' binds".
                if (ctx.bindpat() != null) {
                    fields.add(new IndexedString(ctx.start.getLine(), ctx.bindpat().getText()));
                }
            }

        }, parser.module());

        //System.out.println("field -> " + fields.toString());
        return new LineListArtifact(fields);
    }
}