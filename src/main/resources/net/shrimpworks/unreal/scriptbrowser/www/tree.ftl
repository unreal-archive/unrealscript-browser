<#macro jsonnode node depth>
	{
		"path": "${node.clazz.pkg.sourceSet.outPath}",
		"set": "${node.clazz.pkg.sourceSet.name}",
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
	<link rel="stylesheet" href="../static/style.css">
	<link rel="stylesheet" href="../static/solarized-light.css" id="style">
	<script>
	  const urlParams = new URLSearchParams(window.location.search);
	  const style = document.getElementById("style")
	  let currentStyle = 'solarized-light'

	  if (urlParams.has('s')) {
		  currentStyle = urlParams.get('s')
	  } else if (window.localStorage.getItem("style")) {
		  currentStyle = window.localStorage.getItem("style")
	  }

		style.setAttribute("href", "../static/" + currentStyle + ".css")
	</script>
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
				switch (m.data.event) {
					case "style":
			  		currentStyle = m.data.style
			  		style.setAttribute("href", `../static/${currentStyle}.css`)
						break
					case "goto":
			  		gotoNode(m.data.target)
						break
					default:
						console.log("unknown message event ", m.data.event, m.data)
				}
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
			div.classList.add("node")
			div.id = `${node.pkg.toLowerCase()}.${node.clazz.toLowerCase()}`

			const kids = document.createElement('div')

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

			const link =  document.createElement('a')
			link.textContent = node.clazz
			link.addEventListener("click", () => openClassNode(node))
			div.appendChild(link)

			if (node.children) {
				kids.classList.add("children")
				node.children.forEach((child) => {
					kids.appendChild(treeNode(child, depth + 1))
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
				div.id = `${node.pkg.toLowerCase()}.${node.clazz.toLowerCase()}`
				div.classList.add("node");
				const link = document.createElement('a');
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
				"path": node.path.toLowerCase(),
				"pkg": node.pkg.toLowerCase(),
				"clazz": node.clazz.toLowerCase(),
			});
		}

		function gotoNode(target) {
			const node = document.getElementById(`${target.pkg.toLowerCase()}.${target.clazz.toLowerCase()}`)
			let parent = node.parentElement
			while (parent != null && (parent.classList.contains("node") || parent.classList.contains("children"))) {
				if (parent.classList.contains("children")) {
					if (!parent.classList.contains("open")) parent.classList.add("open")
				}

				parent = parent.parentElement
			}
			node.scrollIntoView(true)
		}

		initTree()

	  filter.addEventListener("keyup", () => {
			filterNodes(filter.value)
		})
  })
</script>
</#noparse>

</html>