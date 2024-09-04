# Android HCE Beginner App

## Host-based card emulation overview

I will quote a lot of times from this official document, see the full document here: 
https://developer.android.com/develop/connectivity/nfc/hce

*Android 4.4 and higher provide an additional method of **card emulation** that doesn't involve a secure element, 
called **host-based card emulation**. This allows any Android application to emulate a card and talk directly 
to the NFC reader. This topic describes how host-based card emulation (**HCE**) works on Android and how you 
can develop an app that emulates an NFC card using this technique.*

## Purpose of this App

This app explains how to setup an application that runs the **Host-Based Card Emulation (HCE)** feature on an 
Android device. I'm describing step by step what requirements are to meet to transfer some very simple data. 
**There will be no special features like emulating a complete NFC card**, but in the end you will understand what the 
steps are to write your own app.

This repository is accompanied by a tutorial on medium.com, please find the article here: ... will follow soon, please be patient

## Requirements to follow the tutorial

As our Android device will act as a real NFC tag, you will need a **second NFC-Reader** (e.g. a second Android device) to run the tests, 
because when an Android device is in HCE mode it cannot read "itself" (on the same device). Unfortunately, not all Android devices, 
even with an NFC reader, are enabled for HCE, so you need to check that your device is capable to run HCE. You can run a simple check 
using "**FEATURE_NFC_HOST_CARD_EMULATION**" to verify that you can run HCE. 

The minimum SDK version for this app is Android 5 (SDK 21), and your device needs to have an NFC reader on board.

This app is developed using Android Studio 2024.1.1 and I'm using the Java Runtime version: 17.0.11+0-17.0.11b1207.24-11852314 aarch64 
on MacOS/Silicon.

## Most important facts

### Application Identifier (AID)

I'm receiving a lot of questions like *"why does my HCE implementation does not work and is not recognized by another device ?"*. 
In most of the cases the reason for the failure is that both devices use a different **Application Identifier" ("**AID**"). In the 
end, that means they speak "different languages" and don't understand each other.

From Wikipedia: *An application identifier (AID) is used to address an application in the card or Host Card Emulation (HCE) if 
delivered without a card. An AID consists of a registered application provider identifier (RID) of five bytes, which is issued 
by the ISO/IEC 7816-5 registration authority. This is followed by a proprietary application identifier extension (PIX), which 
enables the application provider to differentiate among the different applications offered. The AID is printed on all EMV 
cardholder receipts. Card issuers can alter the application name from the name of the card network.*

**Registered Application Identifier**: You cannot choose an Application identifier (AID) on your own because then you may get 
trouble when working with established reader infrastructure. There are three groups of AIDs:
- Category "A": That are international registered AIDs, mainly for Payment Provider like Mastercard or Visa Card. Please don't choose an AID from this category !
- Category "D": That are national registered AIDs, e.g. for Payment Provider like local bank cards or for accessing NFC tags. Please don't choose an AID from this category !
- Category "F": That are proprietary (non registered) AIDs, e.g. for this application.

Please find a very extensive list of existing AID's for payment cards here: https://www.eftlab.com/knowledge-base/complete-list-of-application-identifiers-aid

Please do not use AIDs with a length less than 5 as they won't get routed on Android devices properly. The maximum length of 
an AID is 16 bytes (see https://stackoverflow.com/a/27620724/8166854 for more details).

This app uses the AID **F2233445566**.

### Run a HCE services in the background

Most people believe, that the emulated tag is run by your app, but that is not true. Of course, you need to run an app for the first 
time to start a (HCE) service in the background, but from now on all communication is done between your service and the remote NFC remote 
reader. Your app will get no further updates about the communication or and information that data is exchanged. You need to implement 
an interface to get informed about any updates. For that reason it not very easy to monitor your running app from "inside" your app.

Your app will still persistent to work, even when you close your app with the Android Taskmanager - because it is a service.

### Payment and other applications

If you are running the Google Wallet app on your device you know that your device is HCE capable, because Google Wallet emulates a 
**Payment Card** (like Credit Cards). Those applications are called **Payment applications** and usually a device should run only one payment 
application for emulating. For that reason we will design our app as "other application" HCE type to avoid any conflicts with existing 
payment apps.

### HCE and security

From the documentation: *The HCE architecture provides one core piece of security: because your service is protected by the **BIND_NFC_SERVICE** 
system permission, only the OS can bind to and communicate with your service. This ensures that any APDU you receive is actually an APDU that 
was received by the OS from the NFC controller, and that any APDU you send back only goes to the OS, which in turn directly forwards the APDUs 
to the NFC controller.*

*The last remaining concern is where you get your data that your app sends to the NFC reader. This is intentionally decoupled in the HCE design; 
it does not care where the data comes from, it just makes sure that it is safely transported to the NFC controller and out to the NFC reader.*

*For securely storing and retrieving the data that you want to send from your HCE service, you can, for example, rely on the Android Application 
Sandbox, which isolates your app's data from other apps. For more details about Android security, read Security tips 
(https://developer.android.com/training/articles/security-tips).* 

### What is the application protocol data unit (APDU) ?

You will notice that "**APDU**" is written in many tutorials and source codes. The APDU protocol is the way how an NFC-Reader and NFC-Smart Card are 
going to communicate, it is an **APDU message command-response pair**, consisting of two pieces:

a) The NFC-Reader is sending data by sending a **command APDU** to the NFC Smart Card

b) The  NFC Smart Card answers the  command by sending a **response APDU** to the NFC Reader.

In our case, our HCE app needs to response to a command APDU as our app is acting like a real NFC Smart Card. As each NFC Smartcard follows a card 
specific protocol it is important that our HCE app acts like the same. If not - the real NFC Reader will think "that is not the NFC tag I expected" 
and will stop any further communication with out app.

## Steps to create a HCE application

These are the basic steps you need to implement a HCE application on your device:

1) Create a HCE service class that is extending the **HostApduService**
2) Register your HCE service in AndroidManifest.xml
3) Create an XML-file in your resources that defines the application identifier your HCE application in working on
4) Register the XML-file in AndroidManifest.xml to link the AID with your own HCE service

**That's all ?** - yes we don't need more steps to build a HCE application.



