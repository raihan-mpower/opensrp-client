package org.ei.opensrp.vaccinator.db;

public class ColumnAttribute {
	public enum Type {
		text, bool, date, list, map
	}

	ColumnAttribute(Type type, boolean pk, boolean index) {
		this.type = type;
		this.pk = pk;
		this.index = index;
	}

	private Type type;
	private boolean pk;
	private boolean index;

	public boolean index() {
		return index;
	}

	public boolean pk() {
		return pk;
	}

	public Type type() {
		return type;
	}
}