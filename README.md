AnyPresence-Android-UI-Library
==============================

A UI library with ListViews, Spinners, and Activities that tie into AnyPresence Android SDK.

Getting Started:

1) Add this as a library to your project. This library depends on the Android Support Library (appcompat) which you can download with the SDK Manager.

2) Download the no-dependencies jar from the designer. Add it to /libs for your project.

Sample:

Included is a sample that goes over several of the basic Activities and Fragments included. It's commented with the assumption that you've never coded for Android before but that you're familiar with or can read Java.

The sample requires an SDK with the following object:

Room
-id [String]
-group_id [String]
-name [String]
-desc [String]
-password [String]
-date [Date]

When you toss your SDK into /libs, make sure to modify MainActivity.onCreate() with your server's web address. By default, it's hardcoded to look at calm-garden-7313.