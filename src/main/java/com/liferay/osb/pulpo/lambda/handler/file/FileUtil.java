/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.osb.pulpo.lambda.handler.file;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * @author Ruben Pulido
 */
public class FileUtil {

	/**
	 * Reads a file in the classpath and returns its content as string.
	 *
	 * @param fileName the file name
	 * @return the string
	 * @throws URISyntaxException the uri syntax exception
	 * @throws IOException        the io exception
	 */
	public String fileInClasspathToString(String fileName)
		throws URISyntaxException, IOException {

		Class<? extends FileUtil> aClass = getClass();

		ClassLoader classLoader = aClass.getClassLoader();

		URL fileUrl = classLoader.getResource(fileName);

		URI fileUri = fileUrl.toURI();

		Path filePath = Paths.get(fileUri);

		StringBuilder stringBuilder = new StringBuilder();

		Stream<String> lines = Files.lines(filePath);

		lines.forEach(line -> stringBuilder.append(line).append("\n"));

		lines.close();

		return stringBuilder.toString().trim();
	}


}
