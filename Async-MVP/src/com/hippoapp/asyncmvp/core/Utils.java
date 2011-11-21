/*
 * Copyright (C) 2010-2011 Bnet.inc (http://bnet.su)
 *
 * This file is part of AsyncMvp.
 *
 * AsyncMvp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AsyncMvp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AsyncMvp.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.hippoapp.asyncmvp.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/** package */
class Utils {

	/** package */
	static boolean classContainsAnnotationByName(Class<?> clazz, Class<? extends Annotation> classAnnotation) {
		Annotation[] annotations = clazz.getAnnotations();
		for (Annotation annotation : annotations) {
			if (annotation.annotationType().equals(classAnnotation)) {
				return true;
			}
		}
		return false;
	}

	/** package */
	static boolean classContainsInterfaceByName(Class<?> clazz, Class<?> classInterface) {
		Class<?>[] interfaces = clazz.getInterfaces();
		for (Class interfaze : interfaces) {
			if ((interfaze).equals(classInterface)) {
				return true;
			}
		}
		return false;
	}

	/** package */
	static boolean fieldContainsAnnotationByName(Field field, Class<? extends Annotation> classAnnotation) {
		Annotation[] annotations = field.getAnnotations();
		for (Annotation annotation : annotations) {
			if (annotation.annotationType().equals(classAnnotation)) {
				return true;
			}
		}
		return false;
	}
}
