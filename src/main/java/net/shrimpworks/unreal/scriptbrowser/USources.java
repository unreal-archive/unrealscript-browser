package net.shrimpworks.unreal.scriptbrowser;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class USources {

	public final Map<String, UPackage> packages;

	public USources() {
		this.packages = new TreeMap<>();
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

}
