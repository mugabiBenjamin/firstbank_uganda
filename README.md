# README

- [README](#readme)
  - [Project Structure](#project-structure)

## Project Structure

```plain
firstbank_uganda/
├── .gitattributes
├── .gitignore
├── README.md
├── pom.xml
├── data/
│   └── firstbank.accdb
└── src/
    └── main/
        └── java/
            └── ug/
                └── firstbank/
                    ├── MainApp.java
                    ├── model/
                    │   ├── Account.java              (abstract)
                    │   ├── SavingsAccount.java
                    │   ├── CurrentAccount.java
                    │   ├── FixedDepositAccount.java
                    │   ├── StudentAccount.java
                    │   ├── JointAccount.java
                    │   └── AccountRecord.java        (plain data carrier / DTO)
                    ├── validation/
                    │   ├── FormValidator.java        (orchestrates all rules)
                    │   ├── ValidationResult.java     (holds field→error map)
                    │   └── NinValidator.java         (CM/CF regex, isolated)
                    ├── persistence/
                    │   ├── DatabaseManager.java      (connection, schema bootstrap)
                    │   ├── AccountRepository.java    (insert, duplicate check, lookup)
                    │   └── SequenceGenerator.java    (per-branch-year counter)
                    ├── service/
                    │   └── AccountService.java       (ties validation + persistence, bcrypt)
                    ├── ui/
                    │   ├── MainWindow.java           (root Stage/Scene builder)
                    │   ├── AccountFormPane.java      (the full form, fields + combos)
                    │   ├── SummaryPane.java          (read-only summary area)
                    │   ├── LookupDialog.java         (search by NIN / account number)
                    │   └── AboutDialog.java          (Help → About)
                    └── util/
                        ├── CurrencyFormatter.java    (UGX thousand-separator)
                        ├── DateUtils.java            (leap year, age calc)
                        ├── PasswordUtils.java        (bcrypt wrap)
                        └── PdfExporter.java          (summary → PDF)
```

[Back to Top](#readme)
