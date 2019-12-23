package altline.unistat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.sun.javafx.collections.ElementObservableListDecorator;

import javafx.beans.Observable;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class User implements Serializable {
	
	/* *************************************************************************
	 *                                                                         *
	 * Private fields                                                          *
	 *                                                                         *
	 ************************************************************************* */

	private transient String userID;
	private transient ObservableList<Bill> bills;
	private transient ObservableList<Bill> publicBills;
	private transient ElementObservableListDecorator<Bill> elementObservableBills;
	
	/* *************************************************************************
	 *                                                                         *
	 * Constructors                                                            *
	 *                                                                         *
	 ************************************************************************* */

	User(String userID) {
		if(userID == null) throw new NullPointerException("userID");
		init(userID);
	}

	private void init(String userID) {
		this.userID = userID;
		this.fullName = new SimpleStringProperty(this, "fullName", "");
		this.availableFunds = new SimpleFloatProperty(this, "availableFunds");
		this.bills = FXCollections.observableArrayList();
		this.publicBills = FXCollections.unmodifiableObservableList(this.bills);
		this.elementObservableBills = new ElementObservableListDecorator<Bill>(
				getBills(),
				bill -> new Observable[] {
						bill.dateTimeProperty(),
						bill.sourceProperty(),
						new ElementObservableListDecorator<Bill.Entry>(
								bill.getEntries(),
								entry -> new Observable[] {
										entry.articleNameProperty(),
										entry.amountProperty(),
										entry.articlePriceProperty(),
										entry.subsidyProperty() }) });
	}
	
	/* *************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 ************************************************************************* */
	
	// --- availableFunds
	private transient FloatProperty availableFunds;

	public final FloatProperty availableFundsProperty() {
		return availableFunds;
	}

	public final float getAvailableFunds() {
		return availableFunds.get();
	}

	final void setAvailableFunds(float value) {
		availableFunds.set(value);
	}
	
	
	// --- fullName
	private transient StringProperty fullName;
	
	public final StringProperty fullNameProperty() {
		return fullName;
	}
	
	public final String getFullName() {
		return fullName.get();
	}
	
	final void setFullName(String value) {
		fullName.set(value);
	}
	
	/* *************************************************************************
	 *                                                                         *
	 * Public API                                                              *
	 *                                                                         *
	 ************************************************************************* */
	
	public final String getUserID() {
		return userID;
	}

	/**
	 * @return an unmodifiable observable list of this User's bills
	 */
	public ObservableList<Bill> getBills() {
		return publicBills;
	}
	
	/**
	 * @return An {@link ElementObservableListDecorator} that gets notified when the user's bill list or any bills within it get modified.
	 */
	public ElementObservableListDecorator<Bill> getElementObservableBills(){
		return elementObservableBills;
	}
	
	/* *************************************************************************
	 *                                                                         *
	 * API                                                                     *
	 *                                                                         *
	 ************************************************************************* */

	/**
	 * Adds the specified bill to this User's bill list.
	 * @param bill the bill to add
	 */
	void addBill(Bill bill) {
		if(bill == null) throw new NullPointerException("Bill can not be null");
		bills.add(bill);
	}

	/**
	 * Removes the specified bill from this User's bill list if present.
	 * @param bill the bill to remove
	 * @return true if the bill was present in the list
	 */
	boolean removeBill(Bill bill) {
		if(bill == null) return false;
		return bills.remove(bill);
	}
	
	void addBills(Collection<Bill> bills) {
		this.bills.addAll(bills);
	}
	
	void sortBills() {
		Comparator<LocalDateTime> dateComparator = Comparator.naturalOrder();
		bills.sort((o1, o2) -> {
			return dateComparator.compare(o1.getDateTime(), o2.getDateTime());
		});
	}
	
	/* *************************************************************************
	 *                                                                         *
	 * Private implementation                                                  *
	 *                                                                         *
	 ************************************************************************* */
	
	private void setBills(List<Bill> bills) {
		this.bills.setAll(bills);
	}
	

	/* *************************************************************************
	 *                                                                         *
	 * Serialization                                                           *
	 *                                                                         *
	 ************************************************************************* */
	private static final long serialVersionUID = 1L;
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeUTF(userID);
		out.writeUTF(getFullName() == null ? "" : getFullName());
		out.writeFloat(getAvailableFunds());
		out.writeInt(bills.size());
		for (Bill bill : bills)
			out.writeObject(bill);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		String userID = in.readUTF();
		String fullName = in.readUTF();
		float availableFunds = in.readFloat();
		int billCount = in.readInt();

		ArrayList<Bill> bills = new ArrayList<Bill>(billCount);
		for (int i = 0; i < billCount; i++) {
			bills.add((Bill) in.readObject());
		}

		init(userID);
		setFullName(fullName);
		setAvailableFunds(availableFunds);
		setBills(bills);
	}

}
