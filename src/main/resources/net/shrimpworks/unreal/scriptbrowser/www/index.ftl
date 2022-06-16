<!DOCTYPE html>

<html lang="en">
<head>
	<title>UnrealScript Browser</title>
	<link rel="stylesheet" href="static/style.css">
	<link rel="stylesheet" href="static/solarized-light.css" id="style">
	<script>
		if (window.localStorage.getItem("style")) {
			document.getElementById("style").setAttribute("href", "static/" + window.localStorage.getItem("style") + ".css")
		}
	</script>
</head>

<body>
<div id="page">
	<div id="controls">
		<ul>
			<li id="menu-home"><img src="static/icons/home.svg"/></li>
			<li><img src="static/icons/file-code.svg"/>
				<ul id="menu-sources" class="dropdown"></ul>
			</li>
			<li id="menu-download"><img src="static/icons/file-download.svg"/></li>
			<li><img src="static/icons/palette.svg"/>
				<ul id="menu-styles" class="dropdown">
					<li data-name="solarized-light">Solarized Light</li>
					<li data-name="solarized-dark">Solarized Dark</li>
					<li data-name="monokai-ish">Monokai-ish</li>
					<li data-name="unrealed">UnrealEd</li>
					<li data-name="">Basic</li>
				</ul>
			</li>
			<li id="menu-target"><img src="static/icons/target.svg"/></li>
		</ul>
	</div>

	<iframe id="left"></iframe>

	<header><h1 id="header"></h1></header>

	<iframe id="right"></iframe>
</div>
</body>

<script>
	const sources = [
	  <#list sources as src>
	  	{
			  "name": "${src.name}",
				"path": "${src.outPath}",
			},
	  </#list>
  ]
</script>

<#noparse>
<script>
	document.addEventListener("DOMContentLoaded", () => {
		const header = document.getElementById("header")
		const nav = document.getElementById("left")
		const source = document.getElementById("right")

		const styleChangeListeners = [];

		function sourcesMenu() {
			const menu = document.getElementById("menu-sources")
			sources.forEach(s => {
				const item = document.createElement('li')
				item.textContent = s.name
				item.addEventListener("click", () => {
					nav.src = s.path + "/tree.html"
					source.src = s.path + "/index.html"
				})
				menu.appendChild(item)
			})
		}

		sourcesMenu()

		document.querySelectorAll("#menu-styles li").forEach((e) => {
			e.addEventListener("click", () => {
				styleChangeListeners.forEach((f) => f(e.dataset.name))
			})
		})

		styleChangeListeners.push((s) => {
			document.getElementById("style").setAttribute("href", `static/${s}.css`)
			window.localStorage.setItem("style", s);
		})

		// establish comms with the navigation tree, so it can tell us what to put into the source area
		nav.addEventListener("load", () => {
			const channel = new MessageChannel()
			const port1 = channel.port1

			port1.onmessage = (m) => {
				switch (m.data.event) {
					case "nav":
			  		source.src = m.data.url;
						break
					default:
						console.log("unknown message event ", m.data.type, m.data)
				}
			}

			styleChangeListeners.push((s) => {
				channel.port1.postMessage({
					"event": "style",
					"style": s
				})
			})

			nav.contentWindow.postMessage('hello nav', '*', [channel.port2])
		})

	  // establish comms with the source area, so we can change the title based on what its showing
		source.addEventListener("load", () => {
			const channel = new MessageChannel()
			const port1 = channel.port1

			port1.onmessage = (m) => {
				switch (m.data.event) {
					case "loaded":
						header.innerHTML = `${m.data.set} / ${m.data.pkg} / ${m.data.clazz}`
						break
					default:
						console.log("unknown message event ", m.data.event, m.data)
				}
			}

			styleChangeListeners.push((s) => {
				channel.port1.postMessage({
					"event": "style",
					"style": s
				})
			})

			source.contentWindow.postMessage('hello source', '*', [channel.port2])
		})
	})
</script>
</#noparse>

</html>
