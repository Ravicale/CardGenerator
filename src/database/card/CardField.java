package database.card;

@SuppressWarnings("rawtypes")
public class CardField implements Comparable {
	public final int data; //Value to use for table sorting.
	public final String string; //String to display in the table.

	public CardField (int data) {
		this.data = data;
		string = String.valueOf(data);
	}

	public CardField (String string, int data) {
		this.data = data;
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}

	@Override
	public int compareTo(Object o) {
		if (o instanceof CardField) {
			CardField comparedField = (CardField) o;
			return data - comparedField.data;
		}

		throw new ClassCastException("Invalid comparison!");
	}
}