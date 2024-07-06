<!DOCTYPE html>
<html lang="en">
<head>
	<title>${clazz.pkg.sourceSet.name} / ${clazz.pkg.name}.${clazz.name}</title>
	<link rel="stylesheet" href="../../static/styles/style.css">
	<link rel="stylesheet" href="../../static/styles/solarized-light.css" id="style">
	<script>
	  const urlParams = new URLSearchParams(window.location.search);
	  const style = document.getElementById("style")
	  let currentStyle = 'solarized-light'

	  if (urlParams.has('s')) {
		  currentStyle = urlParams.get('s')
	  } else if (window.localStorage.getItem("style")) {
		  currentStyle = window.localStorage.getItem("style")
	  }

		style.setAttribute("href", "../../static/styles/" + currentStyle + ".css")
	</script>
</head>

<body>
	<article id="script">
		<section id="members">
			<div class="title">Shortcuts</div>
			<div class="group">
				<div><a href="#class">Declaration</a></div>
				<div id="replication-jump"><a href="#replication">Replication</a></div>
				<div id="default-jump"><a href="#default">Default Properties</a></div>
			</div>

			<#list clazz.localVariables?sort_by("name")>
				<div class="title">Variables</div>
				<div class="group">
					<#items as m>
						<div><a href="#${m.name?c_lower_case}">${m.name}</a></div>
					</#items>
				</div>
			</#list>
			<#list clazz.localEnums?sort_by("name")>
				<div class="title">Enums</div>
				<div class="group">
					<#items as m>
						<div><a href="#${m.name?c_lower_case}">${m.name}</a></div>
					</#items>
				</div>
			</#list>
			<#list clazz.localStructs?sort_by("name")>
				<div class="title">Structs</div>
				<div class="group">
					<#items as m>
						<div><a href="#${m.name?c_lower_case}">${m.name}</a></div>
					</#items>
				</div>
			</#list>
			<#list clazz.localFunctions?sort_by("name")>
				<div class="title">Functions</div>
				<div class="group">
					<#items as m>
						<div><a href="#${m.name?c_lower_case}">${m.name}</a></div>
					</#items>
				</div>
			</#list>
			<#list clazz.localStates?sort_by("name")>
				<div class="title">States</div>
				<div class="group">
					<#items as m>
						<div><a href="#${m.name?c_lower_case}">${m.name}</a></div>
					</#items>
				</div>
			</#list>
		</section>

		<section id="lines">
			<#list 1..lines as line><div>${line?c}</div></#list>
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
					"setPath": "${clazz.pkg.sourceSet.outPath}",
					"pkg": "${clazz.pkg.name}",
					"clazz": "${clazz.name}",
			});

			port2.onmessage = (m) => {
				switch (m.data.event) {
					case "style":
							currentStyle = m.data.style
							style.setAttribute("href", "../../static/styles/" + currentStyle + ".css")
						break
					default:
						console.log("unknown message event ", m.data.event, m.data)
				}
			}
		});

	  if (!document.getElementById("replication")) {
		  const j = document.getElementById("replication-jump");
		  j.parentNode.removeChild(j);
		}

	  if (!document.getElementById("default")) {
		  const j = document.getElementById("default-jump");
		  j.parentNode.removeChild(j);
		}
	})
</script>

</html>