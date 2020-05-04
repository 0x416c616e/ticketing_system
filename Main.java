//Alan's final database project
//I could have coded this better, but I had to rush some of it
//due to time limitations

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.sql.*;

class Main{

   //scroll down to the main method to see the program entry point
   
   //escaping quotes and double quotes for SQL stuff
   public static String escapeString(String strToEscape) {
      strToEscape = strToEscape.replaceAll("'", "&quot;");
      strToEscape = strToEscape.replaceAll("\"", "&quot;&quot;");
      return strToEscape;
   }
   
   //unescapes escaped strings
   //not perfect but good enough
   public static String unescapeString(String strToUnescape) {
      
      if ((strToUnescape != null) && (strToUnescape.length() != 0)) {
         String unescapedString;
         unescapedString = strToUnescape.replaceAll("&quot;", "\'");
         return unescapedString;
      } else {
         //some fields are not mandatory (lacking NOT NULL), meaning they can be blank
         //but this method threw exceptions with null fields
         //until I added this
         //example: viewing a list of technicians, some of whom do not have specialties
         //specialty is an OPTIONAL string in the Technician table
         return "None";
      }

   }
   
   //ticket creation method
   public static void createTicket(Scanner in, Statement stmt, Technician tech) {
      boolean isAdmin = tech.getIsAdmin();
      System.out.println("New Ticket Creation");

      //if there is an issue with how the ticket is being made,
      //such as unacceptable input in the fields,
      //the ticket creation will be cancelled and 
      //this boolean will be true and then the logged in technician
      //will go back to the main menu
      boolean problem = false;

      //getting employee ID and verifying that the input is an integer
      //I do a lot of input validation before putting anything into the database
      int empID = -1;
      System.out.print("Enter employee ID of person requesting support: ");
      String empIDString = in.nextLine();
      try {
         empID = Integer.parseInt(empIDString);
      } catch (NumberFormatException ex) {
         System.out.println("Error: empID must be a number");
         problem = true;
      }
      
      //probably should have just used a loop and break;
      //instead of this boolean switch for continuing with the ticket creation
      
      //make sure employee with that employee ID actually exists
      if (!problem) {
         //employee ID 1 is reserved for a "deleted" placeholder used by tickets
         if (empID == 1) {
            System.out.println("Error: invalid employee ID.");
            problem = true;
         }
         String empIdExistsQuery = "SELECT COUNT(*) AS empIdExists FROM Employee WHERE employeeID = " + empID;
         try {
            ResultSet empIdExistsRS = stmt.executeQuery(empIdExistsQuery);
            int empIdExists = 0;
            if (empIdExistsRS.next()) {
               empIdExists = empIdExistsRS.getInt("empIdExists");
            }
            if (!(empIdExists == 1)) {
               System.out.println("Error: employee with specified ID does not exist");
               problem = true;
            }
         } catch (SQLException ex) {
            ex.printStackTrace();
         } 
      }
      
      //if employee is fired but their record still exists, they are not allowed to get support
      if (!problem) {
         //check if employee is fired, with the currentlyHired column in the Employee table
         //fired technicians cannot log in, and fired employees cannot make tickets
         String empIsNotFiredQuery = "SELECT currentlyHired FROM Employee WHERE employeeID = " + empID;
         try {
            ResultSet empIsNotFiredRS = stmt.executeQuery(empIsNotFiredQuery);
            int empHiredStatusInt = 0;
            if (empIsNotFiredRS.next()) {
               empHiredStatusInt = empIsNotFiredRS.getInt("currentlyHired");
            }
            if (empHiredStatusInt == 0) {
               System.out.println("WARNING: This employee has been fired. You are not allowed to help them with tech issues.");
               System.out.println("Ticket cannot be created.");
               problem = true;
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
      
      //get the title of the ticket
      //kind of like the title vs. body in an email
      String titleString = "not set up yet"; //placeholder value just so it's not null
      if (!problem) {
         System.out.print("Enter the title of the issue (max 50 characters): ");
         titleString = in.nextLine();
         if (titleString.length() > 50) {
            System.out.println("Error: title cannot be longer than 50 characters.");
            problem = true;
         } else if (titleString.length() == 0) {
            System.out.println("Error: title cannot be blank.");
            problem = true;
         }
      }
      
      //getting the description for the ticket
      //description is longer text to describe the in-depth details of the tech issue
      String descriptionString = "not set up yet";
      if (!problem) {
         System.out.print("Enter the description of the issue (max 1000 chars but no linebreaks): ");
         descriptionString = in.nextLine();
         if (descriptionString.length() > 1000) {
            System.out.println("Error: description cannot be longer than 1000 characters.");
            problem = true;
         } else if (descriptionString.length() == 0) {
            System.out.println("Error: description cannot be blank.");
            problem = true;
         }
      }
      
      //difficulty number can be used for sorting by difficulty in ascending order
      //can be useful for novice IT interns at a company who only want to look at
      //easy tickets
      //technicians can have experience levels of 1, 2, or 3 (novice, intermediate, expert)
      //tickets can have difficulty levels of 1, 2, or 3 (easy, medium, hard)
      int difficulty = -1;
      if (!problem) {
         System.out.print("Enter estimated difficulty number (1 = easy, 2 = medium, 3 = hard): ");
         String difficultyString = in.nextLine();
         switch (difficultyString) {
            case "1":
               //easy
               difficulty = 1;
               break;
            case "2":
               //medium
               difficulty = 2;
               break;
            case "3":
               //medium
               difficulty = 3;
               break;
            default:
               System.out.println("Error: invalid choice. Difficulty must be 1, 2, or 3.");
               problem = true;
         }
      }
      int openStatus = -1; //I use -1 when I plan on changing it later, and -1 means it hasn't been set up correctly yet
      int assignedID = -1; //nobody can have an ID of -1, this means unassigned
      if (!problem) {
         openStatus = 1; //an unfinished ticket is an open ticket, closed means done
         //1 = true, 0 = false for the mysql tinyint(1)
         System.out.println("Leave the ticket unassigned for now?");
         System.out.println("1. Leave ticket unassigned, figure out who is suitable for it later.");
         System.out.println("2. Assign the ticket.");
         System.out.print("Choice: ");
         String assignmentChoice = in.nextLine();
         if (assignmentChoice.equals("2")) {
            //only admins can assign work to other employees
            //a technician can only assign either nobody or themselves
            
            //delete this next line, only using it for dev purposes
            //isAdmin = true;
            //delete or comment out the above line!!!
            
            if (isAdmin) {
               System.out.println("Do you want to assign yourself to the ticket or someone else?");
               System.out.println("1. Myself");
               System.out.println("2. Some other technician");
               System.out.print("Choice: ");  
               String adminAssignChoice = in.nextLine();
               
               if (adminAssignChoice.equals("1")) {
                  System.out.println("You have been assigned to this ticket.");
                  assignedID = tech.getTechnicianID();
               } else if (adminAssignChoice.equals("2")) {
                  System.out.print("Enter the Technician ID of the technician to assign the ticket to: ");
                  //checking if the technicianID is an integer
                  int adminTechIdChoice = -1;
                  try {
                     adminTechIdChoice = Integer.parseInt(in.nextLine());
                  } catch (NumberFormatException e) {
                     System.out.println("Error: technician ID must be a number.");
                     problem = true;
                  }
                  //tech ID 1 is reserved for a "deleted" placeholder
                  if (adminTechIdChoice == 1) {
                     System.out.println("Error: invalid technician ID.");
                     problem = true;
                  }
                  //check if there is a technician with that ID number
                  String verifyTechIdQuery = "SELECT COUNT(technicianID) as adminTechIdVerify FROM Technician WHERE technicianID = " + adminTechIdChoice;
                  try {
                     ResultSet verifyTechIdRS = stmt.executeQuery(verifyTechIdQuery);
                     int verifyTechIdResults = -1;
                     if (verifyTechIdRS.next()) {
                        verifyTechIdResults = verifyTechIdRS.getInt("adminTechIdVerify");
                        if (verifyTechIdResults == 1) {
                           //there is a technician with that ID
                           assignedID = verifyTechIdResults;
                        } else {
                           System.out.println("Error: there is no technician with that ID number.");
                           problem = true;
                        }
                     } else {
                        System.out.println("Error with SQL query.");
                        problem = true;
                     }
                  
                  } catch (SQLException ex) {
                     ex.printStackTrace();
                  }
                  
               } else {
                  System.out.println("Error: invalid choice.");
                  problem = true;
               }
            } else {
               //not admin, can only assign tickets to themselves
               System.out.println("You have been assigned to this ticket.");
               //assigning yourself to the ticket
               assignedID = tech.getTechnicianID();
            }
         } else if (assignmentChoice.equals("1")) {
            System.out.println("The ticket is unassigned for now. Someone can take it later.");
         } else {
            System.out.println("Error: invalid choice.");
            problem = true;
         }
      }
      
      //assigning a standard procedure to it
      int standardProcedureChoice = -1;
      if (!problem) {
         System.out.print("Would you like to assign a standard procedure to this ticket? y/n: ");
         String procedureChoice = in.nextLine();
         if (procedureChoice.equals("y") || procedureChoice.equals("yes")) {
            //user wants to assign a procedure, but it's useful to see the procedure names and numbers
            //instead of memorizing them
            System.out.println("Enter a procedure # to assign or type c to cancel adding one:");
            System.out.println("Entering c will only cancel adding a procedure, not cancel making the ticket.");
            String viewProceduresQuery = "SELECT procedureID, name FROM StandardProcedure WHERE procedureID != 1 ORDER BY procedureID ASC";
            try {
               ResultSet listOfProcedures = stmt.executeQuery(viewProceduresQuery);
               //multiple results in the result set
               while (listOfProcedures.next()) {
                  //showing list of procedure names along with their numbers
                  int procedureNumber = listOfProcedures.getInt("procedureID");
                  String procedureName = listOfProcedures.getString("name");
                  System.out.println(procedureNumber + ". " + procedureName);
               }
               
               //getting procedure choice from technician
               System.out.print("Choice (enter a number or c): ");
               String procedureNumberChoice = in.nextLine();
               //c means cancel
               if (procedureNumberChoice.equals("c")) {
                  System.out.println("No procedure will be added to this ticket.");
               } else {
                  try {
                     standardProcedureChoice = Integer.parseInt(procedureNumberChoice);
                     if (standardProcedureChoice == 1) {
                        System.out.println("Error: invalid procedure choice. No procedure will be added but the ticket will still be made.");
                     } else {
                        //making sure there is actually a procedure with that number in the procedure table
                        String verifyProcedureIdQuery = "SELECT COUNT(procedureID) AS procedureIdExists FROM StandardProcedure WHERE procedureID = " + standardProcedureChoice;
                        ResultSet procedureIdVerifyRS = stmt.executeQuery(verifyProcedureIdQuery);
                        if (procedureIdVerifyRS.next()) {
                           int procedureIdExists = procedureIdVerifyRS.getInt("procedureIdExists");
                           if (procedureIdExists == 1) {
                              //procedure has been verified and set
                              standardProcedureChoice = standardProcedureChoice;
                           } else {
                              System.out.println("Error: no such procedure. Ticket will still be made, but with no procedure associated with it.");
                           }
                        } else {
                           System.out.println("Error: SQL query issue");
                           problem = true;
                        }
                     }
                  } catch (NumberFormatException num) {
                     System.out.println("Error: invalid procedure choice.");
                     System.out.println("No procedure will be added now, but you can edit the ticket later if you want.");
                  }
               }
               
               
            } catch (SQLException e) {
               e.printStackTrace();
            }
         } else if (procedureChoice.equals("n") || procedureChoice.equals("no")) {
            System.out.println("There will be no standard procedure associated with this ticket.");
         } else {
            System.out.println("Error: invalid choice");
            problem = true;
         }
      }
      

      
      
      //creating a ticket with the values from the user
      //huge query because tickets have so many columns
      if (!problem) {
         //this code is probably vulnerable to SQL injection
         //but this is just a college project and not something that would be used in a production system
         //but in a job environment, I would write extra code that would make sure the values aren't doing SQL injection
         
         //need to escape the title and description fields
         //I tried it with an example ticket that said "computer won't boot" 
         //and the apostrophe messed it up
         
         titleString = titleString.replaceAll("'", "&quot;");
         titleString = titleString.replaceAll("\"", "&quot;&quot;");
         
         descriptionString = descriptionString.replaceAll("\'", "&quot;");
         descriptionString = descriptionString.replaceAll("\"", "&quot;&quot;");
         
         String ticketCreationQuery = "INSERT INTO Ticket (dateCreated, title, description, difficulty, openStatus,";
         //notice that there is no ticketID entered here because I have it auto increment so it will be made automatically
         ticketCreationQuery += "solutionSummary, assignedTechnicianID, assignedEmployeeID, assignedProcedureID) VALUES ";
         ticketCreationQuery += "(CURRENT_TIMESTAMP(), '" + titleString + "', '" + descriptionString + "', " + difficulty + ", ";
         //solutionSummary is NULL because a new ticket hasn't been finished and thus cannot have a solution written for it yet
         ticketCreationQuery += openStatus + ", NULL, ";
         //checking if no technician is assigned to the ticket
         if (assignedID == -1) {
            ticketCreationQuery += "NULL, ";
         } else {
            ticketCreationQuery += assignedID + ", ";
         }
         //all tickets have employees associated with them
         ticketCreationQuery += empID + ", ";
         //not all tickets have a procedure assigned to them
         if (standardProcedureChoice == -1) {
            ticketCreationQuery += "NULL)";
         } else {
            ticketCreationQuery += standardProcedureChoice + ")";
         }
         //System.out.println("ticketCreationQuery: ");
         //System.out.println(ticketCreationQuery);
         
         try {
            //finally making the ticket
            //not a SELECT so I don't need a ResultSet
            stmt.executeUpdate(ticketCreationQuery);
         } catch (SQLException s) {
            System.err.println("Error with SQL query");
            s.printStackTrace();
         }
         
         System.out.println("Ticket has been created.");
      }
   }
   //helper method for viewTickets so I don't have to be repetitive
   //used for many specific queries, but they all have some things in common
   //but with slightly different queries
   public static void generalTicketLister(String query, Statement stmt) {
      System.out.println("Priority should go to the oldest tickets because those people have waited the longest.");
      System.out.println(String.format("%-10s%-22s%-12s%-55s", "TicketID", "Date", "Difficulty", "Title"));
      boolean atLeastOneResult = false;
      try {
         //print results of each ticket -- just basic stuff, not every column
         ResultSet myOpenTicketsRS = stmt.executeQuery(query);
         
         while (myOpenTicketsRS.next()) {
            int myOpenTicketID = myOpenTicketsRS.getInt("ticketID");
            String myOpenTicketDate = myOpenTicketsRS.getString("dateCreated");
            int myOpenTicketDifficulty = myOpenTicketsRS.getInt("difficulty");
            String myOpenTicketTitle = myOpenTicketsRS.getString("title");
            //convert difficulty integers to strings
            String myOpenTicketDifficultyString = "";
            switch (myOpenTicketDifficulty) {
               case 1:
                  myOpenTicketDifficultyString = "Easy";
                  atLeastOneResult = true;
                  break;
               case 2:
                  myOpenTicketDifficultyString = "Medium";
                  atLeastOneResult = true;
                  break;
               case 3:
                  myOpenTicketDifficultyString = "Hard";
                  atLeastOneResult = true;
                  break;
               default:
                  myOpenTicketDifficultyString = "error";
                  break;
            }
            

            //unescaping the escaped stuff from the database
            myOpenTicketTitle = myOpenTicketTitle.replaceAll("&quot;", "'");
            System.out.println(String.format("%-10d%-22s%-12s%-55s", myOpenTicketID, myOpenTicketDate, myOpenTicketDifficultyString, myOpenTicketTitle));
            
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
      if (atLeastOneResult == false) {
         System.out.println("No results.");
      }
   }
   
   //ticket viewing
   public static void viewTickets(Scanner in, Statement stmt, Technician tech) {
      System.out.print("Choice: ");
      String viewTicketMenuChoice = in.nextLine();
      
      switch (viewTicketMenuChoice) {
         case "1":
            //list all tickets you're assigned to
            //just basic info, not the description and other more detailed info
            //to see detailed info, use option 6
            //either basic info about a list of tickets, or detailed info about just one ticket
            //too much stuff per ticket to show it all at once in a list
            System.out.println("Your open tickets:");
            String myOpenTicketsQuery = "SELECT ticketID, dateCreated, difficulty, title FROM AllOpenTickets WHERE assignedTechnicianID = " + tech.getTechnicianID();
            generalTicketLister(myOpenTicketsQuery, stmt);
            break;
         case "2":
            System.out.println("All tickets you are assigned to:");
            String allMyTicketsQuery = "SELECT ticketID, dateCreated, difficulty, title FROM AllTickets WHERE assignedTechnicianID = " + tech.getTechnicianID();
            generalTicketLister(allMyTicketsQuery, stmt);
            break;
         case "3":
            System.out.println("All unassigned open tickets:");
            String allUnassignedOpenTicketsQuery = "SELECT ticketID, dateCreated, difficulty, title FROM AllOpenTickets WHERE assignedTechnicianID IS NULL";
            generalTicketLister(allUnassignedOpenTicketsQuery, stmt);
            break;
         case "4":
            System.out.println("All open tickets (including yours, unassigned, and ones assigned to other people):");
            String allOpenTicketsQuery = "SELECT ticketID, dateCreated, difficulty, title FROM AllOpenTickets";
            generalTicketLister(allOpenTicketsQuery, stmt);
            break;
         case "5":
            System.out.println("All tickets, from everyone, from all time, including closed ones:");
            String allTicketsQuery = "SELECT ticketID, dateCreated, difficulty, title FROM AllTickets";
            generalTicketLister(allTicketsQuery, stmt);
            break;
         case "6":
            //view detailed info about a single ticket
            //the other options are lists with only basic info about them
            System.out.println("Read detailed info about a specific ticket by ID");
            System.out.println("If you don't know the ID of the ticket you want to look up, use the other menu options first.");
            System.out.print("Enter ticketID number: ");
            int ticketChoice = -1;
            boolean searchIntProblem = false;
            try {
               ticketChoice = Integer.parseInt(in.nextLine());
            } catch (NumberFormatException e) {
               System.out.println("Error: ticketID must be a number.");
               searchIntProblem = true;
            }
            //now need to make sure there is actually a ticket with that ID number
            String ticketSearchVerifyQuery = "SELECT COUNT(ticketID) AS searchedTicketExists FROM Ticket WHERE ticketID = " + ticketChoice;
            if (searchIntProblem == false) {
               try {
                  ResultSet searchedTicketExists = stmt.executeQuery(ticketSearchVerifyQuery);
                  if (searchedTicketExists.next()) {
                     int searchTicketInt = searchedTicketExists.getInt("searchedTicketExists");
                     if (searchTicketInt == 1) {
                        //printing detailed ticket info
                        System.out.println("TicketID: " + ticketChoice);
                        String detailedTicketInfoQuery = "SELECT ticketID, dateCreated, title, description, difficulty, ";
                        detailedTicketInfoQuery += "openStatus, solutionSummary, assignedEmployeeID, assignedTechnicianID, assignedProcedureID FROM Ticket WHERE ticketID = " + ticketChoice;
                        try {
                           

                           ResultSet detailedTicketInfoRS = stmt.executeQuery(detailedTicketInfoQuery);
                           if (detailedTicketInfoRS.next()) {
                           
                              String detailedDate = detailedTicketInfoRS.getString("dateCreated");
                              System.out.println("Date created: " + detailedDate);
                              
                              //joining Employee and Ticket tables to get the name of the employee associated with the ticket
                              String getEmployeeNameQuery = "SELECT e.firstName, e.lastName, e.employeeID, e.phoneNumber, e.emailAddress, e.officeLocation FROM Employee e, Ticket t WHERE e.employeeID = t.assignedEmployeeID AND t.ticketID = " + ticketChoice;
                              ResultSet getEmployeeNameRS = stmt.executeQuery(getEmployeeNameQuery);
                              String detailedTicketEmployee = "";
                              if (getEmployeeNameRS.next()) {
                                 detailedTicketEmployee = getEmployeeNameRS.getString("e.firstName") + " ";
                                 detailedTicketEmployee += getEmployeeNameRS.getString("e.lastName") + "\nEmployeeID ";
                                 detailedTicketEmployee += getEmployeeNameRS.getInt("e.employeeID") + ", ";
                                 detailedTicketEmployee += getEmployeeNameRS.getString("e.phoneNumber") + ", ";
                                 detailedTicketEmployee += getEmployeeNameRS.getString("e.emailAddress") + ", Room #";
                                 detailedTicketEmployee += getEmployeeNameRS.getInt("e.officeLocation");
                                 
                              }
                              detailedTicketEmployee = detailedTicketEmployee.replaceAll("&quot;", "'");
                              System.out.println("Requested by: " + detailedTicketEmployee);
                              
                              
                              
                              ResultSet detailedTicketInfoRS2 = stmt.executeQuery(detailedTicketInfoQuery);
                              
                              if (detailedTicketInfoRS2.next()) {
                                 //I'm doing multiple queries because making a separate query for the foreign key stuff
                                 //closes the first thing and messes it up
                                 //apparently only one executeQuery thing at a time, or at least that's what it seems to me
                                 String detailedDifficulty = detailedTicketInfoRS2.getString("difficulty");
                                 System.out.println("Difficulty: " + detailedDifficulty);
                                 int detailedOpenStatus = detailedTicketInfoRS2.getInt("openStatus");
                                 String detailedOpenString = "Closed";
                                 if (detailedOpenStatus == 1) {
                                    detailedOpenString = "Open";
                                 }
   
                                 System.out.println("Status: " + detailedOpenString);
                                 //check if there is even a technician assigned to the ticket
                                 //because it could be null
                                 String checkIfTechIsAssigned = "SELECT COUNT(*) AS detailedTicketTechExists FROM Technician tech, Ticket tick ";
                                 checkIfTechIsAssigned +=  "WHERE tech.technicianID = tick.assignedTechnicianID AND tick.ticketID = " + ticketChoice;
                                 int technicianAssignedToTicket = -1;
                                 ResultSet checkIfTechExistsDetailedRS = stmt.executeQuery(checkIfTechIsAssigned);
                                 String detailedTechnicianString = "None";
                                 if (checkIfTechExistsDetailedRS.next()) {
                                    int techExistsIntDetailedTicket = checkIfTechExistsDetailedRS.getInt("detailedTicketTechExists");
                                    if (techExistsIntDetailedTicket == 1) {
                                       //there is a technician associated with this ticket
                                       String detailedTechnicianQuery = "SELECT tech.firstName, tech.lastName, tech.technicianID, tech.phoneNumber, tech.emailAddress, "; 
                                       detailedTechnicianQuery += "tech.officeLocation FROM Ticket tick, Technician tech ";
                                       detailedTechnicianQuery += "WHERE tech.technicianID = tick.assignedTechnicianID AND tick.ticketID = " + ticketChoice;
                                       ResultSet detailedTechnicianRS3 = stmt.executeQuery(detailedTechnicianQuery);
                                       if (detailedTechnicianRS3.next()) {
                                          //printing technician info
                                          detailedTechnicianString = "";
                                          detailedTechnicianString += detailedTechnicianRS3.getString("tech.firstName") + " ";
                                          detailedTechnicianString += detailedTechnicianRS3.getString("tech.lastName") + "\nTechnicianID ";
                                          detailedTechnicianString += detailedTechnicianRS3.getString("tech.technicianID") + ", ";
                                          detailedTechnicianString += detailedTechnicianRS3.getString("tech.phoneNumber") + ", ";
                                          detailedTechnicianString += detailedTechnicianRS3.getString("tech.emailAddress") + ", Room #";
                                          detailedTechnicianString += detailedTechnicianRS3.getString("tech.officeLocation");
                                       }
                                    } 
                                 }
                                 System.out.println("Technician assigned to ticket: " + detailedTechnicianString);
                                 
                                 
                                 String detailTickCheckIfSPassigned = "SELECT COUNT(*) AS detailedProcExists FROM StandardProcedure sp, Ticket t WHERE ";
                                 detailTickCheckIfSPassigned += "sp.procedureID = t.assignedProcedureID AND t.ticketID = " + ticketChoice;
                                 ResultSet detailedTicketHasProcRS = stmt.executeQuery(detailTickCheckIfSPassigned);
                                 String detailedProcedureString = "None";
                                 
                                 if (detailedTicketHasProcRS.next()) {
                                    int detailedProcInt = detailedTicketHasProcRS.getInt("detailedProcExists");
                                    if (detailedProcInt == 1) {
                                       //this ticket has a standard procedure associated with it
                                       String detailedTicketProcedureQuery = "SELECT sp.procedureID, sp.name, sp.instructions FROM Ticket t, StandardProcedure sp ";
                                       detailedTicketProcedureQuery += "WHERE sp.procedureID = t.assignedProcedureID AND t.ticketID = " + ticketChoice;
                                       ResultSet detailedProcInfoRS = stmt.executeQuery(detailedTicketProcedureQuery);
                                       if (detailedProcInfoRS.next()) {
                                          //setting the var to the procedure info
                                          detailedProcedureString = "";
                                          detailedProcedureString += detailedProcInfoRS.getString("sp.name") + " (procedureID ";
                                          detailedProcedureString += detailedProcInfoRS.getInt("sp.procedureID") + ")\n";
                                          detailedProcedureString += "Procedure instructions: " + detailedProcInfoRS.getString("sp.instructions");
                                          detailedProcedureString = detailedProcedureString.replaceAll("&quot;", "'");
                                       }
                                       
                                       
                                    }
                                 }
                                 System.out.println("Standard procedure: " + detailedProcedureString);
                                 
                                 
                                 String detailedDescription = "";
                                 String detailedTitleAndDescription = "SELECT title, description, solutionSummary FROM Ticket ";
                                 detailedTitleAndDescription += "WHERE ticketID = " + ticketChoice;
                                 ResultSet detailedTitleDescriptRS = stmt.executeQuery(detailedTitleAndDescription);
                                 if (detailedTitleDescriptRS.next()) {
                                    detailedDescription = detailedTitleDescriptRS.getString("description");
                                    detailedDescription = detailedDescription.replaceAll("&quot;", "'");
                                    String detailedTicketTitle = detailedTitleDescriptRS.getString("title");
                                    detailedTicketTitle = detailedTicketTitle.replaceAll("&quot;", "'");
                                    System.out.println("Ticket title: " + detailedTicketTitle);
                                    System.out.println("Ticket description: " + detailedDescription);
                                    String detailTicketSolutionSummary = detailedTitleDescriptRS.getString("solutionSummary");
                                    String detailedSolutionSummaryString = "None";
                                    if (detailTicketSolutionSummary != null) {
                                       detailedSolutionSummaryString = "";
                                       detailedSolutionSummaryString += detailedTitleDescriptRS.getString("solutionSummary");
                                       detailedSolutionSummaryString = detailedSolutionSummaryString.replaceAll("&quot;", "'");
                                       System.out.println("Solution summary: " + detailedSolutionSummaryString);
                                       
                                    }
                                    
                                 }

                                 /*
                                 String detailedSummary = detailedTicketInfoRS2.getString("solutionSummary");
                                 if (detailedSummary != null) {
                                    detailedSummary = detailedSummary.replaceAll("&quot;", "'");
                                 }
                                 System.out.println("Solution summary: " + detailedSummary);*/
                                 
                              
                              }
                              
                                                            
                           }
                           
                        } catch (SQLException e) {
                           e.printStackTrace();
                        }
                        
                     } else {
                        System.out.println("Error: no such ticket with that ID.");
                     }
                  }
               } catch (SQLException e) {
                  e.printStackTrace();
               }
            }
            
            
            break;
         case "7":
            //search by text (for title or description) or by difficulty level
            System.out.println("Search tickets by text or difficulty level");
            System.out.println("What do you want to search by?");
            System.out.println("1. Text");
            System.out.println("2. Difficulty level");
            System.out.print("Choice: ");
            String typeOfSearch = in.nextLine();
            switch (typeOfSearch) {
               case "1":
                  //text search for tickets
                  System.out.println("Which tickets do you want to search by text?");
                  System.out.println("1. My open tickets");
                  System.out.println("2. Unassigned open tickets");
                  System.out.println("3. All tickets");
                  System.out.print("Choice: ");
                  String typeOfTextSearch = in.nextLine();
                  switch (typeOfTextSearch) {
                     case "1":
                        System.out.print("Enter search term: ");
                        String textToSearch = in.nextLine();
                        textToSearch = textToSearch.replaceAll("'", "&quot;");
                        textToSearch = textToSearch.replaceAll("\"", "&quot;&quot;");
                        String searchMyOpenTicketsTextQuery = "SELECT ticketID, dateCreated, title, difficulty FROM TicketTextSearch WHERE assignedTechnicianID = ";
                        searchMyOpenTicketsTextQuery += tech.getTechnicianID() + " AND openStatus = 1 AND (title LIKE '%" + textToSearch;
                        searchMyOpenTicketsTextQuery += "%' OR description LIKE '%" + textToSearch + "%')";
                        generalTicketLister(searchMyOpenTicketsTextQuery, stmt);
                        break;
                     case "2":
                        System.out.print("Enter search term: ");
                        String textToSearch2 = in.nextLine();
                        //I wrote this repetitive manual escaping/unescaping stuff
                        //before I made the escapeString() and unescapeString() methods
                        //but I only used the new method on new parts of the program
                        //because I don't feel like refactoring everything
                        textToSearch2 = textToSearch2.replaceAll("'", "&quot;");
                        textToSearch2 = textToSearch2.replaceAll("\"", "&quot;&quot;");
                        String searchUnassignedOpenTicketsTextQuery = "SELECT ticketID, dateCreated, title, difficulty FROM TicketTextSearch WHERE assignedTechnicianID IS ";
                        searchUnassignedOpenTicketsTextQuery += "NULL AND openStatus = 1 AND (title LIKE '%" + textToSearch2;
                        searchUnassignedOpenTicketsTextQuery += "%' OR description LIKE '%" + textToSearch2 + "%')";
                        generalTicketLister(searchUnassignedOpenTicketsTextQuery, stmt);
                        break;
                     case "3":
                        System.out.print("Enter search term: ");
                        String textToSearch3 = in.nextLine();
                        textToSearch3 = textToSearch3.replaceAll("'", "&quot;");
                        textToSearch3 = textToSearch3.replaceAll("\"", "&quot;&quot;");
                        String searchAllTicketsTextQuery = "SELECT ticketID, dateCreated, title, difficulty FROM TicketTextSearch WHERE ";
                        searchAllTicketsTextQuery += "title LIKE '%" + textToSearch3;
                        searchAllTicketsTextQuery += "%' OR description LIKE '%" + textToSearch3 + "%'";
                        generalTicketLister(searchAllTicketsTextQuery, stmt);
                        break;
                     default:
                        System.out.println("Invalid menu choice.");
                        break;
                  }
                  
                  
                  
                  break;
               case "2":
                  //search tickets by difficulty level
                  //sub-menu with more options
                  System.out.println("What difficulty do you want to search for?");
                  System.out.println("1. Open and unassigned tickets that are suitable for my skill level");
                  System.out.println("2. Open and unassigned tickets that are a specific difficulty level");
                  System.out.println("3. All tickets that are a certain difficulty level");
                  System.out.print("Choice: ");
                  String difficultyChoice = in.nextLine();
                  switch (difficultyChoice) {
                     case "1":
                        String openUnassignedMyDifficultyQuery = "SELECT ticketID, dateCreated, difficulty, title FROM AllOpenTickets WHERE difficulty = ";
                        openUnassignedMyDifficultyQuery += tech.getExperienceLevel() + " AND assignedTechnicianID IS NULL";
                        generalTicketLister(openUnassignedMyDifficultyQuery, stmt);
                        break;
                     case "2":
                        System.out.print("Enter difficulty level to search for (1, 2, or 3): ");
                        String specificDifficultyChoice = in.nextLine();
                           switch (specificDifficultyChoice) {
                              case "1":
                                 String searchLevel1Difficulty = "SELECT ticketID, dateCreated, difficulty, title FROM AllOpenTickets WHERE difficulty = 1";
                                 generalTicketLister(searchLevel1Difficulty, stmt);
                                 break;
                              case "2":
                                 String searchLevel2Difficulty = "SELECT ticketID, dateCreated, difficulty, title FROM AllOpenTickets WHERE difficulty = 2";
                                 generalTicketLister(searchLevel2Difficulty, stmt);
                                 break;
                              case "3":
                                 String searchLevel3Difficulty = "SELECT ticketID, dateCreated, difficulty, title FROM AllOpenTickets WHERE difficulty = 3";
                                 generalTicketLister(searchLevel3Difficulty, stmt);
                                 break;
                              default:
                                 System.out.println("Invalid menu choice.");
                                 break;
                           }
                        break;
                     case "3":
                        System.out.print("Enter difficulty level to search for (1, 2, or 3): ");
                        String allSpecificDifficultyChoice = in.nextLine();
                           switch (allSpecificDifficultyChoice) {
                              case "1":
                                 String searchLevel1Difficulty = "SELECT ticketID, dateCreated, difficulty, title FROM AllTickets WHERE difficulty = 1";
                                 generalTicketLister(searchLevel1Difficulty, stmt);
                                 break;
                              case "2":
                                 String searchLevel2Difficulty = "SELECT ticketID, dateCreated, difficulty, title FROM AllTickets WHERE difficulty = 2";
                                 generalTicketLister(searchLevel2Difficulty, stmt);
                                 break;
                              case "3":
                                 String searchLevel3Difficulty = "SELECT ticketID, dateCreated, difficulty, title FROM AllTickets WHERE difficulty = 3";
                                 generalTicketLister(searchLevel3Difficulty, stmt);
                                 break;
                              default:
                                 System.out.println("Invalid menu choice.");
                                 break;
                           }
                        break;
                     default:
                        System.out.println("Invalid menu choice.");
                        break;
                  }
                  break;
               default:
                  System.out.println("Invalid menu choice.");
                  break;
            }
            break;
         case "8":
            System.out.println("Returning to main menu.");
            break;
         case "q":
         case "Q":
         case "quit":
            System.out.println("Goodbye");
            System.exit(0);
         default:
            System.out.println("Invalid menu choice.");
            break;
         
      }
   }
   
   
   //view procedures method
   public static void viewProcedures(Scanner in, Statement stmt, Technician tech) {
      System.out.print("Choice: ");
      String procViewingMenuChoice = in.nextLine();
      switch (procViewingMenuChoice) {
         case "1":
            System.out.println("View list of procedures");
            //procedureID 1 is a special reserved "deleted" placeholder
            //a ticket can exist even if you delete the procedure, employee, or technician associated with it
            String viewAllProceduresQuery = "SELECT * FROM ListOfProcedures WHERE procedureID <> 1";
            try {
               System.out.println(String.format("%-15s%-32s", "ProcedureID", "Procedure Name"));
               ResultSet viewAllProcRS = stmt.executeQuery(viewAllProceduresQuery);
               while (viewAllProcRS.next()) {
                  int viewProcID = viewAllProcRS.getInt("procedureID");
                  String viewProcName = viewAllProcRS.getString("name");
                  viewProcName = viewProcName.replaceAll("&quot;", "'");
                  System.out.println(String.format("%-15s%-32s", viewProcID, viewProcName));
               }
            } catch (SQLException e) {
               e.printStackTrace();
            }
            break;
         case "2":
            System.out.println("View detailed procedure info for a single procedure (by ID number)");
            System.out.print("Enter procedure ID to look up: ");
            String detailedProcedureChoice = in.nextLine();
            //validate input
            try {
               int procedureToLookUp = Integer.parseInt(detailedProcedureChoice);
               //edge case "deleted" placeholder for ID 1
               if (procedureToLookUp == 1) {
                  System.out.println("Error: invalid procedure ID.");
               } else {               
                  try {
                     //now need to check if there is such a procedure in the database
                     String checkIfProcNumExists = "SELECT COUNT(*) AS procNumExists FROM ListOfProcedures WHERE ";
                     checkIfProcNumExists += "procedureID = " + procedureToLookUp;
                     ResultSet ifProcNumExistsRS = stmt.executeQuery(checkIfProcNumExists);
                     if (ifProcNumExistsRS.next()) {
                        int procNumExistsInt = ifProcNumExistsRS.getInt("procNumExists");
                        if (procNumExistsInt == 1) {
                           //procedure with that ID exists
                           //printing out procedure info
                           String detailedProcedureQuery = "SELECT * FROM StandardProcedure WHERE procedureID = " + procedureToLookUp;
                                               
                           ResultSet detailedProcedureRS = stmt.executeQuery(detailedProcedureQuery);
                           if (detailedProcedureRS.next()) {   
                              String detailedProcedureName = detailedProcedureRS.getString("name");
                              detailedProcedureName = detailedProcedureName.replaceAll("&quot;", "'");
                              String detailedProcedureInstructions = detailedProcedureRS.getString("instructions");
                              detailedProcedureInstructions = detailedProcedureInstructions.replaceAll("&quot;", "'");
                              System.out.println("Procedure Name: " + detailedProcedureName);
                              System.out.println("Procedure ID: " + procedureToLookUp);
                              System.out.println("Procedure instructions: " + detailedProcedureInstructions);
                           }
                        } else {
                           System.out.println("Error: no procedure has that ID.");
                        }
                     }
                  } catch (SQLException s) {
                     s.printStackTrace();
                  }

               
               }
            } catch (NumberFormatException e) {
               System.out.println("Error: procedure ID must be a number.");
            }
            
            break;
         case "3":
            //search for procedures with user input
            System.out.println("Search procedures by text");
            System.out.print("Enter a search term: ");
            String procSearchText = in.nextLine();
            //new escape method so I don't have to keep on repetitively doing manual escapes/unescapes
            procSearchText = escapeString(procSearchText);
            //search through titles and instructions for search term
            String procTextSearchQuery = "SELECT procedureID, name, instructions FROM StandardProcedure WHERE name LIKE '%";
            procTextSearchQuery += procSearchText + "%' OR instructions LIKE '%" + procSearchText + "%'";
            //System.out.println(procTextSearchQuery);
            System.out.println("Procedure ID, name, and instructions");
            System.out.println("Search results:");
            
            boolean atLeastOneProcTextSearchResult = false;
            try {
               ResultSet procTextSearchRS = stmt.executeQuery(procTextSearchQuery);
               //loop through all results, could be zero to many
               while (procTextSearchRS.next()) {
                  int procIdFromProcSearch = procTextSearchRS.getInt("procedureID");
                  String procIdNameSearch = unescapeString(procTextSearchRS.getString("name"));
                  String procInstructionsSearch = unescapeString(procTextSearchRS.getString("instructions"));
                  System.out.println(String.format("Procedure Name: %s, Procedure ID: %d", procIdNameSearch, procIdFromProcSearch));
                  System.out.println("Instructions: " + procInstructionsSearch);
                  System.out.println("----------");
                  atLeastOneProcTextSearchResult = true;   
            }
            if (!atLeastOneProcTextSearchResult) {
               System.out.println("No results.");
            }

            } catch (SQLException s) {
               s.printStackTrace();
            }
            
            break;
         case "4":
            System.out.println("Returning to main menu.");
            break;
         case "q":
         case "Q":
         case "quit":
            System.out.println("Goodbye.");
            System.exit(0);
         default:
            System.out.println("Invalid menu choice.");
            break;
      }
   }
   
   //edit your own technician account info
   //admins can do some things that regular technicians can't
   public static void editSelfInfo(Scanner in, Statement stmt, Technician tech) {
      System.out.println("Please note that apostrophes and quotes will be escaped, making them take up more characters.");
      int idToUpdate = -1;
      //admin gets to choose which technician they want to edit
      //reg tech can only edit their own technician account
      if (tech.getIsAdmin()) {
         System.out.print("Enter the technicianID of the technician you want to edit: ");
         try {
            idToUpdate = Integer.parseInt(in.nextLine());
         } catch (NumberFormatException e) {
            System.out.println("Error: technicianID must be a number.");
            e.printStackTrace();
            return;
         }
      } else {
         idToUpdate = tech.getTechnicianID();
      }
      System.out.print("Column to edit choice (from numeric list above): ");
      String editSelfMainChoice = in.nextLine();
      switch (editSelfMainChoice) {
         case "1":
            //I realize that these cases are repetitive, and I would have refactored stuff
            //and turned this stuff into a more generalized method
            //but I ran out of time
            System.out.print("Enter a new first name (max 15 chars): ");
            String newFirstNameChoice = in.nextLine();
            newFirstNameChoice = escapeString(newFirstNameChoice);
            if ((newFirstNameChoice.length() != 0) && (newFirstNameChoice.length() <= 15)) {
               
               String newFirstNameUpdate = "UPDATE Technician SET firstName = '" + newFirstNameChoice;
               newFirstNameUpdate += "' WHERE technicianID = " + idToUpdate;
               try {
                  stmt.executeUpdate(newFirstNameUpdate);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
            } else {
               System.out.println("Error: input length issue.");
            }
            
            break;
         case "2":
            //I realized how repetitive this is and that I should put it all in a function
            //after I wrote a lot of it
            System.out.print("Enter a new last name (max 15 chars): ");
            String newLastNameChoice = in.nextLine();
            newLastNameChoice = escapeString(newLastNameChoice);
            if ((newLastNameChoice.length() != 0) && (newLastNameChoice.length() <= 15)) {
               
               String newLastNameUpdate = "UPDATE Technician SET lastName = '" + newLastNameChoice;
               newLastNameUpdate += "' WHERE technicianID = " + idToUpdate;
              try {
                  stmt.executeUpdate(newLastNameUpdate);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
            } else {
               System.out.println("Error: input length issue.");
            }
            break;
         case "3":
            //I would have used regular expressions for pattern matching,
            //but I ran out of time
            //so I had to cut out some of the extra features
            //but for a real-world ticketing system, you'd need to verify
            //that the submitted phone number is actually a phone number
            System.out.println("Phone number format: 123-456-7890");
            System.out.print("Enter a phone number (max 12 chars): ");
            String newPhoneNumberChoice = in.nextLine();
            newPhoneNumberChoice = escapeString(newPhoneNumberChoice);
            if ((newPhoneNumberChoice.length() != 0) && (newPhoneNumberChoice.length() <= 12)) {
               
               String newPhoneNumberUpdate = "UPDATE Technician SET phoneNumber = '" + newPhoneNumberChoice;
               newPhoneNumberUpdate += "' WHERE technicianID = " + idToUpdate;
               try {
                  stmt.executeUpdate(newPhoneNumberUpdate);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
            } else {
               System.out.println("Error: input length issue.");
            }
            break;
         case "4":
            System.out.print("Enter a email address (max 30 chars): ");
            String newEmailAddressChoice = in.nextLine();
            newEmailAddressChoice = escapeString(newEmailAddressChoice);
            if ((newEmailAddressChoice.length() != 0) && (newEmailAddressChoice.length() <= 30)) {
               
               String newEmailAddressUpdate = "UPDATE Technician SET emailAddress = '" + newEmailAddressChoice;
               newEmailAddressUpdate += "' WHERE technicianID = " + idToUpdate;
               try {
                  stmt.executeUpdate(newEmailAddressUpdate);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
            } else {
               System.out.println("Error: input length issue.");
            }
            break;
         case "5":
            System.out.print("Enter a room number (integer): ");
            String newRoomNumberChoice = in.nextLine();
            if ((newRoomNumberChoice.length() != 0) && (newRoomNumberChoice.length() <= 10)) {
               try {
                  int newRoomNumberInt = Integer.parseInt(newRoomNumberChoice);
                  String newRoomNumberUpdate = "UPDATE Technician SET officeLocation = " + newRoomNumberInt;
                  newRoomNumberUpdate += " WHERE technicianID = " + idToUpdate;
                  try {
                     stmt.executeUpdate(newRoomNumberUpdate);
                  } catch (SQLException e) {
                     e.printStackTrace();
                  }
               } catch (NumberFormatException e) {
                  System.out.println("Error: room number must be an integer. ");
               }
            } else {
               System.out.println("Error: input length issue.");
            }
            break;
         case "6":
            System.out.print("Enter a specialty (max 30 chars): ");
            String newSpecialtyChoice = in.nextLine();
            newSpecialtyChoice = escapeString(newSpecialtyChoice);
            if ((newSpecialtyChoice.length() != 0) && (newSpecialtyChoice.length() <= 32)) {
               
               String newSpecialtyUpdate = "UPDATE Technician SET specialty = '" + newSpecialtyChoice;
               newSpecialtyUpdate += "' WHERE technicianID = " + idToUpdate;
               try {
                  stmt.executeUpdate(newSpecialtyUpdate);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
            } else {
               System.out.println("Error: input length issue.");
            }
            break;
         case "7":
            System.out.print("Enter a password (max 32 chars): ");
            String newPasswordChoice = in.nextLine();
            newPasswordChoice = escapeString(newPasswordChoice);
            if ((newPasswordChoice.length() != 0)) {
            
               //in a real database, you'd want to hash the passwords for security
               //and also make sure someone doesn't use a weak password or set it to an old password
               //but that would be more complicated to implement
               String newPasswordUpdate = "UPDATE Technician SET password = '" + newPasswordChoice;
               newPasswordUpdate += "' WHERE technicianID = " + idToUpdate;
               try {
                  stmt.executeUpdate(newPasswordUpdate);
                  System.out.println("Password changed. You will need to log back in with your new password.");
                  System.exit(0);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
               
            } else {
               System.out.println("Error: input length issue.");
            }
            break;
         case "8":
            System.out.println("Returning to main menu.");
            break;
         case "9":
            if (tech.getIsAdmin()) {
               System.out.print("Enter new experience level (1 = novice, 2 = medium, 3 = expert): ");
               try {
                  int experienceLevelInt = Integer.parseInt(in.nextLine());
                  String newExperienceLevel = "UPDATE Technician SET experienceLevel = " + experienceLevelInt;
                  newExperienceLevel += " WHERE technicianID = " + idToUpdate;
                  try {
                     stmt.executeUpdate(newExperienceLevel);
                  } catch (SQLException e) {
                     e.printStackTrace();
                  }
               } catch (NumberFormatException e) {
                  System.out.println("Error: must be a number.");
                  e.printStackTrace();
                  return;
               }
            } else {
               System.out.println("Error: invalid menu choice.");
            }
            
            break;
         case "10":
            if (tech.getIsAdmin()) {
               System.out.print("Enter new admin status (0 = non-admin, 1 = admin): ");
               try {
                  int adminStatusInt = Integer.parseInt(in.nextLine());
                  String newAdminStatus = "UPDATE Technician SET isAdmin = " + adminStatusInt;
                  newAdminStatus += " WHERE technicianID = " + idToUpdate;
                  try {
                     stmt.executeUpdate(newAdminStatus);
                  } catch (SQLException e) {
                     e.printStackTrace();
                  }
               } catch (NumberFormatException e) {
                  System.out.println("Error: must be a number.");
                  e.printStackTrace();
                  return;
               }
            } else {
               System.out.println("Error: invalid menu choice.");
            }
            break;
         case "11":
            if (tech.getIsAdmin()) {
               System.out.print("Enter new technicianID (must not be taken already): ");
               try {
                  int newTechIdInt = Integer.parseInt(in.nextLine());
                  String newtechID = "UPDATE Technician SET technicianID = " + newTechIdInt;
                  newtechID += " WHERE technicianID = " + idToUpdate;
                  try {
                     stmt.executeUpdate(newtechID);
                  } catch (SQLException e) {
                     e.printStackTrace();
                  }
               } catch (NumberFormatException e) {
                  System.out.println("Error: must be 0 or 1.");
                  e.printStackTrace();
                  return;
               }
            } else {
               System.out.println("Error: invalid menu choice.");
            }
            break;
         case "12":
            if (tech.getIsAdmin()) {
               System.out.print("Enter new currentlyHired status (0 = fired, 1 = hired): ");
               try {
                  int newHiredStatusInt = Integer.parseInt(in.nextLine());
                  String newHiredStatus = "UPDATE Technician SET currentlyHired = " + newHiredStatusInt;
                  newHiredStatus += " WHERE technicianID = " + idToUpdate;
                  try {
                     stmt.executeUpdate(newHiredStatus);
                  } catch (SQLException e) {
                     e.printStackTrace();
                  }
               } catch (NumberFormatException e) {
                  System.out.println("Error: must be 0 or 1.");
                  e.printStackTrace();
                  return;
               }
            } else {
               System.out.println("Error: invalid menu choice.");
            }
            break;
         case "q":
         case "Q":
         case "quit":
            System.out.println("Goodbye.");
            System.exit(0);
            break;
         default:
            System.out.println("Invalid menu choice.");
            break;
      }
      
   }
   
   public static void readTechnicians(Scanner in, Statement stmt, Technician tech) {
      System.out.print("Choice: ");
      String technicianMenuChoice = in.nextLine();
      switch (technicianMenuChoice) {
         case "1":
            String viewAllTechSortIdQuery = "SELECT technicianID, firstName, lastName, specialty, experienceLevel FROM Technician";
            //technicianID 1 is a reserved "deleted" placeholder
            viewAllTechSortIdQuery += " WHERE technicianID <> 1 ORDER BY technicianID ASC";
            try {
               ResultSet viewAllTechSortIdQueryRS = stmt.executeQuery(viewAllTechSortIdQuery);
               System.out.println(String.format("%-10s%-17s%-17s%-32s%-20s", "ID", "First name", "Last name", "Specialty", "Experience level"));
               while (viewAllTechSortIdQueryRS.next()) {
                  int technicianIdListAllById = viewAllTechSortIdQueryRS.getInt("technicianID");
                  String firstNameListAllById = viewAllTechSortIdQueryRS.getString("firstName");
                  firstNameListAllById = unescapeString(firstNameListAllById);
                  String lastNameListAllById = viewAllTechSortIdQueryRS.getString("lastName");
                  lastNameListAllById = unescapeString(lastNameListAllById);
                  String specialtyListAllById = viewAllTechSortIdQueryRS.getString("specialty");
                  specialtyListAllById = unescapeString(specialtyListAllById);
                  int experienceLevelListAllById = viewAllTechSortIdQueryRS.getInt("experienceLevel");
                  System.out.println(String.format("%-10d%-17s%-17s%-32s%-3d", technicianIdListAllById, firstNameListAllById,
                  lastNameListAllById, specialtyListAllById, experienceLevelListAllById));
                  
               }
            } catch (SQLException e) {
               e.printStackTrace();
            }
            break;
         case "2":
            System.out.print("Enter technicianID to view: ");
            String techIdToView = in.nextLine();
            //make sure it's a number
            try {
               //it's a number, but now need to check that there is such a technician
               int techIdToViewDetailed = Integer.parseInt(techIdToView);
               String checkIfTechnicianExistsQuery = "SELECT COUNT(*) AS technicianIdExists FROM Technician WHERE technicianID = " + techIdToViewDetailed;
               try {
                  //query to see if technician exists
                  ResultSet techIdExistsOrNotRS = stmt.executeQuery(checkIfTechnicianExistsQuery);
                  if (techIdExistsOrNotRS.next()) {
                     int techIdExistsOrNotInt = techIdExistsOrNotRS.getInt("technicianIdExists");
                     if (techIdExistsOrNotInt == 1) {
                        //ID has been verified
                        String detailedTechInfoQuery = "SELECT * FROM Technician WHERE technicianID = " + techIdToViewDetailed;
                        ResultSet detailedTechInfoRS = stmt.executeQuery(detailedTechInfoQuery);
                        if (detailedTechInfoRS.next()) {
                           System.out.print("Technician ID: ");
                           System.out.println(detailedTechInfoRS.getInt("technicianID"));
                           System.out.print("Name: ");
                           System.out.print(unescapeString(detailedTechInfoRS.getString("firstName")) + " ");
                           System.out.println(unescapeString(detailedTechInfoRS.getString("lastName")));
                           System.out.print("Phone #: ");
                           System.out.println(unescapeString(detailedTechInfoRS.getString("phoneNumber")));
                           System.out.print("Email: ");
                           System.out.println(unescapeString(detailedTechInfoRS.getString("emailAddress")));
                           System.out.print("Room #: ");
                           System.out.println(detailedTechInfoRS.getInt("officeLocation"));
                           System.out.print("Employment status: ");
                           int hiredOrNotTech = detailedTechInfoRS.getInt("currentlyHired");
                           if (hiredOrNotTech == 1) {
                              System.out.println("Hired");
                           } else {
                              System.out.println("Fired");
                           }
                           System.out.print("Permission status: ");
                           int adminOrNotTech = detailedTechInfoRS.getInt("isAdmin");
                           if (adminOrNotTech == 1) {
                              System.out.println("Administrator");
                           } else {
                              System.out.println("Regular technician");
                           }
                           System.out.print("Specialty: ");
                           System.out.println(unescapeString(detailedTechInfoRS.getString("specialty")));
                           System.out.print("Experience level: ");
                           int experienceLevelTech = detailedTechInfoRS.getInt("experienceLevel");
                           switch (experienceLevelTech) {
                              case 1:
                                 System.out.println("Novice");
                                 break;
                              case 2:
                                 System.out.println("Intermediate");
                                 break;
                              case 3:
                                 System.out.println("Expert");
                                 break;
                              default:
                                 System.out.println("Error");
                                 break;
                           }
                        }
                     } else {
                        System.out.println("Error: no such technician exists.");
                     }
                  }

               } catch (SQLException s) {
                  s.printStackTrace();
               }
            } catch (NumberFormatException e) {
               System.out.println("Error: technicianID must be a number.");
            }
            break;
         case "3":
            System.out.println("Returning to main menu.");
            break;
         case "q":
         case "Q":
         case "quit":
            System.out.println("Goodbye");
            System.exit(0);
            break;
         default:
            System.out.println("Invalid menu choice.");
            break;
         
      }
   }
   
   
   //read employees
   public static void readEmployees(Scanner in, Statement stmt, Technician tech) {
      System.out.print("Choice: ");
      String employeeMenuChoice = in.nextLine();
      switch (employeeMenuChoice) {
         case "1":
            String viewAllEmployeeSortIdQuery = "SELECT employeeID, firstName, lastName FROM Employee";
            //employeeID 1 is a reserved "deleted" placeholder
            viewAllEmployeeSortIdQuery += " WHERE employeeID <> 1 ORDER BY employeeID ASC";
            try {
               ResultSet viewAllEmployeeSortIdQueryRS = stmt.executeQuery(viewAllEmployeeSortIdQuery);
               System.out.println(String.format("%-10s%-17s%-17s", "ID", "First name", "Last name"));
               while (viewAllEmployeeSortIdQueryRS.next()) {
                  int employeeIdListAllById = viewAllEmployeeSortIdQueryRS.getInt("employeeID");
                  String firstNameListAllById = viewAllEmployeeSortIdQueryRS.getString("firstName");
                  firstNameListAllById = unescapeString(firstNameListAllById);
                  String lastNameListAllById = viewAllEmployeeSortIdQueryRS.getString("lastName");
                  lastNameListAllById = unescapeString(lastNameListAllById);
                  System.out.println(String.format("%-10d%-17s%-17s", employeeIdListAllById, firstNameListAllById,
                  lastNameListAllById));
                  
               }
            } catch (SQLException e) {
               e.printStackTrace();
            }
            break;
         case "2":
            System.out.print("Enter employeeID to view: ");
            String employeeIdToView = in.nextLine();
            //make sure it's a number
            try {
               //it's a number, but now need to check that there is such a technician
               int employeeIdToViewDetailed = Integer.parseInt(employeeIdToView);
               String checkIfEmployeeExistsQuery = "SELECT COUNT(*) AS employeeIdExists FROM Employee WHERE employeeID = " + employeeIdToViewDetailed;
               try {
                  //query to see if technician exists
                  ResultSet employeeIdExistsOrNotRS = stmt.executeQuery(checkIfEmployeeExistsQuery);
                  if (employeeIdExistsOrNotRS.next()) {
                     int employeeIdExistsOrNotInt = employeeIdExistsOrNotRS.getInt("employeeIdExists");
                     if (employeeIdExistsOrNotInt == 1) {
                        //ID has been verified
                        String detailedEmployeeInfoQuery = "SELECT * FROM Employee WHERE employeeID = " + employeeIdToViewDetailed;
                        ResultSet detailedEmployeeInfoRS = stmt.executeQuery(detailedEmployeeInfoQuery);
                        if (detailedEmployeeInfoRS.next()) {
                           System.out.print("Employee ID: ");
                           System.out.println(detailedEmployeeInfoRS.getInt("employeeID"));
                           System.out.print("Name: ");
                           System.out.print(unescapeString(detailedEmployeeInfoRS.getString("firstName")) + " ");
                           System.out.println(unescapeString(detailedEmployeeInfoRS.getString("lastName")));
                           System.out.print("Phone #: ");
                           System.out.println(unescapeString(detailedEmployeeInfoRS.getString("phoneNumber")));
                           System.out.print("Email: ");
                           System.out.println(unescapeString(detailedEmployeeInfoRS.getString("emailAddress")));
                           System.out.print("Room #: ");
                           System.out.println(detailedEmployeeInfoRS.getInt("officeLocation"));
                           System.out.print("Employment status: ");
                           int hiredOrNotTech = detailedEmployeeInfoRS.getInt("currentlyHired");
                           if (hiredOrNotTech == 1) {
                              System.out.println("Hired");
                           } else {
                              System.out.println("Fired");
                           }

                        }
                     } else {
                        System.out.println("Error: no such employee exists.");
                     }
                  }

               } catch (SQLException s) {
                  s.printStackTrace();
               }
            } catch (NumberFormatException e) {
               System.out.println("Error: employeeID must be a number.");
            }
            break;
         case "3":
            System.out.println("Returning to main menu.");
            break;
         case "q":
         case "Q":
         case "quit":
            System.out.println("Goodbye");
            System.exit(0);
            break;
         default:
            System.out.println("Invalid menu choice.");
            break;
         
      }
   }
   
   public static void deleteTicket(Scanner in, Statement stmt, Technician tech) {
      System.out.print("Enter ticket ID to delete: ");
      String deletionChoice = in.nextLine();
      
      try {
         int ticketToDelete = Integer.parseInt(deletionChoice);
         
         //need to check if ticket exists
         String checkTicketExistsQuery = "SELECT COUNT(*) AS ticketExistence FROM Ticket WHERE ticketID = " + ticketToDelete;
         boolean problem = false;
         try {
            ResultSet checkTicketExistsRS = stmt.executeQuery(checkTicketExistsQuery);
            if (checkTicketExistsRS.next()) {
               int ticketIdExists = checkTicketExistsRS.getInt("ticketExistence");
               if (ticketIdExists != 1) {
                  //ticket does not exist
                  System.out.println("Error: specified ticket does not exist.");
                  problem = true;
               }
            } else {
               System.out.println("Error: SQL query issue");
               problem = true;
            }
            
            if (tech.getIsAdmin() == false) {
               //if the user is not an admin, they can only delete their own tickets
               //so this will check if someone is an admin or not
               int myID = tech.getTechnicianID();
               String getAssignedTechnicianIdQuery = "SELECT assignedTechnicianID FROM Ticket WHERE ";
               getAssignedTechnicianIdQuery += "ticketID = " + ticketToDelete;
               ResultSet getAssignedTechIdRS = stmt.executeQuery(getAssignedTechnicianIdQuery);
               if (getAssignedTechIdRS.next()) {
                  int whoMadeTheTicket = getAssignedTechIdRS.getInt("assignedTechnicianID");
                  if (!(whoMadeTheTicket == myID)) {
                     System.out.println("Error: as a regular technician, you are only allowed to delete tickets you are assigned to.");
                     problem = true;
                  }
               }
            }
            
            //problem is true if a non-admin tries to delete someone else's ticket
            //or if the ticket doesn't exist
            if (!problem) {
               //delete the ticket
               String deleteTicketById = "DELETE FROM Ticket WHERE ticketID = " + ticketToDelete;
               stmt.executeUpdate(deleteTicketById);
            }

         
         } catch (SQLException s) {
            s.printStackTrace();
         }
         
      } catch (NumberFormatException e) {
         System.out.println("Error: ticket ID must be a number.");
      }
      
      
   }
   
   //reg tech assign self to an unassigned ticket
   //(doing work that nobody else is currently responsible for)
   public static void assignSelfToUnassigned(Scanner in, Statement stmt, Technician tech) {
      String allUnassignedOpenTicketsQuery = "SELECT ticketID, dateCreated, difficulty, title FROM AllOpenTickets WHERE assignedTechnicianID IS NULL";
      generalTicketLister(allUnassignedOpenTicketsQuery, stmt);
      System.out.print("Unassigned ticket to assign yourself to (hit c to cancel): ");
      String assignToUnassignedChoice = in.nextLine();
      if (!assignToUnassignedChoice.equals("c") && !assignToUnassignedChoice.equals("C")) {
         //check if ticketID is a number
         try {
            int ticketToAssignToSelf = Integer.parseInt(assignToUnassignedChoice);
            //check if ticket ID exists or not
            //also make sure it's unassigned
            String ticketIdExistsUnassigned = "SELECT COUNT(*) AS ticketIdExistsUn FROM Ticket WHERE ticketID = " + ticketToAssignToSelf;
            ticketIdExistsUnassigned += " AND assignedTechnicianID IS NULL";
            try {
               ResultSet ticketIdExistsUnassignedRS = stmt.executeQuery(ticketIdExistsUnassigned);
               if (ticketIdExistsUnassignedRS.next()) {
                  int ticketIsUnassignedAndExists = ticketIdExistsUnassignedRS.getInt("ticketIdExistsUn");
                  if (ticketIsUnassignedAndExists == 1) {
                     //verified that the ticket ID exists and it's unassigned
                     //now time to assign the technician to the ticket
                     String assignSelfToUnassigned = "UPDATE Ticket SET assignedTechnicianID = " + tech.getTechnicianID();
                     assignSelfToUnassigned += " WHERE ticketID = " + ticketToAssignToSelf;
                     stmt.executeUpdate(assignSelfToUnassigned);
                     System.out.println("You have been assigned to the ticket.");
                  } else {
                     System.out.println("Error: either the ticket doesn't exist, or it's not unassigned.");
                  }
               }
            } catch (SQLException e) {
               e.printStackTrace();
            }
         } catch (NumberFormatException e) {
            System.out.println("Error: ticketID must be a number.");
         }
      } else {
         System.out.println("Returning to main menu.");
      }
      
   }
   
   //edit a ticket
   //admins can edit any tickets, but regular techs can only edit their own tickets
   //based on assignedTechnicianID
   public static void editTickets(Scanner in, Statement stmt, Technician tech) {
      System.out.print("Enter the ticket ID of a ticket you want to edit: ");
      String ticketToEditChoice = in.nextLine();
      try {
         //check that the input is a number
         int ticketToEditInt = Integer.parseInt(ticketToEditChoice);
         //need to make sure ticket exists
         //after all, can't edit a ticket if it doesn't exist
         String checkIfTicketExistsQuery = "SELECT COUNT(*) AS editTicketExists FROM Ticket WHERE ticketID = " + ticketToEditInt;
         try {
            ResultSet checkTicketExistsRS = stmt.executeQuery(checkIfTicketExistsQuery);
            if (checkTicketExistsRS.next()) {
               int ticketToEditExists = checkTicketExistsRS.getInt("editTicketExists");

               if (ticketToEditExists == 1) {
                  //ticket with user-specified ID exists
                  //now need to check if it's their ticket
                  boolean problem = false;
                  if (tech.getIsAdmin() == false) {
                     //only regular techs need to check if it's theirs
                     //admins can edit any tickets
                     String checkIfTicketIsMineQuery = "SELECT COUNT(*) AS ticketExistsAndIsMine FROM Ticket WHERE ticketID = " + ticketToEditInt;
                     checkIfTicketIsMineQuery += " AND assignedTechnicianID = " + tech.getTechnicianID();
                     ResultSet checkIfTicketMineRS = stmt.executeQuery(checkIfTicketIsMineQuery);
                     if (checkIfTicketMineRS.next()) {
                        int ticketExists = checkIfTicketMineRS.getInt("ticketExistsAndIsMine");
                        if (ticketExists != 1) {
                           System.out.println("This ticket is not yours, so you can't edit it.");
                           System.out.println("Regular technicians can only edit tickets they are assigned to.");
                           problem = true;
                        }
                     }
                  }
                  if (!problem) {
                     //ticket exists and technician is allowed to edit it
                     //proceed with editing the ticket
                     System.out.println("What part of the ticket do you want to edit?");
                     System.out.println("1. Title");
                     System.out.println("2. Description");
                     System.out.println("3. Difficulty");
                     System.out.println("4. Open status");
                     System.out.println("5. Solution summary");
                     System.out.println("6. Assigned technicianID");
                     System.out.println("7. Assigned procedureID");
                     
                     
                     
                     
                     //ticket editing menu
                     //admins and technicians can use it
                     //but they have different options
                     System.out.print("Choice: ");
                     String editTicketOptionChoice = in.nextLine();
                     
                     switch (editTicketOptionChoice) {
                        case "1":
                           System.out.print("Enter new title (max 50 chars): ");
                           String newTitleTicketEdit = in.nextLine();
                           newTitleTicketEdit = escapeString(newTitleTicketEdit);
                           //length check
                           if ((newTitleTicketEdit.length() != 0) && (newTitleTicketEdit.length() < 50)) {
                              //passed length check
                              //now it's time to put newTitleTicketEdit in an update statement
                              String newTitleUpdate = "UPDATE Ticket SET title = '" + newTitleTicketEdit + "' WHERE ticketID = " + ticketToEditInt;
                              try {
                                 stmt.executeUpdate(newTitleUpdate);
                              } catch (SQLException s) {
                                 s.printStackTrace();
                              }
                           } else {
                              System.out.println("Error: invalid input.");
                           }
                           break;
                        case "2":
                           System.out.print("Enter new description (max 1000 chars): ");
                           String newDescriptionTicketEdit = in.nextLine();
                           newDescriptionTicketEdit = escapeString(newDescriptionTicketEdit);
                           if ((newDescriptionTicketEdit.length() != 0) && (newDescriptionTicketEdit.length() < 1000)) {
                              //proper length
                              //now it's time to put newDescriptionTicketEdit in an update statement
                              String newDescriptionUpdate = "UPDATE Ticket SET description = '" + newDescriptionTicketEdit + "' WHERE ticketID = " + ticketToEditInt;
                              try {
                                 stmt.executeUpdate(newDescriptionUpdate);
                              } catch (SQLException s) {
                                 s.printStackTrace();
                              }
                           } else {
                              System.out.println("Error: invalid input.");
                           }
                           break;
                        case "3":
                           System.out.print("Enter new difficulty (1 = easy, 2 = medium, 3 = hard): ");
                           String newDifficultyTicketEdit = in.nextLine();
                           int newDifficultyEdit = -1;
                           boolean proceed = false;
                           switch (newDifficultyTicketEdit) {
                              case "1":
                                 newDifficultyEdit = 1;
                                 proceed = true;
                                 break;
                              case "2":
                                 newDifficultyEdit = 2;
                                 proceed = true;
                                 break;
                              case "3":
                                 newDifficultyEdit = 3;
                                 proceed = true;
                                 break;
                              default:
                                 System.out.println("Invalid choice");
                                 break;
                           }
                           if (proceed) {
                              //now it's time to put newDifficultyEdit in an update statement
                              String newDifficultyUpdate = "UPDATE Ticket SET difficulty = " + newDifficultyTicketEdit + " WHERE ticketID = " + ticketToEditInt;
                              try {
                                 stmt.executeUpdate(newDifficultyUpdate);
                              } catch (SQLException s) {
                                 s.printStackTrace();
                              }
                           }

                           break;
                        case "4":
                           System.out.print("Enter new open status (0 = closed, 1 = open): ");
                           String newOpenStatusTicketEdit = in.nextLine();
                           int newOpenStatus = -1;
                           boolean proceedOpenStatus = false;
                           switch (newOpenStatusTicketEdit) {
                              case "0":
                                 newOpenStatus = 0;
                                 proceedOpenStatus = true;
                                 break;
                              case "1":
                                 newOpenStatus = 1;
                                 proceedOpenStatus = true;
                                 break;
                              default:
                                 System.out.println("Invalid choice");
                                 break;
                           }
                           if (proceedOpenStatus) {
                              //now time to put newOpenStatus in an update statement
                              String newOpenStatusUpdate = "UPDATE Ticket SET openStatus = " + newOpenStatus + " WHERE ticketID = " + ticketToEditInt;
                              try {
                                 stmt.executeUpdate(newOpenStatusUpdate);
                              } catch (SQLException s) {
                                 s.printStackTrace();
                              }
                           }
                           break;
                        case "5":
                           System.out.print("Enter new solution summary (max 1000 chars): ");
                           String newSolutionSummaryTicketEdit = in.nextLine();
                           newSolutionSummaryTicketEdit = escapeString(newSolutionSummaryTicketEdit);
                           if ((newSolutionSummaryTicketEdit.length() != 0) && (newSolutionSummaryTicketEdit.length() < 1000)) {
                              //passed length check
                              //now you know it's the right length, now time to put it in an update statement
                              String newSolutionSummaryUpdate = "UPDATE Ticket SET solutionSummary = '" + newSolutionSummaryTicketEdit + "' WHERE ticketID = " + ticketToEditInt;
                              try {
                                 stmt.executeUpdate(newSolutionSummaryUpdate);
                              } catch (SQLException s) {
                                 s.printStackTrace();
                              }
                           } else {
                              System.out.println("Error: invalid input.");
                           }
                           break;
                        case "6":
                           if (tech.getIsAdmin()) {
                              //admins can assign anyone to a ticket
                              System.out.print("[Admin] Enter new technicianID (integer of existing technician): ");
                              String newAssignedTechnicianTicketEdit = in.nextLine();
                              try {
                                 int newAssignedTechnicianInt = Integer.parseInt(newAssignedTechnicianTicketEdit);
                                 //now you know it's an int, now time to check if there's a technician with that ID
                                 String checkIfTechExistsTicketEdit = "SELECT COUNT(*) AS techCount FROM Technician WHERE technicianID = " + newAssignedTechnicianInt;
                                 try {
                                    ResultSet checkifTechExistsEditRS = stmt.executeQuery(checkIfTechExistsTicketEdit);
                                    if (checkifTechExistsEditRS.next()) {
                                       int technicianExists = checkifTechExistsEditRS.getInt("techCount");
                                       if (technicianExists == 1) {
                                          //technician exists, proceed with update
                                          String newTechnicianIDUpdate = "UPDATE Ticket SET assignedTechnicianID = " + newAssignedTechnicianInt;
                                          newTechnicianIDUpdate += " WHERE ticketID = " + ticketToEditInt;
                                          stmt.executeUpdate(newTechnicianIDUpdate);
                                       }
                                    }
                                 } catch (SQLException e) {   
                                    e.printStackTrace();
                                 }
                              } catch (NumberFormatException e) {
                                 System.out.println("Error: must be a number.");
                              }
                           } else {
                              //non-admins can only unassign themselves
                              //now you need to unassign the technician from the ticket
                              String newUpdate = "UPDATE Ticket SET assignedTechnicianID = NULL WHERE ticketID = " + ticketToEditInt;
                              try {
                                 stmt.executeUpdate(newUpdate);
                                 System.out.println("You have been unassigned from the ticket.");
                              } catch (SQLException s) {
                                 s.printStackTrace();
                              }
                              
                           }
                           break;
                        case "7":
                           System.out.print("Enter new procedureID (integer of existing procedure): ");
                           String newAssignedProcedureTicketEdit = in.nextLine();
                           try {
                              int newAssignedProcedureInt = Integer.parseInt(newAssignedProcedureTicketEdit);
                              //now you know it's an int, now time to see if a procedure exists with this ID
                              String checkIfProcedureExistsTicketEdit = "SELECT COUNT(*) AS procedureCount FROM StandardProcedure WHERE procedureID = " + newAssignedProcedureInt;
                              try {
                                 ResultSet checkifProcedureExistsEditRS = stmt.executeQuery(checkIfProcedureExistsTicketEdit);
                                 if (checkifProcedureExistsEditRS.next()) {
                                    int procedureExists = checkifProcedureExistsEditRS.getInt("procedureCount");
                                    if (procedureExists == 1) {
                                       //procedure exists, proceed with update
                                       String newProcedureIDUpdate = "UPDATE Ticket SET assignedProcedureID = " + newAssignedProcedureInt;
                                       newProcedureIDUpdate += " WHERE ticketID = " + ticketToEditInt;
                                       stmt.executeUpdate(newProcedureIDUpdate);
                                    }
                                 }
                              } catch (SQLException e) {   
                                 e.printStackTrace();
                              }
                           } catch (NumberFormatException e) {
                              System.out.println("Error: must be a number.");
                           }
                           break;
                        //admin only stuff
                        case "8":
                           if (tech.getIsAdmin()) {
                              System.out.print("[Admin] Enter new employeeID (integer of existing employee): ");
                              String newAssignedEmployeeTicketEdit = in.nextLine();
                              try {
                                 int newAssignedEmployeeInt = Integer.parseInt(newAssignedEmployeeTicketEdit);
                                 //now you know it's an int, now time to see if an employeeID with this ID exists
                                 String checkIfEmployeeExistsTicketEdit = "SELECT COUNT(*) AS employeeCount FROM Employee WHERE employeeID = " + newAssignedEmployeeInt;
                                 try {
                                    ResultSet checkifEmployeeExistsEditRS = stmt.executeQuery(checkIfEmployeeExistsTicketEdit);
                                    if (checkifEmployeeExistsEditRS.next()) {
                                       int employeeExists = checkifEmployeeExistsEditRS.getInt("employeeCount");
                                       if (employeeExists == 1) {
                                          //employee exists, proceed with update
                                          String newEmployeeIDUpdate = "UPDATE Ticket SET assignedEmployeeID = " + newAssignedEmployeeInt;
                                          newEmployeeIDUpdate += " WHERE ticketID = " + ticketToEditInt;
                                          stmt.executeUpdate(newEmployeeIDUpdate);
                                       }
                                    }
                                 } catch (SQLException e) {   
                                    e.printStackTrace();
                                 }
                              } catch (NumberFormatException e) {
                                 System.out.println("Error: must be a number.");
                              }
                           } else {
                              System.out.println("Invalid choice");
                           }
                           break;
                        case "9":
                           if (tech.getIsAdmin()) {
                              System.out.print("[Admin] Enter new ticketID (unique integer): ");
                              String newTicketIDTicketEdit = in.nextLine();
                              try {
                                 int ticketIdInt = Integer.parseInt(newTicketIDTicketEdit);
                                 //now you know it's a number, time to see if this ticketID is NOT taken
                                 String seeIfTicketIdExistsAlready = "SELECT COUNT(*) AS tickExistsAlready FROM Ticket WHERE ticketID = " + ticketIdInt;
                                 try {
                                    ResultSet seeIfTicketIdExistsAlreadyRS = stmt.executeQuery(seeIfTicketIdExistsAlready);
                                    if (seeIfTicketIdExistsAlreadyRS.next()) {
                                       int ticketExistsAlready = seeIfTicketIdExistsAlreadyRS.getInt("tickExistsAlready");
                                       if (ticketExistsAlready != 1) {
                                          //ticketID is not duplicate
                                          System.out.println("new ticketID is not a duplicate");
                                          String setNewTicketID = "UPDATE Ticket SET ticketID = " + ticketIdInt + " WHERE ticketID = " + ticketToEditInt;
                                          stmt.executeUpdate(setNewTicketID);
                                       } else {
                                          System.out.println("Error: specified ticketID already exists. ticketID must be unique.");
                                       }
                                    }
                                 } catch (SQLException e) {
                                    e.printStackTrace();
                                 }
                              } catch (NumberFormatException e) {
                                 System.out.println("Error: must be a number.");
                              }
                           } else {
                              System.out.println("Invalid choice");
                           }
                           break;
                        case "10":
                           if (tech.getIsAdmin()) {
                              System.out.print("[Admin] Enter new dateCreated (i.e. 2020-01-01 12:34:56): ");
                              String newDateCreatedTicketEdit = in.nextLine();
                              newDateCreatedTicketEdit = escapeString(newDateCreatedTicketEdit);
                              if (newDateCreatedTicketEdit.length() == 19) {
                                 //passed length check (but I'm not using regex so it could still be wrong)
                                 //now you know it's the right length, time to put it in an update
                                 String newDateCreatedUpdate = "UPDATE Ticket SET dateCreated = STR_TO_DATE('";
                                 newDateCreatedUpdate += newDateCreatedTicketEdit + "', '%Y-%m-%d %H:%i:%s')";
                                 newDateCreatedUpdate += " WHERE ticketID = " + ticketToEditInt;
                                 try {
                                    stmt.executeUpdate(newDateCreatedUpdate);
                                 } catch (SQLException s) {
                                    System.out.println("Error: is your date formatted correctly?");
                                    s.printStackTrace();
                                 }
                              } else {
                                 System.out.println("Error: invalid input.");
                              }
                           } else {
                              System.out.println("Invalid choice");
                           }
                           break;
                        default:
                           System.out.println("Invalid choice");
                           break;
                     }
                  }
               } else {
                  System.out.println("Error: ticket does not exist.");
               }
            }
            
         } catch (SQLException e) {
            e.printStackTrace();
         }

      } catch (NumberFormatException e) {
         System.out.println("Error: ticket ID must be a number");
      }
      
   }
   
   public static void createProcedure(Scanner in, Technician tech, Statement stmt) {
      System.out.print("Enter a name for the new procedure (max 30 chars): ");
      String newProcedureName = in.nextLine();
      newProcedureName = escapeString(newProcedureName);
      if ((newProcedureName.length() != 0) && (newProcedureName.length() <= 30)) {
         //passed length check
         //get the instructions for the procedure
         System.out.print("Enter the instructions for the procedure (max 1000 chars): ");
         String newProcedureInstructions = in.nextLine();
         newProcedureInstructions = escapeString(newProcedureInstructions);
         if ((newProcedureInstructions.length() != 0) && (newProcedureInstructions.length() <= 1000)) {
            //now both the name and instructions are fine
            String createNewProcedure = "INSERT INTO StandardProcedure (name, instructions) VALUES ";
            createNewProcedure += " ('" + newProcedureName + "', '" + newProcedureInstructions + "')";
            try {
               stmt.executeUpdate(createNewProcedure);               
            } catch (SQLException e) { 
               e.printStackTrace();
            }
         }
      }
   }
   
   public static void createEmployee(Scanner in, Technician tech, Statement stmt) {
      //running out of time for this project so this method doesn't check the length of the input
      System.out.println("Keep in mind that single and double quotes get escaped and thus take up more characters.");
      System.out.print("Enter a first name for the new employee (max 15 chars): ");
      String newEmployeeFirstName = in.nextLine();
      newEmployeeFirstName = escapeString(newEmployeeFirstName);
      System.out.print("Enter a last name for the new employee (max 15 chars): ");
      String newEmployeeLastName = in.nextLine();
      newEmployeeLastName = escapeString(newEmployeeLastName);
      System.out.print("Enter a phone number (in the form of 123-456-7890): ");
      String newEmployeePhoneNumber = in.nextLine();
      newEmployeePhoneNumber = escapeString(newEmployeePhoneNumber);
      System.out.print("Enter an email address for the new employee: ");
      String newEmployeeEmailAddress = in.nextLine();
      newEmployeeEmailAddress = escapeString(newEmployeeEmailAddress);
      System.out.print("Enter the room number for the new employee: ");
      String newEmployeeOfficeLocation = in.nextLine();
      newEmployeeOfficeLocation = escapeString(newEmployeeOfficeLocation);
      try {
         int newEmployeeOfficeLocationInt = Integer.parseInt(newEmployeeOfficeLocation);
         String makeNewEmployee = "INSERT INTO Employee (firstName, lastName, phoneNumber, emailAddress, officeLocation, currentlyHired) VALUES (";
         makeNewEmployee += "'" + newEmployeeFirstName + "', '" + newEmployeeLastName + "', '" + newEmployeePhoneNumber + "', '";
         makeNewEmployee += newEmployeeEmailAddress + "', " + newEmployeeOfficeLocationInt +  ", 1)"; //new employee means hired = 1
         System.out.println(makeNewEmployee);
         try {
            stmt.executeUpdate(makeNewEmployee);
         } catch (SQLException e) {
            e.printStackTrace();
         }
      } catch (NumberFormatException e) {
         System.out.println("Error: room number must be a number.");
      }
      
   }
   
   public static void createTechnician(Scanner in, Technician tech, Statement stmt) {
      //running out of time to complete this project so this method doesn't do input validation, unfortunately
      System.out.print("Enter first name for the new technician (max 15 chars): ");
      String newTechFirstName = in.nextLine();
      newTechFirstName = escapeString(newTechFirstName);
      System.out.print("Enter last name for the new technician (max 15 chars): ");
      String newTechLastName = in.nextLine();
      newTechLastName = escapeString(newTechLastName);
      System.out.print("Enter phone number for the new technician (in the form of 123-456-7890): ");
      String newTechPhoneNumber = in.nextLine();
      newTechPhoneNumber = escapeString(newTechPhoneNumber);
      System.out.print("Enter email address for the new technician (max 30 chars): ");
      String newTechEmail = in.nextLine();
      newTechEmail = escapeString(newTechEmail);
      
      System.out.print("Enter room number for the new technician (integer): ");
      String newTechOffice = in.nextLine();
      int newTechOfficeInt = -1;
      try {
         newTechOfficeInt = Integer.parseInt(newTechOffice);
      } catch (NumberFormatException e) {
         System.out.println("Error: must be a number.");
      }
      
      System.out.print("Enter numeric admin status for the new technician (0 = non-admin, 1 = admin): ");
      String newTechAdminStatus = in.nextLine();
      int newTechAdminStatusInt = -1;
      try {
         newTechAdminStatusInt = Integer.parseInt(newTechAdminStatus);
      } catch (NumberFormatException e) {
         System.out.println("Error: must be a number.");
      }
      
      System.out.print("Enter specialty for the new technician (max 30 chars): ");
      String newTechSpecialty = in.nextLine();
      newTechSpecialty = escapeString(newTechSpecialty);
      
      System.out.print("Enter numeric experience level for the new technician (1 = beginner, 2 = intermediate, 3 = expert): ");
      String newTechExperienceLevel = in.nextLine();
      int newTechExperienceLevelInt = -1;
      try {
         newTechExperienceLevelInt = Integer.parseInt(newTechExperienceLevel);
      } catch (NumberFormatException e) {
         System.out.println("Error: must be a number.");
      }
      
      System.out.print("Enter password for the new technician (max 32 chars): ");
      String newTechPassword = in.nextLine();
      newTechPassword = escapeString(newTechPassword);
      
      String makeNewTechnician = "INSERT INTO Technician (firstName, lastName, phoneNumber, emailAddress, officeLocation, isAdmin, specialty, experienceLevel, password, currentlyHired";
      makeNewTechnician += ") VALUES ('" + newTechFirstName + "', '" + newTechLastName + "', '" + newTechPhoneNumber + "', '" + newTechEmail + "', " + newTechOfficeInt;
      makeNewTechnician += ", " + newTechAdminStatusInt + ", '" + newTechSpecialty + "', " + newTechExperienceLevelInt + ", '" + newTechPassword + "', 1)";
      System.out.println(makeNewTechnician);
      try {
         stmt.executeUpdate(makeNewTechnician);
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }
   
   
   
   
   
   public static void adminCreateMenu(Scanner in, Technician tech, Statement stmt) {
      System.out.println("What do you want to create?");
      System.out.println("1. Ticket");
      System.out.println("2. Employee");
      System.out.println("3. Standard procedure");
      System.out.println("4. Technician");
      System.out.print("Choice: ");
      String adminCreateMenuChoice = in.nextLine();
      switch (adminCreateMenuChoice) {
         case "1":
            createTicket(in, stmt, tech);
            break;
         case "2":
            createEmployee(in, tech, stmt);
            break;
         case "3":
            createProcedure(in, tech, stmt);
            break;
         case "4":
            createTechnician(in, tech, stmt);
            break;
         default:
            System.out.println("Invalid input.");
            break;
      }
   }
   
   public static void adminReadMenu(Scanner in, Technician tech, Statement stmt) {
      System.out.println("To read stuff, go to the regular technician menu. They can read everything.");
   }
   
   public static void editProcedure(Scanner in, Technician tech, Statement stmt) {
      //this method doesn't do input validation because I ran out of time
      System.out.print("Enter a procedureID to edit: ");
      int procedureNumber = Integer.parseInt(in.nextLine());
      System.out.println("What do you want to edit? ");
      System.out.println("1. Name");
      System.out.println("2. Instructions");
      String editProcedureChoice = in.nextLine();
      switch (editProcedureChoice) {
         case "1":
            System.out.print("Enter a new name for the procecure (max 30 chars): ");
            String newProcedureName = in.nextLine();
            newProcedureName = escapeString(newProcedureName);
            try {
               String updateProcedureName = "UPDATE StandardProcedure SET name = '" + newProcedureName + "' ";
               updateProcedureName += "WHERE procedureID = " + procedureNumber;
               stmt.executeUpdate(updateProcedureName);
            } catch (SQLException e) {
               e.printStackTrace();
            }
            break;
         case "2":
            System.out.print("Enter new instructions for the procedure (max 1000 chars): ");
            String newProcedureInstructions = in.nextLine();
            newProcedureInstructions = escapeString(newProcedureInstructions);
            try {
               String updateProcedureInstructions = "UPDATE StandardProcedure SET instructions = '" + newProcedureInstructions + "' ";
               updateProcedureInstructions += "WHERE procedureID = " + procedureNumber;
               stmt.executeUpdate(updateProcedureInstructions);
            } catch (SQLException e) {
               e.printStackTrace();
            }
            break;
         default:
            System.out.println("Error: invalid choice.");
            break;
      }    
   }
   
   public static void editEmployee(Scanner in, Technician tech, Statement stmt) {
      //this method doesn't do input validation because I ran out of time
      System.out.println("Enter an employeeID of the employee you want to edit: ");
      try {
         int employeeToEdit = Integer.parseInt(in.nextLine());
         System.out.println("What do you want to edit?");
         System.out.println("1. First name");
         System.out.println("2. Last name");
         System.out.println("3. Phone number");
         System.out.println("4. Email address");
         System.out.println("5. Office location");
         System.out.println("6. Hired status");
         System.out.println("7. employeeID");
         String columnEditChoice = in.nextLine();
         switch (columnEditChoice) {
            case "1":
               System.out.print("Enter new first name: ");
               String newFirstName = in.nextLine();
               newFirstName = escapeString(newFirstName);
               try {
                  String setNewFirstName = "UPDATE Employee SET firstName = '" + newFirstName + "' WHERE employeeID = " + employeeToEdit;
                  stmt.executeUpdate(setNewFirstName);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
               break;
            case "2":
               System.out.print("Enter new last name: ");
               String newLastName = in.nextLine();
               newLastName = escapeString(newLastName);
               try {
                  String setNewLastName = "UPDATE Employee SET lastName = '" + newLastName + "' WHERE employeeID = " + employeeToEdit;
                  stmt.executeUpdate(setNewLastName);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
               break;
            case "3":
               System.out.print("Enter new phone number: ");
               String newPhone = in.nextLine();
               newPhone = escapeString(newPhone);
               try {
                  String setNewPhone = "UPDATE Employee SET phoneNumber = '" + newPhone + "' WHERE employeeID = " + employeeToEdit;
                  stmt.executeUpdate(setNewPhone);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
               break;
            case "4":
               System.out.print("Enter new email address: ");
               String newEmail = in.nextLine();
               newEmail = escapeString(newEmail);
               try {
                  String setNewEmail = "UPDATE Employee SET emailAddress = '" + newEmail + "' WHERE employeeID = " + employeeToEdit;
                  stmt.executeUpdate(setNewEmail);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
               break;
            case "5":
               System.out.print("Enter new office location: ");
               
               try {
                  int newOfficeInt = Integer.parseInt(in.nextLine());
                  try {
                     String setNewOffice = "UPDATE Employee SET officeLocation = " + newOfficeInt + " WHERE employeeID = " + employeeToEdit;
                     stmt.executeUpdate(setNewOffice);
                  } catch (SQLException e) {
                     e.printStackTrace();
                  }
               } catch (NumberFormatException e) {
                  e.printStackTrace();
               }
               
               break;
            case "6":
               System.out.print("Enter new hired status:");
               try {
                  int newHiredInt = Integer.parseInt(in.nextLine());
                  try {
                     String setNewHired = "UPDATE Employee SET currentlyHired = " + newHiredInt + " WHERE employeeID = " + employeeToEdit;
                     stmt.executeUpdate(setNewHired);
                  } catch (SQLException e) {
                     e.printStackTrace();
                  }
               } catch (NumberFormatException e) {
                  e.printStackTrace();
               }
               break;
            case "7":
               System.out.print("Enter new employeeID: ");
               try {
                  int newEmpIDInt = Integer.parseInt(in.nextLine());
                  try {
                     String setNewEmpID = "UPDATE Employee SET employeeID = " + newEmpIDInt + " WHERE employeeID = " + employeeToEdit;
                     stmt.executeUpdate(setNewEmpID);
                  } catch (SQLException e) {
                     e.printStackTrace();
                  }
               } catch (NumberFormatException e) {
                  e.printStackTrace();
               }
               break;
            default:
               break;
         }
      } catch (NumberFormatException e) {
         System.out.println("Error: employeeID must be a number.");
      }
   }
   
   public static void adminUpdateMenu(Scanner in, Technician tech, Statement stmt, MenuPrinter menu) {
      System.out.println("What do you want to update?");
      System.out.println("1. Ticket");
      System.out.println("2. Employee");
      System.out.println("3. Standard procedure");
      System.out.println("4. Technician");
      System.out.print("Choice: ");
      String adminUpdateMenuChoice = in.nextLine();
      switch (adminUpdateMenuChoice) {
         case "1":
            editTickets(in, stmt, tech);
            break;
         case "2":
            editEmployee(in, tech, stmt);
            break;
         case "3":
            editProcedure(in, tech, stmt);
            break;
         case "4":
            menu.editOwnTechnicianInfo(tech, stmt);
            editSelfInfo(in, stmt, tech);
            break;
         default:
            System.out.println("Invalid input.");
            break;
      }
   }
   
   public static void adminDeleteMenu(Scanner in, Technician tech, Statement stmt) {
      System.out.println("What do you want to delete?");
      System.out.println("1. Ticket");
      System.out.println("2. Employee");
      System.out.println("3. Standard procedure");
      System.out.println("4. Technician");
      System.out.print("Choice: ");
      String adminDeleteMenuChoice = in.nextLine();
      switch (adminDeleteMenuChoice) {
         case "1":
            System.out.print("Enter ticketID of ticket to delete: ");
            try {
               int ticketToDelete = Integer.parseInt(in.nextLine());
               String ticketDeletion = "DELETE FROM Ticket WHERE ticketID = " + ticketToDelete;
               try {
                  stmt.executeUpdate(ticketDeletion);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
            } catch (NumberFormatException e) {
               System.out.println("Error: must be a number.");
            }
            break;
         case "2":
            System.out.print("Enter employeeID of employee to delete: ");
            try {
               int employeeToDelete = Integer.parseInt(in.nextLine());
               if (employeeToDelete == 1) {
                  System.out.println("ID 1 is a reserved placeholder and can't be deleted");
                  return;
               }
               //first: swap out assignedEmployeeID of tickets to 1 for the placeholder
               String swapTicketsToPlaceholderEmployee = "UPDATE Ticket SET assignedEmployeeID = 1 WHERE ";
               swapTicketsToPlaceholderEmployee += "assignedEmployeeID = " + employeeToDelete;
               try {
                  stmt.executeUpdate(swapTicketsToPlaceholderEmployee);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
               //now that there are no tickets associated with this employee, they can be deleted
               String employeeDeletion = "DELETE FROM Employee WHERE employeeID = " + employeeToDelete;
               try {
                  stmt.executeUpdate(employeeDeletion);
               } catch (SQLException e) {
                  
                  e.printStackTrace();
               }
            } catch (NumberFormatException e) {
               System.out.println("Error: must be a number.");
            }
            break;
         case "3":
            System.out.print("Enter procedureID of procedure to delete: ");
            try {
               int procedureToDelete = Integer.parseInt(in.nextLine());
               if (procedureToDelete == 1) {
                  System.out.println("Can't delete procedureID 1. It's a placeholder for deleted procedures.");
                  System.out.println("This is because of how the Ticket table has foreign keys.");
                  return;
               }
               String swapTicketsToPlaceholderProcedure = "UPDATE Ticket SET assignedProcedureID = 1 WHERE ";
               swapTicketsToPlaceholderProcedure += "assignedprocedureID = " + procedureToDelete;
               try {
                  stmt.executeUpdate(swapTicketsToPlaceholderProcedure);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
               
               String procedureDeletion = "DELETE FROM StandardProcedure WHERE procedureID = " + procedureToDelete;
               try {
                  stmt.executeUpdate(procedureDeletion);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
            } catch (NumberFormatException e) {
               System.out.println("Error: must be a number.");
            }
            break;
         case "4":
            System.out.print("Enter technicianID of techniciand to delete: ");
            try {
               int technicianToDelete = Integer.parseInt(in.nextLine());
               if (technicianToDelete == 1) {
                  System.out.println("Can't delete technicianID 1. It's a placeholder for deleted technicians.");
                  System.out.println("This is because of how the Ticket table has foreign keys.");
                  return;
               }
               String swapTicketsToPlaceholderTechnician = "UPDATE Ticket SET assignedTechnicianID = 1 WHERE ";
               swapTicketsToPlaceholderTechnician += "assignedTechnicianID = " + technicianToDelete;
               try {
                  stmt.executeUpdate(swapTicketsToPlaceholderTechnician);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
               
               String technicianDeletion = "DELETE FROM Technician WHERE technicianID = " + technicianToDelete;
               try {
                  stmt.executeUpdate(technicianDeletion);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
            } catch (NumberFormatException e) {
               System.out.println("Error: must be a number.");
            }
            break;
         default:
            System.out.println("Invalid input.");
            break;
      }
   }
   
   
   //main menu cycle for regular technician menu
   public static void regTechMainMenuCycle(String menuChoice, Statement stmt, Technician myself, MenuPrinter menu, Scanner input) {
      switch (menuChoice) {
         //these are unfinished "stub" placeholders
      case "1":
         //regular technician create new ticket
         menu.divider();
         myself.updateAll(stmt);
         createTicket(input, stmt, myself);
         break;
      case "2":
         //view tickets
         menu.divider();
         myself.updateAll(stmt);
         menu.ticketReadingMenu(myself, stmt);
         viewTickets(input, stmt, myself);
         break;
      case "3":
         menu.divider();
         myself.updateAll(stmt);
         System.out.println("Regular tech edit tickets assigned to self");
         editTickets(input, stmt, myself);
         break;
      case "4":
         //assign yourself to an unassigned ticket
         menu.divider();
         myself.updateAll(stmt);
         assignSelfToUnassigned(input, stmt, myself);
         break;
      case "5":
         //delete a ticket
         menu.divider();
         myself.updateAll(stmt);
         deleteTicket(input, stmt, myself);
         //updateAll() gets the latest info about your account
         //if you are fired or your account has been deleted, you get logged out
         //if an employee is logged in remotely when they are fired, their account is
         //logged out before they can do anything else
         //also updates other info displayed at the top of the menu
         break;
      case "6":
         //read employees
         menu.divider();
         myself.updateAll(stmt);
         menu.readEmployeeMenu(myself, stmt);
         readEmployees(input, stmt, myself);
         break;
      case "7":
         //read technicians
         menu.divider();
         myself.updateAll(stmt);
         menu.readTechnicianMenu(myself, stmt);
         readTechnicians(input, stmt, myself);
         break;
      case "8":
         //edit your own info
         menu.divider();
         menu.editOwnTechnicianInfo(myself, stmt);
         editSelfInfo(input, stmt, myself);
         break;
      case "9":
         //read standard procedures
         menu.divider();
         menu.procedureReadingMenu(myself, stmt);
         viewProcedures(input, stmt, myself);
         break;
      case "q":
      case "Q":
      case "quit":
         //when non-admin gets here, q means quit
         //but because admins can also access the tech menu from the admin menu,
         //hitting q here will just take them back to the admin menu loop
         if (!myself.getIsAdmin()) {
            System.out.println("Goodbye.");
            System.exit(1);
         } else {
            System.out.println("Returning to admin menu.");
            break;
         }
         break;
      default:
         System.out.println("Invalid menu choice.");
         break;
   }
   };
   

   //!!!program entry point!!!
   public static void main(String[] args){
      System.out.println("IT Ticketing System");
      Scanner input = new Scanner(System.in);
      
      //checking if setup has been completed
      File finishedFile = new File("finished_setup.txt");
      
      //if setup has not been completed, proceed with initial setup to get credentials/db info to connect
      if (!finishedFile.exists()) {
         System.out.println("It looks like this is your first time running the program.");
         System.out.println("Initiating database connection setup..."); //old one was jdbc:mysql://cs.neiu.edu:3306/
         System.out.println("Hard-coded portion of remote database: jdbc:mysql://");
         System.out.print("Enter database name: ");
         String databaseName = input.nextLine();
         System.out.print("Enter username: ");
         String username = input.nextLine();
         System.out.print("Enter password: ");
         String password = input.nextLine();
         
         //writing credentials to credentials.txt
         //keep in mind: this is just for connecting to the database
         //you'll still need to use a Technician ID and Technician password to access the program
         try {
            PrintWriter credWriter = new PrintWriter("credentials.txt");
            credWriter.println(databaseName);
            credWriter.println(username);
            credWriter.println(password);
            credWriter.close();
            //the existence of finished_setup.txt indicates that the setup was previously completed
            //and as such doesn't need to happen again
            PrintWriter finishedSetup = new PrintWriter("finished_setup.txt");
            finishedSetup.print("");
            finishedSetup.close();
         } catch (IOException ex) {
            ex.printStackTrace();
            
         }
         //end of initial setup
      } else {
         System.out.println("Program has already been set up with database connection info.");
      }      
      
      //load credentials from credentials.txt
      //I am aware that storing credentials in plaintext is not a good idea for security
      String dbName = "";
      String uname = "";
      String pass = "";
      try {
         Scanner creds = new Scanner(new File("credentials.txt"));
         //putting the credentials in a file instead of the source code means the program
         //is more versatile and can accept different accounts or databases instead of just one
         try {
            dbName = creds.nextLine();
            uname = creds.nextLine();
            pass = creds.nextLine();
         } catch (NoSuchElementException no) {
            System.err.println("Error with loading credentials; is it empty or messed up?");
         }
         creds.close();
      } catch (IOException ex) {
         ex.printStackTrace();
      }//finished loading credentials
      
      //making sure credentials are not empty strings
      if (!((dbName == "") || (uname == "") || (pass == ""))) {
         System.out.println("Loaded DB connection credentials from credentials.txt successfully.");
         
         //credentials are loaded from the file but not guaranteed to work (i.e. incorrect password)
      
         //testing connection to make sure credentials are valid
         try {
            String url = "jdbc:mysql://cs.neiu.edu:3306/" + dbName + "?serverTimezone=UTC&";
            url += "user=" + uname + "&password=" + pass;
            
            Connection conn = DriverManager.getConnection(url);
            System.out.println("Successfully connected to the database.");
            //all done with initial database connection
            
            //getting technician ID and password (the ticketing login, not the database connection login)
            MenuPrinter menu = new MenuPrinter();
            menu.divider();
            System.out.println("Technician Login");
            System.out.println("(Accounts from the Technician table data I provided)");
            System.out.println("(When grading this, try using it with 2 accounts:");
            System.out.println("Technician ID 2 (non-admin) and technician ID 9 (admin)");
            System.out.print("Enter your technician ID: ");
            String loginID = input.nextLine();
            int loginIdInt = 0;
            //make sure user enters an int for the technicianID
            try { 
               loginIdInt = Integer.parseInt(loginID);
            } catch (NumberFormatException ex) {
               System.err.println("Error: technicianID must be an integer.");
               //ex.printStackTrace();
               System.exit(1);
            }
            System.out.print("Enter your technician password: ");
            String loginPassword = input.nextLine();
            System.out.println("Verifying technician login details...");
            
            //technician ID 1 is reserved for a "deleted" placeholder
            if (loginIdInt == 1) {
               System.out.println("Invalid technician ID.");
               System.exit(1);
            }
            
            //for running SQL queries
            Statement stmt = conn.createStatement();
            
            //making sure the technician ID entered by the user is an actual technician ID in the database
            String verifyTechnicianExistsQuery = "SELECT COUNT(technicianID) as techVerify FROM Technician WHERE technicianID = " + loginIdInt;
            ResultSet verifyTechIdRS = stmt.executeQuery(verifyTechnicianExistsQuery);
            if (verifyTechIdRS.next()) {
               int accountExists = verifyTechIdRS.getInt("techVerify");
               if (accountExists == 1) {
                  //at this point, technician ID has been verified
                  //but that doesn't necessarily mean their password is correct
                  
                  //proceeding with verifying technician password
                  String verifyTechnicianPasswordQuery = "SELECT password FROM Technician WHERE technicianID = " + loginIdInt;
                  ResultSet verifyPassword = stmt.executeQuery(verifyTechnicianPasswordQuery);
                  String actualPassword = "";
                  if (verifyPassword.next()) {
                     //getting the actual password from the database
                     actualPassword = verifyPassword.getString("password");
                  }
                  //comparing submitted password to actual password
                  if ((actualPassword != "") && (actualPassword.equals(loginPassword))) {
                     //for converting the logged in technician's SQL data to an object in Java
                     Technician myself = new Technician();
                     //Technician object queries database to get logged in user's info
                     myself.setTechnicianID(loginIdInt);
                     myself.updateOneIntToBool("isAdmin", stmt);
                     myself.updateAll(stmt);
                     
                     System.out.println("Successfully logged in");
                     //I know that, in the real world, this username/password verification
                     //would have to happen server-side, not client-side, but this is a limitation of 
                     //my final project because we're only dealing with MySQL instead of doing in-depth
                     //back-end coding
                     //but a ticketing system needs many accounts, one for each technician who uses it
                     
                     //now need to check if the technician is a regular technician or an administrator
                     //I realize that client-validated privileges can easily be tampered with, like with
                     //a memory editor called CheatEngine to change the boolean value in RAM to "true"
                     //in a real ticketing system, this functionality would be done server-side
                     //to be more secure
                     
                     //!!!all done with user authentication, now proceeding with the "real" ticketing system portion!!!
                     if (myself.getIsAdmin() == false) {
                        System.out.println("You are a regular technician");
      
                        
                        //show regular technician main menu (limited privileges)
                        //menu loop
                        
                        //be sure to try using the program with both a non-admin and an admin account
                        //because they can do different things in the ticketing system
                        String menuChoice = "";
                        //type q or quit to quit the program
                        while ((!menuChoice.equals("q")) && (!menuChoice.equals("Q")) && (!menuChoice.equals("quit"))) {
                           //update the logged in technician's details
                           //syncing client with up-to-date database data, basically
                           myself.updateAll(stmt);
                                             
                           //showing main regular technician menu
                           menu.regularTechnicianMainMenu(myself, stmt);
                           System.out.print("Choice: ");
                           menuChoice = input.nextLine();
                           
                           
                           //!!!!!!where everything happens!!!!!!!
                           regTechMainMenuCycle(menuChoice, stmt, myself, menu, input);
                           
                           
                           System.out.print("Press enter to continue.");
                           input.nextLine();
                        }
                        
                     } else if (myself.getIsAdmin()) {
                        System.out.println("You are an administrator");
                        MenuPrinter adminMenu = new MenuPrinter();
                        
                        String adminMenuChoice = "";
                        while ((!adminMenuChoice.equals("q")) && (!adminMenuChoice.equals("Q")) && (!adminMenuChoice.equals("quit"))) {
                           myself.updateAll(stmt);
                           adminMenu.adminMainMenu(myself, stmt);
                           System.out.print("Choice: ");
                           adminMenuChoice = input.nextLine();
                           switch (adminMenuChoice) {
                              case "1":
                                 adminMenu.divider();
                                 //adminMenu.adminCreateMenu(myself, stmt);
                                 adminCreateMenu(input, myself, stmt);
                                 break;
                              case "2":
                                 adminMenu.divider();
                                 adminMenu.adminReadMenu(myself, stmt);
                                 adminReadMenu(input, myself, stmt);
                                 break;
                              case "3":
                                 adminMenu.divider();
                                 //adminMenu.adminUpdateMenu(myself, stmt);
                                 adminUpdateMenu(input, myself, stmt, adminMenu);
                                 break;
                              case "4":
                                 adminMenu.divider();
                                 //adminMenu.adminDeleteMenu(myself, stmt);
                                 adminDeleteMenu(input, myself, stmt);
                                 break;
                              case "5":
                                 //going from admin menu to reg tech menu
                                 menu.adminDivider();
                                 System.out.println("ADMINS ONLY: Hit q to go back to the admin menu.");
                                 menu.regularTechnicianMainMenu(myself, stmt);
                                 System.out.print("Choice: ");
                                 String regTechMenuInAdminMenuChoice = input.nextLine();
                                 regTechMainMenuCycle(regTechMenuInAdminMenuChoice, stmt, myself, adminMenu, input);
                                 System.out.print("Press enter to continue.");
                                 input.nextLine();
                                 break;
                              case "q":
                              case "Q":
                              case "quit":
                                 System.out.println("Goodbye.");
                                 System.exit(0);
                                 break;
                              default:
                                 System.out.println("Invalid menu choice.");   
                                 break;
                           }
                           System.out.print("Press enter to continue.");
                           input.nextLine();
                           
                        }
                        
                     } else {
                        System.err.println("Admin status check error.");
                        System.exit(1);
                     }
                          

                  } else {
                     System.out.println("Invalid password.");
                     System.exit(1);
                  }
                  

               } else {
                  System.err.println("Error: no technician exists with that ID number");
                  System.exit(1);
               }
            }


             
         } catch(SQLException ex) {
            //System.err.println("Error with connecting to database. Possible credential issue?");
            ex.printStackTrace();
            System.exit(1);   
         }
      
         
      } else {
         //issues with credentials.txt file
         System.out.println("Error loading credentials.");

      }
      
      
      //closing scanner
      input.close();
   }
}