package net.shrimpworks.unreal.scriptbrowser;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UClass implements Comparable<UClass> {

	public final Path path;
	public final UPackage pkg;
	public final String name;
	public final String parent;
	public final Set<String> params;
	public final Set<UMember> members;

	public UClass(Path path, UPackage pkg, String name, String parent) {
		this.path = path;
		this.pkg = pkg;
		this.name = name;
		this.parent = parent;

		this.params = new HashSet<>();
		this.members = new HashSet<>();
	}

	public void addMember(UMember.UMemberKind kind, String type, String name) {
		members.add(new UMember(kind, type, name));
	}

	@Override
	public String toString() {
		if (parent != null) return String.format("class %s extends %s", name, parent);
		else return String.format("class %s", name);
	}

	@Override
	public int compareTo(UClass o) {
		return name.compareToIgnoreCase(o.name);
	}

	public Optional<UClass> parent() {
		if (parent == null) return Optional.empty();

		return pkg.clazz(parent)
				  .or(() -> pkg.sourceSet.clazz(parent));
	}

	public Map<UClass, Set<String>> variables() {
		return members(UMember.UMemberKind.VARIABLE);
	}

	public Map<UClass, Set<String>> functions() {
		return members(UMember.UMemberKind.FUNCTION);
	}

	private Map<UClass, Set<String>> members(UMember.UMemberKind kind) {
		Map<UClass, Set<String>> mems = new HashMap<>();
		mems.put(this, members.stream().filter(m -> m.kind == kind).map(m -> m.name).collect(Collectors.toSet()));
		parent().ifPresent(p -> mems.putAll(p.members(kind)));
		return mems;
	}

	public static class UMember {

		public static enum UMemberKind {
			FUNCTION,
			VARIABLE,
			STRUCT,
			ENUM
		}

		public final UMemberKind kind;
		public final String type;
		public final String name;

		public UMember(UMemberKind kind, String type, String name) {
			this.kind = kind;
			this.type = type;
			this.name = name;
		}
	}
}
