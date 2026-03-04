<!DOCTYPE html>
<html lang="en">
<head>
	<title>${sources.name}</title>
	<link rel="stylesheet" href="../static/styles/style.css">
	<link rel="stylesheet" href="../static/styles/solarized-light.css" id="style">
	<script>
	  const urlParams = new URLSearchParams(window.location.search);
	  const style = document.getElementById("style")
	  let currentStyle = 'solarized-light'

	  if (urlParams.has('s')) {
		  currentStyle = urlParams.get('s')
	  } else if (window.localStorage.getItem("style")) {
		  currentStyle = window.localStorage.getItem("style")
	  }

	  style.setAttribute("href", "../static/styles/" + currentStyle + ".css")
	</script>
</head>

<body>
<article id="home">
	<h3>${sources.name} contents:</h3>
	<div class="contents">
		<span class="pkg head">Package</span>
		<span class="count head">Classes</span>
		<#list sources.packages as k, pkg>
			<div>
				<span class="pkg">${pkg.name}</span>
				<span class="count">${pkg.classes?size}</span>
			</div>
		</#list>
		<a href="${sources.name?replace(" ", "_") + ".zip"}">
			<img src="../static/icons/file-download.svg" alt="download"/>
			Download Sources (by package)
		</a>
		<a href="${sources.name?replace(" ", "_") + "_tree.zip"}">
			<img src="../static/icons/file-download.svg" alt="download"/>
			Download Sources (as class tree)
		</a>
	</div>
</article>
</body>

<script>
	document.addEventListener("DOMContentLoaded", () => {
		window.addEventListener('message', (e) => {
			const port2 = e.ports[0]

			port2.postMessage({
				"event": "home",
				"set": "${sources.name}",
				"setPath": "${sources.outPath}",
			});

			port2.onmessage = (m) => {
				switch (m.data.event) {
					case "style":
						currentStyle = m.data.style
						style.setAttribute("href", "../static/styles/" + currentStyle + ".css")
						break
					default:
						console.log("unknown message event ", m.data.event, m.data)
				}
			}
		})
	})
</script>

</html>