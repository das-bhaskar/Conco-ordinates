<p align="center">
  <img width="421" height="210" alt="Logo" src="https://github.com/user-attachments/assets/c0dcf8a8-70f9-4310-9e07-8edf45079b4e" />
</p>

<p align="center">
  <strong>App Name:</strong> 📍 Conco-ordinates
</p>

<p align="center">
  <strong>Course:</strong> SOEN 390 – Software Engineering Team Design Project
</p>

<p align="center">
  <strong>Semester:</strong> Winter 2026
</p>

<p align="center">
  <strong>Group Name:</strong> IRS
</p>

---
## 👥 Team Members

| Name                    | Student ID    | GitHub Username     | Role          |
|-------------------------|---------------|---------------------|---------------|
| Bhaskar Das             |   40325270    | bhaskar             |  Fullstack             |
| Kevin Kerr              |   40281044    | Kevin K.            |  Backend             |
| Ahmad Al Habbal         |   40261029    | ham340i             |               |
| Mathieu Pare            |   40261757    | Mathieu2003         |               |
| Bhumika Bhumika         |   40223877    | bhumi-0902          |               |
| Robert Craciunescu      |   40282245    | Robert Craciunescu  |               |
| Abd Al Rahman Al Kabani |   40247395    | aboudka2003         |               |
| Omar Ghazaly            |   40289795    | HersheyWaffle       |               |
| Nasib Guma              |   40283693    | Nasib Guma          |               |
| Matthew Kazemie         |   40190450    | Kazemie             | 3d specialist              |
| Yifu Li                 |   40286100    | YifuLi-blip         |               |
| Dmitrii Cazacu          |   40314501    | Hildthelsta         |               |
---

## 📖 Project Description

**Conco-ordinates** is a campus navigation and guidance web application designed to help Concordia University students navigate efficiently across both SGW and Loyola campuses. The system provides outdoor and indoor directions, integrates academic schedules, and supports accessibility-aware routing. By combining interactive maps, real-time navigation, and institutional data, the application aims to improve students’ daily campus experience.

---

## 🎯 Project Objectives

- Explore detailed maps of SGW and Loyola campuses  
- Provide outdoor navigation between buildings  
- Support indoor navigation within buildings and across floors  
- Generate directions to a student’s next class based on time and location  
- Highlight indoor and outdoor points of interest  
- Support accessibility-friendly routing  

---

## 👤 User Roles

- Students  
- General campus visitors  
- System administrators (future scope)

---

## 🛠 Languages & Technologies

- **Platform:** Native Android (Mobile)
- **Primary Language:** Kotlin
- **Frontend/UI:** **Jetpack Compose** (Declarative UI) with Material Design 3 (M3)
- **Architecture:** **MVVM (Model-View-ViewModel)**
- **Data Management:** **JSON-based** persistence with **Gson** serialization
- **APIs & Services:** - **Google Maps SDK for Android** (Compose-integrated map rendering)
  - **Google Maps Utility Library** (PolyUtil for advanced geofencing & distance logic)
  - **FusedLocationProviderClient** (Real-time GPS tracking)
  - **Google Calendar API** (Class schedule integration)
- **Testing & CI/CD:** - **JUnit 4** for unit testing logic and viewmodels
  - **SonarCloud** for automated code quality and coverage analysis
  - **GitHub Actions** for continuous integration and automated build pipelines

---

## 📂 Project Architecture

The project follows a reactive **MVVM** pattern, ensuring a clean separation between UI, logic, and data layers to maximize testability and maintainability.

---

## ⭐ Key Features

- Interactive campus maps for SGW and Loyola  
- Campus switching via toggle  
- Outdoor navigation with multiple transportation modes  
- Concordia shuttle service support  
- Directions to next class using calendar data  
- Indoor navigation with shortest-path routing  
- Accessibility-aware directions  
- Highlighted points of interest  

---

## 🚀 Project Goal

The goal of **Conco-ordinates** is to deliver a functional prototype demonstrating intelligent campus navigation, real-time direction generation, and seamless integration with academic data systems, while following Agile software engineering practices.

---

