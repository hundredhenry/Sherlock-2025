package uk.ac.warwick.dcs.sherlock.engine;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Custom class loader that exposes URL additions for modules without using reflection.
 */
class ModuleClassLoader extends URLClassLoader {

	ModuleClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	void addModuleUrl(URL url) {
		super.addURL(url);
	}
}
