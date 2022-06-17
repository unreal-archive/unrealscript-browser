package net.shrimpworks.unreal.scriptbrowser.www;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import net.shrimpworks.unreal.scriptbrowser.entities.UClass;
import net.shrimpworks.unreal.scriptbrowser.entities.UClassNode;
import net.shrimpworks.unreal.scriptbrowser.entities.USources;
import net.shrimpworks.unreal.scriptbrowser.listeners.ClassFormatterListener;

public class Generator {

	private static final Configuration TPL_CONFIG = new Configuration(Configuration.VERSION_2_3_31);

	static {
		TPL_CONFIG.setClassForTemplateLoading(Generator.class, "");
		DefaultObjectWrapper ow = new DefaultObjectWrapper(TPL_CONFIG.getIncompatibleImprovements());
		ow.setExposeFields(true);
		ow.setMethodAppearanceFineTuner((in, out) -> {
			out.setReplaceExistingProperty(false);
			out.setMethodShadowsProperty(false);
			try {
				in.getContainingClass().getField(in.getMethod().getName());
				// this did not throw a NoSuchFieldException, so we know there is a property named after the method - do not expose the method
				out.setExposeMethodAs(null);
			} catch (NoSuchFieldException e) {
				try {
					// we got a NoSuchFieldException, which means there's no property named after the method, so we can expose it
					out.setExposeAsProperty(
						new PropertyDescriptor(in.getMethod().getName(), in.getContainingClass(), in.getMethod().getName(), null)
					);
				} catch (IntrospectionException ex) {
					// pass
				}
				// pass
			}
		});
		TPL_CONFIG.setObjectWrapper(ow);
		TPL_CONFIG.setOutputEncoding(StandardCharsets.UTF_8.name());
		TPL_CONFIG.setOutputFormat(HTMLOutputFormat.INSTANCE);
	}

	public static void index(List<USources> sources, Path outPath) {
		try {
			Template tpl = TPL_CONFIG.getTemplate("index.ftl");
			try (Writer writer = Channels.newWriter(
				Files.newByteChannel(
					outPath.resolve("index.html"),
					StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
				), StandardCharsets.UTF_8)) {
				tpl.process(
					Map.of("sources", sources),
					writer
				);
			}
		} catch (IOException | TemplateException e) {
			throw new RuntimeException(e);
		}
	}

	public static void tree(Collection<UClassNode> nodes, Path outPath) {
		try {
			if (!Files.isDirectory(outPath)) Files.createDirectories(outPath);

			Template tpl = TPL_CONFIG.getTemplate("tree.ftl");
			try (Writer writer = Channels.newWriter(
				Files.newByteChannel(
					outPath.resolve("tree.html"),
					StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
				), StandardCharsets.UTF_8)) {
				tpl.process(Map.of(
					"nodes", nodes
				), writer);
			}
		} catch (IOException | TemplateException e) {
			throw new RuntimeException(e);
		}
	}

	public static void src(UClass clazz, Path outPath) {
		try {
			final Path htmlOut = outPath.resolve(clazz.pkg.name.toLowerCase());

			if (!Files.isDirectory(htmlOut)) Files.createDirectories(htmlOut);

			Template tpl = TPL_CONFIG.getTemplate("script.ftl");

			StringBuilder sb = new StringBuilder();
			AtomicInteger lineCount = new AtomicInteger(0);
			ClassFormatterListener.transformClazz(clazz)
								  .lines()
								  .forEach(l -> {
									  lineCount.incrementAndGet();
									  sb.append(l.isBlank()
													? "&nbsp;"
													: l.replaceAll("<", "&lt;").replaceAll(">", "&gt;")
													   .replaceAll("«", "<").replaceAll("»", ">"))
										.append("\n");
								  });
			try (Writer writer = Channels.newWriter(
				Files.newByteChannel(
					htmlOut.resolve(clazz.name.toLowerCase() + ".html"),
					StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
				), StandardCharsets.UTF_8)) {
				tpl.process(Map.of(
					"clazz", clazz,
					"source", sb.toString(),
					"lines", lineCount.intValue()
				), writer);
			}

		} catch (IOException | TemplateException e) {
			throw new RuntimeException(e);
		}
	}

	public static void offloadStatic(String resourceList, Path destination) throws IOException {
		try (InputStream in = Generator.class.getResourceAsStream(resourceList);
			 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {

			String line;
			while ((line = br.readLine()) != null) {
				String[] nameAndDate = line.split("\t");
				String resource = nameAndDate[0];
				long lastModified = Long.parseLong(nameAndDate[1]);
				try (InputStream res = Generator.class.getResourceAsStream(resource)) {
					Path destPath = destination.resolve(resource);
					Files.createDirectories(destPath.getParent());
					Files.copy(res, destPath, StandardCopyOption.REPLACE_EXISTING);
					Files.setLastModifiedTime(destPath, FileTime.fromMillis(lastModified));
				}
			}
		}
	}
}
