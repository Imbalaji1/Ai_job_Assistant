package com.jobtracker;
import com.jobtracker.model.JobApplication;

public class Main {
    public static void main(String[] args){
        
        JobApplication job = new JobApplication(
            1,
            "TCS",
            "Java Developer",
            5.5,
            "Chennai",
            "Applied",
            "01-09-2026");

    System.out.println(job.getId());
    System.out.println(job.getCompanyName());
    System.out.println(job.getJobRole());
    System.out.println(job.getSalary());
    System.out.println(job.getLocation());
    System.out.println(job.getStatus());
    System.out.println(job.getAppliedDate());
    
    }
}
