package gan.core.utils;

public class TextUtils {

	public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

	public static boolean equals(CharSequence a, CharSequence b) {
		if (a == b) return true;
		int length;
		if (a != null && b != null && (length = a.length()) == b.length()) {
			if (a instanceof String && b instanceof String) {
				return a.equals(b);
			} else {
				for (int i = 0; i < length; i++) {
					if (a.charAt(i) != b.charAt(i)) return false;
				}
				return true;
			}
		}
		return false;
	}

}
