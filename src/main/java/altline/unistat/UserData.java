package altline.unistat;

import java.util.ArrayList;
import java.util.List;

public class UserData {
	
	private final String userID;
	private String fullName;
	private String institution;
	private String privilege;
	private float availableFunds;
	private List<Bill> bills;
	
	UserData(String userID){
		this.userID = userID;
		this.bills = new ArrayList<Bill>();
	}
	
	public final String getUserID() {
		return userID;
	}
	
	public String getFullName() {
		return fullName;
	}
	
	public String getInstitution() {
		return institution;
	}
	
	public String getPrivilege() {
		return privilege;
	}
	
	public float getAvailableFunds() {
		return availableFunds;
	}
	
	
	public void setInstitution(String institution) {
		this.institution = institution;
	}
	
	public void setPrivilege(String privilege) {
		this.privilege = privilege;
	}
	
	public void setAvailableFunds(float availableFunds) {
		this.availableFunds = availableFunds;
	}

	public List<Bill> getBills() {
		return bills;
	}
	
	public void setBills(List<Bill> bills) {
		this.bills = bills;
	}
	
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

}
