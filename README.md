# metadataSorting
The util sorts metadata by alphabet for solutions based on 1C:Enterprise platform. It works with xml files of configuration.
There is CLI only. 
## Supported features
- Parent objects sorting, such as common modules, catalogs and etc. 
- Child objects sorting, such as attributes, tabular sections and etc. 

## Download and compile 
* Download and install Java 8.

* Clone this repository (or download directly from github)
```
git clone https://github.com/fr13Dev/metadataSorting
```
* Compile and run
```
cd metadataSorting
jar cmvf META-INF/MANIFEST.MF  MetadataSorting.jar dev/fr13/*.class
java -jar MetadataSorting.jar
```
## Usage
Open command line and type
```
java -jar MetadataSorting.jar -h
```
For sorting child objects you should to use file metadataDescription.xml (you can find this file in example directory).