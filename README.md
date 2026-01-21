# CS307-BK1: Data Locality

This project investigates the critical role of data locality in software performance by conducting a comparative analysis between Object-Oriented Programming (OOP) and Data-Oriented Programming (DOP) paradigms. Through the implementation of a Java-based physics simulation involving bouncing circles, the study contrasts the traditional "Array of Structures" (AoS) layout, which relies on scattered heap objects, against the cache-friendly "Structure of Arrays" (SoA) layout that utilizes contiguous primitive arrays. By isolating memory access patterns in high-load scenarios, the experiment demonstrates that the DOP approach significantly reduces cache overhead, yielding a performance increase from 120 FPS to 250 FPS compared to the OOP model. These findings underscore the substantial impact of memory layout strategies on computational efficiency in data-intensive applications.

## Build

```bash
javac *.java
```

## Run

```bash
# With rendering
java DataOrientedSimulation 1000
java ObjectOrientedSimulation 1000
# Without rendering
java DataOrientedSimulation 1000000 --norender
java ObjectOrientedSimulation 1000000 --norender
```
