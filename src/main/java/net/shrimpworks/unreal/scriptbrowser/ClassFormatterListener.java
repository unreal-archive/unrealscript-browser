package net.shrimpworks.unreal.scriptbrowser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
	private final Map<String, String> locals = new HashMap<>(); // local, type
	private boolean inFunction = false;
	private boolean inStateLabel = false;
	private Optional<UClass> typePath = Optional.empty();
	private Optional<String> structName = Optional.empty();

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
		// FIXME style to apply to all types?
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
			structName.ifPresentOrElse(
				s -> tokenAnchor(p, String.format("%s_%s", s.toLowerCase(), p.identifier().getText())),
				() -> tokenAnchor(p, p.identifier().getText())
			);
		});

		if (ctx.vartype().packageidentifier() != null) linkClass(ctx.vartype().packageidentifier());
		else if (ctx.vartype().classtype() != null) linkClass(ctx.vartype().classtype().packageidentifier());
			// FIXME link to all the other types here
//		else if (ctx.vartype().basictype() != null) type = ctx.vartype().basictype().getText();
//		else if (ctx.vartype().enumdecl() != null) type = ctx.vartype().enumdecl().identifier().getText();
//		else if (ctx.vartype().arraydecl() != null) type = ctx.vartype().arraydecl().identifier().getText();
		else if (ctx.vartype().dynarraydecl() != null) {
			if (ctx.vartype().dynarraydecl().classtype() != null) linkClass(ctx.vartype().dynarraydecl().classtype().packageidentifier());
			if (ctx.vartype().dynarraydecl().packageidentifier() != null) linkClass(ctx.vartype().dynarraydecl().packageidentifier());
		}
	}

	private void linkClass(UnrealScriptParser.PackageidentifierContext ctx) {
		if (ctx == null) return; // `class` type

		Optional<UPackage> pkg = Optional.empty();
		if (ctx.identifier() != null) {
			pkg = clazz.pkg.sourceSet.pkg(ctx.identifier().getText());
		}

		clazz.pkg.sourceSet.clazz(
			ctx.classname().identifier().getText(), pkg.orElse(null)
		).ifPresent(cls -> classLink(ctx, cls));
	}

	@Override
	public void enterLocaldecl(UnrealScriptParser.LocaldeclContext ctx) {
		String type = null;
		if (ctx.localtype().packageidentifier() != null) {
			type = ctx.localtype().packageidentifier().getText();
			linkClass(ctx.localtype().packageidentifier());
		} else if (ctx.localtype().classtype() != null) {
			type = ctx.localtype().classtype().getText();
			linkClass(ctx.localtype().packageidentifier());
		}
		// FIXME link to more types
		else if (ctx.localtype().basictype() != null) type = ctx.localtype().basictype().getText();
		else if (ctx.localtype().arraydecl() != null) type = ctx.localtype().arraydecl().identifier().getText();
		else if (ctx.localtype().dynarraydecl() != null) {
			if (ctx.localtype().dynarraydecl().basictype() != null) type = ctx.localtype().dynarraydecl().basictype().getText();
			else if (ctx.localtype().dynarraydecl().classtype() != null) type = ctx.localtype().dynarraydecl().classtype().getText();
		}
		final String lolFinal = type;

		ctx.identifier().forEach(p -> locals.put(p.getText().toLowerCase(), lolFinal));

		tokenStyle(ctx.LOCAL().getSymbol(), "kw");
	}

	@Override
	public void enterNormalfunc(UnrealScriptParser.NormalfuncContext ctx) {
		ctx.functionargs().forEach(a -> {
			String type = null;
			if (a.functionargtype().packageidentifier() != null) {
				type = a.functionargtype().packageidentifier().getText();
				linkClass(a.functionargtype().packageidentifier());
			} else if (a.functionargtype().classtype() != null) {
				type = a.functionargtype().classtype().getText();
				linkClass(a.functionargtype().classtype().packageidentifier());
			}
			// FIXME link to more types
			else if (a.functionargtype().basictype() != null) type = a.functionargtype().basictype().getText();
			else if (a.functionargtype().arraydecl() != null) type = a.functionargtype().arraydecl().identifier().getText();
			else if (a.functionargtype().dynarraydecl() != null) {
				if (a.functionargtype().dynarraydecl().basictype() != null) type = a.functionargtype().dynarraydecl().basictype().getText();
				else if (a.functionargtype().dynarraydecl().classtype() != null)
					type = a.functionargtype().dynarraydecl().classtype().getText();
			}
			final String lolFinal = type;

			locals.put(a.identifier().getText().toLowerCase(), lolFinal);

			tokenStyle(a.identifier(), "lcl");
		});
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
	public void enterStatelabel(UnrealScriptParser.StatelabelContext ctx) {
		inStateLabel = true;
	}

	@Override
	public void exitStatelabel(UnrealScriptParser.StatelabelContext ctx) {
		inStateLabel = false;
	}

	@Override
	public void enterIdentifier(UnrealScriptParser.IdentifierContext ctx) {
		if (!inFunction && !inStateLabel) return;

		int start = 1;
		String before = tokens.get(ctx.start.getTokenIndex() - 1).getText();
		while (before.isBlank() && ctx.start.getTokenIndex() - start > 0) {
			before = tokens.get(ctx.start.getTokenIndex() - ++start).getText();
		}

		int stop = 1;
		String after = tokens.get(ctx.stop.getTokenIndex() + 1).getText();
		while (after.isBlank() && ctx.stop.getTokenIndex() + stop < tokens.size() - 1) {
			after = tokens.get(ctx.stop.getTokenIndex() + ++stop).getText();
		}

		stop = 1;
		String afterParent = tokens.get(ctx.getParent().stop.getTokenIndex() + 1).getText();
		while (afterParent.isBlank() && ctx.stop.getTokenIndex() + stop < tokens.size() - 1) {
			afterParent = tokens.get(ctx.stop.getTokenIndex() + ++stop).getText();
		}

		if (!Objects.equals(before, ".") && locals.containsKey(ctx.getText().toLowerCase())) {
			// local variable
			tokenStyle(ctx, "lcl");
		} else if (Objects.equals(after, "(") || Objects.equals(afterParent, "(")) {
			// function calls - note, super is found via member variable `Super` on UClass
			if (Objects.equals(before, ".")) {
				// someVar.Thing()
				String prevToken = tokens.get(ctx.start.getTokenIndex() - start - 1).getText();
				Optional.ofNullable(locals.get(prevToken.toLowerCase()))
						.flatMap(clazz.pkg.sourceSet::clazz)
						.or(() -> clazz.variable(prevToken)
									   .flatMap(v -> v.clazz.pkg.sourceSet.clazz(v.type)))
						.flatMap(c -> c.function(ctx.getText()))
						.ifPresent(f -> memberLink(ctx, f));
			} else {
				// same (or inherited) class function()
				clazz.function(ctx.getText())
					 .ifPresent(f -> memberLink(ctx, f));
			}
		} else if (!Objects.equals(before, ".")) {
			// same (or inherited) class variable
			clazz.variable(ctx.getText()).ifPresent(v -> {
				tokenStyle(ctx, "var");
				memberLink(ctx, v);
			});
		}

		if (ctx.stop.getTokenIndex() + 1 < tokens.size()
			&& (Objects.equals(after, ".") || Objects.equals(afterParent, "."))) {
			typePath.flatMap(c -> c.variable(ctx.getText()))
					.ifPresent(v -> memberLink(ctx, v));

			typePath.ifPresentOrElse(
				p -> typePath = p.variable(ctx.getText()).flatMap(v -> v.clazz.pkg.sourceSet.clazz(v.type)),
				() -> typePath = Optional.ofNullable(locals.get(ctx.getText().toLowerCase()))
										 .flatMap(clazz.pkg.sourceSet::clazz)
										 .or(() -> clazz.variable(ctx.getText())
														.flatMap(v -> v.clazz.pkg.sourceSet.clazz(v.type)))
			);
		} else if (Objects.equals(before, ".")) {
			// SomeVar.member
			typePath.flatMap(c -> c.variable(ctx.getText()))
					.ifPresent(v -> memberLink(ctx, v));

			typePath = Optional.empty();
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
		structName = Optional.ofNullable(ctx.identifier().getText());
		tokenStyle(ctx.STRUCT().getSymbol(), "kw");
		tokenStyle(ctx.identifier(), "ident");
		tokenAnchor(ctx.identifier(), ctx.identifier().getText());
		if (ctx.EXTENDS() != null) tokenStyle(ctx.EXTENDS().getSymbol(), "kw");
		ctx.structparams().forEach(p -> tokenStyle(p, "kw"));
	}

	@Override
	public void exitStructdecl(UnrealScriptParser.StructdeclContext ctx) {
		structName = Optional.empty();
	}

	@Override
	public void enterFunctiondecl(UnrealScriptParser.FunctiondeclContext ctx) {
		if (ctx.normalfunc() != null) {
			tokenStyle(ctx.normalfunc().identifier(), "ident");
			tokenAnchor(ctx.normalfunc().identifier(), ctx.normalfunc().identifier().getText());
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

	// FIXME states

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
		rewriter.insertBefore(ctx.start, "«a id=\"" + name.toLowerCase() + "\"»");
		rewriter.insertAfter(ctx.stop, "«/a»");
	}

	private void memberLink(ParserRuleContext ctx, UClass.UMember member) {
		if (member.name.equalsIgnoreCase("super")) return; // skip linking to super

		if (member.clazz.kind == UClass.UClassKind.STRUCT) {
			rewriter.insertBefore(ctx.start, String.format("«a href=\"../%s/%s.html#%s_%s\"»",
														   member.clazz.pkg.name.toLowerCase(), member.clazz.parent.toLowerCase(),
														   member.clazz.name.toLowerCase(), member.name.toLowerCase()));
		} else {
			rewriter.insertBefore(ctx.start, String.format("«a href=\"../%s/%s.html#%s\"»",
														   member.clazz.pkg.name.toLowerCase(), member.clazz.name.toLowerCase(),
														   member.name.toLowerCase()));
		}
		rewriter.insertAfter(ctx.stop, "«/a»");
	}

	private void classLink(ParserRuleContext ctx, UClass cls) {
		if (cls.kind == UClass.UClassKind.STRUCT) {
			rewriter.insertBefore(ctx.start, String.format("«a href=\"../%s/%s.html#%s\"»",
														   cls.pkg.name.toLowerCase(), cls.parent.toLowerCase(), cls.name.toLowerCase()));
		} else {
			rewriter.insertBefore(ctx.start, String.format("«a href=\"../%s/%s.html\"»",
														   cls.pkg.name.toLowerCase(), cls.name.toLowerCase()));
		}
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
