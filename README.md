![Travis](https://img.shields.io/travis/rust-lang/rust.svg) 

## General Discription:  
We are a team of 9 people all studying Internet Computing (Bsc) at University of Liverpool.   
We came up with the idea and created this website from the ground up, as part of our group project.   
We aim to provide a free platform to allow people to share their books with fellow book lovers, and in exchange can get new books for themselves.  
We believe this website will be an effective tool for students and lecturers alike.    
Website: <https://www.bookswop.me>  

## Team Members && Mailing List:   
+ [Ionut Brusten]:(I.R.Brusten@student.liverpool.ac.uk)  
+ [Huaiyu Dang]:(H.Dang2@student.liverpool.ac.uk)  
+ [James Friar]:(J.Friar@student.liverpool.ac.uk)  
+ [Chonghao Fu]:(C.Fu4@student.liverpool.ac.uk)  
+ [Julia Korcsinszka]:(J.Korcsinszka@student.liverpool.ac.uk)  
+ [Ngoc Duy Pham]:(N.Pham@student.liverpool.ac.uk)  
+ [Ruohan Su]:(R.Su5@student.liverpool.ac.uk)  
+ [Haoxuan Wang]:(H.Wang76@student.liverpool.ac.uk)  
+ [Dingfeng Yuan]:(D.Yuan3@student.liverpool.ac.uk)    
(The name list is based on alphabetical ordering)  

## Run the website:  

The project is built on an Play Framework 2.5.   

To open the project, an play framework 2.5 with activator is required.  
  
To run the website, these are also required:   

+ A server running a GNU/Linux release with JDK 8  
+ A SMTP mail server (Perhaps Amazon SES)  
+ A MySQL Database, passed on version 5.7  
+ A Google Book API key
+ Recommand: Nginx server to do the reverse proxy  

## Pre-Configurations:  
Please check the following files, search and replace ALL <To fill> label with your own server information OR your private key OR Google book API key.  

+ production.conf  
+ conf/application.conf  
+ app/services/CipherHelper.java  
+ app/controllers/BookController.java  

Additionally, if you are using Oracle JDK rather than Open JDK, please install Java Cryptography Extension (JCE).  

## SCredits: 
+ Google Book API  
+ Amazon AWS & SES & RDS  
+ The geolocation search method provided by Meituan Team  
+ mindrot and their jBCrypt Java Library  
+ Lightbend and Play framwork  

Note: The project is still under development so many issues exist and some of code has not been integrated.  
  
Released under Apache 2.0 LICENSE.   