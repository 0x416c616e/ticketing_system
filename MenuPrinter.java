import java.sql.*;

class MenuPrinter {
   public MenuPrinter(){
      //nothing really happens here
      //I just want to de-clutter my Main class by taking all the menu printing out of it
      //so that it's easier to focus on reading the program logic
   }
   
   
   public void title() {
      System.out.println("IT Ticketing System");
   }
   
   public void divider() {
      System.out.println("-------------------------------------------------------");
   }
   
   public void adminDivider() {
      System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~ADMIN~~~~~~~~~~~~~~~~~~~~~~~~~");
   }
   
   public void loggedInUserInfo(Technician tech, Statement stmt) {
      //update Technician object with latest data from the database in case anything has changed
      tech.updateAll(stmt);
      //count number of open tickets assigned to the logged in technician
      String myOpenTicketsQuery = "SELECT COUNT(ticketID) AS myTicketCount FROM Ticket WHERE assignedTechnicianID = " + tech.getTechnicianID() + " AND openStatus = 1";
      String allUnassignedOpenTicketsQuery = "SELECT COUNT(ticketID) AS allUnassignedOpen FROM Ticket WHERE openStatus = 1 AND assignedTechnicianID IS NULL";
      try {
         ResultSet myOpenTicketRS = stmt.executeQuery(myOpenTicketsQuery);
         int myOpenTickets = -1; //if it prints -1 then something went wrong
         if (myOpenTicketRS.next()) {
            myOpenTickets = myOpenTicketRS.getInt("myTicketCount");
         }
         ResultSet unassignedOpenRS = stmt.executeQuery(allUnassignedOpenTicketsQuery);
         int unassignedOpen = -1; //if it prints -1 then something went wrong
         if (unassignedOpenRS.next()) {
            unassignedOpen = unassignedOpenRS.getInt("allUnassignedOpen");
         }
         System.out.print("Logged in as " + tech.getFirstName() + " " + tech.getLastName() + " (ID: " + tech.getTechnicianID() + ", ");
         if (tech.getIsAdmin()) {
            System.out.print("Administrator, ");
         } else {
            System.out.print("Regular Technician, ");
         }
         switch (tech.getExperienceLevel()) {
            case 1:
               System.out.println("Novice)");
               break;
            case 2:
               System.out.println("Intermediate)");
               break;
            case 3:
               System.out.println("Expert)");
               break;
            default:
               System.out.println("Error with getting experience level");
               break;
         }

         System.out.println("You currently have " + myOpenTickets + " unfinished ticket(s). There are currently " + unassignedOpen + " unassigned ticket(s).");

      } catch(SQLException e) {
         e.printStackTrace();
      }
   }
   
   public void ticketReadingMenu(Technician tech, Statement stmt) {
      this.divider();
      loggedInUserInfo(tech, stmt);
      System.out.println("Ticket reading menu");
      System.out.println("Enter a number or hit q to quit:");
      System.out.println("1. List open tickets you are assigned to");
      System.out.println("2. List all tickets you are assigned to");
      System.out.println("3. List all unassigned open tickets");
      System.out.println("4. List all open tickets");
      System.out.println("5. List all tickets");
      System.out.println("6. Read detailed info about a specific ticket (by ticketID)");
      System.out.println("7. Search tickets by text or difficulty level");
      System.out.println("8. Back to main menu");
   }
   
   public void procedureReadingMenu(Technician tech, Statement stmt) {
      this.divider();
      loggedInUserInfo(tech, stmt);
      System.out.println("Procedure reading menu");
      System.out.println("Enter a number of hit q to quit:");
      System.out.println("1. View list of procedures");
      System.out.println("2. View detailed procedure info (by procedureID)");
      System.out.println("3. Search procedures");   
      System.out.println("4. Back to main menu");   
   }
   
   public void regularTechnicianMainMenu(Technician tech, Statement stmt) {
      this.divider();
      loggedInUserInfo(tech, stmt);
      //menu options
      System.out.println("Regular technician main menu");
      System.out.println("Enter a number or q to quit:");
      System.out.println("1. Create a new ticket");
      System.out.println("2. View or search tickets");
      System.out.print("3. Edit tickets");
      if (tech.getIsAdmin() == false) {
         System.out.println(" that you are assigned to");
      } else {
         System.out.println();
      }
      System.out.println("4. Assign yourself to an unassigned ticket");
      System.out.print("5. Delete tickets");
      if (tech.getIsAdmin() == false) {
          System.out.println(" you are assigned to");
      } else {
         System.out.println();
      }
      System.out.println("6. View employees");
      System.out.println("7. View technicians");
      System.out.println("8. Edit your own technician info");
      System.out.println("9. View/search standard procedures");

   }
   
   public void editOwnTechnicianInfo(Technician tech, Statement stmt) {
      this.divider();
      loggedInUserInfo(tech, stmt);
      //menu options
      System.out.println("Use the view employees menu to view your own details.");
      System.out.println("Just enter in your technicianID there to see it.");
      System.out.println("This menu is for replacing old values with new values only.");
      System.out.println("What do you want to edit?");
      System.out.println("Enter a number or q to quit:");
      System.out.println("1. First name");
      System.out.println("2. Last name");
      System.out.println("3. Phone number");
      System.out.println("4. Email address");
      System.out.println("5. Office location");
      System.out.println("6. Specialty");
      System.out.println("7. Password");
      System.out.println("8. Return to main menu");
      if (tech.getIsAdmin()) {
         System.out.println("9. Experience level");
         System.out.println("10. Admin status");
         System.out.println("11. Technician ID");
         System.out.println("12. Employment status");
      }
   }
   
   public void readEmployeeMenu(Technician tech, Statement stmt) {
      this.divider();
      loggedInUserInfo(tech, stmt);
      System.out.println("Employee viewing menu");
      System.out.println("Enter a number or q to quit:");
      System.out.println("1. View all employees");
      System.out.println("2. View detailed employee info by ID");
      System.out.println("3. Return to main menu");
   }
   
   public void readTechnicianMenu(Technician tech, Statement stmt){
      this.divider();
      loggedInUserInfo(tech, stmt);
      //running out of time so some menus have fewer features than I wanted to make
      System.out.println("Technician viewing menu");
      System.out.println("Enter a number or q to quit:");
      System.out.println("1. View all technicians");
      System.out.println("2. View detailed technician info by ID");
      System.out.println("3. Return to main menu");
   }
   
   //admin CRUD features are not finished yet
   
   public void adminMainMenu(Technician tech, Statement stmt) {
      this.adminDivider();
      loggedInUserInfo(tech, stmt);
      System.out.println("Administrator main menu");
      System.out.println("Admins can do the same stuff as regular technicians,");
      System.out.println("but with no limits placed on CRUD operations for all tables.");
      System.out.println("Admin menu options require knowing the ID of something.");
      System.out.println("For looking things up where you don't know its ID number,");
      System.out.println("try using the regular technician menu.");
      System.out.println("Enter a number or hit q to quit:");
      System.out.println("1. Create menu (admin only)");
      System.out.println("2. Read menu (admin only)");
      System.out.println("3. Update menu (admin only)");
      System.out.println("4. Delete menu (admin only)");
      System.out.println("5. View regular technician menu options (searching, sorting, lists, etc)");
   }
   
   public void adminCreateMenu(Technician tech, Statement stmt) {
      this.adminDivider();
      loggedInUserInfo(tech, stmt);
      System.out.println("Administrator create menu");
      System.out.println("1. Create a technician");
      System.out.println("2. Create an employee");
      System.out.println("3. Create a standard procedure");
      System.out.println("4. Create a ticket");
      System.out.println("5. Return to main admin menu");
   }
   
   public void adminReadMenu(Technician tech, Statement stmt) {
      this.adminDivider();
      loggedInUserInfo(tech, stmt);
      System.out.println("To read stuff, please use the regular technician menu. They can read all 4 tables in this system.");
      //running out of time before I have to submit the final project
      //so this wasn't done as well as it should be
      System.out.println("Return to main admin menu");
   }
   
   public void adminUpdateMenu(Technician tech, Statement stmt) {
      this.adminDivider();
      loggedInUserInfo(tech, stmt);
      System.out.println("Administrator update menu");
      System.out.println("1. Update technician");
      System.out.println("2. Update employee");
      System.out.println("3. Update standard procedure");
      System.out.println("4. Update ticket");
      System.out.println("5. Return to main admin menu");
   }
   
   public void adminDeleteMenu(Technician tech, Statement stmt) {
      this.adminDivider();
      loggedInUserInfo(tech, stmt);
      System.out.println("Administrator delete menu");
      System.out.println("1. Delete technician");
      System.out.println("2. Delete employee");
      System.out.println("3. Delete standard procedure");
      System.out.println("4. Delete ticket");
      System.out.println("5. Return to main admin menu");
   }
   
}