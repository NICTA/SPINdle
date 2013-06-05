/**
 * SPINdle (version 2.2.2)
 * Copyright (C) 2009-2013 NICTA Ltd.
 *
 * This file is part of SPINdle project.
 * 
 * SPINdle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SPINdle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with SPINdle.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory 
 */
package com.app.utils;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class ResourcesImageUtils {
	private static GraphicsConfiguration gc = null;
	static {
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
		} catch (Exception e) {
			gc = null;
		}
	}

	public static BufferedImage loadImage(String imageName) throws IOException {
		Class<ResourcesUtils> clazz = ResourcesUtils.class;
		InputStream is = clazz.getResourceAsStream(imageName);
		try {
			BufferedImage img = ImageIO.read(is);
			if (gc == null) {
				return img;
			} else {
				// optimize the image according to the internal data format and
				// colour model as the underlying graphics device
				int transparency = img.getTransparency();
				BufferedImage copy = gc.createCompatibleImage(img.getWidth(), img.getHeight(), transparency);

				Graphics2D graphics = copy.createGraphics();
				graphics.drawImage(img, 0, 0, null);
				graphics.dispose();
				return copy;
			}
		} catch (IOException e) {
			System.err.println(e.getMessage() + ": imageName=" + imageName);
			throw e;
		} finally {
			if (null != is) is.close();
			is = null;
		}
	}
}