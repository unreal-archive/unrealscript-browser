package net.shrimpworks.unreal.scriptbrowser.www;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import net.shrimpworks.unreal.scriptbrowser.entities.UClass;
import net.shrimpworks.unreal.scriptbrowser.entities.UPackage;
import net.shrimpworks.unreal.scriptbrowser.entities.USources;

public class ZipGenerator {

	public static Path zipSources(USources sources, Path outPath) throws IOException {
		final Path zipFile = outPath.resolve(String.format("%s.zip", sources.name.replaceAll(" ", "_")));
		if (!Files.isDirectory(zipFile.getParent())) Files.createDirectories(zipFile.getParent());

		final URI uri = URI.create(String.format("jar:file:%s", zipFile.toAbsolutePath()));

		try (FileSystem zipfs = FileSystems.newFileSystem(uri, Map.of("create", "true"))) {
			for (UPackage pkg : sources.packages.values()) {
				final Path zipPath = zipfs.getPath(String.format("%s/Classes", pkg.name));
				Files.createDirectories(zipPath);
				for (UClass clazz : pkg.classes.values()) {
					if (clazz.path == null) continue; // synthetic classes like structs

					Path dest = zipPath.resolve(String.format("%s.uc", clazz.name));
					Files.copy(clazz.path, dest, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}

		return zipFile;
	}
}
