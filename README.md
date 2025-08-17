# Event Sourcing Demo Application

This application is a showcase for Ralf Westphal's ideas regarding event-based applications.

These ideas are in Ralf's Substack articles found below.  They are worth a read:

- [Killing the Entity: Event Sourcing done the Epistemic Way](https://ralfwestphal.substack.com/p/killing-the-entity)
- [AQ over CRUD](https://ralfwestphal.substack.com/p/aq-over-crud)
- [True Agility Requires Event Sourcing](https://ralfwestphal.substack.com/p/true-agility-requires-event-sourcing)

# Event Sourcing Defined

1. Events are the source of truth. Other repositories may be hydrated from the event stream, for search or reporting or other special requirements, but these other repositories can always be wiped and re-hydrated from the event stream, with no loss of data.
2. Events are immutable

# Demo Scope

First, a simple in-memory event store capable of the basic use cases outlined in the above links.  This provides a simple foundation.

Next, investigate each of the following concerns.

- Audit: how to list the events related to a person, within a datetime range, from a specific IP address, or regarding a specific business entity.
- Access control: 
    1. how to define access control lists on events
    2. how to restrict a list to only events the current user can see
    3. how to verify the current user is allowed to issue a particular command 
      - functional access: Whether the user is allowed to issue this command in the first place
      - data access: Whether the user can issue this command for this paritcular business entity
- Reporting: how to project the data for consumption by a traditional report engine
- Data migration: how to migrate data from an existing CRUD application
- Scale: explore tools for larger systems
    1. Many event types, many commands, many relationships between events
    2. Hundreds, thousands, millions, billions of events
- Queries: 
    1. How to populate lists
    2. How to show reports
- Event handling: 
    1. configurable set of commands to raise in response to events
    2. allow for predicates, so a command can be raised for events on a certain object, but not for events on other objects of the same type
- Event routing
- Extensibility
    1. Feature flags: enable or disable particular event handlers
    2. Customer-specific code
       - custom event handler
       - custom UI

# Documentation

All documents as text files within this repository.

Maintain a requirements document and architecture diagrams.

Requirements document in org-mode.

Architecture diagram in Mermaid C4.

# Technology Stack

- Programming language: Java since that's the language I know best.
- Events from server to browser: SSE
- Events from browser to server: REST

# Constraints

- Support different event stores; no tight coupling to a particular implementation

# Some tools that may be useful

- [Java Faker](https://github.com/DiUS/java-faker) Generate a stream of test events
- Report engines:
    1. [Apache Superset](https://superset.apache.org/)
    2. [Eclipse BIRT](https://projects.eclipse.org/projects/technology.birt)
- Event stores:
    1. [Apache Druid](https://druid.apache.org/)
    2. [Apache Cassandra](https://cassandra.apache.org/_/index.html)
    
Note, I am allergic to the Confluent stack.

# General Approach to Event Sourcing Applications

1. What are the events? the commands? the queries?
   - Every command is mapped to one or more events.
   - Every query will retrieve events
   - The UI, or more generally portals, issue commands
   - The back end records events
   
2. Each command might be a class, and events are classes
   - Vertical Slice Architecture
   - UI (or any portal) sends a command, and the command records events 
   - Commands may return an entity id, or nothing
   - A portal might issue multiple commands back to back, establishing a data flow; earlier commands may raise events, later commands may query events and raise new events.  Each command interacts only with the event store.  Commands don't know about each other.
   
3. Command processing
   - check constraints (that is: by querying event stream).  
   - Events from the query results can be reduced to a data structure. 
   - This data structure is per-command, something like a DTO; avoid a single application-wide domain model (in other words: avoid the "Single Model Fallacy").
   - functions to evaluate rules or make decisions are pure functions, issuing a query and iterating over events.  No connection to commands or to other objects.  These functions do not appear in the public documentation.
   - if you need events from different filters, query the event stream with each filter and join the results into a single context... being careful about the performance impacts.
   - if constraints are OK, execute the command (transform / side effects ...) and generate events to record the changes made
   - if not, raise an error
   - command execution pipeline: query --> replay --> build model --> check model --> generate events --> record events
   
4. What to do if new, relevant events were raised between "replay" and "generate events"?
    - How to tell: execute the query again ... but this introduces yet another race condition: the time between this new query and when we record the new events
    - or, just see if more relevant events were added at all.  If more events were added, relevant to our use case, then we stop processing.  But again this introduces another race.
   
4. Commands do not return a payload, but they can return an envelope with status information (success/failure, reason for failure)
   
5. Queries return a data structure
   - sourced, one way or another, from the event stream 

6. Design documentation
   - List of commands; for each, the events recorded

7. Implement a vertical slice
   - Which events make up the context?  
   - What is the context model - what do I need to know about the state of the system right now?
   - What constraints do I need to check?
   - What decisions do I need to make?
   - Which events do I need to generate?
