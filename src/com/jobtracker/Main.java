package com.jobtracker; 

import com.jobtracker.model.JobApplication;
import com.jobtracker.service.JobApplicationService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);


    JobApplicationService service = new JobApplicationService();

    int nextId = 1;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    while (true) {

        System.out.println("\n===== JOB APPLICATION TRACKER =====");
        System.out.println("1. Add Application");
        System.out.println("2. View Applications");
        System.out.println("3. Search Application");
        System.out.println("4. Update Status");
        System.out.println("5. Delete Application");
        System.out.println("6. Exit");

        System.out.print("Enter your choice: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        if(choice < 1 || choice > 6 ){
            System.out.println("Invalid choice. please enter a number between 1 to 6");
            continue;
        }

            if (choice == 1) {    //ADD

            //companyName

            String companyName;
        while (true){
        System.out.print("Enter company name: ");
        companyName = scanner.nextLine();

        if(companyName.trim().isEmpty()){
            System.out.println("Company name can't be empty.");
        }else if(!companyName.matches(".*[a-zA-Z].*")){
            System.out.println("Company name must contain at least one letter.");
        }else{
            break;
        }
    }
        
        //JobRole
    
        String jobRole;
        
        while(true){

        System.out.print("Enter job role: ");
        jobRole = scanner.nextLine();

        if(jobRole.trim().isEmpty()){
            System.out.println("Job role can't be empty");
        }else if(!jobRole.matches(".*[a-zA-Z].*")){
            System.out.println("Job role contain at least one letter.");
        }else{
            break;
        }
    }

        //Salary

        double salary;

        while (true) { 
            System.out.println("Enter salary (LPA) : ");
            String salaryInput = scanner.nextLine();

            try {
                salary = Double.parseDouble(salaryInput);

                if(salary <= 0){
                    System.out.println("Salary must be greater than 0.");
                }else{
                    break;
                }

            }catch(NumberFormatException e){
                System.out.print("Please enter a valid salary.");
            }
        }
        
        //location


        String location;

        while (true) {
            
            System.out.print("Enter location : ");
            location = scanner.nextLine();

            if(location.trim().isEmpty()){
                System.out.println("Location can't be empty");
            }else if(!location.matches("[a-zA-Z]+")){
                System.out.println("Location must contain only letters.");
            }else{
                break;
            }
            
        }
        
        //Status 

        String status;

        while (true) { 
            System.out.println("enter status : ");
            status = scanner.nextLine().trim();

            if( status.equalsIgnoreCase("Applied") ||
                status.equalsIgnoreCase("Interview") ||
                status.equalsIgnoreCase("Selected") ||
                status.equalsIgnoreCase("Rejected") ||
                status.equalsIgnoreCase("Withdrawn")){
                    
                break;

                }else{
                    System.out.println("Invalid status : ");
                    System.out.println("Valid statuses : Applied, Interview, Selected, Rejected, Withdrawn");
                }
            }

        // Date 

        String appliedDate;

        while (true) { 
            System.out.println("Enter applied date (DD-MM-YYYY) : ");
            appliedDate = scanner.nextLine().trim();

            try {
                LocalDate.parse(appliedDate, formatter);
                break;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Please use DD-MM-YYYY.");
            }
        }

        JobApplication application = new JobApplication(
            nextId,
            companyName,
            jobRole,
            salary,
            location,
            status,
            appliedDate
        );

        service.addApplication(application);
        nextId++;

        System.out.println("Application added Successfully");
    }
        
        if(choice == 2){   //View
            service.viewApplications();
        }

        if(choice == 3){   // Search
            System.out.println("\n===== SEARCH / FILTER APPLICATIONS =====");
            System.out.println("1. Search by ID");
            System.out.println("2. Search by Company Name");
            System.out.println("3. Count Applications by Status");
            System.out.println("4. Back");

            System.out.print("Enter your choice: ");
            int searchChoice = scanner.nextInt();
            scanner.nextLine();

        if(searchChoice == 1){

            int id;
            
            while(true){
                System.out.print("Enter Application ID : ");
                String idInput = scanner.nextLine().trim();

                try {
                    id = Integer.parseInt(idInput);

                    if(id <= 0){
                        System.out.println("Application ID must be greater than 0.");
                    }else{
                        break;
                    }
                } catch (NumberFormatException e){ 
                System.out.print("Please enter a valid Application ID.");
                }
            }

            JobApplication result = service.searchApplication(id);

            if(result != null){
                System.out.println("Application Found : ");
                System.out.println(result);
            }else{
                System.out.println("Application not found : ");
            }
        }else if(searchChoice == 2){
            System.out.print("Enter company name : " );
            String companyName = scanner.nextLine().trim();

            service.searchByCompany(companyName);
        }else if(searchChoice == 3){

        System.out.println("\n===== APPLICATION COUNT =====");
        System.out.println("Applied   : " + service.countByStatus("Applied"));
        System.out.println("Interview : " + service.countByStatus("Interview"));
        System.out.println("Selected  : " + service.countByStatus("Selected"));
        System.out.println("Rejected  : " + service.countByStatus("Rejected"));
        System.out.println("Withdrawn : " + service.countByStatus("Withdrawn"));
        System.out.println("Total     : " + service.countApplications()); 
            
    
            }else if(searchChoice == 4){
                 // Back to main menu
            } else{
            System.out.println("Invalid search choice.");
        }
    }

            if(choice == 4){

            if(!service.hasApplications()){
                System.out.println("No Application found");
            }else{

                int id;

        while (true) {
            System.out.print("Enter Application ID : ");
            String idInput = scanner.nextLine().trim();

            try {
            id = Integer.parseInt(idInput);

        if (id <= 0) {
            System.out.println("Application ID must be greater than 0.");
            } else {
            break;
        }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid Application ID.");
    }
}
            String newStatus;

            while(true){
                System.out.print("Enter new Status : ");
                newStatus = scanner.nextLine().trim();

                if(newStatus.equalsIgnoreCase("Applied")||
                    newStatus.equalsIgnoreCase("Interview")||
                newStatus.equalsIgnoreCase("Selected")||
                newStatus.equalsIgnoreCase("Rejected")||
                newStatus.equalsIgnoreCase("Withdrawn")){
                    break;
                }else{
                    System.out.println("Invalid status. ");
                    System.out.println("Valid statuses : Applied , Interview, Selected, Rejected, Withdrawn");
                }
            }
            
            boolean update = service.updateStatus(id,newStatus);

            if(update){
                System.out.println("Status update successfully.");
            }else{
                System.out.println("Application not found.");
            }
        }
    }
            

        if(choice == 5){
            int id;

            while (true) { 
                System.out.print("Enter Application ID : ");
                String idInput = scanner.nextLine().trim();

                try {
                    id = Integer.parseInt(idInput);

                    if(id <=0){
                        System.out.println("Application ID must be greater than 0.");
                    }else{
                        break;
                    }
                } catch (NumberFormatException e){ 
                        System.out.println("Please enter a valid Application ID.");
                }
            }

            System.out.println("Are you sure you want to delete this application ? (yes / no): ");
            String confirmation = scanner.nextLine().trim();

            if(confirmation.equalsIgnoreCase("yes")){

                boolean deleted = service.deleteApplication(id);

                if(deleted){
                    System.out.println("Application deleted successfully.");
                }else{
                    System.out.println("Application not found");
                }

            } else if(confirmation.equalsIgnoreCase("no")){

                    System.out.println("Deletion cancelled.");

                }else{
                    System.out.println("Please enter yes or no.");
                }
        }
        

             if (choice == 6) {   //Exit
                System.out.println("Exiting...");
                scanner.close(); 
                return;
            }
        }
    }
}

