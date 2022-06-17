package net.shrimpworks.unreal.scriptbrowser.listeners;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import net.shrimpworks.unreal.scriptbrowser.App;

public class UnrealScriptErrorListener extends BaseErrorListener {

	public static final UnrealScriptErrorListener INSTANCE = new UnrealScriptErrorListener();

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
							RecognitionException e) {
		// no-op?
	}
}
