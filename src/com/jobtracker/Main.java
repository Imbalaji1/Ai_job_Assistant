package com.jobtracker;
import com.jobtracker.model.JobApplication;
import com.jobtracker.service.JobApplicationService;

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

        JobApplication job2 = new JobApplication(
            2,
            "IBM",
            "Software developer",
            10.5,
            "Bangaluru",
            "Applied",
            "02-09-2026"
        );

        JobApplication job3 = new JobApplication(
            3,
            "Infotech",
            "Associat Software developer",
            3.7,
            "delhi",
            "Not yet applied",
            "03-09-2026"
        );

    JobApplicationService service = new JobApplicationService();

    service.addApplication(job);
    service.addApplication(job2);
    service.addApplication(job3);

    //update 

    boolean updated = service.updateStatus(2,"Interview");

    if(updated){
        System.out.println("Status updated successfully.");
    }else{
        System.out.println("Application not found.");
    }

    //search

    JobApplication result = service.searchApplication(2);

    System.out.println("Search result :");
    System.out.println(result);

    //delete

    boolean deleted = service.deleteApplication(2);

    if(deleted){
        System.out.println("Application deleted Successfully");
    }else{
        System.out.println("Application not found");
    }

    System.out.println("Remaining application : ");
    service.viewApplications();

    }
}
