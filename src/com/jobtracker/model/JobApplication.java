package com.jobtracker.model;

public class JobApplication {
    private final int id;
    private String companyName;
    private String jobRole;
    private double salary;
    private String location;
    private String status;
    private String appliedDate;

// Constructor

public JobApplication(int id, String companyName, String jobRole, double salary, String location, String status, String appliedDate){

    this.id = id;
    this.companyName = companyName;
    this.jobRole = jobRole;
    this.salary = salary;
    this.location = location;
    this.status = status;
    this.appliedDate = appliedDate;
}


// Getters and Setters 


public int getId(){
    return id;
}

public void setCompanyName(String companyName){
    this.companyName = companyName;
}

public String getCompanyName(){
    return companyName;
}

public void setJobRole(String jobRole){
    this.jobRole = jobRole;
}

public String getJobRole(){
    return jobRole;
}

public void setSalary(double salary){
    this.salary = salary;
}

public double getSalary(){
    return salary;
}

public void setLocation(String location){
    this.location = location;
}

public String getLocation(){
    return location;
}

public void setStatus(String status){
    this.status = status;
}

public String getStatus(){
    return status;
}

public void setAppliedDate(String appliedDate){
    this.appliedDate = appliedDate;
}

public String getAppliedDate(){
    return appliedDate;
}

@Override

public String toString(){

    return "ID : " + id +
    "\nCompany : " + companyName +
    "\nRole : " + jobRole +
    "\nSalary : " + salary +
    "\nLocation : " + location +
    "\nStatus : " + status +
    "\nApplied Date : " + appliedDate;

}

}






