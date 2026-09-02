package com.jobtracker.service;

import com.jobtracker.model.JobApplication;
import java.util.ArrayList;
import java.util.Iterator;


public class JobApplicationService {
    private ArrayList<JobApplication> applications = new ArrayList<>();

    // ADD

    public void addApplication(JobApplication application){
        applications.add(application);
    }

    // view

    public void viewApplications(){
        for(JobApplication application : applications ){
            System.out.println(application);
            System.out.println("--------------------------------");
        }
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

        public boolean updateStatus(int id, String newStatus){
            for(JobApplication application : applications){
                if(application.getId() == id){
                    application.setStatus(newStatus);
                    return true;
                }
            }
            return false;
        }

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
