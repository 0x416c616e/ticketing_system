# IT Ticketing System

## About

This was a final project for a class about databases.

A ticketing system allows IT technicians of a company to keep track of requests non-IT employees make for tech support. This program is written in Java and needs to connect to a MySQL database server. 

I tested this in jGRASP on Windows 10 with Java 14.01 installed. I ran the program directly from jGRASP rather than the command prompt or something. Run Main.java.

SQL commands are located in sql_commands.txt. 

## Instructions

1. Run the table creation commands in phpMyAdmin or the mysql> prompt.
2. Run the table population commands in phpMyAdmin or the mysql> prompt.
3. Run the view creation commands in phpMyAdmin or the mysql> prompt.
4. Compile Main.java, Technician.java, and MenuPrinter.java.
5. You will need to change the configuration for the database, user, and password to connect. See the “Credentials.txt and the initial setup” section in this document for more info. But it’s easiest if you just leave it the way it currently is.
6. Run Main.java in jGRASP
7. Log in with a technicianID and password from the data in the table population commands. If an account’s currentlyHired value is 0, it will not be allowed to log in, because that means the employee was fired and their account is disabled.

## Terminology and overview

**Who uses the ticketing system software:** only technicians and administrators deal with the ticketing system software.

**How employees make tickets:** they call the IT department on the phone or go to the IT office in person. There is no digital ticketing form here, though some real-world ticketing systems allow employees to fill out a form to make a ticket that way.

**Employee:** a non-IT employee at a company who will need tech support every now and then.

**Technician:** someone who can provide support to non-IT employees.

**Regular technician:** a technician with limited privileges. They can only edit their own stuff, and can’t make or delete employees, technicians, or standard procedures. They can make new tickets, edit or delete their own tickets, and edit some of their own information. Some of their own info, like admin status or hired status, cannot be edited. That can only be changed by an admin.

**Administrator:** a technician who has the ability to do anything in the ticketing system. No limits on making/editing/viewing/deleting things.

**Ticket:** a request for support. It says what the issue is, who requested it, who is responsible for working on it, and what standard procedure is suitable.

**Assigned vs. unassigned tickets:** when a technician is responsible for doing the work to finish a support ticket, they are assigned to it. A ticket that nobody is working on is called an unassigned ticket. A technician is allowed to create an unassigned ticket because they need to make a ticket for an employee when they request it, even if the technician is currently unable to figure out which technician would be most suitable for it. So you make the ticket immediately, then figure out who should be assigned to it later.

**Standard procedure:** step-by-step instructions for solving a common tech issue that comes up a lot.

**Open ticket:** an unfinished tech support request.

**Closed ticket:** a finished tech support request.

**Deleted:** in a ticketing system, you don’t usually delete closed stuff. But it’s an option in this program anyway. Finished tickets are still important for historical purposes though.

**Deleted account vs. fired account:** this IT ticketing system allows for disabling employee and technician accounts without deleting them. A fired employee is unable to make support requests and a fired technician or admin is unable to log in. Because of how often the program checks stuff, if a technician is logged in and then gets fired, they will get logged out the next time they go back to the main menu.

**“Deleted” placeholders:** a ticket will have 1 to 3 things associated with it: employee who requested it (mandatory), technician responsible for finishing the ticket (optional), and a standard procedure (optional). If something gets deleted, but they are still referenced in a ticket (as foreign keys), it gets swapped out for a placeholder ID instead. 
Employee ID 1 is reserved for deleted employees. Technician ID 1 is reserved for deleted technicians. Standard procedure ID 1 is reserved for deleted procedures. If a ticket is associated with an employee whose account gets deleted, its assignedEmployeeID will be set to 1, which indicates that the employee associated with the ticket is deleted.

**Credentials.txt and the initial setup:** credentials.txt is a file that stores the database name, username, and password to connect to the server. If the credentials haven’t been set up, the program will prompt the user to input this information. A blank file called finished_setup.txt is basically a boolean. It means the program’s setup has been completed and it no longer needs to ask the user for credentials for connecting to the server. 

The database name, username, and password will be stored in the credentials.txt file after an initial setup has been completed. If you want to reset the program, then delete the finished_setup.txt file and then clear the contents of credentials.txt. The next time you run the program, it will ask you to fill out the connection information.

**Login system:** in addition to the MySQL credentials for connecting to the database, you can only use the program if you enter in a technician ID and password. These are in the SQL queries that populate the tables. I recommend trying the program with one non-admin technician account and one admin technician account. For example, technician ID 2 and technician ID 9. You will notice that the regular account has more limited options compared to the admin. I realize that this is client-side authentication, which, in the real world, is not secure. But this is only because of the limitations of this project.

**Security limitations:** in a real-world ticketing system, in addition to a remote database, there would be back-end code that would deal with authentication, server-side rather than client-side. I also stored the technician passwords in plaintext, which is also not secure. If I had more time, I would do password hashing. My program might also be vulnerable to SQL injection or arbitrary code execution, though I did make some simple escaping/unescaping of single quotes and double quotes. For a real-world ticketing system, it would need to use prepared statements for the SQL queries, as well as a web application firewall.

**Menus:** to use the menus, most of the time you type a number and hit enter. Sometimes you can hit the letter q to quit, or sometimes even c to cancel something. Sometimes you will have to enter data into the program when prompted, depending on the menu.

**Regular technician menu:** limited options.

**Admin menu:** can do anything, but can also access the regular technician menu. I ran out of time towards the end so the admin menu options have less input validation than the regular technician stuff.

**Main.java:** main method as well as some methods relating to different menu options.

**Technician.java:** the logged in technician’s information gets retrieved from the database and put into a Technician object. It gets refreshed every now and then with Technician.updateAll();

**MenuPrinter.java:** in order to de-clutter the Main class, I put some menu methods in the MenuPrinter class. It’s mostly just print statements with lists of menu options.

**4 tables:** Employee, Technician, StandardProcedure, and Ticket.

**General lists vs. detailed individual info:** when you want to view many records at the same time, it only shows certain fields, because it can’t fit everything on a single line. If you want to view detailed info about something, you will have to use the menu option to view it individually, based on ID. Viewing something individually will show all the fields, not just some of them.

**Time limitations:** I made lots of features for the regular technician menu, but ran out of time towards the end. I finished the project, but the admin menu options are a little more minimal than the regular technician ones. But admins can still create, read, update, and delete any of the 4 entities in the system (tickets, employees, technicians, and procedures).
