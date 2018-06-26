package com.providersearch.alexa;

public class ProviderSearch {

	public String providerType;
	public String postalCode;
	public String radius;
	
	public String tricarePlanType;
	public String firstName;
	public String lastName;
	public String provGender;
	public String specialityType;
	public Address address;
	
	
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}
	public String getProviderType() {
		return providerType;
	}
	public void setProviderType(String providerType) {
		this.providerType = providerType;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	
}
