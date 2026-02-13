# ⌬ ChemSolve - Final Year Project

ChemSolve is an Android mobile application that converts images of hydrocarbon structures into chemical representations and interactive 3D molecular models. The system combines computer vision, machine learning inference and real-time 3D rendering through a client-server architecture.

The goal of the project is to demonstrate an end-to-end pipeline from image acquisition to chemical interpretation and visualization on mobile devices.

## 📋 Overview

How it works - Pipeline:
1. User captures or selects an image of a hydrocarbon structure
2. Image is sent to a Flask backend via multipart POST request
3. Backend performs inference and structure reconstruction
4. Backend returns JSON containing:
   * SMILES string
   * Molecular formula
   * SDF representation
5. Android app:
   * Displays textual results
   * Saves scan to local database
   * Loads SDF into Unity for real-time 3D rendering
  
The application demonstrates integration between mobile development, machine learning inference, and 3D visualization.

## System Architecture

### 📱 Mobile Client (Android)

* Java-based Android application
* Image capture via camera or gallery
* REST-based client–server communication using OkHttp
* Local persistence using Room database
* Integrated Unity engine for in-app real-time 3D molecular rendering

### 🧠 Backend Server (Python)

* Flask REST API for image ingestion
* Image-to-structure inference pipeline
* Image → SMILES conversion
* Generation of SDF files for 3D reconstruction
* Stateless request handling for scalability

### 🧪 3D Visualization (Unity)

* Runtime parsing of SDF molecular data
* Dynamic generation of atoms and bonds
* Real-time rendering embedded inside Android activity
* Automatic teardown and reload of molecular scenes between scans

## 📚 Technical Stack

#### Mobile
* Android (Java)
* Room Database
* OkHttp
* Unity (embedded)

#### Backend
* Python
* Flask
* RDKit
* REST API

#### Visualization
* Unity 2021 LTS
* OpenGL ES 3.0
* Custom SDF parser (C#)

## 🎬 DEMO

### Features demonstrated:

* Image scan → chemical interpretation
* Live SMILES and formula generation
* In-app 3D molecular visualization
* Persistent scan history

[Watch demo video](https://github.com/yuryk200/ChemApp-Final-Year-Project/blob/main/chemapp/assests/1684.mp4)


## 🔥 Updates

### Latest Changes

* Added dedicated Recent and Saved pages
* UI Imporvements
* Automatic 3D model refresh on new scan
* Loading overlay added during backend processing
* Removed overlapping molecule instances
* Improved Android–Unity synchronization
* Added gallery image selection in addition to camera capture

#### UI Improvements
Gave the UI a fresh coat of paint and implemented quality of life improvements.

##### Before Scan

| <img src="chemapp/assests/Screenshot_20230626-041550_chemsolve2.jpg" width="200">| <img src="chemapp/assests/UpdatedUI.PNG" width="215" height="410">| 
| :------------------------------------------------------------------------------: | :---------------------------------------------------------------: |
|                                     Old UI (v1.0)                                |                    New UI (v2.0)                                  | 

##### After Scan

| <img src="chemapp/assests/Screenshot_20230626-041538_chemsolve2.jpg" width="200">|<img src="chemapp/assests/UpdatedUI2.PNG" width="215" height="410">|
| :------------------------------------------------------------------------------: | :---------------------------------------------------------------: |
|                                     Old UI (v1.0)                                |                    New UI (v2.0)                                  |

##### New Unity Instance
Instead of you having to press the "Show 3D Object" and it taking you to a seperate Unity instance to show the chemical structure, it wow whenever you scan, opens a small unity instance in the results box to show the 3D structure and updates with each scan

| <img src="chemapp/assests/Screenshot_20230626-041748_chemsolve2.jpg" width="220" height="410">|<img src="chemapp/assests/UpdatedUI2.PNG" width="215" height="410">|
| :-------------------------------------------------------------------------------------------: | :---------------------------------------------------------------: |
|                                     Old UI (v1.0)                                             |                    New UI (v2.0)                                  |

#### Dedicated Recent and Saved pages
Added a recent page to store recent scans and made a saved page to store saved scans, each stored scan can then be viewed again on main app page.

| <img src="chemapp/assests/UIRecent.PNG" width="215" height="410">|<img src="chemapp/assests/UISaved.PNG" width="215" height="410">|
| :--------------------------------------------------------------: | :------------------------------------------------------------: |
|                                     Recent Page                  |                    Saved Page                                  |







 Welcome to Chemsolve, in this repo you can download the android app chemsolve and the machine learning model im2smilesv2.

The App take pictures of Hydrocarbons you want to classify, the hydrocarbon is then sent to the server running the ML model which will send back the information about the hydrocarbon back to the app.

Once the information is retrieved you can get the app to display a 3D rendering of the hydrocarbon you sent to be scanned, process shown in images below.

![Screenshot_20230626-041550_chemsolve2](https://github.com/yuryk200/chemsolve/assets/82842394/ca8d889d-f051-436d-b3ed-960f71b5212c)
![Screenshot_20230626-041538_chemsolve2](https://github.com/yuryk200/chemsolve/assets/82842394/5d675df0-7a67-4d62-8d2b-14c9898b65f2)
![Screenshot_20230626-041748_chemsolve2](https://github.com/yuryk200/chemsolve/assets/82842394/3ae3217a-10ff-4a79-ba58-d8efdd2223cc)

#Note

For this to work you need to be running the server.py script in the folder im2smilesv2, I usually run this in Visual Studio. For the server to run and be fully functional you need to set on the python environment to the file in FYPFinal PythonEnviroment.yaml, otherwise the ML model won’t work.

Also you need an OpenVPN account on your phone and computer and have them both active when using app and server and also in the android app it is important to change the url IP to the IP of the OpenVPN running on the laptop/computer as this can change everytime you turn on OpenVPN, process for this shown below.

Use this IP in OpenVPN
![Picture1](https://github.com/yuryk200/chemsolve/assets/82842394/3d2054a7-1b34-48d9-a995-315feba1eb7c)

In this URL in the chemsolve app
![Picture2](https://github.com/yuryk200/chemsolve/assets/82842394/90ff6d4c-167f-4f5e-8f34-71c3fdb658e9)
