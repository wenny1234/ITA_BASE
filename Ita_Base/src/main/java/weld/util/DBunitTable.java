package weld.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DBunitTable {
	
	String name();

	String[] sort();

	String[] ignore();
}
