package net.shrimpworks.unreal.scriptbrowser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.scriptbrowser.entities.UClass;
import net.shrimpworks.unreal.scriptbrowser.entities.UClassNode;
import net.shrimpworks.unreal.scriptbrowser.entities.UPackage;
import net.shrimpworks.unreal.scriptbrowser.entities.USources;
import net.shrimpworks.unreal.scriptbrowser.listeners.ClassInfoListener;
import net.shrimpworks.unreal.scriptbrowser.www.Generator;
import net.shrimpworks.unreal.scriptbrowser.www.ZipGenerator;

public class App {

	public static class Progress {

		private static final int WIDTH = 50;

		private final double total;

		public Progress(int total) {
			this.total = total;
		}

		public void progress(int processed, UClass current) {
			int prog = (int)((processed / total) * WIDTH);
			int space = WIDTH - prog;
			System.err.printf("\r    [%s%s] %d/%d: %-20s", "=".repeat(prog), " ".repeat(space),
							  processed, (int)total, current.name.substring(0, Math.min(20, current.name.length())));
		}
	}

	public static void main(String[] args) throws IOException {
		final CLI cli = CLI.parse(Map.of(), Set.of(), args);

		final Path outPath = Paths.get(cli.commands()[0]);

		final List<USources> sources = createSourcesFromProperties();

		if (!cli.flag("skip-sources")) {
			for (USources source : sources) {

				System.err.printf("Generating sources for %s%n", source.name);
				loadSources(source);
//				printTree(children(source, null), 0);

				final long loadedTime = System.currentTimeMillis();

				final Path srcOut = outPath.resolve(source.outPath);

				System.err.println("  - Generating navigation tree");
				Generator.tree(children(source, null), srcOut);

				System.err.println("  - Generating source pages");
				Progress p = new Progress(source.classCount());
				AtomicInteger counter = new AtomicInteger();
				source.packages.values().forEach(pkg -> pkg.classes.values().parallelStream()
																   .filter(c -> c.kind == UClass.UClassKind.CLASS)
																   .forEach(c -> {
																	   p.progress(counter.incrementAndGet(), c);
																	   Generator.src(c, srcOut);
																   }));
				System.err.printf("\r%-90s", ""); // clear progress :/

				// FIXME landing page/info for source set

				final long genTime = System.currentTimeMillis();
				System.err.printf("\r  - Generated HTML in %dms%n", genTime - loadedTime);

				System.err.println("  - Generating source archive");
				ZipGenerator.zipSources(source, srcOut);

				final long zipTime = System.currentTimeMillis();
				System.err.printf("\r  - Generated source archive in %dms%n", zipTime - genTime);
			}
		}

		System.err.println("Generating index page");
		Generator.offloadStatic("static.list", outPath);
		Generator.index(sources, outPath);

		System.err.println("Done");
	}

	private static List<USources> createSourcesFromProperties() throws IOException {
		final IniFile config = new IniFile(Paths.get("sources.ini"));
		return config.sections().stream()
					 .map(s -> new USources(
						 s,
						 config.section(s).value("out").toString(),
						 config.section(s)
							   .asList("paths").values
							 .stream()
							 .map(v -> Paths.get(v.toString()))
							 .collect(Collectors.toList()))
					 ).collect(Collectors.toList());
	}

	private static void loadSources(USources sources) throws IOException {
		for (Path srcPath : sources.paths) {
			loadSources(sources, srcPath);
		}
	}

	private static void loadSources(USources sources, Path srcPath) throws IOException {
		final long startTime = System.currentTimeMillis();
		try (Stream<Path> paths = Files.list(srcPath)) {
			System.err.printf("  - Loading classes from %s%n", srcPath);

			paths.map(p -> {
					 if (!Files.isDirectory(p)) return null;
					 final UPackage pkg = new UPackage(sources, fileName(p));

					 try (Stream<Path> all = Files.walk(p, 3)) {
						 all.forEach(f -> {
							 if (!extension(f).equalsIgnoreCase("uc")) return;

							 ClassInfoListener.processFile(f, pkg);
						 });
					 } catch (IOException e) {
						 throw new RuntimeException(e);
					 }
					 return pkg;
				 })
				 .filter(Objects::nonNull)
				 .filter(p -> !p.classes.isEmpty())
				 .forEach(sources::addPackage);
		}
		final long loadedTime = System.currentTimeMillis();
		System.err.printf("  - Loaded %d classes in %d packages in %dms%n",
						  sources.classCount(), sources.packages.size(), loadedTime - startTime);
	}

	public static List<UClassNode> children(USources sources, UClass parent) {
		return sources.packages.values().stream()
							   .flatMap(p -> p.classes.values().stream())
							   .filter(c -> c.kind == UClass.UClassKind.CLASS)
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
