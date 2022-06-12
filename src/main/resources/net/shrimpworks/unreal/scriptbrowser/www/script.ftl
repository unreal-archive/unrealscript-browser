<!DOCTYPE html>
<html lang="en">
<head>
	<title>${clazz.pkg.name}.${clazz.name}</title>
	<link rel="stylesheet" href="../static/style.css">
	<link rel="stylesheet" href="../static/solarized-light.css">
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
					"pkg": "${clazz.pkg.name}",
					"clazz": "${clazz.name}"
			});

			port2.onmessage = (m) => {
				console.log(m.data)
			}
		})
	})
</script>

</html>