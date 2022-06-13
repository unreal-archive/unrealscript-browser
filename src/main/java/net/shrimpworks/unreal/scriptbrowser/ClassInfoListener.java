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

	@Override
	public void enterStatedecl(UnrealScriptParser.StatedeclContext ctx) {
		if (!inState) return;
		ctx.identifier().forEach(i -> clazz.addMember(UClass.UMember.UMemberKind.STATE, null, i.getText()));
	}

	@Override
	public void enterVardecl(UnrealScriptParser.VardeclContext ctx) {
		String type = null;
		if (ctx.vartype().localtype() != null) {
			UnrealScriptParser.LocaltypeContext localtype = ctx.vartype().localtype();
			if (localtype.packageidentifier() != null) type = localtype.packageidentifier().getText();
			else if (localtype.classtype() != null) {
				type = localtype.classtype().packageidentifier() != null
					? localtype.classtype().packageidentifier().getText()
					: localtype.classtype().getText();
			} else if (localtype.basictype() != null) type = localtype.basictype().getText();
			else if (localtype.arraydecl() != null) type = localtype.arraydecl().identifier().getText();
			else if (localtype.dynarraydecl() != null) {
				if (localtype.dynarraydecl().basictype() != null) type = localtype.dynarraydecl().basictype().getText();
				else if (localtype.dynarraydecl().classtype() != null) type = localtype.dynarraydecl().classtype().getText();
				else if (localtype.dynarraydecl().packageidentifier() != null)
					type = localtype.dynarraydecl().packageidentifier().classname().getText();
			}
		} else if (ctx.vartype().enumdecl() != null) type = ctx.vartype().enumdecl().identifier().getText();

		for (UnrealScriptParser.VaridentifierContext varIdent : ctx.varidentifier()) {
			if (struct != null) {
				struct.addMember(UClass.UMember.UMemberKind.VARIABLE, type, varIdent.getText());
			} else {
				clazz.addMember(UClass.UMember.UMemberKind.VARIABLE, type, varIdent.getText());
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
	public void exitStructdecl(UnrealScriptParser.StructdeclContext ctx) {
		struct = null;
	}

	@Override
	public void enterEnumdecl(UnrealScriptParser.EnumdeclContext ctx) {
		pkg.addClass(new UClass(UClass.UClassKind.ENUM, null, pkg, ctx.identifier().getText(), clazz.name));
		clazz.addMember(UClass.UMember.UMemberKind.ENUM, null, ctx.identifier().getText());
	}
}
