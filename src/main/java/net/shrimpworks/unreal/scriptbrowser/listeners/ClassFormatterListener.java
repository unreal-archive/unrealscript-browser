package net.shrimpworks.unreal.scriptbrowser.listeners;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import net.shrimpworks.unreal.scriptbrowser.entities.UClass;
import net.shrimpworks.unreal.scriptbrowser.entities.UPackage;
import net.shrimpworks.unreal.unrealscript.UnrealScriptBaseListener;
import net.shrimpworks.unreal.unrealscript.UnrealScriptLexer;
import net.shrimpworks.unreal.unrealscript.UnrealScriptParser;

public class ClassFormatterListener extends UnrealScriptBaseListener {

	private final UClass clazz;

	private final CommonTokenStream tokens;
	private final TokenStreamRewriter rewriter;

	// stateful processing
	private final Map<String, String> locals = new HashMap<>(); // local, type
	private boolean inFunction = false;
	private boolean inStateLabel = false;
	private boolean inDefaultProps = false;
	private Optional<UClass> typePath = Optional.empty();
	private Optional<String> structName = Optional.empty();

	public static String transformClazz(UClass clazz) {
		try (InputStream is = Files.newInputStream(clazz.path, StandardOpenOption.READ)) {
			UnrealScriptLexer lexer = new UnrealScriptLexer(CharStreams.fromStream(is));
			lexer.removeErrorListeners();
			lexer.addErrorListener(UnrealScriptErrorListener.INSTANCE);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			UnrealScriptParser parser = new UnrealScriptParser(tokens);
			parser.removeErrorListeners();
			parser.addErrorListener(UnrealScriptErrorListener.INSTANCE);
			ClassFormatterListener listener = new ClassFormatterListener(clazz, tokens);
			ParseTreeWalker.DEFAULT.walk(listener, parser.program());

			return listener.getTranslatedText();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public ClassFormatterListener(UClass clazz, CommonTokenStream tokens) {
		this.clazz = clazz;
		this.tokens = tokens;
		this.rewriter = new TokenStreamRewriter(tokens);
	}

	/**
	 * Returns the rewritten content, with formatting applied, following a tree walk.
	 */
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
	public void enterClassdecl(UnrealScriptParser.ClassdeclContext ctx) {
		tokenStyle(ctx.CLASS(), "kw");
		tokenStyle(ctx.classname(), "cls");
		tokenAnchor(ctx.CLASS().getSymbol(), "class");
		tokenStyle(ctx.parentclass(), "cls");
		ctx.classparams().forEach(p -> tokenStyle(p, "kw"));
		tokenStyle(ctx.EXPANDS(), "kw");
		tokenStyle(ctx.EXTENDS(), "kw");
	}

	@Override
	public void enterFunctiontype(UnrealScriptParser.FunctiontypeContext ctx) {
		tokenStyle(ctx, "kw");
	}

	@Override
	public void enterConstvalue(UnrealScriptParser.ConstvalueContext ctx) {
		if (ctx.BoolVal() != null) tokenStyle(ctx, "bool");
		else if (ctx.FloatVal() != null) tokenStyle(ctx, "num");
		else if (ctx.IntVal() != null) tokenStyle(ctx, "num");
		else if (ctx.StringVal() != null) tokenStyle(ctx, "str");
		else if (ctx.NameVal() != null) {
			tokenStyle(ctx, "name");
			linkClass(ctx.NameVal().getSymbol());
		} else if (ctx.NoneVal() != null) tokenStyle(ctx, "none");
		else if (ctx.objectval() != null) {
			// the object type will be highlighted/linked by as a classdecl
			tokenStyle(ctx.objectval().NameVal(), "name");
		} else if (ctx.classval() != null) {
			tokenStyle(ctx.classval().CLASS(), "kw");
			if (ctx.classval().NameVal() != null) {
				tokenStyle(ctx.classval().NameVal(), "name");
				linkClass(ctx.classval().NameVal().getSymbol());
			}
		}
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
		tokenStyle(ctx.VAR(), "kw");
		ctx.varparams().forEach(p -> tokenStyle(p, "kw"));
		ctx.varidentifier().forEach(p -> {
			tokenStyle(p, "var");
			structName.ifPresentOrElse(
				s -> tokenAnchor(p, String.format("%s_%s", s.toLowerCase(), p.identifier().getText())),
				() -> tokenAnchor(p, p.identifier().getText())
			);
		});

		if (ctx.vartype().localtype() != null) localType(ctx.vartype().localtype());
	}

	@Override
	public void enterObjectval(UnrealScriptParser.ObjectvalContext ctx) {
		linkClass(ctx.identifier(), null);
	}

	private void linkClass(UnrealScriptParser.PackageidentifierContext ctx) {
		if (ctx == null) return; // `class` type

		Optional<UPackage> pkg = Optional.empty();
		if (ctx.identifier() != null) {
			pkg = clazz.pkg.sourceSet.pkg(ctx.identifier().getText());
		}

		linkClass(ctx.classname().identifier(), pkg.orElse(clazz.pkg));
	}

	private void linkClass(UnrealScriptParser.IdentifierContext ctx, UPackage pkg) {
		clazz.pkg.sourceSet.clazz(ctx.getText(), pkg).ifPresent(cls -> classLink(ctx, cls));
	}

	private void linkClass(Token token) {
		String[] parts = token.getText().replaceAll("'", "").split("\\.");
		if (parts.length == 0) return;

		UPackage pkg = null;
		if (parts.length == 2) pkg = clazz.pkg.sourceSet.pkg(parts[0]).orElse(clazz.pkg);

		linkClass(token, parts[parts.length - 1], pkg);
	}

	private void linkClass(Token token, String clazz, UPackage pkg) {
		this.clazz.pkg.sourceSet.clazz(clazz, pkg).ifPresent(cls -> classLink(token, cls));
	}

	@Override
	public void enterLocaldecl(UnrealScriptParser.LocaldeclContext ctx) {
		final ParserRuleContext type = localType(ctx.localtype());

		ctx.identifier().forEach(p -> locals.put(p.getText().toLowerCase(), type == null ? null : type.getText()));

		tokenStyle(ctx.LOCAL(), "kw");
	}

	private ParserRuleContext localType(UnrealScriptParser.LocaltypeContext ctx) {
		if (ctx == null) return null;

		ParserRuleContext type = null;
		if (ctx.packageidentifier() != null) {
			type = ctx.packageidentifier();
			linkClass(ctx.packageidentifier());
		} else if (ctx.classtype() != null) {
			tokenStyle(ctx.classtype().CLASS(), "kw");
			tokenStyle(ctx.classtype().packageidentifier(), "typ");
			linkClass(ctx.classtype().packageidentifier());
		} else if (ctx.basictype() != null) type = ctx.basictype();
		else if (ctx.dynarraydecl() != null) {
			tokenStyle(ctx.dynarraydecl().ARRAY(), "kw");
			if (ctx.dynarraydecl().basictype() != null) type = ctx.dynarraydecl().basictype();
			else if (ctx.dynarraydecl().classtype() != null) {
				type = ctx.dynarraydecl().classtype().packageidentifier();
				linkClass(ctx.dynarraydecl().classtype().packageidentifier());
			} else if (ctx.dynarraydecl().packageidentifier() != null) {
				type = ctx.dynarraydecl().packageidentifier();
				linkClass(ctx.dynarraydecl().packageidentifier());
			}
		}

		tokenStyle(type, "typ");

		return type;
	}

	@Override
	public void enterNormalfunc(UnrealScriptParser.NormalfuncContext ctx) {
		localType(ctx.localtype());

		ctx.functionargs().forEach(a -> {
			ParserRuleContext type = localType(a.localtype());

			locals.put(a.identifier().getText().toLowerCase(), type == null ? null : type.getText());

			a.functionargparams().forEach(p -> tokenStyle(p, "kw"));
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
	public void enterDefaultpropertiesblock(UnrealScriptParser.DefaultpropertiesblockContext ctx) {
		inDefaultProps = true;
		tokenStyle(ctx.DEFAULTPROPERTIES(), "defprops");
		tokenStyle(ctx, "defaults");
		tokenAnchor(ctx.DEFAULTPROPERTIES().getSymbol(), "default");
	}

	@Override
	public void exitDefaultpropertiesblock(UnrealScriptParser.DefaultpropertiesblockContext ctx) {
		inDefaultProps = false;
	}

	@Override
	public void enterIdentifier(UnrealScriptParser.IdentifierContext ctx) {
		if (!inFunction && !inStateLabel && !inDefaultProps) return;

		// an objectval: Texture'Package.Texture'; this is handled elsewhere
		if (ctx.getParent() instanceof UnrealScriptParser.ObjectvalContext) return;

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
		} else if (!inDefaultProps && (Objects.equals(after, "(") || Objects.equals(afterParent, "("))) {
			// function calls - note, super is found via member variable `Super` on UClass
			if (Objects.equals(before, ".")) {
				// someVar.Thing()
				typePath.flatMap(c -> c.function(ctx.getText()))
						.ifPresent(f -> memberLink(ctx, f));
				typePath = Optional.empty();
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
		tokenStyle(ctx.ENUM(), "kw");
		tokenStyle(ctx.identifier(), "ident");
		tokenAnchor(ctx.identifier(), ctx.identifier().getText());
	}

	@Override
	public void enterConstdecl(UnrealScriptParser.ConstdeclContext ctx) {
		tokenStyle(ctx.CONST(), "kw");
		tokenStyle(ctx.identifier(), "ident");
	}

	@Override
	public void enterStructdecl(UnrealScriptParser.StructdeclContext ctx) {
		structName = Optional.ofNullable(ctx.identifier().getText());
		tokenStyle(ctx.STRUCT(), "kw");
		tokenStyle(ctx.identifier(), "ident");
		tokenAnchor(ctx.identifier(), ctx.identifier().getText());
		tokenStyle(ctx.EXTENDS(), "kw");
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

			UnrealScriptParser.BinaryoperatorContext binaryoperator = ctx.operatorfunc().operatortype().binaryoperator();
			UnrealScriptParser.UnaryoperatorContext unaryoperator = ctx.operatorfunc().operatortype().unaryoperator();

			if (binaryoperator != null) {
				tokenStyle(binaryoperator.OPERATOR(), "kw");
				tokenStyle(binaryoperator.opidentifier(), "op");
				localType(binaryoperator.localtype());
				binaryoperator.functionargs().forEach(f -> localType(f.localtype()));
			}
			if (unaryoperator != null) {
				tokenStyle(unaryoperator.PREOPERATOR(), "kw");
				tokenStyle(unaryoperator.POSTOPERATOR(), "kw");
				tokenStyle(unaryoperator.opidentifier(), "op");
				localType(unaryoperator.localtype());
				localType(unaryoperator.functionargs().localtype());
			}
		}
	}

	@Override
	public void enterReplicationblock(UnrealScriptParser.ReplicationblockContext ctx) {
		tokenStyle(ctx.REPLICATION(), "kw");
		tokenAnchor(ctx.REPLICATION().getSymbol(), "replication");
	}

	@Override
	public void enterReplicationbody(UnrealScriptParser.ReplicationbodyContext ctx) {
		tokenStyle(ctx.RELIABLE(), "kw");
		tokenStyle(ctx.UNRELIABLE(), "kw");
		tokenStyle(ctx.IF(), "kw");
	}

	@Override
	public void enterReplicationidentifiers(UnrealScriptParser.ReplicationidentifiersContext ctx) {
		ctx.identifier().forEach(i -> {
			tokenStyle(i, "var");
			clazz.variable(i.getText())
				 .ifPresent(v -> memberLink(i, v));
		});
	}

	@Override
	public void enterStatedecl(UnrealScriptParser.StatedeclContext ctx) {
		ctx.stateparams().forEach(p -> tokenStyle(p, "kw"));
		tokenStyle(ctx.STATE(), "kw");
		tokenStyle(ctx.EXTENDS(), "kw");
		tokenStyle(ctx.statename(), "cls");
		tokenStyle(ctx.identifier(), "cls");
		tokenAnchor(ctx.statename(), ctx.statename().getText());
	}

	@Override
	public void enterStateignore(UnrealScriptParser.StateignoreContext ctx) {
		ctx.identifier().forEach(i -> {
			clazz.function(i.getText())
				 .ifPresent(v -> memberLink(i, v));
		});
	}

	@Override
	public void enterStatement(UnrealScriptParser.StatementContext ctx) {
		if (ctx.ifstatement() != null) {
			tokenStyle(ctx.ifstatement().IF(), "kw");
			tokenStyle(ctx.ifstatement().ELSE(), "kw");
		} else if (ctx.whileloop() != null) tokenStyle(ctx.whileloop().WHILE(), "kw");
		else if (ctx.doloop() != null) tokenStyle(ctx.doloop().DO(), "kw");
		else if (ctx.forloop() != null) tokenStyle(ctx.forloop().FOR(), "kw");
		else if (ctx.foreachloop() != null) tokenStyle(ctx.foreachloop().FOREACH(), "kw");
		else if (ctx.switchstatement() != null) tokenStyle(ctx.switchstatement().SWITCH(), "kw");
		else if (ctx.assertion() != null) tokenStyle(ctx.assertion().ASSERT(), "kw");
		else if (ctx.returnstatement() != null) tokenStyle(ctx.returnstatement().RETURN(), "kw");
	}

	private void tokenAnchor(Token token, String name) {
		if (token == null) return;
		rewriter.insertBefore(token, "«a id=\"" + name.toLowerCase() + "\"»");
		rewriter.insertAfter(token, "«/a»");
	}

	private void tokenAnchor(ParserRuleContext ctx, String name) {
		if (ctx == null) return;
		rewriter.insertBefore(ctx.start, "«a id=\"" + name.toLowerCase() + "\"»");
		rewriter.insertAfter(ctx.stop, "«/a»");
	}

	private void memberLink(ParserRuleContext ctx, UClass.UMember member) {
		if (ctx == null) return;
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
		if (ctx == null) return;
		if (cls.kind == UClass.UClassKind.STRUCT || cls.kind == UClass.UClassKind.ENUM) {
			rewriter.insertBefore(ctx.start, String.format("«a href=\"../%s/%s.html#%s\"»",
														   cls.pkg.name.toLowerCase(), cls.parent.toLowerCase(), cls.name.toLowerCase()));
		} else {
			rewriter.insertBefore(ctx.start, String.format("«a href=\"../%s/%s.html\"»",
														   cls.pkg.name.toLowerCase(), cls.name.toLowerCase()));
		}
		rewriter.insertAfter(ctx.stop, "«/a»");
	}

	private void classLink(Token token, UClass cls) {
		if (token == null) return;
		if (cls.kind == UClass.UClassKind.STRUCT || cls.kind == UClass.UClassKind.ENUM) {
			rewriter.insertBefore(token, String.format("«a href=\"../%s/%s.html#%s\"»",
													   cls.pkg.name.toLowerCase(), cls.parent.toLowerCase(), cls.name.toLowerCase()));
		} else {
			rewriter.insertBefore(token, String.format("«a href=\"../%s/%s.html\"»",
													   cls.pkg.name.toLowerCase(), cls.name.toLowerCase()));
		}
		rewriter.insertAfter(token, "«/a»");
	}

	private void tokenStyle(ParserRuleContext ctx, String style) {
		if (ctx == null) return;
		rewriter.insertBefore(ctx.start, "«span class=\"" + style + "\"»");
		rewriter.insertAfter(ctx.stop, "«/span»");
	}

	private void tokenStyle(TerminalNode node, String style) {
		if (node == null) return;
		tokenStyle(node.getSymbol(), style);
	}

	private void tokenStyle(Token token, String style) {
		if (token == null) return;
		rewriter.insertBefore(token, "«span class=\"" + style + "\"»");
		rewriter.insertAfter(token, "«/span»");
	}
}
