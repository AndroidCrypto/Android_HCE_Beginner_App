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

This repository is accompanied by a tutorial on medium.com, please find the article here: https://medium.com/@androidcrypto/how-to-use-host-based-card-emulation-hce-in-android-a-beginner-tutorial-java-32974dd89529

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

This app uses the AID **F22334455667**.

### Run a HCE services in the background

Most people believe, that the emulated tag is run by your app, but that is not true. Of course, you need to run an app for the first 
time to start a (HCE) service in the background, but from now on all communication is done **between your service and the remote NFC reader**. 
Your app will get no further updates about the communication or and information that data is exchanged. You need to implement 
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

### ISO 7816-4 Section 6 - Basic Interindustry Commands

To understand the commands between an NFC reader and a tag (real or emulated one) you should get familiar with the ISO 7816-4 commands. 
In this tutorial I'm using just 3 of them:

**SELECT APPLICATION APDU**: The "Select Application" APDU is the starting point for every communication with an emulated (HCE driven) NFC tag. 
If you miss to send this command your HCE tag won't recognize the request and will not answer or react in any way. The second fact is: as I'm 
using a proprietary AID for this app an NFC reader like the TagInfo app by NXP does not know about this specific AID you cannot read the HCE 
tag with regular NFC reader apps. The "Select Application APDU" for the HCE emulated tag of the Beginner App is "00A4040006F2233445566700h". 
To understand the response see the chapter APDU Responses.

**GET DATA APDU**: The "Get Data" APDU is used to request data from the tag. In most more complex NFC tags the data is organized within files 
like the one you know from your home computer. The file system is organized by using file numbers that are part of the command:

```vplaintext 
Get Data APDU: 00ca0000010100
I'm splitting the bytes into their meaning (all data are hex encoded):

00: CLA = Class of instruction, length 1 byte, the start of the command sequence
CA: INS = Instruction, length 1 byte, is the GET DATA command
00: P 1 = Selection Control 1, length 1 byte, in range '0000'-'003F' it is RFU
00: P 2 = Selection Control 2, length 1 byte, in range '0000'-'003F' it is RFU
01: LC Field: Length of the following data, length 1 byte, number of following data bytes
01: Data: Data send together with the command, here the 1 byte long file number
00: Le field: Empty or maximum length of data expected in response
```
The "Get Data" APDU for reading the content of file 01 from the HCE emulated tag of the Beginner App is "00ca0000010100h". To understand 
the response see the chapter APDU Responses.

**PUT DATA APDU**: The "Put Data" APDU is used to send data to the tag. In most more complex NFC tags the data is organized within files 
like the one you know from your home computer. The file system is organized by using file numbers that are part of the command, together 
with data that gets stored:

```plaintext
PUT DATA APDU: 00da00001d024e65..303200
I'm splitting the bytes into their meaning (all data are hex encoded):

00: CLA = Class of instruction, length 1 byte, the start of the command sequence
DA: INS = Instruction, length 1 byte, is the PUT DATA command
00: P 1 = Selection Control 1, length 1 byte, in range '0000'-'003F' it is RFU
00: P 2 = Selection Control 2, length 1 byte, in range '0000'-'003F' it is RFU
1d: LC Field: Length of the following data, length 1 byte, number of following data bytes
1dh = 29 bytes of data following
01: Data: Data send together with the command, here the 1 byte long file number (here file 02)
4e65..3032: Data: Data send together with the command, here the 28 bytes long data
00: Le field: Empty or maximum length of data expected in response
```

In this command the data consists of 2 single data fields the 1 byte long file number and the data (here 28 bytes). I'm sending this string:

```plaintext
dataToWrite in UTF-8 encoding: New Content in fileNumber 02
dataToWrite in hex encoding: 4e657720436f6e74656e7420696e2066696c654e756d626572203032
```
The "PUT DATA" APDU for the HCE emulated tag of the Beginner App is "00da00001d024e657720436f6e74656e7420696e2066696c654e756d62657220303200h". 
To understand the response see the chapter APDU Responses.

**APDU Responses**: To understand the APDU response we need to divide all commands in one of the two categories:

- send the command only or send the command together with data (e.g. PUT DATA APDU)
- send the command (together with specifying parameter like the file number) and receive data in return (e.g. GET DATA APDU).

- Send the command only or with data: If the card accepts the command it simply answers with a 2 bytes long code: "9000h":

```plaintext 
OK-Response: 9000h
90 = SW 1 = Status byte 1 = Command processing status
00 = SW 2 = Status byte 2 = Command processing qualifier
```

If the bytes are "9000h" the command was "Accepted". Every other response should be treated as "Not Accepted". The ISO can define a more 
specified response for different cases, but in our case we just check for "9000h" or not "9000h".
 
- Send the command and receive data in return: In this case the returned data is a little bit different. In case of command acceptance we 
- receive the data, concatenated with the two Status bytes. This is the sample response to the "Get Data" APDU:

```plaintext 
Get Data APDU: 00ca0000010100
Response: 48434520426567696e6e65722041707020319000
The complete response is 20 bytes long:
Data (18 bytes):  48434520426567696e6e6572204170702031
Status Bytes 1+2: 9000
The Data is this String: HCE Beginner App 1
```

In case of success we get the data, followed by the 2 bytes long Status bytes. In case of failure (e.g. wrong command length) the tag answers 
with "0000h". The Beginner app knows a third response case: in case the file number does not exist (e.g. file number 3) the tag is returning 
the string "HCE Beginner App Unknown", followed by the OK-Status bytes "9000h". Every other response should be treated as "Not Accepted". The 
ISO can define a more specified response for different cases, but in our case we just check for "9000h" or not "9000h".

For more details about ABDU's see: https://cardwerk.com/smart-card-standard-iso7816-4-section-6-basic-interindustry-commands/

PUT DATA command
```plaintext
PUT DATA command APDU
CLA	        00h
INS	        DAh
P1          00h
P2	        00h
Lc field	Length of the subsequent data field
Data field	Parameters and data to be written
Le field	Empty
```

## UID of an emulated NFC tag

If you run my app you will notice that I'm exposing the tag UID in the Logfile within the Reader app. This 
**UID is exact 4 bytes long and random**. This is a security feature to prevent privacy to the user - this 
way a large NFC reader infrastructure (think of a "Smart City" with hundreds of NFC readers) is not being 
able to track a user (better: his NFC card). There is no workaround to change the behaviour of a modern 
Android device to get a static UID each time you tap the emulated tag to an NFC reader, sorry.

The only way would be to store the UID in a "read only" file on the tag to get a unambiguously tag identification.

## Steps to create a HCE application

These are the basic steps you need to implement a HCE application on your device:

1) Create a HCE service class that is extending the **HostApduService**
2) Register your HCE service in **AndroidManifest.xml**
3) Create an **XML-file in your resources** that defines the application identifier your HCE application in working on ("apduservice.xml")
4) Register the XML-file in **AndroidManifest.xml** to link the AID with your own HCE service

**That's all ?** - yes we don't need more steps to build a HCE application.

## Example output

```plaintext
TagId: 08ca20a8
selectApdu with AID: 00a4040006f2233445566700
selectApdu response: 0000
response length: 2 data: 0000
getDataApdu with file01: 00ca0000010100
response length: 20 data: 48434520426567696e6e65722041707020319000
HCE Beginner App 1
getDataApdu with file02: 00ca0000010200
response length: 20 data: 48434520426567696e6e65722041707020329000
HCE Beginner App 2
putDataApdu with file02: 00da00001d024e657720436f6e74656e7420696e2066696c654e756d62657220303200
response length: 2 data: 9000
SUCCESS
getDataApdu with file02: 00ca0000010200
response length: 30 data: 4e657720436f6e74656e7420696e2066696c654e756d6265722030329000
New Content in fileNumber 02
```

## Some data regarding  the reader application

The NFC reader application is designed to communicate with the HCE emulated tag. It will probably not work 
with other (real or emulated) NFC tags as it uses a static, proprietary Application Identifier ("F22334455667").

The reader is following this workflow:

- connect to the HCE tag by sending the **Select Application APDU** command
- send the **Get Data APDU** command to retrieve the content of file number 01 on the HCE tag
- send the **Get Data APDU** command to retrieve the content of file number 02 on the HCE tag
- send the **Put Data APDU** command to write a new string on file number 02 on the HCE tag
- send the **Get Data APDU** command to retrieve the new content of file number 02 on the HCE tag
- EOC - End of Communication

Latest update: Sep. 18.th, 2024

## License

Android HCE Beginner Appis available under the MIT license. See the LICENSE.md file for more info.
