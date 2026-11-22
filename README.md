# mini-ledger

## What it is

A learning project: a small double-entry ledger service built with Java 21 and Spring Boot.
I use it to practice backend technologies step by step, such as database migration with Liquibase,
messaging with Kafka, observability, and deployment on Kubernetes.
It is not production code.

## Why double-entry

Every transfer is recorded as two entries: a debit on one account and a credit on another.
Because the two amounts are equal and opposite, the entries of every transfer always sum to zero.
This invariant makes errors easy to detect: if the total is not zero, something is wrong.

## Tech stack

- Java 21, Spring Boot 4
- PostgreSQL, Liquibase
- Testcontainers, JUnit 5, AssertJ
- GitHub Actions (CI)

## How to run the tests

Docker must be running, because the tests start a real PostgreSQL container with Testcontainers.
    
    ./mvnw verify           # MacOS / Linux
    .\mvnw.cmd verify       # Windows

## Status

Round 1 (in progress): project setup, CI, and the first Liquibase changelog.