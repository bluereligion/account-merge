#AccountMerge

A coding challenge.

##Description


This service reads from a CSV file and combines its information with data from an API. This combined data is then written to a new CSV file.

Given a CSV file with the following columns including a header row containing:
* Account ID, Account Name, First Name, and Created On

And a Restful Status API: 
* http://localhost:8080/v1/accounts/{account_id}

The API returns data in the following JSON format:
* {"account_id": 12345, "status": "good", "created_on": "2011-01-12"}

The "Account ID" in the CSV lines up with the "account_id" in the API and the "created_on" in the API represents when the status was set.


For every line of data in the CSV, we want to:
* Pull the information from the API for the Account ID

* Merge it with the CSV data to output into a new CSV with columns of Account ID, First Name, Created On, Status, and Status Set On

The program is invoked as follows 


`account_merge <input_file> <output_file>`


For example:

`account_merge data/input.csv output.csv`




##Additional Requirements

1. There is no requirement for the ordering of the rows in the output file.

2. If the output file name passed in as a command-line argument already exists the app should overwrite the output file if it already exists.

3. Where possible errors are included in the output row.  Other problems are logged.



##Prerequisite
* Java - version 8 or higher
* Gradle build tool

To install gradle: https://gradle.org/install/


##Building
From inside the root directory, build using gradle:

`gradle clean build`


##Libraies Directly Used

For compiling and running:

    *  'com.google.guava:guava:28.0-jre'
    *  'org.springframework.boot:spring-boot-starter-web'
    *  'org.projectreactor:reactor-spring:1.0.1.RELEASE'
    *  'org.springframework.boot:spring-boot-starter-webflux'
    *  'com.fasterxml.jackson.core:jackson-databind:2.9.7'
    *  'com.fasterxml.jackson.core:jackson-core:2.9.7'

For compiling and testing:

    *  'junit:junit:4.12'
    *  'org.springframework.boot:spring-boot-starter-test'
    *  'org.mockito:mockito-core:2.15.0'


##Running

Note: The app must be built before it can be run.

To run the application, navigate inside the root directory where the compressed file was expanded. There is a shell script account_merge availble to run this from a Unix/Linux machine.

The permissions have been set to be executed by anyone. THIS MAY BE A SECURITY CONCERN.

* First method: 

`./account_merge data/input.csv output.csv`

* Second (As indicated by the requirements):

    Requires an update to the PATH environmental variable in Unix (`export PATH="$PATH:/path/to/approot"`)

`account_merge data/input.csv output.csv`


         
* The app will check if 2 params exist (and only 2 exist).
* The input.csv output.csv file names need to be different.

Additional validations Include:

* The inbound file is under the configurable filesize limit.
* The inbound file actually exists.
* The inbound file is not empty.
* The inbound file is readble by the application.


##Configuration

The configuration file is located: {root}/src/main/resources/application.yml


    application:
      #encoding options: UTF-8, UTF-16LE, UTF-16BE, UTF-16, US-ASCII, ISO-8859-1
      encoding: UTF-8
      # NO trailing /
      restStatusApi: http://localhost:8080
      maxInboundFileSizeMb: 10
      numOfRowConsumers: 2

* encoding - Allows the encoding scheme to be set to what the JVM allows. Please see: https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html
* restStatusApi - The protocol and host to the REST API service. No trailing '/'
* maxInboundFileSizeMb - The maximum file size allowed by the system (in MB).
* numOfRowConsumers - How many threads to generate for the processes that consume the file rows and transforms them into accounts.

Note: If a configuration parameter is changed, the app needs to be rebuilt before the changes will take effect.

#Implementation Details

* The Application uses Spring/Spring Boot.
* The application uses two blocking queues around producers and consumers that implement Java Callable. This allows the data to flow asynchronously.
* com.bluereligion.accountmerge.AccountMerge class is the commandline entry point.
* com.bluereligion.accountmerge.service.AccountService is a thin wrapper around the primary service. It injects config variables using Spring conventions.
* com.bluereligion.accountmerge.service.AccountServiceProvider is the primary service. It initializes the blocking queues and runs the producers and consumers.
* com.bluereligion.accountmerge.producer.RowProducer reads the file, strips the heder row and null/empty lines and puts the rest on a queue.
* com.bluereligion.accountmerge.consumer.RowConsumer reads the lines from the queue, parses them into Account objects. It then calls the Rest API for the remaining data points and places the accounts onto a secondary queue.
* com.bluereligion.accountmerge.consumer.AccountConsumer reads the accounts from the queue and writes them to the output file.
* com.bluereligion.accountmerge.client.AccountStatusClient uses the account Id to invoke the Rest API.
* com.bluereligion.accountmerge.dto.Account is the Account object that represents the user account.
* com.bluereligion.accountmerge.util.AccountMergeUtils is a utility class the encapsulates the low-level details for the overall service.


##CSV Issues That Are Corrected
The app looks to remedy many of the common issues associated with .csv processing.

     *  Clean
     *  -----
     *  Input: 23232,stark industries,Tony,5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *  Uneeded Quotatations
     *  --------------------
     *  Input: "23232",stark industries,Tony,5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *  Input: 23232,"stark industries",Tony,5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *  Input: 23232,stark industries,"Tony",5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *  Input: 23232,stark industries,Tony,"5-12-2015"
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *
     *  Escaped Comma
     *  -------------
     *  Input: 23232,stark""industries,Tony,5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *  Escaped Delimiter
     *  -----------------
     *  Input: 23232,"stark,industries",Tony,5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
##Tests
The app is unit tested with JUnit tests and are located here: {root}/src/test.

Once built, the test reports can be found here: {root}/build/reports/tests/test/index.html



##Refactoring Opportunities
* Integrate with a CSV Library
    * Apache Commons CSV
    * Open CSV
* If the date fields have differing formats (2012-03-01, 2012/03/01, 03-01-2012), enhance the utils class.


