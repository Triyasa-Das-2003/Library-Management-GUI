# Library Management GUI

A simple, standalone desktop application for managing books, members, and transactions in a small library, built with Java and the Swing GUI toolkit.

*(**Note:** Replace the line above with an actual screenshot of your application after you upload it to the repository.)*

---

## ✨ Features

* 📚 **Book Management:** Add new books with a unique ID, title, and author. View all books and their current status (Available/Issued) in a clear table.
* 👥 **Member Management:** Register new library members with a unique ID and name. View all registered members.
* 🔄 **Transaction Handling:** Easily issue books to members and process book returns using their respective IDs.
* 💰 **Fine Calculation:** Automatically calculates fines for overdue books at a rate of ₹1 per day.
* 💾 **Data Persistence:** All library data (books, members, and transactions) is automatically saved to a local file (`library_data.dat`) upon closing the application and reloaded on startup.
* 📊 **Reporting:** Generate simple reports to view all currently issued books and all overdue books.

---

## 🛠️ Technology Stack

* **Language:** **Java**
* **UI Toolkit:** **Java Swing** for the graphical user interface.
* **Data Storage:** Java Serialization for saving application state to a local binary file.
