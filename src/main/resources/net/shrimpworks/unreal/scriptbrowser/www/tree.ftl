<#macro jsonnode node depth>
	{
		"pkg": "${node.clazz.pkg.name}",
		"clazz": "${node.clazz.name}",
		"children": [
			<#list node.children as child><@jsonnode child depth+1/>,</#list>
		]
	}
</#macro>

<!DOCTYPE html>

<html lang="en">
<head>
	<link rel="stylesheet" href="static/style.css">
	<link rel="stylesheet" href="static/solarized-light.css">
</head>

<body>
	<div id="page">
		<header><h1 id="header"></h1></header>

		<section id="tree"></section>

		<iframe id="source"></iframe>
	</div>
</body>

<script>
	const nodes = [
	  <#list nodes as node><@jsonnode node 0/>,</#list>
	]
</script>

<#noparse>
<script>
  document.addEventListener("DOMContentLoaded", () => {
	  const header = document.getElementById("header")
	  const tree = document.getElementById("tree")
	  const source = document.getElementById("source")

	  function treeNode(node, depth) {
			const div = document.createElement('div');
			div.classList.add("node");

			const kids = document.createElement('div');

			for (let i = 0; i <= depth; i++) {
				const pad = document.createElement('span');
				pad.classList.add("pad");
				if (i === depth && node.children.length) {
					pad.classList.add("plus");
					pad.textContent = "+";

					pad.addEventListener("click", () => {
						kids.classList.toggle("open")
					})
				}
				div.appendChild(pad);
			}

			const link =  document.createElement('a');
			link.id = `${node.pkg}.${node.clazz}`
			link.textContent = node.clazz
			link.addEventListener("click", () => {
				source.src = node.pkg.toLowerCase() + "/" + node.clazz.toLowerCase() + ".html";
			})
			div.appendChild(link)

			if (node.children) {
				kids.classList.add("children")
				node.children.forEach((child) => {
					kids.appendChild(treeNode(child, depth + 1));
				});
				div.appendChild(kids)
			}
			return div;
		}

	  nodes.forEach(n => tree.appendChild(treeNode(n, 0)));

		source.addEventListener("load", () => {
			const channel = new MessageChannel();
			const port1 = channel.port1;

			port1.onmessage = (m) => {
				switch (m.data.event) {
					case "loaded":
						header.innerHTML = m.data.pkg + " / " + m.data.clazz
						break;
					default:
						console.log("unknown message event ", m.data.type, m.data)
		  	}
			}

			source.contentWindow.postMessage('hello frame', '*', [channel.port2]);
		})
  })
</script>
</#noparse>

</html>