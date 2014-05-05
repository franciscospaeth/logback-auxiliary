package guru.spaeth.logback;

/**
 * Class to represents a logback context.
 * 
 * @author Francisco Spaeth
 *
 */
public class ContextEntry implements Comparable<ContextEntry> {

	private final String name;
	private final String objectName;

	public ContextEntry(String objectName, String name) {
		super();
		this.name = name;
		this.objectName = objectName;
	}

	public String getName() {
		return name;
	}

	public String getObjectName() {
		return objectName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((objectName == null) ? 0 : objectName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContextEntry other = (ContextEntry) obj;
		if (objectName == null) {
			if (other.objectName != null)
				return false;
		} else if (!objectName.equals(other.objectName))
			return false;
		return true;
	}

	public int compareTo(ContextEntry compareTo) {
		return compareTo.getObjectName().compareTo(objectName);
	}

}
