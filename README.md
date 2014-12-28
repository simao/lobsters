# Lobste.rs Twitter Bot

[![Build Status](https://travis-ci.org/simao/lobsters.png?branch=master)](https://travis-ci.org/simao/lobsters) 

Fetches a [lobste.rs](https://lobste.rs) XML feed and tweets items
newer than a set number of days and with a score greater than a given
number.

This is the source code behind
[lobstersbot](https://twitter.com/lobstersbot)

## Usage

    sbt assembly
    java -jar <jar file> --help

`--help` will show supported options.

