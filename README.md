# metadataSorting
The util sorts metadata by alphabet for solutions based on 1C:Enterprise platform. It works with xml files of configuration.
There is CLI only. 
## Supported features
- Parent objects sorting, such as common modules, catalogs and etc. 
- Child objects sorting, such as attributes, tabular sections and etc. 

## Download and compile 
1. Download and install Java 8.
2. Install Apache Maven. http://maven.apache.org/

* Clone this repository (or download directly from github)
```
git clone https://github.com/lipido/metadataSorting.git
```
* Compile and run
```
cd metadataSorting
mvn package
cd target
java -jar metadataSorting.jar
```
## Usage
Open command line and type
```
java -jar metadataSorting.jar -h
```
press Enter.

For sorting child objects you should to use file metadataDescription.xml (you can find this file in example directory).