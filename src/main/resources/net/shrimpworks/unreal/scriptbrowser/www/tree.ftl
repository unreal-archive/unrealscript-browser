<#macro treenode node depth>
	<div class="node" data-package="${node.clazz.pkg.name?lower_case}" data-name="${node.clazz.name?lower_case}">
		<#list 0..depth as d>
			<#if d == depth && node.children?size gt 0>
				<span class="pad plus">+</span>
			<#else>
				<span class="pad">&nbsp;</span>
			</#if>
		</#list>
		<a id="${node.clazz.pkg.name}.${node.clazz.name}">
			<span class="name">${node.clazz.name}</span>
		</a>
		<#list node.children>
			<div class="children">
				<#items as child><@treenode child depth+1/></#items>
			</div>
		</#list>
	</div>
</#macro>

<!DOCTYPE html>

<html lang="en">
<head>
	<link rel="stylesheet" href="static/style.css">
	<link rel="stylesheet" href="static/solarized-light.css">
</head>

<body>
	<div id="page">
		<section id="tree">
			<#list nodes as node>
				<@treenode node 0/>
			</#list>
		</section>

		<header></header>

		<iframe id="source"></iframe>
	</div>
</body>

<script>
	document.addEventListener("DOMContentLoaded", () => {
		const source = document.getElementById("source")

		document.querySelectorAll('#tree .node').forEach(node => {
			const a = node.querySelector("a");
			a.addEventListener("click", () => {
				source.src = node.dataset.package + "/" + node.dataset.name + ".html";
			})

			const plus = node.querySelector(".plus")
			if (!plus) return;
			plus.addEventListener("click", () => {
				const kids = node.querySelector(".children")
				if (kids) kids.classList.toggle("open")
			})
		})
  })
</script>

</html>