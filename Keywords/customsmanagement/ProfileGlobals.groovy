package customsmanagement

import com.kms.katalon.core.configuration.RunConfiguration

import groovy.xml.XmlSlurper

import internal.GlobalVariable

/**
 * Resolves profile-backed values without throwing when {@code internal.GlobalVariable} is out of date
 * compared to {@code Profiles/*.glbl} (common after pulling Git changes before Katalon regenerates the class).
 *
 * <p>Resolution order:</p>
 * <ol>
 *   <li>If {@code GlobalVariable} declares the field, use its runtime value (Katalon profile overrides).</li>
 *   <li>Else read {@code Profiles/&lt;activeProfile&gt;.glbl} from disk (matches repo / saved profile file).</li>
 *   <li>Else return {@code defaultValue}.</li>
 * </ol>
 */
final class ProfileGlobals {

	private static final Object FIELD_MISSING = new Object()

	private static volatile Map<String, String> glblCache
	private static volatile long glblCacheMtime = -1L
	private static volatile String glblCachePath = ''

	private ProfileGlobals() {
	}

	/**
	 * Resolves a profile variable: non-empty {@code GlobalVariable} wins; otherwise non-empty value from the active {@code .glbl};
	 * otherwise empty string if the field exists on {@code GlobalVariable}, else {@code defaultValue}.
	 */
	static String get(String fieldName, String defaultValue = '') {
		Object rawReflection = readReflectionRaw(fieldName)
		String fromReflection = ''
		boolean fieldPresent = rawReflection != FIELD_MISSING
		if (fieldPresent) {
			fromReflection = rawReflection != null ? rawReflection.toString().trim() : ''
			if (fromReflection) {
				return fromReflection
			}
		}
		Map<String, String> glbl = parseActiveProfileGlbl()
		if (glbl.containsKey(fieldName)) {
			String v = glbl.get(fieldName)
			String fromGlbl = v != null ? v.trim() : ''
			if (fromGlbl) {
				return fromGlbl
			}
		}
		if (fieldPresent) {
			return fromReflection
		}
		return defaultValue
	}

	private static Object readReflectionRaw(String fieldName) {
		try {
			def f = GlobalVariable.class.getDeclaredField(fieldName)
			f.setAccessible(true)
			return f.get(null)
		} catch (NoSuchFieldException e) {
			return FIELD_MISSING
		} catch (Exception e) {
			return FIELD_MISSING
		}
	}

	private static String resolveGlblFilePath() {
		String dir = ''
		String profile = 'default'
		try {
			dir = RunConfiguration.getProjectDir()
			profile = RunConfiguration.getExecutionProfile() ?: 'default'
		} catch (Exception ignored) {
		}
		if (!dir) {
			dir = System.getProperty('user.dir') ?: ''
		}
		if (!dir) {
			return ''
		}
		File glbl = new File(dir, "Profiles${File.separator}${profile}.glbl")
		return glbl.isFile() ? glbl.absolutePath : ''
	}

	private static Map<String, String> parseActiveProfileGlbl() {
		String path = resolveGlblFilePath()
		if (!path) {
			return [:]
		}
		File f = new File(path)
		try {
			if (glblCache != null && path == glblCachePath && f.lastModified() == glblCacheMtime) {
				return glblCache
			}
			def root = new XmlSlurper().parse(f)
			Map<String, String> map = new LinkedHashMap<>()
			root.GlobalVariableEntity.each { entity ->
				// Child element <name> — avoid .name (conflicts with GPath metadata)
				String n = entity.'name'?.text()?.trim()
				if (!n) {
					return
				}
				String rawInit = entity.'initValue'?.text() ?: ''
				map.put(n, stripOuterSingleQuotes(rawInit.trim()))
			}
			glblCache = map
			glblCacheMtime = f.lastModified()
			glblCachePath = path
			return glblCache
		} catch (Exception ignored) {
			return [:]
		}
	}

	private static String stripOuterSingleQuotes(String t) {
		if (t.length() >= 2 && t.startsWith("'") && t.endsWith("'")) {
			return t.substring(1, t.length() - 1)
		}
		return t
	}
}
