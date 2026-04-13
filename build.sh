#!/bin/bash
# Build script for PredatorPreyDemo
# Requires Java 17+ (uses switch expressions and pattern matching)

mkdir -p build/classes

javac -d build/classes \
  src/model/Action.java \
  src/model/NeuralState.java \
  src/model/NeuralStateSpace.java \
  src/model/Agent.java \
  src/ui/Theme.java \
  src/ui/ArenaPanel.java \
  src/ui/AgentReadoutPanel.java \
  src/ui/MainFrame.java \
  src/main/PredatorPreyDemo.java

if [ $? -eq 0 ]; then
  echo "Compilation successful."
  echo "Main-Class: main.PredatorPreyDemo" > build/MANIFEST.MF
  jar cfm PredatorPrey.jar build/MANIFEST.MF -C build/classes .
  echo "JAR created: PredatorPrey.jar"
  echo "Run with:    java -jar PredatorPrey.jar"
else
  echo "Compilation failed."
fi
