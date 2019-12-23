package altline.unistat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

import com.sun.javafx.collections.ElementObservableListDecorator;

import altline.utils.Utils;
import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A class containing information about a single bill.<br>
 * A Bill consists of:
 * <ul>
 * <li>A {@link LocalDateTime} object representing the time the Bill was issued</li>
 * <li>A "source" string that represents the place the Bill was issued</li>
 * <li>A list of {@link Bill.Entry} objects. One for each article group purchased in the Bill</li>
 * </ul>
 * A Bill object also provides helpful {@link Binding}s that calculate common values from the Bill's data
 */
public class Bill implements Serializable {

	/* *************************************************************************
	 *                                                                         *
	 * Private fields                                                          *
	 *                                                                         *
	 ************************************************************************* */

	private transient ObservableList<Entry> entries;
	private transient ObservableList<Entry> publicEntries;
	private boolean edited;

	/* *************************************************************************
	 *                                                                         *
	 * Constructors                                                            *
	 *                                                                         *
	 ************************************************************************* */

	public Bill(LocalDateTime dateTime, String source) {
		this(dateTime, source, null);
	}

	public Bill(LocalDateTime dateTime, String source, Collection<Entry> entries) {
		init();
		setDateTime(dateTime);
		setSource(source);
		if (entries != null) setEntries(entries);
	}

	transient ObservableList<Entry> entriesWrapper;
	private void init() {
		this.dateTime = new SimpleObjectProperty<LocalDateTime>(this, "dateTime");
		this.source = new SimpleStringProperty(this, "source");
		this.entries = FXCollections.observableArrayList();
		this.publicEntries = FXCollections.unmodifiableObservableList(this.entries);

		this.totalArticles = new IntegerBinding() {
			{
				bind(new ElementObservableListDecorator<>(entries, e -> new Observable[] {e.amountProperty()}));
			}
			@Override
			protected int computeValue() {
				int count = 0;
				for (Entry entry : Bill.this.entries) {
					count += entry.getAmount();
				}
				return count;
			}
		};

		this.totalCost = new FloatBinding() {
			{
				bind(new ElementObservableListDecorator<>(entries, e -> new Observable[] {e.articlePriceProperty(), e.amountProperty()}));
			}
			@Override
			protected float computeValue() {
				float sum = 0;
				for (Entry entry : Bill.this.entries) {
					sum += entry.getArticlePrice() * entry.getAmount();
				}
				return Utils.roundDecimal(sum, 2);
			}
		};

		this.totalSubsidy = new FloatBinding() {
			{
				bind(new ElementObservableListDecorator<>(entries, e -> new Observable[] {e.subsidyProperty()}));
			}
			@Override
			protected float computeValue() {
				float sum = 0;
				for (Entry entry : Bill.this.entries) {
					sum += entry.getSubsidy();
				}
				return Utils.roundDecimal(sum, 2);
			}
		};
	}

	/* *************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 ************************************************************************* */

	// --- dateTime
	/**
	 * Represents the date and time this Bill was issued
	 */
	private transient ObjectProperty<LocalDateTime> dateTime;

	public final ObjectProperty<LocalDateTime> dateTimeProperty() {
		return dateTime;
	}

	public final LocalDateTime getDateTime() {
		return dateTime.get();
	}

	public final void setDateTime(LocalDateTime value) {
		dateTime.set(value);
	}


	// --- source
	/**
	 * Represents the place where this Bill was issued
	 */
	private transient StringProperty source;

	public final StringProperty sourceProperty() {
		return source;
	}

	public final String getSource() {
		return source.get();
	}

	public final void setSource(String value) {
		source.set(value);
	}

	/* *************************************************************************
	 *                                                                         *
	 * Bindings                                                                *
	 *                                                                         *
	 ************************************************************************* */

	// --- totalArticles
	private transient IntegerBinding totalArticles;

	/**
	 * @return A {@link Binding} that calculates the total number of individual articles in this Bill
	 */
	public final IntegerBinding totalArticlesBinding() {
		return totalArticles;
	}

	/**
	 * @return The total number of individual articles in this Bill
	 */
	public final int getTotalArticles() {
		return totalArticles.get();
	}

	// --- totalCost
	private transient FloatBinding totalCost;

	/**
	 * @return A {@link Binding} that calculates the total cost of this Bill
	 */
	public final FloatBinding totalCostBinding() {
		return totalCost;
	}

	/**
	 * @return The total cost of this Bill
	 */
	public final float getTotalCost() {
		return totalCost.get();
	}

	// --- totalSubsidy
	private transient FloatBinding totalSubsidy;

	/**
	 * @return A {@link Binding} that calculates the total subsidy value of this Bill
	 */
	public final FloatBinding totalSubsidyBinding() {
		return totalSubsidy;
	}

	/**
	 * @return The total subsidy value of this Bill
	 */
	public final float getTotalSubsidy() {
		return totalSubsidy.get();
	}

	/* *************************************************************************
	 *                                                                         *
	 * Public API                                                              *
	 *                                                                         *
	 ************************************************************************* */

	public boolean isEdited() {
		return edited;
	}
	
	/**
	 * Checks whether the specified bill was issued in the same place at the same time as this bill, disregarding the contents.
	 * @param other The bill to compare with
	 * @return true if the specified Bill object and this Bill have equal dateTimes and source strings, false if at least one differs 
	 */
	public boolean isSameTimePlace(Bill other) {
		return Objects.equals(getDateTime(), other.getDateTime()) && Objects.equals(getSource(), other.getSource());
	}

	/**
	 * @return an unmodifiable observable list of bill entries
	 */
	public ObservableList<Entry> getEntries() {
		return publicEntries;
	}

	/**
	 * Sets the entry list of this Bill to contain <strong><u>replicas</u></strong> of the entries in the specified
	 * collection. The added entries are newly created objects containing the same data as the entries in the provided
	 * collection.
	 * @param entries The collection of entries whose replicas are to be set as elements of this Bill's entry list
	 */
	public void setEntries(Collection<Entry> entries) {
		ArrayList<Entry> toAdd = new ArrayList<>(entries.size());
		for(Entry entry : entries) {
			toAdd.add(new Entry(entry.getArticleName(), entry.getArticlePrice(), entry.getAmount(), entry.getSubsidy()));
		}
		this.entries.setAll(toAdd);
		
		/*clearEntries();
		for (Entry entry : entries) {
			addEntry(entry.getArticleName(), entry.getArticlePrice(), entry.getAmount(), entry.getSubsidy());
		}*/
	}

	/**
	 * Adds a new {@link Entry} with the specified information to this Bill.
	 * @param articleName The name of the article represented by this Entry
	 * @param articlePrice The price of the article represented by this Entry
	 * @param amount The amount of individual articles
	 * @param subsidy The total subsidy of the Entry
	 */
	public void addEntry(String articleName, float articlePrice, int amount, float subsidy) {
		Entry entry = new Entry(articleName, articlePrice, amount, subsidy);
		entries.add(entry);
	}

	/**
	 * Removes the specified {@link Entry} from the Bill if the Entry belongs to it and is present.
	 * @param entry The Entry to remove from the Bill
	 */
	public void removeEntry(Entry entry) {
		if (entry == null || entry.getEnclosingInstance() != this) return;
		entries.remove(entry);
	}

	/**
	 * Removes all {@link Entry Entries} from the Bill.
	 */
	public void clearEntries() {
		entries.clear();
		
		/*ArrayList<Entry> toRemove = new ArrayList<Entry>(entries);
		for (Entry entry : toRemove)
			removeEntry(entry);
			*/
	}
	
	public void edit(LocalDateTime dateTime, String source, Collection<Entry> entries) {
		setDateTime(dateTime);
		setSource(source);
		if(!CollectionUtils.isEqualCollection(getEntries(), entries)) setEntries(entries);
		edited = true;
	}
	
	/* *************************************************************************
	 *                                                                         *
	 * Overridden methods                                                      *
	 *                                                                         *
	 ************************************************************************* */

	@Override
	public String toString() {
		return String.format("Bill [dateTime=%s, source=%s, entries=%s]", getDateTime(), getSource(), entries);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDateTime(), entries, getSource());
	}

	/**
	 * Two Bill objects are equal if they are issued in the same place at the same time and have all equal {@link Entry
	 * Entries}.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Bill other = (Bill) obj;
		return Objects.equals(getDateTime(), other.getDateTime()) && Objects.equals(getSource(), other.getSource())
				&& CollectionUtils.isEqualCollection(entries, other.entries);
	}

	/* *************************************************************************
	 *                                                                         *
	 * Serialization                                                           *
	 *                                                                         *
	 ************************************************************************* */
	private static final long serialVersionUID = 1L;

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();

		out.writeObject(getDateTime() == null ? LocalDateTime.MIN : getDateTime());
		out.writeUTF(getSource() == null ? "" : getSource());
		out.writeInt(entries.size());
		for (Entry entry : entries)
			out.writeObject(entry);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		LocalDateTime dateTime = (LocalDateTime) in.readObject();
		String source = in.readUTF();
		int entryCount = in.readInt();

		ArrayList<Entry> entries = new ArrayList<Entry>(entryCount);
		for (int i = 0; i < entryCount; i++) {
			Entry entry = (Entry) in.readObject();
			entries.add(entry);
		}

		init();
		setDateTime(dateTime);
		setSource(source);
		setEntries(entries);
	}


	/* *************************************************************************
	 *                                                                         *
	 * Inner classes                                                           *
	 *                                                                         *
	 ************************************************************************* */

	public class Entry implements Serializable {

		/* *************************************************************************
		 *                                                                         *
		 * Constructors                                                            *
		 *                                                                         *
		 ************************************************************************* */

		public Entry(String articleName, float articlePrice, int amount, float subsidy) {
			init(articleName, articlePrice, amount, subsidy);
		}

		private void init(String articleName, float articlePrice, int amount, float subsidy) {
			this.articleName = new SimpleStringProperty(this, "articleName", articleName);
			this.articlePrice = new SimpleFloatProperty(this, "articlePrice", articlePrice);
			this.amount = new SimpleIntegerProperty(this, "amount", amount);
			this.subsidy = new SimpleFloatProperty(this, "subsidy", subsidy);
		}

		/* *************************************************************************
		 *                                                                         *
		 * Properties                                                              *
		 *                                                                         *
		 ************************************************************************* */

		// --- articleName
		/**
		 * Name of the article of this Entry
		 */
		private transient StringProperty articleName;

		public final StringProperty articleNameProperty() {
			return articleName;
		}

		public final String getArticleName() {
			return articleName.get();
		}

		public final void setArticleName(String value) {
			articleName.set(value);
		}


		// --- articlePrice
		/**
		 * Price of a single article of this Entry
		 */
		private transient FloatProperty articlePrice;

		public final FloatProperty articlePriceProperty() {
			return articlePrice;
		}

		public final float getArticlePrice() {
			return articlePrice.get();
		}

		public final void setArticlePrice(float value) {
			articlePrice.set(value);
		}


		// --- amount
		/**
		 * The amount of individual articles represented by this Entry
		 */
		private transient IntegerProperty amount;

		public final IntegerProperty amountProperty() {
			return amount;
		}

		public final int getAmount() {
			return amount.get();
		}

		public final void setAmount(int value) {
			amount.set(value);
		}


		// --- subsidy
		/**
		 * The total subsidy value of the Entry (this is not per individual article)
		 */
		private transient FloatProperty subsidy;

		public final FloatProperty subsidyProperty() {
			return subsidy;
		}

		public final float getSubsidy() {
			return subsidy.get();
		}

		public final void setSubsidy(float value) {
			subsidy.set(value);
		}


		/* *************************************************************************
		 *                                                                         *
		 * Public API                                                              *
		 *                                                                         *
		 ************************************************************************* */

		public Bill getEnclosingInstance() {
			return Bill.this;
		}

		/* *************************************************************************
		 *                                                                         *
		 * Overridden methods                                                      *
		 *                                                                         *
		 ************************************************************************* */

		@Override
		public String toString() {
			return String.format("Bill.Entry [articleName=%s, articlePrice=%s, amount=%s, subsidy=%s]", getArticleName(), getArticlePrice(), getAmount(),
					getSubsidy());
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(getAmount(), getArticleName(), getArticlePrice(), getSubsidy());
			return result;
		}

		/**
		 * Two Entry objects are equal if they represent a purchase of an equal amount of articles with equal names,
		 * prices and subsidies, regardless of the enclosing Bill instance.
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Entry other = (Entry) obj;
			return Objects.equals(getAmount(), other.getAmount()) && Objects.equals(getArticleName(), other.getArticleName())
					&& Objects.equals(getArticlePrice(), other.getArticlePrice())
					&& Objects.equals(getSubsidy(), other.getSubsidy());
		}

		/* *************************************************************************
		 *                                                                         *
		 * Serialization                                                           *
		 *                                                                         *
		 ************************************************************************* */
		private static final long serialVersionUID = 1L;

		private void writeObject(ObjectOutputStream out) throws IOException {
			out.defaultWriteObject();
			out.writeUTF(getArticleName() == null ? "" : getArticleName());
			out.writeFloat(getArticlePrice());
			out.writeInt(getAmount());
			out.writeFloat(getSubsidy());
		}

		private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
			in.defaultReadObject();
			init(in.readUTF(), in.readFloat(), in.readInt(), in.readFloat());
		}

	}

}
