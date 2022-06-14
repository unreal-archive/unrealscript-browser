<!DOCTYPE html>

<html lang="en">
<head>
	<title>UnrealScript Browser</title>
	<link rel="stylesheet" href="static/style.css">
	<link rel="stylesheet" href="static/solarized-light.css">
</head>

<body>
<div id="page">
	<iframe id="nav" src="tree.html"></iframe>

	<header><h1 id="header"></h1></header>

	<iframe id="source"></iframe>
</div>
</body>

<#noparse>
<script>
	document.addEventListener("DOMContentLoaded", () => {
		const header = document.getElementById("header")
		const nav = document.getElementById("nav")
		const source = document.getElementById("source")

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

			nav.contentWindow.postMessage('hello nav', '*', [channel.port2])
		})

	  // establish comms with the source area, so we can change the title based on what its showing
		source.addEventListener("load", () => {
			const channel = new MessageChannel()
			const port1 = channel.port1

			port1.onmessage = (m) => {
				switch (m.data.event) {
					case "loaded":
						header.innerHTML = m.data.pkg + " / " + m.data.clazz
						break
					default:
						console.log("unknown message event ", m.data.type, m.data)
				}
			}

			source.contentWindow.postMessage('hello source', '*', [channel.port2])
		})
	})
</script>
</#noparse>

</html>
