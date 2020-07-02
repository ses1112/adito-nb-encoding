#!/bin/bash
read -p "Enter keystore password:" keystorepass
mvn clean install nbm:nbm -Dkeystorepass=$keystorepass
#mvn deploy