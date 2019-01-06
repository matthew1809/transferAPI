## Basic API for transferring money between accounts

## Stack
Written in Kotlin \
Uses Maven build \
Tests using Junit and [Struktural](https://github.com/kennycason/struktural) \
Uses [SparkJava](http://sparkjava.com/) as API framework \
Data stored in memory, no DB implementation

## Usage
Simply build and run, API accessible on port 4567 (Spark default)

GET `/accounts`

GET `/accounts/:id`

POST `/accounts/new` \
Payload: `{"name": string, "email": string}`

POST `/accounts/:id/transfer` \
Payload: `{"recipient": int, "amount": double}`