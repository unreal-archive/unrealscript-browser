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
	<div id="tree-holder">
		<section id="search">
			<input type="text" id="filter" placeholder="filter"/>
		</section>
		<section id="tree"></section>
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
	  const tree = document.getElementById("tree")
	  const filter = document.getElementById("filter")
	  let port2

		// establish comms with the index page
	  window.addEventListener('message', (e) => {
		  port2 = e.ports[0]

		  port2.onmessage = (m) => {
			  console.log(m.data)
		  }
	  })

		function initTree() {
			while (tree.firstChild) {
		  	tree.removeChild(tree.lastChild);
			}
			nodes.forEach(n => tree.appendChild(treeNode(n, 0)))
		}

	  function treeNode(node, depth) {
			const div = document.createElement('div');
			div.classList.add("node");

			const kids = document.createElement('div');

			const pad = document.createElement('span')
			pad.classList.add("pad")
			if (node.children.length) {
				pad.classList.add("plus")
		  	pad.textContent = "+"
		  	pad.addEventListener("click", () => {
			  	kids.classList.toggle("open")
					pad.textContent = kids.classList.contains("open") ? "-" : "+"
				})
	  	}
			div.appendChild(pad);

			const link =  document.createElement('a');
			link.id = `${node.pkg}.${node.clazz}`
			link.textContent = node.clazz
			link.addEventListener("click", () => openClassNode(node))
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

		function filterNodes(filterString) {
		  if (filterString.length === 0) return initTree()
		  if (filterString.length < 3) return

			while (tree.firstChild) {
				tree.removeChild(tree.lastChild);
			}

			nodes.forEach(n => filterNode(tree, n, filterString))
		}

		function filterNode(parent, node, filterString) {
		  if (node.clazz.toLowerCase().includes(filterString.toLowerCase())) {
				const div = document.createElement('div');
				div.classList.add("node");
				const link = document.createElement('a');
				link.id = `${node.pkg}.${node.clazz}`
				link.textContent = node.clazz
				link.addEventListener("click", () => openClassNode(node))
				div.appendChild(link)
				parent.appendChild(div);
			}

		  if (node.children) node.children.forEach(n => filterNode(parent, n, filterString))
		}

		function openClassNode(node) {
			port2.postMessage({
				"event": "nav",
				"url": node.pkg.toLowerCase() + "/" + node.clazz.toLowerCase() + ".html"
			});
		}

		initTree()

	  filter.addEventListener("keyup", () => {
			filterNodes(filter.value)
		})
  })
</script>
</#noparse>

</html>