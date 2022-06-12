package net.shrimpworks.unreal.scriptbrowser;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UClass implements Comparable<UClass> {

	public static enum UClassKind {
		CLASS,
		STRUCT,
		ENUM
	}

	public final UClassKind kind;
	public final Path path;
	public final UPackage pkg;
	public final String name;
	public final String parent;
	public final Set<String> params;
	public final Set<UMember> members;

	public UClass(UClassKind kind, Path path, UPackage pkg, String name, String parent) {
		this.kind = kind;
		this.path = path;
		this.pkg = pkg;
		this.name = name;
		this.parent = parent;

		this.params = new HashSet<>();
		this.members = new HashSet<>();
		addMember(UMember.UMemberKind.VARIABLE, parent, "super");
	}

	public void addMember(UMember.UMemberKind kind, String type, String name) {
		members.add(new UMember(this, kind, type, name));
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

	public Set<UMember> variables() {
		return members(UMember.UMemberKind.VARIABLE);
	}

	public Optional<UMember> variable(String name) {
		return localMember(UMember.UMemberKind.VARIABLE, name)
			.or(() -> variables().stream().filter(v -> v.name.equalsIgnoreCase(name)).findFirst());
	}

	public Set<UMember> functions() {
		return members(UMember.UMemberKind.FUNCTION);
	}

	public Optional<UMember> function(String name) {
		return localMember(UMember.UMemberKind.FUNCTION, name)
			.or(() -> functions().stream().filter(v -> v.name.equalsIgnoreCase(name)).findFirst());
	}

	private Set<UMember> members(UMember.UMemberKind kind) {
		Set<UMember> mems = localMembers(kind);
		parent().ifPresent(p -> mems.addAll(p.members(kind)));
		return mems;
	}

	private Set<UMember> localMembers(UMember.UMemberKind kind) {
		return members.stream().filter(m -> m.kind == kind).collect(Collectors.toSet());
	}

	private Optional<UMember> localMember(UMember.UMemberKind kind, String name) {
		return members.stream().filter(m -> m.kind == kind && m.name.equalsIgnoreCase(name)).findFirst();
	}

	public static class UMember {

		public static enum UMemberKind {
			FUNCTION,
			VARIABLE,
			STRUCT,
			ENUM,
			STATE
		}

		public final UClass clazz;
		public final UMemberKind kind;
		public final String type;
		public final String name;

		public UMember(UClass clazz, UMemberKind kind, String type, String name) {
			this.clazz = clazz;
			this.kind = kind;
			this.type = type;
			this.name = name;
		}

		@Override
		public String toString() {
			return String.format("%s %s %s.%s", kind, type == null ? "[no type]" : type, clazz.name, name);
		}
	}
}
