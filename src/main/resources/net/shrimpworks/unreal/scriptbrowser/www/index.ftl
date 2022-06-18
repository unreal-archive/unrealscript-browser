<!DOCTYPE html>

<html lang="en">
<head>
	<title>UnrealScript Browser</title>
	<link rel="stylesheet" href="static/styles/style.css">
	<link rel="stylesheet" href="static/styles/solarized-light.css" id="style">
	<script>
	  const urlParams = new URLSearchParams(window.location.search);
	  const style = document.getElementById("style")
		let currentStyle = 'solarized-light'

		if (urlParams.has('s')) {
			currentStyle = urlParams.get('s')
	  } else if (window.localStorage.getItem("style")) {
			currentStyle = window.localStorage.getItem("style")
	  }

		style.setAttribute("href", "static/styles/" + currentStyle + ".css")
	</script>
</head>

<body>
<div id="page">
	<div id="controls">
		<ul>
			<li id="menu-home" title="Home"><img src="static/icons/home.svg" alt="home"/></li>
			<li title="Select Source Set"><img src="static/icons/file-code.svg" alt="sources"/>
				<ul id="menu-sources" class="dropdown"></ul>
			</li>
			<li id="menu-download" title="Download Current Sources"><img src="static/icons/file-download.svg" alt="download"/></li>
			<li title="Set Highlighting Style"><img src="static/icons/palette.svg" alt="style"/>
				<ul id="menu-styles" class="dropdown">
					<li data-name="solarized-light">Solarized Light</li>
					<li data-name="solarized-dark">Solarized Dark</li>
					<li data-name="monokai-ish">Monokai-ish</li>
					<li data-name="unrealed">UnrealEd</li>
					<li data-name="recodex">ReCodeX</li>
					<li data-name="">Basic</li>
				</ul>
			</li>
			<li id="menu-target" title="Select Current Class"><img src="static/icons/target.svg" alt="goto"/></li>
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
		let shownSource;

		const styleChangeListeners = [];

	  document.getElementById("menu-home").addEventListener("click", () => window.location = "index.html")

		function sourcesMenu() {
			const menu = document.getElementById("menu-sources")
			sources.forEach(s => {
				const item = document.createElement('li')
				item.textContent = s.name
				item.addEventListener("click", () => {
					loadSources(s)

					history.replaceState(null, header.textContent, `#${s.path}`)
				})
				menu.appendChild(item)
			})
		}

		function loadSources(sources) {
			if (!shownSource || sources.path !== shownSource.setPath) nav.src = sources.path + "/tree.html?s=" + currentStyle
			source.src = sources.path + "/index.html?s=" + currentStyle

			// configure the download button
			document.getElementById("menu-download").addEventListener("click", () => {
				const lnk = document.createElement("a")
				lnk.href = sources.path + "/" + sources.name.replaceAll(" ", "_") + ".zip"
				lnk.click()
			})
		}

		function loadScript(path, pkg, clazz) {
			source.src = `${path.toLowerCase()}/${pkg.toLowerCase()}/${clazz.toLowerCase()}.html?s=${currentStyle}`
		}

		sourcesMenu()

	  document.querySelectorAll("#menu-styles li").forEach((e) => {
		  e.addEventListener("click", () => {
			  styleChangeListeners.forEach((f) => f(e.dataset.name))
		  })
	  })

		styleChangeListeners.push((s) => {
			currentStyle = s
			document.getElementById("style").setAttribute("href", `static/styles/${currentStyle}.css`)
			window.localStorage.setItem("style", currentStyle);
		})

		// establish comms with the navigation tree, so it can tell us what to put into the source area
		nav.addEventListener("load", () => {
			const channel = new MessageChannel()
			const port1 = channel.port1

			port1.onmessage = (m) => {
				switch (m.data.event) {
					case "nav":
						loadScript(m.data.path, m.data.pkg, m.data.clazz)
						break
					default:
						console.log("unknown message event ", m.data.type, m.data)
				}
			}

			styleChangeListeners.push((s) => {
				port1.postMessage({
					"event": "style",
					"style": s
				})
			})

			// clicking "goto source" transfers the current selected node to the tree, which will navigate to the class
			document.getElementById("menu-target").addEventListener("click", () => {
				if (shownSource) {
					port1.postMessage({
						"event": "goto",
						"target": shownSource
					})
				}
			})

			nav.contentWindow.postMessage('hello nav', '*', [channel.port2])
		})

	  // establish comms with the source area, so we can change the title based on what its showing
		source.addEventListener("load", () => {
			const channel = new MessageChannel()
			const port1 = channel.port1

			const pushStyle = (s) => {
				channel.port1.postMessage({
					"event": "style",
					"style": s
				})
			}

			port1.onmessage = (m) => {
				switch (m.data.event) {
					case "loaded":
						header.innerHTML = `${m.data.set} / ${m.data.pkg} / ${m.data.clazz}`

						const loadedState = `#${m.data.setPath}/${m.data.pkg}/${m.data.clazz}`
						if (loadedState !== window.location.hash) {
							history.replaceState(null, header.textContent, loadedState)
						}

			  		pushStyle(currentStyle)
						shownSource = m.data
						break
					case "home":
						header.innerHTML = `${m.data.set}`
						const homeState = `#${m.data.setPath}`
						if (homeState !== window.location.hash) {
							history.replaceState(null, header.textContent, homeState)
						}
						pushStyle(currentStyle)
						shownSource = m.data
						break
					default:
						console.log("unknown message event ", m.data.event, m.data)
				}
			}

			styleChangeListeners.push(pushStyle)

			source.contentWindow.postMessage('hello source', '*', [channel.port2])
		})

		function navFromHash(hash) {
			if (hash) {
				const parts = hash.substring(1).split("/")
				const source = sources.find((s) => s.path === parts[0])
				loadSources(source)

				if (parts.length === 3) loadScript(parts[0], parts[1], parts[2])
			}
		}

		// restore state based on #url (#sources/pkg/clazz)
		if (window.location.hash) {
			navFromHash(window.location.hash)
		} else {
			source.src = "home.html?s=" + currentStyle
		}

		window.addEventListener("popstate", () => navFromHash(window.location.hash));
	})
</script>
</#noparse>

</html>
