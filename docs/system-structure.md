# System Behavior (Architecture)

## Architecture Style

- Monolith (CLI App)

As a proof of concept, I need only a very simple interface.  The event store will be a simple Linux service, with a command line to submit and query events.

To illustrate how events may cause side effects, some events may result in sending email notifications.

## Component Diagram

![Components](components.png)

## Tech Stack

Programming Language: Java

Communication Mechanism: HTTP REST API 

## Repository Strategy

Mono-repo Repository Structure.  The event store and the client will be in the same repository.
