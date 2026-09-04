package com.jobtracker.service;

import com.jobtracker.model.JobApplication;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class JobApplicationService {
    private final List<JobApplication> applications = new ArrayList<>();

    // ADD

    public void addApplication(JobApplication application){
        if(application == null){
            return;
        }
        
        applications.add(application);
    }

    // view

    public void viewApplications(){

        if(applications.isEmpty()){
            System.out.println("No applications found.");
            return;
        }

        System.out.println("\n===== ALL JOB APPLICATIONS =====");

        for(JobApplication application : applications ){
            System.out.println(application);
            System.out.println("--------------------------------");
        }
        System.out.println("Total Applications : " + applications.size());
    }
    
    // check application

    public boolean hasApplications(){
        return !applications.isEmpty();
    }


    // search 
        public JobApplication searchApplication(int id){
            for(JobApplication application : applications){
                if(application.getId() == id){
                    return application;
                }
            }
            return null;
        }

        public void searchByCompany(String companyName){
            boolean found = false;

            for(JobApplication application : applications){

                if(application.getCompanyName().equalsIgnoreCase(companyName)){
                    System.out.println("Application found : ");
                    System.out.println(application);
                    System.out.println("----------------------------------");
                    found = true;

                }
            }
            if(!found){
                System.out.println("No Application found for Company : " + companyName);
            }
        }

        //count by status

        public int countByStatus(String status){

            int count = 0;

            for(JobApplication application : applications){

                if(application.getStatus().equalsIgnoreCase(status)){
                    count++;
                }
            }

            return count;
        }

        //Count total applications

        public int countApplications(){
            return applications.size();
        }

        //update

        public boolean updateStatus(int id, String newStatus){

            for(JobApplication application : applications){

                if(application.getId() == id){
                    application.setStatus(newStatus);

                    return true;
                }
            }
            return false;
        }

        //delete

        public boolean deleteApplication(int id){
            Iterator<JobApplication> iterator = applications.iterator();

            while(iterator.hasNext()){
                JobApplication application = iterator.next();

                if(application.getId() == id){
                    iterator.remove();

                    return true;
                }
            }
            return false;
        }



    }
