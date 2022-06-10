package net.shrimpworks.unreal.scriptbrowser;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;

import net.shrimpworks.unreal.unrealscript.UnrealScriptBaseListener;
import net.shrimpworks.unreal.unrealscript.UnrealScriptParser;

public class ClassFormatterListener extends UnrealScriptBaseListener {

	private final UClass clazz;

	private final CommonTokenStream tokens;
	private final TokenStreamRewriter rewriter;

	// stateful processing
	private final Set<String> locals = new HashSet<>();
	private boolean inFunction = false;

	public ClassFormatterListener(UClass clazz, CommonTokenStream tokens) {
		this.clazz = clazz;
		this.tokens = tokens;
		this.rewriter = new TokenStreamRewriter(tokens);
	}

	public String getTranslatedText() {
		return rewriter.getText();
	}

	@Override
	public void enterProgram(UnrealScriptParser.ProgramContext ctx) {
		List<Token> comments = tokens.getHiddenTokensToLeft(ctx.getStart().getTokenIndex(), 1);
		if (comments != null) {
			for (Token comment : comments) {
				tokenStyle(comment, "cmt");
			}
		}
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		List<Token> comments = tokens.getHiddenTokensToRight(ctx.getStop().getTokenIndex(), 1);
		if (comments != null) {
			for (Token comment : comments) {
				tokenStyle(comment, "cmt");
			}
		}
		List<Token> directives = tokens.getHiddenTokensToRight(ctx.getStop().getTokenIndex(), 2);
		if (directives != null) {
			for (Token directive : directives) {
				tokenStyle(directive, "dir");
			}
		}
	}

	@Override
	public void enterClassname(UnrealScriptParser.ClassnameContext ctx) {
//		tokenStyle(ctx, "cls");
	}

	@Override
	public void enterClassdecl(UnrealScriptParser.ClassdeclContext ctx) {
		tokenStyle(ctx.CLASS().getSymbol(), "kw");
		ctx.classparams().forEach(p -> tokenStyle(p, "kw"));
		if (ctx.EXPANDS() != null) tokenStyle(ctx.EXPANDS().getSymbol(), "kw");
		if (ctx.EXTENDS() != null) tokenStyle(ctx.EXTENDS().getSymbol(), "kw");
	}

	@Override
	public void enterFunctiontype(UnrealScriptParser.FunctiontypeContext ctx) {
		tokenStyle(ctx, "kw");
	}

	@Override
	public void enterBasictype(UnrealScriptParser.BasictypeContext ctx) {
		tokenStyle(ctx, "basic");
	}

	@Override
	public void enterConstvalue(UnrealScriptParser.ConstvalueContext ctx) {
		if (ctx.BoolVal() != null) tokenStyle(ctx, "bool");
		else if (ctx.FloatVal() != null) tokenStyle(ctx, "num");
		else if (ctx.IntVal() != null) tokenStyle(ctx, "num");
		else if (ctx.StringVal() != null) tokenStyle(ctx, "str");
		else if (ctx.NameVal() != null) tokenStyle(ctx, "name");
		else if (ctx.NoneVal() != null) tokenStyle(ctx, "none");
	}

	@Override
	public void enterOperator(UnrealScriptParser.OperatorContext ctx) {
		tokenStyle(ctx, "op");
	}

	@Override
	public void enterComparator(UnrealScriptParser.ComparatorContext ctx) {
		tokenStyle(ctx, "op");
	}

	@Override
	public void enterAssignment(UnrealScriptParser.AssignmentContext ctx) {
		tokenStyle(ctx, "op");
	}

	@Override
	public void enterVardecl(UnrealScriptParser.VardeclContext ctx) {
		tokenStyle(ctx.VAR().getSymbol(), "kw");
		ctx.varparams().forEach(p -> tokenStyle(p, "kw"));
		ctx.varidentifier().forEach(p -> {
			tokenStyle(p, "var");
			tokenAnchor(p, "V_" + p.identifier().getText().toLowerCase());
		});

		if (ctx.vartype().classtype() != null) linkClass(ctx.vartype().classtype());
//		else if (ctx.vartype().basictype() != null) type = ctx.vartype().basictype().getText();
//		else if (ctx.vartype().enumdecl() != null) type = ctx.vartype().enumdecl().identifier().getText();
//		else if (ctx.vartype().arraydecl() != null) type = ctx.vartype().arraydecl().identifier().getText();
//		else if (ctx.vartype().dynarraydecl() != null) {
//			if (ctx.vartype().dynarraydecl().basictype() != null) type = ctx.vartype().dynarraydecl().basictype().getText();
//			else if (ctx.vartype().dynarraydecl().classtype() != null) type = ctx.vartype().dynarraydecl().classtype().getText();
//		}
	}

	private void linkClass(UnrealScriptParser.ClasstypeContext ctx) {
		Optional<UPackage> pkg = Optional.empty();
		if (ctx.packageidentifier().identifier() != null) {
			pkg = clazz.pkg.sourceSet.pkg(ctx.packageidentifier().identifier().getText());
		}

		

		pkg
			.flatMap(p -> p.clazz(ctx.packageidentifier().classname().identifier().getText()))
			.ifPresentOrElse(
				cls -> tokenLink(ctx, String.format("../%s/%s.html", cls.pkg.name, cls.name)),
				() -> clazz.pkg.sourceSet.clazz(ctx.packageidentifier().classname().identifier().getText()));


		pkg
			.flatMap(p -> p.clazz(ctx.packageidentifier().classname().identifier().getText()))
			.ifPresentOrElse(
				cls -> tokenLink(ctx, String.format("../%s/%s.html", cls.pkg.name, cls.name)),
				() -> clazz.pkg.sourceSet.clazz(ctx.packageidentifier().classname().identifier().getText()));

//		clazz.parent()
//			 .flatMap(p -> p.functions().entrySet().stream()
//							.filter(kv -> kv.getValue().contains(ctx.getText()))
//							.findFirst()
//			 )
//			 .ifPresent(f -> tokenLink(
//				 tokens.get(ctx.start.getTokenIndex() - 2), ctx.stop,
//				 String.format("../%s/%s.html#F_%s", f.getKey().pkg.name, f.getKey().name, ctx.getText().toLowerCase())
//			 ));
//
//		tokenLink(
//			tokens.get(ctx.start.getTokenIndex() - 2), ctx.stop,
//			String.format("../%s/%s.html#F_%s", f.getKey().pkg.name, f.getKey().name, ctx.getText().toLowerCase())
//		)
	}

	@Override
	public void enterLocaldecl(UnrealScriptParser.LocaldeclContext ctx) {
		tokenStyle(ctx.LOCAL().getSymbol(), "kw");
		ctx.identifier().forEach(p -> locals.add(p.getText()));
	}

	@Override
	public void enterFunctionbody(UnrealScriptParser.FunctionbodyContext ctx) {
		inFunction = true;
	}

	@Override
	public void exitFunctionbody(UnrealScriptParser.FunctionbodyContext ctx) {
		inFunction = false;
		locals.clear();
	}

	@Override
	public void enterIdentifier(UnrealScriptParser.IdentifierContext ctx) {
		if (!inFunction) return;

		if (!Objects.equals(tokens.get(ctx.start.getTokenIndex() - 1).getText(), ".") && locals.contains(ctx.getText())) {
			tokenStyle(ctx, "lcl");
		} else if (!Objects.equals(tokens.get(ctx.start.getTokenIndex() - 1).getText(), ".")) {
			if (clazz.variables().values().stream().anyMatch(v -> v.contains(ctx.getText()))) {
				tokenStyle(ctx, "var");
			}
		} else if (Objects.equals(tokens.get(ctx.stop.getTokenIndex() + 1).getText(), "(")) {
			if (Objects.equals(tokens.get(ctx.start.getTokenIndex() - 2).getText().toLowerCase(), "super")) {
				clazz.parent()
					 .flatMap(p -> p.functions().entrySet().stream()
									.filter(kv -> kv.getValue().contains(ctx.getText()))
									.findFirst()
					 )
					 .ifPresent(f -> tokenLink(
						 tokens.get(ctx.start.getTokenIndex() - 2), ctx.stop,
						 String.format("../%s/%s.html#F_%s", f.getKey().pkg.name, f.getKey().name, ctx.getText().toLowerCase())
					 ));
			} else {
				if (clazz.functions().values().stream().anyMatch(v -> v.contains(ctx.getText()))) {
					tokenLink(ctx, "#F_" + ctx.getText().toLowerCase());
				}
			}
		}
	}

	@Override
	public void enterEnumdecl(UnrealScriptParser.EnumdeclContext ctx) {
		tokenStyle(ctx.ENUM().getSymbol(), "kw");
		tokenStyle(ctx.identifier(), "ident");
	}

	@Override
	public void enterConstdecl(UnrealScriptParser.ConstdeclContext ctx) {
		tokenStyle(ctx.CONST().getSymbol(), "kw");
		tokenStyle(ctx.identifier(), "ident");
	}

	@Override
	public void enterStructdecl(UnrealScriptParser.StructdeclContext ctx) {
		tokenStyle(ctx.STRUCT().getSymbol(), "kw");
		tokenStyle(ctx.identifier(), "ident");
		if (ctx.EXTENDS() != null) tokenStyle(ctx.EXTENDS().getSymbol(), "kw");
		ctx.structparams().forEach(p -> tokenStyle(p, "kw"));
	}

	@Override
	public void enterFunctiondecl(UnrealScriptParser.FunctiondeclContext ctx) {
		if (ctx.normalfunc() != null) {
			tokenAnchor(ctx.normalfunc().identifier(), "F_" + ctx.normalfunc().identifier().getText().toLowerCase());
			tokenStyle(ctx.normalfunc().identifier(), "ident");
			ctx.normalfunc().functionparams().forEach(p -> tokenStyle(p, "kw"));
		} else if (ctx.operatorfunc() != null) {
			ctx.operatorfunc().functionparams().forEach(p -> tokenStyle(p, "kw"));
			tokenStyle(ctx.operatorfunc().operatortype(), "kw");
		}
	}

	@Override
	public void enterReplicationblock(UnrealScriptParser.ReplicationblockContext ctx) {
		tokenStyle(ctx.REPLICATION().getSymbol(), "kw");
	}

	@Override
	public void enterStatement(UnrealScriptParser.StatementContext ctx) {
		if (ctx.ifstatement() != null) {
			tokenStyle(ctx.ifstatement().IF().getSymbol(), "kw");
			if (ctx.ifstatement().ELSE() != null) tokenStyle(ctx.ifstatement().ELSE().getSymbol(), "kw");
		} else if (ctx.whileloop() != null) tokenStyle(ctx.whileloop().WHILE().getSymbol(), "kw");
		else if (ctx.doloop() != null) tokenStyle(ctx.doloop().DO().getSymbol(), "kw");
		else if (ctx.forloop() != null) tokenStyle(ctx.forloop().FOR().getSymbol(), "kw");
		else if (ctx.foreachloop() != null) tokenStyle(ctx.foreachloop().FOREACH().getSymbol(), "kw");
		else if (ctx.switchstatement() != null) tokenStyle(ctx.switchstatement().SWITCH().getSymbol(), "kw");
		else if (ctx.assertion() != null) tokenStyle(ctx.assertion().ASSERT().getSymbol(), "kw");
		else if (ctx.returnstatement() != null) tokenStyle(ctx.returnstatement().RETURN().getSymbol(), "kw");
	}

	private void tokenAnchor(ParserRuleContext ctx, String name) {
		rewriter.insertBefore(ctx.start, "«a id=\"" + name + "\"»");
		rewriter.insertAfter(ctx.stop, "«/a»");
	}

	private void tokenLink(Token start, Token stop, String path) {
		rewriter.insertBefore(start, "«a href=\"" + path + "\"»");
		rewriter.insertAfter(stop, "«/a»");
	}

	private void tokenLink(ParserRuleContext ctx, String path) {
		rewriter.insertBefore(ctx.start, "«a href=\"" + path + "\"»");
		rewriter.insertAfter(ctx.stop, "«/a»");
	}

	private void tokenStyle(ParserRuleContext ctx, String style) {
		rewriter.insertBefore(ctx.start, "«span class=\"" + style + "\"»");
		rewriter.insertAfter(ctx.stop, "«/span»");
	}

	private void tokenStyle(Token token, String style) {
		rewriter.insertBefore(token, "«span class=\"" + style + "\"»");
		rewriter.insertAfter(token, "«/span»");
	}
}
