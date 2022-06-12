package net.shrimpworks.unreal.scriptbrowser;

import java.nio.file.Path;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.RuleContext;

import net.shrimpworks.unreal.unrealscript.UnrealScriptBaseListener;
import net.shrimpworks.unreal.unrealscript.UnrealScriptParser;

class ClassInfoListener extends UnrealScriptBaseListener {

	private final Path sourceFile;
	private final UPackage pkg;

	private UClass clazz;

	private UClass struct = null;

	// stateful processing
	private boolean inState = false;

	public ClassInfoListener(Path sourceFile, UPackage pkg) {
		this.sourceFile = sourceFile;
		this.pkg = pkg;
	}

	@Override
	public void enterClassdecl(UnrealScriptParser.ClassdeclContext ctx) {
		clazz = new UClass(
			UClass.UClassKind.CLASS,
			sourceFile,
			pkg,
			ctx.classname().getText(),
			ctx.parentclass() == null ? null : ctx.parentclass().getText()
		);
		pkg.addClass(clazz);

		clazz.params.addAll(ctx.classparams().stream().map(RuleContext::getText).collect(Collectors.toSet()));
	}

	@Override
	public void enterStatebody(UnrealScriptParser.StatebodyContext ctx) {
		inState = true;
	}

	@Override
	public void exitStatebody(UnrealScriptParser.StatebodyContext ctx) {
		inState = false;
	}

	// FIXME add states and labels as members

	@Override
	public void enterVardecl(UnrealScriptParser.VardeclContext ctx) {
		String type = null;
		if (ctx.vartype().packageidentifier() != null) type = ctx.vartype().packageidentifier().getText();
		else if (ctx.vartype().classtype() != null) type = ctx.vartype().classtype().getText();
		else if (ctx.vartype().basictype() != null) type = ctx.vartype().basictype().getText();
		else if (ctx.vartype().enumdecl() != null) type = ctx.vartype().enumdecl().identifier().getText();
		else if (ctx.vartype().arraydecl() != null) type = ctx.vartype().arraydecl().identifier().getText();
		else if (ctx.vartype().dynarraydecl() != null) {
			if (ctx.vartype().dynarraydecl().basictype() != null) type = ctx.vartype().dynarraydecl().basictype().getText();
			else if (ctx.vartype().dynarraydecl().classtype() != null) type = ctx.vartype().dynarraydecl().classtype().getText();
			else if (ctx.vartype().dynarraydecl().packageidentifier() != null)
				type = ctx.vartype().dynarraydecl().packageidentifier().classname().getText();
		}

		for (UnrealScriptParser.VaridentifierContext varItent : ctx.varidentifier()) {
			if (struct != null) {
				struct.addMember(UClass.UMember.UMemberKind.VARIABLE, type, varItent.getText());
			} else {
				clazz.addMember(UClass.UMember.UMemberKind.VARIABLE, type, varItent.getText());
			}
		}
	}

	@Override
	public void enterNormalfunc(UnrealScriptParser.NormalfuncContext ctx) {
		// skip tracking function declarations within states for now
		if (inState) return;

		String type = null;
		if (ctx.localtype() != null) type = ctx.localtype().getText();
		clazz.addMember(UClass.UMember.UMemberKind.FUNCTION, type, ctx.identifier().getText());
	}

	@Override
	public void enterStructdecl(UnrealScriptParser.StructdeclContext ctx) {
		struct = new UClass(UClass.UClassKind.STRUCT, null, pkg, ctx.identifier().getText(), clazz.name);
		pkg.addClass(struct);
		clazz.addMember(UClass.UMember.UMemberKind.STRUCT, null, ctx.identifier().getText());
	}

	@Override
	public void enterEnumdecl(UnrealScriptParser.EnumdeclContext ctx) {
		struct = null;
		clazz.addMember(UClass.UMember.UMemberKind.ENUM, null, ctx.identifier().getText());
	}
}
