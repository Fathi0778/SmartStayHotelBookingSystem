# SmartStay Hotel Booking System

Java Swing desktop application for the CI6115 Programming III coursework.

## Requirements
- Apache NetBeans IDE 12.6
- JDK 8, 11 or 17 (the code uses Java 8 features only)
- No external libraries

## Opening the project in NetBeans 12.6
1. File > New Project > Java with Ant > Java Application > Next.
2. Project Name: `SmartStayHotelBookingSystem`. Untick "Create Main Class". Finish.
3. Copy the `smartstay` folder from this download's `src` folder into the new
   project's `src` folder (or create each package and class yourself and paste
   the code - see the documentation for the step-by-step order).
4. Right-click the project > Properties > Run > Main Class: `smartstay.MainApplication`.
5. Press F6 (Run Project).

## First run
- Eight sample rooms are created.
- Click **Administrator Login**: a one-time setup asks you to create the
  administrator account and shows an **administrator recovery code** once.
  Save it - it is needed if the admin password is forgotten.
- Guests create their own accounts and also receive a recovery code once.

## Data files
Saved in the `data` folder inside the project folder (created automatically):
`administrators.txt`, `guests.txt`, `rooms.txt`, `reservations.txt`.
Delete the `data` folder to start again from a clean state.

## Packages
| Package | Purpose |
|---|---|
| `smartstay` | `MainApplication` - starts the program |
| `smartstay.model` | Data classes: User, Administrator, Guest, Room, Reservation, Invoice, enums |
| `smartstay.service` | Business rules: login, registration, rooms, reservations, invoices, password recovery |
| `smartstay.security` | Password hashing, session, access control, reset tokens |
| `smartstay.storage` | `DataStore` - loads and saves text files |
| `smartstay.util` | Booking rules, invoice calculator, merge sort, input checks, formatting |
| `smartstay.ui` | All Java Swing screens |

## UML
`uml/SmartStay_ClassDiagram.puml` - replace every `KUID` with your student ID,
then render it at https://www.plantuml.com/plantuml.
