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
<article id="home">
	<h3>UnrealScript Browser</h3>
	<p>
		This tool provides an easy method of browsing the UnrealScript source code of
		various Unreal Engine games.
	</p>
	<p>
		To begin, use the <img src="static/icons/file-code.svg" alt="source"/> Select
		Source Set menu to choose a game or version of a game's sources to view.
	</p>
	<p>
		You can also download a zipped archive of the sources for offline use using the
		<img src="static/icons/file-download.svg" alt="download"/> Download Sources
		button.
	</p>
	<p>
		Finally, you can use the <img src="static/icons/palette.svg" alt="style"/> Style
		menu to change the syntax highlighting scheme to your preference.
	</p>
	<h4>The following source sets are currently available:</h4>
	<div class="contents">
		<ul>
		<#list sources as src>
			<li>${src.name}</li>
		</#list>
		</ul>
	</div>
</article>
</body>

<script>
	document.addEventListener("DOMContentLoaded", () => {
		window.addEventListener('message', (e) => {
			const port2 = e.ports[0]

			port2.onmessage = (m) => {
				switch (m.data.event) {
					case "style":
						currentStyle = m.data.style
						style.setAttribute("href", "static/styles/" + currentStyle + ".css")
						break
					default:
						console.log("unknown message event ", m.data.event, m.data)
				}
			}
		})
	})
</script>

</html>