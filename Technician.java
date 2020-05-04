import java.sql.*;

class Technician {
   //attributes
   private int technicianID;
   private String firstName;
   private String lastName;
   private String phoneNumber;
   private String emailAddress;
   private int officeLocation;
   private boolean currentlyHired;
   private boolean isAdmin;
   private String specialty;
   private int experienceLevel;
   private String password;
   
   //no arg constructor
   public Technician() {
      this.technicianID = -1;
      this.firstName = "not_loaded";
      this.lastName = "not_loaded";
      this.phoneNumber = "not_loaded";
      this.emailAddress = "not_loaded";
      this.officeLocation = -1;
      this.currentlyHired = false;
      this.isAdmin = false;
      this.specialty = "not_loaded";
      this.experienceLevel = -1;
      this.password = "not_loaded";
   }
   
   //constructor overloading
   //full arg constructor
   public Technician(int technicianID, String firstName, String lastName, String phoneNumber,
   String emailAddress, int officeLocation, boolean currentlyHired, boolean isAdmin, String specialty,
   int experienceLevel, String password) {
      this.technicianID = technicianID;
      this.firstName = firstName;
      this.lastName = lastName;
      this.phoneNumber = phoneNumber;
      this.emailAddress = emailAddress;
      this.officeLocation = officeLocation;
      this.currentlyHired = currentlyHired;
      this.isAdmin = isAdmin;
      this.specialty = specialty;
      this.experienceLevel = experienceLevel;
      this.password = password;
   }
   
   //getters
   public int getTechnicianID() {
      return this.technicianID;
   }
   
   public String getFirstName() {
      return this.firstName;
   }
   
   public String getLastName() {
      return this.lastName;
   }
   
   public String getPhoneNumber() {
      return this.phoneNumber;
   }
   
   public String getEmailAddress() {
      return this.emailAddress;
   }
   
   public int getOfficeLocation() {
      return this.officeLocation;
   }
   
   public boolean getCurrentlyHired() {
      return this.currentlyHired;
   }
   
   //booleans are ints in mysql
   public int getCurrentlyHiredAsInt() {
      if (this.currentlyHired) {
         return 1;
      } else {
         return 0;
      }
   }
   
   public boolean getIsAdmin() {
      return this.isAdmin;
   }
   
   public int getIsAdminAsInt() {
      if (this.isAdmin) {
         return 1;         
      } else {
         return 0;
      }
   }
   
   public String getSpecialty() {
      return this.specialty;
   }
   
   public int getExperienceLevel() {
      return this.experienceLevel;
   }
   
   public String getPassword() {
      return this.password;
   }
   
   //setters
   public void setTechnicianID(int technicianID) {
      this.technicianID = technicianID;
   }
   
   public void setFirstName (String firstName) {
      this.firstName = firstName;
   }
   
   public void setLastName(String lastName) {
      this.lastName = lastName;
   }
   
   public void setPhoneNumber(String phoneNumber) {
      this.phoneNumber = phoneNumber;
   }
   
   public void setEmailAddress(String emailAddress) {
      this.emailAddress = emailAddress;
   }
   
   public void setOfficeLocation(int officeLocation) {
      this.officeLocation = officeLocation;
   }
   
   public void setCurrentlyHired(boolean currentlyHired) {
      this.currentlyHired = currentlyHired;
   }
   
   //booleans are stored as tinyint(1) in mysql
   public void setCurrentlyHiredFromInt(int currentlyHired) {
      if (currentlyHired == 1) {
         this.currentlyHired = true;
      } else {
         this.currentlyHired = false;
      }
   }
   
   public void setIsAdmin(boolean isAdmin) {
      this.isAdmin = isAdmin;
   }
   
   public void setIsAdminFromInt(int isAdmin) {
      if (isAdmin == 1) {
         this.isAdmin = true;
      } else {
         this.isAdmin = false;
      }
   }
   
   public void setSpecialty(String specialty) {
      this.specialty = specialty;
   }
   
   public void setExperienceLevel(int experienceLevel) {
      this.experienceLevel = experienceLevel;
   }
   
   public void setPassword(String password) {
      this.password = password;
   }
   
   //update one attribute from the database to the object
   //so it's synced
   public void updateOne(String columnToUpdate, Statement stmt) {
      //updates FROM the database TO the java client
      String updateColumnQuery = "SELECT " + columnToUpdate + " FROM Technician WHERE technicianID = " + getTechnicianID();
      try {
         ResultSet newColumnRS = stmt.executeQuery(updateColumnQuery);
         if (newColumnRS.next()) {
         String newValue = newColumnRS.getString(columnToUpdate);
         switch(columnToUpdate) {
            case "firstName":
               setFirstName(newValue);
               break;
            case "lastName":
               setLastName(newValue);
               break;
            case "phoneNumber":
               setPhoneNumber(newValue);
               break;
            case "emailAddress":
               setEmailAddress(newValue);
               break;
            case "officeLocation":
               setOfficeLocation(Integer.parseInt(newValue));
               break;
            case "specialty":
               setSpecialty(newValue);
               break;
            case "experienceLevel":
               setExperienceLevel(Integer.parseInt(newValue));
               break;
            case "password":
               setPassword(newValue);
               break;
            default:
               System.out.println("invalid arg");
               break;
         }
      }
      } catch (SQLException e) {
         e.printStackTrace();
      }

   }
   
   //edge-case, as booleans are ints in mysql but bools in java
   //only needed for isAdmin and currentlyHired boolean attributes
   public void updateOneIntToBool(String columnToUpdate, Statement stmt) {
      //updates FROM the database TO the java client
      String updateColumnQuery = "SELECT " + columnToUpdate + " FROM Technician WHERE technicianID = " + getTechnicianID();
      try {
         ResultSet newColumnRS = stmt.executeQuery(updateColumnQuery);
         if (newColumnRS.next()) {
            int newValue = newColumnRS.getInt(columnToUpdate);
            switch(columnToUpdate) {
               case "currentlyHired":
                  if (newValue == 1) {
                     setCurrentlyHired(true);
                  } else {
                     setCurrentlyHired(false);
                  }
                  break;
               case "isAdmin":
                  if (newValue == 1) {
                     setIsAdmin(true);
                  } else {
                     setIsAdmin(false);
                  }
                  break;
               default:
                  System.out.println("invalid arg");
                  break;
            }
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }
   
   //returns out all attributes, useful for testing/debugging
   public String toString() {
      String str = "";
      str += "TechID: " + getTechnicianID() + ". ";
      str += "First name: " + getFirstName() + ". ";
      str += "Last name: " + getLastName() + ". ";
      str += "Phone #: " + getPhoneNumber() + ". ";
      str += "Email: " + getEmailAddress() + ". ";
      str += "Room #: " + getOfficeLocation() + ". ";
      str += "Hired: " + getCurrentlyHired() + ". ";
      str += "Admin: " + getIsAdmin() + ". ";
      str += "Specialty: " + getSpecialty() + ". ";
      str += "Experience: " + getExperienceLevel() + ". ";
      str += "Password: " + getPassword() + ". ";
      return str;
   }
   

   
   
   //update all object attributes from the SQL columns
   //important for consistency in case the technician was changed in the database
   //while the program is still running
   public void updateAll(Statement stmt) {
   //updates FROM the database TO the java client
      try { 
         String checkIfMyAccountStillExists = "SELECT COUNT(technicianID) AS techExists FROM Technician WHERE technicianID = " + getTechnicianID();
         ResultSet accountStillExistsRS = stmt.executeQuery(checkIfMyAccountStillExists);
         if (accountStillExistsRS.next()) {
            int stillExists = accountStillExistsRS.getInt("techExists");
            //if an admin deletes your account while you're logged in, you get logged out
            if (stillExists == 0) {
               System.out.println("Your account has been deleted and now you will be logged out.");
               System.exit(1);
            }
         } else {
            //query response issue
            System.out.println("checkIfMyAccountStillExists error");
            System.exit(1);
         }
         //getting here means your account still exists
         //proceeding to update the Technician object with the latest values from the database
         
         //get most up-to-date firstName value from database and set it as this object's firstName attribute
         updateOne("firstName", stmt);         
         
         //update last name from database
         updateOne("lastName", stmt);  
         
         //update phone number from database
         updateOne("phoneNumber", stmt);
         
         //update email address
         updateOne("emailAddress", stmt);
         
         //update office location
         updateOne("officeLocation", stmt);
         
         //if you get changed from admin to regular technician or vice versa, you get
         //logged out but can log back in, and will have a different menu
         boolean oldAdminStatus = getIsAdmin();
         updateOneIntToBool("isAdmin", stmt);
         if (oldAdminStatus != getIsAdmin()) {
            System.out.println("Your account type has changed. You will need to log back in");
            System.exit(1);
         }
         
         //update currentlyHired
         updateOneIntToBool("currentlyHired", stmt);
         //if you get fired by an admin while you're logged in, you get logged out
         //and can't log back in
         if (!getCurrentlyHired()) {
            System.out.println("Your technician account has been disabled.");
            System.exit(1);
         }
         



                  
         //update specialty
         updateOne("specialty", stmt);
         
         //update experience leve
         updateOne("experienceLevel", stmt);
         
         //update password
         updateOne("password", stmt);
         
      } catch (SQLException s) {
         s.printStackTrace();
      }
   }
   
   
}
