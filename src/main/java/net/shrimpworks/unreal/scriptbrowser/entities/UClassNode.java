package net.shrimpworks.unreal.scriptbrowser.entities;

import java.util.List;

public class UClassNode {

	public final UClass clazz;
	public final List<UClassNode> children;

	public UClassNode(UClass clazz, List<UClassNode> children) {
		this.clazz = clazz;
		this.children = children;
	}

}
