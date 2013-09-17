package com.redhat.ceylon.eclipse.code.html;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * Helper class to get NLSed messages.
 *
 * @since 3.3
 */
class HTMLMessages {

	private static final String RESOURCE_BUNDLE= HTMLMessages.class.getName();

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private HTMLMessages() {
	}

	/**
	 * Gets a string from the resource bundle.
	 *
	 * @param key the string used to get the bundle value, must not be null
	 * @return the string from the resource bundle
	 */
	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}

	/**
	 * Gets a string from the resource bundle and formats it with the given arguments.
	 *
	 * @param key the string used to get the bundle value, must not be null
	 * @param args the arguments used to format the string
	 * @return the formatted string
	 */
	public static String getFormattedString(String key, Object[] args) {
		String format= null;
		try {
			format= fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
		return MessageFormat.format(format, args);
	}

	/**
	 * Gets a string from the resource bundle and formats it with the given argument.
	 *
	 * @param key the string used to get the bundle value, must not be null
	 * @param arg the argument used to format the string
	 * @return the formatted string
	 */
	public static String getFormattedString(String key, Object arg) {
		String format= null;
		try {
			format= fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
		if (arg == null)
			arg= ""; //$NON-NLS-1$
		return MessageFormat.format(format, new Object[] { arg });
	}
}