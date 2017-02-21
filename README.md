# wgdiag
Jeep Grand Cheroke WG 2.7CRD diagnostic app

## Introduction

There doesn't seem to be easy way to get live data from different modules in Grand Cheroke WJ 2.7CRD (a.k.a. Grand Cherokee WG). This is my attempt to create such tool. The initial idea is to be able to read live data from the ECU, like MAF readings, MAP readings, injector corrections and some other bsaic details. The app requires ELM327 or compatible bluetooth OBDII interface (like STN1110 based devices). Fake devices (most of cheap clones) doesn't work as they don't support the required OBDII protocols.

## Current state

The app is able to maintain stable connection with the bluetooth OBDII device. The UI and functionality is very basic. It can read:
 * MAF specified and actual data
 * MAP specified and actual data
 * Injectors correction
 * IQ
 * RPM
 * Coolant temperature
 * IAT
 * TPS

The app is in early stage. It still lacks quite a few useful functionality - logging and graphic data representation. But with time - updates will come.

In the next versions I'll also add functionality for reading and deleting errors. I'll also try to add live data from different modules - gearbox, HVAC, ABS.

## Missing diagnostic commands

Currently the diagnostic commands file is missing (app/src/main/java/com/brum/wgdiag/command/diag/Packages.java). All of this started by talking with a guy behind http://jeepswj.com/ - he gave me the commands to extract the injector corrections data. With this I managed to extract additional data, decode it and map it to the listed data above. He has a plan for releasing a almost dealer level app and this free tool would be a problem for him.

Till I found a way to reverse engineer the same data by myself the file will not be uploaded. I plan to do this by sniffing the traffic from KTS and Launch X-431. This will take probably a month or two. After I manage to do that I'll probably post a blog on it and provide the commands file.
