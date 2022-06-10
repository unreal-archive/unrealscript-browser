<!DOCTYPE html>
<html lang="en">
<head>
	<title>${clazz.pkg.name}.${clazz.name}</title>
	<link rel="stylesheet" href="../static/style.css">
	<link rel="stylesheet" href="../static/solarized-light.css">
</head>

<body>
	<header><h1>${clazz.pkg.name} / ${clazz.name}</h1></header>
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

</html>