<!DOCTYPE html>
<html lang="en">
<head>
	<title>${clazz.pkg.sourceSet.name} / ${clazz.pkg.name}.${clazz.name}</title>
	<link rel="stylesheet" href="../../static/style.css">
	<link rel="stylesheet" href="../../static/solarized-light.css" id="style">
	<script>
		// FIXME query string?
	  if (window.localStorage.getItem("style")) {
		  document.getElementById("style").setAttribute("href", "../../static/" + window.localStorage.getItem("style") + ".css")
	  }
	</script>
</head>

<body>
	<article id="script">
		<section id="lines">
			<#list 1..lines as line>
				<div>${line?c}</div>
			</#list>
		</section>

		<#outputformat "plainText">
			<pre>${source}</pre>
		</#outputformat>
	</article>
</body>

<script>
	document.addEventListener("DOMContentLoaded", () => {
	  window.addEventListener('message', (e) => {
			const port2 = e.ports[0]

			port2.postMessage({
					"event": "loaded",
					"set": "${clazz.pkg.sourceSet.name}",
					"pkg": "${clazz.pkg.name}",
					"clazz": "${clazz.name}"
			});

			port2.onmessage = (m) => {
				switch (m.data.event) {
					case "style":
						document.getElementById("style").setAttribute("href", "../../static/" + m.data.style + ".css")
						break
					default:
						console.log("unknown message event ", m.data.event, m.data)
				}
			}
		})
	})
</script>

</html>