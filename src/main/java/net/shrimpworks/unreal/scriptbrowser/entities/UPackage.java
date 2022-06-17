package net.shrimpworks.unreal.scriptbrowser.entities;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class UPackage implements Comparable<UPackage> {

	public final USources sourceSet;
	public final String name;
	public final Map<String, UClass> classes;

	public UPackage(USources sourceSet, String name) {
		this.sourceSet = sourceSet;
		this.name = name;
		this.classes = new TreeMap<>();
	}

	public void addClass(UClass clazz) {
		classes.put(clazz.name.toLowerCase(), clazz);
	}

	public Optional<UClass> clazz(String name) {
		return Optional.ofNullable(classes.get(name.toLowerCase()));
	}

	@Override
	public String toString() {
		return String.format("package %s", name);
	}

	@Override
	public int compareTo(UPackage o) {
		return name.compareToIgnoreCase(o.name);
	}
}
