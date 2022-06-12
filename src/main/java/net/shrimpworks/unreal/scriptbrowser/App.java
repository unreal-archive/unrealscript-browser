package net.shrimpworks.unreal.scriptbrowser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import net.shrimpworks.unreal.scriptbrowser.www.Generator;
import net.shrimpworks.unreal.unrealscript.UnrealScriptLexer;
import net.shrimpworks.unreal.unrealscript.UnrealScriptParser;

public class App {

	public static class UnrealScriptErrorListener extends BaseErrorListener {

		public static final UnrealScriptErrorListener INSTANCE = new UnrealScriptErrorListener();

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
								RecognitionException e) {
			// no-op?
		}
	}

	public static void main(String[] args) throws IOException {
		CLI cli = CLI.parse(Map.of(), args);

		Path srcPath = Paths.get(cli.commands()[0]);
		Path outPath = Paths.get(cli.commands()[1]);

		USources sources = new USources();

		try (Stream<Path> paths = Files.list(srcPath)) {
			System.err.printf("Loading classes from %s%n", srcPath);
			final AtomicInteger classCounter = new AtomicInteger(0);

			paths.map(p -> {
					 if (!Files.isDirectory(p)) return null;
					 final UPackage pkg = new UPackage(sources, fileName(p));

					 try (Stream<Path> all = Files.walk(p, 3)) {
						 all.forEach(f -> {
							 if (!extension(f).equalsIgnoreCase("uc")) return;

							 try (InputStream is = Files.newInputStream(f, StandardOpenOption.READ)) {
								 UnrealScriptLexer lexer = new UnrealScriptLexer(CharStreams.fromStream(is));
								 lexer.removeErrorListeners();
								 lexer.addErrorListener(UnrealScriptErrorListener.INSTANCE);
								 CommonTokenStream tokens = new CommonTokenStream(lexer);
								 UnrealScriptParser parser = new UnrealScriptParser(tokens);
								 parser.removeErrorListeners();
								 parser.addErrorListener(UnrealScriptErrorListener.INSTANCE);
								 ClassInfoListener listener = new ClassInfoListener(f, pkg);
								 ParseTreeWalker.DEFAULT.walk(listener, parser.program());
							 } catch (IOException e) {
								 throw new RuntimeException(e);
							 }
						 });
					 } catch (IOException e) {
						 throw new RuntimeException(e);
					 }
					 classCounter.addAndGet(pkg.classes.size());
					 return pkg;
				 })
				 .filter(Objects::nonNull)
				 .filter(p -> !p.classes.isEmpty())
				 .forEach(sources::addPackage);

			System.err.printf("Loaded %d classes in %d packages%n", classCounter.get(), sources.packages.size());
		}

		Generator.offloadStatic("static.list", outPath);

		System.err.println("Generating source pages");
		sources.packages.values().forEach(pkg -> pkg.classes.values().parallelStream()
															.filter(c -> c.kind == UClass.UClassKind.CLASS)
															.forEach(e -> Generator.src(e, outPath)));

		System.err.println("Generating navigation tree");
		Generator.tree(children(sources, null), outPath);
//		printTree(children(sources, null), 0);

		System.err.println("Done");
	}

	public static List<UClassNode> children(USources sources, UClass parent) {
		return sources.packages.values().stream()
							   .flatMap(p -> p.classes.values().stream())
							   .filter(c -> {
								   if (parent == null && c.parent == null) return true;
								   else return parent != null && parent.name.equalsIgnoreCase(c.parent);
							   })
							   .sorted()
							   .map(c -> new UClassNode(c, children(sources, c)))
							   .collect(Collectors.toList());
	}

	public static void printTree(List<UClassNode> nodes, int depth) {
		for (UClassNode n : nodes) {
			System.out.printf("%s%s%n", " ".repeat(depth), n.clazz.name);
			printTree(n.children, depth + 2);
		}
	}

	public static String extension(Path path) {
		return extension(path.toString());
	}

	public static String extension(String path) {
		return path.substring(path.lastIndexOf(".") + 1);
	}

	public static String fileName(Path path) {
		return fileName(path.toString());
	}

	public static String fileName(String path) {
		String tmp = path.replaceAll("\\\\", "/");
		return tmp.substring(Math.max(0, tmp.lastIndexOf("/") + 1));
	}

	public static String filePath(String path) {
		String tmp = path.replaceAll("\\\\", "/");
		return tmp.substring(0, Math.max(0, tmp.lastIndexOf("/")));
	}

	public static String plainName(Path path) {
		return plainName(path.toString());
	}

	public static String plainName(String path) {
		String tmp = fileName(path);
		return tmp.substring(0, tmp.lastIndexOf(".")).replaceAll("/", "").trim().replaceAll("[^\\x20-\\x7E]", "");
	}

}
