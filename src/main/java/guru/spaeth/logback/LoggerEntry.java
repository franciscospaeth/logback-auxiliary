package guru.spaeth.logback;

/**
 * Class to represent a logger entry.
 * 
 * @author Francisco Spaeth
 *
 */
public class LoggerEntry implements Comparable<LoggerEntry> {

	private final String name;
	private final String level;
	private final String effectiveLevel;

	public LoggerEntry(String name, String level, String effectiveLevel) {
		super();
		this.name = name;
		this.level = level;
		this.effectiveLevel = effectiveLevel;
	}

	public String getName() {
		return name;
	}

	public String getLevel() {
		return level;
	}

	public String getEffectiveLevel() {
		return effectiveLevel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		LoggerEntry other = (LoggerEntry) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public int compareTo(LoggerEntry compareTo) {
		return compareTo.getName().compareTo(getName());
	}

}
