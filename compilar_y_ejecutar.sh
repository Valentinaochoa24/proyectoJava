#!/bin/sh
rm -rf out
javac -encoding UTF-8 -d out $(find src -name "*.java") || exit 1
java -cp out co.edu.iudigital.tecnomovil.PruebasFuncionales
java -cp out co.edu.iudigital.tecnomovil.Main "$@"
