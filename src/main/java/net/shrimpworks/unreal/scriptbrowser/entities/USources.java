package net.shrimpworks.unreal.scriptbrowser.entities;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class USources {

	public final String name;
	public final String outPath;
	public final List<Path> paths;
	public final Map<String, UPackage> packages;

	public USources(String name, String outPath, List<Path> paths) {
		this.name = name;
		this.outPath = outPath;
		this.paths = paths;
		this.packages = new TreeMap<>();
	}

	public int classCount() {
		return packages.values().stream().mapToInt(p -> p.classes.size()).sum();
	}

	public void addPackage(UPackage pkg) {
		packages.put(pkg.name.toLowerCase(), pkg);
	}

	public Optional<UPackage> pkg(String name) {
		if (name == null) return Optional.empty();
		return Optional.ofNullable(packages.get(name.toLowerCase()));
	}

	public Optional<UClass> clazz(String name) {
		if (name == null) return Optional.empty();
		return clazz(name, (String)null);
	}

	public Optional<UClass> clazz(String name, UPackage pkg) {
		if (pkg == null) return clazz(name, (String)null);
		return pkg.clazz(name)
				  .or(() -> packages.values().stream().flatMap(p -> p.clazz(name).stream()).findFirst());
	}

	public Optional<UClass> clazz(String name, String pkg) {
		return pkg(pkg)
			.flatMap(p -> p.clazz(name))
			.or(() -> packages.values().stream().flatMap(p -> p.clazz(name).stream()).findFirst());
	}

	/**
	 * Generate a hierarchical class tree of the sources within this source set.
	 */
	public List<UClassNode> children() {
		return children(null);
	}

	private List<UClassNode> children(UClass parent) {
		return packages.values().stream()
					   .flatMap(p -> p.classes.values().stream())
					   .filter(c -> c.kind == UClass.UClassKind.CLASS)
					   .filter(c -> {
						   if (parent == null && c.parent == null) return true;
						   else return parent != null && parent.name.equalsIgnoreCase(c.parent);
					   })
					   .sorted()
					   .map(c -> new UClassNode(c, children(c)))
					   .collect(Collectors.toList());
	}
}
